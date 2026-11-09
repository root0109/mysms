# MySMS — proposed enhancements (for discussion)

**Status: proposed. Nothing in this document has been applied.** The database and API designs in `database/` and `api/` are complete and verified as they stand. This document lists what a senior team would add or change next, so MySMS is ready for its target stack and clients:

- **Stack:** Java Spring Boot (Maven), PostgreSQL with Flyway migrations, a deliberately lightweight database (tables, constraints and indexes only).
- **Clients:** a React web app; React Native mobile apps for students, parents and teachers; a tablet app for learning.

Decide each item (accept / reject / defer). Accepted items are then applied to the schema specification, and the migrations, data dictionary, ER diagrams and API contracts are regenerated and re-verified together. Earlier review items that were applied have been removed from this list; open ones live in `PRINCIPAL-REVIEW.md`.

Priority: **P1** needed before the first production release · **P2** needed for the mobile / tablet launch · **P3** later.

---

## 1. Backend platform (Spring Boot, Spring JDBC, Flyway)

| ID | Pri | Enhancement | Why | Proposal | DB impact | API impact |
|---|---|---|---|---|---|---|
| E2 | P1 | **Data-access conventions with Spring JDBC (no ORM)** | Project rule: no ORM layer. Hand-written SQL across ~360 tables needs one consistent pattern so tenant scoping, audit columns, optimistic locking and soft delete are never forgotten. | One repository per table built on `JdbcClient` / `NamedParameterJdbcTemplate`, mapping rows to Java `record`s with explicit `RowMapper`s (or `DataClassRowMapper` where column and component names match). A shared `SqlSupport` helper appends `tenant_id = :tenantId AND deleted_at IS NULL` to every read, sets `created_by` / `updated_by` / timestamps on writes, and performs updates as `UPDATE … SET …, row_version = row_version + 1 WHERE id = :id AND row_version = :expected` (0 rows → 409 conflict). IDs are UUIDv7 generated in Java (e.g. `com.github.f4b6a3:uuid-creator`). Enums map to `varchar` with `Enum.name()` / `valueOf`; `jsonb` is read and written as text through Jackson (`?::jsonb` cast in the SQL). SQL lives next to each repository as constants or `.sql` resources; integration tests run every repository against the real migrations (Testcontainers PostgreSQL). | None. | None. |
| E5 | P1 | **Flyway practice** | Keep 21 modules evolving safely in one history. | Keep the naming already used (`V<release>_<module>_<seq>__…`). Add repeatable migrations `R__<module>_reference_data.sql` for platform-defined rows (roles, permissions, app modules, notification types, release condition types, time zones, currencies). CI runs `flyway validate`, applies all migrations to a Testcontainers PostgreSQL and runs the repository integration tests against it, so SQL that no longer matches the schema fails the build. Forbid editing applied migrations (checksum check). | New `R__` seed files. | None. |
| E7 | P2 | **Background jobs** | Imports / exports, e-book conversion, plagiarism polling, grade exports, archive builds, reminder sends. | One job runner per module (JobRunr or Spring Batch for imports) that records progress in that module's own `operation` table (LMS course packages also in `lms.import_export_job`). | Done: every service schema has `operation`. | Done: each module's `OperationService` + the platform job centre. |

## 2. Mobile and tablet apps (React Native)

| ID | Pri | Enhancement | Why | Proposal | DB impact | API impact |
|---|---|---|---|---|---|---|
| E8 | P2 | **Delta sync for offline use** | Student and teacher apps must work on poor connections: timetable, notices, assignments, gradebook, attendance taking. | `GET /v1/sync/changes?since=<cursor>&scopes=cohort:…,class_section:…` returns rows changed since the cursor per resource type (by `updated_at`, which every table has), soft deletes (`deleted_at`; ordinary List operations hide deleted rows, so sync needs this dedicated operation or a `showDeleted` flag), and hard deletes from `platform.deletion_log` (already in the schema). Cursor = high-water mark of a monotonic change sequence. Needs a `(tenant_id, updated_at)` index on synced tables. | Index per synced table; optional `platform.sync_cursor` (device, scope, cursor). | New `platform.GetChanges` (REST + gRPC streaming). |
| E9 | P2 | **Offline writes with conflict handling** | Teachers take attendance and grade offline; students answer quizzes offline. | Every write carries `If-Match` (row_version) and an `Idempotency-Key` (S8). The server rejects stale writes with 409 + current row; the app shows a merge prompt. Quiz attempts already autosave (`SaveQuizResponses`, last write per question wins). | Done: each module's own `idempotency_key` table (S8). | 409 conflict body documented. |
| E10 | P2 | **Tablet offline learning packages** | The tablet LMS should download a chapter (lessons, files, e-books, quizzes) and work without a connection. | `lms.offline_package` (course / cohort, chapter, version, manifest jsonb, package_file_id, size, content_hash, built_at). A job rebuilds the package when content changes (`content_updated_at`). The app downloads via a signed URL and reports progress back through delta sync. | 1 table. | `lms.ListOfflinePackages`, `lms.GetOfflinePackageDownload`. |
| E11 | P2 | **Adaptive video streaming** | Lesson videos on mobile data need HLS / DASH renditions, not single MP4 files. | Use the existing `platform.file_object.processing_status` for a transcoding job; add `platform.file_rendition` (file_id, kind = hls / mp4_480p / thumbnail, storage_key, bitrate, width, height). Serve through a CDN with signed cookies. | 1 table. | `GetDownloadUrl` returns the HLS manifest when available. |
| E12 | P2 | **Push notifications end to end** | Parents expect push for attendance, fees, notices, grades. | `iam.user_device` already stores push tokens. Add per-user / per-category opt-in (already modelled by `comms.notification_preference`), quiet hours, and delivery receipts on `comms.outbound_message`. Send via FCM (Android / iOS through APNs). | `quiet_hours` on preference set. | Devices are registered through `iam.Login` (device field); add `comms.UpdateQuietHours`. |
| E13 | P2 | **App-specific aggregate endpoints (BFF)** | Mobile screens need one call per screen. | Keep the resource API as is and add a thin backend-for-frontend per app: student home, parent home (all children: attendance today, fees due, notices, homework), teacher home (today's sessions, attendance to take, items to grade). Several already exist (`GetStudentDashboard`, `GetTeacherDashboard`, `ListMyChildren`); add `GetParentHome`, `GetTeacherToday`. | None. | 2–3 query operations. |
| E14 | P2 | **Mobile app release control** | Force-upgrade old app versions; staged feature rollout. | `platform.app_release` (platform, min_supported_version, latest_version, store_url, release_notes) and `platform.feature_flag` (key, tenant_id nullable, enabled, rollout_percent). | 2 tables. | `platform.GetAppConfig` (public). |
| E15 | P2 | **Lean payloads** | List responses carry every column. | Support `fields=` (REST) / `FieldMask read_mask` (gRPC) on List and Get; ETag + `If-None-Match` for 304 responses on reference data; gzip/brotli. | None. | Add the parameters to every List/Get. |

## 3. Learning (LMS) capabilities

| ID | Pri | Enhancement | Why | Proposal | DB impact | API impact |
|---|---|---|---|---|---|---|
| E18 | P1 | **Gradebook → report card bridge** | Teachers should not re-type online grades into marksheets. | Mapping table `assessment.gradebook_mapping` (exam_session / exam ↔ lms.grading_category or content_item, weight). A job copies published gradebook totals into `assessment.exam_result` (status draft) for teacher review. | 1 table. | `assessment.ImportLmsGrades`. |
| E19 | P2 | **LTI 1.3 / LTI Advantage** | The external-tool tables model LTI 1.1-style key/secret settings. Current tools require LTI 1.3 (OIDC launch, JWT, deep linking, grade services, roster services). | `lms.lti_registration` (issuer, client_id, deployment_id, auth_login_url, auth_token_url, jwks_url, tool_public_key, platform key id) linked to `lms.external_tool`; keep `external_tool_setting` for 1.1 tools. Platform key pair in a KMS. | 1 table. | `LaunchExternalTool` returns an OIDC login-initiation form; add JWKS endpoint and AGS score endpoint (replacing the 1.1 outcome webhook). |
| E20 | P2 | **xAPI / cmi5 learning records** | Detailed learning analytics, SCORM successors and external content. | `lms.learning_statement` (actor_user_id, verb, object_type, object_id, result jsonb, context jsonb, timestamp), partitioned by month; or forward statements to an external LRS. | 1 table (partitioned). | `lms.RecordStatements` (batch). |
| E22 | P2 | **One cohort membership table** | Students (`cohort_enrollment`) and staff (`cohort_teacher`) are separate today, but rosters, observers and role changes treat them together. | `lms.cohort_member` (cohort, user, role_id, kind = student / staff / observer, status, final grade fields, removal fields), keeping enrolment payment fields on a 1:1 extension. | 2 tables merged. | `GetCohortRoster` / `SetCohortMembers` stay; standard CRUD merges. |
| E23 | P2 | **Learning outcomes and standards** | `learning_objective` and `course.state_standard_code` are free text. Schools report against curriculum standards (e.g. national frameworks). | `academics.standard_framework` / `academics.standard` (code, description, grade levels, parent) and `lms.objective_standard` mapping; question ↔ objective mapping already exists in answers, add `lms.question_objective`. | 3–4 tables. | Standard CRUD + mastery report. |
| E25 | P3 | **At-risk student model** | Alerts exist (`lms.student_alert`) but rules are implicit. | `lms.alert_rule` (metric, threshold, window, audience) evaluated by a job; feeds `student_alert`. | 1 table. | CRUD + `EvaluateAlertRules`. |
| E26 | P3 | **Open Badges 3.0 / verifiable certificates** | Portable credentials for students. | Add `credential_json` and signing key reference to `lms.certificate` / `lms.badge_award`; public verification already exists. | 2 columns each. | `GetVerifiableCredential`. |

## 4. API

| ID | Pri | Enhancement | Why | Proposal |
|---|---|---|---|---|
| E27 | P1 | **Event contract (AsyncAPI)** | Integrations and modules subscribe to domain events (student admitted, payment received, grade published). | Publish an AsyncAPI 3 document generated from the same model as OpenAPI / proto; reuse the proto messages as event payloads. |
| E28 | P1 | **Outbound webhooks for tenants** | Schools integrate with their ERP, SMS gateways and identity providers. | `platform.webhook_subscription` (url, events, secret_enc, active) + `platform.webhook_delivery` (attempts, status); HMAC-signed deliveries with retry. |
| E29 | P2 | **Single sign-on and workspace integration** | Schools use Google Workspace / Microsoft 365; universities use SAML. Users also expect their workspace mailbox (unread count) and calendar inside the app. | `iam.identity_provider` (tenant, protocol = oidc / saml, client config_enc, domain hints, JIT provisioning rules) and `iam.external_identity` (user, provider, subject, delegated token_enc). Spring Security OAuth2 client / SAML2. With delegated consent, read the mailbox unread count and two-way calendar sync through Microsoft Graph / Google APIs (`comms.calendar_export` already tracks pushed items). |
| E30 | P2 | **gRPC for internal and streaming, REST for apps; proto field behaviour** | Browsers and React Native work best with REST/JSON; gRPC gives typed service-to-service calls and server streaming. | Keep both contracts in parity (already enforced). Expose gRPC-Web only if a client needs streaming; use server streaming for `GetChanges` (E8) and live-class events. |
| E31 | P3 | **Bulk endpoints** | Teachers mark 40 attendance rows or grade 30 submissions at once. | Gradebook grading already has `BatchGradeEntries`; extend the pattern (`:batchCreate` / `:batchUpdate`, AIP-233/234) to rubric assessments, item overrides and other high-volume teacher actions. |

## 5. Security, privacy and compliance

| ID | Pri | Enhancement | Proposal |
|---|---|---|---|
| E32 | P1 | **Key management for `*_enc` columns** | The envelope format `v1.<key_id>.<iv>.<ciphertext>` is now the documented storage convention. Remaining work: per-tenant data keys wrapped by a cloud KMS key, and a background re-encryption job for rotation. |
| E35 | P2 | **Data retention and erasure** | Per-table retention rules (`platform.retention_policy`), purge jobs, and a subject-access export (`platform.ExportMyData`). |
| E36 | P2 | **Abuse and rate limiting** | Gateway rate limits per tenant / user / IP; bot protection on login and signup instead of CAPTCHA pages; lockout already uses `failed_login_count`. |

## 6. Data, performance and operations

| ID | Pri | Enhancement | Proposal |
|---|---|---|---|
| E39 | P2 | **Search** (S16) | Start with PostgreSQL full-text (`tsvector` generated columns + GIN) for people, courses, media and notices; move to OpenSearch fed by the outbox when needed. |
| E40 | P2 | **Read replica and caching** | Route reports and dashboards to a read replica; cache reference data (countries, time zones, roles, permissions) with ETags. |
| E41 | P3 | **Schema linting in CI** | Run `squawk` (or equivalent) on new migrations to catch locking DDL, missing indexes on new FKs and NOT NULL additions without defaults. |

---

## How the items relate to the existing review

- Resolved by the classroom-LMS merge: **S12** (discussions → `lms.forum*`).
- Resolved: **S8** (Idempotency-Key on the API, stored in each module's own `idempotency_key` table), **S3** (every service schema has its own `operation` table, served by that module's `OperationService`; `lms.import_export_job` covers LMS course packages), **S18** (`platform.file_object.status` holds the pending / available / quarantined / deleted scan lifecycle with `scanned_at`, and `processing_status` tracks post-upload processing).
- Partly resolved: **S7** (`org.holiday` and `lms.cohort_holiday` exist; working-day kinds such as working Saturdays are still open), **S10** (`comms.notification_channel` + properties hold e-mail / SMS channel settings; several provider accounts per channel are still open).
- Still open as listed: A1–A16 and S1, S2, S4–S6, S9, S13, S14, S16, S17.
