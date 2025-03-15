import React, { useState, useMemo } from 'react';
import { Modal } from 'react-bootstrap';
import JSONEditor from 'react-json-editor-ajrm';
import locale from 'react-json-editor-ajrm/locale/en';
import { showSuccessToast, showErrorToast } from '../../../utils/toastConfig';
import { apiRouteService } from '../../../services/apiRouteService';
import { useAuth } from '../../../contexts/AuthContext';
import { useTeam } from '../../../contexts/TeamContext';
import { isSuperAdmin } from '../../../utils/roleUtils';
import './styles.css';

const ViewJsonApiRouteModal = ({ show, onHide, route }) => {
  const { user } = useAuth();
  const { currentTeam } = useTeam();
  const [isEditing, setIsEditing] = useState(false);
  const [jsonValue, setJsonValue] = useState(route);
  const [originalJson] = useState(route);

  // Check if user can edit
  const canEdit = useMemo(() => {
    return isSuperAdmin(user) || 
           (currentTeam?.roles && currentTeam.roles.includes('ADMIN'));
  }, [user, currentTeam]);

  const handleSave = async () => {
    try {
      // Create a deep copy of the JSON to ensure all collections are mutable
      const routeToUpdate = JSON.parse(JSON.stringify(jsonValue));
      
      // Ensure filters and other collections are mutable arrays
      if (routeToUpdate.filters) {
        routeToUpdate.filters = [...routeToUpdate.filters];
      }
      if (routeToUpdate.healthCheck?.requiredMetrics) {
        routeToUpdate.healthCheck.requiredMetrics = [...routeToUpdate.healthCheck.requiredMetrics];
      }
      if (routeToUpdate.healthCheck?.alertRules) {
        routeToUpdate.healthCheck.alertRules = [...routeToUpdate.healthCheck.alertRules];
      }

      await apiRouteService.updateRoute(route.routeIdentifier, routeToUpdate);
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
      // Create a deep copy when setting the JSON value
      setJsonValue(JSON.parse(JSON.stringify(event.jsObject)));
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
          {canEdit && (
            <button
              className={`edit-toggle-btn ${isEditing ? 'active' : ''}`}
              onClick={() => setIsEditing(!isEditing)}
            >
              <i className={`fas fa-${isEditing ? 'eye' : 'edit'}`}></i>
              {isEditing ? ' Switch to View' : ' Switch to Edit'}
            </button>
          )}
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
          <div className="w-100 d-flex justify-content-between align-items-center">
            <div>
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setJsonValue(originalJson);
                  setIsEditing(false);
                }}
              >
                Cancel
              </button>
            </div>
            <div>
              <button
                className="btn btn-primary"
                onClick={handleSave}
                disabled={!isJsonValid()}
              >
                Save Changes
              </button>
            </div>
          </div>
        )}
      </Modal.Footer>
    </Modal>
  );
};

export default ViewJsonApiRouteModal; 