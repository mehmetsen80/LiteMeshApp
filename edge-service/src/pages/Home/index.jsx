import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import './styles.css';

function Home() {
  const { user } = useAuth();

  return (
    <div className="dashboard-container">
      <div className="welcome-section">
        <h1>Welcome to LiteMesh, {user?.username}!</h1>
        <p>Monitor and manage your microservices from one central dashboard</p>
      </div>

      <div className="dashboard-grid">
        <div className="dashboard-card">
          <Link to="/metrics" className="card-icon-link">
            <div className="card-icon">
              <i className="fas fa-chart-line"></i>
            </div>
          </Link>
          <h3>Metrics Overview</h3>
          <p>View detailed performance metrics and analytics</p>
          <Link to="/metrics" className="card-link">
            View Metrics <i className="fas fa-arrow-right"></i>
          </Link>
        </div>

        <div className="dashboard-card">
          <Link to="/service-status" className="card-icon-link">
            <div className="card-icon">
              <i className="fas fa-server"></i>
            </div>
          </Link>
          <h3>Services Status</h3>
          <p>Monitor health and performance of your microservices</p>
          <Link to="/service-status" className="card-link">
            Check Status <i className="fas fa-arrow-right"></i>
          </Link>
        </div>

        <div className="dashboard-card">
          <Link to="/teams" className="card-icon-link">
            <div className="card-icon">
              <i className="fas fa-users"></i>
            </div>
          </Link>
          <h3>Teams</h3>
          <p>Manage your teams and collaborate with members</p>
          <Link to="/teams" className="card-link">
            Manage Teams <i className="fas fa-arrow-right"></i>
          </Link>
        </div>

        <div className="dashboard-card">
          <Link to="/organizations" className="card-icon-link">
            <div className="card-icon">
              <i className="fas fa-building"></i>
            </div>
          </Link>
          <h3>Organizations</h3>
          <p>Manage your organizations and their teams</p>
          <Link to="/organizations" className="card-link">
            Manage Organizations <i className="fas fa-arrow-right"></i>
          </Link>
        </div>

        <div className="dashboard-card disabled">
          <Link to="/alerts" className="card-icon-link">
            <div className="card-icon">
              <i className="fas fa-bell"></i>
            </div>
          </Link>
          <h3>Alerts</h3>
          <p>View and manage system alerts and notifications</p>
          <span className="coming-soon-badge">Coming Soon</span>
          <Link to="/alerts" className="card-link disabled" >
            View Alerts <i className="fas fa-arrow-right"></i>
          </Link>
        </div>

        <div className="dashboard-card disabled">
          <Link to="/settings" className="card-icon-link">
            <div className="card-icon">
              <i className="fas fa-cog"></i>
            </div>
          </Link>
          <h3>Settings</h3>
          <p>Configure dashboard and monitoring preferences</p>
          <span className="coming-soon-badge">Coming Soon</span>
          <button className="card-link" disabled>
            Open Settings <i className="fas fa-arrow-right"></i>
          </button>
        </div>
      </div>
    </div>
  );
}

export default Home;