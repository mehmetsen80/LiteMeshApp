package org.lite.gateway.config;

import org.lite.gateway.listener.CustomMessageListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // Define the Redis Pub/Sub topic for route updates
    @Bean
    public ChannelTopic routesTopic() {
        return new ChannelTopic("whitelistPathsTopic");
    }

    // Redis message listener container to handle Pub/Sub
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter messageListener,
            ChannelTopic routesTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
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

    // Configure RedisTemplate with String serializer
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
