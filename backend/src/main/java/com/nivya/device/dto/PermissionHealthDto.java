package com.nivya.device.dto;

public class PermissionHealthDto {

    private String locationPermission;
    private String usagePermission;
    private String notificationPermission;
    private String batteryOptimization;
    private boolean allHealthy;

    public PermissionHealthDto() {
    }

    public PermissionHealthDto(String locationPermission, String usagePermission,
                               String notificationPermission, String batteryOptimization,
                               boolean allHealthy) {
        this.locationPermission = locationPermission;
        this.usagePermission = usagePermission;
        this.notificationPermission = notificationPermission;
        this.batteryOptimization = batteryOptimization;
        this.allHealthy = allHealthy;
    }

    public String getLocationPermission() {
        return locationPermission;
    }

    public void setLocationPermission(String locationPermission) {
        this.locationPermission = locationPermission;
    }

    public String getUsagePermission() {
        return usagePermission;
    }

    public void setUsagePermission(String usagePermission) {
        this.usagePermission = usagePermission;
    }

    public String getNotificationPermission() {
        return notificationPermission;
    }

    public void setNotificationPermission(String notificationPermission) {
        this.notificationPermission = notificationPermission;
    }

    public String getBatteryOptimization() {
        return batteryOptimization;
    }

    public void setBatteryOptimization(String batteryOptimization) {
        this.batteryOptimization = batteryOptimization;
    }

    public boolean isAllHealthy() {
        return allHealthy;
    }

    public void setAllHealthy(boolean allHealthy) {
        this.allHealthy = allHealthy;
    }
}
