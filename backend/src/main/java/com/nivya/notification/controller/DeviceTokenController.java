package com.nivya.notification.controller;

import com.nivya.common.response.ApiResponse;
import com.nivya.notification.dto.PushTokenResponse;
import com.nivya.notification.dto.RegisterPushTokenRequest;
import com.nivya.notification.dto.UnregisterPushTokenRequest;
import com.nivya.notification.service.PushNotificationService;
import com.nivya.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing REST endpoints to securely register, refresh, and unregister FCM push tokens.
 */
@RestController
@RequestMapping("/api/v1/devices/push-token")
@Tag(name = "Push Notifications", description = "Device push token registration and lifecycle operations")
public class DeviceTokenController {

    private final PushNotificationService pushNotificationService;

    public DeviceTokenController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @PostMapping
    @Operation(summary = "Register or refresh FCM push token",
            description = "Associates an FCM push token with an authorized device for the authenticated user, handling device replacement.")
    public ResponseEntity<ApiResponse<PushTokenResponse>> registerPushToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterPushTokenRequest request) {

        PushTokenResponse response = pushNotificationService.registerDeviceToken(principal, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Push token registered successfully"));
    }

    @PostMapping("/unregister")
    @Operation(summary = "Unregister FCM push token upon logout",
            description = "Removes the registered push token to prevent notification delivery to logged-out devices.")
    public ResponseEntity<ApiResponse<Void>> unregisterPushToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) UnregisterPushTokenRequest request) {

        pushNotificationService.unregisterDeviceToken(principal, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Push token unregistered successfully"));
    }

    @GetMapping("/status")
    @Operation(summary = "Check push token registration status",
            description = "Returns current push registration state for the authenticated device.")
    public ResponseEntity<ApiResponse<PushTokenResponse>> getPushTokenStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String deviceUuid) {

        PushTokenResponse response = pushNotificationService.getDeviceTokenStatus(principal, deviceUuid);
        return ResponseEntity.ok(ApiResponse.success(response, "Push token status retrieved"));
    }
}
