CREATE TABLE requirement_relations (
    id UUID PRIMARY KEY,
    source_requirement_id UUID NOT NULL,
    target_requirement_id UUID NOT NULL,
    relation_type VARCHAR(30) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_requirement_relations_source FOREIGN KEY (source_requirement_id)
        REFERENCES requirements (id) ON DELETE CASCADE,
    CONSTRAINT fk_requirement_relations_target FOREIGN KEY (target_requirement_id)
        REFERENCES requirements (id) ON DELETE CASCADE,
    CONSTRAINT fk_requirement_relations_created_by FOREIGN KEY (created_by)
        REFERENCES app_users (id),
    CONSTRAINT uk_requirement_relations_edge
        UNIQUE (source_requirement_id, target_requirement_id, relation_type),
    CONSTRAINT ck_requirement_relations_distinct CHECK (source_requirement_id <> target_requirement_id),
    CONSTRAINT ck_requirement_relations_type CHECK (
        relation_type IN ('DEPENDS_ON', 'SUPERSEDES', 'SPLIT_INTO', 'MERGED_INTO', 'DUPLICATES')
    )
);

CREATE INDEX idx_requirement_relations_source ON requirement_relations (source_requirement_id);
CREATE INDEX idx_requirement_relations_target ON requirement_relations (target_requirement_id);
