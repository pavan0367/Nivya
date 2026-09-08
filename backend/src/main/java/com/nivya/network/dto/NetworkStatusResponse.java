package com.nivya.network.dto;

import java.time.Instant;

public class NetworkStatusResponse {

    private Long deviceId;
    private String deviceUuid;
    private String deviceName;
    private String networkType;
    private String connectionType;
    private Boolean isNetworkAvailable;
    private Boolean isInternetAvailable;
    private Integer signalLevel;
    private Integer signalDbm;
    private String quality;
    private String ssid;
    private String ipAddress;
    private Instant lastSyncAt;

    public NetworkStatusResponse() {
    }

    public NetworkStatusResponse(Long deviceId, String deviceUuid, String deviceName,
                                 String networkType, String connectionType,
                                 Boolean isNetworkAvailable, Boolean isInternetAvailable,
                                 Integer signalLevel, Integer signalDbm, String quality,
                                 String ssid, String ipAddress, Instant lastSyncAt) {
        this.deviceId = deviceId;
        this.deviceUuid = deviceUuid;
        this.deviceName = deviceName;
        this.networkType = networkType;
        this.connectionType = connectionType;
        this.isNetworkAvailable = isNetworkAvailable;
        this.isInternetAvailable = isInternetAvailable;
        this.signalLevel = signalLevel;
        this.signalDbm = signalDbm;
        this.quality = quality;
        this.ssid = ssid;
        this.ipAddress = ipAddress;
        this.lastSyncAt = lastSyncAt;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
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

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Instant lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }
}
