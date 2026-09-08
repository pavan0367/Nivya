# Nivya — Complete Software Requirements Specification (SRS)

**Version 1.0 — Final Consolidated Specification**  
*Document Status: FINAL*  
*Primary Platform: Android First | Backend: Java 21 + Spring Boot | Database: MySQL 8.0 | Web: React + Vite | Realtime: WebSocket + Redis | Notifications: FCM*

---

## 1. Document Purpose
This Software Requirements Specification defines the complete functional and technical requirements for Nivya. It consolidates the project requirements for the Parent and Child mobile experiences, the persistent connection model, device status and health information, permitted activity status, parent history, and the independent Convocation feature.

Nivya is intended as a personal family-safety and device-status system. Parent and Child accounts are separate, the relationship is established through an explicit connection process, and the selected role determines exactly which interface and options are rendered.

## 2. Product Vision
Nivya should feel like a clean, modern phone-status and device-health application rather than a complicated control panel. The opening dashboard is a simple summary. Specific information is opened through separate Options/Side Menu items.

The central experience is:  
**Connect once → remain linked → synchronize permitted device information → present role-specific information clearly.**

## 3. Roles and Visibility

| Area | Parent | Child |
| :--- | :--- | :--- |
| **Account** | Parent account | Child account |
| **Dashboard** | Clean summary | Clean summary |
| **Detailed activity** | Available | Not rendered |
| **History** | Available | Not rendered |
| **App usage details** | Available | Not rendered as parent-side detail |
| **Location detail** | Available | Current location/status only |
| **Battery** | Available | Available |
| **Screen Time** | Available | Available |
| **Network** | Available | Available |
| **Network Quality** | Available | Available |
| **Device Health** | Available | Available |
| **Alerts** | Available | Parent alerts/notifications |
| **Clean Up** | Parent can view/manage supported device status | Available (Interactive device clean-up) |
| **Convocation** | Available | Available |
| **Family/Device management**| Available | Not rendered |
| **Pairing controls** | Available | Not rendered after connection |
| **Parent settings** | Available | Not rendered |

> [!IMPORTANT]
> **Important UI rule:** The Child interface must not show labels such as *"Parent Only"*, *"Common"*, or *"Parent Access"*. Instead, the application renders only the options valid for the selected role. Parent-only controls are simply absent from the Child interface.

## 4. Application Launch and Login Flow
```
App Open
   ↓
Login Screen
   ↓
Successful Authentication
   ↓
Select Role: Parent / Child
   ↓
Connection Screen (if not yet paired)
   ↓
Enter opposite device/account connection code
   ↓
Automatic connection
   ↓
Role-specific Dashboard
```
The role-selection page is the next page after login. Selecting Parent or Child changes the application experience. The wrong-role interface is not merely hidden behind a label; it should not be rendered into the selected role's navigation model.

## 5. Parent-Child Connection
### 5.1 Connection Codes
Immediately after role selection and before the main application is entered, the user receives a connection screen. Each side displays its own connection code and provides a field for entering the opposite person's code:
- **Parent phone:**
  - *Your Connection Code:* `NV-4821-KP90`
  - *Enter Child's Code:* `[____________]`
- **Child phone:**
  - *Your Connection Code:* `NV-7315-QA26`
  - *Enter Parent's Code:* `[____________]`

Entering the opposite device/account's valid code causes the backend to validate the relationship and automatically establish the connection.

### 5.2 Automatic Connection
Once a valid opposite code is entered and the pairing conditions are satisfied, both accounts/devices become linked. No separate repeated pairing step should be required for normal use.

### 5.3 Persistent Relationship
The family relationship remains active until explicitly revoked, the account/device is removed, or security credentials are invalidated. A normal reboot, app restart, temporary network loss, or temporary backend unavailability must not destroy the relationship.

### 5.4 Reboot / Offline
- **Phone restart**:
  Nivya initializes → secure device identity restored → session validation → connection restored → synchronization resumes.
- **Phone powered off / unreachable**:
  Parent sees `Offline` / `Last Seen` → device reconnects when available → synchronization resumes.

## 6. Parent Dashboard & Menu
The Parent dashboard is intentionally a clean summary and should not contain every detail at once:
- Child Device: `ONLINE` / `OFFLINE`
- Battery: `78%`
- Location: `Home`
- Screen Time: `2h 18m`
- Network: `5G`
- Network Quality: `Strong`
- Device Health: `Good`
- Alerts: `1`
- Last Sync: `22:16`

### 6.1 Parent Side Menu
- Dashboard
- Live Activity
- History
- App Usage
- Calls / communication status where supported and authorized
- Location
- Device Health
- Alerts
- Convocation
- Family / Devices
- Settings

## 7. Child Dashboard & Menu
The Child dashboard is deliberately limited to Child-facing device-care information:
- Battery: `62%`
- Screen Time: `1h 35m`
- Network: `4G`
- Network Quality: `Excellent`
- Location: `Near Home`
- Device Health: `Good`
- Alerts: `1`

### 7.1 Child Side Menu
- Dashboard
- Battery
- Screen Time
- Network
- Location
- Device Health
- Clean Up
- Alerts
- Convocation
- Settings

## 8. Device Status and Health
### 8.1 Battery
- Current battery percentage, charging / not charging state, full state where available.
- Battery health and temperature where Android APIs safely expose it.
- Battery trend/history for Parent; configurable low-battery alerts.
### 8.2 Network & Network Quality
- Connected / disconnected, Wi-Fi / mobile data, network type (4G/5G/Wi-Fi), signal level, internet availability.
- Child Network Quality Indicator: `Excellent`, `Good`, `Weak`, `Unavailable`.
- If the platform does not expose a reliable value, Nivya must show `Unavailable` rather than fabricate a result.
### 8.3 Device Health
- Storage availability, permission health relevant to Nivya, application/device status.

## 9. Child Device Care Features
- **Screen Time**: Summary of personal device usage.
- **Current Location Status**: High-level indicator (*"Near Home"*, *"Location Available"*, *"Location Unavailable"*).
- **Clean Up**: Option for supported temporary files/cache cleanup. Explains what will be removed before the operation; never silently deletes user files.

## 10. Parent Live Activity
Parent has a separate Live Activity option. It is not mixed into the dashboard.
- Represents status-level app/device activity without capturing private communication content:
  - *WhatsApp — Chatting with: Contact Name*
  - *Files — Viewing: document.pdf*
  - *Browser — Browsing*
  - *Phone — In call with: Contact Name (where supported)*
  - *Video application — Watching*
- When details cannot be reliably obtained, Nivya shows *"Active in app"* or *"Information unavailable"* rather than inventing values.
- Never intercepts private message bodies, call audio, or keystrokes.

## 11. Parent-Only History
Chronological activity timeline recording time, application, broad activity/status, and duration. Hidden from Child navigation.

## 12. Location
Parent can view child's current/last permitted location and 30-day location history. Requires explicit runtime permission. If permission is denied/revoked, UI surfaces *"Permission Required / Disabled / Unavailable"*.

## 13. App Usage
Dedicated Parent section showing daily/weekly totals and application/category breakdowns (e.g. YouTube 42m, Instagram 28m).

## 14. Communication Status
High-level metadata only (call count, duration, contact label where authorized). Call audio and private message bodies are never recorded or intercepted.

## 15. Alerts and Parent Guidance
- **Device Alerts**: Low battery, device offline, device reconnected, location stale, security/safety events.
- **Parent Guidance**: Short guidance messages sent by parent (*"Drink water"*).

## 16. Convocation — Independent Feature
> [!IMPORTANT]
> Convocation is an independent feature. Changes to Convocation must not modify the behavior of Battery, Network, Location, Usage, Device Health, History, Live Activity, or other modules.

- **16.1 Parent Convocation**: Retains complete persistent message history.
- **16.2 Child Convocation Menu**: Top-right three-dot Options menu contains **one single On/Off toggle** (never separate "Turn On" and "Turn Off" items).
- **16.3 Child Default View**: Empty notepad-style view (*"No messages to show right now"*).
- **16.5 Stealth Notification**: Parent message triggers generic Child notification: `"Check your battery status"`. Never exposes message text; tap opens Nivya normally without deep-linking.
- **16.6 Unread Parent Messages**: When Child toggles ON, all accumulated unread Parent messages become visible.
- **16.7 Seen Status**: Shown **only** on Parent side. Child does not see *"Seen"*.
- **16.8 Two-Minute Auto-Hide**: Child viewing mode automatically turns OFF after 2 minutes, reverting to the empty state.
- **16.9 One-Hour Expiry**: Viewed messages permanently expire from Child-side visibility after 1 hour (Parent history unaffected).
- **16.11 Child → Parent**: Child sent messages disappear immediately from Child UI upon send, while Parent retains them in full history.

## 17. Operational Specifications & Technical Enhancements
### 41. Android Special Permission Fallback UX
- **Usage Access**: Educational explanation → Settings Intent (`Settings.ACTION_USAGE_ACCESS_SETTINGS`) → return → verification → state handling (`Granted`, `Required`, `Try Again`).
- **Notification Listener**: Clear explanation → Settings Intent → verification.
- **Fallback states**: `Required`, `Disabled`, `Unavailable`, `Checking`. Never fabricate information.

### 42. Convocation Timer Enforcement When App Is Killed
Authoritative server-side expiration (`view_started_at` + 120s) combined with Android lifecycle-aware WorkManager / AlarmManager local enforcement.

### 43. Telemetry Scaling Strategy
- Maintain latest state in current-state tables (`device_status`, `battery_status`, `network_status`, `locations`) for instant dashboard reads.
- 0–7 days: Raw operational telemetry.
- 7+ days: Aggregated into hourly/daily summaries.
- Configured retention: Automated archival/purging of stale raw records.
- Indexed on `(device_id, family_id, timestamp)`.

### 44. Distribution & Google Play Compliance
Initial build targeted for personal/private family use. Structured with clean permissions and disclosures for future Play Store policy review.

### 45. Error Tracking & Monitoring
Crashlytics/Sentry integration hooks. Excludes sensitive data (passwords, tokens, message bodies).

### 46. Updates & Remote Configuration
Support for minimum client version enforcement and remote threshold configurations.

### 47. React Web Dashboard Real-Time Behavior
Persistent authenticated STOMP over WebSocket connection with automatic bounded backoff reconnection and snapshot state recovery.
