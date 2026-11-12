package com.bookshelf.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ClaudeProperties.class)
@Slf4j
public class ClaudeConfig {

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Bean
    public AnthropicClient anthropicClient() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Anthropic API key not configured. AI features will not work.");
        }

        return AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
