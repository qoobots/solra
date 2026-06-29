package com.solra.auth.domain.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RoleDefinition value object — defines a role with its permissions.
 * <p>
 * Five standard roles for 索拉平台:
 * <ul>
 *   <li>ROLE_GUEST — 游客 (未登录)</li>
 *   <li>ROLE_USER — 普通用户 (默认)</li>
 *   <li>ROLE_CREATOR — 创作者 (可发布空间)</li>
 *   <li>ROLE_ADMIN — 管理员</li>
 *   <li>ROLE_SUPER_ADMIN — 超级管理员</li>
 * </ul>
 * Covers: AUTH-003 (RBAC permission management).
 */
public final class RoleDefinition {

    private final String roleId;
    private final String roleName;
    private final String description;
    private final Set<Permission> permissions;
    private final int priority;

    private RoleDefinition(String roleId, String roleName, String description,
                           Set<Permission> permissions, int priority) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.description = description;
        this.permissions = Collections.unmodifiableSet(new HashSet<>(permissions));
        this.priority = priority;
    }

    public static RoleDefinition of(String roleId, String roleName, String description,
                                     Set<Permission> permissions, int priority) {
        return new RoleDefinition(roleId, roleName, description, permissions, priority);
    }

    /**
     * Check if this role grants a specific permission.
     */
    public boolean grantsPermission(String resource, String action) {
        return permissions.stream().anyMatch(p -> p.matches(resource, action));
    }

    /**
     * Check if this role grants any of the given permissions.
     */
    public boolean grantsAnyPermission(Set<Permission> required) {
        return required.stream().anyMatch(req ->
                permissions.stream().anyMatch(p -> p.matches(req.getResource(), req.getAction())));
    }

    /**
     * Get all resource:action pairs this role can access.
     */
    public Set<String> getPermissionKeys() {
        return permissions.stream()
                .map(p -> p.getResource() + ":" + p.getAction())
                .collect(Collectors.toSet());
    }

    // -- Getters --

    public String getRoleId() { return roleId; }
    public String getRoleName() { return roleName; }
    public String getDescription() { return description; }
    public Set<Permission> getPermissions() { return permissions; }
    public int getPriority() { return priority; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleDefinition that)) return false;
        return roleId.equals(that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId);
    }

    @Override
    public String toString() {
        return "RoleDefinition{id=" + roleId + ", name=" + roleName + "}";
    }

    // ================================================================
    // Predefined standard roles
    // ================================================================

    /** 游客 — can only view public spaces */
    public static final RoleDefinition GUEST = RoleDefinition.of(
            "role_guest", "ROLE_GUEST", "游客",
            Set.of(Permission.SPACE_VIEW),
            0
    );

    /** 普通用户 — default role for all registered users */
    public static final RoleDefinition USER = RoleDefinition.of(
            "role_user", "ROLE_USER", "普通用户",
            Set.of(
                    Permission.SPACE_VIEW,
                    Permission.AVATAR_INTERACT,
                    Permission.SOCIAL_CHAT,
                    Permission.SOCIAL_SHARE,
                    Permission.CONTENT_REPORT
            ),
            10
    );

    /** 创作者 — can create and publish spaces */
    public static final RoleDefinition CREATOR = RoleDefinition.of(
            "role_creator", "ROLE_CREATOR", "创作者",
            Set.of(
                    Permission.SPACE_VIEW, Permission.SPACE_CREATE,
                    Permission.SPACE_EDIT, Permission.SPACE_PUBLISH,
                    Permission.AVATAR_INTERACT, Permission.AVATAR_CREATE,
                    Permission.SOCIAL_CHAT, Permission.SOCIAL_FRIEND,
                    Permission.SOCIAL_SHARE, Permission.CONTENT_UPLOAD,
                    Permission.CONTENT_REPORT
            ),
            20
    );

    /** 管理员 — can manage users and content */
    public static final RoleDefinition ADMIN = RoleDefinition.of(
            "role_admin", "ROLE_ADMIN", "管理员",
            Set.of(
                    Permission.SPACE_VIEW, Permission.SPACE_DELETE,
                    Permission.AVATAR_INTERACT,
                    Permission.SOCIAL_CHAT, Permission.SOCIAL_FRIEND,
                    Permission.CONTENT_UPLOAD, Permission.CONTENT_REVIEW,
                    Permission.ADMIN_USER_MANAGE
            ),
            50
    );

    /** 超级管理员 — full access */
    public static final RoleDefinition SUPER_ADMIN = RoleDefinition.of(
            "role_super_admin", "ROLE_SUPER_ADMIN", "超级管理员",
            Set.of(Permission.ADMIN_FULL),
            100
    );

    /** All predefined roles registry */
    public static final Map<String, RoleDefinition> PREDEFINED_ROLES = Map.of(
            GUEST.getRoleName(), GUEST,
            USER.getRoleName(), USER,
            CREATOR.getRoleName(), CREATOR,
            ADMIN.getRoleName(), ADMIN,
            SUPER_ADMIN.getRoleName(), SUPER_ADMIN
    );
}
