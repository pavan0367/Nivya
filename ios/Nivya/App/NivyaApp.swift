import SwiftUI

@main
struct NivyaApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject private var appState = AppState.shared

    var body: some Scene {
        WindowGroup {
            Group {
                switch appState.currentDestination {
                case .splash:
                    SplashScreenView()
                case .login:
                    LoginView()
                case .register:
                    RegisterView()
                case .emailVerification(_):
                    LoginView()
                case .roleSelection:
                    RoleSelectionView()
                case .parentPairing:
                    ParentPairingView()
                case .childPairing:
                    ChildPairingView()
                case .parentDashboard:
                    ParentDashboardView()
                case .childDashboard:
                    ChildDashboardView()
                case .convocation:
                    ConvocationView()
                case .settings:
                    SettingsView()
                }
            }
            .preferredColorScheme(.dark)
        }
    }
}

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Transparent permission check and notification registration
        PermissionManager.shared.checkCurrentPermissions()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        NotificationService.shared.handleDeviceToken(deviceToken)
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("Failed to register for remote notifications: \(error.localizedDescription)")
    }
}
