package com.bookshelf.agents;

import com.bookshelf.books.Book;
import com.bookshelf.books.BookRepository;
import com.bookshelf.books.BookSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookSuggestionOrchestrator {

    private final IntakeAgent intakeAgent;
    private final PreferenceAgent preferenceAgent;
    private final CandidateAgent candidateAgent;
    private final BookRepository bookRepository;
    private final BookSearchService bookSearchService;

    /**
     * Orchestrates the multi-agent workflow to generate book suggestions.
     * 
     * Pipeline:
     * 1. Intake agent - analyzes user's current request
     * 2. Preference agent - analyzes user's book collection for preferences
     * 3. Candidate agent - generates 5 book recommendations
     * 
     * @param userId The authenticated user's Firebase UID
     * @param userPrompt The user's book request message
     * @return SuggestionResponse containing 5 book candidates
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
            List<Book> userBooks = bookRepository.findByUserId(userId);
            PreferenceResult preferenceResult = preferenceAgent.analyze(userBooks);
            log.debug("Preference result: {}", preferenceResult);
            
            // Step 3: Candidate - generate recommendations
            log.debug("Step 3: Running candidate agent");
            CandidateResult candidateResult = candidateAgent.generateCandidates(intakeResult, preferenceResult);
            log.debug("Generated {} candidates", candidateResult.getCandidates().size());
            
            candidateResult.getCandidates().forEach(this::attachCover);

            // Build response
            String message = buildResponseMessage(intakeResult, userBooks.size());
            SuggestionResponse response = new SuggestionResponse(
                    candidateResult.getCandidates(),
                    message
            );
            
            log.info("Successfully generated {} suggestions for user: {}", 
                    candidateResult.getCandidates().size(), userId);
            
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

            List<Book> userBooks = bookRepository.findByUserId(userId);
            PreferenceResult preferenceResult = preferenceAgent.analyze(userBooks);

            listener.onMessage(buildResponseMessage(intakeResult, userBooks.size()));

            candidateAgent.streamCandidates(intakeResult, preferenceResult, candidate -> {
                attachCover(candidate);
                listener.onSuggestion(candidate);
            });
        } catch (Exception e) {
            log.error("Error streaming book suggestions for user: {}", userId, e);
            throw new RuntimeException("Failed to generate book suggestions. Please try again.", e);
        }
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
        
        message.append(", here are 5 personalized book recommendations for you:");
        
        return message.toString();
    }
}
