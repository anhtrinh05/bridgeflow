package com.bridgeflow.api.requirement.application;

import static com.bridgeflow.api.requirement.api.RequirementModels.CreateRequirementRequest;
import static com.bridgeflow.api.requirement.api.RequirementModels.CreateRevisionRequest;
import static com.bridgeflow.api.requirement.api.RequirementModels.RequirementResponse;
import static com.bridgeflow.api.requirement.api.RequirementModels.RequirementPageResponse;
import static com.bridgeflow.api.requirement.api.RequirementModels.RevisionResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridgeflow.api.audit.AuditAction;
import com.bridgeflow.api.audit.AuditService;
import com.bridgeflow.api.common.ResourceNotFoundException;
import com.bridgeflow.api.project.membership.ProjectAccessService;
import com.bridgeflow.api.project.membership.ProjectRole;
import com.bridgeflow.api.project.persistence.ProjectRepository;
import com.bridgeflow.api.project.domain.ProjectStatus;
import com.bridgeflow.api.requirement.domain.ChangeType;
import com.bridgeflow.api.requirement.domain.Requirement;
import com.bridgeflow.api.requirement.domain.RequirementRevision;
import com.bridgeflow.api.requirement.domain.RequirementStatus;
import com.bridgeflow.api.requirement.persistence.RequirementRepository;
import com.bridgeflow.api.requirement.persistence.RequirementRevisionRepository;

@Service
@Transactional(readOnly = true)
public class RequirementService {

    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;
    private final RequirementRevisionRepository revisionRepository;
    private final ProjectAccessService accessService;
    private final AuditService auditService;

    public RequirementService(
        ProjectRepository projectRepository,
        RequirementRepository requirementRepository,
        RequirementRevisionRepository revisionRepository,
        ProjectAccessService accessService,
        AuditService auditService
    ) {
        this.projectRepository = projectRepository;
        this.requirementRepository = requirementRepository;
        this.revisionRepository = revisionRepository;
        this.accessService = accessService;
        this.auditService = auditService;
    }

    public RequirementPageResponse list(
        UUID userId,
        UUID projectId,
        RequirementStatus status,
        boolean includeArchived,
        String query,
        int page,
        int size,
        String sortBy,
        String direction
    ) {
        accessService.requireMember(projectId, userId);
        requireProject(projectId);
        var sortProperty = switch (sortBy) {
            case "displayKey", "status", "updatedAt" -> sortBy;
            default -> throw new IllegalArgumentException("sortBy chỉ hỗ trợ displayKey, status hoặc updatedAt.");
        };
        var sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        var result = requirementRepository.search(
            projectId,
            status,
            includeArchived,
            query == null ? "" : query.trim(),
            PageRequest.of(page, size, Sort.by(sortDirection, sortProperty))
        );
        return new RequirementPageResponse(
            result.getContent().stream().map(requirement -> toResponse(requirement, false)).toList(),
            result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()
        );
    }

    public RequirementResponse get(UUID userId, UUID requirementId) {
        var requirement = findRequirement(requirementId);
        accessService.requireMember(requirement.getProject().getId(), userId);
        return toResponse(requirement, true);
    }

    @Transactional
    public RequirementResponse create(UUID userId, UUID projectId, CreateRequirementRequest request) {
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE, ProjectRole.DEVELOPER);
        var project = requireProject(projectId);
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Project đã archive nên không thể thêm requirement.");
        }
        var displayKey = request.displayKey().trim().toUpperCase(Locale.ROOT);
        if (requirementRepository.findByProjectIdAndDisplayKey(projectId, displayKey).isPresent()) {
            throw new IllegalStateException("Requirement " + displayKey + " đã tồn tại trong project.");
        }
        var requirement = requirementRepository.save(new Requirement(project, displayKey));
        revisionRepository.save(new RequirementRevision(
            requirement, 1, request.japaneseText(), request.vietnameseText(), ChangeType.ADDED, userId
        ));
        requirement.markReviewing();
        requirementRepository.save(requirement);
        auditService.record(projectId, userId, AuditAction.REQUIREMENT_CREATED, "REQUIREMENT", requirement.getId());
        return toResponse(requirement, true);
    }

    @Transactional
    public RequirementResponse addRevision(UUID userId, UUID requirementId, CreateRevisionRequest request) {
        var requirement = findRequirement(requirementId);
        var projectId = requirement.getProject().getId();
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE, ProjectRole.DEVELOPER);
        requireActive(requirement);
        var revisionNumber = Math.toIntExact(revisionRepository.countByRequirementId(requirementId) + 1);
        revisionRepository.save(new RequirementRevision(
            requirement, revisionNumber, request.japaneseText(), request.vietnameseText(), request.changeType(), userId
        ));
        requirement.markReviewing();
        requirementRepository.save(requirement);
        auditService.record(projectId, userId, AuditAction.REQUIREMENT_REVISED, "REQUIREMENT", requirementId);
        return toResponse(requirement, true);
    }

    @Transactional
    public RequirementResponse confirm(
        UUID userId, UUID requirementId, UUID revisionId
    ) {
        var requirement = findRequirement(requirementId);
        var projectId = requirement.getProject().getId();
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE);
        requireActive(requirement);
        var revision = revisionRepository.findByIdAndRequirementId(revisionId, requirementId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy revision thuộc requirement này."));
        var latestRevision = revisionRepository.findFirstByRequirementIdOrderByRevisionNumberDesc(requirementId)
            .orElseThrow(() -> new ResourceNotFoundException("Requirement chưa có revision."));
        if (!latestRevision.getId().equals(revisionId)) {
            throw new IllegalStateException("Chỉ revision mới nhất mới có thể được xác nhận.");
        }
        revision.confirm(userId);
        revisionRepository.saveAndFlush(revision);
        requirement.pointToRevision(revision);
        requirementRepository.save(requirement);
        auditService.record(projectId, userId, AuditAction.REQUIREMENT_CONFIRMED, "REQUIREMENT_REVISION", revisionId);
        return toResponse(requirement, true);
    }

    @Transactional
    public RequirementResponse archive(UUID userId, UUID requirementId) {
        var requirement = findRequirement(requirementId);
        var projectId = requirement.getProject().getId();
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE);
        requirement.archive();
        requirementRepository.saveAndFlush(requirement);
        auditService.record(projectId, userId, AuditAction.REQUIREMENT_ARCHIVED, "REQUIREMENT", requirementId);
        return toResponse(requirement, true);
    }

    private com.bridgeflow.api.project.domain.Project requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy project " + projectId + "."));
    }

    private Requirement findRequirement(UUID requirementId) {
        return requirementRepository.findById(requirementId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy requirement " + requirementId + "."));
    }

    private void requireActive(Requirement requirement) {
        if (requirement.isArchived()) {
            throw new IllegalStateException("Requirement đã archive nên không thể thay đổi.");
        }
    }

    private RequirementResponse toResponse(Requirement requirement, boolean includeHistory) {
        var revisions = revisionRepository.findByRequirementIdOrderByRevisionNumberAsc(requirement.getId());
        var latest = revisions.stream().max(Comparator.comparingInt(RequirementRevision::getRevisionNumber))
            .map(this::toRevision).orElse(null);
        var history = includeHistory ? revisions.stream().map(this::toRevision).toList() : List.<RevisionResponse>of();
        return new RequirementResponse(
            requirement.getId(), requirement.getProject().getId(), requirement.getDisplayKey(),
            requirement.getStatus().name(), requirement.getCurrentRevisionId(), latest, history,
            requirement.getCreatedAt(), requirement.getUpdatedAt(), requirement.getArchivedAt()
        );
    }

    private RevisionResponse toRevision(RequirementRevision revision) {
        return new RevisionResponse(
            revision.getId(), revision.getRevisionNumber(), revision.getJapaneseText(),
            revision.getVietnameseText(), revision.getChangeType().name(), revision.getReviewStatus().name(),
            revision.getDocumentVersionId(), revision.getSourceAnchor(),
            revision.getCreatedAt(), revision.getConfirmedBy(), revision.getConfirmedAt()
        );
    }
}
