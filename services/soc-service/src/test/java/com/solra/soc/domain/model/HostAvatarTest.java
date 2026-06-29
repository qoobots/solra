package com.solra.soc.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * HostAvatar 聚合根单元测试。
 */
class HostAvatarTest {

    @Test
    void shouldCreateHostWithFreeFlowMode() {
        HostAvatar host = new HostAvatar("h1", "s1", "a1", "主持人小S", HostAvatar.HostMode.FREE_FLOW);

        assertEquals("h1", host.getHostId());
        assertEquals("s1", host.getSessionId());
        assertEquals(HostAvatar.HostState.IDLE, host.getState());
        assertEquals(HostAvatar.HostMode.FREE_FLOW, host.getMode());
    }

    @Test
    void shouldStartHosting() {
        HostAvatar host = new HostAvatar("h1", "s1", "a1", "Test", HostAvatar.HostMode.GUIDED);
        host.start();

        assertEquals(HostAvatar.HostState.GUIDING, host.getState());
        assertTrue(host.isActive());
    }

    @Test
    void shouldPauseAndResume() {
        HostAvatar host = new HostAvatar("h1", "s1", "a1", "Test", HostAvatar.HostMode.GUIDED);
        host.start();
        host.pause();

        assertEquals(HostAvatar.HostState.PAUSED, host.getState());
        assertFalse(host.isActive());

        host.resume();
        assertEquals(HostAvatar.HostState.GUIDING, host.getState());
    }

    @Test
    void shouldSwitchMode() {
        HostAvatar host = new HostAvatar("h1", "s1", "a1", "Test", HostAvatar.HostMode.FREE_FLOW);
        host.switchMode(HostAvatar.HostMode.ROUND_ROBIN);

        assertEquals(HostAvatar.HostMode.ROUND_ROBIN, host.getMode());
    }

    @Test
    void shouldManageTopicQueue() {
        HostAvatar host = new HostAvatar("h1", "s1", "a1", "Test", HostAvatar.HostMode.GUIDED);
        host.addTopic("话题1");
        host.addTopic("话题2");

        assertEquals(2, host.getTopicQueueSize());

        host.setCurrentTopic("当前话题");
        assertEquals("当前话题", host.getCurrentTopic());

        String next = host.nextTopic();
        assertEquals("话题1", next);
        assertEquals(1, host.getTopicQueueSize());
    }

    @Test
    void shouldNotAddDuplicateTopic() {
        HostAvatar host = new HostAvatar("h1", "s1", "a1", "Test", HostAvatar.HostMode.GUIDED);
        host.addTopic("话题1");
        host.addTopic("话题1");

        assertEquals(1, host.getTopicQueueSize());
    }

    @Test
    void shouldManageSpeakerQueue() {
        HostAvatar host = new HostAvatar("h1", "s1", "a1", "Test", HostAvatar.HostMode.ROUND_ROBIN);
        host.addToSpeakerQueue("u1");
        host.addToSpeakerQueue("u2");
        host.addToSpeakerQueue("u3");

        assertEquals(3, host.getSpeakerQueueSize());

        String speaker = host.grantSpeakingTurn();
        assertEquals("u1", speaker);
        assertEquals("u1", host.getActiveSpeaker());
        assertEquals(2, host.getSpeakerQueueSize());

        host.endSpeakingTurn();
        assertNull(host.getActiveSpeaker());
    }

    @Test
    void shouldRecordInteraction() {
        HostAvatar host = new HostAvatar("h1", "s1", "a1", "Test", HostAvatar.HostMode.GUIDED);
        host.recordInteraction();
        host.recordInteraction();
        host.recordInteraction();

        assertEquals(3, host.getTotalInteractions());
    }

    @Test
    void shouldReturnNullWhenSpeakerQueueEmpty() {
        HostAvatar host = new HostAvatar("h1", "s1", "a1", "Test", HostAvatar.HostMode.ROUND_ROBIN);
        String speaker = host.grantSpeakingTurn();

        assertNull(speaker);
    }

    @Test
    void shouldReturnNullWhenTopicQueueEmpty() {
        HostAvatar host = new HostAvatar("h1", "s1", "a1", "Test", HostAvatar.HostMode.GUIDED);
        String topic = host.nextTopic();

        assertNull(topic);
    }

    @Test
    void shouldDetectIdleStateAsNotActive() {
        HostAvatar host = new HostAvatar("h1", "s1", "a1", "Test", HostAvatar.HostMode.FREE_FLOW);
        assertFalse(host.isActive());
    }
}
