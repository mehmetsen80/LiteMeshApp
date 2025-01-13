import React from 'react';
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

const TrendChart = ({ data, label }) => {
    const chartData = {
        labels: ['Past', 'Current', 'Forecast'],
        datasets: [{
            label,
            data: [
                data.mean - data.stdDev,
                data.mean,
                data.forecast
            ],
            borderColor: data.healthStatus === 'HEALTHY' ? '#4caf50' : 
                        data.healthStatus === 'WARNING' ? '#ff9800' : '#f44336',
            tension: 0.4,
            fill: false
        }]
    };

    const options = {
        responsive: true,
        animation: {
            duration: 2000,
            easing: 'easeInOutQuart',
        },
        plugins: {
            legend: {
                display: false
            },
            tooltip: {
                enabled: true,
                animation: {
                    duration: 200,
                    easing: 'easeOutQuad',
                },
                backgroundColor: 'rgba(0, 0, 0, 0.8)',
                padding: 12,
                cornerRadius: 4,
            }
        },
        scales: {
            y: {
                beginAtZero: true,
                animation: {
                    duration: 1500,
                    easing: 'easeInOutQuart',
                },
            }
        },
        transitions: {
            active: {
                animation: {
                    duration: 300,
                },
            },
        },
    };

    return (
        <div className="trend-chart">
            <Line data={chartData} options={options} />
        </div>
    );
};

export default TrendChart; 