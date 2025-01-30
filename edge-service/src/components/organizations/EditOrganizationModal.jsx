import React, { useState, useEffect } from 'react';
import { Modal, Button, Form, Spinner } from 'react-bootstrap';

function EditOrganizationModal({ show, onHide, onSubmit, loading, organization }) {
  const [formData, setFormData] = useState({
    name: '',
    description: ''
  });
  const [error, setError] = useState('');
  const [validated, setValidated] = useState(false);

  useEffect(() => {
    if (show && organization) {
      setFormData({
        name: organization.name,
        description: organization.description || ''
      });
      setError('');
      setValidated(false);
    }
  }, [show, organization]);

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

    if (!formData.name.trim()) {
      setError('Organization name is required');
      return;
    }

    try {
      await onSubmit(formData);
      onHide();
    } catch (err) {
      setError(err.message || 'Failed to update organization');
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered animation={true}>
      <Modal.Header closeButton>
        <Modal.Title>Edit Organization</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {error && (
          <div className="alert alert-danger fade show">
            {error}
          </div>
        )}
        <Form noValidate validated={validated} onSubmit={handleSubmit}>
          <Form.Group className="mb-3">
            <Form.Label>Organization Name</Form.Label>
            <Form.Control
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              placeholder="Enter organization name"
              required
              minLength={3}
              maxLength={50}
            />
            <Form.Control.Feedback type="invalid">
              Please enter an organization name (3-50 characters)
            </Form.Control.Feedback>
          </Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>Description</Form.Label>
            <Form.Control
              as="textarea"
              name="description"
              value={formData.description}
              onChange={handleChange}
              placeholder="Enter organization description"
              rows={3}
              maxLength={300}
            />
            <Form.Text className="text-muted d-block mt-1 small">
              {300 - (formData.description?.length || 0)} characters remaining
            </Form.Text>
          </Form.Group>
          <div className="d-flex justify-content-end gap-2">
            <Button variant="secondary" onClick={onHide}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={loading}>
              {loading ? (
                <><Spinner size="sm" animation="border" /> Updating...</>
              ) : (
                'Update'
              )}
            </Button>
          </div>
        </Form>
      </Modal.Body>
    </Modal>
  );
}

export default EditOrganizationModal; 