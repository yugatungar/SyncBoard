# SyncBoard — Production-Oriented Distributed Real-Time Workspace

**SyncBoard** is a production-engineered, offline-first, distributed real-time task management platform built with React, Spring Boot, PostgreSQL, and Redis.

---

## 🏗️ System Architecture

```text
React (Vite)
 ├── Authentication UI (JWT Storage)
 ├── Local Offline Queue & Idempotency Engine
 └── STOMP WebSocket Client
       ↓ (REST / WS)
Spring Security / REST Controllers (Port 8080 / 8081)
 ├── JwtAuthenticationFilter & Authorization (RBAC)
 ├── RateLimitService (Redis sliding window)
 ├── TaskService & WorkspaceService
 ├── JPA Entities & Transactional Services
 ├── AuditService (Audit Logs)
 └── WebSocket Message Broker
       ↓                                ↓
PostgreSQL (Port 5432)        Redis (Port 6379)
 ├── Users & Password Hashes     ├── Pub/Sub (Cross-Instance WS Sync)
 ├── Workspace Membership & RBAC ├── Ephemeral Presence (TTL Keys)
 ├── Tasks (Optimistic Version)  └── Rate Limiting Counters
 ├── Idempotent Operations
 └── Audit Log History
```

---

## 🔐 1. Authentication Flow

```text
User Register / Login
      ↓
Spring Security + BCrypt Password Validation
      ↓
JWT Token Issued (HMAC-SHA256, 24h Expiry)
      ↓
Stored in Client (localStorage)
      ↓
Attached to HTTP Requests via 'Authorization: Bearer <token>'
      ↓
JwtAuthenticationFilter verifies signature & sets SecurityContext
```

---

## ⚡ 2. Distributed WebSocket Flow (Multi-Instance Sync)

```text
Client A (Server 1:8080)             Client B (Server 2:8081)
     │                                    ▲
     │ 1. POST Task Mutation              │ 6. WS STOMP Broadcast
     ▼                                    │
 Server 1 ──────► 2. PostgreSQL DB        Server 2
     │               (Save & Flush)          ▲
     │                                       │
     └──────────► 3. Redis Pub/Sub ──────────┘
                  `syncboard:workspace-events`
```

---

## 📶 3. Offline Synchronization (Phase 3+4)

- **Offline Detection**: React `useNetworkStatus` hook detects loss of internet or backend availability.
- **Local Operation Queue**: Task mutations (`CREATE`, `UPDATE`, `DELETE`) are captured locally with client-generated `operationId` UUIDs and displayed with pending visual indicators.
- **Automatic Reconnection Flush**: When connection is restored, the queue is drained sequentially using REST API calls with the original `operationId`.

---

## 🛡️ 4. Concurrency Control (Optimistic Locking)

- **Entity Versioning**: Every `Task` entity contains a `@Version` field incremented automatically by JPA on write operations.
- **Client Echo**: Clients echo back `version` during `PUT /api/tasks/{id}`.
- **Stale Update Detection**: If the version supplied by the client does not match the current database version, a `409 CONFLICT` response is returned containing the server's current task state.
- **Conflict Modal**: The React UI presents a Conflict Resolution Modal allowing the user to either **"Keep Server Version"** or **"Overwrite with My Changes"** (which re-fetches the latest version and retries).

---

## 🔄 5. Idempotency

- Every mutation request includes a unique `operationId` UUID.
- Successfully processed operations are recorded in the `processed_operation` table with a `UNIQUE(operation_id)` constraint.
- If a retried request with the same `operationId` arrives, the backend returns `200 OK` with `{ "duplicate": true }` instead of executing duplicate mutations.

---

## 👥 6. Role-Based Access Control (RBAC)

Each workspace member is assigned one of three granular roles:

| Role | Workspace View | Task Mutation | Member Management | Workspace Deletion |
|------|----------------|---------------|-------------------|--------------------|
| **OWNER** | ✅ | ✅ | ✅ (Add/Remove/Role) | ✅ |
| **EDITOR** | ✅ | ✅ | ❌ | ❌ |
| **VIEWER** | ✅ | ❌ (Read Only) | ❌ | ❌ |

*Authorization is strictly enforced on the Spring Boot backend service layer; UI controls are dynamically toggled as a UX convenience.*

---

## 🚦 7. Rate Limiting

- Implemented using Redis sliding-window counters:
  - **Auth Requests (Login/Register)**: 100 requests per minute per IP.
  - **Task Mutations**: 30 requests per minute per authenticated user.
- Exceeding the rate limit returns `429 TOO MANY REQUESTS` with a clean JSON error response.
- **Graceful Fallback**: If Redis is temporarily unreachable, rate limiting fails open gracefully without interrupting core REST API processing.

---

## 📜 8. Audit History

Important workspace actions record durable audit events in `audit_events`:
- `TASK_CREATED`, `TASK_UPDATED`, `TASK_DELETED`
- `MEMBER_ADDED`, `MEMBER_REMOVED`, `ROLE_CHANGED`
- `WORKSPACE_CREATED`

The UI Activity Panel polls `/api/workspaces/{id}/audit` to display real-time activity trails (*Who did what, to which entity, and when*).

---

## ⚡ 9. Database Indexing

Targeted B-tree database indexes added for high-traffic query paths:
- `workspace_members(workspace_id)` & `workspace_members(user_id)`
- `tasks(workspace_id)`
- `audit_events(workspace_id, created_at DESC)`
- `processed_operation(operation_id)`
- `users(email)` (Unique)

---

## 💥 10. Failure Scenarios & Graceful Recovery

| Component Failure | System Behavior & Mitigation |
|-------------------|------------------------------|
| **PostgreSQL Down** | REST mutations fail gracefully with `500 Internal Server Error`; Health check reports DB: DOWN. |
| **Redis Down** | Fallback to local Spring SimpMessagingBroker for WebSocket events; presence displays fallback notice; rate limiting fails open safely. |
| **WebSocket Disconnect** | Client automatically attempts reconnect every 3s; presence TTL key expires in Redis after 10s. |
| **Client Disconnect** | Operations buffer into offline queue; auto-sync fires upon network reconnection. |
| **Stale Task Update** | `409 CONFLICT` returned with current server task state; Conflict Resolution Modal triggered in UI. |
| **Duplicate Operation** | Handled idempotently via `processed_operation` unique constraint; returns `200 OK { duplicate: true }`. |
| **Expired JWT Token** | Client receives `401 Unauthorized`; auth context automatically logs out user and redirects to Login screen. |

---

## 🧪 Demo User Accounts

Seed data initialized on startup:
- **Owner**: `owner@syncboard.dev` / `password123`
- **Editor**: `editor@syncboard.dev` / `password123`
- **Viewer**: `viewer@syncboard.dev` / `password123`

---

## 🐳 Docker Deployment

To launch the full production stack locally:

```bash
docker-compose up --build -d
```

Services started:
- **PostgreSQL**: `localhost:5432`
- **Redis**: `localhost:6379`
- **Spring Boot Backend**: `http://localhost:8080`
- **Nginx React Frontend**: `http://localhost:80`
