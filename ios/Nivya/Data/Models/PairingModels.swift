import Foundation

public struct GeneratePairingCodeResponse: Codable {
    public let pairingCode: String
    public let expiresAt: String
    public let ttlMinutes: Int
}

public struct PairRequest: Codable {
    public let pairingCode: String
    public let childDeviceName: String
    public let platform: String
    public let deviceUuid: String
    public let appVersion: String

    public init(
        pairingCode: String,
        childDeviceName: String,
        platform: String = "IOS",
        deviceUuid: String,
        appVersion: String = "1.0.0"
    ) {
        self.pairingCode = pairingCode
        self.childDeviceName = childDeviceName
        self.platform = platform
        self.deviceUuid = deviceUuid
        self.appVersion = appVersion
    }
}

public struct ChildDeviceDto: Codable, Identifiable {
    public let id: Int64
    public let deviceUuid: String
    public let deviceName: String
    public let platform: String
    public let pushToken: String?
    public let status: String
    public let lastSeenAt: String?
    public let online: Bool?
}

public struct FamilyResponse: Codable {
    public let familyId: Int64?
    public let parent: UserResponse?
    public let children: [UserResponse]?
    public let devices: [ChildDeviceDto]?
    public let isPaired: Bool
}

public struct GenerateDisconnectCodeResponse: Codable {
    public let code: String
    public let expiresAt: String
    public let ttlMinutes: Int
}

public struct VerifyDisconnectCodeRequest: Codable {
    public let code: String

    public init(code: String) {
        self.code = code
    }
}
