package com.nivya.device.repository;

import com.nivya.device.entity.DeviceHealth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceHealthRepository extends JpaRepository<DeviceHealth, Long> {

    Optional<DeviceHealth> findByDeviceId(Long deviceId);

    Optional<DeviceHealth> findByDevice_DeviceUuid(String deviceUuid);
}
