package com.nivya.battery.entity;

import com.nivya.device.entity.Device;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA entity representing chronological battery telemetry logs for historical trend analysis.
 */
@Entity
@Table(name = "battery_history")
public class BatteryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "battery_pct", nullable = false)
    private Integer batteryPct;

    @Column(name = "charging_state", nullable = false, length = 30)
    private String chargingState;

    @Column(name = "battery_state", nullable = false, length = 30)
    private String batteryState;

    @Column(nullable = false, length = 30)
    private String health;

    @Column(name = "temperature_celsius")
    private Double temperatureCelsius;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public BatteryHistory() {
    }

    public BatteryHistory(Device device, Integer batteryPct, String chargingState, String batteryState,
                          String health, Double temperatureCelsius, Instant recordedAt) {
        this.device = device;
        this.batteryPct = batteryPct;
        this.chargingState = chargingState;
        this.batteryState = batteryState;
        this.health = health;
        this.temperatureCelsius = temperatureCelsius;
        this.recordedAt = recordedAt != null ? recordedAt : Instant.now();
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.recordedAt == null) {
            this.recordedAt = this.createdAt;
        }
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

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
