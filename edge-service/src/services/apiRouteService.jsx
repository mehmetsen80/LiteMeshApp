import axiosInstance from './axiosInstance';

export const apiRouteService = {
  async getAllRoutes() {
    try {
      const response = await axiosInstance.get('/api/routes');
      return response.data;
    } catch (error) {
      console.error('Error fetching routes:', error);
      throw error;
    }
  },

  async getRouteById(routeId) {
    try {
      const response = await axiosInstance.get(`/api/routes/${routeId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching route by ID:', error);
      throw error;
    }
  },

  async getRouteByIdentifier(routeIdentifier) {
    try {
      const response = await axiosInstance.get(`/api/routes/identifier/${routeIdentifier}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching route by identifier:', error);
      throw error;
    }
  },

  async updateRoute(routeIdentifier, routeData) {
    try {
      const response = await axiosInstance.put(`/api/routes/${routeIdentifier}`, routeData);
      return response.data;
    } catch (error) {
      console.error('Error updating route:', error);
      throw error;
    }
  },

  async deleteRoute(routeIdentifier, teamId) {
    try {
      await axiosInstance.delete(`/api/routes/${routeIdentifier}?teamId=${teamId}`);
      return true;
    } catch (error) {
      console.error('Error deleting route:', error);
      throw error;
    }
  },

  async createRoute(routeData) {
    try {
      const response = await axiosInstance.post('/api/routes', routeData);
      return response.data;
    } catch (error) {
      console.error('Error creating route:', error);
      throw error;
    }
  },

  getRouteVersions: async (routeIdentifier) => {
    const response = await axiosInstance.get(`/api/routes/identifier/${routeIdentifier}/versions`);
    return response.data;
  },

  getRouteVersion: async (routeIdentifier, version) => {
    const response = await axiosInstance.get(`/api/routes/identifier/${routeIdentifier}/versions/${version}`);
    return response.data;
  },

  getVersionMetadata: async (routeIdentifier) => {
    const response = await axiosInstance.get(`/api/routes/identifier/${routeIdentifier}/metadata`);
    return response.data;
  },

  compareVersions: async (routeIdentifier, version1, version2) => {
    const response = await axiosInstance.get(
      `/api/routes/identifier/${routeIdentifier}/compare?version1=${version1}&version2=${version2}`
    );
    return response.data;
  }
}; 