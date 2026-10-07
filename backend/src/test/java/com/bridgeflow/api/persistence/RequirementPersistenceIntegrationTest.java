package com.bridgeflow.api.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
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
    static void stopContainer() {
        if (POSTGRES.isRunning()) {
            POSTGRES.stop();
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
        var schemas = contract.required("components").required("schemas");
        assertThat(schemas.has("RequirementResponse")).isTrue();
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
