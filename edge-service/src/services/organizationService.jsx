import axiosInstance from './axiosInstance';

const organizationService = {
  getAllOrganizations: async () => {
    try {
      const response = await axiosInstance.get('/api/organizations');
      return { data: response.data };
    } catch (error) {
      return { error: error.message };
    }
  },

  createOrganization: async (organizationData) => {
    try {
      const response = await axiosInstance.post('/api/organizations', organizationData);
      return { data: response.data };
    } catch (error) {
      return { error: error.message };
    }
  },

  updateOrganization: async (organizationId, organizationData) => {
    try {
      const response = await axiosInstance.put(`/api/organizations/${organizationId}`, organizationData);
      return { data: response.data };
    } catch (error) {
      return { 
        error: error.response?.data?.message || 'Failed to update organization' 
      };
    }
  },

  deleteOrganization: async (organizationId) => {
    try {
      const response = await axiosInstance.delete(`/api/organizations/${organizationId}`);
      return { data: response.data };
    } catch (error) {
      return { 
        error: error.response?.data?.message || 'Failed to delete organization' 
      };
    }
  }
};

export default organizationService; 