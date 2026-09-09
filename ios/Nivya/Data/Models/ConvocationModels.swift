import Foundation

public struct ChildSendMessageRequest: Codable {
    public let clientMessageId: String?
    public let message: String

    public init(message: String, clientMessageId: String? = UUID().uuidString) {
        self.message = message
        self.clientMessageId = clientMessageId
    }
}

public struct ParentSendMessageRequest: Codable {
    public let message: String
    public let childDeviceId: Int64

    public init(message: String, childDeviceId: Int64) {
        self.message = message
        self.childDeviceId = childDeviceId
    }
}

public struct ConvocationMessageResponse: Codable, Identifiable, Equatable {
    public let id: Int64
    public let clientMessageId: String?
    public let familyId: Int64
    public let senderRole: String
    public let message: String
    public let timestamp: String
    public let isSeen: Bool
    public let seenAt: String?

    public var isFromParent: Bool {
        senderRole.uppercased() == "PARENT"
    }

    public var isCrackEvent: Bool {
        message == "Mom,here"
    }

    public var isFreakEvent: Bool {
        message == "Someone's,here"
    }
}

public struct ConvocationStateResponse: Codable {
    public let isActive: Bool
    public let durationSeconds: Int
    public let remainingSeconds: Int
    public let unreadCount: Int
}
