import React, { useState, useEffect, useCallback } from 'react';
import { Table, Card, Badge, Breadcrumb, OverlayTrigger, Tooltip } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import { HiEye } from 'react-icons/hi';
import { useTeam } from '../../../contexts/TeamContext';
import { teamService } from '../../../services/teamService';
import { LoadingSpinner } from '../../common/LoadingSpinner';
import Button from '../../common/Button';
import './styles.css';

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

const TeamRoutes = () => {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { currentTeam } = useTeam();
  const navigate = useNavigate();

  // Move the helper functions outside useEffect
  const getMethodBadges = useCallback((method) => {
    if (!method) return [];
    return method.split(',').map(m => m.trim());
  }, []);

  const loadTeamRoutes = useCallback(async () => {
    if (!currentTeam?.id) {
      setLoading(false);
      return;
    }
    
    try {
      setLoading(true);
      const response = await teamService.getTeamRoutes(currentTeam.id);
      if (response.success) {
        setRoutes(response.data || []);
      } else {
        throw new Error(response.error || 'Failed to fetch routes');
      }
    } catch (err) {
      console.error('Error loading team routes:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [currentTeam?.id]);

  useEffect(() => {
    loadTeamRoutes();
  }, [loadTeamRoutes]);

  const handleViewRoute = (routeId) => {
    navigate(`/api-routes/${routeId}`);
  };

  const renderPermissionBadges = (permissions) => {
    if (!permissions || !Array.isArray(permissions)) return null;
    
    return permissions.map((permission, index) => (
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
    ));
  };

  if (error) {
    return (
      <Card className="error-card">
        <Card.Body>
          <p className="text-danger">{error}</p>
          <Button variant="primary" onClick={loadTeamRoutes}>
            Retry
          </Button>
        </Card.Body>
      </Card>
    );
  }

  if (loading) {
    return <LoadingSpinner />;
  }

  return (
    <Card className="team-routes-card">
      <Card.Header className="bg-light">
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
            {currentTeam?.name || 'Team'}
          </Breadcrumb.Item>
          <Breadcrumb.Item active>
            Team API Routes
          </Breadcrumb.Item>
        </Breadcrumb>
      </Card.Header>
      <Card.Body>
        <Table responsive className="team-routes-table">
          <thead>
            <tr>
              <th>Service</th>
              <th>Method</th>
              <th>Path</th>
              <th>Permissions</th>
              <th>View</th>
            </tr>
          </thead>
          <tbody>
            {routes.map(route => (
              <tr key={route.id}>
                <td>{route.routeIdentifier}</td>
                <td>
                  <div className="method-badges">
                    {getMethodBadges(route.method).map(method => (
                      <span key={method} className={`method-badge ${method.toLowerCase()}`}>
                        {method}
                      </span>
                    ))}
                  </div>
                </td>
                <td>{route.path}</td>
                <td>
                  <div className="permission-badges">
                    {renderPermissionBadges(route.permissions)}
                  </div>
                </td>
                <td>
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => handleViewRoute(route.routeIdentifier)}
                    className="view-button"
                  >
                    <HiEye /> View API
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
        {routes.length === 0 && !loading && (
          <div className="no-routes">
            <p>No routes found for this team.</p>
          </div>
        )}
      </Card.Body>
    </Card>
  );
};

export default TeamRoutes; 