package org.lite.gateway.handler;

import lombok.RequiredArgsConstructor;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@RequiredArgsConstructor
@Component
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

//        return serverRequest.bodyToMono(ApiRoute.class)
//                .flatMap(apiRoute -> {
//                    if(apiRoute.Id() != null){
//                        Mono<ApiRoute> apiRouteMono = routeService.getById(String.valueOf(apiRoute.Id()));
//                        return apiRouteMono.flatMap(route -> {
//                            if(route.Id() != null){
//                                //We already have this ApiRoute in the db
//                                return ServerResponse.status(HttpStatus.BAD_REQUEST)
//                                        .contentType(MediaType.TEXT_PLAIN)
//                                        .bodyValue("This ApiRoute already exists in the database!" + route);
//                            } else {
//                                //we don't have that record in db, create it below
//                                return ServerResponse.status(HttpStatus.OK)
//                                        .contentType(MediaType.APPLICATION_JSON)
//                                        .body(routeService.create(apiRoute), ApiRoute.class);
//                            }
//                        }).onErrorResume(e -> ServerResponse
//                                .badRequest()
//                                .contentType(MediaType.TEXT_PLAIN)
//                                .bodyValue("Invalid request body from database. Please provide valid ApiRoute data."));
//
//                    } else {
//                        return ServerResponse
//                                .badRequest()
//                                .contentType(MediaType.TEXT_PLAIN)
//                                .bodyValue("Invalid request body. Please provide valid ApiRoute data.");
//                    }
//                    // Process the ApiRoute object
//                    //return ServerResponse.ok().bodyValue("ApiRoute processed successfully");
//                })
//                .onErrorResume(e -> {
//                    // Handle deserialization error and return a 400 Bad Request
//                    return ServerResponse
//                            .badRequest()
//                            .contentType(MediaType.TEXT_PLAIN)
//                            .bodyValue("Invalid request body. Please provide valid ApiRoute data.");
//                });
//
//
//
////        Mono<ApiRoute> apiRouteMono = serverRequest.bodyToMono(ApiRoute.class)
////                .onErrorResume(e -> {
////                    // Handle the error and return a fallback or custom error response
////                    return Mono.error(new RuntimeException("Invalid request body!"));
////                });
////
////        apiRouteMono.subscribe(apiRoute -> {
////            if(apiRoute.Id() != null){
////                mono.set(routeService.getById(String.valueOf(apiRoute.Id())));
////            }
////        });
////
////        //This apiRoute does not exist, we can create it
////        if(mono.get() == null){
////            return apiRouteMono.flatMap(route ->
////                    ServerResponse.status(HttpStatus.OK)
////                            .contentType(MediaType.APPLICATION_JSON)
////                            .body(routeService.create(route), ApiRoute.class));
////        }
////
////
////        return ServerResponse.status(HttpStatus.BAD_REQUEST)
////                .contentType(MediaType.TEXT_PLAIN)
////                .bodyValue("The ApiRoute already exists!");

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
        Flux<ApiRoute> routeFlux = routeService.getAll();
        routeFlux.subscribe(f-> dynamicRouteService.addPath(f.path()));
        gatewayRoutesRefresher.refreshRoutes();
        return ServerResponse.ok().body(fromValue("Routes reloaded successfully"));
    }
}
