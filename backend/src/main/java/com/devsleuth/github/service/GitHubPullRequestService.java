package com.devsleuth.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetches Pull Request metadata, files, and diffs from GitHub.
 */
@Service
public class GitHubPullRequestService {

    private final GitHubService gitHubService;

    public GitHubPullRequestService(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    /**
     * List open PRs for a repository.
     */
    public List<GitHubPRInfo> listOpenPullRequests(String owner, String repo, String accessToken) {
        JsonNode prs = gitHubService.getAll("/repos/" + owner + "/" + repo + "/pulls?state=open", accessToken);
        List<GitHubPRInfo> result = new ArrayList<>();
        for (JsonNode pr : prs) {
            result.add(mapPR(pr));
        }
        return result;
    }

    /**
     * Get a single PR by number.
     */
    public GitHubPRInfo getPullRequest(String owner, String repo, int number, String accessToken) {
        JsonNode pr = gitHubService.get("/repos/" + owner + "/" + repo + "/pulls/" + number, accessToken);
        return mapPR(pr);
    }

    /**
     * Get the unified diff of a PR.
     */
    public String getPullRequestDiff(String owner, String repo, int number, String accessToken) {
        return gitHubService.getRaw("/repos/" + owner + "/" + repo + "/pulls/" + number, accessToken);
    }

    /**
     * Get list of files changed in a PR.
     */
    public List<GitHubPRFile> getPullRequestFiles(String owner, String repo, int number, String accessToken) {
        JsonNode files = gitHubService.getAll("/repos/" + owner + "/" + repo + "/pulls/" + number + "/files", accessToken);
        List<GitHubPRFile> result = new ArrayList<>();
        for (JsonNode file : files) {
            result.add(new GitHubPRFile(
                    file.get("filename").asText(),
                    file.get("status").asText(),
                    file.has("additions") ? file.get("additions").asInt() : 0,
                    file.has("deletions") ? file.get("deletions").asInt() : 0,
                    file.has("patch") ? file.get("patch").asText() : null
            ));
        }
        return result;
    }

    private GitHubPRInfo mapPR(JsonNode pr) {
        return new GitHubPRInfo(
                pr.get("id").asLong(),
                pr.get("number").asInt(),
                pr.get("title").asText(),
                pr.get("user").get("login").asText(),
                pr.get("head").get("ref").asText(),
                pr.get("base").get("ref").asText(),
                pr.get("head").get("sha").asText(),
                pr.get("state").asText()
        );
    }

    public record GitHubPRInfo(
            Long githubPrId,
            int number,
            String title,
            String author,
            String sourceBranch,
            String targetBranch,
            String commitSha,
            String state
    ) {}

    public record GitHubPRFile(
            String filename,
            String status,
            int additions,
            int deletions,
            String patch
    ) {}
}
