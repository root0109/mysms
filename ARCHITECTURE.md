# MySMS architecture

How MySMS is built and deployed. It complements the database design (`database/README.md`), the API design (`api/README.md`) and the binding Java rules (`JAVA-CODING-RULES.md`, which wins on any code-level detail).

## 1. One module per schema, every module a service

MySMS is a set of **21 independently deployable services**, one per PostgreSQL schema, built from one Maven repository with **flat modules** (no nesting). The same jars run all together (one process, one database instance — development and small tenants) or as separate services, each with its schema on its own PostgreSQL instance. Nothing in the code or the schema differs between the two; moving a module out is configuration plus a data copy.

The modules are `core` (plain library: base classes, tenancy, errors, paging, ids, `AppTransactionManager`, cross-cutting auto-configuration — no schema, no Flyway, no datasource of its own), `auth-client` (the only auth surface modules touch), `messaging` (event API, runtime and broker adapters), the 21 domain modules — one per schema — and `app` (assembly only, no business code). The module list with each module's schema is in [`backend/README.md` "Modules"](backend/README.md#modules), which also says how to build and run them.

Each domain module has its own `@SpringBootApplication`, its own configuration namespace (`mysms.<module>.*`), its own DataSource (core's `RoutingDataSource`: read transactions on the module's replica pool when one is configured, write transactions on the primary) and transaction manager, and its own Flyway folder; the build produces both a library jar (used by `app`) and an executable service jar.

**Isolation is structural.** A domain module depends on `core`, `auth-client` and the `messaging` runtime — never another domain module — so one module cannot compile against another's classes. A module that declares event handlers also gets `messaging` at compile scope for the narrow `com.mysms.messaging.events` API; modules that only publish through core's outbox keep the dependency runtime-only. Maven exposes adapter dependencies transitively, so the dependency boundary is enforced by the POM policy and conformance check: domain code may use the event API, but not RabbitMQ, Google Pub/Sub or adapter/runtime packages. RabbitMQ is the default; Google Cloud Pub/Sub and in-process are selected with `mysms.events.bus`. Package layout inside a module is defined in `JAVA-CODING-RULES.md` §3.

**Cross-module rules** (these hold identically in one process or across many):

- **No cross-schema SQL** — no joins, reads or writes outside the module's own schema. Another module's data is read through its gRPC service (ids plus compact card messages; JAVA-CODING-RULES §10), or from a local read model kept fresh by that module's events.
- **No cross-module foreign keys.** References into another module are *logical references* (indexed uuid columns) stored as given (no write-time call to the owner), protected by archive-not-delete, event-driven cleanup and RLS (a reconciliation job against the owners waits for service-to-service identity) — see `database/logical-references.md`.
- **No distributed transactions.** A change that other modules care about writes its own rows plus an `outbox_event` row in one local transaction; the module's relay publishes it to the bus — MySMS's own `EventBus` interface, RabbitMQ by default, swappable by configuration — and consumers handle it exactly once through their `inbox_event`. Audit records travel the same way into `platform.audit_log`.
- **Module-local infrastructure.** Idempotency keys, long-running operations and document numbering live in each module's own schema; nothing is borrowed from the platform schema at runtime.
- **Calls are remote by contract.** Modules call each other only over gRPC (`@GrpcClient` stubs, JAVA-CODING-RULES §10); in `app` the same clients call the co-located module through the in-process gRPC server — the same code path as in production — so assembled and distributed deployments behave the same.
- **Trace context** (W3C `traceparent`) is propagated by `core` on every inbound request, gRPC call and published event, so a request can be followed across services.

## 2. Allowed module dependencies

A module may call (over gRPC, from its `integration` package, with copies of the callees' protos) only the modules whose data it references. The table is derived from the 468 forward logical references in `database/logical-references.md`; the number is how many reference columns point at that module. Tenant references are not dependencies (the tenant comes from the token). Every module also uses `core`, `messaging` and `auth-client`; auth-client's permissions filter reads iam's role data (`iam_authz.proto`, over gRPC or, where iam runs in the same process, directly) and every module publishes its API-permission catalogue to iam, so every module reaches iam through auth-client whatever its row says (JAVA-CODING-RULES §16.2).

| Module | May depend on |
|---|---|
| `master` | — |
| `platform` | `master` (4) |
| `iam` | `org` (1), `people` (1) |
| `org` | `master` (4), `platform` (3) |
| `people` | `master` (5), `org` (1), `platform` (4) |
| `academics` | `hr` (2), `iam` (1), `org` (10), `platform` (1) |
| `sis` | `academics` (13), `hr` (1), `iam` (5), `org` (15), `people` (3), `platform` (1) |
| `hr` | `iam` (2), `org` (9), `people` (2) |
| `payroll` | `hr` (4), `iam` (1), `org` (5), `platform` (2) |
| `admission` | `academics` (9), `finance` (3), `master` (2), `org` (6), `people` (3), `platform` (2), `sis` (1) |
| `attendance` | `academics` (4), `hr` (3), `iam` (2), `org` (3), `sis` (2), `timetable` (1) |
| `facility` | `academics` (3), `org` (3) |
| `timetable` | `academics` (4), `facility` (1), `hr` (1), `org` (2), `sis` (1) |
| `assessment` | `academics` (11), `facility` (1), `hr` (4), `iam` (8), `org` (9), `platform` (2), `sis` (5) |
| `lms` | `academics` (3), `assessment` (2), `comms` (7), `finance` (3), `iam` (66), `master` (4), `org` (16), `platform` (23) |
| `finance` | `academics` (3), `iam` (5), `master` (6), `org` (12), `people` (2), `platform` (1), `sis` (6) |
| `library` | `academics` (4), `finance` (1), `hr` (1), `iam` (3), `org` (5), `people` (3), `platform` (1), `sis` (1) |
| `transport` | `org` (3), `sis` (1) |
| `comms` | `academics` (5), `iam` (27), `org` (26), `platform` (10), `sis` (4) |
| `frontoffice` | `hr` (1), `iam` (2), `org` (7), `platform` (1), `sis` (1) |
| `careers` | `iam` (2), `master` (1), `people` (1), `platform` (2) |
| `privacy` | — |

**The graph is a DAG.** The 27 references that point against an arrow are event-only: the owner does not know about them, the referencing module cleans them up from the owner's events, and it never calls the owner for them (listed at the end of `database/logical-references.md`). Each module calls only the modules in its row, through one `<Callee>GrpcClient` per callee; a plain JUnit check in `core`'s test kit (`ModuleChecks`) fails the build if a module imports the gRPC contract (`com.mysms.<x>.v1`) of a module outside its row, and conformance G1 fails it if a module carries the proto of one.

## 3. Data access: Spring JDBC only, no ORM

**Rule: no ORM layer anywhere in the project** (no JPA, Hibernate, Spring Data, jOOQ code generation or MyBatis). Data access is plain SQL with Spring JDBC (`JdbcClient`, `NamedParameterJdbcTemplate`).

- Repositories live in the module's `repository` package; SQL is written by hand, schema-qualified with the module's own schema only (`lms.course`), with named parameters only (never string concatenation of values).
- Rows map through explicit `RowMapper`s into entities; entities never leave the module (`JAVA-CODING-RULES.md` §5–§7).
- Every read adds `tenant_id = :tenantId` and, unless deleted rows are wanted, `deleted_at IS NULL`. Every write sets `created_by` / `updated_by` / timestamps. Updates use optimistic locking: `… SET …, row_version = row_version + 1 WHERE id = :id AND row_version = :expected`; 0 rows updated → **412 Precondition Failed** / `ABORTED` with the current row version, matching the API's `If-Match` contract (409 stays for duplicates and state-transition conflicts).
- Ids are UUIDv7 generated in Java. Enums are stored as `varchar` (`Enum.name()` / `valueOf`). `jsonb` is read and written as text through Jackson with a `CAST(:json AS jsonb)` in the SQL.
- **Transactions are programmatic**: every module declares its `PlatformTransactionManager` as a Spring bean, and each service extends core's `AppTransactionManager`, which drives it (begin → bind the tenant with the transaction-local `app.tenant_id` → the service's work → commit / rollback). `@Transactional` and `TransactionTemplate` are not used.
- Partitioned tables (see the database README) are always queried with their time range or date so PostgreSQL reads only the needed months.
- Flyway is the only thing that creates or changes the schema. Integration tests run every repository against a Testcontainers PostgreSQL migrated with the module's own migrations, so SQL that drifts from the schema fails the build.

### Tenant isolation at the database

Every tenant-owned table has `FORCE ROW LEVEL SECURITY` with the policy `tenant_id = current_setting('app.tenant_id', true)::uuid`. Every service binds the tenant right after it begins a transaction (`ModuleDatabase.bindTenant()`: `set_config('app.tenant_id', …, true)`, transaction-local) before its first statement. Four database roles: `mysms_migrate` owns the schemas and runs Flyway; `mysms_app` is the application login (not the owner, so RLS binds it); `mysms_relay` (BYPASSRLS) is the outbox relay's, granted each module's outbox and inbox only; `mysms_platform_admin` (BYPASSRLS) is used only by the platform console service. The tenant predicate in SQL (`tenant_id = :tenantId`) remains as the first layer; RLS is the backstop that turns a missed predicate into an empty result instead of a data breach.

### Published content is versioned

Lesson, quiz, question, assignment and rubric rows carry `version` / `superseded_by_id` / `published_at`: a published version is immutable; editing creates a new version, and graded evidence snapshots what it was graded against (rubric version, points possible, max marks). Cohorts copy chapters/items from the course template (`copied_from_id`), so teachers adapt copies, never templates.

## 4. Deep links

- One link format for every client: `mysms://<module>/<resource path>`, the API path without `/v1` (e.g. `mysms://lms/cohorts/{cohortId}/gradebook`). Stored in `comms.in_app_notification.action_url` and used in push payloads, e-mails and SMS.
- The React Native apps register the `mysms` scheme plus universal links (iOS) / app links (Android) on `https://<tenant host>/app/<same path>`; the React web app routes the same path. A screen that an app does not have opens the web URL instead.
- Links carry no tokens or personal data; the app signs in and loads the record through the normal API operations.

## 5. Module interdependencies

Read from the code: the `integration/*GrpcClient` classes, each `*ReferenceCleanup` event subscription, [`database/logical-references.md`](database/logical-references.md) and the module poms.

### 5.1 Synchronous calls (gRPC): caller → callee

These are the dependencies that matter most. The caller cannot answer when the callee is down, and each call costs a round trip plus a proto↔JSON conversion.

| Caller | Callee | RPCs used |
|---|---|---|
| admission | sis | getStudent, listStudents, createStudent, listStudentGuardians, createStudentGuardian, … (14 in all: creates the student on enrolment) |
| admission | people | createPerson, listPersons |
| assessment | sis | getStudentSummary, listMyChildren, listClassRoster, listSubjectRegistrations |
| assessment | academics | listCourseSubjects |
| assessment | facility | getRoom |
| assessment | iam | getMe |
| attendance | sis | listClassRoster, listStudents |
| attendance | timetable | getSession, listSessionStudents |
| attendance | org | getInstitution |
| finance | sis | getStudentSummary, listMyChildren, listClassRoster, listProgramEnrollments |
| finance | academics | listClassSections, listCourses |
| finance | iam / master / org | getMe / getCurrency / getAcademicYear |
| sis | academics | getClassSection, listClassSections, getGradeLevel, getSection, listCourseSubjects |
| sis | iam | listUserAccounts, createUserAccount, createUserRole, listRoles |
| sis | org / people | getAcademicYear, getInstitution / listPersons |
| lms | platform | listEntityAttachments, getFileObject, getDownloadUrl, createUploadUrl, completeUpload |
| lms | iam / assessment | getMe, listUserAccounts, activeTenant, callerEmail / listGradingBands |
| iam | org / people | listActiveInstitutions / getMyPerson |
| hr | iam | listUserAccounts, createUserAccount |
| platform | master | getCurrency |
| careers | iam | activeTenant, emailOf |

That makes 26 caller→callee edges and about 66 RPCs. Modules that call nobody: master, org, people, academics, payroll, facility, timetable, library, transport, comms, frontoffice.

**Most called:** iam (6 callers), sis (4), org (4), academics (3). **Proto copies held:** `people.proto` ×10, `org.proto` ×9, `iam.proto` ×7, `sis.proto` ×5. Each copy is compiled again by every holder.

### 5.2 Asynchronous (events): consumer ← producer, by number of event types

| Consumer | Producers it listens to |
|---|---|
| lms | org 3, iam 2, platform 2, comms 2, academics 1, assessment 1, finance 1 |
| comms | iam 3, lms 2, academics 1, org 1, platform 1, sis 1 |
| privacy | sis 6, people 3 |
| finance | lms 2, sis 2, academics 1, people 1, platform 1 |
| admission | people 2, finance 1, platform 1, sis 1 |
| platform | iam 2, lms 1, org 1 |
| sis | assessment 1, iam 1, org 1, platform 1 |
| attendance | hr 1, sis 1, timetable 1 |
| facility | academics 3 |
| library | academics 2, platform 1 |
| others | 1–2 each (mostly `platform.file_object.deleted` and `hr.staff.deleted`) |

Nearly all are `*.deleted` cleanup events ([`database/logical-references.md`](database/logical-references.md) rule 3). The exceptions carry state for a copy the consumer keeps: `sis.student` and `hr.staff` into `iam.member_record`, `hr.staff` into `payroll.staff_member`, and `people.person`, `sis.student` and `sis.student_guardian` into privacy's subjects and guardian links (`privacy.subject`, `privacy.guardian_link`). Because they are asynchronous and the outbox and inbox make them reliable, they are cheap coupling.

### 5.3 Data references (logical, no FK)

The references each module holds into the others are counted per module in §2. Read by owner (inbound references, 469 in all, excluding `tenant_id`, which appears on about 450 tables and is logically `platform.tenant`): org 133, iam 125, academics 59, platform 53, master 26, sis 22, hr 17, people 15, finance 7, comms 7, facility 2, assessment 2, timetable 1; payroll, admission, attendance, lms, library, transport, frontoffice and careers own nothing another module references.

### 5.4 What the tables say

- **The graph is already layered, with almost no cycles.** It runs from a kernel (master, platform, org, iam, people), to academic structure and staff (academics, hr), to student records (sis), to operations (everything else). The only back-edges are small: lms→finance and comms, library→finance, admission→finance (3 references each), and the iam↔org/people and platform↔iam/lms event loops.
- **Eight modules have no inbound dependency at all:** payroll, admission, attendance, lms, library, transport, frontoffice and careers; comms is called by nobody and referenced only by lms (7 references). They are leaves, so merging or splitting them costs nothing elsewhere.
- **org and iam are referenced by everyone** (258 of 469 references), because institution ids and user ids are everywhere. That is normal for a kernel and no merge removes it. The goal is to remove *synchronous* coupling, not id references.
- **The heavy synchronous pairs** are admission→sis (14 RPCs), sis→academics (5 plus 13 references), finance→sis (4), assessment→sis (4), lms→platform (5) and attendance→sis/timetable (4). These are the merge candidates.

## 6. Proposal: merging modules

A proposal awaiting the owner's decision; nothing here is built. It changes rules of [`CLAUDE.md`](CLAUDE.md) and [`JAVA-CODING-RULES.md`](JAVA-CODING-RULES.md), which stay binding until the owner amends them.

### 6.1 Rules used to decide

1. **Same business capability and the same users.** One back-office team or product owner, for example "student lifecycle" or "HR and pay".
2. **Strong synchronous coupling.** Any pair with more than 3 RPCs or more than 10 references, where one side cannot work without the other.
3. **Same change cadence and data sensitivity.** Money (finance) and identity (iam) stay apart. They have stricter audit, review and security needs, and auth is the hot path that must scale on its own.
4. **No new giant.** lms is already 1,133 files. Nothing gets merged into it.
5. **Leaves are free.** A module with no inbound dependency can join its business area without disturbing anyone.

### 6.2 Recommended: 21 → 9 modules (Plan A)

| New module | Merges | Why |
|---|---|---|
| **platform** | platform + master + org | Tenant, files and reference data (currencies, countries, institutions, academic years). Read-mostly, changed by tenant admins only, and depended on by everyone. Removes platform→master and the 9 `org.proto` copies become one. |
| **identity** | iam + people | Who a person is and how they sign in. iam already calls people (getMyPerson) and people listens to iam. Keeps one security-reviewed boundary for personal data. |
| **students** | sis + admission + careers | The student lifecycle: applicant → student → alumni or placement. Removes the largest RPC edge (admission→sis, 14 RPCs). careers is a 67-file leaf. |
| **academics** | academics + timetable + attendance + assessment | Teaching operations: structure, schedule, attendance, exams and marks. Removes attendance→timetable, assessment→academics and most of the academic references. |
| **learning** | lms | Kept on its own: a separate product line, already the largest module, with a different release cadence. |
| **workforce** | hr + payroll | Staff and their pay. payroll is a leaf that only reads hr. |
| **finance** | finance | Kept on its own: money, ledgers, audit. |
| **campus** | library + transport + frontoffice + facility | Campus services run by the admin office. All are leaves or near-leaves (facility is read only by getRoom). |
| **comms** | comms | Kept on its own: high fan-in through events, with a bursty scaling profile (notifications, mail). |

Effect, counted over the tables in §5:

| | Today | Plan A (9) | Plan B (6) |
|---|---|---|---|
| Modules / services | 21 | 9 | 6 |
| Caller→callee gRPC edges | 26 | 16 | 6 |
| RPCs used across modules | 66 | 47 | 34 |
| Event subscriptions crossing modules | 66 | 59 | 48 |
| Cross-module references (no FK) | 469 | 432 | 386 |

**Plan B (6)** goes further for a team of one or two:
- **foundation** = platform + master + org + iam + people
- **school** = sis + admission + careers + academics + timetable + attendance + assessment + facility
- learning, workforce and finance as in Plan A
- **services** = library + transport + frontoffice + comms

Plan B removes most synchronous calls. The cost is a "school" module of about 800 files, and identity no longer sits behind its own boundary. **A** is recommended. Take B only if you are certain no module will ever run as a separate service.

**New modules proposed in [ROADMAP.md §3.10](ROADMAP.md#310-new-modules-and-feature-gaps)** fit Plan A without a tenth module: **students** gains `health` (NM-1) and `activities` (NM-8) and extends sis for certificates (NM-9); **academics** extends timetable for substitution (NM-7); **workforce** extends hr for appraisal and CPD (NM-12); **finance** gains wallets and scholarships (NM-3, NM-10); **comms** gains PTM booking (NM-11); **campus** gains `hostel`, `canteen` and `inventory` (NM-2, NM-3, NM-4) and extends transport and frontoffice (NM-5, NM-6). All new modules are leaves, so rule 5 applies.

### 6.3 How to merge without breaking anything

- **Keep the schemas.** A merged module owns several schemas (`sis`, `admission`, `careers`). Tables, row-level security, migrations and the logical-references rules stay as they are, so no data moves. Inside a module, cross-schema foreign keys and plain joins become allowed, which also lets you turn some logical references into real FKs.
- **Keep the URL prefixes and operation ids** (`/v1/sis/...`, `/v1/admission/...`). The OpenAPI and the generated client SDK then stay the same, and the web and mobile apps do not change. One thing does need changing in the clients: `clients/packages/sdk/scripts/generate.mjs` hard-codes `expected 21 modules` and reads one `api-docs.yml` per module, so it has to learn the new module list.
- **Replace the gRPC clients inside a merged module with direct Java calls** to the other area's service class. Callers outside the module keep gRPC. Delete the proto copies that are no longer needed.
- **Owner rules to amend first.** This needs owner sign-off before any code moves. [`CLAUDE.md`](CLAUDE.md) and [`JAVA-CODING-RULES.md`](JAVA-CODING-RULES.md) require "one schema per module", forbid cross-module FKs, and fix the inter-module gRPC mechanism. The conformance build (ModuleBoundaryRule, MavenLayoutRule) checks those rules, so merging fails the build until they change.
- **Order:** merge leaves first, because nobody depends on them: careers → students, payroll → workforce, transport/frontoffice/library → campus. Then platform + master + org, then sis + admission, then the academics group. Run the full build and component tests after each merge.
