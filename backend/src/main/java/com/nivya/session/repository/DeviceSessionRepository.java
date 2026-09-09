package com.nivya.session.repository;

import com.nivya.session.entity.DeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceSessionRepository extends JpaRepository<DeviceSession, Long> {

    List<DeviceSession> findByUserIdOrderByLoginAtDesc(Long userId);

    Optional<DeviceSession> findFirstByUserIdAndDeviceFingerprintAndStatus(Long userId, String deviceFingerprint, String status);

    long countByUserIdAndDeviceFingerprint(Long userId, String deviceFingerprint);
}
