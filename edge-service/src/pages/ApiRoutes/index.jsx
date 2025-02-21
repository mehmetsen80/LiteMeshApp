import React from 'react';
import { ApiRoutesDisplay } from '../../components/apiroutes/ApiRoutesDisplay';
import './styles.css';
import { showSuccessToast, showErrorToast } from '../../utils/toastConfig';

const ApiRoutes = () => {
  const handleCreateRoute = async (routeData) => {
    try {
      console.log('Creating route:', routeData);
      const response = await apiRouteService.createRoute(routeData);
      console.log('Response:', response);
      if (response.message) {
        showSuccessToast(response.message);
      }
      await fetchRoutes();
      if (setShowCreateModal) {
        setShowCreateModal(false);
      }
    } catch (error) {
      console.error('Error creating route:', error);
      showErrorToast(error.message || 'Failed to create route');
    }
  };

  return (
    <div className="api-routes-page">
      <div className="page-header">
        <h1>API Routes Management</h1>
        <p>Configure and monitor your API gateway routes</p>
      </div>
      <ApiRoutesDisplay />
    </div>
  );
};

export default ApiRoutes; 