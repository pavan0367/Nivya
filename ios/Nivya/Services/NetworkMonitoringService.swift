import Foundation
import Network

public final class NetworkMonitoringService: ObservableObject {
    public static let shared = NetworkMonitoringService()

    @Published public private(set) var isConnected: Bool = true
    @Published public private(set) var connectionType: String = "Wi-Fi"
    @Published public private(set) var isExpensive: Bool = false
    @Published public private(set) var isConstrained: Bool = false
    public let signalStrengthDbmDisplay: String = "Unavailable" // iOS does not expose cellular/wifi dBm

    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.nivya.ios.networkmonitor")

    private init() {
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                self?.isConnected = (path.status == .satisfied)
                self?.isExpensive = path.isExpensive
                self?.isConstrained = path.isConstrained

                if path.usesInterfaceType(.wifi) {
                    self?.connectionType = "Wi-Fi"
                } else if path.usesInterfaceType(.cellular) {
                    self?.connectionType = "Cellular"
                } else if path.usesInterfaceType(.wiredEthernet) {
                    self?.connectionType = "Ethernet"
                } else {
                    self?.connectionType = "Unavailable"
                }
            }
        }
        monitor.start(queue: queue)
    }
}
