package com.nivya.location.dto;

import java.time.Instant;

public class LocationStatusResponse {

    private Long deviceId;
    private String deviceUuid;
    private String deviceName;
    private double latitude;
    private double longitude;
    private Float accuracyMeters;
    private Double altitudeMeters;
    private Float speedMetersPerSec;
    private Float bearingDegrees;
    private String provider;
    private boolean isGpsAvailable;
    private boolean isNetworkAvailable;
    private String permissionState;
    private boolean isBackgroundConsented;
    private boolean isStale;
    private String staleDescription;
    private Instant recordedAt;
    private Instant updatedAt;
    private String lastUpdateAgo;

    public LocationStatusResponse() {
    }

    public LocationStatusResponse(Long deviceId, String deviceUuid, String deviceName,
                                  double latitude, double longitude, Float accuracyMeters,
                                  Double altitudeMeters, Float speedMetersPerSec, Float bearingDegrees,
                                  String provider, boolean isGpsAvailable, boolean isNetworkAvailable,
                                  String permissionState, boolean isBackgroundConsented,
                                  boolean isStale, String staleDescription, Instant recordedAt,
                                  Instant updatedAt, String lastUpdateAgo) {
        this.deviceId = deviceId;
        this.deviceUuid = deviceUuid;
        this.deviceName = deviceName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.altitudeMeters = altitudeMeters;
        this.speedMetersPerSec = speedMetersPerSec;
        this.bearingDegrees = bearingDegrees;
        this.provider = provider;
        this.isGpsAvailable = isGpsAvailable;
        this.isNetworkAvailable = isNetworkAvailable;
        this.permissionState = permissionState;
        this.isBackgroundConsented = isBackgroundConsented;
        this.isStale = isStale;
        this.staleDescription = staleDescription;
        this.recordedAt = recordedAt;
        this.updatedAt = updatedAt;
        this.lastUpdateAgo = lastUpdateAgo;
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

    public String getStaleDescription() {
        return staleDescription;
    }

    public void setStaleDescription(String staleDescription) {
        this.staleDescription = staleDescription;
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

    public String getLastUpdateAgo() {
        return lastUpdateAgo;
    }

    public void setLastUpdateAgo(String lastUpdateAgo) {
        this.lastUpdateAgo = lastUpdateAgo;
    }
}
