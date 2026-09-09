package com.nivya.websocket.dto;

import com.nivya.activity.dto.ActivityEventDto;
import com.nivya.battery.dto.BatteryStatusResponse;
import com.nivya.location.dto.LocationStatusResponse;
import com.nivya.network.dto.NetworkStatusResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.Instant;

/**
 * Consolidated telemetry snapshot DTO delivered to clients immediately upon reconnect.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceTelemetrySnapshotDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long deviceId;
    private String deviceName;
    private boolean online;
    private BatteryStatusResponse battery;
    private NetworkStatusResponse network;
    private LocationStatusResponse location;
    private ActivityEventDto currentActivity;
    private Instant lastSeenAt;
    private Instant snapshotTimestamp;

    public DeviceTelemetrySnapshotDto() {
        this.snapshotTimestamp = Instant.now();
    }

    public DeviceTelemetrySnapshotDto(Long deviceId,
                                      String deviceName,
                                      boolean online,
                                      BatteryStatusResponse battery,
                                      NetworkStatusResponse network,
                                      LocationStatusResponse location,
                                      ActivityEventDto currentActivity,
                                      Instant lastSeenAt) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.online = online;
        this.battery = battery;
        this.network = network;
        this.location = location;
        this.currentActivity = currentActivity;
        this.lastSeenAt = lastSeenAt;
        this.snapshotTimestamp = Instant.now();
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean getIsOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public void setIsOnline(boolean isOnline) {
        this.online = isOnline;
    }

    public BatteryStatusResponse getBattery() {
        return battery;
    }

    public void setBattery(BatteryStatusResponse battery) {
        this.battery = battery;
    }

    public NetworkStatusResponse getNetwork() {
        return network;
    }

    public void setNetwork(NetworkStatusResponse network) {
        this.network = network;
    }

    public LocationStatusResponse getLocation() {
        return location;
    }

    public void setLocation(LocationStatusResponse location) {
        this.location = location;
    }

    public ActivityEventDto getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(ActivityEventDto currentActivity) {
        this.currentActivity = currentActivity;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Instant getSnapshotTimestamp() {
        return snapshotTimestamp;
    }

    public void setSnapshotTimestamp(Instant snapshotTimestamp) {
        this.snapshotTimestamp = snapshotTimestamp;
    }
}
