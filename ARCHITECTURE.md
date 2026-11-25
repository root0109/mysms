# MySMS architecture

How MySMS is structured. Related documents: database design [`database/README.md`](database/README.md), API design [`api/README.md`](api/README.md), binding code rules [`JAVA-CODING-RULES.md`](JAVA-CODING-RULES.md) (wins on any code-level detail), operations [`DEPLOYMENT.md`](DEPLOYMENT.md), open work [`ROADMAP.md`](ROADMAP.md). Diagram index: [`docs/diagrams/README.md`](docs/diagrams/README.md).

## 1. Modules, schemas and deployment units

One Maven reactor (`backend/pom.xml`), flat modules, Java 17, Spring Boot 4.1:

| Build module | Role | Schema |
|---|---|---|
| core | Library: `AppTransactionManager`, `ModuleDatabase`, tenancy, errors, paging, UUIDv7 ids, outbox and audit writers, idempotency, scheduled jobs, partition maintenance, REST and gRPC edge plumbing | none |
| auth-client | The only auth surface modules use: token verification, edge filter and gRPC interceptor, permission resolution, service tokens | none |
| messaging | `EventBus` API, `EventRuntime` (relay, inbox) and the RabbitMQ, Google Cloud Pub/Sub and in-process adapters | none |
| privacy-client | Consent checks against privacy over gRPC, with a per-process cache; the base of a module's privacy tasks and retention sweeps (legal holds from privacy over gRPC) | none |
| 22 domain modules | master, platform, iam, org, people, academics, sis, hr, payroll, admission, attendance, facility, timetable, assessment, lms, finance, library, transport, comms, frontoffice, careers, privacy | one each, same name |
| app | Assembly only: all 22 domain modules in one process | none |
| conformance | Build-time rule checks (module boundaries, gRPC, technology) | none |

Each domain module has its own `@SpringBootApplication`, configuration namespace (`mysms.<module>.*`), `ModuleDatabase` (primary pool plus optional replica pool through `RoutingDataSource`), transaction manager and Flyway folder (`database/migrations/<module>`). The build produces a library jar (used by `app`) and an executable service jar per module, so the same code runs as **one monolith** or as **one service per module**; only configuration differs ([DEPLOYMENT.md §5.2, §5.4](DEPLOYMENT.md#5-the-backend)).

**Isolation is structural** (conformance `ModuleBoundaryRule` A1): a domain module depends only on `core`, `auth-client`, `privacy-client` and `messaging` — never on another domain module. `messaging` is runtime-only unless the module uses its event API (`EventEnvelope`, `EventHandler`, `EventSubscriptions`, `RejectedEventException`); no module imports RabbitMQ, Pub/Sub or messaging runtime types. Package layout: JAVA-CODING-RULES §3.

**Cross-module rules** (identical in one process or many):

- **Own schema only.** No SQL, join or foreign key crosses schemas (core test kit `ModuleChecks`; migrations: `ModuleBoundaryRule` §14). Another module's data comes from its gRPC service or from a local copy kept by its events.
- **Logical references** into other modules are indexed uuid columns, protected by archive-not-delete, event-driven cleanup and RLS: [`database/logical-references.md`](database/logical-references.md).
- **No distributed transactions.** A change other modules care about writes its rows and an `outbox_event` row in one local transaction; the relay publishes it; consumers handle it once through their `inbox_event`. Audit records travel the same way (`audit.recorded` → `platform.audit_log`).
- **Module-local infrastructure.** Idempotency keys, long-running operations, document numbering and job schedules live in each module's own schema.
- **Calls are remote by contract.** Modules call each other only over gRPC (JAVA-CODING-RULES §10). In `app` the clients target the in-process gRPC server (`in-process:mysms`), the same code path as a split deployment. A gRPC call inside a database transaction is refused (`GrpcCallerInterceptor`).
- **Trace context** (W3C `traceparent`) is continued on every inbound request, gRPC call and event.

Diagrams: [system context](docs/diagrams/architecture/system-context.drawio) (product view), [modules and dependencies](docs/diagrams/architecture/modules-and-dependencies.drawio), [deployment](docs/diagrams/architecture/deployment.drawio) — open with draw.io / diagrams.net.

## 2. Allowed module dependencies

A module may call over gRPC (from its `integration` package, with copies of the callees' protos) only the modules in its row. A row lists the modules whose data the module references; the number is the count of reference columns in [`database/logical-references.md`](database/logical-references.md). Tenant references are not dependencies (the tenant comes from the token). Every module also reaches iam through auth-client (role permissions, `IamAuthzService.GetRolePermissions`, and the API-permission catalogue event; JAVA-CODING-RULES §16.2), whatever its row says.

| Module | May depend on |
|---|---|
| `master` | — |
| `platform` | `master` (4) |
| `iam` | `org` (1), `people` (1) |
| `org` | `master` (4), `platform` (3) |
| `people` | `master` (5), `org` (1), `platform` (3) |
| `academics` | `hr` (2), `iam` (1), `org` (10), `platform` (1) |
| `sis` | `academics` (13), `hr` (1), `iam` (5), `org` (15), `people` (3), `platform` (1) |
| `hr` | `iam` (2), `org` (9), `people` (2) |
| `payroll` | `hr` (4), `iam` (1), `org` (5), `platform` (2) |
| `admission` | `academics` (9), `finance` (3), `master` (2), `org` (6), `people` (3), `platform` (2), `sis` (1) |
| `attendance` | `academics` (4), `hr` (3), `iam` (2), `org` (3), `sis` (2), `timetable` (1) |
| `facility` | `academics` (3), `org` (3) |
| `timetable` | `academics` (4), `facility` (1), `hr` (1), `org` (2), `sis` (1) |
| `assessment` | `academics` (11), `facility` (1), `hr` (4), `iam` (8), `org` (9), `platform` (2), `sis` (5) |
| `lms` | `academics` (3), `assessment` (2), `comms` (7), `finance` (3), `iam` (67), `master` (4), `org` (16), `platform` (23) |
| `finance` | `academics` (3), `iam` (5), `master` (6), `org` (12), `people` (2), `platform` (1), `sis` (6) |
| `library` | `academics` (4), `finance` (1), `hr` (1), `iam` (3), `org` (5), `people` (3), `platform` (1), `sis` (1) |
| `transport` | `org` (3), `sis` (1) |
| `comms` | `academics` (5), `iam` (27), `org` (26), `platform` (10), `sis` (4) |
| `frontoffice` | `hr` (1), `iam` (2), `org` (7), `platform` (1), `sis` (1) |
| `careers` | `iam` (2), `master` (1), `people` (1), `platform` (2) |
| `privacy` | — |

- **The graph is a DAG.** The 26 references that point against an arrow are event-only: the referencing module cleans them up from the owner's events and never calls the owner for them (listed at the end of `database/logical-references.md`).
- **Enforcement.** Each module calls only the modules in its row, through one `<Callee>GrpcClient` per callee. `ModuleChecks` (core test kit, run by every module's `ModuleBoundaryTest`) fails the build if a module imports the gRPC contract (`com.mysms.<x>.v1`) of a module outside its row; conformance G1 fails it if a module carries such a proto. Both read this table, so keep its format.
- **privacy** keeps 9 evidence references (iam 3, people 3, platform 3) that it never resolves, and calls no module. Any module may ask privacy whether personal data may be used through the shared `privacy-client` library (design [PRIVACY-AND-CONSENT-CENTRE §7](docs/design/PRIVACY-AND-CONSENT-CENTRE.md)); like auth-client's calls to iam, this keeps the graph a DAG.

## 3. Data access: Spring JDBC only, no ORM

No ORM anywhere (no JPA, Hibernate, Spring Data, jOOQ code generation or MyBatis); conformance `TechnologyRule` checks it. Data access is hand-written SQL with Spring JDBC (`JdbcClient`, `NamedParameterJdbcTemplate`).

- Repositories live in the module's `repository` package; SQL is schema-qualified with the module's own schema (`lms.course`) and uses named parameters only.
- Rows map through explicit `RowMapper`s into entities; entities never leave the module (JAVA-CODING-RULES §5–§7).
- Reads add `tenant_id = :tenantId` and, unless deleted rows are wanted, `deleted_at IS NULL`. Writes set `created_by` / `updated_by` and timestamps. Updates use optimistic locking (`… row_version = row_version + 1 WHERE id = :id AND row_version = :expected`); 0 rows → **412** / `ABORTED`, matching the API's `If-Match` contract (409 stays for duplicates and state conflicts).
- Ids are UUIDv7 generated in Java (`core.id.UuidV7`). Enums are `varchar`. `jsonb` goes through Jackson with `CAST(:json AS jsonb)`.
- **Transactions are programmatic.** Each service extends core's `AppTransactionManager`: begin → bind the tenant (`set_config('app.tenant_id', …, true)`) → work → commit, or roll back on any exception or error. Read-only transactions use the module's replica pool when one is configured; `…PrimaryRead…` variants read the primary; `…Global…` variants bind no tenant. `@Transactional` and `TransactionTemplate` are not used.
- Partitioned tables are always queried with their time range (at most 366 days per list).
- Flyway is the only thing that changes a schema. Integration tests run every repository against Testcontainers PostgreSQL migrated with the module's own migrations.

Diagram: [request lifecycle (REST and gRPC)](docs/diagrams/architecture/request-lifecycle-sequence.drawio) — open with draw.io / diagrams.net.

### Tenant isolation at the database

Every tenant-owned table has `FORCE ROW LEVEL SECURITY` with the policy `tenant_id = current_setting('app.tenant_id', true)::uuid`; the service binds the tenant transaction-locally before its first statement. The SQL tenant predicate is the first layer; RLS turns a missed predicate into an empty result. Roles (created by operations, [DEPLOYMENT.md §2.1](DEPLOYMENT.md#21-roles)):

| Role | Use |
|---|---|
| mysms_migrate | Owns the schemas; runs Flyway and partition maintenance |
| mysms_app | Application login; not the owner, so RLS binds it; DML grants only (`ApplicationRoleGrants`) |
| mysms_relay | Outbox relay (BYPASSRLS); `SELECT, DELETE` on `outbox_event` and `inbox_event`, `UPDATE` of `published_at` and `publish_error` only |

Tables without RLS hold no tenant data or are read across tenants by design (for example `<module>.job_schedule`, `iam.tenant_directory`).

### Published content is versioned

Lesson, quiz, question, assignment and rubric rows carry `version` / `superseded_by_id` / `published_at`: a published version is immutable, editing creates a new version, and graded evidence snapshots what it was graded against. Cohorts copy chapters and items from the course template (`copied_from_id`), so teachers adapt copies, never templates.

## 4. Deep links

- One link format for every client: `mysms://<module>/<resource path>`, the API path without `/v1` (e.g. `mysms://lms/cohorts/{cohortId}/gradebook`). Stored in `comms.in_app_notification.action_url` and used in push payloads, e-mail and SMS.
- The Expo apps register the `mysms` scheme; the web app serves the same path under `https://<tenant host>/app/<path>`. A screen a native app lacks opens the web URL (`resolveLink` in `@mysms/core`).
- Links carry no tokens or personal data; the app signs in and loads the record through the API.

## 5. Module interdependencies

### 5.1 Synchronous calls (gRPC)

The caller cannot answer when the callee is down, so these are the dependencies that matter most. Source: each module's `integration/*GrpcClient` (RPCs as named in the callee's proto).

| Caller | Callee | RPCs |
|---|---|---|
| admission | sis | GetStudent, ListStudents, CreateStudent, ListStudentGuardians, CreateStudentGuardian, ListGuardians, CreateGuardian, ListRelationshipTypes, ListStudentEnrollments, CreateStudentEnrollment, ListProgramEnrollments, CreateProgramEnrollment, CreatePortalAccount, CreateStudentPortalAccount |
| admission | people | CreatePerson, ListPersons |
| assessment | sis | GetStudentSummary, ListMyChildren, ListClassRoster, ListSubjectRegistrations |
| assessment | academics / facility / iam | ListCourseSubjects / GetRoom / GetMe |
| attendance | sis | ListClassRoster, ListStudents |
| attendance | timetable | GetSession, ListSessionStudents |
| attendance | org | GetInstitution |
| careers | iam | ResolveTenantCode, GetUserAccount |
| comms | iam / platform | GetMe, ListUserAccounts / CreateUploadUrl, CompleteUpload, GetDownloadUrl |
| finance | sis | GetStudentSummary, ListMyChildren, ListClassRoster, ListProgramEnrollments |
| finance | academics | ListClassSections, ListCourses |
| finance | iam / master / org | GetMe / GetCurrency / GetAcademicYear |
| hr | iam | ListUserAccounts, CreateUserAccount |
| iam | org / people | ListActiveInstitutions / GetMyPerson |
| lms | platform | ListEntityAttachments, GetFileObject, GetDownloadUrl, CreateUploadUrl, CompleteUpload |
| lms | iam | GetMe, ListUserAccounts, ResolveTenantCode |
| lms | assessment | ListGradingBands |
| platform | master | GetCurrency |
| sis | academics | GetClassSection, ListClassSections, GetGradeLevel, GetSection, ListCourseSubjects |
| sis | iam | ListUserAccounts, CreateUserAccount, CreateUserRole, ListRoles |
| sis | org / people | GetAcademicYear, GetInstitution / ListPersons |

28 caller→callee edges, 70 RPCs. Callers of nobody: master, org, people, academics, payroll, facility, timetable, library, transport, frontoffice, privacy. Most called: iam (7 callers), sis (4), org (4), academics (3). Shared libraries add two more: auth-client → iam (`GetRolePermissions`, every module) and privacy-client → privacy (`BatchCheckConsent`, used by people).

### 5.2 Asynchronous (events)

Consumers declare `EventSubscriptions` in their `events` package. The flows:

| Flow | Producer → consumer |
|---|---|
| Reference cleanup (`*ReferenceCleanup`) | an owner's `<aggregate>.deleted` → every module holding a reference ([`logical-references.md`](database/logical-references.md) rule 3) |
| Audit trail | every module's `audit.recorded` → platform (`platform.audit_log`) |
| Permission catalogue | every module's `<module>.api_permissions.published` → iam (`iam.api_permission`) |
| Tenant directory | `platform.tenant.*` → iam (`iam.tenant_directory`); `iam.tenant_directory.bootstrapped` → platform (`platform.tenant`) |
| Local copies | `hr.staff` and `sis.student` → iam (`iam.member_record`); `hr.staff` → payroll (`payroll.staff_member`); `sis.student`, `hr.staff`, `sis.student_guardian` → comms (`comms.student_record`, `comms.staff_record`, `comms.guardian_link`); `sis.student`, `hr.staff` → finance (`finance.student_record`, `finance.staff_record`); `org.institution` → timetable (`timetable.institution_zone`); `comms.message_template` → people (`people.greeting_template`); `people.person`, `sis.student`, `sis.student_guardian` → privacy (`privacy.subject`, `privacy.guardian_link`) |
| Delivery requests | `people.greeting.requested` → comms (an in-app notification to each account comms knows of the person; people cannot call comms); `comms.greeting_delivery.rejected` → people (a greeting comms could not deliver is taken back) |
| Imports | `platform.import.batch_requested` → hr, library, org, sis; `<module>.import.batch_applied` → platform |

Outbox and inbox make these reliable, so they are cheap coupling. Diagram: [events and outbox](docs/diagrams/architecture/events-outbox-sequence.drawio) — open with draw.io / diagrams.net.

### 5.3 Data references (logical, no FK)

Per module in §2. By owner (forward references): org 133, iam 128, academics 59, platform 55, master 26, sis 22, people 18, hr 17, finance 7, comms 7, facility 2, assessment 2, timetable 1. Nothing references payroll, admission, attendance, lms, library, transport, frontoffice, careers or privacy, except event-only references against the DAG.

## 6. Decided reorganisation: platform kernel and products

Approved, not built; scheduled after the privacy work. Full plan, scope and order:
[docs/design/MODULE-AND-PRODUCT-REORGANISATION.md](docs/design/MODULE-AND-PRODUCT-REORGANISATION.md). Until it lands,
the rules of [`CLAUDE.md`](CLAUDE.md) and [`JAVA-CODING-RULES.md`](JAVA-CODING-RULES.md) and the modules of §1 stay
binding.

Target: 22 → 10 modules — kernel (platform, identity, privacy, comms) and products (hrms; school: students, academics,
finance, campus; learning). Products depend only on the kernel, except school → hrms by id and events. HRMS and
learning can each be assembled and sold on their own; the clients are split into product feature packages the same
way.

Rules for any merge:

1. **Same business capability and the same users** (one back-office team or product owner).
2. **Strong synchronous coupling**: more than 3 RPCs or more than 10 references, where one side cannot work without
   the other.
3. **Same change cadence and data sensitivity.** Money (finance) and identity stay apart; auth is the hot path that
   scales on its own.
4. **No new giant.** Nothing merges into lms, already the largest module.
5. **Leaves are free.** A module nothing depends on can join its business area without disturbing anyone.
6. **Keep the schemas, URL prefixes and operation ids**: tables, RLS, migrations and the SDK stay; inside a merged
   module gRPC becomes direct Java calls and cross-schema foreign keys become allowed; the owner rules and conformance
   (`ModuleBoundaryRule`, `MavenLayoutRule`) are amended first.
