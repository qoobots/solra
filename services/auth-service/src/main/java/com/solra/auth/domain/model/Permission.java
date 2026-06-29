package com.solra.auth.domain.model;

import java.util.Objects;

/**
 * Permission value object — represents a single permission in the RBAC system.
 * <p>
 * A permission is a combination of a resource (what) and an action (what you can do).
 * Covers: AUTH-003 (RBAC permission management).
 */
public final class Permission {

    private final String permissionId;
    private final String resource;
    private final String action;
    private final String description;

    private Permission(String permissionId, String resource, String action, String description) {
        this.permissionId = permissionId;
        this.resource = resource;
        this.action = action;
        this.description = description;
    }

    /**
     * Create a new permission with auto-generated ID.
     */
    public static Permission of(String resource, String action, String description) {
        String id = "perm_" + resource.toLowerCase().replace(':', '_') + "_" + action.toLowerCase();
        return new Permission(id, resource, action, description);
    }

    /**
     * Create a permission with explicit ID (for predefined permissions).
     */
    public static Permission withId(String permissionId, String resource, String action, String description) {
        return new Permission(permissionId, resource, action, description);
    }

    /**
     * Check if this permission matches the given resource and action.
     * Supports wildcard matching: "*" matches any resource or action.
     */
    public boolean matches(String resource, String action) {
        return matchesResource(resource) && matchesAction(action);
    }

    private boolean matchesResource(String requestedResource) {
        return "*".equals(this.resource) || this.resource.equalsIgnoreCase(requestedResource);
    }

    private boolean matchesAction(String requestedAction) {
        return "*".equals(this.action) || this.action.equalsIgnoreCase(requestedAction);
    }

    // -- Getters --

    public String getPermissionId() { return permissionId; }
    public String getResource() { return resource; }
    public String getAction() { return action; }
    public String getDescription() { return description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission that)) return false;
        return permissionId.equals(that.permissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissionId);
    }

    @Override
    public String toString() {
        return "Permission{id=" + permissionId + ", resource=" + resource + ", action=" + action + "}";
    }

    // ================================================================
    // Predefined permissions (索拉平台标准权限)
    // ================================================================

    // Space (空间) permissions
    public static final Permission SPACE_VIEW = Permission.withId("perm_space_view", "space", "read", "查看空间");
    public static final Permission SPACE_CREATE = Permission.withId("perm_space_create", "space", "write", "创建空间");
    public static final Permission SPACE_EDIT = Permission.withId("perm_space_edit", "space", "write", "编辑空间");
    public static final Permission SPACE_DELETE = Permission.withId("perm_space_delete", "space", "delete", "删除空间");
    public static final Permission SPACE_PUBLISH = Permission.withId("perm_space_publish", "space", "publish", "发布空间");

    // Avatar (虚拟人) permissions
    public static final Permission AVATAR_INTERACT = Permission.withId("perm_avatar_interact", "avatar", "read", "与虚拟人互动");
    public static final Permission AVATAR_CREATE = Permission.withId("perm_avatar_create", "avatar", "write", "创建虚拟人");
    public static final Permission AVATAR_TRAIN = Permission.withId("perm_avatar_train", "avatar", "train", "训练虚拟人");

    // Social (社交) permissions
    public static final Permission SOCIAL_CHAT = Permission.withId("perm_social_chat", "social", "read", "发送聊天消息");
    public static final Permission SOCIAL_FRIEND = Permission.withId("perm_social_friend", "social", "write", "添加好友");
    public static final Permission SOCIAL_SHARE = Permission.withId("perm_social_share", "social", "share", "分享空间");

    // Content (内容) permissions
    public static final Permission CONTENT_UPLOAD = Permission.withId("perm_content_upload", "content", "write", "上传内容");
    public static final Permission CONTENT_REVIEW = Permission.withId("perm_content_review", "content", "review", "审核内容");
    public static final Permission CONTENT_REPORT = Permission.withId("perm_content_report", "content", "report", "举报内容");

    // Admin (管理) permissions
    public static final Permission ADMIN_USER_MANAGE = Permission.withId("perm_admin_user_manage", "admin", "manage", "管理用户");
    public static final Permission ADMIN_ROLE_MANAGE = Permission.withId("perm_admin_role_manage", "admin", "role", "管理角色");
    public static final Permission ADMIN_SYSTEM_CONFIG = Permission.withId("perm_admin_system_config", "admin", "config", "系统配置");
    public static final Permission ADMIN_FULL = Permission.withId("perm_admin_full", "*", "*", "超级管理员");
}
