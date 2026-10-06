import React, { useState } from 'react';

const STATUS_COLUMNS = [
  { key: 'TODO', label: '📋 TODO', badgeClass: 'column-todo' },
  { key: 'IN_PROGRESS', label: '⚡ IN PROGRESS', badgeClass: 'column-progress' },
  { key: 'DONE', label: '✅ DONE', badgeClass: 'column-done' },
];

export default function TaskBoard({ tasks, onUpdateTask, onDeleteTask, userRole }) {
  const [editingTaskId, setEditingTaskId] = useState(null);
  const [editTitle, setEditTitle] = useState('');
  const [editDesc, setEditDesc] = useState('');
  const [editPriority, setEditPriority] = useState('MEDIUM');
  const [editStatus, setEditStatus] = useState('TODO');
  const [editVersion, setEditVersion] = useState(null);

  const isReadOnly = userRole === 'VIEWER';

  const startEdit = (task) => {
    if (isReadOnly) return;
    setEditingTaskId(task.id);
    setEditTitle(task.title);
    setEditDesc(task.description || '');
    setEditPriority(task.priority);
    setEditStatus(task.status);
    setEditVersion(task.version);
  };

  const cancelEdit = () => {
    setEditingTaskId(null);
    setEditVersion(null);
  };

  const handleSaveEdit = async (task) => {
    if (!editTitle.trim() || isReadOnly) return;
    await onUpdateTask(task.id, {
      title: editTitle.trim(),
      description: editDesc.trim(),
      status: editStatus,
      priority: editPriority,
      version: editVersion !== undefined && editVersion !== null ? editVersion : task.version
    });
    setEditingTaskId(null);
    setEditVersion(null);
  };

  const handleQuickStatusMove = async (task, newStatus) => {
    if (isReadOnly) return;
    await onUpdateTask(task.id, {
      title: task.title,
      description: task.description,
      status: newStatus,
      priority: task.priority,
      version: task.version
    });
  };

  const getPriorityBadge = (priority) => {
    switch (priority) {
      case 'HIGH':
        return <span className="priority-badge priority-high">HIGH</span>;
      case 'MEDIUM':
        return <span className="priority-badge priority-medium">MEDIUM</span>;
      case 'LOW':
        return <span className="priority-badge priority-low">LOW</span>;
      default:
        return null;
    }
  };

  return (
    <div className="kanban-board">
      {STATUS_COLUMNS.map((col) => {
        const columnTasks = tasks.filter((t) => t.status === col.key);

        return (
          <div key={col.key} className={`kanban-column ${col.badgeClass}`}>
            <div className="column-header">
              <h3>{col.label}</h3>
              <span className="count-badge">{columnTasks.length}</span>
            </div>

            <div className="column-cards">
              {columnTasks.length === 0 ? (
                <div className="empty-column-state">No tasks</div>
              ) : (
                columnTasks.map((task) => {
                  const isEditing = editingTaskId === task.id;

                  if (isEditing) {
                    return (
                      <div key={task.id} className="task-card task-card-editing">
                        <input
                          type="text"
                          className="edit-input"
                          value={editTitle}
                          onChange={(e) => setEditTitle(e.target.value)}
                          placeholder="Task title"
                        />
                        <textarea
                          className="edit-input"
                          rows="2"
                          value={editDesc}
                          onChange={(e) => setEditDesc(e.target.value)}
                          placeholder="Description"
                        />
                        <div className="edit-selectors">
                          <select value={editPriority} onChange={(e) => setEditPriority(e.target.value)}>
                            <option value="LOW">Low Priority</option>
                            <option value="MEDIUM">Medium Priority</option>
                            <option value="HIGH">High Priority</option>
                          </select>
                          <select value={editStatus} onChange={(e) => setEditStatus(e.target.value)}>
                            <option value="TODO">TODO</option>
                            <option value="IN_PROGRESS">IN PROGRESS</option>
                            <option value="DONE">DONE</option>
                          </select>
                        </div>
                        <div className="edit-actions">
                          <button className="btn btn-primary btn-sm" onClick={() => handleSaveEdit(task)}>
                            Save
                          </button>
                          <button className="btn btn-secondary btn-sm" onClick={cancelEdit}>
                            Cancel
                          </button>
                        </div>
                      </div>
                    );
                  }

                  return (
                    <div key={task.id} className={`task-card ${task.isPending ? 'task-pending' : ''}`}>
                      <div className="task-card-header">
                        <span className="task-title">
                          {task.title}
                          {task.version !== undefined && (
                            <span className="version-tag" title={`Version ${task.version}`}>
                              v{task.version}
                            </span>
                          )}
                          {task.isPending && (
                            <span className="pending-badge" title="Pending offline sync">
                              ⏳ Pending Sync
                            </span>
                          )}
                        </span>
                        {getPriorityBadge(task.priority)}
                      </div>

                      {task.description && <p className="task-description">{task.description}</p>}

                      <div className="task-card-footer">
                        {!isReadOnly && (
                          <div className="move-buttons">
                            {col.key !== 'TODO' && (
                              <button
                                className="btn-text"
                                title="Move to TODO"
                                onClick={() => handleQuickStatusMove(task, 'TODO')}
                              >
                                ← TODO
                              </button>
                            )}
                            {col.key !== 'IN_PROGRESS' && (
                              <button
                                className="btn-text"
                                title="Move to IN PROGRESS"
                                onClick={() => handleQuickStatusMove(task, 'IN_PROGRESS')}
                              >
                                {col.key === 'TODO' ? 'IN PROGRESS →' : '← IN PROGRESS'}
                              </button>
                            )}
                            {col.key !== 'DONE' && (
                              <button
                                className="btn-text"
                                title="Move to DONE"
                                onClick={() => handleQuickStatusMove(task, 'DONE')}
                              >
                                DONE →
                              </button>
                            )}
                          </div>
                        )}

                        {!isReadOnly ? (
                          <div className="card-controls">
                            <button className="btn-icon" title="Edit" onClick={() => startEdit(task)}>
                              ✏️
                            </button>
                            <button className="btn-icon text-danger" title="Delete" onClick={() => onDeleteTask(task.id)}>
                              🗑️
                            </button>
                          </div>
                        ) : (
                          <span style={{ fontSize: '0.75rem', color: '#64748b', fontStyle: 'italic' }}>
                            Read Only
                          </span>
                        )}
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
