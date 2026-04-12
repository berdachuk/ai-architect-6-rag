package com.berdachuk.docurag.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 wires Jackson 3 ({@code tools.jackson}) by default; ingestion and evaluation code
 * uses Jackson 2 ({@code com.fasterxml.jackson}). Register a {@link ObjectMapper} for that API.
 */
@Configuration
public class DocuRagJackson2Configuration {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
