import React from 'react';
import './styles.css';

const ServiceStatusCard = ({ serviceHealth }) => {
    // If serviceHealth is null or undefined, create a default offline state
    const defaultHealth = {
        serviceId: serviceHealth?.serviceId || 'Unknown Service',
        status: 'DOWN',
        uptime: 'N/A',
        metrics: {
            cpu: 0,
            memory: 0,
            responseTime: 0
        },
        trends: {
            cpu: { direction: '⬇️', percentageChange: 0 },
            memory: { direction: '⬇️', percentageChange: 0 },
            responseTime: { direction: '⬇️', percentageChange: 0 }
        },
        lastChecked: null
    };

    // Use the actual service health data or the default offline state
    const health = serviceHealth || defaultHealth;

    // Extract metrics and trends safely
    const metrics = health.metrics || {};
    const trends = health.trends || {};
    const status = health.status || 'DOWN';
    const uptime = health.uptime;
    const lastChecked = health.lastChecked;

    const isCritical = metrics.cpu > 90 || metrics.memory > 0.9;
    const hasHighLatency = metrics.responseTime > 1000; // 1 second

    const formatUptime = (uptimeStr) => {
        if (!uptimeStr) return 'N/A';
        // Convert "PT6H32M36S" to "6h 32m 36s"
        return uptimeStr
            .replace('PT', '')
            .replace('H', 'h ')
            .replace('M', 'm ')
            .replace('S', 's');
    };

    const getStatusColor = (status) => {
        return status === 'UP' ? 'green' : 'red';
    };

    return (
        <div className={`service-card ${status === 'UP' ? '' : 'error'} ${isCritical ? 'critical' : ''} ${hasHighLatency ? 'high-latency' : ''}`}>
            <div className="service-header">
                <h3>{health.serviceId}</h3>
                <span className={`status-badge ${getStatusColor(status)}`}>
                    {status}
                </span>
            </div>
            <div className="metrics-grid">
                <MetricItem 
                    label="CPU" 
                    value={metrics.cpu != null ? `${metrics.cpu.toFixed(1)}%` : 'N/A'}
                    trend={trends.cpu || { direction: '⬇️', percentageChange: 0 }}
                />
                <MetricItem 
                    label="Memory" 
                    value={metrics.memory != null ? `${(metrics.memory * 100).toFixed(1)}%` : 'N/A'}
                    trend={trends.memory || { direction: '⬇️', percentageChange: 0 }}
                />
                <MetricItem 
                    label="Response Time" 
                    value={metrics.responseTime != null ? `${metrics.responseTime}ms` : 'N/A'}
                    trend={trends.responseTime || { direction: '⬇️', percentageChange: 0 }}
                />
            </div>
            <div className="service-footer">
                <span>Uptime: {formatUptime(uptime)}</span>
                <span>Last Updated: {lastChecked ? new Date(lastChecked).toLocaleTimeString() : 'N/A'}</span>
            </div>
        </div>
    );
};

const MetricItem = ({ label, value, trend }) => (
    <div className="metric-item">
        <span className="metric-label">{label}</span>
        <span className="metric-value">{value}</span>
        <span className="metric-trend">
            {trend.direction} ({trend.percentageChange > 0 ? '+' : ''}{trend.percentageChange.toFixed(1)}%)
        </span>
    </div>
);

export default ServiceStatusCard; 