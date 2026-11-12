package com.bookshelf.books;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddBookRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String author;

    private String isbn;

    private String coverUrl;

    private Integer publishYear;

    private String description;

    @Min(value = 0, message = "Rating must be between 0 and 5")
    @Max(value = 5, message = "Rating must be between 0 and 5")
    private Integer rating;

    private String testField;
}
