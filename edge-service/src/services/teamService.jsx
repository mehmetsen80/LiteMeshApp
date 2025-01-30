import axiosInstance from './axiosInstance';

const teamService = {
  // Get all teams
  getAllTeams: async () => {
    try {
      const response = await axiosInstance.get('/api/teams');
      return { data: response.data };
    } catch (error) {
      console.error('Error fetching teams:', error);
      return { error: error.message };
    }
  },

  // Create new team
  createTeam: async (teamData) => {
    try {
      const response = await axiosInstance.post('/api/teams', teamData);
      return { data: response.data };
    } catch (error) {
      return { error: error.message };
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
      return { error: error.message };
    }
  },

  // Delete team
  deleteTeam: async (teamId) => {
    try {
      const response = await axiosInstance.delete(`/api/teams/${teamId}`);
      return { data: response.data };
    } catch (error) {
      return { error: error.message };
    }
  },

  // Get team members
  getTeamMembers: async (teamId) => {
    try {
      const response = await axiosInstance.get(`/api/teams/${teamId}/members`);
      return { data: response.data };
    } catch (error) {
      return { error: error.message };
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
      return { error: error.message };
    }
  },

  // Remove team member
  removeTeamMember: async (teamId, userId) => {
    try {
      const response = await axiosInstance.delete(`/api/teams/${teamId}/members/${userId}`);
      return { data: response.data };
    } catch (error) {
      return { error: error.message };
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
      return { error: error.message };
    }
  },

  // Remove team route
  removeTeamRoute: async (teamId, routeId) => {
    try {
      const response = await axiosInstance.delete(`/api/teams/${teamId}/routes/${routeId}`);
      return { data: response.data };
    } catch (error) {
      return { error: error.message };
    }
  },

  // Activate team
  activateTeam: async (teamId) => {
    try {
      const response = await axiosInstance.put(`/api/teams/${teamId}/activate`);
      return { data: response.data };
    } catch (error) {
      return { error: error.message };
    }
  },

  // Deactivate team
  deactivateTeam: async (teamId) => {
    try {
      const response = await axiosInstance.put(`/api/teams/${teamId}/deactivate`);
      return { data: response.data };
    } catch (error) {
      return { error: error.message };
    }
  },

  // Get all available routes
  getAllRoutes: async () => {
    try {
      const response = await axiosInstance.get('/api/routes');
      return { data: response.data };
    } catch (error) {
      return { error: error.message };
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
      return { error: error.message };
    }
  },
};

export default teamService; 