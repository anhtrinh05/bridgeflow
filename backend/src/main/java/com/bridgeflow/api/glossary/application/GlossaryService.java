package com.bridgeflow.api.glossary.application;

import static com.bridgeflow.api.glossary.api.GlossaryModels.GlossaryTermResponse;
import static com.bridgeflow.api.glossary.api.GlossaryModels.SaveGlossaryTermRequest;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridgeflow.api.audit.AuditAction;
import com.bridgeflow.api.audit.AuditService;
import com.bridgeflow.api.auth.persistence.AppUserRepository;
import com.bridgeflow.api.common.ResourceNotFoundException;
import com.bridgeflow.api.glossary.domain.GlossaryTerm;
import com.bridgeflow.api.glossary.persistence.GlossaryTermRepository;
import com.bridgeflow.api.project.domain.ProjectStatus;
import com.bridgeflow.api.project.membership.ProjectAccessService;
import com.bridgeflow.api.project.membership.ProjectRole;
import com.bridgeflow.api.project.persistence.ProjectRepository;

@Service
@Transactional(readOnly = true)
public class GlossaryService {

    private final GlossaryTermRepository termRepository;
    private final ProjectRepository projectRepository;
    private final AppUserRepository userRepository;
    private final ProjectAccessService accessService;
    private final AuditService auditService;

    public GlossaryService(
        GlossaryTermRepository termRepository,
        ProjectRepository projectRepository,
        AppUserRepository userRepository,
        ProjectAccessService accessService,
        AuditService auditService
    ) {
        this.termRepository = termRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.auditService = auditService;
    }

    public List<GlossaryTermResponse> list(UUID userId, UUID projectId, String query) {
        accessService.requireMember(projectId, userId);
        return termRepository.search(projectId, query == null ? "" : query.trim()).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public GlossaryTermResponse create(UUID userId, UUID projectId, SaveGlossaryTermRequest request) {
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE);
        var project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy project " + projectId + "."));
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Project đã archive nên không thể thêm thuật ngữ.");
        }
        var actor = userRepository.getReferenceById(userId);
        var term = termRepository.saveAndFlush(new GlossaryTerm(
            project, request.japaneseTerm(), request.vietnameseTerm(), request.notes(), actor
        ));
        auditService.record(projectId, userId, AuditAction.GLOSSARY_TERM_CREATED, "GLOSSARY_TERM", term.getId());
        return toResponse(term);
    }

    @Transactional
    public GlossaryTermResponse update(
        UUID userId,
        UUID projectId,
        UUID termId,
        SaveGlossaryTermRequest request
    ) {
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE);
        var term = findTerm(projectId, termId);
        if (term.getProject().getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Project đã archive nên không thể sửa thuật ngữ.");
        }
        term.update(request.japaneseTerm(), request.vietnameseTerm(), request.notes());
        termRepository.saveAndFlush(term);
        auditService.record(projectId, userId, AuditAction.GLOSSARY_TERM_UPDATED, "GLOSSARY_TERM", termId);
        return toResponse(term);
    }

    @Transactional
    public void delete(UUID userId, UUID projectId, UUID termId) {
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE);
        var term = findTerm(projectId, termId);
        if (term.getProject().getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Project đã archive nên không thể xóa thuật ngữ.");
        }
        termRepository.delete(term);
        termRepository.flush();
        auditService.record(projectId, userId, AuditAction.GLOSSARY_TERM_DELETED, "GLOSSARY_TERM", termId);
    }

    private GlossaryTerm findTerm(UUID projectId, UUID termId) {
        return termRepository.findByIdAndProjectId(termId, projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thuật ngữ trong project này."));
    }

    private GlossaryTermResponse toResponse(GlossaryTerm term) {
        return new GlossaryTermResponse(
            term.getId(), term.getProject().getId(), term.getJapaneseTerm(), term.getVietnameseTerm(),
            term.getNotes(), term.getCreatedById(), term.getCreatedAt(), term.getUpdatedAt()
        );
    }
}
