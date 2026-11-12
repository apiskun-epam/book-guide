package com.bookshelf.books;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    private static final String USER_A = "uid-a";
    private static final String USER_B = "uid-b";

    @Mock
    private BookRepository bookRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private CollectionService service;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0, 0);

    private void fixClockAtNow() {
        when(clock.instant()).thenReturn(NOW.toInstant(ZoneOffset.UTC));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    private static Book book(int id, String userId, String title, String isbn) {
        return Book.builder().id(id).userId(userId).title(title).isbn(isbn).build();
    }

    private static AddBookRequest request(String title, String isbn) {
        AddBookRequest r = new AddBookRequest();
        r.setTitle(title);
        r.setIsbn(isbn);
        return r;
    }

    // ---- getUserCollection ----

    @Test
    void getUserCollectionReturnsMappedBooksForThatUserOnly() {
        when(bookRepository.findByUserId(USER_A))
                .thenReturn(List.of(book(1, USER_A, "Dune", "111"), book(2, USER_A, "Emma", "222")));

        List<BookDTO> result = service.getUserCollection(USER_A);

        assertEquals(List.of("Dune", "Emma"), result.stream().map(BookDTO::getTitle).toList());
        verify(bookRepository).findByUserId(USER_A);
        verifyNoMoreInteractions(bookRepository);
    }

    @Test
    void getUserCollectionReturnsEmptyListWhenUserHasNoBooks() {
        when(bookRepository.findByUserId(USER_A)).thenReturn(List.of());

        assertTrue(service.getUserCollection(USER_A).isEmpty());
    }

    // ---- addBookToCollection ----

    @Test
    void addBookSavesWithAuthenticatedUserIdAndAllFields() {
        AddBookRequest req = request("Dune", "111");
        req.setAuthor("Frank Herbert");
        req.setCoverUrl("http://cover");
        req.setPublishYear(1965);
        req.setDescription("Spice");
        req.setRating(5);
        when(bookRepository.existsByUserIdAndIsbn(USER_A, "111")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            b.setId(7);
            return b;
        });

        BookDTO dto = service.addBookToCollection(USER_A, req);

        ArgumentCaptor<Book> saved = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(saved.capture());
        Book b = saved.getValue();
        assertEquals(USER_A, b.getUserId());
        assertEquals("Dune", b.getTitle());
        assertEquals("Frank Herbert", b.getAuthor());
        assertEquals("111", b.getIsbn());
        assertEquals("http://cover", b.getCoverUrl());
        assertEquals(1965, b.getPublishYear());
        assertEquals("Spice", b.getDescription());
        assertEquals(5, b.getRating());
        assertEquals(7, dto.getId());
        assertEquals("Dune", dto.getTitle());
    }

    @Test
    void addBookRejectsDuplicateIsbnForSameUser() {
        when(bookRepository.existsByUserIdAndIsbn(USER_A, "111")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addBookToCollection(USER_A, request("Dune", "111")));

        assertEquals("Book already exists in your collection", ex.getMessage());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void addBookDuplicateCheckIsScopedToUser() {
        // Same ISBN exists for user B but not for user A: A may add it.
        when(bookRepository.existsByUserIdAndIsbn(USER_A, "111")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addBookToCollection(USER_A, request("Dune", "111"));

        verify(bookRepository).existsByUserIdAndIsbn(USER_A, "111");
        verify(bookRepository, never()).existsByUserIdAndIsbn(USER_B, "111");
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void addBookSkipsDuplicateCheckWhenIsbnNullOrEmpty() {
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addBookToCollection(USER_A, request("No ISBN", null));
        service.addBookToCollection(USER_A, request("Empty ISBN", ""));

        verify(bookRepository, never()).existsByUserIdAndIsbn(any(), any());
        verify(bookRepository, org.mockito.Mockito.times(2)).save(any(Book.class));
    }

    // ---- updateBookRating ----

    @Test
    void updateRatingChangesRatingAndSaves() {
        Book existing = book(3, USER_A, "Dune", "111");
        when(bookRepository.findByIdAndUserId(3, USER_A)).thenReturn(Optional.of(existing));
        when(bookRepository.save(existing)).thenReturn(existing);

        BookDTO dto = service.updateBookRating(USER_A, 3, 4);

        assertEquals(4, existing.getRating());
        assertEquals(4, dto.getRating());
        verify(bookRepository).save(existing);
    }

    @Test
    void updateRatingThrowsWhenBookNotFound() {
        when(bookRepository.findByIdAndUserId(99, USER_A)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateBookRating(USER_A, 99, 3));

        assertEquals("Book not found", ex.getMessage());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void userCannotUpdateRatingOfAnotherUsersBook() {
        // Book 3 belongs to user B; the lookup scoped to user A finds nothing.
        when(bookRepository.findByIdAndUserId(3, USER_A)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.updateBookRating(USER_A, 3, 1));

        verify(bookRepository, never()).save(any());
    }

    // ---- removeBookFromCollection ----

    @Test
    void removeBookSchedulesDeletionAfterUndoWindowWithoutDeletingRow() {
        fixClockAtNow();
        Book existing = book(3, USER_A, "Dune", "111");
        when(bookRepository.findByIdAndUserId(3, USER_A)).thenReturn(Optional.of(existing));

        service.removeBookFromCollection(USER_A, 3);

        assertEquals(NOW.plusSeconds(5), existing.getDeletionScheduledAt());
        verify(bookRepository).save(existing);
        verify(bookRepository, never()).delete(any());
    }

    @Test
    void removeBookThrowsWhenBookNotFound() {
        when(bookRepository.findByIdAndUserId(99, USER_A)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.removeBookFromCollection(USER_A, 99));

        assertEquals("Book not found", ex.getMessage());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void userCannotRemoveAnotherUsersBook() {
        when(bookRepository.findByIdAndUserId(3, USER_A)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.removeBookFromCollection(USER_A, 3));

        verify(bookRepository, never()).save(any());
    }

    // ---- undoRemoval ----

    @Test
    void undoRemovalRestoresBookWithinWindow() {
        fixClockAtNow();
        Book pending = book(3, USER_A, "Dune", "111");
        pending.setDeletionScheduledAt(NOW.plusSeconds(2));
        when(bookRepository.findPendingDeletion(3, USER_A)).thenReturn(Optional.of(pending));
        when(bookRepository.save(pending)).thenReturn(pending);

        BookDTO dto = service.undoRemoval(USER_A, 3);

        assertNull(pending.getDeletionScheduledAt());
        assertEquals("Dune", dto.getTitle());
        verify(bookRepository).save(pending);
    }

    @Test
    void undoRemovalFailsOnceWindowHasPassed() {
        fixClockAtNow();
        Book pending = book(3, USER_A, "Dune", "111");
        pending.setDeletionScheduledAt(NOW.minusSeconds(1));
        when(bookRepository.findPendingDeletion(3, USER_A)).thenReturn(Optional.of(pending));

        assertThrows(IllegalArgumentException.class, () -> service.undoRemoval(USER_A, 3));

        assertNotNull(pending.getDeletionScheduledAt());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void undoRemovalFailsWhenBookNotPending() {
        when(bookRepository.findPendingDeletion(3, USER_A)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.undoRemoval(USER_A, 3));

        verify(bookRepository, never()).save(any());
    }

    @Test
    void userCannotUndoAnotherUsersRemoval() {
        // Book 3 belongs to user B; the lookup scoped to user A finds nothing.
        when(bookRepository.findPendingDeletion(3, USER_A)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.undoRemoval(USER_A, 3));

        verify(bookRepository, never()).save(any());
        verify(bookRepository, never()).findPendingDeletion(3, USER_B);
    }

    // ---- purgeExpiredRemovals ----

    @Test
    void purgeDeletesExpiredRemovalsAsOfNow() {
        fixClockAtNow();
        when(bookRepository.deleteExpired(NOW)).thenReturn(2);

        assertEquals(2, service.purgeExpiredRemovals());

        verify(bookRepository).deleteExpired(NOW);
    }
}
