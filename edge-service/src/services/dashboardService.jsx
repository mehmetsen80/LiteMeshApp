import axiosInstance from './axiosInstance';

// Types
export const StatTrendType = {
  POSITIVE: 'positive',
  NEGATIVE: 'negative',
  NEUTRAL: 'neutral'
};

export const StatPeriod = {
  HOUR: 'from last hour',
  DAY: 'from yesterday',
  WEEK: 'from last week'
};

// Mock data for development
const mockStats = [
  {
    id: 'active-routes',
    icon: 'fa-route',
    title: 'Active Routes',
    value: '24',
    trend: {
      type: 'positive',
      value: '12%',
      period: 'from last week'
    }
  },
  {
    id: 'response-time',
    icon: 'fa-tachometer-alt',
    title: 'Avg Response Time',
    value: '142ms',
    trend: {
      type: 'negative',
      value: '8%',
      period: 'from last hour'
    }
  },
  {
    id: 'requests-per-min',
    icon: 'fa-chart-line',
    title: 'Requests/min',
    value: '1,284',
    trend: {
      type: 'positive',
      value: '3%',
      period: 'from last minute'
    }
  },
  {
    id: 'success-rate',
    icon: 'fa-shield-alt',
    title: 'Success Rate',
    value: '99.8%',
    trend: {
      type: 'neutral',
      value: '',
      period: 'from last 5 minutes'
    }
  }
];

const statsService = {
  /**
   * Fetches dashboard statistics
   * @returns {Promise<Array>} Array of stat objects
   */
  async getStats() {
    try {
      const response = await axiosInstance.get('/api/dashboard/stats');
      return response.data.map(stat => ({
        id: this.getStatId(stat.title),
        icon: this.getIconForStat(stat.title),
        title: stat.title,
        value: this.formatValue(stat.value, stat.type),
        trend: {
          type: this.getTrendType(stat.trend?.percentChange ?? 0),
          value: Math.abs(stat.trend?.percentChange ?? 0).toFixed(1) + '%',
          period: stat.trend?.period ?? 'No data'
        }
      }));
    } catch (error) {
      console.error('Error fetching stats:', error);
      throw new Error('Failed to fetch statistics');
    }
  },

  getIconForStat(title) {
    const iconMap = {
      'Active Routes': 'fa-route',
      'Avg Response Time': 'fa-tachometer-alt',
      'Requests/min': 'fa-chart-line',
      'Success Rate': 'fa-shield-alt'
    };
    return iconMap[title] || 'fa-chart-bar';
  },

  getTrendType(percentChange) {
    if (percentChange === 0) return 'neutral';
    return percentChange > 0 ? 'positive' : 'negative';
  },

  formatValue(value, type) {
    // Convert string to number if needed
    const numValue = typeof value === 'string' ? parseFloat(value) : value;

    if (isNaN(numValue)) {
      return value.toString();
    }

    switch (type) {
      case 'percentage':
        return `${numValue.toFixed(1)}%`;
      case 'time':
        return `${numValue.toFixed(0)}ms`;
      case 'rate':
        return numValue.toLocaleString();
      case 'count':
        return numValue.toString();
      default:
        return value.toString();
    }
  },

  getStatId(title) {
    const idMap = {
      'Active Routes': 'active-routes',
      'Avg Response Time': 'response-time',
      'Requests/min': 'requests-per-min',
      'Success Rate': 'success-rate'
    };
    return idMap[title] || title.toLowerCase().replace(/\s+/g, '-');
  },

  /**
   * Calculates trend percentage between two values
   * @param {number} current - Current value
   * @param {number} previous - Previous value
   * @returns {Object} Trend object with type and value
   */
  calculateTrend(current, previous) {
    if (current === previous) {
      return { type: StatTrendType.NEUTRAL, value: '' };
    }

    const percentage = ((current - previous) / previous) * 100;
    return {
      type: percentage > 0 ? StatTrendType.POSITIVE : StatTrendType.NEGATIVE,
      value: `${Math.abs(percentage.toFixed(1))}%`
    };
  },

  async getLatencyStats(timeRange = '24h') {
    try {
      const response = await axiosInstance.get(`/api/dashboard/latency?timeRange=${timeRange}`);
      return (response.data || []).map(stat => ({
        ...stat,
        endpoint: `${stat.id?.method || 'Unknown'} ${stat.id?.path || '/'}`,
        p50: parseFloat((stat.p50 || 0).toFixed(2)),
        p75: parseFloat((stat.p75 || 0).toFixed(2)),
        p90: parseFloat((stat.p90 || 0).toFixed(2)),
        p95: parseFloat((stat.p95 || 0).toFixed(2)),
        p99: parseFloat((stat.p99 || 0).toFixed(2))
      }));
    } catch (error) {
      console.error('Error fetching latency stats:', error);
      throw new Error('Failed to fetch latency statistics');
    }
  },

  /**
   * Fetches service usage statistics
   * @returns {Promise<Array>} Array of service usage stats
   */
  async getServiceUsage() {
    try {
      const response = await axiosInstance.get('/api/dashboard/service-usage');
      return response.data.map(stat => ({
        service: stat.service || 'Unknown Service',
        requestCount: parseInt(stat.requestCount) || 0
      }));
    } catch (error) {
      console.error('Error fetching service usage:', error);
      throw new Error('Failed to fetch service usage statistics');
    }
  }
};

export default statsService; 