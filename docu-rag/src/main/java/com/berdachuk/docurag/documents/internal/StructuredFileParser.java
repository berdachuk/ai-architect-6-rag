package com.berdachuk.docurag.documents.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@Component
public class StructuredFileParser {

    private final ObjectMapper objectMapper;

    public StructuredFileParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ParsedDocument> parseFile(Path path) throws IOException {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) {
            throw new IllegalArgumentException("Use PDF extractor for .pdf");
        }
        if (name.endsWith(".csv")) {
            return parseCsv(path);
        }
        return parseJsonLinesOrArray(path);
    }

    private List<ParsedDocument> parseJsonLinesOrArray(Path path) throws IOException {
        String full = Files.readString(path, StandardCharsets.UTF_8).trim();
        if (full.startsWith("[")) {
            JsonNode root = objectMapper.readTree(full);
            List<ParsedDocument> out = new ArrayList<>();
            if (root.isArray()) {
                int i = 0;
                for (JsonNode n : root) {
                    out.add(fromJson(n, path, i++));
                }
            }
            return out;
        }
        List<ParsedDocument> out = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                JsonNode n = objectMapper.readTree(line);
                out.add(fromJson(n, path, lineNo++));
            }
        }
        return out;
    }

    private List<ParsedDocument> parseCsv(Path path) throws IOException {
        List<ParsedDocument> out = new ArrayList<>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return out;
        }
        String headerLine = lines.getFirst();
        String[] headers = splitCsv(headerLine);
        for (int i = 1; i < lines.size(); i++) {
            String[] vals = splitCsv(lines.get(i));
            String text = null;
            String title = null;
            String category = null;
            String source = null;
            String id = "csv:" + path.getFileName() + ":" + i;
            for (int c = 0; c < headers.length && c < vals.length; c++) {
                String h = headers[c].trim().toLowerCase(Locale.ROOT);
                switch (h) {
                    case "text", "content", "body" -> text = vals[c];
                    case "title" -> title = vals[c];
                    case "category" -> category = vals[c];
                    case "source" -> source = vals[c];
                    case "id", "document_id", "doc_id" -> id = vals[c];
                    default -> {
                    }
                }
            }
            if (text != null && !text.isBlank()) {
                out.add(new ParsedDocument(id, title, category, source, null, text));
            }
        }
        return out;
    }

    private static String[] splitCsv(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    private ParsedDocument fromJson(JsonNode n, Path path, int index) {
        String text = firstText(n);
        String title = firstString(n, "title", "Title");
        String category = firstString(n, "category", "Category", "medical_field");
        String source = firstString(n, "source", "Source", "url", "URL");
        String sourceUrl = firstString(n, "source_url", "link");
        String id = firstString(n, "id", "document_id", "doc_id", "uuid");
        if (id == null || id.isBlank()) {
            id = path.getFileName() + ":" + index;
        }
        return new ParsedDocument(id, title, category, source, sourceUrl, text == null ? "" : text);
    }

    private static String firstText(JsonNode n) {
        for (String key : List.of("text", "content", "body", "document", "passage")) {
            if (n.hasNonNull(key) && n.get(key).isTextual()) {
                return n.get(key).asText();
            }
        }
        return "";
    }

    private static String firstString(JsonNode n, String... keys) {
        for (String k : keys) {
            if (n.hasNonNull(k) && n.get(k).isTextual()) {
                String v = n.get(k).asText();
                if (!v.isBlank()) {
                    return v;
                }
            }
        }
        return null;
    }

    public record ParsedDocument(
            String externalId,
            String title,
            String category,
            String sourceName,
            String sourceUrl,
            String text
    ) {
    }
}
