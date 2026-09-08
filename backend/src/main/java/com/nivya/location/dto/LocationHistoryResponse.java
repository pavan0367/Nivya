package com.nivya.location.dto;

import java.util.ArrayList;
import java.util.List;

public class LocationHistoryResponse {

    private Long deviceId;
    private String deviceUuid;
    private List<LocationPointDto> points = new ArrayList<>();
    private int totalPoints;
    private boolean consentGranted;
    private String message;

    public LocationHistoryResponse() {
    }

    public LocationHistoryResponse(Long deviceId, String deviceUuid, List<LocationPointDto> points,
                                   int totalPoints, boolean consentGranted, String message) {
        this.deviceId = deviceId;
        this.deviceUuid = deviceUuid;
        this.points = points != null ? points : new ArrayList<>();
        this.totalPoints = totalPoints;
        this.consentGranted = consentGranted;
        this.message = message;
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

    public List<LocationPointDto> getPoints() {
        return points;
    }

    public void setPoints(List<LocationPointDto> points) {
        this.points = points;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public boolean isConsentGranted() {
        return consentGranted;
    }

    public void setConsentGranted(boolean consentGranted) {
        this.consentGranted = consentGranted;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
