//package org.lite.gateway.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.server.ServerWebExchange;
//import org.springframework.web.server.WebSession;
//import org.springframework.web.server.session.WebSessionManager;
//import reactor.core.publisher.Mono;
//
//@Configuration
//public class WebSessionConfig implements WebSessionManager {
//    @Override
//    public Mono<WebSession> getSession(ServerWebExchange exchange) {
//        return Mono.empty();  // Disable sessions
//    }
//}