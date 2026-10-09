package com.bridgeflow.api.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

import jakarta.persistence.EntityManager;

import com.bridgeflow.api.audit.AuditAction;
import com.bridgeflow.api.audit.AuditEventRepository;
import com.bridgeflow.api.auth.domain.AppUser;
import com.bridgeflow.api.auth.persistence.AppUserRepository;
import com.bridgeflow.api.project.domain.Project;
import com.bridgeflow.api.project.membership.ProjectMember;
import com.bridgeflow.api.project.membership.ProjectMemberRepository;
import com.bridgeflow.api.project.membership.ProjectRole;
import com.bridgeflow.api.project.persistence.ProjectRepository;
import com.bridgeflow.api.requirement.domain.ChangeType;
import com.bridgeflow.api.requirement.domain.Requirement;
import com.bridgeflow.api.requirement.domain.RequirementRevision;
import com.bridgeflow.api.requirement.persistence.RequirementRepository;
import com.bridgeflow.api.requirement.persistence.RequirementRevisionRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequirementPersistenceIntegrationTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
        .withDatabaseName("bridgeflow_test")
        .withUsername("bridgeflow")
        .withPassword("bridgeflow");

    private static final Path DOCUMENT_STORAGE = Path.of(
        System.getProperty("java.io.tmpdir"), "bridgeflow-test-documents-" + UUID.randomUUID()
    );

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("bridgeflow.storage.root", () -> DOCUMENT_STORAGE.toString());
        registry.add("bridgeflow.ai.provider", () -> "stub");
        var externalUrl = System.getenv("BRIDGEFLOW_TEST_DB_URL");
        if (externalUrl != null && !externalUrl.isBlank()) {
            // The external database must be disposable: Flyway migrates it at startup.
            registry.add("spring.datasource.url", () -> externalUrl);
            registry.add("spring.datasource.username", () -> System.getenv("BRIDGEFLOW_TEST_DB_USERNAME"));
            registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("BRIDGEFLOW_TEST_DB_PASSWORD", ""));
        } else {
            POSTGRES.start();
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        }
    }

    @AfterAll
    static void stopContainer() throws Exception {
        if (POSTGRES.isRunning()) {
            POSTGRES.stop();
        }
        if (Files.exists(DOCUMENT_STORAGE)) {
            try (var paths = Files.walk(DOCUMENT_STORAGE)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private RequirementRevisionRepository revisionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private ProjectMemberRepository memberRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String accessToken;

    @Test
    @Transactional
    void keepsAStableRequirementIdWhileItsContentLivesInRevisions() {
        var project = projectRepository.save(
            new Project("DEMO", "BridgeFlow Demo", "Sakura Systems")
        );
        var requirement = requirementRepository.save(new Requirement(project, "REQ-014"));
        var stableRequirementId = requirement.getId();

        var revision = revisionRepository.save(
            new RequirementRevision(
                requirement,
                1,
                "利用者は要件を確認できる。",
                "Người dùng có thể xác nhận yêu cầu.",
                ChangeType.ADDED
            )
        );
        revision.confirm(UUID.randomUUID());
        requirement.pointToRevision(revision);
        requirementRepository.saveAndFlush(requirement);

        var nextRevision = revisionRepository.saveAndFlush(new RequirementRevision(
            requirement, 2, "利用者は要件と履歴を確認できる。",
            "Người dùng có thể xác nhận yêu cầu và xem lịch sử.", ChangeType.MODIFIED
        ));
        assertThatThrownBy(() -> requirement.pointToRevision(nextRevision))
            .isInstanceOf(IllegalArgumentException.class);
        nextRevision.confirm(UUID.randomUUID());
        requirement.pointToRevision(nextRevision);
        requirementRepository.saveAndFlush(requirement);
        entityManager.clear();

        var reloaded = requirementRepository
            .findByProjectIdAndDisplayKey(project.getId(), "REQ-014")
            .orElseThrow();

        assertThat(reloaded.getId()).isEqualTo(stableRequirementId);
        assertThat(reloaded.getCurrentRevisionId()).isEqualTo(nextRevision.getId());
        assertThat(revisionRepository.findByRequirementIdOrderByRevisionNumberAsc(stableRequirementId))
            .extracting(RequirementRevision::getRevisionNumber)
            .containsExactly(1, 2);
        assertThat(revisionRepository.findById(revision.getId()).orElseThrow().getJapaneseText())
            .isEqualTo("利用者は要件を確認できる。");
    }

    @Test
    @Transactional
    void rejectsDuplicateRevisionNumbers() {
        var project = projectRepository.save(new Project("DUP", "Duplicate demo", null));
        var requirement = requirementRepository.save(new Requirement(project, "REQ-001"));
        revisionRepository.saveAndFlush(new RequirementRevision(requirement, 1, "要件", "Yêu cầu", ChangeType.ADDED));
        assertThatThrownBy(() -> revisionRepository.saveAndFlush(
            new RequirementRevision(requirement, 1, "変更", "Thay đổi", ChangeType.MODIFIED)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void databaseRejectsCurrentRevisionOwnedByAnotherRequirement() {
        var project = projectRepository.save(new Project("OWNER", "Ownership demo", null));
        var first = requirementRepository.save(new Requirement(project, "REQ-001"));
        var second = requirementRepository.save(new Requirement(project, "REQ-002"));
        var revision = revisionRepository.saveAndFlush(
            new RequirementRevision(second, 1, "要件", "Yêu cầu", ChangeType.ADDED)
        );
        assertThatThrownBy(() -> jdbcTemplate.update(
            "UPDATE requirements SET current_revision_id = ? WHERE id = ?", revision.getId(), first.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void healthEndpointsRespondWithRunningDatabase() throws Exception {
        assertThat(projectRepository.findByCode("EC-RENEWAL")).isEmpty();
        var baseUrl = "http://127.0.0.1:" + environment.getRequiredProperty("local.server.port");
        try (var client = HttpClient.newHttpClient()) {
            for (var path : new String[] {"/api/v1/health", "/actuator/health"}) {
                var response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).contains("\"status\":\"UP\"");
            }
        }
    }

    @Test
    void publishesTheOpenApiContract() throws Exception {
        var contract = sendJson("GET", "/v3/api-docs", null, 200);

        assertThat(contract.required("info").required("title").asText()).isEqualTo("BridgeFlow API");
        assertThat(contract.required("paths").required("/api/v1/projects").required("get")
            .required("operationId").asText()).isEqualTo("listProjects");
        assertThat(contract.required("paths").required("/api/v1/projects").required("post")
            .required("responses").has("201")).isTrue();
        assertThat(contract.required("paths").required("/api/v1/projects/{projectId}/requirements")
            .required("get").required("operationId").asText()).isEqualTo("listRequirements");
        assertThat(contract.required("paths").required("/api/v1/projects/{projectId}/glossary")
            .required("get").required("operationId").asText()).isEqualTo("listGlossaryTerms");
        assertThat(contract.required("paths").required("/api/v1/projects/{projectId}/documents")
            .required("post").required("operationId").asText()).isEqualTo("uploadDocument");
        assertThat(contract.required("paths")
            .required("/api/v1/documents/{documentId}/versions/{versionId}/ai-extractions")
            .required("post").required("operationId").asText()).isEqualTo("extractRequirements");
        assertThat(contract.required("paths")
            .required("/api/v1/requirements/{requirementId}/revisions/{revisionId}/ai-analysis")
            .required("post").required("operationId").asText()).isEqualTo("generateRequirementAnalysis");
        assertThat(contract.required("paths")
            .required("/api/v1/requirements/{requirementId}/revisions/{revisionId}/test-cases/ai-generation")
            .required("post").required("operationId").asText()).isEqualTo("generateRevisionTestCases");
        assertThat(contract.required("paths")
            .required("/api/v1/requirements/{requirementId}/revisions/{revisionId}/change-impact")
            .required("get").required("operationId").asText()).isEqualTo("getRequirementChangeImpact");
        assertThat(contract.required("paths")
            .required("/api/v1/requirements/{requirementId}/relations")
            .required("post").required("operationId").asText()).isEqualTo("createRequirementRelation");
        assertThat(contract.required("paths")
            .required("/api/v1/projects/{projectId}/exports/requirements.csv")
            .required("get").required("operationId").asText()).isEqualTo("exportRequirementsCsv");
        var schemas = contract.required("components").required("schemas");
        assertThat(schemas.has("RequirementResponse")).isTrue();
        assertThat(schemas.has("RequirementAnalysisResponse")).isTrue();
        assertThat(schemas.has("TestCaseWorkspaceResponse")).isTrue();
        assertThat(schemas.required("RequirementResponse").required("required").toString())
            .contains("id", "projectId", "revisions", "archivedAt");
    }

    @Test
    void createsRevisesAndConfirmsARequirementThroughTheRestApi() throws Exception {
        var authenticatedUserId = loginTestUser();
        var suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var project = sendJson("POST", "/api/v1/projects", """
            {"code":"API-%s","name":"API integration project","customerName":"架空株式会社"}
            """.formatted(suffix), 201);
        var projectId = project.required("id").asText();

        project = sendJson("PATCH", "/api/v1/projects/" + projectId, """
            {"name":"Updated API project","customerName":"更新株式会社"}
            """, 200);
        assertThat(project.required("name").asText()).isEqualTo("Updated API project");
        assertThat(project.required("customerName").asText()).isEqualTo("更新株式会社");
        assertThat(project.required("aiEnabled").asBoolean()).isFalse();

        var requirement = sendJson("POST", "/api/v1/projects/" + projectId + "/requirements", """
            {"displayKey":"REQ-001","japaneseText":"利用者は要件を確認できる。","vietnameseText":"Người dùng có thể xem yêu cầu."}
            """, 201);
        var requirementId = requirement.required("id").asText();
        assertThat(requirement.required("status").asText()).isEqualTo("REVIEWING");
        assertThat(requirement.required("latestRevision").required("revisionNumber").asInt()).isEqualTo(1);

        requirement = sendJson("POST", "/api/v1/requirements/" + requirementId + "/revisions", """
            {"japaneseText":"利用者は要件と履歴を確認できる。","vietnameseText":"Người dùng có thể xem yêu cầu và lịch sử.","changeType":"MODIFIED"}
            """, 200);
        var revisionId = requirement.required("latestRevision").required("id").asText();
        assertThat(requirement.required("latestRevision").required("revisionNumber").asInt()).isEqualTo(2);

        requirement = sendJson("POST", "/api/v1/requirements/" + requirementId
            + "/revisions/" + revisionId + "/confirm", "{}", 200);
        assertThat(requirement.required("status").asText()).isEqualTo("CONFIRMED");
        assertThat(requirement.required("currentRevisionId").asText()).isEqualTo(revisionId);
        assertThat(requirement.required("latestRevision").required("confirmedBy").asText())
            .isEqualTo(authenticatedUserId.toString());

        var detail = sendJson("GET", "/api/v1/requirements/" + requirementId, null, 200);
        assertThat(detail.required("revisions")).hasSize(2);
        assertThat(detail.required("id").asText()).isEqualTo(requirementId);

        var searchPage = sendJson(
            "GET",
            "/api/v1/projects/" + projectId
                + "/requirements?status=CONFIRMED&query=%E5%B1%A5%E6%AD%B4&page=0&size=1"
                + "&sortBy=updatedAt&direction=desc",
            null,
            200
        );
        assertThat(searchPage.required("items")).hasSize(1);
        assertThat(searchPage.required("items").get(0).required("id").asText()).isEqualTo(requirementId);
        assertThat(searchPage.required("totalElements").asLong()).isEqualTo(1);

        requirement = sendJson("POST", "/api/v1/requirements/" + requirementId + "/archive", "{}", 200);
        assertThat(requirement.required("status").asText()).isEqualTo("ARCHIVED");
        assertThat(requirement.required("archivedAt").isNull()).isFalse();
        var activeRequirements = sendJson(
            "GET", "/api/v1/projects/" + projectId + "/requirements", null, 200
        );
        assertThat(activeRequirements.required("items").toString()).doesNotContain(requirementId);
        var archivedRequirements = sendJson(
            "GET",
            "/api/v1/projects/" + projectId + "/requirements?includeArchived=true&status=ARCHIVED",
            null,
            200
        );
        assertThat(archivedRequirements.required("items").toString()).contains(requirementId);
        sendJson("POST", "/api/v1/requirements/" + requirementId + "/revisions", """
            {"japaneseText":"変更不可","vietnameseText":"Không thể sửa","changeType":"MODIFIED"}
            """, 400);

        project = sendJson("POST", "/api/v1/projects/" + projectId + "/archive", "{}", 200);
        assertThat(project.required("status").asText()).isEqualTo("ARCHIVED");
        var activeProjects = sendJson("GET", "/api/v1/projects", null, 200);
        assertThat(activeProjects.toString()).doesNotContain(projectId);
        var allProjects = sendJson("GET", "/api/v1/projects?includeArchived=true", null, 200);
        assertThat(allProjects.toString()).contains(projectId);

        assertThat(auditEventRepository.findAllByProjectIdOrderByOccurredAtDesc(UUID.fromString(projectId)))
            .extracting(event -> event.getAction())
            .contains(
                AuditAction.PROJECT_CREATED,
                AuditAction.PROJECT_UPDATED,
                AuditAction.PROJECT_ARCHIVED,
                AuditAction.REQUIREMENT_CREATED,
                AuditAction.REQUIREMENT_REVISED,
                AuditAction.REQUIREMENT_CONFIRMED,
                AuditAction.REQUIREMENT_ARCHIVED
            );
    }

    @Test
    void returnsStructuredValidationErrors() throws Exception {
        loginTestUser();
        var response = sendJson("POST", "/api/v1/projects", """
            {"code":"","name":"","customerName":"demo"}
            """, 400);
        assertThat(response.required("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.required("fields").has("code")).isTrue();
        assertThat(response.required("fields").has("name")).isTrue();
    }

    @Test
    void authenticatesUsersAndEnforcesProjectRoles() throws Exception {
        var unauthenticated = sendJsonUnauthenticated("GET", "/api/v1/projects", null, 401);
        assertThat(unauthenticated.required("code").asText()).isEqualTo("UNAUTHENTICATED");

        loginTestUser();
        var suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var projectResponse = sendJson("POST", "/api/v1/projects", """
            {"code":"ROLE-%s","name":"Role integration project","customerName":null}
            """.formatted(suffix), 201);
        var projectId = UUID.fromString(projectResponse.required("id").asText());
        assertThat(projectResponse.required("role").asText()).isEqualTo("ADMIN");

        var viewerPassword = "viewer-password";
        var viewer = userRepository.save(new AppUser(
            "viewer-" + UUID.randomUUID() + "@bridgeflow.local",
            "Test Viewer",
            passwordEncoder.encode(viewerPassword)
        ));
        var project = projectRepository.findById(projectId).orElseThrow();
        memberRepository.save(new ProjectMember(project, viewer, ProjectRole.VIEWER));

        login(viewer.getEmail(), viewerPassword);
        var visibleProjects = sendJson("GET", "/api/v1/projects", null, 200);
        assertThat(visibleProjects.toString()).contains(projectId.toString(), "\"role\":\"VIEWER\"");
        var forbidden = sendJson("PATCH", "/api/v1/projects/" + projectId, """
            {"name":"Forbidden update","customerName":null}
            """, 403);
        assertThat(forbidden.required("code").asText()).isEqualTo("ACCESS_DENIED");
        var glossaryForbidden = sendJson("POST", "/api/v1/projects/" + projectId + "/glossary", """
            {"japaneseTerm":"閲覧者","vietnameseTerm":"Người xem","notes":null}
            """, 403);
        assertThat(glossaryForbidden.required("code").asText()).isEqualTo("ACCESS_DENIED");
        var documentForbidden = sendMultipart(
            "/api/v1/projects/" + projectId + "/documents",
            "Forbidden document", "forbidden.txt", "text/plain",
            "viewer cannot upload".getBytes(StandardCharsets.UTF_8), 403
        );
        assertThat(documentForbidden.required("code").asText()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void managesAProjectGlossaryAndRecordsAuditEvents() throws Exception {
        loginTestUser();
        var suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var project = sendJson("POST", "/api/v1/projects", """
            {"code":"GLOSS-%s","name":"Glossary integration project","customerName":null}
            """.formatted(suffix), 201);
        var projectId = project.required("id").asText();

        var term = sendJson("POST", "/api/v1/projects/" + projectId + "/glossary", """
            {"japaneseTerm":"注文履歴","vietnameseTerm":"Lịch sử đơn hàng","notes":"画面タイトル"}
            """, 201);
        var termId = term.required("id").asText();
        assertThat(term.required("japaneseTerm").asText()).isEqualTo("注文履歴");

        term = sendJson("PATCH", "/api/v1/projects/" + projectId + "/glossary/" + termId, """
            {"japaneseTerm":"注文履歴","vietnameseTerm":"Lịch sử đặt hàng","notes":"Thống nhất UI"}
            """, 200);
        assertThat(term.required("vietnameseTerm").asText()).isEqualTo("Lịch sử đặt hàng");

        var search = sendJson(
            "GET", "/api/v1/projects/" + projectId + "/glossary?query=%E6%B3%A8%E6%96%87", null, 200
        );
        assertThat(search).hasSize(1);
        assertThat(search.get(0).required("id").asText()).isEqualTo(termId);

        sendJson("DELETE", "/api/v1/projects/" + projectId + "/glossary/" + termId, "{}", 204);
        assertThat(sendJson("GET", "/api/v1/projects/" + projectId + "/glossary", null, 200)).isEmpty();
        assertThat(auditEventRepository.findAllByProjectIdOrderByOccurredAtDesc(UUID.fromString(projectId)))
            .extracting(event -> event.getAction())
            .contains(
                AuditAction.GLOSSARY_TERM_CREATED,
                AuditAction.GLOSSARY_TERM_UPDATED,
                AuditAction.GLOSSARY_TERM_DELETED
            );
    }

    @Test
    void uploadsVersionsDownloadsAndArchivesAProjectDocument() throws Exception {
        loginTestUser();
        var suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var project = sendJson("POST", "/api/v1/projects", """
            {"code":"DOC-%s","name":"Document integration project","customerName":null}
            """.formatted(suffix), 201);
        var projectId = project.required("id").asText();

        var invalidPdf = sendMultipart(
            "/api/v1/projects/" + projectId + "/documents",
            "Invalid PDF", "invalid.pdf", "application/pdf",
            "not a pdf".getBytes(StandardCharsets.UTF_8), 400
        );
        assertThat(invalidPdf.required("code").asText()).isEqualTo("INVALID_REQUEST");
        assertThat(sendJson("GET", "/api/v1/projects/" + projectId + "/documents", null, 200)).isEmpty();

        var firstContent = "第1版: 利用者は注文履歴を確認できる。".getBytes(StandardCharsets.UTF_8);
        var document = sendMultipart(
            "/api/v1/projects/" + projectId + "/documents",
            "注文管理仕様", "requirements-v1.txt", "text/plain", firstContent, 201
        );
        var documentId = document.required("id").asText();
        assertThat(document.required("latestVersion").required("versionNumber").asInt()).isEqualTo(1);
        assertThat(document.required("latestVersion").required("sha256").asText()).hasSize(64);

        var secondContent = "第2版: 利用者は注文履歴を検索できる。".getBytes(StandardCharsets.UTF_8);
        document = sendMultipart(
            "/api/v1/documents/" + documentId + "/versions",
            null, "requirements-v2.txt", "text/plain", secondContent, 200
        );
        var versionId = document.required("latestVersion").required("id").asText();
        assertThat(document.required("latestVersion").required("versionNumber").asInt()).isEqualTo(2);
        assertThat(document.required("versions")).hasSize(2);

        var documents = sendJson("GET", "/api/v1/projects/" + projectId + "/documents", null, 200);
        assertThat(documents).hasSize(1);
        assertThat(download("/api/v1/documents/" + documentId + "/versions/" + versionId + "/content"))
            .isEqualTo(secondContent);

        document = sendJson("POST", "/api/v1/documents/" + documentId + "/archive", "{}", 200);
        assertThat(document.required("status").asText()).isEqualTo("ARCHIVED");
        assertThat(sendJson("GET", "/api/v1/projects/" + projectId + "/documents", null, 200)).isEmpty();
        assertThat(sendJson(
            "GET", "/api/v1/projects/" + projectId + "/documents?includeArchived=true", null, 200
        )).hasSize(1);
        assertThat(auditEventRepository.findAllByProjectIdOrderByOccurredAtDesc(UUID.fromString(projectId)))
            .extracting(event -> event.getAction())
            .contains(
                AuditAction.DOCUMENT_UPLOADED,
                AuditAction.DOCUMENT_VERSION_ADDED,
                AuditAction.DOCUMENT_ARCHIVED
            );
    }

    @Test
    void extractsDraftRequirementsWithAProjectGlossaryAndAnAuditedAiJob() throws Exception {
        loginTestUser();
        var suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var project = sendJson("POST", "/api/v1/projects", """
            {"code":"AI-%s","name":"AI extraction project","customerName":null}
            """.formatted(suffix), 201);
        var projectId = project.required("id").asText();
        assertThat(project.required("aiEnabled").asBoolean()).isFalse();

        sendJson("POST", "/api/v1/projects/" + projectId + "/glossary", """
            {"japaneseTerm":"注文履歴","vietnameseTerm":"Lịch sử đơn hàng","notes":"UI label"}
            """, 201);

        var content = "利用者は注文履歴を確認できる。\n管理者は注文を検索できる。"
            .getBytes(StandardCharsets.UTF_8);
        var document = sendMultipart(
            "/api/v1/projects/" + projectId + "/documents",
            "AI source", "requirements.txt", "text/plain", content, 201
        );
        var documentId = document.required("id").asText();
        var versionId = document.required("latestVersion").required("id").asText();

        var disabled = sendJson(
            "POST", "/api/v1/documents/" + documentId + "/versions/" + versionId + "/ai-extractions",
            "{}", 400
        );
        assertThat(disabled.required("message").asText()).contains("AI chưa được bật");

        project = sendJson("PATCH", "/api/v1/projects/" + projectId, """
            {"name":"AI extraction project","customerName":null,"aiEnabled":true}
            """, 200);
        assertThat(project.required("aiEnabled").asBoolean()).isTrue();

        var job = sendJson(
            "POST", "/api/v1/documents/" + documentId + "/versions/" + versionId + "/ai-extractions",
            "{}", 200
        );
        assertThat(job.required("status").asText()).isEqualTo("COMPLETED");
        assertThat(job.required("provider").asText()).isEqualTo("stub");
        assertThat(job.required("candidateCount").asInt()).isEqualTo(2);
        assertThat(job.required("requirementIds")).hasSize(2);
        assertThat(job.required("errorCode").isNull()).isTrue();

        var requirements = sendJson(
            "GET", "/api/v1/projects/" + projectId + "/requirements?status=DRAFT", null, 200
        );
        assertThat(requirements.required("items")).hasSize(2);
        var requirementId = requirements.required("items").get(0).required("id").asText();
        var detail = sendJson("GET", "/api/v1/requirements/" + requirementId, null, 200);
        assertThat(detail.required("latestRevision").required("documentVersionId").asText()).isEqualTo(versionId);
        assertThat(detail.required("latestRevision").required("sourceAnchor").asText()).startsWith("line:");
        assertThat(detail.toString()).contains("Lịch sử đơn hàng");

        var sameJob = sendJson(
            "POST", "/api/v1/documents/" + documentId + "/versions/" + versionId + "/ai-extractions",
            "{}", 200
        );
        assertThat(sameJob.required("id").asText()).isEqualTo(job.required("id").asText());
        assertThat(sendJson("GET", "/api/v1/projects/" + projectId + "/ai-jobs", null, 200)).hasSize(1);
        assertThat(auditEventRepository.findAllByProjectIdOrderByOccurredAtDesc(UUID.fromString(projectId)))
            .extracting(event -> event.getAction())
            .contains(AuditAction.AI_EXTRACTION_REQUESTED, AuditAction.AI_EXTRACTION_COMPLETED);
    }

    @Test
    void generatesAnswersAndReviewsRequirementAnalysisDraftsWithoutDuplication() throws Exception {
        loginTestUser();
        var suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var project = sendJson("POST", "/api/v1/projects", """
            {"code":"ANALYZE-%s","name":"AI analysis project","customerName":null}
            """.formatted(suffix), 201);
        var projectId = project.required("id").asText();
        sendJson("POST", "/api/v1/projects/" + projectId + "/glossary", """
            {"japaneseTerm":"注文履歴","vietnameseTerm":"Lịch sử đơn hàng","notes":"UI label"}
            """, 201);

        var requirement = sendJson("POST", "/api/v1/projects/" + projectId + "/requirements", """
            {"displayKey":"REQ-ANALYSIS","japaneseText":"利用者は注文履歴を確認できる。","vietnameseText":"Người dùng có thể xem lịch sử đơn hàng."}
            """, 201);
        var requirementId = requirement.required("id").asText();
        var revisionId = requirement.required("latestRevision").required("id").asText();

        var disabled = sendJson(
            "POST", analysisPath(requirementId, revisionId) + "/ai-analysis", "{}", 400
        );
        assertThat(disabled.required("message").asText()).contains("AI chưa được bật");
        sendJson("PATCH", "/api/v1/projects/" + projectId, """
            {"name":"AI analysis project","customerName":null,"aiEnabled":true}
            """, 200);

        var analysis = sendJson(
            "POST", analysisPath(requirementId, revisionId) + "/ai-analysis", "{}", 200
        );
        var jobId = analysis.required("job").required("id").asText();
        assertThat(analysis.required("job").required("status").asText()).isEqualTo("COMPLETED");
        assertThat(analysis.required("job").required("purpose").asText()).isEqualTo("REQUIREMENT_ANALYSIS");
        assertThat(analysis.required("job").required("requirementRevisionId").asText()).isEqualTo(revisionId);
        assertThat(analysis.required("job").required("documentVersionId").isNull()).isTrue();
        assertThat(analysis.required("job").required("candidateCount").asInt()).isEqualTo(4);
        assertThat(analysis.required("questions")).hasSize(2);
        assertThat(analysis.required("acceptanceCriteria")).hasSize(2);

        var questionId = analysis.required("questions").get(0).required("id").asText();
        var answered = sendJson(
            "PATCH", analysisPath(requirementId, revisionId) + "/questions/" + questionId + "/answer", """
            {"japaneseText":"注文一覧が表示された時点です。","vietnameseText":"Khi danh sách đơn hàng được hiển thị."}
            """, 200
        );
        assertThat(answered.required("answerVietnamese").asText()).contains("danh sách đơn hàng");
        assertThat(answered.required("answeredAt").isNull()).isFalse();

        var reviewedQuestion = sendJson(
            "POST", analysisPath(requirementId, revisionId) + "/questions/" + questionId + "/review",
            "{\"decision\":\"APPROVED\"}", 200
        );
        assertThat(reviewedQuestion.required("status").asText()).isEqualTo("APPROVED");
        var criterionId = analysis.required("acceptanceCriteria").get(0).required("id").asText();
        var reviewedCriterion = sendJson(
            "POST", analysisPath(requirementId, revisionId) + "/acceptance-criteria/" + criterionId + "/review",
            "{\"decision\":\"REJECTED\"}", 200
        );
        assertThat(reviewedCriterion.required("status").asText()).isEqualTo("REJECTED");

        var sameAnalysis = sendJson(
            "POST", analysisPath(requirementId, revisionId) + "/ai-analysis", "{}", 200
        );
        assertThat(sameAnalysis.required("job").required("id").asText()).isEqualTo(jobId);
        assertThat(sameAnalysis.required("questions")).hasSize(2);
        assertThat(sameAnalysis.required("acceptanceCriteria")).hasSize(2);
        assertThat(sendJson("GET", analysisPath(requirementId, revisionId) + "/analysis", null, 200)
            .required("job").required("id").asText()).isEqualTo(jobId);
        assertThat(sendJson("GET", "/api/v1/projects/" + projectId + "/ai-jobs", null, 200)).hasSize(1);

        assertThat(auditEventRepository.findAllByProjectIdOrderByOccurredAtDesc(UUID.fromString(projectId)))
            .extracting(event -> event.getAction())
            .contains(
                AuditAction.AI_ANALYSIS_REQUESTED,
                AuditAction.AI_ANALYSIS_COMPLETED,
                AuditAction.CLARIFICATION_ANSWERED,
                AuditAction.CLARIFICATION_REVIEWED,
                AuditAction.ACCEPTANCE_CRITERION_REVIEWED
            );
    }

    @Test
    void generatesAndReviewsTestCasesFromApprovedAcceptanceCriteria() throws Exception {
        loginTestUser();
        var suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var project = sendJson("POST", "/api/v1/projects", """
            {"code":"TESTCASE-%s","name":"AI test-case project","customerName":null}
            """.formatted(suffix), 201);
        var projectId = project.required("id").asText();
        sendJson("PATCH", "/api/v1/projects/" + projectId, """
            {"name":"AI test-case project","customerName":null,"aiEnabled":true}
            """, 200);
        var requirement = sendJson("POST", "/api/v1/projects/" + projectId + "/requirements", """
            {"displayKey":"REQ-TEST-CASE","japaneseText":"利用者は注文履歴を確認できる。","vietnameseText":"Người dùng có thể xem lịch sử đơn hàng."}
            """, 201);
        var requirementId = requirement.required("id").asText();
        var revisionId = requirement.required("latestRevision").required("id").asText();
        var basePath = analysisPath(requirementId, revisionId);

        var analysis = sendJson("POST", basePath + "/ai-analysis", "{}", 200);
        var blocked = sendJson("POST", basePath + "/test-cases/ai-generation", "{}", 400);
        assertThat(blocked.required("message").asText()).contains("duyệt ít nhất một acceptance criterion");

        var firstCriterionId = analysis.required("acceptanceCriteria").get(0).required("id").asText();
        var secondCriterionId = analysis.required("acceptanceCriteria").get(1).required("id").asText();
        sendJson("POST", basePath + "/acceptance-criteria/" + firstCriterionId + "/review",
            "{\"decision\":\"APPROVED\"}", 200);
        sendJson("POST", basePath + "/acceptance-criteria/" + secondCriterionId + "/review",
            "{\"decision\":\"APPROVED\"}", 200);

        var generated = sendJson("POST", basePath + "/test-cases/ai-generation", "{}", 200);
        var jobId = generated.required("job").required("id").asText();
        assertThat(generated.required("job").required("purpose").asText()).isEqualTo("TEST_CASE_GENERATION");
        assertThat(generated.required("job").required("status").asText()).isEqualTo("COMPLETED");
        assertThat(generated.required("job").required("candidateCount").asInt()).isEqualTo(2);
        assertThat(generated.required("job").required("testCaseIds")).hasSize(2);
        assertThat(generated.required("testCases")).hasSize(2);
        assertThat(generated.required("testCases").get(0).required("acceptanceCriterionId").asText())
            .isIn(firstCriterionId, secondCriterionId);
        assertThat(generated.required("testCases").get(0).required("titleVietnamese").asText())
            .contains("Xác minh tiêu chí");

        var firstTestCaseId = generated.required("testCases").get(0).required("id").asText();
        var secondTestCaseId = generated.required("testCases").get(1).required("id").asText();
        assertThat(sendJson("POST", basePath + "/test-cases/" + firstTestCaseId + "/review",
            "{\"decision\":\"APPROVED\"}", 200).required("status").asText()).isEqualTo("APPROVED");
        assertThat(sendJson("POST", basePath + "/test-cases/" + secondTestCaseId + "/review",
            "{\"decision\":\"REJECTED\"}", 200).required("status").asText()).isEqualTo("REJECTED");

        var same = sendJson("POST", basePath + "/test-cases/ai-generation", "{}", 200);
        assertThat(same.required("job").required("id").asText()).isEqualTo(jobId);
        assertThat(same.required("testCases")).hasSize(2);
        assertThat(sendJson("GET", basePath + "/test-cases", null, 200)
            .required("job").required("id").asText()).isEqualTo(jobId);
        assertThat(sendJson("GET", "/api/v1/projects/" + projectId + "/ai-jobs", null, 200)).hasSize(2);

        assertThat(auditEventRepository.findAllByProjectIdOrderByOccurredAtDesc(UUID.fromString(projectId)))
            .extracting(event -> event.getAction())
            .contains(
                AuditAction.AI_TEST_CASE_GENERATION_REQUESTED,
                AuditAction.AI_TEST_CASE_GENERATION_COMPLETED,
                AuditAction.TEST_CASE_REVIEWED
            );
    }

    @Test
    void tracesArtifactsAndReportsRevisionChangeImpact() throws Exception {
        loginTestUser();
        var suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var project = sendJson("POST", "/api/v1/projects", """
            {"code":"TRACE-%s","name":"Traceability project","customerName":null}
            """.formatted(suffix), 201);
        var projectId = project.required("id").asText();
        sendJson("PATCH", "/api/v1/projects/" + projectId, """
            {"name":"Traceability project","customerName":null,"aiEnabled":true}
            """, 200);
        var requirement = sendJson("POST", "/api/v1/projects/" + projectId + "/requirements", """
            {"displayKey":"REQ-TRACE","japaneseText":"利用者は注文履歴を確認できる。","vietnameseText":"Người dùng có thể xem lịch sử đơn hàng."}
            """, 201);
        var requirementId = requirement.required("id").asText();
        var firstRevisionId = requirement.required("latestRevision").required("id").asText();
        var firstBase = analysisPath(requirementId, firstRevisionId);

        var analysis = sendJson("POST", firstBase + "/ai-analysis", "{}", 200);
        var criterionId = analysis.required("acceptanceCriteria").get(0).required("id").asText();
        sendJson("POST", firstBase + "/acceptance-criteria/" + criterionId + "/review",
            "{\"decision\":\"APPROVED\"}", 200);
        var testCases = sendJson("POST", firstBase + "/test-cases/ai-generation", "{}", 200);
        var testCaseId = testCases.required("testCases").get(0).required("id").asText();
        sendJson("POST", firstBase + "/test-cases/" + testCaseId + "/review",
            "{\"decision\":\"APPROVED\"}", 200);

        var revised = sendJson("POST", "/api/v1/requirements/" + requirementId + "/revisions", """
            {"japaneseText":"利用者は期間を指定して注文履歴を確認できる。","vietnameseText":"Người dùng có thể chọn khoảng thời gian để xem lịch sử đơn hàng.","changeType":"MODIFIED"}
            """, 200);
        var secondRevisionId = revised.required("latestRevision").required("id").asText();

        var traceability = sendJson("GET", "/api/v1/requirements/" + requirementId + "/traceability", null, 200);
        assertThat(traceability.required("revisions")).hasSize(2);
        assertThat(traceability.required("revisions").get(0).required("revisionId").asText())
            .isEqualTo(firstRevisionId);
        assertThat(traceability.required("revisions").get(0).required("artifacts")).hasSize(5);
        assertThat(traceability.required("revisions").get(0).required("artifacts").toString())
            .contains("CLARIFICATION_QUESTION", "ACCEPTANCE_CRITERION", "TEST_CASE", criterionId);
        assertThat(traceability.required("revisions").get(1).required("artifacts")).hasSize(0);

        var firstImpact = sendJson("GET", firstBase + "/change-impact", null, 200);
        assertThat(firstImpact.required("impactLevel").asText()).isEqualTo("LOW");
        assertThat(firstImpact.required("baselineRevisionId").isNull()).isTrue();

        var impact = sendJson("GET", analysisPath(requirementId, secondRevisionId) + "/change-impact", null, 200);
        assertThat(impact.required("baselineRevisionId").asText()).isEqualTo(firstRevisionId);
        assertThat(impact.required("impactLevel").asText()).isEqualTo("HIGH");
        assertThat(impact.required("japaneseChanged").asBoolean()).isTrue();
        assertThat(impact.required("vietnameseChanged").asBoolean()).isTrue();
        assertThat(impact.required("requiresArtifactRegeneration").asBoolean()).isTrue();
        assertThat(impact.required("affectedArtifacts")).hasSize(5);
        assertThat(impact.required("affectedArtifacts").toString())
            .contains("REVALIDATE", "TEST_CASE", testCaseId);
    }

    @Test
    void managesAuditedProjectScopedRequirementRelations() throws Exception {
        loginTestUser();
        var suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var project = sendJson("POST", "/api/v1/projects", """
            {"code":"REL-%s","name":"Relations project","customerName":null}
            """.formatted(suffix), 201);
        var projectId = project.required("id").asText();
        var first = sendJson("POST", "/api/v1/projects/" + projectId + "/requirements", """
            {"displayKey":"REQ-REL-A","japaneseText":"注文を登録する。","vietnameseText":"Tạo đơn hàng."}
            """, 201);
        var second = sendJson("POST", "/api/v1/projects/" + projectId + "/requirements", """
            {"displayKey":"REQ-REL-B","japaneseText":"注文履歴を表示する。","vietnameseText":"Hiển thị lịch sử đơn hàng."}
            """, 201);
        var firstId = first.required("id").asText();
        var secondId = second.required("id").asText();

        var relation = sendJson("POST", "/api/v1/requirements/" + firstId + "/relations", """
            {"targetRequirementId":"%s","relationType":"DEPENDS_ON"}
            """.formatted(secondId), 201);
        var relationId = relation.required("id").asText();
        assertThat(relation.required("sourceRequirementId").asText()).isEqualTo(firstId);
        assertThat(relation.required("targetRequirementId").asText()).isEqualTo(secondId);
        assertThat(relation.required("relationType").asText()).isEqualTo("DEPENDS_ON");

        var duplicate = sendJson("POST", "/api/v1/requirements/" + firstId + "/relations", """
            {"targetRequirementId":"%s","relationType":"DEPENDS_ON"}
            """.formatted(secondId), 400);
        assertThat(duplicate.required("message").asText()).contains("đã tồn tại");
        var self = sendJson("POST", "/api/v1/requirements/" + firstId + "/relations", """
            {"targetRequirementId":"%s","relationType":"DUPLICATES"}
            """.formatted(firstId), 400);
        assertThat(self.required("message").asText()).contains("chính nó");

        var otherProject = sendJson("POST", "/api/v1/projects", """
            {"code":"REL-OTHER-%s","name":"Other project","customerName":null}
            """.formatted(suffix), 201);
        var other = sendJson("POST", "/api/v1/projects/" + otherProject.required("id").asText() + "/requirements", """
            {"displayKey":"REQ-OTHER","japaneseText":"別プロジェクト。","vietnameseText":"Project khác."}
            """, 201);
        var crossProject = sendJson("POST", "/api/v1/requirements/" + firstId + "/relations", """
            {"targetRequirementId":"%s","relationType":"SUPERSEDES"}
            """.formatted(other.required("id").asText()), 400);
        assertThat(crossProject.required("message").asText()).contains("cùng một project");

        assertThat(sendJson("GET", "/api/v1/requirements/" + firstId + "/relations", null, 200)).hasSize(1);
        assertThat(sendJson("GET", "/api/v1/requirements/" + secondId + "/relations", null, 200)).hasSize(1);
        var sourceTrace = sendJson("GET", "/api/v1/requirements/" + firstId + "/traceability", null, 200);
        assertThat(sourceTrace.required("relations").get(0).required("direction").asText()).isEqualTo("OUTGOING");
        assertThat(sourceTrace.required("relations").get(0).required("relatedDisplayKey").asText())
            .isEqualTo("REQ-REL-B");
        var targetTrace = sendJson("GET", "/api/v1/requirements/" + secondId + "/traceability", null, 200);
        assertThat(targetTrace.required("relations").get(0).required("direction").asText()).isEqualTo("INCOMING");

        sendJson("DELETE", "/api/v1/requirements/" + firstId + "/relations/" + relationId, "{}", 204);
        assertThat(sendJson("GET", "/api/v1/requirements/" + firstId + "/relations", null, 200)).hasSize(0);
        assertThat(auditEventRepository.findAllByProjectIdOrderByOccurredAtDesc(UUID.fromString(projectId)))
            .extracting(event -> event.getAction())
            .contains(AuditAction.REQUIREMENT_RELATION_CREATED, AuditAction.REQUIREMENT_RELATION_DELETED);
    }

    @Test
    void exportsAuditedBilingualRequirementsAsCsvAndMarkdown() throws Exception {
        loginTestUser();
        var suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var project = sendJson("POST", "/api/v1/projects", """
            {"code":"EXPORT-%s","name":"Bilingual Export Project","customerName":null}
            """.formatted(suffix), 201);
        var projectId = project.required("id").asText();
        var first = sendJson("POST", "/api/v1/projects/" + projectId + "/requirements", """
            {"displayKey":"REQ-EXPORT-A","japaneseText":"利用者は「注文,履歴」を確認できる。","vietnameseText":"Người dùng xem lịch sử \\\"đơn hàng\\\"."}
            """, 201);
        var second = sendJson("POST", "/api/v1/projects/" + projectId + "/requirements", """
            {"displayKey":"REQ-EXPORT-B","japaneseText":"注文を登録する。","vietnameseText":"Tạo đơn hàng."}
            """, 201);
        var firstId = first.required("id").asText();
        var secondId = second.required("id").asText();
        sendJson("POST", "/api/v1/requirements/" + firstId + "/relations", """
            {"targetRequirementId":"%s","relationType":"DEPENDS_ON"}
            """.formatted(secondId), 201);

        var csvBytes = download("/api/v1/projects/" + projectId + "/exports/requirements.csv");
        assertThat(csvBytes).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        var csv = new String(csvBytes, StandardCharsets.UTF_8);
        assertThat(csv).contains(
            "Display Key,Status,Revision", "REQ-EXPORT-A", "REQ-EXPORT-B",
            "利用者は「注文,履歴」を確認できる。", "Người dùng xem lịch sử \"\"đơn hàng\"\".",
            "DEPENDS_ON → REQ-EXPORT-B", "\r\n"
        );

        var markdown = new String(
            download("/api/v1/projects/" + projectId + "/exports/requirements.md"),
            StandardCharsets.UTF_8
        );
        assertThat(markdown).contains(
            "# Bilingual Export Project — Requirements",
            "## REQ-EXPORT-A", "### 日本語", "### Tiếng Việt",
            "Relations: DEPENDS_ON → REQ-EXPORT-B"
        );
        assertThat(auditEventRepository.findAllByProjectIdOrderByOccurredAtDesc(UUID.fromString(projectId)))
            .extracting(event -> event.getAction())
            .contains(AuditAction.PROJECT_REQUIREMENTS_EXPORTED);
    }

    private String analysisPath(String requirementId, String revisionId) {
        return "/api/v1/requirements/" + requirementId + "/revisions/" + revisionId;
    }

    private UUID loginTestUser() throws Exception {
        var password = "integration-password";
        var user = userRepository.save(new AppUser(
            "integration-" + UUID.randomUUID() + "@bridgeflow.local",
            "Integration BrSE",
            passwordEncoder.encode(password)
        ));
        login(user.getEmail(), password);
        return user.getId();
    }

    private void login(String email, String password) throws Exception {
        var response = sendJsonUnauthenticated("POST", "/api/v1/auth/login", """
            {"email":"%s","password":"%s"}
            """.formatted(email, password), 200);
        accessToken = response.required("accessToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(response.required("user").required("email").asText()).isEqualTo(email);
    }

    private JsonNode sendMultipart(
        String path,
        String title,
        String filename,
        String contentType,
        byte[] fileContent,
        int expectedStatus
    ) throws Exception {
        var boundary = "BridgeFlowBoundary" + UUID.randomUUID().toString().replace("-", "");
        var prefix = new StringBuilder();
        if (title != null) {
            prefix.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"title\"\r\n\r\n")
                .append(title).append("\r\n");
        }
        prefix.append("--").append(boundary).append("\r\n")
            .append("Content-Disposition: form-data; name=\"file\"; filename=\"")
            .append(filename).append("\"\r\n")
            .append("Content-Type: ").append(contentType).append("\r\n\r\n");
        var prefixBytes = prefix.toString().getBytes(StandardCharsets.UTF_8);
        var suffixBytes = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        var body = new byte[prefixBytes.length + fileContent.length + suffixBytes.length];
        System.arraycopy(prefixBytes, 0, body, 0, prefixBytes.length);
        System.arraycopy(fileContent, 0, body, prefixBytes.length, fileContent.length);
        System.arraycopy(suffixBytes, 0, body, prefixBytes.length + fileContent.length, suffixBytes.length);

        var baseUrl = "http://127.0.0.1:" + environment.getRequiredProperty("local.server.port");
        var request = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(response.body()).isEqualTo(expectedStatus);
            return objectMapper.readTree(response.body());
        }
    }

    private byte[] download(String path) throws Exception {
        var baseUrl = "http://127.0.0.1:" + environment.getRequiredProperty("local.server.port");
        var request = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer " + accessToken)
            .GET().build();
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Content-Disposition")).isPresent();
            return response.body();
        }
    }

    private JsonNode sendJson(String method, String path, String body, int expectedStatus) throws Exception {
        return sendJson(method, path, body, expectedStatus, true);
    }

    private JsonNode sendJsonUnauthenticated(String method, String path, String body, int expectedStatus)
        throws Exception {
        return sendJson(method, path, body, expectedStatus, false);
    }

    private JsonNode sendJson(String method, String path, String body, int expectedStatus, boolean authenticated)
        throws Exception {
        var baseUrl = "http://127.0.0.1:" + environment.getRequiredProperty("local.server.port");
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(10));
        if (authenticated && accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        if (body == null) {
            builder.GET();
        } else {
            builder.header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(response.body()).isEqualTo(expectedStatus);
            return objectMapper.readTree(response.body());
        }
    }
}
