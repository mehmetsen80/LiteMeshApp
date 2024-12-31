import { useState, useEffect } from 'react';
import { getApiMetrics, getMetricsByService, getMetricsByTimeRange } from  '../../../services/apiMetricService';
import MetricsChart from '../MetricsChart';
import MetricsTable from '../MetricsTable';
import MetricsFilter from '../MetricsFilter';
import './styles.css';

function MetricsDisplay() {
  const [metrics, setMetrics] = useState([]);
  const [filteredMetrics, setFilteredMetrics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [services, setServices] = useState([]);
  const [selectedService, setSelectedService] = useState('');

  useEffect(() => {
    fetchMetrics();
  }, []);

  const fetchMetrics = async () => {
    try {
      setLoading(true);
      const data = await getApiMetrics();
      data.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
      setMetrics(data);
      setFilteredMetrics(data);

      const uniqueServices = [...new Set([
        ...data.map(metric => metric.fromService),
        ...data.map(metric => metric.toService)
      ])].filter(Boolean).sort();
      
      setServices(uniqueServices);
    } catch (err) {
      setError('Failed to fetch metrics');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = (metrics, filters) => {
    return metrics.filter(metric => {
      const metricDate = new Date(metric.timestamp).getTime();
      
      // Service filter
      if (filters.service && 
         (metric.fromService !== filters.service && metric.toService !== filters.service)) {
        return false;
      }

      // Date filters
      if (filters.startDate && !filters.endDate) {
        // Only start date is set
        return metricDate >= filters.startDate;
      }
      
      if (!filters.startDate && filters.endDate) {
        // Only end date is set
        return metricDate <= filters.endDate;
      }
      
      if (filters.startDate && filters.endDate) {
        // Both dates are set
        return metricDate >= filters.startDate && metricDate <= filters.endDate;
      }

      return true;
    });
  };

  const handleFilterChange = (filters) => {
    // Set selected service for the chart
    setSelectedService(filters.service || '');

    // Use the applyFilters function to filter metrics
    const filtered = applyFilters(metrics, filters);

    // Sort filtered data by timestamp
    filtered.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
    setFilteredMetrics(filtered);
  };

  if (loading) return (
    <div className="loading-container">
      <div className="loading">Loading metrics...</div>
    </div>
  );

  if (error) return (
    <div className="error-container">
      <div className="error">Error: {error}</div>
    </div>
  );

  return (
    <div className="metrics-container">
      <h1>API Metrics Dashboard</h1>
      <div className="metrics-content">
        <div className="section filter-section">  {/* Changed this line */}
          <MetricsFilter 
            onFilterChange={handleFilterChange} 
            services={services}
          />
        </div>
        <div className="section chart-section">
          {/* <h2 className="section-title">API Response Times by Service</h2> */}
          <MetricsChart 
            data={filteredMetrics} 
            selectedService={selectedService}
          />
        </div>
        <div className="section table-section">
          <h2 className="section-title">Detailed Metrics</h2>
          <MetricsTable 
            data={filteredMetrics}
            selectedService={selectedService}
          />
        </div>
      </div>
    </div>
  );
}

export default MetricsDisplay;