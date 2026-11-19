package com.bookshelf.agents;

import com.bookshelf.books.Book;
import com.bookshelf.books.ListType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BacklogGuardTest {

    private final BacklogGuard guard = new BacklogGuard();

    private final List<Book> backlog = List.of(
            book("Emma", "Jane Austen", "222"),
            book("Ulysses", "James Joyce", "333"),
            book("Dracula", "Bram Stoker", "444"),
            book("Persuasion", "Jane Austen", "555"));

    private static Book book(String title, String author, String isbn) {
        return Book.builder().title(title).author(author).isbn(isbn)
                .description("About " + title).coverUrl("http://cover/" + isbn)
                .listType(ListType.READING_LIST).build();
    }

    private static RankingResult result(String... refs) {
        RankingResult result = new RankingResult();
        for (String ref : refs) {
            result.getPicks().add(new RankingResult.Pick(ref, "reason " + ref));
        }
        return result;
    }

    @Test
    void keepsValidRefsInOrderAndBuildsFromDatabaseRow() {
        List<BookCandidate> picks = guard.resolve(result("b2", "b1"), backlog);

        assertEquals(List.of("Ulysses", "Emma"), picks.stream().map(BookCandidate::getTitle).toList());
        assertEquals("James Joyce", picks.get(0).getAuthor());
        assertEquals("333", picks.get(0).getIsbn());
        assertEquals("http://cover/333", picks.get(0).getCoverUrl());
        assertEquals("reason b2", picks.get(0).getWhySuggested());
    }

    @Test
    void dropsUnknownRefs() {
        List<BookCandidate> picks = guard.resolve(result("b9", "b0", "nonsense", "b3"), backlog);

        assertEquals(List.of("Dracula"), picks.stream().map(BookCandidate::getTitle).toList());
    }

    @Test
    void dropsRepeatedRefs() {
        List<BookCandidate> picks = guard.resolve(result("b1", "b1", "b1"), backlog);

        assertEquals(1, picks.size());
    }

    @Test
    void returnsAtMostThreePicks() {
        List<BookCandidate> picks = guard.resolve(result("b1", "b2", "b3", "b4"), backlog);

        assertEquals(3, picks.size());
    }

    @Test
    void emptyOrNullResultGivesNoPicks() {
        assertTrue(guard.resolve(new RankingResult(), backlog).isEmpty());
        assertTrue(guard.resolve(null, backlog).isEmpty());
    }
}
