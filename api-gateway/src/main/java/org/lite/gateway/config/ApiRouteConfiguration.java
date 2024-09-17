package org.lite.gateway.config;

import org.lite.gateway.handler.ApiRouteHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration
public class ApiRouteConfiguration {

    @Bean
    public RouterFunction<ServerResponse> route(ApiRouteHandler apiRouteHandler) {
        return RouterFunctions.route(POST("/routes")
                        .and(accept(MediaType.APPLICATION_JSON)), apiRouteHandler::create)
                .andRoute(GET("/routes/:routeId")
                        .and(accept(MediaType.APPLICATION_JSON)), apiRouteHandler::getById)
                .andRoute(GET("/routes/refresh-routes")
                        .and(accept(MediaType.APPLICATION_JSON)), apiRouteHandler::refreshRoutes);
    }
}
