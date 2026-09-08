# Nivya — REST API & WebSocket Protocol Reference

All API requests must communicate over HTTPS. Protected endpoints require a bearer JWT in the HTTP Authorization header:
```http
Authorization: Bearer <access_token>
```

---

## 1. Authentication Endpoints (`/api/v1/auth`)

| Method | Path | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Public | Register new Parent or Child account. |
| `POST` | `/api/v1/auth/login` | Public | Authenticate with email/password; returns JWT access + refresh tokens and roles. |
| `POST` | `/api/v1/auth/verify` | Authenticated | Validate token integrity and retrieve current user context. |
| `POST` | `/api/v1/auth/refresh` | Public | Exchange valid refresh token for a fresh access token (with rotation). |
| `POST` | `/api/v1/auth/logout` | Authenticated | Revoke refresh token and invalidate device session. |
| `POST` | `/api/v1/auth/change-password` | Authenticated | Change user password; revokes all existing refresh tokens. |
| `POST` | `/api/v1/auth/forgot-password` | Public | Trigger secure password reset link. |

---

## 2. Pairing & Connection Protocol (`/api/v1/pairing`)

| Method | Path | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/pairing/create` | Authenticated | Generate a one-time 12-character pairing code (*e.g., `NV-4821-KP90`*), valid for 10 minutes. |
| `POST` | `/api/v1/pairing/connect` | Authenticated | Submit opposite device's code; verifies compatibility and establishes permanent family link. |
| `GET` | `/api/v1/pairing/status` | Authenticated | Check current pairing state (Pending, Connected, Unlinked). |
| `POST` | `/api/v1/pairing/revoke` | Authenticated (Parent) | Revoke family connection and unlink child device. |

---

## 3. Device Registration & Telemetry (`/api/v1/devices`)

| Method | Path | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/devices/register` | Authenticated | Register device hardware fingerprint, OS version, and FCM push token. |
| `GET` | `/api/v1/devices/{deviceId}` | Authenticated | Fetch device details (authorized family scope only). |
| `POST` | `/api/v1/devices/{deviceId}/heartbeat` | Authenticated (Device) | Periodic keep-alive heartbeat ping updating online status. |
| `POST` | `/api/v1/devices/{deviceId}/telemetry` | Authenticated (Device) | Ingest batch telemetry (battery level, charging state, network type, signal dBm). |
| `GET` | `/api/v1/devices/{deviceId}/status` | Authenticated | Get instant device status snapshot for dashboard display. |

---

## 4. Location, Usage & Activity (`/api/v1/...`)

| Method | Path | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/location/update` | Authenticated (Child) | Post current GPS coordinates with accuracy & timestamp. |
| `GET` | `/api/v1/location/current/{deviceId}` | Authenticated | Parent gets exact/approx location; Child gets simple status (*"Near Home"*). |
| `GET` | `/api/v1/location/history/{deviceId}` | Authenticated (Parent) | Fetch paginated 30-day location history coordinates. |
| `POST` | `/api/v1/usage/sync` | Authenticated (Child) | Sync Android UsageStats package time intervals. |
| `GET` | `/api/v1/usage/today/{deviceId}` | Authenticated | Fetch today's screen time breakdown. |
| `GET` | `/api/v1/usage/week/{deviceId}` | Authenticated (Parent) | Fetch 7-day weekly usage trend and application rankings. |
| `GET` | `/api/v1/activity/live/{deviceId}` | Authenticated (Parent) | Fetch current live broad activity (*e.g., "Active in Chrome"*). |
| `GET` | `/api/v1/history/{deviceId}` | Authenticated (Parent) | Fetch chronological timeline of device events and durations. |

---

## 5. Convocation API (`/api/v1/convocation`)

*Isolated module: Completely segregated from telemetry and health APIs.*

| Method | Path | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/convocation/messages` | Authenticated | **Parent**: Retained history. **Child**: Returns unread set ONLY if 2-minute mode is active on server. |
| `POST` | `/api/v1/convocation/messages` | Authenticated | Send message. If sent by Parent, triggers Child push `"Check your battery status"`. |
| `POST` | `/api/v1/convocation/view` | Authenticated (Child) | Marks unread message set as Seen on Parent side. |
| `POST` | `/api/v1/convocation/toggle` | Authenticated (Child) | Activate or deactivate the 2-minute viewing mode. Records `view_started_at`. |
| `POST` | `/api/v1/convocation/clear` | Authenticated (Child) | Immediately resets Child-side visible notepad to empty. |

---

## 6. Real-Time WebSocket Protocol (STOMP)

- **Endpoint**: `/ws` (with WSS support)
- **Authentication**: JWT token sent during STOMP CONNECT frame via `Authorization: Bearer <token>`.
- **Subscriptions**:
  - `/topic/family.{familyId}`: Family-wide real-time events (Telemetry updates, online/offline, alerts).
  - `/user/queue/convocation`: Private user notifications and message alerts.

### Event Catalog
- `DEVICE_ONLINE` / `DEVICE_OFFLINE`
- `BATTERY_UPDATED`
- `NETWORK_UPDATED`
- `NETWORK_QUALITY_UPDATED`
- `LOCATION_UPDATED`
- `USAGE_SYNCED`
- `ACTIVITY_UPDATED`
- `ALERT_CREATED`
- `PAIRING_CONNECTED`
- `CONVOCATION_MESSAGE_AVAILABLE`
- `CONVOCATION_VIEWED` / `SEEN_UPDATED`
