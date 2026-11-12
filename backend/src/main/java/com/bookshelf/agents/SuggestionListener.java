package com.bookshelf.agents;

/** Receives progressive output from the suggestion pipeline. */
public interface SuggestionListener {

    /** The intro message, available as soon as the request has been analysed. */
    void onMessage(String message);

    /** A single recommendation, emitted as soon as the model finishes writing it. */
    void onSuggestion(BookCandidate candidate);
}
