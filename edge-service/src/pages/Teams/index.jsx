import React, { useState, useEffect } from 'react';
import { 
  HiUserGroup, 
  HiPlus, 
  HiEye, 
  HiPencil, 
  HiUsers, 
  HiTemplate, 
  HiRefresh, 
  HiTrash, 
  HiKey 
} from 'react-icons/hi';
import { Spinner, OverlayTrigger, Tooltip, Table } from 'react-bootstrap';
import CreateTeamModal from '../../components/teams/CreateTeamModal';
import TeamDetailsModal from '../../components/teams/TeamDetailsModal';
import TeamMembersModal from '../../components/teams/TeamMembersModal';
import TeamRoutesModal from '../../components/teams/TeamRoutesModal';
import TeamEditModal from '../../components/teams/TeamEditModal';
import TeamApiKeysModal from '../../components/teams/TeamApiKeysModal';
import { teamService } from '../../services/teamService';
import './styles.css';
import { showSuccessToast, showErrorToast } from '../../utils/toastConfig';
import ConfirmationModal from '../../components/common/ConfirmationModal';
import Button from '../../components/common/Button';

function Teams() {
  const [teams, setTeams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [operationLoading, setOperationLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedTeam, setSelectedTeam] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [showMembersModal, setShowMembersModal] = useState(false);
  const [showRoutesModal, setShowRoutesModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showApiKeysModal, setShowApiKeysModal] = useState(false);
  const [confirmModal, setConfirmModal] = useState({
    show: false,
    title: '',
    message: '',
    onConfirm: () => {},
    variant: 'danger'
  });

  useEffect(() => {
    fetchTeams();
  }, []);

  const fetchTeams = async () => {
    try {
      setLoading(true);
      const { data, error } = await teamService.getAllTeams();
      if (error) throw new Error(error);
      // Sort teams by createdAt in ascending order to match backend
      const sortedTeams = [...data].sort((a, b) => 
        new Date(a.createdAt) - new Date(b.createdAt)
      );
      setTeams(sortedTeams);
      setLoading(false);
    } catch (err) {
      setError('Failed to load teams');
      setLoading(false);
    }
  };

  const handleCreateTeam = async (teamData) => {
    try {
      setOperationLoading(true);
      const { data, error } = await teamService.createTeam(teamData);
      if (error) throw new Error(error);
      
      await fetchTeams();
      setShowCreateModal(false);
      showSuccessToast(`Team "${data.name}" created successfully`);
    } catch (err) {
      showErrorToast(err.message || 'Failed to create team');
    } finally {
      setOperationLoading(false);
    }
  };

  const handleDeleteTeam = async (teamId) => {
    const team = teams.find(t => t.id === teamId);
    try {
      setOperationLoading(true);
      const { error } = await teamService.deleteTeam(teamId);
      if (error) throw new Error(error);
      setConfirmModal(prev => ({ ...prev, show: false }));
      await fetchTeams();
      showSuccessToast(`Team "${team.name}" deleted successfully`);
    } catch (err) {
      showErrorToast(err.message || 'Failed to delete team');
    } finally {
      setOperationLoading(false);
    }
  };

  const confirmDelete = (team) => {
    setConfirmModal({
      show: true,
      title: 'Delete Team',
      message: `Are you sure you want to delete team "${team.name}"? This action cannot be undone.`,
      onConfirm: () => handleDeleteTeam(team.id),
      variant: 'danger',
      confirmLabel: operationLoading ? 'Deleting...' : 'Delete',
      disabled: operationLoading
    });
  };

  const confirmToggleStatus = (team) => {
    const action = team.status === 'ACTIVE' ? 'deactivate' : 'activate';
    setConfirmModal({
      show: true,
      title: `${action.charAt(0).toUpperCase() + action.slice(1)} Team`,
      message: `Are you sure you want to ${action} team "${team.name}"?`,
      onConfirm: () => handleToggleTeamStatus(team),
      variant: 'warning',
      confirmLabel: operationLoading ? `${action.charAt(0).toUpperCase() + action.slice(1)}ing...` : action.charAt(0).toUpperCase() + action.slice(1),
      disabled: operationLoading
    });
  };

  const handleAddMember = async (memberData) => {
    try {
      setOperationLoading(true);
      const { data, error } = await teamService.addTeamMember(
        selectedTeam.id,
        memberData
      );
      
      if (error) {
        throw new Error(error);
      }
      
      await fetchTeams();
      if (selectedTeam) {
        const updatedTeam = data;
        setSelectedTeam(updatedTeam);
      }
      showSuccessToast(`Member added to team "${selectedTeam.name}" successfully`);
    } catch (err) {
      showErrorToast(err.message || 'Failed to add member to team');
    } finally {
      setOperationLoading(false);
    }
  };

  const handleRemoveMember = async (userId) => {
    try {
      setOperationLoading(true);
      const { data, error } = await teamService.removeTeamMember(selectedTeam.id, userId);
      if (error) {
        throw new Error(error);
      }
      
      // Update selectedTeam with the latest data
      setSelectedTeam(data);
      // Refresh the teams list
      await fetchTeams();
      
      showSuccessToast('Team member removed successfully');
    } catch (err) {
      showErrorToast(err.message || 'Failed to remove team member');
    } finally {
      setOperationLoading(false);
    }
  };

  const handleToggleTeamStatus = async (team) => {
    const action = team.status === 'ACTIVE' ? 'deactivate' : 'activate';
    
    try {
      setOperationLoading(true);
      const { data, error } = await teamService[`${action}Team`](team.id);
      if (error) throw new Error(error);
      
      setConfirmModal(prev => ({ ...prev, show: false }));
      setTeams(prev => prev.map(t => 
        t.id === team.id ? data : t
      ));
      showSuccessToast(`Team "${team.name}" ${action}d successfully`);
    } catch (err) {
      showErrorToast(err.message || `Failed to ${action} team`);
    } finally {
      setOperationLoading(false);
    }
  };

  const getDeleteButtonTooltip = (team) => {
    if (team.status === 'ACTIVE') {
      return 'Team must be deactivated before deletion';
    }
    if (team.routes?.length > 0) {
      return 'Team has assigned routes';
    }
    return '';
  };

  const canDeleteTeam = (team) => {
    return team.status === 'INACTIVE' && 
           (!team.routes || team.routes.length === 0);
  };

  const handleRemoveRoute = async (routeId) => {
    try {
      setOperationLoading(true);
      const { data, error } = await teamService.removeTeamRoute(selectedTeam.id, routeId);
      if (error) throw new Error(error);
      
      // Update selectedTeam with the latest data
      setSelectedTeam(data);
      // Refresh the teams list
      await fetchTeams();
      showSuccessToast('Route removed successfully');
    } catch (err) {
      showErrorToast(err.message || 'Failed to remove route');
    } finally {
      setOperationLoading(false);
    }
  };

  const handleAddRoute = async (routeData) => {
    try {
      setOperationLoading(true);
      const { data, error } = await teamService.addTeamRoute(
        selectedTeam.id,
        routeData.routeId,
        routeData.permissions
      );
      
      if (error) {
        throw new Error(error);
      }
      
      await fetchTeams();
      if (selectedTeam) {
        const updatedTeam = data;
        setSelectedTeam(updatedTeam);
      }
      showSuccessToast(`Route added to team "${selectedTeam.name}" successfully`);
    } catch (err) {
      showErrorToast(err.message || 'Failed to add route to team');
    } finally {
      setOperationLoading(false);
    }
  };

  const handleEditTeam = async (teamData) => {
    try {
      setOperationLoading(true);
      const { data, error } = await teamService.updateTeam(selectedTeam.id, {
        name: teamData.name,
        description: teamData.description,
        organizationId: teamData.organizationId
      });
      
      if (error) throw new Error(error);
      
      await fetchTeams();
      setShowEditModal(false);
      showSuccessToast(`Team "${data.name}" updated successfully`);
    } catch (err) {
      showErrorToast(err.message || 'Failed to update team');
    } finally {
      setOperationLoading(false);
    }
  };

  const handleCreateApiKey = async (apiKeyData) => {
    try {
      setOperationLoading(true);
      const { data, error } = await teamService.createApiKey(apiKeyData);
      if (error) throw new Error(error);
      
      // Show single toast with the key
      if (data && data.key) {
        showSuccessToast(
          <div>
            API key created successfully
            <br />
            <small className="text-monospace">Key: {data.key}</small>
          </div>,
          { autoClose: false }
        );
      }

      setShowApiKeysModal(false);
      
      // Refresh the API keys list in the modal
      if (selectedTeam) {
        const updatedTeam = { ...selectedTeam };
        setSelectedTeam(updatedTeam);
      }
    } catch (err) {
      showErrorToast(err.message || 'Failed to create API key');
    } finally {
      setOperationLoading(false);
    }
  };

  // First, add this helper function to group teams by organization
  const groupTeamsByOrganization = (teams) => {
    return teams.reduce((groups, team) => {
      const orgId = team.organization?.id || 'uncategorized';
      const orgName = team.organization?.name || 'Uncategorized';
      if (!groups[orgId]) {
        groups[orgId] = {
          name: orgName,
          teams: []
        };
      }
      groups[orgId].teams.push(team);
      return groups;
    }, {});
  };

  return (
    <div className="teams-container">
      <div className="card mb-4 border-0 mx-0">
        <div className="card-header">
          <div className="d-flex align-items-center">
            <div className="d-flex align-items-center gap-2">
              <HiUserGroup className="teams-icon" />
              <h4 className="mb-0">Teams</h4>
            </div>
            <div className="ms-auto">
              <Button 
                onClick={() => setShowCreateModal(true)}
                disabled={operationLoading}
                variant="primary"
              >
                {operationLoading ? (
                  <>
                    <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                    Creating...
                  </>
                ) : (
                  <>
                    <HiPlus /> Create Team
                  </>
                )}
              </Button>
            </div>
          </div>
        </div>
        <div className="card-body">
          {error && (
            <div className="alert alert-danger" role="alert">
              {error}
            </div>
          )}
          
          {loading ? (
            <div className="text-center">
              <Spinner animation="border" role="status">
                <span className="visually-hidden">Loading...</span>
              </Spinner>
            </div>
          ) : teams.length === 0 ? (
            <div className="no-teams-message text-center py-5">
              <h4>No Teams Found</h4>
              <p className="text-muted">
                Create your first team to start managing members and API routes.
              </p>
              <Button 
                variant="primary" 
                onClick={() => setShowCreateModal(true)}
                className="mt-3"
                disabled={operationLoading}
              >
                {operationLoading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                    Creating...
                  </>
                ) : (
                  <>
                    <HiPlus /> Create Your First Team
                  </>
                )}
              </Button>
            </div>
          ) : (
            <div className="table-responsive">
              <Table hover striped responsive>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Organization</th>
                    <th>Status</th>
                    <th>View</th>
                    <th>Edit</th>
                    <th>Members</th>
                    <th>Routes</th>
                    <th>API Keys</th>
                    <th>Status Action</th>
                    <th>Delete</th>
                  </tr>
                </thead>
                <tbody>
                  {Object.entries(groupTeamsByOrganization(teams)).map(([orgId, org]) => (
                    <React.Fragment key={orgId}>
                      <tr className="table-group-header">
                        <td colSpan="10" className="bg-light">
                          <strong>{org.name}</strong>
                        </td>
                      </tr>
                      {org.teams.map(team => (
                        <tr 
                          key={team.id} 
                          className={team.status === 'INACTIVE' ? 'table-secondary' : ''}
                        >
                          <td>{team.name}</td>
                          <td>{team.organization?.name || '-'}</td>
                          <td>
                            <span className={`badge ${team.status === 'ACTIVE' ? 'bg-success' : 'bg-secondary'}`}>
                              {team.status}
                            </span>
                          </td>
                          <td>
                            <button 
                              className="btn btn-sm btn-outline-primary action-button"
                              onClick={() => {
                                setSelectedTeam(team);
                                setShowDetailsModal(true);
                              }}
                            >
                              <HiEye className="me-1" /> View
                            </button>
                          </td>
                          <td>
                            <button 
                              className="btn btn-sm btn-outline-secondary action-button"
                              onClick={() => {
                                setSelectedTeam(team);
                                setShowEditModal(true);
                              }}
                              disabled={team.status === 'INACTIVE'}
                            >
                              <HiPencil className="me-1" /> Edit
                            </button>
                          </td>
                          <td>
                            <button 
                              className="btn btn-sm btn-outline-secondary action-button"
                              onClick={() => {
                                setSelectedTeam(team);
                                setShowMembersModal(true);
                              }}
                            >
                              <HiUsers className="me-1" /> Members ({team.members?.length || 0})
                            </button>
                          </td>
                          <td>
                            <button 
                              className="btn btn-sm btn-outline-info action-button"
                              onClick={() => {
                                setSelectedTeam(team);
                                setShowRoutesModal(true);
                              }}
                            >
                              <HiTemplate className="me-1" /> Routes ({team.routes?.length || 0})
                            </button>
                          </td>
                          <td>
                            <button 
                              className="btn btn-sm btn-outline-purple action-button"
                              onClick={() => {
                                setSelectedTeam(team);
                                setShowApiKeysModal(true);
                              }}
                              disabled={team.status === 'INACTIVE'}
                            >
                              <HiKey className="me-1" /> 
                              {team.apiKey ? 'View API Key' : 'Generate API Key'}
                            </button>
                          </td>
                          <td>
                            <button 
                              className="btn btn-sm btn-outline-warning action-button status-action-button"
                              onClick={() => confirmToggleStatus(team)}
                              disabled={operationLoading}
                            >
                              <HiRefresh className="me-1" />
                              {team.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                            </button>
                          </td>
                          <td>
                            <OverlayTrigger
                              placement="top"
                              overlay={
                                <Tooltip id={`delete-tooltip-${team.id}`}>
                                  {!canDeleteTeam(team) ? getDeleteButtonTooltip(team) : 'Delete this team'}
                                </Tooltip>
                              }
                            >
                              <span className="d-inline-block">
                                <button 
                                  className="btn btn-sm btn-outline-danger action-button"
                                  onClick={() => confirmDelete(team)}
                                  disabled={operationLoading || !canDeleteTeam(team)}
                                >
                                  <HiTrash className="me-1" /> Delete
                                </button>
                              </span>
                            </OverlayTrigger>
                          </td>
                        </tr>
                      ))}
                    </React.Fragment>
                  ))}
                </tbody>
              </Table>
            </div>
          )}
        </div>
      </div>

      <CreateTeamModal
        show={showCreateModal}
        onHide={() => setShowCreateModal(false)}
        onSubmit={handleCreateTeam}
        loading={loading}
      />

      <TeamDetailsModal
        show={showDetailsModal}
        onHide={() => setShowDetailsModal(false)}
        team={selectedTeam}
      />

      {showMembersModal && (
        <TeamMembersModal
          show={true}
          onHide={() => setShowMembersModal(false)}
          team={selectedTeam}
          onAddMember={handleAddMember}
          onRemoveMember={handleRemoveMember}
          loading={operationLoading}
        />
      )}

      {showRoutesModal && (
        <TeamRoutesModal
          show={true}
          onHide={() => setShowRoutesModal(false)}
          team={selectedTeam}
          onAddRoute={handleAddRoute}
          onRemoveRoute={handleRemoveRoute}
          loading={operationLoading}
        />
      )}

      <TeamEditModal
        show={showEditModal}
        onHide={() => setShowEditModal(false)}
        onSubmit={handleEditTeam}
        loading={operationLoading}
        team={selectedTeam}
      />

      <TeamApiKeysModal
        show={showApiKeysModal}
        onHide={() => setShowApiKeysModal(false)}
        team={selectedTeam}
        onCreateApiKey={handleCreateApiKey}
        loading={operationLoading}
      />

      <ConfirmationModal
        show={confirmModal.show}
        onHide={() => setConfirmModal(prev => ({ ...prev, show: false }))}
        onConfirm={confirmModal.onConfirm}
        title={confirmModal.title}
        message={confirmModal.message}
        variant={confirmModal.variant}
      />
    </div>
  );
}

export default Teams; 
