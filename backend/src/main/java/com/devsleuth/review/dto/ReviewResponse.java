package com.devsleuth.review.dto;

import com.devsleuth.review.entity.Review;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID pullRequestId,
        String commitSha,
        String status,
        Instant startedAt,
        Instant completedAt,
        Long durationMs,
        Integer staticFindingCount,
        Integer aiFindingCount,
        Integer finalFindingCount,
        String errorMessage,
        Instant createdAt
) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getPullRequest() != null ? r.getPullRequest().getId() : null,
                r.getCommitSha(),
                r.getStatus().name(),
                r.getStartedAt(),
                r.getCompletedAt(),
                r.getDurationMs(),
                r.getStaticFindingCount(),
                r.getAiFindingCount(),
                r.getFinalFindingCount(),
                r.getErrorMessage(),
                r.getCreatedAt()
        );
    }
}
