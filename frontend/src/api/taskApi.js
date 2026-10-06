import { apiClient } from './apiClient';

export const getWorkspaceTasks = async (workspaceId) => {
  const response = await apiClient.get(`/api/workspaces/${workspaceId}/tasks`);
  return response.data;
};

export const createTask = async (workspaceId, taskData, operationId) => {
  const body = operationId ? { ...taskData, operationId } : taskData;
  const response = await apiClient.post(`/api/workspaces/${workspaceId}/tasks`, body);
  return response.data;
};

export const updateTask = async (taskId, taskData, operationId) => {
  const body = operationId ? { ...taskData, operationId } : taskData;
  const response = await apiClient.put(`/api/tasks/${taskId}`, body);
  return response.data;
};

export const deleteTask = async (taskId, operationId) => {
  const url = operationId ? `/api/tasks/${taskId}?operationId=${encodeURIComponent(operationId)}` : `/api/tasks/${taskId}`;
  const response = await apiClient.delete(url);
  return response.data;
};
