package com.solra.auth.interfaces.rest;

import com.solra.auth.application.service.AuthApplicationService;
import com.solra.auth.application.service.AuthApplicationService.*;
import com.solra.common.exception.SolraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AUTH-005: REST controller for device binding & multi-device session management.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class DeviceController {

    private static final Logger log = LoggerFactory.getLogger(DeviceController.class);

    private final AuthApplicationService authAppService;

    public DeviceController(AuthApplicationService authAppService) {
        this.authAppService = authAppService;
    }

    /**
     * AUTH-005: Get all registered devices and active sessions for a user.
     * GET /api/v1/auth/users/{userId}/devices
     */
    @GetMapping("/users/{userId}/devices")
    public ResponseEntity<DeviceBindingResult> getUserDevices(@PathVariable String userId) {
        try {
            DeviceBindingResult result = authAppService.getUserDevices(userId);
            return ResponseEntity.ok(result);
        } catch (SolraException.NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * AUTH-005: Get active login sessions for a user.
     * GET /api/v1/auth/users/{userId}/sessions
     */
    @GetMapping("/users/{userId}/sessions")
    public ResponseEntity<List<SessionInfo>> getUserSessions(@PathVariable String userId) {
        try {
            List<SessionInfo> sessions = authAppService.getUserSessions(userId);
            return ResponseEntity.ok(sessions);
        } catch (SolraException.NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * AUTH-005: Remove a device from user's binding (kick device).
     * DELETE /api/v1/auth/users/{userId}/devices/{deviceId}
     */
    @DeleteMapping("/users/{userId}/devices/{deviceId}")
    public ResponseEntity<DeviceBindingResult> removeDevice(
            @PathVariable String userId,
            @PathVariable String deviceId,
            @RequestHeader(value = "X-Operator-UserId", defaultValue = "") String operatorUserId) {

        String operator = operatorUserId.isEmpty() ? userId : operatorUserId;
        try {
            DeviceBindingResult result = authAppService.removeDevice(operator, userId, deviceId);
            log.info("AUTH-005: Device {} removed for user {} by {}", deviceId, userId, operator);
            return ResponseEntity.ok(result);
        } catch (SolraException.PermissionDeniedException e) {
            return ResponseEntity.status(403).build();
        } catch (SolraException.NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * AUTH-005: Logout from a specific device.
     * POST /api/v1/auth/users/{userId}/devices/{deviceId}/logout
     */
    @PostMapping("/users/{userId}/devices/{deviceId}/logout")
    public ResponseEntity<Map<String, String>> logoutDevice(
            @PathVariable String userId,
            @PathVariable String deviceId) {
        try {
            authAppService.logoutDevice(userId, deviceId);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "message", "Device " + deviceId + " logged out successfully"
            ));
        } catch (SolraException.NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * AUTH-005: Global logout from all devices.
     * POST /api/v1/auth/users/{userId}/logout-all
     */
    @PostMapping("/users/{userId}/logout-all")
    public ResponseEntity<Map<String, String>> logoutAllDevices(@PathVariable String userId) {
        authAppService.logout(userId);
        log.info("AUTH-005: Global logout for user {}", userId);
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "All devices logged out successfully"
        ));
    }
}
