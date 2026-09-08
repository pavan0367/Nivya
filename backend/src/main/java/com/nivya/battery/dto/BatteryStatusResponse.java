package com.nivya.battery.dto;

import java.time.Instant;

public class BatteryStatusResponse {

    private Long deviceId;
    private String deviceUuid;
    private String deviceName;
    private Integer batteryPct;
    private String chargingState;
    private String batteryState;
    private String health;
    private Double temperatureCelsius;
    private boolean isLowBattery;
    private Instant updatedAt;

    public BatteryStatusResponse() {
    }

    public BatteryStatusResponse(Long deviceId, String deviceUuid, String deviceName, Integer batteryPct,
                                 String chargingState, String batteryState, String health,
                                 Double temperatureCelsius, boolean isLowBattery, Instant updatedAt) {
        this.deviceId = deviceId;
        this.deviceUuid = deviceUuid;
        this.deviceName = deviceName;
        this.batteryPct = batteryPct;
        this.chargingState = chargingState;
        this.batteryState = batteryState;
        this.health = health;
        this.temperatureCelsius = temperatureCelsius;
        this.isLowBattery = isLowBattery;
        this.updatedAt = updatedAt;
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

    public String getBatteryState() {
        return batteryState;
    }

    public void setBatteryState(String batteryState) {
        this.batteryState = batteryState;
    }

    public String getHealth() {
        return health;
    }

    public void setHealth(String health) {
        this.health = health;
    }

    public Double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(Double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public boolean isLowBattery() {
        return isLowBattery;
    }

    public void setLowBattery(boolean lowBattery) {
        isLowBattery = lowBattery;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
