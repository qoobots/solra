import Foundation
import UserNotifications
import UIKit

/// 推送通知服务 — 管理 APNs 注册、设备 Token 上报、本地通知调度
/// @since 0.1.0
final class PushNotificationService: NSObject, ObservableObject {

    static let shared = PushNotificationService()

    // MARK: - Published State

    @Published var isAuthorized = false
    @Published var deviceToken: Data?
    @Published var deviceTokenString: String?

    // MARK: - Init

    private override init() {
        super.init()
    }

    // MARK: - Registration

    /// 请求通知权限并注册远程推送
    func registerForPushNotifications() {
        let center = UNUserNotificationCenter.current()
        center.delegate = self

        center.requestAuthorization(options: [.alert, .badge, .sound]) { [weak self] granted, error in
            DispatchQueue.main.async {
                self?.isAuthorized = granted
                if let error = error {
                    print("[PushNotification] Authorization error: \(error.localizedDescription)")
                }
                if granted {
                    self?.registerForRemote()
                }
            }
        }
    }

    private func registerForRemote() {
        DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
        }
    }

    // MARK: - Token Management

    /// 由 AppDelegate/SceneDelegate 在 didRegisterForRemoteNotificationsWithDeviceToken 中调用
    func didRegisterWithDeviceToken(_ token: Data) {
        deviceToken = token
        deviceTokenString = token.map { String(format: "%02x", $0) }.joined()
        print("[PushNotification] Device token: \(deviceTokenString ?? "nil")")

        // 将 token 上报到 Solra 后端通知服务
        uploadDeviceToken()
    }

    func didFailToRegisterWithError(_ error: Error) {
        print("[PushNotification] Registration failed: \(error.localizedDescription)")
    }

    /// 上报设备 Token 到后端 not-service
    private func uploadDeviceToken() {
        guard let token = deviceTokenString else { return }

        // TODO: P1 — 调用 not-service gRPC DeviceRegistration API
        // let request = Solra_Not_V1_DeviceRegistrationRequest.with {
        //     $0.deviceToken = token
        //     $0.platform = .ios
        //     $0.pushEnabled = true
        // }
        // SolraAPI.shared.notClient.registerDevice(request) { response, error in ... }

        print("[PushNotification] Device token ready for upload: \(token)")
    }

    // MARK: - Local Notification (Testing / Fallback)

    /// 发送本地通知（用于测试或离线场景）
    func scheduleLocalNotification(title: String, body: String, delay: TimeInterval = 1.0) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        content.badge = 1

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(delay, 1.0), repeats: false)
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: trigger)

        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("[PushNotification] Local notification error: \(error.localizedDescription)")
            }
        }
    }

    /// 更新角标数
    func updateBadgeCount(_ count: Int) {
        DispatchQueue.main.async {
            UIApplication.shared.applicationIconBadgeNumber = count
        }
    }

    // MARK: - Notification Categories & Actions

    /// 注册通知操作类别（如：回复、查看、忽略）
    func registerNotificationCategories() {
        let viewAction = UNNotificationAction(
            identifier: "VIEW_ACTION",
            title: "查看",
            options: .foreground
        )
        let replyAction = UNTextInputNotificationAction(
            identifier: "REPLY_ACTION",
            title: "回复",
            options: [],
            textInputButtonTitle: "发送",
            textInputPlaceholder: "输入回复..."
        )

        let category = UNNotificationCategory(
            identifier: "MESSAGE_CATEGORY",
            actions: [viewAction, replyAction],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )

        UNUserNotificationCenter.current().setNotificationCategories([category])
    }
}

// MARK: - UNUserNotificationCenterDelegate

extension PushNotificationService: UNUserNotificationCenterDelegate {

    /// 前台展示通知
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // 前台展示横幅 + 声音 + 角标
        completionHandler([.banner, .sound, .badge])
    }

    /// 用户点击通知操作
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo

        switch response.actionIdentifier {
        case UNNotificationDefaultActionIdentifier:
            handleNotificationTap(userInfo: userInfo)
        case "VIEW_ACTION":
            handleNotificationTap(userInfo: userInfo)
        case "REPLY_ACTION":
            if let textResponse = response as? UNTextInputNotificationResponse {
                handleNotificationReply(text: textResponse.userText, userInfo: userInfo)
            }
        default:
            break
        }

        completionHandler()
    }

    private func handleNotificationTap(userInfo: [AnyHashable: Any]) {
        // TODO: P1 — 根据通知类型进行深度链接导航
        // 如：空间邀请 → 打开 SpaceDetailView
        // 如：新消息 → 打开 AvatarView 对应对话
        if let spaceId = userInfo["space_id"] as? String {
            print("[PushNotification] Navigate to space: \(spaceId)")
        }
        if let avatarId = userInfo["avatar_id"] as? String {
            print("[PushNotification] Navigate to avatar conversation: \(avatarId)")
        }
    }

    private func handleNotificationReply(text: String, userInfo: [AnyHashable: Any]) {
        // TODO: P1 — 将回复通过 gRPC 发送给虚拟人
        print("[PushNotification] Reply: \(text)")
    }
}
