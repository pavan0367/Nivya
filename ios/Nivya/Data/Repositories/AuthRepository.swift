import Foundation
import UIKit

public final class AuthRepository {
    public static let shared = AuthRepository()
    private let client = NetworkClient.shared
    private let prefs = AppPreferences.shared

    private init() {}

    public func login(email: String, password: String) async throws -> AuthResponseData {
        let deviceName = UIDevice.current.name
        let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"

        let req = LoginRequest(
            email: email,
            password: password,
            deviceId: prefs.deviceUuid,
            deviceName: deviceName,
            platform: "IOS",
            appVersion: appVersion,
            pushToken: prefs.pushToken
        )

        let authData: AuthResponseData = try await client.post(
            path: APIEndpoint.Auth.login,
            body: req,
            requiresAuth: false
        )

        // Securely store credentials
        _ = KeychainManager.shared.saveString(key: "access_token", value: authData.accessToken)
        _ = KeychainManager.shared.saveString(key: "refresh_token", value: authData.refreshToken)

        prefs.userId = authData.user.id
        prefs.userEmail = authData.user.email
        prefs.userName = authData.user.name
        prefs.savedRole = authData.user.role.rawValue

        return authData
    }

    public func register(name: String, email: String, password: String, role: RoleType) async throws -> UserResponse {
        let req = RegisterRequest(name: name, email: email, password: password, role: role)
        return try await client.post(path: APIEndpoint.Auth.register, body: req, requiresAuth: false)
    }

    public func logout() async {
        do {
            let _: EmptyResponse = try await client.postEmpty(path: APIEndpoint.Auth.logout, requiresAuth: true)
        } catch {
            print("Server logout error: \(error.localizedDescription)")
        }
        // Local state cleanup: preserves isPaired and savedRole across logouts!
        prefs.clearSession()
        WebSocketClient.shared.disconnect()
    }

    public func getMe() async throws -> UserResponse {
        return try await client.get(path: APIEndpoint.Auth.me, requiresAuth: true)
    }

    public var isAuthenticated: Bool {
        return KeychainManager.shared.loadString(key: "access_token") != nil
    }
}

public struct EmptyResponse: Codable {}
