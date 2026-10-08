package com.bridgeflow.api.ai.application;

import static com.bridgeflow.api.ai.api.AiModels.AiJobResponse;

import java.util.List;

import org.springframework.stereotype.Component;

import com.bridgeflow.api.ai.domain.AiJob;
import com.bridgeflow.api.analysis.persistence.AcceptanceCriterionRepository;
import com.bridgeflow.api.analysis.persistence.ClarificationQuestionRepository;
import com.bridgeflow.api.requirement.persistence.RequirementRevisionRepository;

@Component
public class AiJobResponseMapper {

    private final RequirementRevisionRepository revisionRepository;
    private final ClarificationQuestionRepository questionRepository;
    private final AcceptanceCriterionRepository criterionRepository;

    public AiJobResponseMapper(
        RequirementRevisionRepository revisionRepository,
        ClarificationQuestionRepository questionRepository,
        AcceptanceCriterionRepository criterionRepository
    ) {
        this.revisionRepository = revisionRepository;
        this.questionRepository = questionRepository;
        this.criterionRepository = criterionRepository;
    }

    public AiJobResponse toResponse(AiJob job) {
        var requirementIds = job.getDocumentVersionId() == null
            ? List.<java.util.UUID>of()
            : revisionRepository.findAllByDocumentVersionIdOrderByCreatedAtAsc(job.getDocumentVersionId()).stream()
                .map(revision -> revision.getRequirement().getId())
                .distinct()
                .toList();
        var questionIds = questionRepository.findAllForJob(job.getId()).stream()
            .map(question -> question.getId())
            .toList();
        var criterionIds = criterionRepository.findAllForJob(job.getId()).stream()
            .map(criterion -> criterion.getId())
            .toList();
        return new AiJobResponse(
            job.getId(), job.getProjectId(), job.getDocumentVersionId(), job.getRequirementRevisionId(),
            job.getPurpose(), job.getStatus().name(), job.getProvider(), job.getModel(), job.getCorrelationId(),
            job.getRequestedById(), job.getCandidateCount(), requirementIds, questionIds, criterionIds,
            job.getErrorCode(), job.getErrorMessage(), job.getCreatedAt(), job.getStartedAt(), job.getCompletedAt()
        );
    }
}
