package com.nivya.websocket.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nivya.websocket.event.RealtimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher that publishes RealtimeEvents onto Redis channel for multi-instance fanout,
 * with resilient local fallback to SimpMessagingTemplate when Redis is unreachable.
 */
@Component
public class RedisMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisMessagePublisher.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic realtimeTopic;
    private final SimpMessagingTemplate localMessagingTemplate;
    private final ObjectMapper objectMapper;
    private final String serverNodeId;

    public RedisMessagePublisher(RedisTemplate<String, Object> redisTemplate,
                                 ChannelTopic realtimeTopic,
                                 SimpMessagingTemplate localMessagingTemplate,
                                 String serverNodeId) {
        this.redisTemplate = redisTemplate;
        this.realtimeTopic = realtimeTopic;
        this.localMessagingTemplate = localMessagingTemplate;
        this.serverNodeId = serverNodeId;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void publish(RealtimeEvent event) {
        event.setOriginNodeId(serverNodeId);

        // 1. Immediately deliver to local STOMP subscribers on this node for instant 0ms real-time delivery
        try {
            Object payload = objectMapper.readValue(event.getPayloadJson(), Object.class);
            localMessagingTemplate.convertAndSend(event.getDestinationTopic(), payload);
            log.debug("Delivered event [{}] locally to {}", event.getEventType(), event.getDestinationTopic());
        } catch (Exception ex) {
            log.error("Failed local delivery for destination {}: {}", event.getDestinationTopic(), ex.getMessage());
        }

        // 2. Publish to Redis channel for multi-instance cluster fanout
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(realtimeTopic.getTopic(), json);
            log.debug("Published event [{}] to Redis topic {}", event.getEventType(), realtimeTopic.getTopic());
        } catch (Exception e) {
            log.warn("Redis unavailable or failed to publish event: {}", e.getMessage());
        }
    }
}
