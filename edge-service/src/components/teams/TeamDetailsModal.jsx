import React from 'react';
import { Modal, Badge, Card, Row, Col } from 'react-bootstrap';
import { HiUserGroup, HiTemplate, HiClock, HiUser, HiUsers, HiLockClosed, HiOfficeBuilding, HiDocumentText } from 'react-icons/hi';
import { formatDateTime } from '../../utils/dateUtils';

function TeamDetailsModal({ show, onHide, team }) {
  if (!team) return null;

  const formatDate = (timestamp) => {
    return new Date(timestamp).toLocaleString();
  };

  return (
    <Modal show={show} onHide={onHide} size="lg">
      <Modal.Header closeButton>
        <Modal.Title>
          {team.name}
          <Badge 
            bg={team.status === 'ACTIVE' ? 'success' : 'secondary'} 
            className="ms-2"
          >
            {team.status}
          </Badge>
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Row className="g-4">
          <Col md={12}>
            <Card className="border-0 bg-light p-2">
              <Card.Body>
                <div className="d-flex align-items-center mb-3">
                  <HiOfficeBuilding className="text-primary me-2" size={24} />
                  <h5 className="mb-0">Organization</h5>
                </div>
                <p className="mb-0">{team.organization?.name || 'No organization assigned'}</p>
              </Card.Body>
            </Card>
          </Col>
          
          <Col md={12}>
            <Card className="border-0 bg-light p-2">
              <Card.Body>
                <div className="d-flex align-items-center mb-3">
                  <HiDocumentText className="text-primary me-2" size={24} />
                  <h5 className="mb-0">Description</h5>
                </div>
                <p>{team.description || 'No description provided.'}</p>
              </Card.Body>
            </Card>
          </Col>
          
          <Col md={6}>
            <Card className="border-0 bg-light h-100 p-2">
              <Card.Body>
                <div className="d-flex align-items-center mb-3">
                  <HiUserGroup className="text-primary me-2" size={24} />
                  <h5 className="mb-0">Team Statistics</h5>
                </div>
                <div className="d-flex justify-content-between align-items-center mb-3">
                  <span className="text-muted h6">Members</span>
                  <Badge bg="info" pill className="px-2">
                    {team.members?.length || 0}
                  </Badge>
                </div>
                <div className="d-flex justify-content-between align-items-center">
                  <span className="text-muted h6">Routes</span>
                  <Badge bg="info" pill className="px-2">
                    {team.routes?.length || 0}
                  </Badge>
                </div>
              </Card.Body>
            </Card>
          </Col>
          
          <Col md={6}>
            <Card className="border-0 bg-light p-2">
              <Card.Body>
                <div className="d-flex align-items-center mb-3">
                  <HiClock className="text-primary me-2" size={24} />
                  <h5 className="mb-0">Timestamps</h5>
                </div>
                <div className="d-flex justify-content-between align-items-center mb-3">
                  <span className="text-muted h6 mb-0">Created</span>
                  <span className="text-secondary">
                    {formatDate(team.createdAt)}
                  </span>
                </div>
                {team.updatedAt && (
                  <div className="d-flex justify-content-between align-items-center">
                    <span className="text-muted h6 mb-0">Last Updated</span>
                    <span className="text-secondary">
                      {formatDate(team.updatedAt)}
                    </span>
                  </div>
                )}
              </Card.Body>
            </Card>
          </Col>
          
          {team.members && team.members.length > 0 && (
            <Col md={12}>
              <Card className="border-0 bg-light p-2">
                <Card.Body>
                  <div className="d-flex align-items-center mb-3">
                    <HiUsers className="text-primary me-2" size={24} />
                    <h5 className="mb-0">Team Members</h5>
                  </div>
                  <div className="table-responsive mb-n3">
                    <table className="table table-sm mb-0">
                      <thead>
                        <tr>
                          <th>Username</th>
                          <th>Role</th>
                          <th>Status</th>
                          <th>Joined At</th>
                        </tr>
                      </thead>
                      <tbody>
                        {team.members.map(member => (
                          <tr key={member.id}>
                            <td>{member.username}</td>
                            <td>
                              <Badge bg="primary">{member.role}</Badge>
                            </td>
                            <td>
                              <Badge bg={member.status === 'ACTIVE' ? 'success' : 'secondary'}>
                                {member.status}
                              </Badge>
                            </td>
                            <td>{formatDate(member.joinedAt)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </Card.Body>
              </Card>
            </Col>
          )}
          
          {team.routes && team.routes.length > 0 && (
            <Col md={12}>
              <Card className="border-0 bg-light p-2">
                <Card.Body>
                  <div className="d-flex align-items-center mb-3">
                    <HiTemplate className="text-primary me-2" size={24} />
                    <h5 className="mb-0">Team Routes</h5>
                  </div>
                  <div className="table-responsive mb-n3">
                    <table className="table table-sm mb-0">
                      <thead>
                        <tr>
                          <th>Route ID</th>
                          <th>Path</th>
                          <th>Version</th>
                          <th>Permissions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {team.routes.map(route => (
                          <tr key={route.id}>
                            <td>{route.routeIdentifier}</td>
                            <td>{route.path}</td>
                            <td>v{route.version}</td>
                            <td>
                              {route.permissions?.map(permission => (
                                <Badge 
                                  key={permission} 
                                  bg="info" 
                                  className="me-1"
                                >
                                  <HiLockClosed className="me-1" size={12} />
                                  {permission}
                                </Badge>
                              ))}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </Card.Body>
              </Card>
            </Col>
          )}
          
          {team.createdBy && (
            <Col md={12}>
              <Card className="border-0 bg-light p-2">
                <Card.Body>
                  <div className="d-flex align-items-center">
                    <HiUser className="text-primary me-2" size={24} />
                    <div>
                      <p className="text-muted mb-1">Created By</p>
                      <p className="mb-0">{team.createdBy}</p>
                    </div>
                  </div>
                </Card.Body>
              </Card>
            </Col>
          )}
        </Row>
      </Modal.Body>
    </Modal>
  );
}

export default TeamDetailsModal; 