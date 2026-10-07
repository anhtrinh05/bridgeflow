package com.bridgeflow.api.glossary.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bridgeflow.api.glossary.domain.GlossaryTerm;

public interface GlossaryTermRepository extends JpaRepository<GlossaryTerm, UUID> {

    @Query("""
        SELECT term FROM GlossaryTerm term
        WHERE term.project.id = :projectId
          AND (:query = ''
            OR LOWER(term.japaneseTerm) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(term.vietnameseTerm) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(COALESCE(term.notes, '')) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY term.japaneseTerm ASC
        """)
    List<GlossaryTerm> search(@Param("projectId") UUID projectId, @Param("query") String query);

    Optional<GlossaryTerm> findByIdAndProjectId(UUID id, UUID projectId);
}
