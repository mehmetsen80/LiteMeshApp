import React from 'react';
import { useTeam } from '../../contexts/TeamContext';
import { useAuth } from '../../contexts/AuthContext';
import { isSuperAdmin, hasAdminAccess } from '../../utils/roleUtils';
import NotAuthorized from '../common/NotAuthorized';

const AdminGuard = ({ children }) => {
  const { currentTeam, loading: teamLoading } = useTeam();
  const { user } = useAuth();
  
  if (teamLoading) {
    return null;
  }

  // Check for either SUPER_ADMIN or team admin access
  const hasAccess = isSuperAdmin(user) || hasAdminAccess(user, currentTeam);

  if (!hasAccess) {
    return <NotAuthorized />;
  }

  return children;
};

export default AdminGuard; 