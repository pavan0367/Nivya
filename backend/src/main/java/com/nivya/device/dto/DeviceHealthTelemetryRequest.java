package com.nivya.device.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * Ingest payload for periodic or event-driven device health telemetry.
 */
public class DeviceHealthTelemetryRequest {

    @NotBlank(message = "deviceUuid is required")
    private String deviceUuid;

    private String deviceModel;
    private String deviceManufacturer;
    private String osVersion;
    private Integer sdkVersion;

    private Integer batteryPct;
    private String chargingState;
    private String batteryHealth;
    private Double batteryTempCelsius;

    private Long storageTotalBytes;
    private Long storageUsedBytes;
    private Long storageFreeBytes;

    private Long ramTotalBytes;
    private Long ramUsedBytes;
    private Long ramFreeBytes;
    private Boolean isLowRam;

    private String networkType;
    private Boolean isOnline;

    private String locationPermission;
    private String usagePermission;
    private String notificationPermission;
    private String batteryOptimization;

    private String syncState;
    private Instant recordedAt;

    public DeviceHealthTelemetryRequest() {
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getDeviceManufacturer() {
        return deviceManufacturer;
    }

    public void setDeviceManufacturer(String deviceManufacturer) {
        this.deviceManufacturer = deviceManufacturer;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public Integer getSdkVersion() {
        return sdkVersion;
    }

    public void setSdkVersion(Integer sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    public Integer getBatteryPct() {
        return batteryPct;
    }

    public void setBatteryPct(Integer batteryPct) {
        this.batteryPct = batteryPct;
    }

    public String getChargingState() {
        return chargingState;
    }

    public void setChargingState(String chargingState) {
        this.chargingState = chargingState;
    }

    public String getBatteryHealth() {
        return batteryHealth;
    }

    public void setBatteryHealth(String batteryHealth) {
        this.batteryHealth = batteryHealth;
    }

    public Double getBatteryTempCelsius() {
        return batteryTempCelsius;
    }

    public void setBatteryTempCelsius(Double batteryTempCelsius) {
        this.batteryTempCelsius = batteryTempCelsius;
    }

    public Long getStorageTotalBytes() {
        return storageTotalBytes;
    }

    public void setStorageTotalBytes(Long storageTotalBytes) {
        this.storageTotalBytes = storageTotalBytes;
    }

    public Long getStorageUsedBytes() {
        return storageUsedBytes;
    }

    public void setStorageUsedBytes(Long storageUsedBytes) {
        this.storageUsedBytes = storageUsedBytes;
    }

    public Long getStorageFreeBytes() {
        return storageFreeBytes;
    }

    public void setStorageFreeBytes(Long storageFreeBytes) {
        this.storageFreeBytes = storageFreeBytes;
    }

    public Long getRamTotalBytes() {
        return ramTotalBytes;
    }

    public void setRamTotalBytes(Long ramTotalBytes) {
        this.ramTotalBytes = ramTotalBytes;
    }

    public Long getRamUsedBytes() {
        return ramUsedBytes;
    }

    public void setRamUsedBytes(Long ramUsedBytes) {
        this.ramUsedBytes = ramUsedBytes;
    }

    public Long getRamFreeBytes() {
        return ramFreeBytes;
    }

    public void setRamFreeBytes(Long ramFreeBytes) {
        this.ramFreeBytes = ramFreeBytes;
    }

    public Boolean getIsLowRam() {
        return isLowRam;
    }

    public void setIsLowRam(Boolean lowRam) {
        isLowRam = lowRam;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean online) {
        isOnline = online;
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

    public String getSyncState() {
        return syncState;
    }

    public void setSyncState(String syncState) {
        this.syncState = syncState;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
