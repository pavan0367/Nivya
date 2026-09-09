import Foundation

public final class TelemetrySyncService {
    public static let shared = TelemetrySyncService()
    private var syncTimer: Timer?
    private let telemetryRepo = TelemetryRepository.shared
    private let batteryService = BatteryService.shared
    private let networkService = NetworkMonitoringService.shared
    private let locationService = LocationService.shared

    private init() {}

    public func startPeriodicSync() {
        stopPeriodicSync()
        syncOnce()
        syncTimer = Timer.scheduledTimer(withTimeInterval: 30.0, repeats: true) { [weak self] _ in
            self?.syncOnce()
        }
    }

    public func stopPeriodicSync() {
        syncTimer?.invalidate()
        syncTimer = nil
    }

    public func syncOnce() {
        Task {
            // Only Child devices publish telemetry
            guard AppPreferences.shared.savedRole == RoleType.child.rawValue else { return }

            let battery = batteryService.currentBattery
            try? await telemetryRepo.syncBattery(
                level: battery.levelPercent,
                isCharging: battery.isCharging
            )

            let network = networkService
            try? await telemetryRepo.syncNetwork(
                networkType: network.connectionType,
                isConnected: network.isConnected
            )

            if let loc = locationService.lastLocation {
                try? await telemetryRepo.syncLocation(
                    latitude: loc.coordinate.latitude,
                    longitude: loc.coordinate.longitude,
                    accuracy: loc.horizontalAccuracy,
                    speed: loc.speed >= 0 ? loc.speed : nil
                )
            }
        }
    }
}
