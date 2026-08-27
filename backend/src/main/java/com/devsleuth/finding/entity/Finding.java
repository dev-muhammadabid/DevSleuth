package com.devsleuth.finding.entity;

import com.devsleuth.common.entity.BaseEntity;
import com.devsleuth.common.enums.FindingCategory;
import com.devsleuth.common.enums.FindingSource;
import com.devsleuth.common.enums.Severity;
import com.devsleuth.review.entity.Review;
import jakarta.persistence.*;

@Entity
@Table(name = "findings")
public class Finding extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FindingSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FindingCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(nullable = false)
    private Integer confidence;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "line_start")
    private Integer lineStart;

    @Column(name = "line_end")
    private Integer lineEnd;

    @Column(nullable = false)
    private String fingerprint;

    @Column(name = "suggested_fix", columnDefinition = "TEXT")
    private String suggestedFix;

    /** User feedback: CONFIRMED, DISMISSED, or null (no feedback yet). */
    @Column(name = "user_verdict")
    private String userVerdict;

    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }
    public FindingSource getSource() { return source; }
    public void setSource(FindingSource source) { this.source = source; }
    public FindingCategory getCategory() { return category; }
    public void setCategory(FindingCategory category) { this.category = category; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public Integer getConfidence() { return confidence; }
    public void setConfidence(Integer confidence) { this.confidence = confidence; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Integer getLineStart() { return lineStart; }
    public void setLineStart(Integer lineStart) { this.lineStart = lineStart; }
    public Integer getLineEnd() { return lineEnd; }
    public void setLineEnd(Integer lineEnd) { this.lineEnd = lineEnd; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public String getSuggestedFix() { return suggestedFix; }
    public void setSuggestedFix(String suggestedFix) { this.suggestedFix = suggestedFix; }
    public String getUserVerdict() { return userVerdict; }
    public void setUserVerdict(String userVerdict) { this.userVerdict = userVerdict; }
}
