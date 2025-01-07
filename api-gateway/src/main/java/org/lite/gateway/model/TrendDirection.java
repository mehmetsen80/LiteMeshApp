package org.lite.gateway.model;

import lombok.Getter;

@Getter
public enum TrendDirection {
    INCREASING("⬆️", "Increasing", "alert"),
    DECREASING("⬇️", "Decreasing", "warning"),
    STABLE("➡️", "Stable", "info");

    private final String symbol;
    private final String label;
    private final String severity;

    TrendDirection(String symbol, String label, String severity) {
        this.symbol = symbol;
        this.label = label;
        this.severity = severity;
    }

    public boolean isSignificant() {
        return this != STABLE;
    }

    public static TrendDirection fromPercentageChange(double change) {
        return change > 5 ? INCREASING :
               change < -5 ? DECREASING :
               STABLE;
    }

    @Override
    public String toString() {
        return String.format("%s %s", symbol, label);
    }
} 