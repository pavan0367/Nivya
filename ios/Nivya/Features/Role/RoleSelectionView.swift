import SwiftUI

public struct RoleSelectionView: View {
    @ObservedObject private var appState = AppState.shared
    private let prefs = AppPreferences.shared

    public init() {}

    public var body: some View {
        ZStack {
            NivyaColors.backgroundDark.ignoresSafeArea()

            VStack(spacing: 28) {
                Spacer().frame(height: 20)

                VStack(spacing: 8) {
                    Image("Logo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 52, height: 52)

                    Text("Choose Device Role")
                        .font(.system(size: 26, weight: .bold))
                        .foregroundColor(.white)

                    Text("Select how this iPhone will be used in your family")
                        .font(.system(size: 14))
                        .foregroundColor(NivyaColors.textSecondary)
                }

                VStack(spacing: 20) {
                    // Parent Role Option
                    Button(action: { selectRole(RoleType.parent) }) {
                        HStack(spacing: 16) {
                            Image(systemName: "person.2.fill")
                                .font(.system(size: 28))
                                .foregroundColor(NivyaColors.blueLight)
                                .frame(width: 54, height: 54)
                                .background(NivyaColors.bluePrimary.opacity(0.2))
                                .cornerRadius(14)

                            VStack(alignment: .leading, spacing: 4) {
                                Text("Parent Device")
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(.white)

                                Text("Monitor child safety telemetry, review screen time, receive instant alerts, and manage convocation")
                                    .font(.system(size: 13))
                                    .foregroundColor(NivyaColors.textSecondary)
                                    .lineLimit(3)
                            }

                            Spacer()

                            Image(systemName: "chevron.right")
                                .foregroundColor(NivyaColors.textMuted)
                        }
                        .padding(18)
                        .background(NivyaColors.surfaceCard)
                        .cornerRadius(18)
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(NivyaColors.bluePrimary.opacity(0.4), lineWidth: 1)
                        )
                    }

                    // Child Role Option
                    Button(action: { selectRole(RoleType.child) }) {
                        HStack(spacing: 16) {
                            Image(systemName: "figure.and.child.holdinghands")
                                .font(.system(size: 28))
                                .foregroundColor(NivyaColors.purpleLight)
                                .frame(width: 54, height: 54)
                                .background(NivyaColors.purpleAccent.opacity(0.2))
                                .cornerRadius(14)

                            VStack(alignment: .leading, spacing: 4) {
                                Text("Child Device")
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(.white)

                                Text("Digital wellbeing companion with rapid safety shortcuts (CRACK, FREAK) and transparent sharing")
                                    .font(.system(size: 13))
                                    .foregroundColor(NivyaColors.textSecondary)
                                    .lineLimit(3)
                            }

                            Spacer()

                            Image(systemName: "chevron.right")
                                .foregroundColor(NivyaColors.textMuted)
                        }
                        .padding(18)
                        .background(NivyaColors.surfaceCard)
                        .cornerRadius(18)
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(NivyaColors.purpleAccent.opacity(0.4), lineWidth: 1)
                        )
                    }
                }
                .padding(.horizontal, 20)

                Spacer()

                Button(action: { appState.handleLogout() }) {
                    Text("Sign Out")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(NivyaColors.errorRed)
                }
                .padding(.bottom, 20)
            }
        }
    }

    private func selectRole(_ role: RoleType) {
        prefs.savedRole = role.rawValue
        if role == .parent {
            appState.currentDestination = .parentPairing
        } else {
            appState.currentDestination = .childPairing
        }
    }
}
