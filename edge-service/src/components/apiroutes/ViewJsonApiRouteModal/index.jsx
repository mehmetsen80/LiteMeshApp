import React, { useState } from 'react';
import { Modal } from 'react-bootstrap';
import JSONEditor from 'react-json-editor-ajrm';
import locale from 'react-json-editor-ajrm/locale/en';
import { showSuccessToast, showErrorToast } from '../../../utils/toastConfig';
import { apiRouteService } from '../../../services/apiRouteService';
import './styles.css';

const ViewJsonApiRouteModal = ({ show, onHide, route }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [jsonValue, setJsonValue] = useState(route);
  const [originalJson] = useState(route);

  const handleSave = async () => {
    try {
      const parsedJson = jsonValue;
      await apiRouteService.updateRoute(route.routeIdentifier, parsedJson);
      showSuccessToast('Route configuration updated successfully');
      setTimeout(() => {
        onHide();
        window.location.reload();
      }, 1500);
    } catch (error) {
      showErrorToast(`Failed to update route: ${error.message}`);
    }
  };

  const handleEditorChange = (event) => {
    if (event.jsObject && !event.error) {
      setJsonValue(event.jsObject);
    }
  };

  const isJsonValid = () => {
    return jsonValue && Object.keys(jsonValue).length > 0;
  };

  return (
    <Modal
      show={show}
      onHide={onHide}
      size="lg"
      centered
      dialogClassName="json-editor-modal"
    >
      <Modal.Header closeButton>
        <Modal.Title>
          Route Configuration
          <span className="mode-indicator">
            {isEditing ? '(Edit Mode)' : '(View Mode)'}
          </span>
          <button
            className={`edit-toggle-btn ${isEditing ? 'active' : ''}`}
            onClick={() => setIsEditing(!isEditing)}
          >
            <i className={`fas fa-${isEditing ? 'eye' : 'edit'}`}></i>
            {isEditing ? ' Switch to View' : ' Switch to Edit'}
          </button>
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <JSONEditor
          placeholder={jsonValue}
          locale={locale}
          height="500px"
          width="100%"
          onBlur={handleEditorChange}
          viewOnly={!isEditing}
          theme={{
            background: '#f8f9fa',
            default: '#1e1e1e',
            string: '#ce9178',
            number: '#b5cea8',
            colon: '#49b4bb',
            keys: '#9cdcfe',
            keys_whiteSpace: '#af74a5',
            primitive: '#6b9955'
          }}
        />
      </Modal.Body>
      <Modal.Footer>
        {isEditing && (
          <>
            <button
              className="btn btn-secondary"
              onClick={() => {
                setJsonValue(originalJson);
                setIsEditing(false);
              }}
            >
              Cancel
            </button>
            <button
              className="btn btn-primary"
              onClick={handleSave}
              disabled={!isJsonValid()}
            >
              Save Changes
            </button>
          </>
        )}
      </Modal.Footer>
    </Modal>
  );
};

export default ViewJsonApiRouteModal; 