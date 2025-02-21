import React, { useState, useEffect } from 'react';
import './styles.css';
import {
    Dialog,
    DialogTitle,
    DialogContent,
    IconButton,
    Typography,
    Box,
    Tooltip,
    useTheme,
    useMediaQuery,
} from '@mui/material';
import Slide from '@mui/material/Slide';
import CloseIcon from '@mui/icons-material/Close';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '@mui/material/Alert';
import TrendChart from './TrendChart';
import ErrorBoundary from '../../common/ErrorBoundary';
import PropTypes from 'prop-types';
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord';

const Transition = React.forwardRef(function Transition(props, ref) {
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
    const isTablet = useMediaQuery(theme.breakpoints.between('sm', 'md'));

    // Choose direction based on screen size
    const direction = isMobile ? 'up' : isTablet ? 'left' : 'down';

    return (
        <Slide 
            direction={direction} 
            ref={ref} 
            {...props}
            timeout={{
                enter: 400,
                exit: 300
            }}
        />
    );
});

const StatTooltip = ({ title, children }) => (
    <Tooltip
        title={title}
        arrow
        placement="top"
    >
        <span style={{ cursor: 'help' }}>
            {children}
            <InfoOutlinedIcon 
                sx={{ fontSize: 14, ml: 0.5, verticalAlign: 'middle' }} 
            />
        </span>
    </Tooltip>
);

const MetricSummary = ({ title, data }) => {
    if (!data) return null;

    const { summary, recommendations, healthStatus } = data;
    const forecasts = summary.forecasts || {
        short: { timeframe: '30 minutes', value: summary.forecast, confidence: { lower: null, upper: null } },
        medium: { timeframe: '2 hours', value: null, confidence: { lower: null, upper: null } },
        long: { timeframe: '24 hours', value: null, confidence: { lower: null, upper: null } }
    };

    return (
        <div className="metric-summary">
            <div className="metric-header">
                <h3 className="metric-title">{title}</h3>
                <span className={`status-chip ${healthStatus.toLowerCase()}`}>
                    {healthStatus}
                </span>
            </div>
            <div className="metric-stats">
                <div className="metric-stat-item">
                    <StatTooltip title="Average value over the monitoring period">
                        Mean: <span className="metric-value">{summary.mean}</span>
                    </StatTooltip> | 
                    <StatTooltip title="Middle value when all measurements are ordered">
                        Median: <span className="metric-value">{summary.median}</span>
                    </StatTooltip> | 
                    <StatTooltip title="Measure of variability in the data">
                        Std Dev: <span className="metric-value">{summary.standardDeviation}</span>
                    </StatTooltip>
                </div>
                <div className="metric-stat-item">
                    Trend: 
                    <span className={`trend-indicator ${summary.trend.toLowerCase().includes('increasing') ? 'increasing' : 
                        summary.trend.toLowerCase().includes('decreasing') ? 'decreasing' : 'stable'}`}>
                        {summary.trend} ({summary.percentageChange}%)
                    </span>
                </div>
                <div className="metric-stat-item forecasts-container">
                    <div className="forecasts-header">
                        <StatTooltip title="Predictions based on historical patterns and current trends">
                            Forecasts
                        </StatTooltip>
                    </div>
                    {Object.entries(forecasts).map(([period, forecast]) => (
                        forecast.value && (
                            <div key={period} className="forecast-period">
                                <StatTooltip title={`Predicted value for next ${forecast.timeframe}${
                                    summary.trend.toLowerCase().includes('increasing') 
                                        ? ' (⚠️ May indicate potential issues if trend continues)' 
                                        : summary.trend.toLowerCase().includes('decreasing')
                                            ? ' (✅ Trending towards normal range)'
                                            : ' (✅ Expected to remain stable)'
                                }`}>
                                    {period.charAt(0).toUpperCase() + period.slice(1)} Term: 
                                    <span className={`metric-value ${
                                        summary.trend.toLowerCase().includes('increasing') && summary.percentageChange > 10
                                            ? 'warning'
                                            : summary.trend.toLowerCase().includes('decreasing') && summary.percentageChange < -10
                                                ? 'good'
                                                : ''
                                    }`}>
                                        {forecast.value}
                                        {forecast.confidence.lower && forecast.confidence.upper && (
                                            <span className="confidence-interval">
                                                ({forecast.confidence.lower} - {forecast.confidence.upper})
                                            </span>
                                        )}
                                    </span>
                                </StatTooltip>
                            </div>
                        )
                    ))}
                </div>

            </div>
            <div className="metric-chart">
                <TrendChart data={data} label={title} />
            </div>
            {recommendations && recommendations.length > 0 && (
                <div className="recommendations">
                    <h4>Recommendations</h4>
                    <div className="recommendation-list">
                        {recommendations.map((rec, index) => (
                            <div key={index} className="recommendation-item">
                                {rec}
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
};

const AnalysisSummaryDialog = ({ 
    open, 
    onClose, 
    data, 
    serviceId,
    loading,
    error 
}) => {
    // Add a prop to check if service is DOWN
    const isServiceDown = !data && !loading && !error;

    return (
        <Dialog 
            open={open} 
            onClose={onClose}
            maxWidth="md"
            fullWidth
        >
            <DialogTitle>
                <Box display="flex" alignItems="center">
                    <Typography variant="h6" component="div" style={{ flexGrow: 1 }}>
                        Service Analysis: {serviceId}
                    </Typography>
                    {/* Update status indicator based on service state */}
                    <Box display="flex" alignItems="center" mr={2}>
                        <FiberManualRecordIcon 
                            sx={{ 
                                color: isServiceDown ? 'error.main' :  // Red for DOWN
                                       error ? 'error.main' :          // Red for error
                                       loading ? 'warning.main' :      // Yellow for loading
                                       'success.main',                 // Green for active
                                marginRight: 1 
                            }} 
                        />
                        <Typography variant="body2">
                            {isServiceDown ? 'Service Down' :
                             error ? 'Error' :
                             loading ? 'Loading' : 'Active'}
                        </Typography>
                    </Box>
                    <IconButton onClick={onClose}>
                        <CloseIcon />
                    </IconButton>
                </Box>
            </DialogTitle>
            <DialogContent>
                {isServiceDown ? (
                    <Alert severity="error">
                        Service is currently down. No analysis data available.
                    </Alert>
                ) : loading ? (
                    <CircularProgress />
                ) : error ? (
                    <Alert severity="error">{error}</Alert>
                ) : data ? (
                    // Your existing analysis data display
                    <Box sx={{ 
                        display: 'grid',
                        gridTemplateColumns: 'repeat(2, 1fr)',
                        gap: 2,
                        mt: 2
                    }}>
                        <ErrorBoundary>
                            <Box>
                                <MetricSummary title="CPU Usage" data={data.cpu} />
                                <MetricSummary title="Memory Usage" data={data.memory} />
                            </Box>
                        </ErrorBoundary>
                        <ErrorBoundary>
                            <Box>
                                <MetricSummary title="Response Time" data={data.responseTime} />
                                <MetricSummary title="Error Rate" data={data.error} />
                            </Box>
                        </ErrorBoundary>
                    </Box>
                ) : (
                    <Alert severity="info">No analysis data available</Alert>
                )}
            </DialogContent>
        </Dialog>
    );
};

AnalysisSummaryDialog.propTypes = {
    open: PropTypes.bool.isRequired,
    onClose: PropTypes.func.isRequired,
    data: PropTypes.object,
    serviceId: PropTypes.string.isRequired,
    loading: PropTypes.bool,
    error: PropTypes.string
};

export default AnalysisSummaryDialog; 