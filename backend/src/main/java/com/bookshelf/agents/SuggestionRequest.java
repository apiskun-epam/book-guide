package com.bookshelf.agents;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuggestionRequest {
    @NotBlank(message = "User prompt is required")
    private String prompt;
}
