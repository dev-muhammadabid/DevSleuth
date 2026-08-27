package com.devsleuth.finding.controller;

import com.devsleuth.common.security.AccessGuard;
import com.devsleuth.finding.dto.FindingResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/findings")
public class FindingDetailController {

    private final AccessGuard accessGuard;

    public FindingDetailController(AccessGuard accessGuard) {
        this.accessGuard = accessGuard;
    }

    @GetMapping("/{id}")
    public ResponseEntity<FindingResponse> getFinding(@PathVariable UUID id, HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        return ResponseEntity.ok(FindingResponse.from(accessGuard.requireFinding(id, userId)));
    }
}
