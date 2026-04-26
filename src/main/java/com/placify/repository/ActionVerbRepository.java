package com.placify.repository;

import com.placify.model.ActionVerb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionVerbRepository extends JpaRepository<ActionVerb, Long> {
    List<ActionVerb> findByIsActiveTrue();
    List<ActionVerb> findByImpactLevelAndIsActiveTrue(String impactLevel);
}
