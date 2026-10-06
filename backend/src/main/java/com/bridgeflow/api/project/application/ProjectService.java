package com.bridgeflow.api.project.application;

import static com.bridgeflow.api.project.api.ProjectModels.CreateProjectRequest;
import static com.bridgeflow.api.project.api.ProjectModels.ProjectResponse;
import static com.bridgeflow.api.project.api.ProjectModels.UpdateProjectRequest;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridgeflow.api.common.ResourceNotFoundException;
import com.bridgeflow.api.project.domain.Project;
import com.bridgeflow.api.project.domain.ProjectStatus;
import com.bridgeflow.api.project.persistence.ProjectRepository;
import com.bridgeflow.api.requirement.persistence.RequirementRepository;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;

    public ProjectService(ProjectRepository projectRepository, RequirementRepository requirementRepository) {
        this.projectRepository = projectRepository;
        this.requirementRepository = requirementRepository;
    }

    public List<ProjectResponse> list(boolean includeArchived) {
        var projects = includeArchived
            ? projectRepository.findAllByOrderByUpdatedAtDesc()
            : projectRepository.findAllByStatusOrderByUpdatedAtDesc(ProjectStatus.ACTIVE);
        return projects.stream().map(this::toResponse).toList();
    }

    public ProjectResponse get(UUID projectId) {
        return toResponse(findProject(projectId));
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        var code = request.code().trim().toUpperCase(Locale.ROOT);
        if (projectRepository.findByCode(code).isPresent()) {
            throw new IllegalStateException("Mã project " + code + " đã tồn tại.");
        }
        return toResponse(projectRepository.save(new Project(code, request.name(), request.customerName())));
    }

    @Transactional
    public ProjectResponse update(UUID projectId, UpdateProjectRequest request) {
        var project = findProject(projectId);
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Project đã archive nên không thể chỉnh sửa.");
        }
        project.updateDetails(request.name(), request.customerName());
        return toResponse(projectRepository.saveAndFlush(project));
    }

    @Transactional
    public ProjectResponse archive(UUID projectId) {
        var project = findProject(projectId);
        project.archive();
        return toResponse(projectRepository.saveAndFlush(project));
    }

    public Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy project " + projectId + "."));
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
            project.getId(), project.getCode(), project.getName(), project.getCustomerName(),
            project.getStatus().name(), requirementRepository.countByProjectId(project.getId()),
            project.getCreatedAt(), project.getUpdatedAt()
        );
    }
}
