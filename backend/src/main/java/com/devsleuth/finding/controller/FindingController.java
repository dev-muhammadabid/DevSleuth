package com.devsleuth.finding.controller;

import com.devsleuth.common.enums.Severity;
import com.devsleuth.finding.dto.FindingResponse;
import com.devsleuth.finding.entity.Finding;
import com.devsleuth.finding.service.FindingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews/{reviewId}/findings")
public class FindingController {

    private final FindingService findingService;

    public FindingController(FindingService findingService) {
        this.findingService = findingService;
    }

    @GetMapping
    public ResponseEntity<List<FindingResponse>> getFindings(
            @PathVariable UUID reviewId,
            @RequestParam(required = false) Severity severity) {

        List<Finding> findings = severity != null
                ? findingService.getByReviewAndSeverity(reviewId, severity)
                : findingService.getByReview(reviewId);

        List<FindingResponse> response = findings.stream()
                .map(FindingResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
