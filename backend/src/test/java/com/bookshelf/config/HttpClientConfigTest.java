package com.bookshelf.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpClientConfigTest {

    @Test
    void encodesQueryVariablesSoUserInputCannotAddParameters() {
        RestTemplate restTemplate = new HttpClientConfig().restTemplate();

        URI uri = restTemplate.getUriTemplateHandler()
                .expand("https://openlibrary.org/search.json?q={q}&limit=10", "a&limit=100#frag +b");

        assertEquals("q=a%26limit%3D100%23frag%20%2Bb&limit=10", uri.getRawQuery());
    }
}
