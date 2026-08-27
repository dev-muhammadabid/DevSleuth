package com.devsleuth.repository.repository;

import com.devsleuth.repository.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RepositoryRepository extends JpaRepository<Repository, UUID> {
    Optional<Repository> findByGithubRepositoryId(Long githubRepositoryId);
    Optional<Repository> findByFullName(String fullName);
}
