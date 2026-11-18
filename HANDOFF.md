# MySMS — session handoff

Rewritten 2026-11-09 for whoever continues (a new Claude session or a developer). Read [CLAUDE.md](CLAUDE.md) first
(the owner's standing instructions and decisions), then this file, then [STATUS.md](STATUS.md) (what is served and
verified) and [api/operations-status.md](api/operations-status.md) (every operation, served or `501`, and why).

## 1. Where everything is

| Path | What |
|---|---|
| `CLAUDE.md` | The owner's standing instructions: role and standard, the decisions of 2026-11-09, how to start any work |
| `README.md`, `STATUS.md`, `DEPLOYMENT.md`, `BUILD-AND-RUN.md` | Overview, verified state, deployment, how to build and run (prerequisites, IDE set-up, troubleshooting) |
| `ARCHITECTURE.md`, `JAVA-CODING-RULES.md`, `PRINCIPAL-REVIEW.md`, `ENHANCEMENTS.md` | Design, the binding Java rules, review findings and their resolution, ideas beyond scope |
| `database/` | Flyway migrations per schema (`migrations/<module>/`), `README.md` (conventions, roles, events, global tables), `data-dictionary.md`, `logical-references.md`, `erd/*.mmd`, `verification/smoke.sql` |
| `api/` | The API guide: `README.md` (API rules), `operations-status.md` (written by the build), `state-machines.md`, `standard-resources.json` (owned data, checked by the build; the clients build their record screens from it) |
| `ui/` | UI specification: 186 screens, flows, design system, frontend architecture, rules |
| `backend/` | The multi-module Maven project only, flat: `core`, `auth-client`, `messaging`, 21 domain modules (one per schema, each owning its `com.mysms.<module>.api` contract package, its protos and its `application.properties`), `app` (the monolith), `conformance` (last). Each domain module also builds as its own microservice |
| `clients/` | pnpm monorepo: apps `web`, `platform-console`, `mobile`, `tablet`; packages `sdk`, `core`, `ui-web`, `ui-native`, `web-kit`, `native-kit`, `tokens`, `i18n`, `fixtures` (test-only), `config` |

There is no Python and no shell tooling. Every generator, check and report is Java inside the Maven build, and every
class the retired generators once wrote is owned source.

## 2a. What changed on 2026-11-10 (owner decision)

The `contracts` Maven module is gone, and with it every OpenAPI→Java generator run and the protoc step (which hung
builds on some machines). Instead, following the owner's reference service pattern:

- Each module owns its contracts as source: `com.mysms.<module>.api` (interfaces with the Spring mappings, implemented
  by the controllers), `com.mysms.<module>.api.model` (the DTOs), and its edge operation table
  (`src/main/resources/META-INF/mysms/operations/<module>.json`). They were materialized once from the generator
  output and are edited directly; the style gates skip the api packages (generator-shaped code).
- springdoc-openapi (3.2.0, via core) serves `/v3/api-docs.yaml` in every service. Core's `OpenApiConfiguration`
  (the reference's `OpenApiConfig` + `OperationCustomizer` pattern) restores the contract operationIds,
  `x-permission`, security schemes and idempotency from the module's operation table. Each module's
  `<M>ServiceComponentTest` fetches the document and writes `target/openapi/<module>.yaml` on every `mvn verify`.
- The build pins the descriptions of each operation together: each module's OpenAPI component test (every documented
  operation is in the operation table, with its permission), conformance G3 (operation table == the module's RPCs),
  `OperationStatusReportTest` (report == operation tables ∧ controllers), and the existing rules.
- Since 2026-11-12 (owner's guideline, CLAUDE.md, JAVA-CODING-RULES §2.1, §10): no `api/openapi` snapshot and no `sdk`
  module. Each module's `<Module>OpenApiComponentTest` writes `target/openapi/api-docs.yml` (the TypeScript SDK and the
  owner's separate client project read it; core's `OpenApiConfiguration` keeps the contract's `required` lists and
  types); each module compiles its own protos and its verbatim copies of its callees'; modules call each other with
  `@GrpcClient` stubs from `<module>/integration`.
- auth-client keeps frozen copies of master/org operation tables as unit-test fixtures; the exhaustive 2049-operation
  routing check moved to `app` (`OperationTablesComponentTest`), where every module's table is on the classpath.

## 2b. Features are now built one at a time, end to end

From 2026-11-10 the remaining work is done feature by feature: the backend operations, the web screens, the tests,
the docs and an independent review, all green before the next one starts.

**Feature 1 — attendance taking (flow F3), on the backend and the web.** STATUS.md "What is served" has the
behaviour and known limit 11 what is still missing (the teacher app, guardian notices, self-views). For whoever builds
the next cross-module feature, the pattern it set:

- **Another module's data is read through its gRPC service with the caller's token** (since 2026-11-12: the
  `<Callee>GrpcClient` classes of `attendance.integration`, configured in attendance's `application.properties`;
  JAVA-CODING-RULES §10.4), behind one lookup class per consumer (`attendance.business.SessionLookup`). Downstream 404 → the row does not exist for this
  caller; 403 → this operation's 403 naming the permission; anything else → 503 (`UnavailableException`), and
  the downstream status or message never reaches the client. Remote reads happen **before** the transaction opens.
- **A reference into another module is checked before the write** (JAVA-CODING-RULES §2): a first mark's
  `studentId` is confirmed by sis (`SessionLookup.unknownStudents`) between a read-only look at the sheet and the
  writing transaction; component tests of the module alone seed the rows a cross-module flow would have created,
  and the `app` component test drives the real flow across the modules.
- **Many rows of one kind go in one statement** (`AttendanceRecordRepository.insertMarks`, `unnest` of arrays, as
  iam's role seeding does); guarded updates of distinct rows stay one statement each.
- **A combined operation needs every permission it stands for**: `RecordAttendance` with `complete` checks the
  completing permission too (the edge filter only checks the operation's own).
- **Web**: hooks per feature in `@mysms/core` (`hooks/attendance.ts`; `api/pages.ts` reads every page of a small
  list), screens in `web-kit/src/screens/<feature>/`, registered in `apps/web/src/routes.ts` by inventory id; a
  list-manager screen links its rows to their own screen with `detail` in `RESOURCE_SCREENS`; unsaved work is
  guarded by `UnsavedChangesGuard`; the fake backend of a feature lives in `@mysms/fixtures` (tests only); apps
  share `src/test/signedIn.tsx`. `@mysms/core` keeps a 90 % coverage threshold, so hooks need their own tests.

**Feature 2 — homework lifecycle (flow F5), on the backend and the web.** One module (assessment), no remote calls.
What it added to the pattern: a workflow whose branch depends on a second permission (publish at once or send for
review) checks that permission in the service; a list-manager screen takes its filter from the address
(`?status=pending`), which is how two inventory screens share one list; the navigation marks the entry whose query
the address carries.

## 2c. Transactions through `AppTransactionManager` (owner decision of 2026-11-10)

The 1,712 hand-written transaction blocks of 363 classes were replaced by calls to the base class every service now
extends (`doInReadTransaction` / `doInWriteTransaction` / `runIn…` / `…Global…`); about 12,000 lines went. Core's
`Transactions` remains for the idempotency store and the messaging relay, which use transaction definitions of their
own. `docs/COMMON-MODULE-REVIEW.md` records the comparison with the owner's `common` module class by class.

## 2d. More of `common`, and the next features (owner decisions of 2026-11-10, second round)

- **Read/write routing** (`common`'s `RoutingDataSource`): every module's DataSource is core's `RoutingDataSource`;
  read transactions use the module's replica pool when `MYSMS_DB_REPLICA_URL` / `MYSMS_<MODULE>_DB_REPLICA_URL` is
  set, write transactions the primary. `DataSourceManager` / `DataSourceWatcher` were not taken (passwords in watched
  files; JAVA-CODING-RULES §6, docs/COMMON-MODULE-REVIEW.md).
- **One hand-built JSON mapper** (`common`'s `CommonConfig`): core's `JsonMappers.create()`, the primary bean.
- **`PagingInfo` / `NavigationInfo` / `PagingUtil`** on every list operation: REST `page`, `size`, repeated `sort`,
  `cursor` → `{data, paging}`; gRPC `pageable` → `pagination` (old fields reserved in the protos and the lock). The
  per-resource `<Resource>List` DTOs went (27 remain as the `{items}` answers of writes and two bounded reads), and so
  did the count queries. Paging under the classes is core's `PageRequest`: keyset by the `next` link's cursor,
  offset (capped at 10,000 rows) for page jumps. Both SDKs are regenerated from the snapshot, whose list schemas are
  now named as springdoc names them (`PagingInfo<Resource>`).
- **Approved leave is cancellable** before it starts; the days go back to the balance (`hr.CancelLeave`).
- **`iam.GetMe` fills `staffIds` and `studentIds`** from `iam.member_record`, kept from hr's and sis's events,
  which now carry each staff or student record's account, person, institution and row version.

## 2e. Batch of 2026-11-10: sis, admission, hr and payroll finished

Every operation of the four modules is served (27 that answered `501`) and their inventory screens are built on
the clients the inventory names (STATUS.md "What is served", known limits 18–21). Built by one agent per module in
its own git worktree, merged and reviewed as one batch. Decisions taken on the owner's behalf, to confirm:

- **Staff status:** active ↔ inactive, either → exited, exited → active (the same record returns). Leaving needs
  no exit date (the contract carries none). The status change publishes `hr.staff.updated` (iam's member sync
  reads created / updated / deleted only).
- **Login accounts:** staff log in with the employee number, students with the admission number (no e-mail or
  mobile attached, so they never clash with a parent's); the person's existing iam account is reused (iam keeps
  one per person). Staff who left get no account. A typed `userId` on the standard staff create / update is
  refused. Each tenant needs roles coded `student` and `guardian`.
- **Applicant self-service** (admission): an account with the application permissions but without the
  institution in scope is an applicant — only its own applications, only open cycles, draft-only changes, only
  submit and cancel. This relies on the applicant role never being granted tenant-wide; an `applicant` role
  template is still to add. Open cycles are readable tenant-wide by holders of the cycle read permission.
- **Step order** of applications follows the database states and operations.md over ui/03 (differences in
  `ui/DISCREPANCIES.md`).
- **202 operations** that finish within the request (bonafide, apply promotions, payslip run, the enrolment's
  portal-account step) are stored already finished; the codebase has no background job pattern yet.
- **Payslip arithmetic:** a period lies inside one calendar month; each monthly component is pro-rated by the days
  a salary of the run's school was in force ÷ the month's days, half-up to the cent; employer-share lines count in
  neither total; an existing payslip of the period is kept (cancel to recalculate).
- **Owner-only payslips without a contract change:** the payroll officer is the holder of
  `payroll.payslip.generate_payslips`; everyone else reads only their own (payroll keeps account ↔ staff record from
  hr's events in `payroll.staff_member`).
- **Applicant numbers:** an applicant's application is numbered from `admission.number_sequence`; the number sent is
  replaced (the contract requires one). `sendInvite: true` is refused by sis and hr alike.
- **Module direction kept:** sis does not call attendance, library or finance (they depend on sis), so the
  student summary leaves attendance, loans and fees empty.

Owner decisions still needed: a self-scoped staff read (limits 14/15/20), whether an explicit own-payslips
permission should replace the officer rule, and a public application operation if ADMN-05 must work without an
account.

## 2. What changed on 2026-11-09

The owner reviewed the repository and set the standard in CLAUDE.md. Done in this session, in this order:

1. **The owner's own recent changes were validated first** and their defects fixed (a wrong argument order in
   `NotificationPreferenceSetService`, messaging test expectations, a duplicate required key in `comms.yaml`,
   client hook tests using the real field names).
2. **Configuration is each module's own `application.properties`** (owner decision of 2026-11-12, replacing the
   configuration core built in code): every value a default over an environment variable; the monolith's own file holds
   what differs in one process and core's `ModulePropertiesEnvironmentPostProcessor` combines every module's file
   below it (JAVA-CODING-RULES §2.1, conformance G9).
3. **Every Python script was replaced by Java in the build** (`backend/conformance`): the design-conformance rules
   as Strategy classes (`ConformanceRule`, one class per rule family), the tenant-predicate SQL parser, the
   permission-catalogue check (P1) and the operation-status report (`OperationStatusReportTest` fails when
   `api/operations-status.md` is stale; `-Dmysms.write-reports=true` rewrites it). The code generators were deleted.
4. **Transactions run on Spring's `PlatformTransactionManager` everywhere.** Each module declares its manager as a
   qualified bean (`<M>DatabaseConfig.TRANSACTIONS`); every service injects it and calls `getTransaction` /
   `commit` / `rollback` itself, with `ModuleDatabase.writeDefinition()` / `readDefinition()` and
   `bindTenant()`. Core's `Transactions.commit/rollback` keep the original failure when a rollback fails;
   `CallContextHolder.withoutTenant()` sets the caller's tenant aside for global writes. Infrastructure
   (`IdempotencyStore`, `InboxProcessor`, `OutboxRelay`) uses the same pattern. The old `TenantTransactionExecutor`
   / `TxWork` were deleted. Tests use the test kit's `TestTransactions.of(database)`.
5. **Platform console screens PLT-01…05** (tenants, SaaS plans, sales enquiries, reserved keywords and notification
   types, system settings) on the shared list manager, with tests.
6. Java 17 everywhere (`--release 17`, Checkstyle 12.3.1), Eclipse/STS (m2e) set-up fixed, IDE and OS files ignored.

## 3. Rules and decisions that bind all further work

CLAUDE.md is the authority. In short: data security first; specs first (`database/`, `api/`, `ui/`); PostgreSQL 16,
one schema per module, no cross-schema foreign keys, joins or writes, no database functions; every SQL statement
states `tenant_id = :tenantId` (rule R1) with row-level security as the backstop; no ORM, no `@Transactional`, no
`TransactionTemplate`, no ArchUnit or Modulith; flat Maven modules; REST and gRPC; RabbitMQ behind MySMS's own
`EventBus`; mocks only in unit tests; tenant-level settings set and seen only by tenant administrators;
student/teacher terminology; no names of reference projects; independent review by another model for
security-relevant changes.

## 4. How to build and verify

Prerequisites, commands and troubleshooting: [BUILD-AND-RUN.md](BUILD-AND-RUN.md). In short:

```bash
cd backend && mvn clean install          # JDK 17; component tests use PostgreSQL 16 and RabbitMQ (Testcontainers or
                                         # MYSMS_TEST_JDBC_URL / MYSMS_TEST_AMQP_URI / MYSMS_TEST_PUBSUB_EMULATOR_HOST)
cd backend && mvn -pl conformance test -Dmysms.write-reports=true   # after serving or adding operations
cd clients && pnpm install && pnpm verify
```

A full `mvn clean install` takes 30–45 minutes and about 6 GB of memory.

## 5. Next steps, in the owner's order

1. **Finish the code-first cutover** — done 2026-11-12: the generators read the springdoc-written documents and the
   `api/openapi` snapshot and the Java `sdk` are gone. The TypeScript `@mysms/sdk` stays until the owner's separate
   client-generation project replaces it.
2. **CRUD layer refactor** — done in two steps on 2026-11-10: transactions are inherited from
   `AppTransactionManager`, and the 139 services of child records were merged into the 67 services of their
   aggregates (one service per aggregate). What remains is the repeated scope / ETag code inside the methods
   (paging is now one call into core's `PageRequest` / `PagingUtil`), which goes into shared helpers, never a
   generic base service.
3. **Remaining backend operations (293 answer `501`)**:

| Area | Ops | What it needs |
|---|---:|---|
| Workflow and custom operations (enrol, approve, issue, pay, grade, publish marks, import/export, password reset by link, verification, impersonation…) | ≈186 | Hand-written logic per operation (`api/state-machines.md`, `api/operations.md`); several need message delivery |
| Public and platform-staff variants (guest catalogue, institution finder, platform views of tenant data) | ≈49 | Guest projections and staff views |
| Read models without a table (dashboards, summaries, gradebook) | ≈33 | Hand-written queries per module; other modules' data through their gRPC services |
| Contract/schema mismatches (no row_version, JSON content, singleton settings…) | ≈49 | A contract or migration decision per resource |

   Infrastructure: gRPC services for all modules and service-to-service identity are done (2026-11-12). Still to
   build: reference reconciliation, message delivery
   (e-mail/SMS/push), gateway rate limiting. The exact list per operation is `api/operations-status.md`.
4. **Remaining screens (93 of 186)**: dashboards, grids (timetable, gradebook, marks entry),
   detail pages with tabs, "my …" views for students, guardians and teachers, the phone and tablet surfaces, the
   tenant-scoped parts of PLT-02 (subscriptions) and PLT-04 (notification channels).

## 6. Open items

- **Idempotency for platform-staff calls.** `IdempotencyFilter` keeps responses only for tenant callers (they are
  encrypted with the tenant's key), so a platform operator's repeated create on a global table runs again; tables
  with a unique key refuse the repeat (409), `platform.sales_enquiry` has none. Fix: keep tenantless responses under
  a platform key.
- **Two decisions for the owner from feature 1.** (a) Settled 2026-11-12: outbound clients live in each module's `integration` package
  (JAVA-CODING-RULES §10.4). (b) `Lookup` was added to
  the SpotBugs `EI_EXPOSE_REP2` class-name allowlist (`backend/config/spotbugs/exclude.xml`) for that class, which
  stores its injected clients like the other listed collaborators; §13 wants each exclusion approved.
- **The bootstrap tenant has no `platform.tenant` row** (STATUS.md known limit 13): iam's first-administrator
  bootstrap creates the tenant only in iam, so platform's audit consumer fails that tenant's `audit.recorded`
  events on `fk_audit_log_tenant_id` (nine warnings in every `app` component-test run). Fix: have the bootstrap
  register the tenant with platform (an event platform consumes, or the deployment step creating the row first).
- **One Flyway baseline per module** (owner decision of 2026-11-10): every later script (the events hardening of every
  module, iam `002`–`009`, the platform and comms global events, homework review, one attendance sheet per session,
  the timetable view indexes) was folded into its module's baseline and deleted; a build of the old scripts and of the
  baselines dumps the same schema and catalogue rows. A local database migrated before must be dropped and recreated
  (Flyway checksums). Until the first release a schema change edits the baseline in place.
- Known limits of the served features: STATUS.md "Known limits".

- **Submissions are institution-wide to read.** A holder of `assessment.homework_submission.read` reads every
  submission of the institution; the student- and guardian-scoped reads belong to the persona operations (ASM-04).

## 7. Before delivering anything

1. `git status` / `git diff` read and the owner's changes validated.
2. `mvn clean install` green on JDK 17 (conformance 0 violations, operation report current); `pnpm verify` green.
3. Docs updated where behaviour changed (STATUS, DEPLOYMENT, database/README, api/README, JAVA-CODING-RULES).
4. An independent review by another model for security-relevant changes; findings fixed with tests.
5. No reference-project names; student/teacher terminology; no Python or shell tooling.
