package com.devsleuth.experiment.entity;

import com.devsleuth.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "experiments")
public class Experiment extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String dataset;

    @Column(columnDefinition = "TEXT")
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDataset() { return dataset; }
    public void setDataset(String dataset) { this.dataset = dataset; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
