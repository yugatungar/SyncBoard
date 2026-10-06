import { createTask, updateTask, deleteTask } from './taskApi';
import { getQueue, removeOperationById } from './offlineQueue';

/**
 * Processes the offline operation queue in FIFO order.
 * 
 * @param {function} onConflict Callback invoked when a 409 Conflict is returned by server.
 *                             Signature: onConflict({ pendingOperation, currentTask })
 * @returns {Promise<{syncedCount: number, conflictOccurred: boolean}>}
 */
export const processOfflineQueue = async (onConflict) => {
  const queue = getQueue();
  if (queue.length === 0) {
    return { syncedCount: 0, conflictOccurred: false };
  }

  let syncedCount = 0;

  for (const item of queue) {
    try {
      if (item.type === 'CREATE') {
        await createTask(item.payload.workspaceId, item.payload.taskData, item.operationId);
      } else if (item.type === 'UPDATE') {
        await updateTask(item.payload.taskId, item.payload.taskData, item.operationId);
      } else if (item.type === 'DELETE') {
        await deleteTask(item.payload.taskId, item.operationId);
      }

      // Successfully processed (or 200 OK duplicate) -> remove from queue
      removeOperationById(item.id);
      syncedCount++;

    } catch (err) {
      if (err.response?.status === 409) {
        // Version conflict! Extract server version from response
        const serverTask = err.response?.data?.currentTask || null;
        removeOperationById(item.id);
        if (onConflict) {
          onConflict({
            pendingOperation: item,
            currentTask: serverTask,
            message: err.response?.data?.message || 'Version conflict detected'
          });
        }
        return { syncedCount, conflictOccurred: true };

      } else if (err.response?.status === 400 || err.response?.status === 404) {
        // Invalid transition or task no longer exists on server -> remove stale op
        console.warn(`Removing invalid offline operation ${item.id} (${item.type}):`, err.response?.data?.message);
        removeOperationById(item.id);

      } else {
        // Network error or server still unreachable -> halt sync loop, retry later
        console.warn('Network error during sync, halting queue processing:', err.message);
        return { syncedCount, conflictOccurred: false };
      }
    }
  }

  return { syncedCount, conflictOccurred: false };
};
