import axios from 'axios';

const API_BASE_URL = `${import.meta.env.VITE_BACKEND_URL}/api`;

axios.defaults.withCredentials = true;

export const getApiMetrics = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/metrics`);
    return response.data;
  } catch (error) {
    console.error('Error fetching API metrics:', error);
    throw error;
  }
};

export const getMetricsByService = async (serviceName) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/metrics`, {
      params: { service: serviceName }
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching metrics by service:', error);
    throw error;
  }
};

export const getMetricsByTimeRange = async (startDate, endDate) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/metrics/timerange`, {
      params: { startDate, endDate }
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching metrics by time range:', error);
    throw error;
  }
};