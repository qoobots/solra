package com.solra.soc.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AudioSource 实体单元测试 — SOC-007 空间音频引擎。
 */
@DisplayName("AudioSource 实体测试")
class AudioSourceTest {

    @Test
    @DisplayName("创建麦克风声源")
    void shouldCreateMicrophone() {
        AudioSource src = AudioSource.microphone("sess-1", "user-a", 1, 2, 3);
        assertEquals("mic_user-a", src.getSourceId());
        assertEquals(AudioSource.AudioSourceType.MICROPHONE, src.getType());
        assertEquals(1f, src.getPositionX(), 0.001f);
        assertEquals(2f, src.getPositionY(), 0.001f);
        assertEquals(3f, src.getPositionZ(), 0.001f);
        assertEquals(0.8f, src.getVolume(), 0.001f);
        assertEquals(1f, src.getMinDistance(), 0.001f);
        assertEquals(15f, src.getMaxDistance(), 0.001f);
        assertTrue(src.isSpatialized());
        assertFalse(src.isLoop());
    }

    @Test
    @DisplayName("创建背景音乐声源")
    void shouldCreateBackgroundMusic() {
        AudioSource src = AudioSource.backgroundMusic("sess-1", "jazz", 0.5f);
        assertEquals("bgm_jazz", src.getSourceId());
        assertEquals(AudioSource.AudioSourceType.BACKGROUND_MUSIC, src.getType());
        assertEquals(0.5f, src.getVolume(), 0.001f);
        assertFalse(src.isSpatialized());
        assertTrue(src.isLoop());
    }

    @Test
    @DisplayName("创建环境音声源")
    void shouldCreateAmbient() {
        AudioSource src = AudioSource.ambient("sess-1", "rain", 5, 0, 10, 0.3f, 20f);
        assertEquals("amb_rain", src.getSourceId());
        assertEquals(AudioSource.AudioSourceType.AMBIENT, src.getType());
        assertEquals(0.3f, src.getVolume(), 0.001f);
        assertEquals(20f, src.getMaxDistance(), 0.001f);
        assertTrue(src.isLoop());
    }

    @Test
    @DisplayName("创建音效声源")
    void shouldCreateSoundEffect() {
        AudioSource src = AudioSource.soundEffect("sess-1", "explosion", 3, 1, 0, 1.0f, 25f);
        assertEquals("sfx_explosion", src.getSourceId());
        assertEquals(AudioSource.AudioSourceType.SOUND_EFFECT, src.getType());
        assertEquals(1.0f, src.getVolume(), 0.001f);
        assertEquals(25f, src.getMaxDistance(), 0.001f);
    }

    @Test
    @DisplayName("更新位置")
    void shouldUpdatePosition() {
        AudioSource src = AudioSource.microphone("sess-1", "user-a", 0, 0, 0);
        src.updatePosition(10, 5, -3);
        assertEquals(10f, src.getPositionX(), 0.001f);
        assertEquals(5f, src.getPositionY(), 0.001f);
        assertEquals(-3f, src.getPositionZ(), 0.001f);
    }

    @Test
    @DisplayName("更新音量")
    void shouldUpdateVolume() {
        AudioSource src = AudioSource.microphone("sess-1", "user-a", 0, 0, 0);
        src.updateVolume(0.3f);
        assertEquals(0.3f, src.getVolume(), 0.001f);
    }

    @Test
    @DisplayName("音量裁剪")
    void shouldClampVolume() {
        AudioSource src = AudioSource.microphone("sess-1", "user-a", 0, 0, 0);
        src.updateVolume(5.0f);
        assertEquals(1.0f, src.getVolume(), 0.001f); // clamped to 1.0
        src.updateVolume(-1.0f);
        assertEquals(0.0f, src.getVolume(), 0.001f); // clamped to 0.0
    }
}
