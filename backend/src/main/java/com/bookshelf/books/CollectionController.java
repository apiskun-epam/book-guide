package com.bookshelf.books;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/collection")
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    public ResponseEntity<List<BookDTO>> getCollection() {
        // TODO: Replace with authenticated user ID from Firebase token
        String userId = "demo-user";
        List<BookDTO> collection = collectionService.getUserCollection(userId);
        return ResponseEntity.ok(collection);
    }

    @PostMapping
    public ResponseEntity<BookDTO> addBook(@Valid @RequestBody AddBookRequest request) {
        // TODO: Replace with authenticated user ID from Firebase token
        String userId = "demo-user";
        BookDTO book = collectionService.addBookToCollection(userId, request);
        return ResponseEntity.ok(book);
    }

    @PatchMapping("/{bookId}/rating")
    public ResponseEntity<BookDTO> updateRating(
            @PathVariable Integer bookId,
            @RequestParam Integer rating) {
        // TODO: Replace with authenticated user ID from Firebase token
        String userId = "demo-user";
        BookDTO book = collectionService.updateBookRating(userId, bookId, rating);
        return ResponseEntity.ok(book);
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeBook(@PathVariable Integer bookId) {
        // TODO: Replace with authenticated user ID from Firebase token
        String userId = "demo-user";
        collectionService.removeBookFromCollection(userId, bookId);
        return ResponseEntity.noContent().build();
    }
}
