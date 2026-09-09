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

    public RedisMessageSubscriber(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            RealtimeEvent event = objectMapper.readValue(json, RealtimeEvent.class);

            if (event.getDestinationTopic() != null && event.getPayloadJson() != null) {
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
