package com.bookshelf.agents;

import java.util.ArrayList;
import java.util.List;

/**
 * Incrementally extracts complete candidate objects from streamed JSON of the form
 * {"candidates":[{...},{...}]}. Feed text chunks as they arrive; each call returns the
 * candidate object strings that were completed by that chunk.
 */
class CandidateStreamParser {

    private final StringBuilder current = new StringBuilder();
    private int depth = 0;
    private boolean inString = false;
    private boolean escaped = false;

    List<String> feed(String chunk) {
        List<String> completed = new ArrayList<>();
        for (int i = 0; i < chunk.length(); i++) {
            char c = chunk.charAt(i);

            if (depth >= 2) {
                current.append(c);
            }

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
                if (depth == 2) {
                    current.setLength(0);
                    current.append(c);
                }
            } else if (c == '}') {
                if (depth == 2) {
                    completed.add(current.toString());
                    current.setLength(0);
                }
                depth--;
            }
        }
        return completed;
    }
}
