const QUEUE_KEY = 'syncboard_offline_queue';

/**
 * Gets all queued offline operations.
 * @returns {Array<{id: string, operationId: string, type: string, payload: object, timestamp: number}>}
 */
export const getQueue = () => {
  try {
    const data = localStorage.getItem(QUEUE_KEY);
    return data ? JSON.parse(data) : [];
  } catch (err) {
    console.error('Failed to read offline queue from localStorage:', err);
    return [];
  }
};

/**
 * Saves queue array to localStorage.
 */
const saveQueue = (queue) => {
  try {
    localStorage.setItem(QUEUE_KEY, JSON.stringify(queue));
    // Dispatch custom event for UI updates across components
    window.dispatchEvent(new Event('offline_queue_changed'));
  } catch (err) {
    console.error('Failed to save offline queue to localStorage:', err);
  }
};

/**
 * Enqueues a new operation to the offline queue.
 * Generates a unique operationId for idempotency.
 */
export const enqueueOperation = (type, payload) => {
  const queue = getQueue();
  const operationId = crypto.randomUUID();
  const item = {
    id: operationId,
    operationId,
    type, // 'CREATE' | 'UPDATE' | 'DELETE'
    payload,
    timestamp: Date.now()
  };

  queue.push(item);
  saveQueue(queue);
  return item;
};

/**
 * Removes and returns the oldest operation from the queue.
 */
export const dequeueOperation = () => {
  const queue = getQueue();
  if (queue.length === 0) return null;
  const item = queue.shift();
  saveQueue(queue);
  return item;
};

/**
 * Removes a specific operation by its ID.
 */
export const removeOperationById = (id) => {
  const queue = getQueue();
  const updated = queue.filter(item => item.id !== id);
  saveQueue(updated);
};

/**
 * Clears all operations from the queue.
 */
export const clearQueue = () => {
  saveQueue([]);
};

/**
 * Gets current count of pending operations.
 */
export const getQueueCount = () => {
  return getQueue().length;
};
