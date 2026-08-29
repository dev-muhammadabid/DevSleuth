package com.devsleuth.review.repository;

import com.devsleuth.review.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByPullRequestIdOrderByCreatedAtDesc(UUID pullRequestId);
    Optional<Review> findFirstByPullRequestIdOrderByCreatedAtDesc(UUID pullRequestId);

    /** Membership-scoped lookup: returns the review only if the user can access its repo. */
    @Query("SELECT r FROM Review r JOIN r.pullRequest pr JOIN pr.repository repo JOIN repo.members m "
            + "WHERE r.id = :id AND m.id = :userId")
    Optional<Review> findByIdForUser(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT r FROM Review r JOIN r.pullRequest pr JOIN pr.repository repo JOIN repo.members m "
            + "WHERE m.id = :userId ORDER BY r.createdAt DESC")
    List<Review> findRecentForUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Review r JOIN r.pullRequest pr JOIN pr.repository repo JOIN repo.members m "
            + "WHERE m.id = :userId")
    long countForUser(@Param("userId") UUID userId);

    /** Access tokens of the review's repository members, for fetching the diff from GitHub. */
    @Query("SELECT m.accessToken FROM Review r JOIN r.pullRequest pr JOIN pr.repository repo JOIN repo.members m "
            + "WHERE r.id = :reviewId AND m.accessToken IS NOT NULL")
    List<String> findMemberAccessTokens(@Param("reviewId") UUID reviewId, Pageable pageable);
}
