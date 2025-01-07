package org.lite.gateway.entity;


import lombok.Getter;

@Getter
public enum AlertSeverity {
    INFO("Low priority", "#3498db"),
    WARNING("Medium priority", "#f1c40f"),
    CRITICAL("High priority", "#e74c3c");

    private final String label;
    private final String color;

    AlertSeverity(String label, String color) {
        this.label = label;
        this.color = color;
    }

}