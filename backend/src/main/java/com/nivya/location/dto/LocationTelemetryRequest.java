package com.nivya.location.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class LocationTelemetryRequest {

    @NotBlank(message = "Device UUID is required")
    private String deviceUuid;

    private double latitude;
    private double longitude;
    private Float accuracyMeters;
    private Double altitudeMeters;
    private Float speedMetersPerSec;
    private Float bearingDegrees;
    private String provider = "gps";
    private Boolean isGpsAvailable = true;
    private Boolean isNetworkAvailable = true;
    private String permissionState = "GRANTED";
    private Boolean isBackgroundConsented = false;
    private String sourceMode = "FOREGROUND";
    private Instant recordedAt;

    public LocationTelemetryRequest() {
    }

    public LocationTelemetryRequest(String deviceUuid, double latitude, double longitude,
                                    Float accuracyMeters, Double altitudeMeters, Float speedMetersPerSec,
                                    Float bearingDegrees, String provider, Boolean isGpsAvailable,
                                    Boolean isNetworkAvailable, String permissionState,
                                    Boolean isBackgroundConsented, String sourceMode, Instant recordedAt) {
        this.deviceUuid = deviceUuid;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.altitudeMeters = altitudeMeters;
        this.speedMetersPerSec = speedMetersPerSec;
        this.bearingDegrees = bearingDegrees;
        this.provider = provider != null ? provider : "gps";
        this.isGpsAvailable = isGpsAvailable != null ? isGpsAvailable : true;
        this.isNetworkAvailable = isNetworkAvailable != null ? isNetworkAvailable : true;
        this.permissionState = permissionState != null ? permissionState : "GRANTED";
        this.isBackgroundConsented = isBackgroundConsented != null ? isBackgroundConsented : false;
        this.sourceMode = sourceMode != null ? sourceMode : "FOREGROUND";
        this.recordedAt = recordedAt;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
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

    public Boolean getIsGpsAvailable() {
        return isGpsAvailable;
    }

    public void setIsGpsAvailable(Boolean isGpsAvailable) {
        this.isGpsAvailable = isGpsAvailable;
    }

    public Boolean getIsNetworkAvailable() {
        return isNetworkAvailable;
    }

    public void setIsNetworkAvailable(Boolean isNetworkAvailable) {
        this.isNetworkAvailable = isNetworkAvailable;
    }

    public String getPermissionState() {
        return permissionState;
    }

    public void setPermissionState(String permissionState) {
        this.permissionState = permissionState;
    }

    public Boolean getIsBackgroundConsented() {
        return isBackgroundConsented;
    }

    public void setIsBackgroundConsented(Boolean isBackgroundConsented) {
        this.isBackgroundConsented = isBackgroundConsented;
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
}
