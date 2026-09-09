import SwiftUI
import UIKit

public struct ChildPairingView: View {
    @ObservedObject private var appState = AppState.shared
    @State private var code: String = ""
    @State private var deviceName: String = UIDevice.current.name
    @State private var isLoading: Bool = false
    @State private var errorMessage: String? = nil

    public init() {}

    public var body: some View {
        ZStack {
            NivyaColors.backgroundDark.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 24) {
                    Spacer().frame(height: 20)

                    VStack(spacing: 8) {
                        Image(systemName: "key.fill")
                            .font(.system(size: 38))
                            .foregroundColor(NivyaColors.purpleLight)

                        Text("Enter Pairing Code")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)

                        Text("Enter the 6-digit code shown on your parent's phone to connect this device")
                            .font(.system(size: 14))
                            .foregroundColor(NivyaColors.textSecondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 20)
                    }

                    if let error = errorMessage {
                        Text(error)
                            .font(.system(size: 13))
                            .foregroundColor(NivyaColors.errorRed)
                            .padding()
                            .background(NivyaColors.errorRed.opacity(0.15))
                            .cornerRadius(10)
                    }

                    VStack(spacing: 18) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Pairing Code")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(NivyaColors.textSecondary)

                            TextField("e.g. 849201", text: $code)
                                .font(.system(size: 24, weight: .bold, design: .monospaced))
                                .keyboardType(.numberPad)
                                .multilineTextAlignment(.center)
                                .foregroundColor(.white)
                                .padding()
                                .background(NivyaColors.surfaceDark)
                                .cornerRadius(12)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(NivyaColors.purpleAccent.opacity(0.5), lineWidth: 1)
                                )
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Child Device Name")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(NivyaColors.textSecondary)

                            TextField("My iPhone", text: $deviceName)
                                .foregroundColor(.white)
                                .padding()
                                .background(NivyaColors.surfaceDark)
                                .cornerRadius(12)
                        }

                        Button(action: handlePair) {
                            HStack {
                                if isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Text("Connect to Family")
                                        .font(.system(size: 16, weight: .bold))
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(NivyaColors.purpleAccent)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                        .disabled(isLoading || code.count < 6)
                        .opacity(isLoading || code.count < 6 ? 0.6 : 1.0)
                    }
                    .padding(20)
                    .background(NivyaColors.surfaceCard)
                    .cornerRadius(18)
                    .padding(.horizontal, 20)

                    Spacer()

                    Button("Back to Role Selection") {
                        appState.currentDestination = .roleSelection
                    }
                    .foregroundColor(NivyaColors.textMuted)
                    .padding(.bottom, 20)
                }
            }
        }
    }

    private func handlePair() {
        guard code.count >= 6 else { return }
        isLoading = true
        errorMessage = nil

        Task {
            do {
                _ = try await PairingRepository.shared.pair(
                    pairingCode: code.trimmingCharacters(in: .whitespacesAndNewlines),
                    childDeviceName: deviceName
                )
                DispatchQueue.main.async {
                    isLoading = false
                    appState.currentDestination = .childDashboard
                    TelemetrySyncService.shared.startPeriodicSync()
                    WebSocketClient.shared.connect()
                }
            } catch {
                DispatchQueue.main.async {
                    isLoading = false
                    errorMessage = error.localizedDescription
                }
            }
        }
    }
}
