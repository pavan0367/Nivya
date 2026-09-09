package com.nivya.activity.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Live Activity response object payload delivered exclusively to Parent accounts.
 */
public class LiveActivityResponse {

    private Long deviceId;
    private String deviceUuid;
    private String deviceName;
    private boolean isOnline;
    private ActivityEventDto currentActivity;
    private List<ActivityEventDto> recentActivities = new ArrayList<>();
    private Instant lastUpdatedAt;

    public LiveActivityResponse() {
    }

    public LiveActivityResponse(Long deviceId, String deviceUuid, String deviceName,
                                boolean isOnline, ActivityEventDto currentActivity,
                                List<ActivityEventDto> recentActivities, Instant lastUpdatedAt) {
        this.deviceId = deviceId;
        this.deviceUuid = deviceUuid;
        this.deviceName = deviceName;
        this.isOnline = isOnline;
        this.currentActivity = currentActivity;
        this.recentActivities = recentActivities != null ? recentActivities : new ArrayList<>();
        this.lastUpdatedAt = lastUpdatedAt;
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

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public ActivityEventDto getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(ActivityEventDto currentActivity) {
        this.currentActivity = currentActivity;
    }

    public List<ActivityEventDto> getRecentActivities() {
        return recentActivities;
    }

    public void setRecentActivities(List<ActivityEventDto> recentActivities) {
        this.recentActivities = recentActivities;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
