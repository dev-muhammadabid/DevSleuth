package com.devsleuth.auth.service;

import com.devsleuth.auth.entity.User;
import com.devsleuth.auth.repository.UserRepository;
import com.devsleuth.common.exception.DevSleuthException;
import com.devsleuth.config.GitHubProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String GITHUB_AUTH_URL = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_USER_URL = "https://api.github.com/user";

    private final GitHubProperties ghProps;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AuthService(GitHubProperties ghProps, UserRepository userRepository) {
        this.ghProps = ghProps;
        this.userRepository = userRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String buildAuthorizationUrl() {
        String clientId = ghProps.getClientId();
        if (clientId == null || clientId.isBlank()) {
            // Without this guard the URL would be built with an empty client_id and
            // GitHub responds with a 404 page instead of the login screen.
            throw new DevSleuthException(
                    "GitHub OAuth is not configured. Set GITHUB_CLIENT_ID (and GITHUB_CLIENT_SECRET).",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return GITHUB_AUTH_URL
                + "?client_id=" + clientId
                + "&scope=read:user,user:email,repo";
    }

    public User handleOAuthCallback(String code) {
        String accessToken = exchangeCodeForToken(code);
        JsonNode profile = fetchGitHubUser(accessToken);

        Long githubUserId = profile.get("id").asLong();
        String username = profile.get("login").asText();
        String email = profile.has("email") && !profile.get("email").isNull()
                ? profile.get("email").asText() : null;
        String avatarUrl = profile.has("avatar_url") ? profile.get("avatar_url").asText() : null;

        User user = userRepository.findByGithubUserId(githubUserId)
                .orElseGet(() -> {
                    User u = new User();
                    u.setGithubUserId(githubUserId);
                    return u;
                });

        user.setUsername(username);
        user.setEmail(email);
        user.setAvatarUrl(avatarUrl);
        user.setAccessToken(accessToken);
        return userRepository.save(user);
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    private String exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
                "client_id", ghProps.getClientId(),
                "client_secret", ghProps.getClientSecret(),
                "code", code
        );

        ResponseEntity<String> response = restTemplate.exchange(
                GITHUB_TOKEN_URL,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );

        JsonNode json;
        try {
            json = objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new DevSleuthException("Unexpected response from GitHub during token exchange",
                    HttpStatus.BAD_GATEWAY);
        }

        JsonNode token = json.get("access_token");
        if (token == null || token.isNull()) {
            // GitHub returns HTTP 200 with an error body (not access_token) when the
            // client_id/secret don't match or the code is invalid/expired. Surface it
            // instead of NPE-ing on a null node.
            String error = json.hasNonNull("error") ? json.get("error").asText() : "unknown_error";
            String description = json.hasNonNull("error_description")
                    ? json.get("error_description").asText() : "";
            log.warn("GitHub token exchange failed: error='{}', description='{}'", error, description);
            throw new DevSleuthException(
                    "GitHub token exchange failed (" + error + ")"
                            + (description.isEmpty() ? "" : ": " + description),
                    HttpStatus.BAD_GATEWAY);
        }
        return token.asText();
    }

    private JsonNode fetchGitHubUser(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = restTemplate.exchange(
                GITHUB_USER_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch GitHub user", e);
        }
    }
}
