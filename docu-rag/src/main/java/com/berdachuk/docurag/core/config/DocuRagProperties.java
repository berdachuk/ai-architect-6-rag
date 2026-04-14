package com.berdachuk.docurag.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "docurag")
@Getter
public class DocuRagProperties {

    private final Ingestion ingestion = new Ingestion();
    private final Chunking chunking = new Chunking();
    private final Rag rag = new Rag();

    @Getter
    @Setter
    public static class Ingestion {
        private String corpusPath = "";
        private String pdfDemoPath = "";
        private final Embeddings embeddings = new Embeddings();
    }

    @Getter
    @Setter
    public static class Embeddings {
        private boolean parallelWithIngest = false;
        private int batchSize = 32;
        private int progressLogIntervalBatches = 1;
        private boolean dropIndexesDuringGeneration = false;
        private final MultiEndpoint multiEndpoint = new MultiEndpoint();
    }

    @Getter
    @Setter
    public static class MultiEndpoint {
        private List<EmbeddingEndpoint> endpoints = new ArrayList<>();
        private int skipDurationMin = 10;
        private int workerPerEndpoint = 1;
        private int apiBatchSize = 50;
    }

    @Getter
    @Setter
    public static class EmbeddingEndpoint {
        private String url = "";
        private String model = "";
        private int priority;
    }

    @Getter
    @Setter
    public static class Chunking {
        private int chunkSize = 512;
        private int chunkOverlap = 64;
        private int minChunkChars = 50;
        private String strategy = "ADAPTIVE";
    }

    @Getter
    @Setter
    public static class Rag {
        private int defaultTopK = 5;
        private double defaultMinScore = 0.5;
    }
}
