import SwiftUI

/// Solra iOS 应用入口。
/// 架构：SwiftUI + Core SDK (C ABI 桥接)
@main
struct SolraApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
        }
    }
}

/// 全局应用状态管理
final class AppState: ObservableObject {
    @Published var isLoggedIn = false
    @Published var currentUser: UserInfo?
}

struct UserInfo {
    let userId: String
    let displayName: String
}
