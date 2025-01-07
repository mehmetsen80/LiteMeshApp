package org.lite.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.MessageChannel;

@Configuration
public class MessagingConfig {
    
    @Bean
    public SimpMessagingTemplate simpMessagingTemplate(MessageChannel messageChannel) {
        return new SimpMessagingTemplate(messageChannel);
    }
} 