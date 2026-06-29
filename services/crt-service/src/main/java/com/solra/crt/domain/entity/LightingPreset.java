package com.solra.crt.domain.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 灯光预设值对象 (CRT-006)。
 * 提供≥10种内置氛围预设，支持实时预览参数。
 */
public class LightingPreset {

    public enum PresetId {
        WARM_HOME("warm-home", "暖居", "温馨家居氛围，暖色调主光+柔和补光", Atmosphere.WARM),
        COOL_STUDIO("cool-studio", "冷调工作室", "专业冷色调工作室照明，高对比度", Atmosphere.COOL),
        SUNNY_OUTDOOR("sunny-outdoor", "晴朗户外", "模拟自然日光环境，HDRI天空+方向光", Atmosphere.NATURAL),
        NEON_CYBERPUNK("neon-cyberpunk", "霓虹赛博", "赛博朋克风格，多色霓虹灯光", Atmosphere.NEON),
        CANDLELIT_ROMANCE("candlelit-romance", "烛光浪漫", "昏暗烛光氛围，柔和阴影", Atmosphere.DIM),
        SUNSET_GLOW("sunset-glow", "落日余晖", "暖橙金色夕阳光线，长阴影", Atmosphere.WARM),
        MOONLIGHT("moonlight", "月夜", "冷蓝月光+微弱环境光，高阴影分辨率", Atmosphere.COOL),
        GALLERY_SPOT("gallery-spot", "画廊聚光", "美术馆展览照明，聚光灯+柔和环境光", Atmosphere.SPOTLIGHT),
        MORNING_FRESH("morning-fresh", "清晨薄雾", "清新晨光，漫反射为主，低对比度", Atmosphere.NATURAL),
        NIGHT_CLUB("night-club", "夜店动感", "多色动态灯光，高饱和度，频闪效果", Atmosphere.NEON),
        READING_NOOK("reading-nook", "阅读角落", "温暖点光源，适合阅读的舒适照明", Atmosphere.WARM),
        FOGGY_FOREST("foggy-forest", "迷雾森林", "低能见度雾效+散射光，神秘氛围", Atmosphere.DIM);

        private final String id;
        private final String displayName;
        private final String description;
        private final Atmosphere atmosphere;

        PresetId(String id, String displayName, String description, Atmosphere atmosphere) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.atmosphere = atmosphere;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public Atmosphere getAtmosphere() { return atmosphere; }
    }

    public enum Atmosphere {
        WARM, COOL, NATURAL, NEON, DIM, SPOTLIGHT
    }

    private PresetId preset;
    private ProjectConfig.LightingConfig config;
    private List<String> tags;
    private float previewRotationSpeed;  // 预览旋转速度（度/秒）
    private float previewExposure;       // 预览曝光度
    private String previewHdriFallback;  // 预览HDRI备选URL

    public LightingPreset() {
        this.tags = new ArrayList<>();
        this.previewRotationSpeed = 0.5f;
        this.previewExposure = 1.0f;
    }

    /**
     * 根据预设ID生成对应的 LightingConfig。
     */
    public static LightingPreset from(PresetId presetId) {
        LightingPreset preset = new LightingPreset();
        preset.preset = presetId;
        preset.config = buildConfig(presetId);
        preset.tags = buildTags(presetId);
        preset.configurePreview(presetId);
        return preset;
    }

    /**
     * 获取所有可用预设的元数据列表（不含完整Config，用于列表展示）。
     */
    public static List<PresetSummary> listAll() {
        return Arrays.stream(PresetId.values())
                .map(p -> new PresetSummary(p.getId(), p.getDisplayName(), p.getDescription(),
                        p.getAtmosphere().name(), buildTags(p)))
                .toList();
    }

    /**
     * 获取指定氛围类型的所有预设。
     */
    public static List<PresetSummary> listByAtmosphere(Atmosphere atmosphere) {
        return Arrays.stream(PresetId.values())
                .filter(p -> p.getAtmosphere() == atmosphere)
                .map(p -> new PresetSummary(p.getId(), p.getDisplayName(), p.getDescription(),
                        p.getAtmosphere().name(), buildTags(p)))
                .toList();
    }

    // ── 构建各类预设的 LightingConfig ──

    private static ProjectConfig.LightingConfig buildConfig(PresetId presetId) {
        return switch (presetId) {
            case WARM_HOME -> buildWarmHome();
            case COOL_STUDIO -> buildCoolStudio();
            case SUNNY_OUTDOOR -> buildSunnyOutdoor();
            case NEON_CYBERPUNK -> buildNeonCyberpunk();
            case CANDLELIT_ROMANCE -> buildCandlelitRomance();
            case SUNSET_GLOW -> buildSunsetGlow();
            case MOONLIGHT -> buildMoonlight();
            case GALLERY_SPOT -> buildGallerySpot();
            case MORNING_FRESH -> buildMorningFresh();
            case NIGHT_CLUB -> buildNightClub();
            case READING_NOOK -> buildReadingNook();
            case FOGGY_FOREST -> buildFoggyForest();
        };
    }

    private static List<String> buildTags(PresetId presetId) {
        return switch (presetId) {
            case WARM_HOME -> List.of("室内", "温馨", "暖色", "家居");
            case COOL_STUDIO -> List.of("室内", "专业", "冷色", "高对比度");
            case SUNNY_OUTDOOR -> List.of("户外", "自然光", "HDRI", "明亮");
            case NEON_CYBERPUNK -> List.of("赛博朋克", "霓虹", "科幻", "夜景");
            case CANDLELIT_ROMANCE -> List.of("浪漫", "烛光", "昏暗", "私密");
            case SUNSET_GLOW -> List.of("户外", "夕阳", "暖色", "黄金时刻");
            case MOONLIGHT -> List.of("户外", "月光", "冷色", "夜景");
            case GALLERY_SPOT -> List.of("室内", "展览", "聚光", "专业");
            case MORNING_FRESH -> List.of("户外", "清晨", "自然光", "柔和");
            case NIGHT_CLUB -> List.of("室内", "夜店", "动感", "多彩");
            case READING_NOOK -> List.of("室内", "阅读", "暖色", "舒适");
            case FOGGY_FOREST -> List.of("户外", "迷雾", "森林", "神秘");
        };
    }

    private void configurePreview(PresetId presetId) {
        switch (presetId) {
            case NEON_CYBERPUNK, NIGHT_CLUB -> {
                this.previewRotationSpeed = 1.5f;
                this.previewExposure = 0.8f;
            }
            case CANDLELIT_ROMANCE, MOONLIGHT -> {
                this.previewRotationSpeed = 0.2f;
                this.previewExposure = 0.6f;
            }
            case SUNNY_OUTDOOR -> {
                this.previewExposure = 1.2f;
            }
            default -> {
                this.previewRotationSpeed = 0.5f;
                this.previewExposure = 1.0f;
            }
        }
    }

    // ── 各预设具体实现 ──

    private static ProjectConfig.LightingConfig buildWarmHome() {
        ProjectConfig.LightingConfig cfg = new ProjectConfig.LightingConfig();
        List<ProjectConfig.LightSource> lights = new ArrayList<>();

        ProjectConfig.LightSource main = makeLight("main", "directional",
                new float[]{1.0f, 0.88f, 0.75f}, 0.8f, 40f);
        ProjectConfig.LightSource fill = makeLight("fill", "point",
                new float[]{1.0f, 0.82f, 0.65f}, 0.4f, 15f);
        ProjectConfig.LightSource accent = makeLight("accent", "point",
                new float[]{1.0f, 0.7f, 0.5f}, 0.3f, 8f);
        lights.add(main);
        lights.add(fill);
        lights.add(accent);

        ProjectConfig.EnvironmentLight env = new ProjectConfig.EnvironmentLight();
        env.setHdriUrl("hdri://preset/warm_interior");
        env.setIntensity(0.35f);

        ProjectConfig.ShadowConfig shadows = new ProjectConfig.ShadowConfig();
        shadows.setEnabled(true);
        shadows.setResolution(1024);
        shadows.setBias(0.003f);

        cfg.setLights(lights);
        cfg.setEnvironment(env);
        cfg.setShadows(shadows);
        return cfg;
    }

    private static ProjectConfig.LightingConfig buildCoolStudio() {
        ProjectConfig.LightingConfig cfg = new ProjectConfig.LightingConfig();
        List<ProjectConfig.LightSource> lights = new ArrayList<>();

        ProjectConfig.LightSource key = makeLight("key", "directional",
                new float[]{0.75f, 0.82f, 1.0f}, 1.0f, 50f);
        ProjectConfig.LightSource rim = makeLight("rim", "directional",
                new float[]{0.6f, 0.7f, 1.0f}, 0.5f, 30f);
        lights.add(key);
        lights.add(rim);

        ProjectConfig.EnvironmentLight env = new ProjectConfig.EnvironmentLight();
        env.setHdriUrl("hdri://preset/studio_neutral");
        env.setIntensity(0.6f);

        ProjectConfig.ShadowConfig shadows = new ProjectConfig.ShadowConfig();
        shadows.setEnabled(true);
        shadows.setResolution(2048);
        shadows.setBias(0.002f);

        cfg.setLights(lights);
        cfg.setEnvironment(env);
        cfg.setShadows(shadows);
        return cfg;
    }

    private static ProjectConfig.LightingConfig buildSunnyOutdoor() {
        ProjectConfig.LightingConfig cfg = new ProjectConfig.LightingConfig();
        List<ProjectConfig.LightSource> lights = new ArrayList<>();

        ProjectConfig.LightSource sun = makeLight("sun", "directional",
                new float[]{1.0f, 0.95f, 0.82f}, 1.5f, 200f);
        ProjectConfig.LightSource sky = makeLight("sky", "directional",
                new float[]{0.5f, 0.7f, 1.0f}, 0.3f, 100f);
        lights.add(sun);
        lights.add(sky);

        ProjectConfig.EnvironmentLight env = new ProjectConfig.EnvironmentLight();
        env.setHdriUrl("hdri://preset/sunny_sky");
        env.setIntensity(1.2f);

        ProjectConfig.ShadowConfig shadows = new ProjectConfig.ShadowConfig();
        shadows.setEnabled(true);
        shadows.setResolution(4096);
        shadows.setBias(0.001f);

        cfg.setLights(lights);
        cfg.setEnvironment(env);
        cfg.setShadows(shadows);
        return cfg;
    }

    private static ProjectConfig.LightingConfig buildNeonCyberpunk() {
        ProjectConfig.LightingConfig cfg = new ProjectConfig.LightingConfig();
        List<ProjectConfig.LightSource> lights = new ArrayList<>();

        ProjectConfig.LightSource neonPink = makeLight("neon-pink", "point",
                new float[]{1.0f, 0.15f, 0.5f}, 2.0f, 10f);
        ProjectConfig.LightSource neonBlue = makeLight("neon-blue", "point",
                new float[]{0.1f, 0.5f, 1.0f}, 2.0f, 10f);
        ProjectConfig.LightSource neonPurple = makeLight("neon-purple", "point",
                new float[]{0.7f, 0.1f, 1.0f}, 1.5f, 8f);
        ProjectConfig.LightSource neonCyan = makeLight("neon-cyan", "point",
                new float[]{0.1f, 1.0f, 0.8f}, 1.0f, 6f);
        lights.add(neonPink);
        lights.add(neonBlue);
        lights.add(neonPurple);
        lights.add(neonCyan);

        ProjectConfig.EnvironmentLight env = new ProjectConfig.EnvironmentLight();
        env.setHdriUrl("hdri://preset/cyberpunk_alley");
        env.setIntensity(0.15f);

        ProjectConfig.ShadowConfig shadows = new ProjectConfig.ShadowConfig();
        shadows.setEnabled(true);
        shadows.setResolution(1024);
        shadows.setBias(0.005f);

        cfg.setLights(lights);
        cfg.setEnvironment(env);
        cfg.setShadows(shadows);
        return cfg;
    }

    private static ProjectConfig.LightingConfig buildCandlelitRomance() {
        ProjectConfig.LightingConfig cfg = new ProjectConfig.LightingConfig();
        List<ProjectConfig.LightSource> lights = new ArrayList<>();

        ProjectConfig.LightSource candle1 = makeLight("candle-1", "point",
                new float[]{1.0f, 0.65f, 0.3f}, 0.6f, 3f);
        ProjectConfig.LightSource candle2 = makeLight("candle-2", "point",
                new float[]{1.0f, 0.65f, 0.3f}, 0.6f, 3f);
        ProjectConfig.LightSource candle3 = makeLight("candle-3", "point",
                new float[]{1.0f, 0.6f, 0.25f}, 0.5f, 2.5f);
        lights.add(candle1);
        lights.add(candle2);
        lights.add(candle3);

        ProjectConfig.EnvironmentLight env = new ProjectConfig.EnvironmentLight();
        env.setIntensity(0.08f);

        ProjectConfig.ShadowConfig shadows = new ProjectConfig.ShadowConfig();
        shadows.setEnabled(true);
        shadows.setResolution(512);
        shadows.setBias(0.01f);

        cfg.setLights(lights);
        cfg.setEnvironment(env);
        cfg.setShadows(shadows);
        return cfg;
    }

    private static ProjectConfig.LightingConfig buildSunsetGlow() {
        ProjectConfig.LightingConfig cfg = new ProjectConfig.LightingConfig();
        List<ProjectConfig.LightSource> lights = new ArrayList<>();

        ProjectConfig.LightSource sun = makeLight("sun", "directional",
                new float[]{1.0f, 0.55f, 0.2f}, 1.3f, 150f);
        ProjectConfig.LightSource ambient = makeLight("ambient", "point",
                new float[]{0.9f, 0.5f, 0.35f}, 0.3f, 50f);
        lights.add(sun);
        lights.add(ambient);

        ProjectConfig.EnvironmentLight env = new ProjectConfig.EnvironmentLight();
        env.setHdriUrl("hdri://preset/sunset_sky");
        env.setIntensity(0.9f);

        ProjectConfig.ShadowConfig shadows = new ProjectConfig.ShadowConfig();
        shadows.setEnabled(true);
        shadows.setResolution(4096);
        shadows.setBias(0.002f);

        cfg.setLights(lights);
        cfg.setEnvironment(env);
        cfg.setShadows(shadows);
        return cfg;
    }

    private static ProjectConfig.LightingConfig buildMoonlight() {
        ProjectConfig.LightingConfig cfg = new ProjectConfig.LightingConfig();
        List<ProjectConfig.LightSource> lights = new ArrayList<>();

        ProjectConfig.LightSource moon = makeLight("moon", "directional",
                new float[]{0.4f, 0.5f, 0.85f}, 0.4f, 100f);
        ProjectConfig.LightSource star = makeLight("star-glow", "point",
                new float[]{0.3f, 0.4f, 0.7f}, 0.1f, 30f);
        lights.add(moon);
        lights.add(star);

        ProjectConfig.EnvironmentLight env = new ProjectConfig.EnvironmentLight();
        env.setHdriUrl("hdri://preset/night_sky");
        env.setIntensity(0.15f);

        ProjectConfig.ShadowConfig shadows = new ProjectConfig.ShadowConfig();
        shadows.setEnabled(true);
        shadows.setResolution(2048);
        shadows.setBias(0.008f);

        cfg.setLights(lights);
        cfg.setEnvironment(env);
        cfg.setShadows(shadows);
        return cfg;
    }

    private static ProjectConfig.LightingConfig buildGallerySpot() {
        ProjectConfig.LightingConfig cfg = new ProjectConfig.LightingConfig();
        List<ProjectConfig.LightSource> lights = new ArrayList<>();

        ProjectConfig.LightSource spot1 = makeLight("spot-1", "spot",
                new float[]{1.0f, 0.98f, 0.95f}, 2.5f, 12f);
        spot1.setSpotAngle(25f);
        ProjectConfig.LightSource spot2 = makeLight("spot-2", "spot",
                new float[]{1.0f, 0.98f, 0.95f}, 2.0f, 10f);
        spot2.setSpotAngle(30f);
        ProjectConfig.LightSource ambient = makeLight("ambient", "point",
                new float[]{0.7f, 0.7f, 0.75f}, 0.15f, 20f);
        lights.add(spot1);
        lights.add(spot2);
        lights.add(ambient);

        ProjectConfig.EnvironmentLight env = new ProjectConfig.EnvironmentLight();
        env.setHdriUrl("hdri://preset/gallery_interior");
        env.setIntensity(0.2f);

        ProjectConfig.ShadowConfig shadows = new ProjectConfig.ShadowConfig();
        shadows.setEnabled(true);
        shadows.setResolution(2048);
        shadows.setBias(0.003f);

        cfg.setLights(lights);
        cfg.setEnvironment(env);
        cfg.setShadows(shadows);
        return cfg;
    }

    private static ProjectConfig.LightingConfig buildMorningFresh() {
        ProjectConfig.LightingConfig cfg = new ProjectConfig.LightingConfig();
        List<ProjectConfig.LightSource> lights = new ArrayList<>();

        ProjectConfig.LightSource morning = makeLight("morning-sun", "directional",
                new float[]{1.0f, 0.9f, 0.8f}, 0.7f, 120f);
        ProjectConfig.LightSource diffuse = makeLight("diffuse-sky", "point",
                new float[]{0.6f, 0.75f, 0.9f}, 0.4f, 60f);
        lights.add(morning);
        lights.add(diffuse);

        ProjectConfig.EnvironmentLight env = new ProjectConfig.EnvironmentLight();
        env.setHdriUrl("hdri://preset/morning_mist");
        env.setIntensity(0.7f);

        ProjectConfig.ShadowConfig shadows = new ProjectConfig.ShadowConfig();
        shadows.setEnabled(true);
        shadows.setResolution(2048);
        shadows.setBias(0.004f);

        cfg.setLights(lights);
        cfg.setEnvironment(env);
        cfg.setShadows(shadows);
        return cfg;
    }

    private static ProjectConfig.LightingConfig buildNightClub() {
        ProjectConfig.LightingConfig cfg = new ProjectConfig.LightingConfig();
        List<ProjectConfig.LightSource> lights = new ArrayList<>();

        ProjectConfig.LightSource strobe1 = makeLight("strobe-red", "point",
                new float[]{1.0f, 0.0f, 0.1f}, 3.0f, 15f);
        ProjectConfig.LightSource strobe2 = makeLight("strobe-blue", "point",
                new float[]{0.0f, 0.2f, 1.0f}, 3.0f, 15f);
        ProjectConfig.LightSource strobe3 = makeLight("strobe-green", "point",
                new float[]{0.0f, 1.0f, 0.1f}, 2.5f, 12f);
        ProjectConfig.LightSource uv = makeLight("uv-glow", "point",
                new float[]{0.3f, 0.0f, 0.6f}, 1.5f, 20f);
        lights.add(strobe1);
        lights.add(strobe2);
        lights.add(strobe3);
        lights.add(uv);

        ProjectConfig.EnvironmentLight env = new ProjectConfig.EnvironmentLight();
        env.setIntensity(0.05f);

        ProjectConfig.ShadowConfig shadows = new ProjectConfig.ShadowConfig();
        shadows.setEnabled(false);

        cfg.setLights(lights);
        cfg.setEnvironment(env);
        cfg.setShadows(shadows);
        return cfg;
    }

    private static ProjectConfig.LightingConfig buildReadingNook() {
        ProjectConfig.LightingConfig cfg = new ProjectConfig.LightingConfig();
        List<ProjectConfig.LightSource> lights = new ArrayList<>();

        ProjectConfig.LightSource reading = makeLight("reading-lamp", "spot",
                new float[]{1.0f, 0.85f, 0.65f}, 1.2f, 6f);
        reading.setSpotAngle(35f);
        ProjectConfig.LightSource ambient = makeLight("room-ambient", "point",
                new float[]{0.8f, 0.7f, 0.6f}, 0.2f, 15f);
        lights.add(reading);
        lights.add(ambient);

        ProjectConfig.EnvironmentLight env = new ProjectConfig.EnvironmentLight();
        env.setHdriUrl("hdri://preset/cozy_interior");
        env.setIntensity(0.2f);

        ProjectConfig.ShadowConfig shadows = new ProjectConfig.ShadowConfig();
        shadows.setEnabled(true);
        shadows.setResolution(1024);
        shadows.setBias(0.005f);

        cfg.setLights(lights);
        cfg.setEnvironment(env);
        cfg.setShadows(shadows);
        return cfg;
    }

    private static ProjectConfig.LightingConfig buildFoggyForest() {
        ProjectConfig.LightingConfig cfg = new ProjectConfig.LightingConfig();
        List<ProjectConfig.LightSource> lights = new ArrayList<>();

        ProjectConfig.LightSource godRay1 = makeLight("godray-1", "directional",
                new float[]{0.7f, 0.8f, 0.6f}, 0.5f, 80f);
        ProjectConfig.LightSource godRay2 = makeLight("godray-2", "directional",
                new float[]{0.6f, 0.75f, 0.65f}, 0.3f, 60f);
        lights.add(godRay1);
        lights.add(godRay2);

        ProjectConfig.EnvironmentLight env = new ProjectConfig.EnvironmentLight();
        env.setHdriUrl("hdri://preset/foggy_forest");
        env.setIntensity(0.4f);

        ProjectConfig.ShadowConfig shadows = new ProjectConfig.ShadowConfig();
        shadows.setEnabled(true);
        shadows.setResolution(1024);
        shadows.setBias(0.01f);

        cfg.setLights(lights);
        cfg.setEnvironment(env);
        cfg.setShadows(shadows);
        return cfg;
    }

    // ── 工具方法 ──

    private static ProjectConfig.LightSource makeLight(String id, String type, float[] color,
                                                        float intensity, float range) {
        ProjectConfig.LightSource light = new ProjectConfig.LightSource();
        light.setLightId(id);
        light.setType(type);
        light.setColor(color);
        light.setIntensity(intensity);
        light.setRange(range);
        return light;
    }

    // ── Getters ──

    public PresetId getPreset() { return preset; }
    public ProjectConfig.LightingConfig getConfig() { return config; }
    public List<String> getTags() { return tags; }
    public float getPreviewRotationSpeed() { return previewRotationSpeed; }
    public float getPreviewExposure() { return previewExposure; }
    public String getPreviewHdriFallback() { return previewHdriFallback; }

    /**
     * 预设摘要（用于列表展示，不包含完整Config）。
     */
    public static class PresetSummary {
        private final String id;
        private final String displayName;
        private final String description;
        private final String atmosphere;
        private final List<String> tags;

        public PresetSummary(String id, String displayName, String description,
                             String atmosphere, List<String> tags) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.atmosphere = atmosphere;
            this.tags = tags;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getAtmosphere() { return atmosphere; }
        public List<String> getTags() { return tags; }
    }
}
