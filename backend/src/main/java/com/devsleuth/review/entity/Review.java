package com.devsleuth.review.entity;

import com.devsleuth.common.entity.BaseEntity;
import com.devsleuth.common.enums.ReviewStatus;
import com.devsleuth.pullrequest.entity.PullRequest;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reviews")
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = false)
    private PullRequest pullRequest;

    @Column(name = "commit_sha", nullable = false)
    private String commitSha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "static_finding_count")
    private Integer staticFindingCount;

    @Column(name = "ai_finding_count")
    private Integer aiFindingCount;

    @Column(name = "final_finding_count")
    private Integer finalFindingCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    public PullRequest getPullRequest() { return pullRequest; }
    public void setPullRequest(PullRequest pullRequest) { this.pullRequest = pullRequest; }
    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }
    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public Integer getStaticFindingCount() { return staticFindingCount; }
    public void setStaticFindingCount(Integer staticFindingCount) { this.staticFindingCount = staticFindingCount; }
    public Integer getAiFindingCount() { return aiFindingCount; }
    public void setAiFindingCount(Integer aiFindingCount) { this.aiFindingCount = aiFindingCount; }
    public Integer getFinalFindingCount() { return finalFindingCount; }
    public void setFinalFindingCount(Integer finalFindingCount) { this.finalFindingCount = finalFindingCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
