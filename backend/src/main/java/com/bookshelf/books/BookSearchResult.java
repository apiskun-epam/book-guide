package com.bookshelf.books;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookSearchResult {
    private String title;
    private String author;
    private String isbn;
    private String coverUrl;
    private Integer publishYear;
    private String description;
}
