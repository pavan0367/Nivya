package com.nivya.location.entity;

import com.nivya.device.entity.Device;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity representing chronological location history breadcrumbs for consented monitoring.
 */
@Entity
@Table(name = "location_history")
public class LocationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "accuracy_meters")
    private Float accuracyMeters;

    @Column(name = "altitude_meters")
    private Double altitudeMeters;

    @Column(name = "speed_meters_per_sec")
    private Float speedMetersPerSec;

    @Column(nullable = false, length = 20)
    private String provider = "gps";

    @Column(name = "source_mode", nullable = false, length = 20)
    private String sourceMode = "FOREGROUND";

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LocationHistory() {
    }

    public LocationHistory(Device device, double latitude, double longitude, Float accuracyMeters,
                           Double altitudeMeters, Float speedMetersPerSec, String provider,
                           String sourceMode, Instant recordedAt) {
        this.device = device;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.altitudeMeters = altitudeMeters;
        this.speedMetersPerSec = speedMetersPerSec;
        this.provider = provider != null ? provider : "gps";
        this.sourceMode = sourceMode != null ? sourceMode : "FOREGROUND";
        this.recordedAt = recordedAt != null ? recordedAt : Instant.now();
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onPersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
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

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Float getAccuracyMeters() {
        return accuracyMeters;
    }

    public void setAccuracyMeters(Float accuracyMeters) {
        this.accuracyMeters = accuracyMeters;
    }

    public Double getAltitudeMeters() {
        return altitudeMeters;
    }

    public void setAltitudeMeters(Double altitudeMeters) {
        this.altitudeMeters = altitudeMeters;
    }

    public Float getSpeedMetersPerSec() {
        return speedMetersPerSec;
    }

    public void setSpeedMetersPerSec(Float speedMetersPerSec) {
        this.speedMetersPerSec = speedMetersPerSec;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSourceMode() {
        return sourceMode;
    }

    public void setSourceMode(String sourceMode) {
        this.sourceMode = sourceMode;
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
