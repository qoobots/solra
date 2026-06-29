package com.solra.crt.domain.entity;

import java.util.*;

/**
 * 音频曲库值对象 (CRT-007)。
 * 提供≥200首内置曲目的管理、搜索和分类。
 */
public class AudioLibrary {

    /**
     * 生成内置曲库（≥200首曲目）。
     * 使用程序化生成确保可重复性，实际生产中从配置文件/数据库加载。
     */
    public static List<AudioTrack> generateBuiltinLibrary() {
        List<AudioTrack> library = new ArrayList<>();

        // ── BGM 背景音乐 (80首) ──
        // 轻松/放松 BGM (20首)
        addBgms(library, "relaxing", AudioTrack.Mood.RELAXING, 20, "bgm");
        // 活力/动感 BGM (15首)
        addBgms(library, "energetic", AudioTrack.Mood.ENERGETIC, 15, "bgm");
        // 神秘/悬疑 BGM (10首)
        addBgms(library, "mysterious", AudioTrack.Mood.MYSTERIOUS, 10, "bgm");
        // 浪漫 BGM (10首)
        addBgms(library, "romantic", AudioTrack.Mood.ROMANTIC, 10, "bgm");
        // 史诗 BGM (10首)
        addBgms(library, "epic", AudioTrack.Mood.EPIC, 10, "bgm");
        // 中性/通用 BGM (10首)
        addBgms(library, "neutral", AudioTrack.Mood.NEUTRAL, 10, "bgm");
        // 专注 BGM (5首)
        addBgms(library, "focus", AudioTrack.Mood.FOCUS, 5, "bgm");

        // ── SFX 音效 (60首) ──
        addSfxs(library, "interaction", "交互", 15, List.of("点击", "悬停", "确认", "取消", "切换"));
        addSfxs(library, "ambient", "环境", 15, List.of("风声", "雨声", "雷声", "鸟鸣", "水流", "城市", "森林", "海洋"));
        addSfxs(library, "transition", "转场", 10, List.of("淡入", "淡出", "翻页", "滑动", "弹入"));
        addSfxs(library, "notification", "通知", 10, List.of("消息", "提醒", "警告", "成功", "错误"));
        addSfxs(library, "object", "物体", 10, List.of("开门", "关门", "脚步", "碰撞", "拾取"));

        // ── AMBIENT 环境音 (30首) ──
        addAmbients(library, "nature", "自然", 10,
                List.of("森林昼", "森林夜", "海滩", "雨林", "沙漠风", "溪流", "瀑布", "草原", "雪山", "洞穴"));
        addAmbients(library, "urban", "城市", 10,
                List.of("街道昼", "街道夜", "咖啡馆", "图书馆", "地铁站", "公园", "市场", "办公室", "餐厅", "商场"));
        addAmbients(library, "indoor", "室内", 10,
                List.of("客厅", "厨房", "浴室", "卧室", "阁楼", "地下室", "走廊", "电梯", "车库", "温室"));

        // ── UI_SOUND UI音效 (20首) ──
        addUiSounds(library, "button", "按钮", 8,
                List.of("primary", "secondary", "danger", "ghost", "link", "icon", "toggle_on", "toggle_off"));
        addUiSounds(library, "feedback", "反馈", 7,
                List.of("success", "error", "warning", "info", "loading", "complete", "empty"));
        addUiSounds(library, "navigation", "导航", 5,
                List.of("back", "forward", "menu_open", "menu_close", "tab_switch"));

        // ── VOICE 语音 (10首) ──
        addVoices(library, "greeting", "问候", 5,
                List.of("欢迎", "你好", "再见", "早上好", "晚上好"));
        addVoices(library, "guide", "引导", 5,
                List.of("点击这里", "向右滑动", "请稍候", "操作成功", "请重试"));

        return library;
    }

    // ── 辅助生成方法 ──

    private static void addBgms(List<AudioTrack> library, String moodKey, AudioTrack.Mood mood,
                                 int count, String prefix) {
        for (int i = 1; i <= count; i++) {
            AudioTrack track = new AudioTrack();
            track.setTrackId(String.format("%s-%s-%02d", prefix, moodKey, i));
            track.setTitle(String.format("%s %s %02d", capitalize(moodKey), prefix.toUpperCase(), i));
            track.setArtist("Solra Audio Lab");
            track.setCategory(AudioTrack.TrackCategory.BGM);
            track.setMoods(List.of(mood));
            track.setSource(AudioTrack.Source.BUILTIN);
            track.setAssetRef(String.format("audio://builtin/bgm/%s/%02d.mp3", moodKey, i));
            track.setDurationSeconds(120 + (i * 7) % 180);  // 2-5分钟
            track.setFileSizeBytes(2_000_000L + i * 500_000L);
            track.setFormat("mp3");
            track.setSampleRateHz(44100);
            track.setLoopable(true);
            track.setDefaultVolume(0.4f);
            track.setTags(List.of("BGM", capitalize(moodKey)));
            library.add(track);
        }
    }

    private static void addSfxs(List<AudioTrack> library, String categoryKey, String categoryName,
                                 int count, List<String> names) {
        for (int i = 0; i < count; i++) {
            String name = names.get(i % names.size());
            String suffix = count > names.size() ? " " + (i / names.size() + 1) : "";
            AudioTrack track = new AudioTrack();
            track.setTrackId(String.format("sfx-%s-%02d", categoryKey, i + 1));
            track.setTitle(name + suffix);
            track.setArtist("Solra Audio Lab");
            track.setCategory(AudioTrack.TrackCategory.SFX);
            track.setMoods(List.of(AudioTrack.Mood.NEUTRAL));
            track.setSource(AudioTrack.Source.BUILTIN);
            track.setAssetRef(String.format("audio://builtin/sfx/%s/%02d.wav", categoryKey, i + 1));
            track.setDurationSeconds(1 + (i % 5));
            track.setFileSizeBytes(50_000L + i * 10_000L);
            track.setFormat("wav");
            track.setSampleRateHz(48000);
            track.setLoopable(false);
            track.setDefaultVolume(0.7f);
            track.setTags(List.of("SFX", categoryName));
            library.add(track);
        }
    }

    private static void addAmbients(List<AudioTrack> library, String categoryKey, String categoryName,
                                     int count, List<String> names) {
        for (int i = 0; i < count; i++) {
            AudioTrack track = new AudioTrack();
            track.setTrackId(String.format("amb-%s-%02d", categoryKey, i + 1));
            track.setTitle(names.get(i % names.size()));
            track.setArtist("Solra Audio Lab");
            track.setCategory(AudioTrack.TrackCategory.AMBIENT);
            track.setMoods(List.of(
                    categoryKey.equals("nature") ? AudioTrack.Mood.NATURE : AudioTrack.Mood.NEUTRAL));
            track.setSource(AudioTrack.Source.BUILTIN);
            track.setAssetRef(String.format("audio://builtin/ambient/%s/%02d.ogg", categoryKey, i + 1));
            track.setDurationSeconds(180 + i * 30);
            track.setFileSizeBytes(3_000_000L + i * 500_000L);
            track.setFormat("ogg");
            track.setSampleRateHz(44100);
            track.setLoopable(true);
            track.setDefaultVolume(0.3f);
            track.setTags(List.of("AMBIENT", categoryName));
            library.add(track);
        }
    }

    private static void addUiSounds(List<AudioTrack> library, String categoryKey, String categoryName,
                                     int count, List<String> names) {
        for (int i = 0; i < count; i++) {
            AudioTrack track = new AudioTrack();
            track.setTrackId(String.format("ui-%s-%02d", categoryKey, i + 1));
            track.setTitle(names.get(i % names.size()));
            track.setArtist("Solra Audio Lab");
            track.setCategory(AudioTrack.TrackCategory.UI_SOUND);
            track.setMoods(List.of(AudioTrack.Mood.NEUTRAL));
            track.setSource(AudioTrack.Source.BUILTIN);
            track.setAssetRef(String.format("audio://builtin/ui/%s/%02d.wav", categoryKey, i + 1));
            track.setDurationSeconds(1);
            track.setFileSizeBytes(20_000L + i * 5_000L);
            track.setFormat("wav");
            track.setSampleRateHz(44100);
            track.setLoopable(false);
            track.setDefaultVolume(0.8f);
            track.setTags(List.of("UI", categoryName));
            library.add(track);
        }
    }

    private static void addVoices(List<AudioTrack> library, String categoryKey, String categoryName,
                                   int count, List<String> names) {
        for (int i = 0; i < count; i++) {
            AudioTrack track = new AudioTrack();
            track.setTrackId(String.format("voice-%s-%02d", categoryKey, i + 1));
            track.setTitle(names.get(i % names.size()));
            track.setArtist("Solra Voice Studio");
            track.setCategory(AudioTrack.TrackCategory.VOICE);
            track.setMoods(List.of(AudioTrack.Mood.NEUTRAL));
            track.setSource(AudioTrack.Source.BUILTIN);
            track.setAssetRef(String.format("audio://builtin/voice/%s/%02d.mp3", categoryKey, i + 1));
            track.setDurationSeconds(2 + i % 3);
            track.setFileSizeBytes(30_000L + i * 10_000L);
            track.setFormat("mp3");
            track.setSampleRateHz(22050);
            track.setLoopable(false);
            track.setDefaultVolume(0.9f);
            track.setTags(List.of("VOICE", categoryName));
            library.add(track);
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
