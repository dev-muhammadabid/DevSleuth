package com.devsleuth.repository.entity;

import com.devsleuth.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "repositories")
public class Repository extends BaseEntity {

    @Column(name = "github_repository_id", unique = true, nullable = false)
    private Long githubRepositoryId;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String name;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "default_branch")
    private String defaultBranch;

    private String language;

    public Long getGithubRepositoryId() { return githubRepositoryId; }
    public void setGithubRepositoryId(Long githubRepositoryId) { this.githubRepositoryId = githubRepositoryId; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
