package com.nivya.history.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * Ingestion request payload for recording historical activity events from device sync workers.
 */
public class RecordHistoryRequest {

    @NotBlank(message = "Device UUID is required")
    private String deviceUuid;

    @NotBlank(message = "Package name is required")
    private String packageName;

    @NotBlank(message = "Application name is required")
    private String appName;

    @NotBlank(message = "Broad activity description is required")
    private String broadActivity;

    private String activityLabel;

    private String category = "GENERAL";

    private Integer durationSeconds;

    private Instant eventTimestamp;

    private String details;

    public RecordHistoryRequest() {
    }

    public RecordHistoryRequest(String deviceUuid, String packageName, String appName,
                                String broadActivity, String activityLabel, String category,
                                Integer durationSeconds, Instant eventTimestamp, String details) {
        this.deviceUuid = deviceUuid;
        this.packageName = packageName;
        this.appName = appName;
        this.broadActivity = broadActivity;
        this.activityLabel = activityLabel;
        this.category = category != null ? category : "GENERAL";
        this.durationSeconds = durationSeconds;
        this.eventTimestamp = eventTimestamp;
        this.details = details;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getBroadActivity() {
        return broadActivity;
    }

    public void setBroadActivity(String broadActivity) {
        this.broadActivity = broadActivity;
    }

    public String getActivityLabel() {
        return activityLabel;
    }

    public void setActivityLabel(String activityLabel) {
        this.activityLabel = activityLabel;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
