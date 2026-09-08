package com.nivya.pairing.dto;

import java.time.Instant;

public class DeviceStatusDto {

    private Long deviceId;
    private String deviceUuid;
    private String deviceName;
    private String platform;
    private boolean isOnline;
    private Integer batteryPct;
    private String networkType;
    private String networkQuality;
    private Instant lastSyncAt;
    private Instant lastSeenAt;
    private boolean isStale;

    public DeviceStatusDto() {
    }

    public DeviceStatusDto(Long deviceId, String deviceUuid, String deviceName, String platform,
                           boolean isOnline, Integer batteryPct, String networkType,
                           String networkQuality, Instant lastSyncAt, Instant lastSeenAt, boolean isStale) {
        this.deviceId = deviceId;
        this.deviceUuid = deviceUuid;
        this.deviceName = deviceName;
        this.platform = platform;
        this.isOnline = isOnline;
        this.batteryPct = batteryPct;
        this.networkType = networkType;
        this.networkQuality = networkQuality;
        this.lastSyncAt = lastSyncAt;
        this.lastSeenAt = lastSeenAt;
        this.isStale = isStale;
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

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public Integer getBatteryPct() {
        return batteryPct;
    }

    public void setBatteryPct(Integer batteryPct) {
        this.batteryPct = batteryPct;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public String getNetworkQuality() {
        return networkQuality;
    }

    public void setNetworkQuality(String networkQuality) {
        this.networkQuality = networkQuality;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Instant lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public boolean isStale() {
        return isStale;
    }

    public void setStale(boolean stale) {
        isStale = stale;
    }
}
