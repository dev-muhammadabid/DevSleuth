package com.devsleuth.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Low-level GitHub API client. All other GitHub*Service classes delegate here.
 */
@Service
public class GitHubService {

    private static final String API_BASE = "https://api.github.com";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode get(String path, String accessToken) {
        HttpHeaders headers = buildHeaders(accessToken);
        ResponseEntity<String> response = restTemplate.exchange(
                API_BASE + path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return parse(response.getBody());
    }

    public JsonNode getAll(String path, String accessToken) {
        // ponytail: no pagination yet; GitHub returns 30 items by default, enough for V1 repo lists
        return get(path + "?per_page=100", accessToken);
    }

    public String getRaw(String path, String accessToken) {
        HttpHeaders headers = buildHeaders(accessToken);
        headers.setAccept(List.of(MediaType.valueOf("application/vnd.github.v3.diff")));
        ResponseEntity<String> response = restTemplate.exchange(
                API_BASE + path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return response.getBody();
    }

    private HttpHeaders buildHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        return headers;
    }

    private JsonNode parse(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse GitHub API response", e);
        }
    }
}
