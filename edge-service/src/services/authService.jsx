import axiosInstance from './axiosInstance';

const authService = {
  registerUser: async (username, email, password) => {
    try {
      console.log('Making registration request:', { username, email });
      const response = await axiosInstance.post('/api/auth/register', {
        username,
        email,
        password
      });
      const data = response.data;
      console.log('Registration response:', data);

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

  loginUser: async (email, password) => {
    try {
      const response = await axiosInstance.post('/api/auth/login', {
        username: email,
        password
      });
      const data = response.data;

      return { data };
    } catch (error) {
      console.error('Login error:', error);
      if (error.response) {
        return { error: error.response.data.message || 'Login failed' };
      }
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

  handleSSOCallback: async (code) => {
    // Add a flag to localStorage to prevent duplicate requests
    const processingKey = `processing_${code}`;
    console.log('Checking processing state:', {
      code: code?.substring(0, 10) + '...',
      isProcessing: localStorage.getItem(processingKey)
    });
    
    if (localStorage.getItem(processingKey)) {
      console.log('SSO callback already in progress for this code');
      return null;
    }
    
    localStorage.setItem(processingKey, 'true');
    
    try {
      console.log('Sending SSO callback request to backend:', {
        url: '/api/auth/sso/callback',
        code: code?.substring(0, 10) + '...',
        fullCode: code
      });

      const response = await axiosInstance.post('/api/auth/sso/callback', { code });
      console.log('Backend response:', {
        status: response.status,
        data: response.data
      });
      return response.data;
    } catch (error) {
      // Check if this is a "Code already in use" error
      if (error.response?.data?.code === 'AUTHENTICATION_ERROR' && 
          error.response?.data?.message === 'Code already in use') {
        console.log('Code already used but token exists, proceeding...');
        return null;
      }
      console.error('SSO callback error:', {
        status: error.response?.status,
        data: error.response?.data,
        message: error.message
      });
      throw error;
    } finally {
      localStorage.removeItem(processingKey);
    }
  },
};

export default authService;