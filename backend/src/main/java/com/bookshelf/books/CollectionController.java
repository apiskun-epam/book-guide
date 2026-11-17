package com.bookshelf.books;

import com.bookshelf.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/collection")
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    public ResponseEntity<List<BookDTO>> getCollection(@AuthenticationPrincipal AuthenticatedUser user) {
        List<BookDTO> collection = collectionService.getUserCollection(user.uid());
        return ResponseEntity.ok(collection);
    }

    @PostMapping
    public ResponseEntity<BookDTO> addBook(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AddBookRequest request) {
        BookDTO book = collectionService.addBookToCollection(user.uid(), request);
        return ResponseEntity.ok(book);
    }

    @PatchMapping("/{bookId}/rating")
    public ResponseEntity<BookDTO> updateRating(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Integer bookId,
            @RequestParam Integer rating) {
        BookDTO book = collectionService.updateBookRating(user.uid(), bookId, rating);
        return ResponseEntity.ok(book);
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeBook(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Integer bookId) {
        collectionService.removeBookFromCollection(user.uid(), bookId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bookId}/undo-removal")
    public ResponseEntity<BookDTO> undoRemoval(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Integer bookId) {
        return ResponseEntity.ok(collectionService.undoRemoval(user.uid(), bookId));
    }
}
