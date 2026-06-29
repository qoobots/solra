package com.solra.soc.domain.service;

import com.solra.soc.domain.model.AudioListener;
import com.solra.soc.domain.model.AudioSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SpatialAudioEngine 领域服务单元测试 — SOC-007 空间音频引擎。
 */
@DisplayName("SpatialAudioEngine 空间音频引擎测试")
class SpatialAudioEngineTest {

    private SpatialAudioEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SpatialAudioEngine();
    }

    @Test
    @DisplayName("注册麦克风声源")
    void shouldRegisterMicrophone() {
        AudioSource src = engine.registerMicrophone("sess-1", "user-a", 0, 0, 0);
        assertEquals(AudioSource.AudioSourceType.MICROPHONE, src.getType());
        assertEquals("mic_user-a", src.getSourceId());
        assertTrue(src.isSpatialized());
    }

    @Test
    @DisplayName("注册背景音乐声源（非空间化）")
    void shouldRegisterBackgroundMusic() {
        AudioSource src = engine.registerBackgroundMusic("sess-1", "bgm-lounge", 0.5f);
        assertEquals(AudioSource.AudioSourceType.BACKGROUND_MUSIC, src.getType());
        assertFalse(src.isSpatialized());
        assertTrue(src.isLoop());
    }

    @Test
    @DisplayName("注册环境音声源")
    void shouldRegisterAmbient() {
        AudioSource src = engine.registerAmbient("sess-1", "forest", 10, 0, 5, 0.4f, 30f);
        assertEquals(AudioSource.AudioSourceType.AMBIENT, src.getType());
        assertEquals(0.4f, src.getVolume(), 0.001f);
        assertEquals(30f, src.getMaxDistance(), 0.001f);
    }

    @Test
    @DisplayName("注册音效声源")
    void shouldRegisterSoundEffect() {
        AudioSource src = engine.registerSoundEffect("sess-1", "doorbell", 5, 1, 3, 0.8f, 10f);
        assertEquals(AudioSource.AudioSourceType.SOUND_EFFECT, src.getType());
    }

    @Test
    @DisplayName("移除声源")
    void shouldRemoveSource() {
        AudioSource src = engine.registerMicrophone("sess-1", "user-a", 0, 0, 0);
        engine.removeSource("sess-1", src.getSourceId());

        List<AudioSource> sources = engine.getSessionSources("sess-1");
        assertTrue(sources.isEmpty());
    }

    @Test
    @DisplayName("更新声源位置")
    void shouldUpdateSourcePosition() {
        AudioSource src = engine.registerMicrophone("sess-1", "user-a", 0, 0, 0);
        engine.updateSourcePosition("sess-1", src.getSourceId(), 5, 2, 3);

        AudioSource updated = engine.getSource("sess-1", src.getSourceId()).orElseThrow();
        assertEquals(5f, updated.getPositionX(), 0.001f);
        assertEquals(2f, updated.getPositionY(), 0.001f);
        assertEquals(3f, updated.getPositionZ(), 0.001f);
    }

    @Test
    @DisplayName("5米内语音清晰")
    void shouldBeClearWithin5Meters() {
        // Source at origin
        engine.registerMicrophone("sess-1", "speaker", 0, 0, 0);
        // Listener within 5m
        AudioListener listener = new AudioListener("listener", 2, 0, 3, 0, 0, 1);
        engine.registerSessionListener("sess-1", listener);

        List<SpatialAudioEngine.AudioMixResult> mix = engine.calculateMix("sess-1", "listener");
        assertEquals(1, mix.size());
        assertTrue(mix.get(0).isClear());
        assertTrue(mix.get(0).distance() <= 5f);
        assertEquals(0f, mix.get(0).reverb(), 0.001f);
    }

    @Test
    @DisplayName("5米外渐弱+混响")
    void shouldAttenuateBeyond5Meters() {
        engine.registerMicrophone("sess-1", "speaker", 0, 0, 0);
        AudioListener listener = new AudioListener("listener", 8, 0, 0, 0, 0, 1);
        engine.registerSessionListener("sess-1", listener);

        List<SpatialAudioEngine.AudioMixResult> mix = engine.calculateMix("sess-1", "listener");
        assertEquals(1, mix.size());
        assertFalse(mix.get(0).isClear());
        assertTrue(mix.get(0).distance() > 5f);
        assertTrue(mix.get(0).reverb() > 0f);
        assertTrue(mix.get(0).volume() < 0.8f); // attenuated from base
    }

    @Test
    @DisplayName("超远距离听不到")
    void shouldBeSilentAtMaxDistance() {
        engine.registerMicrophone("sess-1", "speaker", 0, 0, 0);
        AudioListener listener = new AudioListener("listener", 100, 0, 0, 0, 0, 1);
        engine.registerSessionListener("sess-1", listener);

        List<SpatialAudioEngine.AudioMixResult> mix = engine.calculateMix("sess-1", "listener");
        assertTrue(mix.isEmpty()); // too far, inaudible
    }

    @Test
    @DisplayName("非空间化声源不衰减")
    void shouldNotAttenuateNonSpatialized() {
        engine.registerBackgroundMusic("sess-1", "bgm", 0.5f);
        AudioListener listener = new AudioListener("listener", 100, 0, 0, 0, 0, 1);
        engine.registerSessionListener("sess-1", listener);

        List<SpatialAudioEngine.AudioMixResult> mix = engine.calculateMix("sess-1", "listener");
        assertEquals(1, mix.size());
        assertEquals(0.5f, mix.get(0).volume(), 0.001f);
        assertEquals(0f, mix.get(0).pan(), 0.001f);
    }

    @Test
    @DisplayName("立体声pan计算")
    void shouldCalculateStereoPan() {
        engine.registerMicrophone("sess-1", "speaker", 5, 0, 0);
        // Listener facing +Z, source is to the right (+X)
        AudioListener listener = new AudioListener("listener", 0, 0, 0, 0, 0, 1);
        engine.registerSessionListener("sess-1", listener);

        List<SpatialAudioEngine.AudioMixResult> mix = engine.calculateMix("sess-1", "listener");
        assertEquals(1, mix.size());
        // Source to the right should produce positive pan (>0)
        assertTrue(mix.get(0).pan() > 0f, "Pan should be positive for right-side source");
    }

    @Test
    @DisplayName("不混合自己麦克风")
    void shouldNotMixOwnMicrophone() {
        engine.registerMicrophone("sess-1", "listener", 0, 0, 0);
        AudioListener listener = new AudioListener("listener", 0, 0, 0, 0, 0, 1);
        engine.registerSessionListener("sess-1", listener);

        List<SpatialAudioEngine.AudioMixResult> mix = engine.calculateMix("sess-1", "listener");
        assertTrue(mix.isEmpty()); // shouldn't hear own mic
    }

    @Test
    @DisplayName("多声源混合")
    void shouldMixMultipleSources() {
        engine.registerMicrophone("sess-1", "speaker-a", 3, 0, 0);
        engine.registerMicrophone("sess-1", "speaker-b", -2, 0, 5);
        engine.registerBackgroundMusic("sess-1", "bgm", 0.3f);
        AudioListener listener = new AudioListener("listener", 0, 0, 0, 0, 0, 1);
        engine.registerSessionListener("sess-1", listener);

        List<SpatialAudioEngine.AudioMixResult> mix = engine.calculateMix("sess-1", "listener");
        assertEquals(3, mix.size());
    }

    @Test
    @DisplayName("音频引擎统计")
    void shouldGetEngineStats() {
        engine.registerMicrophone("sess-1", "user-a", 0, 0, 0);
        engine.registerMicrophone("sess-2", "user-b", 5, 0, 0);

        SpatialAudioEngine.AudioEngineStats stats = engine.getStats();
        assertEquals(2, stats.totalSessions());
        assertEquals(2, stats.totalSources());
    }

    @Test
    @DisplayName("清理会话音频资源")
    void shouldCleanupSession() {
        engine.registerMicrophone("sess-1", "user-a", 0, 0, 0);
        engine.registerBackgroundMusic("sess-1", "bgm", 0.5f);
        AudioListener listener = new AudioListener("user-a", 0, 0, 0, 0, 0, 1);
        engine.registerSessionListener("sess-1", listener);

        engine.cleanup("sess-1");
        assertTrue(engine.getSessionSources("sess-1").isEmpty());
    }
}
