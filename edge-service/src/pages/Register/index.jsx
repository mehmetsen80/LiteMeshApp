import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Form, Button, Alert } from 'react-bootstrap';
import { useAuth } from '../../contexts/AuthContext';
import authService from '../../services/authService';
import './styles.css';

function Register() {
  const { register } = useAuth();
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const [passwordStrength, setPasswordStrength] = useState({
    isStrong: false,
    error: null
  });
  const [usernameError, setUsernameError] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Username validation
  useEffect(() => {
    if (formData.username) {
      setUsernameError(
        formData.username.length < 6 
          ? 'Username must be at least 6 characters long'
          : ''
      );
    } else {
      setUsernameError('');
    }
  }, [formData.username]);

  // Password validation
  useEffect(() => {
    const validatePassword = async () => {
      if (formData.password) {
        try {
          const { data, error } = await authService.validatePassword(formData.password);
          if (error) {
            setPasswordStrength({ isStrong: false, error });
            return;
          }
          setPasswordStrength(data);
        } catch (err) {
          setPasswordStrength({ 
            isStrong: false, 
            error: 'Password validation failed' 
          });
        }
      } else {
        setPasswordStrength({ isStrong: false, error: null });
      }
    };
    
    const timeoutId = setTimeout(validatePassword, 500);
    return () => clearTimeout(timeoutId);
  }, [formData.password]);

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

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      setLoading(false);
      return;
    }

    if (!passwordStrength.isStrong) {
      setError('Please ensure your password meets all requirements');
      setLoading(false);
      return;
    }

    try {
      const { error } = await register(
        formData.username,
        formData.email,
        formData.password
      );
      
      if (error) {
        setError(error);
        return;
      }
    } catch (err) {
      setError(err.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-form-section">
        <div className="auth-card">
          <div className="auth-header">
            <h2>Create Account</h2>
            <p className="text-muted">Join us to monitor your microservices</p>
          </div>

          {error && (
            <Alert variant="danger">{error}</Alert>
          )}

          <Form onSubmit={handleSubmit}>
            <Form.Group className="mb-3">
              <Form.Label>Username</Form.Label>
              <Form.Control
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                required
                placeholder="Enter username"
                disabled={loading}
              />
              {usernameError && (
                <Form.Text className="text-danger">
                  {usernameError}
                </Form.Text>
              )}
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Email</Form.Label>
              <Form.Control
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                required
                placeholder="Enter email"
                disabled={loading}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Password</Form.Label>
              <Form.Control
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                required
                placeholder="Enter password"
                disabled={loading}
              />
              {formData.password && (
                <div className="password-strength-container">
                  <div className={`password-strength ${passwordStrength.isStrong ? 'strong' : 'weak'}`}>
                    {passwordStrength.error || (passwordStrength.isStrong ? 'Password is strong' : 'Password is weak')}
                  </div>
                </div>
              )}
            </Form.Group>

            <Form.Group className="mb-4">
              <Form.Label>Confirm Password</Form.Label>
              <Form.Control
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                required
                placeholder="Confirm password"
                disabled={loading}
              />
            </Form.Group>

            <Button 
              type="submit" 
              className="w-100"
              disabled={loading || usernameError || !passwordStrength.isStrong}
            >
              {loading ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  Creating Account...
                </>
              ) : (
                'Create Account'
              )}
            </Button>
          </Form>

          <div className="mt-4 text-center">
            <p className="mb-0">
              Already have an account? <Link to="/login">Login</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Register;