const API_BASE_URL = import.meta.env.VITE_API_GATEWAY_URL || 'https://localhost:7777';

const authService = {
  register: async (username, email, password) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ 
          username: email,
          email,
          password 
        }),
      });

      if (!response.ok) {
        const error = await response.json();
        return { error: error.message || 'Registration failed. Please try again.' };
      }

      const data = await response.json();
      return { data };
    } catch (error) {
      if (!navigator.onLine || error instanceof TypeError && error.message === 'Failed to fetch') {
        return { error: 'Unable to connect to the server. Please check your connection.' };
      }
      return { error: error.message };
    }
  },
  login: async (email, password) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username: email, password }),
      });

      const responseData = await response.json();

      if (!response.ok) {
        return { error: responseData.error || responseData.message || 'Login failed' };
      }

      return { data: responseData };
    } catch (error) {
      if (!navigator.onLine || error instanceof TypeError && error.message === 'Failed to fetch') {
        return { error: 'Unable to connect to the server. Please check your connection.' };
      }
      return { error: error.message };
    }
  },
  // ... rest of the service
};

export default authService;