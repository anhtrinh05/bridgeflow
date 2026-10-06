package com.bridgeflow.api.project.application;

import static com.bridgeflow.api.project.api.ProjectModels.CreateProjectRequest;
import static com.bridgeflow.api.project.api.ProjectModels.ProjectResponse;
import static com.bridgeflow.api.project.api.ProjectModels.UpdateProjectRequest;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridgeflow.api.audit.AuditAction;
import com.bridgeflow.api.audit.AuditService;
import com.bridgeflow.api.auth.persistence.AppUserRepository;
import com.bridgeflow.api.common.ResourceNotFoundException;
import com.bridgeflow.api.project.domain.Project;
import com.bridgeflow.api.project.domain.ProjectStatus;
import com.bridgeflow.api.project.persistence.ProjectRepository;
import com.bridgeflow.api.project.membership.ProjectAccessService;
import com.bridgeflow.api.project.membership.ProjectMember;
import com.bridgeflow.api.project.membership.ProjectMemberRepository;
import com.bridgeflow.api.project.membership.ProjectRole;
import com.bridgeflow.api.requirement.persistence.RequirementRepository;
import com.bridgeflow.api.requirement.domain.RequirementStatus;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;
    private final AppUserRepository userRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectAccessService accessService;
    private final AuditService auditService;

    public ProjectService(
        ProjectRepository projectRepository,
        RequirementRepository requirementRepository,
        AppUserRepository userRepository,
        ProjectMemberRepository memberRepository,
        ProjectAccessService accessService,
        AuditService auditService
    ) {
        this.projectRepository = projectRepository;
        this.requirementRepository = requirementRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.accessService = accessService;
        this.auditService = auditService;
    }

    public List<ProjectResponse> list(UUID userId, boolean includeArchived) {
        return memberRepository.findAllByUserIdOrderByProjectUpdatedAtDesc(userId).stream()
            .filter(member -> includeArchived || member.getProject().getStatus() == ProjectStatus.ACTIVE)
            .map(member -> toResponse(member.getProject(), member.getRole()))
            .toList();
    }

    public ProjectResponse get(UUID userId, UUID projectId) {
        var member = accessService.requireMember(projectId, userId);
        return toResponse(member.getProject(), member.getRole());
    }

    @Transactional
    public ProjectResponse create(UUID userId, CreateProjectRequest request) {
        var code = request.code().trim().toUpperCase(Locale.ROOT);
        if (projectRepository.findByCode(code).isPresent()) {
            throw new IllegalStateException("Mã project " + code + " đã tồn tại.");
        }
        var project = projectRepository.save(new Project(code, request.name(), request.customerName()));
        var user = userRepository.getReferenceById(userId);
        memberRepository.save(new ProjectMember(project, user, ProjectRole.ADMIN));
        auditService.record(project.getId(), userId, AuditAction.PROJECT_CREATED, "PROJECT", project.getId());
        return toResponse(project, ProjectRole.ADMIN);
    }

    @Transactional
    public ProjectResponse update(UUID userId, UUID projectId, UpdateProjectRequest request) {
        var member = accessService.requireRole(projectId, userId, ProjectRole.ADMIN);
        var project = member.getProject();
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Project đã archive nên không thể chỉnh sửa.");
        }
        project.updateDetails(request.name(), request.customerName());
        projectRepository.saveAndFlush(project);
        auditService.record(projectId, userId, AuditAction.PROJECT_UPDATED, "PROJECT", projectId);
        return toResponse(project, member.getRole());
    }

    @Transactional
    public ProjectResponse archive(UUID userId, UUID projectId) {
        var member = accessService.requireRole(projectId, userId, ProjectRole.ADMIN);
        var project = member.getProject();
        project.archive();
        projectRepository.saveAndFlush(project);
        auditService.record(projectId, userId, AuditAction.PROJECT_ARCHIVED, "PROJECT", projectId);
        return toResponse(project, member.getRole());
    }

    public Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy project " + projectId + "."));
    }

    private ProjectResponse toResponse(Project project, ProjectRole role) {
        return new ProjectResponse(
            project.getId(), project.getCode(), project.getName(), project.getCustomerName(),
            project.getStatus().name(), role.name(),
            requirementRepository.countByProjectIdAndStatusNot(project.getId(), RequirementStatus.ARCHIVED),
            project.getCreatedAt(), project.getUpdatedAt()
        );
    }
}
