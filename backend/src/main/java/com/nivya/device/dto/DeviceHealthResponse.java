package com.nivya.device.dto;

import java.time.Instant;

public class DeviceHealthResponse {

    private Long deviceId;
    private String deviceUuid;
    private String deviceName;

    // Hardware & System
    private String deviceModel;
    private String deviceManufacturer;
    private String osVersion;
    private Integer sdkVersion;

    // Subsystem Health
    private StorageHealthDto storage;
    private MemoryHealthDto memory;

    // Battery & Power
    private Integer batteryPct;
    private String chargingState;
    private String batteryHealth;
    private Double batteryTempCelsius;

    // Connectivity & Sync
    private String networkType;
    private boolean isOnline;
    private String syncState;

    // Permission Diagnostics
    private PermissionHealthDto permissionHealth;

    // Computed Health Diagnostics
    private int healthScore;
    private String healthStatus;

    // Child-friendly summaries
    private String conditionSummary;
    private String storageSummary;
    private String batterySummary;
    private String protectionSummary;

    private Instant recordedAt;
    private Instant updatedAt;

    public DeviceHealthResponse() {
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

    public StorageHealthDto getStorage() {
        return storage;
    }

    public void setStorage(StorageHealthDto storage) {
        this.storage = storage;
    }

    public MemoryHealthDto getMemory() {
        return memory;
    }

    public void setMemory(MemoryHealthDto memory) {
        this.memory = memory;
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

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getSyncState() {
        return syncState;
    }

    public void setSyncState(String syncState) {
        this.syncState = syncState;
    }

    public PermissionHealthDto getPermissionHealth() {
        return permissionHealth;
    }

    public void setPermissionHealth(PermissionHealthDto permissionHealth) {
        this.permissionHealth = permissionHealth;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(int healthScore) {
        this.healthScore = healthScore;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getConditionSummary() {
        return conditionSummary;
    }

    public void setConditionSummary(String conditionSummary) {
        this.conditionSummary = conditionSummary;
    }

    public String getStorageSummary() {
        return storageSummary;
    }

    public void setStorageSummary(String storageSummary) {
        this.storageSummary = storageSummary;
    }

    public String getBatterySummary() {
        return batterySummary;
    }

    public void setBatterySummary(String batterySummary) {
        this.batterySummary = batterySummary;
    }

    public String getProtectionSummary() {
        return protectionSummary;
    }

    public void setProtectionSummary(String protectionSummary) {
        this.protectionSummary = protectionSummary;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
