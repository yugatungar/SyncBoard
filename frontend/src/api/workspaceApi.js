import { apiClient } from './apiClient';

export const getWorkspaces = async () => {
  const response = await apiClient.get('/api/workspaces');
  return response.data;
};

export const createWorkspace = async (name) => {
  const response = await apiClient.post('/api/workspaces', { name });
  return response.data;
};

export const getWorkspaceMembers = async (workspaceId) => {
  const response = await apiClient.get(`/api/workspaces/${workspaceId}/members`);
  return response.data;
};

export const addWorkspaceMember = async (workspaceId, email, role) => {
  const response = await apiClient.post(`/api/workspaces/${workspaceId}/members`, { email, role });
  return response.data;
};

export const removeWorkspaceMember = async (workspaceId, userId) => {
  const response = await apiClient.delete(`/api/workspaces/${workspaceId}/members/${userId}`);
  return response.data;
};

export const updateWorkspaceMemberRole = async (workspaceId, userId, role) => {
  const response = await apiClient.put(`/api/workspaces/${workspaceId}/members/${userId}/role`, { role });
  return response.data;
};
