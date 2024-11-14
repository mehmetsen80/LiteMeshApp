//package org.lite.gateway.service;
//
//import lombok.extern.slf4j.Slf4j;
//import org.lite.gateway.entity.ApiRoute;
//import org.lite.gateway.entity.FilterConfig;
//import org.lite.gateway.filter.ForwardingFilter;
//import org.lite.gateway.model.ForwardingFilterRecord;
//import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
//
//import java.util.Objects;
//
//@Slf4j
//public class ForwardingFilterService implements FilterService{
//    @Override
//    public void applyFilter(GatewayFilterSpec gatewayFilterSpec, FilterConfig filter, ApiRoute apiRoute) {
//        try {
//            String xForwardedFor = Objects.requireNonNull(filter.getArgs().get("xforwardedfor"));
//            String xForwardedHost = Objects.requireNonNull(filter.getArgs().get("xforwardedhost"));
//            String host = Objects.requireNonNull(filter.getArgs().get("host"));
//            ForwardingFilterRecord forwardingFilterRecord = new ForwardingFilterRecord(xForwardedFor, xForwardedHost, host);
//            log.info(forwardingFilterRecord.toString());
//            gatewayFilterSpec.filter(new ForwardingFilter(forwardingFilterRecord));
//        } catch (Exception e) {
//            log.error("Error applying Forwarding filter for route {}: {}", apiRoute.getRouteIdentifier(), e.getMessage());
//        }
//    }
//}
