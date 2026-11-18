package com.bookshelf.books;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** Manages both of a user's lists: the rated collection and the unrated reading list. */
@Service
@RequiredArgsConstructor
public class CollectionService {

    /** How long a removal can be undone before the book is permanently deleted. */
    public static final Duration UNDO_WINDOW = Duration.ofSeconds(5);

    private final BookRepository bookRepository;
    private final Clock clock;

    public List<BookDTO> getBooks(String userId, ListType listType) {
        return bookRepository.findByUserIdAndListType(userId, listType)
                .stream()
                .map(BookDTO::new)
                .toList();
    }

    /**
     * Adds a book to the given list. A book (by ISBN) may be in only one list, so the duplicate
     * check covers both. Reading-list books are never rated.
     */
    @Transactional
    public BookDTO addBook(String userId, AddBookRequest request, ListType listType) {
        if (hasIsbn(request.getIsbn()) && bookRepository.existsByUserIdAndIsbn(userId, request.getIsbn())) {
            throw new DuplicateBookException();
        }

        Book book = Book.builder()
                .userId(userId)
                .listType(listType)
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .coverUrl(request.getCoverUrl())
                .publishYear(request.getPublishYear())
                .description(request.getDescription())
                .rating(listType == ListType.COLLECTION ? request.getRating() : null)
                .build();

        return new BookDTO(bookRepository.save(book));
    }

    /** Only collection books can be rated. */
    @Transactional
    public BookDTO updateBookRating(String userId, Integer bookId, Integer rating) {
        Book book = bookRepository.findByIdAndUserIdAndListType(bookId, userId, ListType.COLLECTION)
                .orElseThrow(BookNotFoundException::new);

        book.setRating(rating);
        return new BookDTO(bookRepository.save(book));
    }

    /** Moves a reading-list book into the collection in place, with an optional rating. */
    @Transactional
    public BookDTO moveToCollection(String userId, Integer bookId, Integer rating) {
        Book book = bookRepository.findByIdAndUserIdAndListType(bookId, userId, ListType.READING_LIST)
                .orElseThrow(BookNotFoundException::new);

        book.setListType(ListType.COLLECTION);
        book.setRating(rating);
        return new BookDTO(bookRepository.save(book));
    }

    /** Hides the book and schedules its permanent deletion after {@link #UNDO_WINDOW}. */
    @Transactional
    public void removeBook(String userId, Integer bookId, ListType listType) {
        Book book = bookRepository.findByIdAndUserIdAndListType(bookId, userId, listType)
                .orElseThrow(BookNotFoundException::new);

        book.setDeletionScheduledAt(LocalDateTime.now(clock).plus(UNDO_WINDOW));
        bookRepository.save(book);
    }

    /**
     * Cancels a scheduled removal. Fails if the book is not pending deletion (e.g. already purged)
     * or if an active book with the same ISBN has since been added to either list.
     */
    @Transactional
    public BookDTO undoRemoval(String userId, Integer bookId, ListType listType) {
        Book book = bookRepository.findPendingDeletion(bookId, userId, listType)
                .filter(b -> b.getDeletionScheduledAt().isAfter(LocalDateTime.now(clock)))
                .orElseThrow(BookNotFoundException::new);

        // The book may have been re-added while its removal was pending; don't create a duplicate.
        if (hasIsbn(book.getIsbn()) && bookRepository.existsByUserIdAndIsbn(userId, book.getIsbn())) {
            throw new DuplicateBookException();
        }

        book.setDeletionScheduledAt(null);
        return new BookDTO(bookRepository.save(book));
    }

    /** Permanently deletes every book whose undo window has passed. */
    @Transactional
    public int purgeExpiredRemovals() {
        return bookRepository.deleteExpired(LocalDateTime.now(clock));
    }

    private static boolean hasIsbn(String isbn) {
        return isbn != null && !isbn.isEmpty();
    }
}
