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
        .onAppear {
            loadSessions()
            loadEmailPreferences()
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
