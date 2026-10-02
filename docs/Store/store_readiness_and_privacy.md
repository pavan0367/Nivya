# Nivya — App Store & Google Play Store Readiness Specification

**Document Version:** 1.0.1  
**Status:** COMPLETE & FROZEN  
**Target Stores:** Google Play Console | Apple App Store Connect

---

## 1. Store Metadata & Identifiers

| Parameter | Google Play Store | Apple App Store |
| :--- | :--- | :--- |
| **App Name** | Nivya: Family Care & Connected Well-being | Nivya: Family Care & Safety |
| **Package / Bundle ID** | `com.nivya` | `com.nivya.ios` |
| **Version Name / Number** | `1.0.1` | `1.0.1` |
| **Version Code / Build** | `2` | `1` |
| **Target SDK / Deployment** | Android 16 (API 36), Min SDK 26 | iOS 17.0+, Xcode 15+ |
| **Content Rating** | Everyone (PEGI 3, ESRB Everyone) | 4+ |
| **Category** | Parenting / Lifestyle / Tools | Lifestyle / Utilities |
| **Support Email** | `breversupport+parent2026@gmail.com` | `breversupport+parent2026@gmail.com` |
| **Privacy Policy URL** | `https://nivya.pages.dev/privacy` | `https://nivya.pages.dev/privacy` |

---

## 2. Privacy Policy & Children's Data Disclosure (COPPA / GDPR-K)

### 2.1 Transparency & Purpose of Collection
Nivya collects device data strictly for family safety and device health management consented to by both parent and child accounts:
- **Location Data:** Collected in foreground and while permitted to display child position on OpenStreetMap. Location is never sold, brokered, or used for advertising.
- **Battery & Device Health:** Battery percentage, storage space, and RAM utilization to alert parents to low battery or hardware degradation.
- **Screen Time & App Usage:** Aggregated application usage statistics to help families establish healthy digital boundaries.
- **No Third-Party Tracking:** Nivya contains 0 third-party ad networks, tracking SDKs, or commercial telemetry brokers.

---

## 3. Account & Data Deletion Compliance (Google Play & Apple Guidelines)

Both stores mandate that users can request account and data deletion both within the application and via a public web link.
- **In-App Account Deletion:** Available under `Settings -> Account -> Delete Account`.
  - Initiates verification gate (`POST /api/v1/users/delete-account/request`).
  - Sends verification email OTP to parent account.
  - Requires confirmation code entry (`POST /api/v1/users/delete-account/confirm`).
- **Public Web Deletion Link:** Dedicated web portal located at `https://nivya.pages.dev/delete-account`.
- **Data Purging Policy:** Account deletion purges active user records, device pairings, telemetry logs, location history, and device sessions within 48 hours.

---

## 4. Release Distribution Artifacts

| Platform | Package Format | Location | Status |
| :--- | :--- | :--- | :---: |
| **Android APK** | Signed APK (`app-release.apk`) | `android/app/build/outputs/apk/release/app-release.apk` | **READY** |
| **Android AAB** | Android App Bundle (`app-release.aab`) | `android/app/build/outputs/bundle/release/app-release.aab` | **READY** |
| **iOS IPA** | iOS App Archive (`.ipa`) | `ios/build/Nivya.ipa` | **PENDING (macOS Required)** |
