import SwiftUI

public struct LoginView: View {
    @ObservedObject private var appState = AppState.shared
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var isLoading: Bool = false
    @State private var errorMessage: String? = nil

    public init() {}

    public var body: some View {
        ZStack {
            NivyaColors.backgroundDark.ignoresSafeArea()

            // Ambient background glow
            Circle()
                .fill(NivyaColors.purpleAccent.opacity(0.15))
                .frame(width: 320, height: 320)
                .blur(radius: 80)
                .offset(y: -180)

            ScrollView {
                VStack(spacing: 24) {
                    Spacer().frame(height: 30)

                    // Header with approved Two-Leaf Logo
                    VStack(spacing: 12) {
                        Image("Logo")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 58, height: 58)

                        Text("Welcome to Nivya")
                            .font(.system(size: 26, weight: .bold))
                            .foregroundColor(.white)

                        Text("Sign in to access your family safety console")
                            .font(.system(size: 14))
                            .foregroundColor(NivyaColors.textSecondary)
                    }

                    if let error = errorMessage {
                        HStack(spacing: 10) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(NivyaColors.errorRed)
                            Text(error)
                                .font(.system(size: 13))
                                .foregroundColor(NivyaColors.errorRed)
                        }
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(NivyaColors.errorRed.opacity(0.15))
                        .cornerRadius(12)
                    }

                    // Login Card
                    VStack(spacing: 18) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Email Address")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(NivyaColors.textSecondary)

                            HStack {
                                Image(systemName: "envelope")
                                    .foregroundColor(NivyaColors.textMuted)
                                TextField("parent@example.com", text: $email)
                                    .autocapitalization(.none)
                                    .keyboardType(.emailAddress)
                                    .foregroundColor(.white)
                            }
                            .padding()
                            .background(NivyaColors.surfaceDark)
                            .cornerRadius(12)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(NivyaColors.outlineDark, lineWidth: 1)
                            )
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Password")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(NivyaColors.textSecondary)

                            HStack {
                                Image(systemName: "lock")
                                    .foregroundColor(NivyaColors.textMuted)
                                SecureField("••••••••", text: $password)
                                    .foregroundColor(.white)
                            }
                            .padding()
                            .background(NivyaColors.surfaceDark)
                            .cornerRadius(12)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(NivyaColors.outlineDark, lineWidth: 1)
                            )
                        }

                        Button(action: handleLogin) {
                            HStack {
                                if isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Text("Sign In")
                                        .font(.system(size: 16, weight: .bold))
                                    Image(systemName: "arrow.right")
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(NivyaColors.primaryGradient)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                        .disabled(isLoading || email.isEmpty || password.isEmpty)
                        .opacity(isLoading || email.isEmpty || password.isEmpty ? 0.6 : 1.0)
                    }
                    .padding(20)
                    .background(NivyaColors.surfaceCard)
                    .cornerRadius(18)
                    .overlay(
                        RoundedRectangle(cornerRadius: 18)
                            .stroke(NivyaColors.outlineDark.opacity(0.6), lineWidth: 1)
                    )

                    // Switch to Register
                    Button(action: { appState.currentDestination = .register }) {
                        Text("Don't have an account? Create Account")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(NivyaColors.blueLight)
                    }
                    .padding(.top, 10)

                    Spacer()
                }
                .padding(.horizontal, 20)
            }
        }
    }

    private func handleLogin() {
        guard !email.isEmpty, !password.isEmpty else { return }
        isLoading = true
        errorMessage = nil

        Task {
            do {
                _ = try await AuthRepository.shared.login(email: email, password: password)
                DispatchQueue.main.async {
                    isLoading = false
                    appState.handleLoginSuccess()
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
