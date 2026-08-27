package com.devsleuth.review.dto;

import com.devsleuth.finding.dto.FindingResponse;

import java.util.List;

/**
 * Comparison between two reviews of the same PR.
 */
public record ReviewComparisonResponse(
        String baseReviewId,
        String compareReviewId,
        List<FindingResponse> newFindings,
        List<FindingResponse> resolvedFindings,
        List<FindingResponse> remainingFindings
) {}
