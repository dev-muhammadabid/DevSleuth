package com.devsleuth.pullrequest.repository;

import com.devsleuth.pullrequest.entity.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PullRequestRepository extends JpaRepository<PullRequest, UUID> {
    Optional<PullRequest> findByRepositoryIdAndNumber(UUID repositoryId, Integer number);
    List<PullRequest> findByRepositoryIdOrderByCreatedAtDesc(UUID repositoryId);

    /** Membership-scoped lookup: returns the PR only if the user can access its repo. */
    @Query("SELECT p FROM PullRequest p JOIN p.repository repo JOIN repo.members m "
            + "WHERE p.id = :id AND m.id = :userId")
    Optional<PullRequest> findByIdForUser(@Param("id") UUID id, @Param("userId") UUID userId);
}
