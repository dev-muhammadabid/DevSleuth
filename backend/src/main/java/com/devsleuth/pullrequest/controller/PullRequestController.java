package com.devsleuth.pullrequest.controller;

import com.devsleuth.auth.entity.User;
import com.devsleuth.auth.service.AuthService;
import com.devsleuth.common.security.AccessGuard;
import com.devsleuth.pullrequest.dto.PullRequestResponse;
import com.devsleuth.pullrequest.entity.PullRequest;
import com.devsleuth.pullrequest.service.PullRequestService;
import com.devsleuth.review.entity.Review;
import com.devsleuth.review.repository.ReviewRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/repositories/{repoId}/pull-requests")
public class PullRequestController {

    private final PullRequestService pullRequestService;
    private final AuthService authService;
    private final AccessGuard accessGuard;
    private final ReviewRepository reviewRepository;

    public PullRequestController(PullRequestService pullRequestService, AuthService authService,
                                 AccessGuard accessGuard, ReviewRepository reviewRepository) {
        this.pullRequestService = pullRequestService;
        this.authService = authService;
        this.accessGuard = accessGuard;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping
    public ResponseEntity<List<PullRequestResponse>> list(@PathVariable UUID repoId, HttpSession session) {
        User user = getUser(session);
        if (user == null) return ResponseEntity.status(401).build();
        accessGuard.requireRepository(repoId, user.getId());

        List<PullRequest> prs = pullRequestService.listByRepository(repoId, user);
        return ResponseEntity.ok(prs.stream().map(this::toResponse).toList());
    }

    /**
     * Manual trigger: POST /api/repositories/{repoId}/pull-requests/{number}/analyze
     */
    @PostMapping("/{number}/analyze")
    public ResponseEntity<Map<String, Object>> analyze(
            @PathVariable UUID repoId,
            @PathVariable int number,
            @RequestParam(required = false, defaultValue = "HYBRID") String mode,
            HttpSession session) {

        User user = getUser(session);
        if (user == null) return ResponseEntity.status(401).build();
        accessGuard.requireRepository(repoId, user.getId());

        com.devsleuth.experiment.ExperimentMode experimentMode;
        try {
            experimentMode = com.devsleuth.experiment.ExperimentMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            experimentMode = com.devsleuth.experiment.ExperimentMode.HYBRID;
        }

        Review review = pullRequestService.triggerAnalysis(repoId, number, user, experimentMode);
        return ResponseEntity.ok(Map.of(
                "reviewId", review.getId(),
                "status", review.getStatus().name()
        ));
    }

    private PullRequestResponse toResponse(PullRequest pr) {
        PullRequestResponse.LatestReviewInfo latestReview = reviewRepository
                .findFirstByPullRequestIdOrderByCreatedAtDesc(pr.getId())
                .map(PullRequestResponse.LatestReviewInfo::from)
                .orElse(null);
        return PullRequestResponse.from(pr, latestReview);
    }

    private User getUser(HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) return null;
        return authService.findById(userId).orElse(null);
    }
}
