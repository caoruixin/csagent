CREATE TABLE kb_articles (
    article_id      TEXT PRIMARY KEY,
    title           TEXT NOT NULL,
    summary         TEXT,
    description     TEXT,
    source_url      TEXT,
    url_category    TEXT,
    uc_tags         TEXT[] NOT NULL DEFAULT '{}',
    is_published    BOOLEAN NOT NULL DEFAULT true,
    token_count     INT,
    version         INT NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_kb_articles_published ON kb_articles(is_published);
CREATE INDEX idx_kb_articles_uc_tags ON kb_articles USING gin(uc_tags);
