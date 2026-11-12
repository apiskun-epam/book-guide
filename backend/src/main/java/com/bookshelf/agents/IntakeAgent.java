package com.bookshelf.agents;

import com.anthropic.client.AnthropicClient;
import com.bookshelf.config.ClaudeProperties;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ContentBlock;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntakeAgent {

    private final AnthropicClient anthropicClient;
    private final ClaudeProperties claudeProperties;
    private final ObjectMapper objectMapper;

    private static final int MAX_TOKENS = 1024;

    public IntakeResult analyze(String userPrompt) {
        try {
            String systemPrompt = loadPrompt("classpath:prompts/intake-agent.txt");
            
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(claudeProperties.model())
                    .maxTokens(MAX_TOKENS)
                    .system(systemPrompt)
                    .addUserMessage(userPrompt)
                    .build();

            Message response = anthropicClient.messages().create(params);
            
            String jsonResponse = extractTextContent(response.content());
            log.debug("Intake agent raw response: {}", jsonResponse);
            
            return objectMapper.readValue(jsonResponse, IntakeResult.class);
        } catch (Exception e) {
            log.error("Error in intake agent", e);
            throw new RuntimeException("Failed to analyze user prompt", e);
        }
    }

    private String loadPrompt(String path) throws IOException {
        // Remove "classpath:" prefix and load from resources
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
