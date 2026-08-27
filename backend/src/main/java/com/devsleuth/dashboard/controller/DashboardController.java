package com.devsleuth.dashboard.controller;

import com.devsleuth.pullrequest.entity.PullRequest;
import com.devsleuth.pullrequest.repository.PullRequestRepository;
import com.devsleuth.review.entity.Review;
import com.devsleuth.review.repository.ReviewRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final PullRequestRepository pullRequestRepository;
    private final ReviewRepository reviewRepository;

    public DashboardController(PullRequestRepository pullRequestRepository,
                               ReviewRepository reviewRepository) {
        this.pullRequestRepository = pullRequestRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/pull-requests")
    public ResponseEntity<List<PullRequest>> getRecentPRs(
            @RequestParam UUID repositoryId) {
        return ResponseEntity.ok(
                pullRequestRepository.findByRepositoryIdOrderByCreatedAtDesc(repositoryId));
    }

    @GetMapping("/pull-requests/{prId}/reviews")
    public ResponseEntity<List<Review>> getReviews(@PathVariable UUID prId) {
        return ResponseEntity.ok(
                reviewRepository.findByPullRequestIdOrderByCreatedAtDesc(prId));
    }
}
