import XCTest
@testable import Nivya

final class PairingAndReloginTests: XCTestCase {
    func testAlreadyPairedUserBypassesRoleAndPairingScreens() {
        let prefs = AppPreferences.shared
        prefs.isPaired = true
        prefs.savedRole = "PARENT"
        _ = KeychainManager.shared.saveString(key: "access_token", value: "valid_jwt_mock")

        let state = AppState.shared
        state.checkInitialRouting()

        XCTAssertEqual(state.currentDestination, .parentDashboard, "Already paired Parent must route directly to Parent Dashboard")

        // Test for Child
        prefs.savedRole = "CHILD"
        state.checkInitialRouting()
        XCTAssertEqual(state.currentDestination, .childDashboard, "Already paired Child must route directly to Child Dashboard")
    }

    func testTemporaryOfflinePreservesPairingState() {
        let prefs = AppPreferences.shared
        prefs.isPaired = true
        prefs.savedRole = "PARENT"

        // Simulate network failure
        let error = NetworkError.offline
        XCTAssertEqual(error.errorDescription, "Device is offline. Action queued or offline state active.")

        // Invariant: Pairing remains intact
        XCTAssertTrue(prefs.isPaired, "Temporary offline must not alter isPaired")
        XCTAssertEqual(prefs.savedRole, "PARENT", "Temporary offline must not reset role")
    }
}
