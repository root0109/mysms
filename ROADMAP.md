# MySMS roadmap: what is built, what is open, what to do next

This is the one backlog for MySMS. It lists what exists, every open item, the decisions waiting for the owner and the
next chunk of work. It is not an audit trail: when an item is done, delete its row. Do not record history here.

How to use it:

- Pick work from section 5 (next chunk) unless the owner says otherwise.
- Items keep the IDs they had in earlier reviews (P1-34, E43, C-6, D-05, D1-D14 of the improvement review...) so code
  comments, `ui/` and `api/` references still resolve. New items use an area prefix (ARC-, API-, DB-, SEC-, PERF-,
  BLD-, OPS-, UI-, DOC-, NM- for new modules).
- Priorities are kept as each source gave them: **P0/P1/P2/P3** (P1 = before the first production release, P2 = for
  the mobile/tablet launch, P3 = later) or **H/M/L** (high/medium/low).
- The exact state of every API operation is in [api/operations-status.md](api/operations-status.md), written by the
  build. Do not copy its numbers anywhere else.

---

## 1. What exists today

### Backend

- **Modules:** flat Maven reactor of 28 projects: `core`, `auth-client`, `messaging`, 21 domain modules (one per
  PostgreSQL schema, each also buildable as its own microservice), `app` (the monolith) and `conformance`.
- **Operations:** 2,055 in the contracts, 2,004 served over REST and gRPC with parity tests; 51 answer
  `501 NOT_IMPLEMENTED` (comms 33, people 7, library 5, org 3, timetable 2, frontoffice 1). Every operation of
  academics, admission, assessment, attendance, careers, facility, finance, hr, iam, lms (712), master, payroll,
  platform, sis and transport is served. Per-operation detail: api/operations-status.md.
- **Generic capability:** standard operations of 327 record types (validation, uniqueness 409, in-module references
  422, If-Match/ETag 428/412, masks, filters, sorting, keyset paging with `PagingInfo`, soft delete, audit and outbox
  events, institution scope, encrypted write-only secrets); state transitions of 30 record types
  ([api/state-machines.md](api/state-machines.md)); Idempotency-Key on every operation that declares it (tenant
  callers); monthly partitions with `PartitionMaintenanceJob`; long-running operations in 10 modules.
- **Workflows built end to end:** sign-in with argon2id, lockout, refresh rotation, TOTP second factor and recovery
  codes; hierarchical settings; access management with no-escalation rules; attendance taking (flow F3); homework
  lifecycle (flow F5); lesson plan review; students (sis), admission and enrolment, staff (hr), staff leave (HR-04),
  payroll and payslips, assessment (exams, marks, results, grading), timetable view (TT-03), LMS with provider
  adapters (Zoom, CloudConvert, Turnitin, Google Calendar, LTI 1.1) and guest access by `X-Tenant-Code`.
- **Events:** MySMS `messaging` library (RabbitMQ default; Pub/Sub and in-process alternates), outbox relay as
  `mysms_relay`, exactly-once inbox, dead-letter and replay, per-module broker users; audit flows into
  `platform.audit_log`; generated reference cleanup (153 actions in 19 modules) from
  [database/logical-references.md](database/logical-references.md).
- **Inter-module calls:** gRPC between all 21 modules with the caller's token or a service token (client credentials).
- **Data security:** `tenant_id = :tenantId` on every statement (conformance rule R1) with FORCE row-level security
  as backstop, `DataScope` institution scope, per-tenant AES-256-GCM field encryption, no database error leaks.

### Database

- 21 schemas, one Flyway baseline per module, no cross-schema foreign keys (references into other modules are
  registered logical references), smoke suite `database/verification/smoke.sql`. Conventions and principles:
  [database/README.md](database/README.md).

### Clients

- pnpm monorepo: apps `web`, `platform-console`, `mobile`, `tablet`; shared `sdk` (generated from the backend's
  OpenAPI documents), `core`, `ui-web`, `ui-native`, `web-kit`, `native-kit`, `tokens`, `i18n`.
- **Web:** 93 of the 186 inventory screens built (account and security ACC-01/07/08/10/14, records ADM-11 over all
  327 record types, settings ADM-16, attendance ATT-03/04/05, homework and assessment ASM-01...13, students SIS-02/03,
  admission ADMN-01...05, staff and payroll HR-01...09, LMS-01...55, timetable TT-03, and 65 list-manager screens).
  The rest are routed placeholders. Screens are lazy-loaded per route.
- **Platform console:** PLT-01...05 (tenants, plans, enquiries, reserved keywords and notification types, system
  settings), operations and jobs, master data.
- **Phone and tablet:** sign-in, profile, settings, notification preferences, TT-03, HR-03/04/08, SIS-02/06,
  SIS-10 (student), SIS-11 (parent), assessment screens, LMS screens.

### Build, tests and tooling

- `mvn clean install` (JDK 17): about 5,500 tests (unit, component against real PostgreSQL 16 and RabbitMQ,
  REST/gRPC parity), Checkstyle, SpotBugs, JaCoCo, conformance; about 36 minutes on a 2-core machine.
- `pnpm verify --concurrency=1`: 39 tasks (prettier, SDK generation, typecheck, lint, tests, builds); web initial JS
  287.5 KB gzip of a 300 KB budget.
- All tooling is Java inside the Maven build; no CI pipeline, containers or e2e tests exist yet.
- Deployment rehearsed on a fresh database: [DEPLOYMENT.md](DEPLOYMENT.md).

---

## 2. Decisions waiting for the owner

Defaults apply until decided. "Rec." is the reviewers' recommendation.

### 2.1 Architecture and platform

| ID | Question | Options | Rec. / default |
|---|---|---|---|
| D1 (improvement) | **Operating model.** Monolith and 21 microservices from the same modules; every inter-module call pays gRPC (proto, `ProtoBridge` JSON, Jackson DTO, so double serialization even in-process), separate transactions, PLAINTEXT hops, 26 `*GrpcClient` classes. No measured need to split. Also: distinguish *module boundary*, *runnable artifact* and *production-ready service* in docs, with a written readiness gate per service (its operations served, own database and broker provisioned, service auth and health verified, deploy/rollback/monitoring documented). | (a) keep "21 independent services" target; (b) modular monolith is the only supported production shape for v1, extract a service only against a metric | (b), keep proto contracts |
| DEC-1 | **Module-call path inside the monolith.** CLAUDE.md requires gRPC exactly as the reference service, even in-process. | (a) keep one remote path until benchmarks show a bottleneck, but specify timeouts, bounded retries, circuit breaking/bulkheads and per-call latency targets; (b) in-process Java adapter in the monolith profile with contract-level parity tests | Measure first; (b) if adopted with D1 |
| D2 (improvement) | **Proto copies.** Each caller keeps verbatim copies (`people.proto` x10, `org.proto` x9, `iam.proto` x7) and recompiles them; `lms.proto` is 16.5k lines and its generated code pushed SpotBugs past its timeout. Conflicts with the CLAUDE.md "no deviation" rule. | (a) keep copies; (b) one `<module>-grpc-api` artifact per callee, compiled once | (b) |
| D9 / E46 | **Connection budget.** One Hikari pool per module: 21 x 10 = 210 connections per monolith instance, plus replica pools (x2) and relay pools (21 x 2), times instances; Cloud Run caps an instance at 100. | (a) one shared pool per monolith instance (SQL is schema-qualified, `set_config(..., true)` is transaction-local); (b) pools of 2-4 per module behind PgBouncer (transaction mode); also define a small/local and a production profile | (a) or (b); target <= 20 connections per instance; measure pool wait, event lag, relay batch time, idle query rate first |
| D12 | **Event payloads and relay.** Payloads are id-only, so consumers call back (remote N+1); with several relays, per-aggregate ordering is not guaranteed; relays poll every 500 ms (21 pollers) and hold a transaction across broker confirms (up to 20 s). | (a) keep; (b) payloads carry `row_version` plus minimal state, consumers drop stale events, one relay leader per module, LISTEN/NOTIFY or idle backoff | (b) |
| D5 (improvement) | **Read replica default.** Reads go to the replica when one is configured: read-your-writes trap (GET after PUT, ETag checks, permission reads lag). | (a) keep; (b) primary by default, replica opt-in for lists and reports | (b) |
| D6 | **Flyway baselines edited in place** (23k lines, lms 6.7k) until the first release. Breaks once a pilot holds real data; the upgrade path was never rehearsed. | Freeze at first external pilot and add a migration-upgrade test now | Freeze at first pilot |
| D7 | **PostgreSQL extensions.** "No extensions" means search is `ILIKE '%x%'` (sequential scan) and partition DDL runs in-app as owner. | Allow `pg_trgm`, `pg_partman`, `pg_cron` (all on Cloud SQL) | Allow |
| D8 | **Migrations at app start as `mysms_migrate`.** Every instance holds credentials that can `ALTER TABLE ... NO FORCE ROW LEVEL SECURITY`; 21 Flyway runs and advisory-lock races per boot. | Separate migration job or init container; app holds only `mysms_app` | Separate job |
| D10 | **Java version.** `spring.threads.virtual.enabled=true` does nothing on 17; on 21+ request threads are unbounded against 10-connection pools. | Stay on Java 17; remove the no-op `spring.threads.virtual.enabled`; consider AppCDS for Cloud Run start-up | **Decided: Java 17 stays** |
| D11 | **Token forwarding.** User bearer token forwarded hop to hop over PLAINTEXT gRPC; a compromised service can replay user tokens. | mTLS or mesh plus audience-scoped service tokens before any multi-service deployment | Moot if D1(b) |
| D13 | **Web refresh token** is in memory only: reload or new tab signs the user out (a teacher mid-attendance). | httpOnly SameSite cookie with CSRF protection | Cookie |
| D4 (decision) | **Transactions by inheritance** (`extends AppTransactionManager`). Composition would be equivalent and easier to test. | Keep the owner's choice; fix the edge cases (work item BE-3) | Keep |
| DEC-2 | **Tenant placement.** One shared database for all tenants; the only scale lever is moving a module out. | Design a tenant-to-cluster placement key while the schema is mutable; add `data_region` on the tenant before US expansion (P2-08) | Design now |

### 2.2 Product and API

| ID | Question | Options | Rec. / default |
|---|---|---|---|
| D1 (review) | **Authority model.** Blocks P1-27. Today a token carries one institution set for all grants (union; STATUS limit 1): a user with different roles in two schools has the union in both. | Union of all role grants, or only the active role counts | Union of grants |
| D2 (review) | **Institution scope.** ui D-04 keeps a client-side switcher. | Scope from token grants plus each record's `institutionId` (drop unused `X-Institution-Id`), or wire the header into every operation | Record-field scoping |
| D4 (review) | **Currencies.** | Two-decimal only for v1 (`numeric(14,2)`), or four decimals now | Two-decimal |
| D5 (review) | **Inventory/store module.** | Not in v1, or design now | Not in v1 |
| DEC-3 | **Self-scoped staff read** (`hr` has none; teacher roles need `hr.staff.read`, which shows colleagues' records). Affects TT-03, HR-04, HR-02. | Add a self-scoped read operation, or accept | Add it |
| DEC-4 | **Own payslips.** Payroll officer = holder of `payroll.payslip.generate_payslips` in the institution; everyone else reads only own payslips. | Keep the rule, or add an explicit own-payslips permission | Owner to choose |
| DEC-5 | **Public admission application.** ADMN-05 needs a signed-in applicant account; the contract has no public operation and `applicationNumber` is required on create (client proposes `W-<yyyymmdd>-<6 chars>`, server replaces it from `admission.number_sequence`). | Add a public application operation, or keep account-only; in either case add an `applicant` role template (the role must never be granted tenant-wide) | Owner to choose |
| DEC-6 | **Self-sign-up role scope** (in progress): granted for one school, not tenant-wide. | Confirm and finish | Finish |
| DEC-7 | **Attendance summary by register kind.** `SummarizeAttendance` adds daily (class) and period (subject) registers together; class grouping sees only class registers. | Add a contract parameter for the register kind | Owner to choose |
| DEC-8 | **Problem details in the reader's language** (E52, P3, started: core's `MessageUtil`, `ConstraintMapper`, `messages/mysms*.properties` cover field errors by code; hand-written service sentences and translations are not). | Server resolves from `Accept-Language`, or stable codes only with sentences in client catalogues | Decide once |
| DEC-9 | **Output-only fields sent by a client.** api/README says both "ignored" (Standard methods) and "rejected with 400 `FIELD_NOT_WRITABLE`"; the server ignores them. | Keep one rule and fix the text (and P1-32's "read-only in mask -> 422") | Owner to choose |
| DEC-10 | **`platform.tenant_domain`** (custom domains): tenant-facing resource or platform-internal (part of P1-28). | | Owner to choose |
| DEC-11 | **One cohort membership table (E22, P2)** merging `cohort_enrollment` and `cohort_teacher` into `lms.cohort_member` conflicts with the schema principle that rejected this merge (they share only cohort and user). | Accept E22 or keep rejected | Keep rejected unless roster needs prove otherwise |
| DEC-12 | **SpotBugs exclusion.** `Lookup` was added to the `EI_EXPOSE_REP2` class-name allowlist (`backend/config/spotbugs/exclude.xml`); JAVA-CODING-RULES §13 wants each exclusion approved. | Approve or replace | Approve |
| DEC-13 | **Confirm decisions taken on the owner's behalf.** Staff status moves (active <-> inactive, either -> exited, exited -> active; leaving needs no exit date; publishes `hr.staff.updated`); login names (staff: employee number, students: admission number, existing iam account reused, none for staff who left); applicant self-service rules; application step order follows database states and operations.md over ui/03 (see 2.3); operations that finish within the request are stored as finished 202 operations; payslip arithmetic (period inside one month, pro-rated by days in force, half-up to the cent, employer-share lines in neither total, existing payslip kept); owner-only payslips; applicant numbering; `sendInvite: true` refused by sis and hr; sis does not call attendance, library or finance. LMS: guests by `X-Tenant-Code` resolved through iam (cached 60 s); providers behind ports, unconfigured = 503; webhook HMAC-SHA256 over `<X-Timestamp>.<body>` with 5-minute tolerance; LTI outcome OAuth 1 verified at the edge; lesson content as blocks; graders may delete any draft submission. | Confirm or change each | Confirm |
| DEC-14 | **Release scope.** Which clients, personas and flows form the first release; is offline tablet support a release requirement. Placeholders and 501 operations to be listed as unavailable features in the release status. | | Pick first personas and build them end to end |
| DEC-15 | **CI hosting, required checks and targets.** Confirm the metric targets in section 5.3 and the device/database profiles they are measured on. | | Owner to choose |
| DEC-16 | **ENHANCEMENTS proposals** (the E-items in section 3) were never accepted or rejected item by item. | Accept / reject / defer each | Decide as each is scheduled |

### 2.3 Spec vs contract choices already built (confirm or change the spec)

| Screen / flow | Difference | Built as |
|---|---|---|
| F2 step 1, ADMN-01 | ui/03 lists `StartAdmission` before `OpenApplications`; the cycle states put admission after open. | Confirm -> OpenApplications -> StartAdmission -> Close |
| F2 step 3, ADMN-03 | ui/03 places `ConfirmApplication` after approval; operations.md defines it as "office confirms complete". | Submitted (or on hold) -> Confirm -> Approve -> ConfirmAdmission (fee paid or waived) -> EnrollApplicant |
| ADMN-05 | Inventory marks the form "W (public)"; contract needs a token. | Signed-in applicant (DEC-5); no uploads yet |
| ADMN-04 | A class section has no name in the contract. | Wizard takes the class section or batch id, prefilled |
| HR-08 | Inventory offers a payslip PDF; no operation renders one (`document_file_id` empty). | On-screen payslip with a "PDF not available" note |
| HR-07, F7.2 | ui/03 says "deductions from leave"; request carries no leave/attendance input. | Pay percentage 100, no leave deduction |
| HR-05 | Template components have no order in the contract. | Listed in insertion order |
| LMS-49 | Contract's `ReceiveToolOutcome` also names LTI Advantage scores. | LTI 1.1 Basic Outcomes only; AGS JSON answers 422 (see P1-39) |

---

## 3. Open work by area

Columns: **ID**, **Pri**, **What and why**, **Where / refs / depends on**.

### 3.1 Architecture and cross-module

| ID | Pri | What and why | Where / refs / depends on |
|---|---|---|---|
| P1-34 | P1 / M | **Batch card reads between modules.** Every screen showing a name owned by another module pays for it: attendance reads students and persons forty at a time with `id = ... OR ...` filters; classes need three list reads. Add `BatchGet` (AIP-231) plus small `*Card` projections, cached per request, for the most-referenced resources (person/user, student, staff, class section, download URLs: `BatchGetUserCards`, `BatchGetStudents`, `BatchGetDownloadUrls`, about 11 operations). Base of every grid still to build (gradebook, timetable, fees). Also still open from P1-34: audience tag per operation (public / internal / platform). Service tokens are built. | Merges E43, C-6, P2-02 (batch part), ui D-09 (flag `sdk.batchGet`; interim memoised per-id fetch in `core`) |
| ARC-1 | H | **Reference reconciliation.** References into other modules are stored unchecked (limit 2); cross-school in-module references are possible (limit 3); deleting a row does not check others' references first, cleanup is event-driven and at-least-once (limit 4). The 494 logical references are never reconciled. Add a dangling-reference report per module and a reconciliation job; keep copied keys J2-J4 consistent from the owner's events; make unique-violation races map to 409, not 500. | P2-02, database/logical-references.md; depends on P1-34 |
| ARC-2 | P1 | **Message delivery.** Build the comms relay once: provider adapters (e-mail, SMS, push via FCM/APNs) behind an interface, per-tenant channel settings, several provider accounts per channel (S10), retry and suppression lists, delivery receipts on `comms.outbound_message`, push quiet hours and per-category opt-in (E12). Blocks: absence notices to guardians (attendance sheet completion), homework class/reviewer notifications, password reset by link, fee reminders, announcements, `sendInvite` on portal accounts (refused 422 today), LMS unread counts and cohort announcements, `people.SendGreetings`. | E51, E12, ui D-24 (`notif.quietHours`), comms 501 operations |
| ARC-3 | H | **Persona (own-data) operations.** Missing "own" reads: students and guardians have no attendance view, no homework screens (ASM-04 hand-in and reply), no own timetable (needs sis enrolment of the caller); `assessment.homework_submission.read` and `sis.ListClassRoster` are institution-wide (roster returns id-card and board registration numbers for all classes, not the teacher's sections); `timetable.session.get_timetable` does not narrow to own (ui/02: S own, T teaching, P per child); personal rows with no institution path (addresses, emergency contacts, job applications) are readable tenant-wide by any holder of the read permission. | STATUS limits 5, 11, 12, 14; depends on DEC-3 |
| P1-37 / E13 | S / P2 | **Composite reads (BFF).** Move cross-module summaries to a `/v1/bff/<app>/...` namespace, the only place they are composed; add `GetParentHome` (all children: attendance today, fees due, notices, homework) and `GetTeacherToday`. `sis.GetStudentSummary` leaves attendance, open loans and outstanding fees empty because sis may not call those modules; LMS-20 composes its reads instead of the served teacher dashboard. | |
| ARC-4 | P2 | **Background job pattern.** None exists: payslip runs, bonafide, apply promotions, enrolment portal step run inside the request (grows with staff count); gateway refunds have no sweep job; imports/exports, e-book conversion, plagiarism polling, grade exports, reminder sends need one. One runner per module (JobRunr, or Spring Batch for imports) recording progress in the module's `operation` table. Then sweep unbounded synchronous ops to 202 + `Operation` (`EvaluateReleaseRules`, `PublishFinalGrades`, bulk sends) with one shared `BatchResult` shape (per-row result/error, counts, atomic flag). | E7, P1-31 |
| P1-36 / E27 / E28 | P1 / M | **Event contract and outbound webhooks.** Event messages in proto with a CloudEvents envelope; AsyncAPI 3 generated from them; `platform.webhook_subscription` (url, events, secret_enc, active) and `platform.webhook_delivery` (attempts, status), HMAC-signed with retries. | ui D-20 (`integrations.webhooks`, not built) |
| ARC-5 | M | **Event ordering work** once D12 is decided: versioned payloads, stale-event drop, relay leader, LISTEN/NOTIFY. | D12 |
| ARC-6 | P2 | **Tenant export, erasure and archival** (India's DPDP Act): export/erase a tenant across all tables; per-table retention (`platform.retention_policy`), purge jobs, subject-access export `platform.ExportMyData`; archival by institution and academic year; partition retention defaults to keep forever. | E35, P2-11 (retention and erasure jobs); archival units in database/README.md |

### 3.2 Backend and API

| ID | Pri | What and why | Where / refs / depends on |
|---|---|---|---|
| BE-1 | - | **The 51 operations still answering 501.** comms 33 (send, compose, read marks, retry/cancel, test message, in-app notifications, post and comment reactions, feed, calendar, tasks); people 7 (consent records, birthdays, greetings); library 5 (return, lost, fine invoice, return request, reservation again); org 3 (`ActivateAcademicYear`, `FindInstitutions`, `GenerateTerms`); timetable 2 (`GenerateSessions`, `BatchConfirmSessions`); frontoffice 1 (`CheckOutVisitor`). | api/operations-status.md; comms mostly depends on ARC-2 |
| BE-2 | H | **Cut boilerplate.** A six-column table costs about 1,333 lines over 9 files; `lms/api/model` is 394 files (about 111k lines, 46% of the module); the ETag-mismatch string is copied across 195 files; `new FieldError(` about 2,000 times; 273 services repeat a 7-9 argument constructor. Extract step helpers into core (guarded update/delete with ETag/If-Match, unique check, scope check, field-error builders), never a generic base service; DTOs become Java records. Target < 600 lines per simple resource. | Improvement D3; CLAUDE.md "shared helpers" rule |
| BE-3 | M | **`AppTransactionManager` edge cases.** `commit()` runs in `finally`: if rollback fails a commit is attempted and its exception can mask the original; a sneakily thrown checked exception would commit. Fix and cover with unit tests. | Improvement D4; `backend/core/.../db/AppTransactionManager` |
| BE-4 | H | **Split god classes.** `LmsCourseService` 3,697 lines, `CohortService` 2,737, `ContentItemService` 2,056; split by workflow (authoring, publishing, enrolment). Target largest service < 800 lines. | `backend/lms` |
| P1-24 | M | **39 pure link tables are five-operation resources** (about 195 ops). Expose the set on the parent (`authorIds` via update mask) or a `:set...` method; drop the child CRUD, keep the tables. Fold in the drive-sharing duplicate (`POST .../shares` vs `:share`). | Do before SDK freeze |
| P1-25 | M | **Aggregate children are standalone resources** (question options/blanks/hotspots, rubric grid, invoice/payslip lines, fee-plan installments, release-rule lines). Make Question, Rubric, FeePlan, ReleaseRule and draft Invoice aggregates written whole. Revisit `FeeStructureUpdate` / `FeeConcessionUpdate` scope fields (draft-only today) here. | ui D-16 (`authoring.aggregates`; interim facade in `core`) |
| P1-26 | S | **Parity polish:** `read_mask` on Get/List (gRPC, REST `readMask`/`fields=`); parity check compares operations and shapes but not every parameter. Add ETag + `If-None-Match` 304 on reference data, gzip/brotli. | E15, P2-16 |
| P1-27 | M | **Permission model:** 1,671+ operation-level codes -> resource-level (`read`/`write`/`delete`/`manage` plus separate duties), permission as a proto method option. Includes report/search permission corrections. | Depends on D1 (review); ui D-18 |
| P1-28 | S | **Tenant self-service:** `GET`/`PATCH /v1/platform/tenant` for the current tenant (time zone, locale, currency); tenant UI defaults (default dashboards, quick actions per role, terminology per institution type). Also gives timetable and hr the institution time zone and current year (today whole UTC days; `GetStaffLeaveBalance` requires the year). | DEC-10; ui D-12 (`tenant.selfService`), D-25 (`tenant.uiDefaults`) |
| P1-29 | S | `StartCheckout` takes a short-lived `quoteId` from `QuoteCheckout` so the price cannot change between quote and payment. | finance |
| P1-32 | S | **PATCH cannot clear a field:** declare nullable types where NULL is legal; "in the mask and null => set NULL"; read-only fields in the mask -> 422. | DEC-9 |
| P1-33 | S | **Unbounded filter/sort surface:** declared filterable/sortable fields per resource (from indexes), simple AND grammar, `totalSize` opt-in. | |
| P1-35 | M | **Mobile essentials:** per-module `:changes?since=` feed (gRPC streaming twin) with soft and hard deletes (`platform.deletion_log`), `(tenant_id, updated_at)` index per synced table, optional `platform.sync_cursor`; idempotent `me/devices:register`; `renditions[]` on download URLs; public `GET /v1/platform/app-config`. | E8, E30; ui D-05 (`sync.changesFeed`), D-06 (`push.registerUpsert`, interim: 409 -> update), D-21 |
| P1-38 | M | **File, report and import permissions:** the owning module issues upload/download links for its files; report and import definitions name their own permission. | |
| P1-39 / E19 / E29 | M / P2 | **Launch integrations:** LTI 1.3 / Advantage (`lms.lti_registration`: issuer, client_id, deployment_id, auth URLs, jwks_url, keys; OIDC launch, deep linking, AGS score endpoint, platform key pair in KMS); Google/Microsoft SSO with PKCE and SAML (`iam.identity_provider`, `iam.external_identity`), optional mailbox unread count and calendar sync via Graph/Google APIs. | ui D-22 (`auth.sso`), LMS-49 |
| BE-5 | P1 | **Idempotency for platform-staff calls.** `IdempotencyFilter` keeps responses only for tenant callers (encrypted with the tenant key), so an operator's repeated create on a global table runs again; `platform.sales_enquiry` has no unique key. Keep tenantless responses under a platform key. | `backend/core/.../IdempotencyFilter` |
| BE-6 | M | **Leftover service rules from built features.** hr: weekends/holidays counted in leave (no calendar), leave cannot be shortened once begun, standard create lets a holder apply for another staff member, salary amounts on HR-02 have no currency, staff documents tab has no operation. payroll: leave/attendance ignored, percentage rates ("40% of basic") shown not calculated, a full-month run skips a staff member who moved school (`NO_SALARY`), payroll does not know when someone left hr (events carry no status), no payslip PDF, salary and components written separately. sis: >500 promotions only in batches, unit load not checked on approval, placements close on UTC date, no bonafide document. admission: enrolment creates no fee dues (finance not called), classes not narrowed to the application's academic year, uploads not built. homework: students are the class as placed now; a submission recorded by a teacher cannot be replied to; no answer text in the contract. RecordExamMarks refuses corrections once linked to a marksheet. Paid LMS course answers 409 `PAYMENT_REQUIRED` (no course checkout). | STATUS limits 12, 15, 18-21; P2-11 (holiday/working-day calendar, S7) |
| BE-7 | M | **Wire the platform file store into consumers.** Platform file operations are served; homework answers/attachments, bonafide certificate, payslip PDF, admission photo and documents, profile photo, question import by file and course-package import still do not use it end to end. Verify a storage provider is configured. | P1-38 |
| BE-8 | P2 | **Read models for summaries and dashboards.** `SummarizeAttendance` aggregates record partitions on every call (millions of rows per year for a large school). Daily per-student counters from the sheet-completed event. Remaining read models without a table (dashboards, gradebook). | E47 |
| BE-9 | P1/P2 | **LMS and learning proposals:** gradebook -> report card bridge `assessment.gradebook_mapping` + `ImportLmsGrades` (E18, P1); xAPI/cmi5 `lms.learning_statement` partitioned (E20); learning standards `academics.standard_framework`/`standard`, `lms.objective_standard`, `lms.question_objective` (E23); at-risk `lms.alert_rule` + `EvaluateAlertRules` (E25, P3); Open Badges 3.0 `credential_json` + `GetVerifiableCredential` (E26, P3); tablet offline packages `lms.offline_package` (E10, ui D-07); HLS renditions `platform.file_rendition` (E11, ui D-23); app release control `platform.app_release`, `platform.feature_flag` (E14); bulk teacher endpoints `:batchCreate`/`:batchUpdate` (E31, P2-21). | ENHANCEMENTS, P2-11 |
| BE-10 | L | **API polish backlog:** common.proto slimming (P2-12), lms.proto split (P2-13), typed operation results per kind (P2-14 remainder), gRPC idempotency levels/service config/streaming (P2-15), read-only views as sub-resources (P2-17, ui D-17), fire-and-forget analytics writes (P2-18), typed maps (P2-19), error-code registry (P2-20); one timestamp naming style `*Time` and `Recipients` gains guardians, staff and roles (P3-e); inbound webhooks REST-only (P3-d); state flags still writable through standard update (notice `isClosed`, forum topic `isHidden`, occurrence `isCancelled`) without a reason (limit 10). | |
| BE-11 | M | **Coupons and admission history:** per-redemption table, atomic usage limit, per-user limit, minimum amount (P2-10); admission interviews and document checklist (P3-c). | |

### 3.3 Data and database

| ID | Pri | What and why | Where / refs / depends on |
|---|---|---|---|
| P2-01 | S | **Index hygiene:** replace single-column `tenant_id` indexes with the composites list screens need (`EXPLAIN`); prune write amplification (`attendance_record` has about 9 indexes per partition, e.g. `ix_attendance_record_sheet_id` covered by composites) using `pg_stat_user_indexes` after a load test. | |
| C-1 | - | Aggregate version columns for cached roots: `lms.quiz.definition_version`, `lms.rubric.definition_version`, `lms.course.catalog_version`, bumped with any child write. | Needed by caching (PERF-2) |
| C-2...C-4 | - | Copied keys: `cohort_id` on `lms.forum_topic`/`forum_post` (J2); `class_section_id`, `subject_id` on `academics.teaching_assignment` with `UNIQUE (id, class_section_id, subject_id)` on `class_subject` (J3); `register_id` on `attendance.attendance_record` with `UNIQUE (id, register_id)` on `attendance_sheet` (J4). | ARC-1 |
| C-5 | - | Person cards copied to consumers: `display_name` + `photo_file_id` on `iam.user_account`; `display_name` + `sort_name` on `sis.student`, `sis.guardian`, `hr.staff`, fed by person-updated events, so lists sort and page in their own service. | Alternative or complement to P1-34 |
| C-7 / P2-04 | S | Effective-dated staff history `hr.staff_position` (designation, department, reporting_to, employment_type, effective dates, no overlap); exit/probation/contract dates on staff. | |
| P2-03 | M | Weekly timetable template (class x weekday x period -> subject, teacher, room, effective dates); clash detection as an application rule. | UI-2 timetable builder |
| P2-05 | S | Offline-created rows: client UUIDv7 ids, `client_device_id`, `client_created_at`, idempotent insert. | ui D-08 (`offline.clientIds`) |
| P2-07 | S | Institution type stored twice (varchar + FK): keep one. EAV only for custom fields. | |
| P2-11 | S-M | Full-text search (`tsvector` + GIN, or `pg_trgm`; OpenSearch from the outbox later) for people, courses, media, notices (E39, S16). | D7 |
| P3-a | S | `academics.subject.code` is `varchar(256)`; make it 30. | |
| P3-b | S | `password_changed_at` / password history if a tenant policy needs it. | |
| DB-1 | S | `uq_country_iso2` and `uq_currency_code` are full unique constraints, so a soft-deleted country or currency blocks its code forever. Make them partial. | `database/migrations/master/V1_010_001__master_baseline.sql` |
| DB-2 | S | RLS policies cast `current_setting('app.tenant_id', true)` directly: on a pooled connection reset to `''` it errors (fails closed) instead of returning no rows. Use `NULLIF(..., '')` (only 2 of 21 baselines do). | all baselines |
| DB-3 | L | 421 `DEFAULT gen_random_uuid()` fallbacks silently produce v4 ids if code omits the id. Remove the defaults. | baselines |
| DB-4 | M | Migration-upgrade test and baseline freeze plan. | D6 |
| E5 | P1 | Repeatable `R__<module>_reference_data.sql` for platform-defined rows; CI runs `flyway validate` and forbids editing applied migrations (after D6 freeze). | |
| E41 | P3 | Schema linting (`squawk`) on new migrations. | after D6 |

### 3.4 Security

| ID | Pri | What and why | Where / refs / depends on |
|---|---|---|---|
| SEC-11 | **P0 — next item built** | **Privacy and Consent Centre (approved).** New `privacy` module and schema, shared `privacy-client` module (approved exception like auth-client), Caffeine consent cache, retire `people.consent_record` and people's 501 consent operations into privacy. DPDP: verifiable parental consent for minors, re-consent at 18, access/export/correction/erasure requests, retention engine, breach register. Phases, schema, APIs, client and per-module enforcement points: the design doc. | [docs/design/PRIVACY-AND-CONSENT-CENTRE.md](docs/design/PRIVACY-AND-CONSENT-CENTRE.md); absorbs ARC-6 export/erasure; NM-1, NM-3, NM-5, NM-8, NM-10 depend on it |
| SEC-1 | H | **Owner credentials at runtime** (D8): run migrations and partition DDL outside the app; app holds only `mysms_app`. Fail startup outside dev when running as the migration role (today only a warning). | D8 |
| SEC-2 | M | **Tenant isolation soft spots:** the `TenantPredicates` scanner cannot see dynamically concatenated SQL; a missing tenant shows as 500. Add a wrong-tenant negative component test per module (target 21/21). | conformance |
| SEC-3 | P1 | **`mustResetPassword` not enforced:** a password change or temporary password sets the flag, but the server does not limit the session to the password change. | STATUS limit 7; iam |
| SEC-4 | P1 | **Key management for `*_enc`:** per-tenant data keys wrapped by a cloud KMS key; background re-encryption job for rotation. Encrypted values are bound to tenant and column, not row (a DB writer could move a ciphertext between rows; limit 8). | E32 |
| SEC-5 | P2 | **Rate limiting and abuse:** gateway rate limits per tenant/user/IP; bot protection on login and signup. Restarting sign-in replaces the open MFA step (annoyance; limit 9). | E36 |
| SEC-6 | S | **Audit log immutable by grant:** `mysms_app` INSERT + SELECT only on `platform.audit_log` and evidence tables; request id and actor tenant in the audit event; (tenant, time) index. | P2-09 |
| SEC-7 | M | **Client cache and tenant switching:** verify query keys include tenant and user, and the cache is cleared on sign-out and impersonation switch. | clients `core` |
| SEC-8 | M | mTLS / audience-scoped tokens before any multi-service deployment. | D11 |
| SEC-9 | - | **MFA by SMS and e-mail** answers 501. | ARC-2 |
| SEC-10 | L | **Accepted risks to re-check:** access changes reach a token only at refresh (15 min, limit 6); a crash between commit and keeping the Idempotency-Key response lets a retry run again after the 2-minute lock; kept responses are replayed for 24 h without re-checking access; replica reads can be stale (412 protects writes, limit 16); deep pages beyond 10,000 rows need `next` links, cursors expire after 24 h, no totals (limit 17). | |

### 3.5 Performance and scale

| ID | Pri | What and why | Where / refs / depends on |
|---|---|---|---|
| PERF-1 | P2 | **Measured performance budgets:** component tests with realistic volumes (2,000-student school, a year of attendance) asserting query counts and a time ceiling; `pg_stat_statements` everywhere; Micrometer timers per operation id with a p95 objective. Targets: list p95 < 300 ms at 1,000-row tenant data, <= 10 queries per request with no N+1. | E48 |
| PERF-2 | M | **Caching:** none today. Caffeine (approved dependency) for catalogues and permissions with event invalidation (in use: the permissions filter's role→permission cache in auth-client; next: privacy-client); ETag caching of reference data. | C-1, C-8, E40 |
| PERF-3 | M | **Cross-module call cost:** each call is its own BEGIN, `set_config`, COMMIT plus double JSON. | D1, DEC-1 |
| PERF-4 | P2 | **Reports off the primary:** route reports and dashboards to the replica (opt-in, D5); an event-fed reporting and search projection. | E40 |
| PERF-5 | M | Partition maintenance with `pg_partman`/`pg_cron`; a get-by-id on a partitioned table probes every month's index. | D7 |

### 3.6 Build, CI and testing

| ID | Pri | What and why | Where / refs / depends on |
|---|---|---|---|
| BLD-1 | H / P1 | **CI pipeline:** none exists (no `.github`, Dockerfile, compose, Helm or Terraform). Run the backend reactor with PostgreSQL and RabbitMQ services, `pnpm verify`, Flyway on a clean database plus `smoke.sql`, the migration-upgrade rehearsal, container images; publish per-module OpenAPI documents as artefacts. | E62; DEC-15 |
| BLD-2 | M / P2 | **Build time** (36 min, about 6 GB): Maven build cache, parallel reactor (`-T 1C`) with per-class databases, one Spring context per module in tests, reused Testcontainers, cached generator jar, turbo remote cache. Default turbo concurrency runs out of memory on 7 GB. Target clean CI < 15 min, cached < 5 min. | E49 |
| BLD-3 | M | **Contract artefact caching:** the OpenAPI document is produced only by a booted component test (no contract if tests are skipped; client generation coupled to Testcontainers); the client `generate` task has `cache:false` and starts a JVM every run. Cache the contract keyed by source; give turbo `generate` `inputs`/`outputs`. | Improvement D14 |
| BLD-4 | H | **Test depth:** lms has 18 unit and 24 component test classes for 240k lines; JaCoCo 0.80 excludes `api/**`, proto and MapStruct output. Add service unit tests; coverage gates including `api/**`; SDK-vs-OpenAPI contract tests. | |
| BLD-5 | P2 / H | **End-to-end tests:** none (no Playwright, Detox, Maestro, Storybook). Playwright smoke suite over the real monolith (sign in, take attendance, one CRUD flow, record a fee) with axe on every page; Maestro smoke for mobile; target 15 e2e flows (ui/05 gate). | E57 |
| BLD-6 | - | **Cross-module end-to-end runs:** sis steps calling iam, people, org and academics; admission enrolment against people and sis; question import and course-package import against real files are tested only to refusals and 503. | STATUS limits 18, 19, LMS limits |
| BLD-7 | P3 | **Client coverage gaps:** `hooks/security.ts` (password change, MFA, sessions) has no test of its own; raise `@mysms/core` threshold to 95 %. Native screens have no automated harness. | E60 |
| BLD-8 | L | **Dependencies:** versions pinned with no Renovate; `eslint-plugin-react-hooks` 5.x predates React 19 compiler rules; check Expo SDK 54 support window. | |

### 3.7 Operations and observability

| ID | Pri | What and why | Where / refs / depends on |
|---|---|---|---|
| OPS-1 | H | **Observability:** actuator exposes only `health,info`; no Micrometer registry or OpenTelemetry. Add Prometheus and OTel; gauges for Hikari pool waits, gRPC latency, outbox depth and oldest-unpublished age; `pg_stat_statements`; Sentry (or equivalent) on clients. | |
| OPS-2 | H | **Backup and DR:** only a daily `pg_dump` (RPO up to 24 h); with FORCE RLS a dump by the owner role fails (needs BYPASSRLS or superuser). Managed point-in-time recovery, document the dump role, restore drill, state RPO/RTO (target <= 5 min / <= 1 h). | DEPLOYMENT.md |
| OPS-3 | H | **Connection budget** implementation and a correct `max_connections` formula; worker mode for relays and jobs; PgBouncer. | D9 |
| OPS-4 | M | Per-service readiness gate (see D1) before operating any module as its own service. | D1 |

### 3.8 Clients and UI

| ID | Pri | What and why | Where / refs / depends on |
|---|---|---|---|
| UI-1 | - | **Remaining 93 inventory screens:** dashboards, grids (timetable builder week view, gradebook, marks entry), detail pages with tabs, "my ..." views for students, guardians and teachers, the phone and tablet surfaces, the tenant-scoped parts of PLT-02 (subscriptions) and PLT-04 (notification channels), course-comparison screen (operation served). Lists show only the record type's own fields until P1-34. | ui/02; P1-34 |
| E59 | P2 | **Teacher app first:** teacher Today tab and ATT-03 on the phone with the offline queue of ui/05 §7.4 (flow F3 now starts from web TT-02). Prototype the attendance outbox before building more screens. | ui D-05, D-08 |
| UI-2 | P2 | **Offline:** no NetInfo, persisted query cache, SQLite store or outbox in code; tablet has no offline cache of lessons, SCORM opens in the browser, e-book pages not rendered; nothing kept for offline reading of timetables. | P1-35, P2-05, E9, E10 |
| E53 | P1 | **Route-level error boundary** so one failing screen does not blank the app; prefetch on navigation hover. | `clients/apps/web/src/routes.ts` |
| E54 | P1 | **Break up the list manager:** `RecordsScreen.tsx` (658 lines) does list, filters, form, validation and workflow dialogs for 327 record types; split into list, filter bar, record form and transition dialog with their own tests, keep `RecordsOf` as composition. | `clients/packages/web-kit/src/screens/RecordsScreen.tsx` |
| E55 | P2 | **Screen state in the address** everywhere (filters, search text, month, institution) through one hook; done for list filters only. | |
| E56 | P2 | **Translations complete or absent:** the Hindi catalogue is far smaller than English; ship a locale only at 100 % (test per locale) and add a pseudo-locale run. Server error sentences are English until DEC-8. | `clients/packages/i18n/src/catalogs` |
| E58 | P2 | **Large lists:** windowed rendering in `DataTable` above a few hundred rows; server-side sort for summaries; 1,000-row roster benchmark (`memo`/`useMemo` in only 5 files). | |
| UI-3 | M | **Bundle budgets** per lazy chunk and for React Native (today only the initial web route has one). Review suggestions not yet taken: split the large Vite entry chunk; split the native assessment screens into smaller components. | |
| UI-4 | M | **Shared prop-contract test** for `ui-web` and `ui-native` so the two kits do not drift. | |
| UI-5 | - | **Small UX gaps:** phone/tablet dates typed as year-month-day (no picker), leave decisions carry no comment on native; names fall back to roll numbers without `sis.student.read`/`people.person.read`; ATT-05 names classes by grade and section only (same across years); enrolment wizard needs class-section read permissions; a parent sees a child's name only when the role may read persons; template components cannot be reordered. | STATUS limits 11, 15, 18, 19, 21 |
| ui D-04 | - | Institution switcher client-side until D2 (review). | flag `ctx.institutionHeader` |

Flag process for ui D-items: when the backend item ships, regenerate the SDK, flip the flag in
`packages/core/flags.ts`, delete the interim adapter, then delete the row here.

### 3.9 Docs

| ID | Pri | What and why | Where / refs / depends on |
|---|---|---|---|
| E61 / DOC-1 | P1 / M | **Generated counts:** have `OperationStatusReportTest` write totals and per-area counts; generate table, foreign-key, reference and RLS counts in the conformance build (the database README counts disagree with each other). Docs link instead of copying. | |
| DOC-2 | M | Fix doc drift: the Flyway sample in database/README.md uses `default-schema: public`; docs claim CI runs `smoke.sql` but there is no CI; ui/05 claims per-app budgets and Lighthouse CI that do not exist; label offline behaviour as planned. | |
| DOC-3 | - | Decide whether this roadmap moves to an issue tracker (Markdown backlogs rot). | DEC-15 |
| DOC-4 | L | An earlier enhancement review listed A1-A16 and S1, S2, S4-S6, S9, S13, S14, S16, S17 as still open; their text is no longer in the repository (S7, S10, S16 are folded in above). Recover them from git history and add them here, or drop them. | git history of ENHANCEMENTS.md |

### 3.10 New modules and feature gaps

Twelve capabilities an Indian school or college expects that MySMS does not have, or has only as a stub. They were
chosen by walking the daily life of each persona in [ui/01-product-map.md](ui/01-product-map.md) (nurse, warden,
driver, gate guard and canteen staff are missing personas) and comparing it with the 21 baselines in
`database/migrations/` and the screen inventory in [ui/02-navigation-and-screens.md](ui/02-navigation-and-screens.md).
Excluded because product documents already plan them: the school community network
([docs/product/SCHOOL-COMMUNITY-NETWORK-BRAINSTORM.md](docs/product/SCHOOL-COMMUNITY-NETWORK-BRAINSTORM.md)), alumni and
job board and learning portfolios ([docs/product/LEARNING-AND-ALUMNI-BRAINSTORM.md](docs/product/LEARNING-AND-ALUMNI-BRAINSTORM.md)),
the classroom voice assistant ([docs/product/CLASSROOM-VOICE-ASSISTANT-BRAINSTORM.md](docs/product/CLASSROOM-VOICE-ASSISTANT-BRAINSTORM.md))
and device integration ([docs/product/HARDWARE-INTEGRATION-REQUIREMENTS-DRAFT.md](docs/product/HARDWARE-INTEGRATION-REQUIREMENTS-DRAFT.md));
items below link to those where they touch (GPS units, gate readers, POS terminals).

Conventions used in the entries: operations, permissions and events follow the existing style
(`POST /v1/<module>/<resources>/{id}:verb`, operation id `<module>.VerbNoun`, permission
`<module>.<resource>.<action>`, event `<module>.<resource>.<past_tense>`). Screen ids are proposals to add to ui/02
when an item is scheduled; clients are W (web), T (teacher phone), S (student phone), P (parent phone), B (tablet).
Module placement refers to the 9-module Plan A in [ARCHITECTURE.md §6.2](ARCHITECTURE.md#62-recommended-21--9-modules-plan-a);
a "new module" means a new schema and, until Plan A is decided, a new Maven module. Regulatory statements marked
**to verify** must be checked against the current text before build. Every item handling children's data depends on
the planned Privacy and Consent Centre design, [docs/design/PRIVACY-AND-CONSENT-CENTRE.md](docs/design/PRIVACY-AND-CONSENT-CENTRE.md)
and on the `people` consent operations that still answer 501 (BE-1).

| ID | Pri | What and why | Where / refs / depends on |
|---|---|---|---|
| NM-1 | P1 / M | **Health and infirmary:** medical profile, allergy and condition alerts shown to teachers, vaccinations, nurse visits, medication consent and log, annual check-ups. `sis.student_health_record` holds only free-text fields. | New `health` module (students group); ARC-3, ARC-2, BE-1 consent ops |
| NM-2 | P3 / L | **Hostel and boarding:** blocks, rooms, beds, allotment, mess, night roll-call, out-pass, warden duty. Nothing exists. | New `hostel` module (campus group); NM-1, NM-6, NM-3 |
| NM-3 | P3 / L | **Canteen and meals:** menus with allergens, prepaid cashless wallet, meal counts and PM POSHAN reporting. Nothing exists; finance has no wallet. | New `canteen` module (campus) + `finance` wallet; NM-1, ARC-2 |
| NM-4 | P3 / L | **Inventory and procurement:** stores, vendors, purchase orders, goods receipt, stock of uniforms, books and stationery, sales to parents. Only `facility.asset_item` and `library.purchase_request` exist. | New `inventory` module (campus); reopens D5 (review) |
| NM-5 | P2 / L | **Transport tracking and safety:** vehicles, drivers, trips, live GPS, ETA to parents, boarding scan, compliance documents. `transport` has routes, stops and student assignment only. | Extend `transport` (campus); D7/P2-11, hardware draft §3.7 |
| NM-6 | P1 / M | **Gate and pickup safety:** authorised pickup persons with photo and OTP, gate pass, late arrival and early leave. Only `frontoffice.visitor_log` and the `sis.student_guardian.can_pick_up` flag exist. | Extend `frontoffice` (campus); ARC-2, NM-1 |
| NM-7 | P2 / M | **Substitution and cover:** find a free, qualified teacher for a lesson whose teacher is absent, balance cover load, notify the class. `timetable` has no substitution. | Extend `timetable` (academics group); BE-1 (`GenerateSessions`), DEC-3 |
| NM-8 | P3 / M | **Clubs, sports and houses:** clubs and memberships, houses and points, competitions and results, achievements feeding the portfolio. Only `sis.student_activity` exists. | New `activities` module (students group); portfolio proposal TD-037 |
| NM-9 | P1 / M | **Certificates and documents:** transfer, bonafide, character, conduct and other certificates from templates, numbered, verifiable, pushed to DigiLocker. Only `sis.IssueBonafideCertificate` exists. | Extend `sis` (students group); ARC-4 |
| NM-10 | P3 / M | **Scholarships and financial aid:** schemes, eligibility, applications, National Scholarship Portal tracking, awards and disbursement. Only `finance.fee_concession` exists. | Extend `finance`; NM-9 (bonafide for NSP) |
| NM-11 | P2 / S | **Parent-teacher meeting booking:** slots per teacher, sibling-aware booking, video link, reminders. Only generic `comms.event` exists. | Extend `comms`; ARC-2, ARC-3 |
| NM-12 | P3 / M | **Staff appraisal and CPD:** appraisal cycles, goals, classroom observations, CPD hours against the CBSE 50-hour expectation. `hr` has staff and leave only. | Extend `hr` (workforce group); DEC-3 |

#### NM-1 Health and infirmary

- **Problem and users.** A nurse (persona missing from ui/01) records visits and gives medication; teachers and
  coaches must see "severe peanut allergy, carries EpiPen" before a trip or lab, not dig through a profile; parents
  grant medication consent and update vaccinations; the principal needs incident and check-up reports.
- **Exists today.** `sis.student_health_record` (one row per student and academic year: `height_cm`, `weight_kg`,
  `medical_conditions`, `medication_needs`, `medication_allergies`, `food_allergies`, `other_allergies`,
  `other_medical_info`, all free text) with CRUD in `StudentHealthRecordApi` (`sis.student_health_record.*`, served)
  and screen SIS-05 (W, admin and nurse, "sensitive-permission gated, reads audited"). `people.emergency_contact`
  and `sis.student_guardian` give whom to call. `people.consent_record` exists but its `purpose` check has no
  health purpose and its operations answer 501.
- **Gaps in our modules.** sis: no structured allergy or condition list, no severity, no alert flag, no history
  beyond one row a year, no vaccinations, no visits, no medication administration log. attendance and timetable:
  nothing shows a teacher which students in the session have alerts. comms: no "sent home sick" notice type.
  people: no consent purpose for medication or emergency treatment. Staff health is out of scope.
- **Proposed scope.** Tables (schema `health`): `medical_profile` (student_id, blood_group, primary_physician,
  insurance_ref_enc, review_due_on), `medical_condition` (profile_id, kind allergy/chronic/disability, code or name,
  severity mild/moderate/severe/life_threatening, action_plan, alert_level none/staff_need_to_know/all_teachers,
  valid_until), `immunisation` (student_id, vaccine, dose, given_on, evidence_file_id, source parent/school_camp),
  `nurse_visit` (student_id, visited_at, complaint, vitals_json, treatment, outcome returned_to_class/sent_home/
  referred/emergency, guardian_notified_at, nurse_user_id), `medication_order` (student_id, drug, dose, schedule,
  from/to dates, consent_record_id, prescribed_by_text, status), `medication_administration` (order_id, given_at,
  given_by_user_id, dose_given, refused, note), `health_checkup` (campaign_id, student_id, results_json, referral),
  `checkup_campaign` (institution_id, academic_year_id, kind annual/dental/vision, dates). Move the free-text fields
  of `sis.student_health_record` into `medical_profile`/`medical_condition` (baseline edit while D6 allows it).
  Operations: `health.*` CRUD on `medical-profiles`, `medical-conditions`, `immunisations`, `nurse-visits`,
  `medication-orders`; custom `POST /v1/health/nurse-visits/{id}:sendHome`, `:notifyGuardian`,
  `POST /v1/health/medication-orders/{id}:approve`, `:recordDose`, `:stop`; `GET /v1/health/alerts?classSectionId=&sessionId=`
  (`health.ListStudentAlerts`, returns only alert-level text, never the full record). States: medication order
  requested -> approved (consent present) -> active -> completed or stopped; nurse visit open -> closed.
  Events: `health.nurse_visit.closed` (comms notifies guardians; attendance may mark early leave via NM-6),
  `health.medical_condition.changed` (cache refresh of alert badges); consumes `sis.student.deleted`.
  Permissions: `health.medical_profile.read` (nurse, admin), `health.alert.read` (teachers; alert text only),
  `health.nurse_visit.create`, `health.medication_order.approve`, `health.medication_administration.create`;
  guardians and adult students get own-data reads (ARC-3). Screens: HLT-01 alert badges inside ATT-03, TT-03 and
  SIS-02 (W T B); HLT-02 infirmary desk with visit log (W B); HLT-03 medication round (B T); HLT-04 child health
  and consent tab in parent app (P); HLT-05 check-up campaign and reports (W).
- **Owner module.** New module `health`, not more columns in sis: the data has its own readers (nurse), stricter
  access and audit, and a separate retention rule. Under Plan A it becomes a fourth schema of **students**
  (§6.3 keeps schemas, so the boundary and RLS stay).
- **Integrations and regulation.** School Health and Wellness programme under Ayushman Bharat and RBSK screening
  formats (to verify whether any reporting format is required of private schools). Medication by non-medical staff
  and storage rules vary by state (to verify). POCSO Act 2012 places a duty to report suspected sexual offences
  against children; a nurse-visit note may be the first record, so a "safeguarding concern" outcome should route to
  a restricted workflow (design to verify with counsel).
- **Privacy.** Health data of minors. India's DPDP Act 2023 has no separate "sensitive" class, but processing a
  child's data needs verifiable parental consent; treat health as the highest tier in the Privacy and Consent Centre
  design (purpose `health_care` and `medication_administration` added to `ck_consent_record_purpose`), field-level
  encryption for notes, every read audited (`platform.audit_log`), alerts expose the minimum text.
- **Depends on.** BE-1 (people consent ops), ARC-2 (guardian notices), ARC-3 (own-data reads), P1-34 (badges on
  rosters). **Priority P1 / M** for alerts, conditions and visits; medication and campaigns can follow.
- **Open questions.** Who may see alerts (all teachers of the section, or need-to-know list)? Do we store diagnoses
  or only school-relevant needs? Staff and visitor first aid in scope? Retention after the student leaves?

#### NM-2 Hostel and boarding

- **Problem and users.** Boarding schools and college hostels: the warden allots beds, takes night roll-call, issues
  out-passes; the mess manager plans meals; parents see that their child is in and approve weekend leave; the
  accountant bills hostel and mess fees.
- **Exists today.** Nothing hostel-specific. `facility.facility`, `facility.room` and `facility.room_asset` model
  teaching rooms; `finance.fee_head` can carry a hostel fee; `sis.student_leave_request` covers day leave from
  class; `attendance.attendance_register` has class and subject registers only.
- **Gaps in our modules.** facility: no beds or occupancy. sis: no boarder flag or residence. attendance: no
  night or meal roll-call kind. finance: no automatic hostel fee on allotment. frontoffice: no out-pass gate check.
  hr: no warden duty roster.
- **Proposed scope.** Tables (schema `hostel`): `hostel` (institution_id, name, gender_policy, warden_staff_id),
  `block`, `room` (block_id, floor, room_type, capacity), `bed` (room_id, code, status available/occupied/blocked),
  `allotment` (student_id, bed_id, academic_year_id, from_date, to_date, status), `roll_call` (hostel_id, kind
  night/morning/meal, taken_at, taken_by_user_id) with `roll_call_entry` (student_id, status present/absent/on_leave),
  `out_pass` (student_id, kind day/overnight/weekend/home, out_at, due_back_at, returned_at, guardian_approval_at,
  approved_by_user_id, status), `warden_duty` (staff_id, hostel_id, from_at, to_at), `mess_plan` and `mess_menu`
  (shared with NM-3 if built). Operations: CRUD plus `POST /v1/hostel/allotments/{id}:vacate`, `:transfer`;
  `POST /v1/hostel/roll-calls/{id}:complete`; `POST /v1/hostel/out-passes/{id}:approve`, `:reject`,
  `:checkOut`, `:checkIn`. States: out-pass requested -> guardian_approved -> warden_approved -> out -> returned
  (late flag) or cancelled. Events: `hostel.allotment.created` (finance raises hostel fee due),
  `hostel.roll_call.completed` (comms alerts warden and guardians of absentees), `hostel.out_pass.overdue`.
  Permissions: `hostel.allotment.*`, `hostel.roll_call.create`, `hostel.out_pass.approve`, guardian
  `hostel.out_pass.approve_as_guardian` on own child. Screens: HST-01 hostel and bed map (W), HST-02 allotment
  (W), HST-03 roll-call (B T, offline like ATT-03), HST-04 out-pass desk (W B), HST-05 out-pass request and approve (S P).
- **Owner module.** New module `hostel`; a leaf like the other **campus** members, so it joins campus at no cost.
- **Integrations and regulation.** Gate readers per the hardware draft §3.2 and §3.4 for check-in and out.
  Hostel safety guidance for children (NCPCR, state rules, CBSE affiliation conditions for residential schools)
  to verify. UDISE+ asks about hostel facilities (to verify fields).
- **Privacy.** Night location of minors; out-pass history is movement data. Guardian-only visibility, retention
  per academic year.
- **Depends on.** NM-6 (gate checks), NM-1 (sick bay), NM-3 (mess), UI-2 (offline roll-call), ARC-2.
  **Priority P3 / L.**
- **Open questions.** How many target tenants are residential? College hostels with adult students: guardian
  approval optional? Mess billing per meal or flat?

#### NM-3 Canteen and meals

- **Problem and users.** Canteen staff serve meals and snacks; parents top up a prepaid balance, set daily limits
  and blocked items, and see what the child bought; the school publishes menus with allergens; government and
  aided schools report daily meal counts under PM POSHAN.
- **Exists today.** Nothing. `finance.payment` (purposes `invoice`, `fees`, `lms_course`, `lms_cohort`,
  `lms_certificate`, `admission`), gateway accounts and refunds can collect money but have no stored balance;
  `sis.student_health_record.food_allergies` is free text; `comms.notice` can publish a menu as a notice.
- **Gaps in our modules.** finance: no wallet, no top-up purpose, no closed-loop ledger, no low-balance reminder.
  sis/health: allergies not machine-readable, so a till cannot warn. attendance: daily counts exist
  (`attendance.attendance_record`) but are not exported as meal beneficiaries.
- **Proposed scope.** Tables (schema `canteen`): `outlet` (institution_id, name, operator vendor or school),
  `menu_item` (outlet_id, name, price, allergens text[], veg flag, hfss flag), `menu` and `menu_day` (date, meal slot,
  items), `meal_plan_subscription` (student_id, plan, from/to), `sale` and `sale_line` (outlet_id, student_id or
  staff_id, items, amount, wallet_txn_id, cashier_user_id), `spend_rule` (student_id, daily_limit, blocked_item_ids),
  `meal_count` (institution_id, date, class or grade, enrolled, served, source). In finance: `finance.wallet`
  (holder person_id, institution_id, balance, status) and `finance.wallet_transaction` (wallet_id, kind
  top_up/purchase/refund/adjustment, amount, payment_id, reference), payment purpose `wallet_top_up`.
  Operations: `POST /v1/finance/wallets/{id}:topUp` (gateway, reuses `QuoteCheckout` flow of FIN-07),
  `:debit` (service-to-service from canteen only), `:freeze`; `POST /v1/canteen/sales` (checks spend rule and
  allergens through NM-1 alerts), `POST /v1/canteen/meal-counts:generate` (from attendance),
  `GET /v1/canteen/meal-counts:export`. Events: `finance.wallet_transaction.created` (comms low-balance notice),
  `canteen.sale.created`. Permissions: `canteen.sale.create`, `canteen.menu.update`, `finance.wallet.top_up` (own
  child), `finance.wallet.adjust` (accountant). Screens: CAN-01 menu planner (W), CAN-02 till (B, offline queue),
  CAN-03 wallet, limits and purchase history (P S), CAN-04 meal counts and PM POSHAN export (W).
- **Owner module.** Menus, sales and counts in a new `canteen` module (**campus**); balances in **finance**
  (rule 3 of §6.1: money stays in finance).
- **Integrations and regulation.** Payment gateway already in finance. RBI rules on prepaid payment instruments:
  whether a school-run closed-loop wallet, or one where a third-party canteen operator is paid from it, needs
  authorisation (to verify, this decides the design). FSSAI regulations on food for school children (2020)
  restrict sale of foods high in fat, sugar and salt in and near schools (to verify scope and limits).
  PM POSHAN applies to government and government-aided schools (Balvatika to class 8) with daily reporting
  through the scheme's monitoring system (format and any API to verify). POS hardware per hardware draft §3.9.
- **Privacy.** Purchase history of minors is behavioural data; no profiling or marketing use; parents see
  their own child only.
- **Depends on.** NM-1 (structured allergies), ARC-2, finance gateway; **Priority P3 / L** (PM POSHAN export alone
  could be S if a government tenant needs it earlier).
- **Open questions.** Is the target market private schools (wallet) or government schools (PM POSHAN), or both?
  Who operates the canteen and who holds the money? Cash at the till allowed?

#### NM-4 Inventory and procurement

- **Problem and users.** The store keeper issues stationery and lab consumables, sells uniforms and books to
  parents at the start of term; the admin raises purchase orders to vendors and receives goods; the accountant
  matches vendor bills.
- **Exists today.** `facility.asset_item` (code, name, unit_of_measure) and `facility.room_asset` (room_id,
  asset_item_id, tag_code, quantity) record what sits in a room; `library.purchase_request` (+ `_course`, `_subject`)
  lets people ask for a book. Decision **D5 (review)** in §2.2 set "Inventory/store module: not in v1".
- **Gaps in our modules.** facility: no stock levels, movements, stores, vendors, purchase orders or receipts;
  assets have no purchase value, warranty or depreciation. library: a purchase request ends at "approved" with
  no order. finance: no payable side (vendor bills), no sale of goods with GST lines (invoices are fee-oriented).
- **Proposed scope.** Tables (schema `inventory`): `store` (institution_id, name, keeper_staff_id), `item`
  (sku, name, category uniform/book/stationery/lab/consumable, uom, size and variant, hsn_code, gst_rate, reorder_level,
  sale_price), `stock_level` (store_id, item_id, on_hand, reserved), `stock_movement` (kind receipt/issue/sale/
  transfer/adjustment/return, qty, ref), `vendor` (name, gstin, contact, bank_ref_enc), `purchase_order` and
  `purchase_order_line` (vendor_id, status, expected_on), `goods_receipt` and `goods_receipt_line`, `issue_slip`
  (to staff, department or student), `sale_order` (guardian or student, lines, finance invoice_id), `item_kit`
  (for example "class 6 book set"). Operations: CRUD plus `POST /v1/inventory/purchase-orders/{id}:submit`,
  `:approve`, `:cancel`; `POST /v1/inventory/goods-receipts/{id}:post`; `POST /v1/inventory/sale-orders/{id}:confirm`
  (creates a finance invoice), `:fulfil`; `POST /v1/inventory/stock-movements:adjust`. States: PO draft ->
  submitted -> approved -> partially_received -> received or cancelled; sale order draft -> confirmed -> paid ->
  fulfilled. Events: `inventory.sale_order.confirmed` (finance invoice), `finance.payment.received` consumed
  (mark paid), `inventory.stock_level.below_reorder` (task to keeper via comms tasks), `library.purchase_request.approved`
  consumed (draft PO line). Permissions: `inventory.purchase_order.approve`, `inventory.goods_receipt.post`,
  `inventory.sale_order.create` (guardian own, office), `inventory.stock_movement.adjust`. Screens: INV-01 items
  and kits (W), INV-02 stock by store (W), INV-03 purchase orders and receipts (W), INV-04 counter sale (W B),
  INV-05 uniform and book shop with size picker (P), INV-06 vendor list (W).
- **Owner module.** New module `inventory` in **campus**; `facility.asset_item` becomes either the fixed-asset view
  of an inventory item or stays as room equipment (open question). Vendor bills stay in finance.
- **Integrations and regulation.** GST invoicing for goods sold (HSN codes, rates for uniforms and books to verify);
  e-invoicing thresholds unlikely for schools (to verify). Barcode scanners per hardware draft §3.3 and §3.8.
  Some state rules and court orders forbid forcing parents to buy uniforms or books from the school (to verify per
  state), so the parent shop must stay optional.
- **Privacy.** Low; vendor bank details encrypted.
- **Depends on.** Owner reversing D5 (review); finance invoice lines for goods; **Priority P3 / L.**
- **Open questions.** Is fixed-asset accounting (depreciation) needed? Merge `facility.asset_item` into
  `inventory.item`? Multi-store per institution or one central store per tenant?

#### NM-5 Transport tracking and safety

- **Problem and users.** Parents want to know where the bus is and when it reaches their stop; the transport
  coordinator wants to know that every child who boarded got off at the right place; the driver and attendant need
  a simple phone app; the principal must keep vehicle and driver documents valid.
- **Exists today.** `transport.route` (name, `vehicle_number` text, start and end time), `transport.route_stop`
  (sequence, stop name, pickup and drop time), `transport.student_transport` (student, route, stop, mode,
  `bus_pass_issued`); `RouteApi`, `StudentTransportApi`; screens TRN-01, TRN-02, TRN-03 (S P "my route"). The
  hardware draft §3.7 inventories GPS units and boarding readers.
- **Gaps in our modules.** transport: no vehicle (only a text number), no driver or attendant, no trip, no
  location, no stop coordinates, no boarding or alighting record, no compliance documents, no capacity check.
  hr: drivers are staff but have no licence fields. comms: no ETA or "bus delayed" notice. attendance: no link
  between boarding and arrival.
- **Proposed scope.** Tables in `transport`: `vehicle` (registration_no, capacity, gps_device_id, owner
  school/contractor, status), `vehicle_document` (kind fitness/permit/insurance/puc/speed_governor, number,
  valid_until, file_id), `crew_assignment` (route_id, vehicle_id, driver_staff_id, attendant_staff_id, from/to),
  `driver_credential` (staff_id, licence_no_enc, licence_class, valid_until, police_verification_on, medical_on),
  stop `latitude`/`longitude`/`geofence_m` on `route_stop`, `trip` (route_id, vehicle_id, direction pickup/drop,
  service_date, started_at, ended_at, status), `trip_stop_event` (trip_id, stop_id, arrived_at, departed_at),
  `location_ping` (vehicle_id, recorded_at, lat, lng, speed, source) partitioned by day with short retention,
  `boarding_event` (trip_id, student_id, kind boarded/alighted/absent, at, stop_id, method qr/rfid/manual).
  Operations: CRUD; `POST /v1/transport/trips/{id}:start`, `:end`; `POST /v1/transport/trips/{id}/boarding-events:batchCreate`;
  `POST /v1/transport/location-pings:batchCreate` (device or driver app, service token); `GET /v1/transport/trips/{id}:eta?stopId=`;
  `GET /v1/transport/my/trip` (parent, own child's live trip only, while the trip is running). States: trip planned
  -> running -> completed or aborted. Events: `transport.trip.started`, `transport.trip_stop_event.approaching`
  (comms push "bus 5 minutes away"), `transport.boarding_event.created`, `transport.trip.completed` with an
  "unaccounted children" check (boarded but not alighted) raising an alert, `transport.vehicle_document.expiring`.
  Permissions: `transport.trip.operate` (driver app), `transport.boarding_event.create` (attendant),
  `transport.location.read_own_child` (guardian), `transport.vehicle.*`. Screens: TRN-04 vehicles and documents (W),
  TRN-05 live fleet map (W), TRN-06 driver and attendant trip app (T, a new crew persona, offline queue),
  TRN-07 live bus and ETA (P S), TRN-08 trip exceptions report (W).
- **Owner module.** Extend `transport` (**campus**). Location pings are high-volume, so they need their own
  partitioned table and may later move to a time-series store; GPS ingestion follows the hardware draft (vendor
  webhook, gateway or file import).
- **Integrations and regulation.** GPS vendors' cloud APIs, AIS-140 vehicle tracking devices (whether school buses
  must carry them depends on state notifications, to verify); map and ETA provider (cost). Supreme Court and CBSE
  school-bus safety guidelines (GPS, CCTV, speed governor, female attendant, driver verification) to verify current
  text. FCM/APNs push via ARC-2.
- **Privacy.** Live location of children and staff. Show location only during the trip, only to guardians of
  children on that trip; drop pings after a short window (for example 30 days, owner to set); DPDP limits on
  tracking or behavioural monitoring of children and any exemption for school safety (to verify against DPDP
  Rules); see the Privacy and Consent Centre design.
- **Depends on.** ARC-2 (push), D7 and P2-11 (partitions, retention jobs), UI-2 (offline crew app), NM-6
  (handover at the gate). **Priority P2 / L** (vehicle, crew and document compliance can ship first as S).
- **Open questions.** Which GPS vendors do pilot schools use? Phone-as-tracker acceptable? Contractor-run buses:
  who is the data processor? Parent ETA free or a paid add-on?

#### NM-6 Gate and pickup safety

- **Problem and users.** A guard at the gate must know whether the adult collecting a child is allowed to; a
  class teacher must know who left early and why; parents want to nominate a driver or grandparent for one day; the
  office records late arrivals with a reason.
- **Exists today.** `frontoffice.visitor_log` (visitor, `student_id`, `relation_to_student`, purpose, host,
  entry and exit time) with `VisitorLogApi` and FO-01; `frontoffice.CheckOutVisitor` answers 501.
  `sis.student_guardian.can_pick_up` (boolean per guardian link, plus `has_custody`, `is_legal_guardian`);
  `sis.student_leave_request` (leave by date and day part); `attendance.attendance_record` with absence reasons.
- **Gaps in our modules.** sis: pickup permission is only a flag on guardians; no non-guardian authorised persons,
  no photo, no validity window, no court-order restriction ("must not collect"). frontoffice: no gate pass, no
  pickup event, no late-arrival register, visitor checkout unbuilt. attendance: no "left early at 11:20" mark.
  comms: no "your child was collected by X" notice.
- **Proposed scope.** Tables (frontoffice): `authorised_pickup` (student_id, person_id or name, relation, photo_file_id,
  phone, valid_from, valid_until, kind standing/one_time, created_by guardian or office, status), `pickup_restriction`
  (student_id, person, reason court_order/guardian_request, evidence_file_id, valid_until), `gate_pass`
  (student_id, reason early_leave/medical/event, requested_by, approved_by_user_id, out_at, collected_by_pickup_id,
  otp_hash, otp_expires_at, status), `pickup_event` (student_id, at, collected_by, verified_by method photo/otp/qr,
  guard_user_id), `late_arrival` (student_id, arrived_at, reason, recorded_by). Operations:
  `POST /v1/frontoffice/authorised-pickups` (guardian own child, office); `POST /v1/frontoffice/gate-passes/{id}:approve`,
  `:issueOtp`, `:verify` (guard enters OTP or scans QR, sees photo), `:cancel`;
  `POST /v1/frontoffice/late-arrivals`; `GET /v1/frontoffice/students/{studentId}:pickupCard` (guard view: photo,
  allowed persons, restrictions, today's pass). States: gate pass requested -> approved -> collected or expired.
  Events: `frontoffice.gate_pass.collected` (attendance marks early leave; comms notifies all custodial guardians),
  `frontoffice.late_arrival.created` (attendance updates the record), `frontoffice.pickup_verification.failed`
  (alert to office). Permissions: `frontoffice.gate_pass.approve`, `frontoffice.gate_pass.verify` (guard),
  `frontoffice.authorised_pickup.create` (guardian own child), `frontoffice.pickup_restriction.*` (admin only).
  Screens: GATE-01 guard app with search, QR and OTP verify (T or B, offline list of today's passes), GATE-02 gate
  pass desk (W), GATE-03 authorised pickups and one-day nominee (P), GATE-04 late and early register (W T).
- **Owner module.** Extend `frontoffice` (**campus**); sis keeps guardian links and `can_pick_up` as the source for
  standing permission, read through a lookup class.
- **Integrations and regulation.** OTP by SMS through ARC-2; QR scanners and gate readers per hardware draft
  §3.2-§3.5. Custody orders under family law are honoured as data entered by the office; the system must not infer
  custody. POCSO and child-safety guidance for schools (CBSE safety circulars) to verify for any mandated
  procedure.
- **Privacy.** Photos of non-guardian adults are their personal data, so collect with their consent; restrictions
  are highly sensitive and visible only to admins and the guard view.
- **Depends on.** ARC-2 (OTP, notices), ARC-3 (guardian own-child operations), BE-1 (`CheckOutVisitor`), NM-1
  (sent-home visits create gate passes). **Priority P1 / M.**
- **Open questions.** Is OTP mandatory or school-configurable? Should the guard app work fully offline? Do
  college tenants need this at all (adult students)?

#### NM-7 Substitution and cover

- **Problem and users.** Every morning the timetable in-charge must cover lessons of absent teachers: find who is
  free that period, qualified for the subject or at least for the class, not already over their cover quota, then
  tell the substitute and the class. Today this is a WhatsApp scramble.
- **Exists today.** `timetable.session` (period, class section, subject, `staff_id`, room, status draft/confirmed/
  done/cancelled) and `timetable.period`; `hr.leave_application` with `class_section_id` commented "Class whose cover
  is affected" and served `hr.ApproveLeave`; `attendance.staff_attendance` (status, leave link); `hr.staff_subject`
  and `hr.staff.max_periods_per_week`; `academics.teaching_assignment` (periods per week, remaining). Attendance
  already listens to one hr event and one timetable event. `timetable.GenerateSessions` and
  `timetable.BatchConfirmSessions` still answer 501. Screens TT-02 builder, TT-03 my timetable, TT-04 staff view.
- **Gaps in our modules.** timetable: no substitution record, no "effective teacher" of a session, no free-teacher
  search, no cover load count. hr: approving leave does not list the sessions affected. attendance: the
  substitute cannot open the attendance sheet of a class they do not teach (permission is by teaching assignment).
  comms: no notice to the substitute or the class.
- **Proposed scope.** Tables in `timetable`: `cover_request` (session_id, absent_staff_id, reason leave/duty/
  unplanned, leave_application_id, status open/assigned/no_cover/cancelled), `substitution` (cover_request_id,
  substitute_staff_id, kind teach/supervise/merge_classes/free_period, assigned_by_user_id, notified_at,
  accepted_at), `cover_policy` (institution_id, max_covers_per_week, prefer_same_subject, exclude_roles).
  Operations: `POST /v1/timetable/cover-requests:generateFromLeave` (from an approved leave), CRUD on
  `cover-requests`; `GET /v1/timetable/cover-requests/{id}:suggestSubstitutes` (free that period, same subject via
  `hr.staff_subject`, teaches the class, covers this week vs policy, not on leave by `attendance.staff_attendance`);
  `POST /v1/timetable/cover-requests/{id}:assign`, `:accept`, `:decline`, `:markNoCover`;
  `GET /v1/timetable/cover-board?date=` (day view); `GET /v1/timetable/cover-load` (report). `timetable.GetTimetable`
  returns the effective teacher. States: cover request open -> assigned -> accepted (or declined -> open) -> done;
  open -> no_cover. Events: consumes `hr.leave_application.approved` and `.cancelled` (create or cancel cover
  requests); produces `timetable.substitution.assigned` (comms push to substitute, class notice, attendance grants
  sheet access for that session). Permissions: `timetable.cover_request.manage` (timetable in-charge),
  `timetable.substitution.accept` (own), `timetable.cover_load.read`. Screens: TT-05 cover board for the day with
  suggestions (W B), TT-06 my covers today with accept (T), cover marker on TT-03 and ATT-03 (S T P).
- **Owner module.** Extend `timetable` (**academics** group, which also holds attendance, so the sheet-access rule
  becomes an in-module call after Plan A).
- **Integrations and regulation.** None external. Teacher workload norms (for example RTE schedule norms on
  working hours, state service rules) to verify only if load caps are enforced rather than advisory.
- **Privacy.** Leave reason is not shown to the substitute or class; only "cover".
- **Depends on.** BE-1 (`GenerateSessions` so sessions exist as dated rows), DEC-3 (staff self-scope), ARC-2,
  ARC-3. **Priority P2 / M.**
- **Open questions.** Must the substitute accept, or is assignment final? Should unassigned covers merge classes
  automatically? Count covers per week or per term?

#### NM-8 Clubs, sports and houses

- **Problem and users.** Teachers in charge of clubs and teams manage members, sessions and attendance; the
  sports teacher runs inter-house events; houses earn points across the year; students and parents see
  achievements; admissions to college and scholarships ask for a record of co-curricular work.
- **Exists today.** `sis.activity_type` and `sis.student_activity` (student, staff, type, date, description) with
  served CRUD (`StudentActivityApi`) and the Activities area of SIS (ui/02 §2); `comms.event` with categories
  cultural, education, exam, holiday, meeting, live_class, other and `comms.event_attendee`; `lms.badge` and
  `lms.badge_award` for learning badges; `people.certification` for adult certifications. The portfolio workflow is
  proposed in the learning brainstorm (TD-037, TD-040) and the community network brainstorm plans "approved clubs"
  as audiences.
- **Gaps in our modules.** sis: activities are a flat log, no club, team, membership, house, position or level
  (school/zonal/state/national). comms: no competition, fixture or result, no "sports" category. assessment: CBSE
  co-scholastic grades are not linked to activity evidence (to verify the current CBSE assessment scheme).
- **Proposed scope.** Tables (schema `activities`): `club` (institution_id, name, kind club/society/team/sport,
  in_charge_staff_ids, capacity, academic_year_id), `club_membership` (club_id, student_id, role member/captain/
  secretary, from/to, status), `club_session` with attendance, `house` (name, colour, house_master_staff_id),
  `house_membership` (student_id, house_id, academic_year_id), `house_point` (house_id, points, reason, event_id,
  awarded_by), `competition` (name, level, category, starts_on, organiser internal/external, comms event_id),
  `competition_entry` (competition_id, student_id or team, house_id), `competition_result` (entry_id, position,
  award), `achievement` (student_id, title, level, date, evidence_file_id, source result/manual, verified_by,
  visibility). Migrate `sis.student_activity` rows into `achievement` or keep both (open question).
  Operations: CRUD; `POST /v1/activities/clubs/{id}:join` (student request), `:approveMember`;
  `POST /v1/activities/houses:allocate` (balanced allocation by grade and gender, siblings in the same house
  optional); `POST /v1/activities/competitions/{id}:publishResults` (creates achievements and house points);
  `GET /v1/activities/house-standings`. Events: `activities.achievement.verified` (portfolio, LEARNING-AND-ALUMNI
  TD-037), `activities.competition.results_published` (comms notice and feed post). Permissions:
  `activities.club.manage`, `activities.house_point.award`, `activities.achievement.verify`. Screens: ACT-01
  clubs and members (W T), ACT-02 house standings board (W B, also a kiosk display), ACT-03 competitions and
  results entry (W B), ACT-04 my clubs and achievements (S P).
- **Owner module.** New module `activities` in **students** (a leaf that reads sis); it should not grow sis, and
  lms is excluded by rule 4 of §6.1.
- **Integrations and regulation.** School Games Federation of India and CBSE sports entries (forms only, no API
  known; to verify). Achievements may later feed the APAAR/academic bank of credits (to verify scope).
- **Privacy.** Photos and public results of minors need `photo_video_publication` consent
  (`people.consent_record`); achievements default to school-only visibility.
- **Depends on.** NM-9 (participation certificates), BE-1 consent, ARC-2. **Priority P3 / M.**
- **Open questions.** Fold the portfolio proposal here or keep it in lms? Do houses exist in HE tenants?
  Should house points be visible to parents?

#### NM-9 Certificates and documents

- **Problem and users.** The office issues transfer certificates (TC, called school leaving certificate in some
  states), bonafide, character, conduct, migration, fee paid and study certificates on demand, each numbered, signed
  and logged; receiving schools and employers want to verify them; parents request them online; boards and
  DigiLocker are where families expect to find documents.
- **Exists today.** `sis.IssueBonafideCertificate` (`POST /v1/sis/students/{studentId}:issueBonafideCertificate`,
  permission `sis.student.issue_bonafide_certificate`, served) generating a PDF and storing
  `sis.student.bonafide_certificate_number`; screen SIS-09 (action in SIS-02). `sis.student` has `status`
  (`transferred`, `withdrawn`, `graduated`...), `exit_date`, `exit_reason`, `government_student_id` (PEN/UDISE+).
  `people.person_document` (type certificate/id_proof/other) stores uploaded documents. lms has a full certificate
  stack (`lms.certificate`, `certificate_request`, `lms.VerifyCertificate` public verify, LMS-15).
- **Gaps in our modules.** sis: one hard-coded certificate, number stored on the student (so only one), no
  template, no register of issued documents, no TC, no request workflow, no revocation, no public verification,
  no duplicate copy marking. finance and library: no "no dues" check before a TC. platform: no shared document
  renderer or signing (ARC-4 job pattern needed for PDF).
- **Proposed scope.** Tables in `sis`: `document_template` (institution_id, kind tc/bonafide/character/conduct/
  study/fee_paid/migration/custom, body template, required_fields, numbering_series, signatories),
  `document_request` (student_id, kind, purpose, requested_by guardian/student/office, status), `issued_document`
  (request_id, template_id, number, issued_on, file_id, content_hash, verification_code, copy_kind original/
  duplicate, revoked_at, revoke_reason, digilocker_uri). Drop `sis.student.bonafide_certificate_number` in favour
  of the register. Operations: CRUD on `document-templates`; `POST /v1/sis/document-requests`,
  `:approve`, `:reject`; `POST /v1/sis/document-requests/{id}:issue` (202 + operation for PDF, runs clearance checks:
  `finance.GetFeeStatement` dues, library open loans, hostel vacated); `POST /v1/sis/issued-documents/{id}:revoke`,
  `:issueDuplicate`; public `GET /v1/sis/issued-documents:verify?code=` (minimal fields, rate-limited, SEC-5);
  `sis.IssueBonafideCertificate` kept as a shortcut. TC issue moves the student to `transferred` through the
  existing status change. States: request requested -> approved -> issued or rejected; issued -> revoked.
  Events: `sis.issued_document.issued` (comms notifies guardian; people stores a copy in person documents),
  `sis.issued_document.revoked`. Permissions: `sis.document_request.create` (guardian own child, adult student),
  `sis.document_request.approve`, `sis.issued_document.issue`, `sis.issued_document.revoke`. Screens: SIS-09
  becomes CERT-01 request queue and issue (W), CERT-02 template editor with preview (W), CERT-03 issued register
  with search (W), CERT-04 request and download (P S), CERT-05 public verify page (W public).
- **Owner module.** Extend `sis` (**students**); share the verification and PDF rendering approach with lms
  certificates (a common renderer in platform files or a shared library, decided with ARC-4).
- **Integrations and regulation.** DigiLocker issuer onboarding and push API for "issued documents" (requirements
  for a school or software vendor as issuer to verify; CBSE already publishes marksheets there). RTE Act 2009
  section 5 requires the head teacher to issue a TC promptly and forbids delaying admission elsewhere for lack of
  it (to verify wording for elementary classes; dues must not block a TC there). CBSE affiliation bye-laws prescribe
  a TC format and counter-signature rules (to verify current format). Inclusion of PEN or APAAR id on the TC (to
  verify). Digital signature (eSign or DSC) optional.
- **Privacy.** Certificates carry identity data; the verify endpoint returns only name, document kind, number,
  issue date and status, never the full document.
- **Depends on.** ARC-4 (PDF jobs), SEC-5 (rate limits on verify), BE-1 and ARC-2. **Priority P2 / M.**
- **Open questions.** Who signs (principal only, or delegated)? Should the TC block on unpaid fees for classes
  above 8? One numbering series per kind per year?

#### NM-10 Scholarships and financial aid

- **Problem and users.** Students and parents find schemes they are eligible for (government pre-matric and
  post-matric, state schemes, the school's own merit and need-based aid); the scholarship officer verifies
  applications as institute nodal officer on the National Scholarship Portal (NSP) and on state portals; the
  accountant applies awards to fees or records disbursement; management reports aid given.
- **Exists today.** `finance.fee_concession` (student, fee head, `concession_type`, `concession_value`, reason,
  approved_by, status, validity), `finance.coupon` (LMS), `finance.refund`; `sis.student_category`; guardian
  `annual_income` on `sis.guardian`; the bonafide certificate (NM-9) which portals ask for.
- **Gaps in our modules.** finance: concessions have no scheme, no application, no eligibility rule, no external
  status tracking, no sponsor; awards paid to the student outside fees are not recorded. sis: no caste, minority,
  disability or income-certificate fields (deliberately; collecting them needs a lawful purpose).
- **Proposed scope.** Tables in `finance`: `aid_scheme` (name, provider government/state/school/trust/corporate,
  external_portal nsp/state/none, external_scheme_code, award type fee_waiver/stipend/one_time, amount rule,
  academic_year_id, open/close dates), `aid_eligibility_rule` (scheme_id, field, operator, value; evaluated against
  supplied answers, not stored profile data), `aid_application` (scheme_id, student_id, answers_json,
  documents file_ids, external_application_id, status, verified_by_user_id), `aid_status_event` (application_id,
  status, source manual/import, at, note), `aid_award` (application_id, amount, mode fee_concession/disbursement,
  fee_concession_id, payment ref, disbursed_on). Operations: CRUD on `aid-schemes`;
  `GET /v1/finance/aid-schemes:eligible?studentId=` (own child); `POST /v1/finance/aid-applications/{id}:submit`,
  `:verify`, `:reject`, `:recordExternalStatus`, `:award`; `POST /v1/finance/aid-applications:importStatus` (CSV
  from NSP exports). States: draft -> submitted -> school_verified -> forwarded (external) -> sanctioned or
  rejected -> disbursed. Events: `finance.aid_award.created` (creates a fee concession), consumes
  `sis.issued_document.issued` to attach the bonafide. Permissions: `finance.aid_scheme.manage`,
  `finance.aid_application.verify`, `finance.aid_award.create`, `finance.aid_application.create` (guardian own).
  Screens: AID-01 schemes (W), AID-02 applications queue with document check (W), AID-03 eligible schemes and apply
  (P S), AID-04 aid report by scheme and year (W).
- **Owner module.** Extend **finance**: the award becomes a concession or payment, and §6.1 rule 3 keeps money
  logic there. If applications grow (essays, interviews) move them to students later.
- **Integrations and regulation.** NSP: institutions verify on the portal; a public API for institutions is not
  known (to verify; plan for CSV import of status). State portals (for example MahaDBT, to verify list). Government
  scholarships are paid by DBT to the student's bank account, not to the school (to verify per scheme).
- **Privacy.** Caste, religion, disability and income are highly sensitive; collect per application, per purpose,
  with consent, keep only while the application is live plus audit retention; restricted read permission.
- **Depends on.** NM-9 (bonafide), BE-1 consent, ARC-4 (imports). **Priority P3 / M.**
- **Open questions.** Is the first need government-portal tracking or the school's own aid? Should eligibility
  ever read stored profile data?

#### NM-11 Parent-teacher meeting booking

- **Problem and users.** The school announces a PTM day; each teacher opens slots; parents with two or three
  children book back-to-back slots with different teachers without clashes; some meetings are online; teachers see
  their schedule and record a short note; the class teacher sees who did not come.
- **Exists today.** `comms.event` (category includes `meeting`, audience, class section, start/end) and
  `comms.event_attendee`; COM-03 my calendar, COM-04 events; COM-07 feedback threads; `sis.student_sibling` and
  `sis.student_guardian` (who is a parent of whom); `lms.conferencing_account` (providers `zoom`, `google_meet`)
  and `lms.live_class` with meeting links; LMS-16 evaluation booking is a working slot-booking pattern
  (`lms.GetEvaluatorSlots`, `lms.CreateCertificateRequest`).
- **Gaps in our modules.** comms: events have attendees but no slots, capacity, booking or clash rules; no PTM
  note; no attendance of parents. lms: conferencing is tied to cohorts and live classes, not usable by comms.
  sis: siblings are known but nothing uses them for scheduling.
- **Proposed scope.** Tables in `comms`: `meeting_round` (institution_id, title, date range, mode in_person/online/
  both, audience class sections, slot_minutes, booking opens/closes, event_id), `meeting_host` (round_id, staff_id,
  room or link, availability windows), `meeting_slot` (host_id, start_at, end_at, capacity 1, status),
  `meeting_booking` (slot_id, student_id, guardian_person_id, status booked/cancelled/attended/no_show,
  join_url, booked_at), `meeting_note` (booking_id, note, visible_to_guardian, action_items). Operations: CRUD on
  `meeting-rounds`; `POST /v1/comms/meeting-rounds/{id}:generateSlots`, `:open`, `:close`;
  `GET /v1/comms/meeting-rounds/{id}:availableSlots?studentIds=` (sibling-aware: returns slot sets that do not clash
  and keep a gap for walking between rooms); `POST /v1/comms/meeting-bookings:batchCreate` (one guardian, several
  children, all or nothing); `POST /v1/comms/meeting-bookings/{id}:cancel`, `:checkIn`, `:markNoShow`.
  States: round draft -> open -> closed -> completed; booking booked -> attended/no_show or cancelled. Events:
  `comms.meeting_booking.created` (calendar entry and reminder), `comms.meeting_round.closed` (list of parents not
  booked to the class teacher). Video link from the conferencing account (needs an `lms` call or moving
  conferencing to platform). Permissions: `comms.meeting_round.manage`, `comms.meeting_slot.update` (own as host),
  `comms.meeting_booking.create` (guardian own children), `comms.meeting_note.create`. Screens: PTM-01 round setup
  (W), PTM-02 my slots and day view with notes (T W), PTM-03 book for my children (P), PTM-04 attendance and
  no-show report (W).
- **Owner module.** Extend **comms** (calendar, events and notifications live there); conferencing accounts should
  move from lms to platform or comms so both can use them (open question, affects §6 rule 4).
- **Integrations and regulation.** Zoom and Google Meet already modelled; Jitsi or BigBlueButton as a free option
  (`lms.live_class` already lists `bigbluebutton`). No India-specific rules known.
- **Privacy.** Meeting notes about a child are guardian-visible only when marked; no recording by default.
- **Depends on.** ARC-2 (reminders), ARC-3 (guardian own-children reads), P1-37 BFF for the parent home card.
  **Priority P2 / S.**
- **Open questions.** Separated parents: book separately or jointly? Online links per teacher or per booking?

#### NM-12 Staff appraisal and CPD

- **Problem and users.** The principal and heads of department run yearly appraisals with goals and
  self-assessment; academic coordinators do classroom observations against a rubric; teachers log training
  hours; the school reports CPD completion to the board.
- **Exists today.** `hr.staff` (designation, qualification, specialization, `reporting_to_staff_id`,
  `max_periods_per_week`, status), `hr.designation`, `hr.staff_subject`, leave tables and HR-01 to HR-04;
  `people.qualification`, `people.certification` (name, issuing organisation, dates), `people.education_history`;
  `academics.lesson_plan` and `lesson_plan_approval`; `lms.rubric` and `rubric_assessment` (for LMS grading);
  `lms.learning_program` and `lms.enrollment` (could host in-house training courses).
- **Gaps in our modules.** hr: no appraisal cycle, goal, rating, reviewer chain or observation; no training
  record or hours; `reporting_to_staff_id` is the only line-manager link. people: certifications have no hours or
  category. lms: completion of a staff course is not counted as CPD.
- **Proposed scope.** Tables in `hr`: `appraisal_cycle` (institution_id, academic_year_id, template, phases with
  dates), `appraisal` (cycle_id, staff_id, reviewer_staff_id, status, self_rating, reviewer_rating, final_rating,
  signed_at), `appraisal_goal` (appraisal_id, goal, weight, evidence, rating), `observation_rubric` (criteria and
  levels; may reuse the lms rubric shape), `classroom_observation` (staff_id, observer_staff_id, session_id,
  observed_at, rubric_id, scores_json, strengths, next_steps, shared_at, acknowledged_at), `cpd_requirement`
  (institution_id, academic_year_id, hours_required (CBSE default 50, to verify), category minima), `cpd_activity`
  (staff_id, title, provider, category, hours, from/to, certificate_file_id, source manual/lms_course/import,
  status submitted/approved/rejected). Operations: CRUD; `POST /v1/hr/appraisal-cycles/{id}:start`, `:close`;
  `POST /v1/hr/appraisals/{id}:submitSelfReview`, `:submitReview`, `:finalise`, `:acknowledge`;
  `POST /v1/hr/classroom-observations/{id}:share`, `:acknowledge`; `POST /v1/hr/cpd-activities/{id}:approve`;
  `GET /v1/hr/cpd-summary?academicYearId=`. States: appraisal not_started -> self_review -> manager_review ->
  finalised -> acknowledged; CPD activity submitted -> approved or rejected. Events: consumes
  `lms.enrollment.completed` (or the certificate issued event) for staff to propose a CPD activity; produces
  `hr.appraisal.finalised` (payroll may read for increments, later). Permissions: `hr.appraisal.review` (reviewer
  of own reportees), `hr.appraisal.read` (own via DEC-3 self-scope), `hr.classroom_observation.create`,
  `hr.cpd_activity.approve`. Screens: HR-10 appraisal cycle setup and progress (W), HR-11 my appraisal (W T),
  HR-12 observation form (B, offline in class), HR-13 my CPD log and hours bar (T W), HR-14 CPD compliance report
  (W).
- **Owner module.** Extend `hr` (**workforce**); observations reference timetable sessions by id only.
- **Integrations and regulation.** CBSE expects at least 50 hours of CPD per teacher per year (circular number and
  current text to verify); CBSE training portal and NISHTHA/DIKSHA certificates could be imported (format to
  verify). State teacher-appraisal schemes for aided schools to verify.
- **Privacy.** Appraisal ratings and observation notes are confidential HR data; only the staff member, the
  reviewer chain and HR may read them; not exposed through `hr.staff.read`.
- **Depends on.** DEC-3 (staff self-scope), ARC-3, P1-34 (cards for reviewer names). **Priority P3 / M.**
- **Open questions.** Should ratings feed pay (payroll)? One rubric per school or per chain? Are observations
  shared with the teacher by default?

---

### 3.11 Differentiating features (owner-selected)

Features few school systems offer, picked by the owner from a brainstorm. Same conventions as §3.10 (operation,
permission and event naming; W/T/S/P/B client codes). Children's data in any of them follows the Privacy and Consent
Centre ([docs/design/PRIVACY-AND-CONSENT-CENTRE.md](docs/design/PRIVACY-AND-CONSENT-CENTRE.md), SEC-11).

| ID | Pri / size | What | Owner / depends on |
|---|---|---|---|
| IN-1 | P2 / L | **Self-checking year planner** | Extend `org` + `timetable` (academics group); NM-7 |
| IN-2 | P2 / M | **School-to-school transfer packet** | Extend `sis`; SEC-11, NM-9 |
| IN-3 | P3 / M | **"Where your fees go" statement** | Extend `finance`; NM-4 for spend data |
| IN-4 | P2 / M | **Student voice: "you said, we did"** | Extend `comms`; SEC-11 |
| IN-5 | P2 / M | **Report card explained** | Extend `assessment`; DEC on AI provider; SEC-11 |
| IN-6 | P1 / M | **Parent request desk** | Extend `comms` (+ `frontoffice` intake); NM-9, NM-11 |
| IN-7 | P1 / L | **One timeline per child** | New read model in `comms`; every module's events; SEC-11 |
| IN-8 | P3 / S | **School promises tracker** | Extend `comms`; IN-6 |
| IN-9 | P2 / M | **Ask the rulebook** | Extend `comms`; IN-5 AI provider decision |
| IN-10 | P2 / S | **Full cost of a year, before admission** | Extend `admission` + `finance` |
| IN-11 | P3 / S | **Parent skills and volunteer board** | Extend `comms`; SEC-11 |
| IN-12 | P3 / M | **Book and uniform exchange** | Extend `library` or NM-4 inventory; SEC-11 |
| IN-13 | P3 / M | **Carpool matching** | Extend `transport`; SEC-11 |

#### IN-1 Self-checking year planner

- **Problem.** The year plan lives in spreadsheets and circulars. When one date moves (sports day, a board date,
  an unplanned holiday) nobody sees what else breaks until it does.
- **Exists today.** `org.academic_year` / `org.academic_term`, timetable sessions, assessment exam sessions, comms
  `Event` and calendar export, holidays (verify where held). Nothing links them or checks conflicts.
- **Scope.** A `year_plan` per institution and year holding typed plan items (term, exam window, holiday, event,
  PTM, board date, syllabus milestone) that reference the owning record in its module. Rules, stored as data:
  no exam within N days of a holiday, minimum teaching days per term, rooms and invigilators not double-booked,
  syllabus milestones before the exams that test them, no PTM on a board-exam day. A `CheckYearPlan` operation
  returns conflicts. `SimulateChange` answers "move item X to date D" with everything affected, before anything is
  saved. Publishing the plan pushes dates to the owning modules and to calendars (ICS per class and per child).
- **Operations.** `org.YearPlan` CRUD, `:check`, `:simulate`, `:publish`; permission `org.year_plan.*`;
  event `org.year_plan.published`.
- **Screens.** W: year board (Gantt by term), conflict panel, what-if drawer. P/S: read-only calendar.
- **Open questions.** Which rules ship as defaults per board (CBSE, state)? Who may override a hard rule?

#### IN-2 School-to-school transfer packet

- **Problem.** A moving student's records travel as paper and photocopies. The new school re-keys everything,
  and health and learning history are lost.
- **Exists today.** `sis` holds the student, guardians, enrolments and documents; bonafide certificates are served;
  the transfer certificate is NM-9; consent records are moving to privacy (SEC-11).
- **Scope.** On leaving, the school issues a signed packet: transfer certificate, enrolment and attendance summary,
  marks history, activities, and optionally health summary and learning notes, each part only with guardian consent.
  Format: a JSON bundle plus a PDF, signed with the tenant's key, with a verification URL and code. Importing at
  a MySMS school is one step: preview, accept or edit, create the applicant or student. A non-MySMS school gets the
  PDF and a verify link. Later: APAAR ID and DigiLocker issue (to verify).
- **Operations.** `sis.IssueTransferPacket`, `sis.VerifyTransferPacket` (public, minimal fields),
  `admission.ImportTransferPacket`; permissions `sis.transfer_packet.issue`, `admission.transfer_packet.import`;
  events `sis.transfer_packet.issued`, `admission.transfer_packet.imported`.
- **Privacy.** Guardian chooses the optional parts; the packet expires; every view of the verify link is logged.
- **Open questions.** Packet standard: our own JSON now, or wait for a government schema? Cross-tenant delivery
  inside MySMS or always guardian-carried?

#### IN-3 "Where your fees go" statement

- **Problem.** Fee increases cause distrust because parents cannot see how money is spent.
- **Exists today.** `finance` has fee heads, invoices and payments (income side). No expense ledger;
  `payroll` holds salary costs; procurement is NM-4.
- **Scope.** A yearly (and optionally termly) statement per institution: income by fee head; spending by category
  (salaries in aggregate only, facilities, transport, learning materials, technology, activities, scholarships);
  cost per student; year-on-year change with the school's explanation. Data comes from payroll totals, NM-4
  purchase orders and a simple manual expense entry until NM-4 exists. The school approves before publishing.
- **Operations.** `finance.SpendingStatement` draft, `:approve`, `:publish`; permission `finance.spending_statement.*`.
- **Screens.** W: builder with category mapping. P: a statement with charts.
- **Privacy.** Aggregates only; any category with fewer than 5 staff is merged so salaries cannot be inferred.
- **Open questions.** Mandatory categories? Show the comparison with other schools of the group?

#### IN-4 Student voice: "you said, we did"

- **Problem.** Surveys are run, results vanish, and students stop answering.
- **Exists today.** `comms` has audience groups, feedback threads, notices and tasks; no survey with anonymity
  guarantees; no link from a result to an action.
- **Scope.** Pulse surveys (short, recurring) and topic surveys, sent to audiences (class, grade, school, staff,
  parents). Anonymity: answers stored without the respondent id, with a minimum group size before results show
  (for example 7). Free-text answers optionally screened for safeguarding words, which route to the counsellor
  without breaking anonymity of the rest. Each finding can open an action (comms `Task`) with an owner and date.
  A "you said, we did" board shows findings, actions and their status back to the respondents.
- **Operations.** `comms.Survey` CRUD, `:launch`, `:close`; `comms.SubmitSurveyResponse`; `comms.SurveyFinding`
  with linked tasks; permissions `comms.survey.*`, `comms.survey_finding.*`; events `comms.survey.closed`.
- **Screens.** W: survey builder, results and findings. S/P/T: answer; "you said, we did" board.
- **Open questions.** Student surveys under 18 need guardian consent? (SEC-11.) Who sees raw free text?

#### IN-5 Report card explained

- **Problem.** Report cards are numbers and codes that many parents cannot read, often not in their language.
- **Exists today.** `assessment` produces results and marksheet lines with grading bands; the i18n catalogues cover
  en and hi; no AI integration exists.
- **Scope.** For each published report card, a plain-language explanation in the parent's language: what is going
  well, what to watch, what to do at home, the trend from earlier terms. Generated from structured data only (marks,
  bands, attendance, teacher remarks) with a fixed template and a language model for wording. The teacher reviews or
  edits before release; nothing is published unreviewed. Every sentence links back to the number it explains.
- **Operations.** `assessment.GenerateReportExplanation`, `:approve`, `:publish`;
  permission `assessment.report_explanation.*`; event `assessment.report_explanation.published`.
- **Privacy.** Only the minimum data goes to the model; no name in the prompt; a provider with no training on our
  data and processing in India where possible (to verify); recorded as a processing activity in privacy.
- **Open questions.** AI provider choice and cost per student; languages at launch; whether teachers can switch it
  off per student.

#### IN-6 Parent request desk

- **Problem.** Parent requests arrive by WhatsApp, calls, notes in diaries and visits. Nothing is tracked, the same
  request is asked three times, and nobody knows what is overdue.
- **Exists today.** `comms` conversations, mailbox and tasks; `frontoffice` call logs and visitor logs; requests
  that already have a workflow (leave, certificates, bus changes) live in their own modules.
- **Scope.** One request type catalogue per school (leave, bus change, certificate, fee query, complaint, meeting,
  other), each with a form, an owner role, a response deadline and an escalation ladder (teacher → coordinator →
  principal → management) with timers. Requests that belong to a module's workflow are handed to it and their status
  flows back. Intake from the parent app, front office (calls and visits) and later a WhatsApp Business bridge.
  Duplicates are merged. Parents see status at all times. Reports: volume, overdue, time to close, by type.
- **Operations.** `comms.ParentRequest` create (parent), `:assign`, `:respond`, `:escalate`, `:close`, `:reopen`;
  `comms.RequestType` configuration; permissions `comms.parent_request.*`, `comms.request_type.*`;
  events `comms.parent_request.created`, `.escalated`, `.closed`. State machine: new → assigned → in_progress →
  waiting_parent → resolved → closed, with reopen within 7 days.
- **Screens.** P: new request, my requests. W/T: queue with deadlines, request detail, escalation. Front office: log
  a call or visit as a request.
- **Open questions.** Quiet hours for teachers? Which types may parents raise without the teacher in the loop?
  WhatsApp Business provider.

#### IN-7 One timeline per child

- **Problem.** A parent gets dozens of separate notifications from different modules and still cannot answer
  "how is my child doing this week?". Staff have the same problem when a parent calls.
- **Exists today.** Each module publishes events through its outbox; comms has in-app notifications and
  notification preferences. Nothing assembles them per child.
- **Scope.** A per-student timeline read model fed by events: attendance marks and absences, homework set and
  submitted, marks published, report cards, remarks and discipline notes (if shared), fee invoices and payments,
  health visits (NM-1, consent-gated), bus events (NM-5), requests (IN-6), notices for the child's class. Each entry
  has a type, time, short text, a link to the source record and a visibility (parent, student, staff). Filters by
  type and week, a weekly digest ("this week in one screen"), and a staff view used when a parent calls. Entries
  are copies built from events, so the source module stays the owner; deletion and erasure events remove them.
- **Operations.** `comms.ListChildTimeline` (parent: own children; student: self; staff: in data scope),
  `comms.GetWeeklyDigest`; permission `comms.child_timeline.read`; consumes events of sis, attendance, assessment,
  lms, finance, transport, comms.
- **Screens.** P/S: timeline home tab, weekly digest. W/T: student 360 panel.
- **Privacy.** Visibility per entry type is set per tenant; health and counselling entries never shown without the
  matching consent (SEC-11); retention follows each source's category.
- **Open questions.** Does the timeline replace push notifications (digest only) or sit beside them?

#### IN-8 School promises tracker

- **Scope.** The school publishes commitments (facility, academic, safety, policy) with an owner, target date,
  status (planned, in progress, done, delayed with reason) and updates with photos. Parents can follow and react,
  but not comment publicly. Promises can come from parent requests (IN-6) or survey findings (IN-4). Yearly summary:
  kept, delayed, dropped.
- **Operations.** `comms.SchoolPromise` CRUD, `:update-status`; permission `comms.school_promise.*`;
  event `comms.school_promise.status_changed`.
- **Open questions.** Who may publish (principal only)? Can a promise be withdrawn silently? (Proposed: no, it
  shows as dropped with a reason.)

#### IN-9 Ask the rulebook

- **Scope.** The school's policies (uniform, leave, attendance, fees and refunds, discipline, transport, device use)
  as structured, versioned documents in plain language, in each supported language. Parents and staff acknowledge a
  new version (tracked like IN-6 broadcasts). An assistant answers questions only from the published policies and
  always quotes the clause it used; if no clause covers the question it says so and offers to raise a parent request
  (IN-6). Admins see unanswered questions, which shows gaps in the rulebook.
- **Operations.** `comms.Policy` CRUD, `:publish`, `:acknowledge`; `comms.AskRulebook`; permissions
  `comms.policy.*`, `comms.rulebook.ask`.
- **Privacy.** Questions are stored without the asker's identity after 30 days (proposed).
- **Open questions.** Same AI provider decision as IN-5; may the assistant answer about a specific child (no, by
  default)?

#### IN-10 Full cost of a year, before admission

- **Scope.** For each grade, a published estimate of the year's total cost: tuition and other fee heads from the fee
  structure, transport by distance band, books and uniform (from NM-4 or entered), trips and activities, optional
  items marked optional. Shown on the admission page and in the application, with a downloadable statement. Updated
  when the fee structure changes; the version shown to an applicant is kept with the application.
- **Operations.** `admission.CostEstimate` per grade and year, `:publish`; public read
  `admission.GetCostEstimate`; permission `admission.cost_estimate.*`.
- **Open questions.** Must every optional cost be listed? Show the previous year's actual cost alongside?

#### IN-11 Parent skills and volunteer board

- **Scope.** Parents (and alumni later) list skills and availability they are willing to offer (career talks, lab
  help, coaching, translation, medical camp). The school posts needs with dates; matching suggests people; the school
  confirms. Volunteer visits go through front office (visitor log) and, for time with children, the safe-adult
  rules (identity check, supervised sessions).
- **Operations.** `comms.VolunteerProfile`, `comms.VolunteerNeed`, `:match`, `:confirm`; permissions
  `comms.volunteer_profile.*`, `comms.volunteer_need.*`.
- **Privacy.** Profiles are opt-in, visible only to school staff, never to other parents unless the parent chooses.
- **Open questions.** Background checks for volunteers who work with children (POCSO guidance, to verify).

#### IN-12 Book and uniform exchange

- **Scope.** Parents list used textbooks and uniforms (grade, subject or size, condition, price or free); others
  reserve; hand-over at school on set days through the front office or library desk, so parents need not meet or
  share phone numbers. The school can seed it with last year's book list. Optional donation to a school pool for
  families in need.
- **Operations.** `library.ExchangeListing` (or inventory under NM-4), `:reserve`, `:handover`; permission
  `library.exchange_listing.*`.
- **Open questions.** Money handled in the app (needs payment rules) or cash and free only?

#### IN-13 Carpool matching

- **Scope.** Opt-in families share an approximate pickup area (grid cell, never the exact address) and timings;
  matching suggests families on the same route and grade band; both sides must accept before contact details are
  shared. Shown only where the school bus does not run or is full. The school can disable it.
- **Operations.** `transport.CarpoolProfile`, `transport.ListCarpoolMatches`, `:request`, `:accept`; permission
  `transport.carpool.*`.
- **Privacy.** Location of minors' homes: grid cells only, opt-in, revocable, never visible to non-matched families;
  the school is not a party to the arrangement (disclaimer to verify with counsel).
- **Open questions.** Liability wording; should drivers be verified?

### 3.12 Competitive parity features (owner-selected)

Features that Indian school ERPs sell and that buyers ask for in demos, picked by the owner after a look at Fedena,
Vidyalaya, PowerSchool, MyClassboard and Edunext (their public feature pages, listed under Sources at the end of this
section; those pages are marketing claims, not tested). Same conventions as §3.10: operation, permission and event
naming, W/T/S/P/B client codes, screen ids are proposals for [ui/02](ui/02-navigation-and-screens.md). External facts
were checked on 2026-11-18 and are cited; anything not found in a source is marked **to verify**. Children's data in
any of them follows the Privacy and Consent Centre
([docs/design/PRIVACY-AND-CONSENT-CENTRE.md](docs/design/PRIVACY-AND-CONSENT-CENTRE.md), SEC-11). Where an item
overlaps an earlier one it builds on it rather than repeating it (CP-3 generalises IN-6's WhatsApp bridge, CP-5 is the
parent-facing layer of NM-4, CP-6 shares NM-9's document renderer).

| ID | Pri / size | What | Owner module / depends on |
|---|---|---|---|
| CP-1 | P1 / M | **Admission enquiry CRM:** leads, sources, campaigns, counsellors, follow-ups, funnel, lead to application | Extend `admission` (+ `frontoffice` intake, `comms` delivery); ARC-2, DEC-5, SEC-5, CP-3, IN-10 |
| CP-2 | P2 / L | **Automatic timetable generator** from teaching load, availability and rules | Extend `timetable` (academics group); Timefold Solver 1.x on Java 17; P2-03, BE-1 (`GenerateSessions`), ARC-4, OPS-3; feeds NM-7, IN-1 |
| CP-3 | P1 / M | **WhatsApp, free only:** share to the teacher's class group from the app, invite-link workflow; no paid API | Extend `comms`; ARC-2, BE-1, SEC-11, SEC-4; generalises IN-6 bridge |
| CP-4 | P3 / L | **School website** with CBSE mandatory disclosure pages fed by MySMS. **Parked: approach to be decided** | New `website` module; DEC-10, P1-28, ARC-4, SEC-11, CP-1, IN-10 |
| CP-5 | P3 / M | **School store:** uniforms, books, stationery; online orders, payment, pickup | Extend NM-4 `inventory` + `finance` gateway; D5 (review), ARC-2, CP-3, IN-12 |
| CP-6 | P1 / M | **ID card generator** for students and staff, with QR verification | Extend `people`; renderer shared with NM-9; ARC-4, SEC-11, NM-6 |
| CP-7 | P1 / L | **Management dashboards** for principal, management and school groups | New read-model module `insights`; D12/ARC-5, PERF-4, PERF-2, OPS-1, D1 (review)/P1-27, CP-1 |

#### CP-1 Admission enquiry CRM

- **Problem and users.** In admission season enquiries arrive by phone, walk-in, website form, WhatsApp, school
  fairs, newspaper and social ads, and referrals from current parents. Counsellors (the `admissions` persona) need a
  queue of leads with the next follow-up; the admissions head assigns and balances leads; the principal and management
  want the funnel (enquiry -> campus visit -> application -> admitted) by source and campaign and the cost per
  admission. Today this lives in registers and spreadsheets. Fedena sells an "Enquiry & Registration" add-on
  (embedded forms, multi-stage tracking, counsellor assignment, source- and counsellor-wise reports); MyClassboard
  advertises lead management that consolidates multi-channel campaign leads and avoids duplicates.
- **Exists today.** `admission.admission_cycle` (status draft/confirmed/open/admission/closed/cancelled,
  `min_applications`, `max_applications`, `minimum_age_years`, `application_fee_head_id`) and
  `admission.application` (applicant identity and contact fields, `photo_file_id`, previous school, `status`
  draft ... admitted/done, `payment_status`) with screens ADMN-01..05 (`admission.StartAdmission`,
  `admission.OpenApplications`, `admission.ApproveApplication`, `admission.EnrollApplicant`; the public form ADMN-05
  waits on DEC-5). `frontoffice.call_log` (direction, purpose, caller_name, incoming/outgoing number, duration,
  description) and `frontoffice.visitor_log` (visitor, purpose, `student_id`, `host_staff_id`) with FO-01/FO-02 CRUD,
  not linked to any lead. `platform.sales_enquiry` (contact_name, phone, organisation_name, channel, message, status;
  `platform.MarkEnquiryContacted`, PLT-03) is MySMS's own B2B sales enquiry, useful only as a pattern. The privacy
  design (§8) already lists `admission.application` as legitimate use with retention policy `applicant.not_admitted`;
  `people.consent_record.purpose` has `marketing`.
- **Gaps per module.** admission: no pre-application record, source, campaign, owner, follow-up, lost reason or funnel;
  `application` has no `enquiry_id`. frontoffice: calls and visits cannot be tagged to a lead. comms: no delivery yet
  (ARC-2) and no WhatsApp (CP-3).
- **Scope (schema `admission`).** `lead_source` (code, name, kind walk_in/phone/web_form/whatsapp/referral/event/
  advert/partner/other, is_active); `campaign` (name, lead_source_id, starts_on, ends_on, budget, utm_code,
  grade_level_ids, status draft/active/ended); `enquiry` (institution_id, admission_cycle_id nullable,
  academic_year_id, grade_level_id or program_id, child_name, child_date_of_birth, guardian_name, phone_e164, email,
  locality, preferred_contact, lead_source_id, campaign_id, referrer_person_id, counsellor_staff_id, stage, score,
  next_follow_up_at, lost_reason_code, duplicate_of_id, application_id, utm fields); `enquiry_activity` (kind call/
  visit/whatsapp/sms/email/note/campus_tour/stage_change, occurred_at, actor, outcome, `call_log_id` or
  `visitor_log_id`, notes); `follow_up` (due_at, assignee, channel, status open/done/missed); `counsellor_load`
  (staff_id, grade levels, max_open_leads); `lost_reason` catalogue. Duplicate check on normalised phone plus child
  name within an institution and cycle.
- **State machine.** enquiry new -> contacted -> visit_scheduled -> visited -> application_started -> applied ->
  admitted (on the linked application's `admitted`); any open stage -> lost (reason) or junk; lost -> reopened;
  merge folds a duplicate into its survivor and keeps both activity histories.
- **Operations.** CRUD for lead sources, campaigns, enquiries, follow-ups; `POST /v1/admission/enquiries/{enquiryId}:assign`,
  `POST /v1/admission/enquiries:assign` (bulk, round robin by `counsellor_load`), `:advance`, `:markLost`, `:reopen`,
  `:merge`, `:convert` (creates a pre-filled draft application and returns `applicationId`),
  `POST /v1/admission/enquiries/{enquiryId}/activities`, `POST /v1/admission/enquiries:import` (fair lists through
  platform import jobs), public `POST /v1/admission/public/enquiries` (website and QR-poster form; captcha and rate
  limit per SEC-5), `GET /v1/admission/enquiry-funnel` (counts and conversion by stage, source, campaign, counsellor).
  Permissions `admission.enquiry.read|create|update|assign|convert|export`, `admission.enquiry.read_own` (a
  counsellor sees own leads), `admission.campaign.*`, `admission.lead_source.*`. Events
  `admission.enquiry.created`, `.assigned`, `.stage_changed`, `.converted`, `.lost`; consumes the application's own
  submitted/admitted transitions, `frontoffice.call_log.created` (suggest a link by phone) and
  `comms.inbound_message.received` (CP-3).
- **Screens.** ADMN-06 enquiry inbox (W; kanban by stage and table, filters by counsellor, source, follow-up due);
  ADMN-07 enquiry detail with activity timeline, quick "log call" and "send template" (W T); ADMN-08 today's
  follow-ups (W T, counsellors work from the phone); ADMN-09 sources and campaigns (W); ADMN-10 funnel report (W,
  also a CP-7 tile); embeddable public enquiry form (W public, also a CP-4 block); FO-02 gains "link to enquiry".
- **Integrations.** CP-3 for WhatsApp follow-ups and click-to-chat leads; SMS through ARC-2 (Indian DLT template
  registration, **to verify**); UTM capture on the public form; Meta and Google lead-ad webhooks later (**to verify**
  APIs and terms); IN-10 cost estimate attachable to a follow-up.
- **Privacy and security.** Data of a child and parent before any relationship exists: purpose `admission.enquiry`,
  legitimate use for a voluntary enquiry (**to verify** under DPDP with counsel); lost and junk enquiries erased after a
  retention period (12 months proposed) by the privacy retention engine; later marketing to past enquirers only with
  `marketing` consent; no child photo at enquiry stage; export is a separate audited permission.
- **Performance.** Hundreds to a few thousand enquiries per school per season; funnel from a stage-history table;
  index (tenant_id, institution_id, counsellor_staff_id, next_follow_up_at).
- **Depends on.** DEC-5 (same question for the public enquiry form), ARC-2, SEC-5; CP-3 optional. **Priority P1 / M.**
- **Open questions.** A central call centre for a school group (tenant-level counsellors across institutions)?
  Automatic lead scoring? Paid lead-ad integrations in v1? Charge the application fee at conversion? Retention period
  for lost leads?

#### CP-2 Automatic timetable generator

- **Problem and users.** The timetable coordinator builds the weekly timetable for 30-120 class sections by hand,
  which takes days each term and is redone whenever a teacher leaves. Competitors sell one-click generation: Fedena's
  "Automatic Timetable Generator" (several options in one click, subject placement preferences, teacher clash
  avoidance), Vidyalaya's "AI timetable generator" (teacher availability, room rules, preferred slots, breaks, fair
  workload) and PowerSchool's Master Schedule Builder (scenarios, meeting patterns, teacher and room assignment,
  locked classes not moved by the automated build, then students loaded into the schedule).
- **Exists today.** `timetable.period` (name, start_time, duration_minutes, sequence); `timetable.session`
  (period_id, weekday, start_at, end_at, class_section_id, course_id, batch_id, subject_id, staff_id, room_id,
  session_type, status); `timetable.GenerateSessions` (`POST /v1/timetable/sessions:generate`, "generate sessions
  from a weekly pattern; clashes (teacher, room, class) are reported", still 501 in BE-1); screens TT-01..TT-04
  (TT-02 builder). Demand data already exists: `academics.teaching_assignment` (class_subject_id, staff_id,
  `periods_per_week`, `periods_remaining`, teacher_role), `academics.class_subject`, `academics.course_subject`;
  capacity: `hr.staff.max_periods_per_week`, `facility.room` (room_type, capacity, `default_class_section_id`);
  calendar: `org.academic_term`, `org.holiday`. P2-03 proposes the weekly template table. `lms.timetable_template`
  and `lms.timetable_entry` are LMS-only and stay separate.
- **Gaps per module.** timetable: no stored weekly pattern (P2-03), no versions, no teacher availability, no
  constraint or preference model, no solver, no publish step, no double periods for labs, no parallel or combined
  groups (electives across sections, higher-ed batches). academics: no flag for "needs lab" or "double period" on a
  class subject. hr: no per-day maximum.
- **Scope (schema `timetable`).** `timetable_version` (institution_id, academic_term_id, name, status, effective_from,
  effective_to, hard_score, soft_score, generation_run_id); `weekly_slot` (the P2-03 template line: version_id,
  class_section_id or batch_id, weekday, period_id, subject_id, staff_id, room_id, is_locked); `lesson_requirement`
  (derived from teaching assignments: class_section_id, subject_id, staff_id, periods_per_week, block_size 1 or 2,
  required_room_type, parallel_group_id for electives taught at the same time); `staff_availability` (staff_id,
  weekday, period_id, kind unavailable/prefers_not); `constraint_setting` (version_id, code, enabled, weight,
  params: teacher max per day and max consecutive, subject at most once a day, class teacher in period 1, PE not
  right after lunch, minimise teacher gaps, keep a class in its home room); `generation_run` (version_id, status
  queued/running/succeeded/failed/cancelled, time_limit_seconds, solver_version, input_hash, score,
  unassigned_count, explanation file).
- **State machine.** version draft -> generating -> generated (or failed -> draft) -> published -> archived; editing
  a published version copies it to a new draft; only one published version per term and date range.
- **Solver choice.** (A) **Timefold Solver** (open-source edition under Apache License 2.0, a fork of OptaPlanner);
  its school-timetabling quickstart models a Lesson as planning entity with timeslot and room as planning variables,
  hard constraints room, teacher and student-group conflict and soft constraints such as teacher room stability and
  few gaps. **Java 17 (decided, D10):** Timefold Solver 1.x documents "17+" as its minimum JDK, so the 1.x line is
  used as a library on Java 17 with no port. The quickstart's JDK 21 requirement belongs to the sample application,
  not the solver. Pin the 1.x line; Solver 2.0 is not adopted until MySMS changes Java version. When Timefold stops
  maintaining 1.x (announced through 2026), keep running the last 1.x release, and if a fix is ever needed, maintain
  an internal fork of the Apache 2.0 code on Java 17 (licence and NOTICE kept). Some features (for example
  multithreaded solving) are in commercial editions and are not used. (B) custom greedy plus
  local search: no dependency, months of tuning. (C) a hosted solver API: personal data of staff leaves MySMS (processor
  under SEC-11) and adds cost. Recommendation: A, in-process on a worker (OPS-3 worker mode, ARC-4 job pattern),
  time-boxed (60-300 s), one run per institution at a time, input snapshot stored for reproducibility.
- **Operations.** `timetable.TimetableVersion` CRUD; `POST /v1/timetable/timetable-versions/{timetableVersionId}:generate`
  (202 + operation, ARC-4), `:cancelGeneration`, `:publish` (writes future `timetable.session` rows through
  `GenerateSessions` from the effective date, keeps past sessions), `:copy` (from the previous term);
  `GET /v1/timetable/timetable-versions/{timetableVersionId}/violations` (broken constraints with reasons);
  `POST /v1/timetable/lesson-requirements:sync` (from academics teaching assignments); `staff-availabilities` CRUD.
  Permissions `timetable.timetable_version.read|create|update|generate|publish`, `timetable.staff_availability.manage`,
  `timetable.staff_availability.update_own` (teacher). Events `timetable.timetable_version.generated`,
  `timetable.timetable_version.published` (comms notice to teachers and classes, NM-7 cover, attendance period
  registers, IN-1 planner), `timetable.generation_run.failed`.
- **Screens.** TT-05 generation setup (W): readiness check (teachers above their maximum, missing rooms, unassigned
  subjects), constraints and weights, availability grid; TT-06 run result (W): score, unassigned lessons and why,
  compare with the current version; TT-02 builder gains drag and drop with lock, live clash check and "re-solve
  unlocked only"; TT-07 my availability (T); class, teacher and room timetables as print PDF (W).
- **Performance and scale.** A school of 30-80 sections at about 45 periods a week is 1,500-3,600 lessons per run;
  CPU-bound, so never on API pods; cap concurrent runs per worker; a run must not hold a database connection while
  solving (load, solve, write).
- **Privacy and security.** Low: staff availability may reveal personal reasons, so store only the slot, not the
  reason; publish and generate are separate permissions.
- **Depends on.** P2-03, BE-1 (`GenerateSessions`), ARC-4, OPS-3, D10 (Java version); feeds NM-7 and IN-1.
  **Priority P2 / L.**
- **Open questions.** Higher education: student-level course
  requests (PowerSchool style) in scope, or section-based only? Teachers shared across campuses of a group? May an
  admin override a hard constraint with a recorded reason?

#### CP-3 WhatsApp channel

- **Owner decision: WhatsApp must cost nothing.** The paid routes below stay documented for reference only. Findings
  that shape the design:
  - The WhatsApp Business Platform charges per delivered message for business-initiated templates; only replies
    inside the customer-service window and some free entry points cost nothing, which cannot carry scheduled notices.
  - Meta's **Groups API** is not a way round it: it needs an Official Business Account (green-tick status), groups
    hold at most **8 participants**, and group messages are also charged per delivered message (Meta Groups API docs;
    Kapso, 2026).
  - Posting into WhatsApp groups through unofficial automation of WhatsApp Web (libraries such as whatsapp-web.js or
    Baileys) breaks WhatsApp's terms and risks the number being banned; MySMS will not use it.
- **Free design (adopted).**
  1. **Share to WhatsApp from the teacher app.** For a notice, reminder, homework or event, the teacher taps
     "Share to WhatsApp". MySMS composes the text (in the class's language) with a short link to the full item in
     MySMS, and opens the phone's share sheet or WhatsApp's share intent; the teacher picks their class group and sends.
     Free and within WhatsApp's terms. MySMS records "shared by teacher at time" (not delivery); read and
     acknowledgement come from the link, which opens the item in the app or web.
  2. **Group invite workflow.** The teacher creates the class group in WhatsApp and pastes its invite link into MySMS
     (class page setting). MySMS sends the link to the class's parents and students by push and in-app, shows who has
     not yet opened it, and re-sends to new admissions. Nobody's number is collected outside MySMS's own records.
  3. **WhatsApp Channels (optional).** A school may run a WhatsApp Channel for one-way broadcasts; MySMS prepares the
     post and the share, the admin posts it (no posting API assumed; **to verify**).
  4. **Free channels stay primary.** Push and in-app are the delivery of record (free); SMS is paid and used only
     where a school opts in for urgent notices.
- **Privacy and safety notes.** A teacher's personal number is visible to the group: recommend a school-owned phone per
  class or section, or the school's WhatsApp Business app number as group admin. Groups that include students under
  18 need a staff co-admin and the school's group rules (IN-9 rulebook). Sharing consent: purpose
  `comms.whatsapp_optional` (SEC-11) governs whether a family's contact is offered the invite.

- **Problem and users.** Indian parents read WhatsApp first; schools today push notices through teachers' personal
  numbers and class groups, which exposes staff numbers, has no audit, no opt-out and no delivery record. Admins
  want WhatsApp as one more channel for absence notices, fee reminders, circulars and PTM reminders, with SMS as
  fallback. Fedena's add-on page lists SMS and e-mail only; MyClassboard sells WhatsApp Business marketing as a
  separate admissions product.
- **Exists today.** `comms.notification_channel` (code, is_enabled; global catalogue on PLT-04);
  `comms.message_template` (channel CHECK `email`/`sms`/`push`/`in_app`, `external_template_id`, which fits a
  provider template id); `comms.outbound_message` (channel CHECK `sms`/`email`/`push`/`web_push` - note it lacks
  `in_app` while templates have it; message_kind notification/reminder/broadcast; template_id, template_params,
  status, provider_response, scheduled_at, sent_at); `comms.message_receipt`; notification types, rules, cohort
  settings and preference sets; `comms.SendMessage` (`POST /v1/comms/outbound-messages:send`) answers 501 (BE-1);
  ARC-2 plans the relay with provider adapters and several provider accounts per channel (S10). Consent:
  `people.consent_record.purpose` already has `sms_whatsapp_messages`, and the privacy design (§8) defines activity
  `comms.whatsapp_optional` (consent, checked at channel selection per recipient) next to `comms.operational_notice`
  (legitimate use). IN-6 plans a WhatsApp Business bridge for request intake.
- **Gaps.** No `whatsapp` value in either CHECK; no per-tenant provider account (business account id, phone number
  id, token); no template category, language or approval status; no inbound webhook; no 24-hour window tracking; no
  per-number opt-in and opt-out; no cost metering per tenant.
- **Scope (schema `comms`).** Add `whatsapp` to both channel CHECKs (and fix the `in_app` mismatch);
  `channel_account` (institution_id or tenant-wide, channel, provider meta_cloud or a named BSP, business_account_id,
  phone_number_id, display_name, credential_enc under SEC-4, quality_rating, messaging_limit, status) - the same table
  ARC-2 needs for S10; `message_template` gains locale (`en`, `hi` as separate templates), category
  (utility/marketing/authentication), approval_status (draft/submitted/approved/rejected/paused/disabled),
  rejection_reason, parameter map, last_synced_at; `contact_channel_status` (person_id, channel, address_e164,
  opted_in_at, opt_in_source app/paper/form/whatsapp_reply, opted_out_at, last_inbound_at, window_expires_at);
  `inbound_message` (channel_account_id, from, body, media file_object_id, received_at, linked parent request or
  enquiry); `outbound_message` gains provider_message_id, pricing_category, billable, cost.
- **Routing.** Per notification type an ordered channel list, for example push -> WhatsApp -> SMS when push is not
  read within N minutes; WhatsApp only if the recipient opted in and the template is approved; free text only inside
  an open 24-hour window; marketing-category templates only with `marketing` consent
  (`comms.marketing_broadcast`). Content with marks, health or counselling is sent as a link to the app by default.
- **Platform facts (sourced).** Meta charges per delivered template message since 1 July 2025; template categories
  are marketing, utility and authentication; utility templates and non-template messages sent inside an open 24-hour
  customer service window are free, and free text can only be sent while that window is open. Reported India rates per
  message: marketing ₹0.86, utility and authentication ₹0.13, service free (a BSP's help page; check Meta's rate card at
  build time, **to verify**). INR billing for India started 1 January 2026; non-INR accounts must migrate by
  31 December 2026; rate changes take effect only on the first day of a quarter. Templates must be approved before use
  (review up to 24 hours), have one language each, can be paused or disabled after negative feedback; 250 templates
  for an unverified business, up to 6,000 when verified; messaging limits cap business-initiated sends outside
  windows. Opt-in must name the business and say the person agrees to receive its messages on WhatsApp; any method
  is allowed (app, website, IVR, signed paper form); opt-outs must be honoured. Per-number throughput and the Tech
  Provider or embedded-signup onboarding flow are **to verify**.
- **Provider choice.** Direct Meta Cloud API with MySMS as technology provider and one business account and number
  per school, versus a Business Solution Provider (BSP) that adds a margin but handles onboarding and support.
  Either way behind the ARC-2 adapter interface so the choice can change.
- **Operations.** `comms.ChannelAccount` CRUD and `POST /v1/comms/channel-accounts/{channelAccountId}:verify`;
  `POST /v1/comms/message-templates/{messageTemplateId}:submitForApproval`, `POST /v1/comms/message-templates:syncStatus`;
  `POST /v1/comms/whatsapp/webhook` (public route, provider signature checked, writes to `inbound_message` and
  status updates, processed async); `POST /v1/comms/contact-channel-statuses:optIn` and `:optOut` (own, guardian app;
  office with paper evidence); `GET /v1/comms/messaging-usage` (count and cost by category and month). Permissions
  `comms.channel_account.manage`, `comms.message_template.submit`, `comms.contact_channel_status.update_own`,
  `comms.contact_channel_status.manage`, `comms.messaging_usage.read`. Events `comms.outbound_message.delivered`,
  `.read`, `.failed`, `comms.inbound_message.received` (IN-6, CP-1), `comms.message_template.status_changed`,
  `comms.contact_channel_status.opted_out`.
- **Screens.** COM-13 notification setup gains WhatsApp account, template library with approval status and en/hi
  pairs (W); COM-14 messaging usage and cost (W); COM-06 broadcast shows estimated cost and opted-in reach before
  sending (W); replies inbox shared with IN-6 (W T); P settings "Messages on WhatsApp" with number and opt-out.
- **Privacy and security.** Opt-in recorded with evidence in the privacy centre (SEC-11), separate from marketing
  consent; Meta (and any BSP) recorded as processor in the RoPA; never teachers' personal numbers; no one-to-one
  teacher-to-minor WhatsApp chat (safeguarding, owner to confirm); webhook signature verification and secret rotation;
  tokens encrypted (SEC-4); rate limits (SEC-5).
- **Performance.** Morning bursts (absence notices across all schools within minutes): queue per channel account with
  throughput and messaging-limit awareness, idempotent sends keyed by outbound message id, webhook ingestion
  write-only then async processing; delivery and failure gauges in OPS-1.
- **Cost model.** Usage is metered per tenant; pass-through with margin, bundled quota per plan, or the school's own
  provider account (owner decision; affects the FUNDING-AND-CLOUD-COST estimate,
  [docs/product/FUNDING-AND-CLOUD-COST-ESTIMATE.md](docs/product/FUNDING-AND-CLOUD-COST-ESTIMATE.md)).
- **Depends on.** ARC-2 and the comms BE-1 operations, SEC-11, SEC-4, SEC-5; generalises the IN-6 bridge (IN-6 then
  only consumes `comms.inbound_message.received`). **Priority P1 / M.**
- **Open questions.** School-owned phones per class, or the teacher's own phone? Two-way chat with class teachers allowed for parents? Which notification types default to
  WhatsApp?

#### CP-4 School website CMS

- **Problem and users.** Almost every school needs a public website, and CBSE-affiliated schools must publish
  mandatory disclosures on it: fee structure, academic calendar, SMC and PTA details, three years of class 10 and 12
  results, pupil-teacher ratio, teacher strength and qualifications, affiliation and recognition documents, building,
  fire and sanitation certificates and infrastructure details; CBSE set 15 February 2026 as a deadline and treats
  non-compliance as a breach of affiliation conditions (as reported by Careers360). Sites are often stale and built by
  local vendors, and the data is re-typed from the ERP. Edunext advertises a dynamic school website with its own CMS
  (circulars, pages, galleries, notices, downloads). Users: office admin and a communications teacher edit; the
  principal approves; the public, applicants and parents read.
- **Exists today.** `platform.tenant_domain` (host, is_primary, is_verified, verified_at; tenant-facing or internal is
  DEC-10); `platform.theme` and `platform.theme_token` (ADM-07 branding); `platform.file_object` and
  `platform.upload_policy`; comms notices and events; `comms.page_category` and `comms.class_page` (internal class
  pages, not public); `people.public_profile` (consent-gated); admission cycles and the public form ADMN-05; IN-10 cost
  estimate with a public read; finance fee structures; `org.academic_year` and `org.holiday` for the calendar; hr staff
  with qualifications.
- **Gaps.** No public rendering, pages, menus, galleries, news, downloads, SEO, public forms, publishing workflow or
  disclosure checklist.
- **Scope (new schema `website`).** `site` (institution_id or a group root, default locale, theme_id, domain ids,
  analytics id, status); `page` (slug, title and body per locale en/hi, layout, blocks JSON - heading, rich text,
  image, gallery, notice list, events, downloads, contact, map, CP-1 enquiry form, admission call to action, IN-10 fee
  estimate, disclosure table - status, publish_at, SEO fields); `page_revision`; `menu` and `menu_item`;
  `media_album` and `media_item` (file_object_id, alt text, subject person ids for consent checks); `news_post`;
  `download` (circular PDFs); `form` and `form_submission` (contact form); `disclosure_item` (CBSE checklist code,
  source auto or manual, value, document file, last_reviewed_at). Live blocks read public projections (notices marked
  public, events, results summary, consenting staff fields, fee structure) - never tenant tables directly.
- **State machine.** page draft -> in_review -> published -> archived; a published page edited creates a draft
  revision; scheduled publish at `publish_at`; unpublish removes it from the next build.
- **Rendering and hosting.** Static generation on publish into object storage behind a CDN, so public traffic never
  reaches the API or database; default subdomain per school on a MySMS domain (**to verify** name) and custom domains
  with DNS TXT verification and managed TLS certificates (provider limits and cost **to verify**); sitemap, robots,
  Open Graph; accessibility target WCAG 2.1 AA (GIGW for government-aided schools **to verify**).
- **Operations.** `website.Site` and `website.Page` CRUD; `POST /v1/website/pages/{pageId}:submitForReview`,
  `:publish`, `:unpublish`, `:revert`; `POST /v1/website/sites/{siteId}:rebuild` (202, ARC-4);
  `POST /v1/website/sites/{siteId}/domains/{domainId}:verify`; public `POST /v1/website/public/forms/{formId}:submit`
  (captcha, SEC-5); `GET /v1/website/sites/{siteId}/disclosure-checklist`. Permissions `website.site.manage`,
  `website.page.edit`, `website.page.publish`, `website.media.upload`, `website.form_submission.read`. Events
  `website.page.published`, `website.site.rebuilt`; consumes public notice publication, `admission` cycle opening, fee
  structure changes and `privacy` consent withdrawals (rebuild affected pages).
- **Screens.** WEB-01 site settings and domains (W); WEB-02 pages and menu with block editor and preview (W, desktop);
  WEB-03 media library and albums with a consent badge per photo (W); WEB-04 CBSE disclosure checklist (W); WEB-05
  form submissions (W). The public site is responsive and bilingual.
- **Privacy and security.** Student photos on a public site need `photo_video_publication` consent per child (extend
  the privacy design's `comms.community_photo_publication` with a `website` activity); names next to photos need
  `community_naming`; on withdrawal the image is removed and the site rebuilt within an agreed time; staff fields
  required by CBSE may rest on legal obligation (**to verify**), others need consent. Rich text sanitised, strict CSP,
  no custom JavaScript by default, domain takeover checks when a domain is removed.
- **Performance.** Static output on a CDN; rebuilds are background jobs; no per-visit database load.
- **Depends on.** DEC-10 (`platform.tenant_domain`), P1-28, ARC-4, SEC-11, CP-1 (forms), IN-10. **Priority P3 / L.**
- **Parked by the owner; approach to be decided.** Requirements so far: lightweight; ready-made themes the platform
  team uploads; no drag-and-drop builder or editor UI; integrates with the Spring/Java stack. Options already reviewed
  and not chosen: WordPress Multisite, Ghost, Halo, UJCMS, PublicCMS, Crafter CMS, OpenCms, Apache Roller, JPress,
  JBake, Hugo, Astro, and a hand-built Thymeleaf module with Bootstrap themes.
- **Open questions.** How many themes? Custom HTML or scripts allowed?
  Import of existing sites? One site per school or a group site with campus sub-sites? E-mail hosting stays out of
  scope?

#### CP-5 School store with online orders

- **Problem and users.** At the start of the year parents queue for uniform sets, book kits and stationery; sizes
  run out and the office handles cash. Schools want online pre-orders with payment and a pickup slot (or delivery),
  and the store keeper wants to know demand by size before ordering. The store must stay optional for parents: CBSE
  warned schools in 2017 against coercing parents to buy books, uniforms and stationery only from the school or
  designated vendors, citing Rule 19.1(ii) of the affiliation bye-laws (as reported by Deccan Herald); state rules
  **to verify**. Fedena's inventory page covers internal stores, indents, purchase orders, GRNs and suppliers, with no
  parent sales mentioned.
- **Exists today.** Only the plan: NM-4 proposes `inventory.item` (sku, size and variant, hsn_code, gst_rate,
  sale_price), `item_kit`, `stock_level`, `stock_movement`, `sale_order` (draft -> confirmed -> paid -> fulfilled,
  creating a finance invoice), INV-04 counter sale and INV-05 parent shop. finance has `finance.invoice`
  (invoice_type invoice/credit_note, student_id, bill_to_user_id, tax_total, status), `finance.invoice_line`,
  `finance.payment`, `finance.payment_allocation`, `finance.payment_gateway_account` (institution_id, gateway_name,
  merchant_id, key_secret_enc) and `finance.gateway_event`, and FIN-07 "Pay fees" (`finance.QuoteCheckout`). IN-12
  plans an exchange for used books and uniforms.
- **Scope.** CP-5 is the parent-facing commerce layer on NM-4, not a second stock system (schema `inventory`):
  `store_catalogue` (institution_id, opens_on, closes_on, pickup locations, notice text); `catalogue_item` (item_id,
  images, description en/hi, grade_level_ids, gender variant, max_qty, is_optional - always true unless the owner
  decides otherwise); `size_chart`; `cart` (guardian user, student_id, lines, expires_at); `sale_order` gains channel
  online/counter, fulfilment pickup/delivery, pickup_slot_id, delivery address (encrypted); `pickup_slot` (date, time,
  capacity); `return_request` (sale order line, reason size_exchange/defect/other, status). Invoices for goods carry
  HSN and GST lines and are separate documents from fee receipts (GST rates for uniforms and books and the need for a
  separate GSTIN when a vendor or trust runs the store **to verify** with a CA).
- **State machine.** sale order placed (stock reserved for 30 minutes) -> paid (on `finance.payment.received`) ->
  ready_for_pickup or dispatched -> fulfilled; unpaid -> expired (stock released); cancel allowed before ready;
  return request requested -> approved -> received -> refunded (credit note) or rejected.
- **Operations.** `POST /v1/inventory/carts/{cartId}:checkout` (reserves stock, creates the finance invoice and a
  gateway order through the same flow as FIN-07, idempotent); `POST /v1/inventory/sale-orders/{saleOrderId}:markReady`,
  `:fulfil` (scan of the pickup QR), `:cancel`; `POST /v1/inventory/return-requests/{returnRequestId}:approve`,
  `:refund`; `GET /v1/inventory/store-catalogues/{storeCatalogueId}/items` (guardian, filtered to own children's
  grades); `GET /v1/inventory/demand-report` (pre-orders by item and size). Permissions
  `inventory.store_catalogue.manage`, `inventory.cart.create_own`, `inventory.sale_order.fulfil`,
  `inventory.return_request.approve`. Events `inventory.sale_order.placed`, `.paid`, `.ready`, `.fulfilled`,
  `.cancelled` (comms notices, CP-3); consumes `finance.payment.received` and gateway failures.
- **Screens.** STORE-01 shop per child with "add the class kit" and size picker (P W); STORE-02 cart and checkout (P W);
  STORE-03 my orders with pickup QR (P); STORE-04 pickup desk and counter sale (W B, merges with INV-04); STORE-05
  catalogue and slots setup (W); STORE-06 sales and demand report (W).
- **Privacy and security.** Low: purchase history is the guardian's financial data; delivery addresses encrypted; no
  vendor marketing to parents without `marketing` consent; payment data never stored (gateway tokens only).
- **Performance.** Season-start spike: reservation with `SELECT ... FOR UPDATE` on `stock_level` rows, idempotency
  keys on checkout, confirmations queued; peak a few thousand orders per school in a week.
- **Depends on.** NM-4 (owner reversing D5 (review)), finance gateway flow (FIN-07), ARC-2, CP-3; links to IN-12 ("buy
  used instead"). **Priority P3 / M.**
- **Owner decision:** school-run store only; outside vendors cannot sell or fulfil through it.
- **Open questions.** Pickup only or delivery?
  Separate merchant account and GSTIN for the store? Wallet (NM-3) as a payment method?

#### CP-6 ID card generator

- **Problem and users.** Every year the office sends student and staff data with photos to a printing vendor in
  spreadsheets and chat messages; errors, missing photos and privacy leaks follow. Cards carry photo, name, class or
  designation, admission or employee number, validity, sometimes blood group, guardian phone and bus route, and a code
  used by the gate, library and bus. Fedena's ID card generator offers separate student and employee templates, system
  and custom fields through field codes, one or two sides, background images, bulk or single generation and A4 or
  card-size output (QR not mentioned).
- **Exists today.** `people.person.photo_file_id` (students and staff are both persons), `platform.file_object`;
  `sis.student` (admission_number, status) and `hr.staff` (employee_code, designation_id); `library.library_card` and
  `library.library_card_type`; FO-01 visitor log notes "badge print"; `admission.application.blood_group`; transport
  route assignments; `sis.IssueBonafideCertificate`. NM-9 plans numbered, verifiable documents from templates and
  NM-6 plans authorised pickup persons with photos. The privacy design's `photo_video_publication` covers publication,
  not an ID photo.
- **Gaps.** No card template, card register, numbering, lost and reissue flow, bulk photo capture, print layout or
  verification endpoint.
- **Scope (schema `people`, renderer shared with NM-9).** `id_card_template` (subject_kind student/staff/
  guardian_pickup/visitor, size cr80_portrait/cr80_landscape/a4_n_up, front and back layout JSON with text fields,
  images, QR or barcode element, bleed and safe area, fonts including Devanagari); `id_card_batch` (template_id,
  filter by class sections or departments, status draft/rendering/ready/printed/delivered, output file_object_id,
  card_count, missing_photo_count); `id_card` (person_id, card_number, issued_at, valid_until, status
  active/lost/replaced/revoked, qr_token, print_count, batch_id, replaced_by_id). Photo capture: bulk upload of a zip
  named by admission or employee number, and in-app capture by the class teacher with 3:4 crop and approval.
- **Print facts.** ISO/IEC 7810 ID-1 is 85.60 mm x 53.98 mm x 0.76 mm, the same size as CR80. Output: print-ready PDF
  with bleed and crop marks, n-up on A4 for PVC sheet printing, or one card per page for direct-to-card printers
  (resolution and printer drivers **to verify**; see
  [docs/product/HARDWARE-INTEGRATION-REQUIREMENTS-DRAFT.md](docs/product/HARDWARE-INTEGRATION-REQUIREMENTS-DRAFT.md)).
  Hindi names need correct Devanagari shaping in the PDF engine (library choice and licence **to verify**; HTML to PDF
  with a shaping engine may be needed).
- **Verification.** The QR holds an opaque signed token, no personal data; `GET /v1/people/public/id-cards/{token}:verify`
  answers valid or invalid publicly and shows name and photo only to signed-in staff with `people.id_card.verify`;
  used by NM-6 gate, library issue, NM-5 boarding scan and attendance scan. A revoked or replaced card fails at once.
- **Operations.** `people.IdCardTemplate` CRUD; `POST /v1/people/id-card-batches` and
  `POST /v1/people/id-card-batches/{idCardBatchId}:render` (202, ARC-4), `:markPrinted`;
  `POST /v1/people/id-cards/{idCardId}:reportLost`, `:reissue`, `:revoke`; `POST /v1/people/persons:importPhotos`.
  Permissions `people.id_card_template.manage`, `people.id_card.issue`, `people.id_card.verify`,
  `people.photo.capture`. Events `people.id_card.issued`, `people.id_card.revoked` (gate and library deny the old
  card), `people.person.photo_changed`.
- **Screens.** IDC-01 template designer with sample preview (W); IDC-02 batch print with missing-photo list and PDF
  download (W); IDC-03 photo capture by class (T B); IDC-04 card register, lost and reissue (W); digital ID card with
  QR, cached offline (S P T).
- **Privacy and security.** Print the minimum by default (no home address; guardian phone and blood group off unless
  the school turns them on, blood group being health data shown with a notice); the ID photo rests on legitimate use
  (**to verify**) and never implies publication consent; vendors are processors and get the PDF through an expiring
  link, not chat; card images are kept only as long as the card is valid.
- **Performance.** A 3,000-card batch is a large PDF: render on a worker as a background job, stream to object storage,
  never inside a request.
- **Depends on.** ARC-4, NM-9 (shared template and render engine), SEC-11; used by NM-6, NM-5, library cards.
  **Priority P2 / M.**
- **Owner decision:** cards carry **both** a QR code (verification link) and an RFID chip (gate, attendance and library
  readers, per the hardware draft); the RFID UID is stored against the card and revoked with it.
- **Open questions.** RFID standard and frequency (with the hardware draft)? Digital-only cards allowed? Library card number unified
  with the ID card number? Who pays for printing? Principal's signature image on cards?

#### CP-7 Management dashboards

- **Problem and users.** The principal, management (trustees, directors) and school-group heads want one view each
  morning: attendance today by class and staff, fee collection against target with defaulters by age, admissions
  funnel, academic trends, staff leave and vacancies, and alerts. Groups want to compare campuses. Fedena sells
  custom reports; dashboards are a standard demo item for Indian ERPs.
- **Exists today.** ADM-01 admin dashboard (counts, pending approvals, jobs from composed queries);
  `attendance.SummarizeAttendance` (`GET /v1/attendance/summaries`, by register kind under DEC-7);
  `platform.ListReportDefinitions` and `platform.RunReport` (`GET /v1/platform/reports`, `POST /v1/platform/reports:run`);
  `lms.GetStudentDashboard` and `lms.GetTeacherDashboard`; source data in finance (invoices, payments, fee dues),
  admission (application status), assessment (results), hr (staff, leave); `org.institution` parent tree for groups.
  PERF-4 plans reports off the primary and an event-fed reporting projection; §5.2 lists dashboards as read models
  without a table.
- **Gaps.** No cross-module aggregation, no history or snapshots, no targets, no alerts, no group roll-up, no
  management persona or role, no phone view for the principal.
- **Approach.** No live joins or gRPC fan-out across modules at view time. A new read-model module `insights`
  (schema `insights`) consumes domain events into pre-aggregated daily facts: `fact_attendance_daily` (institution,
  date, class_section_id, grade, present/absent/late/on_leave, staff present/absent); `fact_fee_daily` (institution,
  date, fee head, billed, collected, outstanding by age 0-30/31-60/61-90/90+, payment mode); `fact_admission_daily`
  (cycle, stage counts and conversions by source from CP-1 and applications); `fact_assessment_term` (exam, class,
  subject, mean, pass rate, grade bands); `fact_staff_daily` (present, on leave, vacancies, cover from NM-7);
  `kpi_target` (metric, institution, period, target); `alert_rule` (metric, threshold, recipients, channel);
  `dashboard_layout` per role. A nightly reconciliation job recomputes the last N days from the owning modules so a
  missed event heals; metric definitions live in one glossary (for example whether approved leave counts as absent).
- **Operations.** `GET /v1/insights/dashboards/{dashboardCode}` (institution or group, date range, group-by);
  `GET /v1/insights/metrics/{metricCode}:series`; `GET /v1/insights/metrics/{metricCode}:drillDown` (returns ids; the
  client then reads details from the owning module under that module's permissions); `kpi-targets` and `alert-rules`
  CRUD; `POST /v1/insights/exports` (XLSX or PDF, ARC-4); `POST /v1/insights/reconciliations:run` (platform).
  Permissions `insights.dashboard.read_institution`, `insights.dashboard.read_group`, `insights.kpi_target.manage`,
  `insights.alert_rule.manage`, `insights.export.create`. Events consumed: attendance submissions, invoice posting and
  payments, admission and enquiry transitions, result publication, leave approvals (exact names from each module's
  event catalogue); emitted `insights.alert.triggered` (comms, CP-3 digest).
- **Screens.** DASH-01 principal today (W T B); DASH-02 fees and collections (W); DASH-03 admissions funnel (W, CP-1);
  DASH-04 academics (W); DASH-05 staff (W); DASH-06 group overview comparing institutions (W); daily digest by
  e-mail or WhatsApp. Every chart has a table alternative, en/hi labels and Indian digit grouping (lakh, crore).
- **Privacy and security.** Facts hold counts and ids only, no names; small cells (under 5 students) suppressed at
  group level; health and counselling never aggregated per class; learning-analytics figures follow the privacy
  design's `lms.learning_analytics` rule; group-level access needs the multi-institution authority model
  (D1 (review), P1-27) and the role-based permissions (JAVA-CODING-RULES §16.2).
- **Performance.** Facts are small (institutions x days x classes); target p95 under 500 ms (PERF-1 budgets); read
  from the replica (PERF-4); tile cache of a few minutes (PERF-2); event lag and reconciliation drift as OPS-1
  metrics; monthly partitions for facts (PERF-5).
- **Depends on.** D12/ARC-5 (event payloads and ordering), PERF-4, PERF-2, OPS-1, D1 (review)/P1-27 for
  groups, DEC-7, CP-1 for admissions. **Priority P1 / L**, delivered in slices: attendance and fees first.
- **Owner decision:** campuses are never ranked against each other; group views show each campus against its own
  targets and history only.
- **Open questions.** Which ten KPIs in v1? Who sets targets?
  Is minutes-old data enough, or must attendance be live? Should parents or teachers get any of these views?

**Sources for §3.12** (checked 2026-11-18):
- Meta, WhatsApp Groups API: https://developers.facebook.com/documentation/business-messaging/whatsapp/groups/
- Kapso, state of the WhatsApp Groups API in 2026: https://kapso.com/blog/whatsapp-groups-api-state-2026
- Timefold Solver 1.x Java prerequisites (JDK 17+): https://docs.timefold.ai/timefold-solver/1.x/quickstart/shared/java-prerequisites

- Meta, Pricing on the WhatsApp Business Platform: https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing
- Meta, Get opt-in for WhatsApp: https://developers.facebook.com/documentation/business-messaging/whatsapp/getting-opt-in
- Meta, Message templates overview: https://developers.facebook.com/documentation/business-messaging/whatsapp/templates/overview
- MyOperator (India rates per category): https://support.myoperator.com/portal/en/kb/articles/whatsapp-has-shifted-to-per-message-pricing-effective-july-1-2025-based-on-message-categories-and-your-recipient-s-country-in-india-there-are-three-paid-categories
- Timefold Solver Spring Boot quickstart: https://docs.timefold.ai/timefold-solver/latest/quickstart/spring-boot/spring-boot-quickstart
- Timefold Solver 2.0 release (licence, 1.x maintenance): https://timefold.ai/blog/timefold-solver-2.0-release
- ISO/IEC 7810 ID-1 size (Wikipedia talk page quoting the standard): https://en.wikipedia.org/wiki/Talk:ISO/IEC_7810
- Fedena add-on modules: https://fedena.com/feature-tour/addon-modules
- Fedena ID card generator: https://fedena.com/feature-tour/school-id-card-generator
- Fedena inventory: https://fedena.com/feature-tour/school-inventory-management-system
- Vidyalaya AI timetable generator: https://www.vidyalayaschoolsoftware.com/ai-timetable-generator-for-school
- PowerSchool, build the school schedule: https://support.powerschool.com/help/sms/800/districtuser/Content/Topics/Scheduling/Buildtheschoolschedule.htm
- MyClassboard lead management: https://www.myclassboard.com/?p=2709
- Edunext feature listing (Techjockey): https://www.techjockey.com/detail/edunext-school-erp
- Careers360, CBSE public disclosure deadline: https://news.careers360.com/cbse-schools-class-10-12-results-fees-student-teacher-details-online-by-february-15-2026-affiliation-rules-public-disclosures/amp
- Deccan Herald, CBSE warning on coercive sales: https://www.deccanherald.com/india/cbse-warn-pvt-schools-coercing-2000851

### 3.13 GegoK12 parity (owner benchmark)

The owner set GegoK12 (MIT-licensed Laravel school ERP, gegok12.com) as the benchmark: MySMS must have what it has.
Full feature inventory (191 features, G001–G191), evidence and the module-by-module comparison:
[docs/benchmark/GEGOK12-COMPARISON.md](docs/benchmark/GEGOK12-COMPARISON.md). Rows below are the gaps, in parity order;
where a gap is already an item elsewhere, the row raises or extends that item rather than duplicating it.

| ID | Pri | Gap and what to build | Today / evidence |
|---|---|---|---|
| GK-1 | P0 | **Message delivery: SMS, e-mail, push** (MSG91/Twilio-class SMS, SMTP, FCM/APNs) and the 33 comms 501s — Build ARC-2 in the §5.1 order. Ship at least one Indian SMS provider (MSG91 or similar, DLT templates), SMTP and FCM. It unblocks absence alerts, fee reminders, password-reset links, homework and notice pushes. | Today: ARC-2 planned; tables exist; `SendMessage` 501. GegoK12: G140, G141, G045, G083, G095, G152, G110, G120, G129, G151 |
| GK-2 | P0 | **In-app notifications inbox** (list, mark read, mark all read) — Part of ARC-2 but can ship first: the bell icon is on every GegoK12 screen. | Today: 501. GegoK12: G149, G176 |
| GK-3 | P0 | **Library return** (and fines, return reminders, lost) — A library that cannot take a book back fails a demo. Small job. | Today: `ReturnLoan`, `InvoiceLoanFine`, `RequestLoanReturn`, `MarkLoanLost` 501. GegoK12: G127, G128, G129 |
| GK-4 | P0 | **Calendar view** (events + holidays + exams per user) — Every GegoK12 portal and app has `/events`. | Today: `GetMyCalendar`, `ExportCalendar` 501. GegoK12: G145 |
| GK-5 | P1 | **Account management: income and expense cash book** — GegoK12 gives it free. Add expense heads, expense vouchers, income from non-fee sources, day book and simple income-vs-expense report to `finance`. Also feeds IN-3. | Today: Missing (finance is fee income only). GegoK12: G123 |
| GK-6 | P1 | **ID cards and bus passes** for students and staff — (raise from P2): free in GegoK12; reuse the NM-9 renderer. | Today: CP-6 (P2). GegoK12: G054, G055, G056 |
| GK-7 | P1 | **Certificates** (TC, bonafide, conduct, character, participation, merit) with templates, numbering, bulk, PDF, log — TC is legally needed in India at every exit. | Today: bonafide only; NM-9 (P2). GegoK12: G168 |
| GK-8 | P1 | **Birthdays, greetings and work anniversaries** on dashboards — Cheap and visible; add `ListWorkAnniversaries`. | Today: `ListBirthdays`, `SendGreetings` 501; anniversaries missing. GegoK12: G033, G034 |
| GK-9 | P1 | **Feed, class-wall reactions and follow** — | Today: `GetFeed`, reactions, `FollowClassPage`, `SubmitPost` 501. GegoK12: G039, G146 |
| GK-10 | P1 | **Small 501s GegoK12 has working**: `org.ActivateAcademicYear`, `comms.AddGroupMembers`, `frontoffice.CheckOutVisitor`, `comms.MarkConversationRead`, `comms.ClaimTask`, `comms.MarkFeedbackMessage` — Finish BE-1. | Today: 501. GegoK12: G009, G023, G133, G147, G109, G150 |
| GK-11 | P1 | **Hall tickets / admit cards** and **report-card PDF** — Add `GenerateHallTickets` on `exam_session`; confirm `result_template` renders a branded PDF. | Today: hall ticket missing; report card PDF to verify. GegoK12: G093, G092 |
| GK-12 | P1 | **School setup wizard, demo tenant, one-command install** — See §6.2. A guided first-run checklist plus a seeded demo tenant (`ClearDemoData` already exists) is how GegoK12 sells. | Today: Missing. GegoK12: G001, G002, G003 |
| GK-13 | P2 | **Automatic timetable generator** — (Pro in GegoK12, $200). | Today: CP-2 (P2); `GenerateSessions` 501; TT-02 not built. GegoK12: G099, G100, G101 |
| GK-14 | P2 | **Transport fleet, GPS, boarding, driver and conductor apps** — (Pro in GegoK12, $200). Keep NM-5 but add conductor role and driver odometer log. | Today: NM-5 (P2). GegoK12: G156-G162 |
| GK-15 | P2 | **Inventory and stock** (assets with QR, stock, purchases, sales, vendors, Stock Keeper role) — GegoK12 sells it ($100 + $100); reconsider D5. | Today: NM-4 (P3); D5 says not in v1. GegoK12: G163, G164, G165 |
| GK-16 | P2 | Question paper generator (printable paper from the LMS question bank) — a "print paper" builder over `lms.question_pool`. | Today: Partial. GegoK12: G096 |
| GK-17 | P2 | Gate pass, appointments, host alert at visitor check-in — add general appointments to NM-11. | Today: NM-6, NM-11. GegoK12: G137, G138, G139 |
| GK-18 | P2 | Alumni module — give it a ROADMAP id. | Today: docs/product only. GegoK12: G169, G058 |
| GK-19 | P3 | Facility maintenance work orders, incidents — add to `campus` (Plan A) or NM-4. | Today: Missing. GegoK12: G167 |
| GK-20 | P2 | Add-on catalogue / per-module entitlement on plans — needed for any core/Pro packaging (§6.3). | Today: Missing (to verify). GegoK12: G006 |
| GK-21 | P0 | Role templates for GegoK12's 13 personas and 7 functional roles, on the role-based permissions (JAVA-CODING-RULES §16.2). | Today: role-based permissions built; no templates. GegoK12: G029-G031 |
| GK-22 | P2 | reCAPTCHA (or equivalent) on public admission form and sign-in — . | Today: Missing. GegoK12: G017 |
| GK-23 | P2 | Teacher/parent app connect by QR code — QR encodes tenant code + invite token. | Today: Missing. GegoK12: G190 |
| GK-24 | P1 | Phone screens for parent/teacher flows GegoK12's Android apps already have (attendance taking, homework submit, leave apply, notices, calendar, lesson plans, teacher list, approvals) — audit `clients/apps/mobile` against the G170-G191 list. | Today: many "to verify". GegoK12: G171-G189 |

## 4. Item counts by area

Open work rows (some rows group several source items): Architecture 9, Backend/API 22, Data/DB 18, Security 12,
Performance 5, Build/CI/Testing 8, Operations 4, Clients/UI 12, Docs 4, New modules
and feature gaps 12 (106 rows, plus 13 differentiating features in §3.11, 7 competitive parity features in §3.12 and 24 GegoK12 parity gaps in §3.13 and 7 competitive parity features in
§3.12). Decisions: 32 owner questions plus
8 spec-vs-contract confirmations.

---

## 5. Next chunk of work

### 5.1 Ordered sequence

Phase 0 is needed before any pilot (about 2-4 weeks); phase 1 is architecture debt (about 1-2 months); phase 2 is
scale and product (a quarter). Feature work continues alongside, one feature at a time; rows 22-29 order the new
modules of section 3.10 on that feature track, each starting once its listed dependencies are done; rows with a
letter suffix (21a-29b) place the competitive parity features of section 3.12 on the same track.

| # | Phase | Work | Items |
|---|---|---|---|
| 0 | next | Privacy and Consent Centre, phase by phase as in its design | SEC-11 |
| 1 | 0 | CI pipeline: backend reactor and tests, `pnpm verify`, clean Flyway + `smoke.sql`, migration-upgrade rehearsal, images | BLD-1, DB-4 |
| 2 | 0 | Connection budget: shared or small pools, worker mode for relays and jobs, PgBouncer, `max_connections` formula | D9, OPS-3 |
| 3 | 0 | Backup and DR: PITR, dump role, restore drill, RPO/RTO | OPS-2 |
| 4 | 0 | Observability: Prometheus, OTel, outbox and pool gauges, `pg_stat_statements`, client Sentry | OPS-1 |
| 5 | 0 | Web session persistence through an httpOnly refresh cookie | D13 |
| 6 | 0 | Migrations out of runtime; app holds only `mysms_app` | D8, SEC-1 |
| 7 | 0 | Small open fixes: platform-staff idempotency, `mustResetPassword` enforcement, RLS `NULLIF`, partial uniques on master | BE-5, SEC-3, DB-2, DB-1 |
| 8 | 1 | Decide modular-monolith-first; in-process calls in the monolith profile, shared stub artifacts, mTLS when a service is extracted | D1, DEC-1, D2, D11 |
| 9 | 1 | Batch card reads and reference reconciliation | P1-34, ARC-1 |
| 10 | 1 | Cut boilerplate: core step helpers (scope check, ETag check, field errors), record DTOs, split the three god services | BE-2, BE-4 |
| 11 | 1 | Test depth: wrong-tenant test per module, service unit tests, Playwright and Maestro smoke, coverage including `api/**` | SEC-2, BLD-4, BLD-5 |
| 12 | 1 | Replica reads opt-in; transaction-manager edge cases | D5, BE-3 |
| 13 | 1 | Freeze baselines at the first pilot; generated counts in docs | D6, E61 |
| 14 | 1 | Message delivery (comms relay) and the comms 501 operations | ARC-2, BE-1 |
| 15 | 1 | Persona operations and the teacher phone app (ATT-03 with offline queue) | ARC-3, E59 |
| 16 | 2 | `pg_trgm` search, `pg_partman`/`pg_cron`, index pruning from real usage | D7, P2-11, P2-01 |
| 17 | 2 | Event-ordering contract, relay leader, LISTEN/NOTIFY | D12, ARC-5 |
| 18 | 2 | Offline outbox for mobile and tablet, starting with attendance | UI-2, P1-35, P2-05 |
| 19 | 2 | Tenant export, erasure, archival; tenant placement design | ARC-6, DEC-2 |
| 20 | 2 | Caching for catalogues and permissions; reporting and search projection | PERF-2, PERF-4, C-8 |
| 21 | ongoing | Remaining screens and contract-scale API items (P1-24...P1-33, before the SDK is frozen) | UI-1, section 3.2 |
| 21a | feature, with 1 | WhatsApp channel on the comms relay: provider account, templates with approval status, opt-in, SMS fallback | CP-3; after ARC-2, SEC-11 |
| 21b | feature, with 1 | Management dashboards, first slice: attendance and fee tiles from the `insights` read model | CP-7; after D12/ARC-5, PERF-4, OPS-1 |
| 22 | feature, with 1 | Health alerts on rosters, conditions and nurse visits (medication and check-ups after) | NM-1; after ARC-3, BE-1 consent |
| 23 | feature, with 1 | Pickup safety: authorised pickups, gate passes with OTP, late and early register | NM-6; after ARC-2 |
| 24 | feature, with 1 | Substitution and cover from approved leave | NM-7; after `GenerateSessions` (BE-1), DEC-3 |
| 25 | feature, with 2 | Certificates and documents register, TC, public verify (DigiLocker later) | NM-9; after ARC-4 |
| 25a | feature, with 2 | Admission enquiry CRM: enquiries, follow-ups, counsellors, funnel, convert to application | CP-1; after DEC-5, ARC-2 |
| 25b | feature, with 2 | ID cards for students and staff on the NM-9 renderer, QR verify | CP-6; after NM-9 engine, ARC-4 |
| 26 | feature, with 2 | Parent-teacher meeting booking | NM-11; after ARC-2, ARC-3 |
| 27 | feature, with 2 | Transport compliance first, then trips, boarding scan, live ETA | NM-5; after D7/P2-11, UI-2 |
| 27a | feature, with 2 | Automatic timetable generator (versions, constraints, solver run, publish) | CP-2; after P2-03, `GenerateSessions` (BE-1), D10 |
| 28 | feature, later | Staff appraisal and CPD; clubs, sports and houses; scholarships | NM-12, NM-8, NM-10 |
| 29 | feature, later | Campus operations: inventory and procurement, canteen and wallet, hostel | NM-4, NM-3, NM-2; D5 (review) |
| 29a | feature, later | School store: online orders, payment, pickup (after the inventory module) | CP-5; after NM-4, D5 (review) |
| 29b | feature, later | School website CMS with CBSE disclosure pages | CP-4; after DEC-10, P1-28, CP-1 |

### 5.2 Remaining backend work by kind

| Kind | What it needs |
|---|---|
| Workflow and custom operations (comms, library, org, timetable, frontoffice, people) | Hand-written logic per operation (`api/state-machines.md`, `api/operations.md`); several need message delivery |
| Public and platform-staff variants | Guest projections and staff views |
| Read models without a table (dashboards, summaries, gradebook) | Hand-written queries per module; other modules' data through their gRPC services |
| Infrastructure | Reference reconciliation, message delivery to e-mail/SMS/push providers, gateway rate limiting |

### 5.3 How to measure success

None of these has been measured: capture a baseline first. Targets are suggestions for the owner to confirm (DEC-15).

| Area | Metric | Target |
|---|---|---|
| Speed | Clean CI time (backend + clients) | < 15 min, cached < 5 min |
| Maintainability | Lines per simple resource (six-column table) | < 600 (from about 1,333) |
| Maintainability | Largest service class | < 800 lines |
| Performance | API p95 latency (list endpoints) | < 300 ms at 1,000-row tenant data |
| Performance | Queries per request (p95) | <= 10, no N+1 loops |
| Ops | DB connections per monolith instance | <= 20 |
| Ops | RPO / RTO | <= 5 min / <= 1 h |
| Quality | e2e smoke flows green in CI | 15 (the ui/05 gate) |
| Security | Wrong-tenant negative test per module | 21/21 |

---

## 6. Working rules for whoever continues

The binding rules are in [CLAUDE.md](CLAUDE.md) (how to start any work, independent review, models and agents,
terminology, no Python or shell tooling, mocks only in unit tests) and [JAVA-CODING-RULES.md](JAVA-CODING-RULES.md)
(remote calls never inside a transaction and gRPC clients in `<module>/integration`: §10; SpotBugs exclusions: §13;
baselines: §14). Build and run: [BUILD-AND-RUN.md](BUILD-AND-RUN.md). The rules below are not written there:

1. **Build features one at a time, end to end:** backend operations, screens, tests, docs and an independent review,
   all green before the next feature starts.
2. **Reading another module's data:** with the caller's token, behind one lookup class per consumer (for example
   `attendance.business.SessionLookup`). Downstream 404 -> the row does not exist for this caller; 403 -> this
   operation's 403 naming the permission; anything else -> 503 (`UnavailableException`); the downstream status or
   message never reaches the client.
3. **A reference into another module is checked before the write**, between a read-only look and the writing
   transaction. Component tests of one module seed the rows a cross-module flow would create; the `app` component
   test drives the real flow across modules.
4. **Many rows of one kind go in one statement** (`unnest` of arrays, as `AttendanceRecordRepository.insertMarks`);
   guarded updates of distinct rows stay one statement each.
5. **A combined operation needs every permission it stands for** (e.g. `RecordAttendance` with `complete` also
   checks the completing permission); the edge filter checks only the operation's own.
6. **A workflow whose branch depends on a second permission** checks it in the service (homework: publish at once
   or send for review).
7. **Web pattern:** hooks per feature in `@mysms/core` (`hooks/<feature>.ts`, `api/pages.ts` reads every page of a
   small list); screens in `web-kit/src/screens/<feature>/`, registered in `apps/web/src/routes.ts` by inventory id;
   a list-manager screen links rows to their own screen with `detail` in `RESOURCE_SCREENS`; two inventory screens
   can share one list through an address filter (`?status=pending`); unsaved work is guarded by
   `UnsavedChangesGuard`; a feature's fake backend lives in `@mysms/fixtures` (tests only); apps share
   `src/test/signedIn.tsx`; `@mysms/core` keeps a 90 % coverage threshold, so hooks need their own tests.
8. **After serving or adding operations** run `mvn -pl conformance test -Dmysms.write-reports=true` so
   api/operations-status.md is current.
9. **Before delivering:** docs updated where behaviour changed (DEPLOYMENT, database/README, api/README,
   JAVA-CODING-RULES, and this roadmap: delete finished items, add new ones); no reference-project names anywhere.
10. **Local databases** migrated before a baseline edit must be dropped and recreated (Flyway checksums).
