import './App.css'
import MetricsDisplay from './components/MetricsDisplay';

function App() {
    // const [metrics, setMetrics] = useState([]);

    // useEffect(() => {
    //     fetch(`${import.meta.env.VITE_BACKEND_URL}/api/metrics`, {mode: 'cors'})
    //         .then(response => response.json())
    //         .then(data => setMetrics(data))
    //         .catch(err => console.error('Error fetching metrics:', err));
    // }, []);

    return (
        // <div>
        //     <h1>API Metrics</h1>
        //     <ul>
        //         {metrics.map(metric => (
        //             <li key={metric._id}>
        //                 {metric.fromService} → {metric.toService} | Duration: {metric.duration} ms
        //             </li>
        //         ))}
        //     </ul>
         
        // </div>
        <div>
          <MetricsDisplay />
        </div>
        
    );
}

export default App;
