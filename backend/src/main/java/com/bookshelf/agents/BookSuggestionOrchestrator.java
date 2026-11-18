package com.bookshelf.agents;

import com.bookshelf.books.Book;
import com.bookshelf.books.BookRepository;
import com.bookshelf.books.BookSearchService;
import com.bookshelf.books.ListType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookSuggestionOrchestrator {

    private final IntakeAgent intakeAgent;
    private final PreferenceAgent preferenceAgent;
    private final CandidateAgent candidateAgent;
    private final BookRepository bookRepository;
    private final BookSearchService bookSearchService;
    private final ExclusionGuard exclusionGuard;

    /**
     * Orchestrates the multi-agent workflow to generate book suggestions.
     * 
     * Pipeline:
     * 1. Intake agent - analyzes user's current request
     * 2. Preference agent - analyzes user's book collection for preferences
     * 3. Candidate agent - generates book recommendations
     * 4. Guard - drops books already in the user's collection or reading list
     * 
     * @param userId The authenticated user's Firebase UID
     * @param userPrompt The user's book request message
     * @return SuggestionResponse containing the book candidates that passed the guard
     */
    public SuggestionResponse generateSuggestions(String userId, String userPrompt) {
        log.info("Starting book suggestion workflow for user: {}", userId);
        
        try {
            // Step 1: Intake - analyze current request
            log.debug("Step 1: Running intake agent");
            IntakeResult intakeResult = intakeAgent.analyze(userPrompt);
            log.debug("Intake result: {}", intakeResult);
            
            // Step 2: Preference - analyze user's collection
            log.debug("Step 2: Running preference agent");
            List<Book> allBooks = bookRepository.findByUserId(userId);
            PreferenceResult preferenceResult = preferenceAgent.analyze(readBooks(allBooks));
            log.debug("Preference result: {}", preferenceResult);
            
            // Step 3: Candidate - generate recommendations
            log.debug("Step 3: Running candidate agent");
            CandidateResult candidateResult = candidateAgent.generateCandidates(intakeResult, preferenceResult);
            log.debug("Generated {} candidates", candidateResult.getCandidates().size());
            
            // Step 4: Guard - remove books the user already has in either list
            List<BookCandidate> candidates = exclusionGuard.filter(candidateResult.getCandidates(), allBooks);
            candidates.forEach(this::attachCover);

            // Build response
            String message = buildResponseMessage(intakeResult, readBooks(allBooks).size());
            SuggestionResponse response = new SuggestionResponse(candidates, message);
            
            log.info("Successfully generated {} suggestions for user: {}", 
                    candidates.size(), userId);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error generating book suggestions for user: {}", userId, e);
            throw new RuntimeException("Failed to generate book suggestions. Please try again.", e);
        }
    }

    /**
     * Streaming variant of {@link #generateSuggestions}: reports the intro message once the
     * request is analysed, then each recommendation as the candidate agent produces it.
     */
    public void streamSuggestions(String userId, String userPrompt, SuggestionListener listener) {
        log.info("Starting streaming book suggestion workflow for user: {}", userId);

        try {
            IntakeResult intakeResult = intakeAgent.analyze(userPrompt);

            List<Book> allBooks = bookRepository.findByUserId(userId);
            List<Book> readBooks = readBooks(allBooks);
            PreferenceResult preferenceResult = preferenceAgent.analyze(readBooks);
            Predicate<BookCandidate> allowed = exclusionGuard.allowedAgainst(allBooks);

            listener.onMessage(buildResponseMessage(intakeResult, readBooks.size()));

            candidateAgent.streamCandidates(intakeResult, preferenceResult, candidate -> {
                if (!allowed.test(candidate)) {
                    return;
                }
                attachCover(candidate);
                listener.onSuggestion(candidate);
            });
        } catch (Exception e) {
            log.error("Error streaming book suggestions for user: {}", userId, e);
            throw new RuntimeException("Failed to generate book suggestions. Please try again.", e);
        }
    }

    /** Preferences are learned from books the user has read, not from the reading list. */
    private static List<Book> readBooks(List<Book> allBooks) {
        return allBooks.stream().filter(b -> b.getListType() == ListType.COLLECTION).toList();
    }

    private void attachCover(BookCandidate candidate) {
        candidate.setCoverUrl(bookSearchService.findCoverUrl(
                candidate.getTitle(), candidate.getAuthor(), candidate.getIsbn()));
    }

    private String buildResponseMessage(IntakeResult intake, int collectionSize) {
        StringBuilder message = new StringBuilder();
        message.append("Based on your request");
        
        if (intake.getMood() != null || !intake.getGenres().isEmpty()) {
            message.append(" for ");
            if (intake.getMood() != null) {
                message.append(intake.getMood()).append(" ");
            }
            if (!intake.getGenres().isEmpty()) {
                message.append(String.join(", ", intake.getGenres()));
            }
        }
        
        if (collectionSize > 0) {
            message.append(" and your reading history");
        }
        
        message.append(", here are some personalized book recommendations for you:");
        
        return message.toString();
    }
}
