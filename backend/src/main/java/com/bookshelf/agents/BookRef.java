package com.bookshelf.agents;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Minimal identity of a book already proposed earlier in the current conversation. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookRef {
    @Size(max = 300)
    private String title;
    @Size(max = 300)
    private String author;
    @Size(max = 32)
    private String isbn;
}
