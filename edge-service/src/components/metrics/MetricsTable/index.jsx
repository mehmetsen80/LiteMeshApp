import './styles.css';

function MetricsTable({ data }) {
  return (
    <div className="metrics-table">
      {/* <h2>Detailed Metrics</h2> */}
      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>From Service</th>
              <th>To Service</th>
              <th>Duration</th>
              <th>Timestamp</th>
              <th>Success</th>
            </tr>
          </thead>
          <tbody>
            {data.map((metric, index) => (
              <tr key={index}>
                <td>{metric.fromService}</td>
                <td>{metric.toService}</td>
                <td>{metric.duration}ms</td>
                <td>{new Date(metric.timestamp).toLocaleString()}</td>
                <td className={metric.success ? 'success' : 'error'}>
                  {metric.success ? '✅' : '❌'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default MetricsTable;