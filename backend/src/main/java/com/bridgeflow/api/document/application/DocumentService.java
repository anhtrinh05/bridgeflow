package com.bridgeflow.api.document.application;

import static com.bridgeflow.api.document.api.DocumentModels.DocumentContent;
import static com.bridgeflow.api.document.api.DocumentModels.DocumentResponse;
import static com.bridgeflow.api.document.api.DocumentModels.DocumentVersionResponse;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bridgeflow.api.audit.AuditAction;
import com.bridgeflow.api.audit.AuditService;
import com.bridgeflow.api.auth.persistence.AppUserRepository;
import com.bridgeflow.api.common.ResourceNotFoundException;
import com.bridgeflow.api.document.domain.DocumentStatus;
import com.bridgeflow.api.document.domain.DocumentVersion;
import com.bridgeflow.api.document.domain.ProjectDocument;
import com.bridgeflow.api.document.persistence.DocumentVersionRepository;
import com.bridgeflow.api.document.persistence.ProjectDocumentRepository;
import com.bridgeflow.api.document.storage.LocalDocumentStorage;
import com.bridgeflow.api.project.domain.ProjectStatus;
import com.bridgeflow.api.project.membership.ProjectAccessService;
import com.bridgeflow.api.project.membership.ProjectRole;
import com.bridgeflow.api.project.persistence.ProjectRepository;

@Service
@Transactional(readOnly = true)
public class DocumentService {

    private final ProjectDocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final ProjectRepository projectRepository;
    private final AppUserRepository userRepository;
    private final ProjectAccessService accessService;
    private final AuditService auditService;
    private final LocalDocumentStorage storage;

    public DocumentService(
        ProjectDocumentRepository documentRepository,
        DocumentVersionRepository versionRepository,
        ProjectRepository projectRepository,
        AppUserRepository userRepository,
        ProjectAccessService accessService,
        AuditService auditService,
        LocalDocumentStorage storage
    ) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.auditService = auditService;
        this.storage = storage;
    }

    public List<DocumentResponse> list(UUID userId, UUID projectId, boolean includeArchived) {
        accessService.requireMember(projectId, userId);
        var documents = includeArchived
            ? documentRepository.findAllByProjectIdOrderByUpdatedAtDesc(projectId)
            : documentRepository.findAllByProjectIdAndStatusOrderByUpdatedAtDesc(projectId, DocumentStatus.ACTIVE);
        return documents.stream().map(this::toResponse).toList();
    }

    @Transactional
    public DocumentResponse create(UUID userId, UUID projectId, String title, MultipartFile file) {
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE, ProjectRole.DEVELOPER);
        var project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy project " + projectId + "."));
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Project đã archive nên không thể upload tài liệu.");
        }
        var document = documentRepository.saveAndFlush(new ProjectDocument(project, title));
        return addStoredVersion(userId, document, 1, file, AuditAction.DOCUMENT_UPLOADED);
    }

    @Transactional
    public DocumentResponse addVersion(UUID userId, UUID documentId, MultipartFile file) {
        var document = findDocument(documentId);
        var projectId = document.getProject().getId();
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE, ProjectRole.DEVELOPER);
        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new IllegalStateException("Tài liệu đã archive nên không thể thêm version.");
        }
        var nextVersion = Math.toIntExact(versionRepository.countByDocumentId(documentId) + 1);
        return addStoredVersion(userId, document, nextVersion, file, AuditAction.DOCUMENT_VERSION_ADDED);
    }

    public DocumentContent content(UUID userId, UUID documentId, UUID versionId) {
        var document = findDocument(documentId);
        accessService.requireMember(document.getProject().getId(), userId);
        var version = versionRepository.findByIdAndDocumentId(versionId, documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version của tài liệu này."));
        return new DocumentContent(
            version.getOriginalFilename(), version.getContentType(), storage.read(version.getStorageKey())
        );
    }

    @Transactional
    public DocumentResponse archive(UUID userId, UUID documentId) {
        var document = findDocument(documentId);
        var projectId = document.getProject().getId();
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE);
        document.archive();
        documentRepository.saveAndFlush(document);
        auditService.record(projectId, userId, AuditAction.DOCUMENT_ARCHIVED, "DOCUMENT", documentId);
        return toResponse(document);
    }

    private DocumentResponse addStoredVersion(
        UUID userId,
        ProjectDocument document,
        int versionNumber,
        MultipartFile file,
        AuditAction action
    ) {
        LocalDocumentStorage.StoredFile stored = null;
        try {
            stored = storage.store(document.getProject().getId(), document.getId(), file);
            var actor = userRepository.getReferenceById(userId);
            var version = versionRepository.saveAndFlush(new DocumentVersion(
                document, versionNumber, stored.originalFilename(), stored.contentType(), stored.sizeBytes(),
                stored.sha256(), stored.storageKey(), actor
            ));
            auditService.record(document.getProject().getId(), userId, action, "DOCUMENT_VERSION", version.getId());
            return toResponse(document);
        } catch (RuntimeException exception) {
            if (stored != null) storage.deleteQuietly(stored.storageKey());
            throw exception;
        }
    }

    private ProjectDocument findDocument(UUID documentId) {
        return documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu " + documentId + "."));
    }

    private DocumentResponse toResponse(ProjectDocument document) {
        var versions = versionRepository.findAllByDocumentIdOrderByVersionNumberDesc(document.getId());
        var responses = versions.stream().map(this::toVersionResponse).toList();
        return new DocumentResponse(
            document.getId(), document.getProject().getId(), document.getTitle(), document.getStatus().name(),
            responses.isEmpty() ? null : responses.get(0), responses,
            document.getCreatedAt(), document.getUpdatedAt(), document.getArchivedAt()
        );
    }

    private DocumentVersionResponse toVersionResponse(DocumentVersion version) {
        return new DocumentVersionResponse(
            version.getId(), version.getVersionNumber(), version.getOriginalFilename(), version.getContentType(),
            version.getSizeBytes(), version.getSha256(), version.getUploadedById(), version.getCreatedAt()
        );
    }
}
