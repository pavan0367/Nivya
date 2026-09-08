package com.nivya.battery.dto;

public class BatteryTrendResponse {

    private Long deviceId;
    private Integer currentBatteryPct;
    private String chargingState;
    private Double drainRatePctPerHour;
    private Double estimatedHoursRemaining;
    private Double averageTemperatureCelsius;

    public BatteryTrendResponse() {
    }

    public BatteryTrendResponse(Long deviceId, Integer currentBatteryPct, String chargingState,
                                Double drainRatePctPerHour, Double estimatedHoursRemaining,
                                Double averageTemperatureCelsius) {
        this.deviceId = deviceId;
        this.currentBatteryPct = currentBatteryPct;
        this.chargingState = chargingState;
        this.drainRatePctPerHour = drainRatePctPerHour;
        this.estimatedHoursRemaining = estimatedHoursRemaining;
        this.averageTemperatureCelsius = averageTemperatureCelsius;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getCurrentBatteryPct() {
        return currentBatteryPct;
    }

    public void setCurrentBatteryPct(Integer currentBatteryPct) {
        this.currentBatteryPct = currentBatteryPct;
    }

    public String getChargingState() {
        return chargingState;
    }

    public void setChargingState(String chargingState) {
        this.chargingState = chargingState;
    }

    public Double getDrainRatePctPerHour() {
        return drainRatePctPerHour;
    }

    public void setDrainRatePctPerHour(Double drainRatePctPerHour) {
        this.drainRatePctPerHour = drainRatePctPerHour;
    }

    public Double getEstimatedHoursRemaining() {
        return estimatedHoursRemaining;
    }

    public void setEstimatedHoursRemaining(Double estimatedHoursRemaining) {
        this.estimatedHoursRemaining = estimatedHoursRemaining;
    }

    public Double getAverageTemperatureCelsius() {
        return averageTemperatureCelsius;
    }

    public void setAverageTemperatureCelsius(Double averageTemperatureCelsius) {
        this.averageTemperatureCelsius = averageTemperatureCelsius;
    }
}
