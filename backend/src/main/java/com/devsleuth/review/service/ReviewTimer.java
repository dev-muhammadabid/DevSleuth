package com.devsleuth.review.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-step timing for a review pipeline run.
 * Produces structured log output with reviewId + step name + duration.
 */
public class ReviewTimer {

    private static final Logger log = LoggerFactory.getLogger(ReviewTimer.class);

    private final UUID reviewId;
    private final Map<String, Long> stepDurations = new LinkedHashMap<>();
    private String currentStep;
    private long stepStart;

    public ReviewTimer(UUID reviewId) {
        this.reviewId = reviewId;
    }

    public void startStep(String step) {
        endCurrentStep();
        this.currentStep = step;
        this.stepStart = System.currentTimeMillis();
        log.info("review={} event={}_STARTED", reviewId, step);
    }

    public void endCurrentStep() {
        if (currentStep != null) {
            long elapsed = System.currentTimeMillis() - stepStart;
            stepDurations.put(currentStep, elapsed);
            log.info("review={} event={}_COMPLETED duration={}ms", reviewId, currentStep, elapsed);
            currentStep = null;
        }
    }

    public Map<String, Long> getStepDurations() {
        return stepDurations;
    }

    public long getTotalMs() {
        return stepDurations.values().stream().mapToLong(Long::longValue).sum();
    }

    public void logSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("review=").append(reviewId).append(" event=REVIEW_SUMMARY");
        for (var entry : stepDurations.entrySet()) {
            sb.append(" ").append(entry.getKey().toLowerCase()).append("=").append(entry.getValue()).append("ms");
        }
        sb.append(" total=").append(getTotalMs()).append("ms");
        log.info(sb.toString());
    }
}
