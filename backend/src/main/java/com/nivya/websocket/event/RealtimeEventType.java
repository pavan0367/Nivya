package com.nivya.websocket.event;

/**
 * Enumeration of all supported real-time event types broadcast across the Nivya platform.
 */
public enum RealtimeEventType {
    BATTERY_UPDATE,
    NETWORK_UPDATE,
    LOCATION_UPDATE,
    DEVICE_STATUS_UPDATE,
    ALERT_GENERATED,
    PAIRING_EVENT,
    CONVOCATION_EVENT,
    ACTIVITY_UPDATE
}
