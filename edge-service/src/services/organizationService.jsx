import axiosInstance from './axiosInstance';
import logger from '../utils/logger';

const organizationService = {
  getAllOrganizations: async () => {
    try {
      logger.debug('Fetching all organizations');
      const response = await axiosInstance.get('/api/organizations');
      logger.debug('Organizations fetched successfully', response.data);
      return { data: response.data };
    } catch (error) {
      logger.error('Failed to fetch organizations', error);
      return { 
        error: error.response?.data?.message || 'Failed to fetch organizations' 
      };
    }
  },

  createOrganization: async (organizationData) => {
    try {
      logger.debug('Creating new organization', organizationData);
      const response = await axiosInstance.post('/api/organizations', organizationData);
      logger.info('Organization created successfully', response.data);
      return { data: response.data };
    } catch (error) {
      logger.error('Failed to create organization', error);
      return { 
        error: error.response?.data?.message || 'Failed to create organization' 
      };
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
      logger.debug(`Deleting organization with id: ${organizationId}`);
      const response = await axiosInstance.delete(`/api/organizations/${organizationId}`);
      logger.info('Organization deleted successfully', { organizationId });
      return { data: response.data };
    } catch (error) {
      logger.error(`Failed to delete organization with id: ${organizationId}`, error);
      return { 
        error: error.response?.data?.message || 'Failed to delete organization' 
      };
    }
  }
};

export default organizationService; 