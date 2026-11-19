package com.bookshelf.agents;

import com.bookshelf.books.Book;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Deterministic guard that drops suggestions the user already has in either list.
 * Runs in Java after the candidate agent so exclusion never depends on prompt instructions.
 * Books match by normalized ISBN or by normalized title + author.
 */
@Component
public class ExclusionGuard {

    /** Returns a predicate that is true for candidates NOT already owned by the user. */
    public Predicate<BookCandidate> allowedAgainst(List<Book> ownedBooks) {
        return allowedAgainst(ownedBooks, List.of());
    }

    /**
     * Like {@link #allowedAgainst(List)}, but also rejects books already proposed earlier in the
     * current conversation, so follow-up requests never repeat a recommendation.
     */
    public Predicate<BookCandidate> allowedAgainst(List<Book> ownedBooks, List<BookRef> alreadySuggested) {
        Set<String> isbns = new HashSet<>();
        Set<String> titleAuthors = new HashSet<>();
        for (Book book : ownedBooks) {
            register(isbns, titleAuthors, book.getIsbn(), book.getTitle(), book.getAuthor());
        }
        for (BookRef ref : alreadySuggested == null ? List.<BookRef>of() : alreadySuggested) {
            register(isbns, titleAuthors, ref.getIsbn(), ref.getTitle(), ref.getAuthor());
        }

        return candidate -> {
            String isbn = normalizeIsbn(candidate.getIsbn());
            if (!isbn.isEmpty() && isbns.contains(isbn)) {
                return false;
            }
            return !titleAuthors.contains(titleAuthorKey(candidate.getTitle(), candidate.getAuthor()));
        };
    }

    /**
     * Filter for one streamed reply: rejects owned books and books proposed earlier in the
     * conversation, and remembers every book it accepts so the same reply never repeats one either.
     * Not thread-safe; use one instance per request.
     */
    public Predicate<BookCandidate> newSuggestionFilter(List<Book> ownedBooks, List<BookRef> alreadySuggested) {
        Predicate<BookCandidate> notOwnedOrProposed = allowedAgainst(ownedBooks, alreadySuggested);
        Set<String> acceptedIsbns = new HashSet<>();
        Set<String> acceptedTitleAuthors = new HashSet<>();

        return candidate -> {
            if (!notOwnedOrProposed.test(candidate)) {
                return false;
            }
            String isbn = normalizeIsbn(candidate.getIsbn());
            String key = titleAuthorKey(candidate.getTitle(), candidate.getAuthor());
            if ((!isbn.isEmpty() && acceptedIsbns.contains(isbn)) || acceptedTitleAuthors.contains(key)) {
                return false;
            }
            register(acceptedIsbns, acceptedTitleAuthors, candidate.getIsbn(), candidate.getTitle(), candidate.getAuthor());
            return true;
        };
    }

    private static void register(Set<String> isbns, Set<String> titleAuthors,
                                 String isbn, String title, String author) {
        String normalized = normalizeIsbn(isbn);
        if (!normalized.isEmpty()) {
            isbns.add(normalized);
        }
        titleAuthors.add(titleAuthorKey(title, author));
    }

    public List<BookCandidate> filter(List<BookCandidate> candidates, List<Book> ownedBooks) {
        Predicate<BookCandidate> allowed = allowedAgainst(ownedBooks);
        return candidates.stream().filter(allowed).toList();
    }

    static String normalizeIsbn(String isbn) {
        return isbn == null ? "" : isbn.replaceAll("[^0-9Xx]", "").toUpperCase(Locale.ROOT);
    }

    static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    private static String titleAuthorKey(String title, String author) {
        return normalizeText(title) + "|" + normalizeText(author);
    }
}
