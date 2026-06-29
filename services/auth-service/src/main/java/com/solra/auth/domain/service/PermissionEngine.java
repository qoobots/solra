package com.solra.auth.domain.service;

import com.solra.auth.domain.model.Permission;
import com.solra.auth.domain.model.RoleDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PermissionEngine — stateless domain service that evaluates RBAC permissions.
 * <p>
 * Core responsibility: given a user's roles and a permission request,
 * determine whether access should be allowed or denied.
 * <p>
 * Evaluation order:
 * <ol>
 *   <li>SUPER_ADMIN bypass — granted all permissions</li>
 *   <li>Explicit DENY — if any role has an explicit deny policy (future)</li>
 *   <li>Role-based check — iterate roles by priority, first match wins</li>
 * </ol>
 * Covers: AUTH-003 (RBAC permission management).
 */
@Service
public class PermissionEngine {

    private static final Logger log = LoggerFactory.getLogger(PermissionEngine.class);

    /**
     * Evaluate whether a set of roles grants the requested permission.
     *
     * @param roles    the user's current role names (e.g., "ROLE_USER", "ROLE_CREATOR")
     * @param resource the resource identifier (e.g., "space", "avatar", "admin")
     * @param action   the action (e.g., "read", "write", "delete", "publish")
     * @return evaluation result with allowed flag and reason
     */
    public PermissionResult evaluate(Set<String> roles, String resource, String action) {
        if (roles == null || roles.isEmpty()) {
            return PermissionResult.denied("No roles assigned");
        }

        // 1. SUPER_ADMIN has full access
        if (roles.contains(RoleDefinition.SUPER_ADMIN.getRoleName())) {
            return PermissionResult.allowed("Super admin full access");
        }

        // 2. ADMIN bypass for admin-scoped resources
        if (roles.contains(RoleDefinition.ADMIN.getRoleName())) {
            RoleDefinition adminRole = RoleDefinition.PREDEFINED_ROLES.get(RoleDefinition.ADMIN.getRoleName());
            if (adminRole != null && adminRole.grantsPermission(resource, action)) {
                return PermissionResult.allowed("Admin role grants permission");
            }
        }

        // 3. Sort roles by priority (highest first) and check each
        List<RoleDefinition> sortedRoles = roles.stream()
                .map(RoleDefinition.PREDEFINED_ROLES::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(RoleDefinition::getPriority).reversed())
                .toList();

        for (RoleDefinition roleDef : sortedRoles) {
            if (roleDef.grantsPermission(resource, action)) {
                return PermissionResult.allowed("Granted by role: " + roleDef.getRoleName());
            }
        }

        // 4. No role grants this permission
        String roleList = String.join(", ", roles);
        return PermissionResult.denied(
                String.format("Permission denied: %s:%s not granted by roles [%s]", resource, action, roleList));
    }

    /**
     * Get all effective permissions for a set of roles.
     */
    public Set<String> getEffectivePermissions(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }

        // SUPER_ADMIN gets all
        if (roles.contains(RoleDefinition.SUPER_ADMIN.getRoleName())) {
            return Set.of("*:*");
        }

        Set<String> effective = new LinkedHashSet<>();
        for (String roleName : roles) {
            RoleDefinition roleDef = RoleDefinition.PREDEFINED_ROLES.get(roleName);
            if (roleDef != null) {
                effective.addAll(roleDef.getPermissionKeys());
            }
        }
        return Collections.unmodifiableSet(effective);
    }

    /**
     * Check if a user has a specific role.
     */
    public boolean hasRole(Set<String> roles, String roleName) {
        return roles != null && roles.contains(roleName);
    }

    /**
     * Get all predefined role definitions.
     */
    public Collection<RoleDefinition> getAllPredefinedRoles() {
        return RoleDefinition.PREDEFINED_ROLES.values();
    }

    /**
     * Get a specific role definition by name.
     */
    public Optional<RoleDefinition> getRoleDefinition(String roleName) {
        return Optional.ofNullable(RoleDefinition.PREDEFINED_ROLES.get(roleName));
    }

    /**
     * Assign a role to a user (business rule validation).
     *
     * @return true if the assignment is valid, false with reason otherwise
     */
    public RoleAssignmentResult validateRoleAssignment(Set<String> currentRoles, String roleToAssign) {
        // Cannot assign GUEST to registered users
        if (RoleDefinition.GUEST.getRoleName().equals(roleToAssign)) {
            return RoleAssignmentResult.denied("GUEST role cannot be explicitly assigned");
        }

        // SUPER_ADMIN can only be assigned by another SUPER_ADMIN (checked at application layer)
        if (RoleDefinition.SUPER_ADMIN.getRoleName().equals(roleToAssign)) {
            return RoleAssignmentResult.denied("SUPER_ADMIN can only be assigned by another SUPER_ADMIN");
        }

        // Already has the role
        if (currentRoles.contains(roleToAssign)) {
            return RoleAssignmentResult.denied("User already has role: " + roleToAssign);
        }

        // Verify role exists in predefined roles
        if (!RoleDefinition.PREDEFINED_ROLES.containsKey(roleToAssign)) {
            return RoleAssignmentResult.denied("Unknown role: " + roleToAssign);
        }

        return RoleAssignmentResult.allowed();
    }

    /**
     * Validate role removal.
     */
    public RoleAssignmentResult validateRoleRemoval(Set<String> currentRoles, String roleToRemove) {
        // Cannot remove USER role (minimum)
        if (RoleDefinition.USER.getRoleName().equals(roleToRemove) && currentRoles.size() <= 1) {
            return RoleAssignmentResult.denied("Cannot remove the last role. USER role is the minimum.");
        }

        if (!currentRoles.contains(roleToRemove)) {
            return RoleAssignmentResult.denied("User does not have role: " + roleToRemove);
        }

        return RoleAssignmentResult.allowed();
    }

    /**
     * Permission evaluation result.
     */
    public record PermissionResult(boolean allowed, String reason) {
        public static PermissionResult allowed(String reason) {
            return new PermissionResult(true, reason);
        }

        public static PermissionResult denied(String reason) {
            return new PermissionResult(false, reason);
        }
    }

    /**
     * Role assignment validation result.
     */
    public record RoleAssignmentResult(boolean allowed, String reason) {
        public static RoleAssignmentResult allowed() {
            return new RoleAssignmentResult(true, "OK");
        }

        public static RoleAssignmentResult denied(String reason) {
            return new RoleAssignmentResult(false, reason);
        }
    }
}
