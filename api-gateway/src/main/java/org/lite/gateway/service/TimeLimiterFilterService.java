package org.lite.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.lite.gateway.filter.TimeLimiterFilter;
import org.lite.gateway.model.TimeLimiterRecord;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;

import java.util.Objects;

@Slf4j
public class TimeLimiterFilterService implements FilterService{

    @Override
    public void applyFilter(GatewayFilterSpec gatewayFilterSpec, FilterConfig filter, ApiRoute apiRoute) {
        try {
            int timeoutDuration = Integer.parseInt(Objects.requireNonNull(filter.getArgs().get("timeoutDuration")));
            boolean cancelRunningFuture = Boolean.parseBoolean(Objects.requireNonNull(filter.getArgs().get("cancelRunningFuture")));

            TimeLimiterRecord timeLimiterRecord = new TimeLimiterRecord(apiRoute.getRouteIdentifier(), timeoutDuration, cancelRunningFuture);
            gatewayFilterSpec.filter(new TimeLimiterFilter(timeLimiterRecord));
        } catch (Exception e) {
            log.error("Error applying TimeLimiter filter for route {}: {}", apiRoute.getRouteIdentifier(), e.getMessage());
        }
    }
}
