CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE source_document (
    id TEXT PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    external_id TEXT NOT NULL,
    title TEXT,
    category TEXT,
    source_name TEXT,
    source_url TEXT,
    content TEXT NOT NULL,
    content_hash TEXT NOT NULL,
    source_format TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT source_document_external_unique UNIQUE (external_id)
);

CREATE INDEX idx_source_document_category ON source_document (category);

CREATE TABLE document_chunk (
    id TEXT PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    document_id TEXT NOT NULL REFERENCES source_document (id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    token_estimate INTEGER,
    category TEXT,
    metadata_json JSONB,
    embedding vector(768),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT document_chunk_doc_idx UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_document_chunk_document ON document_chunk (document_id);

CREATE TABLE ingestion_job (
    id TEXT PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    status TEXT NOT NULL,
    documents_loaded INTEGER NOT NULL DEFAULT 0,
    documents_indexed INTEGER NOT NULL DEFAULT 0,
    error_message TEXT
);

CREATE TABLE evaluation_dataset (
    id TEXT PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    name TEXT NOT NULL UNIQUE,
    version TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE evaluation_case (
    id TEXT PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    dataset_id TEXT NOT NULL REFERENCES evaluation_dataset (id) ON DELETE CASCADE,
    external_case_id TEXT NOT NULL,
    question TEXT NOT NULL,
    ground_truth_answer TEXT NOT NULL,
    category TEXT,
    difficulty TEXT,
    metadata_json JSONB
);

CREATE INDEX idx_evaluation_case_dataset ON evaluation_case (dataset_id);

CREATE TABLE evaluation_run (
    id TEXT PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    dataset_id TEXT NOT NULL REFERENCES evaluation_dataset (id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    model_name TEXT NOT NULL,
    embedding_model_name TEXT NOT NULL,
    normalized_accuracy NUMERIC,
    mean_semantic_similarity NUMERIC,
    semantic_accuracy_at_080 NUMERIC,
    notes TEXT
);

CREATE INDEX idx_evaluation_run_dataset ON evaluation_run (dataset_id);

CREATE TABLE evaluation_result (
    id TEXT PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    run_id TEXT NOT NULL REFERENCES evaluation_run (id) ON DELETE CASCADE,
    case_id TEXT NOT NULL REFERENCES evaluation_case (id) ON DELETE CASCADE,
    predicted_answer TEXT NOT NULL,
    exact_match BOOLEAN NOT NULL DEFAULT false,
    normalized_match BOOLEAN NOT NULL DEFAULT false,
    semantic_similarity NUMERIC,
    semantic_pass BOOLEAN NOT NULL DEFAULT false,
    retrieved_chunks_json JSONB
);

CREATE INDEX idx_evaluation_result_run ON evaluation_result (run_id);
