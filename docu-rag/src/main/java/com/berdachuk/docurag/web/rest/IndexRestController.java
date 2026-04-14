package com.berdachuk.docurag.web.rest;

import com.berdachuk.docurag.vector.api.IndexOperationsApi;
import com.berdachuk.docurag.vector.api.IndexStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/index")
@RequiredArgsConstructor
public class IndexRestController {

    private final IndexOperationsApi indexOperationsApi;

    @PostMapping("/rebuild")
    public ResponseEntity<Void> rebuild() {
        indexOperationsApi.rebuildFullIndex();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/incremental")
    public ResponseEntity<String> incremental() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("Deferred per implementation plan.");
    }

    @GetMapping("/status")
    public IndexStatus status() {
        return indexOperationsApi.getStatus();
    }
}
