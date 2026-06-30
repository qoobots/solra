import XCTest
@testable import SolraApp

/// 推送通知服务单元测试
final class PushNotificationServiceTests: XCTestCase {

    var sut: PushNotificationService!

    override func setUp() {
        super.setUp()
        sut = PushNotificationService.shared
    }

    override func tearDown() {
        sut = nil
        super.tearDown()
    }

    // MARK: - Initial State

    func test_initialState_isNotAuthorized() {
        XCTAssertFalse(sut.isAuthorized, "初始状态应为未授权")
        XCTAssertNil(sut.deviceToken, "初始状态 deviceToken 应为 nil")
        XCTAssertNil(sut.deviceTokenString, "初始状态 deviceTokenString 应为 nil")
    }

    // MARK: - Token Processing

    func test_didRegisterWithDeviceToken_generatesTokenString() {
        let testToken = Data([0x01, 0xAB, 0xFF, 0x00])
        sut.didRegisterWithDeviceToken(testToken)

        XCTAssertNotNil(sut.deviceTokenString)
        XCTAssertEqual(sut.deviceTokenString, "01abff00")
    }

    func test_didRegisterWithDeviceToken_emptyToken() {
        sut.didRegisterWithDeviceToken(Data())

        XCTAssertEqual(sut.deviceTokenString, "")
    }

    // MARK: - Singleton

    func test_shared_isSameInstance() {
        let instance1 = PushNotificationService.shared
        let instance2 = PushNotificationService.shared
        XCTAssertTrue(instance1 === instance2, "应为同一单例实例")
    }
}
