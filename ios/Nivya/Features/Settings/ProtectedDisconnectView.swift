import SwiftUI
import UIKit

// MARK: - Parent Code Generation Sheet
public struct ParentGenerateDisconnectCodeView: View {
    @Environment(\.presentationMode) var presentationMode
    @State private var code: String = "------"
    @State private var ttlMinutes: Int = 10
    @State private var remainingSeconds: Int = 600
    @State private var isLoading: Bool = false
    @State private var errorMessage: String? = nil
    @State private var timer: Timer?

    public init() {}

    public var body: some View {
        ZStack {
            NivyaColors.backgroundDark.ignoresSafeArea()

            VStack(spacing: 24) {
                HStack {
                    Spacer()
                    Button("Done") {
                        presentationMode.wrappedValue.dismiss()
                    }
                    .foregroundColor(NivyaColors.blueLight)
                }
                .padding(.top, 16)
                .padding(.horizontal, 20)

                Image(systemName: "lock.shield.fill")
                    .font(.system(size: 42))
                    .foregroundColor(NivyaColors.warningAmber)

                VStack(spacing: 6) {
                    Text("Protected Disconnect Code")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.white)

                    Text("Provide this one-time code to your child's device to unlink the family relationship")
                        .font(.system(size: 13))
                        .foregroundColor(NivyaColors.textSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }

                if let error = errorMessage {
                    Text(error)
                        .font(.system(size: 12))
                        .foregroundColor(NivyaColors.errorRed)
                        .padding()
                        .background(NivyaColors.errorRed.opacity(0.15))
                        .cornerRadius(10)
                }

                VStack(spacing: 14) {
                    if isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: NivyaColors.warningAmber))
                            .frame(height: 50)
                    } else {
                        Text(code)
                            .font(.system(size: 44, weight: .black, design: .monospaced))
                            .foregroundColor(.white)
                            .tracking(6)
                    }

                    Text("Expires in \(remainingSeconds / 60):\(String(format: "%02d", remainingSeconds % 60))")
                        .font(.system(size: 13, weight: .medium, design: .monospaced))
                        .foregroundColor(NivyaColors.textMuted)

                    Button(action: copyToClipboard) {
                        HStack(spacing: 6) {
                            Image(systemName: "doc.on.doc")
                            Text("Copy Code")
                        }
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(NivyaColors.blueLight)
                    }
                }
                .padding(24)
                .frame(maxWidth: .infinity)
                .background(NivyaColors.surfaceCard)
                .cornerRadius(18)
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(NivyaColors.warningAmber.opacity(0.4), lineWidth: 1)
                )
                .padding(.horizontal, 20)

                Spacer()
            }
        }
        .onAppear {
            generateCode()
        }
        .onDisappear {
            timer?.invalidate()
        }
    }

    private func generateCode() {
        isLoading = true
        errorMessage = nil

        Task {
            do {
                let res = try await PairingRepository.shared.generateDisconnectCode()
                DispatchQueue.main.async {
                    self.code = res.code
                    self.ttlMinutes = res.ttlMinutes
                    self.remainingSeconds = res.ttlMinutes * 60
                    self.isLoading = false
                    self.startCountdown()
                }
            } catch {
                DispatchQueue.main.async {
                    self.isLoading = false
                    self.errorMessage = "Failed to generate disconnect code: \(error.localizedDescription)"
                }
            }
        }
    }

    private func startCountdown() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            if remainingSeconds > 0 {
                remainingSeconds -= 1
            } else {
                code = "EXPIRED"
                timer?.invalidate()
            }
        }
    }

    private func copyToClipboard() {
        UIPasteboard.general.string = code
    }
}

// MARK: - Child Code Verification Sheet
public struct ChildEnterDisconnectCodeView: View {
    @Environment(\.presentationMode) var presentationMode
    @State private var code: String = ""
    @State private var isSubmitting: Bool = false
    @State private var errorMessage: String? = nil

    public init() {}

    public var body: some View {
        ZStack {
            NivyaColors.backgroundDark.ignoresSafeArea()

            VStack(spacing: 24) {
                HStack {
                    Spacer()
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                    }
                    .foregroundColor(NivyaColors.textMuted)
                }
                .padding(.top, 16)
                .padding(.horizontal, 20)

                Image(systemName: "key.horizontal.fill")
                    .font(.system(size: 40))
                    .foregroundColor(NivyaColors.purpleLight)

                VStack(spacing: 6) {
                    Text("Enter Parent Disconnect Code")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.white)

                    Text("Ask your parent to generate a 6-digit disconnect code from their Nivya console")
                        .font(.system(size: 13))
                        .foregroundColor(NivyaColors.textSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }

                if let error = errorMessage {
                    Text(error)
                        .font(.system(size: 12))
                        .foregroundColor(NivyaColors.errorRed)
                        .padding()
                        .background(NivyaColors.errorRed.opacity(0.15))
                        .cornerRadius(10)
                }

                VStack(spacing: 16) {
                    TextField("Enter 6-digit code", text: $code)
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

                    Button(action: verifyCode) {
                        HStack {
                            if isSubmitting {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Text("Verify & Unlink")
                                    .font(.system(size: 16, weight: .bold))
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(NivyaColors.purpleAccent)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                    .disabled(isSubmitting || code.trimmingCharacters(in: .whitespaces).count < 6)
                    .opacity(isSubmitting || code.trimmingCharacters(in: .whitespaces).count < 6 ? 0.6 : 1.0)
                }
                .padding(20)
                .background(NivyaColors.surfaceCard)
                .cornerRadius(18)
                .padding(.horizontal, 20)

                Spacer()
            }
        }
    }

    private func verifyCode() {
        let clean = code.trimmingCharacters(in: .whitespacesAndNewlines)
        guard clean.count >= 6 else { return }

        isSubmitting = true
        errorMessage = nil

        Task {
            do {
                _ = try await PairingRepository.shared.verifyDisconnectCode(code: clean)
                DispatchQueue.main.async {
                    self.isSubmitting = false
                    self.presentationMode.wrappedValue.dismiss()
                    AppState.shared.handleDisconnected()
                }
            } catch {
                DispatchQueue.main.async {
                    self.isSubmitting = false
                    self.errorMessage = "Invalid or expired code. Please verify and try again."
                }
            }
        }
    }
}
