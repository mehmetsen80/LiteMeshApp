package org.lite.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.AbstractMessageChannel;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.cors.CorsConfiguration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.reactive.socket.WebSocketSession;
import java.util.UUID;
import reactor.util.retry.Retry;
import java.time.Duration;

@Configuration
@Slf4j
public class WebSocketConfig {

    private final Sinks.Many<String> messagesSink = Sinks.many()
        .multicast()
        .onBackpressureBuffer(1024, false);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Bean
    public MessageChannel messageChannel(ObjectMapper objectMapper) {
        return new AbstractMessageChannel() {
            @Override
            protected boolean sendInternal(@NonNull Message<?> message, long timeout) {
                try {
                    String jsonPayload = objectMapper.writeValueAsString(message.getPayload());
                    Sinks.EmitResult result;
                    int attempts = 0;
                    do {
                        result = messagesSink.tryEmitNext(jsonPayload);
                        if (result.isFailure()) {
                            attempts++;
                            log.warn("Attempt {} failed to emit message. Reason: {}. Retrying...", 
                                attempts, result.name());
                            Thread.sleep(100); // Small delay before retry
                        }
                    } while (result.isFailure() && attempts < 3);
                    
                    if (result.isFailure()) {
                        log.error("Failed to emit message after {} attempts. Reason: {}. Payload: {}", 
                            attempts, result.name(), jsonPayload);
                        return false;
                    }
                    log.debug("Successfully emitted message after {} attempts", attempts + 1);
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
            String sessionId = session.getId();
            sessions.put(sessionId, session);
            log.info("WebSocket session connected: {}", sessionId);

            // Handle outbound messages
            Flux<WebSocketMessage> outbound = messagesSink.asFlux()
                .onBackpressureBuffer(256)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                    .maxBackoff(Duration.ofSeconds(1))
                    .doBeforeRetry(signal -> 
                        log.warn("Retrying message delivery for session {}, attempt {}", 
                            sessionId, signal.totalRetries() + 1)))
                .doOnNext(message -> log.debug("Processing outbound message for session {}: {}", 
                    sessionId, message))
                .map(message -> {
                    String stompFrame = createStompFrame(message);
                    log.debug("Created STOMP frame for session {}: {}", sessionId, stompFrame);
                    return session.textMessage(stompFrame);
                })
                .doOnSubscribe(sub -> 
                    log.info("Session {} subscribed to message stream", sessionId))
                .doOnCancel(() -> 
                    log.info("Session {} message stream cancelled", sessionId))
                .onErrorContinue((error, obj) -> {
                    log.error("Error in message processing for session {}: {}", 
                        sessionId, error.getMessage());
                })
                .doOnError(error -> log.error("Error in outbound stream for session {}: {}", 
                    sessionId, error.getMessage()));

            // Handle inbound messages
            Flux<WebSocketMessage> inbound = session.receive()
                .doOnNext(msg -> log.debug("Received message from session {}: {}", 
                    sessionId, msg.getPayloadAsText()))
                .map(msg -> handleInboundMessage(session, msg))
                .doOnError(error -> log.error("Error in inbound stream for session {}: {}", 
                    sessionId, error.getMessage()));

            // Clean up on session close
            session.closeStatus()
                .subscribe(status -> {
                    log.info("WebSocket session {} closed with status: {}", sessionId, status);
                    sessions.remove(sessionId);
                });

            return session.send(Flux.merge(inbound, outbound))
                .doOnError(error -> {
                    log.error("Error in session {}: {}", sessionId, error.getMessage());
                    sessions.remove(sessionId);
                });
        };
    }

    private String createStompFrame(String message) {
        log.debug("Creating STOMP frame for message: {}", message);
        return String.format(
            "MESSAGE\n" +
            "destination:/topic/health\n" +
            "content-type:application/json\n" +
            "subscription:sub-0\n" +
            "message-id:%s\n\n" +
            "%s\n\u0000",
            UUID.randomUUID().toString(),
            message
        );
    }

    private WebSocketMessage handleInboundMessage(WebSocketSession session, WebSocketMessage msg) {
        try {
            String payload = msg.getPayloadAsText();
            log.debug("Processing STOMP message: {}", payload);

            if (payload.startsWith("CONNECT")) {
                log.debug("Handling CONNECT frame for session: {}", session.getId());
                return session.textMessage(
                    "CONNECTED\n" +
                    "version:1.2\n" +
                    "heart-beat:0,0\n\n" +
                    "\u0000"
                );
            } else if (payload.startsWith("SUBSCRIBE")) {
                log.debug("Handling SUBSCRIBE frame for session: {}", session.getId());
                return session.textMessage(
                    "RECEIPT\n" +
                    "receipt-id:sub-0\n\n" +
                    "\u0000"
                );
            } else if (payload.startsWith("DISCONNECT")) {
                sessions.remove(session.getId());
                return session.textMessage(
                    "RECEIPT\n" +
                    "receipt-id:disconnect-0\n\n" +
                    "\u0000"
                );
            }

            return session.textMessage(
                "ERROR\n" +
                "message:Unknown command\n\n" +
                "\u0000"
            );
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());
            return session.textMessage(
                "ERROR\n" +
                "message:" + e.getMessage() + "\n\n" +
                "\u0000"
            );
        }
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}