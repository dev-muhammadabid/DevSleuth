package com.devsleuth.pullrequest.repository;

import com.devsleuth.pullrequest.entity.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PullRequestRepository extends JpaRepository<PullRequest, UUID> {
    Optional<PullRequest> findByRepositoryIdAndNumber(UUID repositoryId, Integer number);
    List<PullRequest> findByRepositoryIdOrderByCreatedAtDesc(UUID repositoryId);
}
