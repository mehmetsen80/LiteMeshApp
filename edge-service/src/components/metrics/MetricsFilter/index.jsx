import { useState } from 'react';
import './styles.css';

function MetricsFilter({ onFilterChange, services }) {
  const [filters, setFilters] = useState({
    fromService: '',
    toService: '',
    startDate: '',
    startTime: '00:00',
    endDate: '',
    endTime: '23:59'
  });
  const [error, setError] = useState('');

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    const newFilters = {
      ...filters,
      [name]: value
    };
    setFilters(newFilters);
  };

  const handleApplyFilters = (e) => {
    e.preventDefault();
    
    const startDateTime = new Date(`${filters.startDate}T${filters.startTime}`).getTime();
    const endDateTime = new Date(`${filters.endDate}T${filters.endTime}`).getTime();

    if (filters.startDate && filters.endDate && startDateTime > endDateTime) {
      setError('Start date/time cannot be after end date/time');
      return;
    }

    setError('');
    const processedFilters = {
      ...filters,
      startDate: filters.startDate ? startDateTime : '',
      endDate: filters.endDate ? endDateTime : ''
    };
    
    onFilterChange(processedFilters);
  };

  const clearFilters = () => {
    const clearedFilters = {
      fromService: '',
      toService: '',
      startDate: '',
      startTime: '00:00',
      endDate: '',
      endTime: '23:59'
    };
    setFilters(clearedFilters);
    onFilterChange(clearedFilters);
  };

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

    setFilters({
      ...filters,
      startDate: start.toISOString().split('T')[0],
      startTime: '00:00',
      endDate: end.toISOString().split('T')[0],
      endTime: '00:00'  // Changed to 00:00 of next day
    });
  };

  return (
    <div className="metrics-filter">
      <form onSubmit={handleApplyFilters}>
        <div className="filter-group">
          <label htmlFor="fromService" title="Filter metrics by source service">
            From Service:
          </label>
          <select 
            id="fromService"
            name="fromService" 
            value={filters.fromService}
            onChange={handleFilterChange}
            title="Select the source service"
          >
            <option value="">All Services</option>
            {services.map((service, index) => (
              <option key={index} value={service}>
                {service}
              </option>
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
            {services.map((service, index) => (
              <option key={index} value={service}>
                {service}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-group date-time-group">
          <label htmlFor="startDate">Start:</label>
          <div className="date-time-inputs">
            <input
              id="startDate"
              type="date"
              name="startDate"
              value={filters.startDate}
              onChange={handleFilterChange}
            />
            <input
              id="startTime"
              type="time"
              name="startTime"
              value={filters.startTime}
              onChange={handleFilterChange}
            />
          </div>
        </div>

        <div className="filter-group date-time-group">
          <label htmlFor="endDate">End:</label>
          <div className="date-time-inputs">
            <input
              id="endDate"
              type="date"
              name="endDate"
              value={filters.endDate}
              onChange={handleFilterChange}
            />
            <input
              id="endTime"
              type="time"
              name="endTime"
              value={filters.endTime}
              onChange={handleFilterChange}
            />
          </div>
        </div>

        {error && <div className="filter-error">{error}</div>}
        
        <div className="filter-actions">
          <div className="quick-filters">
            <button type="button" onClick={() => handleQuickFilter('today')}>Today</button>
            <button type="button" onClick={() => handleQuickFilter('week')}>Last 7 Days</button>
            <button type="button" onClick={() => handleQuickFilter('month')}>Last 30 Days</button>
          </div>
          <button type="submit" className="apply-button">Apply Filters</button>
          <button type="button" onClick={clearFilters} className="clear-button">Clear</button>
        </div>
      </form>
    </div>
  );
}

export default MetricsFilter;