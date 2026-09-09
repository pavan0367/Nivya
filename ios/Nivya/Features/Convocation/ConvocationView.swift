import SwiftUI

public struct ConvocationView: View {
    @ObservedObject private var appState = AppState.shared
    @State private var messages: [ConvocationMessageResponse] = []
    @State private var remainingViewingSeconds: Int = 120
    @State private var isConvocationActive: Bool = true
    @State private var messageInput: String = ""
    @State private var isSending: Bool = false
    @State private var isLoading: Bool = false
    @State private var viewModeTimer: Timer?

    private var isParent: Bool {
        AppPreferences.shared.savedRole == RoleType.parent.rawValue
    }

    public init() {}

    public var body: some View {
        ZStack {
            NivyaColors.backgroundDark.ignoresSafeArea()

            VStack(spacing: 0) {
                // Topbar with 2-Minute Countdown and Convocation Status
                HStack {
                    Button(action: { returnToDashboard() }) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(.white)
                            .font(.system(size: 16, weight: .semibold))
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Convocation")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)
                        Text(isParent ? "Parent Active History" : "Viewing Mode (Expiring)")
                            .font(.system(size: 11))
                            .foregroundColor(NivyaColors.textSecondary)
                    }

                    Spacer()

                    // 2-Minute Viewing Mode Pill
                    HStack(spacing: 4) {
                        Image(systemName: "timer")
                            .font(.system(size: 12))
                            .foregroundColor(NivyaColors.tealAccent)
                        Text("\(remainingViewingSeconds / 60):\(String(format: "%02d", remainingViewingSeconds % 60))")
                            .font(.system(size: 13, weight: .bold, design: .monospaced))
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(NivyaColors.surfaceCard)
                    .cornerRadius(10)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(NivyaColors.surfaceDark)

                // Message Stream
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(messages) { msg in
                            messageBubble(msg)
                        }
                    }
                    .padding(16)
                }

                // Bottom Composer
                HStack(spacing: 12) {
                    TextField("Type safety message...", text: $messageInput)
                        .foregroundColor(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(NivyaColors.surfaceDark)
                        .cornerRadius(20)
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
                                .stroke(NivyaColors.outlineDark, lineWidth: 1)
                        )

                    Button(action: sendMessage) {
                        Image(systemName: "paperplane.fill")
                            .foregroundColor(.white)
                            .padding(10)
                            .background(NivyaColors.primaryGradient)
                            .clipShape(Circle())
                    }
                    .disabled(messageInput.trimmingCharacters(in: .whitespaces).isEmpty || isSending)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(NivyaColors.surfaceCard)
            }
        }
        .onAppear {
            loadConvocation()
            startViewingTimer()
        }
        .onDisappear {
            viewModeTimer?.invalidate()
        }
    }

    private func messageBubble(_ msg: ConvocationMessageResponse) -> some View {
        let isMine = isParent ? msg.isFromParent : !msg.isFromParent

        return HStack {
            if isMine { Spacer() }

            VStack(alignment: isMine ? .trailing : .leading, spacing: 4) {
                // Special Badge for CRACK and FREAK notes
                if msg.isCrackEvent {
                    HStack(spacing: 4) {
                        Image(systemName: "exclamationmark.triangle.fill")
                        Text("CRACK ALERT")
                    }
                    .font(.system(size: 10, weight: .heavy))
                    .foregroundColor(NivyaColors.errorRed)
                } else if msg.isFreakEvent {
                    HStack(spacing: 4) {
                        Image(systemName: "shield.lefthalf.filled")
                        Text("FREAK ALERT")
                    }
                    .font(.system(size: 10, weight: .heavy))
                    .foregroundColor(NivyaColors.purpleLight)
                }

                Text(msg.message)
                    .font(.system(size: 15))
                    .foregroundColor(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(
                        msg.isCrackEvent ? NivyaColors.errorRed.opacity(0.3) :
                        msg.isFreakEvent ? NivyaColors.purpleAccent.opacity(0.3) :
                        isMine ? NivyaColors.bluePrimary : NivyaColors.surfaceCard
                    )
                    .cornerRadius(16)

                // Timestamp & Seen status (Seen only displayed on Parent side per Invariant 18)
                HStack(spacing: 4) {
                    Text(formatTimestamp(msg.timestamp))
                        .font(.system(size: 10))
                        .foregroundColor(NivyaColors.textMuted)

                    if isParent && isMine {
                        if msg.isSeen {
                            Text("• Seen")
                                .font(.system(size: 10, weight: .medium))
                                .foregroundColor(NivyaColors.tealAccent)
                        } else {
                            Text("• Sent")
                                .font(.system(size: 10))
                                .foregroundColor(NivyaColors.textMuted)
                        }
                    }
                }
            }

            if !isMine { Spacer() }
        }
    }

    private func formatTimestamp(_ iso: String) -> String {
        return "Just now"
    }

    private func returnToDashboard() {
        if isParent {
            appState.currentDestination = .parentDashboard
        } else {
            appState.currentDestination = .childDashboard
        }
    }

    private func startViewingTimer() {
        viewModeTimer?.invalidate()
        viewModeTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            if remainingViewingSeconds > 0 {
                remainingViewingSeconds -= 1
            } else {
                // Invariant: 2-minute viewing mode expiration on child side
                if !isParent {
                    returnToDashboard()
                }
            }
        }
    }

    private func loadConvocation() {
        isLoading = true
        Task {
            do {
                let list = try await ConvocationRepository.shared.getHistory()
                let state = try? await ConvocationRepository.shared.getState()

                DispatchQueue.main.async {
                    self.messages = list
                    if let state = state {
                        self.remainingViewingSeconds = state.remainingSeconds
                        self.isConvocationActive = state.isActive
                    }
                    self.isLoading = false
                }
            } catch {
                DispatchQueue.main.async {
                    self.isLoading = false
                }
            }
        }
    }

    private func sendMessage() {
        let text = messageInput.trimmingCharacters(in: .whitespaces)
        guard !text.isEmpty else { return }

        isSending = true
        messageInput = ""

        Task {
            do {
                if isParent {
                    let targetId = AppPreferences.shared.activeChildDeviceId ?? 1
                    let sent = try await ConvocationRepository.shared.parentSendMessage(message: text, childDeviceId: targetId)
                    DispatchQueue.main.async {
                        self.messages.append(sent)
                        self.isSending = false
                    }
                } else {
                    let sent = try await ConvocationRepository.shared.childSendMessage(message: text)
                    DispatchQueue.main.async {
                        self.messages.append(sent)
                        self.isSending = false
                    }
                }
            } catch {
                DispatchQueue.main.async {
                    self.isSending = false
                }
            }
        }
    }
}
