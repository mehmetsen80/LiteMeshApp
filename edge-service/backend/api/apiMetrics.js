import express from 'express';
import ApiMetric from '../models/ApiMetric.js';

const router = express.Router();

// GET all metrics
router.get('/', async (req, res) => {
    console.log('Received GET /api/metrics request');
    try {
        const metrics = await ApiMetric.find();
        console.log('Metrics retrieved:', metrics);
        res.json(metrics);
    } catch (err) {
        console.error('Error retrieving metrics:', err);
        res.status(500).json({ error: 'Failed to retrieve metrics' });
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
