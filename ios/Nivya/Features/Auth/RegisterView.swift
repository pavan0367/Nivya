import SwiftUI

public struct RegisterView: View {
    @ObservedObject private var appState = AppState.shared
    @State private var name: String = ""
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var selectedRole: RoleType = .parent
    @State private var isLoading: Bool = false
    @State private var errorMessage: String? = nil

    public init() {}

    public var body: some View {
        ZStack {
            NivyaColors.backgroundDark.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 24) {
                    Spacer().frame(height: 20)

                    VStack(spacing: 10) {
                        Image("Logo")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 52, height: 52)

                        Text("Create Nivya Account")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)

                        Text("Join the family safety platform")
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

                    VStack(spacing: 16) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Full Name")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(NivyaColors.textSecondary)
                            TextField("Your name", text: $name)
                                .foregroundColor(.white)
                                .padding()
                                .background(NivyaColors.surfaceDark)
                                .cornerRadius(12)
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Email Address")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(NivyaColors.textSecondary)
                            TextField("email@example.com", text: $email)
                                .autocapitalization(.none)
                                .keyboardType(.emailAddress)
                                .foregroundColor(.white)
                                .padding()
                                .background(NivyaColors.surfaceDark)
                                .cornerRadius(12)
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Password")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(NivyaColors.textSecondary)
                            SecureField("••••••••", text: $password)
                                .foregroundColor(.white)
                                .padding()
                                .background(NivyaColors.surfaceDark)
                                .cornerRadius(12)
                        }

                        VStack(alignment: .leading, spacing: 8) {
                            Text("Account Role")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(NivyaColors.textSecondary)

                            Picker("Role", selection: $selectedRole) {
                                Text("Parent").tag(RoleType.parent)
                                Text("Child").tag(RoleType.child)
                            }
                            .pickerStyle(SegmentedPickerStyle())
                        }

                        Button(action: handleRegister) {
                            HStack {
                                if isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Text("Register")
                                        .font(.system(size: 16, weight: .bold))
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(NivyaColors.primaryGradient)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                        .disabled(isLoading || name.isEmpty || email.isEmpty || password.isEmpty)
                        .opacity(isLoading || name.isEmpty || email.isEmpty || password.isEmpty ? 0.6 : 1.0)
                    }
                    .padding(20)
                    .background(NivyaColors.surfaceCard)
                    .cornerRadius(18)

                    Button(action: { appState.currentDestination = .login }) {
                        Text("Already have an account? Sign In")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(NivyaColors.blueLight)
                    }

                    Spacer()
                }
                .padding(.horizontal, 20)
            }
        }
    }

    private func handleRegister() {
        isLoading = true
        errorMessage = nil

        Task {
            do {
                _ = try await AuthRepository.shared.register(
                    name: name,
                    email: email,
                    password: password,
                    role: selectedRole
                )
                // Log in directly after register
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
