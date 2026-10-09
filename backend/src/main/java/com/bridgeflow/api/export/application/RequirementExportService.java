package com.bridgeflow.api.export.application;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bridgeflow.api.analysis.domain.ArtifactReviewStatus;
import com.bridgeflow.api.analysis.persistence.AcceptanceCriterionRepository;
import com.bridgeflow.api.audit.AuditAction;
import com.bridgeflow.api.audit.AuditService;
import com.bridgeflow.api.common.ResourceNotFoundException;
import com.bridgeflow.api.project.membership.ProjectAccessService;
import com.bridgeflow.api.project.persistence.ProjectRepository;
import com.bridgeflow.api.requirement.domain.Requirement;
import com.bridgeflow.api.requirement.domain.RequirementRevision;
import com.bridgeflow.api.requirement.domain.RequirementStatus;
import com.bridgeflow.api.requirement.persistence.RequirementRelationRepository;
import com.bridgeflow.api.requirement.persistence.RequirementRepository;
import com.bridgeflow.api.requirement.persistence.RequirementRevisionRepository;
import com.bridgeflow.api.testcase.persistence.VerificationTestCaseRepository;

@Service
public class RequirementExportService {
    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;
    private final RequirementRevisionRepository revisionRepository;
    private final AcceptanceCriterionRepository criterionRepository;
    private final VerificationTestCaseRepository testCaseRepository;
    private final RequirementRelationRepository relationRepository;
    private final ProjectAccessService accessService;
    private final AuditService auditService;

    public RequirementExportService(
        ProjectRepository projectRepository,
        RequirementRepository requirementRepository,
        RequirementRevisionRepository revisionRepository,
        AcceptanceCriterionRepository criterionRepository,
        VerificationTestCaseRepository testCaseRepository,
        RequirementRelationRepository relationRepository,
        ProjectAccessService accessService,
        AuditService auditService
    ) {
        this.projectRepository = projectRepository;
        this.requirementRepository = requirementRepository;
        this.revisionRepository = revisionRepository;
        this.criterionRepository = criterionRepository;
        this.testCaseRepository = testCaseRepository;
        this.relationRepository = relationRepository;
        this.accessService = accessService;
        this.auditService = auditService;
    }

    @Transactional
    public ExportFile exportCsv(UUID userId, UUID projectId) {
        var data = load(userId, projectId);
        var output = new StringBuilder("\uFEFF");
        output.append("Display Key,Status,Revision,Review Status,Japanese,Vietnamese,Source Anchor,Approved Criteria,Approved Test Cases,Outgoing Relations\r\n");
        for (var item : data.items()) {
            var revision = item.revision();
            output.append(csv(item.requirement().getDisplayKey())).append(',')
                .append(csv(item.requirement().getStatus().name())).append(',')
                .append(revision == null ? "" : revision.getRevisionNumber()).append(',')
                .append(csv(revision == null ? "" : revision.getReviewStatus().name())).append(',')
                .append(csv(revision == null ? "" : revision.getJapaneseText())).append(',')
                .append(csv(revision == null ? "" : revision.getVietnameseText())).append(',')
                .append(csv(revision == null ? "" : revision.getSourceAnchor())).append(',')
                .append(item.approvedCriteria()).append(',')
                .append(item.approvedTestCases()).append(',')
                .append(csv(String.join("; ", item.outgoingRelations())))
                .append("\r\n");
        }
        auditService.record(projectId, userId, AuditAction.PROJECT_REQUIREMENTS_EXPORTED, "PROJECT", projectId);
        return new ExportFile(data.code() + "-requirements.csv", "text/csv", output.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public ExportFile exportMarkdown(UUID userId, UUID projectId) {
        var data = load(userId, projectId);
        var output = new StringBuilder("# ").append(markdown(data.name())).append(" — Requirements\n\n");
        for (var item : data.items()) {
            var revision = item.revision();
            output.append("## ").append(markdown(item.requirement().getDisplayKey())).append("\n\n")
                .append("- Status: `").append(item.requirement().getStatus().name()).append("`\n")
                .append("- Revision: ").append(revision == null ? "—" : revision.getRevisionNumber()).append("\n")
                .append("- Review: `").append(revision == null ? "—" : revision.getReviewStatus().name()).append("`\n")
                .append("- Approved criteria: ").append(item.approvedCriteria()).append("\n")
                .append("- Approved test cases: ").append(item.approvedTestCases()).append("\n");
            if (revision != null && revision.getSourceAnchor() != null) {
                output.append("- Source: ").append(markdown(revision.getSourceAnchor())).append("\n");
            }
            if (!item.outgoingRelations().isEmpty()) {
                output.append("- Relations: ").append(markdown(String.join("; ", item.outgoingRelations()))).append("\n");
            }
            output.append("\n### 日本語\n\n")
                .append(revision == null ? "—" : markdown(revision.getJapaneseText()))
                .append("\n\n### Tiếng Việt\n\n")
                .append(revision == null ? "—" : markdown(revision.getVietnameseText())).append("\n\n");
        }
        auditService.record(projectId, userId, AuditAction.PROJECT_REQUIREMENTS_EXPORTED, "PROJECT", projectId);
        return new ExportFile(data.code() + "-requirements.md", "text/markdown", output.toString().getBytes(StandardCharsets.UTF_8));
    }

    private ExportData load(UUID userId, UUID projectId) {
        accessService.requireMember(projectId, userId);
        var project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy project " + projectId + "."));
        var items = requirementRepository
            .findAllByProjectIdAndStatusNotOrderByDisplayKeyAsc(projectId, RequirementStatus.ARCHIVED)
            .stream().map(this::toItem).toList();
        return new ExportData(project.getCode(), project.getName(), items);
    }

    private ExportItem toItem(Requirement requirement) {
        var revision = revisionRepository.findFirstByRequirementIdOrderByRevisionNumberDesc(requirement.getId())
            .orElse(null);
        var approvedCriteria = revision == null ? 0 : criterionRepository.findAllForRevision(revision.getId()).stream()
            .filter(item -> item.getStatus() == ArtifactReviewStatus.APPROVED).count();
        var approvedTestCases = revision == null ? 0 : testCaseRepository.findAllForRevision(revision.getId()).stream()
            .filter(item -> item.getStatus() == ArtifactReviewStatus.APPROVED).count();
        var relations = relationRepository.findAllForRequirement(requirement.getId()).stream()
            .filter(item -> item.getSourceRequirement().getId().equals(requirement.getId()))
            .map(item -> item.getRelationType().name() + " → " + item.getTargetRequirement().getDisplayKey())
            .sorted().toList();
        return new ExportItem(requirement, revision, approvedCriteria, approvedTestCases, relations);
    }

    private String csv(Object value) {
        var text = value == null ? "" : value.toString();
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private String markdown(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("`", "\\`");
    }

    public record ExportFile(String filename, String mediaType, byte[] content) { }
    private record ExportData(String code, String name, List<ExportItem> items) { }
    private record ExportItem(
        Requirement requirement, RequirementRevision revision, long approvedCriteria,
        long approvedTestCases, List<String> outgoingRelations
    ) { }
}
