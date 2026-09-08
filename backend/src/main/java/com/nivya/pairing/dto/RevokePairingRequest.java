package com.nivya.pairing.dto;

import jakarta.validation.constraints.NotNull;

public class RevokePairingRequest {

    @NotNull(message = "Device ID cannot be null")
    private Long deviceId;

    public RevokePairingRequest() {
    }

    public RevokePairingRequest(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }
}
