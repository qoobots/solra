package com.solra.crt.domain.service;

import com.solra.crt.domain.entity.ProjectConfig;
import com.solra.crt.domain.entity.SpaceProject;

import java.util.*;

/**
 * AI 辅助空间搭建领域服务 (CRT-001)。
 * 根据自然语言描述生成空间蓝图（场景图、灯光、音频、物理配置）。
 * 当前为 Mock 实现，提供基于关键词的模板匹配生成策略。
 *
 * 验收标准：
 * - 文字描述 → 3D场景蓝图生成
 * - 生成完成度 >60%
 * - 响应时间 <30秒
 * - 支持迭代修改
 */
public class AISpaceGenerator {

    private static final int MAX_ITERATIONS = 5;

    /**
     * 根据文字描述生成空间蓝图。
     *
     * @param description 自然语言描述（如"一个温馨的咖啡馆，暖色调灯光，播放爵士乐"）
     * @param projectType 项目类型
     * @return 生成的 ProjectConfig 蓝图
     */
    public ProjectConfig generateFromDescription(String description, SpaceProject.ProjectType projectType) {
        ProjectConfig config = new ProjectConfig();

        // 解析描述中的关键词
        Set<String> keywords = extractKeywords(description.toLowerCase());

        // 根据关键词生成场景图
        config.setSceneGraph(buildSceneGraph(keywords, projectType));

        // 根据关键词配置灯光
        config.setLighting(buildLighting(keywords));

        // 根据关键词配置音频
        config.setAudio(buildAudio(keywords));

        // 配置物理
        config.setPhysics(buildPhysics(keywords));

        return config;
    }

    /**
     * 对已有蓝图进行迭代修改。
     *
     * @param currentConfig 当前配置
     * @param modification  修改描述
     * @param iteration     当前迭代次数
     * @return 修改后的配置
     */
    public ProjectConfig iterateModification(ProjectConfig currentConfig, String modification, int iteration) {
        if (iteration > MAX_ITERATIONS) {
            return currentConfig;
        }

        Set<String> keywords = extractKeywords(modification.toLowerCase());

        // 仅更新匹配的配置部分
        if (hasLightingKeywords(keywords)) {
            currentConfig.setLighting(buildLighting(keywords));
        }
        if (hasAudioKeywords(keywords)) {
            currentConfig.setAudio(buildAudio(keywords));
        }
        if (hasSceneKeywords(keywords)) {
            currentConfig.setSceneGraph(buildSceneGraph(keywords, null));
        }

        return currentConfig;
    }

    /**
     * 计算生成完成度评分 (0.0 - 1.0)。
     */
    public float calculateCompletionScore(ProjectConfig config) {
        float score = 0.0f;

        if (config.getSceneGraph() != null && !config.getSceneGraph().getNodes().isEmpty()) {
            score += 0.40f;
        }
        if (config.getLighting() != null && !config.getLighting().getLights().isEmpty()) {
            score += 0.25f;
        }
        if (config.getAudio() != null && !config.getAudio().getSources().isEmpty()) {
            score += 0.20f;
        }
        if (config.getPhysics() != null && config.getPhysics().isEnabled()) {
            score += 0.15f;
        }

        return Math.min(1.0f, score);
    }

    // ── 私有辅助方法 ──

    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();
        // 场景关键词
        String[] sceneWords = {"咖啡馆", "cafe", "coffee", "画廊", "gallery", "展厅", "exhibition",
                "办公室", "office", "公园", "park", "花园", "garden", "海滩", "beach", "森林", "forest",
                "太空", "space", "赛博朋克", "cyberpunk", "日式", "japanese", "中式", "chinese",
                "现代", "modern", "复古", "retro", "vintage", "工业", "industrial", "极简", "minimal"};
        for (String w : sceneWords) {
            if (text.contains(w)) keywords.add(w);
        }
        // 灯光关键词
        String[] lightWords = {"暖色", "warm", "冷色", "cool", "明亮", "bright", "昏暗", "dim",
                "自然光", "natural", "霓虹", "neon", "烛光", "candle", "聚光灯", "spotlight"};
        for (String w : lightWords) {
            if (text.contains(w)) keywords.add(w);
        }
        // 音频关键词
        String[] audioWords = {"爵士", "jazz", "古典", "classical", "电子", "electronic",
                "自然声", "nature", "安静", "quiet", "热闹", "lively", "BGM", "bgm", "音乐", "music"};
        for (String w : audioWords) {
            if (text.contains(w)) keywords.add(w);
        }
        // 如果没有匹配任何关键词，添加一个默认标识
        if (keywords.isEmpty()) {
            keywords.add("default");
        }
        return keywords;
    }

    private ProjectConfig.SceneGraph buildSceneGraph(Set<String> keywords, SpaceProject.ProjectType projectType) {
        ProjectConfig.SceneGraph graph = new ProjectConfig.SceneGraph();
        List<ProjectConfig.SceneNode> nodes = new ArrayList<>();

        // 根节点
        ProjectConfig.SceneNode root = createNode("root", null, "Root", null);
        nodes.add(root);
        graph.setRootNodeId("root");

        // 地面
        nodes.add(createNode("floor", "root", "Floor", "asset://default/floor"));
        // 四面墙壁
        nodes.add(createNode("wall-north", "root", "Wall-North", "asset://default/wall"));
        nodes.add(createNode("wall-south", "root", "Wall-South", "asset://default/wall"));
        nodes.add(createNode("wall-east", "root", "Wall-East", "asset://default/wall"));
        nodes.add(createNode("wall-west", "root", "Wall-West", "asset://default/wall"));

        // 根据关键词添加场景物体
        if (keywords.contains("cafe") || keywords.contains("coffee") || keywords.contains("咖啡馆")) {
            nodes.add(createNode("counter", "root", "Counter", "asset://cafe/counter"));
            nodes.add(createNode("table-1", "root", "Table-1", "asset://cafe/table"));
            nodes.add(createNode("table-2", "root", "Table-2", "asset://cafe/table"));
            nodes.add(createNode("chair-1", "table-1", "Chair-1", "asset://cafe/chair"));
            nodes.add(createNode("chair-2", "table-1", "Chair-2", "asset://cafe/chair"));
            nodes.add(createNode("chair-3", "table-2", "Chair-3", "asset://cafe/chair"));
            nodes.add(createNode("chair-4", "table-2", "Chair-4", "asset://cafe/chair"));
            nodes.add(createNode("espresso-machine", "counter", "Espresso Machine", "asset://cafe/espresso_machine"));
        } else if (keywords.contains("gallery") || keywords.contains("画廊") || keywords.contains("exhibition") || keywords.contains("展厅")) {
            nodes.add(createNode("wall-display-n", "root", "Display-Wall-N", "asset://gallery/display_wall"));
            nodes.add(createNode("wall-display-s", "root", "Display-Wall-S", "asset://gallery/display_wall"));
            nodes.add(createNode("artwork-1", "wall-display-n", "Artwork-1", "asset://gallery/frame"));
            nodes.add(createNode("artwork-2", "wall-display-n", "Artwork-2", "asset://gallery/frame"));
            nodes.add(createNode("sculpture-1", "root", "Sculpture-1", "asset://gallery/sculpture_pedestal"));
            nodes.add(createNode("spotlight-1", "root", "Spotlight-1", "asset://lighting/spotlight"));
            nodes.add(createNode("spotlight-2", "root", "Spotlight-2", "asset://lighting/spotlight"));
        } else if (keywords.contains("office") || keywords.contains("办公室")) {
            nodes.add(createNode("desk-1", "root", "Desk-1", "asset://office/desk"));
            nodes.add(createNode("desk-2", "root", "Desk-2", "asset://office/desk"));
            nodes.add(createNode("office-chair-1", "desk-1", "Office-Chair-1", "asset://office/chair"));
            nodes.add(createNode("office-chair-2", "desk-2", "Office-Chair-2", "asset://office/chair"));
            nodes.add(createNode("bookshelf", "root", "Bookshelf", "asset://office/bookshelf"));
            nodes.add(createNode("plant", "root", "Plant", "asset://decor/plant"));
        } else if (keywords.contains("park") || keywords.contains("公园") || keywords.contains("garden") || keywords.contains("花园")) {
            nodes.add(createNode("tree-1", "root", "Tree-1", "asset://nature/tree_oak"));
            nodes.add(createNode("tree-2", "root", "Tree-2", "asset://nature/tree_pine"));
            nodes.add(createNode("bench-1", "root", "Bench-1", "asset://park/bench"));
            nodes.add(createNode("bench-2", "root", "Bench-2", "asset://park/bench"));
            nodes.add(createNode("path", "root", "Path", "asset://park/path"));
            nodes.add(createNode("fountain", "root", "Fountain", "asset://park/fountain"));
            nodes.add(createNode("flowerbed", "root", "Flowerbed", "asset://nature/flowerbed"));
        } else if (keywords.contains("cyberpunk") || keywords.contains("赛博朋克")) {
            nodes.add(createNode("neon-sign-1", "root", "Neon-Sign-1", "asset://cyberpunk/neon_sign"));
            nodes.add(createNode("neon-sign-2", "root", "Neon-Sign-2", "asset://cyberpunk/neon_sign"));
            nodes.add(createNode("hologram-display", "root", "Hologram", "asset://cyberpunk/hologram"));
            nodes.add(createNode("console", "root", "Console", "asset://cyberpunk/console"));
            nodes.add(createNode("cable-cluster", "root", "Cables", "asset://cyberpunk/cables"));
        } else {
            // 默认场景
            nodes.add(createNode("table-center", "root", "Center-Table", "asset://default/table"));
            nodes.add(createNode("chair-1", "root", "Chair-1", "asset://default/chair"));
            nodes.add(createNode("chair-2", "root", "Chair-2", "asset://default/chair"));
            nodes.add(createNode("plant-decor", "root", "Plant", "asset://decor/plant"));
            nodes.add(createNode("lamp", "root", "Lamp", "asset://lighting/floor_lamp"));
        }

        graph.setNodes(nodes);
        return graph;
    }

    private ProjectConfig.LightingConfig buildLighting(Set<String> keywords) {
        ProjectConfig.LightingConfig lighting = new ProjectConfig.LightingConfig();
        List<ProjectConfig.LightSource> lights = new ArrayList<>();

        if (keywords.contains("warm") || keywords.contains("暖色")) {
            ProjectConfig.LightSource main = createLight("main-light", "directional",
                    new float[]{1.0f, 0.85f, 0.7f}, 0.8f, 50f);
            lights.add(main);
            lighting.setEnvironment(createEnvironment(null, 0.4f));
        } else if (keywords.contains("cool") || keywords.contains("冷色")) {
            ProjectConfig.LightSource main = createLight("main-light", "directional",
                    new float[]{0.7f, 0.8f, 1.0f}, 0.9f, 50f);
            lights.add(main);
            lighting.setEnvironment(createEnvironment(null, 0.5f));
        } else if (keywords.contains("dim") || keywords.contains("昏暗") || keywords.contains("candle") || keywords.contains("烛光")) {
            ProjectConfig.LightSource candle1 = createLight("candle-1", "point",
                    new float[]{1.0f, 0.7f, 0.4f}, 0.5f, 3f);
            ProjectConfig.LightSource candle2 = createLight("candle-2", "point",
                    new float[]{1.0f, 0.7f, 0.4f}, 0.5f, 3f);
            lights.add(candle1);
            lights.add(candle2);
            lighting.setEnvironment(createEnvironment(null, 0.1f));
            lighting.getShadows().setEnabled(true);
            lighting.getShadows().setResolution(1024);
        } else if (keywords.contains("neon") || keywords.contains("霓虹")) {
            ProjectConfig.LightSource neon1 = createLight("neon-pink", "point",
                    new float[]{1.0f, 0.2f, 0.6f}, 1.5f, 8f);
            ProjectConfig.LightSource neon2 = createLight("neon-blue", "point",
                    new float[]{0.2f, 0.5f, 1.0f}, 1.5f, 8f);
            ProjectConfig.LightSource neon3 = createLight("neon-purple", "point",
                    new float[]{0.7f, 0.2f, 1.0f}, 1.2f, 6f);
            lights.add(neon1);
            lights.add(neon2);
            lights.add(neon3);
            lighting.setEnvironment(createEnvironment(null, 0.2f));
        } else if (keywords.contains("bright") || keywords.contains("明亮") || keywords.contains("natural") || keywords.contains("自然光")) {
            ProjectConfig.LightSource sun = createLight("sun", "directional",
                    new float[]{1.0f, 0.95f, 0.85f}, 1.2f, 100f);
            lights.add(sun);
            ProjectConfig.LightSource fill = createLight("fill", "directional",
                    new float[]{0.6f, 0.7f, 0.8f}, 0.4f, 50f);
            lights.add(fill);
            lighting.setEnvironment(createEnvironment("hdri://default/sky_day", 1.0f));
        } else if (keywords.contains("spotlight") || keywords.contains("聚光灯")) {
            ProjectConfig.LightSource spot = createLight("spot-main", "spot",
                    new float[]{1.0f, 1.0f, 1.0f}, 2.0f, 15f);
            spot.setSpotAngle(30f);
            lights.add(spot);
            lighting.setEnvironment(createEnvironment(null, 0.1f));
            lighting.getShadows().setEnabled(true);
            lighting.getShadows().setResolution(2048);
        } else {
            // 默认灯光
            ProjectConfig.LightSource main = createLight("main-light", "directional",
                    new float[]{1.0f, 0.95f, 0.9f}, 0.8f, 50f);
            lights.add(main);
            ProjectConfig.LightSource ambient = createLight("ambient", "point",
                    new float[]{0.5f, 0.5f, 0.6f}, 0.3f, 30f);
            lights.add(ambient);
            lighting.setEnvironment(createEnvironment("hdri://default/studio", 0.5f));
        }

        lighting.setLights(lights);
        return lighting;
    }

    private ProjectConfig.AudioConfig buildAudio(Set<String> keywords) {
        ProjectConfig.AudioConfig audio = new ProjectConfig.AudioConfig();
        List<ProjectConfig.AudioSource> sources = new ArrayList<>();

        if (keywords.contains("jazz") || keywords.contains("爵士")) {
            ProjectConfig.AudioSource bgm = createAudioSource("bgm-jazz", "audio://music/jazz_lounge",
                    0.4f, true, false);
            sources.add(bgm);
        } else if (keywords.contains("classical") || keywords.contains("古典")) {
            ProjectConfig.AudioSource bgm = createAudioSource("bgm-classical", "audio://music/classical_ambient",
                    0.35f, true, false);
            sources.add(bgm);
        } else if (keywords.contains("electronic") || keywords.contains("电子")) {
            ProjectConfig.AudioSource bgm = createAudioSource("bgm-electronic", "audio://music/electronic_chill",
                    0.45f, true, false);
            sources.add(bgm);
        } else if (keywords.contains("nature") || keywords.contains("自然声")) {
            ProjectConfig.AudioSource bgm = createAudioSource("bgm-nature", "audio://ambient/nature_forest",
                    0.3f, true, false);
            sources.add(bgm);
            ProjectConfig.AudioSource birds = createAudioSource("sfx-birds", "audio://sfx/birds_chirping",
                    0.15f, true, true);
            sources.add(birds);
        } else if (keywords.contains("lively") || keywords.contains("热闹")) {
            ProjectConfig.AudioSource bgm = createAudioSource("bgm-lively", "audio://music/upbeat_pop",
                    0.5f, true, false);
            sources.add(bgm);
            ProjectConfig.AudioSource crowd = createAudioSource("amb-crowd", "audio://ambient/crowd_murmur",
                    0.2f, true, true);
            sources.add(crowd);
        } else if (keywords.contains("quiet") || keywords.contains("安静")) {
            // 安静场景，不添加音频源
            audio.setMasterVolume(0.1f);
            return audio;
        } else {
            // 默认轻音乐
            ProjectConfig.AudioSource bgm = createAudioSource("bgm-default", "audio://music/ambient_light",
                    0.3f, true, false);
            sources.add(bgm);
        }

        audio.setSources(sources);
        audio.setSpatialAudio(!keywords.contains("quiet"));
        return audio;
    }

    private ProjectConfig.PhysicsConfig buildPhysics(Set<String> keywords) {
        ProjectConfig.PhysicsConfig physics = new ProjectConfig.PhysicsConfig();
        List<ProjectConfig.Collider> colliders = new ArrayList<>();

        // 默认添加地面碰撞体
        ProjectConfig.Collider floorCollider = new ProjectConfig.Collider();
        floorCollider.setNodeId("floor");
        floorCollider.setType("box");
        floorCollider.setTrigger(false);
        colliders.add(floorCollider);

        // 墙壁碰撞体
        for (String wall : new String[]{"wall-north", "wall-south", "wall-east", "wall-west"}) {
            ProjectConfig.Collider wallCollider = new ProjectConfig.Collider();
            wallCollider.setNodeId(wall);
            wallCollider.setType("box");
            wallCollider.setTrigger(false);
            colliders.add(wallCollider);
        }

        // 大型物体碰撞
        if (keywords.contains("cafe") || keywords.contains("coffee") || keywords.contains("咖啡馆")) {
            addCollider(colliders, "counter", "box");
            addCollider(colliders, "table-1", "box");
            addCollider(colliders, "table-2", "box");
        } else if (keywords.contains("office") || keywords.contains("办公室")) {
            addCollider(colliders, "desk-1", "box");
            addCollider(colliders, "desk-2", "box");
            addCollider(colliders, "bookshelf", "box");
        }

        physics.setColliders(colliders);
        return physics;
    }

    private boolean hasLightingKeywords(Set<String> keywords) {
        String[] lightWords = {"warm", "暖色", "cool", "冷色", "bright", "明亮", "dim", "昏暗",
                "natural", "自然光", "neon", "霓虹", "candle", "烛光", "spotlight", "聚光灯"};
        for (String w : lightWords) {
            if (keywords.contains(w)) return true;
        }
        return false;
    }

    private boolean hasAudioKeywords(Set<String> keywords) {
        String[] audioWords = {"jazz", "爵士", "classical", "古典", "electronic", "电子",
                "nature", "自然声", "quiet", "安静", "lively", "热闹", "bgm", "音乐", "music"};
        for (String w : audioWords) {
            if (keywords.contains(w)) return true;
        }
        return false;
    }

    private boolean hasSceneKeywords(Set<String> keywords) {
        String[] sceneWords = {"cafe", "coffee", "咖啡馆", "gallery", "画廊", "exhibition", "展厅",
                "office", "办公室", "park", "公园", "garden", "花园", "cyberpunk", "赛博朋克",
                "beach", "海滩", "forest", "森林"};
        for (String w : sceneWords) {
            if (keywords.contains(w)) return true;
        }
        return false;
    }

    private ProjectConfig.SceneNode createNode(String id, String parentId, String name, String assetRef) {
        ProjectConfig.SceneNode node = new ProjectConfig.SceneNode();
        node.setNodeId(id);
        node.setParentId(parentId);
        node.setName(name);
        node.setAssetRef(assetRef);
        return node;
    }

    private ProjectConfig.LightSource createLight(String id, String type, float[] color, float intensity, float range) {
        ProjectConfig.LightSource light = new ProjectConfig.LightSource();
        light.setLightId(id);
        light.setType(type);
        light.setColor(color);
        light.setIntensity(intensity);
        light.setRange(range);
        return light;
    }

    private ProjectConfig.EnvironmentLight createEnvironment(String hdriUrl, float intensity) {
        ProjectConfig.EnvironmentLight env = new ProjectConfig.EnvironmentLight();
        env.setHdriUrl(hdriUrl);
        env.setIntensity(intensity);
        return env;
    }

    private ProjectConfig.AudioSource createAudioSource(String id, String assetRef, float volume, boolean loop, boolean spatial) {
        ProjectConfig.AudioSource src = new ProjectConfig.AudioSource();
        src.setSourceId(id);
        src.setAudioAssetRef(assetRef);
        src.setVolume(volume);
        src.setLoop(loop);
        src.setSpatial(spatial);
        return src;
    }

    private void addCollider(List<ProjectConfig.Collider> colliders, String nodeId, String type) {
        ProjectConfig.Collider c = new ProjectConfig.Collider();
        c.setNodeId(nodeId);
        c.setType(type);
        c.setTrigger(false);
        colliders.add(c);
    }
}
