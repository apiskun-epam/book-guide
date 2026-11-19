package com.bookshelf.agents;

import com.bookshelf.books.Book;
import com.bookshelf.books.ListType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExclusionGuardTest {

    private final ExclusionGuard guard = new ExclusionGuard();

    private static Book owned(String title, String author, String isbn, ListType list) {
        return Book.builder().title(title).author(author).isbn(isbn).listType(list).build();
    }

    private static BookCandidate candidate(String title, String author, String isbn) {
        return new BookCandidate(title, author, isbn, null, null, null, null);
    }

    @Test
    void removesExactMatch() {
        List<Book> owned = List.of(owned("Dune", "Frank Herbert", "9780441013593", ListType.COLLECTION));

        assertTrue(guard.filter(List.of(candidate("Dune", "Frank Herbert", "9780441013593")), owned).isEmpty());
    }

    @Test
    void removesCaseAndPunctuationDifferences() {
        List<Book> owned = List.of(owned("The Hobbit", "J.R.R. Tolkien", null, ListType.COLLECTION));

        assertTrue(guard.filter(List.of(candidate("the  HOBBIT!", "j r r tolkien", null)), owned).isEmpty());
    }

    @Test
    void removesIsbnMatchEvenWhenTitleDiffersAndHyphensPresent() {
        List<Book> owned = List.of(owned("Dune", "Frank Herbert", "978-0-441-01359-3", ListType.COLLECTION));

        assertTrue(guard.filter(List.of(candidate("Dune (Deluxe)", "F. Herbert", "9780441013593")), owned).isEmpty());
    }

    @Test
    void keepsBooksWithNoMatch() {
        List<Book> owned = List.of(owned("Dune", "Frank Herbert", "111", ListType.COLLECTION));
        List<BookCandidate> candidates = List.of(candidate("Emma", "Jane Austen", "222"));

        assertEquals(candidates, guard.filter(candidates, owned));
    }

    @Test
    void removesBooksOwnedOnTheReadingList() {
        List<Book> owned = List.of(owned("Emma", "Jane Austen", "222", ListType.READING_LIST));

        assertTrue(guard.filter(List.of(candidate("Emma", "Jane Austen", "222")), owned).isEmpty());
    }

    @Test
    void candidateWithoutIsbnDoesNotMatchOwnedBookWithoutIsbn() {
        List<Book> owned = List.of(owned("Dune", "Frank Herbert", null, ListType.COLLECTION));
        List<BookCandidate> candidates = List.of(candidate("Emma", "Jane Austen", null));

        assertEquals(candidates, guard.filter(candidates, owned));
    }

    @Test
    void rejectsBookAlreadyProposedInConversationByTitleAndAuthor() {
        var filter = guard.newSuggestionFilter(List.of(), List.of(new BookRef("The Lightning Thief", "Rick Riordan", null)));

        assertFalse(filter.test(candidate("the lightning thief", "RICK RIORDAN", "9780786838653")));
    }

    @Test
    void rejectsBookAlreadyProposedInConversationByIsbn() {
        var filter = guard.newSuggestionFilter(List.of(), List.of(new BookRef("Percy Jackson 1", "R. Riordan", "978-0-7868-3865-3")));

        assertFalse(filter.test(candidate("The Lightning Thief", "Rick Riordan", "9780786838653")));
    }

    @Test
    void rejectsRepeatWithinTheSameReply() {
        var filter = guard.newSuggestionFilter(List.of(), List.of());

        assertTrue(filter.test(candidate("Emma", "Jane Austen", null)));
        assertFalse(filter.test(candidate("EMMA", "jane austen", null)));
    }

    @Test
    void acceptsNewBookWhenConversationHasOtherSuggestions() {
        var filter = guard.newSuggestionFilter(List.of(), List.of(new BookRef("Dune", "Frank Herbert", "111")));

        assertTrue(filter.test(candidate("Emma", "Jane Austen", "222")));
    }

    @Test
    void treatsNullConversationHistoryAsEmpty() {
        var filter = guard.newSuggestionFilter(List.of(), null);

        assertTrue(filter.test(candidate("Emma", "Jane Austen", "222")));
    }
}
