import XCTest
@testable import Nivya

final class CrackFreakTests: XCTestCase {
    func testCrackExactMessagePayload() {
        let req = ChildSendMessageRequest(message: "Mom,here")
        XCTAssertEqual(req.message, "Mom,here", "CRACK must dispatch exact string 'Mom,here'")
        XCTAssertNotNil(req.clientMessageId, "CRACK note must contain clientMessageId for idempotency guard")
    }

    func testFreakExactMessagePayload() {
        let req = ChildSendMessageRequest(message: "Someone's,here")
        XCTAssertEqual(req.message, "Someone's,here", "FREAK must dispatch exact string 'Someone's,here'")
        XCTAssertNotNil(req.clientMessageId, "FREAK note must contain clientMessageId for idempotency guard")
    }

    func testConvocationMessageIdentifiesSpecialEvents() {
        let crackMsg = ConvocationMessageResponse(
            id: 1,
            clientMessageId: UUID().uuidString,
            familyId: 10,
            senderRole: "CHILD",
            message: "Mom,here",
            timestamp: "2026-09-09T22:00:00Z",
            isSeen: false,
            seenAt: nil
        )
        XCTAssertTrue(crackMsg.isCrackEvent, "Should be recognized as CRACK event")
        XCTAssertFalse(crackMsg.isFreakEvent)

        let freakMsg = ConvocationMessageResponse(
            id: 2,
            clientMessageId: UUID().uuidString,
            familyId: 10,
            senderRole: "CHILD",
            message: "Someone's,here",
            timestamp: "2026-09-09T22:01:00Z",
            isSeen: true,
            seenAt: "2026-09-09T22:02:00Z"
        )
        XCTAssertTrue(freakMsg.isFreakEvent, "Should be recognized as FREAK event")
        XCTAssertFalse(freakMsg.isCrackEvent)
    }
}
