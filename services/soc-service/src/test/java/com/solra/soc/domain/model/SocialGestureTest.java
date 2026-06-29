package com.solra.soc.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SocialGesture 实体单元测试 — SOC-003 空间社交信号系统。
 */
@DisplayName("SocialGesture 实体测试")
class SocialGestureTest {

    @Test
    @DisplayName("创建举手信号")
    void shouldCreateRaiseHand() {
        SocialGesture g = SocialGesture.raiseHand("sess-1", "user-a");
        assertEquals(SocialGesture.GestureSignal.RAISE_HAND, g.getSignal());
        assertEquals("sess-1", g.getSessionId());
        assertEquals("user-a", g.getFromUserId());
        assertEquals(SocialGesture.SignalIntensity.NORMAL, g.getIntensity());
        assertNull(g.getTargetUserId());
        assertTrue(g.isInstantaneous());
    }

    @Test
    @DisplayName("创建鼓掌信号")
    void shouldCreateApplause() {
        SocialGesture g = SocialGesture.applaud("sess-1", "user-a", SocialGesture.SignalIntensity.HIGH);
        assertEquals(SocialGesture.GestureSignal.APPLAUD, g.getSignal());
        assertEquals(SocialGesture.SignalIntensity.HIGH, g.getIntensity());
        assertEquals(3000, g.getDurationMs());
        assertFalse(g.isInstantaneous());
    }

    @Test
    @DisplayName("创建请求安静信号")
    void shouldCreateRequestSilence() {
        SocialGesture g = SocialGesture.requestSilence("sess-1", "user-a");
        assertEquals(SocialGesture.GestureSignal.REQUEST_SILENCE, g.getSignal());
        assertEquals("请大家安静一下", g.getMessage());
    }

    @Test
    @DisplayName("创建定向点赞信号")
    void shouldCreateDirectedThumbsUp() {
        SocialGesture g = SocialGesture.thumbsUp("sess-1", "user-a", "user-b");
        assertEquals(SocialGesture.GestureSignal.THUMBS_UP, g.getSignal());
        assertEquals("user-b", g.getTargetUserId());
        assertTrue(g.isDirected());
        assertEquals(2000, g.getDurationMs());
    }

    @Test
    @DisplayName("创建跺脚信号")
    void shouldCreateStomp() {
        SocialGesture g = SocialGesture.stomp("sess-1", "user-a", SocialGesture.SignalIntensity.HIGH);
        assertEquals(SocialGesture.GestureSignal.STOMP, g.getSignal());
        assertEquals(1500, g.getDurationMs());
    }

    @Test
    @DisplayName("创建舞蹈信号")
    void shouldCreateDance() {
        SocialGesture g = SocialGesture.dance("sess-1", "user-a");
        assertEquals(SocialGesture.GestureSignal.DANCE, g.getSignal());
        assertEquals(SocialGesture.SignalIntensity.HIGH, g.getIntensity());
        assertEquals(5000, g.getDurationMs());
    }

    @Test
    @DisplayName("确认信号")
    void shouldAcknowledge() {
        SocialGesture g = SocialGesture.raiseHand("sess-1", "user-a");
        assertFalse(g.isAcknowledged());
        g.acknowledge();
        assertTrue(g.isAcknowledged());
    }

    @Test
    @DisplayName("瞬时信号判断")
    void shouldDetectInstantaneous() {
        SocialGesture g = SocialGesture.raiseHand("sess-1", "user-a");
        assertTrue(g.isInstantaneous());
        assertFalse(g.isActive());
    }

    @Test
    @DisplayName("持续信号活跃判断")
    void shouldDetectActiveGesture() {
        SocialGesture g = SocialGesture.applaud("sess-1", "user-a", SocialGesture.SignalIntensity.NORMAL);
        assertTrue(g.isActive()); // just created, should be active
    }

    @Test
    @DisplayName("8种信号类型完整覆盖")
    void shouldCoverAllEightGestureTypes() {
        SocialGesture.GestureSignal[] expected = {
                SocialGesture.GestureSignal.RAISE_HAND,
                SocialGesture.GestureSignal.APPLAUD,
                SocialGesture.GestureSignal.REQUEST_SILENCE,
                SocialGesture.GestureSignal.THUMBS_UP,
                SocialGesture.GestureSignal.STOMP,
                SocialGesture.GestureSignal.DANCE,
                SocialGesture.GestureSignal.BOW,
                SocialGesture.GestureSignal.LAUGH
        };
        assertEquals(8, SocialGesture.GestureSignal.values().length);
        assertArrayEquals(expected, SocialGesture.GestureSignal.values());
    }
}
