package com.bookshelf.books;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final BookRepository bookRepository;

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

    @Transactional
    public void removeBookFromCollection(String userId, Integer bookId) {
        Book book = bookRepository.findByIdAndUserId(bookId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        bookRepository.delete(book);
    }
}
