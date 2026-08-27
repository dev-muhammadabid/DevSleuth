package com.devsleuth.finding.controller;

import com.devsleuth.common.security.AccessGuard;
import com.devsleuth.finding.dto.CalibrationResponse;
import com.devsleuth.finding.dto.ChatRequest;
import com.devsleuth.finding.dto.ChatResponse;
import com.devsleuth.finding.dto.FindingResponse;
import com.devsleuth.finding.dto.VerdictRequest;
import com.devsleuth.finding.entity.Finding;
import com.devsleuth.finding.service.CalibrationService;
import com.devsleuth.finding.service.FindingChatService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/findings")
public class FindingDetailController {

    private final AccessGuard accessGuard;
    private final FindingChatService chatService;
    private final CalibrationService calibrationService;

    public FindingDetailController(AccessGuard accessGuard,
                                   FindingChatService chatService,
                                   CalibrationService calibrationService) {
        this.accessGuard = accessGuard;
        this.chatService = chatService;
        this.calibrationService = calibrationService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<FindingResponse> getFinding(@PathVariable UUID id, HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        return ResponseEntity.ok(FindingResponse.from(accessGuard.requireFinding(id, userId)));
    }

    @PostMapping("/{id}/chat")
    public ResponseEntity<ChatResponse> chat(
            @PathVariable UUID id,
            @Valid @RequestBody ChatRequest request,
            HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        Finding finding = accessGuard.requireFinding(id, userId);
        String answer = chatService.chat(finding, request.question());
        return ResponseEntity.ok(new ChatResponse(answer));
    }

    @PostMapping("/{id}/verdict")
    public ResponseEntity<Void> submitVerdict(
            @PathVariable UUID id,
            @Valid @RequestBody VerdictRequest request,
            HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        accessGuard.requireFinding(id, userId);
        calibrationService.submitVerdict(id, request.verdict());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/calibration")
    public ResponseEntity<CalibrationResponse> getCalibration(HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        return ResponseEntity.ok(calibrationService.getCalibrationStats(userId));
    }
}
