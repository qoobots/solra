package com.solra.crt.domain.service;

import com.solra.crt.domain.entity.AudioTrack;
import com.solra.crt.domain.entity.AudioLibrary;
import com.solra.crt.domain.entity.ProjectConfig;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 音频曲库领域服务 (CRT-007)。
 * 管理内置曲库（≥200首）+ 用户上传曲目，支持搜索、分类浏览和BGM配置生成。
 *
 * 验收标准：
 * - 内置曲库 ≥ 200首
 * - 支持用户上传曲目
 * - 按分类/情绪/标签搜索
 * - 生成项目音频配置
 */
public class AudioLibraryService {

    // 内置曲库（启动时初始化）
    private final Map<String, AudioTrack> builtinLibrary = new ConcurrentHashMap<>();
    // 用户上传曲目
    private final Map<String, AudioTrack> userLibrary = new ConcurrentHashMap<>();

    public AudioLibraryService() {
        initializeBuiltinLibrary();
    }

    /**
     * 初始化内置曲库（≥200首）。
     */
    private void initializeBuiltinLibrary() {
        List<AudioTrack> tracks = AudioLibrary.generateBuiltinLibrary();
        for (AudioTrack track : tracks) {
            builtinLibrary.put(track.getTrackId(), track);
        }
    }

    /**
     * 获取内置曲库总数。
     */
    public int getBuiltinTrackCount() {
        return builtinLibrary.size();
    }

    /**
     * 获取用户上传曲目总数。
     */
    public int getUserTrackCount() {
        return userLibrary.size();
    }

    /**
     * 获取全部曲目总数。
     */
    public int getTotalTrackCount() {
        return builtinLibrary.size() + userLibrary.size();
    }

    /**
     * 按分类列出曲目。
     */
    public List<AudioTrack> listByCategory(AudioTrack.TrackCategory category, int offset, int limit) {
        List<AudioTrack> all = getAllTracks();
        return all.stream()
                .filter(t -> t.getCategory() == category)
                .sorted(Comparator.comparingInt(AudioTrack::getUsageCount).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 按情绪列出曲目。
     */
    public List<AudioTrack> listByMood(AudioTrack.Mood mood, int offset, int limit) {
        List<AudioTrack> all = getAllTracks();
        return all.stream()
                .filter(t -> t.getMoods() != null && t.getMoods().contains(mood))
                .sorted(Comparator.comparingInt(AudioTrack::getUsageCount).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 按关键词搜索曲目。
     */
    public List<AudioTrack> searchByKeyword(String keyword, int offset, int limit) {
        String kw = keyword.toLowerCase();
        List<AudioTrack> all = getAllTracks();
        return all.stream()
                .filter(t -> matchesKeyword(t, kw))
                .sorted(Comparator.comparingInt(AudioTrack::getUsageCount).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取单个曲目详情。
     */
    public Optional<AudioTrack> getTrack(String trackId) {
        AudioTrack track = builtinLibrary.get(trackId);
        if (track != null) return Optional.of(track);
        return Optional.ofNullable(userLibrary.get(trackId));
    }

    /**
     * 用户上传曲目。
     */
    public AudioTrack uploadTrack(String trackId, String ownerId, String title,
                                   AudioTrack.TrackCategory category, String format,
                                   int durationSeconds, long fileSizeBytes, int sampleRateHz) {
        if (userLibrary.containsKey(trackId)) {
            throw new IllegalArgumentException("Track already exists: " + trackId);
        }

        AudioTrack track = new AudioTrack();
        track.setTrackId(trackId);
        track.setOwnerId(ownerId);
        track.setTitle(title);
        track.setArtist(ownerId);
        track.setCategory(category);
        track.setSource(AudioTrack.Source.USER_UPLOAD);
        track.setFormat(format);
        track.setDurationSeconds(durationSeconds);
        track.setFileSizeBytes(fileSizeBytes);
        track.setSampleRateHz(sampleRateHz);
        track.setAssetRef("audio://user/" + ownerId + "/" + trackId + "." + format);
        track.setLoopable(category == AudioTrack.TrackCategory.BGM || category == AudioTrack.TrackCategory.AMBIENT);
        track.setUploadedAt(Instant.now());
        track.setMoods(List.of(AudioTrack.Mood.NEUTRAL));

        userLibrary.put(trackId, track);
        return track;
    }

    /**
     * 删除用户上传曲目。
     */
    public boolean deleteUserTrack(String trackId, String ownerId) {
        AudioTrack track = userLibrary.get(trackId);
        if (track != null && ownerId.equals(track.getOwnerId())) {
            userLibrary.remove(trackId);
            return true;
        }
        return false;
    }

    /**
     * 获取用户上传的所有曲目。
     */
    public List<AudioTrack> listUserTracks(String ownerId) {
        return userLibrary.values().stream()
                .filter(t -> ownerId.equals(t.getOwnerId()))
                .sorted(Comparator.comparing(AudioTrack::getUploadedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 基于情绪关键词生成 AudioConfig。
     */
    public ProjectConfig.AudioConfig generateAudioConfig(List<String> moodKeywords) {
        ProjectConfig.AudioConfig config = new ProjectConfig.AudioConfig();
        List<ProjectConfig.AudioSource> sources = new ArrayList<>();

        // 解析情绪
        Set<AudioTrack.Mood> targetMoods = new HashSet<>();
        for (String kw : moodKeywords) {
            AudioTrack.Mood mood = parseMood(kw);
            if (mood != null) targetMoods.add(mood);
        }
        if (targetMoods.isEmpty()) targetMoods.add(AudioTrack.Mood.NEUTRAL);

        // 从曲库中选取匹配的BGM
        List<AudioTrack> matchingBgms = getAllTracks().stream()
                .filter(t -> t.getCategory() == AudioTrack.TrackCategory.BGM
                        && t.getMoods() != null
                        && !Collections.disjoint(t.getMoods(), targetMoods))
                .sorted(Comparator.comparingInt(AudioTrack::getUsageCount).reversed())
                .limit(2)
                .toList();

        for (int i = 0; i < matchingBgms.size(); i++) {
            AudioTrack track = matchingBgms.get(i);
            ProjectConfig.AudioSource src = new ProjectConfig.AudioSource();
            src.setSourceId("bgm-" + (i + 1));
            src.setAudioAssetRef(track.getAssetRef());
            src.setVolume(track.getDefaultVolume());
            src.setLoop(true);
            src.setSpatial(false);
            sources.add(src);
            track.incrementUsage();
        }

        // 添加环境音（如果匹配）
        List<AudioTrack> matchingAmbients = getAllTracks().stream()
                .filter(t -> t.getCategory() == AudioTrack.TrackCategory.AMBIENT
                        && t.getMoods() != null
                        && !Collections.disjoint(t.getMoods(), targetMoods))
                .limit(1)
                .toList();

        for (AudioTrack track : matchingAmbients) {
            ProjectConfig.AudioSource src = new ProjectConfig.AudioSource();
            src.setSourceId("ambient-1");
            src.setAudioAssetRef(track.getAssetRef());
            src.setVolume(track.getDefaultVolume());
            src.setLoop(true);
            src.setSpatial(true);
            sources.add(src);
            track.incrementUsage();
        }

        // 默认BGM兜底
        if (sources.isEmpty()) {
            AudioTrack defaultBgm = builtinLibrary.get("bgm-neutral-01");
            if (defaultBgm != null) {
                ProjectConfig.AudioSource src = new ProjectConfig.AudioSource();
                src.setSourceId("bgm-default");
                src.setAudioAssetRef(defaultBgm.getAssetRef());
                src.setVolume(0.3f);
                src.setLoop(true);
                src.setSpatial(false);
                sources.add(src);
            }
        }

        config.setSources(sources);
        config.setMasterVolume(0.8f);
        config.setSpatialAudio(true);
        return config;
    }

    /**
     * 获取曲库统计信息。
     */
    public Map<String, Object> getLibraryStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTracks", getTotalTrackCount());
        stats.put("builtinTracks", getBuiltinTrackCount());
        stats.put("userTracks", getUserTrackCount());

        Map<String, Long> byCategory = getAllTracks().stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().name(),
                        Collectors.counting()));
        stats.put("byCategory", byCategory);

        Map<String, Long> bySource = getAllTracks().stream()
                .collect(Collectors.groupingBy(
                        t -> t.getSource().name(),
                        Collectors.counting()));
        stats.put("bySource", bySource);

        long totalDuration = getAllTracks().stream()
                .mapToLong(AudioTrack::getDurationSeconds)
                .sum();
        stats.put("totalDurationSeconds", totalDuration);

        long totalSize = getAllTracks().stream()
                .mapToLong(AudioTrack::getFileSizeBytes)
                .sum();
        stats.put("totalSizeBytes", totalSize);

        return stats;
    }

    // ── 私有辅助 ──

    private List<AudioTrack> getAllTracks() {
        List<AudioTrack> all = new ArrayList<>(builtinLibrary.values());
        all.addAll(userLibrary.values());
        return all;
    }

    private boolean matchesKeyword(AudioTrack track, String keyword) {
        if (track.getTitle() != null && track.getTitle().toLowerCase().contains(keyword)) return true;
        if (track.getArtist() != null && track.getArtist().toLowerCase().contains(keyword)) return true;
        if (track.getTags() != null && track.getTags().stream().anyMatch(t -> t.toLowerCase().contains(keyword)))
            return true;
        if (track.getMoods() != null && track.getMoods().stream().anyMatch(m -> m.name().toLowerCase().contains(keyword)))
            return true;
        return false;
    }

    private AudioTrack.Mood parseMood(String keyword) {
        String kw = keyword.toLowerCase().trim();
        return switch (kw) {
            case "放松", "轻松", "relaxing", "calm", "peaceful" -> AudioTrack.Mood.RELAXING;
            case "活力", "动感", "energetic", "upbeat", "dynamic" -> AudioTrack.Mood.ENERGETIC;
            case "神秘", "悬疑", "mysterious", "mystery", "suspense" -> AudioTrack.Mood.MYSTERIOUS;
            case "浪漫", "romantic", "love" -> AudioTrack.Mood.ROMANTIC;
            case "史诗", "宏大", "epic", "grand" -> AudioTrack.Mood.EPIC;
            case "开心", "快乐", "happy", "joy", "cheerful" -> AudioTrack.Mood.HAPPY;
            case "悲伤", "伤感", "sad", "melancholy" -> AudioTrack.Mood.SAD;
            case "恐怖", "可怕", "scary", "horror", "fear" -> AudioTrack.Mood.SCARY;
            case "专注", "focus", "concentration", "study" -> AudioTrack.Mood.FOCUS;
            case "自然", "nature", "natural" -> AudioTrack.Mood.NATURE;
            case "城市", "urban", "city" -> AudioTrack.Mood.URBAN;
            default -> null;
        };
    }
}
