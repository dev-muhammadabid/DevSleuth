package com.devsleuth.dashboard.controller;

import com.devsleuth.common.security.AccessGuard;
import com.devsleuth.dashboard.dto.DashboardSummary;
import com.devsleuth.dashboard.dto.DashboardSummary.RecentReview;
import com.devsleuth.finding.repository.FindingRepository;
import com.devsleuth.pullrequest.dto.PullRequestResponse;
import com.devsleuth.pullrequest.entity.PullRequest;
import com.devsleuth.pullrequest.repository.PullRequestRepository;
import com.devsleuth.review.dto.ReviewResponse;
import com.devsleuth.review.entity.Review;
import com.devsleuth.review.repository.ReviewRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ReviewRepository reviewRepository;
    private final FindingRepository findingRepository;
    private final PullRequestRepository pullRequestRepository;
    private final AccessGuard accessGuard;

    public DashboardController(ReviewRepository reviewRepository,
                               FindingRepository findingRepository,
                               PullRequestRepository pullRequestRepository,
                               AccessGuard accessGuard) {
        this.reviewRepository = reviewRepository;
        this.findingRepository = findingRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.accessGuard = accessGuard;
    }

    /**
     * Summary is scoped to the signed-in user's repositories. Transactional so the
     * lazy PullRequest -> Repository walk below runs with an open session
     * (open-in-view is disabled).
     */
    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<DashboardSummary> getSummary(HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");

        long totalReviews = reviewRepository.countForUser(userId);
        long totalFindings = findingRepository.countForUser(userId);
        long highRisk = findingRepository.countHighRiskForUser(userId);

        List<Review> recent = reviewRepository.findRecentForUser(userId, PageRequest.of(0, 10));
        List<RecentReview> recentReviews = recent.stream().map(r -> {
            PullRequest pr = r.getPullRequest();
            return new RecentReview(
                    r.getId().toString(),
                    pr.getNumber(),
                    pr.getTitle(),
                    pr.getRepository().getFullName(),
                    r.getStatus().name(),
                    r.getFinalFindingCount() != null ? r.getFinalFindingCount() : 0,
                    r.getCreatedAt().toString()
            );
        }).toList();

        return ResponseEntity.ok(new DashboardSummary(totalReviews, totalFindings, highRisk, recentReviews));
    }

    /**
     * Transactional + DTO mapping: with open-in-view disabled, returning raw entities
     * would throw LazyInitializationException when Jackson walks their lazy relations
     * outside a session. Mapping to DTOs inside the transaction avoids that.
     */
    @GetMapping("/pull-requests")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PullRequestResponse>> getRecentPRs(@RequestParam UUID repositoryId, HttpSession session) {
        accessGuard.requireRepository(repositoryId, (UUID) session.getAttribute("userId"));
        List<PullRequestResponse> prs = pullRequestRepository.findByRepositoryIdOrderByCreatedAtDesc(repositoryId)
                .stream().map(PullRequestResponse::from).toList();
        return ResponseEntity.ok(prs);
    }

    @GetMapping("/pull-requests/{prId}/reviews")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable UUID prId, HttpSession session) {
        accessGuard.requirePullRequest(prId, (UUID) session.getAttribute("userId"));
        List<ReviewResponse> reviews = reviewRepository.findByPullRequestIdOrderByCreatedAtDesc(prId)
                .stream().map(ReviewResponse::from).toList();
        return ResponseEntity.ok(reviews);
    }
}
