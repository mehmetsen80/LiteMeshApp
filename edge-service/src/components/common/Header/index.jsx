import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import './styles.css';
import { Navbar, Container, Nav } from 'react-bootstrap';
import UserTeamMenu from './UserTeamMenu';
import { useTeam } from '../../../contexts/TeamContext';
import TokenExpiryDisplay from './TokenExpiryDisplay';
import { NavLink } from 'react-router-dom';

const Header = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { currentTeam } = useTeam();

  const handleNavClick = (path, e) => {
    if (e) e.preventDefault();
    console.log('Navigation clicked:', path);
    console.log('Current location:', window.location.pathname);
    console.log('User state:', user);
    console.log('Navigate function:', typeof navigate);
    navigate(path);
    console.log('Navigation attempted');
  };

  // Check if we're on the ViewToken page
  const isViewTokenPage = location.pathname === '/view-token';

  return (
    <Navbar expand="lg" className="app-header">
      <Container fluid>
        <Navbar.Brand as={Link} to="/">Linqra</Navbar.Brand>
        <Navbar.Toggle aria-controls="basic-navbar-nav" />
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="me-auto">
            <Nav.Link as={NavLink} to="/">Home</Nav.Link>
            <span className="nav-separator"> | </span>
            <Nav.Link as={NavLink} to="/dashboard">Dashboard</Nav.Link>
            <span className="nav-separator"> | </span>
            <Nav.Link as={NavLink} to="/api-routes">API Routes</Nav.Link>
            <span className="nav-separator"> | </span>
            <Nav.Link as={NavLink} to="/metrics">API Metrics</Nav.Link>
            <span className="nav-separator"> | </span>
            <Nav.Link as={NavLink} to="/service-status">Service Status</Nav.Link>
          </Nav>
          {user ? (
            <div className="d-flex align-items-center gap-3">
              {isViewTokenPage && <TokenExpiryDisplay />}
              <UserTeamMenu />
            </div>
          ) : (
            <div className="d-flex">
              <Nav.Link href="#" onClick={(e) => handleNavClick('/login', e)}>Login</Nav.Link>
              <Nav.Link href="#" onClick={(e) => handleNavClick('/register', e)}>Register</Nav.Link>
            </div>
          )}
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
};

export default Header;