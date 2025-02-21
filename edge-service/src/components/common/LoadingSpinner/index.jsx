import React from 'react';
import './styles.css';

export const LoadingSpinner = () => {
  return (
    <div className="spinner-container">
      <div className="spinner">
        <div className="spinner-circle"></div>
        <div className="spinner-circle"></div>
        <div className="spinner-circle"></div>
      </div>
      <p className="loading-text">Loading...</p>
    </div>
  );
};
