package com.bookshelf.agents;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateStreamParserTest {

    private static final String JSON =
            "{\"candidates\":[{\"title\":\"A {braced} \\\"quoted\\\" title\",\"author\":\"X\"},"
                    + "{\"title\":\"B\",\"author\":\"Y\"}]}";

    @Test
    void emitsEachObjectOnceItIsComplete() {
        CandidateStreamParser parser = new CandidateStreamParser();
        int split = JSON.indexOf("{\"title\":\"B\"") + 5;

        List<String> first = parser.feed(JSON.substring(0, split));
        List<String> second = parser.feed(JSON.substring(split));

        assertEquals(1, first.size());
        assertTrue(first.get(0).contains("A {braced}"));
        assertEquals(1, second.size());
        assertEquals("{\"title\":\"B\",\"author\":\"Y\"}", second.get(0));
    }

    @Test
    void handlesOneCharacterChunks() {
        CandidateStreamParser parser = new CandidateStreamParser();
        List<String> all = new ArrayList<>();
        for (char c : JSON.toCharArray()) {
            all.addAll(parser.feed(String.valueOf(c)));
        }
        assertEquals(2, all.size());
    }

    @Test
    void emitsNothingForIncompleteObject() {
        CandidateStreamParser parser = new CandidateStreamParser();
        assertTrue(parser.feed("{\"candidates\":[{\"title\":\"A\"").isEmpty());
    }
}
