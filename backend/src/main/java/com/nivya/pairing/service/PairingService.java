package com.nivya.pairing.service;

import com.nivya.audit.entity.AuditLog;
import com.nivya.audit.repository.AuditLogRepository;
import com.nivya.common.exception.ResourceNotFoundException;
import com.nivya.consent.entity.Consent;
import com.nivya.consent.repository.ConsentRepository;
import com.nivya.device.entity.Device;
import com.nivya.device.entity.DeviceStatus;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.device.repository.DeviceStatusRepository;
import com.nivya.family.entity.Family;
import com.nivya.family.entity.FamilyMember;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.family.repository.FamilyRepository;
import com.nivya.pairing.dto.*;
import com.nivya.pairing.entity.PairingRequest;
import com.nivya.pairing.entity.PairingStatus;
import com.nivya.pairing.exception.PairingException;
import com.nivya.pairing.exception.RateLimitExceededException;
import com.nivya.pairing.repository.PairingRequestRepository;
import com.nivya.role.RoleType;
import com.nivya.security.UserPrincipal;
import com.nivya.user.entity.User;
import com.nivya.user.repository.UserRepository;
import com.nivya.websocket.service.RealtimeBroadcastService;
import com.nivya.pairing.entity.DisconnectCode;
import com.nivya.pairing.repository.DisconnectCodeRepository;
import org.springframework.security.access.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service orchestrating the complete Nivya 10-step Device Pairing Protocol.
 */
@Service
public class PairingService {

    private static final Logger log = LoggerFactory.getLogger(PairingService.class);
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final long CODE_TTL_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PairingRequestRepository pairingRequestRepository;
    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceStatusRepository deviceStatusRepository;
    private final ConsentRepository consentRepository;
    private final AuditLogRepository auditLogRepository;
    private final PairingRateLimiter rateLimiter;
    private final RealtimeBroadcastService realtimeBroadcastService;
    private final DisconnectCodeRepository disconnectCodeRepository;

    public PairingService(PairingRequestRepository pairingRequestRepository,
                          UserRepository userRepository,
                          FamilyRepository familyRepository,
                          FamilyMemberRepository familyMemberRepository,
                          DeviceRepository deviceRepository,
                          DeviceStatusRepository deviceStatusRepository,
                          ConsentRepository consentRepository,
                          AuditLogRepository auditLogRepository,
                          PairingRateLimiter rateLimiter,
                          RealtimeBroadcastService realtimeBroadcastService,
                          DisconnectCodeRepository disconnectCodeRepository) {
        this.pairingRequestRepository = pairingRequestRepository;
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.deviceRepository = deviceRepository;
        this.deviceStatusRepository = deviceStatusRepository;
        this.consentRepository = consentRepository;
        this.auditLogRepository = auditLogRepository;
        this.rateLimiter = rateLimiter;
        this.realtimeBroadcastService = realtimeBroadcastService;
        this.disconnectCodeRepository = disconnectCodeRepository;
    }

    /**
     * Generates a one-time connection code with a 10-minute TTL.
     */
    @Transactional
    public PairingCodeResponse generatePairingCode(UserPrincipal principal, GenerateCodeRequest request, String ipAddress) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        RoleType myRole = user.getRole();
        RoleType targetRole = (myRole == RoleType.PARENT) ? RoleType.CHILD : RoleType.PARENT;

        // Invalidate any existing active pairing codes for this user
        List<PairingRequest> activeRequests = pairingRequestRepository.findByRequesterIdAndStatus(user.getId(), PairingStatus.PENDING);
        for (PairingRequest active : activeRequests) {
            active.setStatus(PairingStatus.REVOKED);
            pairingRequestRepository.save(active);
        }

        // Generate unique code in NV-XXXX-XXXX format
        String code = generateUniqueCode();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(CODE_TTL_MINUTES));
        String fingerprint = (request != null) ? request.getDeviceFingerprint() : null;

        PairingRequest pairingRequest = new PairingRequest(user, code, targetRole, expiresAt, fingerprint);
        pairingRequestRepository.save(pairingRequest);

        auditLogRepository.save(new AuditLog(
                user.getId(),
                "PAIRING_CODE_GENERATED",
                "Code generated with target role: " + targetRole + ", expires at: " + expiresAt,
                ipAddress
        ));

        log.info("User {} ({}) generated pairing code for target role {}", user.getEmail(), myRole, targetRole);
        return new PairingCodeResponse(code, myRole, targetRole, expiresAt, CODE_TTL_MINUTES * 60);
    }

    /**
     * Executes the strict 10-Step Connection Protocol.
     */
    @Transactional
    public PairingStatusResponse connectDevices(UserPrincipal principal, ConnectPairingRequest request, String ipAddress) {
        String rateLimitKey = principal.getId() + ":" + ipAddress;

        // Security check: Rate limiting and brute-force prevention
        if (rateLimiter.isRateLimited(rateLimitKey)) {
            auditLogRepository.save(new AuditLog(
                    principal.getId(),
                    "PAIRING_RATE_LIMIT_BLOCKED",
                    "Blocked due to excessive failed pairing attempts",
                    ipAddress
            ));
            throw new RateLimitExceededException("Too many failed pairing attempts. Please wait 15 minutes before trying again.");
        }

        // 1. Authenticate user
        User currentUser = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user record not found"));

        String rawCode = request.getCode();
        String normalizedCode = normalizeCode(rawCode);

        // 3. Validate code existence
        Optional<PairingRequest> optRequest = pairingRequestRepository.findByConnectionCode(normalizedCode);
        if (optRequest.isEmpty()) {
            rateLimiter.recordFailedAttempt(rateLimitKey);
            auditLogRepository.save(new AuditLog(
                    currentUser.getId(),
                    "PAIRING_CODE_NOT_FOUND",
                    "Attempted non-existent code: " + normalizedCode,
                    ipAddress
            ));
            throw new PairingException("Invalid or non-existent pairing code.");
        }

        PairingRequest pairingRequest = optRequest.get();

        // Check if code was already consumed or revoked
        if (pairingRequest.getStatus() != PairingStatus.PENDING) {
            rateLimiter.recordFailedAttempt(rateLimitKey);
            auditLogRepository.save(new AuditLog(
                    currentUser.getId(),
                    "PAIRING_CODE_ALREADY_USED",
                    "Attempted code with status: " + pairingRequest.getStatus(),
                    ipAddress
            ));
            throw new PairingException("This pairing code has already been used or invalidated.");
        }

        // 4. Validate code expiration
        if (pairingRequest.isExpired()) {
            pairingRequest.setStatus(PairingStatus.EXPIRED);
            pairingRequestRepository.save(pairingRequest);
            rateLimiter.recordFailedAttempt(rateLimitKey);
            auditLogRepository.save(new AuditLog(
                    currentUser.getId(),
                    "PAIRING_CODE_EXPIRED",
                    "Attempted expired code: " + normalizedCode,
                    ipAddress
            ));
            throw new PairingException("Pairing code has expired. Please request a new code.");
        }

        User requester = pairingRequest.getRequester();

        // Prevent self-pairing
        if (requester.getId().equals(currentUser.getId())) {
            rateLimiter.recordFailedAttempt(rateLimitKey);
            auditLogRepository.save(new AuditLog(
                    currentUser.getId(),
                    "PAIRING_SELF_PAIR_ATTEMPT",
                    "User attempted to pair with own code",
                    ipAddress
            ));
            throw new PairingException("Cannot pair a device with itself.");
        }

        // 2. Validate role compatibility (Parent <-> Child only)
        if (currentUser.getRole() == requester.getRole()) {
            rateLimiter.recordFailedAttempt(rateLimitKey);
            auditLogRepository.save(new AuditLog(
                    currentUser.getId(),
                    "PAIRING_ROLE_INCOMPATIBLE",
                    "Role mismatch: both users have role " + currentUser.getRole(),
                    ipAddress
            ));
            throw new PairingException("Role incompatibility: Pairing requires one Parent and one Child device.");
        }

        if (pairingRequest.getTargetRole() != currentUser.getRole()) {
            rateLimiter.recordFailedAttempt(rateLimitKey);
            throw new PairingException("Target role mismatch: Expected " + pairingRequest.getTargetRole());
        }

        // 5. Create / Resolve Family relationship
        Family family = resolveOrCreateFamily(currentUser, requester);

        // Synchronize family memberships
        ensureFamilyMembership(family, currentUser, currentUser.getRole());
        ensureFamilyMembership(family, requester, requester.getRole());

        // 6. Create / Update Device relationship
        Device currentDevice = processDeviceRegistration(currentUser, family, request.getDeviceInfo());

        // 7. Create Consent record
        createConsentRecord(currentUser, family);
        createConsentRecord(requester, family);

        // 8. Invalidate one-time pairing token
        pairingRequest.setStatus(PairingStatus.ACCEPTED);
        pairingRequest.setAcceptedAt(Instant.now());
        pairingRequest.setAcceptedBy(currentUser);
        pairingRequestRepository.save(pairingRequest);

        // Reset rate limiter on successful pairing
        rateLimiter.recordSuccess(rateLimitKey);

        // 9 & 10. Synchronize relationship and persist connection
        auditLogRepository.save(new AuditLog(
                currentUser.getId(),
                "PAIRING_SUCCESSFUL",
                String.format("User %s (%s) successfully paired with user %s (%s) into Family %s",
                        currentUser.getEmail(), currentUser.getRole(),
                        requester.getEmail(), requester.getRole(),
                        family.getFamilyCode()),
                ipAddress
        ));

        log.info("Pairing complete between {} and {} into family {}",
                currentUser.getEmail(), requester.getEmail(), family.getFamilyCode());

        PairingStatusResponse response = buildStatusResponse(family, currentUser.getRole());
        realtimeBroadcastService.broadcastPairingEvent(family.getId(), response);
        return response;
    }

    /**
     * Returns the current pairing and device status, including offline and stale indications.
     */
    @Transactional(readOnly = true)
    public PairingStatusResponse getPairingStatus(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        Optional<FamilyMember> optMembership = familyMemberRepository.findByUserId(user.getId());
        if (optMembership.isEmpty()) {
            return PairingStatusResponse.unpaired(user.getRole());
        }

        Family family = optMembership.get().getFamily();
        return buildStatusResponse(family, user.getRole());
    }

    /**
     * Unlinks a device from the family (Parent only).
     */
    @Transactional
    public void revokePairing(UserPrincipal principal, RevokePairingRequest request, String ipAddress) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != RoleType.PARENT) {
            throw new PairingException("Only parent accounts can revoke device connections.");
        }

        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + request.getDeviceId()));

        FamilyMember parentMembership = familyMemberRepository.findByUserId(user.getId())
                .orElseThrow(() -> new PairingException("Parent is not associated with any family."));

        if (device.getFamily() == null || !device.getFamily().getId().equals(parentMembership.getFamily().getId())) {
            throw new PairingException("Device does not belong to your family.");
        }

        device.setFamily(null);
        device.setStatus("REVOKED");
        deviceRepository.save(device);

        auditLogRepository.save(new AuditLog(
                user.getId(),
                "PAIRING_DEVICE_REVOKED",
                "Device " + device.getDeviceUuid() + " was unlinked from family by parent " + user.getEmail(),
                ipAddress
        ));

        log.info("Device {} revoked from family by {}", device.getDeviceUuid(), user.getEmail());

        PairingStatusResponse statusResponse = buildStatusResponse(parentMembership.getFamily(), RoleType.PARENT);
        realtimeBroadcastService.broadcastPairingEvent(parentMembership.getFamily().getId(), statusResponse);
    }

    /**
     * Generates a secure, one-time, 10-minute expiring disconnect code (Parent only).
     * Never logs plaintext code.
     */
    @Transactional
    public GenerateDisconnectCodeResponse generateDisconnectCode(UserPrincipal principal, String ipAddress) {
        if (principal.getRole() != RoleType.PARENT) {
            throw new AccessDeniedException("Only Parent accounts can generate a disconnect code");
        }

        FamilyMember parentMember = familyMemberRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new PairingException("Parent is not associated with any family"));
        Long familyId = parentMember.getFamily().getId();

        Long childUserId = familyMemberRepository.findByFamilyId(familyId).stream()
                .filter(m -> RoleType.CHILD.equals(m.getMemberRole()) || "CHILD".equalsIgnoreCase(String.valueOf(m.getMemberRole())))
                .map(m -> m.getUser().getId())
                .findFirst()
                .orElse(null);

        // Invalidate previous active disconnect codes for this family
        List<DisconnectCode> existingCodes = disconnectCodeRepository.findByFamilyIdAndStatus(familyId, "PENDING");
        for (DisconnectCode dc : existingCodes) {
            dc.revoke();
            disconnectCodeRepository.save(dc);
        }

        // Generate cryptographically secure random code
        String code = generateDisconnectCodeString();
        String codeHash = sha256(code);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(10));

        DisconnectCode disconnectCode = new DisconnectCode(familyId, principal.getId(), childUserId, codeHash, expiresAt);
        disconnectCodeRepository.save(disconnectCode);

        // Security audit: never log plaintext code
        auditLogRepository.save(new AuditLog(
                principal.getId(),
                "DISCONNECT_CODE_GENERATED",
                "Generated one-time disconnect code for family " + parentMember.getFamily().getFamilyCode() + ", expires: " + expiresAt,
                ipAddress
        ));

        log.info("Parent {} generated disconnect code for family {}", principal.getEmail(), parentMember.getFamily().getFamilyCode());
        return new GenerateDisconnectCodeResponse(code, expiresAt, 600);
    }

    /**
     * Validates Parent-generated disconnect code entered by Child device.
     * Enforces rate limiting, single-use, expiration, and family isolation.
     */
    @Transactional
    public void verifyDisconnectCode(UserPrincipal principal, VerifyDisconnectCodeRequest request, String ipAddress) {
        if (principal.getRole() != RoleType.CHILD) {
            throw new AccessDeniedException("Only Child companion devices can submit a disconnect code");
        }

        FamilyMember childMember = familyMemberRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new PairingException("Child is not associated with any family unit"));
        Long familyId = childMember.getFamily().getId();

        String rateLimitKey = "disconnect:" + principal.getId() + ":" + ipAddress;
        if (rateLimiter.isRateLimited(rateLimitKey)) {
            auditLogRepository.save(new AuditLog(
                    principal.getId(),
                    "DISCONNECT_RATE_LIMIT_BLOCKED",
                    "Rate limited on disconnect attempts",
                    ipAddress
            ));
            throw new RateLimitExceededException("Too many failed disconnect attempts. Please wait 15 minutes.");
        }

        String inputCode = normalizeDisconnectCode(request.getCode());
        String inputHash = sha256(inputCode);

        Optional<DisconnectCode> optCode = disconnectCodeRepository.findFirstByFamilyIdAndStatusOrderByCreatedAtDesc(familyId, "PENDING");
        if (optCode.isEmpty()) {
            rateLimiter.recordFailedAttempt(rateLimitKey);
            auditLogRepository.save(new AuditLog(
                    principal.getId(),
                    "DISCONNECT_CODE_NOT_FOUND",
                    "No pending disconnect code for family",
                    ipAddress
            ));
            throw new PairingException("Invalid, expired, or non-existent disconnect code.");
        }

        DisconnectCode dc = optCode.get();
        if (dc.isExpired()) {
            dc.setStatus("EXPIRED");
            disconnectCodeRepository.save(dc);
            rateLimiter.recordFailedAttempt(rateLimitKey);
            auditLogRepository.save(new AuditLog(
                    principal.getId(),
                    "DISCONNECT_CODE_EXPIRED",
                    "Disconnect code expired",
                    ipAddress
            ));
            throw new PairingException("Disconnect code has expired. Please ask parent for a new code.");
        }

        if (dc.isExhausted()) {
            rateLimiter.recordFailedAttempt(rateLimitKey);
            auditLogRepository.save(new AuditLog(
                    principal.getId(),
                    "DISCONNECT_CODE_EXHAUSTED",
                    "Disconnect code exceeded max attempts",
                    ipAddress
            ));
            throw new PairingException("Disconnect code has exceeded maximum verification attempts.");
        }

        dc.incrementAttempts();

        if (!dc.getCodeHash().equals(inputHash)) {
            disconnectCodeRepository.save(dc);
            rateLimiter.recordFailedAttempt(rateLimitKey);
            auditLogRepository.save(new AuditLog(
                    principal.getId(),
                    "DISCONNECT_CODE_MISMATCH",
                    "Incorrect disconnect code submitted",
                    ipAddress
            ));
            throw new PairingException("Invalid disconnect code.");
        }

        // Success: mark code as used
        dc.markUsed();
        disconnectCodeRepository.save(dc);
        rateLimiter.recordSuccess(rateLimitKey);

        // Unlink child device and remove child membership from family
        List<Device> childDevices = deviceRepository.findByFamilyId(familyId).stream()
                .filter(d -> d.getUser().getId().equals(principal.getId()))
                .toList();

        for (Device d : childDevices) {
            d.setFamily(null);
            d.setStatus("DISCONNECTED");
            deviceRepository.save(d);
        }

        familyMemberRepository.delete(childMember);

        auditLogRepository.save(new AuditLog(
                principal.getId(),
                "DISCONNECT_SUCCESSFUL",
                "Child device and membership successfully unlinked via validated parent code from family " + familyId,
                ipAddress
        ));

        log.info("Child {} successfully disconnected from family {}", principal.getEmail(), familyId);

        // Broadcast updated pairing status to family via WebSocket
        PairingStatusResponse statusResponse = buildStatusResponse(childMember.getFamily(), RoleType.PARENT);
        realtimeBroadcastService.broadcastPairingEvent(familyId, statusResponse);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Family resolveOrCreateFamily(User currentUser, User requester) {
        // Look up if either user has an existing family
        Optional<FamilyMember> curMember = familyMemberRepository.findByUserId(currentUser.getId());
        Optional<FamilyMember> reqMember = familyMemberRepository.findByUserId(requester.getId());

        if (curMember.isPresent()) {
            return curMember.get().getFamily();
        }
        if (reqMember.isPresent()) {
            return reqMember.get().getFamily();
        }

        // Neither has a family: create new
        User parentUser = (currentUser.getRole() == RoleType.PARENT) ? currentUser : requester;
        String familyName = parentUser.getName() + "'s Family";

        Family newFamily = new Family(familyName, parentUser);
        return familyRepository.save(newFamily);
    }

    private void ensureFamilyMembership(Family family, User user, RoleType role) {
        Optional<FamilyMember> existing = familyMemberRepository.findByFamilyIdAndUserId(family.getId(), user.getId());
        if (existing.isEmpty()) {
            FamilyMember member = new FamilyMember(family, user, role);
            familyMemberRepository.save(member);
        }
    }

    private Device processDeviceRegistration(User user, Family family, DeviceInfoDto info) {
        if (info == null || info.getDeviceUuid() == null || info.getDeviceUuid().isBlank()) {
            return null;
        }

        Device device = deviceRepository.findByDeviceUuid(info.getDeviceUuid())
                .orElseGet(() -> {
                    Device d = new Device();
                    d.setDeviceUuid(info.getDeviceUuid());
                    d.setUser(user);
                    return d;
                });

        device.setFamily(family);
        device.setUser(user);
        device.setDeviceName(info.getDeviceName() != null ? info.getDeviceName() : user.getName() + "'s Device");
        device.setPlatform(info.getPlatform() != null ? info.getPlatform() : "UNKNOWN");
        device.setOsVersion(info.getOsVersion());
        device.setAppVersion(info.getAppVersion());
        device.setPushToken(info.getPushToken());
        device.setStatus("ACTIVE");
        device.setLastSeenAt(Instant.now());

        Device savedDevice = deviceRepository.save(device);

        // Ensure DeviceStatus exists and set to online on active connection
        DeviceStatus status = deviceStatusRepository.findByDeviceId(savedDevice.getId())
                .orElseGet(() -> {
                    DeviceStatus s = new DeviceStatus();
                    s.setDevice(savedDevice);
                    return s;
                });

        status.setOnline(true);
        if (status.getBatteryPct() == null) {
            status.setBatteryPct(100);
        }
        if (status.getNetworkType() == null) {
            status.setNetworkType("WIFI");
        }
        if (status.getNetworkQuality() == null) {
            status.setNetworkQuality("GOOD");
        }
        status.setLastSyncAt(Instant.now());
        deviceStatusRepository.save(status);

        return savedDevice;
    }

    private void createConsentRecord(User user, Family family) {
        Consent consent = new Consent(user, family, "1.0", true, true);
        consentRepository.save(consent);
    }

    private PairingStatusResponse buildStatusResponse(Family family, RoleType userRole) {
        List<FamilyMember> members = familyMemberRepository.findByFamilyId(family.getId());
        List<LinkedMemberDto> memberDtos = new ArrayList<>();
        for (FamilyMember m : members) {
            User u = m.getUser();
            memberDtos.add(new LinkedMemberDto(u.getId(), u.getName(), u.getEmail(), m.getMemberRole(), m.getJoinedAt()));
        }

        List<Device> devices = deviceRepository.findByFamilyId(family.getId());
        List<DeviceStatusDto> deviceDtos = new ArrayList<>();
        Instant twoMinutesAgo = Instant.now().minusSeconds(120);

        for (Device d : devices) {
            Optional<DeviceStatus> optStatus = deviceStatusRepository.findByDeviceId(d.getId());
            boolean isOnline = optStatus.map(DeviceStatus::isOnline).orElse(false);
            Integer battery = optStatus.map(DeviceStatus::getBatteryPct).orElse(null);
            String netType = optStatus.map(DeviceStatus::getNetworkType).orElse("UNKNOWN");
            String netQuality = optStatus.map(DeviceStatus::getNetworkQuality).orElse("UNKNOWN");
            Instant lastSync = optStatus.map(DeviceStatus::getLastSyncAt).orElse(d.getLastSeenAt());
            Instant lastSeen = d.getLastSeenAt() != null ? d.getLastSeenAt() : lastSync;

            // Device is considered stale if offline OR hasn't reported within 2 minutes
            boolean isStale = !isOnline || (lastSeen != null && lastSeen.isBefore(twoMinutesAgo));

            deviceDtos.add(new DeviceStatusDto(
                    d.getId(),
                    d.getDeviceUuid(),
                    d.getDeviceName(),
                    d.getPlatform(),
                    isOnline,
                    battery,
                    netType,
                    netQuality,
                    lastSync,
                    lastSeen,
                    isStale
            ));
        }

        return new PairingStatusResponse(
                true,
                family.getId(),
                family.getFamilyCode(),
                family.getName(),
                userRole,
                memberDtos,
                deviceDtos
        );
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("NV-");
            for (int i = 0; i < 4; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            sb.append("-");
            for (int i = 0; i < 4; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (pairingRequestRepository.findByConnectionCode(code).isEmpty()) {
                return code;
            }
        }
        return "NV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return "";
        }
        String clean = code.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (clean.startsWith("NV") && clean.length() == 10) {
            return "NV-" + clean.substring(2, 6) + "-" + clean.substring(6, 10);
        }
        if (clean.length() == 8) {
            return "NV-" + clean.substring(0, 4) + "-" + clean.substring(4, 8);
        }
        return code.trim().toUpperCase();
    }

    private String generateDisconnectCodeString() {
        StringBuilder sb = new StringBuilder("DIS-");
        for (int i = 0; i < 4; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        sb.append("-");
        for (int i = 0; i < 4; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private String normalizeDisconnectCode(String code) {
        if (code == null) return "";
        String clean = code.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (clean.startsWith("DIS") && clean.length() == 11) {
            return "DIS-" + clean.substring(3, 7) + "-" + clean.substring(7, 11);
        }
        if (clean.length() == 8) {
            return "DIS-" + clean.substring(0, 4) + "-" + clean.substring(4, 8);
        }
        return code.trim().toUpperCase();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}
