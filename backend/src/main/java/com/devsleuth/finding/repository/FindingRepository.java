package com.devsleuth.finding.repository;

import com.devsleuth.common.enums.Severity;
import com.devsleuth.finding.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FindingRepository extends JpaRepository<Finding, UUID> {
    List<Finding> findByReviewId(UUID reviewId);
    List<Finding> findByReviewIdAndSeverity(UUID reviewId, Severity severity);
    boolean existsByReviewIdAndFingerprint(UUID reviewId, String fingerprint);

    @Query("SELECT COUNT(f) FROM Finding f WHERE f.severity IN ('CRITICAL','HIGH')")
    long countHighRiskFindings();

    /** Membership-scoped lookup: returns the finding only if the user can access its repo. */
    @Query("SELECT f FROM Finding f JOIN f.review rv JOIN rv.pullRequest pr JOIN pr.repository repo "
            + "JOIN repo.members m WHERE f.id = :id AND m.id = :userId")
    Optional<Finding> findByIdForUser(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT COUNT(f) FROM Finding f JOIN f.review rv JOIN rv.pullRequest pr JOIN pr.repository repo "
            + "JOIN repo.members m WHERE m.id = :userId")
    long countForUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(f) FROM Finding f JOIN f.review rv JOIN rv.pullRequest pr JOIN pr.repository repo "
            + "JOIN repo.members m WHERE m.id = :userId AND f.severity IN ('CRITICAL','HIGH')")
    long countHighRiskForUser(@Param("userId") UUID userId);
}
