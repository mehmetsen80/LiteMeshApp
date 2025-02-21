import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { OverlayTrigger, Tooltip } from 'react-bootstrap';
import statsService from '../../../services/dashboardService';
import './styles.css';

const StatsSection = ({ refreshInterval = 30000 }) => {
  const [stats, setStats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchStats = async () => {
    try {
      const response = await statsService.getStats();
      setStats(response);
      setError(null);
    } catch (err) {
      setError('Failed to load statistics');
      console.error('Error fetching stats:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
    const interval = setInterval(fetchStats, refreshInterval);
    return () => clearInterval(interval);
  }, [refreshInterval]);

  const getStatDescription = (stat) => {
    const descriptions = {
      'active-routes': 'Total number of API routes currently configured and active in the gateway',
      'response-time': 'Average time taken to process and respond to API requests, measured in milliseconds',
      'requests-per-min': 'Total number of API requests being handled by the gateway per minute',
      'success-rate': 'Percentage of requests that completed successfully with 2xx status codes'
    };
    return descriptions[stat] || `Statistics for ${stat}`;
  };

  const getTrendExplanation = (stat) => {
    const explanations = {
      'active-routes': 'Change in number of routes compared to last week',
      'response-time': 'Change in average response time compared to previous hour',
      'requests-per-min': 'Change in request rate compared to previous minute',
      'success-rate': 'Change in success rate compared to previous 5 minutes'
    };
    return explanations[stat] || 'Trend data not available';
  };

  const getTrendIcon = (type) => {
    switch (type) {
      case 'positive':
        return 'fa-arrow-up';
      case 'negative':
        return 'fa-arrow-down';
      default:
        return 'fa-equals';
    }
  };

  if (loading) {
    return (
      <div className="stats-section loading">
        {[1, 2, 3, 4].map(id => (
          <div key={id} className="stat-card skeleton">
            <div className="stat-icon skeleton-icon"></div>
            <div className="stat-content">
              <div className="skeleton-text skeleton-title"></div>
              <div className="skeleton-text skeleton-value"></div>
              <div className="skeleton-text skeleton-trend"></div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="stats-section error">
        <div className="error-message">
          <i className="fas fa-exclamation-circle"></i>
          {error}
          <button onClick={fetchStats} className="retry-button">
            <i className="fas fa-redo"></i> Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="stats-section">
      {stats.map(stat => (
        <OverlayTrigger
          key={stat.id}
          placement="top"
          overlay={
            <Tooltip id={`tooltip-${stat.id}`}>
              <div className="stat-tooltip">
                <p>{getStatDescription(stat.id)}</p>
                <p className="trend-explanation">
                  <small>{getTrendExplanation(stat.id)}</small>
                </p>
              </div>
            </Tooltip>
          }
        >
          <div className="stat-card">
            <div className="stat-icon">
              <i className={`fas ${stat.icon}`}></i>
            </div>
            <div className="stat-content">
              <h4>{stat.title}</h4>
              <div className="stat-value">{stat.value}</div>
              <div className={`stat-trend ${stat.trend.type}`}>
                <i className={`fas ${getTrendIcon(stat.trend.type)}`}></i>
                {stat.trend.value} {stat.trend.period}
              </div>
            </div>
          </div>
        </OverlayTrigger>
      ))}
    </div>
  );
};

StatsSection.propTypes = {
  refreshInterval: PropTypes.number
};

export default StatsSection; 