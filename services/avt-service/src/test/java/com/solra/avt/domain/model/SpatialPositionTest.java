package com.solra.avt.domain.model;

import com.solra.avt.domain.model.SpatialPosition.MovementState;
import com.solra.avt.domain.model.SpatialPosition.Waypoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SpatialPosition — AVT-007 空间移动与追踪领域模型。
 */
@DisplayName("AVT-007: SpatialPosition Domain Model")
class SpatialPositionTest {

    private SpatialPosition position;

    @BeforeEach
    void setUp() {
        position = new SpatialPosition("avatar-001", 0, 0, 0);
    }

    @Test
    @DisplayName("Should initialize at origin with IDLE state")
    void shouldInitializeAtOrigin() {
        assertEquals("avatar-001", position.getAvatarId());
        assertEquals(0f, position.getX());
        assertEquals(0f, position.getY());
        assertEquals(0f, position.getZ());
        assertEquals(MovementState.IDLE, position.getMovementState());
    }

    @Test
    @DisplayName("Should move towards target position")
    void shouldMoveTowardsTarget() {
        position.moveTowards(10, 0, 0, 1.0f);

        // After 1 second at 1.5 m/s, should have moved 1.5 units towards target
        assertTrue(position.getX() > 0);
        assertTrue(position.getX() < 10);
        assertEquals(MovementState.WALKING, position.getMovementState());
    }

    @Test
    @DisplayName("Should reach target and become IDLE")
    void shouldReachTarget() {
        // Move close to target
        position.moveTowards(1, 0, 0, 1.0f);

        assertEquals(1f, position.getX(), 0.01f);
        assertEquals(MovementState.IDLE, position.getMovementState());
    }

    @Test
    @DisplayName("Should face direction of movement")
    void shouldFaceMovementDirection() {
        position.moveTowards(0, 0, 10, 1.0f);

        // Moving along +Z axis, should face 0 degrees
        assertEquals(0f, position.getRotationY(), 1.0f);
    }

    @Test
    @DisplayName("Should look at specific target")
    void shouldLookAtTarget() {
        position.lookAt(10, 10);

        // Should face approximately 45 degrees (atan2(10,10))
        assertEquals(45f, position.getRotationY(), 1.0f);
    }

    @Test
    @DisplayName("Should follow a path of waypoints")
    void shouldFollowPath() {
        List<Waypoint> path = List.of(
                new Waypoint(5, 0, 0, "Point1"),
                new Waypoint(5, 0, 5, "Point2"),
                new Waypoint(0, 0, 5, "Point3")
        );

        position.setPath(path);
        assertEquals(MovementState.WALKING, position.getMovementState());
        assertEquals(3, position.getPath().size());

        // Follow for several seconds
        for (int i = 0; i < 20; i++) {
            position.followPath(0.5f);
        }

        // Should have reached end of path
        assertEquals(MovementState.IDLE, position.getMovementState());
    }

    @Test
    @DisplayName("Should detect users within awareness radius")
    void shouldDetectUsersInAwareness() {
        // User within 10m radius
        assertTrue(position.isUserInAwarenessRadius(5, 0, 5));
        // User outside 10m radius
        assertFalse(position.isUserInAwarenessRadius(10, 0, 10));
    }

    @Test
    @DisplayName("Should detect users within field of view")
    void shouldDetectUsersInFOV() {
        // Avatar facing +Z (0 degrees)
        position.setRotation(0, 0);

        // User directly ahead
        assertTrue(position.isUserInFieldOfView(0, 10, 90));

        // User behind
        assertFalse(position.isUserInFieldOfView(0, -10, 90));
    }

    @Test
    @DisplayName("Should calculate distance correctly")
    void shouldCalculateDistance() {
        float dist = position.distanceTo(3, 4, 0);
        assertEquals(5f, dist, 0.01f);
    }

    @Test
    @DisplayName("Should clear path and become IDLE")
    void shouldClearPath() {
        position.setPath(List.of(new Waypoint(5, 0, 5)));
        position.clearPath();

        assertEquals(MovementState.IDLE, position.getMovementState());
        assertTrue(position.getPath().isEmpty());
    }

    @Test
    @DisplayName("Should pause at waypoints with pause time")
    void shouldPauseAtWaypoints() {
        List<Waypoint> path = List.of(
                new Waypoint(1, 0, 0, 10.0f, "PausePoint") // 10 second pause
        );

        position.setPath(path);

        // Move to reach waypoint
        for (int i = 0; i < 5; i++) {
            position.followPath(0.5f);
        }

        // Should be PAUSED at the waypoint
        assertEquals(MovementState.PAUSED, position.getMovementState());
    }
}
