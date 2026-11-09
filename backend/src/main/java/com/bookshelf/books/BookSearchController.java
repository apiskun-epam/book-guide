package com.bookshelf.books;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/books")
public class BookSearchController {

    private final BookSearchService bookSearchService;

    @GetMapping("/search")
    public ResponseEntity<List<BookSearchResult>> searchBooks(@RequestParam String q) {
        List<BookSearchResult> results = bookSearchService.searchBooks(q);
        return ResponseEntity.ok(results);
    }
}
