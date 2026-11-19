package com.bookshelf.agents;

/** Which kind of recommendation the chat is asking for. */
public enum SuggestionMode {
    /** Recommend new books that are in neither of the user's lists (default). */
    DISCOVER,
    /** Rank books already on the user's Reading List for the current mood or time. */
    BACKLOG
}
