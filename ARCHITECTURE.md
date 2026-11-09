# MySMS architecture

How MySMS is built and deployed. It complements the database design (`database/README.md`), the API design (`api/README.md`) and the binding Java rules (`JAVA-CODING-RULES.md`, which wins on any code-level detail).

## 1. One module per schema, every module a service

MySMS is a set of **21 independently deployable services**, one per PostgreSQL schema, built from one Maven repository with **flat modules** (no nesting). The same jars run all together (one process, one database instance — development and small tenants) or as separate services, each with its schema on its own PostgreSQL instance. Nothing in the code or the schema differs between the two; moving a module out is configuration plus a data copy.

```text
mysms/              (parent pom: Java 17, Spring Boot 4.1, dependency + plugin management, quality gates)
├── core/           (plain library: base classes, tenancy, errors, paging, ids, transaction executor,
│                    cross-cutting auto-configuration — no schema, no Flyway, no datasource of its own)
├── contracts/      (GENERATED from api/openapi + api/proto: server interfaces, DTOs, gRPC stubs)
├── sdk/            (GENERATED Java clients — the only way one module calls another)
├── auth-client/    (the only auth surface modules touch: AuthContext, token verification, permission filters)
├── master/         (schema `master`: Master Data)
├── platform/       (schema `platform`: Platform & Tenancy)
├── iam/            (schema `iam`: Identity & Access — also the OAuth 2.0 / OIDC issuer)
├── org/            (schema `org`: Organisation & Calendar)
├── people/         (schema `people`: People & Profiles)
├── academics/      (schema `academics`: Academic Structure)
├── sis/            (schema `sis`: Student Information)
├── hr/             (schema `hr`: Workforce / HR)
├── payroll/        (schema `payroll`: Payroll)
├── admission/      (schema `admission`: Admissions)
├── attendance/     (schema `attendance`: Attendance)
├── facility/       (schema `facility`: Rooms & Facilities)
├── timetable/      (schema `timetable`: Timetable)
├── assessment/     (schema `assessment`: Assessment & Results)
├── lms/            (schema `lms`: Learning Management)
├── finance/        (schema `finance`: Fees & Payments)
├── library/        (schema `library`: Library)
├── transport/      (schema `transport`: Transport)
├── comms/          (schema `comms`: Communication & Community)
├── frontoffice/    (schema `frontoffice`: Front Office)
├── careers/        (schema `careers`: Careers)
└── app/            (assembly only: runs every module in one process; no business code)
```

Each domain module has its own `@SpringBootApplication`, its own configuration namespace (`mysms.<module>.*`), its own DataSource and transaction manager, and its own Flyway folder; the build produces both a library jar (used by `app`) and an executable service jar.

**Isolation is structural.** A domain module's Maven dependencies are exactly `core`, `contracts`, `sdk` and `auth-client` — never another domain module — so one module cannot compile against another's classes. The event bus implementation (`messaging-rabbitmq`) is added at runtime scope only. Package layout inside a module is defined in `JAVA-CODING-RULES.md` §3.

**Cross-module rules** (these hold identically in one process or across many):

- **No cross-schema SQL** — no joins, reads or writes outside the module's own schema. Another module's data is read through its SDK client (ids plus compact card projections), or from a local read model kept fresh by that module's events.
- **No cross-module foreign keys.** References into another module are *logical references* (indexed uuid columns) stored as given (no write-time call to the owner, decision of 2026-11-07), protected by archive-not-delete, event-driven cleanup and RLS (a reconciliation job against the owners waits for service-to-service identity) — see `database/logical-references.md`.
- **No distributed transactions.** A change that other modules care about writes its own rows plus an `outbox_event` row in one local transaction; the module's relay publishes it to the bus — MySMS's own `EventBus` interface, RabbitMQ by default, swappable by configuration — and consumers handle it exactly once through their `inbox_event`. Audit records travel the same way into `platform.audit_log`.
- **Module-local infrastructure.** Idempotency keys, long-running operations and document numbering live in each module's own schema; nothing is borrowed from the platform schema at runtime.
- **Calls are remote by contract.** In `app`, SDK clients call the co-located module over loopback HTTP/gRPC — the same path as in production — so assembled and distributed deployments behave the same.
- **Trace context** (W3C `traceparent`) is propagated by `core` on every inbound request, SDK call and published event, so a request can be followed across services.

## 2. Allowed module dependencies

A module may call (through `sdk`) only the modules whose data it references. The table is derived from the 468 forward logical references in `database/logical-references.md`; the number is how many reference columns point at that module. Tenant references are not dependencies (the tenant comes from the token). Every module also uses `core`, `contracts`, `sdk` and `auth-client`.

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

**The graph is a DAG.** The 26 references that point against an arrow are event-only: the owner does not know about them, the referencing module cleans them up from the owner's events, and it never calls the owner for them (listed at the end of `database/logical-references.md`). Each module declares the SDK clients it uses in its configuration; a plain JUnit check in `core`'s test kit fails the build if a module imports an SDK client package outside this table.

## 3. Data access: Spring JDBC only, no ORM

**Rule: no ORM layer anywhere in the project** (no JPA, Hibernate, Spring Data, jOOQ code generation or MyBatis). Data access is plain SQL with Spring JDBC (`JdbcClient`, `NamedParameterJdbcTemplate`).

- Repositories live in the module's `repository` package; SQL is written by hand, schema-qualified with the module's own schema only (`lms.course`), with named parameters only (never string concatenation of values).
- Rows map through explicit `RowMapper`s into entities; entities never leave the module (`JAVA-CODING-RULES.md` §5–§7).
- Every read adds `tenant_id = :tenantId` and, unless deleted rows are wanted, `deleted_at IS NULL`. Every write sets `created_by` / `updated_by` / timestamps. Updates use optimistic locking: `… SET …, row_version = row_version + 1 WHERE id = :id AND row_version = :expected`; 0 rows updated → **412 Precondition Failed** / `ABORTED` with the current row version, matching the API's `If-Match` contract (409 stays for duplicates and state-transition conflicts).
- Ids are UUIDv7 generated in Java. Enums are stored as `varchar` (`Enum.name()` / `valueOf`). `jsonb` is read and written as text through Jackson with a `CAST(:json AS jsonb)` in the SQL.
- **Transactions are programmatic**: `core`'s `TenantTransactionExecutor` drives `PlatformTransactionManager` directly (begin → `SET LOCAL app.tenant_id` → work → commit / rollback). `@Transactional` and `TransactionTemplate` are not used.
- Partitioned tables (see the database README) are always queried with their time range or date so PostgreSQL reads only the needed months.
- Flyway is the only thing that creates or changes the schema. Integration tests run every repository against a Testcontainers PostgreSQL migrated with the module's own migrations, so SQL that drifts from the schema fails the build.

### Tenant isolation at the database

Every tenant-owned table has `FORCE ROW LEVEL SECURITY` with the policy `tenant_id = current_setting('app.tenant_id', true)::uuid`. The module's transaction executor (`core`) issues `SET LOCAL app.tenant_id = :tenantId` before the first statement of every transaction. Four database roles: `mysms_migrate` owns the schemas and runs Flyway; `mysms_app` is the application login (not the owner, so RLS binds it); `mysms_relay` (BYPASSRLS) is the outbox relay's, granted each module's outbox and inbox only; `mysms_platform_admin` (BYPASSRLS) is used only by the platform console service. The tenant predicate in SQL (`tenant_id = :tenantId`) remains as the first layer; RLS is the backstop that turns a missed predicate into an empty result instead of a data breach.

### Published content is versioned

Lesson, quiz, question, assignment and rubric rows carry `version` / `superseded_by_id` / `published_at`: a published version is immutable; editing creates a new version, and graded evidence snapshots what it was graded against (rubric version, points possible, max marks). Cohorts copy chapters/items from the course template (`copied_from_id`), so teachers adapt copies, never templates.

## 4. Deep links

- One link format for every client: `mysms://<module>/<resource path>`, the API path without `/v1` (e.g. `mysms://lms/cohorts/{cohortId}/gradebook`). Stored in `comms.in_app_notification.action_url` and used in push payloads, e-mails and SMS.
- The React Native apps register the `mysms` scheme plus universal links (iOS) / app links (Android) on `https://<tenant host>/app/<same path>`; the React web app routes the same path. A screen that an app does not have opens the web URL instead.
- Links carry no tokens or personal data; the app signs in and loads the record through the normal API operations.

