# MySMS backend

Java 17 · Spring Boot 4.1 · plain Spring JDBC · Flyway · PostgreSQL 16. 10 domain modules serving 22 areas (one
schema each, ARCHITECTURE §1); each module builds to its own service and all assemble into one monolith (`app`). Rules: [JAVA-CODING-RULES.md](../JAVA-CODING-RULES.md).
Module graph and allowed calls: [ARCHITECTURE.md](../ARCHITECTURE.md). Build and run: [BUILD-AND-RUN.md](../BUILD-AND-RUN.md).
Configuration and production: [DEPLOYMENT.md](../DEPLOYMENT.md). Served vs 501 per operation:
[api/operations-status.md](../api/operations-status.md). Open work: [ROADMAP.md](../ROADMAP.md).

Jars: `<module>/target/<module>-0.1.0-SNAPSHOT-exec.jar` (one service, `mvn -pl <module> -am package`) and
`app/target/app-0.1.0-SNAPSHOT-exec.jar` (all modules, `mvn -pl app -am package`).

## Modules

| Module | What it is |
|---|---|
| `core` | Library in every module: UUIDv7, trace context, problem+json errors, ETag/If-Match, masks, keyset paging, `filter` parser, per-module database factory and `AppTransactionManager`, tenant binding for RLS (`TenantJdbcClient`), outbox and audit writers, `IdempotencyFilter`, field encryption (`FieldCipher`), `PartitionMaintenanceJob`, scheduled jobs (`JobRuntime`), gRPC edge. Test-jar: `TestDatabase`, `ModuleChecks` |
| `auth-client` | Token verification, `AuthContext`, permission checks at the HTTP and gRPC edge (`GrpcAuthInterceptor`), service tokens (`ServiceTokenProvider`), role→permission cache. Test-jar: `TestTokens` |
| `messaging` | `EventBus` with RabbitMQ (default), Google Pub/Sub and in-process adapters; outbox relay, exactly-once inbox, dead-letter and replay (`EventsAdmin`) |
| `privacy-client` | Consent checker other modules call (`CachingConsentChecker`): gRPC to `privacy` as the caller's service principal, TTL cache, fails closed; `PrivacyTaskHandler`, the base of a module's erasure/export tasks and retention sweeps (legal holds asked through `ListActiveHolds`, fail closed) |
| 10 domain modules | `platform` (areas platform, master, org), `identity` (iam — also the OAuth 2.0 / OIDC issuer —, people), `privacy`, `comms`, `hrms` (hr, payroll), `students` (sis, admission, careers), `academics` (academics, timetable, attendance, assessment), `finance`, `campus` (library, transport, frontoffice, facility), `learning` (lms). Each area owns schema `<area>` (migrations in `database/migrations/<area>`), its contract package `com.mysms.<module>[.<area>].api`, its protos and its operation table `META-INF/mysms/operations/<area>.json` (JAVA-CODING-RULES §2.2) |
| `app` | Assembly only: all domain modules in one process, each with its own pool and Flyway history |
| `conformance` | Last in the reactor: checks the mechanical design rules over the whole repository and writes the generated reports (`OperationStatusReportTest`) |

## What every module gets

- Standard operations (List/Get/Create/Update/Delete) with validation, 409 uniqueness, 422 references, If-Match
  (428/412), masks, filters, keyset paging, soft delete, audit and outbox events, institution data scope
  (JAVA-CODING-RULES §4; resources: [api/standard-resources.json](../api/standard-resources.json)).
- State transitions as listed in [api/state-machines.md](../api/state-machines.md); 409 `INVALID_STATE_TRANSITION`
  from any other state.
- Idempotency-Key where the contract declares it; long-running operations (`List`/`Get`/`CancelOperation`) in the
  modules that have them; monthly partitions for high-volume tables.
- `tenant_id = :tenantId` on every statement, FORCE row-level security as backstop; per-tenant AES-256-GCM for
  `*_enc` columns, write-only and shown masked.
- An unimplemented contract operation answers **501 `NOT_IMPLEMENTED`**, never 404.

## Service rules not covered in JAVA-CODING-RULES

- Write many rows of one kind in one statement (`unnest` of arrays, as `AttendanceRecordRepository.insertMarks`);
  guarded updates of distinct rows stay one statement each.
- A combined operation checks every permission it stands for (e.g. `RecordAttendance` with `complete` also checks
  the completing permission); the edge filter checks only the operation's own.
- A workflow whose branch depends on a second permission checks it in the service (homework: publish at once or
  send for review).
- Component tests of one module seed the rows a cross-module flow would create; the `app` component test drives the
  real flow across modules.
