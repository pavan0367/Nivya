package com.nivya.usage.dto;

import java.time.LocalDate;
import java.util.List;

public class UsageTrendResponse {

    private Long deviceId;
    private List<DailyPointDto> dailyPoints;
    private Long currentWeekTotalSeconds;
    private Long previousWeekTotalSeconds;
    private Double percentageChange;
    private String trendDescription;

    public UsageTrendResponse() {
    }

    public UsageTrendResponse(Long deviceId, List<DailyPointDto> dailyPoints,
                              Long currentWeekTotalSeconds, Long previousWeekTotalSeconds,
                              Double percentageChange, String trendDescription) {
        this.deviceId = deviceId;
        this.dailyPoints = dailyPoints;
        this.currentWeekTotalSeconds = currentWeekTotalSeconds;
        this.previousWeekTotalSeconds = previousWeekTotalSeconds;
        this.percentageChange = percentageChange;
        this.trendDescription = trendDescription;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public List<DailyPointDto> getDailyPoints() {
        return dailyPoints;
    }

    public void setDailyPoints(List<DailyPointDto> dailyPoints) {
        this.dailyPoints = dailyPoints;
    }

    public Long getCurrentWeekTotalSeconds() {
        return currentWeekTotalSeconds;
    }

    public void setCurrentWeekTotalSeconds(Long currentWeekTotalSeconds) {
        this.currentWeekTotalSeconds = currentWeekTotalSeconds;
    }

    public Long getPreviousWeekTotalSeconds() {
        return previousWeekTotalSeconds;
    }

    public void setPreviousWeekTotalSeconds(Long previousWeekTotalSeconds) {
        this.previousWeekTotalSeconds = previousWeekTotalSeconds;
    }

    public Double getPercentageChange() {
        return percentageChange;
    }

    public void setPercentageChange(Double percentageChange) {
        this.percentageChange = percentageChange;
    }

    public String getTrendDescription() {
        return trendDescription;
    }

    public void setTrendDescription(String trendDescription) {
        this.trendDescription = trendDescription;
    }

    public static class DailyPointDto {
        private LocalDate date;
        private Long totalSeconds;
        private String formattedDuration;
        private Long educationalSeconds;

        public DailyPointDto() {
        }

        public DailyPointDto(LocalDate date, Long totalSeconds, String formattedDuration, Long educationalSeconds) {
            this.date = date;
            this.totalSeconds = totalSeconds;
            this.formattedDuration = formattedDuration;
            this.educationalSeconds = educationalSeconds;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public Long getTotalSeconds() {
            return totalSeconds;
        }

        public void setTotalSeconds(Long totalSeconds) {
            this.totalSeconds = totalSeconds;
        }

        public String getFormattedDuration() {
            return formattedDuration;
        }

        public void setFormattedDuration(String formattedDuration) {
            this.formattedDuration = formattedDuration;
        }

        public Long getEducationalSeconds() {
            return educationalSeconds;
        }

        public void setEducationalSeconds(Long educationalSeconds) {
            this.educationalSeconds = educationalSeconds;
        }
    }
}
