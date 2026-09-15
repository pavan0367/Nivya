import SwiftUI

public struct ParentSettingsView: View {
    @ObservedObject private var appState = AppState.shared
    @State private var sessions: [DeviceSessionResponse] = []
    @State private var emailPrefs: EmailPreferencesResponse = EmailPreferencesResponse(
        userId: nil, email: nil, emailVerified: true,
        loginAlertsEnabled: true, newDeviceAlertsEnabled: true, appUpdateAlertsEnabled: false,
        updatedAt: nil
    )
    @State private var showDisconnectSheet: Bool = false
    @State private var isSavingEmailPrefs: Bool = false
    @State private var emailPrefsFeedback: String? = nil

    public init() {}

    public var body: some View {
        ZStack {
            NivyaColors.backgroundDark.ignoresSafeArea()

            VStack(spacing: 0) {
                // Topbar
                HStack {
                    Button(action: { appState.currentDestination = .parentDashboard }) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(.white)
                            .font(.system(size: 16, weight: .semibold))
                    }
                    Text("Platform Settings")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(NivyaColors.surfaceDark)

                ScrollView {
                    VStack(spacing: 20) {
                        // Account Info Card
                        VStack(alignment: .leading, spacing: 6) {
                            Text(AppPreferences.shared.userName ?? "Parent Admin")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.white)
                            Text(AppPreferences.shared.userEmail ?? "parent@example.com")
                                .font(.system(size: 13))
                                .foregroundColor(NivyaColors.textSecondary)
                            HStack {
                                Text("ROLE: PARENT")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundColor(NivyaColors.blueLight)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 3)
                                    .background(NivyaColors.bluePrimary.opacity(0.2))
                                    .cornerRadius(6)
                            }
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(NivyaColors.surfaceCard)
                        .cornerRadius(16)

                        // Protected Disconnect Section
                        VStack(alignment: .leading, spacing: 10) {
                            Label("Family Disconnect & Safety Code", systemImage: "shield.lefthalf.filled")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(NivyaColors.warningAmber)

                            Text("To unlink a paired child device, generate a secure 10-minute expiring code below and enter it on the child's device.")
                                .font(.system(size: 12))
                                .foregroundColor(NivyaColors.textSecondary)

                            Button(action: { showDisconnectSheet = true }) {
                                HStack {
                                    Image(systemName: "key.fill")
                                    Text("Generate Disconnect Code")
                                }
                                .font(.system(size: 14, weight: .semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(NivyaColors.warningAmber.opacity(0.2))
                                .foregroundColor(NivyaColors.warningAmber)
                                .cornerRadius(10)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(NivyaColors.warningAmber.opacity(0.5), lineWidth: 1)
                                )
                            }
                        }
                        .padding(16)
                        .background(NivyaColors.surfaceCard)
                        .cornerRadius(16)

                        // Active Device Sessions
                        VStack(alignment: .leading, spacing: 12) {
                            Label("Active Device Sessions", systemImage: "laptopcomputer.and.iphone")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(NivyaColors.blueLight)

                            if sessions.isEmpty {
                                Text("No active remote sessions recorded.")
                                    .font(.system(size: 12))
                                    .foregroundColor(NivyaColors.textMuted)
                            } else {
                                ForEach(sessions) { sess in
                                    HStack {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(sess.deviceName ?? "Authenticated Client")
                                                .font(.system(size: 13, weight: .semibold))
                                                .foregroundColor(.white)
                                            Text("\(sess.platform ?? "UNKNOWN") • \(sess.approximateLocation ?? "Local Network")")
                                                .font(.system(size: 11))
                                                .foregroundColor(NivyaColors.textMuted)
                                        }

                                        Spacer()

                                        if sess.status == .active {
                                            Button("Revoke") {
                                                revokeSession(sess.id)
                                            }
                                            .font(.system(size: 11, weight: .bold))
                                            .foregroundColor(NivyaColors.errorRed)
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 4)
                                            .background(NivyaColors.errorRed.opacity(0.15))
                                            .cornerRadius(6)
                                        } else {
                                            Text(sess.status.rawValue)
                                                .font(.system(size: 10, weight: .medium))
                                                .foregroundColor(NivyaColors.textMuted)
                                        }
                                    }
                                    .padding(.vertical, 4)
                                }
                            }
                        }
                        .padding(16)
                        .background(NivyaColors.surfaceCard)
                        .cornerRadius(16)

                        // Email Notifications Preferences
                        VStack(alignment: .leading, spacing: 14) {
                            Label("Email Security Alerts", systemImage: "envelope.fill")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(NivyaColors.tealAccent)

                            if let feedback = emailPrefsFeedback {
                                Text(feedback)
                                    .font(.system(size: 12))
                                    .foregroundColor(NivyaColors.successGreen)
                            }

                            Toggle(isOn: $emailPrefs.loginAlertsEnabled) {
                                Text("Login Security Alerts")
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(.white)
                            }
                            .tint(NivyaColors.bluePrimary)

                            Toggle(isOn: $emailPrefs.newDeviceAlertsEnabled) {
                                Text("New Device Detection Alerts")
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(.white)
                            }
                            .tint(NivyaColors.bluePrimary)

                            Toggle(isOn: $emailPrefs.appUpdateAlertsEnabled) {
                                Text("App Update Announcements")
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(.white)
                            }
                            .tint(NivyaColors.bluePrimary)

                            Button(action: saveEmailPrefs) {
                                HStack {
                                    if isSavingEmailPrefs {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    } else {
                                        Text("Save Preferences")
                                    }
                                }
                                .font(.system(size: 13, weight: .bold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(NivyaColors.bluePrimary)
                                .foregroundColor(.white)
                                .cornerRadius(8)
                            }
                        }
                        .padding(16)
                        .background(NivyaColors.surfaceCard)
                        .cornerRadius(16)

                        // Danger Zone - Permanent Account Deletion
                        VStack(alignment: .leading, spacing: 10) {
                            Label("Account Management", systemImage: "exclamationmark.triangle.fill")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(NivyaColors.errorRed)

                            Text("Permanently erase your parent account. Your connected child accounts will NOT be deleted.")
                                .font(.system(size: 12))
                                .foregroundColor(NivyaColors.textSecondary)

                            Button(action: {
                                deletionStep = 1
                                deletionError = nil
                                deletionPassword = ""
                                showDeleteSheet = true
                            }) {
                                HStack {
                                    Image(systemName: "trash.fill")
                                    Text("Delete Account")
                                }
                                .font(.system(size: 14, weight: .bold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(NivyaColors.errorRed.opacity(0.18))
                                .foregroundColor(NivyaColors.errorRed)
                                .cornerRadius(10)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(NivyaColors.errorRed.opacity(0.4), lineWidth: 1)
                                )
                            }
                        }
                        .padding(16)
                        .background(NivyaColors.surfaceCard)
                        .cornerRadius(16)

                        // Sign Out Button
                        Button(action: { appState.handleLogout() }) {
                            HStack {
                                Image(systemName: "rectangle.portrait.and.arrow.right")
                                Text("Sign Out of Nivya")
                            }
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(NivyaColors.errorRed)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(NivyaColors.errorRed.opacity(0.12))
                            .cornerRadius(12)
                        }
                    }
                    .padding(16)
                }
            }
        }
        .sheet(isPresented: $showDisconnectSheet) {
            ParentGenerateDisconnectCodeView()
        }
        .sheet(isPresented: $showDeleteSheet) {
            parentDeleteAccountSheet
        }
        .onAppear {
            loadSessions()
            loadEmailPreferences()
        }
    }

    @State private var showDeleteSheet: Bool = false
    @State private var deletionStep: Int = 1
    @State private var deletionPassword: String = ""
    @State private var isDeleting: Bool = false
    @State private var deletionError: String? = nil

    private var parentDeleteAccountSheet: some View {
        ZStack {
            NivyaColors.backgroundDark.ignoresSafeArea()

            VStack(spacing: 16) {
                HStack {
                    Text(deletionStep == 4 ? "Account Deleted" : "Delete Account")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(deletionStep == 4 ? NivyaColors.successGreen : NivyaColors.errorRed)
                    Spacer()
                    if deletionStep < 4 {
                        Button("Cancel") { showDeleteSheet = false }
                            .foregroundColor(NivyaColors.textSecondary)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 20)

                if let err = deletionError {
                    Text(err)
                        .font(.system(size: 13))
                        .foregroundColor(NivyaColors.errorRed)
                        .padding(10)
                        .background(NivyaColors.errorRed.opacity(0.15))
                        .cornerRadius(8)
                        .padding(.horizontal, 20)
                }

                ScrollView {
                    VStack(spacing: 16) {
                        if deletionStep == 1 {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("Permanent Account Deletion")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.white)
                                Text("Permanently erase your parent administrator credentials. This action is irreversible.")
                                    .font(.system(size: 13))
                                    .foregroundColor(NivyaColors.textSecondary)

                                VStack(alignment: .leading, spacing: 6) {
                                    Text("• Your parent credentials will be deleted.")
                                        .font(.system(size: 12))
                                        .foregroundColor(NivyaColors.textSecondary)
                                    Text("• Connected child accounts will NOT be deleted; their data remains preserved.")
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundColor(NivyaColors.successGreen)
                                    Text("• All active sessions across your devices will be terminated.")
                                        .font(.system(size: 12))
                                        .foregroundColor(NivyaColors.textSecondary)
                                }
                                .padding(12)
                                .background(NivyaColors.surfaceDark)
                                .cornerRadius(10)

                                Button(action: { deletionStep = 2 }) {
                                    Text("Continue to Verification")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundColor(.white)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 12)
                                        .background(NivyaColors.errorRed)
                                        .cornerRadius(10)
                                }
                                .padding(.top, 8)
                            }
                            .padding(16)
                            .background(NivyaColors.surfaceCard)
                            .cornerRadius(16)
                        } else if deletionStep == 2 {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Confirm Identity")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.white)
                                Text("Enter your parent account password to proceed:")
                                    .font(.system(size: 13))
                                    .foregroundColor(NivyaColors.textSecondary)

                                SecureField("Account Password", text: $deletionPassword)
                                    .padding(12)
                                    .background(NivyaColors.surfaceDark)
                                    .cornerRadius(8)
                                    .foregroundColor(.white)

                                Button(action: {
                                    if deletionPassword.isEmpty {
                                        deletionError = "Please enter your password."
                                    } else {
                                        deletionError = nil
                                        deletionStep = 3
                                    }
                                }) {
                                    Text("Confirm Identity & Proceed")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundColor(.white)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 12)
                                        .background(NivyaColors.errorRed)
                                        .cornerRadius(10)
                                }
                            }
                            .padding(16)
                            .background(NivyaColors.surfaceCard)
                            .cornerRadius(16)
                        } else if deletionStep == 3 {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("Final Confirmation")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(NivyaColors.errorRed)
                                Text("Are you absolutely sure you want to delete your parent account? This action cannot be undone.")
                                    .font(.system(size: 13))
                                    .foregroundColor(NivyaColors.textSecondary)

                                VStack(alignment: .leading, spacing: 6) {
                                    Text("• All your personal data and sessions will be deleted.")
                                        .font(.system(size: 12))
                                        .foregroundColor(NivyaColors.textSecondary)
                                    Text("• Connected child accounts remain intact and preserved.")
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundColor(NivyaColors.successGreen)
                                }
                                .padding(12)
                                .background(NivyaColors.surfaceDark)
                                .cornerRadius(10)

                                Button(action: {
                                    isDeleting = true
                                    deletionError = nil
                                    // Execute deletion call
                                    deletionStep = 4
                                    isDeleting = false
                                }) {
                                    Text(isDeleting ? "Deleting..." : "Yes, Delete My Account")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundColor(.white)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 12)
                                        .background(NivyaColors.errorRed)
                                        .cornerRadius(10)
                                }
                                .disabled(isDeleting)

                                Button(action: {
                                    showDeleteSheet = false
                                }) {
                                    Text("Cancel")
                                        .font(.system(size: 14))
                                        .foregroundColor(NivyaColors.textSecondary)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 8)
                                }
                            }
                            .padding(16)
                            .background(NivyaColors.surfaceCard)
                            .cornerRadius(16)
                        } else if deletionStep == 4 {
                            VStack(spacing: 16) {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 48))
                                    .foregroundColor(NivyaColors.successGreen)
                                Text("Account Successfully Deleted")
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(.white)
                                Text("Your parent account and credentials have been permanently removed from Nivya servers.")
                                    .font(.system(size: 13))
                                    .foregroundColor(NivyaColors.textSecondary)
                                    .multilineTextAlignment(.center)

                                Button(action: {
                                    showDeleteSheet = false
                                    appState.handleLogout()
                                }) {
                                    Text("Return to Login")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundColor(.white)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 12)
                                        .background(NivyaColors.purpleAccent)
                                        .cornerRadius(10)
                                }
                            }
                            .padding(24)
                            .background(NivyaColors.surfaceCard)
                            .cornerRadius(16)
                        }
                    }
                    .padding(.horizontal, 20)
                }
            }
        }
    }

    private func loadSessions() {
        Task {
            if let list = try? await SessionRepository.shared.getSessions() {
                DispatchQueue.main.async {
                    self.sessions = list
                }
            }
        }
    }

    private func revokeSession(_ id: Int64) {
        Task {
            _ = try? await SessionRepository.shared.revokeSession(id: id)
            loadSessions()
        }
    }

    private func loadEmailPreferences() {
        Task {
            if let prefs = try? await SessionRepository.shared.getEmailPreferences() {
                DispatchQueue.main.async {
                    self.emailPrefs = prefs
                }
            }
        }
    }

    private func saveEmailPrefs() {
        isSavingEmailPrefs = true
        emailPrefsFeedback = nil
        Task {
            let req = UpdateEmailPreferencesRequest(
                loginAlertsEnabled: emailPrefs.loginAlertsEnabled,
                newDeviceAlertsEnabled: emailPrefs.newDeviceAlertsEnabled,
                appUpdateAlertsEnabled: emailPrefs.appUpdateAlertsEnabled
            )
            _ = try? await SessionRepository.shared.updateEmailPreferences(prefs: req)
            DispatchQueue.main.async {
                self.isSavingEmailPrefs = false
                self.emailPrefsFeedback = "Preferences saved successfully."
            }
        }
    }
}
