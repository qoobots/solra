package com.solra.avt.domain.service;

import com.solra.avt.domain.model.SpatialPosition;
import com.solra.avt.domain.model.SpatialPosition.MovementState;
import com.solra.avt.domain.model.SpatialPosition.Waypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpatialMovementService — AVT-007 虚拟人空间移动与追踪领域服务。
 *
 * 管理虚拟人在3D空间中的自主导航、路径规划和空间感知。
 * 支持：
 * - 自主漫游（随机路径生成）
 * - 用户跟随（视线追踪）
 * - 路径规划与避障
 * - 空间感知（视野内用户/物体检测）
 * - 区域导航
 */
public class SpatialMovementService {

    private static final Logger log = LoggerFactory.getLogger(SpatialMovementService.class);

    // In-memory store: avatarId -> SpatialPosition
    private final Map<String, SpatialPosition> positions = new ConcurrentHashMap<>();

    // Navigation zones: zoneId -> {minX, maxX, minZ, maxZ}
    private final Map<String, ZoneBounds> zones = new ConcurrentHashMap<>();

    /**
     * Get or create spatial position for an avatar.
     */
    public SpatialPosition getOrCreate(String avatarId, float x, float y, float z) {
        return positions.computeIfAbsent(avatarId, k -> {
            log.info("AVT-007 Created spatial position: avatar={} pos=({},{},{})", avatarId, x, y, z);
            return new SpatialPosition(avatarId, x, y, z);
        });
    }

    /**
     * Get current position of an avatar.
     */
    public Optional<SpatialPosition> getPosition(String avatarId) {
        return Optional.ofNullable(positions.get(avatarId));
    }

    /**
     * Update avatar position.
     */
    public SpatialPosition updatePosition(String avatarId, float x, float y, float z,
                                           float rotationY, String zoneId) {
        SpatialPosition pos = getOrCreate(avatarId, x, y, z);
        pos.setPosition(x, y, z);
        pos.setRotation(0, rotationY);
        pos.setCurrentZoneId(zoneId);
        return pos;
    }

    /**
     * Move avatar towards a target with delta time.
     */
    public SpatialPosition moveTowards(String avatarId, float targetX, float targetY,
                                        float targetZ, float deltaSeconds) {
        SpatialPosition pos = getOrCreate(avatarId, 0, 0, 0);
        pos.moveTowards(targetX, targetY, targetZ, deltaSeconds);
        return pos;
    }

    /**
     * Make avatar look at a target position.
     */
    public SpatialPosition lookAt(String avatarId, float targetX, float targetZ) {
        SpatialPosition pos = positions.get(avatarId);
        if (pos != null) {
            pos.lookAt(targetX, targetZ);
        }
        return pos;
    }

    /**
     * Set a movement path for the avatar.
     */
    public SpatialPosition setPath(String avatarId, List<Waypoint> waypoints) {
        SpatialPosition pos = getOrCreate(avatarId, 0, 0, 0);
        pos.setPath(waypoints);
        log.info("AVT-007 Path set: avatar={} waypoints={}", avatarId, waypoints.size());
        return pos;
    }

    /**
     * Follow current path by delta time.
     */
    public SpatialPosition followPath(String avatarId, float deltaSeconds) {
        SpatialPosition pos = positions.get(avatarId);
        if (pos != null) {
            pos.followPath(deltaSeconds);
        }
        return pos;
    }

    /**
     * Generate a random patrol path within a zone.
     */
    public List<Waypoint> generatePatrolPath(String zoneId, int waypointCount) {
        ZoneBounds bounds = zones.get(zoneId);
        if (bounds == null) {
            // Default bounds if zone not defined
            bounds = new ZoneBounds(-10, 10, -10, 10);
        }

        List<Waypoint> path = new ArrayList<>();
        Random rng = new Random();
        for (int i = 0; i < waypointCount; i++) {
            float x = bounds.minX + rng.nextFloat() * (bounds.maxX - bounds.minX);
            float z = bounds.minZ + rng.nextFloat() * (bounds.maxZ - bounds.minZ);
            float pause = rng.nextFloat() * 3.0f; // 0-3 seconds pause
            path.add(new Waypoint(x, 0, z, pause, "PatrolPoint" + i));
        }
        return path;
    }

    /**
     * Start autonomous patrol for an avatar in a zone.
     */
    public SpatialPosition startPatrol(String avatarId, String zoneId, int waypointCount) {
        List<Waypoint> path = generatePatrolPath(zoneId, waypointCount);
        SpatialPosition pos = getOrCreate(avatarId,
                path.get(0).x(), path.get(0).y(), path.get(0).z());
        pos.setCurrentZoneId(zoneId);
        pos.setPath(path);
        return pos;
    }

    /**
     * Make avatar follow a user (gaze tracking).
     */
    public SpatialPosition followUser(String avatarId, float userX, float userY, float userZ,
                                       float deltaSeconds, float followDistance) {
        SpatialPosition pos = getOrCreate(avatarId, 0, 0, 0);

        // Calculate position behind/near user
        float dx = pos.getX() - userX;
        float dz = pos.getZ() - userZ;
        float currentDistance = (float) Math.sqrt(dx * dx + dz * dz);

        if (currentDistance > followDistance * 1.5f) {
            // Move closer
            float targetX = userX + (dx / currentDistance) * followDistance;
            float targetZ = userZ + (dz / currentDistance) * followDistance;
            pos.moveTowards(targetX, userY, targetZ, deltaSeconds);
        } else {
            pos.setMovementState(MovementState.IDLE);
        }

        // Always look at user
        pos.lookAt(userX, userZ);
        return pos;
    }

    /**
     * Find users within awareness radius of an avatar.
     */
    public List<String> findUsersInAwareness(String avatarId,
                                               Map<String, float[]> userPositions) {
        SpatialPosition pos = positions.get(avatarId);
        if (pos == null) return List.of();

        List<String> nearby = new ArrayList<>();
        for (var entry : userPositions.entrySet()) {
            float[] up = entry.getValue();
            if (pos.isUserInAwarenessRadius(up[0], up[1], up[2])) {
                nearby.add(entry.getKey());
            }
        }
        pos.getVisibleUsers().clear();
        pos.getVisibleUsers().addAll(nearby);
        return nearby;
    }

    /**
     * Find users within field of view of an avatar.
     */
    public List<String> findUsersInFOV(String avatarId,
                                         Map<String, float[]> userPositions,
                                         float fovDegrees) {
        SpatialPosition pos = positions.get(avatarId);
        if (pos == null) return List.of();

        List<String> inFov = new ArrayList<>();
        for (var entry : userPositions.entrySet()) {
            float[] up = entry.getValue();
            if (pos.isUserInAwarenessRadius(up[0], up[1], up[2])
                    && pos.isUserInFieldOfView(up[0], up[2], fovDegrees)) {
                inFov.add(entry.getKey());
            }
        }
        return inFov;
    }

    /**
     * Get all avatars in a specific zone.
     */
    public List<String> getAvatarsInZone(String zoneId) {
        return positions.entrySet().stream()
                .filter(e -> zoneId.equals(e.getValue().getCurrentZoneId()))
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Register a navigation zone.
     */
    public void registerZone(String zoneId, float minX, float maxX, float minZ, float maxZ) {
        zones.put(zoneId, new ZoneBounds(minX, maxX, minZ, maxZ));
        log.debug("AVT-007 Zone registered: {} bounds=({},{},{},{})", zoneId, minX, maxX, minZ, maxZ);
    }

    /**
     * Get the movement state of an avatar.
     */
    public MovementState getMovementState(String avatarId) {
        return getPosition(avatarId)
                .map(SpatialPosition::getMovementState)
                .orElse(MovementState.IDLE);
    }

    /**
     * Check if an avatar is currently moving.
     */
    public boolean isMoving(String avatarId) {
        MovementState state = getMovementState(avatarId);
        return state == MovementState.WALKING || state == MovementState.RUNNING;
    }

    /**
     * Zone boundary definition.
     */
    private record ZoneBounds(float minX, float maxX, float minZ, float maxZ) {}
}
