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
      // Service filters
      if (filters.fromService && metric.fromService !== filters.fromService) {
        return false;
      }
      if (filters.toService && metric.toService !== filters.toService) {
        return false;
      }

      // Date filters
      const metricTime = new Date(metric.timestamp).getTime();
      
      if (filters.startDate && filters.endDate) {
        return metricTime >= filters.startDate && metricTime <= filters.endDate;
      }
      
      if (filters.startDate) {
        return metricTime >= filters.startDate;
      }
      
      if (filters.endDate) {
        return metricTime <= filters.endDate;
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

  const exportToCSV = () => {
    const headers = ['From Service', 'To Service', 'Duration', 'Timestamp', 'Success'];
    
    const formatDate = (date) => {
      const d = new Date(date);
      return `${d.toLocaleDateString().replace(/,/g, '')} ${d.toLocaleTimeString()}`;
    };

    const csvData = filteredMetrics.map(metric => [
      metric.fromService,
      metric.toService,
      metric.duration,
      formatDate(metric.timestamp),  // Format date without commas
      metric.success ? 'Yes' : 'No'
    ]);

    const csvContent = [
      headers.join(','),
      ...csvData.map(row => row.join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `metrics_export_${new Date().toISOString()}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
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
          <div className="table-header">
            <h2 className="section-title">Detailed Metrics</h2>
            <button 
              className="export-button"
              onClick={exportToCSV}
              title="Export filtered data to CSV"
            >
              Export to CSV
            </button>
          </div>
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