package org.lite.gateway.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.config.GatewayRoutesRefresher;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.service.DynamicRouteService;
import org.lite.gateway.service.RouteService;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@RequiredArgsConstructor
@Component
@Slf4j
public class ApiRouteHandler {
    private final RouteService routeService;

    private final DynamicRouteService dynamicRouteService;

    private final RouteLocator routeLocator;

    private final GatewayRoutesRefresher gatewayRoutesRefresher;

    public Mono<ServerResponse> create(ServerRequest serverRequest) {
        Mono<ApiRoute> apiRoute = serverRequest.bodyToMono(ApiRoute.class);
        return apiRoute.flatMap(route ->
                ServerResponse.status(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(routeService.create(route), ApiRoute.class));
    }

    public Mono<ServerResponse> getById(ServerRequest serverRequest) {
        final String apiId = serverRequest.pathVariable("routeId");
        Mono<ApiRoute> apiRoute = routeService.getById(apiId);
        return apiRoute.flatMap(route -> ServerResponse.ok()
                        .body(fromValue(route)))
                .switchIfEmpty(ServerResponse.notFound()
                        .build());
    }

    public Mono<ServerResponse> refreshRoutes(ServerRequest serverRequest) {
        return routeService.getAll()
                .collectList()
                .doOnSuccess(list -> {
                    list.forEach(apiRoute -> {
                        dynamicRouteService.addPath(apiRoute);
                        dynamicRouteService.addScope(apiRoute);
                        gatewayRoutesRefresher.refreshRoutes();
                        log.info("Refreshed Path: {}", apiRoute.getPath());

                    });
        }).then(ServerResponse.ok().body(fromValue("Routes reloaded successfully")));
    }
}
