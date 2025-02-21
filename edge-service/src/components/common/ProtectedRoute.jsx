import React, { useEffect } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, loading, user, token } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  
  // Get stored auth state
  const storedAuthState = localStorage.getItem('authState');
  const parsedAuthState = storedAuthState ? JSON.parse(storedAuthState) : null;
  
  // Validate stored auth state
  const isStoredAuthValid = parsedAuthState?.isAuthenticated && 
    parsedAuthState?.user && 
    parsedAuthState?.token;

  // Allow access if either context or storage has valid credentials
  const hasValidAuth = isAuthenticated || isStoredAuthValid;
  
  if (loading) {
    return <div>Loading...</div>;
  }
  
  if (!hasValidAuth && location.pathname !== '/callback') {
    console.log('Not authenticated, redirecting to login');
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  
  return children;
};

export default ProtectedRoute; 