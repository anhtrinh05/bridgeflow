ALTER TABLE ai_jobs
    ALTER COLUMN document_version_id DROP NOT NULL,
    ADD COLUMN requirement_revision_id UUID,
    ADD CONSTRAINT fk_ai_jobs_requirement_revision
        FOREIGN KEY (requirement_revision_id) REFERENCES requirement_revisions (id),
    ADD CONSTRAINT ck_ai_jobs_single_target
        CHECK (num_nonnulls(document_version_id, requirement_revision_id) = 1),
    ADD CONSTRAINT uk_ai_jobs_revision_purpose UNIQUE (requirement_revision_id, purpose);

CREATE TABLE clarification_questions (
    id UUID PRIMARY KEY,
    requirement_revision_id UUID NOT NULL,
    ai_job_id UUID NOT NULL,
    japanese_text TEXT NOT NULL,
    vietnamese_text TEXT NOT NULL,
    rationale TEXT,
    answer_japanese TEXT,
    answer_vietnamese TEXT,
    status VARCHAR(20) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    answered_by UUID,
    answered_at TIMESTAMPTZ,
    CONSTRAINT fk_clarification_questions_revision
        FOREIGN KEY (requirement_revision_id) REFERENCES requirement_revisions (id) ON DELETE CASCADE,
    CONSTRAINT fk_clarification_questions_job FOREIGN KEY (ai_job_id) REFERENCES ai_jobs (id),
    CONSTRAINT fk_clarification_questions_created_by FOREIGN KEY (created_by) REFERENCES app_users (id),
    CONSTRAINT fk_clarification_questions_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES app_users (id),
    CONSTRAINT fk_clarification_questions_answered_by FOREIGN KEY (answered_by) REFERENCES app_users (id),
    CONSTRAINT ck_clarification_questions_status CHECK (status IN ('DRAFT', 'APPROVED', 'REJECTED'))
);

CREATE TABLE acceptance_criteria (
    id UUID PRIMARY KEY,
    requirement_revision_id UUID NOT NULL,
    ai_job_id UUID NOT NULL,
    japanese_text TEXT NOT NULL,
    vietnamese_text TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT fk_acceptance_criteria_revision
        FOREIGN KEY (requirement_revision_id) REFERENCES requirement_revisions (id) ON DELETE CASCADE,
    CONSTRAINT fk_acceptance_criteria_job FOREIGN KEY (ai_job_id) REFERENCES ai_jobs (id),
    CONSTRAINT fk_acceptance_criteria_created_by FOREIGN KEY (created_by) REFERENCES app_users (id),
    CONSTRAINT fk_acceptance_criteria_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES app_users (id),
    CONSTRAINT ck_acceptance_criteria_status CHECK (status IN ('DRAFT', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_clarification_questions_revision ON clarification_questions (requirement_revision_id, created_at);
CREATE INDEX idx_acceptance_criteria_revision ON acceptance_criteria (requirement_revision_id, created_at);
