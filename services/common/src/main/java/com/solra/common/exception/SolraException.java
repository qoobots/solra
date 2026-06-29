package com.solra.common.exception;

/**
 * Unified exception hierarchy for Solra services.
 * Maps to solra.common.v1.ErrorCode Proto enum.
 */
public abstract class SolraException extends RuntimeException {
    private final int errorCode;

    protected SolraException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected SolraException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() { return errorCode; }

    // -- Concrete exception types --

    public static class UnauthorizedException extends SolraException {
        public UnauthorizedException(String message) { super(1, message); }
    }

    public static class TokenExpiredException extends SolraException {
        public TokenExpiredException(String message) { super(2, message); }
    }

    public static class PermissionDeniedException extends SolraException {
        public PermissionDeniedException(String message) { super(3, message); }
    }

    public static class NotFoundException extends SolraException {
        public NotFoundException(String message) { super(20, message); }
    }

    public static class AlreadyExistsException extends SolraException {
        public AlreadyExistsException(String message) { super(21, message); }
    }

    public static class InvalidArgumentException extends SolraException {
        public InvalidArgumentException(String message) { super(40, message); }
    }

    public static class RateLimitedException extends SolraException {
        public RateLimitedException(String message) { super(41, message); }
    }

    public static class InternalException extends SolraException {
        public InternalException(String message) { super(60, message); }
        public InternalException(String message, Throwable cause) { super(60, message, cause); }
    }

    public static class ServiceUnavailableException extends SolraException {
        public ServiceUnavailableException(String message) { super(61, message); }
    }

    public static class ContentRejectedException extends SolraException {
        public ContentRejectedException(String message) { super(80, message); }
    }
}
