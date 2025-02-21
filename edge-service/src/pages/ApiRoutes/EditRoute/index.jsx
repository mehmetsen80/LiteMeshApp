import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Select from 'react-select';
import { LoadingSpinner } from '../../../components/common/LoadingSpinner';
import { apiRouteService } from '../../../services/apiRouteService';
import { Tooltip } from 'react-tooltip';
import './styles.css';

const isBooleanField = (fieldName) => {
  const booleanFields = [
    'cancelRunningFuture',
    'automaticTransitionFromOpenToHalfOpenEnabled'
  ];
  return booleanFields.includes(fieldName);
};

const isNumericField = (fieldName) => {
  const numericFields = [
    'replenishRate',
    'burstCapacity',
    'requestedTokens',
    'timeoutDuration',
    'slidingWindowSize',
    'failureRateThreshold',
    'permittedNumberOfCallsInHalfOpenState',
    'maxAttempts'
  ];
  return numericFields.includes(fieldName);
};

const methodOptions = [
  { value: 'GET', label: 'GET' },
  { value: 'POST', label: 'POST' },
  { value: 'PUT', label: 'PUT' },
  { value: 'DELETE', label: 'DELETE' }
];

const fieldDescriptions = {
  routeIdentifier: "A unique identifier for the route. This cannot be modified after creation as it's used as a primary reference.",
  uri: "The destination URI where requests will be forwarded. This is a core routing property and cannot be modified after creation.",
  method: "HTTP methods that this route will accept. Multiple methods can be selected.",
  path: "The URL path pattern to match incoming requests. This is a core routing property and cannot be modified after creation.",
  scope: "Security scope required to access this route. Used for authorization. Configured in the Identity Provider such as Keycloak.",
  maxCallsPerDay: "Maximum number of API calls allowed per day for this route."
};

const filterDescriptions = {
  CircuitBreaker: "Implements the Circuit Breaker pattern to prevent cascading failures. Opens the circuit when error thresholds are exceeded.",
  RedisRateLimiter: "Controls the rate of requests using Redis as a token bucket store. Helps prevent service overload.",
  TimeLimiter: "Sets timeout duration for requests to prevent long-running operations from blocking resources.",
  Retry: "Automatically retries failed requests based on configured conditions and attempts.",
  name: "The unique identifier for this circuit breaker. Should be an alphanumeric name representing the protected service.",
  failureRateThreshold: "Percentage of failures required to open the circuit.",
  slidingWindowSize: "Number of requests in the sliding window for failure rate calculation.",
  permittedNumberOfCallsInHalfOpenState: "Number of test requests allowed when circuit is half-open.",
  automaticTransitionFromOpenToHalfOpenEnabled: "Whether to automatically transition from open to half-open state.",
  waitDurationInOpenState: "Duration the CircuitBreaker should wait before transitioning from open to half-open.",
  fallbackUri: "The URI to redirect to when the circuit breaks. Format: forward:/fallback/{serviceName}",
  replenishRate: "How many tokens per second to add to the bucket.",
  burstCapacity: "Maximum number of tokens the bucket can hold.",
  requestedTokens: "Number of tokens to withdraw per request.",
  timeoutDuration: "Maximum time to wait for a response before timing out.",
  cancelRunningFuture: "Whether to cancel the running future when timeout occurs.",
  maxAttempts: "Maximum number of retry attempts for a failed request.",
  waitDuration: "Duration to wait between retries. Example: PT2S (2 seconds in ISO-8601 duration format)"
};

const EditRoute = () => {
  const { routeId } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [route, setRoute] = useState(null);
  const [formData, setFormData] = useState({
    routeIdentifier: '',
    uri: '',
    method: '',
    path: '',
    scope: '',
    maxCallsPerDay: 0,
    filters: [],
    healthCheck: {
      enabled: true,
      endpoint: '/health',
      requiredMetrics: [],
      thresholds: {},
      alertRules: []
    }
  });

  useEffect(() => {
    const fetchRouteDetails = async () => {
      try {
        const data = await apiRouteService.getRouteByIdentifier(routeId);
        setRoute(data);
        setFormData(data);
      } catch (err) {
        setError('Failed to fetch route details: ' + err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchRouteDetails();
  }, [routeId]);

  const handleBasicInfoChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleMethodChange = (selectedOptions) => {
    setFormData(prev => ({
      ...prev,
      method: selectedOptions.map(option => option.value).join(',')
    }));
  };

  const handleFilterChange = (index, field, value) => {
    const updatedFilters = [...formData.filters];
    if (field.includes('.')) {
      const [argName, argField] = field.split('.');
      updatedFilters[index].args = {
        ...updatedFilters[index].args,
        [argField]: value
      };
    } else {
      updatedFilters[index][field] = value;
    }
    setFormData(prev => ({
      ...prev,
      filters: updatedFilters
    }));
  };

  const handleHealthCheckChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      healthCheck: {
        ...prev.healthCheck,
        [field]: value
      }
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setLoading(true);
      await apiRouteService.updateRoute(routeId, formData);
      navigate(`/api-routes/${routeId}`);
    } catch (err) {
      setError('Failed to update route: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <div className="error-message">{error}</div>;
  if (!route) return <div className="error-message">Route not found</div>;

  return (
    <div className="edit-route-page">
      <div className="page-header">
        <div className="header-content">
          <h1>Edit Route</h1>
          <button className="back-button" onClick={() => navigate(`/api-routes/${routeId}`)}>
            <i className="fas fa-arrow-left"></i> Back to Details
          </button>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="edit-form">
        {/* Basic Information Section */}
        <div className="form-section">
          <h2>Basic Information</h2>
          <div className="form-grid">
            <div className="form-group">
              <div className="label-with-info">
                <label>Route Identifier</label>
                <i 
                  className="fas fa-question-circle info-icon"
                  data-tooltip-id="route-identifier-tooltip"
                  data-tooltip-content={fieldDescriptions.routeIdentifier}
                />
                <Tooltip id="route-identifier-tooltip" />
              </div>
              <input
                type="text"
                name="routeIdentifier"
                value={formData.routeIdentifier}
                readOnly
                className="readonly-input"
                title="Route Identifier cannot be modified after creation"
                required
              />
            </div>
            <div className="form-group">
              <div className="label-with-info">
                <label>URI</label>
                <i 
                  className="fas fa-question-circle info-icon"
                  data-tooltip-id="uri-tooltip"
                  data-tooltip-content={fieldDescriptions.uri}
                />
                <Tooltip id="uri-tooltip" />
              </div>
              <input
                type="text"
                name="uri"
                value={formData.uri}
                readOnly
                className="readonly-input"
                title="URI cannot be modified after creation"
                required
              />
            </div>
            <div className="form-group">
              <div className="label-with-info">
                <label>Method</label>
                <i 
                  className="fas fa-question-circle info-icon"
                  data-tooltip-id="method-tooltip"
                  data-tooltip-content={fieldDescriptions.method}
                />
                <Tooltip id="method-tooltip" />
              </div>
              <Select
                isMulti
                name="method"
                options={methodOptions}
                value={formData.method ? 
                  formData.method.split(',').map(m => ({ value: m, label: m })) : 
                  []
                }
                onChange={handleMethodChange}
                className="react-select-container"
                classNamePrefix="react-select"
              />
            </div>
            <div className="form-group">
              <div className="label-with-info">
                <label>Path</label>
                <i 
                  className="fas fa-question-circle info-icon"
                  data-tooltip-id="path-tooltip"
                  data-tooltip-content={fieldDescriptions.path}
                />
                <Tooltip id="path-tooltip" />
              </div>
              <input
                type="text"
                name="path"
                value={formData.path}
                readOnly
                className="readonly-input"
                title="Path cannot be modified after creation"
                required
              />
            </div>
            <div className="form-group">
              <div className="label-with-info">
                <label>Scope</label>
                <i 
                  className="fas fa-question-circle info-icon"
                  data-tooltip-id="scope-tooltip"
                  data-tooltip-content={fieldDescriptions.scope}
                />
                <Tooltip id="scope-tooltip" />
              </div>
              <input
                type="text"
                name="scope"
                value={formData.scope}
                onChange={handleBasicInfoChange}
              />
            </div>
            <div className="form-group">
              <div className="label-with-info">
                <label>Max Calls Per Day</label>
                <i 
                  className="fas fa-question-circle info-icon"
                  data-tooltip-id="max-calls-per-day-tooltip"
                  data-tooltip-content={fieldDescriptions.maxCallsPerDay}
                />
                <Tooltip id="max-calls-per-day-tooltip" />
              </div>
              <input
                type="number"
                name="maxCallsPerDay"
                value={formData.maxCallsPerDay}
                onChange={handleBasicInfoChange}
                min="0"
              />
            </div>
          </div>
        </div>

        {/* Filters Section */}
        <div className="form-section">
          <h2>Resiliency Filters</h2>
          <p className="section-description">
            Resiliency filters provide protection mechanisms like rate limiting, circuit breaking, and retry logic to make your API more resilient.
          </p>
          {formData.filters.map((filter, index) => (
            <div key={index} className="filter-form">
              <div className="filter-header">
                <h3>{filter.name}</h3>
              </div>
              <div className="filter-args-grid">
                {Object.entries(filter.args).map(([key, value]) => (
                  <div key={key} className="form-group">
                    <div className="label-with-info">
                      <label>{key}</label>
                      {!['HttpResponsePredicate', 'recordFailurePredicate'].includes(key) && filterDescriptions[key] && (
                        <>
                          <i 
                            className="fas fa-question-circle info-icon"
                            data-tooltip-id={`filter-arg-${key}-tooltip`}
                            data-tooltip-content={filterDescriptions[key]}
                          />
                          <Tooltip id={`filter-arg-${key}-tooltip`} />
                        </>
                      )}
                    </div>
                    {isBooleanField(key) ? (
                      <select
                        value={value}
                        onChange={(e) => handleFilterChange(index, `args.${key}`, e.target.value)}
                      >
                        <option value="true">true</option>
                        <option value="false">false</option>
                      </select>
                    ) : (
                      <input
                        type={isNumericField(key) ? "number" : "text"}
                        value={value}
                        {...(['HttpResponsePredicate', 'recordFailurePredicate'].includes(key)
                          ? {
                              readOnly: true,
                              className: 'readonly-input',
                              disabled: true
                            }
                          : {
                              onChange: (e) => handleFilterChange(index, `args.${key}`, e.target.value)
                            }
                        )}
                      />
                    )}
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        {/* Health Check Section */}
        <div className="form-section">
          <h2>Health Check Configuration</h2>
          <div className="health-check-form">
            <div className="form-group">
              <label>
                <input
                  type="checkbox"
                  checked={formData.healthCheck.enabled}
                  onChange={(e) => handleHealthCheckChange('enabled', e.target.checked)}
                />
                Enable Health Check
              </label>
            </div>
            
            {formData.healthCheck.enabled && (
              <>
                <div className="form-group">
                  <label>Health Check Endpoint</label>
                  <input
                    type="text"
                    value={formData.healthCheck.endpoint}
                    onChange={(e) => handleHealthCheckChange('endpoint', e.target.value)}
                  />
                </div>

                {/* Thresholds */}
                <div className="thresholds-section">
                  <h3>Thresholds</h3>
                  <div className="thresholds-grid">
                    {Object.entries(formData.healthCheck.thresholds).map(([key, value]) => (
                      <div key={key} className="form-group">
                        <label>{key}</label>
                        <input
                          type="number"
                          value={typeof value === 'object' ? value.$numberLong : value}
                          onChange={(e) => {
                            const newThresholds = { ...formData.healthCheck.thresholds };
                            if (key.includes('Threshold') && !key.includes('cpu') && !key.includes('memory')) {
                              newThresholds[key] = { $numberLong: e.target.value };
                            } else {
                              newThresholds[key] = parseInt(e.target.value);
                            }
                            handleHealthCheckChange('thresholds', newThresholds);
                          }}
                        />
                      </div>
                    ))}
                  </div>
                </div>

                {/* Alert Rules */}
                <div className="alert-rules-section">
                  <h3>Alert Rules</h3>
                  <div className="alert-rules-grid">
                    {formData.healthCheck.alertRules.map((rule, index) => (
                      <div key={index} className="alert-rule-form">
                        <div className="form-group">
                          <label>Metric</label>
                          <select
                            value={rule.metric}
                            onChange={(e) => {
                              const newRules = [...formData.healthCheck.alertRules];
                              newRules[index] = { ...rule, metric: e.target.value };
                              handleHealthCheckChange('alertRules', newRules);
                            }}
                          >
                            {formData.healthCheck.requiredMetrics.map(metric => (
                              <option key={metric} value={metric}>{metric}</option>
                            ))}
                          </select>
                        </div>
                        <div className="form-group">
                          <label>Condition</label>
                          <select
                            value={rule.condition}
                            onChange={(e) => {
                              const newRules = [...formData.healthCheck.alertRules];
                              newRules[index] = { ...rule, condition: e.target.value };
                              handleHealthCheckChange('alertRules', newRules);
                            }}
                          >
                            <option value=">">&gt;</option>
                            <option value=">=">&gt;=</option>
                            <option value="<">&lt;</option>
                            <option value="<=">&lt;=</option>
                            <option value="==">=</option>
                          </select>
                        </div>
                        <div className="form-group">
                          <label>Threshold</label>
                          <input
                            type="number"
                            value={rule.threshold}
                            onChange={(e) => {
                              const newRules = [...formData.healthCheck.alertRules];
                              newRules[index] = { ...rule, threshold: parseInt(e.target.value) };
                              handleHealthCheckChange('alertRules', newRules);
                            }}
                          />
                        </div>
                        <div className="form-group">
                          <label>Description</label>
                          <input
                            type="text"
                            value={rule.description}
                            onChange={(e) => {
                              const newRules = [...formData.healthCheck.alertRules];
                              newRules[index] = { ...rule, description: e.target.value };
                              handleHealthCheckChange('alertRules', newRules);
                            }}
                          />
                        </div>
                        <div className="form-group">
                          <label>Severity</label>
                          <select
                            value={rule.severity}
                            onChange={(e) => {
                              const newRules = [...formData.healthCheck.alertRules];
                              newRules[index] = { ...rule, severity: e.target.value };
                              handleHealthCheckChange('alertRules', newRules);
                            }}
                          >
                            <option value="WARNING">WARNING</option>
                            <option value="CRITICAL">CRITICAL</option>
                          </select>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </>
            )}
          </div>
        </div>

        <div className="form-actions">
          <button type="button" className="cancel-button" onClick={() => navigate(`/api-routes/${routeId}`)}>
            Cancel
          </button>
          <button type="submit" className="save-button">
            Save Changes
          </button>
        </div>
      </form>
    </div>
  );
};

export default EditRoute; 