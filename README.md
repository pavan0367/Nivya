# Nivya

> **Consent-Based Family Safety & Device-Status Monitoring System**  
> *Personal Family Safety • Device Health • Activity Status • Location • Usage • Convocation*

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3+-green.svg)](https://spring.io/projects/spring-boot)
[![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84.svg)](https://developer.android.com/jetpack/compose)
[![React](https://img.shields.io/badge/Web-React%20%2B%20Vite-61DAFB.svg)](https://vitejs.dev/)
[![MySQL](https://img.shields.io/badge/Database-MySQL%208.0-4479A1.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Cache-Redis%207.2-DC382D.svg)](https://redis.io/)

---

## 1. Executive Summary & Vision

**Nivya** is designed as a modern, transparent, consent-driven family safety and device-health platform. Unlike invasive surveillance or spyware utilities, Nivya operates under **explicit, mutual, verified consent**:

- **Connect Once → Remain Linked → Synchronize Permitted Information → Present Role-Specific Insights**.
- **No Covert Surveillance**: No keystroke logging, no call audio recording, no private message body interception, and no background camera/mic hijacking.
- **Strict Role Separation**: Parent and Child accounts authenticate separately. A one-time connection-code pairing process permanently links devices until explicitly unlinked.
- **Clean, Uncluttered UI**: Opening dashboards present high-level summaries only. Specific metrics are accessed via dedicated menu screens.
- **Independent Convocation Feature**: A private family guidance communication module completely isolated from device telemetry and health modules.

---

## 2. Core Architecture & Technology Stack

```
                                  ┌──────────────────────────┐
                                  │      Android Client      │
                                  │ (Jetpack Compose, Kotlin)│
                                  └────────────┬─────────────┘
                                               │
                                  HTTPS REST   │   WSS (STOMP)
                                               ▼
┌─────────────────────────┐       ┌──────────────────────────┐       ┌─────────────────────────┐
│     React Dashboard     │──────▶│   Spring Boot Backend    │◀──────│      Redis Cluster      │
│ (Vite, TS, Redux, WSS)  │       │  (Java 21, Security/JWT) │       │ (Pub/Sub, Event Bus)    │
└─────────────────────────┘       └────────────┬─────────────┘       └─────────────────────────┘
                                               │
                                               │ Flyway / JPA
                                               ▼
                                  ┌──────────────────────────┐
                                  │       MySQL 8.0 DB       │
                                  │  (Relational Telemetry)  │
                                  └──────────────────────────┘
```

| Layer | Technologies | Key Responsibilities |
| :--- | :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.x, Spring Security, JWT, Spring Data JPA, Hibernate, Flyway, Redis | Authentication, pairing validation, telemetry ingestion, access control, WebSocket pub/sub, audit logging. |
| **Android** | Kotlin, Jetpack Compose, Room, Coroutines, Flow, WorkManager, Retrofit, OkHttp | Role-specific Compose UI, device telemetry collection (battery, network, usage stats), offline queueing. |
| **Web** | React, Vite, TypeScript, Redux Toolkit, STOMP WebSocket client | Parent-facing real-time operational dashboard, live activity monitoring, historical telemetry analysis. |
| **Data & Cache** | MySQL 8.0, Redis 7.2 | Durable transactional storage, connection-code caching, distributed event broadcasting. |
| **Infra & DevOps** | Docker, Docker Compose, Nginx | Multi-container dev orchestration, production containerization, automated database backup scripts. |

---

## 3. Role-Based Capabilities & Separation

### Role Comparison Matrix

| Feature / Area | Parent Experience | Child Experience |
| :--- | :--- | :--- |
| **Account & Login** | Parent credentials & profile | Child credentials & profile |
| **Dashboard** | Clean high-level summary of linked child device | Clean device-care summary (battery, storage, network) |
| **Detailed Live Activity** | Available (Status-level app activity, contact labels) | **Not rendered** (Hidden from Child) |
| **Activity History** | Chronological timeline & durations | **Not rendered** (Hidden from Child) |
| **App Usage / Screen Time** | Categorized app usage & daily/weekly totals | Personal screen time summary |
| **Location** | Exact / approximate location + 30-day history | Current status only (*e.g., "Near Home", "Available"*) |
| **Battery Status** | Detailed charge %, state, health, historical trends | Current battery status & charging state |
| **Network & Quality** | Connection type (Wi-Fi, 4G, 5G), signal level | Network Quality badge: *Excellent / Good / Weak / Unavailable* |
| **Device Health** | Storage, memory, sync health, permission status | Simplified health summary (storage, battery condition) |
| **Device Clean Up** | Status overview of device storage | **Interactive clean-up tool** for temporary app cache files |
| **Family & Pairing Management** | Manage linked devices, revoke pairs, edit thresholds | Pairing screen active only prior to initial link |
| **Convocation** | Retained message history, send guidance, view "Seen" | Notepad-style, 2-min auto-hide, single toggle, no "Seen" |

> [!IMPORTANT]
> **Strict Child UI Design Rule**: The Child interface must **never** render labels such as *"Parent Only"*, *"Restricted"*, or *"Common"*. Parent features are structurally absent from the Child navigation hierarchy.

---

## 4. The Convocation Module (Independent Specification)

Convocation is an independent, isolated feature with distinct privacy properties:

1. **Complete Architectural Isolation**: Changes to Convocation never touch or affect Battery, Network, Location, Usage, Health, or History modules.
2. **Stealth Child Notification**: When a Parent sends a message, the Child receives a generic notification:
   > `"Check your battery status"`  
   Tapping this notification opens Nivya normally; it does **not** deep-link directly into Convocation.
3. **Single On/Off Toggle**: Inside Child Convocation Options (three-dot menu), there is a single On/Off toggle. No separate "Turn On" and "Turn Off" buttons exist.
4. **2-Minute Authoritative Viewing Mode**:
   - Turning the toggle ON reveals all accumulated unread Parent messages.
   - After **2 minutes**, the viewing mode expires automatically, reverting to the empty notepad screen.
   - Server-side timestamps (`view_started_at`, `visibility_expires_at`) enforce this authoritatively even if the app process is killed or the phone reboots.
5. **1-Hour Expiration**: Viewed messages permanently expire from Child-side visibility after **1 hour**.
6. **One-Way Sent Message Disappearance**: When a Child sends a message, it vanishes from the Child UI immediately upon transmission. The Parent retains the complete conversation history.
7. **Seen Indicator**: The Parent UI displays the `"Seen"` receipt; the Child UI never shows read receipts.

---

## 5. Repository Structure

```
Nivya/
├── backend/                  # Java 21 / Spring Boot REST API & WebSocket server
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/nivya/   # Modular package architecture
│       └── resources/        # Configs (application.yml) & Flyway migrations
├── android/                  # Native Android Jetpack Compose application
│   ├── app/                  # Application module & manifest
│   ├── core/                 # Network, security, permissions, database, utils
│   ├── data/                 # Repositories, local Room DB, remote API models
│   ├── domain/               # Domain models, use cases, business rules
│   ├── ui/                   # Jetpack Compose screens (Auth, Dashboards, Convocation)
│   ├── services/             # Background telemetry sync & lifecycle workers
│   └── permissions/          # Usage access, location, & notification handlers
├── web/                      # React / Vite / TypeScript Parent Web Dashboard
│   ├── package.json
│   └── src/                  # Components, pages, Redux store, layouts, WebSocket
├── database/                 # Flyway migrations, seed scripts, SQL indexes, procedures
├── docker/                   # Dockerfiles and service configs (Backend, MySQL, Redis, Nginx)
├── docs/                     # Technical specifications, SRS, API, DB schema, UI guides
├── scripts/                  # Automation scripts (dev, build, backup, validation)
├── .env.example              # Environment variables template
├── docker-compose.yml        # Development orchestration
├── README.md                 # Primary project documentation
└── LICENSE                   # Project license
```

---

## 6. Development Phased Roadmap

* **Phase 1: Project Foundation & Repository Structure** *(Completed)*
* **Phase 2:** Spring Boot Backend Foundation
* **Phase 3:** MySQL + Flyway Database Schema
* **Phase 4:** Authentication & JWT Authorization
* **Phase 5:** Role Selection Engine
* **Phase 6:** Parent/Child Pairing Code Protocol
* **Phase 7:** Android Foundation & Architecture
* **Phase 8:** Android Parent & Child Navigation/UI
* **Phase 9:** Battery Telemetry Engine
* **Phase 10:** Network Telemetry & Quality Engine
* **Phase 11:** Screen Time & UsageStats Special Permission UX
* **Phase 12:** Location Service & History
* **Phase 13:** Device Health & Storage Monitoring
* **Phase 14:** Child Clean Up Utility
* **Phase 15:** Alert Engine & Threshold Evaluation
* **Phase 16:** Parent Live Activity Pipeline
* **Phase 17:** Parent-Only Activity History
* **Phase 18:** Independent Convocation Feature (2-min auto-hide, single toggle)
* **Phase 19:** FCM Generic Push Notifications
* **Phase 20:** React Web Dashboard Foundation
* **Phase 21:** Real-Time WebSocket + Redis Bus
* **Phase 22:** Security Hardening & Rate Limiting
* **Phase 23:** Automated Unit, Service & UI Testing
* **Phase 24:** Docker Orchestration & Production Packaging
* **Phase 25:** Final Integration & Operational Documentation

---

## 7. Quick Start & Local Setup (Phase 1)

### Prerequisites
- **Java**: OpenJDK 21 LTS
- **Maven**: 3.9+
- **Node.js**: 20+ (with npm)
- **Docker & Docker Compose**: v24+

### Setup Steps

1. **Clone and Initialize Environment**:
   ```powershell
   # Copy environment variable template
   Copy-Item .env.example .env
   # Or run the setup helper script:
   powershell -ExecutionPolicy Bypass -File scripts/dev/setup_dev_env.ps1
   ```

2. **Validate Phase 1 Architecture**:
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts/dev/validate_structure.ps1
   ```

3. **Verify Docker Infrastructure**:
   ```powershell
   docker compose config
   ```

4. **Launch Local Services (MySQL & Redis)**:
   ```powershell
   docker compose up -d mysql redis
   ```

---

## 8. License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
