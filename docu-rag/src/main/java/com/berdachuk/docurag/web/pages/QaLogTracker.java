package com.berdachuk.docurag.web.pages;

import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class QaLogTracker {

    private static final int MAX_ENTRIES = 200;
    private static final int MAX_TEXT_LENGTH = 1200;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final List<String> entries = new ArrayList<>();

    public synchronized void request(String question, int topK, double minScore) {
        add("Request: topK=" + topK
                + ", minScore=" + minScore
                + ", question=\"" + compact(question) + "\"");
    }

    public synchronized void response(String answer, int retrievedChunks, String model) {
        add("Response: model=" + emptyFallback(model)
                + ", retrievedChunks=" + retrievedChunks
                + ", answer=\"" + compact(answer) + "\"");
    }

    public synchronized void failure(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        add("Response failed: " + compact(message));
    }

    public synchronized QaLogSnapshot snapshot() {
        return new QaLogSnapshot(List.copyOf(entries));
    }

    private void add(String message) {
        entries.add("[" + LocalTime.now().format(TIME_FORMAT) + "] " + message);
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
    }

    private String compact(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_TEXT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_TEXT_LENGTH) + "...";
    }

    private String emptyFallback(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
