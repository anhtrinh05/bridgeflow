package com.bridgeflow.api.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bridgeflow.api.ai.domain.AiJob;
import com.bridgeflow.api.ai.domain.AiJobStatus;
import com.bridgeflow.api.ai.persistence.AiJobRepository;
import com.bridgeflow.api.ai.provider.RequirementExtractionProvider;
import com.bridgeflow.api.audit.AuditAction;
import com.bridgeflow.api.audit.AuditService;
import com.bridgeflow.api.auth.domain.AppUser;
import com.bridgeflow.api.auth.persistence.AppUserRepository;
import com.bridgeflow.api.document.domain.DocumentVersion;
import com.bridgeflow.api.document.domain.ProjectDocument;
import com.bridgeflow.api.document.persistence.DocumentVersionRepository;
import com.bridgeflow.api.document.persistence.ProjectDocumentRepository;
import com.bridgeflow.api.document.storage.LocalDocumentStorage;
import com.bridgeflow.api.glossary.persistence.GlossaryTermRepository;
import com.bridgeflow.api.project.domain.Project;
import com.bridgeflow.api.project.membership.ProjectAccessService;
import com.bridgeflow.api.requirement.persistence.RequirementRepository;
import com.bridgeflow.api.requirement.persistence.RequirementRevisionRepository;

class AiExtractionServiceFailureTest {

    @Test
    void providerFailureLeavesJobFailedAndPromotesNoDrafts() {
        var projectId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var documentId = UUID.randomUUID();
        var versionId = UUID.randomUUID();
        var project = new Project("FAILURE", "Failure evidence", null);
        project.updateDetails("Failure evidence", null, true);
        setId(project, projectId);
        var user = new AppUser("failure@example.test", "Failure Test", "{noop}not-used");
        setId(user, userId);
        var document = new ProjectDocument(project, "Synthetic failure document");
        setId(document, documentId);
        var version = new DocumentVersion(
            document, 1, "failure.txt", "text/plain", 6,
            "0".repeat(64), "synthetic/document", user
        );
        setId(version, versionId);

        var savedJobs = new ArrayList<AiJob>();
        var promotedRequirements = new int[] {0};
        var promotedRevisions = new int[] {0};
        var failedAuditEvents = new int[] {0};

        var jobRepository = repository(AiJobRepository.class, (method, arguments) -> switch (method) {
            case "findForVersionAndPurpose" -> Optional.empty();
            case "saveAndFlush" -> {
                savedJobs.add((AiJob) arguments[0]);
                yield arguments[0];
            }
            default -> unexpected(method);
        });
        var documentRepository = repository(ProjectDocumentRepository.class, (method, arguments) -> switch (method) {
            case "findById" -> Optional.of(document);
            default -> unexpected(method);
        });
        var versionRepository = repository(DocumentVersionRepository.class, (method, arguments) -> switch (method) {
            case "findByIdAndDocumentId" -> Optional.of(version);
            default -> unexpected(method);
        });
        var requirementRepository = repository(RequirementRepository.class, (method, arguments) -> switch (method) {
            case "save" -> {
                promotedRequirements[0]++;
                yield arguments[0];
            }
            default -> unexpected(method);
        });
        var revisionRepository = repository(RequirementRevisionRepository.class, (method, arguments) -> switch (method) {
            case "save" -> {
                promotedRevisions[0]++;
                yield arguments[0];
            }
            default -> unexpected(method);
        });
        var glossaryRepository = repository(GlossaryTermRepository.class, (method, arguments) -> switch (method) {
            case "search" -> List.of();
            default -> unexpected(method);
        });
        var userRepository = repository(AppUserRepository.class, (method, arguments) -> switch (method) {
            case "getReferenceById" -> user;
            default -> unexpected(method);
        });

        var accessService = new ProjectAccessService(null) {
            @Override
            public com.bridgeflow.api.project.membership.ProjectMember requireRole(
                UUID requestedProjectId,
                UUID requestedUserId,
                com.bridgeflow.api.project.membership.ProjectRole... roles
            ) {
                assertThat(requestedProjectId).isEqualTo(projectId);
                assertThat(requestedUserId).isEqualTo(userId);
                return null;
            }
        };
        var storage = new LocalDocumentStorage(Path.of("target", "failure-test-storage").toString(), 1024) {
            @Override
            public byte[] read(String storageKey) {
                return "要件".getBytes(StandardCharsets.UTF_8);
            }
        };
        var provider = new RequirementExtractionProvider() {
            @Override
            public ExtractionResult extract(ExtractionRequest request) {
                throw new IllegalStateException("synthetic provider timeout");
            }

            @Override
            public AnalysisResult analyze(AnalysisRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public TestCaseResult generateTestCases(TestCaseRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String providerName() { return "failing-test-provider"; }

            @Override
            public String modelName() { return "failure-model"; }
        };
        var auditService = new AuditService(null) {
            @Override
            public void record(UUID auditedProjectId, UUID actorId, AuditAction action, String type, UUID entityId) {
                if (action == AuditAction.AI_EXTRACTION_FAILED) failedAuditEvents[0]++;
            }
        };
        var responseMapper = new AiJobResponseMapper(null, null, null, null) {
            @Override
            public com.bridgeflow.api.ai.api.AiModels.AiJobResponse toResponse(AiJob job) {
                return null;
            }
        };

        var service = new AiExtractionService(
            jobRepository, documentRepository, versionRepository, requirementRepository,
            revisionRepository, glossaryRepository, userRepository, accessService,
            storage, new DocumentTextExtractor(60_000), new SensitiveTextRedactor(""),
            provider, auditService, responseMapper, 25
        );

        service.extract(userId, documentId, versionId);

        assertThat(savedJobs).isNotEmpty();
        var failedJob = savedJobs.getLast();
        assertThat(failedJob.getStatus()).isEqualTo(AiJobStatus.FAILED);
        assertThat(failedJob.getErrorCode()).isEqualTo("AI_EXTRACTION_FAILED");
        assertThat(failedJob.getErrorMessage()).contains("synthetic provider timeout");
        assertThat(failedJob.getCandidateCount()).isZero();
        assertThat(promotedRequirements[0]).isZero();
        assertThat(promotedRevisions[0]).isZero();
        assertThat(failedAuditEvents[0]).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    private static <T> T repository(Class<T> type, RepositoryInvocation invocation) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (proxy, method, arguments) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> type.getSimpleName() + " test proxy";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == arguments[0];
                        default -> unexpected(method.getName());
                    };
                }
                return invocation.invoke(method.getName(), arguments == null ? new Object[0] : arguments);
            }
        );
    }

    private static Object unexpected(String method) {
        throw new AssertionError("Unexpected repository call: " + method);
    }

    private static void setId(Object target, UUID id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not assign synthetic entity id", exception);
        }
    }

    @FunctionalInterface
    private interface RepositoryInvocation {
        Object invoke(String method, Object[] arguments);
    }
}
