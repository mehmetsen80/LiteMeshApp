import axiosInstance from './axiosInstance';

const userService = {
    searchUsers: async (searchTerm) => {
        try {
            const response = await axiosInstance.get(`/api/users/search?query=${searchTerm}`);
            return { data: response.data };
        } catch (error) {
            return { error: error.response?.data?.message || 'Failed to search users' };
        }
    },

    getUserByUsername: async (username) => {
        try {
            const response = await axiosInstance.get(`/api/users/by-username/${username}`);
            return { data: response.data };
        } catch (error) {
            if (error.response?.status === 404) {
                return { error: `User "${username}" not found` };
            }
            return { 
                error: error.response?.data?.message || 'Failed to fetch user',
                status: error.response?.status
            };
        }
    }
};

export default userService; 