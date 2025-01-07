package org.lite.gateway.service;

import org.lite.gateway.entity.ApiRoute;
import org.lite.gateway.entity.HealthThresholds;
import org.lite.gateway.repository.ApiRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Value;
import org.lite.gateway.model.ServiceHealthStatus;
import org.lite.gateway.model.TrendAnalysis;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.EurekaClient;

@Service
@Validated
@Slf4j
public class HealthCheckService {
    
    @Value("${server.port:7777}")
    private int serverPort;
    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;
    @Value("${eureka.instance.hostname:localhost}")
    private String hostname;

    private final ApiRouteRepository apiRouteRepository;
    private final WebClient.Builder webClientBuilder;
    private final MetricsAggregator metricsAggregator;
    private final AlertService alertService;
    private final EurekaClient eurekaClient;

    public HealthCheckService(
            ApiRouteRepository apiRouteRepository, 
            WebClient.Builder webClientBuilder,
            MetricsAggregator metricsAggregator,
            AlertService alertService,
            EurekaClient eurekaClient) {
        this.apiRouteRepository = apiRouteRepository;
        this.webClientBuilder = webClientBuilder;
        this.metricsAggregator = metricsAggregator;
        this.alertService = alertService;
        this.eurekaClient = eurekaClient;
    }

    public Flux<ApiRoute> getHealthCheckEnabledRoutes() {
        return apiRouteRepository.findAllWithHealthCheckEnabled();
    }
    
    public Mono<Boolean> isServiceHealthy(String routeId) {
        return apiRouteRepository.findByRouteIdentifier(routeId)
            .filter(route -> route.getHealthCheck().isEnabled())
            .flatMap(this::checkHealth)
            .map(healthData -> "UP".equals(healthData.getStatus()))
            .defaultIfEmpty(false);
    }

    public Mono<ServiceHealthStatus> getServiceStatus(String serviceId) {
        return apiRouteRepository.findByRouteIdentifier(serviceId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Service not found: " + serviceId)))
            .flatMap(this::checkHealth);
    }

    private Duration parseDuration(String uptimeStr) {
        try {
            // Format is "0d 0h 0m 46s"
            String[] parts = uptimeStr.split(" ");
            long days = Long.parseLong(parts[0].replace("d", ""));
            long hours = Long.parseLong(parts[1].replace("h", ""));
            long minutes = Long.parseLong(parts[2].replace("m", ""));
            long seconds = Long.parseLong(parts[3].replace("s", ""));
            
            return Duration.ofDays(days)
                .plusHours(hours)
                .plusMinutes(minutes)
                .plusSeconds(seconds);
        } catch (Exception e) {
            log.warn("Failed to parse uptime string: {}", uptimeStr);
            return Duration.ZERO;
        }
    }

    public boolean evaluateMetrics(Map<String, Double> metrics, HealthThresholds thresholds) {
        if (metrics == null) {
            return false;
        }

        Double cpu = metrics.get("cpu");
        Double memory = metrics.get("memory");
        Double responseTime = metrics.get("responseTime");

        return (cpu == null || cpu <= thresholds.getCpuThreshold()) &&
               (memory == null || memory <= thresholds.getMemoryThreshold()) &&
               (responseTime == null || responseTime <= thresholds.getResponseTimeThreshold());
    }

    private ServiceHealthStatus createServiceStatus(String routeId, Map<String, Object> healthData) {
        String serviceId = (String) healthData.get("serviceId");
        
        // Validate that the service IDs match
        if (!routeId.equals(serviceId)) {
            log.error("Service ID mismatch. Route ID: {}, Health Response Service ID: {}", routeId, serviceId);
            throw new RuntimeException("Service ID mismatch in health check response");
        }

        boolean isUp = "UP".equals(healthData.get("status"));
        
        ServiceHealthStatus status = ServiceHealthStatus.builder()
            .serviceId(serviceId)
            .healthy(isUp)
            .status((String) healthData.get("status"))
            .metrics(extractMetrics(healthData))
            .uptime(parseDuration((String) healthData.get("uptime")))
            .lastChecked(System.currentTimeMillis())
            .build();

        // Add new metrics to Redis
        if (status.getMetrics() != null) {
            metricsAggregator.addMetrics(routeId, status.getMetrics());
        }

        return status;
    }

    private Map<String, Double> extractMetrics(Map<String, Object> healthData) {
        Map<String, Double> metrics = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metricsData = (Map<String, Object>) healthData.get("metrics");
        
        if (metricsData != null) {
            metricsData.forEach((key, value) -> {
                if (value instanceof Number) {
                    metrics.put(key, ((Number) value).doubleValue());
                }
            });
        }
        
        return metrics;
    }

    public Map<String, TrendAnalysis> analyzeServiceTrends(String serviceId) {
        return metricsAggregator.analyzeTrends(serviceId); // Use MetricsAggregator to get trends from Redis
    }

    @SuppressWarnings("unchecked")
    private Mono<ServiceHealthStatus> checkHealth(ApiRoute route) {
        String serviceId = route.getRouteIdentifier();

        if (!isServiceRegistered(serviceId)) {
            log.debug("Service {} is not registered with Eureka, skipping health check", serviceId);
            return Mono.empty();
        }

        String healthEndpoint = getHealthEndpoint(route);
        long startTime = System.currentTimeMillis();
        ObjectMapper mapper = new ObjectMapper();
        
        return webClientBuilder.build()
            .get()
            .uri(healthEndpoint)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToMono(String.class)
            .map(responseBody -> {
                try {
                    Map<String, Object> healthData = mapper.readValue(responseBody, Map.class);
                    long responseTime = System.currentTimeMillis() - startTime;
                    
                    Map<String, Object> metrics = (Map<String, Object>) healthData.getOrDefault("metrics", new HashMap<>());
                    healthData.put("metrics", metrics);
                    metrics.put("responseTime", (double) responseTime);

                    ServiceHealthStatus status = createServiceStatus(serviceId, healthData);
                    processHealthStatus(route, status);
                    
                    return status;
                } catch (Exception e) {
                    log.error("Failed to parse health check response for service {}: {}", serviceId, e.getMessage());
                    throw new RuntimeException("Failed to parse health check response", e);
                }
            })
            .onErrorResume(e -> {
                log.error("Health check failed for service {}: {}", serviceId, e.getMessage());
                return handleHealthCheckError(serviceId, e);
            });
    }

    private void processHealthStatus(ApiRoute route, ServiceHealthStatus status) {
        String serviceId = route.getRouteIdentifier();
        
        // Analyze trends using Redis-stored metrics
        Map<String, TrendAnalysis> trends = analyzeServiceTrends(serviceId);
        
        // Check thresholds and trigger alerts
        if (!status.isHealthy() || !evaluateMetrics(status.getMetrics(), route.getHealthCheck().getThresholds())) {
            status.incrementConsecutiveFailures();
            alertService.processHealthStatus(
                serviceId,
                false,
                status.getMetrics(),
                status.getConsecutiveFailures(),
                route.getHealthCheck().getAlertRules()
            ).subscribe();
        } else {
            // Service is healthy, resolve any existing alerts
            status.resetConsecutiveFailures();
            alertService.resolveHealthAlerts(serviceId).subscribe();
        }
        
        // Store trends analysis in Redis if needed
        if (!trends.isEmpty()) {
            metricsAggregator.storeTrendAnalysis(serviceId, trends);
        }
    }

    private Mono<ServiceHealthStatus> handleHealthCheckError(String serviceId, Throwable error) {
        ServiceHealthStatus errorStatus = ServiceHealthStatus.builder()
            .healthy(false)
            .status("DOWN")
            .lastChecked(System.currentTimeMillis())
            .build();

        // Store error status in Redis
        metricsAggregator.addMetrics(serviceId, Map.of("error", 1.0));
        
        return Mono.just(errorStatus);
    }

    public Flux<ServiceHealthStatus> getAllServicesStatus() {
        return apiRouteRepository.findAllWithHealthCheckEnabled()
            .flatMap(route -> checkHealth(route)
                .onErrorResume(e -> {
                    log.error("Error checking health for service {}: {}", 
                        route.getRouteIdentifier(), e.getMessage());
                    return handleHealthCheckError(route.getRouteIdentifier(), e);
                })
            );
    }

    private String getHealthEndpoint(ApiRoute route) {
        String protocol = sslEnabled ? "https" : "http";
        String basePath = route.getPath().replaceAll("/\\*\\*", "");
        String healthPath = route.getHealthCheck().getEndpoint();
        return String.format("%s://%s:%d%s%s", 
            protocol, hostname, serverPort, basePath, healthPath);
    }

    // Add method to check if service is registered with Eureka
    private boolean isServiceRegistered(String serviceId) {
        try {
            com.netflix.discovery.shared.Application application = eurekaClient.getApplication(serviceId);
            return application != null && !application.getInstances().isEmpty();
        } catch (Exception e) {
            log.debug("Error checking service registration: {}", e.getMessage());
            return false;
        }
    }
} 