package com.bridgeflow.api.project.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bridgeflow.retention.enabled", havingValue = "true")
public class ProjectRetentionScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectRetentionScheduler.class);
    private final ProjectDeletionService deletionService;

    public ProjectRetentionScheduler(ProjectDeletionService deletionService) {
        this.deletionService = deletionService;
    }

    @Scheduled(cron = "${bridgeflow.retention.cron:0 30 2 * * *}", zone = "UTC")
    public void purgeExpiredProjects() {
        var deleted = deletionService.purgeExpiredArchivedProjects();
        LOGGER.info("project_retention_completed deletedProjects={}", deleted);
    }
}
