import React, { useState, useEffect } from 'react';
import { jwtDecode } from 'jwt-decode';
import './styles.css';

const TokenExpiryDisplay = () => {
  const [timeLeft, setTimeLeft] = useState('');
  const [isExpired, setIsExpired] = useState(false);

  useEffect(() => {
    const calculateTimeLeft = () => {
      const authState = JSON.parse(localStorage.getItem('authState') || '{}');
      const token = authState.token;

      if (!token) {
        setTimeLeft('No token');
        setIsExpired(true);
        return;
      }

      try {
        const decoded = jwtDecode(token);
        const expiryTime = decoded.exp * 1000; // Convert to milliseconds
        const now = Date.now();
        const difference = expiryTime - now;

        if (difference <= 0) {
          setTimeLeft('Expired');
          setIsExpired(true);
          return;
        }

        // Convert to minutes and seconds
        const minutes = Math.floor(difference / 60000);
        const seconds = Math.floor((difference % 60000) / 1000);
        setTimeLeft(`${minutes}m ${seconds}s`);
        setIsExpired(false);
      } catch (error) {
        setTimeLeft('Invalid token');
        setIsExpired(true);
      }
    };

    // Update immediately and then every second
    calculateTimeLeft();
    const timer = setInterval(calculateTimeLeft, 1000);

    return () => clearInterval(timer);
  }, []);

  return (
    <div className={`token-expiry-display ${isExpired ? 'expired' : ''}`}>
      <span className="expiry-label">Token expires in:</span>
      <span className="expiry-time">{timeLeft}</span>
    </div>
  );
};

export default TokenExpiryDisplay; 