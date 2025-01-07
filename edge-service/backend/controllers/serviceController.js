import { getRoutes, getRouteById } from '../services/routeService.js';

export const getAllRoutes = async (req, res) => {
    try {
        const routes = await getRoutes();
        res.json(routes);
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch routes' });
    }
};

export const getRoute = async (req, res) => {
    try {
        const route = await getRouteById(req.params.id);
        if (!route) {
            return res.status(404).json({ error: 'Route not found' });
        }
        res.json(route);
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch route' });
    }
}; 