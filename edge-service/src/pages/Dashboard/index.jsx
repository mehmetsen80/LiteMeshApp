import React from 'react';
import StatsSection from '../../components/dashboard/StatsSection';
import LatencyChart from '../../components/dashboard/LatencyChart';
import ModulesSection from '../../components/dashboard/ModulesSection';
import ServiceUsagePie from '../../components/dashboard/ServiceUsagePie';
import './styles.css';

function Dashboard() {
  return (
    <div className="dashboard-container">
      <h1>Welcome to LiteMesh</h1>
      <p className="welcome-text">Your API Gateway Management Dashboard</p>
      <StatsSection />
      <div className="dashboard-charts">
        <LatencyChart />
        <ServiceUsagePie />
      </div>
      <ModulesSection />
    </div>
  );
}

export default Dashboard;