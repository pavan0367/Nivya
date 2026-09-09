import Foundation

public struct APIEndpoint {
    public static var baseURL = "http://localhost:8080/api/v1"

    public static func url(for path: String) -> URL {
        let cleanPath = path.hasPrefix("/") ? String(path.dropFirst()) : path
        return URL(string: "\(baseURL)/\(cleanPath)")!
    }

    public enum Auth {
        public static let login = "/auth/login"
        public static let register = "/auth/register"
        public static let refresh = "/auth/refresh"
        public static let logout = "/auth/logout"
        public static let me = "/auth/me"
    }

    public enum Pairing {
        public static let generateCode = "/pairing/code"
        public static let pair = "/pairing/pair"
        public static let family = "/pairing/family"
        public static let disconnectCode = "/pairing/disconnect/code"
        public static let disconnectVerify = "/pairing/disconnect/verify"
        public static func unpairDevice(id: Int64) -> String {
            return "/pairing/devices/\(id)"
        }
    }

    public enum Convocation {
        public static let childSend = "/convocation/child/send"
        public static let parentSend = "/convocation/parent/send"
        public static let history = "/convocation/history"
        public static let state = "/convocation/state"
        public static func markSeen(id: Int64) -> String {
            return "/convocation/\(id)/seen"
        }
    }

    public enum Telemetry {
        public static let battery = "/telemetry/battery"
        public static let location = "/telemetry/location"
        public static let network = "/telemetry/network"
        public static let screenTime = "/telemetry/screen-time"
        public static let deviceHealth = "/telemetry/device-health"
        public static func summary(deviceId: Int64) -> String {
            return "/telemetry/devices/\(deviceId)/summary"
        }
        public static func alerts(deviceId: Int64) -> String {
            return "/alerts/devices/\(deviceId)"
        }
    }

    public enum Sessions {
        public static let list = "/sessions"
        public static func revoke(id: Int64) -> String {
            return "/sessions/\(id)/revoke"
        }
    }

    public enum Email {
        public static let sendVerify = "/email/verify/send"
        public static let confirmVerify = "/email/verify/confirm"
        public static let preferences = "/email/preferences"
    }
}
