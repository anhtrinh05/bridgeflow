package com.bridgeflow.api.requirement.persistence;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bridgeflow.api.requirement.domain.Requirement;
import com.bridgeflow.api.requirement.domain.RequirementStatus;

public interface RequirementRepository extends JpaRepository<Requirement, UUID> {

    Optional<Requirement> findByProjectIdAndDisplayKey(UUID projectId, String displayKey);

    @Query(
        value = """
            SELECT DISTINCT requirement
            FROM Requirement requirement
            WHERE requirement.project.id = :projectId
              AND (:includeArchived = true OR requirement.status <> com.bridgeflow.api.requirement.domain.RequirementStatus.ARCHIVED)
              AND (:status IS NULL OR requirement.status = :status)
              AND (
                :query = ''
                OR LOWER(requirement.displayKey) LIKE LOWER(CONCAT('%', :query, '%'))
                OR EXISTS (
                  SELECT revision.id
                  FROM RequirementRevision revision
                  WHERE revision.requirement = requirement
                    AND (
                      LOWER(revision.japaneseText) LIKE LOWER(CONCAT('%', :query, '%'))
                      OR LOWER(revision.vietnameseText) LIKE LOWER(CONCAT('%', :query, '%'))
                    )
                )
              )
            """,
        countQuery = """
            SELECT COUNT(requirement)
            FROM Requirement requirement
            WHERE requirement.project.id = :projectId
              AND (:includeArchived = true OR requirement.status <> com.bridgeflow.api.requirement.domain.RequirementStatus.ARCHIVED)
              AND (:status IS NULL OR requirement.status = :status)
              AND (
                :query = ''
                OR LOWER(requirement.displayKey) LIKE LOWER(CONCAT('%', :query, '%'))
                OR EXISTS (
                  SELECT revision.id
                  FROM RequirementRevision revision
                  WHERE revision.requirement = requirement
                    AND (
                      LOWER(revision.japaneseText) LIKE LOWER(CONCAT('%', :query, '%'))
                      OR LOWER(revision.vietnameseText) LIKE LOWER(CONCAT('%', :query, '%'))
                    )
                )
              )
            """
    )
    Page<Requirement> search(
        @Param("projectId") UUID projectId,
        @Param("status") RequirementStatus status,
        @Param("includeArchived") boolean includeArchived,
        @Param("query") String query,
        Pageable pageable
    );

    long countByProjectId(UUID projectId);

    long countByProjectIdAndStatusNot(UUID projectId, RequirementStatus status);

    List<Requirement> findAllByProjectIdAndStatusNotOrderByDisplayKeyAsc(
        UUID projectId, RequirementStatus status
    );
}
