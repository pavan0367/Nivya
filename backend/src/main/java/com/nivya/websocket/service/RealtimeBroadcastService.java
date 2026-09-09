package com.nivya.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nivya.websocket.event.RealtimeEvent;
import com.nivya.websocket.event.RealtimeEventType;
import com.nivya.websocket.redis.RedisMessagePublisher;
import com.nivya.websocket.redis.TransientStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * High-level centralized broadcast service orchestrating real-time event publishing
 * across Redis Pub/Sub, transient Redis state caching, and STOMP destinations.
 */
@Service
public class RealtimeBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeBroadcastService.class);

    private final RedisMessagePublisher redisMessagePublisher;
    private final TransientStateStore transientStateStore;
    private final ObjectMapper objectMapper;

    public RealtimeBroadcastService(RedisMessagePublisher redisMessagePublisher,
                                    TransientStateStore transientStateStore) {
        this.redisMessagePublisher = redisMessagePublisher;
        this.transientStateStore = transientStateStore;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void broadcastBatteryUpdate(Long deviceId, Long familyId, Object batteryDto) {
        String topic = "/topic/battery/" + deviceId;
        publishEvent(RealtimeEventType.BATTERY_UPDATE, topic, familyId, deviceId, batteryDto);
    }

    public void broadcastNetworkUpdate(Long deviceId, Long familyId, Object networkDto, boolean isOnline) {
        // Update transient online status
        transientStateStore.setDeviceOnline(deviceId, isOnline);

        String topic = "/topic/network/" + deviceId;
        publishEvent(RealtimeEventType.NETWORK_UPDATE, topic, familyId, deviceId, networkDto);

        // Also broadcast device online/offline state change
        broadcastDeviceStatus(deviceId, familyId, isOnline, isOnline ? "CONNECTED" : "DISCONNECTED");
    }

    public void broadcastLocationUpdate(Long deviceId, Long familyId, Object locationDto) {
        String topic = "/topic/location/" + deviceId;
        publishEvent(RealtimeEventType.LOCATION_UPDATE, topic, familyId, deviceId, locationDto);
    }

    public void broadcastDeviceStatus(Long deviceId, Long familyId, boolean isOnline, String reason) {
        transientStateStore.setDeviceOnline(deviceId, isOnline);

        Map<String, Object> statusPayload = new HashMap<>();
        statusPayload.put("deviceId", deviceId);
        statusPayload.put("online", isOnline);
        statusPayload.put("reason", reason);
        statusPayload.put("timestamp", Instant.now().toString());

        String topic = "/topic/device/" + deviceId + "/status";
        publishEvent(RealtimeEventType.DEVICE_STATUS_UPDATE, topic, familyId, deviceId, statusPayload);
    }

    public void broadcastAlert(Long familyId, Long deviceId, Object alertDto) {
        String topic = "/topic/alerts/" + familyId;
        publishEvent(RealtimeEventType.ALERT_GENERATED, topic, familyId, deviceId, alertDto);
    }

    public void broadcastPairingEvent(Long familyId, Object pairingDto) {
        String topic = "/topic/pairing/" + familyId;
        publishEvent(RealtimeEventType.PAIRING_EVENT, topic, familyId, null, pairingDto);
    }

    public void broadcastConvocationMessage(Long familyId, Object messageDto) {
        String topic = "/topic/convocation/messages";
        publishEvent(RealtimeEventType.CONVOCATION_EVENT, topic, familyId, null, messageDto);
    }

    public void broadcastConvocationSeen(Long familyId, Long messageId, String seenAt) {
        Map<String, Object> seenPayload = new HashMap<>();
        seenPayload.put("messageId", messageId);
        seenPayload.put("seenAt", seenAt);

        String topic = "/topic/convocation/seen";
        publishEvent(RealtimeEventType.CONVOCATION_EVENT, topic, familyId, null, seenPayload);
    }

    public void broadcastConvocationSeen(Long familyId, java.util.List<Long> messageIds, String seenAt) {
        if (messageIds == null || messageIds.isEmpty()) return;
        for (Long id : messageIds) {
            broadcastConvocationSeen(familyId, id, seenAt);
        }
    }

    public void broadcastLiveActivity(Long deviceId, Long familyId, Object activityDto) {
        String topic = "/topic/activity/" + deviceId;
        publishEvent(RealtimeEventType.ACTIVITY_UPDATE, topic, familyId, deviceId, activityDto);
    }

    private void publishEvent(RealtimeEventType type, String topic, Long familyId, Long deviceId, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            RealtimeEvent event = new RealtimeEvent(type, topic, familyId, deviceId, json, null);
            redisMessagePublisher.publish(event);
        } catch (Exception e) {
            log.error("Failed to serialize and broadcast realtime event [{}]: {}", type, e.getMessage());
        }
    }
}
