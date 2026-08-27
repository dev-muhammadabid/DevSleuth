package com.devsleuth.pullrequest.dto;

import com.devsleuth.pullrequest.entity.PullRequest;

import java.util.UUID;

public record PullRequestResponse(
        UUID id,
        int number,
        String title,
        String author,
        String sourceBranch,
        String targetBranch,
        String commitSha,
        String status
) {
    public static PullRequestResponse from(PullRequest pr) {
        return new PullRequestResponse(
                pr.getId(), pr.getNumber(), pr.getTitle(), pr.getAuthor(),
                pr.getSourceBranch(), pr.getTargetBranch(), pr.getCommitSha(),
                pr.getStatus().name()
        );
    }
}
