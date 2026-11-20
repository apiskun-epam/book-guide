package com.bookshelf.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;

/**
 * Shared {@link RestTemplate} for outbound catalog calls, with connect and read timeouts.
 * Open Library can take several seconds on heavier queries, so the read timeout is generous.
 */
@Configuration
@EnableConfigurationProperties(CatalogProperties.class)
public class HttpClientConfig {

    static final int CONNECT_TIMEOUT_MS = 3000;
    static final int READ_TIMEOUT_MS = 8000;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        RestTemplate restTemplate = new RestTemplate(factory);

        // Strictly encode URI variable values (including & = # +) so user input can't alter the query string.
        DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
        uriFactory.setEncodingMode(EncodingMode.TEMPLATE_AND_VALUES);
        restTemplate.setUriTemplateHandler(uriFactory);
        return restTemplate;
    }
}
