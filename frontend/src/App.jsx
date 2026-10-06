import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useAuth } from './contexts/AuthContext';
import { LoginPage } from './pages/LoginPage';
import { getHealthStatus } from './api/healthApi';
import {
  getWorkspaces,
  createWorkspace,
  getWorkspaceMembers,
  addWorkspaceMember,
  removeWorkspaceMember,
  updateWorkspaceMemberRole
} from './api/workspaceApi';
import { getWorkspaceTasks, createTask, updateTask, deleteTask } from './api/taskApi';
import { connectWebSocket, subscribeToWorkspace, disconnectWebSocket } from './api/websocketService';
import { useNetworkStatus } from './hooks/useNetworkStatus';
import { enqueueOperation, getQueueCount } from './api/offlineQueue';
import { processOfflineQueue } from './api/syncService';
import { sendPresenceHeartbeat, getWorkspacePresence, sendPresenceDisconnect } from './api/presenceApi';
import TaskBoard from './components/TaskBoard';
import CreateTaskForm from './components/CreateTaskForm';
import { ConflictModal } from './components/ConflictModal';
import { ActivityPanel } from './components/ActivityPanel';

const getClientId = () => {
  if (typeof window === 'undefined') return 'user-default';
  if (!window.name || !window.name.startsWith('syncboard-tab-')) {
    window.name = 'syncboard-tab-' + crypto.randomUUID().substring(0, 8);
  }
  return window.name;
};

export default function App() {
  const { user, isAuthenticated, logout } = useAuth();

  const [health, setHealth] = useState({ application: 'CHECKING...', database: 'CHECKING...', redis: 'CHECKING...' });
  const [workspaces, setWorkspaces] = useState([]);
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState(null);
  const [tasks, setTasks] = useState([]);
  const [members, setMembers] = useState([]);
  const [currentUserRole, setCurrentUserRole] = useState('EDITOR');
  const [wsStatus, setWsStatus] = useState('DISCONNECTED');
  const [onlineUsers, setOnlineUsers] = useState(1);

  const { isOnline } = useNetworkStatus();
  const [queuedCount, setQueuedCount] = useState(getQueueCount());
  const [isSyncing, setIsSyncing] = useState(false);

  const clientIdRef = useRef(getClientId());

  // Conflict modal state
  const [conflictData, setConflictData] = useState(null);
  const [showConflictModal, setShowConflictModal] = useState(false);

  // Activity panel toggle
  const [showActivity, setShowActivity] = useState(false);
  // Member management modal state
  const [showMemberModal, setShowMemberModal] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState('EDITOR');

  const [workspaceName, setWorkspaceName] = useState('');
  const [showTaskForm, setShowTaskForm] = useState(false);
  const [loadingHealth, setLoadingHealth] = useState(false);
  const [loadingWorkspaces, setLoadingWorkspaces] = useState(false);
  const [loadingTasks, setLoadingTasks] = useState(false);

  const [errorMsg, setErrorMsg] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  const selectedWorkspaceIdRef = useRef(selectedWorkspaceId);
  useEffect(() => {
    selectedWorkspaceIdRef.current = selectedWorkspaceId;
  }, [selectedWorkspaceId]);

  // Reset workspace selection & state when user changes or logs out
  useEffect(() => {
    setSelectedWorkspaceId(null);
    setWorkspaces([]);
    setTasks([]);
    setMembers([]);
    setErrorMsg(null);
    setSuccessMsg(null);
    setShowMemberModal(false);
    setShowTaskForm(false);
    setShowActivity(false);
  }, [user?.id]);

  useEffect(() => {
    const handleQueueChange = () => {
      setQueuedCount(getQueueCount());
    };
    window.addEventListener('offline_queue_changed', handleQueueChange);
    return () => window.removeEventListener('offline_queue_changed', handleQueueChange);
  }, []);

  const handleWsEvent = useCallback((event) => {
    const { type, task, taskId, eventType, onlineCount } = event;

    if (eventType === 'PRESENCE_UPDATED' || onlineCount !== undefined) {
      if (onlineCount !== undefined) {
        setOnlineUsers(onlineCount);
      }
      return;
    }

    if (type === 'MEMBER_UPDATED') {
      if (selectedWorkspaceIdRef.current) {
        fetchMembers(selectedWorkspaceIdRef.current);
      }
      fetchWorkspaces();
      return;
    }

    if (type === 'TASK_CREATED') {
      setTasks((prev) => {
        if (prev.find((t) => t.id === task.id)) return prev;
        return [...prev, task];
      });
    } else if (type === 'TASK_UPDATED') {
      setTasks((prev) => prev.map((t) => (t.id === task.id ? task : t)));
    } else if (type === 'TASK_DELETED') {
      setTasks((prev) => prev.filter((t) => t.id !== taskId));
    }
  }, []);

  // WebSocket connection lifecycle
  useEffect(() => {
    if (!isAuthenticated) return;

    connectWebSocket((status) => {
      setWsStatus(status);
      if (status === 'CONNECTED' && selectedWorkspaceIdRef.current) {
        subscribeToWorkspace(selectedWorkspaceIdRef.current, handleWsEvent);
      }
    });

    return () => {
      disconnectWebSocket();
    };
  }, [isAuthenticated, handleWsEvent]);

  useEffect(() => {
    if (isAuthenticated && selectedWorkspaceId && wsStatus === 'CONNECTED') {
      subscribeToWorkspace(selectedWorkspaceId, handleWsEvent);
    }
  }, [isAuthenticated, selectedWorkspaceId, wsStatus, handleWsEvent]);

  // Presence Heartbeat
  useEffect(() => {
    if (!isAuthenticated || !selectedWorkspaceId || !isOnline) return;

    const clientId = clientIdRef.current;
    sendPresenceHeartbeat(selectedWorkspaceId, clientId).then((res) => {
      if (res?.onlineCount !== undefined) setOnlineUsers(res.onlineCount);
    });

    const intervalId = setInterval(() => {
      sendPresenceHeartbeat(selectedWorkspaceId, clientId).then((res) => {
        if (res?.onlineCount !== undefined) setOnlineUsers(res.onlineCount);
      });
    }, 4000);

    const handleBeforeUnload = () => {
      sendPresenceDisconnect(selectedWorkspaceId, clientId);
    };
    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      clearInterval(intervalId);
      window.removeEventListener('beforeunload', handleBeforeUnload);
      sendPresenceDisconnect(selectedWorkspaceId, clientId);
    };
  }, [isAuthenticated, selectedWorkspaceId, isOnline]);

  const triggerSync = useCallback(async () => {
    if (!isOnline || isSyncing) return;
    setIsSyncing(true);

    try {
      const result = await processOfflineQueue(({ pendingOperation, currentTask, message }) => {
        setConflictData({ pendingOperation, currentTask, message });
        setShowConflictModal(true);
      });

      if (result.syncedCount > 0) {
        setSuccessMsg(`Synced ${result.syncedCount} offline operation(s) successfully!`);
        if (selectedWorkspaceIdRef.current) {
          await fetchTasks(selectedWorkspaceIdRef.current);
        }
      }
    } catch (err) {
      console.error('Error during offline sync:', err);
    } finally {
      setIsSyncing(false);
      setQueuedCount(getQueueCount());
    }
  }, [isOnline, isSyncing]);

  useEffect(() => {
    if (isOnline && getQueueCount() > 0) {
      triggerSync();
    }
  }, [isOnline, triggerSync]);

  const fetchHealth = async () => {
    setLoadingHealth(true);
    try {
      const data = await getHealthStatus();
      setHealth(data);
    } catch (err) {
      setHealth({ application: 'UP', database: 'DOWN', redis: 'DOWN' });
    } finally {
      setLoadingHealth(false);
    }
  };

  const fetchWorkspaces = async () => {
    if (!isAuthenticated) return;
    setLoadingWorkspaces(true);
    try {
      const data = await getWorkspaces();
      setWorkspaces(data);
      if (data.length > 0) {
        const currentValid = data.some(w => w.id === Number(selectedWorkspaceId));
        if (!selectedWorkspaceId || !currentValid) {
          setSelectedWorkspaceId(data[0].id);
        }
      } else {
        setSelectedWorkspaceId(null);
      }
    } catch (err) {
      console.error('Failed to load workspaces:', err);
      setErrorMsg('Failed to connect to backend server.');
    } finally {
      setLoadingWorkspaces(false);
    }
  };

  const fetchMembers = async (wsId) => {
    if (!wsId || !isAuthenticated) return;
    try {
      const data = await getWorkspaceMembers(wsId);
      setMembers(data);
      const myMembership = data.find(
        (m) =>
          (user?.id !== undefined && Number(m.user.id) === Number(user.id)) ||
          (user?.email && m.user.email?.toLowerCase() === user.email.toLowerCase())
      );
      if (myMembership) {
        setCurrentUserRole(myMembership.role);
      } else {
        setCurrentUserRole('VIEWER');
      }
    } catch (err) {
      console.error('Failed to fetch workspace members:', err);
    }
  };

  const fetchTasks = async (wsId) => {
    if (!wsId || !isAuthenticated) return;
    setLoadingTasks(true);
    try {
      const data = await getWorkspaceTasks(wsId);
      setTasks(data);
      await fetchMembers(wsId);
      const presence = await getWorkspacePresence(wsId);
      if (presence?.onlineCount !== undefined) {
        setOnlineUsers(presence.onlineCount);
      }
    } catch (err) {
      console.error('Failed to fetch tasks:', err);
      setErrorMsg('Failed to load tasks for selected workspace.');
    } finally {
      setLoadingTasks(false);
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      fetchHealth();
      fetchWorkspaces();
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (isAuthenticated && selectedWorkspaceId) {
      fetchTasks(selectedWorkspaceId);
    } else {
      setTasks([]);
    }
  }, [isAuthenticated, selectedWorkspaceId]);

  const handleCreateWorkspace = async (e) => {
    e.preventDefault();
    if (!workspaceName.trim()) return;

    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      const created = await createWorkspace(workspaceName.trim());
      setSuccessMsg(`Workspace "${created.name}" created! You are the OWNER.`);
      setWorkspaceName('');
      await fetchWorkspaces();
      setSelectedWorkspaceId(created.id);
    } catch (err) {
      console.error('Failed to create workspace:', err);
      setErrorMsg(err.response?.data?.message || 'Failed to create workspace.');
    }
  };

  const handleInviteMember = async (e) => {
    e.preventDefault();
    if (!inviteEmail.trim() || !selectedWorkspaceId) return;

    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      await addWorkspaceMember(selectedWorkspaceId, inviteEmail.trim(), inviteRole);
      setSuccessMsg(`Added ${inviteEmail} as ${inviteRole}!`);
      setInviteEmail('');
      await fetchMembers(selectedWorkspaceId);
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to add workspace member.');
    }
  };

  const handleChangeMemberRole = async (targetUserId, newRole) => {
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      await updateWorkspaceMemberRole(selectedWorkspaceId, targetUserId, newRole);
      setSuccessMsg(`Member role updated to ${newRole}`);
      await fetchMembers(selectedWorkspaceId);
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to update member role.');
    }
  };

  const handleRemoveMember = async (targetUserId) => {
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      await removeWorkspaceMember(selectedWorkspaceId, targetUserId);
      setSuccessMsg('Member removed from workspace.');
      await fetchMembers(selectedWorkspaceId);
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to remove workspace member.');
    }
  };

  const handleCreateTask = async (taskData) => {
    if (!selectedWorkspaceId) return;
    setErrorMsg(null);

    if (!isOnline) {
      enqueueOperation('CREATE', { workspaceId: selectedWorkspaceId, taskData });
      const tempTask = {
        id: 'temp-' + Date.now(),
        workspaceId: selectedWorkspaceId,
        title: taskData.title,
        description: taskData.description,
        status: 'TODO',
        priority: taskData.priority,
        version: 0,
        isPending: true,
        createdAt: new Date().toISOString()
      };
      setTasks(prev => [...prev, tempTask]);
      setSuccessMsg('Saved offline! Will sync automatically when connected.');
      setShowTaskForm(false);
      return;
    }

    try {
      await createTask(selectedWorkspaceId, taskData);
      setSuccessMsg('Task created successfully!');
      setShowTaskForm(false);
      await fetchTasks(selectedWorkspaceId);
    } catch (err) {
      console.error('Failed to create task:', err);
      setErrorMsg(err.response?.data?.message || 'Failed to create task.');
      throw err;
    }
  };

  const handleUpdateTask = async (taskId, updatedData) => {
    setErrorMsg(null);

    if (!isOnline) {
      enqueueOperation('UPDATE', { taskId, taskData: updatedData });
      setTasks(prev => prev.map(t => t.id === taskId ? { ...t, ...updatedData, isPending: true } : t));
      setSuccessMsg('Update queued offline!');
      return;
    }

    try {
      await updateTask(taskId, updatedData);
      await fetchTasks(selectedWorkspaceId);
    } catch (err) {
      if (err.response?.status === 409) {
        const serverTask = err.response?.data?.currentTask || null;
        setConflictData({
          pendingOperation: {
            id: crypto.randomUUID(),
            type: 'UPDATE',
            payload: { taskId, taskData: updatedData }
          },
          currentTask: serverTask,
          message: err.response?.data?.message || 'Conflict detected'
        });
        setShowConflictModal(true);
      } else {
        console.error('Failed to update task:', err);
        setErrorMsg(err.response?.data?.message || 'Failed to update task.');
      }
    }
  };

  const handleDeleteTask = async (taskId) => {
    setErrorMsg(null);

    if (!isOnline) {
      enqueueOperation('DELETE', { taskId });
      setTasks(prev => prev.filter(t => t.id !== taskId));
      setSuccessMsg('Delete queued offline!');
      return;
    }

    try {
      await deleteTask(taskId);
      await fetchTasks(selectedWorkspaceId);
    } catch (err) {
      console.error('Failed to delete task:', err);
      setErrorMsg(err.response?.data?.message || 'Failed to delete task.');
    }
  };

  const handleResolveKeepServer = async () => {
    setShowConflictModal(false);
    setConflictData(null);
    setSuccessMsg('Resolved: Kept server version.');
    if (selectedWorkspaceId) {
      await fetchTasks(selectedWorkspaceId);
    }
    triggerSync();
  };

  const handleResolveOverwrite = async (newServerVersion) => {
    if (!conflictData) return;
    const { pendingOperation } = conflictData;
    setShowConflictModal(false);
    setConflictData(null);

    try {
      const taskData = {
        ...pendingOperation.payload.taskData,
        version: newServerVersion
      };
      await updateTask(pendingOperation.payload.taskId, taskData, pendingOperation.operationId);
      setSuccessMsg('Resolved: Overwrote with your changes!');
      if (selectedWorkspaceId) {
        await fetchTasks(selectedWorkspaceId);
      }
    } catch (err) {
      console.error('Failed to apply overwrite resolution:', err);
      setErrorMsg(err.response?.data?.message || 'Failed to overwrite task version.');
    } finally {
      triggerSync();
    }
  };

  const handleLogout = () => {
    disconnectWebSocket();
    setSelectedWorkspaceId(null);
    setWorkspaces([]);
    setTasks([]);
    setMembers([]);
    setErrorMsg(null);
    setSuccessMsg(null);
    logout();
  };

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  const activeWorkspace = workspaces.find((w) => w.id === Number(selectedWorkspaceId));
  const wsConnected = wsStatus === 'CONNECTED';
  const isViewer = currentUserRole === 'VIEWER';
  const isOwner = currentUserRole === 'OWNER';

  return (
    <div className="app-container">
      <ConflictModal
        isOpen={showConflictModal}
        conflictData={conflictData}
        onResolveKeepServer={handleResolveKeepServer}
        onResolveOverwrite={handleResolveOverwrite}
      />

      <header className="app-header">
        <div className="header-brand">
          <span className="brand-logo">⚡</span>
          <div>
            <h1>SyncBoard</h1>
            <p className="subtitle">Real-Time Distributed Workspace</p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {/* User Profile Badge & Logout */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '0.25rem 0.75rem', backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '8px' }}>
            <span style={{ fontSize: '0.85rem', fontWeight: 700, color: '#f8fafc' }}>
              👤 {user?.name || user?.email}
            </span>
            <span style={{ fontSize: '0.7rem', padding: '0.1rem 0.4rem', borderRadius: '4px', backgroundColor: currentUserRole === 'OWNER' ? '#e11d48' : currentUserRole === 'EDITOR' ? '#3b82f6' : '#64748b', color: '#fff', fontWeight: 800 }}>
              {currentUserRole}
            </span>
            <button
              onClick={handleLogout}
              title="Sign Out"
              style={{ background: 'none', border: 'none', color: '#ef4444', fontWeight: 700, cursor: 'pointer', marginLeft: '4px', fontSize: '0.85rem' }}
            >
              Sign Out 🚪
            </button>
          </div>

          <div
            title="Active workspace presence"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.75rem',
              color: '#38bdf8',
              fontWeight: 700,
              background: 'rgba(56, 189, 248, 0.15)',
              border: '1px solid rgba(56, 189, 248, 0.3)',
              padding: '0.25rem 0.65rem',
              borderRadius: '8px',
            }}
          >
            👥 Online: {onlineUsers}
          </div>

          <div
            title={`WebSocket: ${wsStatus}`}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.75rem',
              color: wsConnected ? '#4ade80' : '#f87171',
              fontWeight: 600,
            }}
          >
            <span
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                background: wsConnected ? '#4ade80' : '#f87171',
                boxShadow: wsConnected ? '0 0 6px #4ade80' : 'none',
                display: 'inline-block',
              }}
            />
            WS {wsStatus}
          </div>
        </div>
      </header>

      {/* Health & Status Card */}
      <section className="card health-card">
        <div className="card-header">
          <h2>System Operations & Health</h2>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => setShowActivity(!showActivity)}
            >
              {showActivity ? 'Hide Audit Log' : '📜 Audit Activity Log'}
            </button>
            {queuedCount > 0 && (
              <button
                className="btn btn-primary btn-sm"
                onClick={triggerSync}
                disabled={!isOnline || isSyncing}
              >
                {isSyncing ? 'Syncing...' : `⚡ Sync Now (${queuedCount})`}
              </button>
            )}
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => {
                fetchHealth();
                fetchWorkspaces();
                if (selectedWorkspaceId) fetchTasks(selectedWorkspaceId);
              }}
              disabled={loadingHealth}
            >
              {loadingHealth ? 'Refreshing...' : '🔄 Check Status'}
            </button>
          </div>
        </div>
        <div className="health-grid">
          <div className="health-item">
            <span className="label">Database Status</span>
            <div className="status-value">
              <span className={`badge ${health.database === 'UP' ? 'badge-success' : 'badge-danger'}`}>
                DB: {health.database}
              </span>
            </div>
          </div>
          <div className="health-item">
            <span className="label">Redis Pub/Sub</span>
            <div className="status-value">
              <span className={`badge ${health.redis === 'UP' ? 'badge-success' : 'badge-danger'}`}>
                REDIS: {health.redis}
              </span>
            </div>
          </div>
          <div className="health-item">
            <span className="label">Network & Queue</span>
            <div className="status-value">
              <span className={`badge ${isOnline ? 'badge-success' : 'badge-warning'}`}>
                {isOnline ? 'ONLINE' : 'OFFLINE MODE'}
              </span>
              {queuedCount > 0 && <span className="badge badge-warning">{queuedCount} Pending Ops</span>}
            </div>
          </div>
          <div className="health-item">
            <span className="label">Your Workspace Role</span>
            <div className="status-value">
              <span className="badge badge-success" style={{ background: currentUserRole === 'OWNER' ? '#e11d48' : currentUserRole === 'EDITOR' ? '#3b82f6' : '#64748b', color: '#fff' }}>
                ROLE: {currentUserRole}
              </span>
            </div>
          </div>
        </div>

        {showActivity && selectedWorkspaceId && (
          <ActivityPanel workspaceId={selectedWorkspaceId} />
        )}
      </section>

      {!isOnline && (
        <div className="alert alert-warning">
          ⚡ <strong>Offline Mode Active:</strong> Changes are saved locally to your browser queue and will automatically synchronize when connection is restored.
        </div>
      )}

      {errorMsg && <div className="alert alert-error">{errorMsg}</div>}
      {successMsg && <div className="alert alert-success">{successMsg}</div>}

      {/* Workspace Selector Bar */}
      <section className="card workspace-selector-card">
        <div className="selector-header">
          <div className="workspace-tabs-container">
            <label htmlFor="workspaceSelect">Active Workspace:</label>
            <select
              id="workspaceSelect"
              value={selectedWorkspaceId || ''}
              onChange={(e) => setSelectedWorkspaceId(Number(e.target.value))}
              disabled={loadingWorkspaces || workspaces.length === 0}
            >
              {workspaces.length === 0 && <option value="">No Workspaces Available</option>}
              {workspaces.map((ws) => (
                <option key={ws.id} value={ws.id}>
                  {ws.name} (#{ws.id})
                </option>
              ))}
            </select>
          </div>

          <form onSubmit={handleCreateWorkspace} className="quick-workspace-form">
            <input
              type="text"
              placeholder="+ New Workspace Name"
              value={workspaceName}
              onChange={(e) => setWorkspaceName(e.target.value)}
              required
            />
            <button type="submit" className="btn btn-secondary btn-sm" disabled={!workspaceName.trim()}>
              Create
            </button>
          </form>

          {selectedWorkspaceId && (
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => setShowMemberModal(!showMemberModal)}
              style={{ marginLeft: 'auto' }}
            >
              👥 Members ({members.length})
            </button>
          )}
        </div>

        {/* Member Management & Invitation Panel */}
        {showMemberModal && selectedWorkspaceId && (
          <div style={{ marginTop: '1rem', padding: '1.25rem', backgroundColor: '#0f172a', borderRadius: '10px', border: '1px solid #334155' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3 style={{ margin: 0, color: '#38bdf8', fontSize: '1rem' }}>Workspace Members & RBAC Roles</h3>
              <button className="btn btn-secondary btn-sm" onClick={() => setShowMemberModal(false)}>Close</button>
            </div>

            {/* Invite Form (Owner Only) */}
            {isOwner ? (
              <form onSubmit={handleInviteMember} style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.25rem' }}>
                <input
                  type="email"
                  required
                  placeholder="Invite user by email (e.g. editor@syncboard.dev)"
                  value={inviteEmail}
                  onChange={(e) => setInviteEmail(e.target.value)}
                  style={{ flex: 1, padding: '0.6rem 0.8rem', borderRadius: '6px', border: '1px solid #334155', backgroundColor: '#1e293b', color: '#fff', fontSize: '0.9rem' }}
                />
                <select
                  value={inviteRole}
                  onChange={(e) => setInviteRole(e.target.value)}
                  style={{ padding: '0.6rem', borderRadius: '6px', border: '1px solid #334155', backgroundColor: '#1e293b', color: '#fff', fontSize: '0.9rem' }}
                >
                  <option value="EDITOR">EDITOR (Read & Write Tasks)</option>
                  <option value="VIEWER">VIEWER (Read Only)</option>
                  <option value="OWNER">OWNER (Full Admin)</option>
                </select>
                <button type="submit" className="btn btn-primary btn-sm">➕ Add Member</button>
              </form>
            ) : (
              <p style={{ fontSize: '0.85rem', color: '#94a3b8', fontStyle: 'italic', marginBottom: '1rem' }}>
                Only workspace <strong>OWNER</strong> can invite members or change roles.
              </p>
            )}

            {/* Members List Table */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              {members.map((m) => (
                <div key={m.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0.6rem 0.8rem', backgroundColor: '#1e293b', borderRadius: '6px', border: '1px solid #334155' }}>
                  <div>
                    <span style={{ fontWeight: 700, color: '#f8fafc', fontSize: '0.9rem' }}>{m.user.name}</span>
                    <span style={{ marginLeft: '0.5rem', color: '#94a3b8', fontSize: '0.8rem' }}>({m.user.email})</span>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    {isOwner && Number(m.user.id) !== Number(user?.id) ? (
                      <>
                        <select
                          value={m.role}
                          onChange={(e) => handleChangeMemberRole(m.user.id, e.target.value)}
                          style={{ padding: '0.3rem 0.5rem', borderRadius: '4px', border: '1px solid #334155', backgroundColor: '#0f172a', color: '#38bdf8', fontWeight: 700, fontSize: '0.8rem' }}
                        >
                          <option value="OWNER">OWNER</option>
                          <option value="EDITOR">EDITOR</option>
                          <option value="VIEWER">VIEWER</option>
                        </select>
                        <button
                          className="btn-icon text-danger"
                          title="Remove Member"
                          onClick={() => handleRemoveMember(m.user.id)}
                          style={{ cursor: 'pointer', background: 'none', border: 'none' }}
                        >
                          🗑️
                        </button>
                      </>
                    ) : (
                      <span style={{ padding: '0.2rem 0.5rem', borderRadius: '4px', backgroundColor: m.role === 'OWNER' ? '#e11d48' : m.role === 'EDITOR' ? '#3b82f6' : '#64748b', color: '#fff', fontWeight: 800, fontSize: '0.75rem' }}>
                        {m.role}
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </section>

      {/* Task Board Section */}
      {selectedWorkspaceId ? (
        <section className="workspace-content">
          <div className="board-header-bar">
            <div>
              <h2>{activeWorkspace ? activeWorkspace.name : 'Workspace Board'}</h2>
              <span className="task-count">
                {tasks.length} Total Tasks • 👥 {onlineUsers} User(s) Online • Member Role: <strong>{currentUserRole}</strong>
              </span>
            </div>

            {!isViewer && (
              <button className="btn btn-primary" onClick={() => setShowTaskForm(!showTaskForm)}>
                {showTaskForm ? 'Close Form' : '➕ Add Task'}
              </button>
            )}
          </div>

          {showTaskForm && !isViewer && (
            <CreateTaskForm
              onTaskCreated={handleCreateTask}
              onCancel={() => setShowTaskForm(false)}
            />
          )}

          {loadingTasks ? (
            <div className="state-placeholder">Loading tasks...</div>
          ) : (
            <TaskBoard
              tasks={tasks}
              onUpdateTask={handleUpdateTask}
              onDeleteTask={handleDeleteTask}
              userRole={currentUserRole}
            />
          )}
        </section>
      ) : (
        <div className="card empty-state">
          <p>No workspace selected.</p>
          <span className="hint">Create or select a workspace to manage tasks.</span>
        </div>
      )}

      <footer className="app-footer">
        <p>SyncBoard Architecture: JWT Auth • RBAC • Redis Pub/Sub • Ephemeral Presence • PostgreSQL • Audit Trails</p>
      </footer>
    </div>
  );
}
