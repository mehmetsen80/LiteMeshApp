import React, { useState, useEffect } from 'react';
import { Modal, Form, Row, Col } from 'react-bootstrap';
import { HiClipboardCopy, HiTrash, HiBan } from 'react-icons/hi';
import Button from '../common/Button';
import ConfirmationModal from '../common/ConfirmationModal';
import { teamService } from '../../services/teamService';
import { showErrorToast, showSuccessToast } from '../../utils/toastConfig';
import './TeamApiKeysModal.css';

const EXPIRY_OPTIONS = [
  { value: null, label: 'Never expires' },
  { value: 30, label: '30 days' },
  { value: 90, label: '90 days' },
  { value: 180, label: '180 days' },
  { value: 365, label: '1 year' },
  { value: 730, label: '2 years' }
];

const TeamApiKeysModal = ({ show, onHide, team, onCreateApiKey, loading }) => {
  const [keyName, setKeyName] = useState('');
  const [expiryDays, setExpiryDays] = useState(null);
  const [apiKeys, setApiKeys] = useState([]);
  const [fetchingKeys, setFetchingKeys] = useState(false);
  const [confirmModal, setConfirmModal] = useState({
    show: false,
    title: '',
    message: '',
    onConfirm: () => {},
    variant: 'danger'
  });

  useEffect(() => {
    if (show && team) {
      fetchApiKeys();
    }
  }, [show, team]);

  const fetchApiKeys = async () => {
    try {
      setFetchingKeys(true);
      console.log('Fetching API keys for team:', team?.id);
      const { data, error } = await teamService.getTeamApiKeys(team.id);
      if (error) throw new Error(error);
      setApiKeys(data || []);
    } catch (err) {
      showErrorToast(err.message || 'Failed to fetch API keys');
    } finally {
      setFetchingKeys(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    await onCreateApiKey({
      name: keyName,
      teamId: team.id,
      expiresInDays: expiryDays
    });
    setKeyName(''); // Reset form
    fetchApiKeys(); // Refresh the list
  };

  const handleCopyToClipboard = async (key) => {
    try {
      await navigator.clipboard.writeText(key);
      showSuccessToast('API key copied to clipboard');
    } catch (err) {
      showErrorToast('Failed to copy API key');
    }
  };

  const handleRemoveKey = async (keyId, keyName) => {
    setConfirmModal({
      show: true,
      title: 'Remove API Key',
      message: `Are you sure you want to remove the API key "${keyName}"? This action cannot be undone.`,
      onConfirm: async () => {
        try {
          await teamService.removeApiKey(keyId);
          showSuccessToast('API key removed successfully');
          fetchApiKeys();
        } catch (err) {
          showErrorToast(err.message || 'Failed to remove API key');
        }
        setConfirmModal(prev => ({ ...prev, show: false }));
      },
      variant: 'danger',
      confirmLabel: 'Remove'
    });
  };

  const handleRevokeKey = async (keyId, keyName) => {
    setConfirmModal({
      show: true,
      title: 'Revoke API Key',
      message: `Are you sure you want to revoke the API key "${keyName}"? This will immediately invalidate the key.`,
      onConfirm: async () => {
        try {
          await teamService.revokeApiKey(keyId);
          showSuccessToast('API key revoked successfully');
          fetchApiKeys();
        } catch (err) {
          showErrorToast(err.message || 'Failed to revoke API key');
        }
        setConfirmModal(prev => ({ ...prev, show: false }));
      },
      variant: 'warning',
      confirmLabel: 'Revoke'
    });
  };

  return (
    <>
      <Modal show={show} onHide={onHide} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>API Keys - {team?.name}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form onSubmit={handleSubmit}>
            <Row className="align-items-end mb-4">
              <Col md={6}>
                <Form.Group>
                  <Form.Label>Key Name</Form.Label>
                  <Form.Control
                    type="text"
                    placeholder="Enter key name"
                    value={keyName}
                    onChange={(e) => setKeyName(e.target.value)}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group>
                  <Form.Label>Expires In</Form.Label>
                  <Form.Select
                    value={expiryDays}
                    onChange={(e) => setExpiryDays(e.target.value ? Number(e.target.value) : null)}
                  >
                    {EXPIRY_OPTIONS.map(option => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
            </Row>
          </Form>

          {fetchingKeys ? (
            <div className="text-center py-4">
              <div className="spinner-border" role="status">
                <span className="visually-hidden">Loading...</span>
              </div>
            </div>
          ) : apiKeys.length === 0 ? (
            <div className="text-center py-4">
              <p className="text-muted mb-0">No API keys found for this team.</p>
            </div>
          ) : (
            <div className="api-keys-container">
              {apiKeys.map(key => (
                <div key={key.id} className="api-key-block p-3 mb-3 border rounded">
                  <div className="d-flex justify-content-between align-items-start mb-2">
                    <h6 className="mb-0">{key.name}</h6>
                    <div className="d-flex gap-2">
                      <span className={`badge ${key.enabled ? 'bg-success' : 'bg-danger'}`}>
                        {key.enabled ? 'Active' : 'Revoked'}
                      </span>
                    </div>
                  </div>
                  
                  <div className="api-key-details">
                    <div className="key-field mb-2">
                      <div className="d-flex align-items-center bg-light border rounded">
                        <small className="text-muted ps-2 border-end py-2">API Key:</small>
                        <code className="flex-grow-1 p-2 m-0">
                          {key.key}
                        </code>
                        <button
                          type="button"
                          className="action-button btn btn-link p-2"
                          onClick={() => handleCopyToClipboard(key.key)}
                          title="Copy to clipboard"
                        >
                          <HiClipboardCopy size={20} />
                        </button>
                        {key.enabled && (
                          <button
                            type="button"
                            className="action-button btn btn-link p-2 text-warning"
                            onClick={() => handleRevokeKey(key.id, key.name)}
                            title="Revoke API key"
                          >
                            <HiBan size={20} />
                          </button>
                        )}
                        <button
                          type="button"
                          className="action-button btn btn-link p-2 text-danger"
                          onClick={() => handleRemoveKey(key.id, key.name)}
                          title="Remove API key"
                        >
                          <HiTrash size={20} />
                        </button>
                      </div>
                    </div>
                    
                    <Row className="mt-3">
                      <Col md={4}>
                        <small className="text-muted d-block">Created By</small>
                        <div>{key.createdBy}</div>
                      </Col>
                      <Col md={4}>
                        <small className="text-muted d-block">Created At</small>
                        <div>{new Date(key.createdAt).toLocaleString()}</div>
                      </Col>
                      <Col md={4}>
                        <small className="text-muted d-block">Expires At</small>
                        <div>{key.expiresAt ? new Date(key.expiresAt).toLocaleString() : 'Never'}</div>
                      </Col>
                    </Row>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Modal.Body>
        <Modal.Footer className="d-flex justify-content-between">
          <Button
            variant="secondary"
            onClick={onHide}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="primary"
            loading={loading}
            disabled={!keyName.trim() || loading}
            onClick={handleSubmit}
          >
            Generate Key
          </Button>
        </Modal.Footer>
      </Modal>

      <ConfirmationModal
        show={confirmModal.show}
        onHide={() => setConfirmModal(prev => ({ ...prev, show: false }))}
        onConfirm={confirmModal.onConfirm}
        title={confirmModal.title}
        message={confirmModal.message}
        variant={confirmModal.variant}
        confirmLabel={confirmModal.confirmLabel}
      />
    </>
  );
};

export default TeamApiKeysModal; 