import { useState } from 'react';
import Button from '../../common/Button';
import { getDefaultDateRange, formatDateForApi } from '../../../utils/dateUtils';
import './styles.css';

function MetricsFilter({ onFilterChange, services }) {
  const [filters, setFilters] = useState({
    fromService: '',
    toService: '',
    startDate: getDefaultDateRange().startDate.toISOString().split('T')[0],
    startTime: '00:00',
    endDate: getDefaultDateRange().endDate.toISOString().split('T')[0],
    endTime: '23:59'
  });

  const handleQuickFilter = (period) => {
    const end = new Date();
    const start = new Date();
    
    // Set end to end of current day (23:59:59.999)
    end.setHours(23, 59, 59, 999);

    switch (period) {
      case 'today':
        start.setHours(0, 0, 0, 0);
        break;
      case 'week':
        start.setDate(start.getDate() - 6);
        start.setHours(0, 0, 0, 0);
        break;
      case 'month':
        start.setDate(start.getDate() - 29);
        start.setHours(0, 0, 0, 0);
        break;
      default:
        break;
    }

    const newFilters = {
      ...filters,
      startDate: start.toISOString().split('T')[0],
      startTime: '00:00',
      endDate: end.toISOString().split('T')[0],
      endTime: '23:59'
    };
    setFilters(newFilters);
    const processedFilters = {
      ...newFilters,
      startDate: formatDateForApi(start),
      endDate: formatDateForApi(end)
    };
    onFilterChange(processedFilters);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const startDateTime = new Date(`${filters.startDate}T${filters.startTime}`);
    const endDateTime = new Date(`${filters.endDate}T${filters.endTime}`);

    if (startDateTime > endDateTime) {
      return;
    }

    const processedFilters = {
      ...filters,
      startDate: formatDateForApi(startDateTime),
      endDate: formatDateForApi(endDateTime)
    };
    onFilterChange(processedFilters);
  };

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const clearFilters = () => {
    const defaultDates = getDefaultDateRange();
    const clearedFilters = {
      fromService: '',
      toService: '',
      startDate: defaultDates.startDate.toISOString().split('T')[0],
      startTime: '00:00',
      endDate: defaultDates.endDate.toISOString().split('T')[0],
      endTime: '23:59'
    };
    setFilters(clearedFilters);
    onFilterChange({
      ...clearedFilters,
      startDate: defaultDates.startDate.getTime(),
      endDate: defaultDates.endDate.getTime()
    });
  };

  return (
    <div className="metrics-filter">
      <form onSubmit={handleSubmit} className="filter-content">
        <div className="filters-row">
          <div className="filter-group">
            <label htmlFor="fromService">From Service:</label>
            <select 
              id="fromService"
              name="fromService" 
              value={filters.fromService}
              onChange={handleFilterChange}
            >
              <option value="">All Services</option>
              {services.map(service => (
                <option key={service} value={service}>{service}</option>
              ))}
            </select>
          </div>
          <div className="filter-group">
            <label htmlFor="toService">To Service:</label>
            <select 
              id="toService"
              name="toService" 
              value={filters.toService}
              onChange={handleFilterChange}
            >
              <option value="">All Services</option>
              {services.map(service => (
                <option key={service} value={service}>{service}</option>
              ))}
            </select>
          </div>
          <div className="filter-group date-time-group">
            <label htmlFor="startDate">From:</label>
            <div className="date-time-inputs">
              <input
                type="date"
                id="startDate"
                name="startDate"
                value={filters.startDate}
                onChange={handleFilterChange}
              />
              <input
                type="time"
                id="startTime"
                name="startTime"
                value={filters.startTime}
                onChange={handleFilterChange}
              />
            </div>
          </div>
          <div className="filter-group date-time-group">
            <label htmlFor="endDate">To:</label>
            <div className="date-time-inputs">
              <input
                type="date"
                id="endDate"
                name="endDate"
                value={filters.endDate}
                onChange={handleFilterChange}
              />
              <input
                type="time"
                id="endTime"
                name="endTime"
                value={filters.endTime}
                onChange={handleFilterChange}
              />
            </div>
          </div>
        </div>
        <div className="filter-actions">
          <div className="quick-filters">
            <button type="button" onClick={() => handleQuickFilter('today')}>Today</button>
            <button type="button" onClick={() => handleQuickFilter('week')}>Last 7 Days</button>
            <button type="button" onClick={() => handleQuickFilter('month')}>Last 30 Days</button>
          </div>
          <div className="action-buttons">
            <Button 
              type="submit"
              variant="primary"
            >
              Apply Filters
            </Button>
            <Button 
              type="button"
              variant="secondary"
              onClick={clearFilters}
            >
              Clear
            </Button>
          </div>
        </div>
      </form>
    </div>
  );
}

export default MetricsFilter; 