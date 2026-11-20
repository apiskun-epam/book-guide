package com.bookshelf.books;

import com.bookshelf.config.CatalogProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookSearchService {

    private static final String OPEN_LIBRARY_SEARCH_URL = "https://openlibrary.org/search.json";
    private static final String OPEN_LIBRARY_WORK_URL = "https://openlibrary.org/works/";
    private static final String SEARCH_FIELDS = "key,title,author_name,isbn,cover_i,first_publish_year,description,editions";
    private static final String OPEN_LIBRARY_EDITION_URL = "https://openlibrary.org";
    private static final Pattern EDITION_KEY_PATTERN = Pattern.compile("^/books/OL\\d+M$");
    private static final Pattern WORK_KEY_PATTERN = Pattern.compile("^/works/(OL\\d+W)$");

    // JSON field keys
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_AUTHOR_NAME = "author_name";
    private static final String FIELD_ISBN = "isbn";
    private static final String FIELD_COVER_I = "cover_i";
    private static final String FIELD_FIRST_PUBLISH_YEAR = "first_publish_year";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_EDITIONS = "editions";

    /** Max time to wait, in total, for the optional per-result edition description lookups. */
    private static final long EDITION_LOOKUP_DEADLINE_MS = 3000;
    private static final ExecutorService EDITION_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final RestTemplate restTemplate;
    private final CatalogProperties catalogProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<BookSearchResult> searchBooks(String query) {
        try {
            // Open Library falls back to the caller's language when lang is omitted, so always pass it explicitly.
            // The query is a URI variable so the RestTemplate encodes it; never concatenate user input into the URL.
            String url = OPEN_LIBRARY_SEARCH_URL + "?q={q}&limit=10&fields=" + SEARCH_FIELDS + "&lang={lang}";
            String response = getWithOneRetryOnTimeout(url, query, catalogProperties.language());

            JsonNode root = objectMapper.readTree(response);
            JsonNode docs = root.get("docs");

            List<BookSearchResult> results = new ArrayList<>();
            // Results whose work description isn't English; filled in from edition lookups run in parallel below.
            List<PendingDescription> pending = new ArrayList<>();
            if (docs != null && docs.isArray()) {
                for (JsonNode doc : docs) {
                    BookSearchResult result = new BookSearchResult();

                    // Title: prefer the best-matching edition for the configured language (Open Library ranks
                    // editions by language), falling back to the work title.
                    String editionTitle = bestEditionTitle(doc);
                    if (editionTitle != null) {
                        result.setTitle(editionTitle);
                    } else if (doc.has(FIELD_TITLE)) {
                        result.setTitle(doc.get(FIELD_TITLE).asText());
                    }

                    // Author
                    if (doc.has(FIELD_AUTHOR_NAME) && doc.get(FIELD_AUTHOR_NAME).isArray() && doc.get(FIELD_AUTHOR_NAME).size() > 0) {
                        result.setAuthor(doc.get(FIELD_AUTHOR_NAME).get(0).asText());
                    }

                    // ISBN
                    if (doc.has(FIELD_ISBN) && doc.get(FIELD_ISBN).isArray() && doc.get(FIELD_ISBN).size() > 0) {
                        result.setIsbn(doc.get(FIELD_ISBN).get(0).asText());
                    }

                    // Cover URL
                    if (doc.has(FIELD_COVER_I)) {
                        String coverId = doc.get(FIELD_COVER_I).asText();
                        result.setCoverUrl("https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg");
                    }

                    // Publish Year
                    if (doc.has(FIELD_FIRST_PUBLISH_YEAR)) {
                        result.setPublishYear(doc.get(FIELD_FIRST_PUBLISH_YEAR).asInt());
                    }

                    // Description: English only. Work descriptions are not language-tagged, so fall back to the
                    // best edition's description when the work's is in another language; otherwise leave it empty.
                    String workDescription = descriptionText(doc.get(FIELD_DESCRIPTION));
                    if (EnglishText.looksEnglish(workDescription)) {
                        result.setDescription(workDescription);
                    } else {
                        pending.add(new PendingDescription(result,
                                CompletableFuture.supplyAsync(() -> fetchEditionDescription(doc), EDITION_EXECUTOR)));
                    }

                    if (result.getTitle() != null) {
                        results.add(result);
                    }
                }
            }

            applyEditionDescriptions(pending);
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to search books", e);
        }
    }

    private record PendingDescription(BookSearchResult result, CompletableFuture<String> future) {
    }

    /**
     * Waits (bounded) for the parallel edition lookups. Descriptions are optional, so anything that is slow or
     * failed is left empty rather than delaying or failing the search.
     */
    private void applyEditionDescriptions(List<PendingDescription> pending) {
        if (pending.isEmpty()) {
            return;
        }
        try {
            CompletableFuture.allOf(pending.stream().map(PendingDescription::future).toArray(CompletableFuture[]::new))
                    .get(EDITION_LOOKUP_DEADLINE_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("Edition description lookups exceeded {} ms; returning results without the slow ones",
                    EDITION_LOOKUP_DEADLINE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.warn("Edition description lookup failed", e);
        }
        for (PendingDescription p : pending) {
            if (p.future().isDone() && !p.future().isCompletedExceptionally()) {
                p.result().setDescription(p.future().join());
            } else {
                p.future().cancel(true);
            }
        }
    }

    /** One retry, only for I/O timeouts/connection errors; Open Library is occasionally slow on a first attempt. */
    private String getWithOneRetryOnTimeout(String url, Object... uriVariables) {
        try {
            return restTemplate.getForObject(url, String.class, uriVariables);
        } catch (ResourceAccessException e) {
            log.warn("Open Library request failed ({}), retrying once", e.getMessage());
            return restTemplate.getForObject(url, String.class, uriVariables);
        }
    }

    /** Best-effort: the top-ranked (for {@code lang}) edition's own description, if English. Never throws. */
    private String fetchEditionDescription(JsonNode doc) {
        JsonNode first = doc.path(FIELD_EDITIONS).path("docs").path(0);
        String key = first.path("key").asText("");
        if (!EDITION_KEY_PATTERN.matcher(key).matches()) {
            return null;
        }
        try {
            String response = restTemplate.getForObject(OPEN_LIBRARY_EDITION_URL + key + ".json", String.class);
            if (response == null) {
                return null;
            }
            String description = descriptionText(objectMapper.readTree(response).get(FIELD_DESCRIPTION));
            return EnglishText.looksEnglish(description) ? description : null;
        } catch (Exception e) {
            log.warn("Edition description lookup failed for '{}'", key, e);
            return null;
        }
    }

    /** Open Library descriptions are either a plain string or an object like {"type": "...", "value": "..."}. */
    private static String descriptionText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.isObject() ? node.path("value").asText("") : node.asText("");
        return text.isBlank() ? null : text;
    }

    private static String bestEditionTitle(JsonNode doc) {
        JsonNode editions = doc.path(FIELD_EDITIONS).path("docs");
        if (editions.isArray() && !editions.isEmpty() && editions.get(0).hasNonNull(FIELD_TITLE)) {
            String title = editions.get(0).get(FIELD_TITLE).asText();
            return title.isBlank() ? null : title;
        }
        return null;
    }

    /**
     * Best-effort cover lookup for a known book. Prefers an ISBN match, falls back to title and author.
     * Returns null when no cover is found or the lookup fails; never throws.
     */
    public String findCoverUrl(String title, String author, String isbn) {
        try {
            String query;
            if (isbn != null && !isbn.isBlank()) {
                query = "isbn:" + isbn.trim();
            } else if (title != null && !title.isBlank()) {
                query = "title:\"" + title.replace("\"", "") + "\""
                        + (author != null && !author.isBlank() ? " author:\"" + author.replace("\"", "") + "\"" : "");
            } else {
                return null;
            }
            String coverId = fetchFirstCoverId(query);
            if (coverId == null && isbn != null && !isbn.isBlank() && title != null && !title.isBlank()) {
                coverId = fetchFirstCoverId("title:\"" + title.replace("\"", "") + "\""
                        + (author != null && !author.isBlank() ? " author:\"" + author.replace("\"", "") + "\"" : ""));
            }
            return coverId == null ? null : "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg";
        } catch (Exception e) {
            log.warn("Cover lookup failed for '{}'", title, e);
            return null;
        }
    }

    private String fetchFirstCoverId(String query) throws Exception {
        String response = restTemplate.getForObject(
                OPEN_LIBRARY_SEARCH_URL + "?q={q}&limit=5&fields=cover_i", String.class, query);
        if (response == null) {
            return null;
        }
        JsonNode docs = objectMapper.readTree(response).get("docs");
        if (docs != null && docs.isArray()) {
            for (JsonNode doc : docs) {
                if (doc.hasNonNull(FIELD_COVER_I)) {
                    return doc.get(FIELD_COVER_I).asText();
                }
            }
        }
        return null;
    }
}
