package org.lite.product.controller;

import org.lite.product.model.HealthStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

@RestController
@Slf4j
public class HealthController {

    @Value("${spring.application.name}")
    private String serviceId;

    private final Instant startTime = Instant.now();

    @GetMapping(
        path = "/health",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<HealthStatus> getHealth() {
        HealthStatus status = new HealthStatus();
        status.setServiceId(serviceId);
        
        // Get system metrics
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        // Calculate uptime
        Duration uptime = Duration.between(startTime, Instant.now());
        
        // Calculate memory usage
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsage = ((double) usedMemory / maxMemory) * 100;

        // Set health status details
        status.setStatus(isHealthy() ? "UP" : "DOWN");
        status.setUptime(formatUptime(uptime));
        status.setTimestamp(Instant.now());
        status.setMetrics(new HashMap<>());
        status.getMetrics().put("cpu", osBean.getSystemLoadAverage());
        status.getMetrics().put("memory", memoryUsage);
        status.getMetrics().put("responseTime", 0.0);

        return ResponseEntity.ok(status);
    }

    private boolean isHealthy() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            // Check memory usage
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            double memoryUsage = ((double) usedMemory / maxMemory) * 100;
            
            // Check CPU usage
            double cpuLoad = osBean.getSystemLoadAverage();
            
            // Define thresholds (could be made configurable)
            return memoryUsage < 90.0 && cpuLoad >= 0;
        } catch (Exception e) {
            log.error("Error checking service health: {}", e.getMessage());
            return false;
        }
    }

    private String formatUptime(Duration uptime) {
        long days = uptime.toDays();
        long hours = uptime.toHoursPart();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();

        return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
    }
} 