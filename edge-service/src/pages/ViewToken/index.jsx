import React, { useState, useEffect } from 'react';
import { Container, Form } from 'react-bootstrap';
import { useAuth } from '../../contexts/AuthContext';
import { jwtDecode } from 'jwt-decode';
import Button from '../../components/common/Button';
import './styles.css';

const ViewToken = () => {
  const { handleRefreshToken } = useAuth();
  const [loading, setLoading] = useState(false);

  const getTokenExpiryInfo = (token) => {
    try {
      const decoded = jwtDecode(token);
      const expiresIn = decoded.exp * 1000; // Convert to milliseconds
      const currentTime = Date.now();
      const timeUntilExpiry = expiresIn - currentTime;
      
      return {
        expiryDate: new Date(expiresIn).toLocaleString(),
        minutesRemaining: Math.floor(timeUntilExpiry / 60000),
        isExpired: timeUntilExpiry <= 0
      };
    } catch (error) {
      return {
        expiryDate: 'Invalid token',
        minutesRemaining: 0,
        isExpired: true
      };
    }
  };

  const [tokenInfo, setTokenInfo] = useState(() => {
    const authState = JSON.parse(localStorage.getItem('authState') || '{}');
    const tokenExpiryInfo = getTokenExpiryInfo(authState.token);
    
    return {
      accessToken: authState.token || 'Not set',
      refreshToken: authState.refreshToken || 'Not set',
      lastUpdated: new Date().toLocaleTimeString(),
      username: authState.user?.username || 'Unknown',
      isAuthenticated: !!authState.token,
      tokenExpiry: tokenExpiryInfo.expiryDate,
      minutesRemaining: tokenExpiryInfo.minutesRemaining,
      isExpired: tokenExpiryInfo.isExpired
    };
  });

  useEffect(() => {
    const updateTokenInfo = () => {
      const authState = JSON.parse(localStorage.getItem('authState') || '{}');
      const tokenExpiryInfo = getTokenExpiryInfo(authState.token);
      setTokenInfo({
        accessToken: authState.token || 'Not set',
        refreshToken: authState.refreshToken || 'Not set',
        lastUpdated: new Date().toLocaleTimeString(),
        username: authState.user?.username || 'Unknown',
        isAuthenticated: !!authState.token,
        tokenExpiry: tokenExpiryInfo.expiryDate,
        minutesRemaining: tokenExpiryInfo.minutesRemaining,
        isExpired: tokenExpiryInfo.isExpired
      });
    };

    // Update immediately
    updateTokenInfo();

    // Set up listener for storage changes
    window.addEventListener('storage', updateTokenInfo);
    return () => window.removeEventListener('storage', updateTokenInfo);
  }, []);

  useEffect(() => {
    const authState = JSON.parse(localStorage.getItem('authState') || '{}');
    console.log('Button should be:', {
      disabled: !authState.refreshToken,
      refreshTokenExists: !!authState.refreshToken,
      refreshTokenValue: authState.refreshToken?.substring(0, 10) + '...'
    });
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await handleRefreshToken();
      // Update token info after refresh
      const authState = JSON.parse(localStorage.getItem('authState') || '{}');
      const tokenExpiryInfo = getTokenExpiryInfo(authState.token);
      setTokenInfo({
        accessToken: authState.token || 'Not set',
        refreshToken: authState.refreshToken || 'Not set',
        lastUpdated: new Date().toLocaleTimeString(),
        username: authState.user?.username || 'Unknown',
        isAuthenticated: !!authState.token,
        tokenExpiry: tokenExpiryInfo.expiryDate,
        minutesRemaining: tokenExpiryInfo.minutesRemaining,
        isExpired: tokenExpiryInfo.isExpired
      });
    } catch (error) {
      console.error('Error refreshing token:', error);
    } finally {
      setLoading(false);
    }
  };

  const simulateExpiredToken = () => {
    console.log('Simulating expired token');
    // Get current auth state
    const authState = JSON.parse(localStorage.getItem('authState') || '{}');
    const token = authState.token;
    if (!token) return;
    console.log('Token:', token);

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
      
      // Update the auth state with modified token
      const updatedAuthState = {
        ...authState,
        token: modifiedToken
      };
      console.log('Updated auth state:', updatedAuthState);
      // Store the modified auth state
      localStorage.setItem('authState', JSON.stringify(updatedAuthState));
      
      // Update the token info display
      const tokenExpiryInfo = getTokenExpiryInfo(modifiedToken);
      setTokenInfo(prev => ({
        ...prev,
        accessToken: modifiedToken,
        tokenExpiry: tokenExpiryInfo.expiryDate,
        minutesRemaining: tokenExpiryInfo.minutesRemaining,
        isExpired: tokenExpiryInfo.isExpired,
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
          <Form.Control 
            type="text" 
            value={`${tokenInfo.tokenExpiry} (${tokenInfo.minutesRemaining} minutes remaining)`} 
            readOnly 
            className={tokenInfo.minutesRemaining < 5 ? 'text-danger' : ''}
          />
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
            disabled={loading || !tokenInfo.refreshToken || tokenInfo.refreshToken === 'Not set'}
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