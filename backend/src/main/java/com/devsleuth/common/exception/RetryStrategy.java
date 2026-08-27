package com.devsleuth.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Simple retry utility for review pipeline operations.
 *
 * Retryable errors: network timeouts, rate limits, transient 5xx.
 * Non-retryable errors: invalid API keys, malformed requests, missing tools.
 */
public final class RetryStrategy {

    private static final Logger log = LoggerFactory.getLogger(RetryStrategy.class);

    private RetryStrategy() {}

    /**
     * Execute with retry. Only retries if the exception is a retryable ReviewException.
     * Uses exponential backoff: 1s, 2s, 4s...
     *
     * @param maxAttempts total attempts (including first)
     * @param operation the operation to retry
     * @param operationName for logging
     * @return the result
     * @throws ReviewException if all attempts exhausted or non-retryable error
     */
    public static <T> T execute(int maxAttempts, Supplier<T> operation, String operationName) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (ReviewException e) {
                lastException = e;
                if (!e.isRetryable() || attempt == maxAttempts) {
                    log.error("{} failed (attempt {}/{}, non-retryable={}): {}",
                            operationName, attempt, maxAttempts, !e.isRetryable(), e.getMessage());
                    throw e;
                }
                long backoffMs = (long) Math.pow(2, attempt - 1) * 1000;
                log.warn("{} failed (attempt {}/{}), retrying in {}ms: {}",
                        operationName, attempt, maxAttempts, backoffMs, e.getMessage());
                sleep(backoffMs);
            } catch (Exception e) {
                // Unknown exceptions are not retried
                log.error("{} failed with unexpected error: {}", operationName, e.getMessage());
                throw e;
            }
        }

        // Should not reach here, but safety net
        throw new RuntimeException(operationName + " failed after " + maxAttempts + " attempts", lastException);
    }

    /**
     * Void variant for operations that don't return a value.
     */
    public static void executeVoid(int maxAttempts, Runnable operation, String operationName) {
        execute(maxAttempts, () -> { operation.run(); return null; }, operationName);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
