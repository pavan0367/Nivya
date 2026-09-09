# Nivya

> **Consent-Based Family Safety & Device-Status Monitoring Platform**  
> *Personal Family Safety • Device Health • Activity Status • Location • Usage • Convocation*

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green.svg)](https://spring.io/projects/spring-boot)
[![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84.svg)](https://developer.android.com/jetpack/compose)
[![React](https://img.shields.io/badge/Web-React%2018%20%2B%20Vite%205-61DAFB.svg)](https://vitejs.dev/)
[![MySQL](https://img.shields.io/badge/Database-MySQL%208.0-4479A1.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Cache-Redis%207.2%20AOF-DC382D.svg)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Multi--Stage-2496ED.svg)](https://www.docker.com/)
[![Tests](https://img.shields.io/badge/Tests-117%20Backend%20%7C%2071%2B%20Android%20%7C%2026%20Web-brightgreen.svg)]()

---

## 1. Executive Summary & Vision

**Nivya** is a modern, transparent, consent-driven family safety and device-health platform engineered for private personal use. Unlike invasive surveillance or covert spyware, Nivya operates strictly under **explicit, mutual, verified consent**:

- **Connect Once → Remain Linked → Synchronize Permitted Information → Present Role-Specific Insights**.
- **No Covert Surveillance**: No keystroke logging, no call audio recording, no private message body interception, and no background camera/mic hijacking.
- **Strict Role Separation**: Parent and Child accounts authenticate separately. A one-time connection-code pairing process permanently links devices until explicitly unlinked.
- **Clean, Uncluttered UI**: Opening dashboards present high-level summaries only. Specific metrics are accessed via dedicated menu screens.
- **Independent Convocation Feature**: A private family guidance communication module completely isolated from device telemetry and health modules.

---

## 2. Complete Architecture & Technology Stack

```
                              [ Internet / Clients ]
                         (Parent Web App, Android Client)
                                       │
                                       ▼ (Port 80 / 443)
                      ┌─────────────────────────────────┐
                      │    Nginx Reverse Proxy Gateway   │
                      │  (SSL/TLS, Security Headers, Gzip)│
                      └────────────────┬────────────────┘
                                       │ (nivya-network internal)
            ┌──────────────────────────┼──────────────────────────┐
            │ /                        │ /api/, /ws/, /actuator/  │ /healthz
            ▼                          ▼                          ▼
 ┌──────────────────────┐   ┌──────────────────────┐   ┌──────────────────┐
 │    nivya-frontend    │   │    nivya-backend     │   │  Gateway Health  │
 │ (React 18 SPA/Nginx) │   │  (Spring Boot 3.3.5) │   │     (200 OK)     │
 └──────────────────────┘   └──────────┬───────────┘   └──────────────────┘
                                       │
                        ┌──────────────┴──────────────┐
                        ▼                             ▼
             ┌─────────────────────┐       ┌─────────────────────┐
             │     nivya-mysql     │       │     nivya-redis     │
             │     (MySQL 8.0)     │       │    (Redis 7.2 AOF)  │
             │ [mysql_data volume] │       │ [redis_data volume] │
             └─────────────────────┘       └─────────────────────┘
```

| Layer | Technologies | Key Responsibilities |
| :--- | :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.3.5, Spring Security, BCrypt, JWT, Spring Data JPA, Hibernate, Flyway | Authentication, pairing validation, telemetry ingestion, access control, WebSocket pub/sub, audit logging, remote configuration. |
| **Android** | Kotlin 1.9.23, Jetpack Compose, Room, Coroutines, Flow, WorkManager, Retrofit 2, OkHttp 4 | Role-specific Compose UI, device telemetry collection (battery, network, usage stats), offline queueing, FCM. |
| **Web** | React 18, Vite 5.4, TypeScript, Lucide React, STOMP / SockJS | Parent-facing real-time operational dashboard, live activity monitoring, historical telemetry analysis. |
| **Data & Cache** | MySQL 8.0, Redis 7.2 | Durable transactional storage (utf8mb4), connection-code caching, token revocation, distributed event broadcasting. |
| **Infra & DevOps**| Docker, Docker Compose, Nginx 1.27 Alpine | Multi-stage builds, isolated bridge network, health checks, restart policies, automated backup & restore scripts. |

---

## 3. Complete Directory Tree

```
Nivya/
├── .env.example                               # Canonical environment variables template
├── docker-compose.yml                         # Production Docker Compose topology (isolated ports)
├── docker-compose.dev.yml                     # Development Docker Compose override (loopback bindings)
├── DEPLOYMENT.md                              # Production deployment runbook & operational manual
├── TESTING.md                                 # Multi-tier test strategy and execution commands
├── README.md                                  # Platform overview and documentation
├── LICENSE                                    # MIT License
│
├── backend/                                   # Spring Boot 3.3.5 Backend (Java 21)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/nivya/
│       │   │   ├── activity/                  # Live broad application activity tracking
│       │   │   ├── alerts/                    # Rule-based threshold alerts (battery, network, stale)
│       │   │   ├── audit/                     # Security auditing and compliance log entity/service
│       │   │   ├── auth/                      # Registration, login, token refresh, password hashing
│       │   │   ├── battery/                   # Battery telemetry ingestion & 24h historical trends
│       │   │   ├── common/                    # AppConfigController, HealthController, ApiResponse
│       │   │   ├── communication/             # High-level call metadata (duration, count)
│       │   │   ├── consent/                   # Cryptographic consent audit trails
│       │   │   ├── convocation/               # Isolated priority messaging & ephemeral 2m viewing
│       │   │   ├── device/                    # Device registration, sessions, heartbeats, status
│       │   │   ├── family/                    # Family units, membership, ownership authorization
│       │   │   ├── history/                   # Chronological timeline & paginated event audits
│       │   │   ├── location/                  # Geolocation, reverse geocoding, paginated history
│       │   │   ├── network/                   # Network telemetry, Wi-Fi/cellular, quality badges
│       │   │   ├── notification/              # FCM push notification dispatcher & decoy alerts
│       │   │   ├── pairing/                   # One-time connection codes, validation, persistent link
│       │   │   ├── privacy/                   # Granular privacy settings
│       │   │   ├── role/                      # Role definitions (ROLE_PARENT, ROLE_CHILD)
│       │   │   ├── security/                  # JWT token provider, security filter chain, RBAC
│       │   │   ├── usage/                     # Android UsageStats sync, daily/weekly aggregations
│       │   │   ├── user/                      # User identity and profile management
│       │   │   └── websocket/                 # STOMP broker, security interceptor, Redis pub/sub
│       │   └── resources/
│       │       ├── application.yml            # Core configuration & environment interpolations
│       │       ├── application-dev.yml        # Development profile
│       │       ├── application-prod.yml       # Production profile (validate DDL, warn logs)
│       │       └── db/migration/              # Flyway migrations V1__ through V12__
│       └── test/                              # 117 automated integration & unit test suites
│
├── android/                                   # Native Android Client (Kotlin / Jetpack Compose)
│   ├── build.gradle.kts
│   ├── app/                                   # App module, AndroidManifest.xml, build.gradle.kts
│   ├── core/                                  # Network, security, database, utils, AppMonitoring
│   ├── data/                                  # Repositories, local Room database, remote API models
│   ├── domain/                                # Domain entities, use cases, business rules
│   ├── ui/                                    # Jetpack Compose screens:
│   │   ├── auth/                              # Login & Registration
│   │   ├── role/                              # Role Selection (Parent / Child)
│   │   ├── pairing/                           # Connection Code exchange
│   │   ├── dashboard/                         # Role-specific Dashboards & Live Activity
│   │   ├── battery/                           # Battery status & charge condition
│   │   ├── network/                           # Network type & Quality badge
│   │   ├── screen_time/                       # Screen Time & UsageStats breakdown
│   │   ├── location/                          # Location breadcrumbs & status
│   │   ├── device_health/                     # Storage, RAM, and diagnostic state
│   │   ├── cleanup/                           # Interactive app cache clean-up tool
│   │   ├── alerts/                            # Device alerts & threshold warnings
│   │   └── convocation/                       # Notepad interface, 3-dot options, 1 toggle
│   ├── services/                              # Background sync workers & FCM service
│   └── permissions/                           # Educational intent helpers for special permissions
│
├── web/                                       # React 18 / Vite Parent Web Dashboard
│   ├── package.json
│   ├── vite.config.ts
│   ├── src/
│   │   ├── components/                        # Telemetry cards, charts, modals, sidebars
│   │   ├── pages/                             # Login, Dashboard, History, Live, Alerts, Settings
│   │   ├── services/                          # REST API client (axios) & STOMP WebSocket client
│   │   └── test/                              # 26 Vitest unit and component tests
│   └── dist/                                  # Built static production bundle
│
├── docker/                                    # Docker configurations
│   ├── backend/                               # Multi-stage Dockerfile (Temurin 21)
│   ├── frontend/                              # Multi-stage Dockerfile (Node 20 / Nginx) & nginx config
│   ├── nginx/                                 # Reverse proxy gateway config & SSL mount point
│   ├── mysql/                                 # MySQL init.sql (utf8mb4 charset & user privileges)
│   └── redis/                                 # Redis redis.conf (AOF persistence & memory policy)
│
├── docs/                                      # Technical documentation & specifications
│   ├── API/                                   # Complete REST & WebSocket protocol reference
│   ├── Architecture/                          # System architecture blueprints & data flow
│   ├── Database/                              # Relational schema ERD & Flyway migration guide
│   ├── Deployment/                            # Containerization & environment manual
│   ├── SRS/                                   # Complete Software Requirements Specification
│   └── UI/                                    # UI navigation, role separation, and mockups
│
└── scripts/                                   # Automation & operational scripts
    ├── backup.sh                              # POSIX shell backup script (MySQL + Redis + gzip)
    ├── backup.ps1                             # Windows PowerShell backup script
    ├── restore.sh                             # POSIX shell database restore script
    ├── restore.ps1                            # Windows PowerShell database restore script
    └── backup/db_backup.ps1                   # Backward-compatible backup wrapper
```

---

## 4. Role Comparison Matrix & Features

| Feature / Module | Parent Experience | Child Experience |
| :--- | :--- | :--- |
| **Account & Login** | Parent credentials & profile | Child credentials & profile |
| **Dashboard** | High-level summary of linked child device | Device-care summary (battery, storage, network) |
| **Live Activity** | Status-level app activity & contact labels | **Not rendered** (Hidden from Child) |
| **Activity History** | Chronological timeline & durations (paginated) | **Not rendered** (Hidden from Child) |
| **App Usage / Screen Time** | Categorized app usage & daily/weekly totals | Personal screen time summary |
| **Location** | Exact / approx coordinates & paginated history | Current status only (*e.g., "Near Home"*) |
| **Battery Status** | Charge %, state, health, historical trends | Current battery status & charging state |
| **Network & Quality** | Connection type (Wi-Fi, 4G, 5G), signal dBm | Network Quality badge: *Excellent / Good / Weak* |
| **Device Health** | Storage, memory, sync health, permission state | Simplified health summary (storage, battery) |
| **Clean Up** | Storage overview of child device | **Interactive clean-up tool** for app cache |
| **Family & Pairing** | Manage linked devices, revoke pairs | Pairing screen active only prior to initial link |
| **Convocation** | Permanent retained history, send notes, "Seen" | Notepad-style, 2-min auto-hide, 1 toggle, no "Seen" |

---

## 5. Convocation Module Specification

Convocation is an independent module with 21 strict architectural invariants:
1. **Zero Coupling**: Operates independently with zero imports/dependencies on telemetry or health modules.
2. **Permanent Parent History**: All sent and received messages remain retained in Parent history.
3. **Child Default Screen**: Empty notepad with text memo input.
4. **Three-Dot Options**: Top-right options dropdown containing **ONE single On/Off toggle**.
5. **Decoy Notification**: Push notification payload is always generic text: `"Check your battery status"`.
6. **No Message Exposure**: Notification never leaks message text or sender content.
7. **Normal App Launch**: Notification opens Nivya normally without deep-linking into Convocation.
8. **Accumulation**: Multiple unread Parent messages accumulate safely on the server.
9. **Single Reveal**: All unread messages become visible together when Child flips the toggle ON.
10. **Seen Status**: Read receipts appear **only** on the Parent side. Child never sees "Seen".
11. **2-Minute Server Expiry**: Visibility automatically expires after 120 seconds.
12. **Server Authoritative**: Expiry timestamps are calculated and enforced by the server (`visibility_expires_at`).
13. **Background Survival**: Server-enforced expiration survives app kill, backgrounding, or phone restart.
14. **WorkManager Enforcement**: Android client cleans local cache using WorkManager / AlarmManager timers.
15. **1-Hour Absolute Expiry**: Messages permanently expire from Child view after 1 hour.
16. **Parent History Unaffected**: Expiration applies strictly to the Child view.
17. **New Unread Sets**: New Parent messages form a new unread set requiring a new viewing activation.
18. **No Zombie Messages**: Old viewed messages do not reappear.
19. **Child Message Disappearance**: Child-sent messages disappear from Child view upon server receipt.
20. **Parent Retains Child Messages**: Child notes are stored permanently in Parent history.
21. **No Child History Access**: Child accounts receive 403 Forbidden when requesting Parent history.

---

## 6. Security & Privacy Controls

- **Zero Hardcoded Secrets**: Secrets and database credentials are provided exclusively via `.env` (git-ignored).
- **Password Security**: Passwords hashed using BCrypt with salt rounds / cost factor 12.
- **Strict Tenancy Isolation**: Device and family queries enforce row-level ownership validation (`DeviceAccessValidator`). Parent A cannot query Parent B's data; Child cannot query unrelated devices or siblings.
- **WebSocket Channel Authorization**: STOMP `CONNECT` requires valid Bearer JWT; `SUBSCRIBE` commands are intercepted and validated against the user's family ID and role.
- **Token Rotation**: Single-use refresh token rotation; tokens are revoked in Redis upon logout or password change.
- **No Covert Surveillance**: No keystroke logging, no call audio recording, no private message body interception, and no background camera/mic hijacking.
- **Android Official Intent Flows**: Special permissions (`PACKAGE_USAGE_STATS`, `MANAGE_EXTERNAL_STORAGE`) use educational prompts and official platform intents (`ACTION_USAGE_ACCESS_SETTINGS`).

---

## 7. Multi-Tier Test Results & Verification

| Tier | Test Suite | Tests Executed | Passed | Failures / Errors |
| :--- | :--- | :--- | :--- | :--- |
| **Backend** | Spring Boot Integration & Unit Tests | 117 | 117 | **0** |
| **Android** | Gradle JVM Unit & Compose Tests | 71+ | 71+ | **0** |
| **Web** | Vitest Component & Redux Tests | 26 | 26 | **0** |
| **Build** | Backend Package (`app.jar`) | 1 | 1 | **0** |
| **Build** | Android APK (`assembleDebug`) | 1 | 1 | **0** |
| **Build** | Web Bundle (`vite build`) | 1 | 1 | **0** |
| **Compose** | Docker Compose Production Config | 1 | 1 | **0** |
| **Compose** | Docker Compose Dev Override Config | 1 | 1 | **0** |

---

## 8. Quick Start & Deployment

### Development Setup
```bash
# 1. Clone repository and initialize environment
cp .env.example .env

# 2. Launch development stack (exposes loopback DB 127.0.0.1:3306 and Redis 127.0.0.1:6379)
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# 3. Verify health of all services
docker compose ps
```

### Production Deployment
```bash
# 1. Configure environment with strong unique secrets
cp .env.example .env
# Edit .env and generate secrets with: openssl rand -hex 32

# 2. Build and launch all 5 isolated containers in detached mode
docker compose up -d --build

# 3. Tail logs
docker compose logs -f nginx backend
```

---

## 9. Limitations & Future Scope

1. **Initial Private/Personal-Use Deployment**: Nivya is initially tailored for single-family or private self-hosted deployments. Multi-tenant billing and enterprise organization hierarchies are out of scope.
2. **Android Hardware Sensor Variability**: Battery temperature and step sensors vary across device manufacturers; Nivya reports `Unavailable` when hardware sensors are absent rather than fabricating data.
3. **Background Sync Frequency**: Android Doze Mode and OEM battery optimizations (e.g., Xiaomi MIUI, Samsung OneUI) may throttle periodic background workers unless battery optimization is disabled for Nivya.
4. **Initial Web Scope**: The web application is dedicated to Parent monitoring; Child device telemetry ingestion is mobile-first (Android).
