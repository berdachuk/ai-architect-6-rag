package com.berdachuk.docurag.e2e.world;

/**
 * Mutable ids produced during the API scenario chain (same thread, ordered scenarios).
 */
public final class E2eState {

    public static volatile String firstDocumentId;
    public static volatile String evaluationRunId;

    private E2eState() {
    }

    public static void clear() {
        firstDocumentId = null;
        evaluationRunId = null;
    }
}
