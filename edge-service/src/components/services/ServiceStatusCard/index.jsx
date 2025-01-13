import React from 'react';
import './styles.css';
import { Card, Tooltip, Zoom } from '@mui/material';

const ServiceStatusCard = ({ 
    service, 
    onAnalysisClick,
    ...props 
}) => {
    // If serviceHealth is null or undefined, create a default offline state
    const defaultHealth = {
        serviceId: service?.serviceId || 'Unknown Service',
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
    const health = service || defaultHealth;

    // Extract metrics and trends safely
    const serviceMetrics = health.metrics || {};
    const trends = health.trends || {};
    const status = health.status || 'DOWN';
    const uptime = health.uptime;
    const lastChecked = health.lastChecked;

    const isCritical = serviceMetrics.cpu > 90 || serviceMetrics.memory > 0.9;
    const hasHighLatency = serviceMetrics.responseTime > 1000; // 1 second

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
        <Card 
            sx={{ 
                minWidth: 275, 
                padding: '20px',
                backgroundColor: '#fff',
                borderRadius: '10px',
                height: '100%',
                cursor: 'pointer',
                transition: 'all 0.3s ease-in-out',
                '&:hover': {
                    boxShadow: 3,
                    transform: 'scale(1.02)',
                },
                '@keyframes pulseWarning': {
                    '0%': { boxShadow: '0 0 0 0 rgba(255, 152, 0, 0.4)' },
                    '70%': { boxShadow: '0 0 0 10px rgba(255, 152, 0, 0)' },
                    '100%': { boxShadow: '0 0 0 0 rgba(255, 152, 0, 0)' }
                },
                '@keyframes pulseCritical': {
                    '0%': { boxShadow: '0 0 0 0 rgba(244, 67, 54, 0.4)' },
                    '70%': { boxShadow: '0 0 0 10px rgba(244, 67, 54, 0)' },
                    '100%': { boxShadow: '0 0 0 0 rgba(244, 67, 54, 0)' }
                },
                '&.critical': {
                    animation: 'pulseCritical 2s infinite'
                },
                '&.high-latency': {
                    animation: 'pulseWarning 2s infinite'
                },
                '& .service-header': {
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: '15px'
                },
                '& .metrics-grid': {
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
                    gap: '15px',
                    marginBottom: '15px',
                    '& .metric-item': {
                        transition: 'all 0.3s ease-in-out',
                        '&:hover': {
                            transform: 'translateY(-2px)'
                        }
                    }
                },
                '& .service-footer': {
                    display: 'flex',
                    justifyContent: 'space-between',
                    fontSize: '0.875rem',
                    color: 'text.secondary'
                }
            }}
            className={`${isCritical ? 'critical' : ''} ${hasHighLatency ? 'high-latency' : ''}`}
            onClick={() => onAnalysisClick(service.serviceId)}
            {...props}
        >
            <div className="service-header">
                <h3>{health.serviceId}</h3>
                <span className={`status-badge ${getStatusColor(status)}`}>
                    {status}
                </span>
            </div>
            <div className="metrics-grid">
                <MetricItem 
                    label="CPU" 
                    value={serviceMetrics.cpu != null ? `${serviceMetrics.cpu.toFixed(1)}%` : 'N/A'}
                    trend={trends.cpu || { direction: '⬇️', percentageChange: 0 }}
                />
                <MetricItem 
                    label="Memory" 
                    value={serviceMetrics.memory != null ? `${(serviceMetrics.memory * 100).toFixed(1)}%` : 'N/A'}
                    trend={trends.memory || { direction: '⬇️', percentageChange: 0 }}
                />
                <MetricItem 
                    label="Response Time" 
                    value={serviceMetrics.responseTime != null ? `${serviceMetrics.responseTime}ms` : 'N/A'}
                    trend={trends.responseTime || { direction: '⬇️', percentageChange: 0 }}
                />
            </div>
            <div className="service-footer">
                <span>Uptime: {formatUptime(uptime)}</span>
                <span>Last Updated: {lastChecked ? new Date(lastChecked).toLocaleTimeString() : 'N/A'}</span>
            </div>
        </Card>
    );
};

const MetricItem = ({ label, value, trend }) => (
    <Tooltip 
        title={`Current ${label}: ${value}
Trend: ${trend.direction} (${trend.percentageChange > 0 ? '+' : ''}${trend.percentageChange.toFixed(1)}%)`}
        TransitionComponent={Zoom}
        arrow
    >
        <div className="metric-item">
            <span className="metric-label">{label}</span>
            <span className="metric-value">{value}</span>
            <span className={`metric-trend ${trend.percentageChange > 10 ? 'warning' : ''}`}>
                {trend.direction} ({trend.percentageChange > 0 ? '+' : ''}{trend.percentageChange.toFixed(1)}%)
            </span>
        </div>
    </Tooltip>
);

export default ServiceStatusCard; 