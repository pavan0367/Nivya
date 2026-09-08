package com.nivya.location.entity;

import com.nivya.device.entity.Device;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity representing the latest location telemetry snapshot for a device.
 */
@Entity
@Table(name = "location_status")
public class LocationStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false, unique = true)
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

    @Column(name = "bearing_degrees")
    private Float bearingDegrees;

    @Column(nullable = false, length = 20)
    private String provider = "gps";

    @Column(name = "is_gps_available", nullable = false)
    private boolean isGpsAvailable = true;

    @Column(name = "is_network_available", nullable = false)
    private boolean isNetworkAvailable = true;

    @Column(name = "permission_state", nullable = false, length = 30)
    private String permissionState = "GRANTED";

    @Column(name = "is_background_consented", nullable = false)
    private boolean isBackgroundConsented = false;

    @Column(name = "is_stale", nullable = false)
    private boolean isStale = false;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LocationStatus() {
    }

    public LocationStatus(Device device, double latitude, double longitude, Float accuracyMeters,
                          Double altitudeMeters, Float speedMetersPerSec, Float bearingDegrees,
                          String provider, boolean isGpsAvailable, boolean isNetworkAvailable,
                          String permissionState, boolean isBackgroundConsented, boolean isStale,
                          Instant recordedAt) {
        this.device = device;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.altitudeMeters = altitudeMeters;
        this.speedMetersPerSec = speedMetersPerSec;
        this.bearingDegrees = bearingDegrees;
        this.provider = provider != null ? provider : "gps";
        this.isGpsAvailable = isGpsAvailable;
        this.isNetworkAvailable = isNetworkAvailable;
        this.permissionState = permissionState != null ? permissionState : "GRANTED";
        this.isBackgroundConsented = isBackgroundConsented;
        this.isStale = isStale;
        this.recordedAt = recordedAt != null ? recordedAt : Instant.now();
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

    public Float getBearingDegrees() {
        return bearingDegrees;
    }

    public void setBearingDegrees(Float bearingDegrees) {
        this.bearingDegrees = bearingDegrees;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isGpsAvailable() {
        return isGpsAvailable;
    }

    public void setGpsAvailable(boolean gpsAvailable) {
        isGpsAvailable = gpsAvailable;
    }

    public boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }

    public void setNetworkAvailable(boolean networkAvailable) {
        isNetworkAvailable = networkAvailable;
    }

    public String getPermissionState() {
        return permissionState;
    }

    public void setPermissionState(String permissionState) {
        this.permissionState = permissionState;
    }

    public boolean isBackgroundConsented() {
        return isBackgroundConsented;
    }

    public void setBackgroundConsented(boolean backgroundConsented) {
        isBackgroundConsented = backgroundConsented;
    }

    public boolean isStale() {
        return isStale;
    }

    public void setStale(boolean stale) {
        isStale = stale;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
