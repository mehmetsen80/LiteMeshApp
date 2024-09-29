package org.lite.gateway.service.impl;

import lombok.RequiredArgsConstructor;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.service.RouteService;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.BooleanSpec;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ApiRouteLocatorImpl implements RouteLocator {
    private final RouteLocatorBuilder routeLocatorBuilder;
    private final RouteService routeService;

    @Override
    public Flux<Route> getRoutes() {
        RouteLocatorBuilder.Builder routesBuilder = routeLocatorBuilder.routes();
        return routeService.getAll()
                .map(apiRoute -> routesBuilder.route(String.valueOf(apiRoute.routeIdentifier()),
                        predicateSpec -> setPredicateSpec(apiRoute, predicateSpec)))
                .collectList()
                .flatMapMany(builders -> routesBuilder.build()
                        .getRoutes());
    }

    private Buildable<Route> setPredicateSpec(ApiRoute apiRoute, PredicateSpec predicateSpec) {
        BooleanSpec booleanSpec = predicateSpec.path(apiRoute.path());
        URI uri = URI.create(apiRoute.uri());
        String scheme = uri.getScheme();

        //Keep for future refernce
//        URI finalUri = UriComponentsBuilder.fromUri(uri).build().toUri();
//        if ("lb".equals(scheme)) {
//            finalUri = UriComponentsBuilder.fromUri(uri)
//                    .scheme("https")
//                    .build()
//                    .toUri();
//            booleanSpec.and().uri(finalUri);
//        }

        if (StringUtils.hasLength(apiRoute.method())) {
            booleanSpec.and()
                    .method(apiRoute.method());
        }
        return booleanSpec.uri(apiRoute.uri());
    }

    @Override
    public Flux<Route> getRoutesByMetadata(Map<String, Object> metadata) {
        return RouteLocator.super.getRoutesByMetadata(metadata);
    }
}
