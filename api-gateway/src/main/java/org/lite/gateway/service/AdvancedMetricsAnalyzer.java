package org.lite.gateway.service;

import org.lite.gateway.model.MetricPoint;
import org.lite.gateway.model.TrendAnalysis;
import org.lite.gateway.model.TrendDirection;
import org.lite.gateway.model.MetricAnalysis;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AdvancedMetricsAnalyzer {
    
    public MetricAnalysis analyzeMetric(List<MetricPoint> points) {
        if (points.size() < 2) {
            return MetricAnalysis.empty();
        }

        double[] values = points.stream()
            .mapToDouble(MetricPoint::getValue)
            .toArray();

        return new MetricAnalysis()
            .setMean(calculateMean(values))
            .setMedian(calculateMedian(values))
            .setStdDev(calculateStdDev(values))
            .setTrend(calculateTrend(points))
            .setOutliers(detectOutliers(values))
            .setForecast(forecastNextValue(points));
    }

    private TrendAnalysis calculateTrend(List<MetricPoint> points) {
        if (points.size() < 2) {
            return new TrendAnalysis(0.0, TrendDirection.STABLE);
        }
        
        double recent = points.getLast().getValue();
        double previous = points.get(points.size() - 2).getValue();
        double percentageChange = ((recent - previous) / previous) * 100;
        
        TrendDirection direction =
            percentageChange > 5 ? TrendDirection.INCREASING :
            percentageChange < -5 ? TrendDirection.DECREASING :
            TrendDirection.STABLE;
            
        return new TrendAnalysis(percentageChange, direction);
    }

    private double calculateMean(double[] values) {
        return Arrays.stream(values).average().orElse(0.0);
    }

    private double calculateMedian(double[] values) {
        Arrays.sort(values);
        int middle = values.length / 2;
        if (values.length % 2 == 0) {
            return (values[middle-1] + values[middle]) / 2.0;
        }
        return values[middle];
    }

    private double calculateStdDev(double[] values) {
        double mean = calculateMean(values);
        double sum = Arrays.stream(values)
            .map(v -> Math.pow(v - mean, 2))
            .sum();
        return Math.sqrt(sum / values.length);
    }

    private List<Double> detectOutliers(double[] values) {
        double mean = calculateMean(values);
        double stdDev = calculateStdDev(values);
        double threshold = 2 * stdDev;

        return Arrays.stream(values)
            .filter(v -> Math.abs(v - mean) > threshold)
            .boxed()
            .toList();
    }

    private double forecastNextValue(List<MetricPoint> points) {
        // Simple exponential smoothing
        double alpha = 0.3;
        double forecast = points.getFirst().getValue();
        
        for (int i = 1; i < points.size(); i++) {
            forecast = alpha * points.get(i).getValue() + (1 - alpha) * forecast;
        }
        
        return forecast;
    }
} 