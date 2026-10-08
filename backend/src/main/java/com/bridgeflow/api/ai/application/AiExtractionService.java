package com.bridgeflow.api.ai.application;

import static com.bridgeflow.api.ai.api.AiModels.AiJobResponse;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridgeflow.api.ai.domain.AiJob;
import com.bridgeflow.api.ai.domain.AiJobStatus;
import com.bridgeflow.api.ai.persistence.AiJobRepository;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider.ExtractionRequest;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider.GlossaryEntry;
import com.bridgeflow.api.audit.AuditAction;
import com.bridgeflow.api.audit.AuditService;
import com.bridgeflow.api.auth.persistence.AppUserRepository;
import com.bridgeflow.api.common.ResourceNotFoundException;
import com.bridgeflow.api.document.domain.DocumentStatus;
import com.bridgeflow.api.document.persistence.DocumentVersionRepository;
import com.bridgeflow.api.document.persistence.ProjectDocumentRepository;
import com.bridgeflow.api.document.storage.LocalDocumentStorage;
import com.bridgeflow.api.glossary.persistence.GlossaryTermRepository;
import com.bridgeflow.api.project.domain.ProjectStatus;
import com.bridgeflow.api.project.membership.ProjectAccessService;
import com.bridgeflow.api.project.membership.ProjectRole;
import com.bridgeflow.api.requirement.domain.ChangeType;
import com.bridgeflow.api.requirement.domain.Requirement;
import com.bridgeflow.api.requirement.domain.RequirementRevision;
import com.bridgeflow.api.requirement.persistence.RequirementRepository;
import com.bridgeflow.api.requirement.persistence.RequirementRevisionRepository;

@Service
@Transactional(readOnly = true)
public class AiExtractionService {

    private static final String PURPOSE = "REQUIREMENT_EXTRACTION";

    private final AiJobRepository jobRepository;
    private final ProjectDocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final RequirementRepository requirementRepository;
    private final RequirementRevisionRepository revisionRepository;
    private final GlossaryTermRepository glossaryRepository;
    private final AppUserRepository userRepository;
    private final ProjectAccessService accessService;
    private final LocalDocumentStorage storage;
    private final DocumentTextExtractor textExtractor;
    private final SensitiveTextRedactor redactor;
    private final RequirementExtractionProvider provider;
    private final AuditService auditService;
    private final AiJobResponseMapper responseMapper;
    private final int maxCandidates;

    public AiExtractionService(
        AiJobRepository jobRepository,
        ProjectDocumentRepository documentRepository,
        DocumentVersionRepository versionRepository,
        RequirementRepository requirementRepository,
        RequirementRevisionRepository revisionRepository,
        GlossaryTermRepository glossaryRepository,
        AppUserRepository userRepository,
        ProjectAccessService accessService,
        LocalDocumentStorage storage,
        DocumentTextExtractor textExtractor,
        SensitiveTextRedactor redactor,
        RequirementExtractionProvider provider,
        AuditService auditService,
        AiJobResponseMapper responseMapper,
        @Value("${bridgeflow.ai.max-candidates:25}") int maxCandidates
    ) {
        this.jobRepository = jobRepository;
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.requirementRepository = requirementRepository;
        this.revisionRepository = revisionRepository;
        this.glossaryRepository = glossaryRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.storage = storage;
        this.textExtractor = textExtractor;
        this.redactor = redactor;
        this.provider = provider;
        this.auditService = auditService;
        this.responseMapper = responseMapper;
        if (maxCandidates < 1 || maxCandidates > 100) {
            throw new IllegalArgumentException("AI max-candidates must be between 1 and 100");
        }
        this.maxCandidates = maxCandidates;
    }

    public List<AiJobResponse> list(UUID userId, UUID projectId) {
        accessService.requireMember(projectId, userId);
        return jobRepository.findAllForProject(projectId).stream()
            .map(responseMapper::toResponse)
            .toList();
    }

    @Transactional
    public AiJobResponse extract(UUID userId, UUID documentId, UUID versionId) {
        var document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu " + documentId + "."));
        var project = document.getProject();
        var projectId = project.getId();
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE);
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("Project đã archive nên không thể chạy AI.");
        }
        if (document.getStatus() == DocumentStatus.ARCHIVED) {
            throw new IllegalStateException("Tài liệu đã archive nên không thể chạy AI.");
        }
        if (!project.isAiEnabled()) {
            throw new IllegalStateException("AI chưa được bật cho project này.");
        }
        var version = versionRepository.findByIdAndDocumentId(versionId, documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version của tài liệu này."));
        var existing = jobRepository.findForVersionAndPurpose(versionId, PURPOSE).orElse(null);
        if (existing != null && existing.getStatus() != AiJobStatus.FAILED) return responseMapper.toResponse(existing);

        var job = existing == null
            ? new AiJob(
                project, version, provider.providerName(), provider.modelName(), userRepository.getReferenceById(userId)
            )
            : existing;
        job.start();
        jobRepository.saveAndFlush(job);
        auditService.record(projectId, userId, AuditAction.AI_EXTRACTION_REQUESTED, "AI_JOB", job.getId());

        List<ValidatedCandidate> candidates;
        try {
            var documentText = textExtractor.extract(storage.read(version.getStorageKey()), version.getContentType());
            var glossary = glossaryRepository.search(projectId, "").stream()
                .map(term -> new GlossaryEntry(
                    term.getJapaneseTerm(), term.getVietnameseTerm(), term.getNotes()
                ))
                .toList();
            var result = provider.extract(new ExtractionRequest(
                job.getCorrelationId(), redactor.redact(documentText), glossary, maxCandidates
            ));
            var rawCandidates = Objects.requireNonNull(result.requirements(), "AI requirements are required");
            if (rawCandidates.size() > maxCandidates) {
                throw new IllegalStateException("AI trả về quá số candidate cho phép.");
            }
            candidates = rawCandidates.stream()
                .map(candidate -> new ValidatedCandidate(
                    requireText(candidate.japaneseText(), "japaneseText"),
                    requireText(candidate.vietnameseText(), "vietnameseText"),
                    candidate.sourceAnchor()
                ))
                .toList();
        } catch (RuntimeException exception) {
            job.fail("AI_EXTRACTION_FAILED", safeMessage(exception));
            jobRepository.saveAndFlush(job);
            auditService.record(projectId, userId, AuditAction.AI_EXTRACTION_FAILED, "AI_JOB", job.getId());
            return responseMapper.toResponse(job);
        }

        var sequence = 1;
        for (var candidate : candidates) {
            var requirement = requirementRepository.save(new Requirement(
                project, nextDisplayKey(projectId, documentId, sequence++)
            ));
            revisionRepository.save(new RequirementRevision(
                requirement, 1, candidate.japaneseText(), candidate.vietnameseText(), ChangeType.ADDED, userId,
                versionId, candidate.sourceAnchor()
            ));
        }
        job.complete(candidates.size());
        jobRepository.saveAndFlush(job);
        auditService.record(projectId, userId, AuditAction.AI_EXTRACTION_COMPLETED, "AI_JOB", job.getId());
        return responseMapper.toResponse(job);
    }

    private String nextDisplayKey(UUID projectId, UUID documentId, int start) {
        var prefix = "AI-" + documentId.toString().substring(0, 8).toUpperCase() + "-";
        var sequence = start;
        while (sequence <= 999) {
            var candidate = prefix + "%03d".formatted(sequence++);
            if (requirementRepository.findByProjectIdAndDisplayKey(projectId, candidate).isEmpty()) return candidate;
        }
        throw new IllegalStateException("Không thể cấp mã requirement AI mới.");
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalStateException("AI thiếu trường " + field + ".");
        return value.trim();
    }

    private String safeMessage(RuntimeException exception) {
        var message = exception.getMessage();
        return message == null || message.isBlank() ? "AI extraction thất bại." : message;
    }

    private record ValidatedCandidate(String japaneseText, String vietnameseText, String sourceAnchor) {
    }
}
