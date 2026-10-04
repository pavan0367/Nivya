package com.nivya.admin.dto;

import com.nivya.device.entity.Device;

import java.time.Duration;
import java.time.Instant;

/**
 * Administrative device projection.
 */
public class AdminDeviceDto {

    private Long id;
    private String deviceUuid;
    private String deviceName;
    private String platform;
    private String osVersion;
    private String appVersion;
    private String status;
    private Instant lastSeenAt;
    private boolean online;

    public AdminDeviceDto() {
    }

    public AdminDeviceDto(Long id, String deviceUuid, String deviceName, String platform,
                          String osVersion, String appVersion, String status,
                          Instant lastSeenAt, boolean online) {
        this.id = id;
        this.deviceUuid = deviceUuid;
        this.deviceName = deviceName;
        this.platform = platform;
        this.osVersion = osVersion;
        this.appVersion = appVersion;
        this.status = status;
        this.lastSeenAt = lastSeenAt;
        this.online = online;
    }

    public static AdminDeviceDto fromEntity(Device device) {
        boolean isOnline = device.getLastSeenAt() != null &&
                Duration.between(device.getLastSeenAt(), Instant.now()).toMinutes() < 10;

        return new AdminDeviceDto(
                device.getId(),
                device.getDeviceUuid(),
                device.getDeviceName(),
                device.getPlatform(),
                device.getOsVersion(),
                device.getAppVersion(),
                device.getStatus(),
                device.getLastSeenAt(),
                isOnline
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
