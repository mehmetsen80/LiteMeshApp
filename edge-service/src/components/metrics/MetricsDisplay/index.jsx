import React, { useState, useEffect } from 'react';
import { getApiMetrics } from '../../../services/apiMetricService';
import { Alert } from '@mui/material';
import MetricsTable from '../MetricsTable';
import MetricsChart from '../MetricsChart';
import MetricsFilter from '../MetricsFilter';
import './styles.css';

function MetricsDisplay() {
  const [metrics, setMetrics] = useState([]);
  const [filteredMetrics, setFilteredMetrics] = useState([]);
  const [selectedService, setSelectedService] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [services, setServices] = useState([]);

  useEffect(() => {
    let mounted = true;

    const fetchMetrics = async () => {
      try {
        const data = await getApiMetrics();
        if (mounted) {
          data.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
          setMetrics(data);
          setFilteredMetrics(data);
          
          const uniqueServices = [...new Set([
            ...data.map(metric => metric.fromService),
            ...data.map(metric => metric.toService)
          ])].filter(Boolean).sort();
          
          setServices(uniqueServices);
        }
      } catch (err) {
        if (mounted) {
          setError('Failed to fetch metrics');
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    fetchMetrics();

    return () => {
      mounted = false;
    };
  }, []);

  const handleFilterChange = (filters) => {
    setSelectedService(filters.service || '');
    const filtered = metrics.filter(metric => {
      if (filters.fromService && metric.fromService !== filters.fromService) return false;
      if (filters.toService && metric.toService !== filters.toService) return false;
      
      // Date range filtering
      if (filters.startDate || filters.endDate) {
        const metricTime = new Date(metric.timestamp).getTime();
        if (filters.startDate && metricTime < filters.startDate) return false;
        if (filters.endDate && metricTime > filters.endDate) return false;
      }
      
      return true;
    });
    setFilteredMetrics(filtered);
  };

  if (loading) {
    return <Alert severity="info">Loading metrics data...</Alert>;
  }

  if (error) {
    return <Alert severity="error">{error}</Alert>;
  }

  return (
    <div className="metrics-container">
      <h1>API Metrics Dashboard</h1>
      <div className="metrics-content">
        <div className="section filter-section">
          <MetricsFilter 
            onFilterChange={handleFilterChange}
            services={services}
          />
        </div>
        <div className="section chart-section">
          <MetricsChart 
            data={filteredMetrics}
            selectedService={selectedService}
          />
        </div>
        <div className="section table-section">
          <MetricsTable metrics={filteredMetrics} />
        </div>
      </div>
    </div>
  );
}

export default MetricsDisplay;
