package com.devsleuth.common.exception;

/**
 * Base exception for review pipeline failures.
 * Subclasses indicate specific failure types and retryability.
 */
public abstract class ReviewException extends RuntimeException {

    private final ErrorCode errorCode;
    private final boolean retryable;

    protected ReviewException(String message, ErrorCode errorCode, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    protected ReviewException(String message, Throwable cause, ErrorCode errorCode, boolean retryable) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public boolean isRetryable() { return retryable; }

    /**
     * Standardized error codes for review pipeline failures.
     */
    public enum ErrorCode {
        GITHUB_FETCH_FAILED,
        GITHUB_AUTH_FAILED,
        GITHUB_RATE_LIMITED,
        STATIC_ANALYSIS_FAILED,
        STATIC_ANALYSIS_TIMEOUT,
        AI_ANALYSIS_FAILED,
        AI_RATE_LIMITED,
        AI_AUTH_FAILED,
        INVALID_AI_RESPONSE,
        REVIEW_TIMEOUT,
        DATABASE_ERROR,
        NO_ACCESS_TOKEN
    }
}
