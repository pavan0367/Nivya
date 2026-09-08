package com.nivya.pairing.dto;

public class GenerateCodeRequest {

    private String deviceFingerprint;

    public GenerateCodeRequest() {
    }

    public GenerateCodeRequest(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }
}
