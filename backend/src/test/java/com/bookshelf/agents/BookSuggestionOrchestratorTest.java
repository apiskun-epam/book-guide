package com.bookshelf.agents;

import com.bookshelf.books.Book;
import com.bookshelf.books.BookRepository;
import com.bookshelf.books.BookSearchService;
import com.bookshelf.books.ListType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookSuggestionOrchestratorTest {

    private static final String USER = "uid-a";

    @Mock private IntakeAgent intakeAgent;
    @Mock private PreferenceAgent preferenceAgent;
    @Mock private CandidateAgent candidateAgent;
    @Mock private BookRepository bookRepository;
    @Mock private BookSearchService bookSearchService;
    @Mock private RankingAgent rankingAgent;

    private BookSuggestionOrchestrator orchestrator;
    private final IntakeResult intake = new IntakeResult();
    private final PreferenceResult preference = new PreferenceResult();

    @BeforeEach
    void setUp() {
        orchestrator = new BookSuggestionOrchestrator(intakeAgent, preferenceAgent, candidateAgent,
                bookRepository, bookSearchService, new ExclusionGuard(), rankingAgent, new BacklogGuard());
        intake.setGenres(List.of("fantasy"));
        lenient().when(intakeAgent.analyze(any())).thenReturn(intake);
        lenient().when(bookRepository.findByUserId(USER)).thenReturn(List.of(
                Book.builder().title("Dune").author("Frank Herbert").isbn("111").listType(ListType.COLLECTION).build(),
                Book.builder().title("Emma").author("Jane Austen").isbn("222").listType(ListType.READING_LIST).build()));
        lenient().when(preferenceAgent.analyze(any())).thenReturn(preference);
    }

    private static BookCandidate candidate(String title, String author, String isbn) {
        return new BookCandidate(title, author, isbn, "Horror", "A real description of the book.", "Matches your taste in the genre.", null);
    }

    private List<BookCandidate> generated() {
        return new ArrayList<>(List.of(
                candidate("Dune", "Frank Herbert", "111"),
                candidate("Emma", "Jane Austen", "222"),
                candidate("Ulysses", "James Joyce", "333")));
    }

    @Test
    void generateSuggestionsDropsBooksFromBothLists() {
        CandidateResult result = new CandidateResult();
        result.setCandidates(generated());
        when(candidateAgent.generateCandidates(any(), any(), any())).thenReturn(result);

        SuggestionResponse response = orchestrator.generateSuggestions(USER, "something");

        assertEquals(List.of("Ulysses"), response.getSuggestions().stream().map(BookCandidate::getTitle).toList());
    }

    @Test
    void preferenceAgentOnlySeesCollectionBooks() {
        CandidateResult result = new CandidateResult();
        result.setCandidates(new ArrayList<>());
        when(candidateAgent.generateCandidates(any(), any(), any())).thenReturn(result);

        orchestrator.generateSuggestions(USER, "something");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Book>> captor = ArgumentCaptor.forClass(List.class);
        verify(preferenceAgent).analyze(captor.capture());
        assertEquals(List.of("Dune"), captor.getValue().stream().map(Book::getTitle).toList());
    }

    @Test
    void streamSuggestionsDropsBooksFromBothLists() {
        doAnswer(inv -> {
            Consumer<BookCandidate> consumer = inv.getArgument(3);
            generated().forEach(consumer);
            return null;
        }).when(candidateAgent).streamCandidates(any(), any(), any(), any());
        List<String> streamed = new ArrayList<>();

        orchestrator.streamSuggestions(USER, "something", List.of(), new SuggestionListener() {
            @Override public void onMessage(String message) { }
            @Override public void onSuggestion(BookCandidate candidate) { streamed.add(candidate.getTitle()); }
        });

        assertEquals(List.of("Ulysses"), streamed);
    }

    @Test
    void streamSuggestionsDropsBooksAlreadyProposedInConversation() {
        doAnswer(inv -> {
            Consumer<BookCandidate> consumer = inv.getArgument(3);
            List.of(candidate("Ulysses", "James Joyce", "333"),
                    candidate("the hobbit", "J.R.R. Tolkien", null),
                    candidate("Neuromancer", "William Gibson", "444")).forEach(consumer);
            return null;
        }).when(candidateAgent).streamCandidates(any(), any(), any(), any());
        List<String> streamed = new ArrayList<>();

        orchestrator.streamSuggestions(USER, "more", List.of(
                new BookRef("Ulysses", "x", "333"),
                new BookRef("The Hobbit", "J.R.R. Tolkien", null)), new SuggestionListener() {
            @Override public void onMessage(String message) { }
            @Override public void onSuggestion(BookCandidate candidate) { streamed.add(candidate.getTitle()); }
        });

        assertEquals(List.of("Neuromancer"), streamed);
    }

    @Test
    void streamSuggestionsTellsTheModelToSkipBooksFromBothListsAndEarlierProposals() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BookRef>> avoid = ArgumentCaptor.forClass(List.class);

        orchestrator.streamSuggestions(USER, "a book", List.of(new BookRef("Ulysses", "James Joyce", "333")),
                new SuggestionListener() {
                    @Override public void onMessage(String message) { }
                    @Override public void onSuggestion(BookCandidate candidate) { }
                });

        verify(candidateAgent).streamCandidates(any(), any(), avoid.capture(), any());
        assertEquals(List.of("Dune", "Emma", "Ulysses"), avoid.getValue().stream().map(BookRef::getTitle).toList());
    }

    @Test
    void streamSuggestionsDropsPlaceholderCandidatesFromTheModel() {
        BookCandidate fake = new BookCandidate("Hoff the Back of a Lorry", "Unknown", null, "Horror",
                "placeholder", "placeholder", null);
        doAnswer(inv -> {
            Consumer<BookCandidate> consumer = inv.getArgument(3);
            List.of(fake, candidate("Neuromancer", "William Gibson", "444")).forEach(consumer);
            return null;
        }).when(candidateAgent).streamCandidates(any(), any(), any(), any());
        List<String> streamed = new ArrayList<>();

        orchestrator.streamSuggestions(USER, "a book", List.of(), new SuggestionListener() {
            @Override public void onMessage(String message) { }
            @Override public void onSuggestion(BookCandidate candidate) { streamed.add(candidate.getTitle()); }
        });

        assertEquals(List.of("Neuromancer"), streamed);
    }

    @Test
    void streamSuggestionsDropsRepeatsWithinTheSameReply() {
        doAnswer(inv -> {
            Consumer<BookCandidate> consumer = inv.getArgument(3);
            List.of(candidate("Neuromancer", "William Gibson", "444"),
                    candidate("neuromancer", "William Gibson", null)).forEach(consumer);
            return null;
        }).when(candidateAgent).streamCandidates(any(), any(), any(), any());
        List<String> streamed = new ArrayList<>();

        orchestrator.streamSuggestions(USER, "more", List.of(), new SuggestionListener() {
            @Override public void onMessage(String message) { }
            @Override public void onSuggestion(BookCandidate candidate) { streamed.add(candidate.getTitle()); }
        });

        assertEquals(List.of("Neuromancer"), streamed);
    }

    // --- "Pick from my Reading List" mode ---

    private static final class RecordingListener implements SuggestionListener {
        final List<String> messages = new ArrayList<>();
        final List<BookCandidate> suggestions = new ArrayList<>();

        @Override public void onMessage(String message) { messages.add(message); }
        @Override public void onSuggestion(BookCandidate candidate) { suggestions.add(candidate); }
    }

    private static RankingResult picks(String... refs) {
        RankingResult result = new RankingResult();
        for (String ref : refs) {
            result.getPicks().add(new RankingResult.Pick(ref, "fits tonight"));
        }
        return result;
    }

    @Test
    void backlogModeRanksOnlyReadingListBooksAndSkipsUnknownRefs() {
        when(rankingAgent.rank(any(), any(), any())).thenReturn(picks("b1", "b7"));
        RecordingListener listener = new RecordingListener();

        orchestrator.streamBacklogPicks(USER, "something light", listener);

        assertEquals(List.of("Emma"), listener.suggestions.stream().map(BookCandidate::getTitle).toList());
        assertEquals("fits tonight", listener.suggestions.get(0).getWhySuggested());
        ArgumentCaptor<List<Book>> backlog = ArgumentCaptor.forClass(List.class);
        verify(rankingAgent).rank(any(), any(), backlog.capture());
        assertEquals(List.of("Emma"), backlog.getValue().stream().map(Book::getTitle).toList());
    }

    @Test
    void backlogModeWithEmptyReadingListDoesNotCallClaude() {
        when(bookRepository.findByUserId(USER)).thenReturn(List.of(
                Book.builder().title("Dune").author("Frank Herbert").listType(ListType.COLLECTION).build()));
        RecordingListener listener = new RecordingListener();

        orchestrator.streamBacklogPicks(USER, "something light", listener);

        assertEquals(1, listener.messages.size());
        assertTrue(listener.suggestions.isEmpty());
        verifyNoInteractions(rankingAgent);
    }

    @Test
    void backlogModeCapsBooksSentToTheModel() {
        List<Book> many = new ArrayList<>();
        for (int i = 0; i < BookSuggestionOrchestrator.MAX_BACKLOG_BOOKS + 10; i++) {
            many.add(Book.builder().title("Book " + i).author("A").listType(ListType.READING_LIST).build());
        }
        when(bookRepository.findByUserId(USER)).thenReturn(many);
        when(rankingAgent.rank(any(), any(), any())).thenReturn(picks());
        RecordingListener listener = new RecordingListener();

        orchestrator.streamBacklogPicks(USER, "anything", listener);

        ArgumentCaptor<List<Book>> backlog = ArgumentCaptor.forClass(List.class);
        verify(rankingAgent).rank(any(), any(), backlog.capture());
        assertEquals(BookSuggestionOrchestrator.MAX_BACKLOG_BOOKS, backlog.getValue().size());
        assertTrue(listener.suggestions.isEmpty());
        assertEquals(1, listener.messages.size());
    }

    @Test
    void backlogModeOnlyLoadsTheAuthenticatedUsersBooks() {
        when(rankingAgent.rank(any(), any(), any())).thenReturn(picks("b1"));

        orchestrator.streamBacklogPicks(USER, "something", new RecordingListener());

        verify(bookRepository).findByUserId(USER);
        verify(bookRepository, never()).findByUserId("uid-b");
    }
}
