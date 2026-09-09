import Foundation

public enum SessionStatus: String, Codable {
    case active = "ACTIVE"
    case loggedOut = "LOGGED_OUT"
    case revoked = "REVOKED"
    case expired = "EXPIRED"
}

public struct DeviceSessionResponse: Codable, Identifiable {
    public let id: Int64
    public let sessionToken: String
    public let deviceId: String?
    public let deviceName: String?
    public let platform: String?
    public let appVersion: String?
    public let ipAddress: String?
    public let approximateLocation: String?
    public let status: SessionStatus
    public let createdAt: String
    public let lastActiveAt: String?
    public let loggedOutAt: String?
}

public struct EmailPreferencesResponse: Codable {
    public let userId: Int64?
    public let email: String?
    public let emailVerified: Bool?
    public var loginAlertsEnabled: Bool
    public var newDeviceAlertsEnabled: Bool
    public var appUpdateAlertsEnabled: Bool
    public let updatedAt: String?
}

public struct UpdateEmailPreferencesRequest: Codable {
    public let loginAlertsEnabled: Bool
    public let newDeviceAlertsEnabled: Bool
    public let appUpdateAlertsEnabled: Bool

    public init(loginAlertsEnabled: Bool, newDeviceAlertsEnabled: Bool, appUpdateAlertsEnabled: Bool) {
        self.loginAlertsEnabled = loginAlertsEnabled
        self.newDeviceAlertsEnabled = newDeviceAlertsEnabled
        self.appUpdateAlertsEnabled = appUpdateAlertsEnabled
    }
}
