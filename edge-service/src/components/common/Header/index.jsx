import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import './styles.css';

function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const isActive = (path) => {
    return location.pathname === path ? 'active' : '';
  };

  const handleNavClick = (path, e) => {
    if (e) e.preventDefault();
    console.log('Navigation clicked:', path);
    console.log('Current location:', window.location.pathname);
    console.log('User state:', user);
    console.log('Navigate function:', typeof navigate);
    navigate(path);
    console.log('Navigation attempted');
  };

  return (
    <nav className="navbar navbar-expand-lg navbar-dark">
      <div className="container-fluid">
        <a className="navbar-brand" href="#" onClick={(e) => handleNavClick('/', e)}>
          LiteMesh
        </a>
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
                  <a 
                    className={`nav-link ${isActive('/')}`} 
                    href="#" 
                    onClick={(e) => handleNavClick('/', e)}
                  >
                    Home
                  </a>
                </li>

                <li className="nav-item">
                  <a 
                    className={`nav-link ${isActive('/dashboard')}`}
                    href="#" 
                    onClick={(e) => handleNavClick('/dashboard', e)}
                  >
                    Dashboard
                  </a>
                </li>
                <li className="nav-item">
                  <a 
                    className={`nav-link ${isActive('/api-routes')}`}
                    href="#" 
                    onClick={(e) => handleNavClick('/api-routes', e)}
                  >
                    API Routes
                  </a>
                </li>
                <li className="nav-item">
                  <a 
                    className={`nav-link ${isActive('/metrics')}`}
                    href="#" 
                    onClick={(e) => handleNavClick('/metrics', e)}
                  >
                    Metrics
                  </a>
                </li>
                <li className="nav-item">
                  <a 
                    className={`nav-link ${isActive('/service-status')}`}
                    href="#" 
                    onClick={(e) => handleNavClick('/service-status', e)}
                  >
                    Status
                  </a>
                </li>
                <li className="nav-item">
                  <a 
                    className={`nav-link ${isActive('/teams')}`}
                    href="#" 
                    onClick={(e) => handleNavClick('/teams', e)}
                  >
                    Teams
                  </a>
                </li>
                <li className="nav-item">
                  <a 
                    className={`nav-link ${isActive('/organizations')}`}
                    href="#" 
                    onClick={(e) => handleNavClick('/organizations', e)}
                  >
                    Orgs
                  </a>
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
              <a href="#" className="btn btn-outline-light me-2" onClick={(e) => handleNavClick('/login', e)}>Login</a>
              <a href="#" className="btn btn-light" onClick={(e) => handleNavClick('/register', e)}>Register</a>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}

export default Header;