import XCTest
@testable import Nivya

final class ChildDashboardOrderTests: XCTestCase {
    func testStrictNineItemOrderDefinition() {
        let expectedOrder: [String] = [
            "Battery",
            "Screen Time",
            "CRACK",
            "FREAK",
            "Network",
            "Network Quality",
            "Location",
            "Device Health",
            "Alerts"
        ]

        XCTAssertEqual(expectedOrder.count, 9, "Child dashboard must contain exactly 9 top-level items")
        XCTAssertEqual(expectedOrder[0], "Battery")
        XCTAssertEqual(expectedOrder[1], "Screen Time")
        XCTAssertEqual(expectedOrder[2], "CRACK")
        XCTAssertEqual(expectedOrder[3], "FREAK")
        XCTAssertEqual(expectedOrder[4], "Network")
        XCTAssertEqual(expectedOrder[5], "Network Quality")
        XCTAssertEqual(expectedOrder[6], "Location")
        XCTAssertEqual(expectedOrder[7], "Device Health")
        XCTAssertEqual(expectedOrder[8], "Alerts")
    }
}
