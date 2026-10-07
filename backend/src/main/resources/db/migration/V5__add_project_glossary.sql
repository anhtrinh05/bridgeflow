CREATE TABLE glossary_terms (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    japanese_term VARCHAR(160) NOT NULL,
    vietnamese_term VARCHAR(240) NOT NULL,
    notes TEXT,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_glossary_terms_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_glossary_terms_created_by
        FOREIGN KEY (created_by) REFERENCES app_users (id),
    CONSTRAINT uk_glossary_terms_project_japanese UNIQUE (project_id, japanese_term)
);

CREATE INDEX idx_glossary_terms_project_id ON glossary_terms (project_id);
