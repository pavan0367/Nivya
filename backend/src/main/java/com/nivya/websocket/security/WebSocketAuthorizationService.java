package com.nivya.websocket.security;

import com.nivya.device.entity.Device;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.family.repository.FamilyMemberRepository;
import com.nivya.role.RoleType;
import com.nivya.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service enforcing strict destination-level access control on WebSocket subscriptions.
 * Ensures users only receive telemetry, alerts, and events belonging to their authorized family/devices.
 */
@Service
public class WebSocketAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthorizationService.class);

    // Regex patterns for secure topics
    private static final Pattern DEVICE_TELEMETRY_PATTERN = Pattern.compile("^/topic/(battery|network|location|device)/(\\d+)(/.*)?$");
    private static final Pattern PARENT_ACTIVITY_PATTERN = Pattern.compile("^/topic/(activity|history)/(\\d+)(/.*)?$");
    private static final Pattern FAMILY_TOPIC_PATTERN = Pattern.compile("^/topic/(alerts|pairing|convocation)/(\\d+)(/.*)?$");

    private final DeviceRepository deviceRepository;
    private final FamilyMemberRepository familyMemberRepository;

    public WebSocketAuthorizationService(DeviceRepository deviceRepository,
                                         FamilyMemberRepository familyMemberRepository) {
        this.deviceRepository = deviceRepository;
        this.familyMemberRepository = familyMemberRepository;
    }

    /**
     * Validates whether the authenticated UserPrincipal has authorization to subscribe to the destination.
     * Throws AccessDeniedException if unauthorized.
     */
    public void authorizeSubscription(UserPrincipal principal, String destination) {
        if (principal == null) {
            throw new AccessDeniedException("Unauthenticated WebSocket session");
        }

        if (destination == null || destination.isBlank()) {
            throw new AccessDeniedException("Invalid destination");
        }

        // Allow user-specific queues (/user/queue/...) and application destinations (/app/...)
        if (destination.startsWith("/user/") || destination.startsWith("/app/")) {
            return;
        }

        // 1. Parent-only sensitive activity and history topics
        Matcher activityMatcher = PARENT_ACTIVITY_PATTERN.matcher(destination);
        if (activityMatcher.matches()) {
            if (principal.getRole() != RoleType.PARENT) {
                log.warn("Access denied: Non-parent user [{}] attempted to subscribe to sensitive topic [{}]",
                        principal.getId(), destination);
                throw new AccessDeniedException("Parent-only topic: Access denied for role " + principal.getRole());
            }

            Long deviceId = Long.parseLong(activityMatcher.group(2));
            verifyDeviceAccess(principal, deviceId, destination);
            return;
        }

        // 2. Device telemetry topics (/topic/battery/{deviceId}, /topic/network/{deviceId}, etc.)
        Matcher devMatcher = DEVICE_TELEMETRY_PATTERN.matcher(destination);
        if (devMatcher.matches()) {
            Long deviceId = Long.parseLong(devMatcher.group(2));
            verifyDeviceAccess(principal, deviceId, destination);
            return;
        }

        // 3. Family-scoped topics (/topic/alerts/{familyId}, /topic/pairing/{familyId}, etc.)
        Matcher familyMatcher = FAMILY_TOPIC_PATTERN.matcher(destination);
        if (familyMatcher.matches()) {
            Long familyId = Long.parseLong(familyMatcher.group(2));
            boolean isMember = familyMemberRepository.existsByFamilyIdAndUserId(familyId, principal.getId());
            if (!isMember) {
                log.warn("Access denied: User [{}] does not belong to family [{}] for destination [{}]",
                        principal.getId(), familyId, destination);
                throw new AccessDeniedException("Unauthorized family topic: " + destination);
            }
            return;
        }

        // General broadcast or public topics (like base /topic/convocation/messages)
        if (destination.startsWith("/topic/convocation")) {
            // General convocation topic allowed for authenticated family members
            return;
        }

        log.warn("Access denied: Unrecognized or restricted topic destination [{}] requested by user [{}]",
                destination, principal.getId());
        throw new AccessDeniedException("Restricted destination: " + destination);
    }

    private void verifyDeviceAccess(UserPrincipal principal, Long deviceId, String destination) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            log.warn("Subscription denied: Device [{}] not found for destination [{}]", deviceId, destination);
            throw new AccessDeniedException("Device not found: " + deviceId);
        }

        Device device = deviceOpt.get();

        // Check if user owns the device
        if (device.getUser() != null && device.getUser().getId().equals(principal.getId())) {
            return;
        }

        // Check if user belongs to the same family as the device
        if (device.getFamily() != null) {
            boolean isFamilyMember = familyMemberRepository.existsByFamilyIdAndUserId(device.getFamily().getId(), principal.getId());
            if (isFamilyMember) {
                return;
            }
        }

        log.warn("Access denied: User [{}] does not own and is not in the family of device [{}]",
                principal.getId(), deviceId);
        throw new AccessDeniedException("Unauthorized access to device " + deviceId);
    }
}
