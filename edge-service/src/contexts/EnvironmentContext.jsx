import React, { createContext, useContext, useState, useEffect } from 'react';
import { environmentService } from '../services/environmentService';
const EnvironmentContext = createContext(null);

export const EnvironmentProvider = ({ children }) => {
    const [environment, setEnvironment] = useState({
        profile: 'unknown',
        isLoading: true,
        error: null
    });

    const loadEnvironment = async () => {
        try {
            await environmentService.fetchEnvironmentInfo();
            setEnvironment({
                profile: environmentService.getProfile(),
                isLoading: false,
                error: null
            });
        } catch (error) {
            setEnvironment({
                profile: 'unknown',
                isLoading: false,
                error: error.message
            });
        }
    };

    useEffect(() => {
        loadEnvironment();
    }, []);

    const value = {
        ...environment,
        isProd: () => environmentService.isProd(),
        isDev: () => environmentService.isDev(),
        isTest: () => environmentService.isTest(),
        loadEnvironment
    };

    return (
        <EnvironmentContext.Provider value={value}>
            {children}
        </EnvironmentContext.Provider>
    );
};

export const useEnvironment = () => {
    const context = useContext(EnvironmentContext);
    if (!context) {
        throw new Error('useEnvironment must be used within an EnvironmentProvider');
    }
    return context;
};