package com.bridgeflow.api.requirement.application;

import static com.bridgeflow.api.requirement.api.RequirementRelationModels.CreateRequirementRelationRequest;
import static com.bridgeflow.api.requirement.api.RequirementRelationModels.RequirementRelationResponse;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridgeflow.api.audit.AuditAction;
import com.bridgeflow.api.audit.AuditService;
import com.bridgeflow.api.common.ResourceNotFoundException;
import com.bridgeflow.api.project.membership.ProjectAccessService;
import com.bridgeflow.api.project.membership.ProjectRole;
import com.bridgeflow.api.requirement.domain.Requirement;
import com.bridgeflow.api.requirement.domain.RequirementRelation;
import com.bridgeflow.api.requirement.persistence.RequirementRelationRepository;
import com.bridgeflow.api.requirement.persistence.RequirementRepository;

@Service
@Transactional(readOnly = true)
public class RequirementRelationService {
    private final RequirementRepository requirementRepository;
    private final RequirementRelationRepository relationRepository;
    private final ProjectAccessService accessService;
    private final AuditService auditService;

    public RequirementRelationService(
        RequirementRepository requirementRepository,
        RequirementRelationRepository relationRepository,
        ProjectAccessService accessService,
        AuditService auditService
    ) {
        this.requirementRepository = requirementRepository;
        this.relationRepository = relationRepository;
        this.accessService = accessService;
        this.auditService = auditService;
    }

    public List<RequirementRelationResponse> list(UUID userId, UUID requirementId) {
        var requirement = requireRequirement(requirementId);
        accessService.requireMember(requirement.getProject().getId(), userId);
        return relationRepository.findAllForRequirement(requirementId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public RequirementRelationResponse create(
        UUID userId, UUID sourceRequirementId, CreateRequirementRelationRequest request
    ) {
        var source = requireRequirement(sourceRequirementId);
        var projectId = source.getProject().getId();
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE);
        var target = requireRequirement(request.targetRequirementId());
        if (sourceRequirementId.equals(target.getId())) {
            throw new IllegalArgumentException("Requirement không thể liên kết với chính nó.");
        }
        if (!projectId.equals(target.getProject().getId())) {
            throw new IllegalArgumentException("Hai requirement phải thuộc cùng một project.");
        }
        if (source.isArchived() || target.isArchived()) {
            throw new IllegalStateException("Không thể tạo relation với requirement đã archive.");
        }
        if (relationRepository.existsBySourceRequirementIdAndTargetRequirementIdAndRelationType(
            sourceRequirementId, target.getId(), request.relationType()
        )) {
            throw new IllegalStateException("Requirement relation này đã tồn tại.");
        }
        var relation = relationRepository.saveAndFlush(new RequirementRelation(
            source, target, request.relationType(), userId
        ));
        auditService.record(projectId, userId, AuditAction.REQUIREMENT_RELATION_CREATED,
            "REQUIREMENT_RELATION", relation.getId());
        return toResponse(relation);
    }

    @Transactional
    public void delete(UUID userId, UUID requirementId, UUID relationId) {
        var requirement = requireRequirement(requirementId);
        var projectId = requirement.getProject().getId();
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE);
        var relation = relationRepository.findForRequirement(relationId, requirementId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy requirement relation này."));
        relationRepository.delete(relation);
        relationRepository.flush();
        auditService.record(projectId, userId, AuditAction.REQUIREMENT_RELATION_DELETED,
            "REQUIREMENT_RELATION", relationId);
    }

    private RequirementRelationResponse toResponse(RequirementRelation relation) {
        return new RequirementRelationResponse(
            relation.getId(), relation.getSourceRequirement().getId(), relation.getSourceRequirement().getDisplayKey(),
            relation.getTargetRequirement().getId(), relation.getTargetRequirement().getDisplayKey(),
            relation.getRelationType(), relation.getCreatedBy(), relation.getCreatedAt()
        );
    }

    private Requirement requireRequirement(UUID requirementId) {
        return requirementRepository.findById(requirementId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy requirement " + requirementId + "."));
    }
}
