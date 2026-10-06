import React, { useEffect, useState } from 'react';
import { apiClient } from '../api/apiClient';

export const ActivityPanel = ({ workspaceId }) => {
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchAuditEvents = async () => {
    if (!workspaceId) return;
    setLoading(true);
    try {
      const response = await apiClient.get(`/api/workspaces/${workspaceId}/audit?limit=25`);
      setActivities(response.data);
    } catch (err) {
      console.error('Failed to fetch audit events:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAuditEvents();
    const interval = setInterval(fetchAuditEvents, 15000); // refresh every 15s
    return () => clearInterval(interval);
  }, [workspaceId]);

  const formatTime = (isoString) => {
    try {
      const date = new Date(isoString);
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return '';
    }
  };

  const getActionBadgeColor = (action) => {
    switch (action) {
      case 'TASK_CREATED': return '#10b981';
      case 'TASK_UPDATED': return '#3b82f6';
      case 'TASK_DELETED': return '#ef4444';
      case 'MEMBER_ADDED': return '#8b5cf6';
      case 'MEMBER_REMOVED': return '#f59e0b';
      case 'ROLE_CHANGED': return '#ec4899';
      default: return '#6b7280';
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h3 style={styles.title}>📜 Workspace Activity Audit Log</h3>
        <button type="button" onClick={fetchAuditEvents} style={styles.refreshBtn}>
          {loading ? '...' : '🔄 Refresh'}
        </button>
      </div>

      {activities.length === 0 ? (
        <p style={styles.empty}>No audit events recorded yet.</p>
      ) : (
        <ul style={styles.list}>
          {activities.map((item) => (
            <li key={item.id} style={styles.item}>
              <div style={styles.itemHeader}>
                <span style={styles.userName}>{item.userName || `User #${item.userId}`}</span>
                <span
                  style={{
                    ...styles.actionBadge,
                    backgroundColor: getActionBadgeColor(item.action),
                  }}
                >
                  {item.action}
                </span>
                <span style={styles.time}>{formatTime(item.createdAt)}</span>
              </div>
              <p style={styles.details}>{item.details || `${item.action} on ${item.entityType}`}</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

const styles = {
  container: {
    backgroundColor: '#1e293b',
    border: '1px solid #334155',
    borderRadius: '12px',
    padding: '1.25rem',
    marginTop: '1.5rem',
    color: '#f8fafc',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '1rem',
  },
  title: {
    margin: 0,
    fontSize: '1rem',
    fontWeight: '700',
    color: '#38bdf8',
  },
  refreshBtn: {
    padding: '0.25rem 0.6rem',
    backgroundColor: '#334155',
    color: '#cbd5e1',
    border: 'none',
    borderRadius: '6px',
    fontSize: '0.75rem',
    cursor: 'pointer',
  },
  empty: {
    color: '#94a3b8',
    fontSize: '0.875rem',
    fontStyle: 'italic',
    textAlign: 'center',
    margin: '1rem 0',
  },
  list: {
    listStyle: 'none',
    padding: 0,
    margin: 0,
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
    maxHeight: '260px',
    overflowY: 'auto',
  },
  item: {
    backgroundColor: '#0f172a',
    borderRadius: '8px',
    padding: '0.75rem',
    border: '1px solid #1e293b',
  },
  itemHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    marginBottom: '0.25rem',
  },
  userName: {
    fontWeight: '600',
    fontSize: '0.85rem',
    color: '#f1f5f9',
  },
  actionBadge: {
    padding: '0.15rem 0.4rem',
    borderRadius: '4px',
    fontSize: '0.65rem',
    fontWeight: '700',
    color: '#ffffff',
    textTransform: 'uppercase',
  },
  time: {
    marginLeft: 'auto',
    fontSize: '0.75rem',
    color: '#64748b',
  },
  details: {
    margin: 0,
    fontSize: '0.8rem',
    color: '#94a3b8',
  },
};
