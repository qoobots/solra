package com.solra.crt.domain.service;

import com.solra.crt.domain.entity.AudioTrack;
import com.solra.crt.domain.entity.ProjectConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AudioLibraryService 单元测试 (CRT-007)。
 */
@DisplayName("AudioLibraryService 音频曲库")
class AudioLibraryServiceTest {

    private AudioLibraryService service;

    @BeforeEach
    void setUp() {
        service = new AudioLibraryService();
    }

    @Test
    @DisplayName("内置曲库应≥200首")
    void builtinLibraryShouldHaveAtLeast200Tracks() {
        int count = service.getBuiltinTrackCount();
        assertTrue(count >= 200,
                "Expected ≥200 builtin tracks, got " + count);
    }

    @Test
    @DisplayName("总曲目数≥200")
    void totalTracksShouldBeAtLeast200() {
        int count = service.getTotalTrackCount();
        assertTrue(count >= 200,
                "Expected ≥200 total tracks, got " + count);
    }

    @Test
    @DisplayName("可按分类列出BGM曲目")
    void shouldListByCategory() {
        List<AudioTrack> bgms = service.listByCategory(AudioTrack.TrackCategory.BGM, 0, 50);
        assertFalse(bgms.isEmpty(), "Should have BGM tracks");
        assertTrue(bgms.stream().allMatch(t -> t.getCategory() == AudioTrack.TrackCategory.BGM));
    }

    @Test
    @DisplayName("可按情绪列出曲目")
    void shouldListByMood() {
        List<AudioTrack> relaxing = service.listByMood(AudioTrack.Mood.RELAXING, 0, 50);
        assertFalse(relaxing.isEmpty(), "Should have relaxing tracks");
        assertTrue(relaxing.stream()
                .allMatch(t -> t.getMoods() != null && t.getMoods().contains(AudioTrack.Mood.RELAXING)));
    }

    @Test
    @DisplayName("可按关键词搜索")
    void shouldSearchByKeyword() {
        List<AudioTrack> results = service.searchByKeyword("jazz", 0, 20);
        // 可能有也可能没有匹配，但不应抛出异常
        assertNotNull(results);
    }

    @Test
    @DisplayName("可获取单个曲目")
    void shouldGetTrack() {
        var track = service.getTrack("bgm-relaxing-01");
        assertTrue(track.isPresent());
        assertEquals("Solra Audio Lab", track.get().getArtist());
    }

    @Test
    @DisplayName("用户可上传曲目")
    void userCanUploadTrack() {
        AudioTrack uploaded = service.uploadTrack(
                "user-track-001", "user123", "My Custom BGM",
                AudioTrack.TrackCategory.BGM, "mp3", 180, 5_000_000L, 44100);

        assertNotNull(uploaded);
        assertEquals("user-track-001", uploaded.getTrackId());
        assertEquals("user123", uploaded.getOwnerId());
        assertEquals(AudioTrack.Source.USER_UPLOAD, uploaded.getSource());

        int userCount = service.getUserTrackCount();
        assertTrue(userCount >= 1, "Should have at least 1 user track");
    }

    @Test
    @DisplayName("用户可删除自己上传的曲目")
    void userCanDeleteOwnTrack() {
        service.uploadTrack("del-track", "user123", "To Delete",
                AudioTrack.TrackCategory.BGM, "mp3", 60, 1_000_000L, 44100);

        boolean deleted = service.deleteUserTrack("del-track", "user123");
        assertTrue(deleted);

        var track = service.getTrack("del-track");
        assertTrue(track.isEmpty());
    }

    @Test
    @DisplayName("不能删除其他用户的曲目")
    void cannotDeleteOtherUserTrack() {
        service.uploadTrack("other-track", "user123", "Other",
                AudioTrack.TrackCategory.BGM, "mp3", 60, 1_000_000L, 44100);

        boolean deleted = service.deleteUserTrack("other-track", "user456");
        assertFalse(deleted);

        var track = service.getTrack("other-track");
        assertTrue(track.isPresent());
    }

    @Test
    @DisplayName("可根据情绪生成AudioConfig")
    void shouldGenerateAudioConfigByMood() {
        ProjectConfig.AudioConfig config = service.generateAudioConfig(
                List.of("放松", "relaxing"));

        assertNotNull(config);
        assertNotNull(config.getSources());
        // 至少有一个BGM源
        assertFalse(config.getSources().isEmpty(),
                "Should have at least one audio source");
        assertTrue(config.getSources().stream()
                .anyMatch(s -> s.getSourceId().startsWith("bgm-")),
                "Should have a BGM source");
    }

    @Test
    @DisplayName("曲库统计信息正确")
    void libraryStatsShouldBeCorrect() {
        Map<String, Object> stats = service.getLibraryStats();

        assertNotNull(stats);
        assertTrue((int) stats.get("totalTracks") >= 200);
        assertTrue((int) stats.get("builtinTracks") >= 200);
        assertNotNull(stats.get("byCategory"));
        assertNotNull(stats.get("bySource"));
        assertTrue((long) stats.get("totalDurationSeconds") > 0);
        assertTrue((long) stats.get("totalSizeBytes") > 0);
    }

    @Test
    @DisplayName("所有曲目分类都有内容")
    void allCategoriesShouldHaveTracks() {
        for (AudioTrack.TrackCategory cat : AudioTrack.TrackCategory.values()) {
            List<AudioTrack> tracks = service.listByCategory(cat, 0, 200);
            assertFalse(tracks.isEmpty(),
                    "Category " + cat + " should have tracks");
        }
    }

    @Test
    @DisplayName("可列出用户上传曲目")
    void shouldListUserTracks() {
        service.uploadTrack("ut-1", "user123", "Track 1",
                AudioTrack.TrackCategory.BGM, "mp3", 120, 3_000_000L, 44100);
        service.uploadTrack("ut-2", "user123", "Track 2",
                AudioTrack.TrackCategory.SFX, "wav", 3, 50_000L, 48000);

        List<AudioTrack> userTracks = service.listUserTracks("user123");
        assertEquals(2, userTracks.size());
    }

    @Test
    @DisplayName("重复上传应抛出异常")
    void duplicateUploadShouldThrow() {
        service.uploadTrack("dup-track", "user123", "Dup",
                AudioTrack.TrackCategory.BGM, "mp3", 60, 1_000_000L, 44100);

        assertThrows(IllegalArgumentException.class, () ->
                service.uploadTrack("dup-track", "user456", "Dup2",
                        AudioTrack.TrackCategory.BGM, "mp3", 60, 1_000_000L, 44100));
    }
}
