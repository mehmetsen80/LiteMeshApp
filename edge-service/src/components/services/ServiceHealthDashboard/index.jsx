import React, { useState, useEffect } from 'react';
import { serviceHealthWebSocket } from '../../../services/serviceHealthService';
import { apiGatewayService } from '../../../services/apiGatewayService';
import ServiceStatusCard from '../ServiceStatusCard';
import { Alert } from '@mui/material';
import './styles.css';

const ServiceHealthDashboard = () => {
    const [services, setServices] = useState([]);
    const [expectedServices, setExpectedServices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [connectionStatus, setConnectionStatus] = useState('connecting');
    const [error, setError] = useState(null);

    // Fetch expected services from API Gateway
    useEffect(() => {
        const fetchServices = async () => {
            try {
                const services = await apiGatewayService.getAllServices();
                const serviceIds = services.map(service => service.serviceId);
                setExpectedServices(serviceIds);
            } catch (err) {
                console.error('Error fetching services:', err);
                setError('Failed to fetch services from API Gateway');
            }
        };
        
        fetchServices();
    }, []);

    // Function to ensure all expected services are shown
    const getAllServices = (activeServices) => {
        const servicesMap = new Map(activeServices.map(s => [s.serviceId, s]));
        
        // Add placeholder for missing services
        expectedServices.forEach(serviceId => {
            if (!servicesMap.has(serviceId)) {
                servicesMap.set(serviceId, {
                    serviceId: serviceId,
                    status: 'DOWN',
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
                    uptime: null,
                    lastChecked: null
                });
            }
        });
        
        // Transform active services to match expected structure
        return Array.from(servicesMap.values()).map(service => {
            if (typeof service.status === 'object') {
                // If status is an object, flatten the structure
                return {
                    serviceId: service.serviceId,
                    status: service.status.status,
                    metrics: service.status.metrics || {},
                    trends: service.trends || {},
                    uptime: service.status.uptime,
                    lastChecked: service.status.lastChecked
                };
            }
            return service; // Return as-is if already in correct format
        });
    };

    useEffect(() => {
        // Track connection status
        const handleConnectionChange = (status) => {
            setConnectionStatus(status);
            if (status === 'disconnected') {
                setError('Lost connection to server. Attempting to reconnect...');
            } else if (status === 'connected') {
                setError(null);
            }
        };

        // Subscribe to WebSocket updates
        const unsubscribeFromHealthUpdates = serviceHealthWebSocket.subscribe(data => {
            try {
                if (!Array.isArray(data)) {
                    throw new Error('Invalid data format received');
                }
                //console.log('Received service data:', JSON.stringify(data, null, 2));
                setServices(getAllServices(data));
                setError(null);
                setLoading(false);
            } catch (err) {
                setError('Failed to process service health data');
                console.error('Error processing health data:', err);
            }
        });

        // Subscribe to connection status updates
        serviceHealthWebSocket.onConnectionChange(handleConnectionChange);

        // Cleanup subscription on unmount
        return () => {
            unsubscribeFromHealthUpdates();
            serviceHealthWebSocket.offConnectionChange(handleConnectionChange);
            serviceHealthWebSocket.disconnect();
        };
    }, [expectedServices]);

    return (
        <div className="service-health-dashboard">
            <h1>Service Health Dashboard</h1>
            <Alert 
                severity={
                    connectionStatus === 'connected' ? 'success' :
                    connectionStatus === 'connecting' ? 'info' :
                    connectionStatus === 'disconnected' ? 'warning' : 'error'
                }
            >
                {connectionStatus === 'connected' && 'Server connected successfully'}
                {connectionStatus === 'connecting' && 'Connecting to server...'}
                {connectionStatus === 'disconnected' && 'Connection lost. Attempting to reconnect...'}
                {connectionStatus === 'error' && 'Failed to connect to server'}
            </Alert>
            {error && (
                <Alert severity="error">{error}</Alert>
            )}
            <div className="services-grid">
                {services.map(service => (
                    <ServiceStatusCard key={service.serviceId} serviceHealth={service} />
                ))}
            </div>
            {loading && connectionStatus === 'connecting' && (
                    <Alert severity="info">Services loading...</Alert>
                )}
                {connectionStatus === 'connected' && services.length === 0 && (
                    <Alert severity="info">Loading services...</Alert>
                )}
        </div>
    );
};

export default ServiceHealthDashboard; 