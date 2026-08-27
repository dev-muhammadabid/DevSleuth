package com.devsleuth.repository.repository;

import com.devsleuth.repository.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositoryRepository extends JpaRepository<Repository, UUID> {
    Optional<Repository> findByGithubRepositoryId(Long githubRepositoryId);
    Optional<Repository> findByFullName(String fullName);

    /** Repositories the given user is a member of. */
    List<Repository> findByMembers_Id(UUID userId);

    /** Membership-scoped lookup: returns the repository only if the user is a member. */
    Optional<Repository> findByIdAndMembers_Id(UUID id, UUID userId);
}
