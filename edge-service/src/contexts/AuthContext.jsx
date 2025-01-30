import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import authService from '../services/authService';
import { jwtDecode } from "jwt-decode";

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const location = useLocation();
  
  // Define the standard auth state structure
  const createAuthState = (user, token, isAuthenticated) => ({
    user,
    token,
    isAuthenticated
  });
  
  const authStateRef = useRef(createAuthState(null, null, false));
  
  // Function to get initial state from localStorage
  const getInitialState = () => {
    try {
      const savedAuthState = localStorage.getItem('authState');
      
      if (savedAuthState) {
        const parsedAuthState = JSON.parse(savedAuthState);
       
        // Ensure we have a valid token
        if (!parsedAuthState?.token) {
          localStorage.removeItem('authState');
          return createAuthState(null, null, false);
        }
        
        // Validate token
        try {
          const decodedToken = jwtDecode(parsedAuthState.token);
          if (decodedToken.exp < Date.now() / 1000) {//tored token expired, clearing state
            localStorage.removeItem('authState');//stored token expired, clearing state
            return createAuthState(null, null, false);
          }
          
          // Token is valid, return the full state
          return { 
            user: parsedAuthState.user,
            token: parsedAuthState.token,
            isAuthenticated: true
          };
        } catch (error) {
          localStorage.removeItem('authState');//oken validation error
          return createAuthState(null, null, false);
        }
      }
      return createAuthState(null, null, false);//No auth state in storage
    } catch (error) {
      localStorage.removeItem('authState');
      return createAuthState(null, null, false);//Error reading from localStorage
    }
  };

  const initialState = getInitialState();
  
  const [user, setUser] = useState(initialState.user);
  const [loading, setLoading] = useState(false);  // Changed to false since we load synchronously
  const [isAuthenticated, setIsAuthenticated] = useState(initialState.isAuthenticated);
  const [token, setToken] = useState(initialState.token);
  
  const navigate = useNavigate();

  // Function to update all auth state at once
  const updateAuthState = (newState) => {
    // Validate state before updating
    if (newState.isAuthenticated && !newState.token) {
      return;//Invalid auth state
    }
    
    try {
      authStateRef.current = newState;
      localStorage.setItem('authState', JSON.stringify(newState));
      setUser(newState.user);
      setToken(newState.token);
      setIsAuthenticated(newState.isAuthenticated);
      
      // Verify only if we're setting an authenticated state
      if (newState.isAuthenticated) {
        if (!authStateRef.current.token) {
          throw new Error('Failed to update authentication state');
        }
      }
    } catch (error) {
      // Reset to a clean state on error
      const cleanState = createAuthState(null, null, false);
      authStateRef.current = cleanState;
      localStorage.removeItem('authState');
      setUser(null);
      setToken(null);
      setIsAuthenticated(false);
    }
  };

  const checkAuth = useCallback(() => {
    const currentUser = authStateRef.current.user;
    if (!currentUser) {
      return false;//No user found in state or localStorage
    }
    try {
      const token = authStateRef.current.token;
      if (!token) {
        return false;//No token found
      }
      const decodedToken = jwtDecode(token);
      const isValid = decodedToken.exp > Date.now() / 1000;
      return isValid;
    } catch (err) {
      return false;//Token validation error
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('authState');
    updateAuthState(createAuthState(null, null, false));
    navigate('/login');
  }, [navigate]);

  // Prevent unnecessary auth state updates
  useEffect(() => {
    // Skip update if we're already authenticated and have a user
    if (isAuthenticated && user && token) {
      return;
    }

    // Skip if we're in the process of logging in
    if (user?.token && token && !authStateRef.current.token) {
      return;
    }

    const authStatus = checkAuth();
    setIsAuthenticated(authStatus);
    setLoading(false);
  }, [user, token, checkAuth]);

  // Token expiration check effect
  useEffect(() => {
    const checkTokenExpiration = async () => {
      if (user?.token && isAuthenticated) {
        try {
          const decodedToken = jwtDecode(user.token);
          const currentTime = Date.now() / 1000;
          const timeUntilExpiry = decodedToken.exp - currentTime;
          
          // Only logout if token is expired or about to expire in less than 1 minute
          if (timeUntilExpiry <= 60) {
            logout(); //Token expiring soon, logging out
          } else {
            console.log('Token valid, expires in:', Math.round(timeUntilExpiry/60), 'minutes');
          }
        } catch (error) {
          logout();//Token validation error in expiration check
        }
      }
      setLoading(false);
    };
    
    // Only start checking after initial auth is complete
    if (!isAuthenticated || !user || loading) {
      return;
    }
    
    checkTokenExpiration();
    // Check every minute
    const interval = setInterval(checkTokenExpiration, 60 * 1000);
    
    return () => clearInterval(interval);
  }, [user, isAuthenticated, logout, loading]);

  const register = async (username, email, password) => {
    try {
      const { data, error } = await authService.registerUser(username, email, password);
      if (error) {
        return { error };
      }

      // Create new auth state
      const newState = createAuthState(data, data.token, true);
      
      // Validate the state before saving
      if (!newState.token || !newState.user) {
        console.error('Invalid auth state created:', newState);
        return { error: 'Invalid authentication state' };
      }

      // Save to localStorage and update state
      localStorage.setItem('authState', JSON.stringify(newState));
      updateAuthState(newState);

      // Navigate immediately
      navigate('/', { replace: true });
      return { data };
    } catch (err) {
      return { error: err.message };
    }
  };

  const login = async (username, password) => {
    try {
      const { data, error } = await authService.loginUser(username, password);
      
      if (error) {
        return { error };
      }
      
      const userData = { username: data.username, token: data.token };
      
      // Create new auth state
      const newState = createAuthState(userData, data.token, true);
      
      // Validate the state before saving
      if (!newState.token || !newState.user) {
        console.error('Invalid auth state created:', newState);
        return { error: 'Invalid authentication state' };
      }

      // Save to localStorage and update state
      localStorage.setItem('authState', JSON.stringify(newState));
      updateAuthState(newState);

      // Navigate immediately
      navigate('/', { replace: true });
      return { data };
    } catch (err) {
      return { error: err.message };
    }
  };

  const handleSSOCallback = async (code) => {
    try {
      const response = await authService.handleSSOCallback(code);
      
      if (response && response.token) {//Setting auth token...
        const userData = { 
          username: response.username,
          token: response.token 
        };
        
        // Create new auth state
        const newState = createAuthState(userData, response.token, true);
        
        // Validate the state before saving
        if (!newState.token || !newState.user) {
          return { error: 'Invalid authentication state' };//Invalid auth state created
        }
        
        try {
          // Update state in a single atomic operation
          setLoading(true);  // Prevent other effects from running
          authStateRef.current = newState;
          localStorage.setItem('authState', JSON.stringify(newState));
          setUser(newState.user);
          setToken(newState.token);
          setIsAuthenticated(true);
          setLoading(false);
        } catch (error) {
          return { error: 'Failed to update authentication state' };
        }
        
        // Wait for state updates to complete
        await new Promise(resolve => setTimeout(resolve, 100));
        
        // Verify storage
        const savedState = localStorage.getItem('authState');
        if (!savedState) {
          return { error: 'Failed to save authentication state' };
        }
        
        // Final verification of the complete state
        const finalState = JSON.parse(savedState);
        if (!finalState.token || !finalState.user || !finalState.isAuthenticated) {
          return { error: 'Incomplete authentication state' };//Incomplete auth state after save
        }
        
        
        return { data: response, success: true };//SSO authentication complete
      } else {
        console.error('No token in response:', response);
        return { error: 'Invalid response from server' };
      }
    } catch (error) {
      console.error('SSO callback error in context:', error);
      return { error: error.message };
    }
  };

  const value = {
    user,
    setUser,
    login,
    register,
    logout,
    handleSSOCallback,
    isAuthenticated,
    loading,
    token
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};