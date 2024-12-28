import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import Header from '../../components/common/Header';
import './styles.css';

function AdminLayout({ children }) {
  const { user } = useAuth();
  const location = useLocation();

  // List of public routes that don't require authentication
  const publicRoutes = ['/login', '/register'];

  if (!user && !publicRoutes.includes(location.pathname)) {
    // Redirect to login if user is not authenticated and trying to access protected route
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return (
    <div className="admin-layout">
      <Header />
      <main className="main-content">
        {children}
      </main>
    </div>
  );
}

export default AdminLayout; 