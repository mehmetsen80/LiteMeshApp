import React, { useState, useEffect } from 'react';
import { Modal, Form, Spinner, Table } from 'react-bootstrap';
import Select from 'react-select';
import { teamService } from '../../services/teamService';
import ConfirmationModal from '../common/ConfirmationModal';
import Button from '../common/Button';

const TeamRoutesModal = ({ show, onHide, team, onAddRoute, onRemoveRoute, loading }) => {
    const [selectedRoute, setSelectedRoute] = useState(null);
    const [permissions, setPermissions] = useState([]);
    const [error, setError] = useState('');
    const [searching, setSearching] = useState(false);
    const [routes, setRoutes] = useState([]);
    const [availableRoutes, setAvailableRoutes] = useState([]);
    const [loadingRoutes, setLoadingRoutes] = useState(false);
    const [confirmModal, setConfirmModal] = useState({
        show: false,
        title: '',
        message: '',
        onConfirm: () => {},
        variant: 'danger'
    });

    const permissionOptions = [
        { value: 'VIEW', label: 'View' },
        { value: 'USE', label: 'Use' },
        { value: 'MANAGE', label: 'Manage' }
    ];

    useEffect(() => {
        if (team) {
            fetchRoutes();
            fetchAvailableRoutes();
        }
    }, [team]);

    const fetchRoutes = async () => {
        try {
            setLoadingRoutes(true);
            console.log('Fetching routes for team:', team.id);
            const { data, error } = await teamService.getTeamRoutes(team.id);
            if (error) throw new Error(error);
            console.log('Fetched team routes:', data);
            setRoutes(data);
        } catch (err) {
            console.error('Failed to fetch routes:', err);
        } finally {
            setLoadingRoutes(false);
        }
    };

    const fetchAvailableRoutes = async () => {
        try {
            const { data, error } = await teamService.getAllRoutes();
            if (error) throw new Error(error);
            setAvailableRoutes(data);
        } catch (err) {
            console.error('Failed to fetch available routes:', err);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSearching(true);

        try {
            // Check if route is already assigned
            const existingRoute = routes.find(r => 
                r.routeIdentifier === selectedRoute.value
            );
            
            if (existingRoute) {
                throw new Error(
                    `Route ${selectedRoute.value} (v${existingRoute.version}) is already assigned to this team`
                );
            }
            
            await onAddRoute({
                routeId: selectedRoute.value,
                permissions: permissions.map(p => p.value)
            });

            // Clear form on success
            setSelectedRoute(null);
            setPermissions([]);
            setError('');
            await fetchRoutes();
        } catch (err) {
            setError(err.message || err.error || 'Failed to add route');
        } finally {
            setSearching(false);
        }
    };

    const routeOptions = availableRoutes
        .filter(route => !routes.find(r => r.routeIdentifier === route.routeIdentifier))
        .map(route => ({
            value: route.routeIdentifier,
            label: `${route.path} (${route.routeIdentifier}) - v${route.version}`
        }));

    const confirmRemoveRoute = (route) => {
        setConfirmModal({
            show: true,
            title: 'Remove Route',
            message: `Are you sure you want to remove route "${route.path}" (${route.routeIdentifier}) from team "${team.name}"?`,
            onConfirm: async () => {
                try {
                    await onRemoveRoute(route.id);
                    await fetchRoutes(); // Refresh the routes list
                    setConfirmModal(prev => ({ ...prev, show: false }));
                } catch (err) {
                    setError(err.message || 'Failed to remove route');
                }
            },
            variant: 'warning'
        });
    };

    return (
        <>
            <Modal show={show} onHide={onHide} size="lg">
                <Modal.Header closeButton>
                    <Modal.Title>Team Routes - {team?.name}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form onSubmit={handleSubmit} className="mb-4">
                        <Form.Group className="mb-4">
                            <Form.Label>Select Route</Form.Label>
                            <Select
                                value={selectedRoute}
                                onChange={(option) => {
                                    setSelectedRoute(option);
                                    setError('');
                                }}
                                options={routeOptions}
                                className="form-select-lg"
                                placeholder="Select a route..."
                                isDisabled={loading || searching}
                                noOptionsMessage={() => "No routes available or all routes are already assigned"}
                            />
                            {error && <div className="text-danger mt-2">{error}</div>}
                        </Form.Group>

                        <Form.Group className="mb-4">
                            <Form.Label>Permissions</Form.Label>
                            <Select
                                isMulti
                                value={permissions}
                                onChange={setPermissions}
                                options={permissionOptions}
                                className="form-select-lg"
                                placeholder="Select permissions..."
                            />
                        </Form.Group>

                        <Button 
                            variant="primary"
                            type="submit"
                            disabled={loading || searching || !selectedRoute || permissions.length === 0}
                            loading={loading || searching}
                            fullWidth={true}
                        >
                            {searching ? 'Searching...' : 'Add Route'}
                        </Button>
                    </Form>

                    <hr />

                    <h6 className="mb-3">Current Routes</h6>
                    {loadingRoutes ? (
                        <div className="text-center py-4">
                            <Spinner animation="border" size="sm" /> Loading routes...
                        </div>
                    ) : routes.length === 0 ? (
                        <p className="text-muted">No routes assigned to this team yet.</p>
                    ) : (
                        <Table hover className="mt-3">
                            <thead>
                                <tr>
                                    <th>Route ID</th>
                                    <th>Path</th>
                                    <th>Version</th>
                                    <th>Permissions</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {routes.map(route => (
                                    <tr key={route.id}>
                                        <td>{route.routeIdentifier}</td>
                                        <td>{route.path}</td>
                                        <td>v{route.version}</td>
                                        <td>
                                            {route.permissions?.map(permission => (
                                                <span 
                                                    key={permission} 
                                                    className="badge bg-info me-1"
                                                >
                                                    {permission}
                                                </span>
                                            ))}
                                        </td>
                                        <td>
                                            <Button
                                                variant="outline-danger"
                                                size="sm"
                                                onClick={() => confirmRemoveRoute(route)}
                                                disabled={loading}
                                                className="d-flex align-items-center gap-2"
                                            >
                                                {loading ? (
                                                    <>
                                                        <Spinner size="sm" animation="border" />
                                                        Removing...
                                                    </>
                                                ) : (
                                                    'Remove'
                                                )}
                                            </Button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </Table>
                    )}
                </Modal.Body>
            </Modal>

            <ConfirmationModal
                show={confirmModal.show}
                onHide={() => setConfirmModal(prev => ({ ...prev, show: false }))}
                onConfirm={confirmModal.onConfirm}
                title={confirmModal.title}
                message={confirmModal.message}
                variant={confirmModal.variant}
                confirmLabel={loading ? 'Removing...' : 'Remove'}
                disabled={loading}
            />
        </>
    );
};

export default TeamRoutesModal; 