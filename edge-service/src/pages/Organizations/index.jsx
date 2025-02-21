import React, { useState, useEffect } from 'react';
import { Table, Spinner, OverlayTrigger, Tooltip } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { HiUserGroup, HiPlus, HiOfficeBuilding, HiPencil, HiTrash } from 'react-icons/hi';
import Button from '../../components/common/Button';
import CreateOrganizationModal from '../../components/organizations/CreateOrganizationModal';
import EditOrganizationModal from '../../components/organizations/EditOrganizationModal';
import ConfirmationModal from '../../components/common/ConfirmationModal';
import organizationService from '../../services/organizationService';
import './styles.css';

function Organizations() {
  const [organizations, setOrganizations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [operationLoading, setOperationLoading] = useState(false);
  const [selectedOrganization, setSelectedOrganization] = useState(null);
  const [confirmModal, setConfirmModal] = useState({
    show: false,
    title: '',
    message: '',
    onConfirm: () => {},
    variant: 'danger'
  });

  useEffect(() => {
    fetchOrganizations();
  }, []);

  const fetchOrganizations = async () => {
    try {
      const { data, error } = await organizationService.getAllOrganizations();
      if (error) throw new Error(error);
      setOrganizations(data);
    } catch (err) {
      toast.error('Failed to fetch organizations');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateOrganization = async (organizationData) => {
    try {
      setOperationLoading(true);
      const { data, error } = await organizationService.createOrganization(organizationData);
      if (error) throw new Error(error);
      
      setOrganizations(prev => [...prev, data]);
      toast.success('Organization created successfully');
    } catch (err) {
      toast.error(err.message || 'Failed to create organization');
      throw err;
    } finally {
      setOperationLoading(false);
    }
  };

  const handleEditOrganization = async (organizationData) => {
    try {
      setOperationLoading(true);
      const { data, error } = await organizationService.updateOrganization(
        selectedOrganization.id,
        organizationData
      );
      if (error) throw new Error(error);
      
      setOrganizations(prev => prev.map(org => 
        org.id === selectedOrganization.id ? data : org
      ));
      setShowEditModal(false);
      toast.success(`Organization "${data.name}" updated successfully`);
    } catch (err) {
      toast.error(err.message || 'Failed to update organization');
    } finally {
      setOperationLoading(false);
    }
  };

  const handleDeleteOrganization = async (organizationId) => {
    try {
      setOperationLoading(true);
      const { error } = await organizationService.deleteOrganization(organizationId);
      if (error) throw new Error(error);
      
      await fetchOrganizations();
      setConfirmModal(prev => ({ ...prev, show: false }));
      toast.success('Organization deleted successfully');
    } catch (err) {
      toast.error(err.message || 'Failed to delete organization');
    } finally {
      setOperationLoading(false);
    }
  };

  const confirmDelete = (org) => {
    setConfirmModal({
      show: true,
      title: 'Confirm Delete',
      message: `Are you sure you want to delete "${org.name}"? This action cannot be undone.`,
      onConfirm: () => handleDeleteOrganization(org.id),
      variant: 'danger'
    });
  };

  const canDeleteOrganization = (org) => {
    return org.teamCount === 0;
  };

  const getDeleteButtonTooltip = (org) => {
    if (!canDeleteOrganization(org)) {
      const teamCount = org.teamCount;
      return `Cannot delete organization with ${teamCount} assigned team${teamCount === 1 ? '' : 's'}`;
    }
    return '';
  };

  if (loading) {
    return (
      <div className="text-center mt-5">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </div>
    );
  }

  return (
    <div className="organizations-container">
      <div className="card">
        <div className="card-header">
          <div className="d-flex align-items-center">
            <div className="d-flex align-items-center gap-2">
              <HiOfficeBuilding className="organizations-icon" />
              <h4 className="mb-0">Organizations</h4>
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
                    <HiPlus /> Create Organization
                  </>
                )}
              </Button>
            </div>
          </div>
        </div>

        <div className="card-body">
          {organizations.length === 0 ? (
            <div className="text-center mt-4">
              <p>No organizations found. Create your first organization to get started.</p>
            </div>
          ) : (
            <Table hover responsive>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Description</th>
                  <th>Edit</th>
                  <th>Delete</th>
                </tr>
              </thead>
              <tbody>
                {organizations.map(org => (
                  <tr key={org.id}>
                    <td>{org.name}</td>
                    <td>{org.description}</td>
                    <td>
                      <Button
                        variant="outline-primary"
                        size="sm"
                        onClick={() => {
                          setSelectedOrganization(org);
                          setShowEditModal(true);
                        }}
                      >
                        <HiPencil /> Edit
                      </Button>
                    </td>
                    <td>
                      {!canDeleteOrganization(org) ? (
                        <OverlayTrigger
                          overlay={
                            <Tooltip id={`delete-tooltip-${org.id}`}>
                              {getDeleteButtonTooltip(org)}
                            </Tooltip>
                          }
                          placement="top"
                        >
                          <span>
                            <Button
                              variant="outline-danger"
                              size="sm"
                              onClick={() => confirmDelete(org)}
                              disabled={!canDeleteOrganization(org)}
                            >
                              <HiTrash /> Delete
                            </Button>
                          </span>
                        </OverlayTrigger>
                      ) : (
                        <Button
                          variant="outline-danger"
                          size="sm"
                          onClick={() => confirmDelete(org)}
                        >
                          <HiTrash /> Delete
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </div>
      </div>

      <CreateOrganizationModal
        show={showCreateModal}
        onHide={() => setShowCreateModal(false)}
        onSubmit={handleCreateOrganization}
        loading={operationLoading}
      />

      <EditOrganizationModal
        show={showEditModal}
        onHide={() => setShowEditModal(false)}
        onSubmit={handleEditOrganization}
        organization={selectedOrganization}
        loading={operationLoading}
      />

      <ConfirmationModal
        show={confirmModal.show}
        title={confirmModal.title}
        message={confirmModal.message}
        onConfirm={confirmModal.onConfirm}
        onCancel={() => setConfirmModal(prev => ({ ...prev, show: false }))}
        onHide={() => setConfirmModal(prev => ({ ...prev, show: false }))}
        variant={confirmModal.variant}
        loading={operationLoading}
      />
    </div>
  );
}

export default Organizations; 