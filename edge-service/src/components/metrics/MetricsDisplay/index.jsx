import React, { useState, useEffect } from 'react';
import { getApiMetrics } from '../../../services/apiMetricService';
import { Alert } from '@mui/material';
import MetricsTable from '../MetricsTable';
import MetricsChart from '../MetricsChart';
import MetricsFilter from '../MetricsFilter';
import './styles.css';
import { getDefaultDateRange, formatDateForApi } from '../../../utils/dateUtils';

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
        const { startDate, endDate } = getDefaultDateRange();
        
        const params = {
          startDate: formatDateForApi(startDate),
          endDate: formatDateForApi(endDate)
        };

        const data = await getApiMetrics(params);
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
          setError('Failed to fetch metrics: ' + err.message);
          console.error('Error fetching metrics:', err);
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
    
    // Create params for API call
    const params = {};
    if (filters.startDate) {
      params.startDate = new Date(filters.startDate).toISOString();
    }
    if (filters.endDate) {
      params.endDate = new Date(filters.endDate).toISOString();
    }
    if (filters.fromService) {
      params.fromService = filters.fromService;
    }
    if (filters.toService) {
      params.toService = filters.toService;
    }

    // Fetch filtered data from API
    getApiMetrics(params)
      .then(data => {
        setFilteredMetrics(data);
      })
      .catch(err => {
        setError('Failed to fetch filtered metrics: ' + err.message);
        console.error('Error fetching filtered metrics:', err);
      });
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
