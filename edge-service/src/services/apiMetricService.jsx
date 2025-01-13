import axios from 'axios';

const API_GATEWAY_URL = import.meta.env.VITE_API_GATEWAY_URL;

export const getApiMetrics = async (params = {}) => {
    try {
        // Ensure dates are in ISO format
        if (params.startDate) {
            params.startDate = new Date(params.startDate).toISOString();
        }
        if (params.endDate) {
            params.endDate = new Date(params.endDate).toISOString();
        }

        const response = await axios.get(`${API_GATEWAY_URL}/api/metrics`, { params });
        return response.data;
    } catch (error) {
        console.error('Error fetching API metrics:', error);
        throw error;
    }
};

export const getMetricsSummary = async (params = {}) => {
    try {
        const response = await axios.get(`${API_GATEWAY_URL}/api/metrics/summary`, { params });
        return response.data;
    } catch (error) {
        console.error('Error fetching metrics summary:', error);
        throw error;
    }
};

export const getServiceInteractions = async (params = {}) => {
    try {
        const response = await axios.get(`${API_GATEWAY_URL}/api/metrics/service-interactions`, { params });
        return response.data;
    } catch (error) {
        console.error('Error fetching service interactions:', error);
        throw error;
    }
};

export const getTopEndpoints = async (params = {}) => {
    try {
        const response = await axios.get(`${API_GATEWAY_URL}/api/metrics/top-endpoints`, { params });
        return response.data;
    } catch (error) {
        console.error('Error fetching top endpoints:', error);
        throw error;
    }
};