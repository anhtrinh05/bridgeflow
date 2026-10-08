package com.bridgeflow.api.testcase.application;

import static com.bridgeflow.api.testcase.api.TestCaseModels.TestCaseResponse;
import static com.bridgeflow.api.testcase.api.TestCaseModels.TestCaseWorkspaceResponse;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridgeflow.api.ai.application.AiJobResponseMapper;
import com.bridgeflow.api.ai.application.SensitiveTextRedactor;
import com.bridgeflow.api.ai.domain.AiJob;
import com.bridgeflow.api.ai.domain.AiJobStatus;
import com.bridgeflow.api.ai.persistence.AiJobRepository;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider.ApprovedCriterion;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider.GlossaryEntry;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider.TestCaseRequest;
import com.bridgeflow.api.analysis.domain.AcceptanceCriterion;
import com.bridgeflow.api.analysis.domain.ArtifactReviewStatus;
import com.bridgeflow.api.analysis.persistence.AcceptanceCriterionRepository;
import com.bridgeflow.api.audit.AuditAction;
import com.bridgeflow.api.audit.AuditService;
import com.bridgeflow.api.auth.persistence.AppUserRepository;
import com.bridgeflow.api.common.ResourceNotFoundException;
import com.bridgeflow.api.glossary.persistence.GlossaryTermRepository;
import com.bridgeflow.api.project.domain.ProjectStatus;
import com.bridgeflow.api.project.membership.ProjectAccessService;
import com.bridgeflow.api.project.membership.ProjectRole;
import com.bridgeflow.api.requirement.domain.Requirement;
import com.bridgeflow.api.requirement.domain.RequirementRevision;
import com.bridgeflow.api.requirement.persistence.RequirementRepository;
import com.bridgeflow.api.requirement.persistence.RequirementRevisionRepository;
import com.bridgeflow.api.testcase.domain.TestCasePriority;
import com.bridgeflow.api.testcase.domain.VerificationTestCase;
import com.bridgeflow.api.testcase.persistence.VerificationTestCaseRepository;

@Service
@Transactional(readOnly = true)
public class TestCaseService {
    private static final String PURPOSE = "TEST_CASE_GENERATION";

    private final RequirementRepository requirementRepository;
    private final RequirementRevisionRepository revisionRepository;
    private final AcceptanceCriterionRepository criterionRepository;
    private final VerificationTestCaseRepository testCaseRepository;
    private final AiJobRepository jobRepository;
    private final GlossaryTermRepository glossaryRepository;
    private final AppUserRepository userRepository;
    private final ProjectAccessService accessService;
    private final RequirementExtractionProvider provider;
    private final SensitiveTextRedactor redactor;
    private final AuditService auditService;
    private final AiJobResponseMapper responseMapper;
    private final int maxTestCases;

    public TestCaseService(
        RequirementRepository requirementRepository, RequirementRevisionRepository revisionRepository,
        AcceptanceCriterionRepository criterionRepository, VerificationTestCaseRepository testCaseRepository,
        AiJobRepository jobRepository, GlossaryTermRepository glossaryRepository,
        AppUserRepository userRepository, ProjectAccessService accessService,
        RequirementExtractionProvider provider, SensitiveTextRedactor redactor,
        AuditService auditService, AiJobResponseMapper responseMapper,
        @Value("${bridgeflow.ai.max-test-cases:20}") int maxTestCases
    ) {
        this.requirementRepository = requirementRepository;
        this.revisionRepository = revisionRepository;
        this.criterionRepository = criterionRepository;
        this.testCaseRepository = testCaseRepository;
        this.jobRepository = jobRepository;
        this.glossaryRepository = glossaryRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.provider = provider;
        this.redactor = redactor;
        this.auditService = auditService;
        this.responseMapper = responseMapper;
        if (maxTestCases < 1 || maxTestCases > 100) {
            throw new IllegalArgumentException("AI max-test-cases must be between 1 and 100");
        }
        this.maxTestCases = maxTestCases;
    }

    public TestCaseWorkspaceResponse get(UUID userId, UUID requirementId, UUID revisionId) {
        var target = requireTarget(requirementId, revisionId);
        accessService.requireMember(target.requirement().getProject().getId(), userId);
        return toResponse(target.revision());
    }

    @Transactional
    public TestCaseWorkspaceResponse generate(UUID userId, UUID requirementId, UUID revisionId) {
        var target = requireTarget(requirementId, revisionId);
        var project = target.requirement().getProject();
        var projectId = project.getId();
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE);
        if (project.getStatus() == ProjectStatus.ARCHIVED || target.requirement().isArchived()) {
            throw new IllegalStateException("Requirement thuộc project đã archive nên không thể chạy AI.");
        }
        if (!project.isAiEnabled()) throw new IllegalStateException("AI chưa được bật cho project này.");

        var existing = jobRepository.findForRevisionAndPurpose(revisionId, PURPOSE).orElse(null);
        if (existing != null && existing.getStatus() != AiJobStatus.FAILED) return toResponse(target.revision());

        var approved = criterionRepository.findAllForRevisionAndStatus(revisionId, ArtifactReviewStatus.APPROVED);
        if (approved.isEmpty()) {
            throw new IllegalStateException("Cần duyệt ít nhất một acceptance criterion trước khi tạo test case.");
        }
        var approvedById = approved.stream().collect(Collectors.toMap(AcceptanceCriterion::getId, Function.identity()));
        var job = existing == null
            ? new AiJob(project, target.revision(), PURPOSE, provider.providerName(), provider.modelName(), userRepository.getReferenceById(userId))
            : existing;
        job.start();
        jobRepository.saveAndFlush(job);
        auditService.record(projectId, userId, AuditAction.AI_TEST_CASE_GENERATION_REQUESTED, "AI_JOB", job.getId());

        List<ValidatedTestCase> candidates;
        try {
            var glossary = glossaryRepository.search(projectId, "").stream()
                .map(term -> new GlossaryEntry(term.getJapaneseTerm(), term.getVietnameseTerm(), term.getNotes()))
                .toList();
            var criteria = approved.stream().map(criterion -> new ApprovedCriterion(
                criterion.getId(), redactor.redact(criterion.getJapaneseText()), redactor.redact(criterion.getVietnameseText())
            )).toList();
            var result = provider.generateTestCases(new TestCaseRequest(
                job.getCorrelationId(), redactor.redact(target.revision().getJapaneseText()),
                redactor.redact(target.revision().getVietnameseText()), criteria, glossary, maxTestCases
            ));
            var raw = Objects.requireNonNull(result.testCases(), "AI test cases are required");
            if (raw.size() > maxTestCases) throw new IllegalStateException("AI trả về quá số test case cho phép.");
            candidates = raw.stream().map(candidate -> {
                if (!approvedById.containsKey(candidate.acceptanceCriterionId())) {
                    throw new IllegalStateException("AI liên kết test case với acceptance criterion không hợp lệ.");
                }
                return new ValidatedTestCase(
                    candidate.acceptanceCriterionId(), requireText(candidate.titleJapanese(), "titleJapanese"),
                    requireText(candidate.titleVietnamese(), "titleVietnamese"),
                    requireText(candidate.preconditionsJapanese(), "preconditionsJapanese"),
                    requireText(candidate.preconditionsVietnamese(), "preconditionsVietnamese"),
                    requireText(candidate.stepsJapanese(), "stepsJapanese"),
                    requireText(candidate.stepsVietnamese(), "stepsVietnamese"),
                    requireText(candidate.expectedResultJapanese(), "expectedResultJapanese"),
                    requireText(candidate.expectedResultVietnamese(), "expectedResultVietnamese"),
                    TestCasePriority.valueOf(requireText(candidate.priority(), "priority"))
                );
            }).toList();
        } catch (RuntimeException exception) {
            job.fail("AI_TEST_CASE_GENERATION_FAILED", safeMessage(exception));
            jobRepository.saveAndFlush(job);
            auditService.record(projectId, userId, AuditAction.AI_TEST_CASE_GENERATION_FAILED, "AI_JOB", job.getId());
            return toResponse(target.revision());
        }

        testCaseRepository.saveAll(candidates.stream().map(candidate -> new VerificationTestCase(
            target.revision(), approvedById.get(candidate.criterionId()), job,
            candidate.titleJapanese(), candidate.titleVietnamese(),
            candidate.preconditionsJapanese(), candidate.preconditionsVietnamese(),
            candidate.stepsJapanese(), candidate.stepsVietnamese(),
            candidate.expectedResultJapanese(), candidate.expectedResultVietnamese(),
            candidate.priority(), userId
        )).toList());
        job.complete(candidates.size());
        jobRepository.saveAndFlush(job);
        auditService.record(projectId, userId, AuditAction.AI_TEST_CASE_GENERATION_COMPLETED, "AI_JOB", job.getId());
        return toResponse(target.revision());
    }

    @Transactional
    public TestCaseResponse review(
        UUID userId, UUID requirementId, UUID revisionId, UUID testCaseId, ArtifactReviewStatus decision
    ) {
        var target = requireTarget(requirementId, revisionId);
        var projectId = target.requirement().getProject().getId();
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE);
        var testCase = testCaseRepository.findForRevision(testCaseId, revisionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy test case thuộc revision này."));
        testCase.review(decision, userId);
        testCaseRepository.saveAndFlush(testCase);
        auditService.record(projectId, userId, AuditAction.TEST_CASE_REVIEWED, "TEST_CASE", testCaseId);
        return toTestCaseResponse(testCase);
    }

    private TestCaseWorkspaceResponse toResponse(RequirementRevision revision) {
        var job = jobRepository.findForRevisionAndPurpose(revision.getId(), PURPOSE).orElse(null);
        return new TestCaseWorkspaceResponse(
            job == null ? null : responseMapper.toResponse(job),
            testCaseRepository.findAllForRevision(revision.getId()).stream().map(this::toTestCaseResponse).toList()
        );
    }

    private TestCaseResponse toTestCaseResponse(VerificationTestCase testCase) {
        return new TestCaseResponse(
            testCase.getId(), testCase.getRequirementRevisionId(), testCase.getAcceptanceCriterionId(), testCase.getAiJobId(),
            testCase.getTitleJapanese(), testCase.getTitleVietnamese(),
            testCase.getPreconditionsJapanese(), testCase.getPreconditionsVietnamese(),
            testCase.getStepsJapanese(), testCase.getStepsVietnamese(),
            testCase.getExpectedResultJapanese(), testCase.getExpectedResultVietnamese(),
            testCase.getPriority(), testCase.getStatus(), testCase.getCreatedBy(), testCase.getCreatedAt(),
            testCase.getReviewedBy(), testCase.getReviewedAt()
        );
    }

    private Target requireTarget(UUID requirementId, UUID revisionId) {
        var requirement = requirementRepository.findById(requirementId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy requirement " + requirementId + "."));
        var revision = revisionRepository.findByIdAndRequirementId(revisionId, requirementId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy revision thuộc requirement này."));
        return new Target(requirement, revision);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalStateException("AI thiếu trường " + field + ".");
        return value.trim();
    }

    private String safeMessage(RuntimeException exception) {
        var message = exception.getMessage();
        return message == null || message.isBlank() ? "AI test-case generation thất bại." : message;
    }

    private record Target(Requirement requirement, RequirementRevision revision) { }
    private record ValidatedTestCase(
        UUID criterionId, String titleJapanese, String titleVietnamese,
        String preconditionsJapanese, String preconditionsVietnamese,
        String stepsJapanese, String stepsVietnamese,
        String expectedResultJapanese, String expectedResultVietnamese,
        TestCasePriority priority
    ) { }
}
