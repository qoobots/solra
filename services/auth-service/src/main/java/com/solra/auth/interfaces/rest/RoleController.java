package com.solra.auth.interfaces.rest;

import com.solra.auth.application.service.AuthApplicationService;
import com.solra.auth.application.service.AuthApplicationService.PermissionCheckResult;
import com.solra.auth.application.service.AuthApplicationService.RoleOperationResult;
import com.solra.auth.domain.model.RoleDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST controller for AUTH-003 RBAC role & permission management.
 * <p>
 * Provides administrative endpoints for role assignment, removal,
 * permission checking, and role listing.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class RoleController {

    private static final Logger log = LoggerFactory.getLogger(RoleController.class);

    private final AuthApplicationService authAppService;

    public RoleController(AuthApplicationService authAppService) {
        this.authAppService = authAppService;
    }

    /**
     * AUTH-003: Check permission for a user.
     */
    @PostMapping("/permissions/check")
    public ResponseEntity<Map<String, Object>> checkPermission(
            @RequestBody CheckPermissionBody body) {
        PermissionCheckResult result = authAppService.checkPermission(
                body.userId(), body.resource(), body.action());
        return ResponseEntity.ok(Map.of(
                "allowed", result.allowed(),
                "reason", result.reason(),
                "roles", result.roles()
        ));
    }

    /**
     * AUTH-003: Get user's effective permissions.
     */
    @GetMapping("/users/{userId}/permissions")
    public ResponseEntity<Map<String, Object>> getUserPermissions(@PathVariable String userId) {
        Set<String> permissions = authAppService.getUserPermissions(userId);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "permissions", permissions
        ));
    }

    /**
     * AUTH-003: Get user's roles.
     */
    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<Map<String, Object>> getUserRoles(@PathVariable String userId) {
        List<String> roles = authAppService.getUserRoles(userId);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "roles", roles
        ));
    }

    /**
     * AUTH-003: Assign a role to a user (admin only).
     */
    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<Map<String, Object>> assignRole(
            @PathVariable String userId,
            @RequestBody AssignRoleBody body) {
        RoleOperationResult result = authAppService.assignRole(
                body.operatorUserId(), userId, body.role());
        log.info("AUTH-003: Role assigned: operator={} target={} role={}",
                body.operatorUserId(), userId, body.role());
        return ResponseEntity.ok(Map.of(
                "userId", result.userId(),
                "roles", result.roles(),
                "message", result.message()
        ));
    }

    /**
     * AUTH-003: Remove a role from a user (admin only).
     */
    @DeleteMapping("/users/{userId}/roles/{role}")
    public ResponseEntity<Map<String, Object>> removeRole(
            @PathVariable String userId,
            @PathVariable String role,
            @RequestParam String operatorUserId) {
        RoleOperationResult result = authAppService.removeRole(operatorUserId, userId, role);
        log.info("AUTH-003: Role removed: operator={} target={} role={}",
                operatorUserId, userId, role);
        return ResponseEntity.ok(Map.of(
                "userId", result.userId(),
                "roles", result.roles(),
                "message", result.message()
        ));
    }

    /**
     * AUTH-003: Get all predefined role definitions.
     */
    @GetMapping("/roles")
    public ResponseEntity<List<Map<String, Object>>> getAllRoles() {
        List<RoleDefinition> roles = authAppService.getAllRoles();
        List<Map<String, Object>> result = new ArrayList<>();
        for (RoleDefinition role : roles) {
            result.add(Map.of(
                    "roleId", role.getRoleId(),
                    "roleName", role.getRoleName(),
                    "description", role.getDescription(),
                    "priority", role.getPriority(),
                    "permissions", role.getPermissionKeys()
            ));
        }
        return ResponseEntity.ok(result);
    }

    // -- Request bodies --

    public record CheckPermissionBody(String userId, String resource, String action) {}

    public record AssignRoleBody(String operatorUserId, String role) {}
}
