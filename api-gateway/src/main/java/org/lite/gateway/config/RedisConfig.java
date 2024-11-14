package org.lite.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.listener.CustomMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

//    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
//
//    @Autowired
//    public RedisConfig(ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
//        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
//    }

    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        return new ReactiveStringRedisTemplate(connectionFactory);
    }

    // Define the Redis Pub/Sub topic for route updates
    @Bean
    public ChannelTopic routesTopic() {
        return new ChannelTopic("whitelistPathsTopic");
    }

    // Redis message listener container to handle Pub/Sub
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            MessageListenerAdapter messageListener,
            ChannelTopic routesTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListener, routesTopic);
        return container;
    }

    // Message listener adapter to bind the message handling method
    @Bean
    public MessageListenerAdapter messageListener(CustomMessageListener customMessageListener) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(customMessageListener, "handleMessage");
        adapter.setSerializer(new StringRedisSerializer());
        return adapter;
    }

    // Configure Redis connection factory using injected host and port
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Connecting to Redis at {}:{}", redisHost, redisPort);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisHost, redisPort);
        factory.afterPropertiesSet();
        return factory;
    }

    // Configure RedisTemplate with String serializer
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("Initializing RedisTemplate in RedisConfig");
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        log.info("Connecting to Redis at " + redisConnectionFactory.getConnection());
        return template;
    }
}
