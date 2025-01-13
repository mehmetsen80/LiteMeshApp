export const formatDateTime = (timestamp) => {
    if (!timestamp) return '-';
    const date = new Date(timestamp);
    return date.toLocaleString('en-US', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
};

export const getDefaultDateRange = () => {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);
    
    // Set start to beginning of the day (00:00:00)
    startDate.setHours(0, 0, 0, 0);
    
    // Set end to end of the day (23:59:59.999)
    endDate.setHours(23, 59, 59, 999);
    
    return { startDate, endDate };
};

export const formatDateForApi = (date) => {
    if (!date) return null;
    return new Date(date).toISOString();
}; 