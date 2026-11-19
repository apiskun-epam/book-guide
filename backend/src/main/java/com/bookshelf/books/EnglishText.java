package com.bookshelf.books;

import java.util.Locale;
import java.util.Set;

/**
 * Cheap heuristic for "is this prose English?". Open Library work descriptions are not language-tagged and
 * can be in the language of the work's first edition, so we check for common English function words.
 * English prose is dominated by them; Polish, German, French and similar text contains almost none.
 */
final class EnglishText {

    // Deliberately excludes words that are also very common in other languages (a, to, in, on, by, i, die, den...).
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "of", "is", "was", "with", "his", "her", "who", "that", "for", "he", "she", "they",
            "their", "are", "from", "has", "had", "as", "at", "it", "this", "an", "be", "but", "not", "when",
            "which", "have", "will", "into", "its", "one", "them", "after", "only");

    private static final int MIN_WORDS_TO_JUDGE = 4;
    private static final double MIN_STOP_WORD_RATIO = 0.08;

    private EnglishText() {
    }

    static boolean looksEnglish(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String[] words = text.toLowerCase(Locale.ROOT).split("[^\\p{L}']+");
        int total = 0;
        int stop = 0;
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            total++;
            if (STOP_WORDS.contains(word)) {
                stop++;
            }
        }
        if (total < MIN_WORDS_TO_JUDGE) {
            return true; // too short to judge; accept rather than hide a one-line blurb
        }
        return (double) stop / total >= MIN_STOP_WORD_RATIO;
    }
}
