CREATE TABLE projects (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    customer_name VARCHAR(160),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_projects_code UNIQUE (code),
    CONSTRAINT ck_projects_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE TABLE requirements (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    display_key VARCHAR(40) NOT NULL,
    current_revision_id UUID,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ,
    CONSTRAINT fk_requirements_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT uk_requirements_project_display_key UNIQUE (project_id, display_key),
    CONSTRAINT ck_requirements_status
        CHECK (status IN ('DRAFT', 'REVIEWING', 'CONFIRMED', 'REJECTED', 'ARCHIVED'))
);

CREATE TABLE requirement_revisions (
    id UUID PRIMARY KEY,
    requirement_id UUID NOT NULL,
    revision_number INTEGER NOT NULL,
    document_version_id UUID,
    japanese_text TEXT NOT NULL,
    vietnamese_text TEXT NOT NULL,
    change_type VARCHAR(20) NOT NULL,
    review_status VARCHAR(20) NOT NULL,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL,
    confirmed_by UUID,
    confirmed_at TIMESTAMPTZ,
    CONSTRAINT fk_requirement_revisions_requirement
        FOREIGN KEY (requirement_id) REFERENCES requirements (id) ON DELETE CASCADE,
    CONSTRAINT uk_requirement_revisions_number UNIQUE (requirement_id, revision_number),
    CONSTRAINT uk_requirement_revisions_owner UNIQUE (requirement_id, id),
    CONSTRAINT ck_requirement_revisions_number CHECK (revision_number > 0),
    CONSTRAINT ck_requirement_revisions_change_type
        CHECK (change_type IN ('ADDED', 'MODIFIED', 'UNCHANGED', 'DELETED', 'SPLIT', 'MERGED')),
    CONSTRAINT ck_requirement_revisions_review_status
        CHECK (review_status IN ('DRAFT', 'REVIEWING', 'CONFIRMED', 'REJECTED'))
);

ALTER TABLE requirements
    ADD CONSTRAINT fk_requirements_current_revision
    FOREIGN KEY (id, current_revision_id) REFERENCES requirement_revisions (requirement_id, id);

CREATE INDEX idx_requirements_project_id ON requirements (project_id);
CREATE INDEX idx_requirement_revisions_requirement_id ON requirement_revisions (requirement_id);
CREATE INDEX idx_requirement_revisions_document_version_id ON requirement_revisions (document_version_id);
