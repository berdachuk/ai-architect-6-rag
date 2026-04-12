package com.berdachuk.docurag.core.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test & !e2e")
@EnableConfigurationProperties({CustomChatProperties.class, CustomEmbeddingProperties.class})
public class DocuRagAiConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "spring.ai.custom.chat", name = "base-url")
    @Qualifier("primaryChatModel")
    public ChatModel primaryChatModel(CustomChatProperties chat) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(trimTrailingSlash(chat.getBaseUrl()))
                .apiKey(sanitizeApiKey(chat.getApiKey()))
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(chat.getModel())
                .temperature(chat.getTemperature())
                .maxTokens(chat.getMaxTokens())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "spring.ai.custom.embedding", name = "base-url")
    @Qualifier("primaryEmbeddingModel")
    public EmbeddingModel primaryEmbeddingModel(CustomEmbeddingProperties embedding) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(trimTrailingSlash(embedding.getBaseUrl()))
                .apiKey(sanitizeApiKey(embedding.getApiKey()))
                .build();
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embedding.getModel())
                .dimensions(embedding.getDimensions())
                .build();
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "spring.ai.custom.chat", name = "base-url")
    public ChatClient chatClient(@Qualifier("primaryChatModel") ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** ExpertMatch-style placeholders ({@code none}, empty) → no API key for local Ollama. */
    private static String sanitizeApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        if ("none".equalsIgnoreCase(apiKey.trim())) {
            return "";
        }
        return apiKey;
    }
}
