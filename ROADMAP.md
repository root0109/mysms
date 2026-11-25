# MySMS roadmap

The one backlog: open owner decisions, open work, and the order to do it in. Only open work is listed; delete a row
when it is done. What exists: [README.md](README.md); which operations are served:
[api/operations-status.md](api/operations-status.md) (generated, do not copy its numbers).

- IDs are stable; code, `ui/`, `api/` and `docs/` refer to them. New IDs use an area prefix (ARC, BE, DB, SEC, PERF,
  BLD, OPS, UI, DOC, NM).
- Priority: **P0** before any pilot, **P1** before the first production release, **P2** mobile/tablet launch, **P3**
  later; or **H/M/L**. Size: **S/M/L**.
- Pick work from §4 unless the owner says otherwise. Build one feature at a time end to end (operations, screens,
  tests, docs, independent review), green before the next. Rules: [CLAUDE.md](CLAUDE.md),
  [JAVA-CODING-RULES.md](JAVA-CODING-RULES.md).

## 1. Where things stand

- Backend, database and clients are built and green; the open edges are the 501 operations
  ([api/operations-status.md](api/operations-status.md)) and the unbuilt screens ([ui/02](ui/02-navigation-and-screens.md)).
- No CI pipeline, containers, e2e tests, metrics or tracing exist yet (BLD-1, BLD-5, OPS-1).
- None of the §4.2 targets has been measured.

## 2. Open decisions (owner)

Defaults apply until decided. "Rec." is the reviewers' recommendation.

### 2.1 Architecture and platform

| ID | Question | Rec. / default |
|---|---|---|
| D1 (improvement) | Operating model: 22 services, or modular monolith as the only v1 production shape (extract a service only against a metric, with a written readiness gate per service, OPS-4)? | Monolith first; keep proto contracts |
| DEC-1 | Module calls inside the monolith: keep gRPC (add timeouts, bounded retries, circuit breaking, latency targets) or in-process adapter with contract parity tests? | Measure first; adapter if D1 adopted |
| D2 (improvement) | Proto copies (each caller recompiles callees' protos; `people.proto` in 9 callers): keep, or one `<module>-grpc-api` artifact per callee? Needs a CLAUDE.md rule change. | One artifact |
| D9 / E46 | Connection budget (one Hikari pool per module plus replica and relay pools, per instance): one shared pool, or small pools behind PgBouncer? | ≤ 20 connections per instance; measure first |
| D12 | Event payloads are id-only (consumers call back), no per-aggregate ordering with several relays, 500 ms polling: add `row_version` + minimal state, stale-drop, one relay leader, LISTEN/NOTIFY? | Yes |
| D5 (improvement) | Reads go to the replica when one is configured (read-your-writes trap): primary by default, replica opt-in? | Primary by default |
| D6 | Flyway baselines are edited in place: freeze at the first external pilot, add a migration-upgrade test (DB-4). | Freeze at first pilot |
| D7 | Allow PostgreSQL extensions `pg_trgm`, `pg_partman`, `pg_cron`? | Allow |
| D8 | Migrations run at app start as `mysms_migrate`: move to a separate job, app holds only `mysms_app` (SEC-1)? | Separate job |
| D11 | User tokens forwarded hop to hop over plaintext gRPC: mTLS or mesh plus audience-scoped tokens before any multi-service deployment (SEC-8). | Moot if D1 = monolith |
| D13 | Web refresh token lives in memory (reload signs out): httpOnly SameSite cookie with CSRF protection? | Cookie |
| DEC-2 | Tenant placement: design a tenant-to-cluster key while the schema is mutable; `data_region` on the tenant before US expansion. | Design now |

### 2.2 Product and API

| ID | Question | Rec. / default |
|---|---|---|
| D1 (review) | Authority model (blocks P1-27): union of all role grants, or only the active role? | Union |
| D2 (review) | Institution scope: from token grants plus each record's `institutionId` (drop `X-Institution-Id`), or the header on every operation? ui D-04 waits on it. | Record field |
| D4 (review) | Currencies: two decimals for v1, or four now? | Two |
| D5 (review) | Inventory/store module in v1 (NM-4, CP-5, GK-15)? | Not in v1 |
| DEC-3 | Self-scoped staff read (teachers need `hr.staff.read`, which shows colleagues). Affects TT-03, HR-02, HR-04, NM-7, NM-12. | Add it |
| DEC-4 | Own payslips: keep "payroll officer = holder of `generate_payslips`", or an explicit own-payslips permission? | Owner |
| DEC-5 | Public admission application (ADMN-05 needs a signed-in applicant today): add a public operation? Add an `applicant` role template either way. | Owner |
| DEC-6 | Self-sign-up role granted for one school, not tenant-wide: confirm and finish. | Finish |
| DEC-7 | `SummarizeAttendance` adds class and subject registers together: add a register-kind parameter? | Owner |
| DEC-8 | Problem details in the reader's language: server resolves from `Accept-Language`, or codes only with client catalogues (E52)? | Owner |
| DEC-9 | Output-only fields sent by a client: api/README says both "ignored" and "400"; the server ignores them. Pick one and fix the text (P1-32). | Owner |
| DEC-10 | `platform.tenant_domain`: tenant-facing or platform-internal (P1-28)? | Owner |
| DEC-11 | E22 merges `cohort_enrollment` and `cohort_teacher` into `lms.cohort_member`, against the schema principle. | Keep rejected |
| DEC-12 | Approve `Lookup` in the SpotBugs `EI_EXPOSE_REP2` allowlist (`backend/config/spotbugs/exclude.xml`, JAVA-CODING-RULES §13). | Approve |
| DEC-13 | Confirm rules built on the owner's behalf: hr staff status moves; login names (employee / admission number); applicant self-service and numbering; payslip arithmetic (one month, pro-rated by days, half-up, employer share in neither total); `sendInvite: true` refused by sis and hr; LMS guests by `X-Tenant-Code`, unconfigured providers = 503, webhook HMAC-SHA256 with 5-min tolerance, graders may delete any draft. | Confirm |
| DEC-14 | Release scope: first clients, personas and flows; is offline tablet a release requirement? List placeholders and 501s as unavailable. | Pick personas |
| DEC-15 | CI hosting, required checks and the §4.2 targets. | Owner |
| DEC-16 | Accept, reject or defer each E-item as it is scheduled. | Per item |
| DEC-17 | Data-category sensitivity levels for privacy (normal / sensitive / restricted). | Owner |
| DEC-18 | SEC-6 needs a core change (`ApplicationRoleGrants`) to make evidence append-only by privilege. | Owner |
| DEC-19 | Product vocabulary across products (naming mechanics, CLAUDE.md wording) and the packaging and paid add-ons proposal ([MODULE-AND-PRODUCT-REORGANISATION.md §9–§10](docs/design/MODULE-AND-PRODUCT-REORGANISATION.md)) | Option B; proposal §10 |
| DEC-20 | Confirm the privacy rights choices built on the implementer's judgement: separation of duties as 409; nominee activated by a fulfilled nomination request; `:set` / `:revoke` nominee methods; register audiences per activity and the role-filtered access summary (no role = full "may concern" list) | Confirm |
| DEC-21 | Confirm the privacy P3/P4 choices built on the implementer's judgement: breach notices recorded per audience with the Board's initial notice only once contained and closing only after principals and the detailed report; RoPA export as JSON carrying a `csv` field (lists the whole platform register); coverage counts unrounded, per institution for students only; a finished erasure saga ends `closed` with outcome `fulfilled` or `partially_fulfilled`; withdraw-all as its own step (`:fulfilWithdrawAll`); a partial erasure still withdraws consents and publishes the tombstone but keeps privacy's subject facts; retention catalogue global + tenant settings in days, all ten seeded values "to verify"; erasure modules and retention runs limited to modules with a handler (first wave `iam,comms`); module handlers record a receipt in the inbox transaction and run from a per-module scheduled job (gRPC never inside a transaction); sis keeps the admission register and discipline incidents; iam does not sweep `after_inactivity` until its 48-hour notice exists; proposed JAVA-CODING-RULES text: "A module's privacy tasks extend privacy-client's `PrivacyTaskHandler`; event handlers never call gRPC: work that needs a remote answer is recorded and run by the module's scheduled job" | Confirm |
| DEC-22 | Confirm what each module's erasure keeps or clears (design §9.1 table: hr, payroll, admission, attendance, assessment), the person mappings added for it (`payroll.staff_member.person_id`; `attendance.member_record` and `assessment.student_record` fed by hr.staff / sis.student events), `attendance.after_leaving` meaning "not marked since the cutoff" (attendance has no leaving date), and the failed-task back-off (retry-delay PT5M doubling, capped at redispatch-after) | Confirm |
| DEC-23 | Privacy erasure reach: (a) let iam's `user_account` events carry `personId` (reaches lms, comms guardians/staff, careers applications), or (b) module-local projections fed by sis.student / hr.staff events in lms; (c) move retention policy `platform.access_log` to lms (`lms.access_log`); confirm the per-table keep/clear choices of design §9.1 for comms, academics, careers, finance, frontoffice, library, platform, transport | (a) |

Privacy design questions (rights-request SLA, majority defaults, platform as fiduciary, retention values) and the
counsel questions: [PRIVACY-AND-CONSENT-CENTRE.md §16](docs/design/PRIVACY-AND-CONSENT-CENTRE.md#16-open-questions-and-risks)
and [PRIVACY-LEGAL-REVIEW-BRIEF.md](docs/design/PRIVACY-LEGAL-REVIEW-BRIEF.md), answers needed before 13 May 2027.

### 2.3 Built differently from the spec (confirm or change the spec)

| Screen / flow | Built as |
|---|---|
| F2 step 1, ADMN-01 | Confirm → OpenApplications → StartAdmission → Close (ui/03 lists StartAdmission first) |
| F2 step 3, ADMN-03 | Submitted → Confirm → Approve → ConfirmAdmission → EnrollApplicant (ui/03 puts Confirm after approval) |
| ADMN-05 | Signed-in applicant, no uploads (inventory says public; DEC-5) |
| ADMN-04 | Wizard takes a class section or batch id (sections have no name in the contract) |
| HR-08 | On-screen payslip, "PDF not available" (no rendering operation) |
| HR-07, F7.2 | Pay percentage 100, no leave deduction (request carries no leave input) |
| HR-05 | Template components in insertion order (no order in the contract) |
| LMS-49 | LTI 1.1 Basic Outcomes only; AGS JSON answers 422 (P1-39) |

## 3. Open work

Columns: ID | Pri | What | Where / depends on.

### 3.1 Privacy and Consent Centre (SEC-11, P0, in progress)

Built: consent, `privacy-client` with people as first consumer, rights requests of every type (access with a
role-filtered summary, correction, grievance, nomination; erasure, export and withdraw-all on privacy's side) with separation of duties, the nominee, and their screens
(ACC-15, ADM-19); the breach register with the Board's 72-hour clock and recorded notices, the RoPA CSV export and
consent coverage counts (breach register screen ADM-20); privacy's side of erasure, export and withdraw-all requests, legal holds
and retention policies and runs (screens ADM-21, ADM-22; ADM-19 runs fulfilment, withdraw-all and task retries); the modules' side in every module holding personal data except lms (privacy-client's
`PrivacyTaskHandler`: erasure with legal holds, export counts, sis's health retention sweep). Design and phases: [PRIVACY-AND-CONSENT-CENTRE.md](docs/design/PRIVACY-AND-CONSENT-CENTRE.md) §15.1.

| ID | Pri | What | Where / depends on |
|---|---|---|---|
| SEC-11 P1 | P0 | Adopt `privacy-client` at each design §8 enforcement point whose processing is built; decide shadow mode at first rollout | design §8 |
| SEC-11 P2-5 | P1 | Rights follow-ups: correction deep link and closing on the owner's `*.updated` event; grievance letters and reminders through comms (ARC-2); staff recording a request for a nominee outside the school; corrections and the nominee on the phone; an assignee picker on ADM-19 | design §9.4, §12 |
| SEC-11 P3 | P0 | Remains: lms's handler (it stores no person: DEC-23); guardian and staff accounts in comms and job applications in careers (account only: DEC-23); export parts written as platform files and the ZIP package (counts only today); file bytes purged through platform; iam's MFA and legacy credentials; restriction; the pre-erasure notice for `iam.after_inactivity`; purging soft-deleted rows past a grace period; the DR tombstone-replay runbook; `platform.access_log` policy has no platform table (DEC-23) | design §9.1–§9.5, §10 |
| SEC-11 P4 | P1 | Breach notices through comms once it delivers (recorded only today), affected-set identification, clock paging to on-call and the DPO dashboard; a coverage screen (breach register ADM-20, legal holds ADM-21 and retention ADM-22 are built on the web); coverage per class; RoPA as PDF | design §6.1, §11 |
| SEC-11 F | P1 | Follow-ups: fluent review of Hindi consent and request strings (ADM-17/18/19 are English only, ACC-15 partly); markdown renderer for notices (plain text today); offline queue for consent changes on the phone; axe checks on the privacy screens; lms plagiarism check has no register row (legal brief Q14); data dictionary and ERD generator (not in the repo) | DEC-17 |

### 3.2 Architecture and cross-module

| ID | Pri | What | Where / depends on |
|---|---|---|---|
| P1-34 | P1 / M | `BatchGet` (AIP-231) and small `*Card` projections for the most-referenced resources (person, student, staff, class section, download URLs); audience tag per operation | E43, C-6, P2-02; ui D-09 |
| ARC-1 | H | Reference reconciliation: dangling-reference report and job per module; cross-school in-module references; unique-violation races → 409 | P2-02, database/logical-references.md; P1-34 |
| ARC-2 | P1 | Message delivery: e-mail, SMS, push adapters, per-tenant channels and accounts, retries, suppression, receipts, quiet hours, opt-in. Unblocks guardian notices, reset links, fee reminders, `sendInvite`, comms 501s | E51, E12, ui D-24 |
| ARC-3 | H | Persona (own-data) reads: student and guardian attendance, homework, own timetable; narrow roster and submission reads; personal rows readable tenant-wide | DEC-3 |
| P1-37 / E13 | P2 | Composite reads in a `/v1/bff/<app>/` namespace: `GetParentHome`, `GetTeacherToday`; fill `sis.GetStudentSummary` | |
| ARC-4 | P2 | Background jobs beyond scheduled ones: move payslip runs, bonafide, promotions, imports/exports, refund sweeps, plagiarism polling to jobs; long ops → 202 + `Operation` with one `BatchResult` shape | [BACKGROUND-JOB-PATTERN.md](docs/design/BACKGROUND-JOB-PATTERN.md); E7, P1-31 |
| P1-36 / E27 / E28 | P1 / M | Event contract in proto with CloudEvents envelope, AsyncAPI; outbound webhooks (`platform.webhook_subscription`, `webhook_delivery`, HMAC, retries) | ui D-20 |
| ARC-5 | M | Event ordering work once D12 is decided | D12 |
| ARC-6 | P2 | Tenant export, erasure and archival by institution and academic year; partition retention defaults | E35; subject-level part in SEC-11 P3 |
| ARC-7 | P1 / L | Reorganisation into a platform kernel and products (22 → 10 modules, HRMS and learning sellable alone, client feature packages, `web-hrms`, employee phone app, per-tenant products and feature areas), after SEC-11; HRMS v1 scope of design §8.1 | [MODULE-AND-PRODUCT-REORGANISATION.md](docs/design/MODULE-AND-PRODUCT-REORGANISATION.md); DEC-19, D1 |

### 3.3 Backend and API

| ID | Pri | What | Where / depends on |
|---|---|---|---|
| BE-1 | - | Serve the 501 operations: comms (sending, notifications, reactions, feed, calendar, tasks), library (return, lost, fine, return request, reservation again), org (`ActivateAcademicYear`, `FindInstitutions`, `GenerateTerms`), people (`ListBirthdays`, `SendGreetings`), timetable (`GenerateSessions`, `BatchConfirmSessions`), frontoffice (`CheckOutVisitor`) | operations-status.md; comms needs ARC-2 |
| BE-2 | H | Cut boilerplate: core step helpers (guarded update/delete, unique and scope checks, field errors), DTOs as records; < 600 lines per simple resource | improvement D3 |
| BE-4 | H | Split `LmsCourseService`, `CohortService`, `ContentItemService` by workflow; largest service < 800 lines | `backend/lms` |
| D10 | L | Java 17 stays: remove the no-op `spring.threads.virtual.enabled` from the modules' `application.properties` | |
| P1-24 | M | Pure link tables exposed as five-operation resources: set on the parent or `:set…`; fold the drive-sharing duplicate | before SDK freeze |
| P1-25 | M | Write Question, Rubric, FeePlan, ReleaseRule and draft Invoice as aggregates | ui D-16 |
| P1-26 | S | `read_mask` on Get/List; full parameter parity check; ETag + 304 on reference data; compression | E15, P2-16 |
| P1-27 | M | Resource-level permissions as a proto method option | D1 (review); ui D-18 |
| P1-28 | S | Tenant self-service (`GET`/`PATCH /v1/platform/tenant`), UI defaults; gives timetable and hr the time zone and current year | DEC-10; ui D-12, D-25 |
| P1-29 | S | `StartCheckout` takes a short-lived `quoteId` | finance |
| P1-32 | S | PATCH can clear nullable fields; read-only fields in the mask → 422 | DEC-9 |
| P1-33 | S | Declared filterable/sortable fields per resource; `totalSize` opt-in | |
| P1-35 | M | Mobile essentials: `:changes?since=` feed with deletions, `me/devices:register`, download renditions, public app config | E8, E30; ui D-05, D-06, D-21 |
| P1-38 | M | Owning module issues file links; report and import definitions name their permission | |
| P1-39 / E19 / E29 | M / P2 | LTI 1.3 / Advantage; Google/Microsoft SSO with PKCE, SAML | ui D-22, LMS-49 |
| BE-5 | P1 | `IdempotencyFilter` keeps responses only for tenant callers: keep platform-staff responses too | `backend/core` |
| BE-6 | M | Leftover rules: leave ignores holidays; payroll ignores leave and percentage rates; no payslip PDF; enrolment raises no fee dues; RecordExamMarks refuses corrections; paid LMS course has no checkout | P2-11 |
| BE-7 | M | Wire the platform file store into homework, bonafide, payslip PDF, admission documents, profile photo, imports | P1-38 |
| BE-8 | P2 | Read models: daily attendance counters, dashboards, gradebook | E47 |
| BE-9 | P1/P2 | LMS proposals: gradebook → report card (E18), xAPI (E20), standards (E23), alert rules (E25), Open Badges (E26), offline packages (E10), HLS (E11), app release control (E14), bulk teacher endpoints (E31) | |
| BE-10 | L | API polish: P2-12…P2-20 (incl. read-only views as sub-resources, P2-17, ui D-17), P3-d, P3-e; state flags writable through standard update | |
| BE-11 | M | Coupon redemptions and limits (P2-10); admission interviews and checklist (P3-c) | |

### 3.4 Data and database

| ID | Pri | What | Where / depends on |
|---|---|---|---|
| P2-01 | S | Composite indexes for list screens; prune redundant `attendance_record` indexes after a load test | |
| C-1 | - | Version columns on cached roots (`lms.quiz`, `lms.rubric`, `lms.course`) | PERF-2 |
| C-2…C-4 | - | Copied keys J2–J4 (`lms.forum_*.cohort_id`, `academics.teaching_assignment`, `attendance.attendance_record.register_id`) | ARC-1 |
| C-5 | - | Person name and photo copied to consumers, fed by events | alternative to P1-34 |
| C-7 / P2-04 | S | Effective-dated `hr.staff_position`; exit, probation, contract dates | |
| P2-03 | M | Weekly timetable template with clash detection | UI timetable builder |
| P2-05 | S | Offline-created rows: client UUIDv7, device id, idempotent insert | ui D-08 |
| P2-07 | S | Institution type stored twice: keep one | |
| P2-11 | S-M | Full-text search (people, courses, media, notices) | D7; E39 |
| P3-a | S | `academics.subject.code` is `varchar(256)`: make it 30 | |
| P3-b | S | Password history if a tenant policy needs it | |
| DB-1 | S | `uq_country_iso2`, `uq_currency_code` block soft-deleted codes: make partial | `master` baseline |
| DB-2 | S | RLS casts `current_setting('app.tenant_id')` without `NULLIF(…, '')` in 20 of 22 baselines | all baselines |
| DB-3 | L | Remove `DEFAULT gen_random_uuid()` fallbacks (silent v4 ids) | baselines |
| DB-4 | M | Migration-upgrade test and baseline freeze | D6 |
| E5 | P1 | Repeatable `R__<module>_reference_data.sql`; CI runs `flyway validate` | after D6 |
| E41 | P3 | `squawk` lint on new migrations | after D6 |

### 3.5 Security

| ID | Pri | What | Where / depends on |
|---|---|---|---|
| SEC-1 | H | Migrations and partition DDL outside the app; fail startup outside dev as the migration role | D8 |
| SEC-2 | M | Wrong-tenant negative component test per module (22/22) | conformance |
| SEC-3 | P1 | Enforce `mustResetPassword`: limit the session to the password change | iam |
| SEC-4 | P1 | KMS-wrapped per-tenant data keys, re-encryption job; bind ciphertext to the row | E32 |
| SEC-5 | P2 | Gateway rate limits per tenant/user/IP; bot protection on sign-in and sign-up | E36 |
| SEC-6 | S | `platform.audit_log` and evidence tables INSERT + SELECT only for `mysms_app`; request id and actor tenant in audit | DEC-18; P2-09 |
| SEC-7 | M | Client query cache keyed by tenant and user, cleared on sign-out and impersonation | clients `core` |
| SEC-8 | M | mTLS / audience-scoped tokens before any multi-service deployment | D11 |
| SEC-9 | - | MFA by SMS and e-mail (enrolment answers 501) | ARC-2 |
| SEC-10 | L | Re-check accepted risks: 15-min token staleness, idempotency crash window and 24 h replay, replica staleness, deep-page limits | |

### 3.6 Performance

| ID | Pri | What | Where / depends on |
|---|---|---|---|
| PERF-1 | P2 | Volume tests (2,000-student school, a year of attendance) asserting query counts and time; per-operation timers | E48 |
| PERF-2 | M | Caffeine caches with event invalidation for catalogues (in use only in auth-client and privacy-client); ETag caching of reference data | C-1, E40 |
| PERF-3 | M | Cross-module call cost (own transaction and double JSON per call) | D1, DEC-1 |
| PERF-4 | P2 | Reports and dashboards on the replica; event-fed reporting and search projection | D5, E40 |
| PERF-5 | M | `pg_partman` / `pg_cron`; partitioned get-by-id probes every month | D7 |

### 3.7 Build, CI and testing

| ID | Pri | What | Where / depends on |
|---|---|---|---|
| BLD-1 | H / P1 | CI: backend reactor with PostgreSQL and RabbitMQ, `pnpm verify`, clean Flyway + `smoke.sql`, upgrade rehearsal, images, OpenAPI artefacts | E62; DEC-15 |
| BLD-2 | M / P2 | Build time: per-module test context reuse, Testcontainers reuse, cached generator, turbo remote cache; clean CI < 15 min | E49; [BUILD-AND-RUN §3.4](BUILD-AND-RUN.md#34-build-time-and-how-to-shorten-it) |
| BLD-3 | M | OpenAPI documents exist only after component tests; client `generate` is uncached: cache the contract by source | improvement D14 |
| BLD-4 | H | Test depth: lms service unit tests; JaCoCo gate including `api/**`; SDK-vs-OpenAPI contract tests | |
| BLD-5 | P2 / H | e2e: Playwright smoke over the monolith with axe, Maestro on mobile; 15 flows (ui/05 gate) | E57 |
| BLD-6 | - | Cross-module runs (sis → iam/people/org/academics, admission enrolment, imports against real files) | |
| BLD-7 | P3 | Tests for `hooks/security.ts`; native screen harness; core threshold 95 % | E60 |
| BLD-8 | L | Renovate; `eslint-plugin-react-hooks` 5.x vs React 19; Expo SDK 54 support window | |
| BLD-9 | L | Move SDK generation out of `clients/packages/sdk` to the owner's SDK project (owner: last) | CLAUDE.md §3, ui/05 |

### 3.8 Operations

| ID | Pri | What | Where / depends on |
|---|---|---|---|
| OPS-1 | H | Metrics and tracing (actuator exposes only `health,info`): Prometheus, OTel, pool, gRPC and outbox gauges, `pg_stat_statements`, client error reporting | |
| OPS-2 | H | Point-in-time recovery, dump role under FORCE RLS, restore drill, RPO/RTO | [DEPLOYMENT.md](DEPLOYMENT.md) |
| OPS-3 | H | Connection budget, `max_connections` formula, worker mode for relays and jobs, PgBouncer | D9 |
| OPS-4 | M | Per-service readiness gate before running a module as its own service | D1 |

### 3.9 Clients and UI

| ID | Pri | What | Where / depends on |
|---|---|---|---|
| UI-1 | - | Web screens that still show their placeholder (27 of the 179 the web carries): ACA-06 class section workspace; ACC-06 persona switcher, ACC-09 notification inbox (BE-1), ACC-12 public profile editor; ADM-01 admin dashboard, ADM-03 institution detail; ADMN-04 enrol applicant; ASM-04 homework submit, ASM-07 assignment submit; CAR-01…04 careers (job board, opening + apply, employer console, my applications); COM-01 notice board, COM-02 notice composer, COM-03 my calendar (BE-1), COM-05 conversations, COM-13 notification setup; LIB-02 title detail, LIB-07 my loans; LMS-03 enrol/buy, LMS-28 cohort announcements, LMS-35 grade history; SIS-08 documents, SIS-09 bonafide (BE-7), SIS-12 birthdays (BE-1); TT-04 room/staff view. Phone apps: only the native-kit screens exist (home, account, profile, settings, timetable, fees, payslips, leave, coursework, assessment, LMS, cohort, subject registration, consent centre, notification preferences); PLT-02/PLT-04 tenant parts | ui/02; `clients/apps/web/src/routes.ts`; P1-34 |
| E59 | P2 | Teacher phone app first: Today and ATT-03 with the offline queue | ui D-05, D-08 |
| UI-2 | P2 | Offline: network state, persisted cache, SQLite outbox; tablet lesson cache | P1-35, P2-05, E9, E10 |
| E53 | P1 | Route-level error boundary; prefetch on hover | `clients/apps/web/src/routes.ts` |
| E54 | P1 | Split `RecordsScreen.tsx` (658 lines) into list, filter bar, form, transition dialog | `packages/web-kit/src/screens` |
| E55 | P2 | Screen state in the address everywhere (done only for list filters) | |
| E56 | P2 | Ship a locale only when complete (`hi` is partial); pseudo-locale run | `packages/i18n`; DEC-8 |
| E58 | P2 | Windowed `DataTable` for large lists; 1,000-row roster benchmark | |
| UI-3 | M | Bundle budgets per lazy chunk and for React Native (only the initial web route has one) | |
| UI-4 | M | Shared prop-contract test for `ui-web` and `ui-native` | |
| UI-5 | - | Small UX gaps: native date picker, leave comments on native, name fallbacks without read permissions | |
| ui D-04 | - | Institution switcher in the web shell (not built); its scoping waits for D2 (review) | ui/02 |

ui D-items name client needs waiting on a backend item; when the item ships, regenerate the SDK, build the client
side and delete the reference.

### 3.10 New modules and feature gaps

Children's data in any item follows SEC-11. Product context: [ui/01](ui/01-product-map.md), [docs/product/](docs/product/).

| ID | Pri | What | Where / depends on |
|---|---|---|---|
| NM-1 | P1 / M | Health: conditions and alerts for teachers, visits, medication, check-ups (`sis.student_health_record` is free text) | new `health`; ARC-2, ARC-3, SEC-11 |
| NM-2 | P3 / L | Hostel: rooms, beds, allotment, roll-call, out-pass | new `hostel`; NM-1, NM-3, NM-6 |
| NM-3 | P3 / L | Canteen: menus with allergens, wallet, meal counts (PM POSHAN) | new `canteen` + finance wallet; NM-1 |
| NM-4 | P3 / L | Inventory and procurement: stores, vendors, POs, stock, sales | new `inventory`; D5 (review) |
| NM-5 | P2 / L | Transport tracking: vehicles, crew, trips, GPS, boarding, ETA, documents | `transport`; ARC-2, D7, UI-2 |
| NM-6 | P1 / M | Gate and pickup: authorised pickups with photo/OTP, gate passes, late/early register | `frontoffice`; ARC-2, ARC-3, BE-1 |
| NM-7 | P2 / M | Substitution and cover from approved leave | `timetable`; BE-1 (`GenerateSessions`), DEC-3 |
| NM-8 | P3 / M | Clubs, houses, competitions, achievements | new `activities` |
| NM-9 | P1 / M | Certificates (TC, bonafide, character…) from templates, numbered, verifiable | `sis`; ARC-4 |
| NM-10 | P3 / M | Scholarships: schemes, applications, NSP tracking, disbursement | `finance`; NM-9 |
| NM-11 | P2 / S | Parent-teacher meeting booking | `comms`; ARC-2, ARC-3 |
| NM-12 | P3 / M | Staff appraisal and CPD hours | `hr`; DEC-3 |

### 3.11 Differentiating features (owner-selected)

| ID | Pri / size | What | Where / depends on |
|---|---|---|---|
| IN-1 | P2 / L | Self-checking year planner (conflicts, what-if) | `org` + `timetable`; NM-7 |
| IN-2 | P2 / M | School-to-school transfer packet, signed and verifiable | `sis`; SEC-11, NM-9 |
| IN-3 | P3 / M | "Where your fees go" statement | `finance`; GK-5, NM-4 |
| IN-4 | P2 / M | Anonymous student surveys with "you said, we did" | `comms`; SEC-11 |
| IN-5 | P2 / M | Report card explained in the parent's language (teacher-reviewed AI text) | `assessment`; AI provider decision |
| IN-6 | P1 / M | Parent request desk with deadlines and escalation | `comms`; NM-9, NM-11 |
| IN-7 | P1 / L | One timeline per child, fed by events | `comms` read model; SEC-11 |
| IN-8 | P3 / S | School promises tracker | `comms`; IN-6 |
| IN-9 | P2 / M | Ask the rulebook (answers quote policy clauses) | `comms`; IN-5 provider |
| IN-10 | P2 / S | Full cost of a year, before admission | `admission` + `finance` |
| IN-11 | P3 / S | Parent skills and volunteer board | `comms`; SEC-11 |
| IN-12 | P3 / M | Book and uniform exchange | `library` or NM-4 |
| IN-13 | P3 / M | Carpool matching (grid cells, opt-in) | `transport`; SEC-11 |

### 3.12 Competitive parity features (owner-selected)

| ID | Pri / size | What | Where / depends on |
|---|---|---|---|
| CP-1 | P1 / M | Admission enquiry CRM: leads, sources, follow-ups, funnel, convert to application | `admission`; ARC-2, DEC-5, CP-3 |
| CP-2 | P2 / L | Automatic timetable generator (Timefold Solver) | `timetable`; P2-03, BE-1, ARC-4 |
| CP-3 | P1 / M | WhatsApp, free only: share to class groups, invite links | `comms`; ARC-2, SEC-11 |
| CP-4 | P3 / L | School website with CBSE disclosure pages (approach to be decided) | new `website`; DEC-10, P1-28 |
| CP-5 | P3 / M | School store: online orders, payment, pickup | NM-4 + `finance`; D5 (review) |
| CP-6 | P1 / M | ID cards with QR verification (NM-9 renderer) | `people`; ARC-4, NM-9 |
| CP-7 | P1 / L | Management dashboards | new `insights` read model; D12, PERF-4, OPS-1 |

### 3.13 GegoK12 parity (owner benchmark)

Evidence and feature ids (G001–G191): [docs/benchmark/GEGOK12-COMPARISON.md](docs/benchmark/GEGOK12-COMPARISON.md).

| ID | Pri | Gap | Covered by / note |
|---|---|---|---|
| GK-1 | P0 | SMS (an Indian provider with DLT templates), SMTP, FCM delivery | ARC-2 |
| GK-2 | P0 | In-app notifications inbox (can ship before the rest of ARC-2) | BE-1 |
| GK-3 | P0 | Library return, fines, return requests, lost | BE-1 |
| GK-4 | P0 | Calendar view (`GetMyCalendar`, `ExportCalendar`) | BE-1 |
| GK-5 | P1 | Income and expense cash book in `finance` | feeds IN-3 |
| GK-6 | P1 | ID cards and bus passes: raise CP-6 | CP-6 |
| GK-7 | P1 | Certificates incl. TC: raise NM-9 | NM-9 |
| GK-8 | P1 | Birthdays, greetings, work anniversaries (`ListWorkAnniversaries`) | BE-1 |
| GK-9 | P1 | Feed, class-wall reactions, follow | BE-1 |
| GK-10 | P1 | `ActivateAcademicYear`, `AddGroupMembers`, `CheckOutVisitor`, `MarkConversationRead`, `ClaimTask`, `MarkFeedbackMessage` | BE-1 |
| GK-11 | P1 | Hall tickets (`GenerateHallTickets`); report-card PDF | `assessment` |
| GK-12 | P1 | Setup wizard, seeded demo tenant, one-command install | |
| GK-13 | P2 | Timetable generator | CP-2 |
| GK-14 | P2 | Transport with conductor role and odometer log | NM-5 |
| GK-15 | P2 | Inventory and stock; reconsider D5 (review) | NM-4 |
| GK-16 | P2 | Printable question paper from `lms.question_pool` | `lms` |
| GK-17 | P2 | Gate pass, appointments, host alert | NM-6, NM-11 |
| GK-18 | P2 | Alumni module (needs an ID) | docs/product |
| GK-19 | P3 | Facility maintenance work orders | NM-4 |
| GK-20 | P2 | Per-module entitlement on plans | `platform` |
| GK-21 | P0 | Role templates for GegoK12's personas and functional roles | `iam` |
| GK-22 | P2 | CAPTCHA on public admission form and sign-in | SEC-5 |
| GK-23 | P2 | App connect by QR (tenant code + invite token) | |
| GK-24 | P1 | Audit phone flows against G170–G191 | `clients/apps/mobile` |

### 3.14 Docs

| ID | Pri | What | Where / depends on |
|---|---|---|---|
| E61 / DOC-1 | P1 / M | A Java generator in the build for `database/data-dictionary.md`, `relationships.md`, `logical-references.md` and `erd/` (all stale: 466 of 489 tables, no privacy schema) and for the totals (tables, FKs, references, RLS); docs link, never copy | conformance |
| DOC-2 | M | `api/operations.md` and `api/state-machines.md` are kept by hand (in step today): generate them in the build like `operations-status.md` | conformance; DOC-1 |
| DOC-5 | H | School Community Network: add the design's screen rows (COM-30…COM-74, PC-COM-01, routes in design §C2) to ui/02 when the owner approves the design, and mark COM-08 as replaced; build follows the design's slices S1–S4 (§16); search (§22.5) needs D7 (`pg_trgm`); timing against ARC-7 | [design](docs/product/SCHOOL-COMMUNITY-NETWORK-TECHNICAL-DESIGN.md) |
| DOC-6 | - | `JAVA-CODING-RULES.md`: reorganise and consolidate with more samples, losing no rule or sample (only when the owner asks); its §7.1a still names `common`'s `CommonConfig` | owner-controlled |
| DOC-3 | - | Move this backlog to an issue tracker? | DEC-15 |
| DOC-4 | L | Recover A1–A16 and S-items of the old enhancement review from git history, or drop them | |

## 4. Sequencing

### 4.1 Order

Phase 0 before any pilot; phase 1 architecture debt; phase 2 scale. Features run alongside, one at a time, each
once its dependencies are done.

1. **Now:** SEC-11 phases in order (P1 rest, P3, P4, P2-5 and follow-ups); P1–P3 by March 2027. Then ARC-7 (kernel and products, HRMS first).
2. **Phase 0:** BLD-1 + DB-4; D9 + OPS-3; OPS-2; OPS-1; D13; D8 + SEC-1; small fixes BE-5, SEC-3, DB-2, DB-1.
3. **Phase 1:** D1, DEC-1, D2, D11; P1-34 + ARC-1; BE-2 + BE-4; SEC-2, BLD-4, BLD-5; D5 (improvement); D6 + E61;
   ARC-2 + BE-1; ARC-3 + E59.
4. **Phase 2:** D7, P2-11, P2-01; D12 + ARC-5; UI-2, P1-35, P2-05; ARC-6 + DEC-2; PERF-2, PERF-4.
5. **Ongoing:** UI-1 and the contract-shape items P1-24…P1-33 before the SDK freezes.
6. **Features with phase 1:** CP-3, CP-7 (first slice), NM-1, NM-6, NM-7.
7. **Features with phase 2:** NM-9, CP-1, CP-6, NM-11, NM-5, CP-2.
8. **Later:** NM-12, NM-8, NM-10; NM-4, NM-3, NM-2; CP-5; CP-4.

### 4.2 Targets (unmeasured; owner to confirm, DEC-15)

| Area | Metric | Target |
|---|---|---|
| Speed | Clean CI (backend + clients) | < 15 min, cached < 5 min |
| Maintainability | Lines per simple resource / largest service | < 600 / < 800 |
| Performance | List p95 at 1,000-row tenant data; queries per request | < 300 ms; ≤ 10, no N+1 |
| Ops | DB connections per monolith instance; RPO / RTO | ≤ 20; ≤ 5 min / ≤ 1 h |
| Quality | e2e smoke flows in CI; wrong-tenant tests | 15; 22/22 |
