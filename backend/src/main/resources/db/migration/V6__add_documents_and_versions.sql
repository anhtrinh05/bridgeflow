CREATE TABLE documents (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ,
    CONSTRAINT fk_documents_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT ck_documents_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE TABLE document_versions (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    uploaded_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_document_versions_document
        FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_document_versions_uploaded_by
        FOREIGN KEY (uploaded_by) REFERENCES app_users (id),
    CONSTRAINT uk_document_versions_number UNIQUE (document_id, version_number),
    CONSTRAINT uk_document_versions_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_document_versions_number CHECK (version_number > 0),
    CONSTRAINT ck_document_versions_size CHECK (size_bytes > 0)
);

CREATE INDEX idx_documents_project_id ON documents (project_id);
CREATE INDEX idx_document_versions_document_id ON document_versions (document_id);
