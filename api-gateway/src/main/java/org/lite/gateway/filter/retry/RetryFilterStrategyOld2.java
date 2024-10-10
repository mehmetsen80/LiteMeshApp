package org.lite.gateway.filter.retry;

import lombok.extern.slf4j.Slf4j;
import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.FilterConfig;
import org.lite.gateway.filter.FilterStrategy;
import org.lite.gateway.model.RetryRecord;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;

import java.time.Duration;

@Slf4j
public class RetryFilterStrategyOld2 implements FilterStrategy {

    @Override
    public void apply(ApiRoute apiRoute, GatewayFilterSpec gatewayFilterSpec, FilterConfig filter) {

        // Extract Retry parameters from FilterConfig
        int maxAttempts = Integer.parseInt(filter.getArgs().get("maxAttempts"));
        Duration waitDuration = Duration.parse(filter.getArgs().get("waitDuration"));
        String retryExceptions = filter.getArgs().get("retryExceptions"); // Optional
        String fallbackUri = filter.getArgs().get("fallbackUri");
        String routeId = apiRoute.getRouteIdentifier();

        log.info("Configuring Retry for route: {}, maxAttempts: {}, waitDuration: {}, retryExceptions: {}",
                apiRoute.getRouteIdentifier(), maxAttempts, waitDuration, retryExceptions);

        CustomRetryResponseFilterOld customRetryResponseFilter = new CustomRetryResponseFilterOld(new RetryRecord(routeId, maxAttempts, waitDuration, retryExceptions, fallbackUri));
        gatewayFilterSpec.filter(customRetryResponseFilter);
    }
}
