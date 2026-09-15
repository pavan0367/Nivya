# Nivya Platform — Locked Baseline Specification

> **LOCKED BASELINE DECLARATION**  
> All currently approved functionality, layouts, flows, security rules, and platform behavior are locked. Future changes must not modify them unless the user explicitly requests the specific change.

---

## 1. Scope & Ecosystem Architecture

The Nivya platform is a cross-platform family protection, digital wellness, and priority communication system spanning:
- **Web**: React 18, Vite, TypeScript, TailwindCSS/Vanilla design tokens, Lucide icons, SockJS, STOMP.
- **Android**: Native Jetpack Compose, Kotlin, OkHttp, Coroutines/Flow, Material 3 design system.
- **iOS**: Native SwiftUI, Combine, URLSession WebSocket, Crack/Freak haptics.
- **Backend**: Spring Boot 3.3.5, Java 21, Spring Security 6, Spring Data JPA, Hibernate 6, Flyway migrations.
- **Datastores & Infra**: MySQL 8.0 (InnoDB, utf8mb4), Redis 7.2 (Pub/Sub & caching), Nginx 1.27 Gateway.

---

## 2. Locked Core Modules & Behaviors

### 2.1 Parent & Child Account System
- Distinct roles: `PARENT` (administrator) and `CHILD` (companion).
- Role persistence in local storage/token claims with automatic session routing.
- Mandatory email verification with 6-digit cryptographic OTP (15-minute TTL, SHA-256 at rest, single-use, resend-invalidation, attempt-capped).

### 2.2 Pairing & Family Relationship Security
- Cryptographic 6-character connection code generation by Parent with 10-minute expiry.
- Automatic family unit creation (`families` and `family_members`).
- Persistent relationship survive device reboots, app restarts, and network drops.
- Disconnect Security: Child companion disconnection requires explicit Parent disconnect authorization code (`POST /api/v1/pairing/disconnect/code` and `/verify`).

### 2.3 Real Device Telemetry Engine
- **Battery**: Real-time percentage, charging state, health status, and historical logs.
- **Screen Time / App Usage**: Per-app usage duration, foreground timestamps, category rollups.
- **Network & Network Quality**: Active transport (Wi-Fi, Cellular), signal strength (RSSI), download/upload throughput estimates, and connection stability.
- **Location**: Latitude, longitude, accuracy radius, location timestamps, and movement events.
- **Device Health & Heartbeat**: Online/offline status, memory pressure, storage utilization, and OS/app versions.

### 2.4 Child Home & Parent Dashboard Layouts
- Exact locked visual hierarchy: Child home screen preserves fixed card ordering (Status, Battery, Network, Screen Time, Device Health, Quick Actions).
- Child navigation contains dedicated views for Dashboard, Battery, Network, Location, Screen Time, Device Health, Alerts, and Convocation.
- Parent dashboard supports seamless multi-device targeting and child device switching.

### 2.5 Convocation Priority Communication
- Priority communication between Parent and Child with immediate alert banner/dialog.
- **Child Ephemeral Visibility**: 2-minute authoritative viewing timer with client/server sync; automatic expiration and history purge on child view.
- **Parent Retained History**: Parents retain complete historical record of sent messages with read/seen indicators.
- **Realtime Infrastructure**: WebSocket/STOMP over `/ws` topic subscriptions (`/topic/family/{familyId}/convocation`) backed by Redis pub/sub fanout.
- **Interactive Triggers**: Quick-action tags (CRACK, FREAK), parent action popup (DONE, Pin, Reactions), Child "Turn on / Turn off" visibility control.
- **Input Auto-Focus**: Automatic field focus after sending messages on Web, Android, and iOS.

### 2.6 Permanent Account Deletion & Post-Deletion Notifications
- **Settings Integration**: Dedicated "Delete Account" action under Account Management in Settings.
- **Parent Deletion Flow**: 
  - Password identity confirmation.
  - Final warning dialog with `[Cancel]` and `[Yes, Delete My Account]` (no "Type DELETE" requirement).
  - Permanent removal of credentials, tokens, devices, and personal records.
  - Automatic delivery of irreversible deletion confirmation email to parent's registered address.
- **Connected Child Deletion Flow**:
  - Requires Parent approval code (6 digits dispatched to parent email, 15-min TTL, SHA-256 hashed).
  - Verification of code followed by final confirmation (`[Cancel]` and `[Yes, Delete My Account]`).
  - Child account permanently deleted. Parent account and remaining family members remain intact.
  - Separate confirmation email dispatched to Child.
  - Separate notification email dispatched to Connected Parent: *"Your child's Nivya account has been permanently deleted."*
- **Disconnected Child Deletion Flow**:
  - Uses password identity confirmation and deletes child account; sends confirmation email to child only.
- **Non-Cascading Referential Integrity**:
  - Ownership of families created by deleting user is safely reassigned to surviving family members or deleted if user was sole member.
  - Email recipient addresses captured upfront before destructive database operations.
  - Notification dispatch occurs strictly post-commit (`TransactionSynchronization.afterCommit()`).

---

## 3. Platform & Testing Status

| Component | Status | Test Automation | Notes |
|---|---|---|---|
| **Backend** | LOCKED | `mvn test` (Integration & Unit) | 44/44 tests passing (Auth, Convocation, Telemetry, Email, Account Deletion). |
| **Web** | LOCKED | `vitest run` (106 tests) & `npm run build` | Zero TypeScript errors, production build verified. |
| **Android** | LOCKED | `.\gradlew.bat test` | 54 tasks up-to-date, unit tests passing. |
| **iOS** | LOCKED | Static inspection & Swift syntax review | Note: Local Windows environment lacks Xcode toolchain; automated `xcodebuild` requires macOS runner. |
| **Docker** | LOCKED | 5 containers (nginx, frontend, backend, redis, mysql) | All containers healthy and verified. |
