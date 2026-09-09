import Foundation

public final class TelemetryRepository {
    public static let shared = TelemetryRepository()
    private let client = NetworkClient.shared

    private init() {}

    public func syncBattery(level: Int, isCharging: Bool, powerSaveMode: Bool = false) async throws {
        struct BatterySyncReq: Codable {
            let batteryLevel: Int
            let isCharging: Bool
            let powerSaveMode: Bool
        }
        let req = BatterySyncReq(batteryLevel: level, isCharging: isCharging, powerSaveMode: powerSaveMode)
        let _: EmptyResponse = try await client.post(path: APIEndpoint.Telemetry.battery, body: req)
    }

    public func syncLocation(latitude: Double, longitude: Double, accuracy: Double? = nil, speed: Double? = nil) async throws {
        struct LocationSyncReq: Codable {
            let latitude: Double
            let longitude: Double
            let accuracyMeters: Double?
            let speedMps: Double?
        }
        let req = LocationSyncReq(latitude: latitude, longitude: longitude, accuracyMeters: accuracy, speedMps: speed)
        let _: EmptyResponse = try await client.post(path: APIEndpoint.Telemetry.location, body: req)
    }

    public func syncNetwork(networkType: String, isConnected: Bool) async throws {
        struct NetworkSyncReq: Codable {
            let networkType: String
            let isConnected: Bool
        }
        let req = NetworkSyncReq(networkType: networkType, isConnected: isConnected)
        let _: EmptyResponse = try await client.post(path: APIEndpoint.Telemetry.network, body: req)
    }

    /// Fetches complete telemetry summary for a child device.
    /// Critical Cross-Platform Rule: When Child is Android, this contains the full Android telemetry
    /// (Battery, Screen Time apps, Network latency/dBm, GPS, Device Health, and Alerts).
    public func getDeviceSummary(deviceId: Int64) async throws -> ChildTelemetrySummary {
        return try await client.get(path: APIEndpoint.Telemetry.summary(deviceId: deviceId))
    }

    public func getAlerts(deviceId: Int64) async throws -> [AlertItemResponse] {
        return try await client.get(path: APIEndpoint.Telemetry.alerts(deviceId: deviceId))
    }
}
