package com.berdachuk.docurag;

import com.berdachuk.docurag.documents.api.DocumentIngestApi;
import com.berdachuk.docurag.documents.api.IngestSummary;
import com.berdachuk.docurag.evaluation.api.EvaluationApi;
import com.berdachuk.docurag.evaluation.api.EvaluationRunRequest;
import com.berdachuk.docurag.llm.api.RagAskApi;
import com.berdachuk.docurag.llm.api.RagAskRequest;
import com.berdachuk.docurag.vector.api.IndexOperationsApi;
import com.berdachuk.docurag.vector.api.IndexStatus;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocuRagFullFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16"))
            .withDatabaseName("docurag")
            .withUsername("docurag")
            .withPassword("docurag");

    @Autowired
    private DocumentIngestApi documentIngestApi;

    @Autowired
    private IndexOperationsApi indexOperationsApi;

    @Autowired
    private RagAskApi ragAskApi;

    @Autowired
    private EvaluationApi evaluationApi;

    @Test
    @Order(1)
    void ingestRebuildAskAndEvalFlow() throws Exception {
        Path tmp = Files.createTempFile("docurag-fixture", ".jsonl");
        Files.copy(new ClassPathResource("fixtures/sample.jsonl").getInputStream(), tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        IngestSummary ingest = documentIngestApi.ingestPaths(List.of(tmp.toAbsolutePath().toString()));
        assertThat(ingest.status()).isEqualTo("COMPLETED");
        assertThat(ingest.documentsLoaded()).isGreaterThan(0);

        indexOperationsApi.rebuildFullIndex();

        IndexStatus status = indexOperationsApi.getStatus();
        assertThat(status.documentCount()).isGreaterThan(0);
        assertThat(status.embeddedChunkCount()).isGreaterThan(0);

        var answer = ragAskApi.ask(new RagAskRequest("What is hypertension?", 3, 0.0));
        assertThat(answer.answer()).isNotBlank();

        var evalOut = evaluationApi.run(new EvaluationRunRequest("medical-rag-eval-v1", 3, 0.0, 0.5));
        assertThat(evalOut.runId()).isNotBlank();
        assertThat(evalOut.total()).isGreaterThan(0);
    }

    @Test
    @Order(2)
    void pdfIngestRebuildAndIndexStatus() throws Exception {
        Path tmpPdf = Files.createTempFile("docurag-fixture", ".pdf");
        tmpPdf.toFile().deleteOnExit();
        Files.copy(
                new ClassPathResource("fixtures/tiny.pdf").getInputStream(),
                tmpPdf,
                StandardCopyOption.REPLACE_EXISTING);

        IngestSummary pdfIngest = documentIngestApi.ingestPaths(List.of(tmpPdf.toAbsolutePath().toString()));
        assertThat(pdfIngest.status()).isEqualTo("COMPLETED");
        assertThat(pdfIngest.documentsLoaded()).isGreaterThan(0);

        indexOperationsApi.rebuildFullIndex();

        IndexStatus status = indexOperationsApi.getStatus();
        assertThat(status.documentCount()).isGreaterThan(0);
        assertThat(status.embeddedChunkCount()).isGreaterThan(0);
    }
}
