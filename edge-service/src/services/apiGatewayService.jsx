import axios from 'axios';

// Create axios instance with default config
const apiClient = axios.create({
    baseURL: '/api',
    withCredentials: true,
    headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
    }
});

// Add response interceptor for error handling
apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        console.error('API Error:', error.response?.data || error.message);
        return Promise.reject(error);
    }
);

// Service endpoints
const endpoints = {
    services: '/services',
    teams: '/teams',
    health: '/health'
};

// API Service object
export const apiGatewayService = {
    // Services
    getAllServices: async () => {
        try {
            const response = await apiClient.get(endpoints.services);
            return response.data;
        } catch (error) {
            console.error('Failed to fetch services:', error);
            throw error;
        }
    },

    getServiceById: async (serviceId) => {
        try {
            const response = await apiClient.get(`${endpoints.services}/${serviceId}`);
            return response.data;
        } catch (error) {
            console.error(`Failed to fetch service ${serviceId}:`, error);
            throw error;
        }
    },

    // Teams
    getTeamServices: async (teamId) => {
        try {
            const response = await apiClient.get(`${endpoints.teams}/${teamId}/services`);
            return response.data;
        } catch (error) {
            console.error(`Failed to fetch team ${teamId} services:`, error);
            throw error;
        }
    },

    // Health checks
    getServiceHealth: async (serviceId) => {
        try {
            const response = await apiClient.get(`${endpoints.services}/${serviceId}/health`);
            return response.data;
        } catch (error) {
            console.error(`Failed to fetch health for service ${serviceId}:`, error);
            throw error;
        }
    },

    // Teams management
    createTeam: async (teamData) => {
        try {
            const response = await apiClient.post(endpoints.teams, teamData);
            return response.data;
        } catch (error) {
            console.error('Failed to create team:', error);
            throw error;
        }
    },

    updateTeam: async (teamId, teamData) => {
        try {
            const response = await apiClient.put(`${endpoints.teams}/${teamId}`, teamData);
            return response.data;
        } catch (error) {
            console.error(`Failed to update team ${teamId}:`, error);
            throw error;
        }
    },

    deleteTeam: async (teamId) => {
        try {
            await apiClient.delete(`${endpoints.teams}/${teamId}`);
        } catch (error) {
            console.error(`Failed to delete team ${teamId}:`, error);
            throw error;
        }
    }
};

// Error handling utility
export const handleApiError = (error) => {
    if (error.response) {
        // Server responded with error
        return {
            status: error.response.status,
            message: error.response.data.message || 'An error occurred',
            details: error.response.data
        };
    } else if (error.request) {
        // Request made but no response
        return {
            status: 503,
            message: 'Service unavailable',
            details: 'No response from server'
        };
    } else {
        // Request setup error
        return {
            status: 500,
            message: 'Request failed',
            details: error.message
        };
    }
};

export default apiGatewayService; 