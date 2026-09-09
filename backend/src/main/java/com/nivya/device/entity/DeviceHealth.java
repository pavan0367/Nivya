package com.nivya.device.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity representing full hardware diagnostics, system resources,
 * connectivity, and permission health for enrolled devices.
 */
@Entity
@Table(name = "device_health")
public class DeviceHealth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false, unique = true)
    private Device device;

    @Column(name = "battery_pct")
    private Integer batteryPct;

    @Column(name = "charging_state", length = 30)
    private String chargingState;

    @Column(name = "battery_health", length = 30)
    private String batteryHealth;

    @Column(name = "battery_temp_celsius")
    private Double batteryTempCelsius;

    @Column(name = "storage_total_bytes")
    private Long storageTotalBytes;

    @Column(name = "storage_used_bytes")
    private Long storageUsedBytes;

    @Column(name = "storage_free_bytes")
    private Long storageFreeBytes;

    @Column(name = "ram_total_bytes")
    private Long ramTotalBytes;

    @Column(name = "ram_used_bytes")
    private Long ramUsedBytes;

    @Column(name = "ram_free_bytes")
    private Long ramFreeBytes;

    @Column(name = "is_low_ram", nullable = false)
    private boolean lowRam = false;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    @Column(name = "device_manufacturer", length = 100)
    private String deviceManufacturer;

    @Column(name = "os_version", length = 50)
    private String osVersion;

    @Column(name = "sdk_version")
    private Integer sdkVersion;

    @Column(name = "network_type", length = 30)
    private String networkType;

    @Column(name = "is_online", nullable = false)
    private boolean online = true;

    @Column(name = "location_permission", length = 30, nullable = false)
    private String locationPermission = "GRANTED";

    @Column(name = "usage_permission", length = 30, nullable = false)
    private String usagePermission = "GRANTED";

    @Column(name = "notification_permission", length = 30, nullable = false)
    private String notificationPermission = "GRANTED";

    @Column(name = "battery_optimization", length = 30, nullable = false)
    private String batteryOptimization = "OPTIMIZED";

    @Column(name = "all_permissions_healthy", nullable = false)
    private boolean allPermissionsHealthy = true;

    @Column(name = "health_score", nullable = false)
    private int healthScore = 100;

    @Column(name = "health_status", length = 30, nullable = false)
    private String healthStatus = "HEALTHY";

    @Column(name = "sync_state", length = 30, nullable = false)
    private String syncState = "SYNCED";

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DeviceHealth() {
    }

    public DeviceHealth(Device device, Instant recordedAt) {
        this.device = device;
        this.recordedAt = recordedAt != null ? recordedAt : Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
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

    public boolean isLowRam() {
        return lowRam;
    }

    public void setLowRam(boolean lowRam) {
        this.lowRam = lowRam;
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

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
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

    public boolean isAllPermissionsHealthy() {
        return allPermissionsHealthy;
    }

    public void setAllPermissionsHealthy(boolean allPermissionsHealthy) {
        this.allPermissionsHealthy = allPermissionsHealthy;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
