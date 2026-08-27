package com.devsleuth.experiment.entity;

import com.devsleuth.analysis.model.AnalysisInput.FileChange;
import com.devsleuth.common.entity.BaseEntity;
import com.devsleuth.experiment.model.GroundTruthEntry;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "experiments")
public class Experiment extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Structured file-change dataset stored as JSONB (Hibernate 6 native JSON support).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<FileChange> dataset;

    // Expected findings used to score a run, stored as JSONB.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ground_truth", columnDefinition = "jsonb")
    private List<GroundTruthEntry> groundTruth;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<FileChange> getDataset() { return dataset; }
    public void setDataset(List<FileChange> dataset) { this.dataset = dataset; }
    public List<GroundTruthEntry> getGroundTruth() { return groundTruth; }
    public void setGroundTruth(List<GroundTruthEntry> groundTruth) { this.groundTruth = groundTruth; }
}
