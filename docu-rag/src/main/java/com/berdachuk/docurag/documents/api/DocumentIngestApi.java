package com.berdachuk.docurag.documents.api;

import java.util.List;

public interface DocumentIngestApi {

    IngestSummary ingestConfiguredPaths();

    IngestSummary ingestPaths(List<String> paths);

    default IngestSummary ingestConfiguredPaths(IngestProgressListener progressListener) {
        return ingestConfiguredPaths();
    }

    default IngestSummary ingestPaths(List<String> paths, IngestProgressListener progressListener) {
        return ingestPaths(paths);
    }
}
