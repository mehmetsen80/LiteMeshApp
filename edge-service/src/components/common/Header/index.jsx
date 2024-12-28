import { Link } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import './styles.css';

function Header() {
  const { user, logout } = useAuth();

  return (
    <nav className="navbar navbar-expand-lg navbar-dark">
      <div className="container-fluid">
        <Link className="navbar-brand" to="/">Admin Dashboard</Link>
        <button 
          className="navbar-toggler" 
          type="button" 
          data-bs-toggle="collapse" 
          data-bs-target="#navbarNav"
        >
          <span className="navbar-toggler-icon"></span>
        </button>
        <div className="collapse navbar-collapse" id="navbarNav">
          <ul className="navbar-nav me-auto">
            {user && (
              <>
                <li className="nav-item">
                  <Link className="nav-link" to="/">Home</Link>
                </li>
                <li className="nav-item">
                  <Link className="nav-link" to="/metrics">Metrics</Link>
                </li>
              </>
            )}
          </ul>
          {user ? (
            <div className="d-flex align-items-center">
              <span className="text-light me-3">Welcome, {user.username}</span>
              <button 
                className="btn btn-outline-light" 
                onClick={logout}
              >
                Sign Out
              </button>
            </div>
          ) : (
            <div className="d-flex">
              <Link to="/login" className="btn btn-outline-light me-2">Login</Link>
              <Link to="/register" className="btn btn-light">Register</Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}

export default Header;