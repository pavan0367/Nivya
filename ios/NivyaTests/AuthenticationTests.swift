import XCTest
@testable import Nivya

final class AuthenticationTests: XCTestCase {
    func testKeychainTokenPersistence() {
        let testToken = "test_jwt_access_token_value_xyz"
        let saved = KeychainManager.shared.saveString(key: "test_token", value: testToken)
        XCTAssertTrue(saved, "Token should be stored successfully in Keychain")

        let loaded = KeychainManager.shared.loadString(key: "test_token")
        XCTAssertEqual(loaded, testToken, "Loaded token must match saved token")

        KeychainManager.shared.delete(key: "test_token")
        let deleted = KeychainManager.shared.loadString(key: "test_token")
        XCTAssertNil(deleted, "Deleted token must return nil")
    }

    func testAppPreferencesRoleAndIdPersistence() {
        let prefs = AppPreferences.shared
        prefs.savedRole = "PARENT"
        XCTAssertEqual(prefs.savedRole, "PARENT")

        prefs.userId = 12345
        XCTAssertEqual(prefs.userId, 12345)

        prefs.clearSession()
        XCTAssertNil(prefs.userId, "UserId should be cleared on session clear")
        XCTAssertEqual(prefs.savedRole, "PARENT", "Logout must NOT reset saved role")
    }
}
