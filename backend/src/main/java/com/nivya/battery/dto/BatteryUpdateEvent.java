package com.nivya.battery.dto;

import java.time.Instant;

public class BatteryUpdateEvent {

    private String eventType = "BATTERY_UPDATE";
    private Long deviceId;
    private String deviceUuid;
    private Long familyId;
    private Integer batteryPct;
    private String chargingState;
    private boolean isLowBattery;
    private Instant timestamp;

    public BatteryUpdateEvent() {
    }

    public BatteryUpdateEvent(Long deviceId, String deviceUuid, Long familyId,
                              Integer batteryPct, String chargingState, boolean isLowBattery) {
        this.deviceId = deviceId;
        this.deviceUuid = deviceUuid;
        this.familyId = familyId;
        this.batteryPct = batteryPct;
        this.chargingState = chargingState;
        this.isLowBattery = isLowBattery;
        this.timestamp = Instant.now();
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
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

    public boolean isLowBattery() {
        return isLowBattery;
    }

    public void setLowBattery(boolean lowBattery) {
        isLowBattery = lowBattery;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
