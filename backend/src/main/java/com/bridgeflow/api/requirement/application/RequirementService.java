package com.bridgeflow.api.requirement.application;

import static com.bridgeflow.api.requirement.api.RequirementModels.ConfirmRevisionRequest;
import static com.bridgeflow.api.requirement.api.RequirementModels.CreateRequirementRequest;
import static com.bridgeflow.api.requirement.api.RequirementModels.CreateRevisionRequest;
import static com.bridgeflow.api.requirement.api.RequirementModels.RequirementResponse;
import static com.bridgeflow.api.requirement.api.RequirementModels.RevisionResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridgeflow.api.common.ResourceNotFoundException;
import com.bridgeflow.api.project.persistence.ProjectRepository;
import com.bridgeflow.api.requirement.domain.ChangeType;
import com.bridgeflow.api.requirement.domain.Requirement;
import com.bridgeflow.api.requirement.domain.RequirementRevision;
import com.bridgeflow.api.requirement.persistence.RequirementRepository;
import com.bridgeflow.api.requirement.persistence.RequirementRevisionRepository;

@Service
@Transactional(readOnly = true)
public class RequirementService {

    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;
    private final RequirementRevisionRepository revisionRepository;

    public RequirementService(
        ProjectRepository projectRepository,
        RequirementRepository requirementRepository,
        RequirementRevisionRepository revisionRepository
    ) {
        this.projectRepository = projectRepository;
        this.requirementRepository = requirementRepository;
        this.revisionRepository = revisionRepository;
    }

    public List<RequirementResponse> list(UUID projectId) {
        requireProject(projectId);
        return requirementRepository.findByProjectIdOrderByDisplayKeyAsc(projectId).stream()
            .map(requirement -> toResponse(requirement, false)).toList();
    }

    public RequirementResponse get(UUID requirementId) {
        return toResponse(findRequirement(requirementId), true);
    }

    @Transactional
    public RequirementResponse create(UUID projectId, CreateRequirementRequest request) {
        var project = requireProject(projectId);
        var displayKey = request.displayKey().trim().toUpperCase(Locale.ROOT);
        if (requirementRepository.findByProjectIdAndDisplayKey(projectId, displayKey).isPresent()) {
            throw new IllegalStateException("Requirement " + displayKey + " đã tồn tại trong project.");
        }
        var requirement = requirementRepository.save(new Requirement(project, displayKey));
        revisionRepository.save(new RequirementRevision(
            requirement, 1, request.japaneseText(), request.vietnameseText(), ChangeType.ADDED
        ));
        requirement.markReviewing();
        return toResponse(requirementRepository.save(requirement), true);
    }

    @Transactional
    public RequirementResponse addRevision(UUID requirementId, CreateRevisionRequest request) {
        var requirement = findRequirement(requirementId);
        var revisionNumber = Math.toIntExact(revisionRepository.countByRequirementId(requirementId) + 1);
        revisionRepository.save(new RequirementRevision(
            requirement, revisionNumber, request.japaneseText(), request.vietnameseText(), request.changeType()
        ));
        requirement.markReviewing();
        return toResponse(requirementRepository.save(requirement), true);
    }

    @Transactional
    public RequirementResponse confirm(
        UUID requirementId, UUID revisionId, ConfirmRevisionRequest request
    ) {
        var requirement = findRequirement(requirementId);
        var revision = revisionRepository.findByIdAndRequirementId(revisionId, requirementId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy revision thuộc requirement này."));
        var latestRevision = revisionRepository.findFirstByRequirementIdOrderByRevisionNumberDesc(requirementId)
            .orElseThrow(() -> new ResourceNotFoundException("Requirement chưa có revision."));
        if (!latestRevision.getId().equals(revisionId)) {
            throw new IllegalStateException("Chỉ revision mới nhất mới có thể được xác nhận.");
        }
        revision.confirm(request.reviewerId());
        revisionRepository.saveAndFlush(revision);
        requirement.pointToRevision(revision);
        requirementRepository.save(requirement);
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

    private RequirementResponse toResponse(Requirement requirement, boolean includeHistory) {
        var revisions = revisionRepository.findByRequirementIdOrderByRevisionNumberAsc(requirement.getId());
        var latest = revisions.stream().max(Comparator.comparingInt(RequirementRevision::getRevisionNumber))
            .map(this::toRevision).orElse(null);
        var history = includeHistory ? revisions.stream().map(this::toRevision).toList() : List.<RevisionResponse>of();
        return new RequirementResponse(
            requirement.getId(), requirement.getProject().getId(), requirement.getDisplayKey(),
            requirement.getStatus().name(), requirement.getCurrentRevisionId(), latest, history,
            requirement.getCreatedAt(), requirement.getUpdatedAt()
        );
    }

    private RevisionResponse toRevision(RequirementRevision revision) {
        return new RevisionResponse(
            revision.getId(), revision.getRevisionNumber(), revision.getJapaneseText(),
            revision.getVietnameseText(), revision.getChangeType().name(), revision.getReviewStatus().name(),
            revision.getCreatedAt(), revision.getConfirmedBy(), revision.getConfirmedAt()
        );
    }
}
