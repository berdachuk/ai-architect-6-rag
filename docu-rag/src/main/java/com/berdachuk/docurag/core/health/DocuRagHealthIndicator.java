package com.berdachuk.docurag.core.health;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class DocuRagHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DocuRagHealthIndicator.class);
    private static final Duration LLM_CACHE_UP = Duration.ofMinutes(5);
    private static final Duration LLM_CACHE_DOWN = Duration.ofSeconds(30);

    private final NamedParameterJdbcTemplate jdbc;
    private final Environment environment;
    @Nullable
    private final ChatModel chatModel;
    @Nullable
    private final EmbeddingModel embeddingModel;

    private final AtomicReference<CachedHealthResult> llmHealthCache = new AtomicReference<>();
    private final AtomicReference<CachedHealthResult> embeddingHealthCache = new AtomicReference<>();

    @Override
    public Health health() {
        Instant start = Instant.now();
        Map<String, Object> details = new HashMap<>();
        boolean up = true;

        Health dbHealth = checkDatabase();
        details.put("database", dbHealth.getDetails());
        if (!"UP".equals(dbHealth.getStatus().getCode())) {
            up = false;
            log.warn("DocuRAG health: database {}", dbHealth.getStatus());
        }

        Health vectorHealth = checkVectorStore();
        details.put("vectorStore", vectorHealth.getDetails());
        if (!"UP".equals(vectorHealth.getStatus().getCode())) {
            up = false;
            log.warn("DocuRAG health: vectorStore {}", vectorHealth.getStatus());
        }

        Health llmHealth = checkLlmModel();
        details.put("llm", llmHealth.getDetails());
        if (!"UP".equals(llmHealth.getStatus().getCode())) {
            up = false;
            log.warn("DocuRAG health: llm {}", llmHealth.getStatus());
        }

        Health embHealth = checkEmbeddingModel();
        details.put("embedding", embHealth.getDetails());
        if (!"UP".equals(embHealth.getStatus().getCode())) {
            up = false;
            log.warn("DocuRAG health: embedding {}", embHealth.getStatus());
        }

        details.put("checkDurationMs", Duration.between(start, Instant.now()).toMillis());
        details.put("timestamp", Instant.now().toString());

        Health.Builder builder = up ? Health.up() : Health.down();
        builder.withDetails(details);
        return builder.build();
    }

    private Health checkDatabase() {
        try {
            Integer one = jdbc.queryForObject("SELECT 1", Map.of(), Integer.class);
            if (one != null && one == 1) {
                Map<String, Object> d = new HashMap<>();
                d.put("status", "UP");
                d.put("description", "PostgreSQL application database (Flyway + JDBC)");
                return Health.up().withDetails(d).build();
            }
            return Health.down().withDetail("error", "Unexpected SELECT 1 result").build();
        } catch (Exception e) {
            log.debug("Database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build();
        }
    }

    private Health checkVectorStore() {
        try {
            Integer extensionCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM pg_catalog.pg_extension WHERE extname = 'vector'",
                    Map.of(),
                    Integer.class
            );

            Integer typeCount = jdbc.queryForObject("""
                            SELECT COUNT(*) FROM pg_catalog.pg_type t
                            JOIN pg_catalog.pg_namespace n ON t.typnamespace = n.oid
                            WHERE n.nspname = 'public' AND t.typname = 'vector'
                            """, Map.of(), Integer.class);

            Integer columnCount = jdbc.queryForObject("""
                            SELECT COUNT(*) FROM information_schema.columns
                            WHERE table_schema = 'public'
                              AND table_name = 'document_chunk'
                              AND column_name = 'embedding'
                            """, Map.of(), Integer.class);

            boolean extensionOk = (extensionCount != null && extensionCount > 0)
                    || (typeCount != null && typeCount > 0);
            boolean columnOk = columnCount != null && columnCount > 0;

            Map<String, Object> d = new HashMap<>();
            d.put("vectorExtensionAvailable", extensionOk);
            d.put("embeddingColumnExists", columnOk);
            if (extensionCount != null) {
                d.put("extensionCount", extensionCount);
            }
            if (typeCount != null) {
                d.put("vectorTypeCount", typeCount);
            }
            if (columnCount != null) {
                d.put("embeddingColumnCount", columnCount);
            }

            if (!extensionOk) {
                d.put("status", "DOWN");
                return Health.down().withDetails(d).withDetail("error", "pgvector extension not available").build();
            }
            if (!columnOk) {
                d.put("status", "DOWN");
                return Health.down().withDetails(d).withDetail("error", "document_chunk.embedding not found").build();
            }
            d.put("status", "UP");
            d.put("description", "pgvector + public.document_chunk.embedding");
            return Health.up().withDetails(d).build();
        } catch (Exception e) {
            log.debug("Vector store health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build();
        }
    }

    private Health checkLlmModel() {
        CachedHealthResult cached = llmHealthCache.get();
        if (cached != null && !cached.isExpired()) {
            return cached.health();
        }
        try {
            if (chatModel == null) {
                Health r = Health.down()
                        .withDetail("error", "ChatModel bean not available")
                        .withDetail("cached", false)
                        .build();
                cacheResult(llmHealthCache, r);
                return r;
            }
            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("modelType", chatModel.getClass().getSimpleName());
            details.put("cached", false);
            details.putAll(extractChatModelConfig());
            Health r = Health.up().withDetails(details).build();
            cacheResult(llmHealthCache, r);
            return r;
        } catch (Exception e) {
            Health r = Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("cached", false)
                    .build();
            cacheResult(llmHealthCache, r);
            return r;
        }
    }

    private Health checkEmbeddingModel() {
        CachedHealthResult cached = embeddingHealthCache.get();
        if (cached != null && !cached.isExpired()) {
            return cached.health();
        }
        try {
            if (embeddingModel == null) {
                Health r = Health.down()
                        .withDetail("error", "EmbeddingModel bean not available")
                        .withDetail("cached", false)
                        .build();
                cacheResult(embeddingHealthCache, r);
                return r;
            }
            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("modelType", embeddingModel.getClass().getSimpleName());
            details.put("dimensions", embeddingModel.dimensions());
            details.put("cached", false);
            details.putAll(extractEmbeddingModelConfig());
            Health r = Health.up().withDetails(details).build();
            cacheResult(embeddingHealthCache, r);
            return r;
        } catch (Exception e) {
            Health r = Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("cached", false)
                    .build();
            cacheResult(embeddingHealthCache, r);
            return r;
        }
    }

    private Map<String, Object> extractChatModelConfig() {
        Map<String, Object> config = new HashMap<>();
        String model = environment.getProperty("spring.ai.custom.chat.model");
        String baseUrl = environment.getProperty("spring.ai.custom.chat.base-url");
        String provider = environment.getProperty("spring.ai.custom.chat.provider", "openai-compatible");
        if (model != null && !model.isBlank()) {
            config.put("model", model);
        }
        if (baseUrl != null && !baseUrl.isBlank()) {
            config.put("baseUrl", baseUrl);
        }
        if (provider != null && !provider.isBlank()) {
            config.put("provider", provider);
        }
        return config;
    }

    private Map<String, Object> extractEmbeddingModelConfig() {
        Map<String, Object> config = new HashMap<>();
        String model = environment.getProperty("spring.ai.custom.embedding.model");
        String baseUrl = environment.getProperty("spring.ai.custom.embedding.base-url");
        String provider = environment.getProperty("spring.ai.custom.embedding.provider", "openai");
        String dim = environment.getProperty("spring.ai.custom.embedding.dimensions");
        if (model != null && !model.isBlank()) {
            config.put("model", model);
        }
        if (baseUrl != null && !baseUrl.isBlank()) {
            config.put("baseUrl", baseUrl);
        }
        if (provider != null && !provider.isBlank()) {
            config.put("provider", provider);
        }
        if (dim != null && !dim.isBlank()) {
            config.put("configuredDimensions", dim);
        }
        return config;
    }

    private void cacheResult(AtomicReference<CachedHealthResult> cache, Health health) {
        Duration ttl = "UP".equals(health.getStatus().getCode()) ? LLM_CACHE_UP : LLM_CACHE_DOWN;
        cache.set(new CachedHealthResult(health, Instant.now(), ttl));
    }

    private record CachedHealthResult(Health health, Instant timestamp, Duration cacheDuration) {

        boolean isExpired() {
            return Duration.between(timestamp, Instant.now()).compareTo(cacheDuration) >= 0;
        }
    }
}
