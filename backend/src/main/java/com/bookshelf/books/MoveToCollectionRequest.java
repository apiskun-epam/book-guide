package com.bookshelf.books;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveToCollectionRequest {
    /** Optional rating given when the book moves to the collection. */
    @Min(0)
    @Max(5)
    private Integer rating;
}
