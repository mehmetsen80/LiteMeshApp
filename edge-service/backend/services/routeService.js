import axios from 'axios';

const API_BASE_URL = 'http://localhost:7777'; // Java backend URL

export const getRoutes = async () => {
    try {
        const response = await axios.get(`${API_BASE_URL}/api/routes`);
        return response.data;
    } catch (error) {
        console.error('Error fetching routes:', error);
        throw error;
    }
};

export const getRouteById = async (routeId) => {
    try {
        const response = await axios.get(`${API_BASE_URL}/api/routes/${routeId}`);
        return response.data;
    } catch (error) {
        console.error(`Error fetching route ${routeId}:`, error);
        throw error;
    }
}; 