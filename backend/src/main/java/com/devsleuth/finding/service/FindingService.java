package com.devsleuth.finding.service;

import com.devsleuth.common.enums.Severity;
import com.devsleuth.finding.entity.Finding;
import com.devsleuth.finding.repository.FindingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FindingService {

    private final FindingRepository findingRepository;

    public FindingService(FindingRepository findingRepository) {
        this.findingRepository = findingRepository;
    }

    public List<Finding> getByReview(UUID reviewId) {
        return findingRepository.findByReviewId(reviewId);
    }

    public List<Finding> getByReviewAndSeverity(UUID reviewId, Severity severity) {
        return findingRepository.findByReviewIdAndSeverity(reviewId, severity);
    }

    public Finding save(Finding finding) {
        return findingRepository.save(finding);
    }

    public List<Finding> saveAll(List<Finding> findings) {
        return findingRepository.saveAll(findings);
    }

    public boolean isDuplicate(UUID reviewId, String fingerprint) {
        return findingRepository.existsByReviewIdAndFingerprint(reviewId, fingerprint);
    }
}
