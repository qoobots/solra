package com.solra.avt.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * SpatialPosition — AVT-007 虚拟人空间移动与追踪。
 *
 * 虚拟人在3D空间中的位置、移动路径和导航能力。
 * 支持：3D坐标、朝向、移动路径、导航区域、空间感知。
 */
public class SpatialPosition {

    private String avatarId;
    private float x, y, z;               // 3D 世界坐标
    private float rotationY;              // Y轴旋转（朝向，0-360度）
    private float rotationX;              // X轴旋转（俯仰角）
    private String currentZoneId;         // 当前所在区域ID
    private MovementState movementState;  // 当前移动状态
    private List<Waypoint> path;          // 移动路径
    private int currentWaypointIndex;     // 当前路径点索引
    private float moveSpeed;              // 移动速度 (m/s)
    private Instant lastMoved;            // 最后移动时间
    private Instant createdAt;

    // 空间感知
    private List<String> visibleUsers;    // 视线范围内的用户
    private List<String> nearbyObjects;   // 附近物体
    private float awarenessRadius;        // 感知半径（米）

    public SpatialPosition() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.rotationY = 0;
        this.movementState = MovementState.IDLE;
        this.path = new ArrayList<>();
        this.currentWaypointIndex = 0;
        this.moveSpeed = 1.5f; // default walking speed
        this.awarenessRadius = 10.0f;
        this.visibleUsers = new ArrayList<>();
        this.nearbyObjects = new ArrayList<>();
        this.createdAt = Instant.now();
        this.lastMoved = this.createdAt;
    }

    public SpatialPosition(String avatarId, float x, float y, float z) {
        this();
        this.avatarId = avatarId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Move towards a target position.
     * Returns the new position after one step of movement.
     */
    public SpatialPosition moveTowards(float targetX, float targetY, float targetZ, float deltaSeconds) {
        float dx = targetX - x;
        float dy = targetY - y;
        float dz = targetZ - z;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 0.1f) {
            // Reached destination
            this.movementState = MovementState.IDLE;
            this.lastMoved = Instant.now();
            return this;
        }

        // Normalize direction
        float stepDistance = moveSpeed * deltaSeconds;
        if (stepDistance >= distance) {
            this.x = targetX;
            this.y = targetY;
            this.z = targetZ;
            this.movementState = MovementState.IDLE;
        } else {
            float ratio = stepDistance / distance;
            this.x += dx * ratio;
            this.y += dy * ratio;
            this.z += dz * ratio;
            this.movementState = MovementState.WALKING;
        }

        // Update rotation to face direction
        this.rotationY = (float) Math.toDegrees(Math.atan2(dx, dz));

        this.lastMoved = Instant.now();
        return this;
    }

    /**
     * Follow a predefined path of waypoints.
     */
    public SpatialPosition followPath(float deltaSeconds) {
        if (path.isEmpty() || currentWaypointIndex >= path.size()) {
            this.movementState = MovementState.IDLE;
            return this;
        }

        Waypoint target = path.get(currentWaypointIndex);
        moveTowards(target.x(), target.y(), target.z(), deltaSeconds);

        // Check if reached current waypoint
        float dx = target.x() - x;
        float dy = target.y() - y;
        float dz = target.z() - z;
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) < 0.1f) {
            currentWaypointIndex++;

            // Pause at waypoint if specified
            if (target.pauseSeconds() > 0) {
                this.movementState = MovementState.PAUSED;
            }
        }

        return this;
    }

    /**
     * Look at a specific target position.
     */
    public void lookAt(float targetX, float targetZ) {
        float dx = targetX - x;
        float dz = targetZ - z;
        this.rotationY = (float) Math.toDegrees(Math.atan2(dx, dz));
    }

    /**
     * Check if a user is within the avatar's awareness radius.
     */
    public boolean isUserInAwarenessRadius(float userX, float userY, float userZ) {
        float dx = userX - x;
        float dy = userY - y;
        float dz = userZ - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz) <= awarenessRadius;
    }

    /**
     * Check if a user is within the avatar's field of view.
     */
    public boolean isUserInFieldOfView(float userX, float userZ, float fovDegrees) {
        float dx = userX - x;
        float dz = userZ - z;
        float angleToUser = (float) Math.toDegrees(Math.atan2(dx, dz));
        float angleDiff = Math.abs(normalizeAngle(angleToUser - rotationY));
        return angleDiff <= fovDegrees / 2.0f;
    }

    /**
     * Set a movement path for the avatar to follow.
     */
    public void setPath(List<Waypoint> newPath) {
        this.path = new ArrayList<>(newPath);
        this.currentWaypointIndex = 0;
        this.movementState = MovementState.WALKING;
    }

    /**
     * Clear current movement path.
     */
    public void clearPath() {
        this.path.clear();
        this.currentWaypointIndex = 0;
        this.movementState = MovementState.IDLE;
    }

    /**
     * Calculate distance to another position.
     */
    public float distanceTo(float tx, float ty, float tz) {
        float dx = tx - x;
        float dy = ty - y;
        float dz = tz - z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private float normalizeAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    // -- Getters and Setters --
    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public void setPosition(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    public float getRotationY() { return rotationY; }
    public float getRotationX() { return rotationX; }
    public void setRotation(float x, float y) { this.rotationX = x; this.rotationY = y; }
    public String getCurrentZoneId() { return currentZoneId; }
    public void setCurrentZoneId(String currentZoneId) { this.currentZoneId = currentZoneId; }
    public MovementState getMovementState() { return movementState; }
    public List<Waypoint> getPath() { return path; }
    public int getCurrentWaypointIndex() { return currentWaypointIndex; }
    public float getMoveSpeed() { return moveSpeed; }
    public void setMoveSpeed(float moveSpeed) { this.moveSpeed = moveSpeed; }
    public Instant getLastMoved() { return lastMoved; }
    public Instant getCreatedAt() { return createdAt; }
    public List<String> getVisibleUsers() { return visibleUsers; }
    public List<String> getNearbyObjects() { return nearbyObjects; }
    public float getAwarenessRadius() { return awarenessRadius; }
    public void setAwarenessRadius(float awarenessRadius) { this.awarenessRadius = awarenessRadius; }

    /**
     * Movement states.
     */
    public enum MovementState {
        IDLE,       // 静止
        WALKING,    // 行走中
        RUNNING,    // 跑动
        PAUSED,     // 路径暂停
        TELEPORTING // 传送中
    }

    /**
     * A waypoint in a movement path.
     */
    public record Waypoint(float x, float y, float z, float pauseSeconds, String label) {
        public Waypoint(float x, float y, float z) {
            this(x, y, z, 0, "");
        }
        public Waypoint(float x, float y, float z, String label) {
            this(x, y, z, 0, label);
        }
    }
}
