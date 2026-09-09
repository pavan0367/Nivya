package com.nivya.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload to register or refresh a device's push notification token.
 * Securely associates the push token with the authenticated user's device.
 */
public class RegisterPushTokenRequest {

    @Size(max = 64, message = "Device UUID must not exceed 64 characters")
    private String deviceUuid;

    @NotBlank(message = "Push token is required")
    @Size(max = 1024, message = "Push token must not exceed 1024 characters")
    private String pushToken;

    @Size(max = 20, message = "Platform must not exceed 20 characters")
    private String platform;

    @Size(max = 100, message = "Device name must not exceed 100 characters")
    private String deviceName;

    public RegisterPushTokenRequest() {
    }

    public RegisterPushTokenRequest(String deviceUuid, String pushToken, String platform, String deviceName) {
        this.deviceUuid = deviceUuid;
        this.pushToken = pushToken;
        this.platform = platform;
        this.deviceName = deviceName;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
