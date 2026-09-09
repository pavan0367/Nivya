package com.nivya.notification.dto;

import jakarta.validation.constraints.Size;

/**
 * Request payload to unregister a device push token upon logout.
 */
public class UnregisterPushTokenRequest {

    @Size(max = 64, message = "Device UUID must not exceed 64 characters")
    private String deviceUuid;

    @Size(max = 1024, message = "Push token must not exceed 1024 characters")
    private String pushToken;

    public UnregisterPushTokenRequest() {
    }

    public UnregisterPushTokenRequest(String deviceUuid, String pushToken) {
        this.deviceUuid = deviceUuid;
        this.pushToken = pushToken;
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
}
