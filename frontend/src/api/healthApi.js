import { apiClient } from './apiClient';

export const getHealthStatus = async () => {
  try {
    const response = await apiClient.get('/api/health');
    return response.data;
  } catch (error) {
    return {
      status: 'DISCONNECTED',
      database: 'DOWN',
      error: error.message || 'Failed to connect to backend server',
    };
  }
};
