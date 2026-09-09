import Foundation

public enum NetworkError: LocalizedError {
    case invalidURL
    case noData
    case decodingError(Error)
    case serverError(code: Int, message: String)
    case unauthorized
    case forbidden
    case offline
    case unknown(Error)

    public var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid request URL."
        case .noData:
            return "No response data received from server."
        case .decodingError(let err):
            return "Failed to parse response: \(err.localizedDescription)"
        case .serverError(_, let msg):
            return msg
        case .unauthorized:
            return "Session expired. Please sign in again."
        case .forbidden:
            return "You do not have permission to perform this action."
        case .offline:
            return "Device is offline. Action queued or offline state active."
        case .unknown(let err):
            return err.localizedDescription
        }
    }
}
