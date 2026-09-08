package com.nivya.battery.repository;

import com.nivya.battery.entity.BatteryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BatteryStatusRepository extends JpaRepository<BatteryStatus, Long> {

    Optional<BatteryStatus> findByDeviceId(Long deviceId);

    Optional<BatteryStatus> findByDeviceDeviceUuid(String deviceUuid);
}
