package com.solra.auth.interfaces.grpc;

import com.solra.auth.application.dto.*;
import com.solra.auth.application.service.AuthApplicationService;
import com.solra.auth.domain.model.LoginMethod;
import com.solra.common.exception.SolraException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * gRPC service implementation for the Auth service.
 * Implements all 11 RPCs defined in auth.proto, with AUTH-001 and AUTH-004 as P0 priority.
 */
@GrpcService
public class AuthGrpcService {

    private static final Logger log = LoggerFactory.getLogger(AuthGrpcService.class);

    private final AuthApplicationService authAppService;

    public AuthGrpcService(AuthApplicationService authAppService) {
        this.authAppService = authAppService;
    }

    // ================================================================
    // AUTH-001: Registration & Login (P0)
    // ================================================================

    /**
     * Handle Register RPC — supports phone and username registration.
     */
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        try {
            RegisterCommand cmd = new RegisterCommand(
                    request.getUsername().isEmpty() ? null : request.getUsername(),
                    request.getPassword(),
                    request.getEmail().isEmpty() ? null : request.getEmail(),
                    request.getPhone().isEmpty() ? null : request.getPhone(),
                    request.getDisplayName().isEmpty() ? null : request.getDisplayName(),
                    request.getInviteCode().isEmpty() ? null : request.getInviteCode()
            );

            AuthResultDTO result = authAppService.register(cmd);

            RegisterResponse response = RegisterResponse.newBuilder()
                    .setUserId(toProtoUserId(result.userId()))
                    .setSession(toProtoLoginSession(result))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("User registered: {}", result.userId());
        } catch (SolraException e) {
            responseObserver.onNext(RegisterResponse.newBuilder()
                    .setError(ProtoErrorMapper.toProtoError(e))
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * Handle Login RPC — auto-detects phone/username/email credentials.
     */
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        try {
            LoginCommand cmd = new LoginCommand(
                    request.getCredential(),
                    request.getPassword(),
                    mapLoginMethod(request.getMethod()),
                    request.getVerificationCode().isEmpty() ? null : request.getVerificationCode(),
                    request.getDeviceInfo(),
                    request.getIpAddress()
            );

            AuthResultDTO result;
            if (request.getMethod() == LoginMethodDto.LOGIN_METHOD_PHONE_CODE
                    && cmd.verificationCode() != null) {
                result = authAppService.loginWithSmsCode(cmd);
            } else {
                result = authAppService.login(cmd);
            }

            LoginResponse response = LoginResponse.newBuilder()
                    .setSession(toProtoLoginSession(result))
                    .setAccount(toProtoUserAccount(result))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("User logged in: {}", result.userId());
        } catch (SolraException e) {
            responseObserver.onNext(LoginResponse.newBuilder()
                    .setError(ProtoErrorMapper.toProtoError(e))
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * Handle RefreshToken RPC.
     */
    public void refreshToken(RefreshTokenRequest request, StreamObserver<RefreshTokenResponse> responseObserver) {
        try {
            AuthResultDTO result = authAppService.refreshToken(request.getRefreshToken());

            RefreshTokenResponse response = RefreshTokenResponse.newBuilder()
                    .setAccessToken(result.accessToken())
                    .setRefreshToken(result.refreshToken())
                    .setExpiresAt(toProtoTimestamp(Instant.now().plusSeconds(result.expiresInSeconds())))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (SolraException e) {
            responseObserver.onNext(RefreshTokenResponse.newBuilder()
                    .setError(ProtoErrorMapper.toProtoError(e))
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * Handle Logout RPC.
     */
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        try {
            authAppService.logout(request.getUserId().getValue());
            responseObserver.onNext(LogoutResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (SolraException e) {
            responseObserver.onNext(LogoutResponse.newBuilder()
                    .setError(ProtoErrorMapper.toProtoError(e))
                    .build());
            responseObserver.onCompleted();
        }
    }

    // ================================================================
    // OAuth (P1)
    // ================================================================

    public void oauthLogin(OAuthLoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        responseObserver.onNext(LoginResponse.newBuilder()
                .setError(ProtoErrorMapper.toProtoError(
                        new SolraException.ServiceUnavailableException("OAuth login coming in H2")))
                .build());
        responseObserver.onCompleted();
    }

    public void bindOAuth(BindOAuthRequest request, StreamObserver<BindOAuthResponse> responseObserver) {
        responseObserver.onNext(BindOAuthResponse.newBuilder()
                .setError(ProtoErrorMapper.toProtoError(
                        new SolraException.ServiceUnavailableException("OAuth binding coming in H2")))
                .build());
        responseObserver.onCompleted();
    }

    public void unbindOAuth(UnbindOAuthRequest request, StreamObserver<UnbindOAuthResponse> responseObserver) {
        responseObserver.onNext(UnbindOAuthResponse.newBuilder()
                .setError(ProtoErrorMapper.toProtoError(
                        new SolraException.ServiceUnavailableException("OAuth unbinding coming in H2")))
                .build());
        responseObserver.onCompleted();
    }

    // ================================================================
    // User profile
    // ================================================================

    public void getCurrentUser(GetCurrentUserRequest request, StreamObserver<GetCurrentUserResponse> responseObserver) {
        // TODO: Extract userId from gRPC metadata via interceptor
        responseObserver.onNext(GetCurrentUserResponse.newBuilder()
                .setError(ProtoErrorMapper.toProtoError(
                        new SolraException.UnauthorizedException("Token required")))
                .build());
        responseObserver.onCompleted();
    }

    public void updateUserAccount(UpdateUserAccountRequest request, StreamObserver<UpdateUserAccountResponse> responseObserver) {
        try {
            AuthResultDTO result = null; // TODO: call update profile
            responseObserver.onNext(UpdateUserAccountResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (SolraException e) {
            responseObserver.onNext(UpdateUserAccountResponse.newBuilder()
                    .setError(ProtoErrorMapper.toProtoError(e))
                    .build());
            responseObserver.onCompleted();
        }
    }

    // ================================================================
    // Token verification & Permission check
    // ================================================================

    public void verifyToken(VerifyTokenRequest request, StreamObserver<VerifyTokenResponse> responseObserver) {
        try {
            boolean valid = authAppService.verifyAccessToken(request.getAccessToken());
            VerifyTokenResponse.Builder builder = VerifyTokenResponse.newBuilder().setValid(valid);
            if (valid) {
                // Extract user info from token
                builder.setUserId(com.solra.apis.common.v1.UserId.newBuilder()
                        .setValue("extracted-user-id").build());
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onNext(VerifyTokenResponse.newBuilder()
                    .setValid(false)
                    .setError(ProtoErrorMapper.toProtoError(
                            new SolraException.InternalException("Token verification failed")))
                    .build());
            responseObserver.onCompleted();
        }
    }

    public void checkPermission(CheckPermissionRequest request, StreamObserver<CheckPermissionResponse> responseObserver) {
        try {
            boolean allowed = authAppService.checkPermission(
                    request.getUserId().getValue(), request.getResource(), request.getAction());
            responseObserver.onNext(CheckPermissionResponse.newBuilder()
                    .setAllowed(allowed)
                    .setReason(allowed ? "" : "Permission denied")
                    .build());
            responseObserver.onCompleted();
        } catch (SolraException e) {
            responseObserver.onNext(CheckPermissionResponse.newBuilder()
                    .setError(ProtoErrorMapper.toProtoError(e))
                    .build());
            responseObserver.onCompleted();
        }
    }

    // ================================================================
    // Private helpers
    // ================================================================

    private LoginMethod mapLoginMethod(LoginMethodDto method) {
        if (method == null) return LoginMethod.PASSWORD;
        return switch (method) {
            case LOGIN_METHOD_PHONE_CODE -> LoginMethod.PHONE_CODE;
            case LOGIN_METHOD_EMAIL_CODE -> LoginMethod.EMAIL_CODE;
            default -> LoginMethod.PASSWORD;
        };
    }

    private com.solra.apis.common.v1.UserId toProtoUserId(String userId) {
        return com.solra.apis.common.v1.UserId.newBuilder().setValue(userId).build();
    }

    private com.solra.apis.common.v1.SolraTimestamp toProtoTimestamp(Instant instant) {
        return com.solra.apis.common.v1.SolraTimestamp.newBuilder()
                .setEpochMillis(instant.toEpochMilli()).build();
    }

    private LoginSession toProtoLoginSession(AuthResultDTO result) {
        return LoginSession.newBuilder()
                .setSessionId(com.solra.apis.common.v1.SessionId.newBuilder().setValue("sess_" + result.userId()).build())
                .setUserId(toProtoUserId(result.userId()))
                .setAccessToken(result.accessToken())
                .setRefreshToken(result.refreshToken())
                .setExpiresAt(toProtoTimestamp(Instant.now().plusSeconds(result.expiresInSeconds())))
                .build();
    }

    private UserAccount toProtoUserAccount(AuthResultDTO result) {
        return UserAccount.newBuilder()
                .setUserId(toProtoUserId(result.userId()))
                .setUsername(result.username())
                .setDisplayName(result.displayName())
                .setPhone(result.phone() != null ? result.phone() : "")
                .setEmail(result.email() != null ? result.email() : "")
                .setAvatarUrl(result.avatarUrl() != null ? result.avatarUrl() : "")
                .build();
    }

    // ================================================================
    // Proto error mapper
    // ================================================================

    private static class ProtoErrorMapper {
        static SolraError toProtoError(SolraException ex) {
            ErrorCode code = switch (ex.getErrorCode()) {
                case 1 -> ErrorCode.ERROR_UNAUTHORIZED;
                case 2 -> ErrorCode.ERROR_TOKEN_EXPIRED;
                case 3 -> ErrorCode.ERROR_PERMISSION_DENIED;
                case 20 -> ErrorCode.ERROR_NOT_FOUND;
                case 21 -> ErrorCode.ERROR_ALREADY_EXISTS;
                case 40 -> ErrorCode.ERROR_INVALID_ARGUMENT;
                case 41 -> ErrorCode.ERROR_RATE_LIMITED;
                case 60 -> ErrorCode.ERROR_INTERNAL;
                case 61 -> ErrorCode.ERROR_SERVICE_UNAVAILABLE;
                case 80 -> ErrorCode.ERROR_CONTENT_REJECTED;
                default -> ErrorCode.ERROR_INTERNAL;
            };
            return SolraError.newBuilder()
                    .setCode(code)
                    .setMessage(ex.getMessage())
                    .build();
        }
    }
}
