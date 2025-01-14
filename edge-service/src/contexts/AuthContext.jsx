import React, { createContext, useContext, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';
import { jwtDecode } from "jwt-decode";

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const savedAuth = localStorage.getItem('user');
    if (savedAuth) {
      const parsedAuth = JSON.parse(savedAuth);
      return parsedAuth;
    }
    return null;
  });
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);
  
  const navigate = useNavigate();

  useEffect(() => {
    const checkTokenExpiration = async () => {
      if (token) {
        try {
          const decodedToken = jwtDecode(token);
          const currentTime = Date.now() / 1000;
          
          if (decodedToken.exp - currentTime < 300) {
            logout();
          }
        } catch (error) {
          logout();
        }
      }
      setLoading(false);
    };
    
    checkTokenExpiration();
    const interval = setInterval(checkTokenExpiration, 15 * 60 * 1000);
    
    return () => clearInterval(interval);
  }, [token]);

  useEffect(() => {
    if (user) {
      localStorage.setItem('user', JSON.stringify(user));
    } else {
      localStorage.removeItem('user');
    }
  }, [user]);

  const register = async (username, email, password) => {
    const { data, error } = await authService.register(username, email, password);
    if (error) {
      return { error };
    }
    
    setUser({ username: data.username });
    setToken(data.token);
    localStorage.setItem('token', data.token);
    localStorage.setItem('username', data.username);
    localStorage.setItem('user', JSON.stringify({ 
      user: { username: data.username },
      token: data.token 
    }));
    return { data };
  };

  const login = async (username, password) => {
    const { data, error } = await authService.login(username, password);
    
    if (error) {
      return { error };
    }
    
    setUser({ username: data.username });
    setToken(data.token);
    localStorage.setItem('token', data.token);
    localStorage.setItem('username', data.username);
    localStorage.setItem('user', JSON.stringify({ 
      user: { username: data.username },
      token: data.token 
    }));
    return { data };
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    navigate('/login');
  };

  const checkAuth = () => {
    if (!token || !user) return false;
    try {
      const decodedToken = jwtDecode(token);
      return decodedToken.exp > Date.now() / 1000;
    } catch {
      return false;
    }
  };

  return (
    <AuthContext.Provider value={{ 
      user: user?.user || user,
      token,
      register,
      login, 
      logout, 
      isAuthenticated: checkAuth(),
      loading
    }}>
      {!loading ? children : null}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};