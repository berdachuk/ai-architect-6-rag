package com.berdachuk.docurag.evaluation.internal;

import com.berdachuk.docurag.evaluation.api.EvaluationLogSnapshot;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Component
public class EvaluationProgressTracker {

    private static final int MAX_ENTRIES = 500;

    private final Deque<String> entries = new ArrayDeque<>();
    private boolean running;
    private String runId;

    public synchronized void start(String runId, String datasetName, int totalCases) {
        entries.clear();
        this.running = true;
        this.runId = runId;
        add("Started evaluation run " + runId + " for dataset " + datasetName + ".");
        add("Loaded " + totalCases + " evaluation case(s).");
    }

    public synchronized void log(String message) {
        add(message);
    }

    public synchronized void finish(String runId) {
        add("Finished evaluation run " + runId + ".");
        this.running = false;
    }

    public synchronized void fail(String runId, String message) {
        add("Evaluation run " + runId + " failed: " + message);
        this.running = false;
    }

    public synchronized EvaluationLogSnapshot snapshot() {
        return new EvaluationLogSnapshot(running, runId, List.copyOf(entries));
    }

    public synchronized void clear() {
        entries.clear();
        running = false;
        runId = null;
        add("Evaluation log cleared.");
    }

    private void add(String message) {
        entries.addLast("[" + LocalTime.now().truncatedTo(ChronoUnit.SECONDS) + "] " + message);
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
    }
}
