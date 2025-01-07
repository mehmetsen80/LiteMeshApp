package org.lite.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.cors.CorsConfiguration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.reactive.socket.WebSocketSession;

@Configuration
@Slf4j
public class WebSocketConfig {

    private final Sinks.Many<String> messagesSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public MessageChannel messageChannel() {
        return new AbstractMessageChannel() {
            @Override
            protected boolean sendInternal(Message<?> message, long timeout) {
                try {
                    String jsonPayload = objectMapper.writeValueAsString(message.getPayload());
                    messagesSink.tryEmitNext(jsonPayload);
                    return true;
                } catch (Exception e) {
                    log.error("Error converting message to JSON", e);
                    return false;
                }
            }
        };
    }

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws-lite-mesh", stompWebSocketHandler());

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1);

        Map<String, CorsConfiguration> corsConfigMap = new HashMap<>();
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOriginPattern("*");
        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");
        corsConfig.setAllowCredentials(true);
        corsConfigMap.put("/ws-lite-mesh", corsConfig);
        mapping.setCorsConfigurations(corsConfigMap);

        return mapping;
    }

    @Bean
    public WebSocketHandler stompWebSocketHandler() {
        return session -> {
            // Add session to active sessions
            sessions.add(session);

            // Subscribe to messages from SimpMessagingTemplate
            Flux<WebSocketMessage> outbound = messagesSink.asFlux()
                .map(message -> {
                    // Properly format STOMP message with health data
                    String stompMessage = String.format(
                        "MESSAGE\n" +
                        "destination:/topic/health\n" +
                        "content-type:application/json\n" +
                        "subscription:sub-0\n\n" +
                        "%s\n\u0000",
                        message
                    );
                    log.debug("Sending message: {}", stompMessage);
                    return session.textMessage(stompMessage);
                });

            // Handle inbound messages
            Flux<WebSocketMessage> inbound = session.receive()
                .map(msg -> {
                    try {
                        String payload = msg.getPayloadAsText();
                        log.debug("Received STOMP message: {}", payload);

                        if (payload.startsWith("CONNECT")) {
                            return session.textMessage("CONNECTED\nversion:1.2\n\n\u0000");
                        } else if (payload.startsWith("SUBSCRIBE")) {
                            // The HealthCheckServiceController will handle updates through its handleSubscription method
                            return session.textMessage("RECEIPT\nreceipt-id:sub-0\n\n\u0000");
                        } else if (payload.startsWith("DISCONNECT")) {
                            sessions.remove(session);
                            return session.textMessage("RECEIPT\nreceipt-id:disconnect-0\n\n\u0000");
                        }

                        return session.textMessage("ERROR\nmessage:Unknown command\n\n\u0000");
                    } catch (Exception e) {
                        log.error("Error processing message: {}", e.getMessage());
                        return session.textMessage("ERROR\nmessage:" + e.getMessage() + "\n\n\u0000");
                    }
                });

            // When session closes, remove it from active sessions
            session.closeStatus().subscribe(status -> sessions.remove(session));

            // Merge both inbound and outbound streams
            return session.send(Flux.merge(inbound, outbound));
        };
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}