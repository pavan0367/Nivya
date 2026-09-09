import SwiftUI

public struct SplashScreenView: View {
    @ObservedObject private var appState = AppState.shared

    public init() {}

    public var body: some View {
        ZStack {
            // Approved mother-and-child sunset hill illustration (Rule 0 authority)
            Image("LaunchImage")
                .resizable()
                .scaledToFill()
                .ignoresSafeArea()

            // Subtle dark overlay to ensure text legibility
            Color.black.opacity(0.35)
                .ignoresSafeArea()

            VStack {
                Spacer()

                VStack(spacing: 12) {
                    Image("Logo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 56, height: 56)

                    Text("Nivya")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(.white)

                    Text("Family Safety & Digital Wellbeing")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(NivyaColors.textSecondary)

                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: NivyaColors.tealAccent))
                        .scaleEffect(1.1)
                        .padding(.top, 16)
                }
                .padding(.bottom, 60)
            }
        }
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                appState.checkInitialRouting()
            }
        }
    }
}
