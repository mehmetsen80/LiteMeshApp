import React from 'react';
import { Modal, Button } from 'react-bootstrap';
import { HiExclamationCircle } from 'react-icons/hi';
import './ConfirmationModal.css';

const ConfirmationModal = ({ 
  show, 
  onHide, 
  onConfirm, 
  title, 
  message, 
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  variant = 'danger',
  disabled = false
}) => {
  return (
    <Modal
      show={show}
      onHide={onHide}
      centered
      animation={true}
      className="confirmation-modal"
    >
      <Modal.Header closeButton className="border-0 pb-0">
        <Modal.Title className="d-flex align-items-center gap-2">
          <HiExclamationCircle className={`text-${variant}`} size={24} />
          {title}
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {message}
      </Modal.Body>
      <Modal.Footer className="border-0">
        <Button 
          variant="light" 
          onClick={onHide}
          className="px-4"
          disabled={disabled}
        >
          {cancelLabel}
        </Button>
        <Button 
          variant={variant} 
          onClick={onConfirm}
          className="px-4"
          disabled={disabled}
        >
          {confirmLabel}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default ConfirmationModal; 