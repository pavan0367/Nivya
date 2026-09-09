package com.nivya.activity.dto;

import com.nivya.activity.entity.ActivityEvent;
import java.time.Duration;
import java.time.Instant;

/**
 * Representation of an activity event presented to Parent users.
 */
public class ActivityEventDto {

    private Long id;
    private Long deviceId;
    private String packageName;
    private String appName;
    private String broadActivity;
    private String category;
    private Integer durationSeconds;
    private String durationFormatted;
    private boolean isCurrent;
    private Instant startedAt;
    private Instant endedAt;
    private Instant createdAt;

    public ActivityEventDto() {
    }

    public static ActivityEventDto fromEntity(ActivityEvent entity) {
        if (entity == null) {
            return null;
        }
        ActivityEventDto dto = new ActivityEventDto();
        dto.setId(entity.getId());
        if (entity.getDevice() != null) {
            dto.setDeviceId(entity.getDevice().getId());
        }
        dto.setPackageName(entity.getPackageName());
        dto.setAppName(entity.getAppName());
        dto.setBroadActivity(entity.getBroadActivity());
        dto.setCategory(entity.getCategory());
        dto.setCurrent(entity.isCurrent());
        dto.setStartedAt(entity.getStartedAt());
        dto.setEndedAt(entity.getEndedAt());
        dto.setCreatedAt(entity.getCreatedAt());

        // Calculate or format duration
        Integer duration = entity.getDurationSeconds();
        if (duration == null && entity.getStartedAt() != null) {
            Instant end = entity.getEndedAt() != null ? entity.getEndedAt() : Instant.now();
            long secs = Duration.between(entity.getStartedAt(), end).getSeconds();
            duration = (int) Math.max(0, secs);
        }
        dto.setDurationSeconds(duration);
        dto.setDurationFormatted(formatDuration(duration));

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

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
