package com.berdachuk.docurag.evaluation.internal;

import com.berdachuk.docurag.evaluation.api.EvaluationLogSnapshot;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Component
public class EvaluationProgressTracker {

    private static final int MAX_ENTRIES = 500;

    private final Deque<String> entries = new ArrayDeque<>();
    private boolean running;
    private String runId;
    private boolean terminationRequested;
    private Thread runningThread;

    public synchronized void start(String runId, String datasetName, int totalCases) {
        entries.clear();
        this.running = true;
        this.runId = runId;
        this.terminationRequested = false;
        this.runningThread = Thread.currentThread();
        add("Started evaluation run " + runId + " for dataset " + datasetName + ".");
        add("Loaded " + totalCases + " evaluation case(s).");
    }

    public synchronized void log(String message) {
        add(message);
    }

    public synchronized void finish(String runId) {
        add("Finished evaluation run " + runId + ".");
        stop();
    }

    public synchronized void fail(String runId, String message) {
        add("Evaluation run " + runId + " failed: " + message);
        stop();
    }

    public synchronized void terminated(String runId) {
        add("Evaluation run " + runId + " terminated.");
        stop();
    }

    public synchronized boolean requestTermination() {
        if (!running || terminationRequested) {
            return false;
        }
        terminationRequested = true;
        add("Termination requested for evaluation run " + runId + ".");
        if (runningThread != null) {
            runningThread.interrupt();
        }
        return true;
    }

    public synchronized boolean terminationRequested() {
        return terminationRequested;
    }

    public synchronized EvaluationLogSnapshot snapshot() {
        return new EvaluationLogSnapshot(running, runId, terminationRequested, List.copyOf(entries));
    }

    public synchronized void clear() {
        entries.clear();
        stop();
        add("Evaluation log cleared.");
    }

    private void stop() {
        running = false;
        runId = null;
        terminationRequested = false;
        runningThread = null;
    }

    private void add(String message) {
        entries.addLast("[" + LocalTime.now().truncatedTo(ChronoUnit.SECONDS) + "] " + message);
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
    }
}
