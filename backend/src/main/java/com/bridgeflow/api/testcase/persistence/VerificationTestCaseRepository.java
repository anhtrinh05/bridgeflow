package com.bridgeflow.api.testcase.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bridgeflow.api.testcase.domain.VerificationTestCase;

public interface VerificationTestCaseRepository extends JpaRepository<VerificationTestCase, UUID> {
    @Query("SELECT tc FROM VerificationTestCase tc WHERE tc.requirementRevision.id = :revisionId ORDER BY tc.createdAt ASC")
    List<VerificationTestCase> findAllForRevision(@Param("revisionId") UUID revisionId);

    @Query("SELECT tc FROM VerificationTestCase tc WHERE tc.aiJob.id = :jobId ORDER BY tc.createdAt ASC")
    List<VerificationTestCase> findAllForJob(@Param("jobId") UUID jobId);

    @Query("SELECT tc FROM VerificationTestCase tc WHERE tc.id = :id AND tc.requirementRevision.id = :revisionId")
    Optional<VerificationTestCase> findForRevision(@Param("id") UUID id, @Param("revisionId") UUID revisionId);
}
