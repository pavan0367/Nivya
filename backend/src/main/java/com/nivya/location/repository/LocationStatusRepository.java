package com.nivya.location.repository;

import com.nivya.location.entity.LocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationStatusRepository extends JpaRepository<LocationStatus, Long> {

    Optional<LocationStatus> findByDeviceId(Long deviceId);

    Optional<LocationStatus> findByDeviceDeviceUuid(String deviceUuid);
}
