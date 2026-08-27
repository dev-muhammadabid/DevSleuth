package com.devsleuth.common.exception;

/**
 * AI/LLM analysis failures (timeout, rate limit, invalid response, auth).
 */
public class AiAnalysisException extends ReviewException {

    public AiAnalysisException(String message, ErrorCode code, boolean retryable) {
        super(message, code, retryable);
    }

    public AiAnalysisException(String message, Throwable cause, ErrorCode code, boolean retryable) {
        super(message, cause, code, retryable);
    }

    /** LLM timeout or 5xx — retryable */
    public static AiAnalysisException timeout() {
        return new AiAnalysisException("AI analysis timed out", ErrorCode.AI_ANALYSIS_FAILED, true);
    }

    /** LLM rate limited — retryable after backoff */
    public static AiAnalysisException rateLimited() {
        return new AiAnalysisException("AI rate limit exceeded", ErrorCode.AI_RATE_LIMITED, true);
    }

    /** Invalid API key — NOT retryable */
    public static AiAnalysisException authFailed() {
        return new AiAnalysisException("AI API authentication failed", ErrorCode.AI_AUTH_FAILED, false);
    }

    /** Malformed response after retries — NOT retryable */
    public static AiAnalysisException invalidResponse(String detail) {
        return new AiAnalysisException("Invalid AI response: " + detail, ErrorCode.INVALID_AI_RESPONSE, false);
    }
}
