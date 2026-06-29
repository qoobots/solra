/// Solra Core SDK Swift 桥接层 — C ABI 封装。
/// 将 C++ Core SDK 的 C 接口封装为 Swift 友好的 API。
import Foundation

/// Core SDK 桥接管理器
final class SolraCoreBridge {
    static let shared = SolraCoreBridge()

    private init() {}

    /// 初始化 Core SDK
    func initialize() -> Bool {
        // TODO: P1 — 加载 SolraCore.framework 并调用 solra_init()
        return true
    }

    /// 版本信息
    func version() -> String {
        // TODO: P1 — solra_version()
        return "0.1.0"
    }
}
