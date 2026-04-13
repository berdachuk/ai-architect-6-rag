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
import java.util.Locale;
import java.util.Set;

@Configuration
@Profile({"test", "e2e"})
public class TestAIConfig {

    private static final int DIM = 768;
    private static final Set<String> stopWords = Set.of(
            "what", "which", "how", "when", "where", "why", "can", "does", "is", "should", "could", "would", "the", "and", "for", "with", "from");
    private static final Locale LOCALE = Locale.ROOT;

    @Bean
    @Primary
    public ChatModel chatModel() {
        return prompt -> {
            String text = prompt.toString();
            // Echo the topic word (first significant noun) back in the stub answer so E2E
            // assertions on answer content can succeed without a real LLM.
            String topic = extractTopicWord(text);
            String answer = "stub answer for tests about " + topic
                    + " — cite only given context.";
            return ChatResponse.builder()
                    .generations(List.of(new Generation(new AssistantMessage(answer))))
                    .build();
        };
    }

    private static String extractTopicWord(String text) {
        if (text == null || text.isBlank()) {
            return "Medical";
        }
        // Look for the question — it typically contains the topic after "Question:".
        int qi = text.indexOf("Question:");
        int from = qi >= 0 ? qi + 9 : 0;
        // Grab the first word after "Question:" that is longer than 2 chars.
        // Spring AI lowercases the prompt internally, so capitalize to restore expected casing.
        String rest = text.substring(from).trim();
        String[] words = rest.split("\\s+");
        for (String w : words) {
            String cleaned = w.replaceAll("[^a-zA-Z]", "");
            if (cleaned.length() > 2 && !stopWords.contains(cleaned.toLowerCase(Locale.ROOT))) {
                return capitalize(cleaned);
            }
        }
        return "Medical";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
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
