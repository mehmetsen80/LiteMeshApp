package org.lite.gateway.filter;

import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;

public interface FilterStrategy {
    void apply(ApiRoute apiRoute, GatewayFilterSpec gatewayFilterSpec, FilterConfig filter);
}
