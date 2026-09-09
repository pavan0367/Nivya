import SwiftUI

public struct ChildDashboardView: View {
    @ObservedObject private var appState = AppState.shared
    @ObservedObject private var batteryService = BatteryService.shared
    @ObservedObject private var networkService = NetworkMonitoringService.shared
    @ObservedObject private var locationService = LocationService.shared
    @ObservedObject private var healthService = DeviceHealthService.shared

    @State private var isSendingCrack: Bool = false
    @State private var isSendingFreak: Bool = false
    @State private var showSuccessDialog: Bool = false
    @State private var successPopupMessage: String = "Message sent"
    @State private var errorMessage: String? = nil

    public init() {}

    public var body: some View {
        ZStack {
            NivyaColors.backgroundDark.ignoresSafeArea()

            VStack(spacing: 0) {
                // Topbar
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Child Dashboard")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(.white)
                        Text(AppPreferences.shared.userName ?? "Connected Device")
                            .font(.system(size: 12))
                            .foregroundColor(NivyaColors.textSecondary)
                    }

                    Spacer()

                    Button(action: { appState.currentDestination = .settings }) {
                        Image(systemName: "gearshape.fill")
                            .font(.system(size: 20))
                            .foregroundColor(NivyaColors.textSecondary)
                            .padding(8)
                            .background(NivyaColors.surfaceCard)
                            .clipShape(Circle())
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 14)
                .background(NivyaColors.surfaceDark)

                if let error = errorMessage {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(NivyaColors.errorRed)
                        Text(error)
                            .font(.system(size: 12))
                            .foregroundColor(NivyaColors.errorRed)
                        Spacer()
                        Button(action: { errorMessage = nil }) {
                            Image(systemName: "xmark")
                                .foregroundColor(NivyaColors.textMuted)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(NivyaColors.errorRed.opacity(0.15))
                }

                // Strict 9-Item Scroll List
                ScrollView {
                    VStack(spacing: 12) {
                        // 1. Battery
                        dashboardMetricCard(
                            title: "Battery",
                            subtitle: "\(batteryService.currentBattery.levelPercent)% • \(batteryService.currentBattery.statusDescription)",
                            icon: "battery.100.bolt",
                            iconColor: NivyaColors.successGreen
                        )

                        // 2. Screen Time
                        dashboardMetricCard(
                            title: "Screen Time",
                            subtitle: "Active device controls & wellbeing",
                            icon: "hourglass",
                            iconColor: NivyaColors.bluePrimary
                        )

                        // 3. CRACK (Immediate send of exact "Mom,here")
                        // Invariant: Zero explanatory subtext, immediate send, no pre-send dialog
                        Button(action: sendCrack) {
                            HStack {
                                Spacer()
                                if isSendingCrack {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Text("CRACK")
                                        .font(.system(size: 20, weight: .heavy))
                                        .foregroundColor(.white)
                                }
                                Spacer()
                            }
                            .frame(height: 60)
                            .background(NivyaColors.errorRed)
                            .cornerRadius(14)
                        }
                        .disabled(isSendingCrack || isSendingFreak)

                        // 4. FREAK (Immediate send of exact "Someone's,here")
                        // Invariant: Zero explanatory subtext, immediate send, no pre-send dialog
                        Button(action: sendFreak) {
                            HStack {
                                Spacer()
                                if isSendingFreak {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Text("FREAK")
                                        .font(.system(size: 20, weight: .heavy))
                                        .foregroundColor(.white)
                                }
                                Spacer()
                            }
                            .frame(height: 60)
                            .background(NivyaColors.purpleAccent)
                            .cornerRadius(14)
                        }
                        .disabled(isSendingCrack || isSendingFreak)

                        // 5. Network
                        dashboardMetricCard(
                            title: "Network",
                            subtitle: "\(networkService.connectionType) • \(networkService.isConnected ? "Connected" : "Offline")",
                            icon: "wifi",
                            iconColor: NivyaColors.blueSecondary
                        )

                        // 6. Network Quality
                        dashboardMetricCard(
                            title: "Network Quality",
                            subtitle: networkService.isConstrained ? "Constrained Latency" : "Normal Latency",
                            icon: "speedometer",
                            iconColor: NivyaColors.blueLight
                        )

                        // 7. Location
                        dashboardMetricCard(
                            title: "Location",
                            subtitle: locationSubtitle(),
                            icon: "location.fill",
                            iconColor: NivyaColors.warningAmber
                        )

                        // 8. Device Health
                        dashboardMetricCard(
                            title: "Device Health",
                            subtitle: "\(healthService.metrics.deviceModel) • \(healthService.metrics.thermalState)",
                            icon: "heart.text.square.fill",
                            iconColor: NivyaColors.tealAccent
                        )

                        // 9. Alerts
                        dashboardMetricCard(
                            title: "Alerts",
                            subtitle: "Safety notifications & parent updates",
                            icon: "bell.fill",
                            iconColor: NivyaColors.purpleLight
                        )
                    }
                    .padding(16)
                }
            }

            // Post-send Acknowledgement Modal
            // Invariant: Shows "Message sent" with "DONE" button
            if showSuccessDialog {
                Color.black.opacity(0.6).ignoresSafeArea()

                VStack(spacing: 20) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 48))
                        .foregroundColor(NivyaColors.successGreen)

                    Text(successPopupMessage)
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.white)

                    Button(action: { showSuccessDialog = false }) {
                        Text("DONE")
                            .font(.system(size: 16, weight: .bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(NivyaColors.purpleAccent)
                            .foregroundColor(.white)
                            .cornerRadius(10)
                    }
                }
                .padding(24)
                .background(NivyaColors.surfaceCard)
                .cornerRadius(18)
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(NivyaColors.outlineDark, lineWidth: 1)
                )
                .padding(.horizontal, 40)
            }
        }
    }

    private func dashboardMetricCard(title: String, subtitle: String, icon: String, iconColor: Color) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 22))
                .foregroundColor(iconColor)
                .frame(width: 44, height: 44)
                .background(iconColor.opacity(0.15))
                .cornerRadius(12)

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.white)
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundColor(NivyaColors.textSecondary)
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.system(size: 12))
                .foregroundColor(NivyaColors.textMuted)
        }
        .padding(14)
        .background(NivyaColors.surfaceCard)
        .cornerRadius(14)
    }

    private func locationSubtitle() -> String {
        if let loc = locationService.lastLocation {
            return String(format: "%.4f, %.4f", loc.coordinate.latitude, loc.coordinate.longitude)
        }
        return "Location active • GPS available"
    }

    // CRACK: Send immediately "Mom,here" without pre-confirmation dialog
    private func sendCrack() {
        isSendingCrack = true
        errorMessage = nil

        Task {
            do {
                _ = try await ConvocationRepository.shared.childSendMessage(message: "Mom,here")
                DispatchQueue.main.async {
                    self.isSendingCrack = false
                    self.successPopupMessage = "Message sent"
                    self.showSuccessDialog = true
                }
            } catch {
                DispatchQueue.main.async {
                    self.isSendingCrack = false
                    self.errorMessage = "Failed to dispatch CRACK note. Check network connection."
                }
            }
        }
    }

    // FREAK: Send immediately "Someone's,here" without pre-confirmation dialog
    private func sendFreak() {
        isSendingFreak = true
        errorMessage = nil

        Task {
            do {
                _ = try await ConvocationRepository.shared.childSendMessage(message: "Someone's,here")
                DispatchQueue.main.async {
                    self.isSendingFreak = false
                    self.successPopupMessage = "Message sent"
                    self.showSuccessDialog = true
                }
            } catch {
                DispatchQueue.main.async {
                    self.isSendingFreak = false
                    self.errorMessage = "Failed to dispatch FREAK note. Check network connection."
                }
            }
        }
    }
}
