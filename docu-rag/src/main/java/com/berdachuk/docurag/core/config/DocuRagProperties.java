package com.berdachuk.docurag.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "docurag")
public class DocuRagProperties {

    private final Ingestion ingestion = new Ingestion();
    private final Chunking chunking = new Chunking();
    private final Rag rag = new Rag();

    public Ingestion getIngestion() {
        return ingestion;
    }

    public Chunking getChunking() {
        return chunking;
    }

    public Rag getRag() {
        return rag;
    }

    public static class Ingestion {
        private String corpusPath = "";
        private String pdfDemoPath = "";
        private final Embeddings embeddings = new Embeddings();

        public String getCorpusPath() {
            return corpusPath;
        }

        public void setCorpusPath(String corpusPath) {
            this.corpusPath = corpusPath;
        }

        public String getPdfDemoPath() {
            return pdfDemoPath;
        }

        public void setPdfDemoPath(String pdfDemoPath) {
            this.pdfDemoPath = pdfDemoPath;
        }

        public Embeddings getEmbeddings() {
            return embeddings;
        }
    }

    /**
     * ExpertMatch-aligned ingestion embedding tuning ({@code docurag.ingestion.embeddings.*}).
     * Multi-endpoint pool wiring is optional; {@link #batchSize} and API sub-batching are used by {@code EmbeddingIndexerImpl}.
     */
    public static class Embeddings {
        private boolean parallelWithIngest = false;
        private int batchSize = 32;
        private int progressLogIntervalBatches = 1;
        private boolean dropIndexesDuringGeneration = false;
        private final MultiEndpoint multiEndpoint = new MultiEndpoint();

        public boolean isParallelWithIngest() {
            return parallelWithIngest;
        }

        public void setParallelWithIngest(boolean parallelWithIngest) {
            this.parallelWithIngest = parallelWithIngest;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getProgressLogIntervalBatches() {
            return progressLogIntervalBatches;
        }

        public void setProgressLogIntervalBatches(int progressLogIntervalBatches) {
            this.progressLogIntervalBatches = progressLogIntervalBatches;
        }

        public boolean isDropIndexesDuringGeneration() {
            return dropIndexesDuringGeneration;
        }

        public void setDropIndexesDuringGeneration(boolean dropIndexesDuringGeneration) {
            this.dropIndexesDuringGeneration = dropIndexesDuringGeneration;
        }

        public MultiEndpoint getMultiEndpoint() {
            return multiEndpoint;
        }
    }

    public static class MultiEndpoint {
        private List<EmbeddingEndpoint> endpoints = new ArrayList<>();
        private int skipDurationMin = 10;
        private int workerPerEndpoint = 1;
        private int apiBatchSize = 50;

        public List<EmbeddingEndpoint> getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(List<EmbeddingEndpoint> endpoints) {
            this.endpoints = endpoints != null ? endpoints : new ArrayList<>();
        }

        public int getSkipDurationMin() {
            return skipDurationMin;
        }

        public void setSkipDurationMin(int skipDurationMin) {
            this.skipDurationMin = skipDurationMin;
        }

        public int getWorkerPerEndpoint() {
            return workerPerEndpoint;
        }

        public void setWorkerPerEndpoint(int workerPerEndpoint) {
            this.workerPerEndpoint = workerPerEndpoint;
        }

        public int getApiBatchSize() {
            return apiBatchSize;
        }

        public void setApiBatchSize(int apiBatchSize) {
            this.apiBatchSize = apiBatchSize;
        }
    }

    public static class EmbeddingEndpoint {
        private String url = "";
        private String model = "";
        private int priority;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }
    }

    public static class Chunking {
        private int chunkSize = 512;
        private int chunkOverlap = 64;
        private int minChunkChars = 50;

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public int getChunkOverlap() {
            return chunkOverlap;
        }

        public void setChunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
        }

        public int getMinChunkChars() {
            return minChunkChars;
        }

        public void setMinChunkChars(int minChunkChars) {
            this.minChunkChars = minChunkChars;
        }
    }

    public static class Rag {
        private int defaultTopK = 5;
        private double defaultMinScore = 0.5;

        public int getDefaultTopK() {
            return defaultTopK;
        }

        public void setDefaultTopK(int defaultTopK) {
            this.defaultTopK = defaultTopK;
        }

        public double getDefaultMinScore() {
            return defaultMinScore;
        }

        public void setDefaultMinScore(double defaultMinScore) {
            this.defaultMinScore = defaultMinScore;
        }
    }
}
