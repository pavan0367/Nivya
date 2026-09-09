import SwiftUI

public struct SettingsView: View {
    private var isParent: Bool {
        AppPreferences.shared.savedRole == RoleType.parent.rawValue
    }

    public init() {}

    public var body: some View {
        if isParent {
            ParentSettingsView()
        } else {
            ChildSettingsView()
        }
    }
}
