package com.solra.soc.domain.model;

/**
 * AudioListener — SOC-007 空间听者状态值对象。
 *
 * 描述空间中一个听者的 3D 位置和朝向，用于空间音频计算。
 * 每个参与者都是一个潜在的听者，接收混合后的空间音频。
 */
public class AudioListener {

    private String userId;
    private float positionX, positionY, positionZ;
    private float forwardX, forwardY, forwardZ;   // facing direction
    private float upX = 0, upY = 1, upZ = 0;      // up vector
    private float earDistance = 0.15f;              // distance between ears (meters)
    private float masterVolume = 1f;                // 0.0-1.0

    public AudioListener() {}

    /**
     * Create an audio listener at a position.
     */
    public AudioListener(String userId, float x, float y, float z,
                          float forwardX, float forwardY, float forwardZ) {
        this.userId = userId;
        this.positionX = x;
        this.positionY = y;
        this.positionZ = z;
        this.forwardX = forwardX;
        this.forwardY = forwardY;
        this.forwardZ = forwardZ;
    }

    /**
     * Update listener position and orientation.
     */
    public void updateTransform(float x, float y, float z,
                                 float fx, float fy, float fz) {
        this.positionX = x;
        this.positionY = y;
        this.positionZ = z;
        this.forwardX = fx;
        this.forwardY = fy;
        this.forwardZ = fz;
    }

    /**
     * Set master volume (0.0 = mute, 1.0 = full).
     */
    public void setMasterVolume(float vol) {
        this.masterVolume = Math.max(0f, Math.min(1f, vol));
    }

    // -- Getters/Setters --
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public float getPositionX() { return positionX; }
    public void setPositionX(float x) { this.positionX = x; }
    public float getPositionY() { return positionY; }
    public void setPositionY(float y) { this.positionY = y; }
    public float getPositionZ() { return positionZ; }
    public void setPositionZ(float z) { this.positionZ = z; }
    public float getForwardX() { return forwardX; }
    public void setForwardX(float fx) { this.forwardX = fx; }
    public float getForwardY() { return forwardY; }
    public void setForwardY(float fy) { this.forwardY = fy; }
    public float getForwardZ() { return forwardZ; }
    public void setForwardZ(float fz) { this.forwardZ = fz; }
    public float getUpX() { return upX; }
    public void setUpX(float ux) { this.upX = ux; }
    public float getUpY() { return upY; }
    public void setUpY(float uy) { this.upY = uy; }
    public float getUpZ() { return upZ; }
    public void setUpZ(float uz) { this.upZ = uz; }
    public float getEarDistance() { return earDistance; }
    public void setEarDistance(float d) { this.earDistance = d; }
    public float getMasterVolume() { return masterVolume; }
}
