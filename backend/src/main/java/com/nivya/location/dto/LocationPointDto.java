package com.nivya.location.dto;

import java.time.Instant;

public class LocationPointDto {

    private double latitude;
    private double longitude;
    private Float accuracyMeters;
    private String provider;
    private String sourceMode;
    private Instant recordedAt;

    public LocationPointDto() {
    }

    public LocationPointDto(double latitude, double longitude, Float accuracyMeters,
                            String provider, String sourceMode, Instant recordedAt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.provider = provider;
        this.sourceMode = sourceMode;
        this.recordedAt = recordedAt;
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
}
