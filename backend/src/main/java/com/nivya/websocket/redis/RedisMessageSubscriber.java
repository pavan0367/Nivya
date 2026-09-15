package com.nivya.websocket.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nivya.websocket.event.RealtimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * MessageListener receiving distributed events via Redis Pub/Sub channel
 * and delivering them to local WebSocket STOMP subscribers.
 */
@Component
public class RedisMessageSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final String serverNodeId;

    public RedisMessageSubscriber(SimpMessagingTemplate messagingTemplate, String serverNodeId) {
        this.messagingTemplate = messagingTemplate;
        this.serverNodeId = serverNodeId;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String rawJson = new String(message.getBody(), StandardCharsets.UTF_8).trim();
            RealtimeEvent event;
            if (rawJson.startsWith("\"") && rawJson.endsWith("\"")) {
                String unescaped = objectMapper.readValue(rawJson, String.class);
                event = objectMapper.readValue(unescaped, RealtimeEvent.class);
            } else {
                event = objectMapper.readValue(rawJson, RealtimeEvent.class);
            }

            if (event.getDestinationTopic() != null && event.getPayloadJson() != null) {
                // Skip re-delivering event on the originating node where it was already delivered locally
                if (serverNodeId != null && serverNodeId.equals(event.getOriginNodeId())) {
                    log.debug("Skipping duplicate delivery of own event [{}] from node {}", event.getEventType(), serverNodeId);
                    return;
                }
                // Parse payload object or send raw JSON string
                Object payloadObject = objectMapper.readValue(event.getPayloadJson(), Object.class);
                messagingTemplate.convertAndSend(event.getDestinationTopic(), payloadObject);
                log.debug("Delivered distributed event [{}] to STOMP destination: {}",
                        event.getEventType(), event.getDestinationTopic());
            }
        } catch (Exception e) {
            log.warn("Failed to process Redis pub/sub message: {}", e.getMessage());
        }
    }
}
