ALTER TABLE projects
    ADD COLUMN ai_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE requirement_revisions
    ADD COLUMN source_anchor VARCHAR(500);

ALTER TABLE requirement_revisions
    ADD CONSTRAINT fk_requirement_revisions_document_version
    FOREIGN KEY (document_version_id) REFERENCES document_versions (id);

CREATE TABLE ai_jobs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    document_version_id UUID NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider VARCHAR(80) NOT NULL,
    model VARCHAR(120) NOT NULL,
    correlation_id UUID NOT NULL,
    requested_by UUID NOT NULL,
    candidate_count INTEGER NOT NULL DEFAULT 0,
    error_code VARCHAR(80),
    error_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    CONSTRAINT fk_ai_jobs_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_jobs_document_version FOREIGN KEY (document_version_id) REFERENCES document_versions (id),
    CONSTRAINT fk_ai_jobs_requested_by FOREIGN KEY (requested_by) REFERENCES app_users (id),
    CONSTRAINT uk_ai_jobs_version_purpose UNIQUE (document_version_id, purpose),
    CONSTRAINT uk_ai_jobs_correlation_id UNIQUE (correlation_id),
    CONSTRAINT ck_ai_jobs_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_ai_jobs_candidate_count CHECK (candidate_count >= 0)
);

CREATE INDEX idx_ai_jobs_project_created_at ON ai_jobs (project_id, created_at DESC);
