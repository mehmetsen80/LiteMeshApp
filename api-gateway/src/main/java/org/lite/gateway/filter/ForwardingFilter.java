//package org.lite.gateway.filter;
//import lombok.extern.slf4j.Slf4j;
//import org.lite.gateway.model.ForwardingFilterRecord;
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.core.Ordered;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//@Slf4j
//public class ForwardingFilter implements GatewayFilter, Ordered {
//
//    ForwardingFilterRecord forwardingFilterRecord;
//
//    public ForwardingFilter(ForwardingFilterRecord forwardingFilterRecord) {
//        this.forwardingFilterRecord = forwardingFilterRecord;
//    }
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        ServerHttpRequest request = exchange.getRequest().mutate()
//                .header("X-Forwarded-Proto", "https")
//                .header("X-Forwarded-For", forwardingFilterRecord.xForwardedFor())
//                .header("X-Forwarded-Host", forwardingFilterRecord.xForwardedHost())
//                .header(HttpHeaders.HOST, forwardingFilterRecord.host())
//                .build();
//        log.info("Forwarding request from external source as if it came from gateway");
//        return chain.filter(exchange.mutate().request(request).build());
//    }
//
//    @Override
//    public int getOrder() {
//        return Ordered.HIGHEST_PRECEDENCE;
//    }
//}
