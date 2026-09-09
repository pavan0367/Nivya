import Foundation
import CoreLocation

public final class LocationService: NSObject, ObservableObject, CLLocationManagerDelegate {
    public static let shared = LocationService()

    @Published public private(set) var lastLocation: CLLocation?
    private let manager = CLLocationManager()

    private override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        manager.distanceFilter = 50
    }

    public func startUpdating() {
        if CLLocationManager.locationServicesEnabled() {
            manager.startUpdatingLocation()
        }
    }

    public func stopUpdating() {
        manager.stopUpdatingLocation()
    }

    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let loc = locations.last else { return }
        self.lastLocation = loc
    }

    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("CoreLocation error: \(error.localizedDescription)")
    }
}
