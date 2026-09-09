package com.nivya.websocket.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages transient device state in Redis with automatic TTL expiration.
 * Provides in-memory fallback cache if Redis is temporarily offline or restarted.
 * Does NOT act as permanent source of truth (relational database holds historical data).
 */
@Component
public class TransientStateStore {

    private static final Logger log = LoggerFactory.getLogger(TransientStateStore.class);

    private static final String KEY_PREFIX_ONLINE = "device:transient:online:";
    private static final String KEY_PREFIX_SNAPSHOT = "device:transient:snapshot:";
    private static final Duration ONLINE_TTL = Duration.ofMinutes(10);
    private static final Duration SNAPSHOT_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Resilient local memory cache used when Redis is unavailable or restarted
    private final Map<Long, Boolean> localOnlineCache = new ConcurrentHashMap<>();
    private final Map<Long, String> localSnapshotCache = new ConcurrentHashMap<>();

    public TransientStateStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void setDeviceOnline(Long deviceId, boolean online) {
        if (deviceId == null) return;
        localOnlineCache.put(deviceId, online);

        try {
            String key = KEY_PREFIX_ONLINE + deviceId;
            redisTemplate.opsForValue().set(key, online, ONLINE_TTL);
        } catch (Exception e) {
            log.debug("Redis unavailable, stored device online status in local cache: {}", e.getMessage());
        }
    }

    public boolean isDeviceOnline(Long deviceId) {
        if (deviceId == null) return false;

        try {
            String key = KEY_PREFIX_ONLINE + deviceId;
            Object val = redisTemplate.opsForValue().get(key);
            if (val != null) {
                boolean online = Boolean.parseBoolean(val.toString());
                localOnlineCache.put(deviceId, online);
                return online;
            }
        } catch (Exception e) {
            log.debug("Redis unavailable, querying local online cache: {}", e.getMessage());
        }

        return localOnlineCache.getOrDefault(deviceId, false);
    }

    public void saveSnapshot(Long deviceId, Object snapshotDto) {
        if (deviceId == null || snapshotDto == null) return;

        try {
            String json = objectMapper.writeValueAsString(snapshotDto);
            localSnapshotCache.put(deviceId, json);
            String key = KEY_PREFIX_SNAPSHOT + deviceId;
            redisTemplate.opsForValue().set(key, json, SNAPSHOT_TTL);
        } catch (Exception e) {
            log.debug("Redis unavailable, stored snapshot in local cache: {}", e.getMessage());
        }
    }

    public <T> T getSnapshot(Long deviceId, Class<T> clazz) {
        if (deviceId == null) return null;

        try {
            String key = KEY_PREFIX_SNAPSHOT + deviceId;
            Object val = redisTemplate.opsForValue().get(key);
            if (val != null) {
                return objectMapper.readValue(val.toString(), clazz);
            }
        } catch (Exception e) {
            log.debug("Redis unavailable, querying local snapshot cache: {}", e.getMessage());
        }

        String localJson = localSnapshotCache.get(deviceId);
        if (localJson != null) {
            try {
                return objectMapper.readValue(localJson, clazz);
            } catch (Exception e) {
                log.warn("Failed to parse local cached snapshot: {}", e.getMessage());
            }
        }

        return null;
    }
}
