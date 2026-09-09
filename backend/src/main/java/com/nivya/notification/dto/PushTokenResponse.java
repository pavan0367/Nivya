package com.nivya.notification.dto;

import java.time.Instant;

/**
 * Response returned following push token registration or status inquiry.
 */
public class PushTokenResponse {

    private String deviceUuid;
    private String maskedToken;
    private String status;
    private Instant registeredAt;

    public PushTokenResponse() {
    }

    public PushTokenResponse(String deviceUuid, String pushToken, String status, Instant registeredAt) {
        this.deviceUuid = deviceUuid;
        this.maskedToken = maskToken(pushToken);
        this.status = status;
        this.registeredAt = registeredAt;
    }

    private static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public String getMaskedToken() {
        return maskedToken;
    }

    public void setMaskedToken(String maskedToken) {
        this.maskedToken = maskedToken;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }
}
