package com.nivya.session.service;

import com.nivya.audit.service.AuditService;
import com.nivya.auth.repository.RefreshTokenRepository;
import com.nivya.email.service.EmailService;
import com.nivya.session.dto.DeviceSessionDto;
import com.nivya.session.entity.DeviceSession;
import com.nivya.session.repository.DeviceSessionRepository;
import com.nivya.user.entity.User;
import com.nivya.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DeviceSessionService {

    private static final Logger log = LoggerFactory.getLogger(DeviceSessionService.class);

    private final DeviceSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public DeviceSessionService(
            DeviceSessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            EmailService emailService,
            AuditService auditService,
            UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailService = emailService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @Transactional
    public DeviceSession recordLogin(User user, String fingerprint, String deviceName,
                                     String platform, String osVersion, String appVersion, String ipAddress) {
        String safeFingerprint = fingerprint != null && !fingerprint.isBlank()
                ? fingerprint
                : "fp-" + user.getId() + "-" + (ipAddress != null ? ipAddress.replace(":", ".") : "local");

        long previousSessionsCount = sessionRepository.countByUserIdAndDeviceFingerprint(user.getId(), safeFingerprint);
        boolean isNewDevice = (previousSessionsCount == 0);

        String approxLocation = resolveApproximateLocation(ipAddress);

        Optional<DeviceSession> activeSessionOpt = sessionRepository
                .findFirstByUserIdAndDeviceFingerprintAndStatus(user.getId(), safeFingerprint, "ACTIVE");

        DeviceSession session;
        if (activeSessionOpt.isPresent()) {
            session = activeSessionOpt.get();
            session.updateActivity();
            session.setIpAddress(ipAddress);
            session.setApproximateLocation(approxLocation);
            session.setAppVersion(appVersion);
        } else {
            session = new DeviceSession(user, safeFingerprint, deviceName, platform, osVersion, appVersion, ipAddress, approxLocation);
        }

        session = sessionRepository.save(session);
        log.info("Recorded authenticated device session id={} for user={} (isNewDevice={})",
                session.getId(), user.getEmail(), isNewDevice);

        auditService.logEvent(user.getId(), isNewDevice ? "AUTH_NEW_DEVICE_LOGIN" : "AUTH_DEVICE_LOGIN",
                "Session created on " + (platform != null ? platform : "UNKNOWN") + " (" + (deviceName != null ? deviceName : "Device") + ")",
                ipAddress);

        // Dispatches asynchronously without blocking login
        if (isNewDevice) {
            emailService.sendNewDeviceLoginNotificationAsync(user, deviceName, platform, osVersion, appVersion, ipAddress, approxLocation);
        }
        emailService.sendLoginNotificationAsync(user, deviceName, platform, osVersion, appVersion, ipAddress, approxLocation);

        return session;
    }

    @Transactional
    public void recordLogout(User user, String fingerprint, String ipAddress) {
        if (user == null) return;

        if (fingerprint != null && !fingerprint.isBlank()) {
            sessionRepository.findFirstByUserIdAndDeviceFingerprintAndStatus(user.getId(), fingerprint, "ACTIVE")
                    .ifPresent(session -> {
                        session.markLoggedOut();
                        sessionRepository.save(session);
                    });
        }

        // Send non-blocking logout email
        emailService.sendLogoutNotificationAsync(user, ipAddress);
    }

    @Transactional(readOnly = true)
    public List<DeviceSessionDto> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByLoginAtDesc(userId).stream()
                .map(DeviceSessionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeSession(Long userId, Long sessionId) {
        DeviceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized access to revoke session");
        }

        session.revoke();
        sessionRepository.save(session);

        // Also revoke any refresh tokens bound to this device fingerprint
        if (session.getDeviceFingerprint() != null) {
            refreshTokenRepository.revokeAllUserTokens(userId, Instant.now());
        }

        auditService.logEvent(userId, "SESSION_REVOKED", "Revoked session id " + sessionId, session.getIpAddress());
        log.info("Session {} revoked by user {}", sessionId, userId);
    }

    private String resolveApproximateLocation(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank() || "127.0.0.1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress)) {
            return "Local Network (Private)";
        }
        if (ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.") || ipAddress.startsWith("172.16.")) {
            return "Private Subnet";
        }
        return "Network Region (" + ipAddress + ")";
    }
}
