package com.nivya.network.entity;

import com.nivya.device.entity.Device;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity representing chronological network telemetry points for timeline and connectivity tracking.
 */
@Entity
@Table(name = "network_history")
public class NetworkHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "network_type", nullable = false, length = 30)
    private String networkType;

    @Column(name = "connection_type", length = 50)
    private String connectionType;

    @Column(name = "is_network_available", nullable = false)
    private boolean isNetworkAvailable;

    @Column(name = "is_internet_available", nullable = false)
    private boolean isInternetAvailable;

    @Column(name = "signal_level")
    private Integer signalLevel;

    @Column(nullable = false, length = 20)
    private String quality;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public NetworkHistory() {
    }

    public NetworkHistory(Device device, String networkType, String connectionType,
                          boolean isNetworkAvailable, boolean isInternetAvailable,
                          Integer signalLevel, String quality, Instant recordedAt) {
        this.device = device;
        this.networkType = networkType;
        this.connectionType = connectionType;
        this.isNetworkAvailable = isNetworkAvailable;
        this.isInternetAvailable = isInternetAvailable;
        this.signalLevel = signalLevel;
        this.quality = quality;
        this.recordedAt = recordedAt != null ? recordedAt : Instant.now();
        this.createdAt = Instant.now();
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

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
