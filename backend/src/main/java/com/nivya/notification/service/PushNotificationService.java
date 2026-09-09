package com.nivya.notification.service;

import com.google.firebase.messaging.*;
import com.nivya.common.exception.ResourceNotFoundException;
import com.nivya.device.entity.Device;
import com.nivya.device.repository.DeviceRepository;
import com.nivya.notification.config.FirebaseConfig;
import com.nivya.notification.dto.PushTokenResponse;
import com.nivya.notification.dto.RegisterPushTokenRequest;
import com.nivya.notification.dto.UnregisterPushTokenRequest;
import com.nivya.security.UserPrincipal;
import com.nivya.user.entity.User;
import com.nivya.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service orchestrating FCM push notification delivery, device token lifecycle,
 * token revocation on error, and decoy notifications.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    public static final String CONVOCATION_DECOY_TITLE = "Device Update";
    public static final String CONVOCATION_DECOY_BODY = "Check your battery status";

    private final FirebaseConfig firebaseConfig;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public PushNotificationService(FirebaseConfig firebaseConfig,
                                   DeviceRepository deviceRepository,
                                   UserRepository userRepository) {
        this.firebaseConfig = firebaseConfig;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    /**
     * Sends generic decoy push notification for Convocation messages.
     * STRICT REQUIREMENT:
     * - EXACT text: "Check your battery status"
     * - Never send the actual message in the notification payload.
     * - Tapping opens Nivya normally.
     */
    public boolean sendConvocationNotification(String pushToken) {
        if (pushToken == null || pushToken.isBlank()) {
            log.warn("Cannot send Convocation push notification: push token is null or blank.");
            return false;
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", "CONVOCATION");
        data.put("title", CONVOCATION_DECOY_TITLE);
        data.put("body", CONVOCATION_DECOY_BODY);
        data.put("action", "OPEN_APP");

        return dispatchPush(pushToken, CONVOCATION_DECOY_TITLE, CONVOCATION_DECOY_BODY, data);
    }

    /**
     * Sends alert push notification for low battery, offline, reconnected, or important alerts.
     */
    public boolean sendAlertNotification(String pushToken, String alertType, String severity, String title, String message) {
        if (pushToken == null || pushToken.isBlank()) {
            log.warn("Cannot send Alert push notification: push token is null or blank.");
            return false;
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", "ALERT");
        data.put("alertType", alertType);
        data.put("severity", severity != null ? severity : "WARNING");
        data.put("title", title);
        data.put("message", message);
        data.put("action", "OPEN_APP");

        return dispatchPush(pushToken, title, message, data);
    }

    /**
     * Core dispatch routine to FCM or simulation fallback.
     */
    private boolean dispatchPush(String pushToken, String title, String body, Map<String, String> data) {
        // Handle mock/simulation triggers or test cases
        if ("REVOKED_TOKEN".equals(pushToken) || "INVALID_TOKEN".equals(pushToken)) {
            log.warn("Simulating FCM error for token '{}': token is revoked/unregistered.", pushToken);
            handleInvalidOrRevokedToken(pushToken);
            return false;
        }

        if (!firebaseConfig.isInitialized()) {
            log.info("Simulated FCM Push dispatched [token='{}']: title='{}', body='{}', type='{}'",
                    maskToken(pushToken), title, body, data.get("type"));
            return true;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(pushToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data);

            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("FCM message successfully dispatched: messageId={}, type={}", response, data.get("type"));
            return true;
        } catch (FirebaseMessagingException e) {
            log.error("FCM dispatch failed for token='{}': code={}, message={}",
                    maskToken(pushToken), e.getMessagingErrorCode(), e.getMessage());

            if (isUnregisteredOrInvalidToken(e)) {
                handleInvalidOrRevokedToken(pushToken);
            }
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during FCM dispatch: {}", e.getMessage());
            return false;
        }
    }

    private boolean isUnregisteredOrInvalidToken(FirebaseMessagingException e) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();
        if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            return true;
        }
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("not-registered") || msg.contains("invalid-registration-token") || msg.contains("not found");
    }

    /**
     * Revokes and cleanses an invalid or unregistered token from the database.
     */
    @Transactional
    public void handleInvalidOrRevokedToken(String pushToken) {
        List<Device> devices = deviceRepository.findAllByPushToken(pushToken);
        for (Device device : devices) {
            device.setPushToken(null);
            deviceRepository.save(device);
            log.warn("Revoked invalid/unregistered push token from device uuid={}", device.getDeviceUuid());
        }
    }

    /**
     * Securely registers or refreshes a push token for an authorized device.
     * Handles device replacement by dissociating this token if previously bound to another device.
     */
    @Transactional
    public PushTokenResponse registerDeviceToken(UserPrincipal principal, RegisterPushTokenRequest request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + principal.getId()));

        String newToken = request.getPushToken().trim();

        // 1. Device replacement handling: clear this token if already assigned to any other device
        List<Device> previousHolders = deviceRepository.findAllByPushToken(newToken);
        for (Device holder : previousHolders) {
            if (request.getDeviceUuid() == null || !request.getDeviceUuid().equals(holder.getDeviceUuid())) {
                holder.setPushToken(null);
                deviceRepository.save(holder);
                log.info("Cleared token from replaced device uuid={}", holder.getDeviceUuid());
            }
        }

        // 2. Identify or create device for authenticated user
        Device device;
        if (request.getDeviceUuid() != null && !request.getDeviceUuid().isBlank()) {
            Optional<Device> deviceOpt = deviceRepository.findByUserIdAndDeviceUuid(user.getId(), request.getDeviceUuid().trim());
            if (deviceOpt.isPresent()) {
                device = deviceOpt.get();
            } else {
                // New device record for this authorized user
                device = new Device();
                device.setUser(user);
                device.setDeviceUuid(request.getDeviceUuid().trim());
                device.setDeviceName(request.getDeviceName() != null ? request.getDeviceName() : "Android Device");
                device.setPlatform(request.getPlatform() != null ? request.getPlatform() : "ANDROID");
                device.setStatus("ACTIVE");
                device.setCreatedAt(Instant.now());
            }
        } else {
            // Match first device for user, or create one
            device = deviceRepository.findFirstByUserIdOrderByIdAsc(user.getId())
                    .orElseGet(() -> {
                        Device d = new Device();
                        d.setUser(user);
                        d.setDeviceUuid(java.util.UUID.randomUUID().toString());
                        d.setDeviceName(request.getDeviceName() != null ? request.getDeviceName() : "Android Device");
                        d.setPlatform(request.getPlatform() != null ? request.getPlatform() : "ANDROID");
                        d.setStatus("ACTIVE");
                        d.setCreatedAt(Instant.now());
                        return d;
                    });
        }

        device.setPushToken(newToken);
        if (request.getPlatform() != null && !request.getPlatform().isBlank()) {
            device.setPlatform(request.getPlatform());
        }
        if (request.getDeviceName() != null && !request.getDeviceName().isBlank()) {
            device.setDeviceName(request.getDeviceName());
        }
        device.setLastSeenAt(Instant.now());
        device.setUpdatedAt(Instant.now());

        device = deviceRepository.save(device);
        log.info("Associated push token for authorized device uuid={} and user id={}", device.getDeviceUuid(), user.getId());

        return new PushTokenResponse(device.getDeviceUuid(), device.getPushToken(), "REGISTERED", device.getUpdatedAt());
    }

    /**
     * Unregisters push token upon user logout.
     */
    @Transactional
    public void unregisterDeviceToken(UserPrincipal principal, UnregisterPushTokenRequest request) {
        if (request != null && request.getDeviceUuid() != null && !request.getDeviceUuid().isBlank()) {
            deviceRepository.findByUserIdAndDeviceUuid(principal.getId(), request.getDeviceUuid().trim())
                    .ifPresent(d -> {
                        d.setPushToken(null);
                        d.setUpdatedAt(Instant.now());
                        deviceRepository.save(d);
                        log.info("Unregistered push token for device uuid={} on logout", d.getDeviceUuid());
                    });
            return;
        }

        if (request != null && request.getPushToken() != null && !request.getPushToken().isBlank()) {
            handleInvalidOrRevokedToken(request.getPushToken().trim());
            return;
        }

        // Default: clear push tokens for all devices belonging to logging-out user
        List<Device> userDevices = deviceRepository.findByUserId(principal.getId());
        for (Device d : userDevices) {
            d.setPushToken(null);
            d.setUpdatedAt(Instant.now());
            deviceRepository.save(d);
        }
        log.info("Unregistered push tokens for all devices of user id={} on logout", principal.getId());
    }

    /**
     * Returns push token status for the authenticated user's device.
     */
    @Transactional(readOnly = true)
    public PushTokenResponse getDeviceTokenStatus(UserPrincipal principal, String deviceUuid) {
        Optional<Device> deviceOpt;
        if (deviceUuid != null && !deviceUuid.isBlank()) {
            deviceOpt = deviceRepository.findByUserIdAndDeviceUuid(principal.getId(), deviceUuid.trim());
        } else {
            deviceOpt = deviceRepository.findFirstByUserIdOrderByIdAsc(principal.getId());
        }

        if (deviceOpt.isPresent()) {
            Device d = deviceOpt.get();
            String status = (d.getPushToken() != null && !d.getPushToken().isBlank()) ? "ACTIVE" : "UNREGISTERED";
            return new PushTokenResponse(d.getDeviceUuid(), d.getPushToken(), status, d.getUpdatedAt());
        }

        return new PushTokenResponse(deviceUuid, null, "UNREGISTERED", Instant.now());
    }

    private static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
