package com.bridgeflow.api.ai.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bridgeflow.api.ai.domain.AiJob;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {

    @Query("""
        SELECT job FROM AiJob job
        WHERE job.project.id = :projectId
        ORDER BY job.createdAt DESC
        """)
    List<AiJob> findAllForProject(@Param("projectId") UUID projectId);

    @Query("""
        SELECT job FROM AiJob job
        WHERE job.documentVersion.id = :documentVersionId
          AND job.purpose = :purpose
        """)
    Optional<AiJob> findForVersionAndPurpose(
        @Param("documentVersionId") UUID documentVersionId,
        @Param("purpose") String purpose
    );

    @Query("""
        SELECT job FROM AiJob job
        WHERE job.requirementRevision.id = :revisionId
          AND job.purpose = :purpose
        """)
    Optional<AiJob> findForRevisionAndPurpose(
        @Param("revisionId") UUID revisionId,
        @Param("purpose") String purpose
    );
}
