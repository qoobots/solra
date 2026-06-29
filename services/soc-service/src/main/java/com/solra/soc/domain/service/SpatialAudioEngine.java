package com.solra.soc.domain.service;

import com.solra.soc.domain.model.AudioListener;
import com.solra.soc.domain.model.AudioSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpatialAudioEngine — SOC-007 空间音频引擎领域服务。
 *
 * 管理空间音频的声源注册、听者注册、距离衰减计算和音频混合。
 *
 * 核心算法：
 * - 距离衰减：使用反距离衰减模型，minDistance 内音量 100%，
 *   maxDistance 外音量 0%，中间按 rolloffFactor 衰减。
 * - 声源定位：根据听者朝向和声源方位角计算左右声道平衡（pan）。
 * - 多普勒效应：基于声源与听者相对速度的频率偏移（简化模型）。
 * - 混响模拟：远距离声源增加混响系数。
 *
 * 验收标准：5米范围内语音清晰，>5米渐弱+混响。
 */
public class SpatialAudioEngine {

    private static final Logger log = LoggerFactory.getLogger(SpatialAudioEngine.class);

    /** 会话声源：sessionId -> Map<sourceId, AudioSource> */
    private final Map<String, Map<String, AudioSource>> sessionSources = new ConcurrentHashMap<>();

    /** 会话听者：sessionId -> Map<userId, AudioListener> */
    private final Map<String, Map<String, AudioListener>> sessionListeners = new ConcurrentHashMap<>();

    /** 声源上次位置（用于计算速度/多普勒）：sourceId -> lastPosition[3] + lastTimeMs */
    private final Map<String, SourceVelocity> sourceVelocities = new ConcurrentHashMap<>();

    /** 默认清晰距离（米），在此距离内语音 100% 清晰 */
    private static final float DEFAULT_CLARITY_DISTANCE = 5.0f;

    /** 混响开始距离（米），超出此距离开始添加混响 */
    private static final float REVERB_START_DISTANCE = 5.0f;

    /** 最大混响系数 */
    private static final float MAX_REVERB = 0.6f;

    public SpatialAudioEngine() {}

    // ===== Source Management =====

    /**
     * Register a new audio source in a session.
     */
    public AudioSource registerSource(AudioSource source) {
        sessionSources.computeIfAbsent(source.getSessionId(), k -> new ConcurrentHashMap<>())
                .put(source.getSourceId(), source);
        log.info("SOC-007 audio source registered: session={} source={} type={}",
                source.getSessionId(), source.getSourceId(), source.getType());
        return source;
    }

    /**
     * Register a microphone source for a user.
     */
    public AudioSource registerMicrophone(String sessionId, String userId,
                                           float x, float y, float z) {
        return registerSource(AudioSource.microphone(sessionId, userId, x, y, z));
    }

    /**
     * Register a sound effect at a position.
     */
    public AudioSource registerSoundEffect(String sessionId, String effectId,
                                            float x, float y, float z,
                                            float volume, float maxDistance) {
        return registerSource(AudioSource.soundEffect(sessionId, effectId, x, y, z, volume, maxDistance));
    }

    /**
     * Register background music.
     */
    public AudioSource registerBackgroundMusic(String sessionId, String musicId, float volume) {
        return registerSource(AudioSource.backgroundMusic(sessionId, musicId, volume));
    }

    /**
     * Register ambient sound.
     */
    public AudioSource registerAmbient(String sessionId, String ambientId,
                                        float x, float y, float z,
                                        float volume, float maxDistance) {
        return registerSource(AudioSource.ambient(sessionId, ambientId, x, y, z, volume, maxDistance));
    }

    /**
     * Remove an audio source.
     */
    public void removeSource(String sessionId, String sourceId) {
        Map<String, AudioSource> sources = sessionSources.get(sessionId);
        if (sources != null) {
            sources.remove(sourceId);
            sourceVelocities.remove(sourceId);
            log.info("SOC-007 audio source removed: session={} source={}", sessionId, sourceId);
        }
    }

    /**
     * Update source position.
     */
    public void updateSourcePosition(String sessionId, String sourceId,
                                      float x, float y, float z) {
        Map<String, AudioSource> sources = sessionSources.get(sessionId);
        if (sources == null) return;
        AudioSource src = sources.get(sourceId);
        if (src == null) return;

        // Track velocity for Doppler effect
        SourceVelocity vel = sourceVelocities.computeIfAbsent(sourceId,
                k -> new SourceVelocity(x, y, z, System.currentTimeMillis()));
        long now = System.currentTimeMillis();
        float dt = (now - vel.lastTimeMs) / 1000f;
        if (dt > 0.001f) {
            vel.vx = (x - vel.lx) / dt;
            vel.vy = (y - vel.ly) / dt;
            vel.vz = (z - vel.lz) / dt;
        }
        vel.lx = x;
        vel.ly = y;
        vel.lz = z;
        vel.lastTimeMs = now;

        src.updatePosition(x, y, z);
    }

    // ===== Listener Management =====

    /**
     * Register or update a listener.
     */
    public AudioListener registerListener(AudioListener listener) {
        // Store globally by userId (listeners are session-agnostic for simplicity)
        log.debug("SOC-007 listener registered: user={} pos=({},{},{})",
                listener.getUserId(), listener.getPositionX(),
                listener.getPositionY(), listener.getPositionZ());
        return listener;
    }

    /**
     * Update listener transform.
     */
    public void updateListenerTransform(String userId, float x, float y, float z,
                                         float fx, float fy, float fz) {
        // Update listener position in all session listeners
        for (Map<String, AudioListener> listeners : sessionListeners.values()) {
            AudioListener l = listeners.get(userId);
            if (l != null) {
                l.updateTransform(x, y, z, fx, fy, fz);
            }
        }
    }

    /**
     * Register a listener for a specific session.
     */
    public void registerSessionListener(String sessionId, AudioListener listener) {
        sessionListeners.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(listener.getUserId(), listener);
        log.debug("SOC-007 session listener registered: session={} user={}",
                sessionId, listener.getUserId());
    }

    /**
     * Remove a listener from a session.
     */
    public void removeSessionListener(String sessionId, String userId) {
        Map<String, AudioListener> listeners = sessionListeners.get(sessionId);
        if (listeners != null) {
            listeners.remove(userId);
        }
    }

    // ===== Spatial Audio Calculation =====

    /**
     * Calculate the mixed audio for a specific listener in a session.
     * Returns a list of per-source audio mix parameters.
     */
    public List<AudioMixResult> calculateMix(String sessionId, String listenerUserId) {
        Map<String, AudioSource> sources = sessionSources.getOrDefault(sessionId, Map.of());
        Map<String, AudioListener> listeners = sessionListeners.getOrDefault(sessionId, Map.of());
        AudioListener listener = listeners.get(listenerUserId);

        if (listener == null) {
            return List.of();
        }

        List<AudioMixResult> results = new ArrayList<>();
        for (AudioSource source : sources.values()) {
            // Don't mix own microphone
            if (source.getOwnerUserId() != null
                    && source.getOwnerUserId().equals(listenerUserId)
                    && source.getType() == AudioSource.AudioSourceType.MICROPHONE) {
                continue;
            }

            AudioMixResult mix = calculateSingleSource(source, listener);
            if (mix != null) {
                results.add(mix);
            }
        }

        return results;
    }

    /**
     * Calculate audio mix for a single source and listener pair.
     */
    private AudioMixResult calculateSingleSource(AudioSource source, AudioListener listener) {
        // For non-spatialized sources (e.g., BGM), use flat mix
        if (!source.isSpatialized()) {
            return new AudioMixResult(source.getSourceId(), source.getType(),
                    source.getVolume() * listener.getMasterVolume(),
                    0f, 0f, 0f, false, 0f);
        }

        // Calculate distance
        float dx = source.getPositionX() - listener.getPositionX();
        float dy = source.getPositionY() - listener.getPositionY();
        float dz = source.getPositionZ() - listener.getPositionZ();
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Distance attenuation
        float attenuatedVolume;
        if (distance <= source.getMinDistance()) {
            attenuatedVolume = source.getVolume();
        } else if (distance >= source.getMaxDistance()) {
            attenuatedVolume = 0f;
        } else {
            // Inverse distance attenuation with rolloff
            float range = source.getMaxDistance() - source.getMinDistance();
            float t = (distance - source.getMinDistance()) / range;
            // Apply rolloff: t^rolloffFactor for smoother falloff
            attenuatedVolume = source.getVolume() * (float) Math.pow(1f - t, source.getRolloffFactor());
        }

        // If inaudible, skip
        if (attenuatedVolume < 0.001f) {
            return null;
        }

        // Apply listener master volume
        attenuatedVolume *= listener.getMasterVolume();

        // Calculate pan (left/right balance based on azimuth)
        float pan = calculatePan(dx, dz, listener.getForwardX(), listener.getForwardZ());

        // Calculate reverb based on distance
        float reverb = calculateReverb(distance);

        // Calculate Doppler shift (simplified)
        float dopplerShift = calculateDoppler(source.getSourceId(), dx, dy, dz);

        // Clarity check for voice sources
        boolean isClear = source.getType() == AudioSource.AudioSourceType.MICROPHONE
                && distance <= DEFAULT_CLARITY_DISTANCE;

        return new AudioMixResult(source.getSourceId(), source.getType(),
                attenuatedVolume, pan, reverb, dopplerShift, isClear, distance);
    }

    /**
     * Calculate stereo pan based on listener orientation and source position.
     * Returns -1.0 (full left) to 1.0 (full right).
     */
    private float calculatePan(float dx, float dz, float fx, float fz) {
        // Normalize forward vector
        float fLen = (float) Math.sqrt(fx * fx + fz * fz);
        if (fLen < 0.001f) return 0f;
        float nfx = fx / fLen;
        float nfz = fz / fLen;

        // Right vector (perpendicular to forward in XZ plane)
        float rx = nfz;
        float rz = -nfx;

        // Project source direction onto right vector for pan
        float dLen = (float) Math.sqrt(dx * dx + dz * dz);
        if (dLen < 0.001f) return 0f;

        float panValue = (dx * rx + dz * rz) / dLen;
        return Math.max(-1f, Math.min(1f, panValue));
    }

    /**
     * Calculate reverb coefficient based on distance.
     * Closer than REVERB_START_DISTANCE = no reverb,
     * Beyond that = linearly increasing reverb up to MAX_REVERB.
     */
    private float calculateReverb(float distance) {
        if (distance <= REVERB_START_DISTANCE) {
            return 0f;
        }
        // Full reverb at 3x the start distance
        float maxReverbDistance = REVERB_START_DISTANCE * 3f;
        float t = Math.min(1f, (distance - REVERB_START_DISTANCE)
                / (maxReverbDistance - REVERB_START_DISTANCE));
        return t * MAX_REVERB;
    }

    /**
     * Calculate Doppler pitch shift (simplified).
     * Returns pitch multiplier (1.0 = no shift, >1 = higher pitch/approaching,
     * <1 = lower pitch/receding).
     */
    private float calculateDoppler(String sourceId, float dx, float dy, float dz) {
        SourceVelocity vel = sourceVelocities.get(sourceId);
        if (vel == null) return 1f;

        float dLen = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dLen < 0.001f) return 1f;

        // Radial velocity (positive = approaching)
        float radialVel = -(dx * vel.vx + dy * vel.vy + dz * vel.vz) / dLen;

        // Speed of sound = 343 m/s
        float speedOfSound = 343f;
        float shift = speedOfSound / (speedOfSound - radialVel);

        // Clamp to reasonable range (0.5x to 2x)
        return Math.max(0.5f, Math.min(2f, shift));
    }

    // ===== Queries =====

    /**
     * Get all audio sources in a session.
     */
    public List<AudioSource> getSessionSources(String sessionId) {
        Map<String, AudioSource> sources = sessionSources.getOrDefault(sessionId, Map.of());
        return List.copyOf(sources.values());
    }

    /**
     * Get a specific audio source.
     */
    public Optional<AudioSource> getSource(String sessionId, String sourceId) {
        Map<String, AudioSource> sources = sessionSources.get(sessionId);
        return sources != null ? Optional.ofNullable(sources.get(sourceId)) : Optional.empty();
    }

    /**
     * Get audio engine statistics.
     */
    public AudioEngineStats getStats() {
        int totalSessions = sessionSources.size();
        int totalSources = sessionSources.values().stream().mapToInt(Map::size).sum();
        int totalListeners = sessionListeners.values().stream().mapToInt(Map::size).sum();
        return new AudioEngineStats(totalSessions, totalSources, totalListeners);
    }

    /**
     * Clean up all audio resources for a session.
     */
    public void cleanup(String sessionId) {
        Map<String, AudioSource> sources = sessionSources.remove(sessionId);
        if (sources != null) {
            sources.keySet().forEach(sourceVelocities::remove);
        }
        sessionListeners.remove(sessionId);
        log.info("SOC-007 audio engine cleaned for session: {}", sessionId);
    }

    // -- Inner types --

    /** Per-source audio mix parameters. */
    public record AudioMixResult(
            String sourceId,
            AudioSource.AudioSourceType type,
            float volume,           // 0.0-1.0 attenuated volume
            float pan,              // -1.0 (left) to 1.0 (right)
            float reverb,           // 0.0-1.0 reverb amount
            float dopplerShift,     // pitch multiplier (1.0 = normal)
            boolean isClear,        // whether voice is clear (within clarity distance)
            float distance          // distance from listener to source in meters
    ) {}

    /** Audio engine statistics. */
    public record AudioEngineStats(int totalSessions, int totalSources, int totalListeners) {}

    /** Internal velocity tracking for Doppler effect. */
    private static class SourceVelocity {
        float lx, ly, lz;       // last position
        float vx, vy, vz;       // velocity
        long lastTimeMs;

        SourceVelocity(float x, float y, float z, long timeMs) {
            this.lx = x;
            this.ly = y;
            this.lz = z;
            this.lastTimeMs = timeMs;
        }
    }
}
