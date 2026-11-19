package com.bookshelf.agents;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.bookshelf.books.Book;
import com.bookshelf.config.ClaudeProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/** Ranks books from the user's Reading List against the current request. */
@Service
@RequiredArgsConstructor
@Slf4j
public class RankingAgent {

    private static final int MAX_TOKENS = 1024;
    private static final int MAX_DESCRIPTION_CHARS = 300;

    private final AnthropicClient anthropicClient;
    private final ClaudeProperties claudeProperties;
    private final ObjectMapper objectMapper;

    /** Calls Claude once, and once more if the first attempt fails or returns unparseable output. */
    public RankingResult rank(IntakeResult intake, PreferenceResult preference, List<Book> backlog) {
        try {
            String systemPrompt = loadPrompt();
            String userMessage = buildUserMessage(intake, preference, backlog);
            try {
                return callOnce(systemPrompt, userMessage);
            } catch (Exception first) {
                log.warn("Ranking agent attempt failed, retrying once", first);
                return callOnce(systemPrompt, userMessage);
            }
        } catch (Exception e) {
            log.error("Error in ranking agent", e);
            throw new RuntimeException("Failed to rank reading list", e);
        }
    }

    private RankingResult callOnce(String systemPrompt, String userMessage) throws IOException {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(claudeProperties.model())
                .maxTokens(MAX_TOKENS)
                .system(systemPrompt)
                .addUserMessage(userMessage)
                .build();
        Message response = anthropicClient.messages().create(params);
        String json = stripCodeFence(extractTextContent(response));
        return objectMapper.readValue(json, RankingResult.class);
    }

    private String buildUserMessage(IntakeResult intake, PreferenceResult preference, List<Book> backlog)
            throws IOException {
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < backlog.size(); i++) {
            Book book = backlog.get(i);
            list.append(BacklogGuard.refFor(i)).append(": ").append(book.getTitle())
                    .append(" by ").append(book.getAuthor() != null ? book.getAuthor() : "Unknown");
            if (book.getDescription() != null && !book.getDescription().isBlank()) {
                String description = book.getDescription();
                if (description.length() > MAX_DESCRIPTION_CHARS) {
                    description = description.substring(0, MAX_DESCRIPTION_CHARS) + "...";
                }
                list.append(" - ").append(description.replace('\n', ' '));
            }
            list.append('\n');
        }
        return String.format("User's current request:\n%s\n\nUser's historical preferences:\n%s\n\nReading List:\n%s",
                objectMapper.writeValueAsString(intake),
                objectMapper.writeValueAsString(preference),
                list);
    }

    static String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private String loadPrompt() throws IOException {
        return new String(getClass().getClassLoader()
                .getResourceAsStream("prompts/ranking-agent.txt")
                .readAllBytes());
    }

    /** Joins the reply's text blocks; the model may put other block types (e.g. thinking) first. */
    private String extractTextContent(Message response) {
        List<ContentBlock> content = response.content();
        if (content != null) {
            String text = content.stream()
                    .filter(ContentBlock::isText)
                    .map(block -> block.asText().text())
                    .collect(Collectors.joining());
            if (!text.isBlank()) {
                return text;
            }
        }
        log.warn("Ranking agent reply had no text; stop reason: {}, block types: {}",
                response.stopReason(),
                content == null ? List.of() : content.stream()
                        .map(block -> block.isText() ? "text" : block.isThinking() ? "thinking"
                                : block.isToolUse() ? "tool_use" : "other")
                        .toList());
        throw new RuntimeException("No text content in Claude response");
    }
}
