package com.solra.avt.domain.service;

import com.solra.avt.domain.model.SpatialPosition;
import com.solra.avt.domain.model.SpatialPosition.MovementState;
import com.solra.avt.domain.model.SpatialPosition.Waypoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SpatialMovementService — AVT-007 空间移动与追踪领域服务。
 */
@DisplayName("AVT-007: SpatialMovementService")
class SpatialMovementServiceTest {

    private SpatialMovementService service;

    @BeforeEach
    void setUp() {
        service = new SpatialMovementService();
    }

    @Test
    @DisplayName("Should create spatial position for new avatar")
    void shouldCreateNewPosition() {
        SpatialPosition pos = service.getOrCreate("avatar-001", 10, 0, 20);

        assertNotNull(pos);
        assertEquals("avatar-001", pos.getAvatarId());
        assertEquals(10f, pos.getX());
        assertEquals(0f, pos.getY());
        assertEquals(20f, pos.getZ());
    }

    @Test
    @DisplayName("Should update avatar position")
    void shouldUpdatePosition() {
        SpatialPosition pos = service.updatePosition("avatar-001", 5, 0, 5, 90, "zone-1");

        assertEquals(5f, pos.getX());
        assertEquals(5f, pos.getZ());
        assertEquals(90f, pos.getRotationY(), 0.1f);
        assertEquals("zone-1", pos.getCurrentZoneId());
    }

    @Test
    @DisplayName("Should move avatar towards target")
    void shouldMoveTowards() {
        service.getOrCreate("avatar-001", 0, 0, 0);
        SpatialPosition pos = service.moveTowards("avatar-001", 10, 0, 0, 1.0f);

        assertTrue(pos.getX() > 0);
        assertTrue(pos.getX() < 10);
        assertEquals(MovementState.WALKING, pos.getMovementState());
    }

    @Test
    @DisplayName("Should make avatar look at target")
    void shouldLookAt() {
        service.getOrCreate("avatar-001", 0, 0, 0);
        service.lookAt("avatar-001", 0, 10);

        Optional<SpatialPosition> pos = service.getPosition("avatar-001");
        assertTrue(pos.isPresent());
        assertEquals(0f, pos.get().getRotationY(), 1.0f); // facing +Z
    }

    @Test
    @DisplayName("Should set and follow a movement path")
    void shouldFollowPath() {
        List<Waypoint> path = List.of(
                new Waypoint(5, 0, 0),
                new Waypoint(5, 0, 5),
                new Waypoint(0, 0, 5)
        );

        service.setPath("avatar-001", path);

        // Follow for several seconds
        for (int i = 0; i < 30; i++) {
            service.followPath("avatar-001", 0.5f);
        }

        SpatialPosition pos = service.getPosition("avatar-001").orElseThrow();
        assertEquals(MovementState.IDLE, pos.getMovementState());
    }

    @Test
    @DisplayName("Should generate patrol path within zone")
    void shouldGeneratePatrolPath() {
        service.registerZone("zone-1", 0, 100, 0, 100);
        List<Waypoint> path = service.generatePatrolPath("zone-1", 5);

        assertEquals(5, path.size());
        for (Waypoint wp : path) {
            assertTrue(wp.x() >= 0 && wp.x() <= 100);
            assertTrue(wp.z() >= 0 && wp.z() <= 100);
        }
    }

    @Test
    @DisplayName("Should start autonomous patrol")
    void shouldStartPatrol() {
        service.registerZone("zone-1", 0, 100, 0, 100);
        SpatialPosition pos = service.startPatrol("avatar-001", "zone-1", 5);

        assertEquals("zone-1", pos.getCurrentZoneId());
        assertEquals(MovementState.WALKING, pos.getMovementState());
        assertFalse(pos.getPath().isEmpty());
    }

    @Test
    @DisplayName("Should find users within awareness radius")
    void shouldFindUsersInAwareness() {
        service.getOrCreate("avatar-001", 0, 0, 0);

        Map<String, float[]> userPositions = Map.of(
                "user-001", new float[]{5, 0, 5},    // within radius
                "user-002", new float[]{20, 0, 20}    // outside radius
        );

        List<String> nearby = service.findUsersInAwareness("avatar-001", userPositions);

        assertEquals(1, nearby.size());
        assertEquals("user-001", nearby.get(0));
    }

    @Test
    @DisplayName("Should find users within field of view")
    void shouldFindUsersInFOV() {
        SpatialPosition pos = service.getOrCreate("avatar-001", 0, 0, 0);
        pos.setRotation(0, 0); // facing +Z

        Map<String, float[]> userPositions = Map.of(
                "user-001", new float[]{0, 0, 5},     // directly ahead
                "user-002", new float[]{0, 0, -5}     // behind
        );

        List<String> inFov = service.findUsersInFOV("avatar-001", userPositions, 90);

        assertEquals(1, inFov.size());
        assertEquals("user-001", inFov.get(0));
    }

    @Test
    @DisplayName("Should get avatars in a specific zone")
    void shouldGetAvatarsInZone() {
        service.updatePosition("avatar-001", 0, 0, 0, 0, "zone-1");
        service.updatePosition("avatar-002", 10, 0, 10, 0, "zone-1");
        service.updatePosition("avatar-003", 0, 0, 0, 0, "zone-2");

        List<String> inZone1 = service.getAvatarsInZone("zone-1");
        assertEquals(2, inZone1.size());
        assertTrue(inZone1.contains("avatar-001"));
        assertTrue(inZone1.contains("avatar-002"));
    }

    @Test
    @DisplayName("Should report movement state correctly")
    void shouldReportMovementState() {
        assertEquals(MovementState.IDLE, service.getMovementState("avatar-001"));

        service.moveTowards("avatar-001", 10, 0, 0, 1.0f);
        assertEquals(MovementState.WALKING, service.getMovementState("avatar-001"));
    }

    @Test
    @DisplayName("Should check if avatar is moving")
    void shouldCheckIfMoving() {
        assertFalse(service.isMoving("avatar-001"));

        service.moveTowards("avatar-001", 10, 0, 0, 1.0f);
        assertTrue(service.isMoving("avatar-001"));
    }
}
