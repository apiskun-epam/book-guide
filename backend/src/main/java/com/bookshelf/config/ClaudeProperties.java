package com.bookshelf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Claude settings bound from the {@code anthropic.*} properties. */
@ConfigurationProperties(prefix = "anthropic")
public record ClaudeProperties(String model) {
}
