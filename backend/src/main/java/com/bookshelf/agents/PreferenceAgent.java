package com.bookshelf.agents;

import com.anthropic.client.AnthropicClient;
import com.bookshelf.config.ClaudeProperties;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ContentBlock;
import com.bookshelf.books.Book;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreferenceAgent {

    private final AnthropicClient anthropicClient;
    private final ClaudeProperties claudeProperties;
    private final ObjectMapper objectMapper;

    private static final int MAX_TOKENS = 1024;

    public PreferenceResult analyze(List<Book> userBooks) {
        try {
            String systemPrompt = loadPrompt("classpath:prompts/preference-agent.txt");
            String userCollection = formatUserCollection(userBooks);
            
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(claudeProperties.model())
                    .maxTokens(MAX_TOKENS)
                    .system(systemPrompt)
                    .addUserMessage(userCollection)
                    .build();

            Message response = anthropicClient.messages().create(params);
            
            String jsonResponse = extractTextContent(response.content());
            log.debug("Preference agent raw response: {}", jsonResponse);
            
            return objectMapper.readValue(jsonResponse, PreferenceResult.class);
        } catch (Exception e) {
            log.error("Error in preference agent", e);
            throw new RuntimeException("Failed to analyze user preferences", e);
        }
    }

    private String formatUserCollection(List<Book> books) {
        if (books.isEmpty()) {
            return "User has no books in their collection yet.";
        }
        
        return books.stream()
                .map(book -> String.format(
                        "Title: %s\nAuthor: %s\nISBN: %s\nRating: %s/5\nDescription: %s\n",
                        book.getTitle(),
                        book.getAuthor() != null ? book.getAuthor() : "Unknown",
                        book.getIsbn() != null ? book.getIsbn() : "N/A",
                        book.getRating() != null ? book.getRating() : "Not rated",
                        book.getDescription() != null ? book.getDescription() : "No description"
                ))
                .collect(Collectors.joining("\n---\n"));
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
