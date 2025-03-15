import React from 'react';
import { Navigate } from 'react-router-dom';
import { useEnvironment } from '../../contexts/EnvironmentContext';
import { Card } from 'react-bootstrap';

const NonProdGuard = ({ children }) => {
  const { isProd, isLoading } = useEnvironment();

  if (isLoading) {
    return <Card><Card.Body>Loading...</Card.Body></Card>;
  }

  if (isProd()) {
    return <Navigate to="/404" replace />;
  }

  return children;
};

export default NonProdGuard; 