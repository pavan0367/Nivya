package com.nivya.usage.dto;

import java.time.LocalDate;
import java.util.Map;

public class UsageSummaryResponse {

    private Long deviceId;
    private String deviceUuid;
    private String deviceName;
    private LocalDate date;
    private Long totalForegroundSeconds;
    private String formattedTotalTime;
    private Long educationalSeconds;
    private Long recreationalSeconds;
    private Long socialSeconds;
    private Long productivitySeconds;
    private Integer screenUnlocks;
    private Map<String, Long> categoryBreakdown;

    public UsageSummaryResponse() {
    }

    public UsageSummaryResponse(Long deviceId, String deviceUuid, String deviceName,
                                LocalDate date, Long totalForegroundSeconds, String formattedTotalTime,
                                Long educationalSeconds, Long recreationalSeconds, Long socialSeconds,
                                Long productivitySeconds, Integer screenUnlocks, Map<String, Long> categoryBreakdown) {
        this.deviceId = deviceId;
        this.deviceUuid = deviceUuid;
        this.deviceName = deviceName;
        this.date = date;
        this.totalForegroundSeconds = totalForegroundSeconds;
        this.formattedTotalTime = formattedTotalTime;
        this.educationalSeconds = educationalSeconds;
        this.recreationalSeconds = recreationalSeconds;
        this.socialSeconds = socialSeconds;
        this.productivitySeconds = productivitySeconds;
        this.screenUnlocks = screenUnlocks;
        this.categoryBreakdown = categoryBreakdown;
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

    public String getFormattedTotalTime() {
        return formattedTotalTime;
    }

    public void setFormattedTotalTime(String formattedTotalTime) {
        this.formattedTotalTime = formattedTotalTime;
    }

    public Long getEducationalSeconds() {
        return educationalSeconds;
    }

    public void setEducationalSeconds(Long educationalSeconds) {
        this.educationalSeconds = educationalSeconds;
    }

    public Long getRecreationalSeconds() {
        return recreationalSeconds;
    }

    public void setRecreationalSeconds(Long recreationalSeconds) {
        this.recreationalSeconds = recreationalSeconds;
    }

    public Long getSocialSeconds() {
        return socialSeconds;
    }

    public void setSocialSeconds(Long socialSeconds) {
        this.socialSeconds = socialSeconds;
    }

    public Long getProductivitySeconds() {
        return productivitySeconds;
    }

    public void setProductivitySeconds(Long productivitySeconds) {
        this.productivitySeconds = productivitySeconds;
    }

    public Integer getScreenUnlocks() {
        return screenUnlocks;
    }

    public void setScreenUnlocks(Integer screenUnlocks) {
        this.screenUnlocks = screenUnlocks;
    }

    public Map<String, Long> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(Map<String, Long> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }
}
