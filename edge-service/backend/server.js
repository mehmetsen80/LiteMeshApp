import express from 'express';
import mongoose from 'mongoose';
import cors from 'cors';
import dotenv from 'dotenv';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import apiMetricsRoutes from './api/apiMetrics.js';
import authRoutes from './api/authRoutes.js';
import serviceRoutes from './routes/serviceRoutes.js';

// Get the directory path of the current module
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Load .env from root directory
dotenv.config({ path: join(__dirname, '../.env') });

const app = express();
const PORT = process.env.BACKEND_PORT || 5000;
const HOST = 'localhost';
const FRONTEND_URL = process.env.VITE_FRONTEND_URL || 'http://localhost:3000';

// Debug log to verify environment variables are loaded
console.log('Environment Variables:');
console.log('VITE_MONGODB_URI:', process.env.VITE_MONGODB_URI);
console.log('VITE_MONGODB_DB_NAME:', process.env.VITE_MONGODB_DB_NAME);
console.log('BACKEND_PORT:', process.env.BACKEND_PORT);
console.log('FRONTEND_URL:', FRONTEND_URL);

// CORS configuration
const corsOptions = {
    origin: FRONTEND_URL,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
    credentials: true,
    optionsSuccessStatus: 200
};

// Middleware
app.use(cors(corsOptions));
app.use(express.json());

// Add pre-flight handling
app.options('*', cors(corsOptions));

// MongoDB connection
const mongoURI = process.env.VITE_MONGODB_URI;
const dbName = process.env.VITE_MONGODB_DB_NAME;

if (!mongoURI || !dbName) {
    console.error('MongoDB configuration missing. Please check your .env file.');
    process.exit(1);
}

mongoose.connect(mongoURI, {
    useNewUrlParser: true,
    useUnifiedTopology: true,
    dbName: dbName
})
.then(() => console.log(`Connected to MongoDB database: ${dbName}`))
.catch((err) => {
    console.error('MongoDB connection error:', err);
    process.exit(1);
});

// Error handling for MongoDB connection
mongoose.connection.on('error', (err) => {
    console.error('MongoDB connection error:', err);
});

mongoose.connection.on('disconnected', () => {
    console.log('MongoDB disconnected');
});

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/metrics', apiMetricsRoutes);
app.use('/api/services', serviceRoutes);

// Global error handler
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ 
        error: 'Something broke!',
        message: process.env.NODE_ENV === 'development' ? err.message : 'Internal Server Error'
    });
});

// Start Server
app.listen(PORT, HOST, () => {
    console.log(`Server running at http://${HOST}:${PORT}`);
    console.log(`Accepting requests from: ${FRONTEND_URL}`);
});

// Handle graceful shutdown
process.on('SIGTERM', gracefulShutdown);
process.on('SIGINT', gracefulShutdown);

async function gracefulShutdown() {
    try {
        await mongoose.connection.close();
        console.log('MongoDB connection closed');
        
        // Close WebSocket server
        wss.close(() => {
            console.log('WebSocket server closed');
            process.exit(0);
        });
    } catch (err) {
        console.error('Error during shutdown:', err);
        process.exit(1);
    }
}