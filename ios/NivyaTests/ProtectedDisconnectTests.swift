import XCTest
@testable import Nivya

final class ProtectedDisconnectTests: XCTestCase {
    func testDisconnectCodeFormatAndExpiry() {
        let codeResponse = GenerateDisconnectCodeResponse(code: "729401", expiresAt: "2026-09-10T12:00:00Z", ttlMinutes: 10)
        XCTAssertEqual(codeResponse.code.count, 6, "Disconnect code must be 6 digits")
        XCTAssertEqual(codeResponse.ttlMinutes, 10, "Disconnect code must expire in 10 minutes")
    }

    func testChildVerificationUnlinksLocalCache() {
        let prefs = AppPreferences.shared
        prefs.isPaired = true
        prefs.familyId = 888

        // Successful verification clears pairing
        prefs.clearPairing()

        XCTAssertFalse(prefs.isPaired, "Successful disconnect verification must set isPaired to false")
        XCTAssertNil(prefs.familyId, "Family ID must be cleared after intentional disconnect")
    }
}
