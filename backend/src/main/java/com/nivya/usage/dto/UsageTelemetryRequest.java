package com.nivya.usage.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UsageTelemetryRequest {

    @NotBlank(message = "Device UUID is required")
    private String deviceUuid;

    private LocalDate date;
    private Long totalForegroundSeconds;
    private Integer screenUnlocks;
    private List<AppUsageItemDto> apps = new ArrayList<>();

    public UsageTelemetryRequest() {
    }

    public UsageTelemetryRequest(String deviceUuid, LocalDate date, Long totalForegroundSeconds,
                                 Integer screenUnlocks, List<AppUsageItemDto> apps) {
        this.deviceUuid = deviceUuid;
        this.date = date;
        this.totalForegroundSeconds = totalForegroundSeconds;
        this.screenUnlocks = screenUnlocks;
        this.apps = apps != null ? apps : new ArrayList<>();
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getTotalForegroundSeconds() {
        return totalForegroundSeconds;
    }

    public void setTotalForegroundSeconds(Long totalForegroundSeconds) {
        this.totalForegroundSeconds = totalForegroundSeconds;
    }

    public Integer getScreenUnlocks() {
        return screenUnlocks;
    }

    public void setScreenUnlocks(Integer screenUnlocks) {
        this.screenUnlocks = screenUnlocks;
    }

    public List<AppUsageItemDto> getApps() {
        return apps;
    }

    public void setApps(List<AppUsageItemDto> apps) {
        this.apps = apps;
    }

    public static class AppUsageItemDto {
        private String packageName;
        private String appName;
        private String category;
        private Long foregroundSeconds;
        private Instant lastTimeUsed;

        public AppUsageItemDto() {
        }

        public AppUsageItemDto(String packageName, String appName, String category,
                               Long foregroundSeconds, Instant lastTimeUsed) {
            this.packageName = packageName;
            this.appName = appName;
            this.category = category;
            this.foregroundSeconds = foregroundSeconds;
            this.lastTimeUsed = lastTimeUsed;
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

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Long getForegroundSeconds() {
            return foregroundSeconds;
        }

        public void setForegroundSeconds(Long foregroundSeconds) {
            this.foregroundSeconds = foregroundSeconds;
        }

        public Instant getLastTimeUsed() {
            return lastTimeUsed;
        }

        public void setLastTimeUsed(Instant lastTimeUsed) {
            this.lastTimeUsed = lastTimeUsed;
        }
    }
}
