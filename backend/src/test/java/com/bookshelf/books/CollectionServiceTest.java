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
        when(bookRepository.findByUserIdAndListType(USER_A, ListType.COLLECTION))
                .thenReturn(List.of(book(1, USER_A, "Dune", "111"), book(2, USER_A, "Emma", "222")));

        List<BookDTO> result = service.getBooks(USER_A, ListType.COLLECTION);

        assertEquals(List.of("Dune", "Emma"), result.stream().map(BookDTO::getTitle).toList());
        verify(bookRepository).findByUserIdAndListType(USER_A, ListType.COLLECTION);
        verifyNoMoreInteractions(bookRepository);
    }

    @Test
    void getUserCollectionReturnsEmptyListWhenUserHasNoBooks() {
        when(bookRepository.findByUserIdAndListType(USER_A, ListType.COLLECTION)).thenReturn(List.of());

        assertTrue(service.getBooks(USER_A, ListType.COLLECTION).isEmpty());
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

        BookDTO dto = service.addBook(USER_A, req, ListType.COLLECTION);

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
                () -> service.addBook(USER_A, request("Dune", "111"), ListType.COLLECTION));

        assertEquals("Book already exists in your collection or reading list", ex.getMessage());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void addBookDuplicateCheckIsScopedToUser() {
        // Same ISBN exists for user B but not for user A: A may add it.
        when(bookRepository.existsByUserIdAndIsbn(USER_A, "111")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addBook(USER_A, request("Dune", "111"), ListType.COLLECTION);

        verify(bookRepository).existsByUserIdAndIsbn(USER_A, "111");
        verify(bookRepository, never()).existsByUserIdAndIsbn(USER_B, "111");
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void addBookSkipsDuplicateCheckWhenIsbnNullOrEmpty() {
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addBook(USER_A, request("No ISBN", null), ListType.COLLECTION);
        service.addBook(USER_A, request("Empty ISBN", ""), ListType.COLLECTION);

        verify(bookRepository, never()).existsByUserIdAndIsbn(any(), any());
        verify(bookRepository, org.mockito.Mockito.times(2)).save(any(Book.class));
    }

    // ---- updateBookRating ----

    @Test
    void updateRatingChangesRatingAndSaves() {
        Book existing = book(3, USER_A, "Dune", "111");
        when(bookRepository.findByIdAndUserIdAndListType(3, USER_A, ListType.COLLECTION)).thenReturn(Optional.of(existing));
        when(bookRepository.save(existing)).thenReturn(existing);

        BookDTO dto = service.updateBookRating(USER_A, 3, 4);

        assertEquals(4, existing.getRating());
        assertEquals(4, dto.getRating());
        verify(bookRepository).save(existing);
    }

    @Test
    void updateRatingThrowsWhenBookNotFound() {
        when(bookRepository.findByIdAndUserIdAndListType(99, USER_A, ListType.COLLECTION)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateBookRating(USER_A, 99, 3));

        assertEquals("Book not found", ex.getMessage());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void userCannotUpdateRatingOfAnotherUsersBook() {
        // Book 3 belongs to user B; the lookup scoped to user A finds nothing.
        when(bookRepository.findByIdAndUserIdAndListType(3, USER_A, ListType.COLLECTION)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.updateBookRating(USER_A, 3, 1));

        verify(bookRepository, never()).save(any());
    }

    // ---- removeBookFromCollection ----

    @Test
    void removeBookSchedulesDeletionAfterUndoWindowWithoutDeletingRow() {
        fixClockAtNow();
        Book existing = book(3, USER_A, "Dune", "111");
        when(bookRepository.findByIdAndUserIdAndListType(3, USER_A, ListType.COLLECTION)).thenReturn(Optional.of(existing));

        service.removeBook(USER_A, 3, ListType.COLLECTION);

        assertEquals(NOW.plusSeconds(5), existing.getDeletionScheduledAt());
        verify(bookRepository).save(existing);
        verify(bookRepository, never()).delete(any());
    }

    @Test
    void removeBookThrowsWhenBookNotFound() {
        when(bookRepository.findByIdAndUserIdAndListType(99, USER_A, ListType.COLLECTION)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.removeBook(USER_A, 99, ListType.COLLECTION));

        assertEquals("Book not found", ex.getMessage());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void userCannotRemoveAnotherUsersBook() {
        when(bookRepository.findByIdAndUserIdAndListType(3, USER_A, ListType.COLLECTION)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.removeBook(USER_A, 3, ListType.COLLECTION));

        verify(bookRepository, never()).save(any());
    }

    // ---- undoRemoval ----

    @Test
    void undoRemovalRestoresBookWithinWindow() {
        fixClockAtNow();
        Book pending = book(3, USER_A, "Dune", "111");
        pending.setDeletionScheduledAt(NOW.plusSeconds(2));
        when(bookRepository.findPendingDeletion(3, USER_A, ListType.COLLECTION)).thenReturn(Optional.of(pending));
        when(bookRepository.existsByUserIdAndIsbn(USER_A, "111")).thenReturn(false);
        when(bookRepository.save(pending)).thenReturn(pending);

        BookDTO dto = service.undoRemoval(USER_A, 3, ListType.COLLECTION);

        assertNull(pending.getDeletionScheduledAt());
        assertEquals("Dune", dto.getTitle());
        verify(bookRepository).save(pending);
    }

    @Test
    void undoRemovalFailsOnceWindowHasPassed() {
        fixClockAtNow();
        Book pending = book(3, USER_A, "Dune", "111");
        pending.setDeletionScheduledAt(NOW.minusSeconds(1));
        when(bookRepository.findPendingDeletion(3, USER_A, ListType.COLLECTION)).thenReturn(Optional.of(pending));

        assertThrows(IllegalArgumentException.class, () -> service.undoRemoval(USER_A, 3, ListType.COLLECTION));

        assertNotNull(pending.getDeletionScheduledAt());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void undoRemovalFailsWhenSameIsbnWasReAddedDuringWindow() {
        fixClockAtNow();
        Book pending = book(3, USER_A, "Dune", "111");
        pending.setDeletionScheduledAt(NOW.plusSeconds(2));
        when(bookRepository.findPendingDeletion(3, USER_A, ListType.COLLECTION)).thenReturn(Optional.of(pending));
        when(bookRepository.existsByUserIdAndIsbn(USER_A, "111")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.undoRemoval(USER_A, 3, ListType.COLLECTION));

        assertEquals("Book already exists in your collection or reading list", ex.getMessage());
        assertNotNull(pending.getDeletionScheduledAt());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void undoRemovalSkipsDuplicateCheckWhenIsbnMissing() {
        fixClockAtNow();
        Book pending = book(3, USER_A, "No ISBN", null);
        pending.setDeletionScheduledAt(NOW.plusSeconds(2));
        when(bookRepository.findPendingDeletion(3, USER_A, ListType.COLLECTION)).thenReturn(Optional.of(pending));
        when(bookRepository.save(pending)).thenReturn(pending);

        service.undoRemoval(USER_A, 3, ListType.COLLECTION);

        assertNull(pending.getDeletionScheduledAt());
        verify(bookRepository, never()).existsByUserIdAndIsbn(any(), any());
    }

    @Test
    void undoRemovalFailsWhenBookNotPending() {
        when(bookRepository.findPendingDeletion(3, USER_A, ListType.COLLECTION)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.undoRemoval(USER_A, 3, ListType.COLLECTION));

        verify(bookRepository, never()).save(any());
    }

    @Test
    void userCannotUndoAnotherUsersRemoval() {
        // Book 3 belongs to user B; the lookup scoped to user A finds nothing.
        when(bookRepository.findPendingDeletion(3, USER_A, ListType.COLLECTION)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.undoRemoval(USER_A, 3, ListType.COLLECTION));

        verify(bookRepository, never()).save(any());
        verify(bookRepository, never()).findPendingDeletion(3, USER_B, ListType.COLLECTION);
    }

    // ---- reading list ----

    @Test
    void addToReadingListIgnoresRatingAndSetsListType() {
        AddBookRequest req = request("Dune", "111");
        req.setRating(5);
        when(bookRepository.existsByUserIdAndIsbn(USER_A, "111")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookDTO dto = service.addBook(USER_A, req, ListType.READING_LIST);

        assertEquals(ListType.READING_LIST, dto.getListType());
        assertNull(dto.getRating());
    }

    @Test
    void addToReadingListRejectsBookAlreadyInCollection() {
        // The duplicate check is not list-scoped, so a collection book blocks a reading-list add.
        when(bookRepository.existsByUserIdAndIsbn(USER_A, "111")).thenReturn(true);

        assertThrows(DuplicateBookException.class,
                () -> service.addBook(USER_A, request("Dune", "111"), ListType.READING_LIST));

        verify(bookRepository, never()).save(any());
    }

    @Test
    void getReadingListQueriesReadingListOnly() {
        when(bookRepository.findByUserIdAndListType(USER_A, ListType.READING_LIST))
                .thenReturn(List.of(book(1, USER_A, "Dune", "111")));

        assertEquals(1, service.getBooks(USER_A, ListType.READING_LIST).size());
    }

    @Test
    void moveToCollectionFlipsListAndSetsRating() {
        Book existing = book(3, USER_A, "Dune", "111");
        existing.setListType(ListType.READING_LIST);
        when(bookRepository.findByIdAndUserIdAndListType(3, USER_A, ListType.READING_LIST))
                .thenReturn(Optional.of(existing));
        when(bookRepository.save(existing)).thenReturn(existing);

        BookDTO dto = service.moveToCollection(USER_A, 3, 4);

        assertEquals(ListType.COLLECTION, existing.getListType());
        assertEquals(4, dto.getRating());
    }

    @Test
    void moveToCollectionAllowsMissingRating() {
        Book existing = book(3, USER_A, "Dune", "111");
        existing.setListType(ListType.READING_LIST);
        when(bookRepository.findByIdAndUserIdAndListType(3, USER_A, ListType.READING_LIST))
                .thenReturn(Optional.of(existing));
        when(bookRepository.save(existing)).thenReturn(existing);

        assertNull(service.moveToCollection(USER_A, 3, null).getRating());
        assertEquals(ListType.COLLECTION, existing.getListType());
    }

    @Test
    void moveToCollectionFailsWhenBookNotOnUsersReadingList() {
        // Covers another user's book and a book already in the collection.
        when(bookRepository.findByIdAndUserIdAndListType(3, USER_A, ListType.READING_LIST))
                .thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> service.moveToCollection(USER_A, 3, 5));

        verify(bookRepository, never()).save(any());
    }

    @Test
    void removeAndUndoOnReadingListAreScopedToThatList() {
        fixClockAtNow();
        Book existing = book(3, USER_A, "Dune", "111");
        existing.setListType(ListType.READING_LIST);
        when(bookRepository.findByIdAndUserIdAndListType(3, USER_A, ListType.READING_LIST))
                .thenReturn(Optional.of(existing));

        service.removeBook(USER_A, 3, ListType.READING_LIST);
        assertEquals(NOW.plusSeconds(5), existing.getDeletionScheduledAt());

        when(bookRepository.findPendingDeletion(3, USER_A, ListType.READING_LIST))
                .thenReturn(Optional.of(existing));
        when(bookRepository.existsByUserIdAndIsbn(USER_A, "111")).thenReturn(false);
        when(bookRepository.save(existing)).thenReturn(existing);

        service.undoRemoval(USER_A, 3, ListType.READING_LIST);
        assertNull(existing.getDeletionScheduledAt());
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
