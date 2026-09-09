import Foundation
import UIKit

public final class PairingRepository {
    public static let shared = PairingRepository()
    private let client = NetworkClient.shared
    private let prefs = AppPreferences.shared

    private init() {}

    public func generatePairingCode() async throws -> GeneratePairingCodeResponse {
        return try await client.postEmpty(path: APIEndpoint.Pairing.generateCode, requiresAuth: true)
    }

    public func pair(pairingCode: String, childDeviceName: String) async throws -> FamilyResponse {
        let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
        let req = PairRequest(
            pairingCode: pairingCode,
            childDeviceName: childDeviceName,
            platform: "IOS",
            deviceUuid: prefs.deviceUuid,
            appVersion: appVersion
        )

        let family: FamilyResponse = try await client.post(
            path: APIEndpoint.Pairing.pair,
            body: req,
            requiresAuth: true
        )

        prefs.isPaired = family.isPaired
        prefs.familyId = family.familyId
        return family
    }

    public func getFamily() async throws -> FamilyResponse {
        let family: FamilyResponse = try await client.get(
            path: APIEndpoint.Pairing.family,
            requiresAuth: true
        )
        prefs.isPaired = family.isPaired
        prefs.familyId = family.familyId
        if let firstDev = family.devices?.first {
            if prefs.activeChildDeviceId == nil {
                prefs.activeChildDeviceId = firstDev.id
            }
        }
        return family
    }

    public func generateDisconnectCode() async throws -> GenerateDisconnectCodeResponse {
        return try await client.postEmpty(
            path: APIEndpoint.Pairing.disconnectCode,
            requiresAuth: true
        )
    }

    public func verifyDisconnectCode(code: String) async throws -> Bool {
        let req = VerifyDisconnectCodeRequest(code: code)
        let _: EmptyResponse = try await client.post(
            path: APIEndpoint.Pairing.disconnectVerify,
            body: req,
            requiresAuth: true
        )
        // On successful code validation, unlink locally
        prefs.clearPairing()
        return true
    }

    public func unpairDevice(deviceId: Int64) async throws {
        let _: EmptyResponse = try await client.delete(
            path: APIEndpoint.Pairing.unpairDevice(id: deviceId),
            requiresAuth: true
        )
    }
}
