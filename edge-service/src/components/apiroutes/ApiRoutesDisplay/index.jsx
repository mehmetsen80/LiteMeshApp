import React, { useState, useEffect } from 'react';
import { Card, Breadcrumb, OverlayTrigger, Tooltip } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { LoadingSpinner } from '../../common/LoadingSpinner';
import { apiRouteService } from '../../../services/apiRouteService';
import { teamService } from '../../../services/teamService';
import { useTeam } from '../../../contexts/TeamContext';
import { useAuth } from '../../../contexts/AuthContext';
import CreateApiRouteModal from '../CreateApiRouteModal';
import './styles.css';
import { useNavigate } from 'react-router-dom';
import { showSuccessToast, showErrorToast } from '../../../utils/toastConfig';
import Button from '../../common/Button';
import { HiPlus, HiShieldCheck, HiClock, HiRefresh, HiLightningBolt, HiTrash } from 'react-icons/hi';
import ConfirmationModal from '../../common/ConfirmationModal';
import { isSuperAdmin, hasAdminAccess } from '../../../utils/roleUtils';

const PERMISSION_COLORS = {
  VIEW: '#2196f3',  // Blue
  USE: '#ff9800',   // Orange
  MANAGE: '#4caf50' // Green
};

const PERMISSION_INFO = {
  VIEW: {
    description: "Can view API endpoints and documentation",
    color: "#2196f3" // Blue
  },
  USE: {
    description: "Can make API calls to these endpoints",
    color: "#ff9800" // Orange
  },
  MANAGE: {
    description: "Can configure and update these endpoints",
    color: "#4caf50" // Green
  }
};

const FILTER_INFO = {
  RedisRateLimiter: {
    description: "Limits the number of requests using Redis as a rate limiter",
    icon: <HiLightningBolt />
  },
  TimeLimiter: {
    description: "Sets a maximum duration for request processing",
    icon: <HiClock />
  },
  CircuitBreaker: {
    description: "Prevents cascading failures by temporarily disabling failing routes",
    icon: <HiShieldCheck />
  },
  Retry: {
    description: "Automatically retries failed requests based on configured conditions",
    icon: <HiRefresh />
  }
};

export const ApiRoutesDisplay = () => {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const { currentTeam, loading: teamLoading } = useTeam();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [routeToDelete, setRouteToDelete] = useState(null);

  const canManageRoutes = (isSuperAdmin(user) || hasAdminAccess(user, currentTeam));

  const getFilterIcon = (filterName) => {
    switch (filterName) {
      case 'CircuitBreaker':
        return <HiShieldCheck />;
      case 'TimeLimiter':
        return <HiClock />;
      case 'Retry':
        return <HiRefresh />;
      case 'RedisRateLimiter':
        return <HiLightningBolt />;
      default:
        return null;
    }
  };

  useEffect(() => {
    if (!teamLoading) {
      if (!currentTeam && !isSuperAdmin(user)) {
        navigate('/dashboard');
        return;
      }
      fetchRoutes();
    }
  }, [currentTeam, teamLoading, user]);

  const fetchRoutes = async () => {
    try {
      setLoading(true);
      let response;
      
      if (isSuperAdmin(user) && !currentTeam) {
        // SUPER_ADMIN without team selected - fetch all team routes
        response = await teamService.getAllTeamRoutes();
      } else if (currentTeam?.id) {
        // Team selected - fetch team-specific routes
        response = await teamService.getTeamRoutes(currentTeam.id);
      } else {
        setRoutes([]);
        return;
      }

      // Handle both response formats
      if (response.success) {
        setRoutes(response.data || []);
      } else if (response.error) {
        throw new Error(response.error);
      } else {
        setRoutes([]);
      }
    } catch (err) {
      console.error('Error loading routes:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateRoute = async (routeData) => {
    try {      
      const response = await apiRouteService.createRoute(routeData);
      await fetchRoutes();
      setShowCreateModal(false);
      if (response.message) {
        showSuccessToast(response.message);
      }
    } catch (err) {
      console.error('Failed to create route:', err);
      const errorResponse = err.response?.data;
      
      if (errorResponse?.code === 'AUTH_4002') {
        showErrorToast(errorResponse.message || 'Only team administrators can create API routes');
      } else if (errorResponse?.code === 'ROUTE_3007') {
        showErrorToast(errorResponse.message || 'Route already exists');
      } else {
        showErrorToast(errorResponse?.message || 'Failed to create route');
      }
    }
  };

  const handleDelete = (routeId, e) => {
    e.stopPropagation();
    setRouteToDelete(routeId);
    setShowDeleteConfirm(true);
  };

  const handleDeleteConfirm = async () => {
    if (!currentTeam?.id) {
      showErrorToast('No team selected');
      return;
    }

    try {
      await apiRouteService.deleteRoute(routeToDelete, currentTeam.id);
      showSuccessToast('Route deleted successfully');
      fetchRoutes();
    } catch (err) {
      const errorResponse = err.response?.data;
      
      if (errorResponse?.code) {
        switch (errorResponse.code) {
          case 'ROUTE_3001': // ROUTE_NOT_FOUND
            showErrorToast(errorResponse.message || 'Route not found', {
              onClose: () => fetchRoutes()
            });
            break;

          case 'AUTH_4002': // FORBIDDEN
            showErrorToast(errorResponse.message || 'You don\'t have permission to delete this route');
            break;

          case 'ROUTE_3002': // ROUTE_ALREADY_ASSIGNED
          case 'ROUTE_3003': // ROUTE_OPERATION_ERROR
            showErrorToast(errorResponse.message, {
              autoClose: 7000 // Longer duration for important messages
            });
            break;

          case 'USER_6001': // USER_NOT_FOUND
            showErrorToast(errorResponse.message || 'User not found');
            break;

          case 'SYS_5001': // INTERNAL_ERROR
            showErrorToast(
              'An internal error occurred. Please try again or contact support.',
              { autoClose: 7000 }
            );
            break;

          default:
            showErrorToast(
              errorResponse.message || 
              'Failed to delete route. Please try again or contact support if the problem persists.'
            );
        }
      } else {
        // Fallback for unexpected error format
        showErrorToast(
          'An unexpected error occurred. Please try again or contact support.'
        );
      }
    } finally {
      setShowDeleteConfirm(false);
      setRouteToDelete(null);
    }
  };

  if (teamLoading || loading) {
    return <LoadingSpinner />;
  }

  if (error) {
    return (
      <div className="error-message">
        <p>Error: {error}</p>
        <Button variant="primary" onClick={fetchRoutes}>Retry</Button>
      </div>
    );
  }

  return (
    <div className="routes-container">
      <Card className="mb-4 mx-1 p-0">
        <Card.Header className="d-flex justify-content-between align-items-center bg-light">
          <Breadcrumb className="bg-light mb-0">
            <Breadcrumb.Item 
              linkAs={Link} 
              linkProps={{ to: '/organizations' }}
            >
              {currentTeam?.organization?.name || 'Organization'}
            </Breadcrumb.Item>
            <Breadcrumb.Item 
              linkAs={Link} 
              linkProps={{ to: '/teams' }}
            >
              {currentTeam?.name || 'All Teams'}
            </Breadcrumb.Item>
            <Breadcrumb.Item active>
              API Routes
            </Breadcrumb.Item>
          </Breadcrumb>
          {canManageRoutes && (
            isSuperAdmin(user) ? (
              <Button 
                variant="primary" 
                onClick={() => setShowCreateModal(true)}
                disabled={!currentTeam && !hasAdminAccess(user)}
              >
                <HiPlus /> Create API Route
              </Button>
            ) : (
              <OverlayTrigger
                placement="left"
                overlay={
                  <Tooltip id="create-route-tooltip">
                    {!currentTeam 
                      ? "Please select a team first to create an API route"
                      : "Create new API route"
                    }
                  </Tooltip>
                }
              >
                <span className="d-inline-block">
                  <Button 
                    variant="primary" 
                    onClick={() => setShowCreateModal(true)}
                    disabled={!currentTeam && !hasAdminAccess(user)}
                  >
                    <HiPlus /> Create API Route
                  </Button>
                </span>
              </OverlayTrigger>
            )
          )}
        </Card.Header>
      </Card>

      {routes.length === 0 ? (
        <div className="no-routes-message text-center py-5">
          <h4>No API Routes Found</h4>
          <p className="text-muted">
            {currentTeam 
              ? `No API routes have been created for ${currentTeam.name} yet.`
              : 'No API routes have been created yet.'}
          </p>
          {canManageRoutes && (
            <Button 
              variant="primary" 
              onClick={() => setShowCreateModal(true)}
              className="mt-3"
            >
              <HiPlus /> Create Your First API Route
            </Button>
          )}
        </div>
      ) : (
        <div className="routes-grid">
          {routes.map((route) => (
            <div 
              key={route.id}
              className="route-card"
              onClick={() => navigate(`/api-routes/${route.routeIdentifier}`)}
            >
              <div className="route-header">
                <div className="route-title">
                  <h3>{route.routeIdentifier}</h3>
                  <div className="method-badges">
                    {route.method.split(',').map((method, idx) => (
                      <span key={idx} className={`method-badge ${method.trim().toLowerCase()}`}>
                        {method.trim()}
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              <div className="route-breadcrumb-container">
                <Breadcrumb className="mb-0 p-0">
                  <Breadcrumb.Item
                    linkAs={Link}
                    linkProps={{ to: `/organizations/${route.team?.organizationId}` }}
                  >
                    {route.team?.organizationName || 'Organization'}
                  </Breadcrumb.Item>
                  <Breadcrumb.Item
                    linkAs={Link}
                    linkProps={{ to: `/teams/${route.team?.teamId}` }}
                  >
                    {route.team?.teamName || 'Team'}
                  </Breadcrumb.Item>
                  <Breadcrumb.Item active>
                    {route.routeIdentifier}
                  </Breadcrumb.Item>
                </Breadcrumb>
              </div>

              <div className="route-details" style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', padding: '0', width: '100%' }}>
                {/* First Row: PATH and VERSION */}
                <div className="detail-row" style={{ 
                  display: 'flex', 
                  flexDirection: 'row', 
                  justifyContent: 'space-between',
                  width: '100%'
                }}>
                  <div className="detail-item path-detail" style={{ width: '48%' }}>
                    <span className="detail-label">ROUTE IDENTIFIER</span>
                    <code className="detail-value">{route.routeIdentifier}</code>
                  </div>
                  <div className="detail-item path-detail" style={{ width: '48%' }}>
                    <span className="detail-label">PATH</span>
                    <code className="detail-value">{route.path}</code>
                  </div>
                </div>

                {/* Second Row: DAILY LIMIT and HEALTH CHECK */}
                <div className="detail-row" style={{ 
                  display: 'flex', 
                  flexDirection: 'row', 
                  justifyContent: 'space-between',
                  width: '100%'
                }}>
                  <div className="detail-item" style={{ width: '48%' }}>
                    <span className="detail-label">ROUTE VERSION</span>
                    <code className="detail-value">v{route.version}</code>
                  </div>
                  <div className="detail-item" style={{ width: '48%' }}>
                    <span className="detail-label">DAILY LIMIT</span>
                    <code className="detail-value">
                      {route.maxCallsPerDay?.toLocaleString() || 'Unlimited'} calls/day
                    </code>
                  </div>
                </div>

                {/* Third Row: ASSIGNED BY and ROLES */}
                <div className="detail-row" style={{ 
                  display: 'flex', 
                  flexDirection: 'row', 
                  justifyContent: 'space-between',
                  width: '100%'
                }}>
                  <div className="detail-item" style={{ width: '48%' }}>
                    <span className="detail-label">HEALTH CHECK</span>
                    <code className="detail-value health-check">
                      {route.healthCheckEnabled ? 'ENABLED' : 'DISABLED'}
                    </code>
                  </div>
                  <div className="detail-item" style={{ width: '48%' }}>
                    <span className="detail-label">ASSIGNED BY</span>
                    <code className="detail-value assigned-by">
                      {route.assignedBy || 'System'}
                    </code>
                  </div>
                  
                </div>
              </div>

              <div className="permission-tags">
                      {route.permissions.map((permission, index) => (
                        <OverlayTrigger
                          key={index}
                          placement="top"
                          overlay={
                            <Tooltip id={`permission-${permission}-${index}`}>
                              {PERMISSION_INFO[permission]?.description || permission}
                            </Tooltip>
                          }
                        >
                          <span 
                            className={`permission-badge ${permission.toLowerCase()}`}
                            style={{ backgroundColor: PERMISSION_INFO[permission]?.color }}
                          >
                            {permission}
                          </span>
                        </OverlayTrigger>
                      ))}
                    </div>

              <div className="route-filters">
                <div className="filters-header">
                  <span>Resiliency Filters</span>
                </div>
                <div className="filter-tags">
                  {route.filters?.map((filter, index) => (
                    <OverlayTrigger
                      key={index}
                      placement="top"
                      overlay={
                        <Tooltip id={`filter-${filter.name}-${index}`}>
                          {FILTER_INFO[filter.name]?.description || filter.name}
                          <br />
                          <small>Click to view configuration</small>
                        </Tooltip>
                      }
                    >
                      <div 
                        className="filter-tag" 
                        title={JSON.stringify(filter.args, null, 2)}
                      >
                        {FILTER_INFO[filter.name]?.icon}
                        <span>{filter.name}</span>
                      </div>
                    </OverlayTrigger>
                  ))}
                </div>
              </div>

              <div className="route-actions">
                {(canManageRoutes && route.permissions.includes('MANAGE')) && (
                  <Button 
                    variant="danger"
                    size="sm"
                    onClick={(e) => handleDelete(route.routeIdentifier, e)}
                    className="delete-button"
                  >
                    <HiTrash /> Delete
                  </Button>
                )}
                <Button 
                  variant="secondary"
                  size="sm"
                  onClick={(e) => {
                    e.stopPropagation();
                    navigate(`/api-routes/${route.routeIdentifier}`);
                  }}
                  className="view-button"
                >
                  View Details
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      <CreateApiRouteModal
        show={showCreateModal}
        onHide={() => setShowCreateModal(false)}
        onSubmit={handleCreateRoute}
      />

      <ConfirmationModal
        show={showDeleteConfirm}
        onHide={() => setShowDeleteConfirm(false)}
        onConfirm={handleDeleteConfirm}
        title="Delete Route"
        message={
          "This action will permanently delete the route.\n\n" +
          "Before proceeding, please ensure the route is not assigned to any teams. " +
          "This action cannot be undone."
        }
        confirmLabel="Delete"
        variant="danger"
      />
    </div>
  );
}; 