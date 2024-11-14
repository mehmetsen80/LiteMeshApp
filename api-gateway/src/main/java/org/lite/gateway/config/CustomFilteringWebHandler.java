//package org.lite.gateway.config;
//
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.cloud.gateway.handler.FilteringWebHandler;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//import java.util.List;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//public class CustomFilteringWebHandler extends FilteringWebHandler {
//
//    public CustomFilteringWebHandler(List<GlobalFilter> filters) {
//        super(filters);
//    }
//
//    @Override
//    public Mono<Void> handle(ServerWebExchange exchange) {
//        log.info("Handling request in CustomFilteringWebHandler for URI: {}", exchange.getRequest().getURI());
//        return super.handle(exchange)
//                .doOnSuccess(unused -> log.info("Request handled successfully"))
//                .doOnError(error -> log.error("Error in FilteringWebHandler: {}", error.getMessage()));
//    }
//}
