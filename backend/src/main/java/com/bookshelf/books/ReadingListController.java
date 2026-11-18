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
@RequestMapping("/api/reading-list")
public class ReadingListController {

    private final CollectionService collectionService;

    @GetMapping
    public ResponseEntity<List<BookDTO>> getReadingList(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(collectionService.getBooks(user.uid(), ListType.READING_LIST));
    }

    @PostMapping
    public ResponseEntity<BookDTO> addBook(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AddBookRequest request) {
        return ResponseEntity.ok(collectionService.addBook(user.uid(), request, ListType.READING_LIST));
    }

    @PostMapping("/{bookId}/move-to-collection")
    public ResponseEntity<BookDTO> moveToCollection(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Integer bookId,
            @Valid @RequestBody(required = false) MoveToCollectionRequest request) {
        Integer rating = request == null ? null : request.getRating();
        return ResponseEntity.ok(collectionService.moveToCollection(user.uid(), bookId, rating));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeBook(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Integer bookId) {
        collectionService.removeBook(user.uid(), bookId, ListType.READING_LIST);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bookId}/undo-removal")
    public ResponseEntity<BookDTO> undoRemoval(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Integer bookId) {
        return ResponseEntity.ok(collectionService.undoRemoval(user.uid(), bookId, ListType.READING_LIST));
    }
}
