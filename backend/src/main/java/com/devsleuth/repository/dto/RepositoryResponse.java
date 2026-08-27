package com.devsleuth.repository.dto;

import com.devsleuth.repository.entity.Repository;

import java.util.UUID;

public record RepositoryResponse(
        UUID id,
        Long githubRepositoryId,
        String owner,
        String name,
        String fullName,
        String defaultBranch,
        String language,
        boolean connected
) {
    public static RepositoryResponse from(Repository r) {
        return new RepositoryResponse(
                r.getId(), r.getGithubRepositoryId(), r.getOwner(), r.getName(),
                r.getFullName(), r.getDefaultBranch(), r.getLanguage(), r.isConnected()
        );
    }
}
