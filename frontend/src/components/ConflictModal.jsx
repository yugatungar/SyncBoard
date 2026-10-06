import React from 'react';

/**
 * Modal component displayed when a 409 Conflict is received from the server.
 * Allows the user to compare their pending change with the current server state,
 * and choose to keep the server version or force overwrite with their changes.
 */
export const ConflictModal = ({ isOpen, conflictData, onResolveKeepServer, onResolveOverwrite }) => {
  if (!isOpen || !conflictData) return null;

  const { pendingOperation, currentTask } = conflictData;
  const clientPayload = pendingOperation?.payload?.taskData || {};

  return (
    <div className="modal-overlay">
      <div className="modal-content conflict-modal">
        <div className="conflict-header">
          <div className="conflict-icon">⚠️</div>
          <div>
            <h2>Sync Conflict Detected</h2>
            <p className="conflict-subtitle">
              This task was modified on the server while you were editing or offline.
            </p>
          </div>
        </div>

        <div className="conflict-comparison">
          {/* Your Local Version */}
          <div className="conflict-box local-box">
            <h3>Your Pending Version</h3>
            <div className="conflict-field">
              <span className="field-label">Title:</span>
              <span className="field-value">{clientPayload.title || '(No change)'}</span>
            </div>
            <div className="conflict-field">
              <span className="field-label">Description:</span>
              <span className="field-value">{clientPayload.description || '(Empty)'}</span>
            </div>
            <div className="conflict-field">
              <span className="field-label">Status:</span>
              <span className={`badge status-${clientPayload.status}`}>
                {clientPayload.status}
              </span>
            </div>
            <div className="conflict-field">
              <span className="field-label">Priority:</span>
              <span className={`badge priority-${clientPayload.priority}`}>
                {clientPayload.priority}
              </span>
            </div>
          </div>

          {/* Server Version */}
          <div className="conflict-box server-box">
            <h3>Current Server Version (v{currentTask?.version ?? '?'})</h3>
            <div className="conflict-field">
              <span className="field-label">Title:</span>
              <span className="field-value">{currentTask?.title || '(Deleted)'}</span>
            </div>
            <div className="conflict-field">
              <span className="field-label">Description:</span>
              <span className="field-value">{currentTask?.description || '(Empty)'}</span>
            </div>
            <div className="conflict-field">
              <span className="field-label">Status:</span>
              <span className={`badge status-${currentTask?.status}`}>
                {currentTask?.status}
              </span>
            </div>
            <div className="conflict-field">
              <span className="field-label">Priority:</span>
              <span className={`badge priority-${currentTask?.priority}`}>
                {currentTask?.priority}
              </span>
            </div>
          </div>
        </div>

        <div className="conflict-actions">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onResolveKeepServer}
          >
            Keep Server Version
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => onResolveOverwrite(currentTask?.version)}
          >
            Overwrite with My Changes
          </button>
        </div>
      </div>
    </div>
  );
};
