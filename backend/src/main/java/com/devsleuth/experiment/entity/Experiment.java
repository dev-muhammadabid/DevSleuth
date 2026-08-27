package com.devsleuth.experiment.entity;

import com.devsleuth.common.entity.BaseEntity;
import jakarta.persistence.*;

/**
 * Tracks experimental metrics: prompt variants, analyzer toggles, accuracy measurements.
 * ponytail: simple key-value for now; upgrade to structured experiment framework if A/B testing becomes formal.
 */
@Entity
@Table(name = "experiments")
public class Experiment extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "variant")
    private String variant;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "review_id")
    private java.util.UUID reviewId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public java.util.UUID getReviewId() { return reviewId; }
    public void setReviewId(java.util.UUID reviewId) { this.reviewId = reviewId; }
}
