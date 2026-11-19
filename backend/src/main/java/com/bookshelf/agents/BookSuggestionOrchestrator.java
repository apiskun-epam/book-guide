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
    private final RankingAgent rankingAgent;
    private final BacklogGuard backlogGuard;

    /** Upper bound on Reading List books sent to the model in one request. */
    static final int MAX_BACKLOG_BOOKS = 50;

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
     * Books in {@code alreadySuggested} (earlier turns of this conversation) are told to the model
     * and also dropped by the guard, as are repeats within this reply.
     */
    public void streamSuggestions(String userId, String userPrompt, List<BookRef> alreadySuggested,
                                  SuggestionListener listener) {
        log.info("Starting streaming book suggestion workflow for user: {}", userId);

        try {
            IntakeResult intakeResult = intakeAgent.analyze(userPrompt);

            List<Book> allBooks = bookRepository.findByUserId(userId);
            List<Book> readBooks = readBooks(allBooks);
            PreferenceResult preferenceResult = preferenceAgent.analyze(readBooks);
            Predicate<BookCandidate> allowed = exclusionGuard.newSuggestionFilter(allBooks, alreadySuggested);

            listener.onMessage(buildResponseMessage(intakeResult, readBooks.size()));

            candidateAgent.streamCandidates(intakeResult, preferenceResult, alreadySuggested, candidate -> {
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

    /**
     * "Pick from my Reading List" mode: ranks books already on the user's Reading List for the
     * current request. The backlog guard keeps only picks that really are on the list, and the
     * book details come from the database. An empty list answers directly without calling Claude.
     */
    public void streamBacklogPicks(String userId, String userPrompt, SuggestionListener listener) {
        log.info("Starting backlog pick workflow for user: {}", userId);

        try {
            List<Book> allBooks = bookRepository.findByUserId(userId);
            List<Book> backlog = allBooks.stream()
                    .filter(b -> b.getListType() == ListType.READING_LIST)
                    .limit(MAX_BACKLOG_BOOKS)
                    .toList();

            if (backlog.isEmpty()) {
                listener.onMessage("Your Reading List is empty. Add a few books to it first, "
                        + "or switch to \"Find something new\" for fresh recommendations.");
                return;
            }

            IntakeResult intakeResult = intakeAgent.analyze(userPrompt);
            PreferenceResult preferenceResult = preferenceAgent.analyze(readBooks(allBooks));
            RankingResult ranking = rankingAgent.rank(intakeResult, preferenceResult, backlog);
            List<BookCandidate> picks = backlogGuard.resolve(ranking, backlog);

            if (picks.isEmpty()) {
                listener.onMessage("None of the books on your Reading List seem to fit this right now. "
                        + "Try rephrasing, or switch to \"Find something new\".");
                return;
            }

            listener.onMessage("Here " + (picks.size() == 1 ? "is the best pick" : "are my top picks")
                    + " from your Reading List:");
            picks.forEach(listener::onSuggestion);
        } catch (Exception e) {
            log.error("Error picking from reading list for user: {}", userId, e);
            throw new RuntimeException("Failed to pick from your reading list. Please try again.", e);
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
