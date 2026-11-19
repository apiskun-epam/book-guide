package com.bookshelf.agents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Output of the ranking agent: picks from the user's Reading List, best first. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankingResult {

    private List<Pick> picks = new ArrayList<>();

    /** One pick: an opaque reference to a backlog book plus why it fits right now. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pick {
        private String ref;
        private String reason;
    }
}
