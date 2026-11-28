# MySMS Java coding rules

Binding rules for every line of Java written for MySMS. They adapt a reviewed enterprise rule set (ADRs and conventions from an internal platform distribution) to MySMS's own decisions; where the two disagreed, MySMS's established architecture wins — the deviations are listed in [Appendix B](#appendix-b--source-material-and-deviations). The SDD/OpenSpec workflow of the source was deliberately **not** adopted; only the engineering rules were.

**Precedence (highest first):** `database/README.md` + migrations (schema truth) → `api/` contracts (interface truth) → `ARCHITECTURE.md` (module/dependency truth) → this file (code-shape truth). A rule here never licenses drift from the contract or the schema.

**Audience:** humans and AI agents equally. An agent that cannot satisfy a MUST stops and says so; it does not approximate.

---

## 1. Hard constraints (never violated, by anyone)

1. **No ORM, ever.** No JPA, no Hibernate, no Spring Data JDBC repositories, no entity proxies, no `ddl-auto`. Persistence is plain Spring JDBC (`JdbcClient` / `NamedParameterJdbcTemplate`) with hand-written, reviewable SQL. (Project rule; reinforced by the source ADR that rejected JPA for batch-oriented write paths.)
2. **Flyway owns the schema.** No DDL from application code, no schema generation, no manual DDL in shared environments. The one exception is core's partition maintenance (database/README "Partitioned high-volume tables"): it only attaches next months' partitions to the tables Flyway created and, when a retention is configured, drops months older than it — with a lock timeout, as the schema owner, never touching any other object. Until the first release each module has exactly one migration, its baseline, and a schema change edits it in place (owner decision of 2026-11-10; §14). From the first release on, a migration applied in any non-ephemeral environment is immutable — never edited, renamed, reordered or deleted; corrections are new migrations.
3. **Never** drop or truncate tables or schemas (except the configured retention drop of a past month's partition, rule 2); never delete a released migration file; never force-push protected branches.
4. **Never commit secrets, credentials or personal data** to version control — including test fixtures, example payloads and OpenAPI examples. Student/guardian data is sensitive by default: synthetic names only, everywhere.
5. **No personal data in logs, error messages, metrics or traces.** `*_enc` values are write-only and never logged; display uses the masked form (`••••1234`, from a `*_masked` column or computed in memory from the decrypted value). Error `detail` strings carry ids, not names.
6. **Every SQL statement is tenant-scoped.** Every transaction binds `app.tenant_id` (`ModuleDatabase.bindTenant()`, transaction-local); every hand-written read adds `tenant_id = :tenantId` (RLS is the backstop, not the mechanism) and, unless deleted rows are wanted, `deleted_at IS NULL`.
7. **`backend/` is the Maven multi-module project and nothing else**: poms, `src/`, build configuration (`config/`, `.mvn/`). Every tool the build needs is Java inside the build: the `conformance` module (last in the reactor) checks these rules and fails on anything else (M0). No Python or shell scripts.
8. **Mocking only in unit tests.** Component, contract and end-to-end tests run against real PostgreSQL and real HTTP; no Mockito, `@MockBean` or stub servers outside `src/test`.
9. **This file is edited by humans** (or by an agent only when the change itself is the requested task). An agent never weakens a rule here to make its own diff pass, and never adds lint/static-analysis suppressions for findings it introduced without developer approval.

---

## 1a. Javadoc and comments: short and to the point

A javadoc says what the class or method is for and anything a caller cannot see from the
signature (a rule it enforces, a refusal it answers, a non-obvious choice) — in one to three sentences. A comment
explains *why*, never *what* the next line does.

- No file or document references that do not help the reader act (`(JAVA-CODING-RULES §4)`, `(DEPLOYMENT.md §4)`,
  `(the reference service's …)`, `(owner decision of …)`, review or ticket ids). Cite a rule only where the code
  looks wrong without it.
- No restating the code, the parameter names, the class's collaborators or its history ("was …", "since …").
- No `@param` / `@return` that repeat the name or type; keep them only when they add a constraint (`blank = none`).
- Generated code is not documented by hand.

```java
// ✗
/**
 * sis's client of the academics service (the reference service's gRPC client shape, JAVA-CODING-RULES §10.4): the
 * starter injects the blocking stubs of the client academics-service (grpc.client.academics-service.*); every call
 * carries the caller's own identity (auth-client's GrpcCallerInterceptor), and a failed call is an
 * IntegrationGrpcClientException the academics lookup maps onward.
 */
// ✓
/** Calls the academics service; a failed call throws {@link IntegrationGrpcClientException}. */
```

## 2. Maven multi-module layout

> Amended by §2.2: a domain module serves one or more areas, each one schema.

One repository, one parent POM, **flat modules — no nesting, no second level**. One Maven module per database schema; each domain module **is** its own microservice. Module names carry no product prefix — the groupId (`com.mysms`) namespaces the artifacts.

```
mysms/                # repository root (directory name only — not an artifact name)
├── pom.xml           # reactor + parent: dependencyManagement, plugin management, quality gates
├── core/             # plain library jar plugged into every module: BaseEntity, error model, paging,
│                     #   tenant context, ids (UUIDv7), validation, SQL fragment + batch helpers, the
│                     #   transaction base class (AppTransactionManager), the gRPC edge and client failure, cross-cutting auto-configurations. NO db schema, NO Flyway,
│                     #   NO datasource of its own, NO domain code.
├── auth-client/      # hand-written auth facade + servlet/gRPC filters (see §16). Owned by the iam team;
│                     #   plugged into every module; the ONLY auth code a module ever touches.
├── messaging/        # event API, outbox/inbox runtime and adapters: RabbitMQ (default), Google Cloud Pub/Sub,
│                     #   in-process; runtime in all domain modules, compile API for event consumers only.
├── master/           # microservice ← schema "master"
├── platform/         # microservice ← schema "platform"
├── iam/              # microservice ← schema "iam" (also the OIDC issuer, §16)
├── org/              # microservice ← schema "org"
├── people/           # microservice ← schema "people"
├── academics/        # microservice ← schema "academics"
├── sis/              # microservice ← schema "sis"
├── hr/               # microservice ← schema "hr"
├── payroll/          # microservice ← schema "payroll"
├── admission/        # microservice ← schema "admission"
├── attendance/       # microservice ← schema "attendance"
├── facility/         # microservice ← schema "facility"
├── timetable/        # microservice ← schema "timetable"
├── assessment/       # microservice ← schema "assessment"
├── lms/              # microservice ← schema "lms"
├── finance/          # microservice ← schema "finance"
├── library/          # microservice ← schema "library"
├── transport/        # microservice ← schema "transport"
├── comms/            # microservice ← schema "comms"
├── frontoffice/      # microservice ← schema "frontoffice"
├── careers/          # microservice ← schema "careers"
└── app/              # assembly ONLY: one main class running every module in one process, merged
                      #   config. No business code, no schema, no migrations of its own, ever.
```

Rules:

- **One module = one schema = one deployable.** Every domain module carries its own `@SpringBootApplication` main (component-scanning its own package + core/auth-client auto-configs) and its own `application.properties` (§2.1) — nothing else describes it: no descriptor class, no registration file. The spring-boot plugin repackages with `classifier=exec`, so each flat module produces **both** a plain jar (consumed by `app`) and an executable microservice jar: `mvn -pl finance -am package` builds the finance microservice; `mvn -pl app -am package` builds the monolith.
- **Domain modules never depend on each other.** Every module uses `core`, `auth-client`, and the `messaging` runtime. Only modules that declare event consumers get `messaging` at compile scope; conformance permits them to import only `com.mysms.messaging.events` API types and rejects RabbitMQ, Google Pub/Sub, and adapter/runtime imports. Publishers use the core outbox writer and need no event API imports. Cross-module imports are therefore impossible at compile time from the dependency graph alone — no Modulith, no ArchUnit, no enforcement library.
- **Cross-module calls are gRPC only** (§10): the caller's `<module>/integration/<Callee>GrpcClient` with `@GrpcClient` stubs compiled from its verbatim copy of the callee's proto (ids + card messages), in-process in `app` and over the network when split — one code path whether assembled or split. No module depends on another and there is no shared client module. Against-DAG needs use events (outbox), never a direct call. The allowed-dependency table in `ARCHITECTURE.md` governs which modules a module may call.
- **One DataSource per module.** `core` provides the datasource factory; each module binds `mysms.<module>.datasource.*` and owns its pool and its transaction manager. All 21 default to the same PostgreSQL instance; moving a schema to its own instance is a configuration change, zero code. No connection is ever shared across modules, so a cross-schema join cannot exist.
- **Own-schema-only SQL.** Every statement in a module references only that module's schema. Enforced by a ~20-line plain JUnit check from `core` (regex over the module's SQL constants — no library) plus review check R3.
- **Flyway per module, per datasource.** Each module migrates only its own schema from `src/main/resources/db/migration/<module>/` with its own `flyway_schema_history` (`default-schema` = the module's schema). There are no cross-module migrations and no cross-module foreign keys, so a module's folder applies unchanged whether its schema shares an instance with others or has a dedicated one.
- **Logical references, not foreign keys, across modules.** A column pointing into another module's data is validated **before the write** by resolving the id through the owner's gRPC service (tenant-scoped; not found / other tenant → 422). Referenced entities are archived, never hard-deleted; the owner publishes `*.deleted` / `*.archived` events and every dependent handles them as `database/logical-references.md` prescribes; each module runs a scheduled reconciliation job that batch-verifies its reference ids against the owners and reports orphans (never auto-fixes). Against-DAG references (listed there) are maintained from events only — the module never calls the owner for them.
- **Module-local infrastructure only.** Idempotency keys, long-running operations, document numbers, outbox and inbox all live in the module's own schema (`idempotency_key`, `operation`, `number_sequence`, `outbox_event`, `inbox_event`). Audit records are written as events into the module's own outbox in the business transaction; the platform module materialises them into `platform.audit_log`. A module never writes, reads or locks another schema's rows.
- **gRPC allowlist.** `core`'s test kit (`ModuleChecks`) has a plain JUnit check, run by every module's `ModuleBoundaryTest`, that fails the build when the module imports the gRPC contract (`com.mysms.<other>.v1`) of a module outside its row of the allowed-dependency table in `ARCHITECTURE.md` — the table is read from the file itself, so the document and the build cannot disagree.
- **Trace context everywhere.** `core` propagates W3C `traceparent` on every inbound request, outbound gRPC call and published/consumed event, and puts trace id, tenant id and request id in the logging MDC.
- **No shared domain code.** `core` holds base classes and cross-cutting infrastructure only — never a domain entity or domain DTO. An entity belongs to exactly one module; cross-module data travels as ids + Card DTOs kept fresh by outbox events.
- **Per-module config namespace** (`mysms.finance.*`), so a module's properties move with it unchanged when it is split out. How a module is configured: §2.1.
- **Versioning:** the project version ends in `-SNAPSHOT` during development. A released (suffix-free) version is never built on again: the first commit after a release bumps to the next `-SNAPSHOT`. CI tags artifacts; the POM carries no build qualifier.
- Java toolchain, dependency versions and plugin versions are pinned in the parent only; child POMs never override a managed version silently.
- Lombok and MapStruct run as annotation processors; APT is enabled in CI and IDE setup docs. `lombok` then `mapstruct-processor` ordering in `annotationProcessorPaths` (MapStruct must see Lombok-generated getters).
- CI builds every module's exec jar and runs every module's own Testcontainers suite (own schema, own Flyway) on every commit, so "can this run alone on its own database?" is proven continuously.

### 2.1 Configuration: one `application.properties` per module, combined in the monolith

Every domain module ships `src/main/resources/application.properties` — the whole
configuration of that module's microservice, like the reference service's — and the monolith combines them.

- **What a module's file holds:** its name (`spring.application.name=<module>`, `mysms.auth.modules[0]=<module>`),
  server and management settings, authentication (`mysms.auth.*`), the gRPC server, one gRPC client per service its
  `integration` package calls (§10.4), the event bus, its database (`mysms.<module>.datasource.*`) and its own
  settings (`mysms.<module>.*`). Every value is a default over an environment variable of DEPLOYMENT.md §4, written
  `key=${MYSMS_<VARIABLE>:<default>}` (the module's own variable first, then the shared one:
  `${MYSMS_SIS_DB_URL:${MYSMS_DB_URL:jdbc:…}}`). A setting with no safe default has none (`mysms.auth.issuer=${MYSMS_AUTH_ISSUER}`):
  the service refuses to start without it. Secrets are never written in the file, only their variable.
- **Code reads the key, never a default of its own:** `@Value("${mysms.iam.smtp.port}")`, or a
  `@ConfigurationProperties` class. The file is the one place a module's setting and its default are written. (A
  library — core, auth-client, messaging — keeps code defaults for its own settings, since it ships no file.)
- **Microservice:** the module's exec jar has one `application.properties` on its classpath; Spring Boot loads it.
  An operator's `application.properties` beside the jar (or in `config/`), environment variables, system
  properties and the command line override any value, in Spring Boot's usual order.
- **Monolith:** `app` has its own `application.properties`, first on its classpath, holding only what differs when
  every module runs in one process — `spring.application.name=mysms`, every module in `mysms.auth.modules[i]`, the
  in-process gRPC server and an in-process client per module, the shared broker user. Core's
  `ModulePropertiesEnvironmentPostProcessor` adds every module's file after it (a file counts as a module's only when
  its `spring.application.name` names a module whose operation table lies beside it), so app's file and anything
  the operator sets win over every module file.
- **No conflicts left to chance:** a key that two module files set to different values must be set in app's file,
  so the order of the module files never decides a value. The libraries ship no `application.properties`.
  Conformance G9 checks all of this, and that every `@GrpcClient` key has its client configured.
- **Tests** set what differs with `@DynamicPropertySource` (core's `TestDatabase.register`: the test database,
  no gRPC port, absent services, the in-process event bus); they never ship a second `application.properties`.

```properties
# sis/src/main/resources/application.properties (shortened)
spring.application.name=sis
mysms.auth.modules[0]=sis
mysms.auth.issuer=${MYSMS_AUTH_ISSUER}
grpc.server.port=${MYSMS_GRPC_SERVER_PORT:9112}
grpc.client.academics-service.address=${MYSMS_ACADEMICS_GRPC_ADDRESS:dns:///academics-service.${MYSMS_NAMESPACE:mysms}.svc.cluster.local:${MYSMS_ACADEMICS_SERVICE_PORT_RPC:9112}}
grpc.client.academics-service.negotiationType=PLAINTEXT
mysms.sis.datasource.url=${MYSMS_SIS_DB_URL:${MYSMS_DB_URL:jdbc:postgresql://localhost:5432/mysms}}

# app/src/main/resources/application.properties (shortened)
spring.application.name=mysms
mysms.auth.modules[0]=academics
…
mysms.auth.modules[20]=transport
grpc.server.in-process-name=mysms
grpc.client.academics-service.address=${MYSMS_ACADEMICS_GRPC_ADDRESS:in-process:mysms}
```

**Toolchain (pinned in the parent POM; upgraded deliberately, never by a child):**

| Component | Version | Notes |
|---|---|---|
| Java | 17 (LTS) | `maven.compiler.release`; no language feature or API newer than 17 (`--release 17` enforces it) |
| Spring Boot | 4.1.2 | parent POM; manages Spring Framework 7.0, Spring Security 7.1, Jackson 3, Flyway 12, PostgreSQL JDBC 42.7, HikariCP 7, gRPC 1.84, protobuf 4.36, Testcontainers 2.0, JUnit 6, Lombok 1.18 |
| OpenAPI Generator | 7.26.0 | the clients' TypeScript SDK (CLI, in `clients/`), from each module's `target/openapi/api-docs.yml` |
| protobuf-maven-plugin (xolstice) + os-maven-plugin | 0.6.1 / 1.7.1 | every module: protoc + protoc-gen-grpc-java compile the module's own protos and its copies of its callees' (§10.2) |
| grpc-spring-boot-starter (net.devh) | 3.1.0.RELEASE | the gRPC server, `@GrpcService`, `@GrpcClient`, global interceptors (§10) |
| springdoc-openapi | 3.2.0 | every service: `/v3/api-docs.yaml` written from the contract interfaces (core `OpenApiConfiguration`); each module's OpenAPI component test writes it to `target/openapi/api-docs.yml` (§10.5) |
| MapStruct | 1.6.3 | with lombok-mapstruct-binding 0.2.0 |
| Checkstyle / SpotBugs / JaCoCo | 12.3.1 (newest that runs on Java 17) / 4.10.4 / 0.8.15 | quality gates on `mvn verify` (§13) |
| PostgreSQL | 16 | Testcontainers image `postgres:16-alpine` |

---

### 2.2 Modules of areas (ARC-7 amendment, owner decision DEC-19)

> Added with the kernel-and-products reorganisation ([MODULE-AND-PRODUCT-REORGANISATION.md](docs/design/MODULE-AND-PRODUCT-REORGANISATION.md) §3, §11). Where §2, §3, §10.5 and §14 say *module* for a schema, its contract or its packages, read **area**; every rule of those sections still holds, per area.

- **An area** is one database schema with its contract: its protos (`mysms.<area>.v1`), its operation table (`META-INF/mysms/operations/<area>.json`), its URL prefix (`/v1/<area>`), its configuration namespace (`mysms.<area>.*`), its `ModuleDatabase`, transaction manager, outbox, Flyway history (`database/migrations/<area>`) and service identity. Areas keep the 22 names of the former modules; contracts, operation ids, permission codes and schemas did not change. A 23rd area, `time_attendance` (hrms, staff attendance; ARC-7 step 2c), was added afterwards: there are 23 areas.
- **A domain module** (still a flat Maven module, still its own microservice) serves one or more areas. Its pom packages each area's migration folder (`${mysms.migrations.dir}/<area>`): that list *is* the module's set of areas, read by conformance and by `ModuleChecks.moduleSchemas`. The ten modules: `platform` (platform, master, org), `identity` (iam, people), `privacy`, `comms` (kernel); `hrms` (hr, payroll, time_attendance); `students` (sis, admission, careers), `academics` (academics, timetable, attendance, assessment), `finance`, `campus` (library, transport, frontoffice, facility); `learning` (lms).
- **Packages.** An area named like its module uses the module's root package `com.mysms.<module>` with the §3 layer packages; any other area uses `com.mysms.<module>.<area>` with the same layer packages. Nothing else lives under `com.mysms.<module>` but the one `<Module>Application` (`@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)`, so two areas may each have, say, a `PersonMapper`). A module serving several areas sets `mysms.spotbugs.onlyAnalyze` to its root package and every layer package of every area (conformance §3).
- **Inside a module** an area may read and join its sibling areas' schemas and declare foreign keys into them (`ModuleChecks.foreignSchemaReferences(moduleSchemas, …)`, conformance §14 allow the module's own schemas); across modules everything in §10 and §14 holds unchanged. An area may still call a sibling area over gRPC (the in-process server); replacing such calls by direct Java calls between the areas' business services is allowed and preferred. Since ARC-7 step 3 (2026-11-27) every sibling call in the codebase is direct: the caller checks the callee operation's permission through auth-client's `SiblingAccess` (the check the gRPC edge made) and keeps the callee's `request_id` rules through core's `InProcessIdempotency`; the callee's business service runs its own transaction on its own area's transaction manager, outside any transaction of the caller, and the caller maps the callee's refusals to the same answers its gRPC hop gave.
- **Area names with an underscore** (`time_attendance`). The area code keeps the underscore wherever an identifier allows it: schema, migration folder, proto package (`mysms.time_attendance.v1`), operation table file, permission codes (`time_attendance.staff_attendance.read`), event types, `mysms.auth.modules[i]`, broker queues. The URL prefix and the configuration namespace hyphenate it (`/v1/time-attendance`, `mysms.time-attendance.*`), because a canonical Spring property name cannot hold an underscore; environment variables drop it (`MYSMS_TIMEATTENDANCE_…`, `MYSMS_IAM_SERVICECLIENTS_TIMEATTENDANCE_SECRETSHA256`), because Spring reads `_` in a variable name as `.`. Java packages drop it (`com.mysms.hrms.timeattendance`); bean names are lower camel case (`timeAttendanceDatabase`). Guards that validate area or module codes accept `[a-z][a-z_]*`.
- **Configuration.** One `application.properties` per module: `spring.application.name=<module>`, `mysms.auth.modules[i]` = its areas, then each area's keys (`mysms.<area>.*`). Callers address an area's gRPC service at its module's host (`dns:///<module>-service…`, `MYSMS_<MODULE>_SERVICE_PORT_RPC`); the per-area variable `MYSMS_<AREA>_GRPC_ADDRESS` still overrides it.
- **gRPC callers.** A module may call what any of its areas may (the union of their ARCHITECTURE §2 rows), from any of its areas' `integration` packages (G1, G4).
- **OpenAPI.** core publishes one springdoc group per area (`OpenApiGroupsEnvironmentPostProcessor`: `/v3/api-docs/<area>`, paths `/v1/<area>/**`); each area's `<Area>OpenApiComponentTest` boots its module and writes `backend/<module>/target/openapi/<area>/api-docs.yml`, the input of that area's SDK namespace (G8).
- **Tests.** One `ModuleBoundaryTest` per module (over all its areas); component tests boot the module (`<Module>Application`) and register every area's database (`TestDatabase.register(registry, "<area>", …)`); component-test resources sit in a folder per area (`<area>/crud-spec.json`).
- **Products.** Kernel modules (platform, identity, privacy, comms) serve every product and know none; a product module depends only on the kernel, except school → hrms by id and events (design §2). Learning never depends on school modules.

---

## 3. Package layout inside a domain module

> Amended by §2.2: the layout below is per area — `com.mysms.<module>` for the area named like its module, `com.mysms.<module>.<area>` for the others.

Single service-root package per module: `com.mysms.<module>`. Map by **layer role**:

```
com.mysms.<module>
├── api/           The contract: one interface per resource carrying the mappings, DTOs in api/model (§7)
├── controller/    REST controllers (thin; implement the module's own api package interfaces)
├── grpc/          gRPC resolvers/controllers (delegate to the same business services)
├── business/      Business services, one per aggregate (orchestration), long-running operation runners, cache adapters
├── repository/    Plain-JDBC repositories, SQL constants, RowMappers, batch helpers
├── domain/        Entities and value objects (never leave business/repository layers)
├── model/         (rare) internal shapes that are not in the contract — transport DTOs live in api/model
├── mapper/        MapStruct mappers (entity↔DTO, entity↔proto) and mapping configurations
├── validation/    Validators, custom Jakarta constraints, sort-field allowlists
├── errors/        Typed exceptions for this module
├── events/        Outbox event types this module publishes, inbox consumers for events it handles (incl. logical-reference cleanup), reconciliation job
├── config/        Spring configuration local to the module
├── constants/     Message keys, sort constants, resource names
└── Other modules can never import this module — they are not on its Maven graph; they call its gRPC services (§10)
```

A class whose role doesn't fit a package above is a design smell — raise it rather than inventing a new top-level package per feature.

---

## 4. Layering and the canonical write path

The layer contract (adapted from the source base template, re-based onto plain JDBC):

| Layer | Responsibility | Forbidden |
|---|---|---|
| Controller / gRPC resolver | HTTP/gRPC decode, validation trigger, paging decode, delegate, 404/412 mapping | Business rules, repository access, SQL, mapping logic |
| Business service | Orchestration: map DTO→entity, lifecycle hooks, programmatic transaction, batch write, audit+outbox, cache | HTTP types (`ResponseEntity`, status codes), SQL text |
| Repository | SQL + parameters + RowMappers + batch | Business decisions, mapping to DTOs, HTTP anything |
| Mapper (MapStruct) | Pure shape conversion | I/O, repository calls, business decisions |

**Standard operations.** The standard methods (`List`, `Get`, `Create`, `Update`, `Delete`) of every resource whose contract maps column for column onto its table follow one shape: one concrete entity, repository and mapper per resource, **one service per aggregate** and one controller per contract interface (a record type that exists only as part of another — an approval of a homework, a line of a marksheet — has no service of its own; its methods live in the service of the aggregate it belongs to, the one its contract interface and controller already belong to, after the aggregate's own methods under a `// ---- <Record>: a record of the <Aggregate> aggregate ----` line, with its constants prefixed `<RECORD>_`, its private helpers suffixed `<Record>` and its repository and mapper injected as `<record>Repository` / `<record>Mapper`; a record type with a lifecycle and a contract interface of its own keeps its own service, and unrelated aggregates are never merged), each a plain class in exactly the shape this document prescribes (no base class, no type parameters, no reflection). They are owned source, written and reviewed like any other code; no generator writes them, and repetition between them is removed by extracting shared helpers. Their rules come from the schema: uniqueness pre-checks mirror each unique constraint and each unique index on live rows (409; indexes with other conditions or expressions are left to the index itself, which also answers 409), in-module references are checked before writing (422), a delete is refused while live rows of the module still reference the row (409), NOT NULL, numeric precision/scale and the string-encoded 64-bit integers and times are enforced before the database sees the value (422, never a rounded amount or a 500), omitted values take the column default, and every read and write applies the institution data scope (§16). When a resource has invariants of its own, a `<Resource>Rules` component next to its service carries them and the service calls it at fixed points — `beforeWrite` (create and update, after the scope check and before uniqueness), `beforeUpdate`/`beforeDelete` (on the row as it stands) and `afterDelete` (same transaction): iam's access management uses this for the no-escalation rules (`AccessRules`), fixed system roles and signing a deleted account out everywhere. **State transitions** (documented in `api/state-machines.md`) follow the same shape: the service loads the row under the operation's own permission scope (404 if invisible), requires it writable (403), compares the body `etag` when the request has one (412) and `If-Match` when the operation declares it (428/412), checks the guard on the row as read (409 `INVALID_STATE_TRANSITION`), then runs one constant `UPDATE … SET <listed columns>, updated_at, updated_by, row_version + 1 WHERE id = :id AND row_version = :expected` (0 rows → 412, so a concurrent change can never slip past the guard) and writes `AuditWriter.recordTransition` (step, changed fields, reason) and the `<resource>.<step>` event in the same transaction. A step with further effects is written as its own workflow operation. **Secrets:** a contract field marked `writeOnly` whose table has `<field>_enc` is a secret: the service encrypts it with `FieldCipher` for the row's tenant and column before it is stored (create and update; an omitted value keeps the stored one), the mapper never returns it, a `<field>Masked` output comes from a `*_masked` column written at the same time or is computed from the decrypted value on read, secrets are never filterable or sortable, and the merged-model validation skips them (their plaintext was validated with the request). A service with secret columns refuses to start without a master key. Each resource's contract test runs from its entry in the module's `crud-spec.json` (§12).

**Events between modules.** A change writes its event to the module's own outbox in the same transaction (above); the `messaging` library's `OutboxRelay` publishes it to the bus and marks it published once the bus confirms. The bus is MySMS's own interface, `com.mysms.messaging.events.EventBus` (CloudEvents 1.0 envelopes, `EventEnvelope`); one `messaging` library contains the API, runtime and RabbitMQ (default), Google Cloud Pub/Sub and in-process adapters. Event-consuming modules compile against the narrow `com.mysms.messaging.events` API only; the POM and conformance check reject broker/provider and adapter imports from domain code. Choose an adapter with `mysms.events.bus`. RabbitMQ uses a broker user per service and checks the verified AMQP user on every event. Pub/Sub uses one topic per source module; IAM limits each producer to its own topic, and consumers validate the CloudEvent source and type against the topic and message attributes. `in-process` serves a single JVM. A module consumes events by declaring an `EventSubscriptions` bean (event type → handler); the `messaging` library's `InboxProcessor` runs each handler once per event inside the event's tenant transaction (the module's `inbox_event`), and a failing handler is redelivered after a growing delay and finally dead-lettered. A module publishes only its own event types (`<module>.…`) and its audit records — core's `OutboxWriter` refuses anything else, and consumers reject an event its source does not own (`EventEnvelope.ownedBySource`); a handler throws `RejectedEventException` for an event that must never be retried. Several `EventSubscriptions` beans of one module are merged; two handlers for one type stop the application. Handlers change only their own module's tables, audit like any write, and announce their changes on their own outbox. Examples: each module's reference cleanup of database/logical-references.md (`<Module>ReferenceCleanup`) and the platform's audit trail.

**No templatized service layer.** There is no generic `BusinessServiceApi<T, P, ID>`, no `BaseDatabaseBusinessService` skeleton, no listener-pipeline framework and no `MapperContext` indirection — that part of the source template was **deliberately rejected**. Each aggregate gets a plain, hand-written service class (`CourseService`) with explicitly named methods (`getCourse`, `listCourses`, `createCourse`, `updateCourse`, `patchCourse`, `deleteCourse`, plus its workflow operations). Methods take and return that aggregate's own DTOs — no type parameters, no inherited write pipeline, no reflective hooks. Shared behaviour (paging decode, tenant context, batch helpers, audit-row builder) is composed in as small injected helpers from `core`; the only thing a service inherits is its transaction handling (`AppTransactionManager`, below).

**Transactions are programmatic — never annotation-driven — and every service inherits them from `AppTransactionManager`**. `@Transactional` and `TransactionTemplate` are both banned in MySMS code (Checkstyle regexp rule and conformance B1 enforce it). Every module declares its **`PlatformTransactionManager` as a qualified Spring bean** (`<Module>DatabaseConfig.TRANSACTIONS`); every class that works in a transaction extends core's `com.mysms.core.db.AppTransactionManager`, passes the module's `ModuleDatabase` and that bean to `super(database, transactions)`, and hands each unit of work to `doInReadTransaction` / `doInWriteTransaction` as a `CallBack` (`runIn…` for work that answers nothing; `…Global…` for global tables and reads before a tenant is known, which bind no tenant). The base class begins the module's transaction (`ModuleDatabase.readDefinition()` / `writeDefinition()`: READ COMMITTED, the module's timeout, read-only for reads), binds the caller's tenant (the transaction-local `app.tenant_id` row-level security reads), commits the work's value only when the work completes normally, and rolls back on any failure (a `RuntimeException`, an `Error`, or a checked exception thrown sneakily), rethrowing it unchanged; a failing rollback is attached to it as suppressed and is never followed by a commit. Work that calls another service of the same module joins the running transaction (one commit, at the end of the outermost work); the base class refuses a write started inside a read-only transaction and tenant work started inside a global one (`IllegalStateException`: both are programming errors). The `…Global…` methods only leave the tenant unbound — a write to a global table still runs inside `CallContextHolder.withoutTenant()` where its policy requires that. No service writes `getTransaction` / `commit` / `rollback` itself; only infrastructure with a transaction definition of its own (the idempotency store, the outbox relay and inbox) does. This is the one thing a service inherits: there is still no generic CRUD base service.

```java
public Subject createSubject(Subject request, boolean validateOnly) {
    validator.validate(request);                                   // the full DTO, explicitly
    SubjectEntity entity = mapper.toEntity(request);               // ignores id, tenant and the audit set (§8)
    entity.onCreatingData(Actor.current(), clock);                 // UUIDv7 id, both stamps, rowVersion = 1 (§5.8)
    entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
    DataScope scope = DataScope.current(PERMISSION_CREATE);
    return doInWriteTransaction(() -> {                            // begin; row-level security sees this tenant only
        assertValid(entity, true);                                 // business invariants → one 422
        scope.requireWritable(institutionOf(entity));              // the caller's data scope → 403
        assertUnique(entity);                                      // → 409 DUPLICATE
        if (validateOnly) {
            return mapper.toDto(entity);                           // committed on the way out: nothing was written
        }
        SubjectEntity stored = repository.insert(entity);
        audit.recordCreate(AGGREGATE, stored.getId());             // audit event into this module's outbox, same tx
        outbox.publish(AGGREGATE + AcademicsEvents.CREATED, AGGREGATE, stored.getId(),
                AcademicsEvents.payload(stored.getId()));          // domain event row, same tx
        return mapper.toDto(stored);
    });                                                            // committed; rolled back on any failure
}
```

The write sequence itself is still law, just written out visibly in each method:

1. Validate the DTO, map DTO → entity.
2. Entity lifecycle hook: `onCreatingData()` / `onUpdatingData()` / `onDeletingData()` stamps audit columns, UUIDv7 id, `row_version`.
3. Inside **one programmatic transaction** on the module's own DataSource: the aggregate write (batched with its child rows), the audit event row(s) and, when other modules care, the domain event row — both in the module's own `outbox_event`. Optimistic-lock guard `AND row_version = :expected`; 0 rows → `PreconditionFailedException` → HTTP 412.
4. After commit (callback on `TransactionSynchronization` afterCommit, or plain code after `tx.write` returns): cache refresh, notification fan-out triggers. After-commit code never writes business rows.
5. Map entity → DTO for the response.

**Audit + outbox are not optional.** Every create/update/patch/soft-delete writes its audit event (materialised into `platform.audit_log` by the platform module) and, when other modules care, a domain event — both as rows of the module's own `outbox_event`, **in the same transaction** as the aggregate row — via the explicit `auditWriter`/`outboxWriter` calls, no annotation-driven audit of any kind. If a service mutates a row and cannot point at the audit entry it produced, the change is non-compliant. Reviewers check for the calls; there is no framework to do it silently.

**Patch flow rule:** apply the partial to the loaded entity, then **re-validate the full DTO** produced from the merged entity before persisting. A patch can never produce a state that `create` would have rejected. The partial is the PATCH body's `Optional` fields, applied one explicit line each through core's `Patches` (§7.2, §8.3).

**Identity established by the service itself.** Sign-in, token refresh and the startup bootstrap act before the edge has bound a tenant: they resolve it (tenant code, refresh-token prefix, bootstrap setting), then run their transactions inside `CallContextHolder.callAs(context.withIdentity(tenant, user), …)`, so `database.bindTenant()` binds the right tenant and audit/outbox rows carry it. A transaction that binds no tenant exists only for a module's global tables (catalogues, `iam.tenant_directory`); row-level security still fails closed there.

**Reads:** `findById` and every list query exclude soft-deleted rows in the SQL itself. Optional read-through cache sits in front of `findById` only, via a `CacheAdapter` — never inside repositories.

---

## 5. Entities (`domain/`)

`domain/` holds entities and nothing else: one class per table, extending `BaseEntity`. A read-only projection is a record in `repository/` (§6); a value object stored in a column lives next to its entity only when the entity uses it.

**5.1 The audit set is exact.** `BaseEntity` (in `core`) carries the schema's audit columns column for column, with these names and types and no others:

| Field | Column | Type |
|---|---|---|
| `id` | `id uuid` | `UUID`, UUIDv7 generated by the application (`UuidV7.next(clock)`) |
| `createdAt` / `updatedAt` | `timestamptz` | `OffsetDateTime` (UTC), never `LocalDateTime` |
| `createdBy` / `updatedBy` | `uuid` | `UUID` of the acting user account; null for system work |
| `deletedAt` | `timestamptz NULL` | `OffsetDateTime`; null = live. Never an `is_deleted` flag, never a numeric (epoch) `deleted_at` |
| `rowVersion` | `integer` | `int`; 1 on insert, +1 on every update and soft delete (the ETag) |

Extra audit-like columns (`createdByOrg`, `deletedBy`…) do not exist; who did what lives in the audit trail.

```java
// ✗ not like this
@Column("created_on") private LocalDateTime createdOn;
@Column("created_by_org") private UUID createdByOrg;
@Column("is_deleted") private Boolean isDeleted;
@Column("deleted_at") private long deletedAt;              // epoch seconds; no version column at all
protected void setEntityId() { id = UUID.randomUUID(); }  // v4

// ✓ BaseEntity (core) — the whole audit set
@EqualsAndHashCode.Include private UUID id;
private OffsetDateTime createdAt;
private UUID createdBy;
private OffsetDateTime updatedAt;
private UUID updatedBy;
private OffsetDateTime deletedAt;
private int rowVersion;
```

**5.2 Lombok, exactly.** Entities use `@Getter @Setter @SuperBuilder @NoArgsConstructor` and `@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)`, so equality is the `id` alone (`@EqualsAndHashCode.Include` on `BaseEntity.id`). Never `@Data` (it adds all-field `equals`/`hashCode` and `toString`, which would print personal data), never `@AllArgsConstructor`, never `@ToString` on an entity.

```java
// ✗
@Data @AllArgsConstructor @EqualsAndHashCode(callSuper = true)
public class Organization extends TaggedEntity { … }

// ✓
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SubjectEntity extends BaseEntity {
    public static final String TABLE = "academics.subject";
    private UUID tenantId;
    private UUID institutionId;
    private String code;
    private String name;
}
```

**5.3 An entity is the row, nothing more.** No Jackson annotations (`@JsonSerialize`, `@JsonDeserialize`, `@JsonIgnore`, `@JsonProperty`…): Jackson never sees an entity (§7). No mapping-framework annotations (`@Table`, `@Column`, `@Id`, `@Transient`). No view or computed data that is not a column (`depth`, child lists, counts): a computed value is a field of the response DTO or of a projection record, filled by the service.

```java
// ✗
@JsonSerialize(using = LocalDateTimeWithMillisSerializer.class) private LocalDateTime createdOn;
@Transient private Integer depth;                                   // view data on the row
@Transient @JsonIgnore private List<OrganizationQualification> qualifications;

// ✓ the depth is computed by the service and set on the response DTO (or a projection record)
OrganizationNode node = new OrganizationNode(entity.getId(), depthOf(entity.getId(), parents));
```

**5.4 Read-only projections are records in `repository/`** — report rows, join results, sign-in shapes — never entities, never in `domain/`, never DTOs. Card DTOs are contract schemas (`api/model`; on the caller's side the card messages of the callee's proto, `com.mysms.<callee>.v1`).

```java
// ✓ repository/RoleGrantRow.java (iam)
public record RoleGrantRow(String roleCode, String scopeType, Optional<UUID> scopeId, boolean primary) { }
```

**5.5 JSONB is rare by design** (3 columns in the whole schema). Where it exists, the field is a typed value object serialized with Jackson inside the repository — never a raw `String`, never `Map<String, Object>` or `Map<String, Serializable>`. Repeating structured data (addresses, contact points, identifiers) is a child table, not a jsonb list.

```java
// ✗
private Map<String, Serializable> content;          // BaseJsonEntity
private AddressList address;                        // a jsonb list where a child table belongs

// ✓ (illustrative) a typed value object, serialized by the repository
private Layout layout;                              // record Layout(String template, List<LayoutField> fields)
```

**5.6 Enum-like columns** are Java enums persisted by `name()` lowercased to match the DB CHECK list; the enum and the CHECK constraint are kept in sync in the same PR that changes either. Generated standard-operation entities keep the contract enum's wire value as a `String` instead: the generated DTO enum validates the value on the way in, the CHECK constraint guards the column, and a second hand-kept enum per column would only drift.

**5.7 The table name exists once.** Each entity declares `public static final String TABLE = "<schema>.<table>"`. Outside its repository, code names the table only through that constant (the service's `AGGREGATE`, audit, event types). The only other place the name appears is the SQL constants of the entity's own repository, schema-qualified (§6).

```java
// ✗ the table spelled in services and helpers, unqualified in SQL
new GdprTableStatusDTO("organization", 0);
"select * from organization o where o.id = :id"

// ✓
public static final String AGGREGATE = SubjectEntity.TABLE;     // service
audit.recordCreate(AGGREGATE, entity.getId());
outbox.publish(AGGREGATE + AcademicsEvents.CREATED, AGGREGATE, entity.getId(), AcademicsEvents.payload(entity.getId()));
```

**5.8 Lifecycle hooks are called by the service, with the actor and the clock.** `onCreatingData(actor, clock)` / `onUpdatingData(actor, clock)` / `onDeletingData(actor, clock)` live on `BaseEntity` and are invoked **only** by the owning business service, immediately before the write (§4). Never from a framework callback (`BeforeSaveCallback`, `BeforeConvertCallback`, `@PrePersist`, an entity listener), never from a repository; an entity never reads a security context or the system clock itself.

```java
// ✗ framework callback; the entity reads the security context and the wall clock
public class BeforeSaveRepositoryCallback implements BeforeSaveCallback<BaseEntity> { … baseEntity.onUpdatingData(); }
public void onUpdatingData() { updatedOn = DateUtils.nowInUTC(); updatedBy = SecurityContextHolder.getContext().getUserId(); }

// ✓ business service
entity.onCreatingData(Actor.current(), clock);            // UUIDv7 id, both stamps, rowVersion = 1
// … then repository.insert(entity) inside the service's transaction (§4)
```

- Entities never cross the API boundary: not in controller signatures, not serialized, not returned from controller or gRPC surfaces. The compiler should make leaking an entity awkward; the reviewer makes it a blocker.

---

## 6. Repositories (plain Spring JDBC)

- **A repository is a plain class**, annotated `@Repository`, constructor-injected with the module's `ModuleDatabase`, one per aggregate (plus per child table where written independently), built on `JdbcClient` / `NamedParameterJdbcTemplate`. Never a repository *interface*: no `CrudRepository` / `ListCrudRepository` / `PagingAndSortingRepository`, no derived query methods (`findAllByIsDeleted`, `countByActiveAndIsDeleted`), no `@Query`, no `*RepositoryImpl` fragments, no `@NoRepositoryBean` base interface, no inheritance deeper than one abstract helper.

```java
// ✗
public interface OrganizationRepository extends RepositoryAPI<Organization, UUID>, OrganizationChildRepository {
    @Query("select * from organization o where upper(o.name) = upper(:name) and o.is_deleted = false limit 1")
    Optional<Organization> findByName(String name);
}

// ✓
@Repository
public class SubjectRepository {

    private static final String FIND_BY_ID = """
            SELECT r.id, r.tenant_id, r.institution_id, r.code, r.name, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM academics.subject r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
               AND r.id = :id
            """;

    private final JdbcClient jdbc;

    public SubjectRepository(@Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<SubjectEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(SubjectRepository::map).optional();
    }
}
```

- **SQL is a named constant or a `.sql` resource next to the repository** — Java text blocks, schema-qualified (`lms.course`), named parameters only. Never concatenate values into SQL, never `String.format` names into it, never build column lists from strings or entity metadata at runtime (dynamic filters go through `core`'s reviewed `FilterParser` / `SqlFragment` with a parameter map). When constant SQL fragments are joined, remember that a text block drops trailing blanks: a fragment appended after `WHERE`/`AND` must begin with a space (or the line must end with the keyword's own line break) — a component test against PostgreSQL catches the slip, a unit test does not.
- **No database functions or procedures** in queries (`select … from get_children(:id)`): hierarchy and recursion are written as a `WITH RECURSIVE` query in the repository's own SQL.
- Every statement over a tenant table: `tenant_id = :tenantId` (and, for reads, `deleted_at IS NULL`). The value is never passed by hand: `ModuleDatabase.jdbc()` is core's `TenantJdbcClient`, which binds `:tenantId` from the call's tenant, so a statement run without a bound tenant fails instead of reading every tenant. Every repository states it itself, on every table of the FROM list and each JOIN … ON; the conformance module (R1) fails the build on a statement that omits it; every write stamps audit columns from the entity; every update/soft-delete carries `AND row_version = :expected` and increments `row_version`.
- Soft delete = `UPDATE … SET deleted_at = :now, updated_* …, row_version = row_version + 1` — never `DELETE` on audited tables. Hard deletes exist only where the schema documents them (purge jobs).
- **Row mapping:** explicit `RowMapper` per entity (or `DataClassRowMapper` for records when names align). Mapping code lives with the repository, not in services.
- **Batch:** multi-row writes use JDBC batch (`batchUpdate` / `JdbcClient` batch) through `core`'s batch helper so aggregate + child + audit/outbox rows share one round trip and one transaction.
- **No N+1 — structurally.** Every child-loading method takes a list of parent ids (`WHERE parent_id = ANY(:ids)`). A loop that queries per row fails review regardless of measured cost.
- **Paging and sorting:** sort fields are validated against a per-aggregate allowlist (`SORT_FIELDS`) and mapped to columns through an explicit map in the repository — never interpolated from client input, never resolved from entity metadata. Pagination follows the API contract (`page`/`size`/`cursor`/`sort`, api/README "Pagination") through core's `PageRequest`: every list query appends `page.keysetPredicate()` to its WHERE, orders by `page.orderByClause()`, ends with `page.limitClause()` and binds `page.limitParams()`, then hands the rows to `page.toPage(...)`. A `next` link's cursor makes the read a keyset seek (no OFFSET); a page number without a cursor is read by OFFSET, which `PageRequest` caps at 10,000 skipped rows. One look-ahead row tells whether a next page exists; **lists are never counted** (no `COUNT(*)` per list request).
- Partitioned-table queries always carry the partition-key range filter: the list service calls `FilterParser.requireRange(filter, FILTER_FIELDS, "<field>")` before it parses the filter, which answers 400 `INVALID_FILTER` unless the filter bounds the field on both sides at the top level (at most 366 days). Partitions are created and retired by core's `PartitionMaintenanceJob` as the schema owner, never by the application role.
- Transactions are owned by the business layer, **programmatically** (the module's `PlatformTransactionManager` bean, driven through the `AppTransactionManager` every service extends — §4; `@Transactional` and `TransactionTemplate` are banned); repositories never begin/commit, and `AppTransactionManager` binds the caller's tenant (transaction-local `app.tenant_id`, through `ModuleDatabase.bindTenant()`) before the first statement of the work. Each module's repositories use only that module's own DataSource and schema (§2).
- **Reads go to the replica, writes to the primary — by the transaction, never by the repository.** A module's DataSource is core's `RoutingDataSource` (taken from `common`): `doInReadTransaction` opens its connection on the module's replica pool (read-only connections, `MYSMS_<MODULE>_DB_REPLICA_URL` / `MYSMS_DB_REPLICA_URL`), `doInWriteTransaction` on the primary; without a replica both are the primary. A read nested in a write joins the write's primary connection, and a write inside a read fails (read-only). So a read that must see a write just made (read-your-writes) runs in the write transaction, and a read that decides on security — sign-in, lockout, second factor, tenant status — runs in `doInPrimaryReadTransaction` / `doInGlobalPrimaryReadTransaction` (read-only, on the primary); a plain read may lag the primary by the replication delay, which the ETag check of every update turns into a 412, never a lost update. A replica that cannot give a connection within two seconds is skipped: the read runs on the primary, and health reports the replica as a detail without taking the module out of readiness. Migrations run on the primary only.

---

## 7. The contract layer — the module's own `api` package, strictly separate from entities

**7.1 The contract is owned source inside the module (CLAUDE.md).** Each module's API interfaces and transport DTOs live in `com.mysms.<module>.api` and `com.mysms.<module>.api.model` — one interface per resource carrying the Spring mappings, one mutable bean per schema carrying `@JsonInclude(NON_NULL)` and the Jakarta constraints. There is no separate contracts module and no OpenAPI→Java generation: the packages are edited directly, in the shape the OpenAPI generator produces (the style gates skip them for that reason). The OpenAPI documents are written FROM this code: springdoc serves `/v3/api-docs.yaml` in every service (core's `OpenApiConfiguration` restores the contract operationIds, `x-permission`, security schemes and idempotency from the module's edge operation table), and each module's `<Module>OpenApiComponentTest` writes it to `target/openapi/api-docs.yml` on every `mvn verify` (§10.5).

Three owned artifacts describe the same operations, and the build fails when they drift: the `api` package (the code), `src/main/resources/META-INF/mysms/operations/<module>.json` (the edge table REST and gRPC are authorised by; the OpenAPI component test checks every documented operation against it, and conformance G3 checks it names exactly the module's RPCs), and the module's proto (§10.2). A contract change edits the interface/DTO, the operation table and the proto (with its field-number lock, and the callers' copies) in one reviewed change. There is no hand-kept OpenAPI snapshot: the document is the build output `target/openapi/api-docs.yml`.

```java
// ✗ an ad-hoc transport class next to the contract package
@Data public class OrganizationDTO extends AuditableDTO { @NotBlank @Size(max = 255) private String name; … }

// ✓ the controller and service use the module's contract package
import com.mysms.academics.api.model.Subject;        // resource
import com.mysms.academics.api.model.SubjectUpdate;  // its writable fields only
import com.mysms.core.paging.PagingInfo;              // every list: data + paging (NavigationInfo)
```

- **Required or optional is stated by the field:** a property the contract requires is a plain field; an optional one is `@Nullable` (collections included, even when initialised empty). Core's `OpenApiConfiguration` writes the document's `required` lists from that, and never writes a `null` type (MySMS never sends null), so the generated clients get the contract's types. The one exception is a PATCH body (§7.2): none of its fields is required, and a clearable one (type argument neither `@NotNull` nor `@NotBlank`) keeps `"null"` in its type, because JSON null is how a client clears it.
- **`@Valid` goes on the type argument, never on the container:** `List<@Valid InvoiceLine> lines`, `Optional<@Valid Address> address`, never `@Valid List<InvoiceLine> lines` (Hibernate Validator deprecates the container form, HV000271). A container of plain values (`List<String> sort`) carries no `@Valid` at all — there is nothing to cascade into. Nor does an untyped `Object` body (HV000272: it may be a container at runtime). Conformance D2 checks the contracts.
- Contract DTOs are mutable beans with `@JsonInclude(NON_NULL)` (the contract omits absent values) and Jakarta constraints (`@NotNull`, `@Size`, `@Min`/`@Max`, `@Pattern`). Because the contract interfaces mark bodies `@Valid`, shape errors are rejected before a controller runs (422 `VALIDATION_FAILED`, parameter errors 400).
- **Validation is still explicit in the write path:** the business service calls `DtoValidator.validate(dto)` on create, and again on the merged model after a partial update (§4 patch rule); business invariants (codes, cross-row rules, logical references) are named private methods that collect every failing field into one 422.
- **Output-only fields** (`id`, `createTime`, `updateTime`, `createdBy`, `updatedBy`, `etag`) appear in the resource DTO but are never read from a request: mappers ignore them on the way in (§8).

**7.1a One JSON mapper, built by hand.** Every Jackson use in the process — HTTP bodies, page cursors, events, the OpenAPI table — goes through the one `JsonMapper` core declares (`JsonMappers.create()`, taken from `common`'s `CommonConfig`): ISO-8601 dates in UTC, unknown properties ignored (additive contract changes), a primitive set to null refused (an absent one takes its default since Jackson 3.2, so contract DTOs carry no primitives), a single value never accepted for a list, properties in declaration order, enums by name in any letter case and never by number, UUIDs only in canonical form, boxed booleans only as true/false, members without a view in every view, and the Jackson modules on the classpath registered. The bean is named `jacksonJsonMapper`, Boot's own name, so Boot's auto-configured mapper backs off for it; no module builds its own `JsonMapper`/`ObjectMapper` or customizes the shared one.

**7.2 Partial updates: `PATCH` + the `<Resource>Update` DTO with `Optional` fields (owner decision; PATCH APIs only).** Every PATCH operation — every one that exists and every one built later — takes the reference service's shape: the `<Resource>Update` DTO carries only the writable fields, each wrapped in `Optional<…>`, with the Bean Validation constraints on the **type argument**, and no field is ever initialised. The JSON body alone decides what changes; there is no `updateMask` query parameter. Three states, one per field:

| JSON body | DTO field | Effect on the column |
|---|---|---|
| property absent | `null` | untouched |
| `"field": null` | `Optional.empty()` | cleared — a 422 when the type argument is `@NotNull`/`@NotBlank`: a required column can change, never be cleared |
| `"field": value` | `Optional.of(value)` | set (after the type argument's constraints pass) |

A write-only secret (§4) is replaced, never wiped: its type argument is `@NotNull` (`Optional<@NotNull String> keySecret`), so JSON null is a 422 and an absent one keeps the stored envelope — unless the contract documents that null removes it (an optional integration key such as lms `Settings.unsplashAccessKey`). Shape checks a service runs on the request before applying it read a field through `Patches.value(changes.getX())`. The rule is for PATCH bodies only: every other contract DTO (resources, create bodies, workflow requests, responses) keeps plain nullable properties, and an `Optional` field anywhere else is a D2 violation. Unknown properties are ignored like on every body (§7.1a). The service applies each field with one explicit line through core's `Patches` (§8.3) — never reflection, never MapStruct — then **re-validates the merged DTO** and runs its business checks exactly as before (§4 patch flow rule). Over gRPC the update takes the partial resource directly, as the reference's `rpc patch(OrganizationPartial) returns (Organization)` does (ARC-9, owner decision 2026-11-26): the RPC keeps its name `Update<Resource>`, its request is the `<Resource>Update` message — the row's `id`, `etag` (required, If-Match), `validate_only` and `request_id` where the operation has them, the parent ids REST carries in the path, then one field per writable field of the REST body — and there is no `FieldMask`. The same three states, field by field: **unset** is absent (unchanged); a field that may be cleared is a `mysms.common.v1.Nullable*` wrapper (core's `patch.proto`: `NullableString` — also UUIDs, dates, times and decimals, which are strings on the wire —, `NullableBool`, `NullableInt32`, `NullableInt64`, `NullableTimestamp`, `NullableDuration`; `oneof kind { google.protobuf.NullValue null = 1; <type> value = 2; }`) whose **null** clears it and whose **value** sets it; a field the contract never lets be cleared (type argument `@NotNull`/`@NotBlank`) is a plain proto3 `optional` (so false and 0 are sent), an enum is `optional <Enum>` whose `_UNSPECIFIED` value is its null (422 where the field is required, as REST's null), free-form JSON is a `google.protobuf.Value` (its `null_value` clears), and a list travels in a `StringList` wrapper, so an unset list is absent and never replaced. Each resource's `<Resource>ProtoBridge` (in the module's `grpc` package, beside the resolver) reads the message into the **same** REST `<Resource>Update` DTO with one explicit line per field through `ProtoBridge`'s readers (`text`, `uuid`, `date`, `flag`, `number`, `int64`, `time`, `duration`, `choice`, `json`, `texts`, `message`), validates it with `bridge.patch(changes)` like REST's `@Valid`, and the resolver hands it to the same business service — hand-written like the rest of our mappers, never MapStruct (§8.3). Every area's update RPCs take `<Resource>Update` messages (ARC-9's two batches, 2026-11-26): no proto carries an `update_mask` and `ProtoBridge` has no mask reader; the field numbers the old `Update<Resource>Request` messages used are gone with those messages, and a `<Resource>Update` that reuses an older message name reserves them (`reserved 1 to N;`). On REST every module is on the `Optional` shape: core's `UpdateMask`, `ProtoBridge.updateMask`, D2's transitional allow-list and the clients' masked-update branch are gone, and conformance D2 checks every PATCH contract.

**An empty PATCH changes nothing (owner decision 2026-11-26).** A body or message that sets no field (`{}`, an update message with only its keys) answers the row as it is — after the service's checks (If-Match, scope, validation of the stored row), without a write: no new version (the ETag stays), no audit record, no event. The service returns the loaded row as soon as `apply(...)` answers no field names, before the write stamp (`onUpdatingData`); the CRUD contract test expects the version unchanged for `{}`, and the gRPC parity contract for an empty `<Resource>Update`.

```java
// ✗ plain nullable properties + an updateMask query parameter decide what a patch changes
private @Nullable String name;
List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
// ✗ an initialised field (absent and "set to the default" become one state) / constraints on the container
private Optional<String> note = Optional.empty();
@Size(max = 255) private Optional<String> name;

// ✓ api/model/FeeHeadUpdate.java — the PATCH body: constraints on the type argument, nothing initialised
public class FeeHeadUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> name;        // may change, never be cleared

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;                           // JSON null clears it

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> defaultAmount;

  @JsonProperty("name")
  public @Nullable Optional<String> getName() { return name; }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) { this.name = name; }
  // … one getter/setter pair per field, as generated
}

// ✓ business/FeeHeadService.java — one explicit line per field; answers the applied names for the audit record
private static List<String> apply(FeeHeadEntity entity, FeeHeadUpdate changes) {
    Patches patch = new Patches();
    patch.apply("name", changes.getName(), entity::setName);
    patch.apply("description", changes.getDescription(), entity::setDescription);
    patch.apply("defaultAmount", changes.getDefaultAmount(), v -> entity.setDefaultAmount(WireValues.decimal(v)));
    return patch.applied();
}

// ✓ the update method: apply, re-validate the merged DTO, business checks — unchanged
List<String> fields = apply(entity, changes);
validator.validate(mapper.toDto(entity));
assertValid(entity, false);
if (fields.isEmpty()) {
    return mapper.toDto(entity);              // an empty PATCH: the row as it is, no write, version, audit or event
}
entity.onUpdatingData(Actor.current(), clock);
…
audit.recordUpdate(AGGREGATE, id, fields);
```

```proto
// ✓ finance.proto (shortened) — the update RPC takes the partial resource; new message, new numbers (lock)
service FeeHeadService {
  rpc UpdateFeeHead(FeeHeadUpdate) returns (UpdateFeeHeadResponse);
}
message FeeHeadUpdate {
  string id = 1;
  string etag = 2;                                   // required: If-Match
  bool validate_only = 3;
  optional string name = 6;                          // may change, never be cleared
  mysms.common.v1.NullableString description = 7;    // null clears, a value sets
  mysms.common.v1.NullableString default_amount = 8; // a decimal: a string on the wire
  optional bool is_active = 11;                      // false is sent
}

// ✗ the reference's always-replaced list, a mask, or a plain string for a field that may be cleared
repeated string alias = 6;
google.protobuf.FieldMask update_mask = 2;
string description = 5;
```

```java
// ✓ grpc/FeeHeadProtoBridge.java (shortened) — one explicit line per field into the REST body, validated like @Valid
static FeeHeadUpdate changes(ProtoBridge bridge, com.mysms.finance.v1.FeeHeadUpdate request) {
    FeeHeadUpdate changes = new FeeHeadUpdate();
    changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
    changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
    changes.setDefaultAmount(ProtoBridge.text(request.hasDefaultAmount(), request.getDefaultAmount()));
    changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
    return bridge.patch(changes);
}

// ✓ grpc/FeeHeadResolver.java — the same business service as REST's controller
FeeHead updated = service.updateFeeHead(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()),
        FeeHeadProtoBridge.changes(bridge, request), request.getValidateOnly());
```

Two flaws of the reference service are **not** copied: patching through MapStruct (`@MappingTarget` with a null-value strategy — §8.3 still bans it), and gRPC list fields that are always replaced (here an unset `StringList` is absent, so unchanged). Each resource's `crud-spec.json` entry names a clearable field (`patch.clears`) and a required one (`patch.keeps`), and the CRUD contract test checks all three states on them over REST, and the gRPC parity contract over the `<Resource>Update` message (an empty message keeps the version, a wrapper holding null clears `patch.clears`, an UNSPECIFIED required enum is refused like REST's null).

**7.3 One update method: PATCH.** A resource is never replaced as a whole with `PUT` next to its `PATCH`. `PUT` exists only where the contract defines an idempotent *set the value at this key* operation (today `iam.SetMySetting`; conformance D2 checks the contracts).

**7.4 Contract shapes only — no envelopes of our own.** Every list operation answers core's `PagingInfo<Resource>` (`data` + `paging`, the `NavigationInfo` with `page`, `size` and the `first` / `previous` / `next` links), built by `PagingUtil.pagingInfo(page)` from the service's `Page<Resource>` (the classes are core's, the paging under them is core's keyset/offset `PageRequest`); a write that returns the items it wrote answers `{items}`; a delete answers **204 with no body**; an error answers problem+json (§11). No other paging envelope, never a `{"success": true}` body.

```java
// ✗
return new ResponseEntity<>(new ResponseMessage(Boolean.TRUE), HttpStatus.OK);                 // delete
SubjectList list = new SubjectList(items).nextPageToken(token).totalSize(count);               // per-resource envelope

// ✓
return ResponseEntity.noContent().build();
PagingInfo<Subject> list = PagingUtil.pagingInfo(subjectService.listSubjects(page, size, cursor, filter, sort));
list.data().forEach(item -> ReadMask.apply(item, fields));
return ResponseEntity.ok(list);
```

Services return `Page<Dto>` (`repository.list(page, where).map(mapper::toDto)`); only the controller knows the request URL the links are built from.

**7.5 Partial responses go through the read mask.** `fields` (read mask) is applied by `core`'s `ReadMask` on the response DTO; unknown names → 400. No `@JsonView`, no `ResponseBodyAdvice` that reshapes or slims responses, no per-endpoint DTO variants for the same resource.

- **No entity ↔ JSON anywhere.** Jackson never sees a `domain/` class; a module's `model/` package holds only rare internal shapes that are not part of the contract.

---

## 8. MapStruct rules

MapStruct is the mapping mechanism between entities and generated DTOs (and protos). Hand-rolled mapping needs a stated reason in review.

**8.1 One `@MapperConfig` per module, and every mapper uses it.** No mapper sets `componentModel` or policies itself, and there is no generic base mapper interface (`BaseEntityMapper<E, T, P>`): the DTOs are generated, so a common type bound cannot exist.

```java
// ✗ a generic base config/interface; mappers set componentModel inline; unmapped fields only warn
@MapperConfig(uses = CommonMapper.class)
public interface BaseEntityMapper<E extends BaseEntity, T extends BaseDTO, P extends BasePartialDTO> { … }
@Mapper(componentModel = "spring", config = BaseEntityMapper.class) public abstract class OrganizationMapper …

// ✓ mapper/AcademicsMapperConfig.java — the module's only configuration
@MapperConfig(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @Builder(disableBuilder = true))
public interface AcademicsMapperConfig {
}

// ✓ mapper/SubjectMapper.java
@Mapper(config = AcademicsMapperConfig.class, imports = {Etags.class, WireValues.class})
public interface SubjectMapper { … }
```

- `unmappedTargetPolicy = ERROR`: every new column or contract field forces a conscious mapping decision at compile time. `ignore = true` is a decision, silence is not.

**8.2 The way in ignores the id, the tenant and the whole audit set** — `id`, `tenantId`, `createdAt`, `createdBy`, `updatedAt`, `updatedBy`, `deletedAt`, `rowVersion` — on every method that builds an entity from a DTO, plus every read-only field. A request can never choose an id, resurrect, re-parent or re-tenant a row, or set an audit stamp; the service's lifecycle hook sets those (§5.8). The ETag is always derived from `row_version`.

```java
@Mapper(config = MasterMapperConfig.class, imports = Etags.class)
public interface CountryMapper {
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    Country toDto(CountryEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "active", source = "isActive", defaultValue = "true")
    CountryEntity toEntity(Country dto);
}
```

**8.3 A partial update is applied by explicit code in the service, never by MapStruct.** No `@MappingTarget` update/patch methods, no `NullValuePropertyMappingStrategy` / `NullValueCheckStrategy`: the service's `apply(entity, changes)` has one `Patches.apply(name, changes.getX(), setter)` line per writable field of the PATCH body (§7.2), and answers the applied names for the audit record. Stated reason: the three states of a PATCH field (absent: keep, null: clear, value: set) cannot be expressed by a null-value strategy, which cannot tell absent from null, and an explicit line per field is reviewable and reflection-free.

```java
// ✗
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void patch(OrganizationPartialDTO partial, @MappingTarget Organization target);

// ✓ business service
private static List<String> apply(SubjectEntity entity, SubjectUpdate changes) {
    Patches patch = new Patches();
    patch.apply("code", changes.getCode(), entity::setCode);
    patch.apply("name", changes.getName(), entity::setName);
    return patch.applied();
}
```

- **Mappers are interfaces without state.** `expression = "java(...)"` and `@AfterMapping` **only delegate to helpers** (`Etags.of`, `WireValues.int64`) — no conditionals or business logic in expressions, no repository access, no static lookups, no `@Autowired` fields in abstract mapper classes.
- Transport variants: proto mappers (`CountryProtoMapper`) convert generated DTO ↔ proto at the gRPC edge; the gRPC resolver calls the same business service as REST.
- Generated mappers are never edited; mapper behaviour is covered by the module's component tests (round-trip through the real API) and, for non-trivial expressions, unit tests.

---

## 9. REST controllers

**9.1 Controllers implement the module's contract interfaces** (`CountryController implements CountryApi` from `com.mysms.<module>.api`; method names are the operationIds without the module prefix: `createCountry`). The mappings live ONLY on the contract interfaces — no `@RequestMapping`/`@GetMapping`/`@PostMapping`/… and no springdoc annotations on a controller class itself (conformance C3); a mapping outside the contract is a blocker — and unreachable: the auth filter refuses any route that is not in the module's operation table. No base controller class (`BaseController<E, T, P, ID>`). The one controller outside the contract is iam's `JwksController`, which serves the OIDC key set at `/oauth2/jwks` — an infrastructure path the edge lets through (§16, step 1), not an API operation.

```java
// ✗ mappings on the controller class, a generic base controller
public class OrganizationController extends BaseController<Organization, OrganizationDTO, OrganizationPartialDTO, UUID> {
    @PostMapping(…) ResponseEntity<OrganizationDTO> create(…) { … }
}

// ✓
@RestController
public class SubjectController implements SubjectApi {    // com.mysms.academics.api (the owned contract)

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @Override
    public ResponseEntity<Subject> createSubject(Subject subject, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Subject created = subjectService.createSubject(subject, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/academics/subjects/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }
}
```

- **Thin by contract:** decode (the generated interface binds and `@Valid`-checks the body) → delegate to the aggregate's business service, which runs `DtoValidator` explicitly (§4, §7) → encode. No repository imports, no SQL, no conditionals beyond routing between service methods, and **no side effects**: a controller never publishes events, writes audit, calls another service or touches a cache — those belong to the service's transaction (§4).
- Status mapping follows `api/README.md`: 201+Location+ETag on create (200 with the would-be resource when `validateOnly`), 200+ETag on get/update/patch, 204 on delete, 404 via `ResourceNotFoundException` for absent-or-invisible rows, 409 for duplicates/state conflicts, **412 for stale `If-Match`**, 422 for semantic validation, problem+json everywhere (`GlobalExceptionHandler` is the single place that builds error bodies).
- `If-Match` is required on update/delete/patch (the contract says so): controller passes the ETag into the service; the service turns it into the `row_version` guard.
- `Idempotency-Key` handling is a shared filter from `core` (`IdempotencyFilter`) backed by the module's own `idempotency_key` table (`IdempotencyStore`), not per-controller code. The edge marks each operation that declares the header as `optional` or `required` (from the contract); required-key operations (money/bulk) answer 400 `INVALID_ARGUMENT` without a key. The kept response is encrypted with the field cipher; a 5xx is not kept.
- List endpoints: decode `pageable` (`page`/`size`/`sort`/`cursor`) and `filter` via the same core paging classes (`PageRequest`, `SortSpec`) and answer `pagination` from the same `Page`; sort values validated against the aggregate allowlist (§6); the partitioned-list time-range requirement is enforced here with a clear 400.
- Long-running work returns **202** with an `Operation` recorded in the module's own `operation` table (`module` field set); the module serves it through its `OperationService` (`GetOperation` / `ListOperations` / `CancelOperation`) and publishes operation events that feed the platform job centre.
- Every list/get honours the read mask `fields` when present (§7.5).
- A controller implements **every** method of its generated interface. An operation that is not built yet throws `NotImplementedException` (501 `NOT_IMPLEMENTED`) explicitly — no default methods, no silent 404; an interface without any controller is answered 501 by the edge marker (§16). Building the operation replaces the stub with the real method; the compiler keeps the set complete.

---

## 10. gRPC — servers, inter-service calls and the generated documents

**Modules call each other over gRPC exactly the way the owner's reference service
(`plt-organization-service`) does, and every module's OpenAPI document is produced by its OpenAPI component test.
There is no `sdk` module, no generated Java HTTP client and no `Transactions` helper.** This section is the whole
mechanism; do not deviate from it. The conformance build enforces it (G1–G9, §13).

### 10.1 The mechanism at a glance

| Concern | How | Where |
|---|---|---|
| gRPC runtime | `net.devh:grpc-spring-boot-starter` 3.1.0.RELEASE, Netty (not netty-shaded) | parent POM, `core` |
| Protos | each module owns `src/main/resources/proto/mysms/<module>/v1/*.proto`; a caller keeps a **byte-identical copy** of each callee proto it uses; the shared messages (`mysms/common/v1`) are core's alone | G1 |
| Proto compile | `os-maven-plugin` + xolstice `protobuf-maven-plugin` in **every** module: messages + grpc-java stubs into the module jar, descriptor set to `target/docs/protos/grpc.protobin` | parent POM |
| Server | one gRPC server per process (`grpc.server.port`, default 9112), health service on, reflection off unless opened | core |
| Server implementation | `@GrpcService` resolver per proto service, delegating to the same business service as REST | `<module>/grpc`, G5 |
| Errors out | `@GrpcGlobalServerInterceptor` `GlobalGrpcExceptionHandler` at `ORDER_GLOBAL_EXCEPTION_HANDLING`: status + trailers `error-message-bin` (`Any` of `ErrorMessage`) and `error-details` | core |
| Client | `@GrpcClient("<callee>-service")` setter injection of blocking stubs into a `<Callee>GrpcClient` `@Service` | `<module>/integration`, G4 |
| Client config | `grpc.client.<callee>-service.address` + `negotiationType=PLAINTEXT` | core defaults, overridable |
| Errors in | `IntegrationGrpcClientException.call(service, stubCall)` unpacks the callee's trailers (code, HTTP status, fields) | core |
| Caller identity | `GrpcCallerInterceptor` (`@GrpcGlobalClientInterceptor`) forwards the caller's own token, tenant, institution, `traceparent` and a deadline | auth-client |
| OpenAPI | `<Module>OpenApiComponentTest` fetches `/v3/api-docs.yaml` from the running service and writes `target/openapi/api-docs.yml` | every module, G8 |
| No REST between modules | no `RestClient`, `RestTemplate`, `WebClient` or HTTP interface client between modules | G6 |

### 10.2 Protos and the build

A module's protos are source of that module; the module that serves a service is the only one that edits it. A caller
copies the callee's file verbatim into its own proto tree (same path, same bytes) and recompiles; the conformance
build fails when a copy differs from the owner's file, or when the callee is not in the caller's row of
ARCHITECTURE.md §2.

```
sis/src/main/resources/proto/mysms/
├── sis/v1/sis.proto              # owned: sis's services and messages
├── people/v1/people.proto        # verbatim copy (sis calls people)
├── academics/v1/academics.proto  # verbatim copy
├── org/v1/org.proto              # verbatim copy
└── iam/v1/iam.proto              # verbatim copy
core/src/main/resources/proto/mysms/common/v1/   # shared messages (paging, ErrorMessage …): core's only
```

```proto
syntax = "proto3";
package mysms.sis.v1;

import "mysms/common/v1/common.proto";
import "mysms/common/v1/patch.proto";   // the Nullable* wrappers of <Resource>Update messages (§7.2)

option java_multiple_files = true;
option java_outer_classname = "SisProto";
option java_package = "com.mysms.sis.v1";   // com.mysms.<module>.v1, always

service StudentCategoryService {
  rpc GetStudentCategory(GetStudentCategoryRequest) returns (GetStudentCategoryResponse);
  rpc UpdateStudentCategory(StudentCategoryUpdate) returns (UpdateStudentCategoryResponse);  // PATCH: §7.2
}
```

The parent POM compiles them in every module (the reference's plugin pair; nothing is downloaded at build time
beyond the Maven artifacts):

```xml
<plugin>
  <groupId>org.xolstice.maven.plugins</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <configuration>
    <protocArtifact>com.google.protobuf:protoc:${protobuf-java.version}:exe:${os.detected.classifier}</protocArtifact>
    <pluginId>grpc-java</pluginId>
    <pluginArtifact>io.grpc:protoc-gen-grpc-java:${grpc-java.version}:exe:${os.detected.classifier}</pluginArtifact>
    <protoSourceRoot>${basedir}/src/main/resources/proto</protoSourceRoot>
  </configuration>
  <executions>
    <execution>
      <id>protobuf-compile</id>
      <goals><goal>compile</goal><goal>compile-custom</goal></goals>
      <configuration>
        <writeDescriptorSet>true</writeDescriptorSet>
        <includeDependenciesInDescriptorSet>true</includeDependenciesInDescriptorSet>
        <includeSourceInfoInDescriptorSet>true</includeSourceInfoInDescriptorSet>
        <descriptorSetFileName>grpc.protobin</descriptorSetFileName>
        <descriptorSetOutputDirectory>${project.build.directory}/docs/protos</descriptorSetOutputDirectory>
      </configuration>
    </execution>
  </executions>
</plugin>
```

- **Field numbers are law.** `backend/conformance/src/main/resources/field-numbers.lock.json` records the number of
  every field of every owned message (G2). A new field takes a new number and is added to the lock in the same
  change; an existing number is never reused or changed.
- **The operation table is the module's RPCs** (G3): every operation in `META-INF/mysms/operations/<module>.json`
  names its RPC, and every RPC is in the table, so REST and gRPC are authorised by the same row.
- Generated classes (`com.mysms.<x>.v1`) are never edited and are excluded from Checkstyle, SpotBugs and coverage
  (§13).

### 10.3 Serving: `@GrpcService` resolvers

One resolver per proto service in `<module>/grpc`, annotated `@GrpcService`, extending the generated `…ImplBase` and
delegating every RPC to the **same** business service REST's controller uses — business rules exist once. The
resolver is thin: proto → DTO through core's `ProtoBridge` (validated with the same Jakarta validator as REST's
`@Valid`), the service call, DTO → proto.

```java
@GrpcService
public class ActivityTypeResolver extends ActivityTypeServiceGrpc.ActivityTypeServiceImplBase {

    private final ActivityTypeService service;
    private final ProtoBridge bridge;

    public ActivityTypeResolver(ActivityTypeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void getActivityType(GetActivityTypeRequest request, StreamObserver<GetActivityTypeResponse> out) {
        ActivityType found = service.getActivityType(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetActivityTypeResponse.newBuilder(), "activity_type", found).build());
    }
}
```

Nothing else goes in a resolver: no `try/catch` (errors are the edge's), no permission check, no transaction, no
repository. Authentication, the operation's permission, idempotency (`request_id`) and error mapping run at the gRPC
edge as global server interceptors, in this order (`GrpcEdge`):

| Order | Interceptor | Does |
|---|---|---|
| `TRACE_ORDER` | `GrpcTraceInterceptor` | continues `traceparent`, binds the trace in the MDC |
| `ERROR_ORDER` = `InterceptorOrder.ORDER_GLOBAL_EXCEPTION_HANDLING` | `GlobalGrpcExceptionHandler` | typed exception → status + trailers |
| `AUTH_ORDER` | auth-client's edge | token, tenant, institution, the operation's permission (`EdgeAuthorizer`, shared with REST) |
| `IDEMPOTENCY_ORDER` | `GrpcIdempotencyInterceptor` | replays a repeated `request_id` from the module's ledger |

The exception handler maps the same typed exceptions REST maps (§11) — `NOT_FOUND`, `PERMISSION_DENIED`,
`ALREADY_EXISTS`/`FAILED_PRECONDITION` for 409, `ABORTED` for 412, `INVALID_ARGUMENT` for 400/422,
`UNAVAILABLE` for 503 — and writes the refusal to the trailers the reference service uses:

```java
@GrpcGlobalServerInterceptor
@Order(GrpcEdge.ERROR_ORDER)   // InterceptorOrder.ORDER_GLOBAL_EXCEPTION_HANDLING
public final class GlobalGrpcExceptionHandler implements ServerInterceptor { … }

// trailers of every refusal
Metadata.Key<byte[]> ERROR_MESSAGE = Metadata.Key.of("error-message-bin", Metadata.BINARY_BYTE_MARSHALLER);
//   Any.pack(ErrorMessage{code, status, message, errors[field, message, code]})
Metadata.Key<String> ERROR_DETAILS = Metadata.Key.of("error-details", Metadata.ASCII_STRING_MARSHALLER);
```

Each module's `<Module>GrpcParityComponentTest` (core's `GrpcParity` kit) proves the RPCs answer what REST answers —
the same rows, the same refusals — over a real server.

### 10.4 Calling another module: `@GrpcClient`

A module calls another only if the callee is in its row of ARCHITECTURE.md §2 (core's `ModuleChecks` reads the table;
the build fails on any other `com.mysms.<x>.v1` import). The call lives in one class per callee,
`<module>/integration/<Callee>GrpcClient`, the only place another module's stubs may appear (G4). The starter injects
the blocking stubs through `@GrpcClient` setters, as in the reference service:

```java
package com.mysms.sis.integration;

@Service
public class AcademicsGrpcClient {

    /** The starter's client of the academics service: grpc.client.academics-service.* */
    public static final String ACADEMICS_SERVICE_KEY = "academics-service";
    private static final String SERVICE = "academics";

    private SectionServiceGrpc.SectionServiceBlockingStub sections;

    @GrpcClient(ACADEMICS_SERVICE_KEY)
    public void initSectionStub(SectionServiceGrpc.SectionServiceBlockingStub stub) {
        this.sections = stub;
    }

    public GetSectionResponse getSection(GetSectionRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> sections.getSection(request));
    }
}
```

**Configuration** — the reference's keys, in the module's own `application.properties` (§2.1). The monolith's file
turns every client to the in-process server; an operator's file, environment variables or command line override any
of them:

```properties
# server (one per process)
grpc.server.port=${MYSMS_GRPC_SERVER_PORT:9112}
grpc.server.health-service-enabled=true
grpc.server.reflection-service-enabled=${MYSMS_GRPC_REFLECTION:false}
grpc.server.shutdown-grace-period=20s
grpc.server.max-inbound-message-size=10MB

# one client per service the module calls; split deployment (Kubernetes DNS)
grpc.client.academics-service.address=${MYSMS_ACADEMICS_GRPC_ADDRESS:dns:///academics-service.${MYSMS_NAMESPACE:mysms}.svc.cluster.local:${MYSMS_ACADEMICS_SERVICE_PORT_RPC:9112}}
grpc.client.academics-service.negotiationType=PLAINTEXT

# app's file (the monolith): the same clients call its in-process server
grpc.server.in-process-name=mysms
grpc.client.academics-service.address=${MYSMS_ACADEMICS_GRPC_ADDRESS:in-process:mysms}

# every call waits at most this long when the stub sets no deadline
mysms.grpc.client.deadline=${MYSMS_GRPC_CLIENT_DEADLINE:10s}
```

Tests that need no network port set `grpc.server.port=-1` (the reference's test setting); parity tests that record
the peer address run on a real port.

**Identity.** Every call carries the caller's own identity: auth-client's `GrpcCallerInterceptor`
(`@GrpcGlobalClientInterceptor`) adds `authorization: Bearer <caller's token>`, `x-tenant-id`, `x-institution-id`,
`traceparent` and the deadline. Work with no user behind it (an event consumer, a scheduled job) calls with a service
token: `stub.withCallCredentials(serviceTokens.callCredentials("<module>"))` (auth-client's `ServiceTokenProvider`:
iam's client-credentials grant, `sub=service:<module>`, audience `mysms-internal`, in the tenant of the current call
context) — and the interceptor keeps those credentials.

**Never inside a transaction.** Read transaction → remote call → write transaction that re-checks status, scope and
version. The interceptor refuses a call made while a transaction is active (`IllegalStateException`), so the first
test of the path finds it. sis's `PortalAccountService`, shortened:

```java
Student linkStudentAccount(DataScope scope, UUID id) {
    PortalAccountStore.Request opening = doInPrimaryReadTransaction(() -> {          // 1. read and check
        StudentEntity student = eligibleStudent(scope, id);
        if (student.getUserId() != null) {
            throw new InvalidStateException("The student already has a portal account.");
        }
        return new PortalAccountStore.Request(student.getPersonId(), …);
    });
    UUID account = accounts.open(opening);                                           // 2. iam over gRPC, no transaction
    return doInWriteTransaction(() -> {                                              // 3. check again, then write
        StudentEntity student = eligibleStudent(scope, id);   // may have left or moved out of scope meanwhile
        if (students.linkAccount(id, student.getRowVersion(), account, now(), Actor.current().orElse(null)) == 0) {
            throw new PreconditionFailedException("The student changed meanwhile; try again.");
        }
        …
    });
}
```

**Failures.** `IntegrationGrpcClientException.call` turns every `StatusRuntimeException` into an
`IntegrationGrpcClientException` carrying the callee's service, gRPC code, problem code, HTTP status and refused fields
(from `error-message-bin`; without one, the status code mapped by api/README's table). The caller maps it to its own
typed exception — never forwards the callee's sentence or fields (sis's `Downstream`, shortened):

```java
static <T> Optional<T> required(String service, String permission, String purpose, Supplier<T> request) {
    try {
        return Optional.of(request.get());
    } catch (IntegrationGrpcClientException e) {
        if (e.httpStatus() == 404) { return Optional.empty(); }                  // not there for this caller
        if (e.httpStatus() == 403) { throw new PermissionDeniedException(purpose + " also needs " + permission + "."); }
        throw new UnavailableException("The " + service + " service could not answer.");   // 503
    }
}
```

An exception nobody maps answers 503 `UNAVAILABLE` with no detail of the callee.

**Unit tests** of a client or its caller stub the generated blocking stub with Mockito (`doThrow` a
`StatusRuntimeException` carrying the trailers for refusals); component tests run the real callee in-process.

### 10.5 The OpenAPI document of every module

> Amended by §2.2: one document per area, `/v3/api-docs.yaml/<area>` written to `backend/<module>/target/openapi/<area>/api-docs.yml` by `<Area>OpenApiComponentTest`.

Every module has `src/component-test/java/com/mysms/<module>/<Module>OpenApiComponentTest.java`, the reference
service's mechanism: boot the module on a random port, fetch `/v3/api-docs.yaml` with
`Accept: application/vnd.oai.openapi`, check it, and write it to **`backend/<module>/target/openapi/api-docs.yml`**.
That file is the module's published contract — the input of the separate client-generation project (web, mobile)
and of the clients' TypeScript SDK. It is never hand-written, never copied into the repository, and never produced by a
script or a plugin outside the test (G8).

```java
@SpringBootTest(classes = SisApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SisOpenApiComponentTest {

    private static final String OPEN_API_OUTPUT_LOCATION = "target/openapi/api-docs.yml";
    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "sis");
        registry.add("mysms.auth.api-docs", () -> true);
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @Test
    void writesItsOpenApiDocumentFromTheCode() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs.yaml"))
                .header("Accept", "application/vnd.oai.openapi")
                .header("Authorization", "Bearer " + TOKENS.tenantUser(UUID.randomUUID(), UUID.randomUUID()))
                .GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        String document = OpenApiDocuments.checked("sis", response.body());

        Path output = Path.of(OPEN_API_OUTPUT_LOCATION);
        Files.createDirectories(output.getParent());
        Files.writeString(output, document, StandardCharsets.UTF_8);
        assertThat(output).isNotEmptyFile();
    }
}
```

### 10.6 What is banned

- A generated or hand-written HTTP client between modules, an `sdk`-style module of shared clients, or a module
  dependency on another domain module (G6, A1).
- Another module's stub outside `<module>/integration` (G4); a proto copy that differs from the owner's file, or an edit
  to another module's proto (G1).
- A gRPC server class that is not a `@GrpcService`, or a resolver with business rules, transactions or `try/catch`
  (G5, §4).
- A `Transactions` (or similar) helper: transactions are `AppTransactionManager`'s alone (G7, §4).
- An OpenAPI document from anywhere but the module's OpenAPI component test (G8); a list of gRPC services in core that
  differs from the domain modules (G9).

---

## 11. Validation and errors

- All request validation is declarative on DTOs + custom Jakarta constraints in `validation/`; `core`'s `DtoValidator` is the single trigger. Business invariants beyond field shape live in the business service as named private methods (`assertEnrollmentOpen(...)`), called explicitly before the write (§4).
- Validation-to-code mapping is uniform (`@NotNull`→required-field code, `@Size`→length code, enum→allowed-values code) and implemented once in the exception handler.

**11.1 One error format: problem+json, built in one place.** Every error answer is `application/problem+json` per `api/README.md` (`type`, `title`, `status`, `code`, `detail`, `errors[]` with `field`/`message`/`code`, `traceId`), produced only by `core`'s `GlobalExceptionHandler` and `ProblemResponseWriter` (for the filters). No module defines its own error body class (`ErrorMessage`, `ErrorDetail`, `issues`) or its own `@RestControllerAdvice`.

**11.2 Never echo an exception's message to the client.** `detail` is a sentence the code writes on purpose, naming at most a parameter, a field or an id — never `exception.getMessage()`, never a database, framework or library message (they carry SQL, row values, class names and stack details), never personal data or secrets. The catch-all handler logs the exception with the trace id and answers 500 `INTERNAL` with no detail.

```java
// ✗
@ExceptionHandler
public ResponseEntity<ErrorMessage> handleException(Throwable ex) { … new ErrorDetail(prefix, INTERNAL_ERROR, ex.getMessage()) … }

// ✓ core GlobalExceptionHandler
@ExceptionHandler(MissingServletRequestParameterException.class)
public ResponseEntity<Map<String, Object>> missingParameter(MissingServletRequestParameterException exception) {
    return badRequest("Missing parameter " + exception.getParameterName() + ".");      // a name, not a message
}

@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, Object>> unexpected(Exception exception) {
    LOG.error("unhandled exception", exception);                                         // logged with the trace id
    return Problems.response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", null, List.of());
}
```

**11.3 One typed exception per meaning — never a catch-all.** Expected failures throw `core`'s typed exceptions (all extend `ApiException`, which fixes the status and the machine code), never a bare `RuntimeException` / `IllegalStateException` / `IllegalArgumentException` for a case a client can trigger, and never one general exception (`BusinessException(code, args)`) whose status defaults to 400 for every rule:

| Situation | Exception | Status / code |
|---|---|---|
| Absent, deleted or outside the caller's scope | `ResourceNotFoundException` | 404 `NOT_FOUND` |
| A unique key is taken | `DuplicateResourceException` | 409 `DUPLICATE` |
| The row's state does not allow the step; a referencing row prevents it | `InvalidStateException` | 409 `INVALID_STATE_TRANSITION` |
| Another business conflict with its own code (a limit reached, an overlapping period) | `ConflictException(code, detail)` | 409 `<code>` |
| Field and business-rule validation (every failing field at once) | `ValidationFailedException` | 422 `VALIDATION_FAILED` |
| Malformed parameter, filter, mask or header | `InvalidArgumentException` | 400 `INVALID_ARGUMENT` / `INVALID_FILTER` |
| Stale `If-Match` or body etag | `PreconditionFailedException` | 412 `ETAG_MISMATCH` |
| Missing `If-Match` where required | `PreconditionRequiredException` | 428 `PRECONDITION_REQUIRED` |
| The caller may not do this to this row | `PermissionDeniedException` | 403 `PERMISSION_DENIED` |
| A contract operation not served yet | `NotImplementedException` | 501 `NOT_IMPLEMENTED` |

```java
// ✗ every rule a 400 under one class
throw new BusinessException(CommonMessages.PARAM_DUPLICATE, Organization.FIELD_NAME, name, Organization.ENTITY_NAME);
throw new BusinessException(MAX_ACTIVE_ORGANIZATIONS_LIMIT_REACHED, limit);
throw new BusinessException(PARENT_DELETION, SOFT_DELETE_OPERATION, id);

// ✓
throw new DuplicateResourceException("A subject with this code already exists.");
throw new ConflictException("ACTIVE_LIMIT_REACHED", "The tenant already has the most active institutions its plan allows.");
throw new InvalidStateException("The subject is still referenced by academics.class_subject rows; remove those first.");
```

- Message texts live in `constants/` when shared; reuse an existing code before minting one, and add a new code to the problem+json registry in `api/README.md` in the same change.

---

## 12. Testing — four layers, four triggers

Adopted unchanged from the source ADR, including source roots and naming:

| Layer | Source root | Pattern | Runs in `mvn test` | Trigger |
|---|---|---|---|---|
| Unit | `src/test/java` | `*Test` | yes | always |
| Component | `src/component-test/java` | `*ComponentTest` | yes (build-helper merges the root) | always |
| Contract | the test kit's `CrudContract`, run by each `<Module>CrudComponentTest` | owned | yes | always |
| E2E | `src/e2e-test/java` | `*E2ETest` | no (Surefire exclude) | `-P e2e` |

- **Unit**: no Spring context, no I/O. Mappers (round-trip + ignore-list), validators, SQL fragment builders, pure logic. Deterministic fixtures; no live dependencies; Mockito for collaborators.
- **Component**: `@SpringBootTest` against **real PostgreSQL with the real Flyway migrations** — never H2, never a hand-built schema. `core`'s test kit `TestDatabase` gives each test class a fresh database with two fresh non-superuser, non-BYPASSRLS roles — the owner Flyway migrates as and the application role that only receives DML grants — so row-level security is enforced and cannot be switched off, exactly as in production on a Testcontainers PostgreSQL 16, or on an existing server named by `MYSMS_TEST_JDBC_URL` / `_USER` / `_PASSWORD` (CI without Docker). `TestDatabase.createDropOnExit()` drops the database at JVM exit only after Spring's shutdown hook has closed the cached contexts (it waits up to 15 s for the database's sessions to end, then forces the drop), so the outbox relay is stopped first and logs no terminated-connection failures. `auth-client`'s test kit `TestTokens` signs access tokens with a throwaway key. Every module ships `ModuleBoundaryTest` (unit: own-schema SQL, gRPC allowlist) and `<Module>ServiceComponentTest` (the microservice boots alone on its own database, migrates only its schema, reports its database in health, and its edge refuses undeclared routes and missing tokens). Each repository has at least one component test proving tenant-scoping, soft-delete filtering and the `row_version` guard (0-row update → 412 path). Test properties files carry no production secrets.
- **Contract tests per resource**: each module's `<Module>CrudComponentTest` runs `CrudContract` (auth-client test kit) over every resource in its `crud-spec.json` (owned test data: one entry per resource, with sample bodies valid against the migrated schema) — create (201, ETag, Location, echoed values), validateOnly writes nothing, a duplicate is 409, get and the read mask, list, filter and paging, 404 for an unknown id, 403 without the permission, another tenant never sees the row, the institution scope (404 outside it, 403 for writes; tenant-level rows read by all and changed only tenant-wide), update under If-Match (428 without, 412 when stale, 400 for an unknown mask field), every generated state transition (403 without its permission, 404 outside the scope or 403 for a tenant-level row without a tenant-wide grant, 428/412 for a missing or stale If-Match where declared, 412 for a stale body etag, 200 from the guarded state with the listed values set and the version moved on by one, then 409 with nothing changed when the new state no longer allows the step) and soft delete. The sample payloads and their parent rows are generated from the schema and are first proven against a migrated database by the generator, so a failure is a real defect. `<Module>ControllerStubsTest` pins how many contract operations still answer 501.
- **Contract**: locks the REST surface for published resources against the module's operation table and contract interfaces by driving the real service over HTTP, and the gRPC surface by the parity tests (§10.3) (the component tests do this for every implemented operation); a contract-breaking controller change fails before integration.
- **Mocks only in unit tests.** Mockito, fakes and stubs are allowed in unit tests and nowhere else: component, contract and E2E tests run real code against real PostgreSQL, real Flyway migrations and real signed tokens (`TestTokens`). An outbound adapter to an external provider (mail, SMS, payment gateway, storage) is exercised in component tests against a real local implementation of that protocol (a Testcontainers container such as a local SMTP or S3-compatible server), never a mock bean.
- **E2E**: black-box against a deployed stack; always excluded by default; no sleep-based flakiness without a stated reason.
- A change to the canonical write path, a mapper ignore-list or a migration requires tests in the matching layer in the same PR. Minimum line coverage is enforced by JaCoCo in the parent POM (bar set there, not debated per PR).

---

## 13. Build quality gates

- `mvn verify` = compile + unit/component tests + **Checkstyle** + **SpotBugs** + JaCoCo check. All four bound in the parent POM; CI runs exactly `mvn verify`. A PR that needs `-DskipTests` or `-Dcheckstyle.skip` does not merge.
- Suppressions (`checkstyle-suppressions.xml`, `spotbugs-ignore.xml`) each carry a one-line reason; whole-package or wildcard suppressions require explicit review; agents never add suppressions for their own findings without approval (§1.9).
- Fix order: tests green first, then static analysis — don't iterate style on broken code.
- Generated code (MapStruct impls, protobuf and gRPC stubs) is excluded from style/coverage; it is never hand-edited. SpotBugs analyses only a module's own packages (the parent POM's `mysms.spotbugs.onlyAnalyze`: the root package and every §3 layer package), so the thousands of generated proto classes a module compiles are not analysed; conformance §3 fails the build when that list misses a layer or a domain module narrows it.
- The build runs two modules at a time (`.mvn/maven.config`: `-T 2`) with the Maven build cache (`.mvn/extensions.xml`), which skips a module whose sources and inputs are unchanged. A gate build — before a merge or a release — runs with `-Dmaven.build.cache.enabled=false`. Test JVMs are capped (`mysms.test.jvm`: 1 GB heap, C1 JIT) so two of them run beside the Maven JVM (4 GB, `.mvn/jvm.config`) and the PostgreSQL containers.
- The `conformance` module (`backend/conformance`, run by `mvn verify`) checks the rules a compiler, Checkstyle or SpotBugs cannot: R1 tenant predicate, R2 SQL as repository constants, R5 plain repository classes, B1 no `@Transactional`/`TransactionTemplate`/ORM/ArchUnit/Modulith, A1 module dependencies, §3 package roles, §5/N1 entities (base class, Lombok, no Jackson/mapping annotations, no `Map` columns, projections outside `domain/`, hooks only in services), §6 no hard DELETE on audited tables, C3 controllers implement contract interfaces, M1 mapper configuration and way-in ignores, D2 DTO shapes, E2 error format, X1/X2 no token handling in modules, test rules (mocks only in unit tests; every module has its boundary and service component tests) §14 migration schema and no cross-schema foreign key, and G1–G9 the gRPC guideline (§10.6: owned protos and verbatim copies, field-number lock, operation table = RPCs, stubs only in `integration`, `@GrpcService` servers, no REST client between modules, no `Transactions` helper, every module's OpenAPI component test, each module's application.properties combined by the monolith's — §2.1). The build fails on any violation.

---

## 14. Migrations (Flyway) — authoring rules

> Amended by §2.2: per area (`database/migrations/<area>`); an area's migration may reference its module's sibling schemas, never another module's.

The platform-wide migration principles of the source, applied to MySMS's existing Flyway layout (`database/migrations/<module>/V<release>_<module-order>_<seq>__description.sql`):

- **Until the first release, one baseline per module.** Each module has exactly one script, `V1_<module-order>_001__<module>_baseline.sql`, which by itself builds the module's final schema and catalogue data. A change edits it in place the way a DBA would write it from scratch: a new column goes into the `CREATE TABLE` (business columns before the audit/version columns), a changed constraint or index replaces the old one, a comment replaces the old comment, a new table sits next to its related tables with its comments, indexes and RLS, and catalogue rows are inserted in their final form. No incremental `V1_…_002` script, no backfill.
- A migration, the baseline included, touches only its own module's schema; a foreign key never crosses schemas — a reference into another module is an indexed uuid column registered in `database/logical-references.md` (regenerate with the schema docs).
- **Foreign keys: inside a module yes, across modules never (owner rule, sacred — never relaxed, no exception, no
  waiver).** A foreign key may join tables of one area or of sibling areas of the same module: a module's schemas
  always live together on one database instance, so the key is always enforceable. A foreign key into another
  module's schema is forbidden in every case; that reference is a logical reference (an indexed uuid column, §2.1
  "Logical references", `database/logical-references.md`). Conformance §14 fails the build on a cross-module key.

  ```sql
  -- ✓ hrms: payroll → hr (sibling areas of one module)
  ALTER TABLE payroll.staff_salary ADD CONSTRAINT fk_staff_salary_staff_id
      FOREIGN KEY (staff_id, tenant_id) REFERENCES hr.staff (id, tenant_id);
  -- ✗ academics → hrms (another module): never; academics.teacher_subject.staff_id stays a logical reference
  ```
- After the first release: one logical change per migration file; descriptive name; the owning module's folder.
- Released migrations are immutable (§1.2). Post-go-live changes are backward-compatible with the previous running binary: add nullable/defaulted columns, add tables/indexes (`CONCURRENTLY` outside transactions where needed); renames and drops are expand-contract across two releases; `NOT NULL` lands as `NOT VALID` + `VALIDATE` after backfill.
- Every new table carries the full MySMS audit set, tenant column + RLS policy, comments on every column, and partial-unique business keys per `database/README.md`. Naming follows the schema's existing conventions (`pk_/fk_/uq_/ix_/ck_` prefixes) — consistency with this schema outranks any other naming scheme.
- Reference data ships as repeatable seed templates used by tenant provisioning, never as hard-coded inserts in versioned migrations.
- CI rehearses every release: clean apply + previous-release upgrade apply + `verification/smoke.sql`.

---

## 15. Review checklist (apply to every Java PR)

Adapted from the source's design-compliance aspect list; a reviewer (human or agent) reports pass/fail per ID:

| ID | Check |
|----|-------|
| C1 | Controllers thin: no repository/SQL/business rules; contract interface implemented, not re-declared |
| C2 | Full-DTO validation invoked on create/update; patch re-validates the merged model |
| C3 | Controllers implement the module's api-package contract interface: mappings only on those interfaces, none on controllers, no base controller, no side effects (events, audit, calls, cache) |
| A1 | Module depends only on core/auth-client (messaging per §2); cross-module data fetched through its `integration` gRPC clients (§10); SQL touches only the module's own schema |
| B1 | One plain service per aggregate, explicit methods; no generic CRUD base service, no `@Transactional` — each service extends `AppTransactionManager` and runs its work through `doInReadTransaction` / `doInWriteTransaction` on its module's `PlatformTransactionManager` bean |
| B2 | Audit + outbox rows written explicitly in the same transaction as the aggregate write |
| B3 | `row_version` guard present on update/patch/delete; 0 rows → 412 |
| R1 | Reads filter `tenant_id` and `deleted_at IS NULL`; partitioned queries carry the range filter |
| R2 | SQL as constants/resources, named params, `ANY(:ids)` child loads, sort allowlist |
| R3 | SQL references only the module's own schema — no cross-schema join, read, write or lock; another module's data comes through its gRPC service |
| R4 | New cross-module reference columns are validated through the owner's gRPC service before the write, indexed, registered in `logical-references.md`, and handled in the owner's delete/archive event consumer |
| M1 | Every mapper uses the module's one `@MapperConfig` (`unmappedTargetPolicy=ERROR`); the way in ignores id, tenant and the whole audit set; no null-value strategies, no `@MappingTarget` patching, no logic or state in mappers (§8) |
| D1 | No entity beyond the business layer; DTOs match the contract; Update DTO contains only writable fields |
| D2 | Transport DTOs only in the api/model contract package (no ad-hoc `*DTO` classes); PATCH with an `Optional`-field `<Resource>Update` and no `updateMask` (§7.2; `Optional` nowhere else; no PUT replace); `@Valid` on type arguments, never on containers; contract shapes only (`<Resource>List`, 204 delete, no envelopes); read mask via `ReadMask` (no `@JsonView`/`ResponseBodyAdvice`) (§7) |
| N1 | Entities: exact audit set (`OffsetDateTime`, nullable `deletedAt`, `rowVersion`, UUIDv7), exact Lombok, no Jackson/mapping annotations, no view data, no `Map` columns; projections are records in `repository/`; the table named through `TABLE`; hooks called by the service with actor and clock (§5) |
| R5 | Repositories are plain `@Repository` classes: no repository interfaces, derived queries, `@Query` or `*Impl` fragments; no SQL built at runtime from names or metadata; no database functions; keyset paging (§6) |
| L1 | After-commit code (cache refresh, notification triggers) writes no business rows and runs only after a successful commit |
| O1 | Idempotency keys, operations and document numbers use the module's own tables; long-running work returns 202 + `Operation` with `module` set |
| G1 | gRPC delegates to the same business service; REST/gRPC error parity |
| E1 | Typed exceptions + problem+json codes; no personal data in messages/logs |
| E2 | Errors only through `GlobalExceptionHandler` / `ProblemResponseWriter` (problem+json); `detail` never carries an exception's message; one typed exception per meaning (§11.3), no catch-all business exception |
| S1 | No secrets/PII in code, config, fixtures; `*_enc` handled write-only |
| T1–T5 | Tests in the right layer, Testcontainers+Flyway for components, contracts updated with surface changes, E2E still excluded by default (N/A when no test paths in diff) |

Pre-PR self-check = the same list plus: `mvn verify` green locally, no new suppressions without comments, released migration files immutable (before the first release: the module's baseline edited in place, no new script).

---

## 16. Authentication and authorization

**Protocol.** OAuth 2.0 + OpenID Connect, built the OAuth 2.1 way (PKCE everywhere, no implicit/password grants, refresh-token rotation with reuse detection). The **iam module is the OIDC issuer** (Spring Authorization Server embedded — identity data stays in the `iam` schema); every module, iam included, is an OAuth2 **resource server** validating JWTs against the issuer's JWKS. Because trust is "an OIDC issuer", swapping or adding an external IdP later (Zitadel, Keycloak, Ory Hydra — e.g. for enterprise SSO, P1-39) is an issuer-URI/brokering change, never a service-code change.

**The `auth-client` module is the only auth surface a module ever touches.** It is a hand-written facade owned by the iam team, plugged into all 21 modules, and it hides every protocol and vendor detail behind MySMS's own wrapper types:

- `AuthContext` — the request's identity: user id, tenant id, institution ids, permission codes, token id, impersonation flag. Read-only; the only thing business code sees.
- `TokenVerifier` — validates the bearer token (local JWKS verification, no network call per request; key cache + rotation handling). Interface ours; the Spring Security/Nimbus machinery is an implementation detail inside auth-client.
- `PermissionChecker` — answers "may this principal perform `<x-permission code>`?" from the caller's role grants, resolved through the role→permission cache that iam keeps current (§16.2); a tenant user's token carries role claims, never a permission list; platform-staff and service tokens carry `perms`.
- Filters/interceptors — **all checks happen at the edge, never scattered in business code** (the servlet `AuthFilter` is built; the gRPC interceptor follows with the first gRPC service): one servlet filter chain (REST) and one gRPC interceptor (both auto-configured) that verify the token, resolve the tenant, enforce the route's `x-permission` code from the contract, populate `AuthContext`, and emit the access-log/audit hooks. A controller or service never parses a token, never reads a header, never checks a permission inline; it reads `AuthContext`.

**The issuer today.** iam signs in MySMS's own apps through the contract's first-party operations — `iam.Login`, `iam.RefreshToken`, `iam.Logout` — not through an OAuth password grant for third parties. Passwords are argon2id (19 MiB, 2 iterations, 1 lane); every refusal in a known tenant costs one verification and one write transaction whether or not the account exists; every failure gets the same 401 `INVALID_CREDENTIALS`; from the 5th consecutive failure password sign-in is locked for 1, 2, 4… up to 60 minutes (the lockout never ends existing sessions: refresh needs only an active account in an active tenant). Access tokens are RS256 JWTs (15 min) with `sub`, `tid`, `perms`, `ins` (`*` for a tenant-wide grant: every institution), `roles`, `pid` (linked person), `sid` (session), `jti`, `amr`, audience `mysms`; the key comes only from the environment and its public half is served at `/oauth2/jwks`, together with the retiring key during a rotation (`MYSMS_IAM_PREVIOUS_PUBLIC_KEY`). Refresh tokens are opaque (`<tenant>.<256 random bits>`), stored as SHA-256, rotated on every use, and a reused one revokes its whole session (a token revoked by sign-out is refused without that alarm). Sign-out also deactivates the session's push device (or all devices). A complete permission set is compacted in `perms` (`*`, `<module>.*`, `<module>.<resource>.*`) and `AuthContext.hasPermission` expands it, so broad roles fit in a header. The authorization-code + PKCE endpoints for partners and external IdP brokering (Spring Authorization Server) are the next step and change nothing for modules.

**How the edge decides (servlet filter, in order).** (1) Infrastructure paths (`/actuator/health/**`, `/actuator/info`, `/oauth2/jwks`; plus `/v3/api-docs*` and `/swagger-ui*` only when the operator sets `mysms.auth.api-docs` — `MYSMS_API_DOCS`, off by default: the document maps every operation and permission) pass. (2) The request must match an operation of the contract — the operation table is each module's own `META-INF/mysms/operations/<module>.json` (owned data, pinned to the contract by the conformance build), and the most specific path template wins — otherwise 404. (3) A bearer token, when present, is verified locally (signature against the issuer's JWKS, issuer, expiry with 60 s skew, audience `mysms` or `mysms-platform-admin`); invalid → 401 (`TOKEN_EXPIRED` / `UNAUTHENTICATED`, with `WWW-Authenticate`). (4) Operations whose contract allows calling without a token (`security: []` or `{}`) proceed, with the identity bound if a valid token came along. (5) Otherwise the token kind must match the operation's scheme: tenant users (`bearerAuth`, token has `tid`) or platform staff (`platformAdminAuth`, audience `mysms-platform-admin` and `amr` containing `mfa`) → else 403. (6) `X-Institution-Id`, when sent, must be one of the token's `ins` → else 403 (400 if not a UUID). (7) The operation's `x-permission` must be granted to the caller (§16.2: for a tenant user the role claims resolved through the role→permission cache, 503 when iam cannot answer; for platform staff and services the token's `perms`) → else 403 `PERMISSION_DENIED`. Only then are `AuthContext` and the core call context (tenant, user) bound for the request and cleared afterwards. The matched operation is marked on the request, so an operation of the contract that this release does not serve yet is answered **501 `NOT_IMPLEMENTED`**, never 404. Claims used: `sub`, `tid`, `perms`, `ins`, `jti`, `amr`, `act`, `pid`, `sid`.

**Data rules stay in the service.** The edge decides *whether* a caller may run an operation; the business service decides *which rows*: `people.GetMyPerson` acts only on the token's `pid`; an administrator holding `iam.setting_value.*` through a department grant acts only on that department's values (a tenant-wide or direct grant covers every scope); a setting users may not change is invisible (404) to anyone not administering settings at one of its scopes. A row outside the caller's rules is 404 on reads and 403 on writes.

**The institution data scope (`DataScope`).** Every standard read and write of a tenant table applies the same rule through `auth-client`'s `DataScope`, never re-implemented per service. A row belongs to an institution through its own `institution_id`, or through the in-module foreign keys that lead to a table carrying one (a guardian link → the student → the institution); a row with neither is *tenant-level*. Reads see institution rows of the caller's institutions only — narrowed to the one selected with `X-Institution-Id`, all of them for a tenant-wide grant — plus every tenant-level row; writes need the row's institution in that set, and a tenant-level row (catalogues, categories, tenant configuration) changes only through a tenant-wide grant. So a school-level administrator can manage their school's data but can neither see another school's rows (404) nor change what applies to the whole tenant (403). The scope is computed for the permission being exercised: only a permission carried by a tenant-wide (or direct) grant — the token's `tperms` — reaches every institution and tenant-level rows, so a tenant-wide read-only role combined with a school role that may write never lets the writes leave that school; an absent `tperms` claim means no tenant-wide permission at all. An institution row is its own scope (`org.institution`: school-level callers see and change only their own institutions). Known limit: `ins` is one set for all of a caller's institution grants, so a caller holding different roles in two schools has the union of those roles' permissions in both. Nested creates under a parent the caller cannot see answer 404, like reading it. Finer persona rules (a guardian's own children, a teacher's sections) are separate operations.

**16.1 No token handling in a module.** No module contains an authentication filter, reads the `Authorization` header, or imports a token library (`io.jsonwebtoken`, `com.nimbusds`, `org.springframework.security.oauth2`): the token is verified once, by `auth-client`'s `AuthFilter`, and a module only reads `AuthContext` / `DataScope`. The one exception is the issuer: iam's `business/TokenIssuer` signs access tokens and publishes the JWKS with Nimbus, and nothing in iam parses or verifies a token — incoming requests to iam go through the same `AuthFilter`. Password hashing (argon2id, `org.springframework.security.crypto`) is iam's alone and is not token handling.

```java
// ✗ inside the service's own source tree
public class JWTAuthorizationTokenFilter extends OncePerRequestFilter {
    String jwtToken = request.getHeader(AUTHORIZATION_HEADER).replace(TOKEN_PREFIX, "");
    Claims claims = tokenInterpreter.getClaimsFromToken(jwtToken);          // io.jsonwebtoken
    SecurityContextHolder.setUserContext(tokenInterpreter.parseClaims(jwtToken, claims));
}

// ✓ a module never sees the token; auth-client's AuthFilter is auto-configured
DataScope scope = DataScope.current(PERMISSION_UPDATE);                     // who may write which rows
Optional<UUID> actor = Actor.current();                                     // who is acting (audit stamps)
```

**16.2 Every service publishes its API permissions and enforces them in its own permissions filter.**

- **Catalogue, published by the service.** A module's operation table (`META-INF/mysms/operations/<module>.json`: method, path template, operation id, `x-permission`, security scheme) is its API-permission catalogue. At start-up (`ApplicationReadyEvent`) and every `mysms.auth.catalogue-interval` auth-client's `ApiPermissionPublisher` publishes it to iam, the access-control authority, as the event `<module>.api_permissions.published` with the module name, release version and a SHA-256 of the catalogue. Applying it is idempotent, and the publish never blocks start-up. iam keeps the catalogue as data: new permission codes become assignable to roles, and codes no running release has published for `mysms.iam.api-permission.retire-after` are retired (never deleted); every role read carries `holdsRetiredPermissions` for the roles that still hold one.
- **Decision, made in the serving process.** Every module's servlet `AuthFilter` and gRPC `GrpcAuthInterceptor` (auth-client, auto-configured, so every module has them with no code of its own) is the service's permissions filter. It checks the operation's permission before the controller or resolver runs. It resolves the caller's role grants (from the token) to permissions through a per-process role→permission cache, keyed by tenant and role. iam fills the cache on demand (batch gRPC `GetRolePermissions`, in-process in the monolith), as the reference access-control service's filter falls back to its role service. There is no invalidation event: a short TTL bounds staleness, and the token's `authz_version` lets the filter refetch when its cache is older than the caller's grants. A role change therefore takes effect within the TTL, not at token expiry.
- **Fail closed.** If the permission cannot be resolved (iam unreachable and nothing cached), the filter refuses with 503 for authenticated callers. It never admits.
- **The decision never leaves the service.** A gateway, iam or any other central service may hold the catalogue and the role data, but the decision is always made by the serving process's own filter. A request that reaches the service by another route (an internal call, a misconfigured gateway) is still checked.
- **Built and checked at build time too.** The conformance build still pins the operation table to the contract and checks that every `x-permission` is a known code (`PermissionCatalogueRule`). Runtime publishing does not replace that check.

Status: built in every module (auth-client `ApiPermissionPublisher`, `RolePermissionResolver`, `IamAuthzGrpcClient`; iam `ApiPermissionService`, `AuthzService`, `LocalRolePermissions`); operating notes in DEPLOYMENT.md §5.10.

**16.3 Authentication is tested for real.** Component and contract tests sign tokens with `TestTokens` (a throwaway key the test process trusts), send them through the real `AuthFilter`, and assert 401 for a missing, expired or forged token, 403 for a missing permission or the wrong kind of account, and 404 outside the data scope. No test replaces the verifier, the claim parser or the filter with a mock (§1.8, §12).

```java
// ✗
@MockitoBean protected TokenInterpreter tokenInterpreter;      // every request "authenticated"
when(tokenInterpreter.parseClaims(any(), any())).thenReturn(adminContext);

// ✓
private static final TestTokens TOKENS = new TestTokens();
registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
String token = TOKENS.tenantUser(TENANT, user, "academics.subject.create");     // signed, verified for real
```

**Switching implementations.** Only auth-client's internals change when the issuer changes; its public types (`AuthContext`, `TokenVerifier`, `PermissionChecker`, filter wiring) are frozen API for the 21 modules. No Spring Security type, Nimbus type, or vendor class may appear in a module's signatures or imports — checkstyle bans the packages outside auth-client and iam.

**Hard security floor** (regardless of issuer): argon2id password hashing; tokens stored as hashes only; short-lived access tokens; refresh rotation + reuse detection; MFA per the contract (`EnrollMfa`/`CompleteMfa`); rate limiting + progressive lockout on login/reset/verification; per-tenant envelope encryption for `_enc` columns (core `FieldCipher`: AES-256-GCM, a per-tenant key derived with HKDF-SHA256 from a master key that comes only from the environment / secret store, never the DB; values bound to tenant and column; rotation by key id); RLS as the cross-tenant backstop; immutable audit log; JWKS key rotation; secrets never in git. A change that weakens any of these is a blocker, whatever it simplifies.

| ID | Review check |
|----|-------|
| X1 | No token/header parsing or permission check outside auth-client; business code reads `AuthContext` only |
| X2 | No Spring Security / Nimbus / vendor auth type in any module's imports (auth-client and iam excepted) |
| X3 | New operations carry the contract's `x-permission` code and are covered by the filter mapping |
| X4 | Component and contract tests authenticate with real signed tokens (`TestTokens`) through the real filter; the verifier and the filter are never mocked |

---

## Appendix A — what was deliberately NOT adopted

- **Nested Maven modules and per-module `-contract`/`-client`/`-service`/`-boot` artifacts** — per project decision the layout is flat: one module per schema owning its own contract package and protos, no shared client module, exec-classifier jars instead of `-boot` launchers.
- **Per-operation springdoc annotation blocks and ad-hoc `*DTO` classes** (`@Operation`/`@ApiResponse` walls on every method; ad-hoc `*DTO` classes, patches included; custom paging and success envelopes; `@JsonView` / response-body advice for slim responses) — MySMS's contract interfaces carry the Spring mappings and validation, core's `OpenApiConfiguration` restores ids, permissions and security document-wide, patches use the contract's own `<Resource>Update` with `Optional` fields (the reference pattern, adopted by owner decision — §7.2) applied by explicit service code, never MapStruct, and responses are the contract's shapes and the read mask (§7).
- **Spring Data repository interfaces and framework lifecycle callbacks** (`CrudRepository`, derived queries, `@Query`, `*Impl` fragments, `BeforeSaveCallback` stamping audit columns) — repositories are plain classes and the service calls the hooks (§5.8, §6).
- **A generic `BaseController` / `BaseEntityMapper` and one catch-all `BusinessException`** — controllers implement the contract interfaces, each module has one mapper configuration, and failures are typed (§8, §9, §11.3).
- **Token parsing in each module** (a per-module JWT filter in the module's own source) — one auth facade (`auth-client`) verifies the token; the permissions filter it supplies runs in every module (§16.1, §16.2). Publishing API permissions to iam *is* adopted (§16.2); taking the decision out of the serving process is not.
- **Spring Modulith and ArchUnit** — per project decision. Isolation is structural: domain modules have no Maven path to each other, so there is nothing for a boundary library to check.

- The SDD/OpenSpec workflow (specs, change folders, human gates, discovery phases) — per project decision, only engineering rules carry over.
- The **templatized service layer** (`BusinessServiceApi<T,P,ID>`, `BaseDatabaseBusinessService`, the `OperationListener` pre-/post-commit pipeline, `MapperContext` transport binding) — per project decision. MySMS writes one plain, explicit service class per aggregate; the *obligations* the template enforced (one-transaction writes, lifecycle hooks, audit + outbox in-tx, row_version guard, after-commit side effects) are kept as visible code and review checks (§4, §15).
- **Annotation-driven transaction management** (`@Transactional`) and `TransactionTemplate` — per project decision. All transactions are programmatic: each service extends `AppTransactionManager` (§4, §6), which drives the module's `PlatformTransactionManager` bean and binds the tenant through `ModuleDatabase.bindTenant()`.
- Liquibase (source bi-0006): MySMS standardized on Flyway before these rules; bi-0006's *principles* (immutability, backward compatibility, naming discipline, audit columns) are kept in §14.
- Spring Data JDBC repositories (source bi-java-0001): MySMS forbids every mapping framework on the write path; the ADR's *reasons for rejecting JPA* and its batch-write architecture are kept, implemented on plain `JdbcClient`.
- The source's offset pagination envelope (`data`/`paging`, no totalCount) and `issues` error envelope: MySMS's API contract already defines cursor paging + problem+json; the *intent* (no expensive counts, stable machine codes, uniform envelopes) is kept.
- `is_deleted` boolean + `deleted_at BIGINT DEFAULT 0` soft delete and uniqueness-includes-deleted_at: MySMS uses `deleted_at timestamptz NULL` + partial unique indexes `WHERE deleted_at IS NULL`, which achieves the same re-creation guarantee.
- FHIR type mappings, org-hierarchy scoping, Apigee gateway rules, GCP Pub/Sub audit sinks, consent framework — platform-specific to the source; MySMS has its own equivalents (RLS tenancy, audit via each module's outbox into `platform.audit_log`, gateway TBD).
- VARCHAR length whitelist (255/1024/4096): MySMS's schema already defines per-column lengths; follow the schema.

## Appendix B — source material and deviations

| Section here | Source | Adopted | Deviations |
|---|---|---|---|
| §1 | rules/permissions.md, rules/phi-pii.md | hard-constraint floor, agent editing policy | branch names/memory rules generalized; PHI→student data |
| §2 | ADR bi-0014, project-conventions.md | SNAPSHOT lifecycle, APT, pinned parent | module set = MySMS DAG |
| §3 | project-conventions.md, layer-reference.md | package tree by layer role | `example/` vertical dropped; `api/` holds the module's own contract (owned source, §7.1); cross-module access only through the module's `integration` gRPC clients (§10) |
| §4 | canonical-template-reference.md §1–§4, §12–§13, §23–§24, §30; layer-reference.md | the write-path *obligations*: one-tx writes, lifecycle hooks, audit+outbox in-tx, 412 on stale version | generic base service, listener pipeline and `MapperContext` rejected — plain per-aggregate services; `@Transactional` and `TransactionTemplate` replaced by programmatic `PlatformTransactionManager`; re-based from Spring Data JDBC onto JdbcClient |
| §5 | data-model.md, canonical §9, §22 | BaseEntity + hooks, no plain POJOs, records for projections | TaggedEntity default narrowed: MySMS tags via platform.tag_assignment where the schema says so |
| §6 | ADR bi-java-0001, canonical §10, §16, §24, §26, §32 | explicit SQL, batch template, sort allowlists, no N+1 | no repository derivation at all; tenant predicate + RLS added |
| §7 | rest-api.md DTO + Partial DTO sections | DTO/entity separation; partial-update DTOs carry only writable fields | DTOs are owned source in the module's `api/model` package instead of hand-written Lombok classes; three-way patch semantics come from the reference service's `Optional` fields (owner decision, PATCH bodies only; `updateMask` dropped), applied by core `Patches` instead of MapStruct; gRPC lists are replaced only when masked |
| §8 | canonical §6, §8; design-review M1 | id/audit ignores on the way in, AfterMapping discipline | per-module `@MapperConfig` + per-aggregate mappers instead of the generic BaseEntityMapper (the contract DTOs share no base type); unmappedTargetPolicy=ERROR made mandatory; partial updates applied by one explicit `Patches` line per field (absent / null / value); MapperContext transport binding dropped with the service template |
| §9 | canonical §7, §20; ADR bi-0007 | thin controllers, explicit validator call, single exception handler, `PagingInfo`/`NavigationInfo`/`PagingUtil` list responses | status map replaced by MySMS api/README (ETag/412, Idempotency-Key); the paging under `PagingInfo` is keyset-by-cursor with a capped offset for page jumps instead of plain offset |
| §10 | canonical §14, §27; ADR bi-0008 | shared business service, proto-as-T, status parity | field-numbers lock is MySMS's own mechanism |
| §11 | rest-api.md validation mapping; ADR bi-0007 §3 | typed exceptions, code registry discipline, no-PII details | codes from MySMS problem+json registry |
| §12 | ADR bi-java-0002, testing-philosophy.md | four layers, roots, naming, triggers, Testcontainers, no prod secrets | H2 explicitly banned; Flyway-in-tests required |
| §13 | project-conventions.md static analysis | checkstyle+spotbugs+jacoco on verify, suppression policy | — |
| §14 | ADR bi-0006, liquibase-migrations.md | immutability, one-change-per-file, expand-contract, comments | Flyway + MySMS naming/audit set |
| §15 | design-compliance-review.md | aspect checklist | IDs remapped to MySMS layers |
