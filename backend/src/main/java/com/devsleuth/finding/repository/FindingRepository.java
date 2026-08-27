package com.devsleuth.finding.repository;

import com.devsleuth.common.enums.Severity;
import com.devsleuth.finding.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FindingRepository extends JpaRepository<Finding, UUID> {
    List<Finding> findByReviewId(UUID reviewId);
    List<Finding> findByReviewIdAndSeverity(UUID reviewId, Severity severity);
    boolean existsByReviewIdAndFingerprint(UUID reviewId, String fingerprint);
}
