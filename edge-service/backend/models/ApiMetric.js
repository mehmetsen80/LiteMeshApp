import mongoose from 'mongoose';

const apiMetricSchema = new mongoose.Schema({
    fromService: { type: String, required: true },
    toService: { type: String, required: true },
    timestamp: { type: Date, required: true },
    duration: { type: Number, required: true },
    routeIdentifier: { type: String, required: true },
    interactionType: { type: String, required: true },
    gatewayBaseUrl: { type: String, required: true },
    pathEndPoint: { type: String, required: true },
    queryParameters: { type: String, default: '' },
    success: { type: Boolean, required: true },
    _class: { type: String, required: true },
}, { collection: 'apiMetrics' });

export default mongoose.model('ApiMetric', apiMetricSchema);
