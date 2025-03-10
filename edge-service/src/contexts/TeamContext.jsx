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
  const { isAuthenticated, user } = useAuth();

  const fetchUserTeams = useCallback(async () => {
    if (!isAuthenticated) {
      setUserTeams([]);
      setCurrentTeam(null);
      setLoading(false);
      return;
    }

    try {
      console.log('Fetching user teams...'); // Debug log
      const response = await teamService.getUserTeams();
      const teams = response.data || [];
      setUserTeams(teams);
      
      if (teams.length > 0) {
        const savedTeamId = localStorage.getItem('currentTeamId');
        let teamToSet;

        if (savedTeamId && teams.some(team => team.id === savedTeamId)) {
          teamToSet = teams.find(t => t.id === savedTeamId);
        } else {
          teamToSet = teams[0];
          localStorage.setItem('currentTeamId', teams[0].id);
        }

        console.log('Setting current team to:', teamToSet); // Debug log
        setCurrentTeam(teamToSet);
      }
    } catch (error) {
      console.error('Failed to fetch user teams:', error);
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    console.log('Auth state changed:', isAuthenticated); // Debug log
    if (isAuthenticated) {
      fetchUserTeams();
    }
  }, [isAuthenticated, fetchUserTeams]);

  // Add a debug effect to monitor state changes
  useEffect(() => {
    console.log('Current team state:', currentTeam);
    console.log('User teams state:', userTeams);
  }, [currentTeam, userTeams]);

  const fetchAllTeams = useCallback(async () => {
    // Add check for login page
    if (window.location.pathname === '/login') {
      return;
    }

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
    fetchAllTeams();
  }, [fetchAllTeams]);

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