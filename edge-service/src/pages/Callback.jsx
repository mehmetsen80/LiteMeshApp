import React, { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const Callback = () => {
  const [searchParams] = useSearchParams();
  const { handleSSOCallback } = useAuth();

  useEffect(() => {
    const processCallback = async () => {
      const code = searchParams.get('code');
      if (code) {
        try {
          await handleSSOCallback(code);
        } catch (error) {
          console.error('SSO callback error:', error);
        }
      }
    };

    processCallback();
  }, [searchParams, handleSSOCallback]);

  return (
    <div className="callback-container">
      <p>Processing login...</p>
    </div>
  );
};

export default Callback; 