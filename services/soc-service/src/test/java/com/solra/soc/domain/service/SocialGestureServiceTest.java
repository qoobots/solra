package com.solra.soc.domain.service;

import com.solra.soc.domain.model.SocialGesture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SocialGestureService 领域服务单元测试 — SOC-003 空间社交信号系统。
 */
@DisplayName("SocialGestureService 领域服务测试")
class SocialGestureServiceTest {

    private SocialGestureService service;

    @BeforeEach
    void setUp() {
        service = new SocialGestureService();
    }

    @Test
    @DisplayName("发送举手信号")
    void shouldSendRaiseHand() {
        SocialGesture g = service.raiseHand("sess-1", "user-a");
        assertEquals(SocialGesture.GestureSignal.RAISE_HAND, g.getSignal());
        assertEquals("sess-1", g.getSessionId());
    }

    @Test
    @DisplayName("发送鼓掌信号")
    void shouldSendApplause() {
        SocialGesture g = service.applaud("sess-1", "user-a", SocialGesture.SignalIntensity.HIGH);
        assertEquals(SocialGesture.GestureSignal.APPLAUD, g.getSignal());
    }

    @Test
    @DisplayName("发送请求安静信号")
    void shouldSendRequestSilence() {
        SocialGesture g = service.requestSilence("sess-1", "user-a");
        assertEquals(SocialGesture.GestureSignal.REQUEST_SILENCE, g.getSignal());
    }

    @Test
    @DisplayName("发送点赞信号")
    void shouldSendThumbsUp() {
        SocialGesture g = service.thumbsUp("sess-1", "user-a", "user-b");
        assertEquals("user-b", g.getTargetUserId());
    }

    @Test
    @DisplayName("发送跺脚信号")
    void shouldSendStomp() {
        SocialGesture g = service.stomp("sess-1", "user-a", SocialGesture.SignalIntensity.HIGH);
        assertEquals(SocialGesture.GestureSignal.STOMP, g.getSignal());
    }

    @Test
    @DisplayName("发送舞蹈信号")
    void shouldSendDance() {
        SocialGesture g = service.dance("sess-1", "user-a");
        assertEquals(SocialGesture.GestureSignal.DANCE, g.getSignal());
    }

    @Test
    @DisplayName("获取最近信号")
    void shouldGetRecentGestures() {
        service.raiseHand("sess-1", "user-a");
        service.applaud("sess-1", "user-b", SocialGesture.SignalIntensity.NORMAL);
        service.thumbsUp("sess-1", "user-a", "user-b");

        List<SocialGesture> recent = service.getRecentGestures("sess-1", 5);
        assertEquals(3, recent.size());
    }

    @Test
    @DisplayName("获取增量信号")
    void shouldGetGesturesSince() {
        SocialGesture g1 = service.raiseHand("sess-1", "user-a");
        SocialGesture g2 = service.applaud("sess-1", "user-b", SocialGesture.SignalIntensity.NORMAL);
        service.thumbsUp("sess-1", "user-a", "user-b");

        List<SocialGesture> since = service.getGesturesSince("sess-1", g1.getGestureId());
        assertEquals(1, since.size());
        assertEquals(g2.getGestureId(), since.get(0).getGestureId());
    }

    @Test
    @DisplayName("获取活跃信号")
    void shouldGetActiveGestures() {
        // Applause has 3000ms duration, so it should be active immediately
        service.applaud("sess-1", "user-a", SocialGesture.SignalIntensity.NORMAL);

        List<SocialGesture> active = service.getActiveGestures("sess-1");
        assertTrue(active.size() >= 1); // at least the applaud is active
    }

    @Test
    @DisplayName("确认信号")
    void shouldAcknowledgeGesture() {
        SocialGesture g = service.raiseHand("sess-1", "user-a");
        assertFalse(g.isAcknowledged());

        service.acknowledgeGesture("sess-1", g.getGestureId());
        List<SocialGesture> recent = service.getRecentGestures("sess-1", 1);
        assertTrue(recent.get(0).isAcknowledged());
    }

    @Test
    @DisplayName("获取社交信号统计")
    void shouldGetGestureStats() {
        service.raiseHand("sess-1", "user-a");
        service.raiseHand("sess-1", "user-a");
        service.applaud("sess-1", "user-b", SocialGesture.SignalIntensity.NORMAL);
        service.thumbsUp("sess-1", "user-a", "user-b");

        SocialGestureService.GestureStats stats = service.getStats("sess-1");
        assertEquals(4, stats.totalGestures());
        assertNotNull(stats.signalCounts());
        assertTrue(stats.signalCounts().containsKey(SocialGesture.GestureSignal.RAISE_HAND));
        assertEquals(2L, stats.signalCounts().get(SocialGesture.GestureSignal.RAISE_HAND));
    }

    @Test
    @DisplayName("冷却时间生效")
    void shouldEnforceCooldown() {
        service.raiseHand("sess-1", "user-a");

        // Immediate second gesture from same user should fail
        assertThrows(IllegalStateException.class, () -> {
            service.raiseHand("sess-1", "user-a");
        });
    }

    @Test
    @DisplayName("清理会话信号")
    void shouldCleanupSession() {
        service.raiseHand("sess-1", "user-a");
        service.applaud("sess-1", "user-b", SocialGesture.SignalIntensity.NORMAL);

        service.cleanup("sess-1");
        List<SocialGesture> recent = service.getRecentGestures("sess-1", 10);
        assertTrue(recent.isEmpty());
    }

    @Test
    @DisplayName("获取用户信号")
    void shouldGetGesturesByUser() {
        service.raiseHand("sess-1", "user-a");
        service.applaud("sess-1", "user-b", SocialGesture.SignalIntensity.NORMAL);
        service.thumbsUp("sess-1", "user-a", "user-b");

        List<SocialGesture> userAGestures = service.getGesturesByUser("sess-1", "user-a");
        assertEquals(2, userAGestures.size());
    }

    @Test
    @DisplayName("通用发送信号")
    void shouldSendCustomGesture() {
        SocialGesture g = SocialGesture.create("sess-1", "user-a",
                SocialGesture.GestureSignal.BOW,
                SocialGesture.SignalIntensity.SUBTLE,
                "user-c", "感谢大家", 2000);

        SocialGesture result = service.sendGesture(g);
        assertEquals(SocialGesture.GestureSignal.BOW, result.getSignal());
        assertEquals("感谢大家", result.getMessage());
    }
}
