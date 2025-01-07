package org.lite.gateway.model;

import lombok.Data;
import lombok.experimental.Accessors;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.Instant;

@Data
@Accessors(chain = true)
public class MetricAnalysis {
    private double mean;
    private double median;
    private double stdDev;
    private TrendAnalysis trend;
    private List<Double> outliers = new ArrayList<>();
    private double forecast;
    private Instant analysisTime;

    public MetricAnalysis() {
        this.analysisTime = Instant.now();
    }

    // Helper method to create an empty analysis
    public static MetricAnalysis empty() {
        return new MetricAnalysis()
            .setMean(0.0)
            .setMedian(0.0)
            .setStdDev(0.0)
            .setTrend(new TrendAnalysis(0.0, TrendDirection.STABLE))
            .setForecast(0.0);
    }

    // Get a summary of the analysis
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("mean", formatDouble(mean));
        summary.put("median", formatDouble(median));
        summary.put("standardDeviation", formatDouble(stdDev));
        summary.put("trend", trend.getDirection().toString());
        summary.put("percentageChange", formatDouble(trend.getPercentageChange()));
        summary.put("outliers", outliers.size());
        summary.put("forecast", formatDouble(forecast));
        summary.put("analysisTime", analysisTime);
        return summary;
    }

    // Check if the metric is showing concerning patterns
    public boolean isConcerning() {
        return trend.getDirection().isSignificant() || 
               !outliers.isEmpty() || 
               stdDev > mean * 0.5;  // High variability
    }

    // Get health status based on the analysis
    public String getHealthStatus() {
        if (outliers.size() > 2) return "CRITICAL";
        if (trend.getDirection() == TrendDirection.INCREASING && trend.getPercentageChange() > 20) return "WARNING";
        if (stdDev > mean) return "WARNING";
        return "HEALTHY";
    }

    // Format metrics for display
    public String getFormattedSummary() {
        return String.format("""
            Metric Analysis Summary:
            - Mean: %.2f
            - Median: %.2f
            - Standard Deviation: %.2f
            - Trend: %s (%.1f%%)
            - Outliers: %d found
            - Forecast: %.2f
            - Health: %s
            """,
            mean, median, stdDev, 
            trend.getDirection(), trend.getPercentageChange(),
            outliers.size(), forecast,
            getHealthStatus()
        );
    }

    // Helper method to format doubles
    private double formatDouble(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // Get recommendations based on the analysis
    public List<String> getRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        if (trend.getDirection() == TrendDirection.INCREASING && trend.getPercentageChange() > 20) {
            recommendations.add("Investigate rapid increase in metric values");
        }
        
        if (outliers.size() > 2) {
            recommendations.add("Multiple outliers detected - check for anomalies");
        }
        
        if (stdDev > mean * 0.5) {
            recommendations.add("High variability observed - consider stabilization measures");
        }
        
        if (forecast > mean * 1.5) {
            recommendations.add("Forecast suggests potential capacity issues");
        }
        
        return recommendations;
    }
} 