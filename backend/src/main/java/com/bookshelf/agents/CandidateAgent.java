package com.bookshelf.agents;

import com.anthropic.client.AnthropicClient;
import com.bookshelf.config.ClaudeProperties;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.TextDelta;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateAgent {

    private final AnthropicClient anthropicClient;
    private final ClaudeProperties claudeProperties;
    private final ObjectMapper objectMapper;

    private static final int MAX_TOKENS = 2048;

    public CandidateResult generateCandidates(IntakeResult intakeResult, PreferenceResult preferenceResult) {
        try {
            String systemPrompt = loadPrompt("classpath:prompts/candidate-agent.txt");
            String userMessage = buildUserMessage(intakeResult, preferenceResult);
            
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(claudeProperties.model())
                    .maxTokens(MAX_TOKENS)
                    .system(systemPrompt)
                    .addUserMessage(userMessage)
                    .build();

            Message response = anthropicClient.messages().create(params);
            
            String jsonResponse = extractTextContent(response.content());
            log.debug("Candidate agent raw response: {}", jsonResponse);
            
            return objectMapper.readValue(jsonResponse, CandidateResult.class);
        } catch (Exception e) {
            log.error("Error in candidate agent", e);
            throw new RuntimeException("Failed to generate book candidates", e);
        }
    }

    /**
     * Streams candidates from Claude, invoking {@code onCandidate} for each book as soon as its
     * JSON object is complete, rather than waiting for the whole response.
     */
    public List<BookCandidate> streamCandidates(IntakeResult intakeResult,
                                                PreferenceResult preferenceResult,
                                                Consumer<BookCandidate> onCandidate) {
        try {
            String systemPrompt = loadPrompt("classpath:prompts/candidate-agent.txt");
            String userMessage = buildUserMessage(intakeResult, preferenceResult);

            MessageCreateParams params = MessageCreateParams.builder()
                    .model(claudeProperties.model())
                    .maxTokens(MAX_TOKENS)
                    .system(systemPrompt)
                    .addUserMessage(userMessage)
                    .build();

            CandidateStreamParser parser = new CandidateStreamParser();
            List<BookCandidate> candidates = new ArrayList<>();

            try (StreamResponse<RawMessageStreamEvent> stream =
                         anthropicClient.messages().createStreaming(params)) {
                stream.stream()
                        .flatMap(event -> event.contentBlockDelta().stream())
                        .flatMap(deltaEvent -> deltaEvent.delta().text().stream())
                        .map(TextDelta::text)
                        .forEach(chunk -> {
                            for (String json : parser.feed(chunk)) {
                                try {
                                    BookCandidate candidate = objectMapper.readValue(json, BookCandidate.class);
                                    candidates.add(candidate);
                                    onCandidate.accept(candidate);
                                } catch (IOException e) {
                                    log.warn("Skipping malformed streamed candidate", e);
                                }
                            }
                        });
            }
            return candidates;
        } catch (Exception e) {
            log.error("Error streaming candidate agent", e);
            throw new RuntimeException("Failed to generate book candidates", e);
        }
    }

    private String buildUserMessage(IntakeResult intake, PreferenceResult preference) throws IOException {
        String intakeJson = objectMapper.writeValueAsString(intake);
        String preferenceJson = objectMapper.writeValueAsString(preference);
        
        return String.format(
                "User's current request:\n%s\n\nUser's historical preferences:\n%s",
                intakeJson,
                preferenceJson
        );
    }

    private String loadPrompt(String path) throws IOException {
        String resourcePath = path.replace("classpath:", "");
        return new String(getClass().getClassLoader()
                .getResourceAsStream(resourcePath)
                .readAllBytes());
    }

    private String extractTextContent(List<ContentBlock> content) {
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Empty response from Claude API");
        }

        ContentBlock firstBlock = content.get(0);
        if (firstBlock.isText()) {
            return firstBlock.asText().text();
        }

        throw new RuntimeException("No text content in Claude response");
    }
}
