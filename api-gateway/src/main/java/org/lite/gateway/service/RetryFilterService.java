package org.lite.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.lite.gateway.filter.RetryFilter;
import org.lite.gateway.model.RetryRecord;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;

import java.time.Duration;

@Slf4j
public class RetryFilterService implements FilterService{

    @Override
    public void applyFilter(GatewayFilterSpec gatewayFilterSpec, FilterConfig filter, ApiRoute apiRoute) {
        try {
            int maxAttempts = Integer.parseInt(filter.getArgs().get("maxAttempts"));
            Duration waitDuration = Duration.parse(filter.getArgs().get("waitDuration"));
            String retryExceptions = filter.getArgs().get("retryExceptions"); // Optional

            RetryRecord retryRecord = new RetryRecord(apiRoute.getRouteIdentifier(), maxAttempts, waitDuration, retryExceptions);
            gatewayFilterSpec.filter(new RetryFilter(retryRecord));
        } catch (Exception e) {
            log.error("Error applying Retry filter for route {}: {}", apiRoute.getRouteIdentifier(), e.getMessage());
        }
    }
}
