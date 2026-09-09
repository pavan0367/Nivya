package com.nivya.session.dto;

import com.nivya.session.entity.DeviceSession;
import java.time.Instant;

public class DeviceSessionDto {

    private Long id;
    private String deviceFingerprint;
    private String deviceName;
    private String platform;
    private String osVersion;
    private String appVersion;
    private String ipAddress;
    private String approximateLocation;
    private String status;
    private Instant loginAt;
    private Instant lastActivityAt;
    private Instant logoutAt;

    public DeviceSessionDto() {
    }

    public static DeviceSessionDto fromEntity(DeviceSession session) {
        DeviceSessionDto dto = new DeviceSessionDto();
        dto.setId(session.getId());
        dto.setDeviceFingerprint(session.getDeviceFingerprint());
        dto.setDeviceName(session.getDeviceName());
        dto.setPlatform(session.getPlatform());
        dto.setOsVersion(session.getOsVersion());
        dto.setAppVersion(session.getAppVersion());
        dto.setIpAddress(session.getIpAddress());
        dto.setApproximateLocation(session.getApproximateLocation());
        dto.setStatus(session.getStatus());
        dto.setLoginAt(session.getLoginAt());
        dto.setLastActivityAt(session.getLastActivityAt());
        dto.setLogoutAt(session.getLogoutAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getApproximateLocation() { return approximateLocation; }
    public void setApproximateLocation(String approximateLocation) { this.approximateLocation = approximateLocation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getLoginAt() { return loginAt; }
    public void setLoginAt(Instant loginAt) { this.loginAt = loginAt; }

    public Instant getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(Instant lastActivityAt) { this.lastActivityAt = lastActivityAt; }

    public Instant getLogoutAt() { return logoutAt; }
    public void setLogoutAt(Instant logoutAt) { this.logoutAt = logoutAt; }
}
