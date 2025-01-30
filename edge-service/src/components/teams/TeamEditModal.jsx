import React, { useState, useEffect } from 'react';
import { Modal, Button, Form, Spinner } from 'react-bootstrap';
import organizationService from '../../services/organizationService';

function TeamEditModal({ show, onHide, onSubmit, loading, team }) {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    organizationId: ''
  });
  const [error, setError] = useState('');
  const [validated, setValidated] = useState(false);
  const [organizations, setOrganizations] = useState([]);
  const [loadingOrgs, setLoadingOrgs] = useState(false);

  useEffect(() => {
    if (show && team) {
      setFormData({
        name: team.name,
        description: team.description || '',
        organizationId: team.organization?.id || ''
      });
      setError('');
      setValidated(false);
      fetchOrganizations();
    }
  }, [show, team]);

  const fetchOrganizations = async () => {
    try {
      setLoadingOrgs(true);
      const { data, error } = await organizationService.getAllOrganizations();
      if (error) throw new Error(error);
      setOrganizations(data);
    } catch (err) {
      setError('Failed to fetch organizations');
      console.error('Error fetching organizations:', err);
    } finally {
      setLoadingOrgs(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setValidated(true);

    const form = e.currentTarget;
    if (!form.checkValidity()) {
      e.stopPropagation();
      return;
    }

    if (!formData.name.trim() || !formData.organizationId) {
      setError('Team name and organization are required');
      return;
    }

    try {
      const updateData = {
        name: formData.name.trim(),
        description: formData.description?.trim() || '',
        organizationId: formData.organizationId
      };
      await onSubmit(updateData);
      onHide();
    } catch (err) {
      setError(err.message || 'Failed to update team');
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered animation={true}>
      <Modal.Header closeButton>
        <Modal.Title>Edit Team</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {error && (
          <div className="alert alert-danger fade show">
            {error}
          </div>
        )}
        <Form noValidate validated={validated} onSubmit={handleSubmit}>
          <Form.Group className="mb-3">
            <Form.Label>Organization</Form.Label>
            <Form.Select
              name="organizationId"
              value={formData.organizationId}
              onChange={handleChange}
              required
              disabled={loadingOrgs}
            >
              <option value="">Select an organization</option>
              {organizations.map(org => (
                <option key={org.id} value={org.id}>
                  {org.name}
                </option>
              ))}
            </Form.Select>
            <Form.Control.Feedback type="invalid">
              Please select an organization
            </Form.Control.Feedback>
          </Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>Team Name</Form.Label>
            <Form.Control
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              placeholder="Enter team name"
              required
              minLength={3}
              maxLength={50}
            />
            <Form.Control.Feedback type="invalid">
              Please enter a team name (3-50 characters)
            </Form.Control.Feedback>
          </Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>Description</Form.Label>
            <Form.Control
              as="textarea"
              name="description"
              value={formData.description}
              onChange={handleChange}
              placeholder="Enter team description"
              rows={3}
              maxLength={300}
            />
            <Form.Text className="text-muted d-block mt-1 small">
              {300 - (formData.description?.length || 0)} characters remaining
            </Form.Text>
          </Form.Group>
        </Form>
      </Modal.Body>
      <Modal.Footer className="d-flex justify-content-between">
        <div>
          <Button 
            variant="secondary" 
            onClick={onHide}
            className="btn-cancel"
            disabled={loading}
          >
            Cancel
          </Button>
        </div>
        <div>
          <Button 
            variant="primary" 
            onClick={handleSubmit}
            className="btn-save"
            disabled={loading}
          >
            {loading ? (
              <>
                <Spinner
                  as="span"
                  animation="border"
                  size="sm"
                  role="status"
                  aria-hidden="true"
                  className="me-2"
                />
                Saving...
              </>
            ) : (
              'Save Changes'
            )}
          </Button>
        </div>
      </Modal.Footer>
    </Modal>
  );
}

export default TeamEditModal; 