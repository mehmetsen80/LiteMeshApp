import React from 'react';
import { Alert, Button, Box } from '@mui/material';

class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }

    componentDidCatch(error, errorInfo) {
        console.error('Error caught by boundary:', error, errorInfo);
    }

    handleReset = () => {
        this.setState({ hasError: false, error: null });
        window.location.reload();
    }

    render() {
        if (this.state.hasError) {
            return (
                <Box sx={{ p: 3 }}>
                    <Alert 
                        severity="error" 
                        action={
                            <Button color="inherit" size="small" onClick={this.handleReset}>
                                Retry
                            </Button>
                        }
                    >
                        Something went wrong. Please try again.
                    </Alert>
                </Box>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary; 