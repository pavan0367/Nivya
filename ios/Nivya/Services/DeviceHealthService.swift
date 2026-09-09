import Foundation
import UIKit

public struct iOSDeviceHealthMetrics {
    public let deviceModel: String
    public let systemVersion: String
    public let storageTotalBytes: Int64?
    public let storageAvailableBytes: Int64?
    public let thermalState: String
    public let cpuTempMetric: String = "Unavailable"
    public let hardwareHealthMetric: String = "Unavailable"
}

public final class DeviceHealthService: ObservableObject {
    public static let shared = DeviceHealthService()

    @Published public private(set) var metrics: iOSDeviceHealthMetrics

    private init() {
        self.metrics = DeviceHealthService.gatherMetrics()
    }

    public static func gatherMetrics() -> iOSDeviceHealthMetrics {
        let model = UIDevice.current.model
        let version = "\(UIDevice.current.systemName) \(UIDevice.current.systemVersion)"

        var totalDisk: Int64?
        var freeDisk: Int64?

        if let home = try? URL(fileURLWithPath: NSHomeDirectory()).resourceValues(forKeys: [.volumeTotalCapacityKey, .volumeAvailableCapacityForImportantUsageKey]) {
            if let total = home.volumeTotalCapacity {
                totalDisk = Int64(total)
            }
            if let free = home.volumeAvailableCapacityForImportantUsage {
                freeDisk = free
            }
        }

        let thermal: String
        switch ProcessInfo.processInfo.thermalState {
        case .nominal: thermal = "Nominal (Cool)"
        case .fair: thermal = "Fair"
        case .serious: thermal = "Serious (Throttling)"
        case .critical: thermal = "Critical"
        @unknown default: thermal = "Unavailable"
        }

        return iOSDeviceHealthMetrics(
            deviceModel: model,
            systemVersion: version,
            storageTotalBytes: totalDisk,
            storageAvailableBytes: freeDisk,
            thermalState: thermal
        )
    }
}
