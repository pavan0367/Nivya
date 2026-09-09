import Foundation

public final class ConvocationRepository {
    public static let shared = ConvocationRepository()
    private let client = NetworkClient.shared

    private init() {}

    public func childSendMessage(message: String) async throws -> ConvocationMessageResponse {
        let req = ChildSendMessageRequest(message: message, clientMessageId: UUID().uuidString)
        let response: ConvocationMessageResponse = try await client.post(
            path: APIEndpoint.Convocation.childSend,
            body: req,
            requiresAuth: true
        )
        // Invariant: Child does NOT retain sent message history locally
        return response
    }

    public func parentSendMessage(message: String, childDeviceId: Int64) async throws -> ConvocationMessageResponse {
        let req = ParentSendMessageRequest(message: message, childDeviceId: childDeviceId)
        return try await client.post(
            path: APIEndpoint.Convocation.parentSend,
            body: req,
            requiresAuth: true
        )
    }

    public func getHistory() async throws -> [ConvocationMessageResponse] {
        return try await client.get(
            path: APIEndpoint.Convocation.history,
            requiresAuth: true
        )
    }

    public func getState() async throws -> ConvocationStateResponse {
        return try await client.get(
            path: APIEndpoint.Convocation.state,
            requiresAuth: true
        )
    }

    public func markSeen(messageId: Int64) async throws {
        let _: EmptyResponse = try await client.postEmpty(
            path: APIEndpoint.Convocation.markSeen(id: messageId),
            requiresAuth: true
        )
    }
}
