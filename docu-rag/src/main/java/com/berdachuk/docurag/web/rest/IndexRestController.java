package com.berdachuk.docurag.web.rest;

import com.berdachuk.docurag.vector.api.IndexOperationsApi;
import com.berdachuk.docurag.web.openapi.api.IndexApi;
import com.berdachuk.docurag.web.openapi.model.IndexStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class IndexRestController implements IndexApi {

    private final IndexOperationsApi indexOperationsApi;

    @Override
    public ResponseEntity<Void> rebuildIndex() {
        indexOperationsApi.rebuildFullIndex();
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> incrementalIndex() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<IndexStatus> getIndexStatus() {
        com.berdachuk.docurag.vector.api.IndexStatus status = indexOperationsApi.getStatus();
        IndexStatus payload = new IndexStatus()
                .documentCount(status.documentCount())
                .chunkCount(status.chunkCount())
                .embeddedChunkCount(status.embeddedChunkCount())
                .lastIngestJobId(status.lastIngestJobId())
                .lastIngestStatus(status.lastIngestStatus())
                .lastIngestStartedAt(status.lastIngestStartedAt())
                .lastIngestFinishedAt(status.lastIngestFinishedAt());
        return ResponseEntity.ok(payload);
    }
}
