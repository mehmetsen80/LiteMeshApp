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

function MetricsChart({ data }) {
  // Generate random color
  const getRandomColor = () => {
    const letters = '0123456789ABCDEF';
    let color = '#';
    for (let i = 0; i < 6; i++) {
      color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
  };

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
    datasets: Object.entries(serviceData).map(([serviceName, metrics]) => {
      const color = getRandomColor();
      return {
        label: serviceName,
        data: timestamps.map(timestamp => {
          const point = metrics.find(m => m.timestamp === timestamp);
          return point ? point.duration : null;
        }),
        fill: false,
        borderColor: color,
        backgroundColor: color,
        tension: 0.1,
        spanGaps: true
      };
    })
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
        labels: {
          padding: 20,
          usePointStyle: true,
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
        callbacks: {
          label: (context) => {
            return `${context.dataset.label}: ${context.parsed.y} ms`;
          },
          title: (tooltipItems) => {
            const timestamp = timestamps[tooltipItems[0].dataIndex];
            return new Date(timestamp).toLocaleString();
          }
        }
      }
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
    <div className="metrics-chart">
      <Line 
        data={chartData} 
        options={options}
        height={300}
      />
    </div>
  );
}

export default MetricsChart;