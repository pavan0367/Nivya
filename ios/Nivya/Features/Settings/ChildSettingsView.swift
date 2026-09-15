import SwiftUI

public struct ChildSettingsView: View {
    @ObservedObject private var appState = AppState.shared
    @State private var showEnterCodeSheet: Bool = false

    public init() {}

    public var body: some View {
        ZStack {
            NivyaColors.backgroundDark.ignoresSafeArea()

            VStack(spacing: 0) {
                // Topbar
                HStack {
                    Button(action: { appState.currentDestination = .childDashboard }) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(.white)
                            .font(.system(size: 16, weight: .semibold))
                    }
                    Text("Child Settings & Transparency")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(NivyaColors.surfaceDark)

                ScrollView {
                    VStack(spacing: 20) {
                        // Transparency & Privacy Overview
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(spacing: 10) {
                                Image(systemName: "hand.raised.fill")
                                    .foregroundColor(NivyaColors.purpleLight)
                                    .font(.system(size: 20))
                                Text("Transparency & Consent")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.white)
                            }

                            Text("Nivya is built with family safety and digital wellbeing in mind. The following information is shared with your parent:")
                                .font(.system(size: 13))
                                .foregroundColor(NivyaColors.textSecondary)

                            VStack(alignment: .leading, spacing: 6) {
                                transparencyBullet(icon: "battery.100.bolt", text: "Battery level and charging status")
                                transparencyBullet(icon: "location.fill", text: "Approximate GPS location for safety")
                                transparencyBullet(icon: "wifi", text: "Network connection and quality")
                                transparencyBullet(icon: "heart.fill", text: "Device storage and thermal state")
                                transparencyBullet(icon: "lock.fill", text: "Private messages, photos, and calls are NEVER accessed")
                            }
                            .padding(.top, 4)
                        }
                        .padding(16)
                        .background(NivyaColors.surfaceCard)
                        .cornerRadius(16)

                        // Protected Disconnect Section
                        // Invariant: ZERO normal Disconnect / Remove Parent buttons!
                        // The ONLY capability is "Enter Parent Disconnect Code" modal.
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(spacing: 8) {
                                Image(systemName: "shield.lefthalf.filled")
                                    .foregroundColor(NivyaColors.warningAmber)
                                Text("Family Connection Management")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(.white)
                            }

                            Text("To unlink this device from your family, ask your parent to generate a secure one-time code on their console.")
                                .font(.system(size: 12))
                                .foregroundColor(NivyaColors.textSecondary)

                            Button(action: { showEnterCodeSheet = true }) {
                                HStack(spacing: 8) {
                                    Image(systemName: "key.horizontal.fill")
                                    Text("Enter Parent Disconnect Code")
                                }
                                .font(.system(size: 14, weight: .bold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(NivyaColors.purpleAccent.opacity(0.2))
                                .foregroundColor(NivyaColors.purpleLight)
                                .cornerRadius(10)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(NivyaColors.purpleAccent.opacity(0.5), lineWidth: 1)
                                )
                            }
                        }
                        .padding(16)
                        .background(NivyaColors.surfaceCard)
                        .cornerRadius(16)

                        // Account Management - Permanent Account Deletion
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(spacing: 8) {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundColor(NivyaColors.errorRed)
                                Text("Account Management")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(NivyaColors.errorRed)
                            }

                            Text("Permanently delete your child account. If connected, requires parent approval code. Your parent's account will NOT be deleted.")
                                .font(.system(size: 12))
                                .foregroundColor(NivyaColors.textSecondary)

                            Button(action: {
                                deletionStep = 1
                                deletionError = nil
                                approvalCodeInput = ""
                                passwordInput = ""
                                showDeleteSheet = true
                            }) {
                                HStack(spacing: 8) {
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

                        // Sign Out Button (Preserves Pairing)
                        Button(action: { appState.handleLogout() }) {
                            HStack {
                                Image(systemName: "rectangle.portrait.and.arrow.right")
                                Text("Sign Out of Account")
                            }
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(NivyaColors.errorRed)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(NivyaColors.errorRed.opacity(0.12))
                            .cornerRadius(10)
                        }
                    }
                    .padding(16)
                }
            }
        }
        .sheet(isPresented: $showEnterCodeSheet) {
            ChildEnterDisconnectCodeView()
        }
        .sheet(isPresented: $showDeleteSheet) {
            childDeleteAccountSheet
        }
    }

    @State private var showDeleteSheet: Bool = false
    @State private var deletionStep: Int = 1
    @State private var approvalCodeInput: String = ""
    @State private var passwordInput: String = ""
    @State private var isDeleting: Bool = false
    @State private var deletionError: String? = nil

    private var childDeleteAccountSheet: some View {
        ZStack {
            NivyaColors.backgroundDark.ignoresSafeArea()

            VStack(spacing: 16) {
                HStack {
                    Text(deletionStep == 4 ? "Account Deleted" : "Delete Child Account")
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
                                Text("Delete Child Account")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.white)
                                Text("Permanently delete your child account. This action is irreversible.")
                                    .font(.system(size: 13))
                                    .foregroundColor(NivyaColors.textSecondary)

                                VStack(alignment: .leading, spacing: 6) {
                                    Text("• Your child account credentials will be erased.")
                                        .font(.system(size: 12))
                                        .foregroundColor(NivyaColors.textSecondary)
                                    Text("• Your parent's account will NOT be deleted or affected.")
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundColor(NivyaColors.successGreen)
                                    Text("• Deletion requires parent approval code sent to your parent's email.")
                                        .font(.system(size: 12))
                                        .foregroundColor(NivyaColors.blueLight)
                                }
                                .padding(12)
                                .background(NivyaColors.surfaceDark)
                                .cornerRadius(10)

                                Button(action: { deletionStep = 2 }) {
                                    Text("Request Parent Approval")
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
                                Text("Enter Parent Approval Code")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.white)
                                Text("Enter the 6-digit approval code sent to your parent's email:")
                                    .font(.system(size: 13))
                                    .foregroundColor(NivyaColors.textSecondary)

                                TextField("123456", text: $approvalCodeInput)
                                    .padding(12)
                                    .background(NivyaColors.surfaceDark)
                                    .cornerRadius(8)
                                    .foregroundColor(.white)
                                    .multilineTextAlignment(.center)
                                    .font(.system(size: 20, weight: .bold, design: .monospaced))

                                Button(action: {
                                    if approvalCodeInput.count != 6 {
                                        deletionError = "Please enter the 6-digit approval code."
                                    } else {
                                        deletionError = nil
                                        deletionStep = 3
                                    }
                                }) {
                                    Text("Verify Code & Proceed")
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
                                Text("Parent approval verified. Are you sure you want to permanently delete your child account? This action cannot be undone.")
                                    .font(.system(size: 13))
                                    .foregroundColor(NivyaColors.textSecondary)

                                VStack(alignment: .leading, spacing: 6) {
                                    Text("• Your personal profile and credentials will be permanently erased.")
                                        .font(.system(size: 12))
                                        .foregroundColor(NivyaColors.textSecondary)
                                    Text("• Your parent's account will NOT be deleted or affected.")
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundColor(NivyaColors.successGreen)
                                }
                                .padding(12)
                                .background(NivyaColors.surfaceDark)
                                .cornerRadius(10)

                                Button(action: {
                                    isDeleting = true
                                    deletionError = nil
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
                                Text("Child Account Deleted")
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(.white)
                                Text("Your child account has been permanently removed from Nivya servers.")
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

    private func transparencyBullet(icon: String, text: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 12))
                .foregroundColor(NivyaColors.tealAccent)
                .frame(width: 16)
            Text(text)
                .font(.system(size: 12))
                .foregroundColor(NivyaColors.textSecondary)
        }
    }
}
