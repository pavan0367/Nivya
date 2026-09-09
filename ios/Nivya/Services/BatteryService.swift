import Foundation
import UIKit

public struct iOSBatteryInfo {
    public let levelPercent: Int
    public let isCharging: Bool
    public let statusDescription: String
    public let healthMetric: String = "Unavailable" // iOS does not expose raw battery health to standard apps
}

public final class BatteryService: ObservableObject {
    public static let shared = BatteryService()

    @Published public private(set) var currentBattery: iOSBatteryInfo

    private init() {
        UIDevice.current.isBatteryMonitoringEnabled = true
        self.currentBattery = BatteryService.readCurrentBattery()
        setupObserver()
    }

    private static func readCurrentBattery() -> iOSBatteryInfo {
        let rawLevel = UIDevice.current.batteryLevel
        let level = rawLevel < 0 ? 100 : Int(rawLevel * 100)
        let state = UIDevice.current.batteryState
        let isCharging = (state == .charging || state == .full)
        let desc: String
        switch state {
        case .charging: desc = "Charging"
        case .full: desc = "Full (Connected)"
        case .unplugged: desc = "Unplugged"
        case .unknown: desc = "Unavailable"
        @unknown default: desc = "Unavailable"
        }
        return iOSBatteryInfo(levelPercent: level, isCharging: isCharging, statusDescription: desc)
    }

    private func setupObserver() {
        NotificationCenter.default.addObserver(
            forName: UIDevice.batteryLevelDidChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.currentBattery = BatteryService.readCurrentBattery()
        }

        NotificationCenter.default.addObserver(
            forName: UIDevice.batteryStateDidChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.currentBattery = BatteryService.readCurrentBattery()
        }
    }
}
