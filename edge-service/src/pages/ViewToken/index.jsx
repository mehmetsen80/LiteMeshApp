import React, { useState, useEffect } from 'react';
import { Container, Form } from 'react-bootstrap';
import { useAuth } from '../../contexts/AuthContext';
import authService from '../../services/authService';
import { jwtDecode } from 'jwt-decode';
import Button from '../../components/common/Button';
import './styles.css';

const ViewToken = () => {
  const { handleRefreshToken } = useAuth();
  const [loading, setLoading] = useState(false);
  const [tokenInfo, setTokenInfo] = useState(() => {
    const authState = JSON.parse(localStorage.getItem('authState') || '{}');
    return {
      accessToken: authState.token || 'Not set',
      refreshToken: localStorage.getItem('refreshToken') || 'Not set',
      lastUpdated: new Date().toLocaleTimeString(),
      username: authState.user?.username || 'Unknown',
      isAuthenticated: !!authState.token,
      tokenExpiry: authState.token ? getExpiryTime(authState.token) : 'Unknown'
    };
  });

  // Helper function to get token expiry time
  function getExpiryTime(token) {
    try {
      const decoded = jwtDecode(token);
      return new Date(decoded.exp * 1000).toLocaleString();
    } catch (error) {
      return 'Invalid token';
    }
  }

  useEffect(() => {
    const updateTokenInfo = () => {
      const authState = JSON.parse(localStorage.getItem('authState') || '{}');
      setTokenInfo({
        accessToken: authState.token || 'Not set',
        refreshToken: localStorage.getItem('refreshToken') || 'Not set',
        lastUpdated: new Date().toLocaleTimeString(),
        username: authState.user?.username || 'Unknown',
        isAuthenticated: !!authState.token,
        tokenExpiry: authState.token ? getExpiryTime(authState.token) : 'Unknown'
      });
    };

    // Update immediately
    updateTokenInfo();

    // Set up listener for storage changes
    window.addEventListener('storage', updateTokenInfo);
    return () => window.removeEventListener('storage', updateTokenInfo);
  }, []);

  useEffect(() => {
    console.log('Button should be:', {
      disabled: !localStorage.getItem('refreshToken'),
      refreshTokenExists: !!localStorage.getItem('refreshToken'),
      refreshTokenValue: localStorage.getItem('refreshToken')?.substring(0, 10) + '...'
    });
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const success = await handleRefreshToken();
      if (success) {
        // Update the token info display after successful refresh
        const authState = JSON.parse(localStorage.getItem('authState') || '{}');
        setTokenInfo({
          accessToken: authState.token || 'Not set',
          refreshToken: localStorage.getItem('refreshToken') || 'Not set',
          lastUpdated: new Date().toLocaleTimeString(),
          username: authState.user?.username || 'Unknown',
          isAuthenticated: !!authState.token,
          tokenExpiry: authState.token ? getExpiryTime(authState.token) : 'Unknown'
        });
      }
    } catch (error) {
      console.error('Failed to refresh token:', error);
    } finally {
      setLoading(false);
    }
  };

  const simulateExpiredToken = () => {
    const token = localStorage.getItem('accessToken');
    if (!token) return;

    try {
      const [header, payload, signature] = token.split('.');
      
      // Decode the payload
      const decodedPayload = JSON.parse(atob(payload));
      
      // Modify the exp claim to be in the past
      decodedPayload.exp = Math.floor(Date.now() / 1000) - 3600; // 1 hour ago
      
      // Encode the modified payload
      const modifiedPayload = btoa(JSON.stringify(decodedPayload));
      
      // Reconstruct the token
      const modifiedToken = `${header}.${modifiedPayload}.${signature}`;
      
      // Store the modified token
      localStorage.setItem('accessToken', modifiedToken);
      
      // Update the token info display
      const authState = JSON.parse(localStorage.getItem('authState') || '{}');
      setTokenInfo(prev => ({
        ...prev,
        accessToken: modifiedToken,
        tokenExpiry: getExpiryTime(modifiedToken),
        lastUpdated: new Date().toLocaleTimeString()
      }));

    } catch (error) {
      console.error('Error simulating token expiration:', error);
    }
  };

  return (
    <Container className="token-view-container">
      <h2>Token Information</h2>
      <Form onSubmit={handleSubmit}>
        <Form.Group className="mb-3">
          <Form.Label>Username</Form.Label>
          <Form.Control type="text" value={tokenInfo.username} readOnly />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Authentication Status</Form.Label>
          <Form.Control 
            type="text" 
            value={tokenInfo.isAuthenticated ? 'Authenticated' : 'Not Authenticated'} 
            readOnly 
          />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Last Updated</Form.Label>
          <Form.Control type="text" value={tokenInfo.lastUpdated} readOnly />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Token Expiry</Form.Label>
          <Form.Control type="text" value={tokenInfo.tokenExpiry} readOnly />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Access Token</Form.Label>
          <Form.Control 
            as="textarea" 
            rows={4} 
            value={tokenInfo.accessToken} 
            readOnly 
          />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Refresh Token</Form.Label>
          <Form.Control 
            as="textarea" 
            rows={4} 
            value={tokenInfo.refreshToken} 
            readOnly 
          />
        </Form.Group>

        <div className="d-flex gap-2">
          <Button 
            type="submit"
            variant="primary"
            loading={loading}
            disabled={loading || !localStorage.getItem('refreshToken')}
          >
            {loading ? 'Refreshing...' : 'Refresh Token'}
          </Button>

          <Button
            type="button"
            variant="secondary"
            onClick={simulateExpiredToken}
            disabled={loading || !tokenInfo.accessToken || tokenInfo.accessToken === 'Not set'}
          >
            Simulate Expired Token
          </Button>
        </div>
      </Form>
    </Container>
  );
};

export default ViewToken; 