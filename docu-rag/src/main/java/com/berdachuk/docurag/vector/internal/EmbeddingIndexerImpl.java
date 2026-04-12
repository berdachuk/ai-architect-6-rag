package com.berdachuk.docurag.vector.internal;

import com.berdachuk.docurag.core.config.DocuRagProperties;
import com.berdachuk.docurag.core.util.PgVector;
import com.berdachuk.docurag.vector.api.EmbeddingIndexerApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingIndexerImpl implements EmbeddingIndexerApi {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingIndexerImpl.class);
    private static final int MAX_ATTEMPTS = 3;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingJdbcRepository repository;
    private final DocuRagProperties docuRagProperties;

    public EmbeddingIndexerImpl(
            EmbeddingModel embeddingModel,
            EmbeddingJdbcRepository repository,
            DocuRagProperties docuRagProperties
    ) {
        this.embeddingModel = embeddingModel;
        this.repository = repository;
        this.docuRagProperties = docuRagProperties;
    }

    @Override
    @Transactional
    public int embedAllMissing() {
        var emb = docuRagProperties.getIngestion().getEmbeddings();
        int dbBatch = Math.max(1, emb.getBatchSize());
        int apiBatch = Math.max(1, emb.getMultiEndpoint().getApiBatchSize());
        int logEvery = Math.max(1, emb.getProgressLogIntervalBatches());

        if (emb.isDropIndexesDuringGeneration()) {
            log.warn("docurag.ingestion.embeddings.drop-indexes-during-generation=true — no chunk embedding indexes to drop yet; ignoring");
        }

        int total = 0;
        int dbBatchCount = 0;
        while (true) {
            List<ChunkEmbeddingRow> batch = repository.findChunksWithoutEmbedding(dbBatch);
            if (batch.isEmpty()) {
                break;
            }
            dbBatchCount++;
            for (int start = 0; start < batch.size(); start += apiBatch) {
                int end = Math.min(start + apiBatch, batch.size());
                List<ChunkEmbeddingRow> slice = batch.subList(start, end);
                List<String> texts = slice.stream().map(ChunkEmbeddingRow::chunkText).toList();
                float[][] vectors = embedWithRetry(texts);
                for (int i = 0; i < slice.size(); i++) {
                    String literal = PgVector.toLiteral(vectors[i]);
                    repository.updateEmbedding(slice.get(i).id(), literal);
                    total++;
                }
            }
            if (dbBatchCount % logEvery == 0) {
                log.info("Embedding progress: {} DB batch(es), {} vectors written so far", dbBatchCount, total);
            }
        }
        if (dbBatchCount > 0 && dbBatchCount % logEvery != 0) {
            log.info("Embedding finished: {} DB batch(es), {} vectors total", dbBatchCount, total);
        }
        return total;
    }

    private float[][] embedWithRetry(List<String> texts) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                var response = embeddingModel.call(new EmbeddingRequest(texts, null));
                List<float[]> out = new ArrayList<>();
                response.getResults().forEach(e -> out.add(e.getOutput()));
                if (out.size() != texts.size()) {
                    throw new IllegalStateException("Embedding count mismatch");
                }
                return out.toArray(float[][]::new);
            } catch (RuntimeException e) {
                last = e;
                log.warn("Embedding batch attempt {} failed: {}", attempt, e.getMessage());
                try {
                    Thread.sleep(200L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ie);
                }
            }
        }
        throw last != null ? last : new IllegalStateException("embed failed");
    }
}
