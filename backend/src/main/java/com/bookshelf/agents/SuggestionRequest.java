package com.bookshelf.agents;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SuggestionRequest {
    @NotBlank(message = "User prompt is required")
    private String prompt;

    /** Books already proposed earlier in this conversation; they are never proposed again. */
    @Valid
    @Size(max = 200)
    private List<BookRef> alreadySuggested = new ArrayList<>();
}
