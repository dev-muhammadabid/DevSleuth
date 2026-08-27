package com.devsleuth.github.entity;

import com.devsleuth.auth.entity.User;
import com.devsleuth.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "github_connections")
public class GitHubConnection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "installation_id", nullable = false)
    private Long installationId;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "token_expires_at")
    private java.time.Instant tokenExpiresAt;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Long getInstallationId() { return installationId; }
    public void setInstallationId(Long installationId) { this.installationId = installationId; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public java.time.Instant getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(java.time.Instant tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }
}
