package com.devsleuth.auth.entity;

import com.devsleuth.common.entity.BaseEntity;
import com.devsleuth.common.security.EncryptedStringConverter;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "github_user_id", unique = true, nullable = false)
    private Long githubUserId;

    @Column(nullable = false)
    private String username;

    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "access_token", length = 512)
    @Convert(converter = EncryptedStringConverter.class)
    private String accessToken;

    public Long getGithubUserId() { return githubUserId; }
    public void setGithubUserId(Long githubUserId) { this.githubUserId = githubUserId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}
