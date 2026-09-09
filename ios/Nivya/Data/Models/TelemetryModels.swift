import Foundation

public struct BatteryTelemetryResponse: Codable {
    public let deviceId: Int64
    public let batteryLevel: Int
    public let isCharging: Bool
    public let powerSaveMode: Bool?
    public let temperatureCelsius: Double?
    public let voltageMv: Int?
    public let health: String?
    public let timestamp: String?
}

public struct AppUsageItem: Codable, Identifiable {
    public var id: String { packageName }
    public let packageName: String
    public let appName: String
    public let usageMinutes: Int
    public let category: String?
}

public struct ScreenTimeResponse: Codable {
    public let deviceId: Int64
    public let totalUsageMinutes: Int
    public let screenOnTimeMinutes: Int?
    public let unlockCount: Int?
    public let apps: [AppUsageItem]?
}

public struct NetworkTelemetryResponse: Codable {
    public let deviceId: Int64
    public let networkType: String
    public let isConnected: Bool
    public let signalStrengthDbm: Int?
    public let latencyMs: Int?
    public let wifiSsid: String?
    public let cellularCarrier: String?
    public let quality: String?
    public let timestamp: String?
}

public struct LocationTelemetryResponse: Codable {
    public let deviceId: Int64
    public let latitude: Double
    public let longitude: Double
    public let accuracyMeters: Double?
    public let speedMps: Double?
    public let altitudeMeters: Double?
    public let address: String?
    public let timestamp: String?
}

public struct DeviceHealthResponse: Codable {
    public let deviceId: Int64
    public let storageUsedBytes: Int64?
    public let storageTotalBytes: Int64?
    public let memoryUsedBytes: Int64?
    public let memoryTotalBytes: Int64?
    public let batteryHealth: String?
    public let cpuUsagePercent: Double?
    public let thermalState: String?
    public let osVersion: String?
    public let deviceModel: String?
    public let timestamp: String?
}

public struct AlertItemResponse: Codable, Identifiable {
    public let id: Int64
    public let deviceId: Int64
    public let alertType: String
    public let severity: String
    public let title: String
    public let message: String
    public let timestamp: String
    public let isRead: Bool
}

public struct ChildTelemetrySummary: Codable {
    public let deviceId: Int64
    public let deviceName: String
    public let platform: String
    public let isOnline: Bool
    public let battery: BatteryTelemetryResponse?
    public let network: NetworkTelemetryResponse?
    public let location: LocationTelemetryResponse?
    public let screenTime: ScreenTimeResponse?
    public let deviceHealth: DeviceHealthResponse?
    public let unreadAlertsCount: Int?
    public let lastSyncAt: String?
}
