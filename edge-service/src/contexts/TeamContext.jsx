import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import { useAuth } from './AuthContext';
import { teamService } from '../services/teamService';

// Create the context
const TeamContext = createContext(null);

// Export the hook separately before the provider!
export const useTeam = () => {
  const context = useContext(TeamContext);
  if (!context) {
    throw new Error('useTeam must be used within a TeamProvider');
  }
  return context;
};

// Export the provider component
export const TeamProvider = ({ children }) => {
  const [currentTeam, setCurrentTeam] = useState(null);
  const [userTeams, setUserTeams] = useState([]);
  const [teams, setTeams] = useState([]);
  const [loading, setLoading] = useState(true);
  const { user, isAuthenticated } = useAuth();

  const fetchUserTeams = useCallback(async () => {
    if (!isAuthenticated) {
      setUserTeams([]);
      setCurrentTeam(null);
      setLoading(false);
      return;
    }

    try {
      const response = await teamService.getUserTeams();
      setUserTeams(response.data || []);
      
      // Restore logic to set the current team based on savedTeamId
      const savedTeamId = localStorage.getItem('currentTeamId');
      if (savedTeamId && response.data.some(team => team.id === savedTeamId)) {
        const team = response.data.find(t => t.id === savedTeamId);
        setCurrentTeam(team);
      } else if (response.data.length > 0) {
        setCurrentTeam(response.data[0]);
        localStorage.setItem('currentTeamId', response.data[0].id);
      }
    } catch (error) {
      console.error('Failed to fetch user teams:', error);
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  const fetchAllTeams = useCallback(async () => {
    try {
      const response = await teamService.getAllTeams();
      if (response.error) {
        console.error('Error fetching all teams:', response.error);
        return;
      }
      setTeams(response.data);
    } catch (error) {
      console.error('Error fetching all teams:', error);
    }
  }, []);

  useEffect(() => {
    fetchUserTeams();
    fetchAllTeams();
  }, [fetchUserTeams, fetchAllTeams]);

  const switchTeam = useCallback((teamId) => {
    const team = userTeams.find(t => t.id === teamId);
    if (team) {
      setCurrentTeam(team);
      localStorage.setItem('currentTeamId', teamId);
    }
  }, [userTeams]);

  const hasPermission = useCallback((routeId, permission) => {
    if (!currentTeam) return false;
    const route = currentTeam.routes?.find(r => r.routeId === routeId);
    return route?.permissions?.includes(permission) || false;
  }, [currentTeam]);

  const value = useMemo(() => ({
    currentTeam,
    userTeams,
    teams,
    loading,
    switchTeam,
    hasPermission,
    refreshTeams: fetchUserTeams,
    fetchAllTeams
  }), [currentTeam, userTeams, teams, loading, switchTeam, hasPermission, fetchUserTeams, fetchAllTeams]);

  return (
    <TeamContext.Provider value={value}>
      {children}
    </TeamContext.Provider>
  );
}; 