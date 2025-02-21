import axios from 'axios';
import authService from './authService';

const API_BASE_URL = import.meta.env.VITE_API_GATEWAY_URL || 'https://localhost:7777';

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          throw new Error('No refresh token available');
        }

        const response = await axiosInstance.post('/api/auth/refresh', {
          token: refreshToken
        });

        if (response.data?.token) {
          localStorage.setItem('accessToken', response.data.token);
          if (response.data.refreshToken) {
            localStorage.setItem('refreshToken', response.data.refreshToken);
          }

          // Update auth state
          const authState = JSON.parse(localStorage.getItem('authState') || '{}');
          authState.token = response.data.token;
          localStorage.setItem('authState', JSON.stringify(authState));

          // Retry original request with new token
          originalRequest.headers.Authorization = `Bearer ${response.data.token}`;
          return axiosInstance(originalRequest);
        }

        throw new Error('Token refresh failed');
      } catch (refreshError) {
        console.error('Token refresh failed:', refreshError);
        authService.logout();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default axiosInstance; 