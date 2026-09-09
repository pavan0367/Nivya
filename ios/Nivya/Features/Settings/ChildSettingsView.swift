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
