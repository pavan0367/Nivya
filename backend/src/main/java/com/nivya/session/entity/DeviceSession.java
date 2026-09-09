package com.nivya.session.entity;

import com.nivya.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "authenticated_device_sessions")
public class DeviceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_fingerprint", nullable = false, length = 128)
    private String deviceFingerprint;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(length = 30)
    private String platform;

    @Column(name = "os_version", length = 50)
    private String osVersion;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "approximate_location", length = 150)
    private String approximateLocation;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "login_at", nullable = false)
    private Instant loginAt = Instant.now();

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt = Instant.now();

    @Column(name = "logout_at")
    private Instant logoutAt;

    public DeviceSession() {
    }

    public DeviceSession(User user, String deviceFingerprint, String deviceName, String platform,
                         String osVersion, String appVersion, String ipAddress, String approximateLocation) {
        this.user = user;
        this.deviceFingerprint = deviceFingerprint;
        this.deviceName = deviceName;
        this.platform = platform;
        this.osVersion = osVersion;
        this.appVersion = appVersion;
        this.ipAddress = ipAddress;
        this.approximateLocation = approximateLocation;
        this.status = "ACTIVE";
        this.loginAt = Instant.now();
        this.lastActivityAt = Instant.now();
    }

    public void updateActivity() {
        this.lastActivityAt = Instant.now();
    }

    public void markLoggedOut() {
        this.status = "LOGGED_OUT";
        this.logoutAt = Instant.now();
    }

    public void revoke() {
        this.status = "REVOKED";
        this.logoutAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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
