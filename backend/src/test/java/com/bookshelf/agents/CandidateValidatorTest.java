package com.bookshelf.agents;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateValidatorTest {

    private static BookCandidate candidate(String title, String author, String description, String why) {
        return new BookCandidate(title, author, null, "Horror", description, why, null);
    }

    private static final String DESC = "A family moves into a haunted house and slowly unravels.";
    private static final String WHY = "Matches your taste for supernatural horror.";

    @Test
    void acceptsCompleteCandidate() {
        assertTrue(CandidateValidator.isValid(candidate("The Shining", "Stephen King", DESC, WHY)));
    }

    @Test
    void rejectsNull() {
        assertFalse(CandidateValidator.isValid(null));
    }

    @Test
    void rejectsMissingFields() {
        assertFalse(CandidateValidator.isValid(candidate(null, "Stephen King", DESC, WHY)));
        assertFalse(CandidateValidator.isValid(candidate("The Shining", " ", DESC, WHY)));
        assertFalse(CandidateValidator.isValid(candidate("The Shining", "Stephen King", null, WHY)));
        assertFalse(CandidateValidator.isValid(candidate("The Shining", "Stephen King", DESC, "")));
    }

    @Test
    void rejectsUnknownAuthorCaseInsensitively() {
        assertFalse(CandidateValidator.isValid(candidate("The Shining", "Unknown", DESC, WHY)));
        assertFalse(CandidateValidator.isValid(candidate("The Shining", " UNKNOWN. ", DESC, WHY)));
    }

    @Test
    void rejectsPlaceholderAndTooShortText() {
        assertFalse(CandidateValidator.isValid(candidate("The Shining", "Stephen King", "placeholder", WHY)));
        assertFalse(CandidateValidator.isValid(candidate("The Shining", "Stephen King", DESC, "TBD")));
        assertFalse(CandidateValidator.isValid(candidate("The Shining", "Stephen King", "Scary.", WHY)));
        assertFalse(CandidateValidator.isValid(candidate("The Shining", "Stephen King", "Lorem ipsum dolor sit amet", WHY)));
    }
}
