import React from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { Navigate, Outlet } from 'react-router-dom';
import Header from '../../components/common/Header';
import './styles.css';

const AdminLayout = ({ children }) => {
    const { user } = useAuth();

    // If no user, redirect to login
    if (!user) {
        return <Navigate to="/login" replace />;
    }

    // If authenticated, show header and content
    return (
        <div className="admin-layout">
            <Header />
            <main className="main-content">
                <Outlet />
            </main>
        </div>
    );
};

export default AdminLayout; 