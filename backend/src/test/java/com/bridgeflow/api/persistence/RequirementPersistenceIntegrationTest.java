package com.bridgeflow.api.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

import jakarta.persistence.EntityManager;

import com.bridgeflow.api.project.domain.Project;
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
}
