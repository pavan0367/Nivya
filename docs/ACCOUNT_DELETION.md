# Nivya — Settings & Permanent Account Deletion Specification

**Status**: LOCKED & FINAL  
**Feature Version**: 1.0.0  
**Platforms**: Web, Android (Jetpack Compose), iOS (SwiftUI), Backend (Spring Boot 3.3.5 / Java 21)

---

## 1. Executive Summary & Design Lock

The **Settings & Permanent Account Deletion** feature provides users across all three Nivya client platforms (Web, Android, iOS) with a unified, high-aesthetic Settings side-navigation interface and a cryptographically secure, irreversible account deletion workflow.

> [!IMPORTANT]
> **CRITICAL ARCHITECTURAL INVARIANT: NON-CASCADING ISOLATION**
> - Deleting a **Parent** account will **NEVER** delete or cascade to any connected **Child** account or child telemetry. The child account remains completely intact and preserved.
> - Deleting a **Child** account will **NEVER** delete or cascade to the supervising **Parent** account.
> - If a Parent was the sole creator of the family unit, family ownership is safely reassigned to the remaining member prior to parent deletion, preventing foreign-key cascade destruction.

---

## 2. Settings Side-Navigation Structure

Both Parent and Child accounts have access to a dedicated **Settings** section:
- **Parent Web URL**: `/settings` (inside `DashboardLayout`)
- **Child Web URL**: `/child/settings` (inside `ChildLayout`)
- **Android**: `ParentSettingsScreen.kt` & `ChildSettingsScreen.kt`
- **iOS**: `ParentSettingsView.swift` & `ChildSettingsView.swift`

### Navigation Tabs / Sections:

| Section | Parent View | Child View |
| :--- | :--- | :--- |
| **1. Profile** | Parent Name, Email, Role badge, ID, Active status | Child Name, Email, Role badge, ID, Active status |
| **2. Account Settings** | Email verification badge, Role details, Password reset | Email verification badge, Role details, Password reset |
| **3. Connected Unit** | **Connected Child**: Paired devices, Enrolment QR code, Pairing code generator, Unpair action | **Connected Parent**: Guardian email (masked), Protected connection status |
| **4. Notifications** | Email alerts preferences (Login, New device, App updates) + Push delivery preferences (Low battery, Disconnect, Convocation) | Push delivery preferences & alert settings |
| **5. Privacy & Security** | Active device sessions table with remote revoke, Consent agreements v1.0 | Transparency overview, Mutual consent terms |
| **6. Data & Storage** | Local cache size, Clear temporary cache, Sync telemetry | Local cache stats, Clear app cache |
| **7. Delete Account** | Danger Zone with 4-step permanent parent deletion flow | Danger Zone with 4-step child deletion flow (requires parent code) |
| **8. Help & Support** | FAQs, Safety documentation, Support contact (`support@nivya.local`) | FAQs, Safety documentation, Support contact |
| **9. Logout** | Confirm session termination and sign out | Confirm session termination and sign out |

---

## 3. Permanent Account Deletion Flows

### 3.1. Parent Account Deletion Flow (4-Screen Wizard)

1. **Screen 1: Overview & Warning**
   - Red warning shield banner highlighting irreversible deletion.
   - Explicit impact bullets:
     - Parent personal credentials and active sessions are permanently erased.
     - **Child accounts are NOT deleted; their data remains preserved.**
   - Action: `[Continue to Verification]`
2. **Screen 2: Identity Confirmation (Password)**
   - Secure password input field.
   - Validated against BCrypt salted password hash on the backend.
   - Action: `[Confirm Identity & Proceed]`
3. **Screen 3: Final Confirmation**
   - Approved warning notification explaining that deletion is permanent and irreversible, and confirming other family member accounts remain safe and intact.
   - Action Buttons:
     - `[Yes, Delete My Account]` (danger action executing permanent deletion)
     - `[Cancel]` (aborts deletion and returns to safety)
4. **Screen 4: Success & Departure**
   - Checkmark icon and confirmation message: *"Account Successfully Deleted"*.
   - Action: `[Return to Home / Login]` (clears all local tokens and redirects to `/login`).

---

### 3.2. Child Account Deletion Flow

#### Case A: Connected Child (Supervised by Parent)
1. **Screen 1: Overview & Approval Notice**
   - Informs child that connected deletion requires parent authorization.
   - Displays masked parent email (`p***@example.com`).
   - Action: `[Request Parent Approval]`
2. **Screen 2: 6-Digit Approval Code Entry**
   - 6-digit cryptographically secure code dispatched to parent via real SMTP/EmailService.
   - 15-minute TTL countdown timer.
   - Rate limit: Maximum 5 verification attempts.
   - Resend Code capability (invalidates previous pending codes).
   - Action: `[Verify Code & Proceed]`
3. **Screen 3: Final Confirmation**
   - Approved warning notification explaining that parent approval was verified and deletion is permanent.
   - Action Buttons:
     - `[Yes, Delete My Account]` (danger action executing permanent deletion)
     - `[Cancel]` (aborts deletion)
4. **Screen 4: Success & Departure**
   - Confirmation of successful removal and `[Return to Login]`.

#### Case B: Disconnected Child (Independent Account)
- Follows the Password Verification flow identical to the Parent flow (Screen 1 -> Screen 2 Password -> Screen 3 Confirmation -> Screen 4 Success).

---

## 4. Backend REST API Specification

Base Path: `/api/v1/account`  
Authorization: Bearer JWT Token required on all endpoints.

### 1. `GET /api/v1/account/deletion/status`
Returns deletion prerequisites and connection status for the authenticated user.
```json
{
  "success": true,
  "data": {
    "role": "CHILD",
    "isChild": true,
    "hasConnectedParent": true,
    "connectedParentEmailMasked": "pa***@nivya.local",
    "instructions": "Child account is connected to a parent. Deletion requires parent approval."
  }
}
```

### 2. `POST /api/v1/account/deletion/request-child-approval`
Generates a cryptographically secure 6-digit code, stores its SHA-256 hash in `deletion_approval_codes`, and sends an approval email to the connected parent.
```json
{
  "success": true,
  "data": {
    "success": true,
    "approvalCodeRequired": true,
    "parentEmailMasked": "pa***@nivya.local",
    "expiresInMinutes": 15,
    "message": "Approval code sent to connected parent email."
  }
}
```

### 3. `POST /api/v1/account/deletion/verify-child-code`
Validates the approval code entered by the child without executing deletion yet.
- **Request Body**:
  ```json
  {
    "approvalCode": "748291"
  }
  ```
- **Response**:
  ```json
  {
    "success": true,
    "data": {
      "valid": true,
      "message": "Approval code verified successfully."
    }
  }
  ```

### 4. `POST /api/v1/account/delete`
Executes permanent deletion of the authenticated account.
- **Request Body (Parent or Disconnected Child)**:
  ```json
  {
    "password": "UserCurrentPassword123!"
  }
  ```
- **Request Body (Connected Child)**:
  ```json
  {
    "approvalCode": "748291"
  }
  ```
- **Response**:
  ```json
  {
    "success": true,
    "message": "Account successfully deleted."
  }
  ```

---

## 5. Database Schema: `deletion_approval_codes`

```sql
CREATE TABLE IF NOT EXISTS deletion_approval_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    child_user_id BIGINT NOT NULL,
    parent_user_id BIGINT NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    INDEX idx_del_child_status (child_user_id, status),
    INDEX idx_del_parent_status (parent_user_id, status),
    INDEX idx_del_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 6. Audit Logging & Security Guarantees

All deletion operations record immutable audit events via `AuditService`:
- `ACCOUNT_DELETED`: Logged when an account is permanently wiped.
- `ACCOUNT_DELETION_UNAUTHORIZED`: Logged on invalid password attempts.
- `CHILD_DELETION_APPROVAL_REQUESTED`: Logged when a child requests a code.
- `CHILD_DELETION_CODE_MISMATCH`: Logged on failed approval code attempts.
- `CHILD_DELETION_CODE_VERIFIED`: Logged on successful code verification.

### Sensitive Data Cleanup Order:
Prior to deleting the `User` entity, all dependent foreign records are explicitly removed in a transactional unit:
1. `deletion_approval_codes` (records involving child or parent)
2. `refresh_tokens` (all tokens revoked and purged)
3. `email_verification_codes` (all OTP records wiped)
4. `email_preferences` (preference row purged)
5. `device_sessions` (active session rows deleted)
6. `pairing_requests` (all requests deleted)
7. `consents` (user consent row deleted)
8. `convocation_messages` (messages sent/received purged)
9. `devices` (enrolled devices unregistered)
10. `family_members` (family membership unlinked; family reassigned if owner)
11. `users` (user row permanently deleted and transaction flushed)

---

## 7. Platform Parity Matrix

| Feature | Backend | Web Console | Android Companion | iOS Companion |
| :--- | :---: | :---: | :---: | :---: |
| Settings Navigation Structure | Full API | Full Side-Nav Layout | Compose Sections | SwiftUI Sections |
| Profile & Account Details | Yes | Yes | Yes | Yes |
| Connected Family Unit Info | Yes | Yes | Yes | Yes |
| Notifications Preferences | Yes | Yes | Yes | Yes |
| Privacy & Sessions Revoke | Yes | Yes | Yes | Yes |
| Data & Cache Management | Yes | Yes | Yes | Yes |
| Parent 4-Step Deletion Flow | Yes | Yes | Yes | Yes |
| Child Parent Approval Deletion | Yes | Yes | Yes | Yes |
| Non-Cascading Data Isolation | Yes | Yes | Yes | Yes |
| Audit Logging | Yes | N/A | N/A | N/A |

*All components verified and locked.*
