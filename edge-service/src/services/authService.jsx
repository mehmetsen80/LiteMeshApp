import axiosInstance from './axiosInstance';

const authService = {
  registerUser: async (username, email, password) => {
    try {
      // Clear existing auth state before registration
      localStorage.removeItem('authState');
      
      const response = await axiosInstance.post('/api/auth/register', {
        username,
        email,
        password
      });
      const data = response.data;

      if (!data.token || !data.user?.username) {
        console.error('Invalid registration response:', data);
        return { error: 'Invalid response from server' };
      }

      // Create and store auth state, just like in loginUser
      const authState = {
        user: data.user,
        token: data.token,
        refreshToken: data.refreshToken,
        isAuthenticated: true
      };

      // Store auth state as single source of truth
      localStorage.setItem('authState', JSON.stringify(authState));

      return { data: authState };
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
      // Clear existing auth state before login attempt
      localStorage.removeItem('authState');
      
      const response = await axiosInstance.post('/api/auth/login', {
        username,
        password
      });
      
      const data = response.data;
      
      if (data.token) {
        const authState = {
          user: data.user,
          token: data.token,
          refreshToken: data.refreshToken,
          isAuthenticated: true
        };

        // Store auth state as single source of truth
        localStorage.setItem('authState', JSON.stringify(authState));

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
    // Clear auth state and team selection
    localStorage.removeItem('authState');
    localStorage.removeItem('currentTeamId');
    sessionStorage.clear();
    
    // Force reload to clear any in-memory state
    window.location.href = '/login';
  },

  handleSSOCallback: async (code) => {
    try {
      const response = await axiosInstance.post('/api/auth/sso/callback', { code });
      return response.data;
    } catch (error) {
      throw error;
    }
  },

  refreshToken: async (refreshToken) => {
    try {
      const response = await axiosInstance.post('/api/auth/refresh', {
        refresh_token: refreshToken
      });

      if (response.data?.token) {
        return {
          success: true,
          data: {
            user: response.data.user,
            token: response.data.token,
            refreshToken: response.data.refreshToken
          }
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

  setupAuthInterceptors: (logout) => {
    axiosInstance.interceptors.response.use(
      (response) => response,
      async (error) => {
        if (error.response?.status === 401) {
          console.log('Received 401 response, logging out...');
          logout();
        }
        return Promise.reject(error);
      }
    );
  }
};

export default authService;