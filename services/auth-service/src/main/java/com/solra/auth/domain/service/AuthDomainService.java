package com.solra.auth.domain.service;

import com.solra.auth.domain.model.*;
import com.solra.auth.domain.repository.DeviceBindingRepository;
import com.solra.auth.domain.repository.LoginSessionRepository;
import com.solra.auth.domain.repository.UserAccountRepository;
import com.solra.common.exception.SolraException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Domain service — encapsulates core authentication business logic.
 * Covers: AUTH-001 (phone registration/login), AUTH-004 (real-name verification).
 */
@Service
public class AuthDomainService {

    private final UserAccountRepository userAccountRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final DeviceBindingRepository deviceBindingRepository;
    private final PasswordEncoder passwordEncoder;

    public interface PasswordEncoder {
        String encode(String rawPassword);
        boolean matches(String rawPassword, String encodedPassword);
    }

    public AuthDomainService(UserAccountRepository userAccountRepository,
                              LoginSessionRepository loginSessionRepository,
                              DeviceBindingRepository deviceBindingRepository,
                              PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.loginSessionRepository = loginSessionRepository;
        this.deviceBindingRepository = deviceBindingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * AUTH-001: Register by phone number.
     */
    public UserAccount registerByPhone(String phone, String password, String displayName) {
        if (!com.solra.common.security.SecurityUtils.isValidChinesePhone(phone)) {
            throw new SolraException.InvalidArgumentException("Invalid phone number format");
        }
        if (userAccountRepository.existsByPhone(phone)) {
            throw new SolraException.AlreadyExistsException("Phone already registered");
        }
        String passwordHash = passwordEncoder.encode(password);
        UserAccount account = UserAccount.registerByPhone(phone, passwordHash, displayName);
        return userAccountRepository.save(account);
    }

    /**
     * AUTH-001: Register by username + password.
     */
    public UserAccount registerByUsername(String username, String password, String displayName) {
        if (username == null || username.length() < 3 || username.length() > 32) {
            throw new SolraException.InvalidArgumentException("Username must be 3-32 characters");
        }
        if (userAccountRepository.existsByUsername(username)) {
            throw new SolraException.AlreadyExistsException("Username already taken");
        }
        String passwordHash = passwordEncoder.encode(password);
        UserAccount account = UserAccount.registerByUsername(username, passwordHash, displayName);
        return userAccountRepository.save(account);
    }

    /**
     * AUTH-001: Login by phone + password.
     */
    public UserAccount loginByPhone(String phone, String password) {
        UserAccount account = userAccountRepository.findByPhone(phone)
                .orElseThrow(() -> new SolraException.UnauthorizedException("Phone not registered"));
        authenticate(account, password);
        return account;
    }

    /**
     * AUTH-001: Login by username + password.
     */
    public UserAccount loginByUsername(String username, String password) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new SolraException.UnauthorizedException("Username not found"));
        authenticate(account, password);
        return account;
    }

    /**
     * AUTH-001: Login by phone verification code.
     */
    public UserAccount loginByPhoneCode(String phone, String verificationCode, String expectedCode) {
        if (!expectedCode.equals(verificationCode)) {
            throw new SolraException.UnauthorizedException("Invalid verification code");
        }
        return userAccountRepository.findByPhone(phone)
                .orElseGet(() -> {
                    // Auto-register for verification code login
                    UserAccount newAccount = UserAccount.registerByPhone(phone, "", phone);
                    return userAccountRepository.save(newAccount);
                });
    }

    /**
     * AUTH-001: Login by credential (auto-detect phone/username/email).
     */
    public UserAccount loginByCredential(String credential, String password) {
        if (com.solra.common.security.SecurityUtils.isValidChinesePhone(credential)) {
            return loginByPhone(credential, password);
        }
        if (com.solra.common.security.SecurityUtils.isValidEmail(credential)) {
            return loginByEmail(credential, password);
        }
        return loginByUsername(credential, password);
    }

    private UserAccount loginByEmail(String email, String password) {
        UserAccount account = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new SolraException.UnauthorizedException("Email not registered"));
        authenticate(account, password);
        return account;
    }

    /**
     * AUTH-004: Submit real-name verification.
     */
    public UserAccount submitRealNameVerification(String userId, RealNameInfo info) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new SolraException.NotFoundException("User not found"));
        if (account.getRealNameInfo() != null && account.getRealNameInfo().isVerified()) {
            throw new SolraException.AlreadyExistsException("Real-name already verified");
        }
        account.submitRealNameVerification(info);
        return userAccountRepository.save(account);
    }

    /**
     * AUTH-004: Approve real-name verification.
     */
    public UserAccount approveRealNameVerification(String userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new SolraException.NotFoundException("User not found"));
        account.approveRealNameVerification();
        return userAccountRepository.save(account);
    }

    /**
     * AUTH-004: Check minor protection status & apply restrictions.
     */
    public MinorProtectionStatus checkMinorProtection(String userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new SolraException.NotFoundException("User not found"));
        if (account.getRealNameInfo() == null) {
            return MinorProtectionStatus.UNVERIFIED;
        }
        if (!account.getRealNameInfo().isVerified()) {
            return MinorProtectionStatus.PENDING;
        }
        if (account.getRealNameInfo().isMinor()) {
            return MinorProtectionStatus.MINOR_RESTRICTED;
        }
        return MinorProtectionStatus.ADULT;
    }

    public UserAccount getById(String userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new SolraException.NotFoundException("User not found: " + userId));
    }

    public UserAccount updateProfile(String userId, String displayName, String email, String avatarUrl) {
        UserAccount account = getById(userId);
        account.updateProfile(displayName, email, avatarUrl);
        return userAccountRepository.save(account);
    }

    // ===== AUTH-005: Multi-Device Session Management =====

    /**
     * AUTH-005: Create a session with device association.
     * Replaces old single-device strategy: now keeps up to 3 device sessions.
     */
    public LoginSession createSession(String userId, LoginMethod method,
                                       String deviceInfo, String ipAddress,
                                       String accessToken, String refreshToken,
                                       long expiresInSeconds) {
        // Derive deviceId from raw deviceInfo
        String deviceId = DeviceInfo.fingerprint(deviceInfo);
        return createSessionWithDevice(userId, method, deviceInfo, ipAddress,
                accessToken, refreshToken, expiresInSeconds, deviceId);
    }

    /**
     * AUTH-005: Create a session with explicit deviceId and enforce device limit.
     */
    public LoginSession createSessionWithDevice(String userId, LoginMethod method,
                                                 String deviceInfo, String ipAddress,
                                                 String accessToken, String refreshToken,
                                                 long expiresInSeconds, String deviceId) {
        // Get or create device binding
        DeviceBinding binding = deviceBindingRepository.findByUserId(userId)
                .orElseGet(() -> DeviceBinding.create(userId));

        // Parse device info and register
        DeviceInfo parsedDevice = DeviceInfo.fromRaw(deviceInfo);
        DeviceInfo registeredDevice = binding.registerDevice(
                new DeviceInfo.Builder()
                        .deviceId(deviceId)
                        .deviceName(parsedDevice.getDeviceName())
                        .platform(parsedDevice.getPlatform())
                        .osVersion(parsedDevice.getOsVersion())
                        .appVersion(parsedDevice.getAppVersion())
                        .deviceModel(parsedDevice.getDeviceModel())
                        .build());
        deviceBindingRepository.save(binding);

        // Remove old sessions for this specific device (same device re-login)
        loginSessionRepository.deleteByUserIdAndDeviceId(userId, deviceId);

        // Create new session with device association
        LoginSession session = LoginSession.create(userId, method, deviceInfo, ipAddress,
                accessToken, refreshToken, expiresInSeconds, deviceId);
        return loginSessionRepository.save(session);
    }

    /**
     * AUTH-005: Refresh tokens for a specific device session.
     */
    public LoginSession refreshSession(String refreshTokenStr, String newAccessToken,
                                        String newRefreshToken, long expiresInSeconds) {
        LoginSession session = loginSessionRepository.findByRefreshToken(refreshTokenStr)
                .orElseThrow(() -> new SolraException.TokenExpiredException("Invalid refresh token"));
        if (session.isExpired()) {
            loginSessionRepository.deleteById(session.getSessionId());
            throw new SolraException.TokenExpiredException("Session expired");
        }
        session.refreshTokens(newAccessToken, newRefreshToken, expiresInSeconds);
        return loginSessionRepository.save(session);
    }

    /**
     * AUTH-005: Revoke all sessions for a user (global logout).
     */
    public void revokeUserSessions(String userId) {
        loginSessionRepository.deleteByUserId(userId);
    }

    /**
     * AUTH-005: Revoke sessions for a specific device (single-device logout).
     */
    public void revokeDeviceSessions(String userId, String deviceId) {
        loginSessionRepository.deleteByUserIdAndDeviceId(userId, deviceId);
        // Also remove from device binding
        deviceBindingRepository.findByUserId(userId).ifPresent(binding -> {
            binding.removeDevice(deviceId);
            deviceBindingRepository.save(binding);
        });
    }

    /**
     * AUTH-005: Revoke a single session by sessionId.
     */
    public void revokeSession(String sessionId) {
        loginSessionRepository.deleteById(sessionId);
    }

    /**
     * AUTH-005: Get all active sessions for a user.
     */
    public List<LoginSession> getUserSessions(String userId) {
        return loginSessionRepository.findByUserId(userId).stream()
                .filter(s -> !s.isExpired())
                .collect(Collectors.toList());
    }

    /**
     * AUTH-005: Get device binding info for a user.
     */
    public DeviceBinding getDeviceBinding(String userId) {
        return deviceBindingRepository.findByUserId(userId)
                .orElseGet(() -> DeviceBinding.create(userId));
    }

    /**
     * AUTH-005: Remove a device from user's device binding (admin/manual operation).
     */
    public DeviceBinding removeDeviceFromBinding(String userId, String deviceId) {
        DeviceBinding binding = deviceBindingRepository.findByUserId(userId)
                .orElseThrow(() -> new SolraException.NotFoundException("No device binding found"));
        binding.removeDevice(deviceId);
        loginSessionRepository.deleteByUserIdAndDeviceId(userId, deviceId);
        return deviceBindingRepository.save(binding);
    }

    // ===== AUTH-003: RBAC Role Management =====

    /**
     * AUTH-003: Assign a role to a user.
     */
    public UserAccount assignRole(String userId, String role) {
        UserAccount account = getById(userId);
        account.assignRole(role);
        return userAccountRepository.save(account);
    }

    /**
     * AUTH-003: Remove a role from a user.
     */
    public UserAccount removeRole(String userId, String role) {
        UserAccount account = getById(userId);
        account.removeRole(role);
        return userAccountRepository.save(account);
    }

    /**
     * AUTH-003: Get user's roles.
     */
    public Set<String> getUserRoles(String userId) {
        UserAccount account = getById(userId);
        return account.getRoles();
    }

    private void authenticate(UserAccount account, String rawPassword) {
        if (!account.isActive()) {
            throw new SolraException.UnauthorizedException("Account is " + account.getStatus().name().toLowerCase());
        }
        if (account.getPasswordHash() == null || account.getPasswordHash().isEmpty()) {
            throw new SolraException.UnauthorizedException("Account has no password set");
        }
        if (!passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            throw new SolraException.UnauthorizedException("Invalid password");
        }
    }

    /**
     * Minor protection status enum (AUTH-004).
     */
    public enum MinorProtectionStatus {
        UNVERIFIED,     // No real-name info submitted
        PENDING,        // Real-name submitted, awaiting verification
        MINOR_RESTRICTED, // Verified as minor — apply restrictions
        ADULT           // Verified as adult — no restrictions
    }
}
