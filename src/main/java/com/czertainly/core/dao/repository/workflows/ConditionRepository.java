package com.czertainly.core.dao.repository.workflows;

import com.czertainly.api.model.core.auth.Resource;
import com.czertainly.core.dao.entity.workflows.Condition;
import com.czertainly.core.dao.repository.SecurityFilterRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConditionRepository extends SecurityFilterRepository<Condition, UUID> {

    boolean existsByName(String name);

    @EntityGraph(attributePaths = {"rules"})
    Optional<Condition> findWithRulesByUuid(UUID uuid);

    List<Condition> findAllByResource(Resource resource);

}
