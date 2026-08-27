package com.devsleuth.auth.controller;

import com.devsleuth.auth.dto.UserResponse;
import com.devsleuth.auth.entity.User;
import com.devsleuth.auth.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Returns the GitHub OAuth authorization URL.
     * Frontend redirects the user here.
     */
    @GetMapping("/github")
    public ResponseEntity<Map<String, String>> getAuthUrl() {
        String url = authService.buildAuthorizationUrl();
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * GitHub redirects here after user authorizes.
     * Exchanges code for token, creates/updates user, sets session.
     */
    @GetMapping("/github/callback")
    public ResponseEntity<UserResponse> handleCallback(
            @RequestParam String code,
            HttpSession session) {
        User user = authService.handleOAuthCallback(code);
        session.setAttribute("userId", user.getId());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Returns the currently authenticated user, or 401.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return authService.findById(userId)
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(401).build());
    }

    /**
     * Logs out the current user.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }
}
