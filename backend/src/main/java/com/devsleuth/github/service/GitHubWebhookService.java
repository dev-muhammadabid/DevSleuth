package com.devsleuth.github.service;

import com.devsleuth.common.exception.DevSleuthException;
import com.devsleuth.review.service.ReviewOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class GitHubWebhookService {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookService.class);

    private final String webhookSecret;
    private final ReviewOrchestrator reviewOrchestrator;

    public GitHubWebhookService(
            @Value("${devsleuth.github.webhook-secret}") String webhookSecret,
            ReviewOrchestrator reviewOrchestrator) {
        this.webhookSecret = webhookSecret;
        this.reviewOrchestrator = reviewOrchestrator;
    }

    public void process(String event, String signature, String payload) {
        verifySignature(payload, signature);

        if ("pull_request".equals(event)) {
            reviewOrchestrator.handlePullRequestEvent(payload);
        } else {
            log.debug("Ignoring event: {}", event);
        }
    }

    private void verifySignature(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Webhook secret not configured, skipping verification");
            return;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            if (!expected.equalsIgnoreCase(signature)) {
                throw new DevSleuthException("Invalid webhook signature", HttpStatus.UNAUTHORIZED);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new DevSleuthException("Signature verification failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
