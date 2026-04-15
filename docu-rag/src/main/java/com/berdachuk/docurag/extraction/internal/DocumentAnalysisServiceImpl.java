package com.berdachuk.docurag.extraction.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.berdachuk.docurag.extraction.api.AnalysisRequest;
import com.berdachuk.docurag.extraction.api.AnalysisResponse;
import com.berdachuk.docurag.extraction.api.CategoryCount;
import com.berdachuk.docurag.extraction.api.DocumentAnalysisApi;
import com.berdachuk.docurag.extraction.api.GraphEdge;
import com.berdachuk.docurag.extraction.api.GraphNode;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class DocumentAnalysisServiceImpl implements DocumentAnalysisApi {

    private static final String PROMPT = """
            From the following medical document excerpts, extract up to 15 entity-relation triples as JSON ONLY.
            Format: {"triples":[{"subject":"...","relation":"...","object":"..."}]}
            Use short English labels. No markdown, no commentary.
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Override
    public AnalysisResponse analyze(AnalysisRequest request) {
        int max = request.maxDocuments() == null ? 12 : Math.min(40, Math.max(1, request.maxDocuments()));
        List<CategoryCount> categories = jdbc.query("""
                        SELECT category, COUNT(*) AS cnt FROM source_document
                        WHERE category IS NOT NULL AND category <> ''
                        GROUP BY category ORDER BY cnt DESC
                        """, Map.of(), (rs, rowNum) -> new CategoryCount(rs.getString("category"), rs.getLong("cnt")));

        String sample = jdbc.queryForList(
                        "SELECT content FROM source_document ORDER BY created_at LIMIT :limit",
                        Map.of("limit", max),
                        String.class
                )
                .stream()
                .map(c -> c == null ? "" : c.substring(0, Math.min(400, c.length())))
                .reduce((a, b) -> a + "\n---\n" + b)
                .orElse("");
        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        String notes = "Heuristic graph from LLM extraction over sample excerpts.";
        if (!sample.isBlank()) {
            try {
                String raw = chatClient.prompt()
                        .system(PROMPT)
                        .user(sample)
                        .call()
                        .content();
                parseTriples(raw, nodes, edges);
            } catch (RuntimeException e) {
                notes = "Extraction model call failed: " + e.getMessage();
            }
        } else {
            notes = "No document text available for extraction.";
        }
        return new AnalysisResponse(categories, nodes, edges, notes);
    }

    private void parseTriples(String raw, List<GraphNode> nodes, List<GraphEdge> edges) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String json = raw.trim();
        if (json.startsWith("```")) {
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode triples = root.path("triples");
            if (!triples.isArray()) {
                return;
            }
            AtomicInteger id = new AtomicInteger(1);
            for (JsonNode t : triples) {
                String s = text(t, "subject");
                String r = text(t, "relation");
                String o = text(t, "object");
                if (s == null || o == null) {
                    continue;
                }
                String sid = "s" + id.getAndIncrement();
                String oid = "o" + id.getAndIncrement();
                nodes.add(new GraphNode(sid, s, "entity"));
                nodes.add(new GraphNode(oid, o, "entity"));
                edges.add(new GraphEdge(sid, oid, r == null ? "related" : r));
            }
        } catch (Exception ignored) {
            // leave nodes/edges empty
        }
    }

    private static String text(JsonNode t, String field) {
        JsonNode n = t.get(field);
        return n != null && n.isTextual() ? n.asText().trim() : null;
    }
}
