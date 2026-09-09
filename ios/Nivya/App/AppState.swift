import Foundation
import SwiftUI

public enum AppDestination: Hashable {
    case splash
    case login
    case register
    case emailVerification(email: String)
    case roleSelection
    case parentPairing
    case childPairing
    case parentDashboard
    case childDashboard
    case convocation
    case settings
}

public final class AppState: ObservableObject {
    public static let shared = AppState()

    @Published public var currentDestination: AppDestination = .splash
    @Published public var isAuthenticated: Bool = false
    @Published public var errorMessage: String? = nil

    private let prefs = AppPreferences.shared

    private init() {
        checkInitialRouting()
    }

    /// Evaluates current credentials and pairing state to establish root destination.
    /// Strict Invariant: If user is authenticated and already paired, directly opens
    /// the correct Dashboard (Parent or Child), completely bypassing Role Selection & Pairing screens.
    public func checkInitialRouting() {
        let hasToken = AuthRepository.shared.isAuthenticated

        if hasToken {
            self.isAuthenticated = true
            if prefs.isPaired {
                // Already paired: Direct dashboard routing
                if prefs.savedRole == RoleType.child.rawValue {
                    self.currentDestination = .childDashboard
                    TelemetrySyncService.shared.startPeriodicSync()
                } else {
                    self.currentDestination = .parentDashboard
                }
                WebSocketClient.shared.connect()
            } else if prefs.savedRole != nil {
                // Role selected but not yet paired
                if prefs.savedRole == RoleType.child.rawValue {
                    self.currentDestination = .childPairing
                } else {
                    self.currentDestination = .parentPairing
                }
            } else {
                // First-time setup incomplete
                self.currentDestination = .roleSelection
            }
        } else {
            self.isAuthenticated = false
            self.currentDestination = .login
        }
    }

    /// Called immediately upon successful login.
    public func handleLoginSuccess() {
        self.isAuthenticated = true
        // If device has cached pairing relationship, jump directly to dashboard
        if prefs.isPaired {
            if prefs.savedRole == RoleType.child.rawValue {
                self.currentDestination = .childDashboard
                TelemetrySyncService.shared.startPeriodicSync()
            } else {
                self.currentDestination = .parentDashboard
            }
            WebSocketClient.shared.connect()
        } else {
            // Unpaired user routes to role selection
            self.currentDestination = .roleSelection
        }
    }

    /// Called on logout: preserves isPaired and savedRole so relogin remembers the role & pairing!
    public func handleLogout() {
        Task {
            await AuthRepository.shared.logout()
            DispatchQueue.main.async {
                self.isAuthenticated = false
                self.currentDestination = .login
            }
        }
    }

    /// Called on intentional disconnect verification: unlinks pairing and resets to role selection.
    public func handleDisconnected() {
        prefs.clearPairing()
        TelemetrySyncService.shared.stopPeriodicSync()
        self.currentDestination = .roleSelection
    }
}
