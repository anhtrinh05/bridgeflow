package com.bridgeflow.api.requirement.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bridgeflow.api.requirement.domain.RequirementRelation;
import com.bridgeflow.api.requirement.domain.RequirementRelationType;

public interface RequirementRelationRepository extends JpaRepository<RequirementRelation, UUID> {
    @Query("""
        SELECT relation FROM RequirementRelation relation
        WHERE relation.sourceRequirement.id = :requirementId OR relation.targetRequirement.id = :requirementId
        ORDER BY relation.createdAt ASC
        """)
    List<RequirementRelation> findAllForRequirement(@Param("requirementId") UUID requirementId);

    boolean existsBySourceRequirementIdAndTargetRequirementIdAndRelationType(
        UUID sourceRequirementId, UUID targetRequirementId, RequirementRelationType relationType
    );

    @Query("""
        SELECT relation FROM RequirementRelation relation
        WHERE relation.id = :relationId
          AND (relation.sourceRequirement.id = :requirementId OR relation.targetRequirement.id = :requirementId)
        """)
    Optional<RequirementRelation> findForRequirement(
        @Param("relationId") UUID relationId,
        @Param("requirementId") UUID requirementId
    );
}
