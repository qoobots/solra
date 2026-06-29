package com.solra.auth.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * UserAccount aggregate root — the central identity entity.
 * <p>
 * Covers: AUTH-001 (phone registration/login), AUTH-004 (real-name verification).
 */
public class UserAccount {

    private String userId;
    private String username;
    private String displayName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String passwordHash;
    private AccountStatus status;
    private List<OAuthAccount> linkedAccounts = new ArrayList<>();
    private RealNameInfo realNameInfo;
    private Set<String> roles = new HashSet<>();
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
    private Map<String, String> metadata = new HashMap<>();

    // -- Factory: phone registration (AUTH-001) --
    public static UserAccount registerByPhone(String phone, String passwordHash, String displayName) {
        UserAccount account = new UserAccount();
        account.userId = com.solra.common.security.SecurityUtils.generateUserId();
        account.phone = phone;
        account.passwordHash = passwordHash;
        account.displayName = displayName != null ? displayName : "用户" + phone.substring(phone.length() - 4);
        account.username = "user_" + account.userId.substring(4, 12);
        account.status = AccountStatus.ACTIVE;
        account.roles.add("ROLE_USER");
        Instant now = Instant.now();
        account.createdAt = now;
        account.updatedAt = now;
        account.lastLoginAt = now;
        return account;
    }

    // -- Factory: username + password registration --
    public static UserAccount registerByUsername(String username, String passwordHash, String displayName) {
        UserAccount account = new UserAccount();
        account.userId = com.solra.common.security.SecurityUtils.generateUserId();
        account.username = username;
        account.passwordHash = passwordHash;
        account.displayName = displayName != null ? displayName : username;
        account.status = AccountStatus.ACTIVE;
        account.roles.add("ROLE_USER");
        Instant now = Instant.now();
        account.createdAt = now;
        account.updatedAt = now;
        account.lastLoginAt = now;
        return account;
    }

    // -- Business methods --

    public void recordLogin(String ipAddress, String deviceInfo) {
        this.lastLoginAt = Instant.now();
        this.updatedAt = Instant.now();
        this.metadata.put("lastLoginIp", ipAddress);
        this.metadata.put("lastLoginDevice", deviceInfo);
    }

    public void updateProfile(String displayName, String email, String avatarUrl) {
        if (displayName != null) this.displayName = displayName;
        if (email != null) this.email = email;
        if (avatarUrl != null) this.avatarUrl = avatarUrl;
        this.updatedAt = Instant.now();
    }

    /**
     * AUTH-004: Submit real-name verification info.
     */
    public void submitRealNameVerification(RealNameInfo info) {
        if (info == null || !info.isValid()) {
            throw new IllegalArgumentException("Invalid real-name information");
        }
        this.realNameInfo = info;
        this.metadata.put("realNameStatus", "PENDING");
        this.updatedAt = Instant.now();
    }

    /**
     * AUTH-004: Mark real-name verification as approved.
     */
    public void approveRealNameVerification() {
        if (this.realNameInfo == null) {
            throw new IllegalStateException("No real-name info submitted");
        }
        this.realNameInfo = this.realNameInfo.withVerified(true);
        this.metadata.put("realNameStatus", "VERIFIED");
        this.roles.add("ROLE_VERIFIED");
        this.updatedAt = Instant.now();
    }

    /**
     * AUTH-004: Check if this is a minor account requiring protection.
     */
    public boolean isMinor() {
        return realNameInfo != null && realNameInfo.isMinor();
    }

    public void bindOAuth(OAuthAccount oauthAccount) {
        this.linkedAccounts.removeIf(a -> a.getProvider() == oauthAccount.getProvider());
        this.linkedAccounts.add(oauthAccount);
        this.updatedAt = Instant.now();
    }

    public void unbindOAuth(String provider) {
        this.linkedAccounts.removeIf(a -> a.getProvider().name().equalsIgnoreCase(provider));
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        this.status = AccountStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    public void delete() {
        this.status = AccountStatus.DELETED;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    public boolean hasRole(String role) {
        return this.roles.contains(role);
    }

    /**
     * AUTH-003: Assign a role to the user.
     */
    public void assignRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be empty");
        }
        this.roles.add(role);
        this.updatedAt = Instant.now();
    }

    /**
     * AUTH-003: Remove a role from the user.
     */
    public void removeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be empty");
        }
        this.roles.remove(role);
        this.updatedAt = Instant.now();
    }

    /**
     * AUTH-003: Check if user has any of the given roles.
     */
    public boolean hasAnyRole(Set<String> checkRoles) {
        return checkRoles != null && checkRoles.stream().anyMatch(this.roles::contains);
    }

    // -- Getters --

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getPasswordHash() { return passwordHash; }
    public AccountStatus getStatus() { return status; }
    public List<OAuthAccount> getLinkedAccounts() { return Collections.unmodifiableList(linkedAccounts); }
    public RealNameInfo getRealNameInfo() { return realNameInfo; }
    public Set<String> getRoles() { return Collections.unmodifiableSet(roles); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Map<String, String> getMetadata() { return Collections.unmodifiableMap(metadata); }
}
