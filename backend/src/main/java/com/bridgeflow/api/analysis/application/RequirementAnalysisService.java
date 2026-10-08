package com.bridgeflow.api.analysis.application;

import static com.bridgeflow.api.analysis.api.AnalysisModels.AcceptanceCriterionResponse;
import static com.bridgeflow.api.analysis.api.AnalysisModels.ClarificationQuestionResponse;
import static com.bridgeflow.api.analysis.api.AnalysisModels.RequirementAnalysisResponse;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridgeflow.api.ai.application.AiJobResponseMapper;
import com.bridgeflow.api.ai.application.SensitiveTextRedactor;
import com.bridgeflow.api.ai.domain.AiJob;
import com.bridgeflow.api.ai.domain.AiJobStatus;
import com.bridgeflow.api.ai.persistence.AiJobRepository;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider.AnalysisRequest;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider.GlossaryEntry;
import com.bridgeflow.api.analysis.domain.AcceptanceCriterion;
import com.bridgeflow.api.analysis.domain.ArtifactReviewStatus;
import com.bridgeflow.api.analysis.domain.ClarificationQuestion;
import com.bridgeflow.api.analysis.persistence.AcceptanceCriterionRepository;
import com.bridgeflow.api.analysis.persistence.ClarificationQuestionRepository;
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

@Service
@Transactional(readOnly = true)
public class RequirementAnalysisService {

    private static final String PURPOSE = "REQUIREMENT_ANALYSIS";

    private final RequirementRepository requirementRepository;
    private final RequirementRevisionRepository revisionRepository;
    private final AiJobRepository jobRepository;
    private final ClarificationQuestionRepository questionRepository;
    private final AcceptanceCriterionRepository criterionRepository;
    private final GlossaryTermRepository glossaryRepository;
    private final AppUserRepository userRepository;
    private final ProjectAccessService accessService;
    private final RequirementExtractionProvider provider;
    private final SensitiveTextRedactor redactor;
    private final AuditService auditService;
    private final AiJobResponseMapper responseMapper;
    private final int maxQuestions;
    private final int maxCriteria;

    public RequirementAnalysisService(
        RequirementRepository requirementRepository,
        RequirementRevisionRepository revisionRepository,
        AiJobRepository jobRepository,
        ClarificationQuestionRepository questionRepository,
        AcceptanceCriterionRepository criterionRepository,
        GlossaryTermRepository glossaryRepository,
        AppUserRepository userRepository,
        ProjectAccessService accessService,
        RequirementExtractionProvider provider,
        SensitiveTextRedactor redactor,
        AuditService auditService,
        AiJobResponseMapper responseMapper,
        @Value("${bridgeflow.ai.max-questions:8}") int maxQuestions,
        @Value("${bridgeflow.ai.max-criteria:12}") int maxCriteria
    ) {
        this.requirementRepository = requirementRepository;
        this.revisionRepository = revisionRepository;
        this.jobRepository = jobRepository;
        this.questionRepository = questionRepository;
        this.criterionRepository = criterionRepository;
        this.glossaryRepository = glossaryRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.provider = provider;
        this.redactor = redactor;
        this.auditService = auditService;
        this.responseMapper = responseMapper;
        if (maxQuestions < 1 || maxQuestions > 25) {
            throw new IllegalArgumentException("AI max-questions must be between 1 and 25");
        }
        if (maxCriteria < 1 || maxCriteria > 50) {
            throw new IllegalArgumentException("AI max-criteria must be between 1 and 50");
        }
        this.maxQuestions = maxQuestions;
        this.maxCriteria = maxCriteria;
    }

    public RequirementAnalysisResponse get(UUID userId, UUID requirementId, UUID revisionId) {
        var target = requireTarget(requirementId, revisionId);
        accessService.requireMember(target.requirement().getProject().getId(), userId);
        return toResponse(target.revision());
    }

    @Transactional
    public RequirementAnalysisResponse generate(UUID userId, UUID requirementId, UUID revisionId) {
        var target = requireTarget(requirementId, revisionId);
        var requirement = target.requirement();
        var revision = target.revision();
        var project = requirement.getProject();
        var projectId = project.getId();
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE);
        if (project.getStatus() == ProjectStatus.ARCHIVED || requirement.isArchived()) {
            throw new IllegalStateException("Requirement thuộc project đã archive nên không thể chạy AI.");
        }
        if (!project.isAiEnabled()) throw new IllegalStateException("AI chưa được bật cho project này.");

        var existing = jobRepository.findForRevisionAndPurpose(revisionId, PURPOSE).orElse(null);
        if (existing != null && existing.getStatus() != AiJobStatus.FAILED) return toResponse(revision);

        var job = existing == null
            ? new AiJob(project, revision, provider.providerName(), provider.modelName(), userRepository.getReferenceById(userId))
            : existing;
        job.start();
        jobRepository.saveAndFlush(job);
        auditService.record(projectId, userId, AuditAction.AI_ANALYSIS_REQUESTED, "AI_JOB", job.getId());

        List<ValidatedQuestion> questions;
        List<ValidatedCriterion> criteria;
        try {
            var glossary = glossaryRepository.search(projectId, "").stream()
                .map(term -> new GlossaryEntry(term.getJapaneseTerm(), term.getVietnameseTerm(), term.getNotes()))
                .toList();
            var result = provider.analyze(new AnalysisRequest(
                job.getCorrelationId(), redactor.redact(revision.getJapaneseText()),
                redactor.redact(revision.getVietnameseText()), glossary, maxQuestions, maxCriteria
            ));
            var rawQuestions = Objects.requireNonNull(result.clarificationQuestions(), "AI questions are required");
            var rawCriteria = Objects.requireNonNull(result.acceptanceCriteria(), "AI criteria are required");
            if (rawQuestions.size() > maxQuestions || rawCriteria.size() > maxCriteria) {
                throw new IllegalStateException("AI trả về quá số artifact cho phép.");
            }
            questions = rawQuestions.stream().map(question -> new ValidatedQuestion(
                requireText(question.japaneseText(), "question.japaneseText"),
                requireText(question.vietnameseText(), "question.vietnameseText"),
                optionalText(question.rationale())
            )).toList();
            criteria = rawCriteria.stream().map(criterion -> new ValidatedCriterion(
                requireText(criterion.japaneseText(), "criterion.japaneseText"),
                requireText(criterion.vietnameseText(), "criterion.vietnameseText")
            )).toList();
        } catch (RuntimeException exception) {
            job.fail("AI_ANALYSIS_FAILED", safeMessage(exception));
            jobRepository.saveAndFlush(job);
            auditService.record(projectId, userId, AuditAction.AI_ANALYSIS_FAILED, "AI_JOB", job.getId());
            return toResponse(revision);
        }

        questionRepository.saveAll(questions.stream().map(question -> new ClarificationQuestion(
            revision, job, question.japaneseText(), question.vietnameseText(), question.rationale(), userId
        )).toList());
        criterionRepository.saveAll(criteria.stream().map(criterion -> new AcceptanceCriterion(
            revision, job, criterion.japaneseText(), criterion.vietnameseText(), userId
        )).toList());
        job.complete(questions.size() + criteria.size());
        jobRepository.saveAndFlush(job);
        auditService.record(projectId, userId, AuditAction.AI_ANALYSIS_COMPLETED, "AI_JOB", job.getId());
        return toResponse(revision);
    }

    @Transactional
    public ClarificationQuestionResponse answerQuestion(
        UUID userId, UUID requirementId, UUID revisionId, UUID questionId, String japanese, String vietnamese
    ) {
        var target = requireTarget(requirementId, revisionId);
        var projectId = target.requirement().getProject().getId();
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE, ProjectRole.DEVELOPER);
        var question = questionRepository.findForRevision(questionId, revisionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy clarification question này."));
        question.answer(japanese, vietnamese, userId);
        questionRepository.saveAndFlush(question);
        auditService.record(projectId, userId, AuditAction.CLARIFICATION_ANSWERED, "CLARIFICATION_QUESTION", questionId);
        return toQuestionResponse(question);
    }

    @Transactional
    public ClarificationQuestionResponse reviewQuestion(
        UUID userId, UUID requirementId, UUID revisionId, UUID questionId, ArtifactReviewStatus decision
    ) {
        var target = requireTarget(requirementId, revisionId);
        var projectId = target.requirement().getProject().getId();
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE);
        var question = questionRepository.findForRevision(questionId, revisionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy clarification question này."));
        question.review(decision, userId);
        questionRepository.saveAndFlush(question);
        auditService.record(projectId, userId, AuditAction.CLARIFICATION_REVIEWED, "CLARIFICATION_QUESTION", questionId);
        return toQuestionResponse(question);
    }

    @Transactional
    public AcceptanceCriterionResponse reviewCriterion(
        UUID userId, UUID requirementId, UUID revisionId, UUID criterionId, ArtifactReviewStatus decision
    ) {
        var target = requireTarget(requirementId, revisionId);
        var projectId = target.requirement().getProject().getId();
        accessService.requireRole(projectId, userId, ProjectRole.ADMIN, ProjectRole.BRSE);
        var criterion = criterionRepository.findForRevision(criterionId, revisionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy acceptance criterion này."));
        criterion.review(decision, userId);
        criterionRepository.saveAndFlush(criterion);
        auditService.record(projectId, userId, AuditAction.ACCEPTANCE_CRITERION_REVIEWED, "ACCEPTANCE_CRITERION", criterionId);
        return toCriterionResponse(criterion);
    }

    private RequirementAnalysisResponse toResponse(RequirementRevision revision) {
        var job = jobRepository.findForRevisionAndPurpose(revision.getId(), PURPOSE).orElse(null);
        return new RequirementAnalysisResponse(
            job == null ? null : responseMapper.toResponse(job),
            questionRepository.findAllForRevision(revision.getId()).stream().map(this::toQuestionResponse).toList(),
            criterionRepository.findAllForRevision(revision.getId()).stream().map(this::toCriterionResponse).toList()
        );
    }

    private ClarificationQuestionResponse toQuestionResponse(ClarificationQuestion question) {
        return new ClarificationQuestionResponse(
            question.getId(), question.getRequirementRevisionId(), question.getAiJobId(),
            question.getJapaneseText(), question.getVietnameseText(), question.getRationale(),
            question.getAnswerJapanese(), question.getAnswerVietnamese(), question.getStatus(),
            question.getCreatedBy(), question.getCreatedAt(), question.getReviewedBy(), question.getReviewedAt(),
            question.getAnsweredBy(), question.getAnsweredAt()
        );
    }

    private AcceptanceCriterionResponse toCriterionResponse(AcceptanceCriterion criterion) {
        return new AcceptanceCriterionResponse(
            criterion.getId(), criterion.getRequirementRevisionId(), criterion.getAiJobId(),
            criterion.getJapaneseText(), criterion.getVietnameseText(), criterion.getStatus(),
            criterion.getCreatedBy(), criterion.getCreatedAt(), criterion.getReviewedBy(), criterion.getReviewedAt()
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

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String safeMessage(RuntimeException exception) {
        var message = exception.getMessage();
        return message == null || message.isBlank() ? "AI analysis thất bại." : message;
    }

    private record Target(Requirement requirement, RequirementRevision revision) {
    }

    private record ValidatedQuestion(String japaneseText, String vietnameseText, String rationale) {
    }

    private record ValidatedCriterion(String japaneseText, String vietnameseText) {
    }
}
