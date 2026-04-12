package com.berdachuk.docurag.core.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile({"test", "e2e"})
public class TestAIConfig {

    private static final int DIM = 768;

    @Bean
    @Primary
    public ChatModel chatModel() {
        return prompt -> ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(
                        "stub answer for tests — cite only given context."))))
                .build();
    }

    @Bean
    @Primary
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> out = new ArrayList<>();
                int i = 0;
                for (String text : request.getInstructions()) {
                    out.add(new Embedding(stubVector(text), i++));
                }
                return new EmbeddingResponse(out);
            }

            @Override
            public float[] embed(org.springframework.ai.document.Document document) {
                return stubVector(document.getText());
            }

            @Override
            public int dimensions() {
                return DIM;
            }
        };
    }

    static float[] stubVector(String text) {
        float[] v = new float[DIM];
        int h = text == null ? 0 : text.hashCode();
        for (int i = 0; i < DIM; i++) {
            v[i] = (float) (((h + i * 31) % 10007) / 10007.0);
        }
        return v;
    }
}
