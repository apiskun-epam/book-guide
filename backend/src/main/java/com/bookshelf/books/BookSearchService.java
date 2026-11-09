package com.bookshelf.books;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookSearchService {

    private static final String OPEN_LIBRARY_SEARCH_URL = "https://openlibrary.org/search.json";
    private static final String OPEN_LIBRARY_WORK_URL = "https://openlibrary.org/works/";
    private static final String SEARCH_FIELDS = "key,title,author_name,isbn,cover_i,first_publish_year,description";
    private static final Pattern WORK_KEY_PATTERN = Pattern.compile("^/works/(OL\\d+W)$");

    // JSON field keys
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_AUTHOR_NAME = "author_name";
    private static final String FIELD_ISBN = "isbn";
    private static final String FIELD_COVER_I = "cover_i";
    private static final String FIELD_FIRST_PUBLISH_YEAR = "first_publish_year";
    private static final String FIELD_DESCRIPTION = "description";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<BookSearchResult> searchBooks(String query) {
        try {
            String url = OPEN_LIBRARY_SEARCH_URL + "?q=" + query + "&limit=10&fields=" + SEARCH_FIELDS;
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode docs = root.get("docs");

            List<BookSearchResult> results = new ArrayList<>();
            Map<BookSearchResult, String> workKeys = new IdentityHashMap<>();
            if (docs != null && docs.isArray()) {
                for (JsonNode doc : docs) {
                    BookSearchResult result = new BookSearchResult();

                    // Title
                    if (doc.has(FIELD_TITLE)) {
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

                    // Description
                    if (doc.has(FIELD_DESCRIPTION)) {
                        result.setDescription(doc.get(FIELD_DESCRIPTION).asText());
                    }

                    if (result.getTitle() != null) {
                        results.add(result);
                    }
                }
            }

            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to search books", e);
        }
    }
}
