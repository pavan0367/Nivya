package com.nivya.alerts.repository;

import com.nivya.alerts.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findByFamilyId(Long familyId);

    Optional<AlertRule> findByFamilyIdAndRuleType(Long familyId, String ruleType);

    boolean existsByFamilyId(Long familyId);
}
