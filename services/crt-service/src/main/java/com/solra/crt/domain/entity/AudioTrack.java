package com.solra.crt.domain.entity;

import java.time.Instant;
import java.util.List;

/**
 * 音频曲目实体 (CRT-007)。
 * 表示内置曲库或用户上传的音频曲目。
 */
public class AudioTrack {

    public enum TrackCategory {
        BGM, SFX, AMBIENT, UI_SOUND, VOICE
    }

    public enum Mood {
        RELAXING, ENERGETIC, MYSTERIOUS, ROMANTIC, EPIC, NEUTRAL,
        HAPPY, SAD, SCARY, FOCUS, NATURE, URBAN
    }

    public enum Source {
        BUILTIN, USER_UPLOAD, THIRD_PARTY
    }

    private String trackId;
    private String title;
    private String artist;
    private TrackCategory category;
    private List<Mood> moods;
    private Source source;
    private String ownerId;       // 上传者ID（仅 USER_UPLOAD）
    private String assetRef;      // 音频资源引用路径
    private int durationSeconds;
    private long fileSizeBytes;
    private String format;        // mp3, wav, ogg, flac
    private int sampleRateHz;
    private boolean loopable;
    private float defaultVolume;
    private List<String> tags;
    private int usageCount;
    private float rating;
    private Instant uploadedAt;
    private Instant createdAt;

    public AudioTrack() {
        this.createdAt = Instant.now();
        this.usageCount = 0;
        this.rating = 0.0f;
        this.defaultVolume = 0.5f;
        this.loopable = false;
    }

    public void incrementUsage() { this.usageCount++; }

    public void updateRating(float rating) {
        this.rating = Math.max(0.0f, Math.min(5.0f, rating));
    }

    // ── Getters and Setters ──

    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public TrackCategory getCategory() { return category; }
    public void setCategory(TrackCategory category) { this.category = category; }
    public List<Mood> getMoods() { return moods; }
    public void setMoods(List<Mood> moods) { this.moods = moods; }
    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getAssetRef() { return assetRef; }
    public void setAssetRef(String assetRef) { this.assetRef = assetRef; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public int getSampleRateHz() { return sampleRateHz; }
    public void setSampleRateHz(int sampleRateHz) { this.sampleRateHz = sampleRateHz; }
    public boolean isLoopable() { return loopable; }
    public void setLoopable(boolean loopable) { this.loopable = loopable; }
    public float getDefaultVolume() { return defaultVolume; }
    public void setDefaultVolume(float defaultVolume) { this.defaultVolume = defaultVolume; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public int getUsageCount() { return usageCount; }
    public float getRating() { return rating; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
