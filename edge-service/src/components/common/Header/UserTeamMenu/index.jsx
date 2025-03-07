import React, { useState } from 'react';
import { Dropdown, Badge } from 'react-bootstrap';
import { useAuth } from '../../../../contexts/AuthContext';
import { useTeam } from '../../../../contexts/TeamContext';
import { HiOutlineTemplate } from 'react-icons/hi';
import Tippy from '@tippyjs/react';
import 'tippy.js/dist/tippy.css';
import 'tippy.js/themes/light.css';
import './styles.css';
import ConfirmationModal from '../../ConfirmationModal';
import { RoleBadge, AuthBadge } from '../../../../utils/roleUtils';

const UserTeamMenu = () => {
  const { user, logout } = useAuth();
  const { currentTeam, userTeams, switchTeam } = useTeam();
  const [isOpen, setIsOpen] = useState(false);
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);

  const handleTeamSwitch = async (teamId) => {
    await switchTeam(teamId);
    setIsOpen(false);
  };

  const handleLogoutClick = () => {
    setShowLogoutConfirm(true);
  };

  const handleLogoutConfirm = () => {
    setShowLogoutConfirm(false);
    logout();
  };

  const renderRoutesList = (routes) => {
    if (!routes || routes.length === 0) return null;
    
    return (
      <div className="routes-list">
        {routes.map((route, index) => (
          <div key={index} className="route-item">
            <span className="route-name">{route.name}</span>
            <div className="route-details">
              <span className="route-path">{route.path}</span>
              <Badge bg="light" text="dark" className="route-method">
                {route.method}
              </Badge>
            </div>
          </div>
        ))}
      </div>
    );
  };

  return (
    <>
      <Dropdown show={isOpen} onToggle={(isOpen) => setIsOpen(isOpen)} align="end">
        <Dropdown.Toggle variant="link" id="user-team-dropdown">
          {currentTeam ? (
            <>
              <div className="current-team-info">
                <span className="team-name">{currentTeam.name}</span>
                <span className="current-org-name">{currentTeam.organization?.name}</span>
              </div>
              {currentTeam.roles?.map((role, index) => (
                <Badge key={index} bg="light" text="dark" className="role-badge">
                  {role}
                </Badge>
              ))}
              <Tippy
                content={renderRoutesList(currentTeam.routes)}
                interactive={true}
                arrow={true}
                duration={200}
                placement="bottom"
              >
                <span className="routes-wrapper">
                  <Badge bg="info" className="routes-badge">
                    <HiOutlineTemplate size={14} /> {currentTeam.routes?.length || 0}
                  </Badge>
                </span>
              </Tippy>
              <span className="separator">|</span>
              <div className="user-info">
                <span className="user-name">{user?.username}</span>
                <span className="user-email">{user?.email}</span>
              </div>
            </>
          ) : (
            <span className="user-name">{user?.name || user?.email}</span>
          )}
        </Dropdown.Toggle>

        <Dropdown.Menu>
          {userTeams.length > 0 && (
            <>
              <Dropdown.Header>Your Teams</Dropdown.Header>
              {userTeams.map((team) => (
                <Dropdown.Item
                  key={team.id}
                  onClick={() => handleTeamSwitch(team.id)}
                  active={currentTeam?.id === team.id}
                >
                  <div className="team-item">
                    <div className="team-info">
                      <div className="team-header">
                        <span className="team-item-name">{team.name}</span>
                        <span className="team-item-org">{team.organization?.name || 'No Organization'}</span>
                      </div>
                      <div className="team-badges">
                        <Tippy
                          content={renderRoutesList(team.routes)}
                          interactive={true}
                          arrow={true}
                          duration={200}
                          placement="right"
                        >
                          <Badge bg="info" className="routes-badge">
                            <HiOutlineTemplate size={14} /> {team.routes?.length || 0}
                          </Badge>
                        </Tippy>
                        {team.roles?.map((role, index) => (
                          <Badge key={index} bg="secondary" className="role-badge">
                            {role}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  </div>
                </Dropdown.Item>
              ))}
              <Dropdown.Divider />
            </>
          )}

          {/* User Section */}
          <Dropdown.Header>User Information</Dropdown.Header>
          <div className="user-info-section">
            <div className="auth-type">
              <AuthBadge authType={user?.authType} />
            </div>
            <div className="user-name">
              {user?.username}
              <RoleBadge user={user} />
            </div>
            <div className="user-email">{user?.email}</div>
          </div>
          <Dropdown.Divider />
          <Dropdown.Item onClick={handleLogoutClick}>Sign Out</Dropdown.Item>
        </Dropdown.Menu>
      </Dropdown>

      <ConfirmationModal
        show={showLogoutConfirm}
        onHide={() => setShowLogoutConfirm(false)}
        onConfirm={handleLogoutConfirm}
        title="Sign Out Confirmation"
        message="Are you sure you want to sign out?"
      />
    </>
  );
};

export default UserTeamMenu; 