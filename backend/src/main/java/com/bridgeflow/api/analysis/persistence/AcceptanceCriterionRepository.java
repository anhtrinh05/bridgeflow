package com.bridgeflow.api.analysis.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bridgeflow.api.analysis.domain.AcceptanceCriterion;
import com.bridgeflow.api.analysis.domain.ArtifactReviewStatus;

public interface AcceptanceCriterionRepository extends JpaRepository<AcceptanceCriterion, UUID> {
    @Query("""
        SELECT criterion FROM AcceptanceCriterion criterion
        WHERE criterion.requirementRevision.id = :revisionId
        ORDER BY criterion.createdAt ASC
        """)
    List<AcceptanceCriterion> findAllForRevision(@Param("revisionId") UUID revisionId);

    @Query("""
        SELECT criterion FROM AcceptanceCriterion criterion
        WHERE criterion.aiJob.id = :jobId
        ORDER BY criterion.createdAt ASC
        """)
    List<AcceptanceCriterion> findAllForJob(@Param("jobId") UUID jobId);

    @Query("""
        SELECT criterion FROM AcceptanceCriterion criterion
        WHERE criterion.id = :id AND criterion.requirementRevision.id = :revisionId
        """)
    Optional<AcceptanceCriterion> findForRevision(
        @Param("id") UUID id,
        @Param("revisionId") UUID revisionId
    );

    @Query("""
        SELECT criterion FROM AcceptanceCriterion criterion
        WHERE criterion.requirementRevision.id = :revisionId AND criterion.status = :status
        ORDER BY criterion.createdAt ASC
        """)
    List<AcceptanceCriterion> findAllForRevisionAndStatus(
        @Param("revisionId") UUID revisionId,
        @Param("status") ArtifactReviewStatus status
    );
}
