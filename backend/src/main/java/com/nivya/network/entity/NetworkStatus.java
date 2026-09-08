package com.nivya.network.entity;

import com.nivya.device.entity.Device;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity representing the latest network connectivity telemetry snapshot for a device.
 */
@Entity
@Table(name = "network_status")
public class NetworkStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false, unique = true)
    private Device device;

    @Column(name = "network_type", nullable = false, length = 30)
    private String networkType = "NONE";

    @Column(name = "connection_type", length = 50)
    private String connectionType;

    @Column(name = "is_network_available", nullable = false)
    private boolean isNetworkAvailable = false;

    @Column(name = "is_internet_available", nullable = false)
    private boolean isInternetAvailable = false;

    @Column(name = "signal_level")
    private Integer signalLevel;

    @Column(name = "signal_dbm")
    private Integer signalDbm;

    @Column(nullable = false, length = 20)
    private String quality = "UNAVAILABLE";

    @Column(length = 100)
    private String ssid;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NetworkStatus() {
    }

    public NetworkStatus(Device device, String networkType, String connectionType,
                         boolean isNetworkAvailable, boolean isInternetAvailable,
                         Integer signalLevel, Integer signalDbm, String quality,
                         String ssid, String ipAddress) {
        this.device = device;
        this.networkType = networkType != null ? networkType : "NONE";
        this.connectionType = connectionType;
        this.isNetworkAvailable = isNetworkAvailable;
        this.isInternetAvailable = isInternetAvailable;
        this.signalLevel = signalLevel;
        this.signalDbm = signalDbm;
        this.quality = quality != null ? quality : "UNAVAILABLE";
        this.ssid = ssid;
        this.ipAddress = ipAddress;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
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

    public boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }

    public void setNetworkAvailable(boolean networkAvailable) {
        isNetworkAvailable = networkAvailable;
    }

    public boolean isInternetAvailable() {
        return isInternetAvailable;
    }

    public void setInternetAvailable(boolean internetAvailable) {
        isInternetAvailable = internetAvailable;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
