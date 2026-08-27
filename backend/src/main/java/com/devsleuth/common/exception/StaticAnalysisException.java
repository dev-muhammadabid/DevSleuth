package com.devsleuth.common.exception;

/**
 * Static analysis tool failures (tool missing, timeout, compiler error).
 */
public class StaticAnalysisException extends ReviewException {

    public StaticAnalysisException(String message) {
        super(message, ErrorCode.STATIC_ANALYSIS_FAILED, false);
    }

    public StaticAnalysisException(String message, Throwable cause) {
        super(message, cause, ErrorCode.STATIC_ANALYSIS_FAILED, false);
    }

    /** Tool timed out — retryable once */
    public static StaticAnalysisException timeout(String tool) {
        return new StaticAnalysisException("Static analysis timeout: " + tool) {
            @Override public ErrorCode getErrorCode() { return ErrorCode.STATIC_ANALYSIS_TIMEOUT; }
            @Override public boolean isRetryable() { return true; }
        };
    }
}
