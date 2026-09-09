# Nivya Platform — Comprehensive Testing Guide & Specification

This document details the multi-tier testing strategy, automated test commands, test suite inventory, known limitations, and manual verification checklists for the **Nivya Consent-Based Family Safety & Device Status Monitoring Platform**.

---

## 1. Test Strategy

Nivya follows a defense-in-depth testing pyramid designed to guarantee absolute family privacy, sibling data isolation, real-time telemetry reliability, and seamless offline-to-online recovery:

```
                  ▲
                 / \
                /E2E\       Platform Lifecycle Integration Tests
               /=====\      (Parent/Child Register, Pair, Telemetry,
              /   IT  \      Alerts, Convocation, Reconnect)
             /=========\
            / Component \   Security Invariants, Role Authorization,
           /             \  WebSocket STOMP Channel Auth, Rate Limiting
          /===============\
         /    Unit Tests   \ Domain Services, Android Sync Workers,
        /___________________\ React Navigation, Telemetry Formatters
```

### Architectural Testing Boundaries
1. **Zero-Trust Family Isolation**: Every domain service query and WebSocket topic subscription is verified to ensure Parent A cannot access Parent B's data, and Child accounts cannot access other Child devices (Sibling Isolation).
2. **Authoritative Server Expiration**: Convocation visibility windows and ephemeral viewing sessions are tested strictly against server time (`Instant.now()`), proving client timestamps cannot manipulate message retention.
3. **Decoy Notification Verification**: Push notifications dispatched for priority messages are verified to contain solely generic decoy payloads (`"Check your battery status"`), preventing private message leakage to lock screens.
4. **Resilient Real-Time Fallbacks**: Real-time communication and transient caching are tested both with active Redis coordination and with automatic local broker fallback during Redis unavailability.

---

## 2. Automated Test Commands

### A. Backend (Java 21 / Spring Boot)

Navigate to the `backend/` directory:

```bash
cd backend
```

- **Run all automated tests (117 tests)**:
  ```bash
  mvn test
  ```

- **Run specific test suites**:
  ```bash
  # Complete end-to-end lifecycle integration test
  mvn test -Dtest=PlatformLifecycleIntegrationTest

  # Security hardening and invariant verification suite
  mvn test -Dtest=SecurityHardeningIntegrationTest

  # Real-time WebSocket and Redis coordination suite
  mvn test -Dtest=RealtimeCommunicationIntegrationTest

  # Convocation priority messaging and ephemeral viewing suite
  mvn test -Dtest=ConvocationIntegrationTest

  # Authentication, rate limiting, and token rotation suite
  mvn test -Dtest=AuthIntegrationTest
  ```

- **Compile and verify without running tests**:
  ```bash
  mvn clean compile test-compile
  ```

---

### B. Android Client (Kotlin / Android SDK 34)

Navigate to the `android/` directory:

```bash
cd android
```

- **Run all local unit tests (71+ tests)**:
  ```bash
  ./gradlew testDebugUnitTest
  ```

- **Run a specific Android test class**:
  ```bash
  ./gradlew testDebugUnitTest --tests "com.nivya.auth.AuthAndPairingUnitTest"
  ./gradlew testDebugUnitTest --tests "com.nivya.permissions.PermissionsUnitTest"
  ./gradlew testDebugUnitTest --tests "com.nivya.network.RealtimeCommunicationUnitTest"
  ```

- **Compile and assemble Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```

---

### C. React Web Dashboard (TypeScript / Vite / Vitest)

Navigate to the `web/` directory:

```bash
cd web
```

- **Run all React test suites (26 tests)**:
  ```bash
  npm run test
  ```

- **Run tests in watch mode (during development)**:
  ```bash
  npx vitest
  ```

- **Run TypeScript type checking & Production Build**:
  ```bash
  npm run build
  ```

---

## 3. Test Suite Inventory

| Tier | Test Suite File | Test Scope | Status |
| :--- | :--- | :--- | :--- |
| **Backend** | `PlatformLifecycleIntegrationTest.java` | End-to-end user lifecycle: Register -> Pair -> Ingest -> Alert -> Convocation -> Reconnect Snapshot | **PASS (100%)** |
| **Backend** | `SecurityHardeningIntegrationTest.java` | 11 core security invariants, cross-family isolation, sibling isolation, rate limiting (429), refresh token reuse revocation | **PASS (100%)** |
| **Backend** | `RealtimeCommunicationIntegrationTest.java` | STOMP CONNECT JWT auth, SUBSCRIBE destination authorization, transient store cache, fallback broker | **PASS (100%)** |
| **Backend** | `AuthIntegrationTest.java` | User registration, login, rate limiting, token rotation, logout revocation | **PASS (100%)** |
| **Backend** | `RoleIntegrationTest.java` | Role probe, role selection persistence, role endpoint restrictions | **PASS (100%)** |
| **Backend** | `PairingIntegrationTest.java` | Ephemeral pairing code generation, device connection, code expiration | **PASS (100%)** |
| **Backend** | `BatteryIntegrationTest.java` | Battery status ingestion, drain rate calculation, low battery alerts | **PASS (100%)** |
| **Backend** | `NetworkIntegrationTest.java` | WiFi/cellular telemetry, offline transition, signal quality classification | **PASS (100%)** |
| **Backend** | `LocationIntegrationTest.java` | GPS coordinate ingestion, reverse geocoding fallback, breadcrumb history | **PASS (100%)** |
| **Backend** | `UsageIntegrationTest.java` | Screen time aggregation, top apps ranking, weekly trend calculation | **PASS (100%)** |
| **Backend** | `LiveActivityIntegrationTest.java` | Foreground app tracking, elapsed duration, timeline history | **PASS (100%)** |
| **Backend** | `HistoryIntegrationTest.java` | Paginated chronological event log, app name filtering, date range filter | **PASS (100%)** |
| **Backend** | `DeviceHealthIntegrationTest.java` | Storage headroom, RAM pressure, permission health, composite score | **PASS (100%)** |
| **Backend** | `AlertIntegrationTest.java` | Rule evaluation, threshold triggering, notification records, resolution | **PASS (100%)** |
| **Backend** | `ConvocationIntegrationTest.java` | Parent priority send, decoy push notification, child 2-min viewing session | **PASS (100%)** |
| **Backend** | `PushNotificationIntegrationTest.java` | FCM token registration, topic subscription, alert notification dispatch | **PASS (100%)** |
| **Android** | `AuthAndPairingUnitTest.kt` | Email sanitization, role routing, pairing code formatting, token buffer expiry | **PASS (100%)** |
| **Android** | `PermissionsUnitTest.kt` | Location precision, usage stats AppOps, notification SDK checks, Doze mode | **PASS (100%)** |
| **Android** | `RealtimeCommunicationUnitTest.kt` | STOMP frame construction, message parser, exponential backoff with jitter | **PASS (100%)** |
| **Android** | `BatterySyncUnitTest.kt` | Battery percentage quantization, charging status, offline sync queue | **PASS (100%)** |
| **Android** | `NetworkSyncUnitTest.kt` | ConnectivityManager network callback, offline state transition | **PASS (100%)** |
| **Android** | `LocationSyncUnitTest.kt` | High-accuracy vs balanced power GPS updates, breadcrumb buffer | **PASS (100%)** |
| **Android** | `UsageSyncUnitTest.kt` | UsageStatsManager aggregation, daily screen time duration rollup | **PASS (100%)** |
| **Android** | `LiveActivityUnitTest.kt` | Foreground app transition detection, private description filtering | **PASS (100%)** |
| **Android** | `HistoryUnitTest.kt` | Chronological event buffer, local SQLite retention limit pruning | **PASS (100%)** |
| **Android** | `DeviceHealthSyncUnitTest.kt` | Storage free bytes, RAM headroom, permission score calculation | **PASS (100%)** |
| **Android** | `StorageCleanUpUnitTest.kt` | 30-day telemetry retention purge, cache directory truncation | **PASS (100%)** |
| **Android** | `AlertUnitTest.kt` | Local threshold checking, urgent reminder badge formatting | **PASS (100%)** |
| **Android** | `ConvocationUnitTest.kt` | Ephemeral countdown timer (120s), single toggle activation | **PASS (100%)** |
| **Android** | `FcmNotificationUnitTest.kt` | Decoy notification parser, payload isolation, background wake-lock | **PASS (100%)** |
| **React Web**| `authService.test.ts` | Session storage, JWT expiration check, role permission guard, logout cleanup | **PASS (100%)** |
| **React Web**| `navigation.test.ts` | Parent dashboard routes, public auth routes, child access denial | **PASS (100%)** |
| **React Web**| `dashboard.test.ts` | Telemetry card formatting, live activity sanitization, alert counters | **PASS (100%)** |
| **React Web**| `convocation.test.ts` | Retained parent history, ephemeral child countdown, Seen indicators | **PASS (100%)** |
| **React Web**| `websocketService.test.ts` | STOMP connection states, exponential backoff, auto-resubscribe, reconnect | **PASS (100%)** |

---

## 4. Known Limitations & Environment Specifications

1. **H2 In-Memory DB vs. MySQL 8.0**:
   - Automated backend tests run on H2 in-memory mode (`@ActiveProfiles("test")`).
   - Production uses MySQL 8.0 with InnoDB foreign key constraints.
   - Specific SQL dialect functions (e.g. `TIMEDIFF`, `JSON_EXTRACT`) rely on JPA repository abstraction for cross-compatibility.
2. **MockMvc Transport Simulation**:
   - MockMvc simulates the Spring MVC dispatcher servlet and STOMP message channel in-process. Full TCP network latency and raw WebSocket frames are validated using the Android OkHttp client and Vitest WebSocket tests.
3. **Hardware Keystore in Android Unit Tests**:
   - Android JVM unit tests (`testDebugUnitTest`) run on desktop Java without an Android hardware Trusted Execution Environment (TEE). Android Keystore-backed `EncryptedSharedPreferences` is tested via mock abstraction; physical on-device cryptographic operations must be verified manually on a device/emulator.
4. **Redis Coordination Fallback**:
   - Tests execute in an environment where Redis may be unavailable; `RedisPubSubConfig` and `RedisMessagePublisher` automatically route events to the local Spring STOMP broker and in-memory concurrent map without failure.

---

## 5. Manual Test Checklist

Use this checklist prior to major staging releases or production deployments:

### Physical Android Device Validation
- [ ] **Initial Setup & Permissions**:
  - Install debug APK on physical Android device (`adb install -r app-debug.apk`).
  - Grant Location (`Allow all the time` for background breadcrumbs).
  - Enable Usage Access in Android Settings (`Apps > Special app access > Usage access`).
  - Disable Battery Optimization (`Battery > Unrestricted`).
  - Grant Notification permission (Android 13+).
- [ ] **Device Pairing**:
  - Open React Web Dashboard as Parent; generate pairing code (`NV-XXXX-XXXX`).
  - Enter pairing code in Android Child app; verify instant pairing confirmation and family enrollment.
- [ ] **Telemetry Background Ingestion**:
  - Put app in background; verify WorkManager periodic telemetry workers run every 15 minutes.
  - Check Battery percentage and Network state updates reflect in Parent dashboard.
- [ ] **Network Loss & Recovery**:
  - Enable Airplane Mode on Android device for 2 minutes.
  - Observe Parent Dashboard indicates device offline or stale state.
  - Disable Airplane Mode; confirm Android `NetworkMonitor` triggers immediate reconnection and transmits buffered telemetry.
- [ ] **Convocation Ephemeral Viewing**:
  - From Parent Dashboard, send a Convocation message.
  - Verify Android notification displays decoy title/body: `"Device Update: Check your battery status"`.
  - Open Android app Options, toggle Convocation ON; verify 120-second countdown timer initiates.
  - Verify message disappears completely when the 120s timer expires.

### React Web Dashboard Validation
- [ ] **Parent Login & Session Guard**:
  - Login with registered Parent credentials; verify redirection to `/dashboard`.
  - Attempt manual navigation to unauthenticated route; verify redirect to `/login`.
- [ ] **Real-Time WebSocket Indicator**:
  - Observe top navigation bar status badge displays `"Live"` (green pulse).
  - Terminate backend server; observe badge transitions to `"Reconnecting..."` with exponential backoff.
  - Restart backend server; observe badge transitions back to `"Live"` and dashboard state auto-refreshes.
- [ ] **Alerts & Threshold Management**:
  - Simulate low battery on child device (<15%); verify yellow/red warning pill appears in dashboard.
  - Navigate to `/alerts`; click `"Mark Resolved"`; verify unread badge count updates immediately.
- [ ] **Cross-Family Access Rejection**:
  - Attempt to inspect another family's device ID via direct URL (e.g. `/history/9999`); verify server returns `403 Forbidden` and UI displays Access Denied toast.
