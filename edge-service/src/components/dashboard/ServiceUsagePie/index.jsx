import React, { useState, useEffect } from 'react';
import { Card } from 'react-bootstrap';
import { Pie } from 'react-chartjs-2';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import statsService from '../../../services/dashboardService';
import { LoadingSpinner } from '../../common/LoadingSpinner';
import './styles.css';

ChartJS.register(ArcElement, Tooltip, Legend);

const COLORS = [
  '#FF6384', // pink
  '#36A2EB', // blue
  '#FFCE56', // yellow
  '#4BC0C0', // teal
  '#9966FF', // purple
  '#FF9F40', // orange
  '#7BC043', // green
  '#FF5733', // coral
  '#C70039', // red
  '#4A90E2'  // light blue
];

const ServiceUsagePie = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [data, setData] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const response = await statsService.getServiceUsage();
      
      // Sort by request count in descending order
      const sortedData = response.sort((a, b) => b.requestCount - a.requestCount);
      
      // Calculate total requests
      const totalRequests = sortedData.reduce((sum, item) => sum + item.requestCount, 0);
      
      const chartData = {
        // Add request count and percentage to labels
        labels: sortedData.map(item => {
          const percentage = totalRequests > 0 
            ? ((item.requestCount / totalRequests) * 100).toFixed(1) 
            : '0.0';
          return `${item.service} (${item.requestCount} - ${percentage}%)`;
        }),
        datasets: [{
          data: sortedData.map(item => item.requestCount),
          backgroundColor: COLORS.slice(0, sortedData.length),
          borderWidth: 1
        }]
      };

      setData(chartData);
      setError(null);
    } catch (err) {
      setError('Failed to load service usage data');
      console.error('Error fetching service usage:', err);
    } finally {
      setLoading(false);
    }
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'right',
        labels: {
          padding: 20,
          usePointStyle: true,
          font: {
            size: 12 // Adjust font size if needed
          }
        }
      },
      tooltip: {
        callbacks: {
          label: (context) => {
            const value = context.raw || 0;
            const total = context.dataset.data.reduce((a, b) => a + b, 0);
            const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0.0';
            return `${value} requests (${percentage}%)`;
          }
        }
      }
    }
  };

  return (
    <Card className="service-usage-card">
      <Card.Header>
        <h3 className="chart-title">Service Usage Distribution</h3>
      </Card.Header>
      <Card.Body>
        {loading ? (
          <LoadingSpinner />
        ) : error ? (
          <div className="error-message">{error}</div>
        ) : (
          <div className="pie-container">
            {data && <Pie data={data} options={options} />}
          </div>
        )}
      </Card.Body>
    </Card>
  );
};

export default ServiceUsagePie; 