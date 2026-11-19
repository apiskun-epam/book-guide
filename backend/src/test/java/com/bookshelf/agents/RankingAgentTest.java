package com.bookshelf.agents;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.bookshelf.books.Book;
import com.bookshelf.books.ListType;
import com.bookshelf.config.ClaudeProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingAgentTest {

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS) private AnthropicClient client;
    @Mock private ClaudeProperties properties;

    private RankingAgent agent;
    private final List<Book> backlog = List.of(
            Book.builder().title("Emma").author("Jane Austen").listType(ListType.READING_LIST).build());

    @BeforeEach
    void setUp() {
        agent = new RankingAgent(client, properties, new ObjectMapper());
        lenient().when(properties.model()).thenReturn("test-model");
    }

    private static Message reply(String text) {
        TextBlock textBlock = mock(TextBlock.class);
        when(textBlock.text()).thenReturn(text);
        ContentBlock block = mock(ContentBlock.class);
        when(block.isText()).thenReturn(true);
        when(block.asText()).thenReturn(textBlock);
        Message message = mock(Message.class);
        when(message.content()).thenReturn(List.of(block));
        return message;
    }

    @Test
    void parsesPicksFromModelJson() {
        Message ok = reply("{\"picks\":[{\"ref\":\"b1\",\"reason\":\"Light and short\"}]}");
        when(client.messages().create(any(MessageCreateParams.class))).thenReturn(ok);

        RankingResult result = agent.rank(new IntakeResult(), new PreferenceResult(), backlog);

        assertEquals("b1", result.getPicks().get(0).getRef());
        assertEquals("Light and short", result.getPicks().get(0).getReason());
    }

    @Test
    void readsTheTextBlockWhenAnotherBlockComesFirst() {
        ContentBlock thinking = mock(ContentBlock.class);
        when(thinking.isText()).thenReturn(false);
        Message ok = reply("{\"picks\":[{\"ref\":\"b1\",\"reason\":\"Short\"}]}");
        List<ContentBlock> blocks = new java.util.ArrayList<>(ok.content());
        blocks.add(0, thinking);
        when(ok.content()).thenReturn(blocks);
        when(client.messages().create(any(MessageCreateParams.class))).thenReturn(ok);

        RankingResult result = agent.rank(new IntakeResult(), new PreferenceResult(), backlog);

        assertEquals("b1", result.getPicks().get(0).getRef());
    }

    @Test
    void retriesOnceWhenFirstReplyIsNotJson() {
        Message bad = reply("sorry, no JSON");
        Message ok = reply("{\"picks\":[{\"ref\":\"b1\",\"reason\":\"ok\"}]}");
        when(client.messages().create(any(MessageCreateParams.class))).thenReturn(bad).thenReturn(ok);

        RankingResult result = agent.rank(new IntakeResult(), new PreferenceResult(), backlog);

        assertEquals(1, result.getPicks().size());
        verify(client.messages(), times(2)).create(any(MessageCreateParams.class));
    }

    @Test
    void failsAfterTheRetryIsExhausted() {
        Message bad = reply("not json");
        when(client.messages().create(any(MessageCreateParams.class))).thenReturn(bad);

        assertThrows(RuntimeException.class,
                () -> agent.rank(new IntakeResult(), new PreferenceResult(), backlog));
    }

    @Test
    void stripsMarkdownCodeFence() {
        assertEquals("{\"picks\":[]}", RankingAgent.stripCodeFence("```json\n{\"picks\":[]}\n```"));
    }
}
