package org.lite.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.listener.CustomMessageListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfig {

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

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("redisConnectionFactory");
        //LettuceConnectionFactory factory = new LettuceConnectionFactory("redis-service", 6379); // Replace with actual host and port
        LettuceConnectionFactory factory = new LettuceConnectionFactory();
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
