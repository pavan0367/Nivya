package com.nivya.history.dto;

import com.nivya.history.entity.HistoryEvent;
import java.time.Duration;
import java.time.Instant;

/**
 * Compact history timeline event DTO.
 */
public class HistoryEventDto {

    private Long id;
    private Long deviceId;
    private String packageName;
    private String appName;
    private String broadActivity;
    private String activityLabel;
    private String category;
    private Integer durationSeconds;
    private String durationFormatted;
    private Instant eventTimestamp;

    public HistoryEventDto() {
    }

    public static HistoryEventDto fromEntity(HistoryEvent entity) {
        if (entity == null) {
            return null;
        }
        HistoryEventDto dto = new HistoryEventDto();
        dto.setId(entity.getId());
        if (entity.getDevice() != null) {
            dto.setDeviceId(entity.getDevice().getId());
        }
        dto.setPackageName(entity.getPackageName());
        dto.setAppName(entity.getAppName());
        dto.setBroadActivity(entity.getBroadActivity());
        dto.setActivityLabel(entity.getActivityLabel());
        dto.setCategory(entity.getCategory());
        dto.setDurationSeconds(entity.getDurationSeconds());
        dto.setDurationFormatted(formatDuration(entity.getDurationSeconds()));
        dto.setEventTimestamp(entity.getEventTimestamp());
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

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }
}
