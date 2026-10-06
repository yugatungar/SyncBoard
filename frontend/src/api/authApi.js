import { apiClient } from './apiClient';

export const registerUser = async (name, email, password) => {
  const response = await apiClient.post('/api/auth/register', { name, email, password });
  return response.data;
};

export const loginUser = async (email, password) => {
  const response = await apiClient.post('/api/auth/login', { email, password });
  return response.data;
};
