import React from 'react';
import { Alert } from 'react-bootstrap';
import { HiShieldExclamation } from 'react-icons/hi';
import { useNavigate } from 'react-router-dom';
import Button from './Button';

const NotAuthorized = () => {
  const navigate = useNavigate();

  return (
    <div className="not-authorized-container">
      <Alert variant="warning" className="text-center">
        <HiShieldExclamation className="not-authorized-icon mb-3" />
        <Alert.Heading>Not Authorized</Alert.Heading>
        <p>
          You don't have permission to access this page. This area is restricted to administrators only.
        </p>
        <Button 
          variant="primary" 
          className="mt-3"
          onClick={() => navigate('/dashboard')}
        >
          Return to Dashboard
        </Button>
      </Alert>
    </div>
  );
};

export default NotAuthorized; 