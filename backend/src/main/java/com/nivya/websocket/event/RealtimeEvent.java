package com.nivya.websocket.event;

import java.io.Serializable;
import java.time.Instant;

/**
 * Canonical distributed event envelope exchanged via Redis Pub/Sub and WebSocket STOMP.
 */
public class RealtimeEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private RealtimeEventType eventType;
    private String destinationTopic;
    private Long targetFamilyId;
    private Long targetDeviceId;
    private String payloadJson;
    private Instant timestamp;
    private String originNodeId;

    public RealtimeEvent() {
        this.timestamp = Instant.now();
    }

    public RealtimeEvent(RealtimeEventType eventType,
                         String destinationTopic,
                         Long targetFamilyId,
                         Long targetDeviceId,
                         String payloadJson,
                         String originNodeId) {
        this.eventType = eventType;
        this.destinationTopic = destinationTopic;
        this.targetFamilyId = targetFamilyId;
        this.targetDeviceId = targetDeviceId;
        this.payloadJson = payloadJson;
        this.timestamp = Instant.now();
        this.originNodeId = originNodeId;
    }

    public RealtimeEventType getEventType() {
        return eventType;
    }

    public void setEventType(RealtimeEventType eventType) {
        this.eventType = eventType;
    }

    public String getDestinationTopic() {
        return destinationTopic;
    }

    public void setDestinationTopic(String destinationTopic) {
        this.destinationTopic = destinationTopic;
    }

    public Long getTargetFamilyId() {
        return targetFamilyId;
    }

    public void setTargetFamilyId(Long targetFamilyId) {
        this.targetFamilyId = targetFamilyId;
    }

    public Long getTargetDeviceId() {
        return targetDeviceId;
    }

    public void setTargetDeviceId(Long targetDeviceId) {
        this.targetDeviceId = targetDeviceId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getOriginNodeId() {
        return originNodeId;
    }

    public void setOriginNodeId(String originNodeId) {
        this.originNodeId = originNodeId;
    }

    @Override
    public String toString() {
        return "RealtimeEvent{" +
                "eventType=" + eventType +
                ", destinationTopic='" + destinationTopic + '\'' +
                ", targetFamilyId=" + targetFamilyId +
                ", targetDeviceId=" + targetDeviceId +
                ", timestamp=" + timestamp +
                ", originNodeId='" + originNodeId + '\'' +
                '}';
    }
}
