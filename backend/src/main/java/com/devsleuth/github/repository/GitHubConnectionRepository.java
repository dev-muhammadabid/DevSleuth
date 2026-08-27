package com.devsleuth.github.repository;

import com.devsleuth.github.entity.GitHubConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GitHubConnectionRepository extends JpaRepository<GitHubConnection, UUID> {
    Optional<GitHubConnection> findByInstallationId(Long installationId);
    Optional<GitHubConnection> findByUserId(UUID userId);
}
