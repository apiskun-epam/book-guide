package com.bookshelf.books;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionService {

    /** How long a removal can be undone before the book is permanently deleted. */
    public static final Duration UNDO_WINDOW = Duration.ofSeconds(5);

    private final BookRepository bookRepository;
    private final Clock clock;

    public List<BookDTO> getUserCollection(String userId) {
        return bookRepository.findByUserId(userId)
                .stream()
                .map(BookDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookDTO addBookToCollection(String userId, AddBookRequest request) {
        // Check if book already exists in user's collection
        if (request.getIsbn() != null && !request.getIsbn().isEmpty()) {
            if (bookRepository.existsByUserIdAndIsbn(userId, request.getIsbn())) {
                throw new IllegalArgumentException("Book already exists in your collection");
            }
        }

        Book book = Book.builder()
                .userId(userId)
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .coverUrl(request.getCoverUrl())
                .publishYear(request.getPublishYear())
                .description(request.getDescription())
                .rating(request.getRating())
                .build();

        Book savedBook = bookRepository.save(book);
        return new BookDTO(savedBook);
    }

    @Transactional
    public BookDTO updateBookRating(String userId, Integer bookId, Integer rating) {
        Book book = bookRepository.findByIdAndUserId(bookId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        book.setRating(rating);
        Book updatedBook = bookRepository.save(book);
        return new BookDTO(updatedBook);
    }

    /** Hides the book and schedules its permanent deletion after {@link #UNDO_WINDOW}. */
    @Transactional
    public void removeBookFromCollection(String userId, Integer bookId) {
        Book book = bookRepository.findByIdAndUserId(bookId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        book.setDeletionScheduledAt(LocalDateTime.now(clock).plus(UNDO_WINDOW));
        bookRepository.save(book);
    }

    /** Cancels a scheduled removal. Fails if the book is not pending deletion (e.g. already purged). */
    @Transactional
    public BookDTO undoRemoval(String userId, Integer bookId) {
        Book book = bookRepository.findPendingDeletion(bookId, userId)
                .filter(b -> b.getDeletionScheduledAt().isAfter(LocalDateTime.now(clock)))
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        book.setDeletionScheduledAt(null);
        return new BookDTO(bookRepository.save(book));
    }

    /** Permanently deletes every book whose undo window has passed. */
    @Transactional
    public int purgeExpiredRemovals() {
        return bookRepository.deleteExpired(LocalDateTime.now(clock));
    }
}
