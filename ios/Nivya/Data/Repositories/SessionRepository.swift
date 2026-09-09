import Foundation

public final class SessionRepository {
    public static let shared = SessionRepository()
    private let client = NetworkClient.shared

    private init() {}

    public func getSessions() async throws -> [DeviceSessionResponse] {
        return try await client.get(path: APIEndpoint.Sessions.list)
    }

    public func revokeSession(id: Int64) async throws {
        let _: EmptyResponse = try await client.postEmpty(path: APIEndpoint.Sessions.revoke(id: id))
    }

    public func getEmailPreferences() async throws -> EmailPreferencesResponse {
        return try await client.get(path: APIEndpoint.Email.preferences)
    }

    public func updateEmailPreferences(prefs: UpdateEmailPreferencesRequest) async throws -> EmailPreferencesResponse {
        return try await client.put(path: APIEndpoint.Email.preferences, body: prefs)
    }
}
