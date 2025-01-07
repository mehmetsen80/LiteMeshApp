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
    endDate.setHours(23, 59, 59, 999); // Set to end of day
    
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 60); // 60 days ago
    startDate.setHours(0, 0, 0, 0); // Set to start of day

    return { startDate, endDate };
}; 