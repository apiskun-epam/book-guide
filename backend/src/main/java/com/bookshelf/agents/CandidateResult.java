package com.bookshelf.agents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateResult {
    private List<BookCandidate> candidates;
}
