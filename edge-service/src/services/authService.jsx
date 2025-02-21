import axiosInstance from './axiosInstance';
import { jwtDecode } from 'jwt-decode';

const authService = {
  registerUser: async (username, email, password) => {
    try {
      const response = await axiosInstance.post('/api/auth/register', {
        username,
        email,
        password
      });
      const data = response.data;

      if (!data.token || !data.username) {
        console.error('Invalid registration response:', data);
        return { error: 'Invalid response from server' };
      }

      return { data };
    } catch (error) {
      console.error('Registration error:', error);
      if (error.response) {
        return { error: error.response.data.message || 'Registration failed' };
      }
      return { error: error.message };
    }
  },

  loginUser: async (username, password) => {
    try {
      const response = await axiosInstance.post('/api/auth/login', {
        username,
        password
      });
      
      const data = response.data;
      
      if (data.token) {
        const authState = {
          user: {
            username: data.user.username,
            email: data.user.email,
            roles: data.user.roles || []
          },
          token: data.token,
          refreshToken: data.refreshToken,
          success: data.success,
          message: data.message
        };

        // Store everything with proper keys
        localStorage.setItem('authState', JSON.stringify(authState));
        localStorage.setItem('accessToken', data.token);
        localStorage.setItem('refreshToken', data.refreshToken);

        return { data: authState };
      }
      return { error: 'Invalid response from server' };
    } catch (error) {
      console.error('Login error:', error);
      return { error: error.message };
    }
  },

  validatePassword: async (password) => {
    try {
      const response = await axiosInstance.post('/api/users/validate-password', { password });
      const data = response.data;

      return { data };
    } catch (error) {
      console.error('Password validation error:', error);
      if (error.response) {
        return { error: error.response.data.message || 'Password validation failed' };
      }
      return { error: error.message };
    }
  },

  logout: () => {
    // Clear all auth-related data from localStorage
    localStorage.removeItem('authState');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  },

  handleSSOCallback: async (code) => {
    try {
      const response = await axiosInstance.post('/api/auth/sso/callback', { code });

      if (response.data?.token) {
        const authState = {
          user: response.data.user || {
            username: response.data.username,
            roles: response.data.roles || []
          },
          token: response.data.token,
          isAuthenticated: true
        };
        
        // Store auth state and access token
        localStorage.setItem('authState', JSON.stringify(authState));
        localStorage.setItem('accessToken', response.data.token);
        
        // Store refresh token only if it exists and is different from access token
        if (response.data.refreshToken && response.data.refreshToken !== response.data.token) {
          localStorage.setItem('refreshToken', response.data.refreshToken);//Storing refresh token from SSO
        }

        return response.data;
      }

      throw new Error('Invalid response from SSO callback');
    } catch (error) {
      console.error('SSO callback error:', error);
      authService.logout();
      throw error;
    }
  },

  refreshToken: async (refreshToken) => {
    try {
      const response = await axiosInstance.post('/api/auth/refresh', 
        { refresh_token: refreshToken }
      );

      if (response.data?.token) {
        const authState = {
          user: response.data.user,
          token: response.data.token,
          refreshToken: response.data.refreshToken,
          isAuthenticated: true
        };

        return {
          success: true,
          data: authState
        };
      }
      return {
        success: false,
        error: 'Invalid response structure'
      };
    } catch (error) {
      console.error('Token refresh error:', error);
      return {
        success: false,
        error: error.message || 'Failed to refresh token'
      };
    }
  },

  // Silent refresh - attempts to refresh without showing warnings
  silentRefresh: async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        console.log('No refresh token available');
        return false;
      }

      return await authService.refreshToken(refreshToken);
    } catch (error) {
      console.error('Silent refresh failed:', error);
      return false;
    }
  }
};

export default authService;