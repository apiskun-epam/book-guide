package com.bookshelf.books;

import com.bookshelf.config.CatalogProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookSearchServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private CatalogProperties catalogProperties = new CatalogProperties("en");

    @InjectMocks
    private BookSearchService service;

    private void stubResponse(String json) {
        when(restTemplate.getForObject(anyString(), eq(String.class), any(Object[].class))).thenReturn(json);
    }

    @Test
    void mapsAllFieldsFromFullDoc() {
        stubResponse("""
                {"docs":[{"key":"/works/OL1W","title":"Dune","author_name":["Frank Herbert","Other"],
                "isbn":["111","222"],"cover_i":42,"first_publish_year":1965,"description":"Spice"}]}
                """);

        List<BookSearchResult> results = service.searchBooks("dune");

        assertEquals(1, results.size());
        BookSearchResult r = results.get(0);
        assertEquals("Dune", r.getTitle());
        assertEquals("Frank Herbert", r.getAuthor());
        assertEquals("111", r.getIsbn());
        assertEquals("https://covers.openlibrary.org/b/id/42-M.jpg", r.getCoverUrl());
        assertEquals(1965, r.getPublishYear());
        assertEquals("Spice", r.getDescription());
    }

    @Test
    void leavesOptionalFieldsNullWhenMissing() {
        stubResponse("{\"docs\":[{\"title\":\"Bare\"}]}");

        BookSearchResult r = service.searchBooks("bare").get(0);

        assertEquals("Bare", r.getTitle());
        assertNull(r.getAuthor());
        assertNull(r.getIsbn());
        assertNull(r.getCoverUrl());
        assertNull(r.getPublishYear());
        assertNull(r.getDescription());
    }

    @Test
    void ignoresEmptyAuthorAndIsbnArrays() {
        stubResponse("{\"docs\":[{\"title\":\"T\",\"author_name\":[],\"isbn\":[]}]}");

        BookSearchResult r = service.searchBooks("t").get(0);

        assertNull(r.getAuthor());
        assertNull(r.getIsbn());
    }

    @Test
    void skipsDocsWithoutTitle() {
        stubResponse("{\"docs\":[{\"author_name\":[\"No Title\"]},{\"title\":\"Has Title\"}]}");

        List<BookSearchResult> results = service.searchBooks("x");

        assertEquals(1, results.size());
        assertEquals("Has Title", results.get(0).getTitle());
    }

    @Test
    void preservesOrderOfResults() {
        stubResponse("{\"docs\":[{\"title\":\"A\"},{\"title\":\"B\"},{\"title\":\"C\"}]}");

        List<String> titles = service.searchBooks("x").stream().map(BookSearchResult::getTitle).toList();

        assertEquals(List.of("A", "B", "C"), titles);
    }

    @Test
    void returnsEmptyListWhenDocsEmpty() {
        stubResponse("{\"docs\":[]}");

        assertTrue(service.searchBooks("nothing").isEmpty());
    }

    @Test
    void returnsEmptyListWhenDocsMissingOrNotArray() {
        stubResponse("{\"numFound\":0}");
        assertTrue(service.searchBooks("a").isEmpty());

        stubResponse("{\"docs\":\"oops\"}");
        assertTrue(service.searchBooks("b").isEmpty());
    }

    @Test
    void callsOpenLibraryWithQueryLimitAndFields() {
        stubResponse("{\"docs\":[]}");

        service.searchBooks("foundation");

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).getForObject(url.capture(), eq(String.class), eq("foundation"), eq("en"));
        assertTrue(url.getValue().startsWith("https://openlibrary.org/search.json?q={q}"));
        assertTrue(url.getValue().contains("&limit=10"));
        assertTrue(url.getValue().contains("fields="));
    }

    @Test
    void alwaysRequestsEnglishRegardlessOfCaller() {
        stubResponse("{\"docs\":[]}");

        service.searchBooks("last wish");

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).getForObject(url.capture(), eq(String.class), eq("last wish"), eq("en"));
        assertTrue(url.getValue().contains("&lang={lang}"));
        assertTrue(url.getValue().contains("editions"));
    }

    @Test
    void prefersBestEditionTitleOverWorkTitle() {
        stubResponse("""
                {"docs":[{"title":"Ostatnie Życzenie","editions":{"docs":[{"title":"The Last Wish"}]}}]}
                """);

        assertEquals("The Last Wish", service.searchBooks("last wish").get(0).getTitle());
    }

    @Test
    void fallsBackToWorkTitleWhenNoUsableEdition() {
        stubResponse("""
                {"docs":[{"title":"Work Title","editions":{"docs":[]}},
                         {"title":"Other","editions":{"docs":[{"title":" "}]}}]}
                """);

        List<String> titles = service.searchBooks("x").stream().map(BookSearchResult::getTitle).toList();

        assertEquals(List.of("Work Title", "Other"), titles);
    }

    private static final String POLISH = "Pozycja, która przedstawia początki historii słynnego Geralta z Rivii. "
            + "Sięgnij po tę lekturę i zanurz się w świecie, pełnym niebezpiecznych stworzeń.";

    @Test
    void keepsEnglishWorkDescriptionWithoutExtraCalls() {
        stubResponse("{\"docs\":[{\"title\":\"T\",\"description\":\"Geralt is a witcher who hunts the monsters of the land.\"}]}");

        assertEquals("Geralt is a witcher who hunts the monsters of the land.",
                service.searchBooks("t").get(0).getDescription());
        verify(restTemplate).getForObject(anyString(), eq(String.class), any(Object[].class));
    }

    @Test
    void usesEnglishEditionDescriptionWhenWorkDescriptionIsPolish() {
        when(restTemplate.getForObject(anyString(), eq(String.class), any(Object[].class))).thenReturn(
                "{\"docs\":[{\"title\":\"Ostatnie życzenie\",\"description\":\"" + POLISH + "\","
                        + "\"editions\":{\"docs\":[{\"key\":\"/books/OL1M\",\"title\":\"The Last Wish\"}]}}]}",
                "{\"description\":{\"type\":\"/type/text\",\"value\":\"Geralt of Rivia is a witcher, and he hunts the monsters.\"}}");

        BookSearchResult r = service.searchBooks("last wish").get(0);

        assertEquals("The Last Wish", r.getTitle());
        assertEquals("Geralt of Rivia is a witcher, and he hunts the monsters.", r.getDescription());
        verify(restTemplate).getForObject(eq("https://openlibrary.org/books/OL1M.json"), eq(String.class), any(Object[].class));
    }

    @Test
    void leavesDescriptionEmptyWhenNothingEnglishIsAvailable() {
        when(restTemplate.getForObject(anyString(), eq(String.class), any(Object[].class))).thenReturn(
                "{\"docs\":[{\"title\":\"T\",\"description\":\"" + POLISH + "\","
                        + "\"editions\":{\"docs\":[{\"key\":\"/books/OL1M\"}]}}]}",
                "{\"description\":\"" + POLISH + "\"}");

        assertNull(service.searchBooks("t").get(0).getDescription());
    }

    @Test
    void leavesDescriptionEmptyWhenEditionLookupFailsOrKeyIsInvalid() {
        when(restTemplate.getForObject(anyString(), eq(String.class), any(Object[].class)))
                .thenReturn("{\"docs\":[{\"title\":\"A\",\"description\":\"" + POLISH + "\","
                        + "\"editions\":{\"docs\":[{\"key\":\"/books/OL1M\"}]}},"
                        + "{\"title\":\"B\",\"description\":\"" + POLISH + "\","
                        + "\"editions\":{\"docs\":[{\"key\":\"../evil\"}]}}]}")
                .thenThrow(new RestClientException("boom"));

        List<BookSearchResult> results = service.searchBooks("x");

        assertNull(results.get(0).getDescription());
        assertNull(results.get(1).getDescription());
    }

    @Test
    void englishTextHeuristic() {
        assertTrue(EnglishText.looksEnglish("A witcher who hunts the monsters of the land."));
        assertTrue(EnglishText.looksEnglish("Short blurb"));
        assertFalse(EnglishText.looksEnglish(POLISH));
        assertFalse(EnglishText.looksEnglish(" "));
        assertFalse(EnglishText.looksEnglish(null));
    }

    @Test
    void catalogPropertiesDefaultToEnglishWhenBlank() {
        assertEquals("en", new CatalogProperties(null).language());
        assertEquals("en", new CatalogProperties(" ").language());
        assertEquals("pl", new CatalogProperties("pl").language());
    }

    @Test
    void wrapsHttpFailureInRuntimeException() {
        RestClientException cause = new RestClientException("boom");
        when(restTemplate.getForObject(anyString(), eq(String.class), any(Object[].class))).thenThrow(cause);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.searchBooks("x"));

        assertEquals("Failed to search books", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void retriesOnceWhenTheFirstAttemptTimesOut() {
        when(restTemplate.getForObject(anyString(), eq(String.class), any(Object[].class)))
                .thenThrow(new ResourceAccessException("Read timed out"))
                .thenReturn("{\"docs\":[{\"title\":\"Dune\"}]}");

        List<BookSearchResult> results = service.searchBooks("dune");

        assertEquals("Dune", results.get(0).getTitle());
        verify(restTemplate, times(2)).getForObject(anyString(), eq(String.class), any(Object[].class));
    }

    @Test
    void failsWhenTheRetryTimesOutToo() {
        when(restTemplate.getForObject(anyString(), eq(String.class), any(Object[].class)))
                .thenThrow(new ResourceAccessException("Read timed out"));

        assertThrows(RuntimeException.class, () -> service.searchBooks("dune"));
        verify(restTemplate, times(2)).getForObject(anyString(), eq(String.class), any(Object[].class));
    }

    @Test
    void doesNotRetryNonTimeoutFailures() {
        when(restTemplate.getForObject(anyString(), eq(String.class), any(Object[].class)))
                .thenThrow(new RestClientException("500"));

        assertThrows(RuntimeException.class, () -> service.searchBooks("dune"));
        verify(restTemplate).getForObject(anyString(), eq(String.class), any(Object[].class));
    }

    @Test
    void wrapsMalformedJsonInRuntimeException() {
        stubResponse("not json {");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.searchBooks("x"));

        assertEquals("Failed to search books", ex.getMessage());
    }

    @Test
    void wrapsNullResponseInRuntimeException() {
        stubResponse(null);

        assertThrows(RuntimeException.class, () -> service.searchBooks("x"));
    }

    @Test
    void findCoverUrlBuildsUrlFromFirstDocWithCover() {
        when(restTemplate.getForObject(anyString(), eq(String.class), eq("isbn:123")))
                .thenReturn("{\"docs\":[{\"title\":\"x\"},{\"cover_i\":77}]}");

        assertEquals("https://covers.openlibrary.org/b/id/77-M.jpg", service.findCoverUrl("T", "A", "123"));
    }

    @Test
    void findCoverUrlReturnsNullWhenNoCoverOrFailure() {
        when(restTemplate.getForObject(anyString(), eq(String.class), any(Object[].class)))
                .thenReturn("{\"docs\":[]}")
                .thenThrow(new RestClientException("boom"));

        assertNull(service.findCoverUrl("T", "A", null));
        assertNull(service.findCoverUrl("T", "A", null));
        assertNull(service.findCoverUrl(null, null, null));
    }
}
