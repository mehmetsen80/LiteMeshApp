import React, { useMemo } from 'react';
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js';
import './styles.css';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

// Create a color generator function
const generateColors = (index, totalServices) => {
  // Base color (your primary color #0c056d)
  const baseHue = 240; // Hue value for #0c056d
  const baseSaturation = 90;
  const baseLightness = 22;

  // Calculate evenly distributed hues
  const hueStep = 360 / totalServices;
  const hue = (baseHue + (index * hueStep)) % 360;

  // Alternate saturation and lightness for better distinction
  const saturation = baseSaturation + (index % 2) * 5;
  const lightness = baseLightness + (index % 3) * 10;

  return `hsl(${hue}, ${saturation}%, ${lightness}%)`;
};

function MetricsChart({ data, selectedService }) {
  // Create a persistent color map for services
  const serviceColors = useMemo(() => {
    const colors = {};
    
    // Get unique service pairs
    const uniqueServices = [...new Set(data.map(metric => 
      `${metric.fromService} → ${metric.toService}`
    ))];

    // Assign generated colors to services
    uniqueServices.forEach((service, index) => {
      colors[service] = generateColors(index, uniqueServices.length);
    });

    return colors;
  }, []); // Empty dependency array means this only runs once

  // Format date and time
  const formatDateTime = (timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleString('en-US', {
      month: '2-digit',
      day: '2-digit',
      year: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  };

  // Group data by service
  const serviceData = data.reduce((acc, metric) => {
    const serviceName = `${metric.fromService} → ${metric.toService}`;
    if (!acc[serviceName]) {
      acc[serviceName] = [];
    }
    acc[serviceName].push({
      timestamp: metric.timestamp,
      duration: metric.duration
    });
    return acc;
  }, {});

  // Get all timestamps and sort them
  const timestamps = [...new Set(data.map(metric => metric.timestamp))].sort();

  const chartData = {
    labels: timestamps.map(formatDateTime),
    datasets: Object.entries(serviceData).map(([serviceName, metrics]) => ({
      label: serviceName,
      data: timestamps.map(timestamp => {
        const point = metrics.find(m => m.timestamp === timestamp);
        return point ? point.duration : null;
      }),
      fill: false,
      borderColor: serviceColors[serviceName],
      backgroundColor: serviceColors[serviceName],
      tension: 0.1,
      spanGaps: true,
      animation: {
        duration: 100
      }
    }))
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    animation: {
        duration: 100
    },
    animations: {
        colors: false
    },
    plugins: {
        legend: {
            position: 'top',
            labels: {
                color: '#333',
                font: {
                    size: 12
                }
            }
        },
        title: {
            display: true,
            text: 'API Response Times by Service',
            font: {
                size: 16,
                weight: 'bold'
            }
        },
        tooltip: {
            enabled: true,
            mode: 'nearest',
            intersect: false,
            backgroundColor: '#000000',
            titleColor: '#ffffff',
            bodyColor: '#ffffff',
            borderColor: '#333',
            borderWidth: 1,
            padding: 10,
            bodySpacing: 4,
            bodyFont: {
                size: 12
            },
            titleFont: {
                size: 13,
                weight: 'bold'
            },
            boxPadding: 6,
            boxWidth: 6,
            usePointStyle: true,
            animation: false,
            transitions: false,
            animationDuration: 0,
            callbacks: {
                label: (context) => {
                    // Find the corresponding metric using timestamp and service name
                    const timestamp = timestamps[context.dataIndex];
                    const serviceName = context.dataset.label;
                    const metric = data.find(m => 
                        m.timestamp === timestamp && 
                        `${m.fromService} → ${m.toService}` === serviceName
                    );

                    if (!metric) return null;

                    return [
                        `Duration: ${metric.duration}ms`,
                        `From: ${metric.fromService}`,
                        `To: ${metric.toService}`,
                        `Gateway URL: ${metric.gatewayBaseUrl}`,
                        `Query Params: ${metric.queryParameters || 'none'}`,
                        `Path: ${metric.pathEndPoint}`,
                        `Route ID: ${metric.routeIdentifier}`,
                        `Type: ${metric.interactionType}`,
                        `Status: ${metric.success ? '✅ SUCCESS' : '❌ FAILED'}`
                    ];
                },
                title: (context) => {
                    const timestamp = timestamps[context[0].dataIndex];
                    return formatDateTime(timestamp);
                }
            }
        }
    },
    interaction: {
        mode: 'nearest',
        intersect: false,
        axis: 'x'
    },
    scales: {
      y: {
        beginAtZero: true,
        title: {
          display: true,
          text: 'Duration (ms)'
        }
      },
      x: {
        title: {
          display: true,
          text: 'Date/Time'
        },
        ticks: {
          maxRotation: 45,
          minRotation: 45
        }
      }
    }
  };

  return (
    <div className="metrics-chart" style={{ position: 'relative', zIndex: 0 }}>
      <Line
        data={chartData}
        options={options}
        height={300}
      />
    </div>
  );
}

export default MetricsChart;