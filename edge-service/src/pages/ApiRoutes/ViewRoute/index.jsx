import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { LoadingSpinner } from '../../../components/common/LoadingSpinner';
import { apiRouteService } from '../../../services/apiRouteService';
import ViewJsonApiRouteModal from '../../../components/apiroutes/ViewJsonApiRouteModal';
import VersionControl from '../../../components/apiroutes/VersionControl';
import { OverlayTrigger, Tooltip } from 'react-bootstrap';
import { HiQuestionMarkCircle } from 'react-icons/hi';
import './styles.css';
import _ from 'lodash';
import { showSuccessToast, showErrorToast } from '../../../utils/toastConfig';
import { useAuth } from '../../../contexts/AuthContext';

const ViewRoute = () => {
  const [route, setRoute] = useState(null);
  const [currentVersion, setCurrentVersion] = useState(null);
  const [originalRoute, setOriginalRoute] = useState(null);
  const [hasChanges, setHasChanges] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showJsonModal, setShowJsonModal] = useState(false);
  const { routeId } = useParams();
  const navigate = useNavigate();
  const { checkAuth } = useAuth();

  useEffect(() => {
    const fetchRouteDetails = async () => {
      try {
        const data = await apiRouteService.getRouteByIdentifier(routeId);
        setRoute(data);
        setOriginalRoute(data);
      } catch (err) {
        setError('Failed to fetch route details: ' + err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchRouteDetails();
  }, [routeId]);

  const handleVersionChange = (newVersion) => {
    setRoute(newVersion);
    setCurrentVersion(newVersion.version);
    setHasChanges(true);
  };

  const handleCancelChanges = () => {
    setRoute(originalRoute);
    setCurrentVersion(null);
    setHasChanges(false);
  };

  const handleSaveVersion = async () => {
    try {
      await apiRouteService.updateRoute(route.routeIdentifier, route);
      setOriginalRoute(route);
      setHasChanges(false);
      showSuccessToast(`Version ${currentVersion} has been saved as the new version for route ${route.routeIdentifier}`);
      
      setTimeout(async () => {
        if (checkAuth()) {
          window.location.reload();
        } else {
          showErrorToast('Your session has expired. Please log in again.');
        }
      }, 1500);

    } catch (error) {
      console.error('Failed to save version:', error);
      showErrorToast(`Failed to save version: ${error.message}`);
    }
  };

  const isFieldChanged = (newValue, oldValue) => {
    try {
      if (typeof newValue !== typeof oldValue) return true;
      if (newValue === null || oldValue === null) return newValue !== oldValue;
      if (Array.isArray(newValue)) {
        if (!Array.isArray(oldValue) || newValue.length !== oldValue.length) return true;
        return JSON.stringify(newValue) !== JSON.stringify(oldValue);
      }
      if (typeof newValue === 'object') {
        return JSON.stringify(newValue) !== JSON.stringify(oldValue);
      }
      return newValue !== oldValue;
    } catch (error) {
      console.error('Error comparing fields:', error);
      return false;
    }
  };

  const getFieldHighlight = (fieldPath) => {
    if (!originalRoute || !hasChanges) return '';
    const newValue = _.get(route, fieldPath);
    const oldValue = _.get(originalRoute, fieldPath);
    return isFieldChanged(newValue, oldValue) ? {
      changed: true,
      oldValue: oldValue,
      newValue: newValue
    } : { changed: false };
  };

  const renderFieldValue = (fieldPath, currentValue, formatter = (v) => v) => {
    const highlight = getFieldHighlight(fieldPath);
    if (!highlight.changed) {
      return <span>{formatter(currentValue)}</span>;
    }
    
    return (
      <div className="field-value-container field-changed">
        <span className="new-value">{formatter(currentValue)}</span>
        <div className="old-value-tooltip">
          <div className="tooltip-content">
            <div className="value-comparison">
              <div className="old-value">
                <label>Old:</label> {formatter(highlight.oldValue)}
              </div>
              <div className="new-value">
                <label>New:</label> {formatter(highlight.newValue)}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <div className="error-message">{error}</div>;
  if (!route) return <div className="error-message">Route not found</div>;

  const formatDate = (timestamp) => {
    return new Date(Number(timestamp)).toLocaleString();
  };

  const renderTooltip = (content) => (
    <Tooltip>{content}</Tooltip>
  );

  const QuestionMarkIcon = ({ tooltip }) => (
    <OverlayTrigger
      placement="right"
      delay={{ show: 250, hide: 400 }}
      overlay={renderTooltip(tooltip)}
    >
      <span className="question-mark-icon">
        <HiQuestionMarkCircle />
      </span>
    </OverlayTrigger>
  );

  return (
    <div className="view-route-page">
      <div className="page-header">
        <div className="header-section">
          <div className="header-content">
            <h1>Route Details</h1>
            <div className="header-actions">
              <button className="back-button" onClick={() => navigate('/api-routes')}>
                <i className="fas fa-arrow-left"></i> Back to Routes
              </button>
              <button 
                className="edit-button" 
                onClick={() => navigate(`/api-routes/${route.routeIdentifier}/edit`)}
              >
                <i className="fas fa-edit"></i> Edit Route
              </button>
              <button 
                className="json-view-button" 
                onClick={() => setShowJsonModal(true)}
              >
                <i className="fas fa-code"></i> View JSON
              </button>
              {hasChanges && (
                <>
                  <button 
                    className="cancel-version-button" 
                    onClick={handleCancelChanges}
                  >
                    <i className="fas fa-times"></i> Cancel Changes
                  </button>
                  <button 
                    className="save-version-button" 
                    onClick={handleSaveVersion}
                  >
                    <i className="fas fa-save"></i> Save Version {currentVersion}
                  </button>
                </>
              )}
            </div>
          </div>
          <div className="version-section">
            <VersionControl
              routeIdentifier={route.routeIdentifier}
              currentVersion={currentVersion}
              onVersionChange={handleVersionChange}
            />
          </div>
        </div>
      </div>

      <div className={`route-details-container ${hasChanges ? 'version-preview' : ''}`}>
        <div className="route-info-section">
          <div className="section-header">
            <h2>Basic Information</h2>
            <QuestionMarkIcon tooltip="Basic configuration details for the API route" />
          </div>
          <h2>{route.routeIdentifier}</h2>
          <div className="route-info-grid">
            <div className={`info-item ${getFieldHighlight('method')}`}>
              <div className="label-with-icon">
                <label>Method</label>
                <QuestionMarkIcon tooltip="HTTP method for this route (GET, POST, etc.)" />
              </div>
              {renderFieldValue('method', route.method || 'ALL', value => (
                <code>{value}</code>
              ))}
            </div>
            <div className={`info-item ${getFieldHighlight('path')}`}>
              <div className="label-with-icon">
                <label>Path</label>
                <QuestionMarkIcon tooltip="URL path pattern for this route" />
              </div>
              {renderFieldValue('path', route.path, value => (
                <code>{value}</code>
              ))}
            </div>
            <div className={`info-item ${getFieldHighlight('uri')}`}>
              <div className="label-with-icon">
                <label>URI</label>
                <QuestionMarkIcon tooltip="Target service URI (e.g., lb://service-name)" />
              </div>
              {renderFieldValue('uri', route.uri, value => (
                <code>{value}</code>
              ))}
            </div>
            <div className={`info-item ${getFieldHighlight('scope')}`}>
              <div className="label-with-icon">
                <label>Scope</label>
                <QuestionMarkIcon tooltip="OAuth2 scope required to access this route" />
              </div>
              {renderFieldValue('scope', route.scope, value => (
                <code>{value}</code>
              ))}
            </div>
            <div className={`info-item ${getFieldHighlight('maxCallsPerDay')}`}>
              <div className="label-with-icon">
                <label>Max Calls Per Day</label>
                <QuestionMarkIcon tooltip="Maximum number of API calls allowed per day" />
              </div>
              {renderFieldValue('maxCallsPerDay', route.maxCallsPerDay?.toLocaleString(), value => (
                <span>{value}</span>
              ))}
            </div>
            <div className={`info-item ${getFieldHighlight('createdAt')}`}>
              <div className="label-with-icon">
                <label>Created At</label>
                <QuestionMarkIcon tooltip="When this route was first created" />
              </div>
              {renderFieldValue('createdAt', formatDate(route.createdAt), value => (
                <span>{value}</span>
              ))}
            </div>
            <div className={`info-item ${getFieldHighlight('updatedAt')}`}>
              <div className="label-with-icon">
                <label>Last Updated</label>
                <QuestionMarkIcon tooltip="When this route was last modified" />
              </div>
              {renderFieldValue('updatedAt', formatDate(route.updatedAt), value => (
                <span>{value}</span>
              ))}
            </div>
            <div className={`info-item ${getFieldHighlight('routeIdentifier')}`}>
              <div className="label-with-icon">
                <label>Route Identifier</label>
                <QuestionMarkIcon tooltip="Unique identifier for this route" />
              </div>
              {renderFieldValue('routeIdentifier', route.routeIdentifier, value => (
                <span>{value}</span>
              ))}
            </div>
          </div>
        </div>

        <div className="filters-section">
          <div className="section-header">
            <h2>Resiliency Filters</h2>
            <QuestionMarkIcon tooltip="Filters that handle rate limiting, timeouts, circuit breaking, and retries" />
          </div>
          <div className="filters-grid">
            {route.filters?.map((filter, index) => (
              <div key={index} className={`filter-detail-card ${getFieldHighlight(`filters[${index}]`)}`}>
                <div className="filter-header">
                  <i className={`fas fa-${getFilterIcon(filter.name)}`}></i>
                  <div className="filter-title-with-icon">
                    <h4>{filter.name}</h4>
                    <QuestionMarkIcon tooltip={getFilterTitleTooltip(filter.name)} />
                  </div>
                </div>
                <div className="filter-properties">
                  {Object.entries(filter.args || {}).map(([key, value]) => (
                    <div key={key} className="property-item">
                      <div className="property-key-with-icon">
                        <span className="property-key">{key}:</span>
                        {filter.name === 'RedisRateLimiter' && (
                          <QuestionMarkIcon tooltip={getRateLimiterTooltip(key)} />
                        )}
                        {filter.name === 'TimeLimiter' && (
                          <QuestionMarkIcon tooltip={getTimeLimiterTooltip(key)} />
                        )}
                        {filter.name === 'CircuitBreaker' && (
                          <QuestionMarkIcon tooltip={getCircuitBreakerTooltip(key)} />
                        )}
                        {filter.name === 'Retry' && (
                          <QuestionMarkIcon tooltip={getRetryTooltip(key)} />
                        )}
                      </div>
                      {renderFieldValue(`filters[${index}].args.${key}`, value, value => (
                        <span className="property-value">{value}</span>
                      ))}
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="health-check-section">
          <div className="section-header">
            <h2>Health Check Configuration</h2>
            <QuestionMarkIcon tooltip="Settings for monitoring the health and performance of the API" />
          </div>
          {route.healthCheck && (
            <div className="health-check-content">
              <div className="health-check-status-section">
                <div className="section-header">
                  <h4>Status</h4>
                  <QuestionMarkIcon tooltip="Current state of health check monitoring for this route" />
                </div>
                <div className="health-check-status">
                  <span className={`status-badge ${route.healthCheck.enabled ? 'enabled' : 'disabled'}`}>
                    {route.healthCheck.enabled ? 'Enabled' : 'Disabled'}
                  </span>
                  {renderFieldValue('healthCheck.endpoint', route.healthCheck.endpoint, value => (
                    <code className="endpoint">{value}</code>
                  ))}
                </div>
              </div>
              
              <div className="metrics-section">
                <div className="section-header">
                  <h4>Required Metrics</h4>
                  <QuestionMarkIcon tooltip="Metrics that must be reported by the service for health monitoring" />
                </div>
                <div className="metrics-grid">
                  {route.healthCheck.requiredMetrics?.map((metric, index) => (
                    <span key={index} className="metric-badge">{metric}</span>
                  ))}
                </div>
              </div>

              <div className="thresholds-section">
                <div className="section-header">
                  <h4>Thresholds</h4>
                  <QuestionMarkIcon tooltip="Performance thresholds that trigger alerts when exceeded" />
                </div>
                <div className="thresholds-grid">
                  {route.healthCheck.thresholds && Object.entries(route.healthCheck.thresholds).map(([key, value]) => (
                    <div key={key} className={`threshold-item ${getFieldHighlight(`healthCheck.thresholds.${key}`)}`}>
                      <div className="threshold-key-with-icon">
                        <span className="threshold-key">{key}</span>
                        <QuestionMarkIcon tooltip={getThresholdTooltip(key)} />
                      </div>
                      {renderFieldValue(`healthCheck.thresholds.${key}`, value, value => (
                        <span className="threshold-value">
                          {typeof value === 'object' ? value.$numberLong : value}
                          {key.includes('cpu') || key.includes('memory') ? '%' : 'ms'}
                        </span>
                      ))}
                    </div>
                  ))}
                </div>
              </div>

              <div className="alert-rules-section">
                <div className="section-header">
                  <h4>Alert Rules</h4>
                  <QuestionMarkIcon tooltip="Rules that define when and how alerts are triggered based on metrics" />
                </div>
                <div className="alert-rules-grid">
                  {route.healthCheck.alertRules?.map((rule, index) => (
                    <div key={index} className={`alert-rule-card ${getFieldHighlight(`healthCheck.alertRules[${index}]`)}`}>
                      <div className="alert-rule-header">
                        <span className={`severity-badge ${rule.severity.toLowerCase()}`}>
                          {rule.severity}
                        </span>
                      </div>
                      <div className="alert-rule-content">
                        <p className="rule-description">{rule.description}</p>
                        <div className="rule-condition">
                          <span className="metric">{rule.metric}</span>
                          <span className="condition">{rule.condition}</span>
                          <span className="threshold">{rule.threshold}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      <ViewJsonApiRouteModal
        show={showJsonModal}
        onHide={() => setShowJsonModal(false)}
        route={route}
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

const getRateLimiterTooltip = (key) => {
  const tooltips = {
    'requestedTokens': 'Number of tokens that each request costs',
    'replenishRate': 'Number of tokens replenished per second',
    'burstCapacity': 'Maximum number of tokens that can be consumed at once'
  };
  return tooltips[key] || key;
};

const getTimeLimiterTooltip = (key) => {
  const tooltips = {
    'timeoutDuration': 'Maximum amount of time (in seconds) to wait for a response',
    'cancelRunningFuture': 'Whether to cancel the running task when timeout occurs'
  };
  return tooltips[key] || key;
};

const getCircuitBreakerTooltip = (key) => {
  const tooltips = {
    'fallbackUri': 'URI to redirect to when the circuit breaker is open',
    'name': 'Unique identifier for this circuit breaker instance',
    'recordFailurePredicate': 'Predicate to determine what should count as a failure',
    'slidingWindowSize': 'Number of calls to use when calculating failure rate',
    'failureRateThreshold': 'Percentage of failures required to open the circuit',
    'permittedNumberOfCallsInHalfOpenState': 'Number of calls allowed in half-open state',
    'automaticTransitionFromOpenToHalfOpenEnabled': 'Whether to automatically transition from open to half-open',
    'waitDurationInOpenState': 'Time to wait before transitioning from open to half-open state'
  };
  return tooltips[key] || key;
};

const getRetryTooltip = (key) => {
  const tooltips = {
    'waitDuration': 'Time to wait between retry attempts',
    'maxAttempts': 'Maximum number of retry attempts',
    'retryExceptions': 'List of exceptions that should trigger a retry'
  };
  return tooltips[key] || key;
};

const getThresholdTooltip = (key) => {
  const tooltips = {
    'cpuThreshold': 'Maximum CPU usage percentage before triggering alerts',
    'memoryThreshold': 'Maximum memory usage percentage before triggering alerts',
    'responseTimeThreshold': 'Maximum response time in milliseconds before triggering alerts',
    'timeoutThreshold': 'Maximum time in milliseconds before considering a request timed out'
  };
  return tooltips[key] || key;
};

const getFilterTitleTooltip = (filterName) => {
  const tooltips = {
    'RedisRateLimiter': 'Controls the rate of requests using Redis as a token bucket store',
    'TimeLimiter': 'Sets timeout duration for requests to prevent long-running operations',
    'CircuitBreaker': 'Prevents cascading failures by stopping requests when error threshold is exceeded',
    'Retry': 'Automatically retries failed requests based on configured conditions'
  };
  return tooltips[filterName] || filterName;
};

export default ViewRoute; 