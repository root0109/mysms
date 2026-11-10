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
| `api/` | OpenAPI contracts (`openapi/<module>.yaml`, 2,049 operations), gRPC protos (`proto/`), `README.md` (API rules), `operations-status.md` (written by the build), `state-machines.md`, `standard-resources.json` (owned data, checked by the build; the clients build their record screens from it) |
| `ui/` | UI specification: 186 screens, flows, design system, frontend architecture, rules |
| `backend/` | The multi-module Maven project only, flat: `core`, `sdk`, `auth-client`, `messaging`, 21 domain modules (one per schema, each owning its `com.mysms.<module>.api` contract package), `app` (the monolith), `conformance` (last). Each domain module also builds as its own microservice |
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
- The conformance build pins the three descriptions of each operation together: `EdgeOperationTablesTest` (operation
  table == contract snapshot), `OperationStatusReportTest` (report == controllers ∧ snapshot), and the existing rules.
- The `api/openapi` YAML snapshot remains the input of the TypeScript and Java SDK generators; cutting the SDKs over
  to the springdoc-written documents retires it (next steps).
- gRPC stubs compile in `sdk` from `api/proto` — one protoc run with OS-resolved binaries (`os-maven-plugin` +
  xolstice `protobuf-maven-plugin`, the owner's reference mechanism; the previous dynamic-download plugin stalled
  builds). Modules get every stub from the `sdk` dependency; the resolvers remain to write.
- auth-client keeps frozen copies of master/org operation tables as unit-test fixtures; the exhaustive 2049-operation
  routing check moved to `app` (`OperationTablesComponentTest`), where every module's table is on the classpath.

## 2b. Features are now built one at a time, end to end

From 2026-11-10 the remaining work is done feature by feature: the backend operations, the web screens, the tests,
the docs and an independent review, all green before the next one starts.

**Feature 1 — attendance taking (flow F3), on the backend and the web.** STATUS.md "What is served" has the
behaviour and known limit 11 what is still missing (the teacher app, guardian notices, self-views). For whoever builds
the next cross-module feature, the pattern it set:

- **Another module's data is read through its SDK client with the caller's token**, behind one gateway class per
  consumer (`attendance.business.SessionLookup`, an Adapter). Its base URLs are the module's own settings
  (`AttendanceClientsConfig` → `mysms.attendance.*-base-url` ← `MYSMS_TIMETABLE_URL`, `MYSMS_SIS_URL`,
  `MYSMS_ORG_URL`; in the monolith all three are its own URL). Downstream 404 → the row does not exist for this
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

## 2. What changed on 2026-11-09

The owner reviewed the repository and set the standard in CLAUDE.md. Done in this session, in this order:

1. **The owner's own recent changes were validated first** and their defects fixed (a wrong argument order in
   `NotificationPreferenceSetService`, messaging test expectations, a duplicate required key in `comms.yaml`,
   client hook tests using the real field names).
2. **Configuration is built by Spring code, not YAML.** Core's `ModuleDefaultsEnvironmentPostProcessor` reads the modules of a process from
   the packages its application class scans (the `*Module` descriptor classes were removed on 2026-11-10) and
   assembles the configuration for them — one module is a microservice, several are the monolith — as
   the lowest-precedence property source of environment-variable placeholders. The 43 hand-kept YAML files are gone.
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

1. **Finish the code-first cutover** — the contracts are owned source and springdoc writes each module's document
   (§2a); what remains is pointing the SDK generators at the springdoc output instead of the `api/openapi` snapshot
   and then retiring the snapshot. Pilot findings that still matter: Spring 7 serves `@HttpExchange` interfaces, so
   the owned api interfaces could one day double as the Java client and replace the generated `sdk`; the vendor
   extensions that must survive the cutover are `x-permission` (restored by `OpenApiConfiguration` already),
   `x-extensible-enum` (426 schemas), `x-displayName` and `x-owner-field`; the TypeScript SDK and the conformance
   checks would then read the written documents.
2. **CRUD layer refactor** — done in two steps on 2026-11-10: transactions are inherited from
   `AppTransactionManager`, and the 139 services of child records were merged into the 67 services of their
   aggregates (one service per aggregate). What remains is the repeated scope / ETag code inside the methods
   (paging is now one call into core's `PageRequest` / `PagingUtil`), which goes into shared helpers, never a
   generic base service.
3. **Remaining backend operations (340 answer `501`)**:

| Area | Ops | What it needs |
|---|---:|---|
| Workflow and custom operations (enrol, approve, issue, pay, grade, publish marks, import/export, password reset by link, verification, impersonation…) | ≈208 | Hand-written logic per operation (`api/state-machines.md`, `api/operations.md`); several need message delivery |
| Public and platform-staff variants (guest catalogue, institution finder, platform views of tenant data) | ≈54 | Guest projections and staff views |
| Read models without a table (dashboards, summaries, gradebook) | ≈33 | Hand-written queries per module; other modules' data through their SDK |
| Contract/schema mismatches (no row_version, JSON content, singleton settings…) | ≈49 | A contract or migration decision per resource |

   Infrastructure still to build: gRPC services (resolvers, auth interceptor, error mapping, parity tests),
   service-to-service identity (PRINCIPAL-REVIEW P1-34), reference reconciliation, message delivery
   (e-mail/SMS/push), gateway rate limiting. The exact list per operation is `api/operations-status.md`.
4. **Remaining screens (98 of 186)**: dashboards, wizards, grids (timetable, gradebook, marks entry),
   detail pages with tabs, "my …" views for students, guardians and teachers, the phone and tablet surfaces, the
   tenant-scoped parts of PLT-02 (subscriptions) and PLT-04 (notification channels).

## 6. Open items

- **Idempotency for platform-staff calls.** `IdempotencyFilter` keeps responses only for tenant callers (they are
  encrypted with the tenant's key), so a platform operator's repeated create on a global table runs again; tables
  with a unique key refuse the repeat (409), `platform.sales_enquiry` has none. Fix: keep tenantless responses under
  a platform key.
- **Two decisions for the owner from feature 1.** (a) `attendance.business.SessionLookup` is an adapter over SDK
  clients and handles HTTP types; JAVA-CODING-RULES §4 keeps those out of the business layer, but the module
  layout has no package for outbound adapters — name one (e.g. `client`) and it moves. (b) `Lookup` was added to
  the SpotBugs `EI_EXPOSE_REP2` class-name allowlist (`backend/config/spotbugs/exclude.xml`) for that class, which
  stores its injected clients like the other listed collaborators; §13 wants each exclusion approved.
- **The bootstrap tenant has no `platform.tenant` row** (STATUS.md known limit 13): iam's first-administrator
  bootstrap creates the tenant only in iam, so platform's audit consumer fails that tenant's `audit.recorded`
  events on `fk_audit_log_tenant_id` (nine warnings in every `app` component-test run). Fix: have the bootstrap
  register the tenant with platform (an event platform consumes, or the deployment step creating the row first).
- **`V1_030_004__iam_permission_catalogue.sql`** still names the retired Python generator in its header comment. It is
  left unchanged on purpose: editing an applied migration changes its Flyway checksum.
- Known limits of the served features: STATUS.md "Known limits".

- **Submissions are institution-wide to read.** A holder of `assessment.homework_submission.read` reads every
  submission of the institution; the student- and guardian-scoped reads belong to the persona operations (ASM-04).

## 7. Before delivering anything

1. `git status` / `git diff` read and the owner's changes validated.
2. `mvn clean install` green on JDK 17 (conformance 0 violations, operation report current); `pnpm verify` green.
3. Docs updated where behaviour changed (STATUS, DEPLOYMENT, database/README, api/README, JAVA-CODING-RULES).
4. An independent review by another model for security-relevant changes; findings fixed with tests.
5. No reference-project names; student/teacher terminology; no Python or shell tooling.
