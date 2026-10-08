package com.bridgeflow.api.analysis.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bridgeflow.api.analysis.domain.ClarificationQuestion;

public interface ClarificationQuestionRepository extends JpaRepository<ClarificationQuestion, UUID> {
    @Query("""
        SELECT question FROM ClarificationQuestion question
        WHERE question.requirementRevision.id = :revisionId
        ORDER BY question.createdAt ASC
        """)
    List<ClarificationQuestion> findAllForRevision(@Param("revisionId") UUID revisionId);

    @Query("""
        SELECT question FROM ClarificationQuestion question
        WHERE question.aiJob.id = :jobId
        ORDER BY question.createdAt ASC
        """)
    List<ClarificationQuestion> findAllForJob(@Param("jobId") UUID jobId);

    @Query("""
        SELECT question FROM ClarificationQuestion question
        WHERE question.id = :id AND question.requirementRevision.id = :revisionId
        """)
    Optional<ClarificationQuestion> findForRevision(
        @Param("id") UUID id,
        @Param("revisionId") UUID revisionId
    );
}
