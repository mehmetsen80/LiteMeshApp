import React from 'react';
import { Alert } from 'react-bootstrap';
import { HiExclamationCircle } from 'react-icons/hi';
import { useNavigate } from 'react-router-dom';
import Button from './Button';

const NotFound = () => {
  const navigate = useNavigate();

  return (
    <div className="not-found-container">
      <Alert variant="warning" className="text-center">
        <HiExclamationCircle className="not-found-icon mb-3" />
        <Alert.Heading>Page Not Found</Alert.Heading>
        <p>
          The page you are looking for does not exist or has been moved.
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

export default NotFound; 