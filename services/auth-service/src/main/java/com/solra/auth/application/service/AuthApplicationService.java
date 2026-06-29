package com.solra.auth.application.service;

import com.solra.auth.application.dto.*;
import com.solra.auth.domain.event.AuthDomainEvents.*;
import com.solra.auth.domain.model.*;
import com.solra.auth.domain.service.AuthDomainService;
import com.solra.auth.domain.service.OAuthAuthenticationService;
import com.solra.auth.domain.service.PermissionEngine;
import com.solra.auth.infrastructure.security.AuthTokenService;
import com.solra.auth.infrastructure.sms.SmsProvider;
import com.solra.common.exception.SolraException;
import com.solra.common.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Application service — orchestrates the auth workflow across domain & infrastructure.
 * Covers: AUTH-001 (registration/login), AUTH-002 (third-party OAuth login), AUTH-004 (real-name verification).
 */
@Service
public class AuthApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);

    private final AuthDomainService authDomainService;
    private final OAuthAuthenticationService oauthService;
    private final AuthTokenService tokenService;
    private final SmsProvider smsProvider;
    private final PermissionEngine permissionEngine;
    private final ApplicationEventPublisher eventPublisher;

    // In-memory verification code store (production: use Redis with TTL)
    private final Map<String, VerificationCodeEntry> verificationCodes = new ConcurrentHashMap<>();

    public AuthApplicationService(AuthDomainService authDomainService,
                                   OAuthAuthenticationService oauthService,
                                   AuthTokenService tokenService,
                                   SmsProvider smsProvider,
                                   PermissionEngine permissionEngine,
                                   ApplicationEventPublisher eventPublisher) {
        this.authDomainService = authDomainService;
        this.oauthService = oauthService;
        this.tokenService = tokenService;
        this.smsProvider = smsProvider;
        this.permissionEngine = permissionEngine;
        this.eventPublisher = eventPublisher;
    }

    /**
     * AUTH-001: Register a new account.
     */
    @Transactional
    public AuthResultDTO register(RegisterCommand cmd) {
        UserAccount account;
        if (cmd.isPhoneRegistration()) {
            account = authDomainService.registerByPhone(cmd.phone(), cmd.password(), cmd.displayName());
        } else if (cmd.isUsernameRegistration()) {
            account = authDomainService.registerByUsername(cmd.username(), cmd.password(), cmd.displayName());
        } else {
            throw new SolraException.InvalidArgumentException("Either phone or username is required");
        }

        // Generate tokens
        List<String> roles = new ArrayList<>(account.getRoles());
        String accessToken = tokenService.generateAccessToken(account.getUserId(), roles);
        String refreshToken = tokenService.generateRefreshToken(account.getUserId());

        // Create session
        authDomainService.createSession(account.getUserId(), LoginMethod.PHONE_CODE,
                "registration", "0.0.0.0", accessToken, refreshToken,
                tokenService.getAccessTokenExpirationSeconds());

        // Publish domain event
        eventPublisher.publishEvent(new UserRegisteredEvent(
                account.getUserId(), account.getPhone(), account.getUsername(),
                cmd.isPhoneRegistration() ? LoginMethod.PHONE_CODE : LoginMethod.PASSWORD,
                account.getCreatedAt()));

        return toAuthResult(account, accessToken, refreshToken, roles);
    }

    /**
     * AUTH-001: Login with password.
     */
    @Transactional
    public AuthResultDTO login(LoginCommand cmd) {
        UserAccount account = authDomainService.loginByCredential(cmd.credential(), cmd.password());

        List<String> roles = new ArrayList<>(account.getRoles());
        String accessToken = tokenService.generateAccessToken(account.getUserId(), roles);
        String refreshToken = tokenService.generateRefreshToken(account.getUserId());

        LoginMethod method = SecurityUtils.isValidChinesePhone(cmd.credential())
                ? LoginMethod.PASSWORD : LoginMethod.PASSWORD;

        authDomainService.createSession(account.getUserId(), method,
                cmd.deviceInfo(), cmd.ipAddress(), accessToken, refreshToken,
                tokenService.getAccessTokenExpirationSeconds());

        account.recordLogin(cmd.ipAddress(), cmd.deviceInfo());

        eventPublisher.publishEvent(new UserLoggedInEvent(
                account.getUserId(), "", method, cmd.ipAddress(), java.time.Instant.now()));

        return toAuthResult(account, accessToken, refreshToken, roles);
    }

    /**
     * AUTH-001: Send SMS verification code.
     */
    public void sendSmsCode(String phone) {
        if (!SecurityUtils.isValidChinesePhone(phone)) {
            throw new SolraException.InvalidArgumentException("Invalid phone number");
        }

        String code = SecurityUtils.generateVerificationCode(6);
        verificationCodes.put(phone, new VerificationCodeEntry(code,
                System.currentTimeMillis() + 300_000)); // 5 minutes TTL

        boolean sent = smsProvider.sendVerificationCode(phone, code);
        if (!sent) {
            verificationCodes.remove(phone);
            throw new SolraException.InternalException("Failed to send verification code");
        }

        log.info("Verification code sent to {}", SecurityUtils.maskPhoneNumber(phone));
    }

    /**
     * AUTH-001: Login with SMS verification code.
     */
    @Transactional
    public AuthResultDTO loginWithSmsCode(LoginCommand cmd) {
        VerificationCodeEntry entry = verificationCodes.get(cmd.credential());
        if (entry == null || entry.isExpired()) {
            throw new SolraException.UnauthorizedException("Verification code expired or not sent");
        }

        UserAccount account = authDomainService.loginByPhoneCode(cmd.credential(),
                cmd.verificationCode(), entry.code());
        verificationCodes.remove(cmd.credential());

        List<String> roles = new ArrayList<>(account.getRoles());
        String accessToken = tokenService.generateAccessToken(account.getUserId(), roles);
        String refreshToken = tokenService.generateRefreshToken(account.getUserId());

        authDomainService.createSession(account.getUserId(), LoginMethod.PHONE_CODE,
                cmd.deviceInfo(), cmd.ipAddress(), accessToken, refreshToken,
                tokenService.getAccessTokenExpirationSeconds());

        return toAuthResult(account, accessToken, refreshToken, roles);
    }

    /**
     * Refresh access token using refresh token.
     */
    @Transactional
    public AuthResultDTO refreshToken(String refreshTokenStr) {
        if (!tokenService.validateToken(refreshTokenStr)) {
            throw new SolraException.TokenExpiredException("Invalid or expired refresh token");
        }
        String userId = tokenService.getUserIdFromToken(refreshTokenStr);
        UserAccount account = authDomainService.getById(userId);

        List<String> roles = new ArrayList<>(account.getRoles());
        String newAccessToken = tokenService.generateAccessToken(userId, roles);
        String newRefreshToken = tokenService.generateRefreshToken(userId);

        authDomainService.revokeUserSessions(userId);
        authDomainService.createSession(userId, LoginMethod.PASSWORD,
                "token-refresh", "0.0.0.0", newAccessToken, newRefreshToken,
                tokenService.getAccessTokenExpirationSeconds());

        return toAuthResult(account, newAccessToken, newRefreshToken, roles);
    }

    /**
     * Logout and revoke session.
     */
    public void logout(String userId) {
        authDomainService.revokeUserSessions(userId);
        log.info("User {} logged out", userId);
    }

    /**
     * AUTH-004: Submit real-name verification.
     */
    @Transactional
    public RealNameVerificationResultDTO submitRealNameVerification(RealNameVerificationCommand cmd) {
        // Encrypt ID number before storage (simulated with hash for dev)
        String encryptedId = SecurityUtils.hashSensitive(cmd.idNumber(), "solra_realname_salt");
        RealNameInfo info = RealNameInfo.create(cmd.realName(), encryptedId, cmd.birthDate());

        UserAccount account = authDomainService.submitRealNameVerification(cmd.userId(), info);

        // In production: call external real-name verification API
        // For now: auto-approve for demo purposes
        account = authDomainService.approveRealNameVerification(cmd.userId());

        eventPublisher.publishEvent(new RealNameVerifiedEvent(
                account.getUserId(), account.isMinor(), java.time.Instant.now()));

        return buildVerificationResult(account);
    }

    /**
     * AUTH-004: Check real-name verification & minor protection status.
     */
    public RealNameVerificationResultDTO checkVerificationStatus(String userId) {
        var status = authDomainService.checkMinorProtection(userId);
        UserAccount account = authDomainService.getById(userId);

        return switch (status) {
            case UNVERIFIED -> RealNameVerificationResultDTO.unverified(userId);
            case PENDING -> new RealNameVerificationResultDTO(userId, false, false, 0, "PENDING",
                    "Real-name verification is under review");
            case MINOR_RESTRICTED -> RealNameVerificationResultDTO.minorRestricted(userId,
                    account.getRealNameInfo().getAge());
            case ADULT -> RealNameVerificationResultDTO.adult(userId,
                    account.getRealNameInfo().getAge());
        };
    }

    // ===== AUTH-002: Third-Party OAuth Login =====

    /**
     * AUTH-002: Login/register via OAuth provider (微信/Apple/Google/Facebook).
     */
    @Transactional
    public AuthResultDTO loginByOAuth(OAuthLoginCommand cmd) {
        log.info("AUTH-002 OAuth login: provider={} providerUserId={}", cmd.provider(), cmd.providerUserId());

        UserAccount account = oauthService.loginByOAuth(
                cmd.provider(), cmd.providerUserId(), cmd.displayName(),
                cmd.email(), cmd.avatarUrl(), cmd.accessToken(), cmd.expiresAt());

        List<String> roles = new ArrayList<>(account.getRoles());
        String accessToken = tokenService.generateAccessToken(account.getUserId(), roles);
        String refreshToken = tokenService.generateRefreshToken(account.getUserId());

        authDomainService.createSession(account.getUserId(),
                LoginMethod.valueOf("OAUTH_" + cmd.provider().toUpperCase()),
                cmd.deviceInfo(), cmd.ipAddress(), accessToken, refreshToken,
                tokenService.getAccessTokenExpirationSeconds());

        account.recordLogin(cmd.ipAddress(), cmd.deviceInfo());

        eventPublisher.publishEvent(new UserLoggedInEvent(
                account.getUserId(), cmd.provider(), LoginMethod.PASSWORD, cmd.ipAddress(),
                java.time.Instant.now()));

        return toAuthResult(account, accessToken, refreshToken, roles);
    }

    /**
     * AUTH-002: Bind OAuth account to existing user.
     */
    @Transactional
    public AuthResultDTO bindOAuth(OAuthBindCommand cmd) {
        log.info("AUTH-002 bindOAuth: user={} provider={}", cmd.userId(), cmd.provider());

        UserAccount account = oauthService.bindOAuth(
                cmd.userId(), cmd.provider(), cmd.providerUserId(),
                cmd.displayName(), cmd.email(), cmd.avatarUrl(), cmd.accessToken(), cmd.expiresAt());

        List<String> roles = new ArrayList<>(account.getRoles());
        return toAuthResult(account, null, null, roles);
    }

    /**
     * AUTH-002: Unbind OAuth account from user.
     */
    @Transactional
    public AuthResultDTO unbindOAuth(OAuthUnbindCommand cmd) {
        log.info("AUTH-002 unbindOAuth: user={} provider={}", cmd.userId(), cmd.provider());

        UserAccount account = oauthService.unbindOAuth(cmd.userId(), cmd.provider());

        List<String> roles = new ArrayList<>(account.getRoles());
        return toAuthResult(account, null, null, roles);
    }

    /**
     * Verify an access token (for inter-service calls).
     */
    public boolean verifyAccessToken(String token) {
        return tokenService.validateToken(token);
    }

    // ===== AUTH-003: RBAC Permission Management =====

    /**
     * AUTH-003: Check if user has a specific permission using the RBAC engine.
     */
    public PermissionCheckResult checkPermission(String userId, String resource, String action) {
        Set<String> roles = authDomainService.getUserRoles(userId);
        PermissionEngine.PermissionResult result = permissionEngine.evaluate(roles, resource, action);
        return new PermissionCheckResult(result.allowed(), result.reason(), new ArrayList<>(roles));
    }

    /**
     * AUTH-003: Get effective permissions for a user.
     */
    public Set<String> getUserPermissions(String userId) {
        Set<String> roles = authDomainService.getUserRoles(userId);
        return permissionEngine.getEffectivePermissions(roles);
    }

    /**
     * AUTH-003: Assign a role to a user.
     */
    @Transactional
    public RoleOperationResult assignRole(String operatorUserId, String targetUserId, String role) {
        // Only SUPER_ADMIN or ADMIN can assign roles
        Set<String> operatorRoles = authDomainService.getUserRoles(operatorUserId);
        if (!permissionEngine.hasRole(operatorRoles, "ROLE_SUPER_ADMIN")
                && !permissionEngine.hasRole(operatorRoles, "ROLE_ADMIN")) {
            throw new SolraException.PermissionDeniedException("Only administrators can assign roles");
        }

        // Only SUPER_ADMIN can assign SUPER_ADMIN role
        if ("ROLE_SUPER_ADMIN".equals(role)
                && !permissionEngine.hasRole(operatorRoles, "ROLE_SUPER_ADMIN")) {
            throw new SolraException.PermissionDeniedException("Only super admin can assign SUPER_ADMIN role");
        }

        // Validate the assignment
        Set<String> targetRoles = authDomainService.getUserRoles(targetUserId);
        PermissionEngine.RoleAssignmentResult validation =
                permissionEngine.validateRoleAssignment(targetRoles, role);
        if (!validation.allowed()) {
            throw new SolraException.InvalidArgumentException(validation.reason());
        }

        authDomainService.assignRole(targetUserId, role);
        Set<String> newRoles = authDomainService.getUserRoles(targetUserId);

        log.info("AUTH-003: Role {} assigned to user {} by {}", role, targetUserId, operatorUserId);
        return new RoleOperationResult(targetUserId, new ArrayList<>(newRoles), "Role assigned: " + role);
    }

    /**
     * AUTH-003: Remove a role from a user.
     */
    @Transactional
    public RoleOperationResult removeRole(String operatorUserId, String targetUserId, String role) {
        Set<String> operatorRoles = authDomainService.getUserRoles(operatorUserId);
        if (!permissionEngine.hasRole(operatorRoles, "ROLE_SUPER_ADMIN")
                && !permissionEngine.hasRole(operatorRoles, "ROLE_ADMIN")) {
            throw new SolraException.PermissionDeniedException("Only administrators can remove roles");
        }

        // Only SUPER_ADMIN can remove SUPER_ADMIN role
        if ("ROLE_SUPER_ADMIN".equals(role)
                && !permissionEngine.hasRole(operatorRoles, "ROLE_SUPER_ADMIN")) {
            throw new SolraException.PermissionDeniedException("Only super admin can remove SUPER_ADMIN role");
        }

        Set<String> targetRoles = authDomainService.getUserRoles(targetUserId);
        PermissionEngine.RoleAssignmentResult validation =
                permissionEngine.validateRoleRemoval(targetRoles, role);
        if (!validation.allowed()) {
            throw new SolraException.InvalidArgumentException(validation.reason());
        }

        authDomainService.removeRole(targetUserId, role);
        Set<String> newRoles = authDomainService.getUserRoles(targetUserId);

        log.info("AUTH-003: Role {} removed from user {} by {}", role, targetUserId, operatorUserId);
        return new RoleOperationResult(targetUserId, new ArrayList<>(newRoles), "Role removed: " + role);
    }

    /**
     * AUTH-003: Get all roles for a user.
     */
    public List<String> getUserRoles(String userId) {
        return new ArrayList<>(authDomainService.getUserRoles(userId));
    }

    /**
     * AUTH-003: Get all predefined role definitions.
     */
    public List<RoleDefinition> getAllRoles() {
        return new ArrayList<>(permissionEngine.getAllPredefinedRoles());
    }

    // -- Private helpers --

    private AuthResultDTO toAuthResult(UserAccount account, String accessToken,
                                        String refreshToken, List<String> roles) {
        return new AuthResultDTO(
                account.getUserId(),
                account.getUsername(),
                account.getDisplayName(),
                account.getPhone(),
                account.getEmail(),
                account.getAvatarUrl(),
                accessToken,
                refreshToken,
                tokenService.getAccessTokenExpirationSeconds(),
                roles
        );
    }

    private RealNameVerificationResultDTO buildVerificationResult(UserAccount account) {
        if (account.getRealNameInfo() == null || !account.getRealNameInfo().isVerified()) {
            return RealNameVerificationResultDTO.unverified(account.getUserId());
        }
        if (account.isMinor()) {
            return RealNameVerificationResultDTO.minorRestricted(account.getUserId(),
                    account.getRealNameInfo().getAge());
        }
        return RealNameVerificationResultDTO.adult(account.getUserId(),
                account.getRealNameInfo().getAge());
    }

    // -- Inner types --

    private record VerificationCodeEntry(String code, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    /**
     * AUTH-003: Permission check result.
     */
    public record PermissionCheckResult(boolean allowed, String reason, List<String> roles) {}

    /**
     * AUTH-003: Role operation result.
     */
    public record RoleOperationResult(String userId, List<String> roles, String message) {}
}
