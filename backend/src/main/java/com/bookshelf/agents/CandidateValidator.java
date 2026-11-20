package com.bookshelf.agents;

import java.util.Locale;
import java.util.Set;

/**
 * Deterministic sanity check on a candidate returned by the model. Drops entries that are
 * incomplete or filled with placeholder text instead of a real book (e.g. author "Unknown",
 * description "placeholder"). Pure logic with no collaborators.
 */
public final class CandidateValidator {

    private static final int MIN_TEXT_LENGTH = 15;
    private static final Set<String> PLACEHOLDERS = Set.of(
            "unknown", "n/a", "na", "none", "null", "tbd", "todo", "placeholder", "anonymous", "unknown author");

    private CandidateValidator() {
    }

    public static boolean isValid(BookCandidate candidate) {
        return candidate != null
                && isReal(candidate.getTitle(), 1)
                && isReal(candidate.getAuthor(), 1)
                && isReal(candidate.getDescription(), MIN_TEXT_LENGTH)
                && isReal(candidate.getWhySuggested(), MIN_TEXT_LENGTH);
    }

    private static boolean isReal(String value, int minLength) {
        if (value == null) {
            return false;
        }
        String text = value.trim().toLowerCase(Locale.ROOT);
        return text.length() >= minLength
                && !PLACEHOLDERS.contains(text.replaceAll("[\\p{Punct}\\s]+$", ""))
                && !text.startsWith("lorem ipsum");
    }
}
