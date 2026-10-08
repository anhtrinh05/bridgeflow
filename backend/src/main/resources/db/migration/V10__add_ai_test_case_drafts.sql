CREATE TABLE verification_test_cases (
    id UUID PRIMARY KEY,
    requirement_revision_id UUID NOT NULL,
    acceptance_criterion_id UUID NOT NULL,
    ai_job_id UUID NOT NULL,
    title_japanese TEXT NOT NULL,
    title_vietnamese TEXT NOT NULL,
    preconditions_japanese TEXT NOT NULL,
    preconditions_vietnamese TEXT NOT NULL,
    steps_japanese TEXT NOT NULL,
    steps_vietnamese TEXT NOT NULL,
    expected_result_japanese TEXT NOT NULL,
    expected_result_vietnamese TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT fk_test_cases_revision FOREIGN KEY (requirement_revision_id)
        REFERENCES requirement_revisions (id) ON DELETE CASCADE,
    CONSTRAINT fk_test_cases_criterion FOREIGN KEY (acceptance_criterion_id)
        REFERENCES acceptance_criteria (id),
    CONSTRAINT fk_test_cases_job FOREIGN KEY (ai_job_id) REFERENCES ai_jobs (id),
    CONSTRAINT fk_test_cases_created_by FOREIGN KEY (created_by) REFERENCES app_users (id),
    CONSTRAINT fk_test_cases_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES app_users (id),
    CONSTRAINT ck_test_cases_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT ck_test_cases_status CHECK (status IN ('DRAFT', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_test_cases_revision ON verification_test_cases (requirement_revision_id, created_at);
CREATE INDEX idx_test_cases_job ON verification_test_cases (ai_job_id);
