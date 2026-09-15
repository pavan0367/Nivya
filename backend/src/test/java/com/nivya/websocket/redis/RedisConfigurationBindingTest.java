package com.nivya.websocket.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;

import static org.junit.jupiter.api.Assertions.*;

class RedisConfigurationBindingTest {

    @Test
    @DisplayName("Verify Standalone Redis configuration with Upstash TLS parameters")
    void testUpstashStandaloneConfiguration() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("cheerful-salmon-86505.upstash.io");
        config.setPort(6379);
        config.setUsername("default");
        config.setPassword(RedisPassword.of("dummy_test_password"));
        config.setDatabase(0);

        assertEquals("cheerful-salmon-86505.upstash.io", config.getHostName());
        assertEquals(6379, config.getPort());
        assertEquals("default", config.getUsername());
        assertEquals(RedisPassword.of("dummy_test_password"), config.getPassword());
        assertEquals(0, config.getDatabase());
    }

    @Test
    @DisplayName("Verify LettuceClientConfiguration enables SSL when requested")
    void testLettuceSslConfiguration() {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .useSsl()
                .build();

        assertTrue(clientConfig.isUseSsl(), "LettuceClientConfiguration must have SSL enabled for Upstash");
    }

    @Test
    @DisplayName("Verify local non-SSL Standalone configuration")
    void testLocalStandaloneConfiguration() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(6379);
        config.setUsername("default");
        config.setPassword(RedisPassword.none());

        assertEquals("localhost", config.getHostName());
        assertEquals(6379, config.getPort());
        assertEquals(RedisPassword.none(), config.getPassword());

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder().build();
        assertFalse(clientConfig.isUseSsl(), "Local development must have SSL disabled by default");
    }

    @Test
    @DisplayName("Verify RedisProperties supports URL, Username, and SSL Enabled")
    void testRedisPropertiesBindingSupport() {
        RedisProperties properties = new RedisProperties();
        properties.setUrl("rediss://default:dummySecret@cheerful-salmon-86505.upstash.io:6379");
        properties.setHost("cheerful-salmon-86505.upstash.io");
        properties.setPort(6379);
        properties.setUsername("default");
        properties.setPassword("dummySecret");
        properties.getSsl().setEnabled(true);

        assertEquals("rediss://default:dummySecret@cheerful-salmon-86505.upstash.io:6379", properties.getUrl());
        assertEquals("cheerful-salmon-86505.upstash.io", properties.getHost());
        assertEquals(6379, properties.getPort());
        assertEquals("default", properties.getUsername());
        assertEquals("dummySecret", properties.getPassword());
        assertTrue(properties.getSsl().isEnabled());
    }
}
