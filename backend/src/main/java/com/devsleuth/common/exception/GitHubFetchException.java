package com.devsleuth.common.exception;

/**
 * GitHub API call failures (rate limit, auth, network, deleted branch, invalid PR).
 */
public class GitHubFetchException extends ReviewException {

    public GitHubFetchException(String message) {
        super(message, ErrorCode.GITHUB_FETCH_FAILED, true);
    }

    public GitHubFetchException(String message, Throwable cause) {
        super(message, cause, ErrorCode.GITHUB_FETCH_FAILED, true);
    }

    /** Rate limited — retryable after backoff */
    public static GitHubFetchException rateLimited() {
        return new GitHubFetchException("GitHub API rate limit exceeded") {
            @Override public ErrorCode getErrorCode() { return ErrorCode.GITHUB_RATE_LIMITED; }
        };
    }

    /** Auth failure — NOT retryable (invalid token) */
    public static GitHubFetchException authFailed(String detail) {
        return new GitHubFetchException("GitHub authentication failed: " + detail) {
            @Override public ErrorCode getErrorCode() { return ErrorCode.GITHUB_AUTH_FAILED; }
            @Override public boolean isRetryable() { return false; }
        };
    }
}
