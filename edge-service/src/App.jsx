import './App.css'
import { BrowserRouter as Router } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import './assets/styles/globals.css';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import AppRoutes from './routes/AppRoutes';
import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { TeamProvider } from './contexts/TeamContext';
import { EnvironmentProvider } from './contexts/EnvironmentContext';

function RouteLogger() {
  const location = useLocation();
  
  useEffect(() => {
    const fullUrl = new URL(window.location.href);
  }, [location]);
  
  return null;
}

function App() {
  return (
    <EnvironmentProvider>
      <Router>
        <div>
          <RouteLogger />
          <AuthProvider>
            <TeamProvider>
              <div style={{ display: 'none' }}>
                Current route: {window.location.pathname}
                Search: {window.location.search}
              </div>
              <AppRoutes />
              <ToastContainer />
            </TeamProvider>
          </AuthProvider>
        </div>
      </Router>
    </EnvironmentProvider>
  );
}

export default App;
