package com.bridgeflow.api.requirement.application;

import static com.bridgeflow.api.requirement.api.TraceabilityModels.AffectedArtifactResponse;
import static com.bridgeflow.api.requirement.api.TraceabilityModels.ArtifactTraceResponse;
import static com.bridgeflow.api.requirement.api.TraceabilityModels.ChangeImpactResponse;
import static com.bridgeflow.api.requirement.api.TraceabilityModels.RequirementTraceabilityResponse;
import static com.bridgeflow.api.requirement.api.TraceabilityModels.RevisionTraceResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridgeflow.api.analysis.domain.ArtifactReviewStatus;
import com.bridgeflow.api.analysis.persistence.AcceptanceCriterionRepository;
import com.bridgeflow.api.analysis.persistence.ClarificationQuestionRepository;
import com.bridgeflow.api.common.ResourceNotFoundException;
import com.bridgeflow.api.project.membership.ProjectAccessService;
import com.bridgeflow.api.requirement.domain.ChangeType;
import com.bridgeflow.api.requirement.domain.Requirement;
import com.bridgeflow.api.requirement.domain.RequirementRevision;
import com.bridgeflow.api.requirement.persistence.RequirementRepository;
import com.bridgeflow.api.requirement.persistence.RequirementRevisionRepository;
import com.bridgeflow.api.testcase.persistence.VerificationTestCaseRepository;

@Service
@Transactional(readOnly = true)
public class RequirementTraceabilityService {
    private final RequirementRepository requirementRepository;
    private final RequirementRevisionRepository revisionRepository;
    private final ClarificationQuestionRepository questionRepository;
    private final AcceptanceCriterionRepository criterionRepository;
    private final VerificationTestCaseRepository testCaseRepository;
    private final ProjectAccessService accessService;

    public RequirementTraceabilityService(
        RequirementRepository requirementRepository,
        RequirementRevisionRepository revisionRepository,
        ClarificationQuestionRepository questionRepository,
        AcceptanceCriterionRepository criterionRepository,
        VerificationTestCaseRepository testCaseRepository,
        ProjectAccessService accessService
    ) {
        this.requirementRepository = requirementRepository;
        this.revisionRepository = revisionRepository;
        this.questionRepository = questionRepository;
        this.criterionRepository = criterionRepository;
        this.testCaseRepository = testCaseRepository;
        this.accessService = accessService;
    }

    public RequirementTraceabilityResponse getTraceability(UUID userId, UUID requirementId) {
        var requirement = requireRequirement(requirementId);
        accessService.requireMember(requirement.getProject().getId(), userId);
        var revisions = revisionRepository.findByRequirementIdOrderByRevisionNumberAsc(requirementId);
        return new RequirementTraceabilityResponse(
            requirementId, requirement.getDisplayKey(), requirement.getCurrentRevisionId(),
            revisions.stream().map(this::toRevisionTrace).toList()
        );
    }

    public ChangeImpactResponse getChangeImpact(UUID userId, UUID requirementId, UUID revisionId) {
        var requirement = requireRequirement(requirementId);
        accessService.requireMember(requirement.getProject().getId(), userId);
        var revisions = revisionRepository.findByRequirementIdOrderByRevisionNumberAsc(requirementId);
        var target = revisions.stream().filter(revision -> revision.getId().equals(revisionId)).findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy revision thuộc requirement này."));
        var baseline = revisions.stream()
            .filter(revision -> revision.getRevisionNumber() == target.getRevisionNumber() - 1)
            .findFirst().orElse(null);
        if (baseline == null) {
            return new ChangeImpactResponse(
                requirementId, revisionId, null, "LOW", false, false, false,
                List.of("Revision đầu tiên chưa có baseline để so sánh."), List.of()
            );
        }

        var japaneseChanged = !Objects.equals(baseline.getJapaneseText(), target.getJapaneseText());
        var vietnameseChanged = !Objects.equals(baseline.getVietnameseText(), target.getVietnameseText());
        var contentChanged = japaneseChanged || vietnameseChanged;
        var affected = contentChanged ? affectedArtifacts(baseline) : List.<AffectedArtifactResponse>of();
        var reasons = new ArrayList<String>();
        if (japaneseChanged) reasons.add("Nội dung tiếng Nhật đã thay đổi so với revision trước.");
        if (vietnameseChanged) reasons.add("Nội dung tiếng Việt đã thay đổi so với revision trước.");
        if (target.getChangeType() == ChangeType.DELETED
            || target.getChangeType() == ChangeType.SPLIT
            || target.getChangeType() == ChangeType.MERGED) {
            reasons.add("Loại thay đổi " + target.getChangeType() + " có thể làm thay đổi phạm vi truy vết.");
        }
        if (!affected.isEmpty()) {
            reasons.add(affected.size() + " artifact của revision trước cần được revalidate.");
        }
        if (reasons.isEmpty()) reasons.add("Không phát hiện thay đổi nội dung ảnh hưởng artifact.");

        var structural = target.getChangeType() == ChangeType.DELETED
            || target.getChangeType() == ChangeType.SPLIT
            || target.getChangeType() == ChangeType.MERGED;
        var level = structural || !affected.isEmpty() ? "HIGH" : contentChanged ? "MEDIUM" : "LOW";
        return new ChangeImpactResponse(
            requirementId, revisionId, baseline.getId(), level, japaneseChanged, vietnameseChanged,
            contentChanged && !affected.isEmpty(), List.copyOf(reasons), affected
        );
    }

    private RevisionTraceResponse toRevisionTrace(RequirementRevision revision) {
        var artifacts = new ArrayList<ArtifactTraceResponse>();
        questionRepository.findAllForRevision(revision.getId()).forEach(question -> artifacts.add(
            new ArtifactTraceResponse(question.getId(), "CLARIFICATION_QUESTION", question.getStatus().name(), null)
        ));
        criterionRepository.findAllForRevision(revision.getId()).forEach(criterion -> artifacts.add(
            new ArtifactTraceResponse(criterion.getId(), "ACCEPTANCE_CRITERION", criterion.getStatus().name(), null)
        ));
        testCaseRepository.findAllForRevision(revision.getId()).forEach(testCase -> artifacts.add(
            new ArtifactTraceResponse(
                testCase.getId(), "TEST_CASE", testCase.getStatus().name(), testCase.getAcceptanceCriterionId()
            )
        ));
        return new RevisionTraceResponse(
            revision.getId(), revision.getRevisionNumber(), revision.getChangeType().name(),
            revision.getReviewStatus().name(), revision.getDocumentVersionId(), revision.getSourceAnchor(),
            List.copyOf(artifacts)
        );
    }

    private List<AffectedArtifactResponse> affectedArtifacts(RequirementRevision baseline) {
        var affected = new ArrayList<AffectedArtifactResponse>();
        questionRepository.findAllForRevision(baseline.getId()).stream()
            .filter(question -> question.getStatus() != ArtifactReviewStatus.REJECTED)
            .forEach(question -> affected.add(new AffectedArtifactResponse(
                question.getId(), "CLARIFICATION_QUESTION", question.getStatus().name(), baseline.getId(),
                question.getAnsweredAt() == null ? "REVIEW" : "REVALIDATE_ANSWER"
            )));
        criterionRepository.findAllForRevision(baseline.getId()).stream()
            .filter(criterion -> criterion.getStatus() != ArtifactReviewStatus.REJECTED)
            .forEach(criterion -> affected.add(new AffectedArtifactResponse(
                criterion.getId(), "ACCEPTANCE_CRITERION", criterion.getStatus().name(), baseline.getId(), "REVALIDATE"
            )));
        testCaseRepository.findAllForRevision(baseline.getId()).stream()
            .filter(testCase -> testCase.getStatus() != ArtifactReviewStatus.REJECTED)
            .forEach(testCase -> affected.add(new AffectedArtifactResponse(
                testCase.getId(), "TEST_CASE", testCase.getStatus().name(), baseline.getId(), "REVALIDATE"
            )));
        return List.copyOf(affected);
    }

    private Requirement requireRequirement(UUID requirementId) {
        return requirementRepository.findById(requirementId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy requirement " + requirementId + "."));
    }
}
