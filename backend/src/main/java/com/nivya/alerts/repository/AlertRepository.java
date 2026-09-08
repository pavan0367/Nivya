package com.nivya.alerts.repository;

import com.nivya.alerts.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(Long familyId);

    Optional<Alert> findFirstByDeviceIdAndAlertTypeAndResolvedFalse(Long deviceId, String alertType);
}
