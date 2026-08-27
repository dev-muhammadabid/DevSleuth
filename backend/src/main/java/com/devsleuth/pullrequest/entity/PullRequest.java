package com.devsleuth.pullrequest.entity;

import com.devsleuth.common.entity.BaseEntity;
import com.devsleuth.common.enums.PullRequestStatus;
import com.devsleuth.repository.entity.Repository;
import jakarta.persistence.*;

@Entity
@Table(name = "pull_requests")
public class PullRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "github_pr_id", nullable = false)
    private Long githubPrId;

    @Column(nullable = false)
    private Integer number;

    @Column(nullable = false)
    private String title;

    private String author;

    @Column(name = "source_branch")
    private String sourceBranch;

    @Column(name = "target_branch")
    private String targetBranch;

    @Column(name = "commit_sha")
    private String commitSha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PullRequestStatus status;

    public Repository getRepository() { return repository; }
    public void setRepository(Repository repository) { this.repository = repository; }
    public Long getGithubPrId() { return githubPrId; }
    public void setGithubPrId(Long githubPrId) { this.githubPrId = githubPrId; }
    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getSourceBranch() { return sourceBranch; }
    public void setSourceBranch(String sourceBranch) { this.sourceBranch = sourceBranch; }
    public String getTargetBranch() { return targetBranch; }
    public void setTargetBranch(String targetBranch) { this.targetBranch = targetBranch; }
    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }
    public PullRequestStatus getStatus() { return status; }
    public void setStatus(PullRequestStatus status) { this.status = status; }
}
