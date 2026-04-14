package com.berdachuk.docurag.chunking.internal;

import com.berdachuk.docurag.chunking.api.Chunker;
import com.berdachuk.docurag.chunking.api.ChunkingApi;
import com.berdachuk.docurag.core.config.DocuRagProperties;
import com.berdachuk.docurag.core.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChunkingServiceImpl implements ChunkingApi {

    private final DocuRagProperties properties;
    private final ChunkJdbcRepository chunks;
    private final DocumentContentReader documentContentReader;
    private final NamedParameterJdbcTemplate jdbc;
    private final ChunkerFactory chunkerFactory;

    @Override
    @Transactional
    public int rebuildAllChunks() {
        chunks.deleteAll();
        List<String> ids = jdbc.queryForList(
                "SELECT id FROM source_document ORDER BY created_at",
                Map.of(),
                String.class
        );
        int total = 0;
        for (String id : ids) {
            total += chunkSingleDocument(id);
        }
        return total;
    }

    @Override
    @Transactional
    public void rebuildChunksForDocument(String documentId) {
        chunks.deleteByDocumentId(documentId);
        chunkSingleDocument(documentId);
    }

    private int chunkSingleDocument(String documentId) {
        return documentContentReader.find(documentId)
                .map(row -> insertChunksForText(documentId, row.content(), row.category()))
                .orElse(0);
    }

    private int insertChunksForText(String documentId, String content, String category) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        int size = properties.getChunking().getChunkSize();
        int overlap = Math.min(properties.getChunking().getChunkOverlap(), size - 1);
        int minChars = properties.getChunking().getMinChunkChars();
        String strategyName = properties.getChunking().getStrategy();

        Chunker chunker = chunkerFactory.getChunker(strategyName);
        List<String> parts = chunker.chunk(content, size, overlap, minChars);

        int idx = 0;
        for (String part : parts) {
            int estTokens = Math.max(1, part.length() / 4);
            chunks.insertChunk(IdGenerator.generateId(), documentId, idx++, part, estTokens, category);
        }
        return parts.size();
    }

    @Deprecated
    public static List<String> characterChunks(String text, int chunkSize, int overlap, int minChunkChars) {
        return RecursiveCharacterChunker.recursiveSplit(text, chunkSize, minChunkChars);
    }
}