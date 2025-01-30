import axiosInstance from './axiosInstance';
import logger from '../utils/logger';

const teamService = {
  // Get all teams
  getAllTeams: async () => {
    try {
      logger.debug('Fetching all teams');
      const response = await axiosInstance.get('/api/teams');
      logger.debug('Teams fetched successfully', response.data);
      return { data: response.data };
    } catch (error) {
      logger.error('Failed to fetch teams', error);
      return { 
        error: error.response?.data?.message || 'Failed to fetch teams' 
      };
    }
  },

  // Create new team
  createTeam: async (teamData) => {
    try {
      logger.debug('Creating new team', teamData);
      const response = await axiosInstance.post('/api/teams', teamData);
      logger.info('Team created successfully', response.data);
      return { data: response.data };
    } catch (error) {
      logger.error('Failed to create team', error);
      return { 
        error: error.response?.data?.message || 'Failed to create team' 
      };
    }
  },

  // Update team
  updateTeam: async (teamId, teamData) => {
    try {
      const response = await axiosInstance.put(`/api/teams/${teamId}`, {
        name: teamData.name,
        description: teamData.description,
        organizationId: teamData.organizationId
      });
      return { data: response.data };
    } catch (error) {
      return { 
        error: error.response?.data?.message || 'Failed to update team' 
      };
    }
  },

  // Delete team
  deleteTeam: async (teamId) => {
    try {
      logger.debug(`Deleting team with id: ${teamId}`);
      const response = await axiosInstance.delete(`/api/teams/${teamId}`);
      logger.info('Team deleted successfully', { teamId });
      return { data: response.data };
    } catch (error) {
      logger.error(`Failed to delete team with id: ${teamId}`, error);
      return { 
        error: error.response?.data?.message || 'Failed to delete team' 
      };
    }
  },

  // Get team members
  getTeamMembers: async (teamId) => {
    try {
      const response = await axiosInstance.get(`/api/teams/${teamId}/members`);
      return { data: response.data };
    } catch (error) {
      return { 
        error: error.response?.data?.message || 'Failed to fetch team members' 
      };
    }
  },

  // Add team member
  addTeamMember: async (teamId, memberData) => {
    try {
      const response = await axiosInstance.post(
        `/api/teams/${teamId}/members`,
        null,
        {
          params: {
            username: memberData.username,
            role: memberData.role
          }
        }
      );
      return { data: response.data };
    } catch (error) {
      return {
        error: error.response?.data?.message || 'Failed to add team member'
      };
    }
  },

  // Remove team member
  removeTeamMember: async (teamId, userId) => {
    try {
      const response = await axiosInstance.delete(`/api/teams/${teamId}/members/${userId}`);
      return { data: response.data };
    } catch (error) {
      return { 
        error: error.response?.data?.message || 'Failed to remove team member' 
      };
    }
  },

  // Add team route
  addTeamRoute: async (teamId, routeId, permissions) => {
    try {
      const response = await axiosInstance.post(
        `/api/teams/route-assignment`,
        { teamId, routeId, permissions }
      );
      return { data: response.data };
    } catch (error) {
      return { error: error.response?.data?.message || error.message };
    }
  },

  // Remove team route
  removeTeamRoute: async (teamId, routeId) => {
    try {
      const response = await axiosInstance.delete(`/api/teams/${teamId}/routes/${routeId}`);
      return { data: response.data };
    } catch (error) {
      return {
        error: error.response?.data?.message || 'Failed to remove route from team'
      };
    }
  },

  // Activate team
  activateTeam: async (teamId) => {
    try {
      const response = await axiosInstance.put(`/api/teams/${teamId}/activate`);
      return { data: response.data };
    } catch (error) {
      return {
        error: error.response?.data?.message || 'Failed to activate team'
      };
    }
  },

  // Deactivate team
  deactivateTeam: async (teamId) => {
    try {
      const response = await axiosInstance.put(`/api/teams/${teamId}/deactivate`);
      return { data: response.data };
    } catch (error) {
      return {
        error: error.response?.data?.message || 'Failed to deactivate team'
      };
    }
  },

  // Get all available routes
  getAllRoutes: async () => {
    try {
      const response = await axiosInstance.get('/api/routes');
      return { data: response.data };
    } catch (error) {
      return { error: error.response?.data?.message || 'Failed to fetch routes' };
    }
  },

  // Get team routes
  getTeamRoutes: async (teamId) => {
    try {
      console.log('Calling getTeamRoutes API for team:', teamId);
      const response = await axiosInstance.get(`/api/teams/${teamId}/routes`);
      console.log('API response:', response.data);
      return { data: response.data };
    } catch (error) {
      return { 
        error: error.response?.data?.message || 'Failed to fetch team routes' 
      };
    }
  },
};

export default teamService; 