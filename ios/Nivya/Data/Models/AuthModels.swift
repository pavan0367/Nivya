import Foundation

public struct ApiResponse<T: Codable>: Codable {
    public let success: Bool
    public let message: String?
    public let data: T?
    public let timestamp: String?
    public let error: String?
    public let status: Int?
}

public enum RoleType: String, Codable, CaseIterable {
    case parent = "PARENT"
    case child = "CHILD"
}

public struct LoginRequest: Codable {
    public let email: String
    public let password: String
    public let deviceId: String?
    public let deviceName: String?
    public let platform: String
    public let appVersion: String?
    public let pushToken: String?

    public init(
        email: String,
        password: String,
        deviceId: String? = nil,
        deviceName: String? = nil,
        platform: String = "IOS",
        appVersion: String? = "1.0.0",
        pushToken: String? = nil
    ) {
        self.email = email
        self.password = password
        self.deviceId = deviceId
        self.deviceName = deviceName
        self.platform = platform
        self.appVersion = appVersion
        self.pushToken = pushToken
    }
}

public struct RegisterRequest: Codable {
    public let name: String
    public let email: String
    public let password: String
    public let role: RoleType

    public init(name: String, email: String, password: String, role: RoleType) {
        self.name = name
        self.email = email
        self.password = password
        self.role = role
    }
}

public struct UserResponse: Codable, Identifiable {
    public let id: Int64
    public let uuid: String?
    public let name: String
    public let email: String
    public let role: RoleType
    public let status: String?
}

public struct AuthResponseData: Codable {
    public let accessToken: String
    public let refreshToken: String
    public let tokenType: String
    public let expiresIn: Int64
    public let user: UserResponse
}
