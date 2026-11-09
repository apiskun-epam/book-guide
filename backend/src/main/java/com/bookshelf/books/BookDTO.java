package com.bookshelf.books;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class BookDTO {
    private Integer id;
    private String title;
    private String author;
    private String isbn;
    private String coverUrl;
    private Integer publishYear;
    private String description;
    private Integer rating;
    private LocalDateTime addedAt;
    private LocalDateTime updatedAt;

    public BookDTO(Book book) {
        this.id = book.getId();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.isbn = book.getIsbn();
        this.coverUrl = book.getCoverUrl();
        this.publishYear = book.getPublishYear();
        this.description = book.getDescription();
        this.rating = book.getRating();
        this.addedAt = book.getAddedAt();
        this.updatedAt = book.getUpdatedAt();
    }
}
