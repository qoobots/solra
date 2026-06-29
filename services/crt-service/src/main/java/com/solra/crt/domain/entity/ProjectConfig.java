package com.solra.crt.domain.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目配置值对象。
 * 包含场景图、光照、音频和物理等完整的空间配置。
 */
public class ProjectConfig {

    private SceneGraph sceneGraph;
    private LightingConfig lighting;
    private AudioConfig audio;
    private PhysicsConfig physics;

    public ProjectConfig() {
        this.sceneGraph = new SceneGraph();
        this.lighting = new LightingConfig();
        this.audio = new AudioConfig();
        this.physics = new PhysicsConfig();
    }

    public SceneGraph getSceneGraph() { return sceneGraph; }
    public void setSceneGraph(SceneGraph sceneGraph) { this.sceneGraph = sceneGraph; }
    public LightingConfig getLighting() { return lighting; }
    public void setLighting(LightingConfig lighting) { this.lighting = lighting; }
    public AudioConfig getAudio() { return audio; }
    public void setAudio(AudioConfig audio) { this.audio = audio; }
    public PhysicsConfig getPhysics() { return physics; }
    public void setPhysics(PhysicsConfig physics) { this.physics = physics; }

    public static class SceneGraph {
        private List<SceneNode> nodes = new ArrayList<>();
        private String rootNodeId;

        public List<SceneNode> getNodes() { return nodes; }
        public void setNodes(List<SceneNode> nodes) { this.nodes = nodes; }
        public String getRootNodeId() { return rootNodeId; }
        public void setRootNodeId(String rootNodeId) { this.rootNodeId = rootNodeId; }
    }

    public static class SceneNode {
        private String nodeId;
        private String parentId;
        private String name;
        private String assetRef;
        private boolean visible = true;
        private boolean locked;

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getParentId() { return parentId; }
        public void setParentId(String parentId) { this.parentId = parentId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAssetRef() { return assetRef; }
        public void setAssetRef(String assetRef) { this.assetRef = assetRef; }
        public boolean isVisible() { return visible; }
        public void setVisible(boolean visible) { this.visible = visible; }
        public boolean isLocked() { return locked; }
        public void setLocked(boolean locked) { this.locked = locked; }
    }

    public static class LightingConfig {
        private List<LightSource> lights = new ArrayList<>();
        private EnvironmentLight environment;
        private ShadowConfig shadows = new ShadowConfig();

        public List<LightSource> getLights() { return lights; }
        public void setLights(List<LightSource> lights) { this.lights = lights; }
        public EnvironmentLight getEnvironment() { return environment; }
        public void setEnvironment(EnvironmentLight environment) { this.environment = environment; }
        public ShadowConfig getShadows() { return shadows; }
        public void setShadows(ShadowConfig shadows) { this.shadows = shadows; }
    }

    public static class LightSource {
        private String lightId;
        private String type;
        private float[] color = {1.0f, 1.0f, 1.0f};
        private float intensity = 1.0f;
        private float range = 10.0f;
        private float spotAngle;

        public String getLightId() { return lightId; }
        public void setLightId(String lightId) { this.lightId = lightId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public float[] getColor() { return color; }
        public void setColor(float[] color) { this.color = color; }
        public float getIntensity() { return intensity; }
        public void setIntensity(float intensity) { this.intensity = intensity; }
        public float getRange() { return range; }
        public void setRange(float range) { this.range = range; }
        public float getSpotAngle() { return spotAngle; }
        public void setSpotAngle(float spotAngle) { this.spotAngle = spotAngle; }
    }

    public static class EnvironmentLight {
        private String hdriUrl;
        private float intensity = 1.0f;

        public String getHdriUrl() { return hdriUrl; }
        public void setHdriUrl(String hdriUrl) { this.hdriUrl = hdriUrl; }
        public float getIntensity() { return intensity; }
        public void setIntensity(float intensity) { this.intensity = intensity; }
    }

    public static class ShadowConfig {
        private boolean enabled = true;
        private int resolution = 2048;
        private float bias = 0.005f;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getResolution() { return resolution; }
        public void setResolution(int resolution) { this.resolution = resolution; }
        public float getBias() { return bias; }
        public void setBias(float bias) { this.bias = bias; }
    }

    public static class AudioConfig {
        private float masterVolume = 1.0f;
        private boolean spatialAudio = true;
        private List<AudioSource> sources = new ArrayList<>();

        public float getMasterVolume() { return masterVolume; }
        public void setMasterVolume(float masterVolume) { this.masterVolume = masterVolume; }
        public boolean isSpatialAudio() { return spatialAudio; }
        public void setSpatialAudio(boolean spatialAudio) { this.spatialAudio = spatialAudio; }
        public List<AudioSource> getSources() { return sources; }
        public void setSources(List<AudioSource> sources) { this.sources = sources; }
    }

    public static class AudioSource {
        private String sourceId;
        private String audioAssetRef;
        private float volume = 1.0f;
        private boolean loop;
        private boolean spatial = true;

        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }
        public String getAudioAssetRef() { return audioAssetRef; }
        public void setAudioAssetRef(String audioAssetRef) { this.audioAssetRef = audioAssetRef; }
        public float getVolume() { return volume; }
        public void setVolume(float volume) { this.volume = volume; }
        public boolean isLoop() { return loop; }
        public void setLoop(boolean loop) { this.loop = loop; }
        public boolean isSpatial() { return spatial; }
        public void setSpatial(boolean spatial) { this.spatial = spatial; }
    }

    public static class PhysicsConfig {
        private boolean enabled = true;
        private List<Collider> colliders = new ArrayList<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<Collider> getColliders() { return colliders; }
        public void setColliders(List<Collider> colliders) { this.colliders = colliders; }
    }

    public static class Collider {
        private String nodeId;
        private String type;
        private boolean isTrigger;

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public boolean isTrigger() { return isTrigger; }
        public void setTrigger(boolean trigger) { isTrigger = trigger; }
    }
}
