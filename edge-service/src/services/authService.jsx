import { fetchWithConfig } from '../utils/api';

const authService = {
  register: async (username, email, password) => {
    try {
      console.log('Attempting registration:', { username, email });
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ 
          username,
          email,
          password 
        }),
      });
      console.log('Register response status:', response.status);

      if (!response.ok) {
        const error = await response.json();
        console.log('Register error response:', error);
        if (error.message.includes('Username already exists')) {
          throw new Error('This username is already taken. Please choose another one.');
        }
        if (error.message.includes('Email already exists')) {
          throw new Error('An account with this email already exists. Please use another email or login.');
        }
        throw new Error(error.message || 'Registration failed. Please try again.');
      }

      const data = await response.json();
      console.log('Registration success data:', data);
      return data;
    } catch (error) {
      console.error('Registration error:', error);
      throw error;
    }
  },
  login: async (email, password) => {
    try {
      console.log('Attempting login with:', { email });
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ 
          username: email,
          password 
        }),
      });
      console.log('Login response status:', response.status);

      if (!response.ok) {
        const error = await response.json();
        console.log('Login error response:', error);
        throw new Error(error.message || 'Login failed');
      }

      const data = await response.json();
      console.log('Login success data:', data);
      return data;
    } catch (error) {
      console.error('Login error:', error);
      if (error.message.includes('JSON')) {
        console.error('Response was not valid JSON. Raw response:', await response.text());
      }
      throw error;
    }
  },
  // ... rest of the service
};

export default authService;