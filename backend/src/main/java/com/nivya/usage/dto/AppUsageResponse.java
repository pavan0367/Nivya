package com.nivya.usage.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class AppUsageResponse {

    private Long deviceId;
    private LocalDate date;
    private List<AppUsageDetailDto> apps;

    public AppUsageResponse() {
    }

    public AppUsageResponse(Long deviceId, LocalDate date, List<AppUsageDetailDto> apps) {
        this.deviceId = deviceId;
        this.date = date;
        this.apps = apps;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<AppUsageDetailDto> getApps() {
        return apps;
    }

    public void setApps(List<AppUsageDetailDto> apps) {
        this.apps = apps;
    }

    public static class AppUsageDetailDto {
        private String packageName;
        private String appName;
        private String category;
        private Long foregroundSeconds;
        private String formattedDuration;
        private Double percentageOfTotal;
        private Instant lastTimeUsed;

        public AppUsageDetailDto() {
        }

        public AppUsageDetailDto(String packageName, String appName, String category,
                                 Long foregroundSeconds, String formattedDuration,
                                 Double percentageOfTotal, Instant lastTimeUsed) {
            this.packageName = packageName;
            this.appName = appName;
            this.category = category;
            this.foregroundSeconds = foregroundSeconds;
            this.formattedDuration = formattedDuration;
            this.percentageOfTotal = percentageOfTotal;
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

        public String getFormattedDuration() {
            return formattedDuration;
        }

        public void setFormattedDuration(String formattedDuration) {
            this.formattedDuration = formattedDuration;
        }

        public Double getPercentageOfTotal() {
            return percentageOfTotal;
        }

        public void setPercentageOfTotal(Double percentageOfTotal) {
            this.percentageOfTotal = percentageOfTotal;
        }

        public Instant getLastTimeUsed() {
            return lastTimeUsed;
        }

        public void setLastTimeUsed(Instant lastTimeUsed) {
            this.lastTimeUsed = lastTimeUsed;
        }
    }
}
