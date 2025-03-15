import React, { useState, useEffect, useCallback } from 'react';
import { Card, Form, Alert, Row, Col } from 'react-bootstrap';
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  defaults
} from 'chart.js';
import statsService from '../../../services/dashboardService';
import { useTeam } from '../../../contexts/TeamContext';
import './styles.css';

// Register ChartJS components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

// Add percentile descriptions with clearer explanations
const PERCENTILE_DESCRIPTIONS = {
  p50: 'Median latency - The point at which 50% of requests are faster and 50% are slower',
  p75: '75th percentile - The point at which 75% of requests are faster and 25% are slower',
  p90: '90th percentile - The point at which 90% of requests are faster and 10% are slower. Often used in Service Level Agreements (SLA)',
  p95: '95th percentile - The point at which 95% of requests are faster and 5% are slower. Used to track performance anomalies',
  p99: '99th percentile - The point at which 99% of requests are faster and 1% are slower. Shows the typical worst-case performance excluding extreme outliers'
};

const TIME_RANGES = {
  '1h': 'Last Hour',
  '24h': 'Last 24 Hours',
  '7d': 'Last 7 Days',
  '30d': 'Last 30 Days',
  '90d': 'Last 90 Days'
};

const DEFAULT_LATENCY_DATA = [
  {
    id: { method: 'GET', path: '/api/*', service: 'all' },
    p50: 0,
    p75: 0,
    p90: 0,
    p95: 0,
    p99: 0
  }
];

const LatencyChart = () => {
  const [latencyData, setLatencyData] = useState(DEFAULT_LATENCY_DATA);
  const [loading, setLoading] = useState(false); // Start with false to show chart immediately
  const [error, setError] = useState(null);
  const [timeRange, setTimeRange] = useState('30d');
  const { currentTeam } = useTeam();

  const fetchLatencyStats = useCallback(async () => {
    if (!currentTeam?.id) {
      setLatencyData(DEFAULT_LATENCY_DATA);
      return;
    }
    
    try {
      setLoading(true);
      const data = await statsService.getLatencyStats(currentTeam.id, timeRange);
      setLatencyData(data.length > 0 ? data : DEFAULT_LATENCY_DATA);
      setError(null);
    } catch (err) {
      setError('Failed to load latency statistics');
      console.error('Error fetching latency stats:', err);
      setLatencyData(DEFAULT_LATENCY_DATA);
    } finally {
      setLoading(false);
    }
  }, [currentTeam?.id, timeRange]);

  useEffect(() => {
    fetchLatencyStats();
    const intervalId = setInterval(fetchLatencyStats, 5 * 60 * 1000);
    return () => clearInterval(intervalId);
  }, [fetchLatencyStats]);

  // Get unique services
  const uniqueServices = ['all', ...new Set(latencyData.map(stat => stat.id.service))];

  // Filter stats by selected service
  const filteredStats = 'all' === 'all' 
    ? latencyData 
    : latencyData.filter(stat => stat.id.service === 'all');

  // Prepare data for Chart.js
  const chartData = {
    labels: filteredStats.map(stat => `${stat.id.method} ${stat.id.path}`),
    datasets: [
      {
        label: 'p50 (Median)',
        description: PERCENTILE_DESCRIPTIONS.p50,
        data: filteredStats.map(stat => stat.p50),
        borderColor: 'rgb(53, 162, 235)',
        backgroundColor: 'rgba(53, 162, 235, 0.5)',
      },
      {
        label: 'p75',
        description: PERCENTILE_DESCRIPTIONS.p75,
        data: filteredStats.map(stat => stat.p75),
        borderColor: 'rgb(75, 192, 192)',
        backgroundColor: 'rgba(75, 192, 192, 0.5)',
      },
      {
        label: 'p90',
        description: PERCENTILE_DESCRIPTIONS.p90,
        data: filteredStats.map(stat => stat.p90),
        borderColor: 'rgb(255, 159, 64)',
        backgroundColor: 'rgba(255, 159, 64, 0.5)',
      },
      {
        label: 'p95',
        description: PERCENTILE_DESCRIPTIONS.p95,
        data: filteredStats.map(stat => stat.p95),
        borderColor: 'rgb(255, 99, 132)',
        backgroundColor: 'rgba(255, 99, 132, 0.5)',
      },
      {
        label: 'p99',
        description: PERCENTILE_DESCRIPTIONS.p99,
        data: filteredStats.map(stat => stat.p99),
        borderColor: 'rgb(153, 102, 255)',
        backgroundColor: 'rgba(153, 102, 255, 0.5)',
      }
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
        labels: {
          // Simplified legend labels
          generateLabels: function(chart) {
            const datasets = chart.data.datasets;
            return datasets.map((dataset, i) => ({
              text: dataset.label,
              fillStyle: dataset.backgroundColor,
              strokeStyle: dataset.borderColor,
              lineWidth: 2,
              hidden: !chart.isDatasetVisible(i),
              index: i
            }));
          }
        },
        onClick: (e, legendItem, legend) => {
          const index = legendItem.index;
          const ci = legend.chart;
          if (ci.isDatasetVisible(index)) {
            ci.hide(index);
          } else {
            ci.show(index);
          }
        }
      },
      tooltip: {
        callbacks: {
          label: function(context) {
            const dataset = context.dataset;
            const description = PERCENTILE_DESCRIPTIONS[dataset.label.toLowerCase().split(' ')[0]];
            return [
              `${dataset.label}: ${context.parsed.y.toFixed(0)}ms`,
              description
            ];
          }
        }
      }
    },
    scales: {
      y: {
        title: {
          display: true,
          text: 'Response Time (ms)'
        },
        beginAtZero: true
      },
      x: {
        ticks: {
          maxRotation: 45,
          minRotation: 45
        }
      }
    },
    interaction: {
      mode: 'index',
      intersect: false
    },
    elements: {
      line: {
        tension: 0.4
      }
    }
  };

  if (loading) {
    return (
      <Card>
        <Card.Body>
          <div className="loading-spinner">Loading...</div>
        </Card.Body>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <Card.Body>
          <Alert variant="danger">{error}</Alert>
        </Card.Body>
      </Card>
    );
  }

  return (
    <Card>
      <Card.Header>
        <Row className="align-items-center">
          <Col>
            <h3 className="chart-title">
              API Latency Distribution
              {loading && <small className="text-muted ms-2">(Updating...)</small>}
            </h3>
          </Col>
          <Col xs="auto">
            <Form.Select 
              value={timeRange}
              onChange={e => setTimeRange(e.target.value)}
              className="me-2"
            >
              {Object.entries(TIME_RANGES).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Form.Select>
          </Col>
        </Row>
      </Card.Header>
      <Card.Body>
        {error && (
          <Alert variant="danger" className="mb-3">
            {error}
          </Alert>
        )}
        <div style={{ height: '400px' }}>
          <Line 
            options={options} 
            data={chartData} 
          />
        </div>
        <div className="mt-4 latency-info">
          <h5>Understanding Latency Percentiles</h5>
          <p className="text-muted">
            Percentiles help understand the distribution of response times across all requests:
          </p>
          <ul className="percentile-list">
            {Object.entries(PERCENTILE_DESCRIPTIONS).map(([key, desc]) => (
              <li key={key}>
                <strong>{key.toUpperCase()}</strong>: {desc}
              </li>
            ))}
          </ul>
        </div>
      </Card.Body>
    </Card>
  );
};

export default LatencyChart; 