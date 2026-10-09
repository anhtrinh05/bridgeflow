package com.bridgeflow.api.project.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.bridgeflow.api.document.storage.LocalDocumentStorage;
import com.bridgeflow.api.project.domain.Project;
import com.bridgeflow.api.project.domain.ProjectDeletionReceipt;
import com.bridgeflow.api.project.domain.ProjectStatus;
import com.bridgeflow.api.project.persistence.ProjectDeletionReceiptRepository;
import com.bridgeflow.api.project.persistence.ProjectRepository;

@Service
public class ProjectDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectDeletionService.class);

    private final ProjectRepository projectRepository;
    private final ProjectDeletionReceiptRepository receiptRepository;
    private final LocalDocumentStorage storage;
    private final int archivedProjectDays;

    public ProjectDeletionService(
        ProjectRepository projectRepository,
        ProjectDeletionReceiptRepository receiptRepository,
        LocalDocumentStorage storage,
        @Value("${bridgeflow.retention.archived-project-days:365}") int archivedProjectDays
    ) {
        if (archivedProjectDays < 1) throw new IllegalArgumentException("Retention days must be positive.");
        this.projectRepository = projectRepository;
        this.receiptRepository = receiptRepository;
        this.storage = storage;
        this.archivedProjectDays = archivedProjectDays;
    }

    @Transactional
    public int purgeExpiredArchivedProjects() {
        var cutoff = Instant.now().minus(archivedProjectDays, ChronoUnit.DAYS);
        var projects = projectRepository.findAllByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
            ProjectStatus.ARCHIVED, cutoff
        );
        projects.forEach(project -> deleteProject(project, null, ProjectDeletionReason.RETENTION));
        return projects.size();
    }

    void deleteProject(Project project, UUID deletedBy, ProjectDeletionReason reason) {
        var projectId = project.getId();
        receiptRepository.save(new ProjectDeletionReceipt(projectId, project.getCode(), deletedBy, reason));
        if (projectRepository.deleteProjectAggregateById(projectId) != 1) {
            throw new IllegalStateException("Project deletion did not affect exactly one row: " + projectId);
        }
        registerStorageCleanup(projectId);
    }

    private void registerStorageCleanup(UUID projectId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            storage.deleteProject(projectId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    storage.deleteProject(projectId);
                } catch (RuntimeException exception) {
                    LOGGER.error("project_storage_cleanup_failed projectId={}", projectId, exception);
                }
            }
        });
    }
}
