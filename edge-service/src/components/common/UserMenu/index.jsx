import React from 'react';
import { Dropdown } from 'react-bootstrap';
import { useAuth } from '../../../contexts/AuthContext';
import { jwtDecode } from "jwt-decode";
import './styles.css';

const UserMenu = () => {
  const { user, logout } = useAuth();
  const token = localStorage.getItem('accessToken');
  const decodedToken = token ? jwtDecode(token) : null;
  const isSSO = decodedToken?.azp === 'lite-mesh-gateway-client';

  return (
    <Dropdown align="end">
      <Dropdown.Toggle variant="outline-light" id="user-dropdown">
        <i className="bi bi-person-circle me-2"></i>
        {user?.username || user?.user?.username}
        {isSSO && <span className="ms-2 badge bg-info">SSO</span>}
      </Dropdown.Toggle>

      <Dropdown.Menu>
        <Dropdown.Header>User Information</Dropdown.Header>
        <Dropdown.Item disabled>
          <small>
            <i className="bi bi-envelope me-2"></i>
            {decodedToken?.email || 'No email'}
          </small>
        </Dropdown.Item>
        <Dropdown.Item disabled>
          <small>
            <i className="bi bi-shield-lock me-2"></i>
            {isSSO ? 'SSO Authentication' : 'Local Authentication'}
          </small>
        </Dropdown.Item>
        <Dropdown.Divider />
        <Dropdown.Item onClick={logout}>
          <i className="bi bi-box-arrow-right me-2"></i>
          Sign Out
        </Dropdown.Item>
      </Dropdown.Menu>
    </Dropdown>
  );
};

export default UserMenu; 