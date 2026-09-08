package com.nivya.battery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class BatteryTelemetryRequest {

    @NotBlank(message = "Device UUID cannot be blank")
    private String deviceUuid;

    @NotNull(message = "Battery percentage is required")
    @Min(value = 0, message = "Battery percentage must be at least 0")
    @Max(value = 100, message = "Battery percentage cannot exceed 100")
    private Integer batteryPct;

    private String chargingState = "DISCHARGING";

    private String batteryState = "UNPLUGGED";

    private String health = "GOOD";

    private Double temperatureCelsius;

    private Instant recordedAt;

    public BatteryTelemetryRequest() {
    }

    public BatteryTelemetryRequest(String deviceUuid, Integer batteryPct, String chargingState,
                                   String batteryState, String health, Double temperatureCelsius, Instant recordedAt) {
        this.deviceUuid = deviceUuid;
        this.batteryPct = batteryPct;
        this.chargingState = chargingState;
        this.batteryState = batteryState;
        this.health = health;
        this.temperatureCelsius = temperatureCelsius;
        this.recordedAt = recordedAt != null ? recordedAt : Instant.now();
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
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

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
