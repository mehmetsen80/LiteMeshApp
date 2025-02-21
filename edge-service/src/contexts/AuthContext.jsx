import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import authService from '../services/authService';
import { jwtDecode } from "jwt-decode";
import { showWarningToast } from '../utils/toastConfig';

export const AuthContext = createContext({
  user: null,
  token: null,
  isAuthenticated: false,
  login: () => {},
  logout: () => {},
  handleRefreshToken: () => {},
});

export const AuthProvider = ({ children }) => {
  const location = useLocation();
  
  // Define the standard auth state structure
  const createAuthState = (user, token, isAuthenticated) => ({
    user,
    token,
    isAuthenticated
  });
  
  const setTokens = (accessToken, refreshToken) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
  };
  
  const clearTokens = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  };
  
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

  const logout = () => {
    // Clear all auth-related items from localStorage
    localStorage.removeItem('authState');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    sessionStorage.removeItem('oauth_state');  // Clear SSO state too
    
    // Reset all state
    setUser(null);
    setToken(null);
    setIsAuthenticated(false);
    
    // Clear any pending requests or intervals
    if (window.refreshInterval) {
        clearInterval(window.refreshInterval);
    }
    
    // Navigate to login
    navigate('/login');
  };
  
  const handleRefreshToken = async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      console.log('Starting refresh with token:', refreshToken?.substring(0, 20) + '...');

      if (!refreshToken) {
        console.error('No refresh token available');
        logout();
        return false;
      }

      const response = await authService.refreshToken(refreshToken);
      console.log('Refresh response in context:', response);

      if (response.success && response.data) {
        // Store the complete auth state from the service
        console.log('About to store auth state:', response.data);
        
        localStorage.setItem('authState', JSON.stringify(response.data));
        localStorage.setItem('accessToken', response.data.token);
        
        // Only store refresh token if it's provided and different
        if (response.data.refreshToken && response.data.refreshToken !== response.data.token) {
          localStorage.setItem('refreshToken', response.data.refreshToken);
        }

        // Update context state
        setUser(response.data.user);
        setToken(response.data.token);
        setIsAuthenticated(true);

        return true;
      } else {
        console.error('Refresh failed:', response.error);
        logout();
        return false;
      }
    } catch (error) {
      console.error('Error during refresh:', error);
      logout();
      return false;
    }
  };

  // Function to update all auth state at once
  const updateAuthState = (newState) => {
    // Validate state before updating
    if (newState.isAuthenticated && !newState.token) {
      return;
    }
    
    try {
      // Update localStorage
      localStorage.setItem('authState', JSON.stringify(newState));
      
      // Explicitly store access token
      if (newState.token) {
        localStorage.setItem('accessToken', newState.token);
      } else {
        localStorage.removeItem('accessToken');
      }
      
      // Update state
      setUser(newState.user);
      setToken(newState.token);
      setIsAuthenticated(newState.isAuthenticated);
      
      console.log('Auth state updated:', {
        hasUser: !!newState.user,
        hasToken: !!newState.token,
        isAuthenticated: newState.isAuthenticated
      });
    } catch (error) {
      console.error('Error updating auth state:', error);
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

  // Modify the validateAuthState function
  const validateAuthState = () => {
    try {
      const storedAuthState = localStorage.getItem('authState');
      const accessToken = localStorage.getItem('accessToken');

      // If we're on the login or callback page, skip validation
      if (location.pathname === '/login' || location.pathname === '/callback') {
        return true;
      }

      // If there's no auth state but we're not on a protected route, that's okay
      if (!storedAuthState || !accessToken) {
        console.log('No auth state found, but might be on public route');
        return true;
      }

      // Parse and validate auth state only if it exists
      if (storedAuthState) {
        const authState = JSON.parse(storedAuthState);
        if (!authState.token || !authState.user) {
          console.log('Invalid auth state structure, clearing');
          localStorage.removeItem('authState');
          return false;
        }

        // If valid, update context state
        setUser(authState.user);
        setToken(authState.token);
        setIsAuthenticated(true);
      }

      return true;
    } catch (error) {
      console.error('Error validating auth state:', error);
      return false;
    }
  };

  const checkTokenExpiration = async () => {
    if (!validateAuthState()) {
      logout();//Auth state validation failed
      return;
    }

    const token = localStorage.getItem('accessToken');
    const authState = JSON.parse(localStorage.getItem('authState'));

    // Verify token matches auth state
    if (token !== authState.token) {
      logout();//Token mismatch between storage and auth state
      return;
    }

    try {
      const decoded = jwtDecode(token);
      const currentTime = Date.now() / 1000;

      if (decoded.exp <= currentTime) {
        const refreshed = await handleRefreshToken();//Token expired, attempting refresh
        if (!refreshed) {
          console.error('Token refresh failed, logging out');
          logout();
        } else {
          console.log('Token refresh successful');
          // Verify refresh result
          if (!validateAuthState()) {
            console.error('Auth state invalid after refresh');
            logout();
          }
        }
      } else if (decoded.exp - currentTime < 120) {
        console.log('Token expiring soon, attempting refresh');
        const refreshed = await handleRefreshToken();
        console.log('Proactive refresh result:', refreshed);
      }
    } catch (error) {
      console.error('Token check error:', error);
      logout();
    }
  };

  // Modify the initial useEffect
  useEffect(() => {
    // Initial validation
    if (!validateAuthState()) {
      console.error('Initial validation failed, redirecting to login');
      logout();
      return;
    }

    // Only set up token check interval if we're authenticated
    if (isAuthenticated) {
      const interval = setInterval(checkTokenExpiration, 30000);
      checkTokenExpiration();

      return () => {
        clearInterval(interval);
      };
    }
  }, [isAuthenticated]); // Add isAuthenticated to dependencies

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
      const response = await authService.loginUser(username, password);
      if (response.data && response.data.token) {
        // Create auth state with user info from the response
        const authState = {
          user: response.data.user,  // Use the entire user object directly
          token: response.data.token,
          isAuthenticated: true
        };

        // Store everything
        localStorage.setItem('authState', JSON.stringify(authState));
        localStorage.setItem('accessToken', response.data.token);
        localStorage.setItem('refreshToken', response.data.refreshToken);

        // Update context state
        setUser(authState.user);
        setToken(authState.token);
        setIsAuthenticated(true);

        // Navigate to dashboard
        navigate('/dashboard');
        return true;
      } else {
        throw new Error('Invalid login response - no token received');
      }
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  };

  const handleSSOCallback = async (code) => {
    try {
      const response = await authService.handleSSOCallback(code);
      
      if (response && response.token) {
        // Get the stored auth state
        const authState = JSON.parse(localStorage.getItem('authState'));
        if (!authState) {
          console.error('No auth state found after SSO callback');
          navigate('/login');
          return { error: 'Authentication failed' };
        }
        
        // Update the context state
        setUser(authState.user);
        setToken(authState.token);
        setIsAuthenticated(true);
        
        navigate('/dashboard', { replace: true });//Auth state updated, redirecting to dashboard
        return { data: response, success: true };
      }
      
      console.error('Invalid SSO response');
      navigate('/login');
      return { error: 'Invalid response from server' };
    } catch (error) {
      console.error('SSO callback error in context:', error);
      navigate('/login');
      return { error: error.message };
    }
  };

  // Add a useEffect to check auth state on mount
  useEffect(() => {
    const checkAuthState = () => {
      const authState = localStorage.getItem('authState');
      if (authState) {
        const parsed = JSON.parse(authState);
        setUser(parsed.user);
        setToken(parsed.token);
        setIsAuthenticated(true);
      }
    };
    
    checkAuthState();
  }, []);

  const handleSSOLogin = () => {
    // Clear any existing tokens first
    clearTokens();
    localStorage.removeItem('authState');
    
    const state = Math.random().toString(36).substring(7);
    const stateData = {
      value: state,
      timestamp: Date.now()
    };
    sessionStorage.setItem('oauth_state', JSON.stringify(stateData));
    
    const params = new URLSearchParams({
      client_id: process.env.REACT_APP_KEYCLOAK_CLIENT_ID,
      redirect_uri: `${window.location.origin}/callback`,
      response_type: 'code',
      state: state,
      scope: 'openid'
    });
    
    const authUrl = `${process.env.REACT_APP_KEYCLOAK_URL}/realms/${process.env.REACT_APP_KEYCLOAK_REALM}/protocol/openid-connect/auth`;
    window.location.href = `${authUrl}?${params}`;
  };

  const value = {
    user,
    setUser,
    login,
    register,
    logout,
    handleSSOLogin,
    handleSSOCallback,
    isAuthenticated,
    loading,
    token,
    handleRefreshToken,
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