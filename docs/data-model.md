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

### AiJob

Each AI attempt records its project, exactly one immutable document version or
requirement revision, purpose,
provider/model identifiers, correlation ID, requesting user, lifecycle timestamps,
candidate count, and a bounded error summary. Raw prompts and document text are not
stored in the job table. A successful job creates only draft requirement revisions;
each revision retains `document_version_id` and `source_anchor` provenance.

### ClarificationQuestion and AcceptanceCriterion

Both artifacts belong to a specific `RequirementRevision` and retain the creating
`AiJob`. Japanese and Vietnamese text are stored together. Review state is one of
`DRAFT`, `APPROVED`, or `REJECTED`, with reviewer and timestamp provenance.
Clarification questions additionally retain bilingual answers and answer provenance.
The database keeps one analysis job per revision and purpose so repeated generation
cannot silently duplicate drafts.

### VerificationTestCase

Each generated test case belongs to one `RequirementRevision`, one creating
`AiJob`, and exactly one approved `AcceptanceCriterion`. It stores bilingual
title, preconditions, steps, and expected result together with priority and the
same `DRAFT`, `APPROVED`, or `REJECTED` review provenance. The database permits
one test-case generation job per revision and purpose; the service validates
that provider-returned criterion IDs are part of the approved input set.

## Project glossary

Each glossary term belongs to exactly one project and stores a Japanese term,
its preferred Vietnamese equivalent, optional usage notes, the creating user,
and timestamps. Japanese terms are unique within a project. All members can
search the glossary; only `ADMIN` and `BRSE` members can create, update, or
delete terms. Mutations are recorded in the project audit trail.

## Documents and versions

`Document` is the stable project-scoped identity and lifecycle record.
`DocumentVersion` is immutable and stores the version number, original filename,
media type, byte size, SHA-256 checksum, opaque storage key, uploader, and upload
time. File bytes stay outside PostgreSQL. A new upload appends a version instead
of replacing prior content, and archiving the document retains its complete
history for traceability.
