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
        Set<String> isbns = new HashSet<>();
        Set<String> titleAuthors = new HashSet<>();
        for (Book book : ownedBooks) {
            String isbn = normalizeIsbn(book.getIsbn());
            if (!isbn.isEmpty()) {
                isbns.add(isbn);
            }
            titleAuthors.add(titleAuthorKey(book.getTitle(), book.getAuthor()));
        }

        return candidate -> {
            String isbn = normalizeIsbn(candidate.getIsbn());
            if (!isbn.isEmpty() && isbns.contains(isbn)) {
                return false;
            }
            return !titleAuthors.contains(titleAuthorKey(candidate.getTitle(), candidate.getAuthor()));
        };
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
