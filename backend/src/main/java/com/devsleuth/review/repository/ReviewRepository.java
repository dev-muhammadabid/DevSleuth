package com.devsleuth.review.repository;

import com.devsleuth.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByPullRequestIdOrderByCreatedAtDesc(UUID pullRequestId);
}
