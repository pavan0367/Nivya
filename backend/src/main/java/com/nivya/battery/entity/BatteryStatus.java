package com.nivya.battery.entity;

import com.nivya.device.entity.Device;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity representing the latest battery telemetry snapshot for a device.
 */
@Entity
@Table(name = "battery_status")
public class BatteryStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false, unique = true)
    private Device device;

    @Column(name = "battery_pct", nullable = false)
    private Integer batteryPct;

    @Column(name = "charging_state", nullable = false, length = 30)
    private String chargingState = "DISCHARGING";

    @Column(name = "battery_state", nullable = false, length = 30)
    private String batteryState = "UNPLUGGED";

    @Column(nullable = false, length = 30)
    private String health = "GOOD";

    @Column(name = "temperature_celsius")
    private Double temperatureCelsius;

    @Column(name = "is_low_battery", nullable = false)
    private boolean isLowBattery = false;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BatteryStatus() {
    }

    public BatteryStatus(Device device, Integer batteryPct, String chargingState, String batteryState,
                         String health, Double temperatureCelsius, boolean isLowBattery) {
        this.device = device;
        this.batteryPct = batteryPct;
        this.chargingState = chargingState;
        this.batteryState = batteryState;
        this.health = health;
        this.temperatureCelsius = temperatureCelsius;
        this.isLowBattery = isLowBattery;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        this.updatedAt = Instant.now();
    }

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
