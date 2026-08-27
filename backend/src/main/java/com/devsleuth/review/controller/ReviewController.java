package com.devsleuth.review.controller;

import com.devsleuth.common.security.AccessGuard;
import com.devsleuth.finding.dto.FindingResponse;
import com.devsleuth.finding.entity.Finding;
import com.devsleuth.finding.service.FindingService;
import com.devsleuth.review.dto.ReviewComparisonResponse;
import com.devsleuth.review.dto.ReviewResponse;
import com.devsleuth.review.entity.Review;
import com.devsleuth.review.service.ReviewComparisonService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final FindingService findingService;
    private final ReviewComparisonService comparisonService;
    private final AccessGuard accessGuard;

    public ReviewController(FindingService findingService,
                            ReviewComparisonService comparisonService,
                            AccessGuard accessGuard) {
        this.findingService = findingService;
        this.comparisonService = comparisonService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable UUID id, HttpSession session) {
        Review review = accessGuard.requireReview(id, userId(session));
        return ResponseEntity.ok(ReviewResponse.from(review));
    }

    @GetMapping("/{id}/findings")
    public ResponseEntity<List<FindingResponse>> getFindings(@PathVariable UUID id, HttpSession session) {
        accessGuard.requireReview(id, userId(session));
        List<Finding> findings = findingService.getByReview(id);
        return ResponseEntity.ok(findings.stream().map(FindingResponse::from).toList());
    }

    /**
     * Compare two reviews: GET /api/reviews/{baseId}/compare/{compareId}
     * Returns new findings, resolved findings, and remaining findings.
     */
    @GetMapping("/{baseId}/compare/{compareId}")
    public ResponseEntity<ReviewComparisonResponse> compare(
            @PathVariable UUID baseId,
            @PathVariable UUID compareId,
            HttpSession session) {
        UUID uid = userId(session);
        accessGuard.requireReview(baseId, uid);
        accessGuard.requireReview(compareId, uid);
        return ResponseEntity.ok(comparisonService.compare(baseId, compareId));
    }

    private UUID userId(HttpSession session) {
        return (UUID) session.getAttribute("userId");
    }
}
