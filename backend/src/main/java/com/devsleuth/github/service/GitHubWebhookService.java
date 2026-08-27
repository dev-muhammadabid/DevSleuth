package com.devsleuth.github.service;

import com.devsleuth.common.exception.DevSleuthException;
import com.devsleuth.config.GitHubProperties;
import com.devsleuth.pullrequest.service.PullRequestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class GitHubWebhookService {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GitHubProperties ghProps;
    private final PullRequestService pullRequestService;

    public GitHubWebhookService(GitHubProperties ghProps, PullRequestService pullRequestService) {
        this.ghProps = ghProps;
        this.pullRequestService = pullRequestService;
    }

    public void process(String event, String signature, String payload) {
        verifySignature(payload, signature);

        if ("pull_request".equals(event)) {
            handlePullRequestEvent(payload);
        } else {
            log.debug("Ignoring event: {}", event);
        }
    }

    @Async("analysisExecutor")
    public void handlePullRequestEvent(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String action = root.get("action").asText();
            if (!"opened".equals(action) && !"synchronize".equals(action)) {
                log.debug("Ignoring PR action: {}", action);
                return;
            }

            JsonNode pr = root.get("pull_request");
            String repoFullName = root.get("repository").get("full_name").asText();

            var info = new GitHubPullRequestService.GitHubPRInfo(
                    pr.get("id").asLong(),
                    pr.get("number").asInt(),
                    pr.get("title").asText(),
                    pr.get("user").get("login").asText(),
                    pr.get("head").get("ref").asText(),
                    pr.get("base").get("ref").asText(),
                    pr.get("head").get("sha").asText(),
                    pr.get("state").asText()
            );

            pullRequestService.handleWebhookPR(repoFullName, info);
        } catch (Exception e) {
            log.error("Failed to handle PR webhook event", e);
        }
    }

    public void verifySignature(String payload, String signature) {
        String secret = ghProps.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("Webhook secret not configured, skipping verification");
            return;
        }
        if (signature == null || signature.isBlank()) {
            throw new DevSleuthException("Missing webhook signature", HttpStatus.UNAUTHORIZED);
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            // Constant-time comparison to avoid leaking the signature via timing.
            byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
            byte[] providedBytes = signature.getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expectedBytes, providedBytes)) {
                throw new DevSleuthException("Invalid webhook signature", HttpStatus.UNAUTHORIZED);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new DevSleuthException("Signature verification failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
