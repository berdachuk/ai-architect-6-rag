package com.berdachuk.docurag.llm.internal;

import com.berdachuk.docurag.core.config.CustomChatProperties;
import com.berdachuk.docurag.core.config.DocuRagProperties;
import com.berdachuk.docurag.llm.api.RagAskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RagAskServiceImplTest {

    @Test
    void asksLlmDirectlyWhenNoChunksAreRetrieved() {
        AtomicReference<String> promptText = new AtomicReference<>();
        ChatClient chatClient = ChatClient.builder(prompt -> {
            promptText.set(prompt.toString());
            return ChatResponse.builder()
                    .generations(List.of(new Generation(new AssistantMessage("direct answer"))))
                    .build();
        }).build();
        CustomChatProperties chatProperties = new CustomChatProperties();
        chatProperties.setModel("test-chat");
        RagAskServiceImpl service = new RagAskServiceImpl(
                chatClient,
                (query, topK, minScore) -> List.of(),
                new DocuRagProperties(),
                chatProperties
        );

        var response = service.ask(new RagAskRequest("What are neutropenia symptoms?", 5, 0.5));

        assertThat(response.answer()).isEqualTo("direct answer");
        assertThat(response.model()).isEqualTo("test-chat");
        assertThat(response.retrievedChunks()).isEmpty();
        assertThat(promptText.get()).contains("What are neutropenia symptoms?");
        assertThat(promptText.get()).doesNotContain("Context excerpts:");
    }
}
