import SwiftUI

public extension Color {
    init(hex: UInt, alpha: Double = 1.0) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xff) / 255,
            green: Double((hex >> 08) & 0xff) / 255,
            blue: Double((hex >> 00) & 0xff) / 255,
            opacity: alpha
        )
    }
}

public struct NivyaColors {
    public static let bluePrimary = Color(hex: 0x2563EB)
    public static let blueSecondary = Color(hex: 0x3B82F6)
    public static let blueLight = Color(hex: 0x60A5FA)
    public static let purpleAccent = Color(hex: 0x7C3AED)
    public static let purpleLight = Color(hex: 0xA78BFA)
    public static let tealAccent = Color(hex: 0x14B8A6)

    public static let backgroundDark = Color(hex: 0x090D16)
    public static let surfaceDark = Color(hex: 0x0F172A)
    public static let surfaceCard = Color(hex: 0x1E293B)
    public static let outlineDark = Color(hex: 0x334155)

    public static let textPrimary = Color(hex: 0xF8FAFC)
    public static let textSecondary = Color(hex: 0x94A3B8)
    public static let textMuted = Color(hex: 0x64748B)

    public static let successGreen = Color(hex: 0x10B981)
    public static let errorRed = Color(hex: 0xEF4444)
    public static let warningAmber = Color(hex: 0xF59E0B)

    public static let primaryGradient = LinearGradient(
        colors: [bluePrimary, purpleAccent],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
}
