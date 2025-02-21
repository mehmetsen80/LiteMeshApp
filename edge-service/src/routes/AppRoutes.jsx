import React from 'react';
import { Routes, Route, Navigate, Outlet } from 'react-router-dom';
import Login from '../pages/Login';
import Register from '../pages/Register';
import Callback from '../pages/Callback';
import Dashboard from '../pages/Dashboard';
import Metrics from '../pages/Metrics';
import ServiceStatus from '../pages/ServiceStatus';
import Alerts from '../pages/Alerts';
import Teams from '../pages/Teams';
import AdminLayout from '../layouts/AdminLayout/index';
import ProtectedRoute from '../components/common/ProtectedRoute';
import Organizations from '../pages/Organizations';
import ApiRoutes from '../pages/ApiRoutes';
import ViewRoute from '../pages/ApiRoutes/ViewRoute';
import EditRoute from '../pages/ApiRoutes/EditRoute';
import Home from '../pages/Home';
import ViewToken from '../pages/ViewToken';

const AppRoutes = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/callback" element={<Callback />} />

      {/* Protected Routes - All under AdminLayout */}
      <Route
        element={
          <ProtectedRoute>
            <AdminLayout>
              <Outlet />
            </AdminLayout>
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/metrics" element={<Metrics />} />
        <Route path="/service-status" element={<ServiceStatus />} />
        <Route path="/teams" element={<Teams />} />
        <Route path="/organizations" element={<Organizations />} />
        <Route path="/api-routes" element={<ApiRoutes />} />
        <Route path="/api-routes/:routeId" element={<ViewRoute />} />
        <Route path="/api-routes/:routeId/edit" element={<EditRoute />} />
        <Route path="/view-token" element={<ViewToken />} />
      </Route>

      {/* Redirect unmatched routes to home */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default AppRoutes; 