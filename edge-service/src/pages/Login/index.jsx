import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Form, Button, Alert } from 'react-bootstrap';
import { HiLockClosed } from 'react-icons/hi';
import { useAuth } from '../../contexts/AuthContext';
import './styles.css';

function Login() {
  const { login } = useAuth();
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const { error } = await login(formData.email, formData.password);
      
      if (error) {
        setError(error);
        return;
      }
    } catch (err) {
      setError(err.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  const handleSSOLogin = () => {
    const state = Math.random().toString(36).substring(7);
    const stateData = {
      value: state,
      timestamp: Date.now()
    };
    sessionStorage.setItem('oauth_state', JSON.stringify(stateData));
    
    const params = new URLSearchParams({
      client_id: process.env.REACT_APP_KEYCLOAK_CLIENT_ID,
      redirect_uri: `${window.location.origin}/callback`,
      response_type: 'code',
      state: state,
      scope: 'openid'
    });
    
    const authUrl = `${process.env.REACT_APP_KEYCLOAK_URL}/realms/${process.env.REACT_APP_KEYCLOAK_REALM}/protocol/openid-connect/auth`;
    console.log('Redirecting to:', authUrl, 'with params:', params.toString());
    window.location.href = `${authUrl}?${params}`;
  };

  return (
    <div className="auth-container">      
      <div className="auth-form-section">
        <div className="auth-card">
          <div className="auth-header">
            <h2>Welcome Back</h2>
            <p className="text-muted">Please sign in to continue</p>
          </div>
          
          <Button 
            variant="outline-primary" 
            className="w-100 mb-3"
            onClick={handleSSOLogin}
          >
            <HiLockClosed className="me-2" />
            Sign in with SSO
          </Button>
          
          <div className="separator my-3">
            <span className="separator-text">OR</span>
          </div>

          {error && (
            <Alert variant="danger">{error}</Alert>
          )}

          <Form onSubmit={handleSubmit}>
            <Form.Group className="mb-3">
              <Form.Label>Email</Form.Label>
              <Form.Control
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                required
                autoComplete="email"
                placeholder="Enter your email"
                disabled={loading}
              />
            </Form.Group>

            <Form.Group className="mb-4">
              <Form.Label>Password</Form.Label>
              <Form.Control
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                required
                autoComplete="current-password"
                placeholder="Enter your password"
                disabled={loading}
              />
            </Form.Group>

            <Button 
              type="submit" 
              className="w-100"
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  Signing in...
                </>
              ) : (
                'Sign In'
              )}
            </Button>
          </Form>

          <div className="mt-4 text-center">
            <p className="mb-0">
              Don't have an account? <Link to="/register">Register</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;