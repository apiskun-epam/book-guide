package com.bookshelf.agents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntakeResult {
    private String mood;
    private List<String> genres;
    private String length;
    private String theme;
    private String otherPreferences;
}
