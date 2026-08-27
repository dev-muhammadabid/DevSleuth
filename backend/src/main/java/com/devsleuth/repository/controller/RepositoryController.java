package com.devsleuth.repository.controller;

import com.devsleuth.auth.entity.User;
import com.devsleuth.auth.service.AuthService;
import com.devsleuth.repository.dto.RepositoryResponse;
import com.devsleuth.repository.entity.Repository;
import com.devsleuth.repository.service.RepositoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final AuthService authService;

    public RepositoryController(RepositoryService repositoryService, AuthService authService) {
        this.repositoryService = repositoryService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<RepositoryResponse>> list(HttpSession session) {
        User user = getUser(session);
        if (user == null) return ResponseEntity.status(401).build();

        List<Repository> repos = repositoryService.listRepositories(user);
        return ResponseEntity.ok(repos.stream().map(RepositoryResponse::from).toList());
    }

    @PostMapping("/{id}/connect")
    public ResponseEntity<RepositoryResponse> connect(@PathVariable UUID id, HttpSession session) {
        User user = getUser(session);
        if (user == null) return ResponseEntity.status(401).build();

        Repository repo = repositoryService.connect(id, user);
        return ResponseEntity.ok(RepositoryResponse.from(repo));
    }

    private User getUser(HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) return null;
        return authService.findById(userId).orElse(null);
    }
}
