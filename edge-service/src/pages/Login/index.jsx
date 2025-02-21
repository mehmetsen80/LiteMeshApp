import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Form, Alert } from 'react-bootstrap';
import { HiLockClosed, HiHome } from 'react-icons/hi';
import { useAuth } from '../../contexts/AuthContext';
import Button from '../../components/common/Button';
import './styles.css';

function Login() {
  const { login, handleSSOLogin } = useAuth();
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

  return (
    <div className="auth-container">
      <Link to="/" className="home-link">
        <HiHome size={20} />
        Home
      </Link>      
      <div className="auth-form-section">
        <div className="auth-card">
          <div className="auth-header">
            <h2>Welcome Back</h2>
            <p className="text-muted">Please sign in to continue</p>
          </div>
          
          <Button 
            variant="secondary"
            fullWidth
            onClick={handleSSOLogin}
          >
            <HiLockClosed />
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
              variant="primary"
              fullWidth
              loading={loading}
            >
              Sign In
            </Button>
          </Form>

          <div className="mt-4 text-center">
            <p className="mb-0">
              Don't have an account? <Link to="/register" className="primary-link">Register</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;