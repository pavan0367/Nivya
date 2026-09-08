package com.nivya.network.dto;

import java.time.Instant;

public class NetworkUpdateEvent {

    private Long deviceId;
    private String deviceUuid;
    private String networkType;
    private String connectionType;
    private Boolean isNetworkAvailable;
    private Boolean isInternetAvailable;
    private Integer signalLevel;
    private Integer signalDbm;
    private String quality;
    private Instant timestamp;

    public NetworkUpdateEvent() {
    }

    public NetworkUpdateEvent(Long deviceId, String deviceUuid, String networkType, String connectionType,
                              Boolean isNetworkAvailable, Boolean isInternetAvailable,
                              Integer signalLevel, Integer signalDbm, String quality, Instant timestamp) {
        this.deviceId = deviceId;
        this.deviceUuid = deviceUuid;
        this.networkType = networkType;
        this.connectionType = connectionType;
        this.isNetworkAvailable = isNetworkAvailable;
        this.isInternetAvailable = isInternetAvailable;
        this.signalLevel = signalLevel;
        this.signalDbm = signalDbm;
        this.quality = quality;
        this.timestamp = timestamp;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
