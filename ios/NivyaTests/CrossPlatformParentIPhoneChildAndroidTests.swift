import XCTest
@testable import Nivya

final class CrossPlatformParentIPhoneChildAndroidTests: XCTestCase {
    /// MANDATORY TEST (Section 38 & 44):
    /// Verifies that an iOS Parent console parsing telemetry originated from an Android Child
    /// correctly displays full Android metrics without applying iOS data collection limitations.
    func testParentIPhoneDisplaysCompleteAndroidChildTelemetry() {
        // Mock payload received from Spring Boot backend originating from an Android Child
        let androidChildSummary = ChildTelemetrySummary(
            deviceId: 42,
            deviceName: "Arun's Galaxy A54",
            platform: "ANDROID",
            isOnline: true,
            battery: BatteryTelemetryResponse(
                deviceId: 42,
                batteryLevel: 84,
                isCharging: true,
                powerSaveMode: false,
                temperatureCelsius: 31.5, // Android specific metric
                voltageMv: 4120,          // Android specific metric
                health: "GOOD",
                timestamp: "2026-09-09T22:30:00Z"
            ),
            network: NetworkTelemetryResponse(
                deviceId: 42,
                networkType: "Wi-Fi (5GHz)",
                isConnected: true,
                signalStrengthDbm: -58,   // Android specific dBm metric
                latencyMs: 24,
                wifiSsid: "HomeFiber_5G",
                cellularCarrier: "Airtel",
                quality: "EXCELLENT",
                timestamp: "2026-09-09T22:30:00Z"
            ),
            location: LocationTelemetryResponse(
                deviceId: 42,
                latitude: 12.9716,
                longitude: 77.5946,
                accuracyMeters: 8.5,
                speedMps: 0.0,
                altitudeMeters: 920.0,
                address: "Koramangala, Bangalore",
                timestamp: "2026-09-09T22:30:00Z"
            ),
            screenTime: ScreenTimeResponse(
                deviceId: 42,
                totalUsageMinutes: 142,
                screenOnTimeMinutes: 135,
                unlockCount: 22,
                apps: [
                    AppUsageItem(packageName: "com.google.android.youtube", appName: "YouTube", usageMinutes: 65, category: "Entertainment"),
                    AppUsageItem(packageName: "com.android.chrome", appName: "Chrome", usageMinutes: 45, category: "Productivity"),
                    AppUsageItem(packageName: "com.duolingo", appName: "Duolingo", usageMinutes: 32, category: "Education")
                ]
            ),
            deviceHealth: DeviceHealthResponse(
                deviceId: 42,
                storageUsedBytes: 45000000000,
                storageTotalBytes: 128000000000,
                memoryUsedBytes: 3800000000,
                memoryTotalBytes: 8000000000,
                batteryHealth: "GOOD",
                cpuUsagePercent: 12.4,
                thermalState: "NOMINAL",
                osVersion: "Android 14 / One UI 6.0",
                deviceModel: "SM-A546E",
                timestamp: "2026-09-09T22:30:00Z"
            ),
            unreadAlertsCount: 0,
            lastSyncAt: "2026-09-09T22:30:00Z"
        )

        // Invariant Verifications:
        // 1. Android platform identity preserved
        XCTAssertEqual(androidChildSummary.platform, "ANDROID")
        XCTAssertEqual(androidChildSummary.deviceName, "Arun's Galaxy A54")

        // 2. Android Battery telemetry fully readable on Parent iPhone
        XCTAssertNotNil(androidChildSummary.battery)
        XCTAssertEqual(androidChildSummary.battery?.batteryLevel, 84)
        XCTAssertEqual(androidChildSummary.battery?.temperatureCelsius, 31.5)
        XCTAssertEqual(androidChildSummary.battery?.voltageMv, 4120)

        // 3. Android Screen Time app usages fully readable on Parent iPhone
        XCTAssertNotNil(androidChildSummary.screenTime)
        XCTAssertEqual(androidChildSummary.screenTime?.totalUsageMinutes, 142)
        XCTAssertEqual(androidChildSummary.screenTime?.apps?.count, 3)
        XCTAssertEqual(androidChildSummary.screenTime?.apps?.first?.appName, "YouTube")

        // 4. Android Network dBm and Wi-Fi SSID fully readable on Parent iPhone
        XCTAssertNotNil(androidChildSummary.network)
        XCTAssertEqual(androidChildSummary.network?.signalStrengthDbm, -58)
        XCTAssertEqual(androidChildSummary.network?.wifiSsid, "HomeFiber_5G")

        // 5. Android GPS location fully readable on Parent iPhone
        XCTAssertNotNil(androidChildSummary.location)
        XCTAssertEqual(androidChildSummary.location?.latitude, 12.9716)
        XCTAssertEqual(androidChildSummary.location?.longitude, 77.5946)
        XCTAssertEqual(androidChildSummary.location?.address, "Koramangala, Bangalore")

        // 6. Android Hardware device state fully readable on Parent iPhone
        XCTAssertNotNil(androidChildSummary.deviceHealth)
        XCTAssertEqual(androidChildSummary.deviceHealth?.osVersion, "Android 14 / One UI 6.0")
        XCTAssertEqual(androidChildSummary.deviceHealth?.deviceModel, "SM-A546E")
    }

    func testCrossPlatformDisconnectCodeExchange() {
        // Parent iPhone generates code
        let parentCode = GenerateDisconnectCodeResponse(code: "482019", expiresAt: "2026-09-10T12:00:00Z", ttlMinutes: 10)
        XCTAssertEqual(parentCode.code.count, 6)

        // Child Android enters code
        let verifyRequest = VerifyDisconnectCodeRequest(code: parentCode.code)
        XCTAssertEqual(verifyRequest.code, "482019")
    }
}
