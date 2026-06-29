package com.solra.auth.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for UserAccount persistence.
 */
@Entity
@Table(name = "user_accounts", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true),
    @Index(name = "idx_phone", columnList = "phone", unique = true),
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_status", columnList = "status")
})
public class UserAccountEntity {

    @Id
    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(length = 64, unique = true)
    private String username;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(length = 256)
    private String email;

    @Column(length = 20, unique = true)
    private String phone;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "password_hash", length = 256)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private String status;

    @Column(name = "real_name", length = 64)
    private String realName;

    @Column(name = "id_number_hash", length = 256)
    private String idNumberHash;

    @Column(name = "birth_date")
    private String birthDate; // ISO format: 2000-01-01

    @Column(name = "real_name_verified")
    private boolean realNameVerified;

    @Column(name = "real_name_verified_at")
    private Instant realNameVerifiedAt;

    @Column(name = "roles", length = 512)
    private String roles; // comma-separated

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    // -- Getters and Setters --

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public String getIdNumberHash() { return idNumberHash; }
    public void setIdNumberHash(String idNumberHash) { this.idNumberHash = idNumberHash; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public boolean isRealNameVerified() { return realNameVerified; }
    public void setRealNameVerified(boolean realNameVerified) { this.realNameVerified = realNameVerified; }

    public Instant getRealNameVerifiedAt() { return realNameVerifiedAt; }
    public void setRealNameVerifiedAt(Instant realNameVerifiedAt) { this.realNameVerifiedAt = realNameVerifiedAt; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
