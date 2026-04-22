CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE kb_chunks (
    chunk_id        TEXT PRIMARY KEY,
    article_id      TEXT NOT NULL REFERENCES kb_articles(article_id),
    chunk_index     INT NOT NULL,
    chunk_text      TEXT NOT NULL,
    embedding       vector(768) NOT NULL,
    token_count     INT NOT NULL,
    section_heading TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (article_id, chunk_index)
);
CREATE INDEX idx_kb_chunks_embedding_hnsw ON kb_chunks USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
