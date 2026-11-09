# MySMS — schema principles and remaining proposals

The consolidation review this file came from has been applied: catalogue-by-code tables became enums, duplicated concepts were merged (reactions; annotations absorb lesson notes; one `assessment.exam_result` row per student and paper; cohort activities became content items), dead data was dropped, misplaced tables moved to their owning modules, reference data got one shape and one scope rule, JSONB went from 16 columns to 3, document numbering got a module-local `number_sequence` in each number-issuing module, and the module graph is a DAG with no cross-module foreign keys (references into other modules are registered logical references). What remains here is (1) the standing principles every future schema change follows and (2) the proposals not yet applied.

## 1. Principles (standing rules)

1. **One table per domain concept.** Reference lists are concepts too: each pick-list stays its own small typed table, owned by its module, with its own constraints and foreign keys.
2. **Generic in code, specific in schema.** Repetitive reference-data handling is written once in the application: one column shape, one generic repository, one API template, one cache.
3. **A `kind` column only for an exclusive arc** (kinds share key and columns). Merge two tables only when the same concept is modelled twice; never to save a table.
4. **Catalogues that only change with code, and carry no run-time state, are enums.** `comms.notification_type` stays a table because tenants switch types on and off.
5. **No JSON for structured data.** Typed columns, or key/value rows for genuinely open-ended sets. JSON only for opaque documents the database never queries (lesson content, audit properties, outbox payloads).
6. **Integrity lives in the database.** Foreign keys, unique constraints and CHECKs stay. Link tables stay link tables; arrays only for closed lists of codes, never for references to rows.
7. **Published content is versioned, never edited in place,** once anything (an attempt, a grade, an enrolment) refers to it. Lesson, quiz, question, assignment and rubric carry `version` / `superseded_by_id` / `published_at`.
8. **Each table lives in the module that owns its concept.** Module dependencies form a DAG; a reference against that direction is a logical id with an event-driven delete rule.
9. **Joins inside a service are fine; joins across services never happen.** Hot paths are fixed with caches keyed by an aggregate version, immutable snapshots and copied scope keys. Across services the tools are SDK batch calls, caches and event-carried copies.

## 2. Service-boundary rules (standing)

### 2.1 Who owns cross-cutting rows, and file lifecycle

- **Polymorphic rows.** `platform.entity_attachment`, `tag_assignment`, `entity_note`, `audit_log`, `deletion_log` and `file_object.owner_entity_type` hold rows *about* other services' entities, keyed by a table name. The owner is the service of that entity type; it validates the id and emits deleted events that platform acts on. Platform stores the pair opaquely. Allowed entity types live in each owning service's configuration, not in a CHECK in platform's schema.
- **Files.** Files are only soft-deleted. Owners release a file by event when the referencing row goes, and a reaper hard-deletes unreferenced files after a grace period (the `status` lifecycle on `platform.file_object` carries this).

### 2.2 The LMS is the boundary to watch

`lms` holds ~130 tables around one hub (`content_item`). If one service has to be split later, it is this one, along this line:

- **Authoring:** course, chapter, lesson, quiz, question bank, rubric, e-book, external tools.
- **Delivery:** cohort, enrolment, `content_item`, gradebook, release rules, forums, live classes.

`content_item` is the seam: delivery points at versioned authoring content (principle 7). Keep new LMS tables on one side of that line, so the split stays possible.

## 3. Remaining proposals (not yet applied)

| # | Proposal | What it needs | Why |
|---|---|---|---|
| C-1 | **Aggregate version columns** for cached roots: `lms.quiz.definition_version`, `lms.rubric.definition_version`, `lms.course.catalog_version`, bumped in the same transaction as any child write. | 3 columns + a code-review rule | `row_version` is per-row; it cannot key a cache of a whole aggregate. |
| C-2 | **Copied keys** J2: `cohort_id` on `lms.forum_topic` / `forum_post` (nullable; composite FK for section-based topics). | 2 columns | post → topic → section → forum on every cohort feed and unread count. |
| C-3 | **Copied keys** J3: `class_section_id`, `subject_id` on `academics.teaching_assignment` with `UNIQUE (id, class_section_id, subject_id)` on `class_subject`. | 2 columns + 1 unique | "my classes / subjects" without the extra hop. |
| C-4 | **Copied keys** J4: `register_id` on `attendance.attendance_record` with `UNIQUE (id, register_id)` on `attendance_sheet`. | 1 column + 1 unique | class-wise reports on the partitioned table. |
| C-5 | **User/person cards copied to consumers**: `display_name` + `photo_file_id` on `iam.user_account`; `display_name` + `sort_name` on `sis.student`, `sis.guardian`, `hr.staff`, fed by the person-updated event. | 8 columns | class lists, rosters and staff lists sort and page inside their own service. |
| C-6 | **Batch-get API** (AIP-231) for the most-referenced resources (`BatchGetUserCards`, `BatchGetStudents`, `BatchGetDownloadUrls`, ...). | ~11 operations | one call per screen, never one per row (PRINCIPAL-REVIEW P1-34). |
| C-7 | **Effective-dated staff history**: `hr.staff_position (staff_id, designation_id, department_id, reporting_to_staff_id, employment_type, effective_from, effective_to)`, no overlapping periods; `staff` columns become the current projection. | +1 table | promotions and transfers are lost today; payroll and seniority reports need them. |
| C-8 | **Authorisation without joins**: token carries role grants + an `authz_version`; per-tenant role→permission matrix cache refreshed by iam events. | code + cache | removes the 5-table authorisation walk from every request. |

## 4. Archival and retention (standing)

| Module | Archival unit |
|---|---|
| sis, assessment, attendance, finance | institution + academic year |
| lms | cohort end date |
| comms, logs | month (partitions) |

- Retention per table follows ENHANCEMENTS E35.
- `platform.deletion_log` is kept for at least the longest offline-sync window. A tombstone that expires before a tablet syncs is a correctness bug.

## 5. Modelling risks to keep watching

- **Institution attributes (EAV)** (`org.attribute_definition`, `org.institution_attribute`): keep for genuinely custom fields; promote any attribute the product logic depends on to a typed column.
- **`lms.content_item` exclusive arc:** new item types follow the same pattern — a typed FK plus the exactly-one and kind-match CHECKs.
- **No N+1:** every repository method that loads children takes a list of parent ids (`= ANY(:ids)`). The single most valuable code-review rule in this file.

## 6. Considered and rejected (so the discussion is not reopened)

| Idea | Why not |
|---|---|
| Lookup tables for the simple pick-lists | Uniformity comes from one shape and one code path, not one table. |
| `lms.bookmark` → `lms.annotation` | A bookmark is a saved navigation link; an annotation is a position in a document. Different concepts. |
| Fold `comms.notification_preference_set` into its preferences | Per-user, per-role settings such as quiet hours need the set as a real entity. |
| `lms.external_tool_option` → code arrays | An exclusive arc of two open code lists; keeps uniqueness in the database. |
| The question answer tables, rubric criterion/level/cell, catalogue-page tables | Different concepts with different shapes; load by `= ANY(:ids)` and cache by aggregate version. |
| `cohort_enrollment` + `cohort_teacher` | A student's enrolment and a staff assignment share only (cohort, user). |
| `homework` → `assignment`; the approval tables; the submission tables | Different workflows that evolve independently. |
| `public_profile` → `person` | An optional public aspect with its own lifecycle and visibility. |
| Reactions across modules | Same idea in different modules; merging would cross service boundaries. |
| Link tables → arrays | Principle 6. |
| Fewer, larger services | The DAG and the SDK rules exist precisely to avoid this. |
