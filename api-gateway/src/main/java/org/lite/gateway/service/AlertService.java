package org.lite.gateway.service;

import org.lite.gateway.entity.Alert;
import org.lite.gateway.entity.AlertRule;
import org.lite.gateway.repository.AlertRepository;
import org.lite.gateway.model.AlertSeverity;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Service
@Slf4j
public class AlertService {
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;

    public AlertService(AlertRepository alertRepository, NotificationService notificationService) {
        this.alertRepository = alertRepository;
        this.notificationService = notificationService;
    }

    public Mono<Alert> createAlert(String routeId, AlertRule rule, Map<String, Double> metrics) {
        return alertRepository.findByRouteIdAndMetricAndConditionAndThreshold(
                routeId, rule.getMetric(), rule.getCondition(), rule.getThreshold())
            .next()
            .flatMap(existingAlert -> updateExistingAlert(existingAlert, metrics))
            .switchIfEmpty(Mono.defer(() -> createNewAlert(routeId, rule, metrics)));
    }

    private Mono<Alert> createNewAlert(String routeId, AlertRule rule, Map<String, Double> metrics) {
        Alert alert = Alert.fromRule(rule, routeId, metrics);
        return alertRepository.save(alert)
            .doOnSuccess(a -> log.info("Created new alert for route {} metric {}: {}", 
                routeId, rule.getMetric(), alert.getLastErrorMessage()));
    }

    private Mono<Alert> updateExistingAlert(Alert alert, Map<String, Double> metrics) {
        alert.setLastMetrics(metrics);
        alert.setLastUpdated(LocalDateTime.now());
        alert.setConsecutiveFailures(alert.getConsecutiveFailures() + 1);
        
        Double value = metrics.get(alert.getMetric());
        alert.setLastErrorMessage(String.format("%s usage (%.1f%%) exceeded threshold (%.1f%%)", 
            alert.getMetric(), value, alert.getThreshold()));

        return alertRepository.save(alert)
            .doOnSuccess(a -> log.info("Updated alert {}: {}", alert.getId(), alert.getLastErrorMessage()));
    }

    public Mono<Void> processHealthStatus(String routeId, boolean isHealthy, Map<String, Double> metrics, 
            int consecutiveFailures, List<AlertRule> rules) {
        if (isHealthy) {
            return resolveHealthAlerts(routeId);
        }

        // Send notification based on severity
        AlertSeverity severity = AlertSeverity.fromConsecutiveFailures(consecutiveFailures);
        String subject = severity == AlertSeverity.CRITICAL ? 
            "CRITICAL Service Alert" : "Service Health Alert";
        String message = String.format("%s: Service %s has failed %d times consecutively", 
            severity, routeId, consecutiveFailures);
        
        notificationService.sendEmailAlert(subject, message);
        notificationService.sendSlackNotification("danger", subject, message);

        return Flux.fromIterable(rules)
            .flatMap(rule -> {
                boolean healthAlert = consecutiveFailures >= rule.getThreshold();
                boolean metricAlert = shouldCreateAlert(rule, metrics);
                return Mono.just(healthAlert || metricAlert)
                    .filter(shouldAlert -> shouldAlert)
                    .flatMap(__ -> {
                        if (consecutiveFailures >= rule.getThreshold()) {
                            log.warn("Service {} has failed {} times consecutively, severity: {}", 
                                routeId, consecutiveFailures, severity);
                            return createHealthAlert(routeId, rule, consecutiveFailures);
                        } else {
                            log.warn("Service {} metric {} triggered alert: {} {} {}", 
                                routeId, rule.getMetric(), metrics.get(rule.getMetric()), 
                                rule.getCondition(), rule.getThreshold());
                            return createAlert(routeId, rule, metrics);
                        }
                    });
            })
            .then();
    }

    private boolean shouldCreateAlert(AlertRule rule, Map<String, Double> metrics) {
        if (metrics == null) return false;
        
        Double value = metrics.get(rule.getMetric());
        if (value == null) return false;

        return switch (rule.getCondition()) {
            case ">" -> value > rule.getThreshold();
            case ">=" -> value >= rule.getThreshold();
            case "<" -> value < rule.getThreshold();
            case "<=" -> value <= rule.getThreshold();
            case "=" -> Math.abs(value - rule.getThreshold()) < 0.0001;
            default -> false;
        };
    }

    public Flux<Alert> getActiveAlerts(String routeId) {
        return alertRepository.findByRouteIdAndActive(routeId, true);
    }

    public Mono<Alert> acknowledgeAlert(String alertId) {
        return alertRepository.findById(alertId)
            .flatMap(alert -> {
                alert.setActive(false);
                return alertRepository.save(alert);
            });
    }

    public Mono<Void> deleteAlert(String alertId) {
        return alertRepository.findById(alertId)
            .flatMap(alert -> alertRepository.deleteById(alertId))
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Alert not found: " + alertId)));
    }

    private Mono<Alert> createHealthAlert(String routeId, AlertRule rule, int consecutiveFailures) {
        AlertSeverity severity = AlertSeverity.fromConsecutiveFailures(consecutiveFailures);
        return alertRepository.findByRouteIdAndMetricAndConditionAndThreshold(
                routeId, "health", "=", 0.0)
            .next()
            .flatMap(existingAlert -> {
                existingAlert.setConsecutiveFailures(consecutiveFailures);
                existingAlert.setSeverity(severity.getLevel());
                existingAlert.setLastErrorMessage(String.format(
                    "Service is DOWN. Failed %d times consecutively. Severity: %s",
                    consecutiveFailures,
                    severity.getLevel()
                ));
                existingAlert.setLastUpdated(LocalDateTime.now());
                return alertRepository.save(existingAlert);
            })
            .switchIfEmpty(Mono.defer(() -> {
                Alert alert = new Alert();
                alert.setRouteId(routeId);
                alert.setMetric("health");
                alert.setCondition("=");
                alert.setThreshold(0.0);
                alert.setActive(true);
                alert.setSeverity(severity.getLevel());
                alert.setConsecutiveFailures(consecutiveFailures);
                alert.setLastErrorMessage(String.format(
                    "Service is DOWN. Failed %d times consecutively. Severity: %s",
                    consecutiveFailures,
                    severity.getLevel()
                ));
                alert.setCreatedAt(LocalDateTime.now());
                alert.setLastUpdated(LocalDateTime.now());
                return alertRepository.save(alert);
            }))
            .doOnSuccess(a -> log.info("Created/Updated health alert for route {}: {}", 
                routeId, a.getLastErrorMessage()));
    }

    public Mono<Void> resolveHealthAlerts(String routeId) {
        return alertRepository.findByRouteIdAndMetricAndActive(routeId, "health", true)
            .flatMap(alert -> {
                alert.setActive(false);
                alert.setLastErrorMessage("Service recovered");
                alert.setLastUpdated(LocalDateTime.now());
                alert.setConsecutiveFailures(0);
                return alertRepository.save(alert);
            })
            .then()
            .doOnSuccess(__ -> log.info("Resolved health alerts for service: {}", routeId));
    }
} 