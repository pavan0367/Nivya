# Nivya — iOS Architecture & Platform Readiness Specification

**Platform:** iOS 17.0+  
**Language:** Swift 5.9  
**UI Framework:** SwiftUI  
**Architecture:** MVVM + AppState Coordinator  
**Status:** SOURCE READY | BUILD PENDING EXTERNAL DEPENDENCY (macOS/Xcode Environment)

---

## 1. Project Structure

The iOS application is located in `ios/` and structured as follows:

```text
ios/
├── Nivya.xcodeproj/            # Xcode project bundle
├── Nivya/
│   ├── App/                   # NivyaApp entry, AppState, LaunchScreen
│   ├── Core/
│   │   ├── Network/           # APIEndpoint, NetworkClient, WebSocketClient
│   │   ├── Permissions/       # PermissionManager (Location, Notifications)
│   │   ├── Security/          # KeychainManager (Secure token persistence)
│   │   ├── Storage/           # AppPreferences (UserDefaults wrapper)
│   │   └── Theme/             # NivyaColors, UI tokens
│   ├── Data/
│   │   ├── Models/            # Auth, Session, Pairing, Telemetry, Convocation
│   │   └── Repositories/      # AuthRepository, TelemetryRepository, etc.
│   ├── Features/
│   │   ├── Auth/              # LoginView, RegisterView
│   │   ├── Role/              # RoleSelectionView (Strict Role Isolation)
│   │   ├── Pairing/           # ChildPairingView, ParentPairingView
│   │   ├── ChildDashboard/    # ChildDashboardView (Real-time telemetry, CRACK/FREAK)
│   │   ├── ParentDashboard/   # ParentDashboardView (Family devices, Safe zones)
│   │   ├── Convocation/       # ConvocationView (Guidance messaging)
│   │   ├── Settings/          # SettingsView, ProtectedDisconnectView
│   │   └── Splash/            # SplashScreenView (Session auto-restoration)
│   ├── Services/              # BatteryService, LocationService, TelemetrySync
│   └── Resources/             # Assets.xcassets, Info.plist
└── NivyaTests/                # XCTest unit and integration test suite
```

---

## 2. Security & Network Integration

- **Token Storage:** Access and refresh tokens are persisted in the iOS Keychain with `kSecAttrAccessibleAfterFirstUnlock`.
- **WebSocket Transport:** Built on `URLSessionWebSocketTask` connecting to `wss://nivya-blbf.onrender.com/ws`.
- **Background Telemetry:** Utilizes `CLLocationManager` with `desiredAccuracy = kCLLocationAccuracyBest` and `pausesLocationUpdatesAutomatically = true`.

---

## 3. Platform Readiness & External Dependencies

| Step | Requirement | Status | Classification |
| :--- | :--- | :---: | :--- |
| **Swift Source Code** | Complete SwiftUI codebase & view models | COMPLETE | DONE |
| **Xcode Project** | `Nivya.xcodeproj` with valid build phases | COMPLETE | DONE |
| **Assets & Icons** | AppIcon, LaunchImage, and Logos in Assets.xcassets | COMPLETE | DONE |
| **Unit Test Suite** | 6 test suites covering auth, pairing, and CRACK/FREAK | COMPLETE | DONE |
| **Compilation & Archive** | macOS workstation with Xcode 15+ installed | PENDING | **PENDING-EXTERNAL-DEPENDENCY** |
| **Apple Developer Signing** | Paid Apple Developer account for Provisioning Profile | PENDING | **PENDING-EXTERNAL-DEPENDENCY** |
| **TestFlight Distribution** | App Store Connect submission | PENDING | **PENDING-EXTERNAL-DEPENDENCY** |
