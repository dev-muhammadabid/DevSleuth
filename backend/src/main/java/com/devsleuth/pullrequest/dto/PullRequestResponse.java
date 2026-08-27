package com.devsleuth.pullrequest.dto;

import com.devsleuth.pullrequest.entity.PullRequest;
import com.devsleuth.review.entity.Review;

import java.util.UUID;

public record PullRequestResponse(
        UUID id,
        int number,
        String title,
        String author,
        String sourceBranch,
        String targetBranch,
        String commitSha,
        String status,
        LatestReviewInfo latestReview
) {
    public record LatestReviewInfo(UUID reviewId, String status, Integer finalFindingCount) {
        public static LatestReviewInfo from(Review review) {
            return new LatestReviewInfo(
                    review.getId(),
                    review.getStatus() == null ? null : review.getStatus().name(),
                    review.getFinalFindingCount()
            );
        }
    }

    public static PullRequestResponse from(PullRequest pr) {
        return from(pr, null);
    }

    public static PullRequestResponse from(PullRequest pr, LatestReviewInfo latestReview) {
        return new PullRequestResponse(
                pr.getId(), pr.getNumber(), pr.getTitle(), pr.getAuthor(),
                pr.getSourceBranch(), pr.getTargetBranch(), pr.getCommitSha(),
                pr.getStatus().name(), latestReview
        );
    }
}
