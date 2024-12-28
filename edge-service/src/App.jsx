import './App.css'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AdminLayout from './layouts/AdminLayout';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Metrics from './pages/Metrics';
import './assets/styles/global.css';

function App() {
    // // const [metrics, setMetrics] = useState([]);

    // // useEffect(() => {
    // //     fetch(`${import.meta.env.VITE_BACKEND_URL}/api/metrics`, {mode: 'cors'})
    // //         .then(response => response.json())
    // //         .then(data => setMetrics(data))
    // //         .catch(err => console.error('Error fetching metrics:', err));
    // // }, []);

    // return (
    //     // <div>
    //     //     <h1>API Metrics</h1>
    //     //     <ul>
    //     //         {metrics.map(metric => (
    //     //             <li key={metric._id}>
    //     //                 {metric.fromService} → {metric.toService} | Duration: {metric.duration} ms
    //     //             </li>
    //     //         ))}
    //     //     </ul>
         
    //     // </div>
    //     <div>
    //       <MetricsDisplay />
    //     </div>
        
    // );

    return (
      <Router>
        <AuthProvider>
          <AdminLayout>
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route 
                path="/" 
                element={
                  <ProtectedRoute>
                    <Home />
                  </ProtectedRoute>
                } 
              />
              <Route 
                path="/metrics" 
                element={
                  <ProtectedRoute>
                    <Metrics />
                  </ProtectedRoute>
                } 
              />
            </Routes>
          </AdminLayout>
        </AuthProvider>
      </Router>
    );
}

export default App;
