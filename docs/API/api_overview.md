# Nivya — REST API & WebSocket Protocol Reference

All API requests communicate over HTTPS (or HTTP during local dev). Protected endpoints require a Bearer JWT in the HTTP Authorization header:
```http
Authorization: Bearer <access_token>
```

---

## 1. System, Remote Configuration & Health (`/api/v1`)

| Method | Path | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/app/config` | Public | Dynamic feature flags, minimum client versions, and remote operational parameters. |
| `GET` | `/api/v1/config` | Public | Alias for `/api/v1/app/config`. |
| `GET` | `/api/v1/health` | Public | System liveness probe, version, and server uptime. |
| `GET` | `/api/v1/ping` | Public | Fast latency check returning `"pong"`. |
| `GET` | `/actuator/health` | Public | Spring Boot detailed subsystem health (MySQL, Redis, disk). |
| `GET` | `/actuator/prometheus` | Public | Prometheus scrape metrics endpoint. |

---

## 2. Authentication Endpoints (`/api/v1/auth`)

| Method | Path | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Public | Register new Parent or Child account. |
| `POST` | `/api/v1/auth/login` | Public | Authenticate with email/password; returns JWT access & refresh tokens and user profile. |
| `POST` | `/api/v1/auth/refresh` | Public | Exchange refresh token for fresh access token with automatic single-use rotation. |
| `POST` | `/api/v1/auth/logout` | Authenticated | Revoke refresh token and invalidate active sessions. |

---

## 3. Pairing & Connection Protocol (`/api/v1/pairing`)

| Method | Path | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/pairing/code` | Authenticated | Generate a one-time 8-character pairing code (*e.g., `NV-4821`*), valid for 10 minutes. |
| `POST` | `/api/v1/pairing/verify` | Authenticated | Submit opposite device's code; verifies role compatibility and links family. |
| `GET` | `/api/v1/pairing/status` | Authenticated | Check current family link state (PENDING, CONNECTED, UNLINKED). |
| `POST` | `/api/v1/pairing/unlink` | Authenticated (Parent) | Revoke family connection and unlink child device. |

---

## 4. Device Management & Telemetry (`/api/v1/device`, `/api/v1/battery`, `/api/v1/network`)

| Method | Path | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/device/register` | Authenticated | Register device hardware fingerprint, model, OS version, and FCM token. |
| `GET` | `/api/v1/device/{id}` | Authenticated | Fetch device details (authorized family scope only). |
| `POST` | `/api/v1/device/heartbeat` | Authenticated (Device) | Periodic keep-alive ping updating online status in Redis and MySQL. |
| `POST` | `/api/v1/battery/telemetry` | Authenticated (Device) | Ingest battery level, charging state, temperature, and health metrics. |
| `GET` | `/api/v1/battery/current/{deviceId}` | Authenticated | Fetch current battery status. |
| `GET` | `/api/v1/battery/history/{deviceId}` | Authenticated (Parent) | Fetch 24-hour battery percentage historical trend. |
| `POST` | `/api/v1/network/telemetry` | Authenticated (Device) | Ingest network type (Wi-Fi, 4G, 5G), signal dBm, and quality badge. |
| `GET` | `/api/v1/network/current/{deviceId}` | Authenticated | Fetch current network connectivity status. |
| `GET` | `/api/v1/network/history/{deviceId}` | Authenticated (Parent) | Fetch network connection history log. |

---

## 5. Location, Usage & Activity (`/api/v1/...`)

| Method | Path | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/location/telemetry` | Authenticated (Child) | Post current GPS coordinates, accuracy meters, provider, and source mode. |
| `GET` | `/api/v1/location/current/{deviceId}` | Authenticated | Parent gets exact/approx location; Child gets simple status. |
| `GET` | `/api/v1/location/history/{deviceId}` | Authenticated (Parent) | Fetch paginated location history (`?page=0&limit=100`, max 500). |
| `POST` | `/api/v1/location/purge` | Authenticated (Parent) | Purge historical locations older than N days. |
| `POST` | `/api/v1/usage/sync` | Authenticated (Child) | Sync Android UsageStats daily/weekly app runtime intervals. |
| `GET` | `/api/v1/usage/today/{deviceId}` | Authenticated | Fetch today's screen time breakdown. |
| `GET` | `/api/v1/usage/week/{deviceId}` | Authenticated (Parent) | Fetch 7-day weekly usage trend and application rankings. |
| `GET` | `/api/v1/activity/live/{deviceId}` | Authenticated (Parent) | Fetch current broad active application status. |
| `GET` | `/api/v1/history/{deviceId}` | Authenticated (Parent) | Fetch paginated chronological timeline of device events (`Pageable`). |

---

## 6. Device Health & Clean Up (`/api/v1/device-health`, `/api/v1/cleanup`)

| Method | Path | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/device-health/telemetry` | Authenticated (Child) | Ingest storage (total, free), RAM usage, and permission states. |
| `GET` | `/api/v1/device-health/current/{deviceId}` | Authenticated | Fetch device storage & memory health summary. |
| `GET` | `/api/v1/cleanup/summary/{deviceId}` | Authenticated | Fetch storage breakdown and cache clean-up recommendations. |
| `POST` | `/api/v1/cleanup/execute` | Authenticated (Child) | Record user-initiated app cache clean-up execution. |

---

## 7. Convocation API (`/api/v1/convocation`)

*Completely isolated module: Segregated from all telemetry and health pipelines.*

| Method | Path | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/convocation/parent/send` | Authenticated (Parent) | Parent sends priority message. Dispatches decoy push `"Check your battery status"` to Child. |
| `GET` | `/api/v1/convocation/parent/history` | Authenticated (Parent) | Permanent retained message history for the family unit. |
| `GET` | `/api/v1/convocation/parent/seen` | Authenticated (Parent) | Map of message IDs to Seen boolean states. |
| `POST` | `/api/v1/convocation/child/start-viewing` | Authenticated (Child) | Child toggles ON. Reveals all accumulated unread messages; starts server 2-min expiry. |
| `GET` | `/api/v1/convocation/child/visibility-state`| Authenticated (Child) | Check if 2-minute viewing mode is active, remaining seconds, and unread count. |
| `POST` | `/api/v1/convocation/child/send` | Authenticated (Child) | Child sends message to Parent. Disappears from Child view upon server receipt. |

---

## 8. Real-Time WebSocket Protocol (STOMP over WebSocket)

- **Handshake Endpoint**: `/ws`
- **Authentication**: Bearer JWT token sent in `Authorization` header during STOMP `CONNECT` frame.
- **Topics**:
  - `/topic/battery/{deviceId}`: Battery updates
  - `/topic/network/{deviceId}`: Network connectivity and quality updates
  - `/topic/location/{deviceId}`: GPS coordinate breadcrumbs
  - `/topic/activity/{deviceId}`: Broad application activity (Parent only)
  - `/topic/alerts/{familyId}`: Immediate battery/network/stale alerts
  - `/topic/convocation/{familyId}`: Real-time Convocation messages & Seen notifications
  - `/topic/pairing/{familyId}`: Pairing handshake state changes
