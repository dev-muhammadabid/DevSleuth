package com.devsleuth.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetches repository metadata from GitHub.
 */
@Service
public class GitHubRepositoryService {

    private final GitHubService gitHubService;

    public GitHubRepositoryService(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    /**
     * List repositories accessible by the authenticated user.
     */
    public List<GitHubRepoInfo> listUserRepositories(String accessToken) {
        JsonNode repos = gitHubService.getAll("/user/repos", accessToken);
        List<GitHubRepoInfo> result = new ArrayList<>();
        for (JsonNode repo : repos) {
            result.add(new GitHubRepoInfo(
                    repo.get("id").asLong(),
                    repo.get("owner").get("login").asText(),
                    repo.get("name").asText(),
                    repo.get("full_name").asText(),
                    repo.has("default_branch") ? repo.get("default_branch").asText() : "main",
                    repo.has("language") && !repo.get("language").isNull()
                            ? repo.get("language").asText() : null,
                    repo.has("stargazers_count") ? repo.get("stargazers_count").asInt() : 0
            ));
        }
        return result;
    }

    public record GitHubRepoInfo(
            Long githubId,
            String owner,
            String name,
            String fullName,
            String defaultBranch,
            String language,
            int stars
    ) {}
}
