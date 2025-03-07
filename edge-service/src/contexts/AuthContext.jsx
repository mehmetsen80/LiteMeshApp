import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import authService from '../services/authService';
import { jwtDecode } from "jwt-decode";
import { showWarningToast, showErrorToast } from '../utils/toastConfig';
import { useEnvironment } from './EnvironmentContext';
// Create context without export
const AuthContext = createContext(null);

// Export the hook separately before the provider
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

// Provider component as named export
export const AuthProvider = ({ children }) => {
  const location = useLocation();
  const { loadEnvironment } = useEnvironment();
  
  // Define the standard auth state structure
  const createAuthState = (user, token, isAuthenticated) => ({
    user,
    token,
    isAuthenticated
  });
  
  // Add refreshTimerRef
  const refreshTimerRef = useRef(null);
  const authStateRef = useRef(createAuthState(null, null, false));
  
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  
  const navigate = useNavigate();


  const logout = useCallback(() => {
    // Reset all state
    setUser(null);
    setToken(null);
    setIsAuthenticated(false);
    authService.logout();
  }, []);
  
  const handleRefreshToken = useCallback(async () => {
    try {
      const authData = JSON.parse(localStorage.getItem('authState') || '{}');
      const refreshToken = authData.refreshToken;
      
      if (!refreshToken) {
        console.log('No refresh token available, logging out');
        logout();
        return false;
      }

      const response = await authService.refreshToken(refreshToken);
      
      if (response.success && response.data) {
        // Only update state if the user data has actually changed
        if (JSON.stringify(user) !== JSON.stringify(response.data.user)) {
          setUser(response.data.user);
        }
        
        // Update auth state
        const authState = {
          user: response.data.user,
          token: response.data.token,
          refreshToken: response.data.refreshToken,
          isAuthenticated: true
        };
        
        // Single source of truth in localStorage
        localStorage.setItem('authState', JSON.stringify(authState));

        setToken(response.data.token);
        setIsAuthenticated(true);

        return true;
      } else {
        console.error('Refresh failed:', response.error);
        logout();
        return false;
      }
    } catch (error) {
      console.error('Token refresh failed:', error);
      logout();
      return false;
    }
  }, [logout, user]);

  // Function to update all auth state at once
  const updateAuthState = (newState) => {
    // Validate state before updating
    if (newState.isAuthenticated && !newState.token) {
      return;
    }
    
    try {
      const authState = {
        user: newState.user,
        token: newState.token,
        refreshToken: newState.refreshToken,
        isAuthenticated: newState.isAuthenticated
      };

      // Update localStorage with single source of truth
      localStorage.setItem('authState', JSON.stringify(authState));
      
      // Update state
      setUser(newState.user);
      setToken(newState.token);
      setIsAuthenticated(newState.isAuthenticated);

      // Set up refresh token timer if we have a valid token
      if (newState.token) {
        setupRefreshTokenTimer(newState.token);
      }
      
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

      // If we're on the login or callback page, skip validation
      if (location.pathname === '/login' || location.pathname === '/callback') {
        return true;
      }

      // If there's no auth state but we're not on a protected route, that's okay
      if (!storedAuthState) {
        console.log('No auth state found, but might be on public route');
        return true;
      }

      // Parse and validate auth state
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

      return true;
    } catch (error) {
      console.error('Error validating auth state:', error);
      return false;
    }
  };

  const checkTokenExpiration = useCallback(async () => {
    if (!validateAuthState()) {
      logout(); // Auth state validation failed
      return false;
    }

    const authState = JSON.parse(localStorage.getItem('authState'));

    try {
      const decoded = jwtDecode(authState.token);
      const currentTime = Date.now() / 1000;

      if (decoded.exp <= currentTime) {
        if (refreshing) return false;
        
        setRefreshing(true);
        const refreshed = await handleRefreshToken();
        if (!refreshed) {
          console.error('Token refresh failed, logging out');
          logout();
          return false;
        } else {
          console.log('Token refresh successful');
          // Verify refresh result
          if (!validateAuthState()) {
            console.error('Auth state invalid after refresh');
            logout();
            return false;
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
      return false;
    } finally {
      setRefreshing(false);
    }

    return true;
  }, [refreshing]);

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
      // Check token expiration immediately
      checkTokenExpiration();
      
      // Set up a longer interval for token checks (e.g., every 5 minutes)
      const interval = setInterval(checkTokenExpiration, 300000); // 5 minutes
      
      return () => {
        clearInterval(interval);
      };
    }
  }, [isAuthenticated]);

  const register = async (username, email, password) => {
    try {
      const response = await authService.registerUser(username, email, password);
      
      if (response.data && response.data.token) {
        // Create auth state with all necessary info from the response
        const authState = {
          user: response.data.user,
          token: response.data.token,
          refreshToken: response.data.refreshToken,
          isAuthenticated: true
        };

        // Update context state
        setUser(authState.user);
        setToken(authState.token);
        setIsAuthenticated(true);

        // Store single source of truth
        localStorage.setItem('authState', JSON.stringify(authState));

        await loadEnvironment();

        // Add a small delay to ensure state is updated before navigation
        setTimeout(() => {
          localStorage.setItem('lastLoginTime', new Date().toISOString());
          navigate('/dashboard');
        }, 100);
        
        return true;
      } else {
        throw new Error('Invalid registration response - no token received');
      }
    } catch (err) {
      console.error('Registration error:', err);
      throw err;
    }
  };

  const login = async (username, password) => {
    try {
      const response = await authService.loginUser(username, password);
      if (response.data && response.data.token) {
        const authState = {
          user: response.data.user,
          token: response.data.token,
          refreshToken: response.data.refreshToken,
          isAuthenticated: true
        };

        setUser(authState.user);
        setToken(authState.token);
        setIsAuthenticated(true);
        localStorage.setItem('authState', JSON.stringify(authState));

        await loadEnvironment();
        
        setTimeout(() => {
          localStorage.setItem('lastLoginTime', new Date().toISOString());
          navigate('/dashboard');
        }, 100);

        return true;
      } else {
        // Handle specific error cases from the backend
        if (response.error) {
          throw new Error(response.error);
        }
        throw new Error('Authentication failed');
      }
    } catch (error) {
      console.error('Login error:', error);
      // Show the error message from the server or a fallback message
      showErrorToast(error.message || 'Authentication failed');
      throw error;
    }
  };

  const handleSSOCallback = useCallback(async (code) => {
    try {
      // Get and validate state parameter
      const params = new URLSearchParams(window.location.search);
      const receivedState = params.get('state');
      const storedStateData = JSON.parse(sessionStorage.getItem('oauth_state') || '{}');
      
      // Validate state parameter
      if (!receivedState || receivedState !== storedStateData.value) {
        console.error('Invalid state parameter');
        navigate('/login');
        return false;
      }

      // Clear state after validation
      sessionStorage.removeItem('oauth_state');

      const response = await authService.handleSSOCallback(code);
      console.log('SSO callback response:', response);

      if (response.success) {  // Check response.success instead of response.data
        const newAuthState = {
          token: response.token,           // Direct from response
          refreshToken: response.refreshToken,  // Direct from response
          user: response.user,             // Direct from response
          isAuthenticated: true
        };

        updateAuthState(newAuthState);

        await loadEnvironment();

        // Navigate after state is updated
        setTimeout(() => {
          // Add this where login is successful in your auth service or context
          localStorage.setItem('lastLoginTime', new Date().toISOString());
          navigate('/dashboard');
        }, 100);
        
        return true;
      }

      if (response.error) {
        if (response.error === "Code already in use") {
          // Clear any existing state before redirecting
          localStorage.removeItem('authState');
          sessionStorage.removeItem('oauth_state');

          const keycloakUrl = process.env.REACT_APP_KEYCLOAK_URL;
          const realm = process.env.REACT_APP_KEYCLOAK_REALM;
          const clientId = process.env.REACT_APP_KEYCLOAK_CLIENT_ID;
          const redirectUri = encodeURIComponent(window.location.origin + '/callback');
          
          // Generate new state with timestamp
          const state = Math.random().toString(36).substring(7);
          const timestamp = Date.now();
          sessionStorage.setItem('oauth_state', JSON.stringify({
            value: state,
            timestamp
          }));

          // Force a fresh login by adding prompt=login and timestamp
          window.location.href = `${keycloakUrl}/realms/${realm}/protocol/openid-connect/auth?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code&state=${state}&scope=openid&prompt=login&timestamp=${timestamp}`;
          return;
        }
        throw new Error(response.error);
      }

      updateAuthState(createAuthState(null, null, false));
      localStorage.removeItem('authState');
      navigate('/login');
      return false;

    } catch (error) {
      console.error('SSO callback error:', error);
      updateAuthState(createAuthState(null, null, false));
      sessionStorage.removeItem('oauth_state');
      localStorage.removeItem('authState');
      navigate('/login');
      return false;
    }
  }, [navigate, updateAuthState]);

  // Add this to check initial auth state
  useEffect(() => {
    const checkAuthState = () => {
      const authState = localStorage.getItem('authState');
      if (authState) {
        const parsed = JSON.parse(authState);
        console.log('Found stored auth state:', parsed);
        setUser(parsed.user);
        setToken(parsed.token);
        setIsAuthenticated(true);
      }
    };
    
    checkAuthState();
  }, []);

  const handleSSOLogin = async () => {
    console.log('handleSSOLogin');
    
    // Check if we already have valid auth
    const currentAuthState = localStorage.getItem('authState');
    if (currentAuthState) {
      const isValid = await checkTokenExpiration();
      if (isValid) {
        console.log('Valid auth found, redirecting to dashboard');
        navigate('/dashboard');
        return;
      }
    }

    // Clear any existing invalid auth state before SSO login
    localStorage.removeItem('authState');
    sessionStorage.removeItem('oauth_state');

    // Generate new state for PKCE with timestamp
    const state = Math.random().toString(36).substring(7);
    const timestamp = Date.now();
    const stateData = {
      value: state,
      timestamp
    };
    sessionStorage.setItem('oauth_state', JSON.stringify(stateData));
    
    // Get environment variables
    const keycloakUrl = process.env.REACT_APP_KEYCLOAK_URL;
    const clientId = process.env.REACT_APP_KEYCLOAK_CLIENT_ID;
    const redirectUri = encodeURIComponent(window.location.origin + '/callback');
    const realm = process.env.REACT_APP_KEYCLOAK_REALM;
    
    // Build auth URL with state parameter and timestamp
    const authUrl = `${keycloakUrl}/realms/${realm}/protocol/openid-connect/auth?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code&state=${state}&scope=openid&timestamp=${timestamp}`;
    
    // Redirect to Keycloak
    window.location.href = authUrl;
  };

  const setupRefreshTokenTimer = useCallback((token) => {
    try {
      if (!token) return;
      
      console.log('Setting up refresh timer...');
      
      // Clear any existing timer
      if (refreshTimerRef.current) {
        console.log('Clearing existing timer');
        clearTimeout(refreshTimerRef.current);
      }

      const decodedToken = jwtDecode(token);
      const expiresIn = decodedToken.exp * 1000;
      const currentTime = Date.now();
      const timeUntilExpiry = expiresIn - currentTime;
      
      if (timeUntilExpiry < 30000) {
        console.log('Token expired or expiring soon, logging out');
        logout();
        return;
      }

      const refreshTime = timeUntilExpiry - 120000;
      console.log('Setting up new refresh timer for', refreshTime / 1000, 'seconds');
      
      refreshTimerRef.current = setTimeout(() => {
        console.log('Refresh timer triggered');
        refreshToken();
      }, refreshTime);

    } catch (error) {
      console.error('Error setting up refresh timer:', error);
    }
  }, [logout]);

  const refreshToken = useCallback(async () => {
    if (refreshing) {
      console.log('Token refresh already in progress, skipping');
      return;
    }

    try {
      setRefreshing(true);
      const currentAuthState = JSON.parse(localStorage.getItem('authState') || '{}');
      if (!currentAuthState.refreshToken) {
        logout();
        return;
      }

      const response = await authService.refreshToken(currentAuthState.refreshToken);
      
      if (response.success && response.data) {
        const { token, refreshToken, user } = response.data;
        
        const newAuthState = {
          user,
          token,
          refreshToken,
          isAuthenticated: true
        };

        localStorage.setItem('authState', JSON.stringify(newAuthState));
        updateAuthState(newAuthState);
        setupRefreshTokenTimer(token);
      } else {
        logout();
      }
    } catch (error) {
      console.error('Error refreshing token:', error);
      logout();
    } finally {
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    const initAuth = () => {
      const authState = JSON.parse(localStorage.getItem('authState') || '{}');
      if (authState.token) {
        setUser(authState.user);
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  // Add cleanup in useEffect
  useEffect(() => {
    return () => {
      // Cleanup timer on unmount
      if (refreshTimerRef.current) {
        clearTimeout(refreshTimerRef.current);
      }
    };
  }, []);

  // Add this single useEffect
  useEffect(() => {
    authService.setupAuthInterceptors(logout);
  }, [logout]);

  const value = {
    user,
    loading,
    isAuthenticated: !!user,
    login,
    logout,
    register,
    checkAuth,
    handleRefreshToken,
    handleSSOLogin,
    handleSSOCallback
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};