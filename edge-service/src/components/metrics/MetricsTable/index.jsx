import React, { useEffect, useState } from 'react';
import { 
    Table, 
    TableBody, 
    TableCell, 
    TableContainer, 
    TableHead, 
    TableRow, 
    Paper,
    Typography,
    TextField,
    Box,
    InputAdornment,
    IconButton 
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { formatDateTime } from '../../../utils/dateUtils';
import './styles.css';

const MetricsTable = ({ metrics = [] }) => {
    const [filters, setFilters] = useState({
        timestamp: '',
        fromService: '',
        toService: '',
        routeIdentifier: '',
        interactionType: '',
        pathEndPoint: '',
        queryParameters: '',
        duration: '',
        status: ''
    });

    const filteredMetrics = metrics.filter(metric => {
        return Object.keys(filters).every(key => {
            const filterValue = filters[key].toLowerCase();
            if (!filterValue) return true;

            if (key === 'status') {
                const success = metric.success ? 'success' : 'failed';
                return success.includes(filterValue);
            }

            if (key === 'timestamp') {
                return formatDateTime(metric.timestamp)
                    .toLowerCase()
                    .includes(filterValue);
            }

            const metricValue = String(metric[key] || '').toLowerCase();
            return metricValue.includes(filterValue);
        });
    });

    const handleFilterChange = (column) => (event) => {
        setFilters(prev => ({
            ...prev,
            [column]: event.target.value
        }));
    };

    if (!metrics || metrics.length === 0) {
        return (
            <TableContainer component={Paper} sx={{ mt: 4 }}>
                <Typography variant="h6" sx={{ p: 2 }}>
                    Detailed Metrics Table
                </Typography>
                <Typography sx={{ p: 2 }}>
                    No metrics data available
                </Typography>
            </TableContainer>
        );
    }

    return (
        <TableContainer 
            component={Paper} 
            className="metrics-table-container"
            sx={{ 
                mt: 0,
                maxHeight: 800,
                '& .MuiTableHead-root': {
                    position: 'sticky',
                    top: 0,
                    backgroundColor: '#f5f5f5',
                    zIndex: 0
                },
                '& .MuiTableBody-root .MuiTableRow-root:nth-of-type(odd)': {
                    backgroundColor: '#ffffff',
                },
                '& .MuiTableBody-root .MuiTableRow-root:nth-of-type(even)': {
                    backgroundColor: '#fafafa',
                }
            }}
        >
            {/* <Typography variant="h6" sx={{ p: 2 }}>
                Detailed Metrics Table
            </Typography> */}
            <Table stickyHeader className="metrics-table">
                <TableHead className="table-header">
                    <TableRow>
                        {Object.keys(filters).map(column => (
                            <TableCell 
                                key={column}
                                sx={{
                                    fontWeight: 'bold',
                                    backgroundColor: '#f5f5f5'
                                }}
                            >
                                <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                                    {column.charAt(0).toUpperCase() + column.slice(1)}
                                    <TextField
                                        size="small"
                                        variant="outlined"
                                        placeholder={`Filter ${column}`}
                                        value={filters[column]}
                                        onChange={handleFilterChange(column)}
                                        sx={{ mt: 1 }}
                                        InputProps={{
                                            startAdornment: (
                                                <InputAdornment position="start">
                                                    <SearchIcon fontSize="small" />
                                                </InputAdornment>
                                            )
                                        }}
                                    />
                                </Box>
                            </TableCell>
                        ))}
                    </TableRow>
                </TableHead>
                <TableBody>
                    {filteredMetrics.map((metric, index) => (
                        <TableRow 
                            key={index} 
                            className={metric.success ? 'success-row' : 'error-row'}
                            sx={{ 
                                '&:hover': {
                                    backgroundColor: metric.success ? '#f5f5f5' : '#ffcdd2'
                                }
                            }}
                        >
                            <TableCell>{formatDateTime(metric.timestamp)}</TableCell>
                            <TableCell>{metric.fromService}</TableCell>
                            <TableCell>{metric.toService}</TableCell>
                            <TableCell>{metric.routeIdentifier}</TableCell>
                            <TableCell>{metric.interactionType}</TableCell>
                            <TableCell>{metric.pathEndPoint}</TableCell>
                            <TableCell>{metric.queryParameters || '-'}</TableCell>
                            <TableCell>{metric.duration}</TableCell>
                            <TableCell>
                                <span style={{ 
                                    color: metric.success ? 'green' : 'red',
                                    fontWeight: 'bold'
                                }}>
                                    {metric.success ? 'SUCCESS' : 'FAILED'}
                                </span>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
};

export default MetricsTable;