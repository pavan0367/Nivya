package com.nivya.device.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Real-time and last-known hardware status for a device.
 */
@Entity
@Table(name = "device_status")
public class DeviceStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false, unique = true)
    private Device device;

    @Column(name = "is_online", nullable = false)
    private boolean isOnline = false;

    @Column(name = "battery_pct")
    private Integer batteryPct;

    @Column(name = "network_type", length = 30)
    private String networkType;

    @Column(name = "network_quality", length = 30)
    private String networkQuality;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    public DeviceStatus() {
    }

    public DeviceStatus(Device device, boolean isOnline, Integer batteryPct, String networkType, String networkQuality) {
        this.device = device;
        this.isOnline = isOnline;
        this.batteryPct = batteryPct;
        this.networkType = networkType;
        this.networkQuality = networkQuality;
        this.lastSyncAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    protected void onSync() {
        if (this.lastSyncAt == null) {
            this.lastSyncAt = Instant.now();
        }
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
}
