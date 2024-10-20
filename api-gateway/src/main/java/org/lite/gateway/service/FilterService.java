package org.lite.gateway.service;

import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;

public interface FilterService {
    void applyFilter(GatewayFilterSpec gatewayFilterSpec, FilterConfig filter, ApiRoute apiRoute);
}

