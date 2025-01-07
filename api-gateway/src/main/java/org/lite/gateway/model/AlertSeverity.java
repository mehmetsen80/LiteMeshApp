package org.lite.gateway.model;

import lombok.Getter;

public enum AlertSeverity {
    CRITICAL("CRITICAL", 10),
    HIGH("HIGH", 5),
    MEDIUM("MEDIUM", 3),
    LOW("LOW", 1);

    @Getter
    private final String level;
    private final int threshold;

    AlertSeverity(String level, int threshold) {
        this.level = level;
        this.threshold = threshold;
    }

    public static AlertSeverity fromConsecutiveFailures(int failures) {
        if (failures >= CRITICAL.threshold) return CRITICAL;
        if (failures >= HIGH.threshold) return HIGH;
        if (failures >= MEDIUM.threshold) return MEDIUM;
        return LOW;
    }
} 