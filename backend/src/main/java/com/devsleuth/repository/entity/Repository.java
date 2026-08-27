package com.devsleuth.repository.entity;

import com.devsleuth.auth.entity.User;
import com.devsleuth.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

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

    @Column(nullable = false)
    private boolean connected;

    /**
     * DevSleuth users who have access to this repository (synced it from their own
     * GitHub account). Access control is membership in this set, not a single owner.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "repository_members",
            joinColumns = @JoinColumn(name = "repository_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();

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
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
    public Set<User> getMembers() { return members; }
    public void setMembers(Set<User> members) { this.members = members; }
}
