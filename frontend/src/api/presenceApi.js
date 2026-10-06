import { apiClient } from './apiClient';

/**
 * Sends a periodic presence heartbeat for a client connected to a workspace.
 */
export const sendPresenceHeartbeat = async (workspaceId, clientId) => {
  if (!workspaceId || !clientId) return null;
  try {
    const response = await apiClient.post(
      `/api/workspaces/${workspaceId}/presence/heartbeat?clientId=${encodeURIComponent(clientId)}`
    );
    return response.data;
  } catch (err) {
    console.warn('Presence heartbeat failed:', err.message);
    return null;
  }
};

/**
 * Fetches the current count of online users for a workspace.
 */
export const getWorkspacePresence = async (workspaceId) => {
  if (!workspaceId) return { onlineCount: 0 };
  try {
    const response = await apiClient.get(`/api/workspaces/${workspaceId}/presence`);
    return response.data;
  } catch (err) {
    console.warn('Failed to fetch workspace presence:', err.message);
    return { onlineCount: 1 };
  }
};

/**
 * Sends a presence disconnect request when a client leaves a workspace.
 */
export const sendPresenceDisconnect = async (workspaceId, clientId) => {
  if (!workspaceId || !clientId) return null;
  try {
    const response = await apiClient.post(
      `/api/workspaces/${workspaceId}/presence/disconnect?clientId=${encodeURIComponent(clientId)}`
    );
    return response.data;
  } catch (err) {
    console.warn('Presence disconnect failed:', err.message);
    return null;
  }
};
