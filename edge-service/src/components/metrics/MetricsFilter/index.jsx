import { useState } from 'react';
import { getDefaultDateRange } from '../../../utils/dateUtils';
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
    
    // Set end to start of next day (00:00)
    end.setDate(end.getDate() + 1);
    end.setHours(0, 0, 0, 0);

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
      endDate: end.toISOString().split('T')[0]
    };
    setFilters(newFilters);
    const processedFilters = {
      ...newFilters,
      startDate: start.getTime(),
      endDate: end.getTime()
    };
    onFilterChange(processedFilters);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const startDateTime = new Date(`${filters.startDate}T${filters.startTime}`).getTime();
    const endDateTime = new Date(`${filters.endDate}T${filters.endTime}`).getTime();

    if (startDateTime > endDateTime) {
      // Could add error handling here if needed
      return;
    }

    const processedFilters = {
      ...filters,
      startDate: startDateTime,
      endDate: endDateTime
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
      endDate: defaultDates.endDate.toISOString().split('T')[0]
    };
    setFilters(clearedFilters);
    onFilterChange(clearedFilters);
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
            <button type="submit" className="apply-button">Apply Filters</button>
            <button type="button" onClick={clearFilters} className="clear-button">Clear</button>
          </div>
        </div>
      </form>
    </div>
  );
}

export default MetricsFilter; 