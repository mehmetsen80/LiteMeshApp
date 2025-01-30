import React, { useState, useEffect } from 'react';
import { Modal, Button, Form, Spinner, Table } from 'react-bootstrap';
import AsyncSelect from 'react-select/async';
import Select from 'react-select';
import userService from '../../services/userService';
import teamService from '../../services/teamService';
import ConfirmationModal from '../common/ConfirmationModal';

const TeamMembersModal = ({ show, onHide, team, onAddMember, onRemoveMember, loading }) => {
    const [selectedUser, setSelectedUser] = useState(null);
    const [role, setRole] = useState('MEMBER');
    const [error, setError] = useState('');
    const [searching, setSearching] = useState(false);
    const [members, setMembers] = useState([]);
    const [loadingMembers, setLoadingMembers] = useState(false);
    const [confirmModal, setConfirmModal] = useState({
        show: false,
        title: '',
        message: '',
        onConfirm: () => {},
        variant: 'danger'
    });

    const roleOptions = [
        { value: 'MEMBER', label: 'Member' },
        { value: 'ADMIN', label: 'Admin' }
    ];

    const loadUserOptions = async (inputValue) => {
        if (!inputValue) return [];
        try {
            const { data, error } = await userService.searchUsers(inputValue);
            if (error) throw new Error(error);
            
            // Filter out users that are already members
            const filteredUsers = data.filter(user => 
                !members.find(m => m.username === user.username)
            );
            
            return filteredUsers.map(user => ({
                value: user.username,
                label: user.email 
                    ? `${user.username} - ${user.email}`
                    : user.username,
                searchLabel: `${user.username} ${user.email || ''}` // For better searching
            }));
        } catch (err) {
            console.error('Failed to load users:', err);
            return [];
        }
    };

    useEffect(() => {
        if (team) {
            fetchMembers();
        }
    }, [team]);

    const fetchMembers = async () => {
        try {
            setLoadingMembers(true);
            const { data, error } = await teamService.getTeamMembers(team.id);
            if (error) throw new Error(error);
            setMembers(data);
        } catch (err) {
            console.error('Failed to fetch members:', err);
        } finally {
            setLoadingMembers(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSearching(true);

        try {
            await onAddMember({
                username: selectedUser.value,
                role: role
            });

            // Clear form on success
            setSelectedUser(null);
            setRole('MEMBER');
            setError('');
            await fetchMembers();
        } catch (err) {
            setError(err.message || err.error || 'Failed to add member');
        } finally {
            setSearching(false);
        }
    };

    const confirmRemoveMember = (member) => {
        setConfirmModal({
            show: true,
            title: 'Remove Member',
            message: `Are you sure you want to remove ${member.username} from team "${team.name}"?`,
            onConfirm: async () => {
                try {
                    await onRemoveMember(member.userId);
                    await fetchMembers(); // Refresh the members list
                    setConfirmModal(prev => ({ ...prev, show: false }));
                } catch (err) {
                    setError(err.message || 'Failed to remove member');
                }
            },
            variant: 'warning'
        });
    };

    return (
        <>
            <Modal show={show} onHide={onHide} size="lg">
                <Modal.Header closeButton>
                    <Modal.Title>Team Members - {team?.name}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form onSubmit={handleSubmit} className="mb-4">
                        <Form.Group className="mb-4">
                            <Form.Label>Username</Form.Label>
                            <AsyncSelect
                                cacheOptions
                                defaultOptions
                                value={selectedUser}
                                onChange={(option) => {
                                    setSelectedUser(option);
                                    setError('');
                                }}
                                loadOptions={loadUserOptions}
                                className="form-select-lg"
                                placeholder="Search by username or email..."
                                isDisabled={loading || searching}
                                noOptionsMessage={({ inputValue }) => 
                                    inputValue ? "No users found" : "Type to search by username or email"
                                }
                                filterOption={(option, input) => {
                                    if (!input) return true;
                                    return option.data.searchLabel
                                        .toLowerCase()
                                        .includes(input.toLowerCase());
                                }}
                            />
                            {error && <div className="text-danger mt-2">{error}</div>}
                        </Form.Group>

                        <Form.Group className="mb-4">
                            <Form.Label>Role</Form.Label>
                            <Select
                                value={roleOptions.find(o => o.value === role)}
                                onChange={(option) => setRole(option.value)}
                                options={roleOptions}
                                className="form-select-lg"
                            />
                        </Form.Group>

                        <Button 
                            variant="primary" 
                            type="submit"
                            disabled={loading || searching || !selectedUser}
                        >
                            {searching ? (
                                <><Spinner size="sm" animation="border" /> Searching...</>
                            ) : loading ? (
                                <><Spinner size="sm" animation="border" /> Adding...</>
                            ) : (
                                'Add Member'
                            )}
                        </Button>
                    </Form>

                    <hr />

                    <h6 className="mb-3">Current Members</h6>
                    {loadingMembers ? (
                        <div className="text-center py-4">
                            <Spinner animation="border" size="sm" /> Loading members...
                        </div>
                    ) : members.length === 0 ? (
                        <p className="text-muted">No members in this team yet.</p>
                    ) : (
                        <Table hover className="mt-3">
                            <thead>
                                <tr>
                                    <th>Username</th>
                                    <th>Role</th>
                                    <th>Status</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {members.map(member => (
                                    <tr key={member.userId}>
                                        <td>{member.username}</td>
                                        <td>
                                            <span className={`badge bg-${member.role === 'ADMIN' ? 'primary' : 'secondary'}`}>
                                                {member.role}
                                            </span>
                                        </td>
                                        <td>
                                            <span className={`badge bg-${member.status === 'ACTIVE' ? 'success' : 'warning'}`}>
                                                {member.status}
                                            </span>
                                        </td>
                                        <td>
                                            <Button
                                                variant="outline-danger"
                                                size="sm"
                                                onClick={() => confirmRemoveMember(member)}
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
            />
        </>
    );
};

export default TeamMembersModal; 