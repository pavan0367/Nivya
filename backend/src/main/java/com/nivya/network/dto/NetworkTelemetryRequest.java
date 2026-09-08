package com.nivya.network.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class NetworkTelemetryRequest {

    @NotBlank(message = "Device UUID is required")
    private String deviceUuid;

    private String networkType = "NONE";
    private String connectionType;
    private Boolean isNetworkAvailable = false;
    private Boolean isInternetAvailable = false;
    private Integer signalLevel;
    private Integer signalDbm;
    private String quality = "UNAVAILABLE";
    private String ssid;
    private String ipAddress;
    private Instant recordedAt;

    public NetworkTelemetryRequest() {
    }

    public NetworkTelemetryRequest(String deviceUuid, String networkType, String connectionType,
                                   Boolean isNetworkAvailable, Boolean isInternetAvailable,
                                   Integer signalLevel, Integer signalDbm, String quality) {
        this.deviceUuid = deviceUuid;
        this.networkType = networkType;
        this.connectionType = connectionType;
        this.isNetworkAvailable = isNetworkAvailable;
        this.isInternetAvailable = isInternetAvailable;
        this.signalLevel = signalLevel;
        this.signalDbm = signalDbm;
        this.quality = quality;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public Boolean getIsNetworkAvailable() {
        return isNetworkAvailable;
    }

    public void setIsNetworkAvailable(Boolean isNetworkAvailable) {
        this.isNetworkAvailable = isNetworkAvailable;
    }

    public Boolean getIsInternetAvailable() {
        return isInternetAvailable;
    }

    public void setIsInternetAvailable(Boolean isInternetAvailable) {
        this.isInternetAvailable = isInternetAvailable;
    }

    public Integer getSignalLevel() {
        return signalLevel;
    }

    public void setSignalLevel(Integer signalLevel) {
        this.signalLevel = signalLevel;
    }

    public Integer getSignalDbm() {
        return signalDbm;
    }

    public void setSignalDbm(Integer signalDbm) {
        this.signalDbm = signalDbm;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
