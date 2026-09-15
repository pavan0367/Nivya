package com.nivya.websocket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nivya.websocket.redis.RedisMessageSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Configuration for Redis Pub/Sub distributed event streaming and transient state cache.
 */
@Configuration
public class RedisPubSubConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisPubSubConfig.class);

    public static final String REALTIME_CHANNEL = "nivya:realtime:events";

    private final String nodeId = UUID.randomUUID().toString().substring(0, 8);

    @Bean
    public String serverNodeId() {
        return nodeId;
    }

    @Bean
    public ChannelTopic realtimeTopic() {
        return new ChannelTopic(REALTIME_CHANNEL);
    }

    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationCustomizer(
            @Value("${spring.data.redis.ssl.enabled:false}") boolean sslEnabled,
            @Value("${spring.data.redis.url:}") String redisUrl) {
        return clientConfigurationBuilder -> {
            if (sslEnabled || (StringUtils.hasText(redisUrl) && redisUrl.startsWith("rediss://"))) {
                log.info("Configuring Lettuce client with TLS/SSL for secure Redis connection");
                clientConfigurationBuilder.useSsl();
            }
        };
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(mapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            RedisMessageSubscriber subscriber,
            ChannelTopic realtimeTopic) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer() {
            @Override
            public void start() {
                try {
                    super.start();
                } catch (Throwable t) {
                    log.warn("Redis pub/sub container could not connect at startup (in-memory fallback active): {}", t.getMessage());
                }
            }
        };
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, realtimeTopic);
        container.setErrorHandler(t -> log.warn("Redis pub/sub connection listener issue (graceful fallback active): {}", t.getMessage()));
        return container;
    }
}
