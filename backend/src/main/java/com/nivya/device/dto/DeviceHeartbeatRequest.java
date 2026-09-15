package com.nivya.device.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceHeartbeatRequest {

    private Long deviceId;
    private String deviceUuid;
    private Integer batteryPct;
    private String networkType;
    private String networkQuality;
    private Boolean isOnline;

    public DeviceHeartbeatRequest() {
    }

    public DeviceHeartbeatRequest(Long deviceId, String deviceUuid, Integer batteryPct, String networkType, String networkQuality, Boolean isOnline) {
        this.deviceId = deviceId;
        this.deviceUuid = deviceUuid;
        this.batteryPct = batteryPct;
        this.networkType = networkType;
        this.networkQuality = networkQuality;
        this.isOnline = isOnline;
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

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
    }
}
