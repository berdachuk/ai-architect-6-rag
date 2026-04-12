package com.berdachuk.docurag.documents.api;

import java.util.List;

public interface DocumentIngestApi {

    IngestSummary ingestConfiguredPaths();

    IngestSummary ingestPaths(List<String> paths);
}
