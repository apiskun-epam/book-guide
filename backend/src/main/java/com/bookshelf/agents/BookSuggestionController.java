package com.bookshelf.agents;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
@Slf4j
public class BookSuggestionController {

    private final BookSuggestionOrchestrator orchestrator;

    /**
     * Generate personalized book suggestions based on user's prompt and reading history.
     * 
     * POST /api/suggestions
     * Request body: { "prompt": "I want something light and fun" }
     * 
     * @param request The user's book request
     * @return SuggestionResponse with 5 book candidates
     */
    @PostMapping
    public ResponseEntity<SuggestionResponse> getSuggestions(@Valid @RequestBody SuggestionRequest request) {
        // TODO: Replace with authenticated user ID from Firebase token
        String userId = "demo-user";
        
        log.info("Received suggestion request from user: {}", userId);
        log.debug("User prompt: {}", request.getPrompt());
        
        SuggestionResponse response = orchestrator.generateSuggestions(userId, request.getPrompt());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Streams suggestions as Server-Sent Events.
     *
     * POST /api/suggestions/stream
     * Events: "message" (intro text), "suggestion" (one BookCandidate each), "done", "error".
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSuggestions(@Valid @RequestBody SuggestionRequest request) {
        // TODO: Replace with authenticated user ID from Firebase token
        String userId = "demo-user";
        String prompt = request.getPrompt();

        SseEmitter emitter = new SseEmitter(120_000L);

        CompletableFuture.runAsync(() -> {
            try {
                orchestrator.streamSuggestions(userId, prompt, new SuggestionListener() {
                    @Override
                    public void onMessage(String message) {
                        send(emitter, "message", message);
                    }

                    @Override
                    public void onSuggestion(BookCandidate candidate) {
                        send(emitter, "suggestion", candidate);
                    }
                });
                send(emitter, "done", "done");
                emitter.complete();
            } catch (Exception e) {
                log.error("Streaming suggestions failed for user: {}", userId, e);
                try {
                    send(emitter, "error", "Failed to generate book suggestions. Please try again.");
                } finally {
                    emitter.complete();
                }
            }
        });

        return emitter;
    }

    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException e) {
            throw new IllegalStateException("Client disconnected", e);
        }
    }
}
