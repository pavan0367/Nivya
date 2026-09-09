import SwiftUI

public struct ParentPairingView: View {
    @ObservedObject private var appState = AppState.shared
    @State private var pairingCode: String = "------"
    @State private var ttlMinutes: Int = 10
    @State private var isLoading: Bool = false
    @State private var errorMessage: String? = nil
    @State private var pollTimer: Timer?

    public init() {}

    public var body: some View {
        ZStack {
            NivyaColors.backgroundDark.ignoresSafeArea()

            VStack(spacing: 24) {
                Spacer().frame(height: 20)

                VStack(spacing: 8) {
                    Image(systemName: "link.badge.plus")
                        .font(.system(size: 40))
                        .foregroundColor(NivyaColors.blueLight)

                    Text("Pair Child Device")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.white)

                    Text("Enter this one-time pairing key on your child's device (iPhone or Android)")
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

                // Pairing Code Display Card
                VStack(spacing: 16) {
                    Text("ACTIVE PAIRING CODE")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(NivyaColors.textMuted)
                        .tracking(1.5)

                    if isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: NivyaColors.blueLight))
                            .frame(height: 60)
                    } else {
                        Text(pairingCode)
                            .font(.system(size: 44, weight: .black, design: .monospaced))
                            .foregroundColor(.white)
                            .tracking(6)
                    }

                    Text("Valid for \(ttlMinutes) minutes • Single child device link")
                        .font(.system(size: 12))
                        .foregroundColor(NivyaColors.textMuted)

                    Button(action: generateCode) {
                        HStack {
                            Image(systemName: "arrow.clockwise")
                            Text("Generate New Code")
                        }
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(NivyaColors.blueLight)
                    }
                    .padding(.top, 4)
                }
                .padding(24)
                .frame(maxWidth: .infinity)
                .background(NivyaColors.surfaceCard)
                .cornerRadius(20)
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(NivyaColors.bluePrimary.opacity(0.4), lineWidth: 1)
                )
                .padding(.horizontal, 20)

                HStack(spacing: 10) {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: NivyaColors.tealAccent))
                        .scaleEffect(0.8)
                    Text("Waiting for child device to connect...")
                        .font(.system(size: 13))
                        .foregroundColor(NivyaColors.textSecondary)
                }
                .padding(.top, 10)

                Spacer()

                HStack {
                    Button("Back to Role") {
                        appState.currentDestination = .roleSelection
                    }
                    .foregroundColor(NivyaColors.textMuted)

                    Spacer()

                    Button("Skip to Dashboard") {
                        appState.currentDestination = .parentDashboard
                    }
                    .foregroundColor(NivyaColors.blueLight)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 20)
            }
        }
        .onAppear {
            generateCode()
            startPolling()
        }
        .onDisappear {
            pollTimer?.invalidate()
        }
    }

    private func generateCode() {
        isLoading = true
        errorMessage = nil
        Task {
            do {
                let res = try await PairingRepository.shared.generatePairingCode()
                DispatchQueue.main.async {
                    self.pairingCode = res.pairingCode
                    self.ttlMinutes = res.ttlMinutes
                    self.isLoading = false
                }
            } catch {
                DispatchQueue.main.async {
                    self.isLoading = false
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }

    private func startPolling() {
        pollTimer?.invalidate()
        pollTimer = Timer.scheduledTimer(withTimeInterval: 4.0, repeats: true) { _ in
            Task {
                if let family = try? await PairingRepository.shared.getFamily(), family.isPaired {
                    DispatchQueue.main.async {
                        self.pollTimer?.invalidate()
                        self.appState.currentDestination = .parentDashboard
                    }
                }
            }
        }
    }
}
