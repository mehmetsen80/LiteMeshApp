package org.lite.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardUpdate {
    private String serviceId;
    private ServiceHealthStatus status;
    private Map<String, TrendAnalysis> trends;

    @Override
    public String toString() {
        return "DashboardUpdate{" +
                "serviceId='" + serviceId + '\'' +
                ", status=" + status +
                ", trends=" + trends +
                '}';
    }
}