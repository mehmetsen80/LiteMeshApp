import axios from 'axios';
import authService from './authService';
import { jwtDecode } from 'jwt-decode';

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_GATEWAY_URL || 'https://localhost:7777',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
});

// Request interceptor
axiosInstance.interceptors.request.use(
  (config) => {
    const authData = JSON.parse(localStorage.getItem('authState') || '{}');
    if (authData.token) {
      try {
        const decoded = jwtDecode(authData.token);
        const currentTime = Date.now() / 1000;
        
        if (decoded.exp < currentTime) {
          // Token is expired
          authService.logout();
          window.location.href = '/login';
          return Promise.reject('Token expired');
        }
        
        config.headers.Authorization = `Bearer ${authData.token}`;
      } catch (error) {
        // Invalid token
        authService.logout();
        window.location.href = '/login';
        return Promise.reject('Invalid token');
      }
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    // Extract error message from response if available
    if (error.response?.data?.message) {
      error.message = error.response.data.message;
    }
    
    // Only attempt refresh if:
    // 1. It's a 401 error
    // 2. We haven't tried to refresh for this request yet
    // 3. We're not on the login page
    // 4. We have a refresh token
    // 5. The request is not for /api/auth/login or /api/auth/refresh
    if (error.response?.status === 401 && 
        !originalRequest._retry && 
        window.location.pathname !== '/login' &&
        !originalRequest.url.includes('/api/auth/')) {
      
      originalRequest._retry = true;
      
      try {
        const authData = JSON.parse(localStorage.getItem('authState') || '{}');
        if (!authData.refreshToken) {
          return Promise.reject(error);
        }

        const response = await authService.refreshToken(authData.refreshToken);
        if (response.data?.token) {
          const newAuthState = {
            user: response.data.user,
            token: response.data.token,
            refreshToken: response.data.refreshToken,
            isAuthenticated: true
          };
          localStorage.setItem('authState', JSON.stringify(newAuthState));
          
          // Update headers for the retry
          axiosInstance.defaults.headers.Authorization = `Bearer ${response.data.token}`;
          originalRequest.headers.Authorization = `Bearer ${response.data.token}`;
          
          // Retry the original request
          return axiosInstance(originalRequest);
        }
      } catch (refreshError) {
        console.log('Token refresh failed:', refreshError);
        // Clear auth data and redirect to login
        authService.logout();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export default axiosInstance; 