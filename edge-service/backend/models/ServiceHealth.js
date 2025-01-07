import mongoose from 'mongoose';

const trendAnalysisSchema = new mongoose.Schema({
    percentageChange: Number,
    direction: String
}, { _id: false });

const serviceHealthSchema = new mongoose.Schema({
    serviceId: { type: String, required: true },
    healthy: { type: Boolean, required: true },
    status: { type: String, required: true },
    metrics: {
        memory: Number,
        responseTime: Number,
        cpu: Number
    },
    lastChecked: { type: Number, required: true },
    consecutiveFailures: { type: Number, default: 0 },
    uptime: String,
    trends: {
        memory: trendAnalysisSchema,
        responseTime: trendAnalysisSchema,
        cpu: trendAnalysisSchema,
        error: trendAnalysisSchema
    }
}, { timestamps: true });

export const ServiceHealth = mongoose.model('ServiceHealth', serviceHealthSchema); 