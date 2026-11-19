package com.bookshelf.agents;

import com.bookshelf.books.Book;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deterministic guard for the "pick from my Reading List" mode: the mirror of {@link ExclusionGuard}.
 * The model only returns opaque references; this guard keeps those that point at a book actually
 * on the user's list and builds the response from the database row, never from model output.
 * Unknown or repeated references are dropped.
 */
@Component
public class BacklogGuard {

    static final int MAX_PICKS = 3;

    /** Reference shown to the model for the book at {@code index} in the backlog list. */
    static String refFor(int index) {
        return "b" + (index + 1);
    }

    public List<BookCandidate> resolve(RankingResult result, List<Book> backlog) {
        List<BookCandidate> picks = new ArrayList<>();
        if (result == null || result.getPicks() == null) {
            return picks;
        }
        Set<String> seen = new HashSet<>();
        for (RankingResult.Pick pick : result.getPicks()) {
            if (picks.size() >= MAX_PICKS) {
                break;
            }
            if (pick == null || pick.getRef() == null || !seen.add(pick.getRef())) {
                continue;
            }
            for (int i = 0; i < backlog.size(); i++) {
                if (refFor(i).equals(pick.getRef())) {
                    Book book = backlog.get(i);
                    picks.add(new BookCandidate(book.getTitle(), book.getAuthor(), book.getIsbn(), null,
                            book.getDescription(), pick.getReason(), book.getCoverUrl()));
                    break;
                }
            }
        }
        return picks;
    }
}
