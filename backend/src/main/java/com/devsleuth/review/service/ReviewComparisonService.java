package com.devsleuth.review.service;

import com.devsleuth.finding.entity.Finding;
import com.devsleuth.finding.service.FindingService;
import com.devsleuth.review.dto.ReviewComparisonResponse;
import com.devsleuth.finding.dto.FindingResponse;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Compares two reviews by fingerprint to produce new/resolved/remaining findings.
 */
@Service
public class ReviewComparisonService {

    private final FindingService findingService;

    public ReviewComparisonService(FindingService findingService) {
        this.findingService = findingService;
    }

    public ReviewComparisonResponse compare(UUID baseReviewId, UUID compareReviewId) {
        List<Finding> baseFindings = findingService.getByReview(baseReviewId);
        List<Finding> compareFindings = findingService.getByReview(compareReviewId);

        Set<String> baseFingerprints = new HashSet<>();
        for (Finding f : baseFindings) {
            baseFingerprints.add(f.getFingerprint());
        }

        Set<String> compareFingerprints = new HashSet<>();
        for (Finding f : compareFindings) {
            compareFingerprints.add(f.getFingerprint());
        }

        List<FindingResponse> newFindings = new ArrayList<>();
        List<FindingResponse> remaining = new ArrayList<>();

        for (Finding f : compareFindings) {
            if (baseFingerprints.contains(f.getFingerprint())) {
                remaining.add(FindingResponse.from(f));
            } else {
                newFindings.add(FindingResponse.from(f));
            }
        }

        List<FindingResponse> resolved = new ArrayList<>();
        for (Finding f : baseFindings) {
            if (!compareFingerprints.contains(f.getFingerprint())) {
                resolved.add(FindingResponse.from(f));
            }
        }

        return new ReviewComparisonResponse(
                baseReviewId.toString(),
                compareReviewId.toString(),
                newFindings,
                resolved,
                remaining
        );
    }
}
