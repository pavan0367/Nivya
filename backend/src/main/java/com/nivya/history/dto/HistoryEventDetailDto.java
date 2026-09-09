package com.nivya.history.dto;

import com.nivya.history.entity.HistoryEvent;
import java.time.Instant;

/**
 * Detailed history event view accessible within consented scope.
 */
public class HistoryEventDetailDto {

    private Long id;
    private Long deviceId;
    private String deviceName;
    private String packageName;
    private String appName;
    private String broadActivity;
    private String activityLabel;
    private String category;
    private Integer durationSeconds;
    private String durationFormatted;
    private String details;
    private Instant eventTimestamp;
    private Instant recordedAt;

    public HistoryEventDetailDto() {
    }

    public static HistoryEventDetailDto fromEntity(HistoryEvent entity) {
        if (entity == null) {
            return null;
        }
        HistoryEventDetailDto dto = new HistoryEventDetailDto();
        dto.setId(entity.getId());
        if (entity.getDevice() != null) {
            dto.setDeviceId(entity.getDevice().getId());
            dto.setDeviceName(entity.getDevice().getDeviceName());
        }
        dto.setPackageName(entity.getPackageName());
        dto.setAppName(entity.getAppName());
        dto.setBroadActivity(entity.getBroadActivity());
        dto.setActivityLabel(entity.getActivityLabel());
        dto.setCategory(entity.getCategory());
        dto.setDurationSeconds(entity.getDurationSeconds());
        dto.setDurationFormatted(formatDuration(entity.getDurationSeconds()));
        dto.setDetails(entity.getDetails());
        dto.setEventTimestamp(entity.getEventTimestamp());
        dto.setRecordedAt(entity.getCreatedAt());
        return dto;
    }

    private static String formatDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return "< 1m";
        }
        long minutes = seconds / 60;
        if (minutes < 1) {
            return "< 1m";
        }
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = minutes / 60;
        long remainingMins = minutes % 60;
        if (remainingMins == 0) {
            return hours + "h";
        }
        return hours + "h " + remainingMins + "m";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
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

    public String getDurationFormatted() {
        return durationFormatted;
    }

    public void setDurationFormatted(String durationFormatted) {
        this.durationFormatted = durationFormatted;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
