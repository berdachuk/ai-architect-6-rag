package com.berdachuk.docurag.llm.internal;

import com.berdachuk.docurag.core.config.CustomChatProperties;
import com.berdachuk.docurag.core.config.DocuRagProperties;
import com.berdachuk.docurag.llm.api.RagAskApi;
import com.berdachuk.docurag.llm.api.RagAskRequest;
import com.berdachuk.docurag.llm.api.RagAskResponse;
import com.berdachuk.docurag.llm.api.RetrievedChunkDto;
import com.berdachuk.docurag.retrieval.api.ChunkHit;
import com.berdachuk.docurag.retrieval.api.SemanticSearchApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagAskServiceImpl implements RagAskApi {

    private static final String SYSTEM = """
            You are a careful medical information assistant. Answer only using the provided context excerpts.
            If the context is insufficient, say you cannot answer from the indexed documents and suggest what is missing.
            Use clear, neutral language; this is not a substitute for professional medical advice.
            """;

    private final ChatClient chatClient;
    private final SemanticSearchApi semanticSearchApi;
    private final DocuRagProperties docuRagProperties;
    private final CustomChatProperties chatProperties;

    public RagAskServiceImpl(
            ChatClient chatClient,
            SemanticSearchApi semanticSearchApi,
            DocuRagProperties docuRagProperties,
            CustomChatProperties chatProperties
    ) {
        this.chatClient = chatClient;
        this.semanticSearchApi = semanticSearchApi;
        this.docuRagProperties = docuRagProperties;
        this.chatProperties = chatProperties;
    }

    @Override
    public RagAskResponse ask(RagAskRequest request) {
        int topK = request.topK() != null ? request.topK() : docuRagProperties.getRag().getDefaultTopK();
        double minScore = request.minScore() != null ? request.minScore() : docuRagProperties.getRag().getDefaultMinScore();
        List<ChunkHit> hits = semanticSearchApi.search(request.question(), topK, minScore);
        String context = hits.stream()
                .map(h -> "[%s] %s".formatted(h.title() == null ? h.documentId() : h.title(), h.snippet()))
                .collect(Collectors.joining("\n\n"));
        if (context.isBlank()) {
            return new RagAskResponse(
                    "No relevant passages were retrieved from the index. Try rephrasing or rebuild the index.",
                    chatProperties.getModel(),
                    List.of()
            );
        }
        String user = "Context excerpts:\n" + context + "\n\nQuestion:\n" + request.question();
        String answer = chatClient.prompt()
                .system(SYSTEM)
                .user(user)
                .call()
                .content();
        List<RetrievedChunkDto> dtos = hits.stream()
                .map(h -> new RetrievedChunkDto(h.documentId(), h.title(), h.category(), h.score(), h.snippet()))
                .toList();
        return new RagAskResponse(answer == null ? "" : answer, chatProperties.getModel(), dtos);
    }
}
