import React, { useState, useEffect } from 'react';
import { Modal, Form, Spinner, Table } from 'react-bootstrap';
import AsyncSelect from 'react-select/async';
import Select from 'react-select';
import userService from '../../services/userService';
import { teamService } from '../../services/teamService';
import ConfirmationModal from '../common/ConfirmationModal';
import Button from '../common/Button';

const TeamMembersModal = ({ show, onHide, team, onAddMember, onRemoveMember, loading }) => {
    const [selectedUser, setSelectedUser] = useState(null);
    const [role, setRole] = useState('USER');
    const [error, setError] = useState('');
    const [searching, setSearching] = useState(false);
    const [members, setMembers] = useState([]);
    const [loadingMembers, setLoadingMembers] = useState(false);
    const [showConfirmRemove, setShowConfirmRemove] = useState(false);
    const [memberToRemove, setMemberToRemove] = useState(null);

    const roleOptions = [
        { value: 'USER', label: 'User' },
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
        if (!team?.id) return;

        try {
            setLoadingMembers(true);
            const response = await teamService.getTeamMembers(team.id);
            if (response.error) {
                throw new Error(response.error);
            }
            setMembers(response.data || []);
        } catch (err) {
            console.error('Failed to fetch members:', err);
            setError('Failed to load team members');
            setMembers([]);
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
            setRole('USER');
            setError('');
            await fetchMembers();
        } catch (err) {
            setError(err.message || err.error || 'Failed to add member');
        } finally {
            setSearching(false);
        }
    };

    const handleRemoveMember = async () => {
        try {
            const response = await onRemoveMember(memberToRemove.userId);
            if (response?.error) {
                throw new Error(response.error);
            }
            await fetchMembers(); // Refresh the list after successful removal
            setShowConfirmRemove(false);
            setMemberToRemove(null);
            setError('');
        } catch (err) {
            console.error('Failed to remove member:', err);
            setError(err.message || 'Failed to remove member');
        }
    };

    const confirmRemoveMember = (member) => {
        setMemberToRemove(member);
        setShowConfirmRemove(true);
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
                            disabled={!selectedUser}
                            loading={loading || searching}
                            fullWidth
                        >
                            Add Member
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
                                                loading={loading}
                                            >
                                                Remove
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
                show={showConfirmRemove}
                onHide={() => {
                    setShowConfirmRemove(false);
                    setMemberToRemove(null);
                }}
                onConfirm={handleRemoveMember}
                title="Remove Member"
                message={memberToRemove ? 
                    `Are you sure you want to remove ${memberToRemove.username} from team "${team.name}"?` 
                    : ''
                }
                confirmLabel="Remove"
                variant="danger"
            />
        </>
    );
};

export default TeamMembersModal; 