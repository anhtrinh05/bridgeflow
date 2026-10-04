# Requirement Identity and Revision Model

## Core decision

A displayed key such as `REQ-014` is not the database primary key. Each logical requirement receives an immutable UUID that survives document updates. Changes create revisions instead of overwriting the previous text.

## Core entities

### Requirement

Represents the stable business concept.

```text
id                  UUID, immutable primary key
project_id          owning project
display_key         human-readable key such as REQ-014
current_revision_id active reviewed revision
status              lifecycle status
created_at
archived_at
```

### RequirementRevision

Represents the requirement content at a point in time.

```text
id                  UUID
requirement_id      stable parent requirement
revision_number     increasing integer
document_version_id source document version
japanese_text
vietnamese_text
change_type         added, modified, unchanged, deleted, split, merged
review_status       draft, reviewing, confirmed, rejected
created_by
created_at
confirmed_by
confirmed_at
```

### SourceAnchor

Connects a revision to evidence in the source document.

```text
id
requirement_revision_id
document_version_id
page_number
section_key
quoted_text
start_offset
end_offset
```

### RequirementRelation

Tracks continuity when requirements split or merge.

```text
source_requirement_id
target_requirement_id
relation_type        split_into, merged_into, supersedes, duplicates
created_at
```

## Version matching workflow

1. Extract candidate requirements from the new document version.
2. Match candidates against stable requirements using source anchors, normalized text, glossary terms, and structural context.
3. Assign a proposed change type and confidence score.
4. Require human review for uncertain matches, splits, and merges.
5. Create new revisions only after the mapping decision is recorded.

The AI must never silently replace a confirmed revision.

## Traceability

Q&A items, acceptance criteria, and test cases should link to a specific requirement revision when their meaning depends on that version. Product-level views may additionally resolve the current revision through the stable requirement ID.

## MVP simplification

The MVP uses PostgreSQL UUIDs and relational indexes. Vector matching is deferred. Initial matching can combine normalized text hashes, document section keys, and AI-proposed mappings that require human approval.
