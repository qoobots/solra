package com.solra.soc.domain.model;

/**
 * AudioSource — SOC-007 空间声源实体。
 *
 * 描述空间中的一个声源，包含 3D 位置、类型、音量、衰减参数等。
 * 用于空间音频引擎计算每个听者接收到的声音混合。
 */
public class AudioSource {

    private String sourceId;
    private String sessionId;
    private String ownerUserId;           // which user/entity produces this sound
    private AudioSourceType type;
    private float positionX, positionY, positionZ;
    private float volume;                 // base volume 0.0-1.0
    private float minDistance;            // distance at which volume = 100% (meters)
    private float maxDistance;            // distance at which volume = 0% (meters)
    private float rolloffFactor;          // attenuation curve steepness (1.0 = linear)
    private boolean spatialized;          // whether 3D positioning applies
    private boolean loop;
    private long startedAtMs;

    private AudioSource() {}

    /**
     * Create a spatial audio source.
     */
    public static AudioSource create(String sourceId, String sessionId, String ownerUserId,
                                      AudioSourceType type, float x, float y, float z,
                                      float volume, float minDistance, float maxDistance,
                                      float rolloffFactor, boolean spatialized, boolean loop) {
        AudioSource src = new AudioSource();
        src.sourceId = sourceId;
        src.sessionId = sessionId;
        src.ownerUserId = ownerUserId;
        src.type = type;
        src.positionX = x;
        src.positionY = y;
        src.positionZ = z;
        src.volume = clamp(volume, 0f, 1f);
        src.minDistance = Math.max(0.1f, minDistance);
        src.maxDistance = Math.max(src.minDistance + 0.1f, maxDistance);
        src.rolloffFactor = clamp(rolloffFactor, 0.5f, 3f);
        src.spatialized = spatialized;
        src.loop = loop;
        src.startedAtMs = System.currentTimeMillis();
        return src;
    }

    /**
     * Factory: user microphone source.
     */
    public static AudioSource microphone(String sessionId, String userId,
                                          float x, float y, float z) {
        return create("mic_" + userId, sessionId, userId, AudioSourceType.MICROPHONE,
                x, y, z, 0.8f, 1f, 15f, 1.5f, true, false);
    }

    /**
     * Factory: spatial sound effect (SFX) at a position.
     */
    public static AudioSource soundEffect(String sessionId, String effectId,
                                           float x, float y, float z, float volume,
                                           float maxDistance) {
        return create("sfx_" + effectId, sessionId, null, AudioSourceType.SOUND_EFFECT,
                x, y, z, volume, 0.5f, maxDistance, 2f, true, false);
    }

    /**
     * Factory: background music (non-spatialized, fills the space).
     */
    public static AudioSource backgroundMusic(String sessionId, String musicId, float volume) {
        return create("bgm_" + musicId, sessionId, null, AudioSourceType.BACKGROUND_MUSIC,
                0, 0, 0, volume, 0, 0, 1f, false, true);
    }

    /**
     * Factory: ambient sound (spatialized, looping).
     */
    public static AudioSource ambient(String sessionId, String ambientId,
                                       float x, float y, float z, float volume,
                                       float maxDistance) {
        return create("amb_" + ambientId, sessionId, null, AudioSourceType.AMBIENT,
                x, y, z, volume, 1f, maxDistance, 1f, true, true);
    }

    /**
     * Update position of this audio source.
     */
    public void updatePosition(float x, float y, float z) {
        this.positionX = x;
        this.positionY = y;
        this.positionZ = z;
    }

    /**
     * Update volume.
     */
    public void updateVolume(float volume) {
        this.volume = clamp(volume, 0f, 1f);
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    // -- Getters/Setters --
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
    public AudioSourceType getType() { return type; }
    public void setType(AudioSourceType type) { this.type = type; }
    public float getPositionX() { return positionX; }
    public void setPositionX(float x) { this.positionX = x; }
    public float getPositionY() { return positionY; }
    public void setPositionY(float y) { this.positionY = y; }
    public float getPositionZ() { return positionZ; }
    public void setPositionZ(float z) { this.positionZ = z; }
    public float getVolume() { return volume; }
    public void setVolume(float volume) { this.volume = clamp(volume, 0f, 1f); }
    public float getMinDistance() { return minDistance; }
    public void setMinDistance(float minDistance) { this.minDistance = minDistance; }
    public float getMaxDistance() { return maxDistance; }
    public void setMaxDistance(float maxDistance) { this.maxDistance = maxDistance; }
    public float getRolloffFactor() { return rolloffFactor; }
    public void setRolloffFactor(float rolloffFactor) { this.rolloffFactor = rolloffFactor; }
    public boolean isSpatialized() { return spatialized; }
    public void setSpatialized(boolean spatialized) { this.spatialized = spatialized; }
    public boolean isLoop() { return loop; }
    public void setLoop(boolean loop) { this.loop = loop; }
    public long getStartedAtMs() { return startedAtMs; }
    public void setStartedAtMs(long startedAtMs) { this.startedAtMs = startedAtMs; }

    public enum AudioSourceType {
        MICROPHONE,         // 用户麦克风
        SOUND_EFFECT,       // 音效
        BACKGROUND_MUSIC,   // 背景音乐
        AMBIENT             // 环境音
    }
}
