import SwiftUI
import UserNotifications

/// Solra iOS 应用入口。
/// 架构：SwiftUI + Core SDK (C ABI 桥接)
@main
struct SolraApp: App {
    @StateObject private var appState = AppState()
    @StateObject private var pushService = PushNotificationService.shared
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .environmentObject(pushService)
                .onAppear {
                    pushService.registerForPushNotifications()
                    pushService.registerNotificationCategories()
                }
        }
    }
}

/// UIApplicationDelegate — 处理 APNs 注册回调和远程通知
final class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // 设置通知中心代理
        UNUserNotificationCenter.current().delegate = PushNotificationService.shared
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        PushNotificationService.shared.didRegisterWithDeviceToken(deviceToken)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        PushNotificationService.shared.didFailToRegisterWithError(error)
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
