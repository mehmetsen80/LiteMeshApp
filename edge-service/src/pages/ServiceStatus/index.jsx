import React from 'react';
import ServiceHealthDashboard from '../../components/services/ServiceHealthDashboard';
import './styles.css';

const ServiceStatus = () => {
    return (
        <div className="service-status-page">
            <ServiceHealthDashboard />
        </div>
    );
};

export default ServiceStatus; 