package com.nivya.network.repository;

import com.nivya.network.entity.NetworkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NetworkStatusRepository extends JpaRepository<NetworkStatus, Long> {

    Optional<NetworkStatus> findByDeviceId(Long deviceId);

    Optional<NetworkStatus> findByDeviceDeviceUuid(String deviceUuid);
}
