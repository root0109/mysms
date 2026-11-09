# MySMS — session handoff

Written 2026-11-07 at the close of a long build session, for whoever continues (a new Claude session or a developer).
Read this first, then [STATUS.md](STATUS.md) (what is served and verified) and
[api/operations-status.md](api/operations-status.md) (every operation, served or `501`, and why).

## 1. Where everything is

| Path | What |
|---|---|
| `README.md`, `STATUS.md`, `DEPLOYMENT.md` | Overview, verified state, how to deploy and run every part |
| `ARCHITECTURE.md`, `JAVA-CODING-RULES.md`, `PRINCIPAL-REVIEW.md`, `ENHANCEMENTS.md` | Design, the binding Java rules, review findings and their resolution, ideas beyond scope |
| `database/` | Flyway migrations per schema (`migrations/<module>/`), `README.md` (conventions, roles, events, global tables), `data-dictionary.md`, `logical-references.md` (cross-module references and their cleanup), `erd/*.mmd`, `verification/smoke.sql` |
| `api/` | OpenAPI contracts (`openapi/<module>.yaml`, 2,052 operations), gRPC protos (`proto/`), `README.md` (API rules), `operations-status.md` (generated), `state-machines.md`, `standard-resources.json` (generated; the clients build generic screens from it) |
| `ui/` | UI specification: 186 screens, flows, design system, frontend architecture, rules (`ui/05` §13: mocking only in tests) |
| `backend/` | The multi-module Maven project only (27 child modules plus the parent reactor, flat): `core`, `contracts`, `sdk`, `auth-client`, unified `messaging`, 21 domain modules (one per schema), `app` (the monolith). Each domain module also builds as its own microservice |
| `clients/` | pnpm monorepo: apps `web`, `platform-console`, `mobile`, `tablet`; packages `sdk` (OpenAPI Generator), `core`, `ui-web`, `ui-native`, `web-kit`, `native-kit`, `tokens`, `i18n`, `fixtures` (test-only), `config` |
| `tools/backend/` | Generators and checks (Python): `crud/gen.py` (standard operations), `crud/gen_events.py` (reference cleanup), `crud/gen_operations.py`, `crud/specgen.py` (samples proven against a database), `gen_backend_modules.py` (poms, config), `conformance.py` (design rules), `operation_status.py` |

`_to_delete/` was the holding area for files moved out of the tree (deletes on the PC were blocked); it was emptied
and removed at handoff.

## 2. State at handoff

- **Backend baseline (before the messaging refactor):** 1,663 of 2,052 operations served; 389 answer `501 NOT_IMPLEMENTED`
  with their reason. The last verified Maven build covered the earlier messaging layout (27 child modules, 28 reactor
  projects including the parent) and real PostgreSQL/RabbitMQ; run the checks in §4 after messaging changes before
  treating that result as current.
- **Clients:** `pnpm verify` green; 76 of 186 inventory screens built in the web app (the rest are routed and show a
  placeholder naming the screen); console: master data, operations and jobs; phone and tablet: sign-in, profile,
  settings.
- **Checks:** `conformance.py` 0 violations; `specgen.py` 327 specs, 0 failures; all 34 ER diagrams render.
- **Events:** all adapters are in one `messaging` library: RabbitMQ (default), Google Cloud Pub/Sub, and in-process.
  The outbox relay and inbox remain in `core`; RabbitMQ uses delayed retries, dead-letter/unrouted queues and replay,
  while Pub/Sub uses source topics, IAM, retries and consumer dead-letter subscriptions.
- The exact figures of the last verified build are in STATUS.md "How this was verified".

## 3. Rules and decisions that bind all further work

Stated by the project owner; do not relitigate them.

- **Specs first.** The specifications in `database/`, `api/` and `ui/` are the source of truth; code follows them, and
  a spec gap is fixed in the spec first.
- **Monolith and microservices.** Every domain module builds into the monolith (`app`) and as its own service.
- **Database.** PostgreSQL 16, one schema per module, Flyway. **No cross-schema foreign keys, joins or writes** (each
  schema may move to its own instance). Lightweight: tables, no database functions, avoid jsonb and joins where
  possible, no generic lookup tables. Every tenant table has `tenant_id` with `FORCE ROW LEVEL SECURITY`.
- **References across modules** are stored unchecked (decision C, 2026-11-07) and cleaned up from the owner's
  `*.deleted` events (`database/logical-references.md`).
- **Java.** Spring Boot 4.1, Java 17, JdbcClient. **No JPA or any ORM. No `@Transactional`, no
  `TransactionTemplate`** (core's `TenantTransactionExecutor` on `PlatformTransactionManager`). No templatized/generic
  service layer (one plain service per aggregate). No ArchUnit or Modulith. Flat Maven modules, no product prefix.
- **The code-shape rules of JAVA-CODING-RULES §5–§9, §11 and §16 are explicit and checked** (sharpened 2026-11-07 against a
  reference service that broke them): the exact entity audit set and Lombok set, no Jackson/mapping annotations or
  view data on entities, projections as records in `repository/`, hooks called by the service; plain repository
  classes; contract-first generated DTOs, PATCH + `updateMask`, contract shapes only, read mask; one mapper
  configuration with full way-in ignores and no null strategies; controllers implementing the generated interfaces
  with no side effects; problem+json only, no exception messages to clients, one typed exception per meaning; no token
  handling outside `auth-client`, the edge enforcing every permission, real signed tokens in tests. `conformance.py`
  enforces them (N1, R5, C3, M1, D2, E2, X1, T); each check was proven by planting one violation per rule.
- **Every SQL statement states the tenant** (`tenant_id = :tenantId`; rule R1, checked by `conformance.py`); RLS is
  the backstop.
- **SDKs** with OpenAPI Generator only. **REST and gRPC** must both be served (gRPC not built yet, see §6).
- **Events:** RabbitMQ is the default implementation behind MySMS's own classes, so the broker can be replaced.
- **Data security is of utmost importance.** Tenant-level settings are set — and seen — only by the tenant-admin role.
- **Mocking only in unit tests** (backend: component tests use real PostgreSQL/RabbitMQ; UI: fakes only inside tests,
  no mock server; ESLint enforces it).
- **Terminology:** student and teacher, everywhere.
- **No names of the reference projects** anywhere in the repository; MySMS is written as if from scratch.
- **`backend/` holds only the Maven project**; tooling lives in `tools/`.
- **UI:** beautiful, user friendly, configurable per user type and role; screens are built alongside the backend.
- **Settings:** hierarchical tenant → school → department/class → user; for staff in several departments the primary
  department's value wins.
- **Delivery:** complete and verified; everything linted and built; flag anything incomplete honestly; independent
  review by a different model for high-stakes changes.

## 4. How to build and verify

Prerequisites: JDK 17, Maven 3.9, PostgreSQL 16, Docker (Testcontainers) or PostgreSQL/RabbitMQ/Pub/Sub emulator,
Node 22 + pnpm, Python 3.

```bash
# backend: everything, with tests (component tests use PostgreSQL, RabbitMQ and Pub/Sub emulator)
export MYSMS_TEST_JDBC_URL=jdbc:postgresql://localhost:5432/postgres MYSMS_TEST_JDBC_USER=postgres MYSMS_TEST_JDBC_PASSWORD=...
export MYSMS_TEST_AMQP_URI=amqp://guest:guest@localhost:5672      # or leave unset: Testcontainers starts one
export MYSMS_TEST_PUBSUB_EMULATOR_HOST=localhost:8085            # or leave unset: Testcontainers starts one
cd backend && mvn install

# generators and checks, after a change to a contract, a migration or a generator
python3 tools/backend/crud/gen.py
python3 tools/backend/crud/merge_controllers.py
python3 tools/backend/operation_status.py
PGHOST=localhost PGUSER=postgres python3 tools/backend/crud/specgen.py
python3 tools/backend/conformance.py
python3 tools/backend/gen_backend_modules.py      # poms and module configuration

# clients
cd clients && pnpm install && pnpm verify
```

Running and deploying (monolith, microservices, roles, keys, RabbitMQ and Pub/Sub setup, bootstrap): DEPLOYMENT.md.

Practical notes:
- A full `mvn install` takes 30–45 minutes and about 6 GB of memory; on a 7 GB machine run nothing else alongside it.
- On a shared RabbitMQ, a queue keeps the arguments of the run that created it; the monolith test removes its
  modules' queues first. In production a mismatch stops the service on purpose (DEPLOYMENT §5.7).
- Generated files carry a `GENERATED` mark and are rewritten by their generator; never edit them by hand — change
  the generator, the contract or `tools/backend/crud/overrides.json`.

## 5. Work in progress at handoff: the global-table operations (35)

The next chunk was started and is half done. **Done and verified:**
- Contract: platform staff may read notification types and their properties (`comms.yaml`, `platformAdminAuth` added
  to the four reads); the SaaS plan reads state their guest projection (everyone but platform staff sees active plans
  only, without `createdBy`/`updatedBy`).
- Migrations `platform/V1_020_003`, `comms/V1_190_003`, `iam/V1_030_007`: `tenant_id` nullable in
  `platform.outbox_event`, `comms.outbox_event` and `platform.audit_log` under two RLS policies (`tenant_isolation`,
  `global_insert`: a tenantless row only from a tenantless transaction); `iam.tenant_directory.source_version`.
  Probed on PostgreSQL as an RLS-bound role: global rows are written only by tenantless transactions (also on a reused
  connection), a tenant session can neither forge one nor write another tenant's row, and no session reads them.
- Docs: database/README "Global tables", data dictionary, ERD.

**Remaining, in this order:**
1. **Generator** `tools/backend/crud/gen_global.py`, called from `gen.py` `main()` for the resources `analyse()` skips
   with "global table" (all except `master`, which stays hand-written): platform `Tenant`, `SaasPlan`,
   `ReservedKeyword`, `SalesEnquiry`, `SystemSetting`; comms `NotificationType`, `NotificationTypeProperty` (child of
   NotificationType).
   - Reuse `gen.py`'s field analysis (split the field part out of `analyse()`), `unique_checks`, `fk_checks`,
     `child_checks`, and `entity_src` / `repository_src` / `mapper_src` with a `global` flag (no `tenant_id` column,
     no `tenant_id = :tenantId`, no `DataScope`). `controller_src` is shared as is (the `analysed` dict has the same
     shape), so a tag mixing global and tenant resources still gets one controller.
   - A new service template modelled on `master/business/CountryService.java`: `database.tx().readWithoutTenant` /
     `writeWithoutTenant`, validation, uniqueness (409), If-Match (412), child check on delete (409, a notification
     type with properties), audit plus `<table>.created|updated|deleted` on the module's outbox. Event payload: `id`
     and `rowVersion`; for `platform.tenant` also `code` and `status` (needed by step 3).
   - Authorization is at the edge (the operation's schemes: writes `platformAdminAuth` only). In the service, the
     SaaS plan guest projection: when `AuthContext.current()` is empty or not `platformOperator()`, list and get
     return active plans only (an inactive one is 404) with `createdBy`/`updatedBy` nulled.
   - `SalesEnquiry` create is anonymous (public form): no actor; it needs the gateway rate limiting of §6 before it is
     exposed to the internet.
   - Generated component test per module (platform, comms): staff token CRUD, tenant-user token refused on writes
     (403), guest and tenant reads of plans projected, 409/412 paths, the audit record stored with no tenant and
     invisible to a tenant session.
   - `served.json` / `standard-resources.json`: publish them with `scope: "global"`; check that the clients' generic
     screens accept that scope.
2. **Global audit storage.** `PlatformAuditTrail.record`: for an event without a tenant, insert into
   `platform.audit_log` with `tenant_id` NULL inside `database.tx().writeWithoutTenant`, with
   `ON CONFLICT (id, occurred_at) DO NOTHING` (no inbox for tenantless events, so the insert itself must be
   idempotent). Today such records — `master`'s too — are only logged. Update its component test.
3. **Tenant directory sync.** A hand-written `iam/events/TenantDirectorySync` (`EventSubscriptions`, merged with
   iam's reference cleanup): on `platform.tenant.created|updated|deleted` upsert `iam.tenant_directory` (`tenant_code`,
   `status`, deleted → `closed`) inside `writeWithoutTenant`, applying an event only when its `rowVersion` is newer
   than `source_version`. Test: out-of-order and repeated events.
4. Console list managers for the seven resources (platform-console app), docs (STATUS "What is served", DEPLOYMENT if
   anything operational changes), regenerate `operations-status.md`, full build, `pnpm verify`, independent review.

## 6. What is left — backend

| Area | Size | What it needs |
|---|---:|---|
| Global-table operations | 35 ops | §5 |
| Workflow and custom operations (enrol, approve with approval records, issue, pay, grade, publish marks, import/export, password reset by link, e-mail/mobile verification, impersonation, role switching…) | 216 ops | Hand-written business logic per operation (`api/state-machines.md`, `api/operations.md`); several need message delivery first |
| Public and platform-staff variants of tenant data (guest catalogue, institution finder, platform administration) | 54 ops | Guest projections (published rows, internal fields omitted) and staff views |
| Read models without a table (dashboards, summaries, gradebook, outlines…) | 35 ops | Hand-written queries per module (no cross-schema joins: other modules' data through their SDK) |
| Contract/schema mismatches (no row_version, JSON content, read-only NOT NULL columns, a payment's allocations, singleton settings) | 49 ops | One decision per resource, in the contract or a migration (listed in `api/operations-status.md`) |
| gRPC | all services | Stubs and messages are generated; still to write: resolvers per service, the gRPC auth and permission interceptor (same rules as the REST `AuthFilter`), error → `Status` mapping, proto ↔ DTO bridge, REST/gRPC parity tests |
| Service-to-service identity (PRINCIPAL-REVIEW P1-34) | — | Service-principal tokens and per-operation audience; unlocks batch card reads (names of records held by other modules) and the reference reconciliation job |
| Reference reconciliation | — | Periodic per-module check of logical references against their owners (reports orphans, never fixes); needs P1-34 |
| Message delivery | — | E-mail/SMS/push providers behind MySMS's own interface, fed by comms' outbound messages; needed by password reset by link, verification and notifications |
| Gateway rate limiting | — | Per-IP and per-account limits at the edge, first on the anonymous operations (sign-in, sign-up, sales enquiry, password reset) |
| Known limits | 10 | STATUS.md "Known limits": deliberate, documented gaps (union of institution grants, unchecked cross-school references, personal rows not yet owner-limited, forced password change not yet enforced server-side…) |

Suggested order: §5 → message delivery → P1-34 → the workflow operations module by module (each with its screens) →
read models → public/staff variants → gRPC → mismatches → rate limiting before any public launch.

## 7. What is left — frontend

| Area | What it needs |
|---|---|
| 110 inventory screens beyond list managers (`ui/` inventory) | Dashboards; wizards (promotion, enrolment, admission); grids (take attendance, timetable builder week view, gradebook, marks entry); detail pages with tabs (student 360°, staff profile); "my …" views for students, guardians and teachers; reports |
| Names from other modules in lists | A student's name lives in `people`: lists show the record's own fields until batch card reads exist (P1-34) |
| Platform console | Tenants, SaaS plans, reserved keywords, sales enquiries, system settings, notification types (with §5) |
| Phone and tablet apps | Today sign-in, profile and settings only: the student, guardian and teacher surfaces and the tablet LMS (lessons, quizzes, live classes) |
| Screens of the workflow operations | Each workflow operation of §6 gets its step in the UI (approve, publish, issue…) as it is served; list managers already offer the generated state transitions |
| Configurability | Per-role navigation and dashboards are specified (`ui/`), built only for the list managers' grouping |

Rules for the UI: beautiful, user friendly and configurable per user type and role; built from the design system
(`clients/packages/tokens`, `ui-web`, `ui-native`); fakes only inside tests; every screen through the generated SDK.

## 8. Before delivering anything

1. Generators run, nothing hand-edited in GENERATED files.
2. `conformance.py` 0 violations; `specgen.py` 0 failures; `operation_status.py` regenerated.
3. `mvn install` green in all modules; `pnpm verify` green.
4. Docs updated where behaviour changed (STATUS, DEPLOYMENT, database/README, API README, JAVA-CODING-RULES).
5. An independent review by another model for security-relevant changes; fix with tests.
6. No reference-project names; student/teacher terminology.

## Open item found while making the build green

- **Idempotency for platform-staff calls.** `IdempotencyFilter` keeps responses only for tenant callers (they are
  encrypted with the tenant's key), so a platform operator's repeated create on a global table runs again. Tables
  with a unique key refuse the repeat (409, asserted by `CrudContract`); `platform.sales_enquiry` has none. Fix: keep
  tenantless responses under a platform key (`FieldCipher` with a reserved tenant id) and drop the global branch in
  `CrudContract.create`.

## Historical build state before the messaging refactor (verified 2026-11-08)

- **Java 17 everywhere:** `java.version` 17 (`--release 17`) in the parent pom, Checkstyle 12.3.1 (13+ needs Java 21).
  `mvn clean install` on Temurin JDK 17.0.17: BUILD SUCCESS, 28 modules, 3,925 tests, all gates.
- **Backend `mvn install`: BUILD SUCCESS** — all 28 reactor modules, unit and component tests (real PostgreSQL 16 with
  row-level security, real RabbitMQ), Checkstyle, SpotBugs and JaCoCo gates. `conformance.py`: 0 violations;
  `specgen.py`: 334 specs, 0 failures.
- **Clients `pnpm verify`: 39/39 tasks successful** (prettier, SDK generation, typecheck, lint, tests, builds).
- How to build and run, prerequisites and troubleshooting: [BUILD-AND-RUN.md](BUILD-AND-RUN.md).
- Fixed on the way to green: global-table RLS read policies (`global_read`) for the idempotent audit insert and the
  outbox read-back; tenantless writes now drop the caller's tenant from the call context (global events even from a
  public form); public creates (`publicCreate` in crud-spec) tested as such; global rows are not blocked by tenants'
  references; the boundary check accepts `ON CONFLICT … DO UPDATE SET`; Eclipse (m2e) no longer runs the quality
  gates on auto-build (`<?m2e ignore?>`).
- UI rework done so far: task-based navigation groups, "find a screen" (`/`, Ctrl/⌘-K), groups open on the active
  page. Still to do: role homes (ui/02 §2.1.1), detail pages with related-record tabs, richer list pages,
  platform-console screens for the seven global resources.
