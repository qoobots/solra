package com.solra.soc.application.dto;

import com.solra.soc.domain.model.AudioSource;
import com.solra.soc.domain.service.SpatialAudioEngine;

import java.util.List;

/**
 * SOC-007 空间音频相关 DTO。
 */

/** 注册声源命令。 */
public class AudioSourceCommand {
    private String sessionId;
    private String ownerUserId;
    private String type;        // MICROPHONE, SOUND_EFFECT, BACKGROUND_MUSIC, AMBIENT
    private float positionX, positionY, positionZ;
    private float volume = 0.8f;
    private float minDistance = 1f;
    private float maxDistance = 15f;
    private float rolloffFactor = 1.5f;
    private boolean spatialized = true;
    private boolean loop;

    public AudioSourceCommand() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public float getPositionX() { return positionX; }
    public void setPositionX(float positionX) { this.positionX = positionX; }
    public float getPositionY() { return positionY; }
    public void setPositionY(float positionY) { this.positionY = positionY; }
    public float getPositionZ() { return positionZ; }
    public void setPositionZ(float positionZ) { this.positionZ = positionZ; }
    public float getVolume() { return volume; }
    public void setVolume(float volume) { this.volume = volume; }
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
}

/** 声源 DTO。 */
public record AudioSourceDTO(
        String sourceId,
        String sessionId,
        String ownerUserId,
        String type,
        float positionX, float positionY, float positionZ,
        float volume,
        float minDistance, float maxDistance,
        float rolloffFactor,
        boolean spatialized,
        boolean loop
) {
    public static AudioSourceDTO from(AudioSource src) {
        return new AudioSourceDTO(
                src.getSourceId(),
                src.getSessionId(),
                src.getOwnerUserId(),
                src.getType().name(),
                src.getPositionX(), src.getPositionY(), src.getPositionZ(),
                src.getVolume(),
                src.getMinDistance(), src.getMaxDistance(),
                src.getRolloffFactor(),
                src.isSpatialized(),
                src.isLoop()
        );
    }
}

/** 音频混合结果 DTO。 */
public record AudioMixResultDTO(
        String sourceId,
        String type,
        float volume,
        float pan,
        float reverb,
        float dopplerShift,
        boolean isClear,
        float distance
) {
    public static AudioMixResultDTO from(SpatialAudioEngine.AudioMixResult mix) {
        return new AudioMixResultDTO(
                mix.sourceId(),
                mix.type().name(),
                mix.volume(),
                mix.pan(),
                mix.reverb(),
                mix.dopplerShift(),
                mix.isClear(),
                mix.distance()
        );
    }
}

/** 空间音频混合输出 DTO。 */
public record SpatialAudioMixDTO(
        String sessionId,
        String listenerUserId,
        List<AudioMixResultDTO> mixes,
        int sourceCount
) {}
