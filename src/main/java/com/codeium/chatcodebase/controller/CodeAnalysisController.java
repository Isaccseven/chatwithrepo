package com.codeium.chatcodebase.controller;

import com.codeium.chatcodebase.service.ChatService;
import com.codeium.chatcodebase.service.CodeAnalysisOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CodeAnalysisController {
    private final CodeAnalysisOrchestrator orchestrator;
    private final ChatService chatService;

    public CodeAnalysisController(CodeAnalysisOrchestrator orchestrator, ChatService chatService) {
        this.orchestrator = orchestrator;
        this.chatService = chatService;
    }

    @PostMapping("/analyze/{projectId}")
    public ResponseEntity<Void> startAnalysis(@PathVariable String projectId) {
        orchestrator.analyzeRepository(projectId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/analyze/{projectId}/status")
    public ResponseEntity<CodeAnalysisOrchestrator.AnalysisStatus> getAnalysisStatus(
            @PathVariable String projectId) {
        return ResponseEntity.ok(orchestrator.getAnalysisStatus(projectId));
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String response = chatService.chatWithContext(request.query());
        return ResponseEntity.ok(new ChatResponse(response));
    }

    public record ChatRequest(String query) {}
    public record ChatResponse(String response) {}
}
