import Foundation

public final class AppPreferences: ObservableObject {
    public static let shared = AppPreferences()
    private let defaults = UserDefaults.standard

    private enum Keys {
        static let savedRole = "nivya_saved_role"
        static let isPaired = "nivya_is_paired"
        static let familyId = "nivya_family_id"
        static let userId = "nivya_user_id"
        static let userEmail = "nivya_user_email"
        static let userName = "nivya_user_name"
        static let deviceUuid = "nivya_device_uuid"
        static let activeChildDeviceId = "nivya_active_child_device_id"
        static let pushToken = "nivya_push_token"
    }

    private init() {
        if defaults.string(forKey: Keys.deviceUuid) == nil {
            defaults.set(UUID().uuidString, forKey: Keys.deviceUuid)
        }
    }

    public var savedRole: String? {
        get { defaults.string(forKey: Keys.savedRole) }
        set {
            defaults.set(newValue, forKey: Keys.savedRole)
            objectWillChange.send()
        }
    }

    public var isPaired: Bool {
        get { defaults.bool(forKey: Keys.isPaired) }
        set {
            defaults.set(newValue, forKey: Keys.isPaired)
            objectWillChange.send()
        }
    }

    public var familyId: Int64? {
        get {
            let val = defaults.integer(forKey: Keys.familyId)
            return val > 0 ? Int64(val) : nil
        }
        set {
            if let val = newValue {
                defaults.set(Int(val), forKey: Keys.familyId)
            } else {
                defaults.removeObject(forKey: Keys.familyId)
            }
            objectWillChange.send()
        }
    }

    public var userId: Int64? {
        get {
            let val = defaults.integer(forKey: Keys.userId)
            return val > 0 ? Int64(val) : nil
        }
        set {
            if let val = newValue {
                defaults.set(Int(val), forKey: Keys.userId)
            } else {
                defaults.removeObject(forKey: Keys.userId)
            }
            objectWillChange.send()
        }
    }

    public var userEmail: String? {
        get { defaults.string(forKey: Keys.userEmail) }
        set {
            defaults.set(newValue, forKey: Keys.userEmail)
            objectWillChange.send()
        }
    }

    public var userName: String? {
        get { defaults.string(forKey: Keys.userName) }
        set {
            defaults.set(newValue, forKey: Keys.userName)
            objectWillChange.send()
        }
    }

    public var deviceUuid: String {
        return defaults.string(forKey: Keys.deviceUuid) ?? UUID().uuidString
    }

    public var activeChildDeviceId: Int64? {
        get {
            let val = defaults.integer(forKey: Keys.activeChildDeviceId)
            return val > 0 ? Int64(val) : nil
        }
        set {
            if let val = newValue {
                defaults.set(Int(val), forKey: Keys.activeChildDeviceId)
            } else {
                defaults.removeObject(forKey: Keys.activeChildDeviceId)
            }
            objectWillChange.send()
        }
    }

    public var pushToken: String? {
        get { defaults.string(forKey: Keys.pushToken) }
        set {
            defaults.set(newValue, forKey: Keys.pushToken)
            objectWillChange.send()
        }
    }

    public func clearSession() {
        // Keeps deviceUuid, but clears auth tokens and cached identifiers
        KeychainManager.shared.delete(key: "access_token")
        KeychainManager.shared.delete(key: "refresh_token")
        userId = nil
        userEmail = nil
        userName = nil
        // Invariant: Logout does NOT disconnect pairing or clear saved relationship
    }

    public func clearPairing() {
        isPaired = false
        familyId = nil
        savedRole = nil
        activeChildDeviceId = nil
    }
}
