import { useState } from 'react';
import './styles.css';

function MetricsFilter({ onFilterChange, services }) {
  const [filters, setFilters] = useState({
    service: '',
    startDate: '',
    endDate: ''
  });

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
    
    // Convert dates to the end of day for endDate and start of day for startDate
    const processedFilters = {
      ...filters,
      startDate: filters.startDate 
        ? new Date(filters.startDate).setHours(0, 0, 0, 0) 
        : '',
      endDate: filters.endDate 
        ? new Date(filters.endDate).setHours(23, 59, 59, 999) 
        : ''
    };
    
    onFilterChange(processedFilters);
  };

  const clearFilters = () => {
    const clearedFilters = {
      service: '',
      startDate: '',
      endDate: ''
    };
    setFilters(clearedFilters);
    onFilterChange(clearedFilters);
  };

  // Format date to YYYY-MM-DD
  const formatDateForInput = (date) => {
    if (!date) return '';
    return new Date(date).toISOString().split('T')[0];
  };

  return (
    <div className="metrics-filter">
      <form onSubmit={handleApplyFilters}>
        <div className="filter-group">
          <label htmlFor="service">Service:</label>
          <select 
            id="service"
            name="service" 
            value={filters.service}
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

        <div className="filter-group">
          <label htmlFor="startDate">Start Date:</label>
          <input
            id="startDate"
            type="date"
            name="startDate"
            value={formatDateForInput(filters.startDate)}
            onChange={handleFilterChange}
          />
        </div>

        <div className="filter-group">
          <label htmlFor="endDate">End Date:</label>
          <input
            id="endDate"
            type="date"
            name="endDate"
            value={formatDateForInput(filters.endDate)}
            onChange={handleFilterChange}
          />
        </div>

        <div className="filter-actions">
          <button type="submit" className="apply-button">Apply Filters</button>
          <button type="button" onClick={clearFilters} className="clear-button">Clear Filters</button>
        </div>
      </form>
    </div>
  );
}

export default MetricsFilter;