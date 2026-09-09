import Foundation
import CoreLocation
import UserNotifications
import UIKit

public enum PermissionStatus: String {
    case granted
    case denied
    case restricted
    case notDetermined
    case unavailable
}

public final class PermissionManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    public static let shared = PermissionManager()

    @Published public var locationStatus: PermissionStatus = .notDetermined
    @Published public var notificationStatus: PermissionStatus = .notDetermined
    @Published public var screenTimeStatus: PermissionStatus = .notDetermined

    private let locationManager = CLLocationManager()

    private override init() {
        super.init()
        locationManager.delegate = self
        checkCurrentPermissions()
    }

    public func checkCurrentPermissions() {
        checkLocationPermission()
        checkNotificationPermission()
        checkScreenTimePermission()
    }

    // MARK: - Location
    public func requestLocationPermission() {
        locationManager.requestAlwaysAuthorization()
    }

    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        checkLocationPermission()
    }

    private func checkLocationPermission() {
        switch locationManager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            locationStatus = .granted
        case .denied:
            locationStatus = .denied
        case .restricted:
            locationStatus = .restricted
        case .notDetermined:
            locationStatus = .notDetermined
        @unknown default:
            locationStatus = .unavailable
        }
    }

    // MARK: - Notifications
    public func requestNotificationPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { [weak self] granted, _ in
            DispatchQueue.main.async {
                self?.notificationStatus = granted ? .granted : .denied
                if granted {
                    UIApplication.shared.registerForRemoteNotifications()
                }
            }
        }
    }

    private func checkNotificationPermission() {
        UNUserNotificationCenter.current().getNotificationSettings { [weak self] settings in
            DispatchQueue.main.async {
                switch settings.authorizationStatus {
                case .authorized, .provisional, .ephemeral:
                    self?.notificationStatus = .granted
                case .denied:
                    self?.notificationStatus = .denied
                case .notDetermined:
                    self?.notificationStatus = .notDetermined
                @unknown default:
                    self?.notificationStatus = .unavailable
                }
            }
        }
    }

    // MARK: - Screen Time / Family Controls
    public func requestScreenTimePermission() {
        // Handled via FamilyControls framework when entitled
        // Graceful fallback for non-managed devices
        screenTimeStatus = .unavailable
    }

    private func checkScreenTimePermission() {
        // Without special MDM/FamilyControls entitlement, transparently report unavailable
        screenTimeStatus = .unavailable
    }

    public func openAppSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }
}
