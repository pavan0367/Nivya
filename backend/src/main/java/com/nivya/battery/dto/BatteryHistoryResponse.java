package com.nivya.battery.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BatteryHistoryResponse {

    private Long deviceId;
    private String deviceUuid;
    private List<BatteryDataPoint> points = new ArrayList<>();

    public BatteryHistoryResponse() {
    }

    public BatteryHistoryResponse(Long deviceId, String deviceUuid, List<BatteryDataPoint> points) {
        this.deviceId = deviceId;
        this.deviceUuid = deviceUuid;
        this.points = points != null ? points : new ArrayList<>();
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

    public List<BatteryDataPoint> getPoints() {
        return points;
    }

    public void setPoints(List<BatteryDataPoint> points) {
        this.points = points;
    }

    public static class BatteryDataPoint {
        private Integer batteryPct;
        private String chargingState;
        private String batteryState;
        private Double temperatureCelsius;
        private Instant recordedAt;

        public BatteryDataPoint() {
        }

        public BatteryDataPoint(Integer batteryPct, String chargingState, String batteryState,
                                Double temperatureCelsius, Instant recordedAt) {
            this.batteryPct = batteryPct;
            this.chargingState = chargingState;
            this.batteryState = batteryState;
            this.temperatureCelsius = temperatureCelsius;
            this.recordedAt = recordedAt;
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
}
