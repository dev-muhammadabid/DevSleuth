package com.devsleuth.common.security;

import com.devsleuth.common.exception.DevSleuthException;
import com.devsleuth.finding.entity.Finding;
import com.devsleuth.finding.repository.FindingRepository;
import com.devsleuth.pullrequest.entity.PullRequest;
import com.devsleuth.pullrequest.repository.PullRequestRepository;
import com.devsleuth.repository.entity.Repository;
import com.devsleuth.repository.repository.RepositoryRepository;
import com.devsleuth.review.entity.Review;
import com.devsleuth.review.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Centralizes per-user resource authorization. Access to every resource is derived
 * (transitively) from Repository membership, so it is enforced in the query itself
 * rather than by loading the object and comparing ids in each controller.
 *
 * <p>Not-found and not-owned both map to 404 so an attacker can't distinguish
 * "doesn't exist" from "exists but isn't yours" (avoids resource enumeration).
 */
@Service
public class AccessGuard {

    private final ReviewRepository reviewRepository;
    private final FindingRepository findingRepository;
    private final RepositoryRepository repositoryRepository;
    private final PullRequestRepository pullRequestRepository;

    public AccessGuard(ReviewRepository reviewRepository,
                       FindingRepository findingRepository,
                       RepositoryRepository repositoryRepository,
                       PullRequestRepository pullRequestRepository) {
        this.reviewRepository = reviewRepository;
        this.findingRepository = findingRepository;
        this.repositoryRepository = repositoryRepository;
        this.pullRequestRepository = pullRequestRepository;
    }

    public Review requireReview(UUID reviewId, UUID userId) {
        return reviewRepository.findByIdForUser(reviewId, userId)
                .orElseThrow(() -> notFound("Review"));
    }

    public Finding requireFinding(UUID findingId, UUID userId) {
        return findingRepository.findByIdForUser(findingId, userId)
                .orElseThrow(() -> notFound("Finding"));
    }

    public Repository requireRepository(UUID repositoryId, UUID userId) {
        return repositoryRepository.findByIdAndMembers_Id(repositoryId, userId)
                .orElseThrow(() -> notFound("Repository"));
    }

    public PullRequest requirePullRequest(UUID pullRequestId, UUID userId) {
        return pullRequestRepository.findByIdForUser(pullRequestId, userId)
                .orElseThrow(() -> notFound("Pull request"));
    }

    private DevSleuthException notFound(String what) {
        return new DevSleuthException(what + " not found", HttpStatus.NOT_FOUND);
    }
}
