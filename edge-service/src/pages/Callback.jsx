import { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

function Callback() {
  try {
    const { handleSSOCallback } = useAuth();
    const navigate = useNavigate();
    const [isProcessing, setIsProcessing] = useState(false);
    const codeRef = useRef(null);
    const navigationAttempted = useRef(false);
    const processedRef = useRef(false);

    useEffect(() => {
      const handleCallback = async () => {
        if (isProcessing || processedRef.current || navigationAttempted.current) {
          return;
        }

        const urlParams = new URLSearchParams(window.location.search);
        const code = urlParams.get('code');
        
        // Prevent duplicate processing of the same code
        if (codeRef.current === code) {
          return;//ode already being processed
        }
        codeRef.current = code;
        
        // Check if this code has already been processed
        const processingKey = `processing_${code}`;
        if (localStorage.getItem(processingKey)) {
          window.location.href = '/';//Code already being processed, redirecting to home
          return;
        }

        const state = urlParams.get('state');
        const savedStateData = sessionStorage.getItem('oauth_state');
        let savedState;
        
        try {//Starting callback processing
          const stateObj = JSON.parse(savedStateData);
          // Check if state is not expired (30 minutes)
          if (Date.now() - stateObj.timestamp > 30 * 60 * 1000) {
            navigate('/login');//State expired
            return;
          }
          savedState = stateObj.value;
        } catch (e) {
          // If we have a code but no state, proceed anyway
          if (code) {
            savedState = state;//No state in session storage, but proceeding with code
          } else {
            navigate('/login');//Invalid state data in session storage
            return;
          }
        }

        if (state !== savedState) {
          navigate('/login');// console.error('State mismatch, possible CSRF attack');
          return;
        }

        // Clear the state from session storage
        sessionStorage.removeItem('oauth_state');

        if (code) {
          try {
            setIsProcessing(true);
            processedRef.current = true;
            const response = await handleSSOCallback(code);
            if (response.success) {
              // Wait for state to be fully updated
              await new Promise(resolve => setTimeout(resolve, 500));
              // Use navigate for smoother transition since state is now consistent
              navigate('/', { replace: true });//avigating to home
            } else {
              navigate('/login', { replace: true });//SSO failed
            }
          } catch (error) {
            navigate('/login', { replace: true });//Callback error
          } finally {
            setIsProcessing(false);
          }
        } else {
          navigate('/login', { replace: true });//No code found in callback URL
        }
      };

      handleCallback();
    }, [handleSSOCallback, navigate, isProcessing]);

    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        {isProcessing ? (
          <div>Authenticating...</div>
        ) : (
          <div>Redirecting to dashboard...</div>
        )}
      </div>
    );
  } catch (error) {
    console.error('Error in Callback component:', error);
    throw error;
  }
}

export default Callback; 