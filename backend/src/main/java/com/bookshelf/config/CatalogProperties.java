package com.bookshelf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Book catalog settings bound from {@code app.catalog.*}.
 *
 * @param language two-letter ISO 639-1 code sent to Open Library as {@code lang}. Fixed to English for now;
 *                 a future per-user locale can replace this value without touching the search code.
 */
@ConfigurationProperties(prefix = "app.catalog")
public record CatalogProperties(String language) {

    public static final String DEFAULT_LANGUAGE = "en";

    public CatalogProperties {
        if (language == null || language.isBlank()) {
            language = DEFAULT_LANGUAGE;
        }
    }
}
