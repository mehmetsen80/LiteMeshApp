import React from 'react';
import { Badge } from 'react-bootstrap';

export const ROLES = {
  SUPER_ADMIN: 'SUPER_ADMIN',
  ADMIN: 'ADMIN',
  USER: 'USER'
};

export const AUTH_TYPES = {
  SSO: 'SSO',
  LOCAL: 'LOCAL'
};

export const isSuperAdmin = (user) => {
  return user?.roles?.includes(ROLES.SUPER_ADMIN);
};

export const hasAdminAccess = (user, currentTeam) => {
  if (!user) return false;
  
  if (isSuperAdmin(user)) return true;

  return currentTeam?.members?.some(
    member => member.userId === user.id && member.role === ROLES.ADMIN
  );
};

export const RoleBadge = ({ user }) => {
  if (!user?.roles) return null;
  
  return isSuperAdmin(user) ? (
    <Badge bg="danger">SUPER ADMIN</Badge>
  ) : null;
};

export const AuthBadge = ({ authType }) => {
  if (!authType || !AUTH_TYPES[authType]) return null;

  return (
    <>
      <Badge bg={authType === AUTH_TYPES.SSO ? "primary" : "secondary"}>
        {authType}
      </Badge>
      <span>Authentication</span>
    </>
  );
}; 