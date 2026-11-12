package com.bookshelf.agents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceResult {
    private List<String> favoriteGenres;
    private List<String> favoriteAuthors;
    private String preferredLength;
    private String readingPatterns;
    private List<String> avoidTopics;
}
