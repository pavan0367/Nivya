package com.nivya.activity.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * Inbound telemetry request for recording device activity.
 * Strict privacy enforcement: NO private messages, credentials, or audio recordings.
 */
public class LiveActivityRequest {

    @NotBlank(message = "Device UUID is required")
    private String deviceUuid;

    @NotBlank(message = "Package name is required")
    private String packageName;

    @NotBlank(message = "Application name is required")
    private String appName;

    @NotBlank(message = "Broad activity description is required")
    private String broadActivity;

    private String category = "GENERAL";

    private Integer durationSeconds;

    private Boolean isCurrent = true;

    private Instant startedAt;

    private Instant endedAt;

    public LiveActivityRequest() {
    }

    public LiveActivityRequest(String deviceUuid, String packageName, String appName,
                               String broadActivity, String category, Integer durationSeconds,
                               Boolean isCurrent, Instant startedAt, Instant endedAt) {
        this.deviceUuid = deviceUuid;
        this.packageName = packageName;
        this.appName = appName;
        this.broadActivity = broadActivity;
        this.category = category != null ? category : "GENERAL";
        this.durationSeconds = durationSeconds;
        this.isCurrent = isCurrent != null ? isCurrent : true;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
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

    public Boolean getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(Boolean current) {
        isCurrent = current;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
}
