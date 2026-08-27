package com.devsleuth.analysis.service;

import com.devsleuth.analysis.model.AnalysisInput;
import com.devsleuth.analysis.model.AnalysisInput.FileChange;
import com.devsleuth.auth.entity.User;
import com.devsleuth.github.service.GitHubPullRequestService;
import com.devsleuth.github.service.GitHubPullRequestService.GitHubPRFile;
import com.devsleuth.github.service.GitHubService;
import com.devsleuth.pullrequest.entity.PullRequest;
import com.devsleuth.repository.entity.Repository;
import com.devsleuth.review.entity.Review;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetches PR changed files from GitHub, filters to supported extensions (.java for V1),
 * and builds the AnalysisInput for downstream analyzers.
 */
@Service
public class DiffExtractionService {

    private static final Logger log = LoggerFactory.getLogger(DiffExtractionService.class);
    private static final String SUPPORTED_EXTENSION = ".java";

    private final GitHubPullRequestService gitHubPullRequestService;
    private final GitHubService gitHubService;

    public DiffExtractionService(GitHubPullRequestService gitHubPullRequestService,
                                 GitHubService gitHubService) {
        this.gitHubPullRequestService = gitHubPullRequestService;
        this.gitHubService = gitHubService;
    }

    /**
     * Extracts analysis input from a review's PR.
     * Requires an access token (from repository owner or installation).
     */
    public AnalysisInput extract(Review review, String accessToken) {
        PullRequest pr = review.getPullRequest();
        Repository repo = pr.getRepository();

        List<GitHubPRFile> allFiles = gitHubPullRequestService.getPullRequestFiles(
                repo.getOwner(), repo.getName(), pr.getNumber(), accessToken);

        List<FileChange> javaFiles = new ArrayList<>();
        for (GitHubPRFile file : allFiles) {
            if (!isSupportedFile(file.filename())) {
                continue;
            }
            if ("removed".equals(file.status())) {
                continue; // No point analyzing deleted files
            }

            // ponytail: fetch full content only for files < reasonable size; skip huge generated files
            String fullContent = fetchFileContent(repo.getOwner(), repo.getName(),
                    review.getCommitSha(), file.filename(), accessToken);

            javaFiles.add(new FileChange(file.filename(), file.patch(), fullContent));
        }

        log.info("Extracted {} Java files for review {} (out of {} total changed files)",
                javaFiles.size(), review.getId(), allFiles.size());

        return new AnalysisInput(
                review.getId(),
                repo.getFullName(),
                review.getCommitSha(),
                javaFiles
        );
    }

    private boolean isSupportedFile(String filename) {
        return filename != null && filename.endsWith(SUPPORTED_EXTENSION);
    }

    private String fetchFileContent(String owner, String repo, String sha, String path, String accessToken) {
        try {
            // GitHub Contents API returns base64-encoded content; use raw media type
            var node = gitHubService.get(
                    "/repos/" + owner + "/" + repo + "/contents/" + path + "?ref=" + sha,
                    accessToken);
            if (node.has("content")) {
                String encoded = node.get("content").asText().replaceAll("\\s", "");
                return new String(java.util.Base64.getDecoder().decode(encoded));
            }
            return null;
        } catch (Exception e) {
            log.warn("Could not fetch content for {}: {}", path, e.getMessage());
            return null;
        }
    }
}
