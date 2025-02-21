import React, { useState, useEffect } from 'react';
import { LoadingSpinner } from '../../common/LoadingSpinner';
import { apiRouteService } from '../../../services/apiRouteService';
import CreateApiRouteModal from '../CreateApiRouteModal';
import './styles.css';
import { useNavigate } from 'react-router-dom';
import { showSuccessToast, showErrorToast } from '../../../utils/toastConfig';
import Button from '../../common/Button';
import { HiPlus } from 'react-icons/hi';

export const ApiRoutesDisplay = () => {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const navigate = useNavigate();

  const handleCreateRoute = async (routeData) => {
    try {
      console.log('Creating route:', routeData);
      const response = await apiRouteService.createRoute(routeData);
      const updatedRoutes = await apiRouteService.getAllRoutes();
      setRoutes(updatedRoutes);
      setShowCreateModal(false);
      if (response.message) {
        showSuccessToast(response.message);
      }
    } catch (err) {
      console.error('Failed to create route:', err);
      showErrorToast(err.message || 'Failed to create route');
    }
  };

  useEffect(() => {
    const fetchRoutes = async () => {
      try {
        const data = await apiRouteService.getAllRoutes();
        setRoutes(data);
      } catch (err) {
        setError('Failed to fetch routes: ' + err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchRoutes();
  }, []);

  if (loading) {
    return <LoadingSpinner />;
  }

  if (error) {
    return <div className="error-message">Error: {error}</div>;
  }

  return (
    <div className="routes-container">
      <div className="routes-header">
        <div>
          <h2>API Routes</h2>
        </div>
        <div>
          <Button 
            variant="primary" 
            onClick={() => setShowCreateModal(true)}
          >
            <HiPlus /> Create API Route
          </Button>
        </div>
      </div>

      <div className="routes-grid">
        {routes.map((route) => (
          <div 
            key={route.id || `${route.routeIdentifier}-${route.createdAt}`}
            className="route-card"
            onClick={() => navigate(`/api-routes/${route.routeIdentifier}`)}
          >
            <div className="route-header">
              <div className="route-icon">
                <i className="fas fa-route"></i>
              </div>
              <div className="route-title">
                <h3>{route.routeIdentifier}</h3>
                <span className="route-method">{route.method || 'ALL'}</span>
              </div>
            </div>
            <div className="route-details">
              <div className="detail-item">
                <span className="detail-label">Path</span>
                <code className="detail-value">{route.path}</code>
              </div>
              <div className="detail-item">
                <span className="detail-label">URI</span>
                <code className="detail-value">{route.uri}</code>
              </div>
            </div>
            <div className="route-filters">
              <div className="filters-header">
                <i className="fas fa-filter"></i>
                <span>Active Filters</span>
              </div>
              <div className="filter-tags">
                {route.filters?.map((filter, index) => (
                  <span key={index} className="filter-tag">
                    <i className={`fas fa-${getFilterIcon(filter.name)}`}></i>
                    {filter.name}
                  </span>
                ))}
              </div>
            </div>
            <div className="route-actions">
              <button 
                className="action-button view-button"
                onClick={(e) => {
                  e.stopPropagation();
                  navigate(`/api-routes/${route.routeIdentifier}`);
                }}
              >
                <i className="fas fa-eye"></i> View
              </button>
              <button 
                className="action-button edit-button"
                onClick={(e) => {
                  e.stopPropagation();
                  navigate(`/api-routes/${route.routeIdentifier}/edit`);
                }}
              >
                <i className="fas fa-edit"></i> Edit
              </button>
              <button 
                className="action-button delete-button"
                onClick={(e) => e.stopPropagation()}
              >
                <i className="fas fa-trash"></i> Delete
              </button>
            </div>
          </div>
        ))}
      </div>

      <CreateApiRouteModal
        show={showCreateModal}
        onHide={() => setShowCreateModal(false)}
        onSubmit={handleCreateRoute}
      />
    </div>
  );
};

const getFilterIcon = (filterName) => {
  const iconMap = {
    'CircuitBreaker': 'shield-alt',
    'RedisRateLimiter': 'tachometer-alt',
    'TimeLimiter': 'clock',
    'Retry': 'redo',
    'default': 'filter'
  };
  return iconMap[filterName] || iconMap.default;
}; 