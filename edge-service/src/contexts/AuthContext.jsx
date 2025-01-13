import { createContext, useContext, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';

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
  
  const navigate = useNavigate();

  useEffect(() => {
    if (user) {
      localStorage.setItem('user', JSON.stringify(user));
    } else {
      localStorage.removeItem('user');
    }
  }, [user]);

  const register = async (username, email, password) => {
    try {
      const data = await authService.register(username, email, password);
      console.log('Setting auth state after registration:', data);
      setUser({ username: data.username });
      setToken(data.token);
      localStorage.setItem('token', data.token);
      localStorage.setItem('username', data.username);
      localStorage.setItem('user', JSON.stringify({ 
        user: { username: data.username },
        token: data.token 
      }));
      return data;
    } catch (error) {
      console.error('Registration error:', error);
      throw error;
    }
  };

  const login = async (username, password) => {
    try {
      const data = await authService.login(username, password);
      console.log('Setting auth state with:', data);
      setUser({ username: data.username });
      setToken(data.token);
      localStorage.setItem('token', data.token);
      localStorage.setItem('username', data.username);
      localStorage.setItem('user', JSON.stringify({ 
        user: { username: data.username },
        token: data.token 
      }));
      return data;
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    navigate('/login');
  };

  return (
    <AuthContext.Provider value={{ 
      user: user?.user || user,
      token,
      register,
      login, 
      logout, 
      isAuthenticated: !!(user?.user || user)
    }}>
      {children}
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