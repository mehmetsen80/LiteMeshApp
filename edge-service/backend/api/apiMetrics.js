import express from 'express';
import ApiMetric from '../models/ApiMetric.js';

const router = express.Router();

// GET all metrics
router.get('/', async (req, res) => {
    try {
        const metrics = await ApiMetric.find({})
            .select({
                routeIdentifier: 1,
                fromService: 1,
                toService: 1,
                interactionType: 1,
                gatewayBaseUrl: 1,
                pathEndPoint: 1,
                queryParameters: 1,
                timestamp: 1,
                duration: 1,
                success: 1
            })
            .sort({ timestamp: -1 });
        res.json(metrics);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
});


// GET a specific metric by ID
router.get('/:id', async (req, res) => {
    const { id } = req.params;
    try {
        const metric = await ApiMetric.findById(id);
        if (!metric) {
            return res.status(404).json({ error: 'Metric not found' });
        }
        res.json(metric);
    } catch (err) {
        console.error(`Error retrieving metric with ID ${id}:`, err);
        res.status(500).json({ error: 'Failed to retrieve metric' });
    }
});

// POST a new metric
router.post('/', async (req, res) => {
    try {
        const newMetric = new ApiMetric(req.body);
        const savedMetric = await newMetric.save();
        res.status(201).json(savedMetric);
    } catch (err) {
        console.error('Error saving new metric:', err);
        res.status(500).json({ error: 'Failed to save new metric' });
    }
});

// DELETE a metric by ID
router.delete('/:id', async (req, res) => {
    const { id } = req.params;
    try {
        const deletedMetric = await ApiMetric.findByIdAndDelete(id);
        if (!deletedMetric) {
            return res.status(404).json({ error: 'Metric not found' });
        }
        res.json({ message: 'Metric deleted successfully' });
    } catch (err) {
        console.error(`Error deleting metric with ID ${id}:`, err);
        res.status(500).json({ error: 'Failed to delete metric' });
    }
});

export default router;
