import { Client } from '@stomp/stompjs';

let stompClient = null;
let activeSubscription = null;
let currentSubscribedWorkspaceId = null;

export const connectWebSocket = (onStatusChange) => {
  if (stompClient && stompClient.active) {
    return stompClient;
  }

  const token = localStorage.getItem('syncboard_jwt_token');

  stompClient = new Client({
    brokerURL: 'ws://localhost:8080/ws',
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 3000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: () => {
      console.log('[WebSocket] Connected to STOMP broker');
      if (onStatusChange) onStatusChange('CONNECTED');
    },
    onDisconnect: () => {
      console.log('[WebSocket] Disconnected from STOMP broker');
      if (onStatusChange) onStatusChange('DISCONNECTED');
    },
    onStompError: (frame) => {
      console.error('[WebSocket] STOMP error:', frame.headers['message'], frame.body);
      if (onStatusChange) onStatusChange('DISCONNECTED');
    },
    onWebSocketClose: () => {
      if (onStatusChange) onStatusChange('DISCONNECTED');
    },
  });

  stompClient.activate();
  return stompClient;
};

export const subscribeToWorkspace = (workspaceId, onEvent) => {
  if (!stompClient || !stompClient.active) {
    console.warn('[WebSocket] Cannot subscribe: Client is not connected');
    return;
  }

  if (currentSubscribedWorkspaceId === workspaceId && activeSubscription) {
    console.log(`[WebSocket] Already subscribed to workspace #${workspaceId}`);
    return;
  }

  if (activeSubscription) {
    console.log(`[WebSocket] Unsubscribing from previous workspace #${currentSubscribedWorkspaceId}`);
    activeSubscription.unsubscribe();
    activeSubscription = null;
    currentSubscribedWorkspaceId = null;
  }

  const token = localStorage.getItem('syncboard_jwt_token');
  const topicDestination = `/topic/workspaces/${workspaceId}`;
  console.log(`[WebSocket] Subscribing to ${topicDestination}`);

  activeSubscription = stompClient.subscribe(
    topicDestination,
    (message) => {
      try {
        const eventPayload = JSON.parse(message.body);
        console.log('[WebSocket] Event received:', eventPayload);
        if (onEvent) {
          onEvent(eventPayload);
        }
      } catch (err) {
        console.error('[WebSocket] Failed to parse event payload:', err);
      }
    },
    token ? { Authorization: `Bearer ${token}` } : {}
  );

  currentSubscribedWorkspaceId = workspaceId;
};

export const disconnectWebSocket = () => {
  if (activeSubscription) {
    activeSubscription.unsubscribe();
    activeSubscription = null;
    currentSubscribedWorkspaceId = null;
  }

  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
  }
};
