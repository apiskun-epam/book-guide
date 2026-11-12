package com.bookshelf.agents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookCandidate {
    private String title;
    private String author;
    private String isbn;
    private String genre;
    private String description;
    private String whySuggested;
}
