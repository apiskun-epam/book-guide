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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookSuggestionOrchestratorTest {

    private static final String USER = "uid-a";

    @Mock private IntakeAgent intakeAgent;
    @Mock private PreferenceAgent preferenceAgent;
    @Mock private CandidateAgent candidateAgent;
    @Mock private BookRepository bookRepository;
    @Mock private BookSearchService bookSearchService;

    private BookSuggestionOrchestrator orchestrator;
    private final IntakeResult intake = new IntakeResult();
    private final PreferenceResult preference = new PreferenceResult();

    @BeforeEach
    void setUp() {
        orchestrator = new BookSuggestionOrchestrator(intakeAgent, preferenceAgent, candidateAgent,
                bookRepository, bookSearchService, new ExclusionGuard());
        intake.setGenres(List.of("fantasy"));
        when(intakeAgent.analyze(any())).thenReturn(intake);
        when(bookRepository.findByUserId(USER)).thenReturn(List.of(
                Book.builder().title("Dune").author("Frank Herbert").isbn("111").listType(ListType.COLLECTION).build(),
                Book.builder().title("Emma").author("Jane Austen").isbn("222").listType(ListType.READING_LIST).build()));
        when(preferenceAgent.analyze(any())).thenReturn(preference);
    }

    private static BookCandidate candidate(String title, String author, String isbn) {
        return new BookCandidate(title, author, isbn, null, null, null, null);
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
        when(candidateAgent.generateCandidates(any(), any())).thenReturn(result);

        SuggestionResponse response = orchestrator.generateSuggestions(USER, "something");

        assertEquals(List.of("Ulysses"), response.getSuggestions().stream().map(BookCandidate::getTitle).toList());
    }

    @Test
    void preferenceAgentOnlySeesCollectionBooks() {
        CandidateResult result = new CandidateResult();
        result.setCandidates(new ArrayList<>());
        when(candidateAgent.generateCandidates(any(), any())).thenReturn(result);

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
}
