package com.devsleuth.github.controller;

import com.devsleuth.github.service.GitHubWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class GitHubWebhookController {

    private final GitHubWebhookService webhookService;

    public GitHubWebhookController(GitHubWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * GitHub posts here on PR events. We verify, parse, queue a job, and return 200 immediately.
     */
    @PostMapping("/github")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader("X-GitHub-Event") String event,
            @RequestBody String payload) {

        webhookService.process(event, signature, payload);
        return ResponseEntity.ok().build();
    }
}
