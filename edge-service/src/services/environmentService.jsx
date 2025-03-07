import axiosInstance from './axiosInstance';

class EnvironmentService {
    constructor() {
        this.environmentInfo = null;
        this.error = null;
        this.isLoading = false;
        this.isFetching = false;
    }

    async fetchEnvironmentInfo() {
        // Check if we have auth state before making the request
        const authState = JSON.parse(localStorage.getItem('authState') || '{}');
        if (!authState.isAuthenticated) {
            return;
        }

        // Prevent multiple simultaneous calls
        if (this.isFetching) {
            console.log("Environment fetch already in progress");
            return;
        }

        console.log("Fetching environment info");
        this.isLoading = true;
        this.isFetching = true;

        try {
            const response = await axiosInstance.get('/api/environment/profile');
            this.environmentInfo = response.data;
            return this.environmentInfo;
        } catch (error) {
            this.error = error;
            console.error('Failed to fetch environment info:', error);
            throw error;
        } finally {
            this.isLoading = false;
            this.isFetching = false;
        }
    }

    getEnvironmentInfo() {
        return this.environmentInfo;
    }

    clearEnvironmentInfo() {
        this.environmentInfo = null;
        this.error = null;
        this.isLoading = false;
        this.isFetching = false;
    }

    isProd() {
        return this.environmentInfo?.profile === 'prod';
    }

    isDev() {
        return this.environmentInfo?.profile === 'dev';
    }

    isTest() {
        return this.environmentInfo?.profile === 'test';
    }

    getProfile() {
        return this.environmentInfo?.profile || 'unknown';
    }
}

export const environmentService = new EnvironmentService();
