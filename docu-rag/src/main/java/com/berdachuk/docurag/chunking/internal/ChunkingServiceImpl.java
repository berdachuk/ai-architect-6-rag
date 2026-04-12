package com.berdachuk.docurag.chunking.internal;

import com.berdachuk.docurag.chunking.api.ChunkingApi;
import com.berdachuk.docurag.core.config.DocuRagProperties;
import com.berdachuk.docurag.core.util.IdGenerator;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChunkingServiceImpl implements ChunkingApi {

    private final DocuRagProperties properties;
    private final ChunkJdbcRepository chunks;
    private final DocumentContentReader documentContentReader;
    private final NamedParameterJdbcTemplate jdbc;

    public ChunkingServiceImpl(
            DocuRagProperties properties,
            ChunkJdbcRepository chunks,
            DocumentContentReader documentContentReader,
            NamedParameterJdbcTemplate jdbc
    ) {
        this.properties = properties;
        this.chunks = chunks;
        this.documentContentReader = documentContentReader;
        this.jdbc = jdbc;
    }

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
        List<String> parts = characterChunks(content, size, overlap, minChars);
        int idx = 0;
        for (String part : parts) {
            int estTokens = Math.max(1, part.length() / 4);
            chunks.insertChunk(IdGenerator.generateId(), documentId, idx++, part, estTokens, category);
        }
        return parts.size();
    }

    static List<String> characterChunks(String text, int chunkSize, int overlap, int minChunkChars) {
        List<String> out = new ArrayList<>();
        int start = 0;
        int n = text.length();
        while (start < n) {
            int end = Math.min(n, start + chunkSize);
            String slice = text.substring(start, end).trim();
            if (slice.length() >= minChunkChars || out.isEmpty() && slice.length() > 0) {
                out.add(slice);
            } else if (!out.isEmpty() && !slice.isEmpty()) {
                String last = out.removeLast();
                out.add((last + " " + slice).trim());
            }
            if (end >= n) {
                break;
            }
            start = end - overlap;
            if (start < 0) {
                start = 0;
            }
        }
        return out;
    }
}
