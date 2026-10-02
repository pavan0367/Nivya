# Nivya — Security, RBAC & Data Protection Specification

**Document Version:** 1.0.1  
**Status:** COMPLETE & FROZEN  
**Applicability:** Backend (Spring Boot 3.3.5) | Mobile (Android Jetpack Compose & iOS SwiftUI) | Web (React 18 + Vite)

---

## 1. Authentication & Session Security Architecture

### 1.1 Token Hierarchy & Storage
Nivya implements an industry-standard dual-token authentication model:
- **Short-Lived Access Token:** JWT (JSON Web Token) with standard claims (`sub`, `roles`, `iat`, `exp`). Validity: 15 minutes.
- **Single-Use Refresh Token:** High-entropy cryptographically secure random token persisted in the database with revocation tracking and single-use rotation. Validity: 30 days.

#### Client Storage Matrix
- **Android:** Encrypted `DataStore` backed by Android Keystore (`UserPreferencesDataStore.kt`).
- **iOS:** Secure Enclave Keychain services via `KeychainManager.swift` with `kSecAttrAccessibleAfterFirstUnlock`.
- **Web:** LocalStorage isolated to origin `https://nivya.pages.dev` with HTTPS transport encryption and strict Content-Security-Policy.

---

## 2. 401 Interception & Access-Token Refresh Flow

```
Client API Request
       │
       ▼
 [HTTP 401 Unauthorized]
       │
       ▼
OkHttp TokenAuthenticator / Axios Interceptor
       │
 ┌─────┴──────────────────────────────────────────────────────┐
 │ Check response count: responseCount(response) >= 2 ?       │
 │   - YES: Abort (Break infinite loop), dispatch LOGOUT      │
 │   - NO: Acquire synchronized thread lock / Mutex           │
 └─────┬──────────────────────────────────────────────────────┘
       │
       ▼
 POST /api/v1/auth/refresh (with current Refresh Token)
       │
 ┌─────┴──────────────────────────────────────────────────────┐
 │ Backend Token Rotation:                                    │
 │   1. Validate active status and expiry                     │
 │   2. Revoke old refresh token immediately                  │
 │   3. Issue new Access Token + new Rotated Refresh Token    │
 └─────┬──────────────────────────────────────────────────────┘
       │
       ▼
 [HTTP 200 OK]
       │
 ┌─────┴──────────────────────────────────────────────────────┐
 │ Update secure local storage with fresh token pair          │
 │ Rebuild failed request: Authorization: Bearer <new_token>  │
 │ Release lock & transparently retry original request        │
 └────────────────────────────────────────────────────────────┘
```

### 2.1 Infinite Loop & Concurrency Protection
- **Lock Coordination:** Requests running in parallel that hit 401 wait on the synchronized refresh block and re-use the newly received token without generating multiple redundant refresh calls.
- **Single Retry Enforcement:** Authenticator tracks request retry count. If retrying with the new token returns 401 again, the session is treated as revoked, local state is purged, and the user is routed to the Login screen.
- **Auth Endpoint Exemption:** `/api/v1/auth/login`, `/api/v1/auth/register`, and `/api/v1/auth/refresh` are excluded from triggering authentication interception.

---

## 3. Role-Based Access Control (RBAC) Matrix

| Resource / Endpoint | PARENT Role | CHILD Role | Unauthenticated |
| :--- | :---: | :---: | :---: |
| `POST /api/v1/auth/login` | Allowed | Allowed | Allowed |
| `POST /api/v1/auth/refresh` | Allowed | Allowed | Allowed |
| `GET /api/v1/family` | **ALLOWED** | **DENIED (HTTP 403)** | DENIED (HTTP 401) |
| `GET /api/v1/family/devices` | **ALLOWED** | **DENIED (HTTP 403)** | DENIED (HTTP 401) |
| `POST /api/v1/convocation/guidance` | **ALLOWED** | **DENIED (HTTP 403)** | DENIED (HTTP 401) |
| `POST /api/v1/convocation/acknowledge` | DENIED | **ALLOWED** | DENIED (HTTP 401) |
| `POST /api/v1/telemetry/location` | DENIED | **ALLOWED** | DENIED (HTTP 401) |
| `GET /api/v1/telemetry/device/{id}` | **ALLOWED (Family Tenant)** | DENIED | DENIED (HTTP 401) |
| `POST /api/v1/pairing/generate` | Allowed | Allowed | DENIED (HTTP 401) |
| `POST /api/v1/pairing/connect` | Allowed | Allowed | DENIED (HTTP 401) |

---

## 4. Multi-Tenant Device Access & Family Isolation
Every device telemetry and location query is guarded by `DeviceAccessValidator.java`:
1. The authenticated user's active family tenancy is resolved.
2. The target device is queried to verify it belongs to the exact same family entity.
3. If a Parent attempts to access telemetry or location of a device from a foreign family, access is denied immediately with HTTP 403 Forbidden and logged as a security alert.

---

## 5. Location Telemetry Protection & Boundary Rules
- **Zero Coordinates Protection:** Coordinates `(0.0, 0.0)` ("Null Island") are strictly rejected as invalid hardware GPS anomalies.
- **NaN / Out-of-Range Rejection:** Latitude must be within `[-90.0, 90.0]` and Longitude within `[-180.0, 180.0]`. `Double.NaN` and infinite coordinates fail validation.
- **Accuracy Bounds:** Accuracy is clamped to physical bounds (`5.0m` to `500.0m`).
- **Stationary Filtering:** Telemetry updates where location has not changed beyond GPS jitter threshold are filtered to prevent battery drain.

---

## 6. Secret Redaction & Logging Safeguards
- **HTTP Header Redaction:** OkHttp `HttpLoggingInterceptor` explicitly sets `.redactHeader("Authorization")`.
- **Hardware Push Token Masking:** FCM and APNs device push tokens are masked when logging (`fcm_tok...1234`).
- **Production Build Log Elimination:** In release APK builds, `ENABLE_LOGGING = false`, completely suppressing debug and telemetry console logging.
