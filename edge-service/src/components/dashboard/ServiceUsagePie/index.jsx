import React, { useState, useEffect } from 'react';
import { Card } from 'react-bootstrap';
import { Pie } from 'react-chartjs-2';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import { useTeam } from '../../../contexts/TeamContext';
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
];

const ServiceUsagePie = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { currentTeam } = useTeam();

  useEffect(() => {
    let mounted = true;
    
    const fetchData = async () => {
      if (!currentTeam?.id) {
        setLoading(false);
        return;
      }
      
      try {
        setLoading(true);
        setError(null);
        
        const stats = await statsService.getServiceUsage(currentTeam.id);
        
        if (!mounted) return;

        if (!stats || stats.length === 0) {
          setData(null);
          return;
        }

        // Sort by request count in descending order
        const sortedStats = stats.sort((a, b) => b.requestCount - a.requestCount);
        const totalRequests = sortedStats.reduce((sum, item) => sum + item.requestCount, 0);
        
        setData({
          labels: sortedStats.map(item => {
            const percentage = totalRequests > 0 ? ((item.requestCount / totalRequests) * 100).toFixed(1): '0.0';
            return `${item.service} (${item.requestCount} - ${percentage}%)`;
          }),
          datasets: [{
            data: sortedStats.map(item => item.requestCount),
            backgroundColor: COLORS,
            borderColor: COLORS.map(color => color.replace('0.2', '1')),
            borderWidth: 1,
          }],
        });
      } catch (err) {
        console.error('Error fetching service usage:', err);
        setError('Failed to load service usage data');
        setData(null);
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    fetchData();

    return () => {
      mounted = false;
    };
  }, [currentTeam?.id]); // Only depend on team ID

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'right',
        labels: {
          font: {
            size: 12
          }
        }
      },
      tooltip: {
        callbacks: {
          label: (context) => {
            const label = context.label || '';
            const value = context.raw || 0;
            const total = context.dataset.data.reduce((a, b) => a + b, 0);
            const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0.0';
            return `${label}: ${value} requests (${percentage}%)`;
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
        ) : data?.datasets[0]?.data?.length > 0 ? (
          <div className="pie-container">
            <Pie data={data} options={options} />
          </div>
        ) : (
          <div className="no-data-message">
            No service usage data available for your team
          </div>
        )}
      </Card.Body>
    </Card>
  );
};

export default ServiceUsagePie; 