import React from 'react';
import { Modal } from 'react-bootstrap';
import './styles.css';

const ImageModal = ({ show, onHide, imageSrc }) => {
  return (
    <Modal show={show} onHide={onHide} size="xl" centered className="image-modal">
      <Modal.Header closeButton />
      <Modal.Body>
        <img src={imageSrc} alt="Full size diagram" className="modal-image" />
      </Modal.Body>
    </Modal>
  );
};

export default ImageModal; 