CREATE TABLE project_deletion_receipts (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    project_code VARCHAR(40) NOT NULL,
    deleted_by UUID,
    reason VARCHAR(20) NOT NULL,
    deleted_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_project_deletion_receipts_reason CHECK (reason IN ('MANUAL', 'RETENTION'))
);

CREATE INDEX idx_project_deletion_receipts_project_deleted_at
    ON project_deletion_receipts (project_id, deleted_at DESC);
