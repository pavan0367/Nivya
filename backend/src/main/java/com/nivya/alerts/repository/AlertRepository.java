package com.nivya.alerts.repository;

import com.nivya.alerts.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    List<Alert> findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(Long familyId);

    List<Alert> findByFamilyIdAndSeverityOrderByCreatedAtDesc(Long familyId, String severity);

    List<Alert> findByFamilyIdAndIsReadFalseOrderByCreatedAtDesc(Long familyId);

    List<Alert> findByFamilyIdAndTargetRoleInOrderByCreatedAtDesc(Long familyId, Collection<String> roles);

    List<Alert> findByDeviceIdAndTargetRoleInOrderByCreatedAtDesc(Long deviceId, Collection<String> roles);

    Optional<Alert> findFirstByDeviceIdAndAlertTypeAndResolvedFalse(Long deviceId, String alertType);

    long countByFamilyIdAndIsReadFalse(Long familyId);

    long countByDeviceIdAndTargetRoleInAndIsReadFalse(Long deviceId, Collection<String> roles);
}
