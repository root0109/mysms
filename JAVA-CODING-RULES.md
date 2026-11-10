# MySMS Java coding rules

Binding rules for every line of Java written for MySMS. They adapt a reviewed enterprise rule set (ADRs and conventions from an internal platform distribution) to MySMS's own decisions; where the two disagreed, MySMS's established architecture wins — the deviations are listed in [Appendix B](#appendix-b--source-material-and-deviations). The SDD/OpenSpec workflow of the source was deliberately **not** adopted; only the engineering rules were.

**Precedence (highest first):** `database/README.md` + migrations (schema truth) → `api/` contracts (interface truth) → `ARCHITECTURE.md` (module/dependency truth) → this file (code-shape truth). A rule here never licenses drift from the contract or the schema.

**Audience:** humans and AI agents equally. An agent that cannot satisfy a MUST stops and says so; it does not approximate.

---

## 1. Hard constraints (never violated, by anyone)

1. **No ORM, ever.** No JPA, no Hibernate, no Spring Data JDBC repositories, no entity proxies, no `ddl-auto`. Persistence is plain Spring JDBC (`JdbcClient` / `NamedParameterJdbcTemplate`) with hand-written, reviewable SQL. (Project rule; reinforced by the source ADR that rejected JPA for batch-oriented write paths.)
2. **Flyway owns the schema.** No DDL from application code, no schema generation, no manual DDL in shared environments. The one exception is core's partition maintenance (database/README "Partitioned high-volume tables"): it only attaches next months' partitions to the tables Flyway created and, when a retention is configured, drops months older than it — with a lock timeout, as the schema owner, never touching any other object. A migration applied in any non-ephemeral environment is immutable — never edited, renamed, reordered or deleted; corrections are new migrations.
3. **Never** drop or truncate tables or schemas (except the configured retention drop of a past month's partition, rule 2); never delete migration files; never force-push protected branches.
4. **Never commit secrets, credentials or personal data** to version control — including test fixtures, example payloads and OpenAPI examples. Student/guardian data is sensitive by default: synthetic names only, everywhere.
5. **No personal data in logs, error messages, metrics or traces.** `*_enc` values are write-only and never logged; display uses the masked form (`••••1234`, from a `*_masked` column or computed in memory from the decrypted value). Error `detail` strings carry ids, not names.
6. **Every SQL statement is tenant-scoped.** Every transaction binds `app.tenant_id` (`ModuleDatabase.bindTenant()`, transaction-local); every hand-written read adds `tenant_id = :tenantId` (RLS is the backstop, not the mechanism) and, unless deleted rows are wanted, `deleted_at IS NULL`.
7. **`backend/` is the Maven multi-module project and nothing else**: poms, `src/`, build configuration (`config/`, `.mvn/`). Every tool the build needs is Java inside the build: the `conformance` module (last in the reactor) checks these rules and fails on anything else (M0). No Python or shell scripts.
8. **Mocking only in unit tests.** Component, contract and end-to-end tests run against real PostgreSQL and real HTTP; no Mockito, `@MockBean` or stub servers outside `src/test`.
9. **This file is edited by humans** (or by an agent only when the change itself is the requested task). An agent never weakens a rule here to make its own diff pass, and never adds lint/static-analysis suppressions for findings it introduced without developer approval.

---

## 2. Maven multi-module layout

One repository, one parent POM, **flat modules — no nesting, no second level**. One Maven module per database schema; each domain module **is** its own microservice. Module names carry no product prefix — the groupId (`com.mysms`) namespaces the artifacts.

```
mysms/                # repository root (directory name only — not an artifact name)
├── pom.xml           # reactor + parent: dependencyManagement, plugin management, quality gates
├── core/             # plain library jar plugged into every module: BaseEntity, error model, paging,
│                     #   tenant context, ids (UUIDv7), validation, SQL fragment + batch helpers, the
│                     #   transaction definitions and helpers, cross-cutting auto-configurations. NO db schema, NO Flyway,
│                     #   NO datasource of its own, NO domain code.
├── sdk/              # GENERATED jar: the Java clients modules use to call each other (HTTP), and the gRPC stubs +
│                     #   protobuf messages of every module (com.mysms.<module>.v1), compiled from /api/proto in one
│                     #   protoc run (protobuf-maven-plugin + os-maven-plugin, the reference service's mechanism).
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

- **One module = one schema = one deployable.** Every domain module carries its own `@SpringBootApplication` main (component-scanning its own package + core/auth-client auto-configs) and nothing else that describes it: no descriptor class, no registration file and no configuration file (§2). The spring-boot plugin repackages with `classifier=exec`, so each flat module produces **both** a plain jar (consumed by `app`) and an executable microservice jar: `mvn -pl finance -am package` builds the finance microservice; `mvn -pl app -am package` builds the monolith.
- **Domain modules never depend on each other.** Every module uses `core`, `sdk`, `auth-client`, and the `messaging` runtime. Only modules that declare event consumers get `messaging` at compile scope; conformance permits them to import only `com.mysms.messaging.events` API types and rejects RabbitMQ, Google Pub/Sub, and adapter/runtime imports. Publishers use the core outbox writer and need no event API imports. Cross-module imports are therefore impossible at compile time from the dependency graph alone — no Modulith, no ArchUnit, no enforcement library.
- **Cross-module calls go through `sdk` only** (ids + Card DTOs), over HTTP/gRPC even when deployed together in `app` (loopback) — one code path whether assembled or split. Against-DAG needs use events (outbox), never a direct call. The allowed-dependency table in `ARCHITECTURE.md` governs which clients a module may configure.
- **One DataSource per module.** `core` provides the datasource factory; each module binds `mysms.<module>.datasource.*` and owns its pool and its transaction manager. All 21 default to the same PostgreSQL instance; moving a schema to its own instance is a configuration change, zero code. No connection is ever shared across modules, so a cross-schema join cannot exist.
- **Own-schema-only SQL.** Every statement in a module references only that module's schema. Enforced by a ~20-line plain JUnit check from `core` (regex over the module's SQL constants — no library) plus review check R3.
- **Flyway per module, per datasource.** Each module migrates only its own schema from `src/main/resources/db/migration/<module>/` with its own `flyway_schema_history` (`default-schema` = the module's schema). There are no cross-module migrations and no cross-module foreign keys, so a module's folder applies unchanged whether its schema shares an instance with others or has a dedicated one.
- **Logical references, not foreign keys, across modules.** A column pointing into another module's data is validated **before the write** by resolving the id through the owner's SDK client (tenant-scoped; not found / other tenant → 422). Referenced entities are archived, never hard-deleted; the owner publishes `*.deleted` / `*.archived` events and every dependent handles them as `database/logical-references.md` prescribes; each module runs a scheduled reconciliation job that batch-verifies its reference ids against the owners and reports orphans (never auto-fixes). Against-DAG references (listed there) are maintained from events only — the module never calls the owner for them.
- **Module-local infrastructure only.** Idempotency keys, long-running operations, document numbers, outbox and inbox all live in the module's own schema (`idempotency_key`, `operation`, `number_sequence`, `outbox_event`, `inbox_event`). Audit records are written as events into the module's own outbox in the business transaction; the platform module materialises them into `platform.audit_log`. A module never writes, reads or locks another schema's rows.
- **SDK allowlist.** `core`'s test kit (`ModuleChecks`) has a plain JUnit check, run by every module's `ModuleBoundaryTest`, that fails the build when the module imports an SDK client package (`com.mysms.sdk.<other>`) of a module outside its row of the allowed-dependency table in `ARCHITECTURE.md` — the table is read from the file itself, so the document and the build cannot disagree.
- **Trace context everywhere.** `core` propagates W3C `traceparent` on every inbound request, outbound SDK call and published/consumed event, and puts trace id, tenant id and request id in the logging MDC.
- **No shared domain code.** `core` holds base classes and cross-cutting infrastructure only — never a domain entity or domain DTO. An entity belongs to exactly one module; cross-module data travels as ids + Card DTOs kept fresh by outbox events.
- **Per-module config namespace** (`mysms.finance.*`), so a module's properties move with it unchanged when it is split out. No module ships a configuration file. A module is not declared anywhere (owner decision of 2026-11-10): core's `ModuleDefaultsEnvironmentPostProcessor` reads the modules of a process from the application class — a module is a package `com.mysms.<name>`, a module's own `@SpringBootApplication` scans its own package (its microservice), the monolith's lists every module's — and builds the configuration for them as placeholders over the environment variables of DEPLOYMENT.md §3, added as the lowest-precedence property source. A module's settings beyond its datasource are read where they are used, in its own `config` class, as `@Value("${mysms.<module>.<key>:${MYSMS_<VARIABLE>:<default>}}")` constructor or bean-method parameters (iam's `IamSecurityConfig`, attendance's `AttendanceClientsConfig`): the property wins, then the environment variable, then the default. No module jar contains an `application.yml`, so assembling them can never shadow one module's settings with another's.
- **Versioning:** the project version ends in `-SNAPSHOT` during development. A released (suffix-free) version is never built on again: the first commit after a release bumps to the next `-SNAPSHOT`. CI tags artifacts; the POM carries no build qualifier.
- Java toolchain, dependency versions and plugin versions are pinned in the parent only; child POMs never override a managed version silently.
- Lombok and MapStruct run as annotation processors; APT is enabled in CI and IDE setup docs. `lombok` then `mapstruct-processor` ordering in `annotationProcessorPaths` (MapStruct must see Lombok-generated getters).
- CI builds every module's exec jar and runs every module's own Testcontainers suite (own schema, own Flyway) on every commit, so "can this run alone on its own database?" is proven continuously.

**Toolchain (pinned in the parent POM; upgraded deliberately, never by a child):**

| Component | Version | Notes |
|---|---|---|
| Java | 17 (LTS) | `maven.compiler.release`; no language feature or API newer than 17 (`--release 17` enforces it) |
| Spring Boot | 4.1.2 | parent POM; manages Spring Framework 7.0, Spring Security 7.1, Jackson 3, Flyway 12, PostgreSQL JDBC 42.7, HikariCP 7, gRPC 1.84, protobuf 4.36, Testcontainers 2.0, JUnit 6, Lombok 1.18 |
| OpenAPI Generator | 7.26.0 | `sdk` (Maven plugin) and the TypeScript SDK (CLI), from the api/openapi snapshot |
| protobuf-maven-plugin (xolstice) + os-maven-plugin | 0.6.1 / 1.7.1 | `sdk`: protoc + protoc-gen-grpc-java (Boot-managed versions) compile api/proto into the gRPC stubs, one run for all modules |
| springdoc-openapi | 3.2.0 | every service: `/v3/api-docs.yaml` written from the contract interfaces (core `OpenApiConfiguration`); the component tests keep it as `target/openapi/<module>.yaml` |
| MapStruct | 1.6.3 | with lombok-mapstruct-binding 0.2.0 |
| Checkstyle / SpotBugs / JaCoCo | 12.3.1 (newest that runs on Java 17) / 4.10.4 / 0.8.15 | quality gates on `mvn verify` (§13) |
| PostgreSQL | 16 | Testcontainers image `postgres:16-alpine` |

---

## 3. Package layout inside a domain module

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
└── Other modules can never import this module — they are not on its Maven graph; they call its SDK client
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

**Standard operations.** The standard methods (`List`, `Get`, `Create`, `Update`, `Delete`) of every resource whose contract maps column for column onto its table follow one shape: one concrete entity, repository and mapper per resource, **one service per aggregate** and one controller per contract interface (owner decision of 2026-11-10: a record type that exists only as part of another — an approval of a homework, a line of a marksheet — has no service of its own; its methods live in the service of the aggregate it belongs to, the one its contract interface and controller already belong to, after the aggregate's own methods under a `// ---- <Record>: a record of the <Aggregate> aggregate ----` line, with its constants prefixed `<RECORD>_`, its private helpers suffixed `<Record>` and its repository and mapper injected as `<record>Repository` / `<record>Mapper`; a record type with a lifecycle and a contract interface of its own keeps its own service, and unrelated aggregates are never merged), each a plain class in exactly the shape this document prescribes (no base class, no type parameters, no reflection). They are owned source, written and reviewed like any other code; no generator writes them (decision of 2026-11-09), and repetition between them is removed by extracting shared helpers. Their rules come from the schema: uniqueness pre-checks mirror each unique constraint and each unique index on live rows (409; indexes with other conditions or expressions are left to the index itself, which also answers 409), in-module references are checked before writing (422), a delete is refused while live rows of the module still reference the row (409), NOT NULL, numeric precision/scale and the string-encoded 64-bit integers and times are enforced before the database sees the value (422, never a rounded amount or a 500), omitted values take the column default, and every read and write applies the institution data scope (§16). When a resource has invariants of its own, a `<Resource>Rules` component next to its service carries them and the service calls it at fixed points — `beforeWrite` (create and update, after the scope check and before uniqueness), `beforeUpdate`/`beforeDelete` (on the row as it stands) and `afterDelete` (same transaction): iam's access management uses this for the no-escalation rules (`AccessRules`), fixed system roles and signing a deleted account out everywhere. **State transitions** (documented in `api/state-machines.md`) follow the same shape: the service loads the row under the operation's own permission scope (404 if invisible), requires it writable (403), compares the body `etag` when the request has one (412) and `If-Match` when the operation declares it (428/412), checks the guard on the row as read (409 `INVALID_STATE_TRANSITION`), then runs one constant `UPDATE … SET <listed columns>, updated_at, updated_by, row_version + 1 WHERE id = :id AND row_version = :expected` (0 rows → 412, so a concurrent change can never slip past the guard) and writes `AuditWriter.recordTransition` (step, changed fields, reason) and the `<resource>.<step>` event in the same transaction. A step with further effects is written as its own workflow operation. **Secrets:** a contract field marked `writeOnly` whose table has `<field>_enc` is a secret: the service encrypts it with `FieldCipher` for the row's tenant and column before it is stored (create and update; an omitted value keeps the stored one), the mapper never returns it, a `<field>Masked` output comes from a `*_masked` column written at the same time or is computed from the decrypted value on read, secrets are never filterable or sortable, and the merged-model validation skips them (their plaintext was validated with the request). A service with secret columns refuses to start without a master key. Each resource's contract test runs from its entry in the module's `crud-spec.json` (§12).

**Events between modules.** A change writes its event to the module's own outbox in the same transaction (above); the `messaging` library's `OutboxRelay` publishes it to the bus and marks it published once the bus confirms. The bus is MySMS's own interface, `com.mysms.messaging.events.EventBus` (CloudEvents 1.0 envelopes, `EventEnvelope`); one `messaging` library contains the API, runtime and RabbitMQ (default), Google Cloud Pub/Sub and in-process adapters. Event-consuming modules compile against the narrow `com.mysms.messaging.events` API only; the POM and conformance check reject broker/provider and adapter imports from domain code. Choose an adapter with `mysms.events.bus`. RabbitMQ uses a broker user per service and checks the verified AMQP user on every event. Pub/Sub uses one topic per source module; IAM limits each producer to its own topic, and consumers validate the CloudEvent source and type against the topic and message attributes. `in-process` serves a single JVM. A module consumes events by declaring an `EventSubscriptions` bean (event type → handler); the `messaging` library's `InboxProcessor` runs each handler once per event inside the event's tenant transaction (the module's `inbox_event`), and a failing handler is redelivered after a growing delay and finally dead-lettered. A module publishes only its own event types (`<module>.…`) and its audit records — core's `OutboxWriter` refuses anything else, and consumers reject an event its source does not own (`EventEnvelope.ownedBySource`); a handler throws `RejectedEventException` for an event that must never be retried. Several `EventSubscriptions` beans of one module are merged; two handlers for one type stop the application. Handlers change only their own module's tables, audit like any write, and announce their changes on their own outbox. Examples: each module's reference cleanup of database/logical-references.md (`<Module>ReferenceCleanup`) and the platform's audit trail.

**No templatized service layer.** There is no generic `BusinessServiceApi<T, P, ID>`, no `BaseDatabaseBusinessService` skeleton, no listener-pipeline framework and no `MapperContext` indirection — that part of the source template was **deliberately rejected**. Each aggregate gets a plain, hand-written service class (`CourseService`) with explicitly named methods (`getCourse`, `listCourses`, `createCourse`, `updateCourse`, `patchCourse`, `deleteCourse`, plus its workflow operations). Methods take and return that aggregate's own DTOs — no type parameters, no inherited write pipeline, no reflective hooks. Shared behaviour (paging decode, tenant context, batch helpers, audit-row builder) is composed in as small injected helpers from `core`; the only thing a service inherits is its transaction handling (`AppTransactionManager`, below).

**Transactions are programmatic — never annotation-driven — and every service inherits them from `AppTransactionManager`** (owner decision of 2026-11-10). `@Transactional` and `TransactionTemplate` are both banned in MySMS code (Checkstyle regexp rule and conformance B1 enforce it). Every module declares its **`PlatformTransactionManager` as a qualified Spring bean** (`<Module>DatabaseConfig.TRANSACTIONS`); every class that works in a transaction extends core's `com.mysms.core.db.AppTransactionManager`, passes the module's `ModuleDatabase` and that bean to `super(database, transactions)`, and hands each unit of work to `doInReadTransaction` / `doInWriteTransaction` as a `CallBack` (`runIn…` for work that answers nothing; `…Global…` for global tables and reads before a tenant is known, which bind no tenant). The base class begins the module's transaction (`ModuleDatabase.readDefinition()` / `writeDefinition()`: READ COMMITTED, the module's timeout, read-only for reads), binds the caller's tenant (the transaction-local `app.tenant_id` row-level security reads), commits the work's value, and rolls back on any `RuntimeException` or `Error`, rethrowing it unchanged (core's `Transactions` holds the two endings). Work that calls another service of the same module joins the running transaction (one commit, at the end of the outermost work); the base class refuses a write started inside a read-only transaction and tenant work started inside a global one (`IllegalStateException`: both are programming errors). The `…Global…` methods only leave the tenant unbound — a write to a global table still runs inside `CallContextHolder.withoutTenant()` where its policy requires that. No service writes `getTransaction` / `commit` / `rollback` itself; only infrastructure with a transaction definition of its own (the idempotency store, the outbox relay and inbox) does. This is the one thing a service inherits: there is still no generic CRUD base service.

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

**Patch flow rule:** apply the partial to the loaded entity, then **re-validate the full DTO** produced from the merged entity before persisting. A patch can never produce a state that `create` would have rejected.

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

**5.4 Read-only projections are records in `repository/`** — report rows, join results, sign-in shapes — never entities, never in `domain/`, never DTOs. Card DTOs are contract schemas (`api/model`, and `com.mysms.sdk.<module>.model` on the caller's side).

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

- Entities never cross the API boundary: not in controller signatures, not serialized, not returned from controller or SDK surfaces. The compiler should make leaking an entity awkward; the reviewer makes it a blocker.

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

**7.1 The contract is owned source inside the module (owner decisions of 2026-11-09/10, CLAUDE.md).** Each module's API interfaces and transport DTOs live in `com.mysms.<module>.api` and `com.mysms.<module>.api.model` — one interface per resource carrying the Spring mappings, one mutable bean per schema carrying `@JsonInclude(NON_NULL)` and the Jakarta constraints. There is no separate contracts module and no OpenAPI→Java generation: the packages were materialized once from the retired generator on 2026-11-10 and are edited directly, in the generator's shape (the style gates skip them for that reason). The OpenAPI documents are written FROM this code: springdoc serves `/v3/api-docs.yaml` in every service (core's `OpenApiConfiguration` restores the contract operationIds, `x-permission`, security schemes and idempotency from the module's edge operation table), and each module's service component test writes it to `target/openapi/<module>.yaml` on every `mvn verify`.

Three owned artifacts describe the same operations, and the build fails when they drift: the `api` package (the code), `src/main/resources/META-INF/mysms/operations/<module>.json` (the edge table the auth filter enforces, checked against the contract snapshot by conformance), and `api/openapi/<module>.yaml` (the snapshot the TypeScript/Java SDKs and the conformance checks still read; it retires when the SDKs cut over to the springdoc output — HANDOFF). A contract change edits the interface/DTO, the operation table and the snapshot in one reviewed change.

```java
// ✗ an ad-hoc transport class next to the contract package
@Data public class OrganizationDTO extends AuditableDTO { @NotBlank @Size(max = 255) private String name; … }

// ✓ the controller and service use the module's contract package
import com.mysms.academics.api.model.Subject;        // resource
import com.mysms.academics.api.model.SubjectUpdate;  // its writable fields only
import com.mysms.core.paging.PagingInfo;              // every list: data + paging (NavigationInfo)
```

- Contract DTOs are mutable beans with `@JsonInclude(NON_NULL)` (the contract omits absent values) and Jakarta constraints (`@NotNull`, `@Size`, `@Min`/`@Max`, `@Pattern`). Because the contract interfaces mark bodies `@Valid`, shape errors are rejected before a controller runs (422 `VALIDATION_FAILED`, parameter errors 400).
- **Validation is still explicit in the write path:** the business service calls `DtoValidator.validate(dto)` on create, and again on the merged model after a partial update (§4 patch rule); business invariants (codes, cross-row rules, logical references) are named private methods that collect every failing field into one 422.
- **Output-only fields** (`id`, `createTime`, `updateTime`, `createdBy`, `updatedBy`, `etag`) appear in the resource DTO but are never read from a request: mappers ignore them on the way in (§8).

**7.1a One JSON mapper, built by hand.** Every Jackson use in the process — HTTP bodies, page cursors, events, the OpenAPI table — goes through the one `JsonMapper` core declares (`JsonMappers.create()`, taken from `common`'s `CommonConfig`): ISO-8601 dates in UTC, unknown properties ignored (additive contract changes), a missing primitive refused, a single value never accepted for a list, properties in declaration order. Boot's auto-configured mapper backs off for it; no module builds its own `JsonMapper`/`ObjectMapper` or customizes the shared one.

**7.2 Partial updates: `PATCH` + the `<Resource>Update` DTO + `updateMask`.** The Update DTO carries only the writable fields, as plain nullable properties — never `Optional<>` fields or other three-state wrappers. `core`'s `UpdateMask` decides which fields apply: with `updateMask`, exactly the listed fields (a listed field absent from the body is cleared; clearing a required field is a 422); without it, the non-null body fields. Unknown or non-writable names → 400.

```java
// ✗ three-state Optional fields decide what a patch changes
private Optional<@NotBlank @Size(max = 255) String> name;
private Optional<UUID> parentId;

// ✓
List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);   // WRITABLE = the SubjectUpdate fields
apply(entity, changes, fields);                                          // §8: one case per field
```

**7.3 One update method: PATCH.** A resource is never replaced as a whole with `PUT` next to its `PATCH`. `PUT` exists only where the contract defines an idempotent *set the value at this key* operation (today `iam.SetMySetting`; conformance D2 checks the contracts).

**7.4 Contract shapes only — no envelopes of our own.** Every list operation answers core's `PagingInfo<Resource>` (`data` + `paging`, the `NavigationInfo` with `page`, `size` and the `first` / `previous` / `next` links), built by `PagingUtil.pagingInfo(page)` from the service's `Page<Resource>` (owner decision of 2026-11-10, taken from `common`: the classes are core's, the paging under them is core's keyset/offset `PageRequest`); a write that returns the items it wrote answers `{items}`; a delete answers **204 with no body**; an error answers problem+json (§11). No other paging envelope, never a `{"success": true}` body.

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

**8.3 A partial update is applied by explicit code in the service, never by MapStruct.** No `@MappingTarget` update/patch methods, no `NullValuePropertyMappingStrategy` / `NullValueCheckStrategy`: the service's `apply(entity, changes, fields)` has one `case` per writable field. Stated reason: update-mask semantics (clear a listed field, skip an unlisted one) cannot be expressed by a null-value strategy, and an explicit switch is reviewable and reflection-free.

```java
// ✗
@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
void patch(OrganizationPartialDTO partial, @MappingTarget Organization target);

// ✓ business service
private static void apply(SubjectEntity entity, SubjectUpdate changes, List<String> fields) {
    for (String field : fields) {
        switch (field) {
            case "code" -> entity.setCode(changes.getCode());
            case "name" -> entity.setName(changes.getName());
            default -> throw new IllegalStateException("unhandled writable field " + field);
        }
    }
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

## 10. gRPC services

- The stubs and protobuf messages (`com.mysms.<module>.v1`) are compiled from `/api/proto` into `sdk` (one protoc run, §1.7), so every module already has its own on the classpath.
- One resolver per service implementing the generated stub, delegating to the **same** hand-written business services as REST — business rules exist once. The resolver converts proto ↔ DTO with the proto mappers at its edge.
- Validation path: proto → DTO (proto mapper) → the same Jakarta validator. No duplicate hand-written checks.
- One global gRPC exception interceptor maps the same typed exceptions to `io.grpc.Status` (NOT_FOUND, FAILED_PRECONDITION for 412, ALREADY_EXISTS for 409, INVALID_ARGUMENT for 400/422) so REST and gRPC fail identically.
- Field numbers are law: `proto/field-numbers.lock.json` is regenerated and committed with any proto change; renumbering an existing field is release-gated.

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
- **Component**: `@SpringBootTest` against **real PostgreSQL with the real Flyway migrations** — never H2, never a hand-built schema. `core`'s test kit `TestDatabase` gives each test class a fresh database with two fresh non-superuser, non-BYPASSRLS roles — the owner Flyway migrates as and the application role that only receives DML grants — so row-level security is enforced and cannot be switched off, exactly as in production on a Testcontainers PostgreSQL 16, or on an existing server named by `MYSMS_TEST_JDBC_URL` / `_USER` / `_PASSWORD` (CI without Docker). `auth-client`'s test kit `TestTokens` signs access tokens with a throwaway key. Every module ships `ModuleBoundaryTest` (unit: own-schema SQL, SDK allowlist) and `<Module>ServiceComponentTest` (the microservice boots alone on its own database, migrates only its schema, reports its database in health, and its edge refuses undeclared routes and missing tokens). Each repository has at least one component test proving tenant-scoping, soft-delete filtering and the `row_version` guard (0-row update → 412 path). Test properties files carry no production secrets.
- **Contract tests per resource**: each module's `<Module>CrudComponentTest` runs `CrudContract` (auth-client test kit) over every resource in its `crud-spec.json` (owned test data: one entry per resource, with sample bodies valid against the migrated schema) — create (201, ETag, Location, echoed values), validateOnly writes nothing, a duplicate is 409, get and the read mask, list, filter and paging, 404 for an unknown id, 403 without the permission, another tenant never sees the row, the institution scope (404 outside it, 403 for writes; tenant-level rows read by all and changed only tenant-wide), update under If-Match (428 without, 412 when stale, 400 for an unknown mask field), every generated state transition (403 without its permission, 404 outside the scope or 403 for a tenant-level row without a tenant-wide grant, 428/412 for a missing or stale If-Match where declared, 412 for a stale body etag, 200 from the guarded state with the listed values set and the version moved on by one, then 409 with nothing changed when the new state no longer allows the step) and soft delete. The sample payloads and their parent rows are generated from the schema and are first proven against a migrated database by the generator, so a failure is a real defect. `<Module>ControllerStubsTest` pins how many contract operations still answer 501.
- **Contract**: locks the REST surface for published resources against `/api/openapi` by driving the real service over HTTP (the component tests do this for every implemented operation); a contract-breaking controller change fails before integration.
- **Mocks only in unit tests.** Mockito, fakes and stubs are allowed in unit tests and nowhere else: component, contract and E2E tests run real code against real PostgreSQL, real Flyway migrations and real signed tokens (`TestTokens`). An outbound adapter to an external provider (mail, SMS, payment gateway, storage) is exercised in component tests against a real local implementation of that protocol (a Testcontainers container such as a local SMTP or S3-compatible server), never a mock bean.
- **E2E**: black-box against a deployed stack; always excluded by default; no sleep-based flakiness without a stated reason.
- A change to the canonical write path, a mapper ignore-list or a migration requires tests in the matching layer in the same PR. Minimum line coverage is enforced by JaCoCo in the parent POM (bar set there, not debated per PR).

---

## 13. Build quality gates

- `mvn verify` = compile + unit/component tests + **Checkstyle** + **SpotBugs** + JaCoCo check. All four bound in the parent POM; CI runs exactly `mvn verify`. A PR that needs `-DskipTests` or `-Dcheckstyle.skip` does not merge.
- Suppressions (`checkstyle-suppressions.xml`, `spotbugs-ignore.xml`) each carry a one-line reason; whole-package or wildcard suppressions require explicit review; agents never add suppressions for their own findings without approval (§1.9).
- Fix order: tests green first, then static analysis — don't iterate style on broken code.
- Generated code (MapStruct impls, protobuf, OpenAPI interfaces) is excluded from style/coverage; it is never hand-edited.
- The `conformance` module (`backend/conformance`, run by `mvn verify`) checks the rules a compiler, Checkstyle or SpotBugs cannot: R1 tenant predicate, R2 SQL as repository constants, R5 plain repository classes, B1 no `@Transactional`/`TransactionTemplate`/ORM/ArchUnit/Modulith, A1 module dependencies, §3 package roles, §5/N1 entities (base class, Lombok, no Jackson/mapping annotations, no `Map` columns, projections outside `domain/`, hooks only in services), §6 no hard DELETE on audited tables, C3 controllers implement contract interfaces, M1 mapper configuration and way-in ignores, D2 DTO shapes, E2 error format, X1/X2 no token handling in modules, test rules (mocks only in unit tests; every module has its boundary and service component tests) and §14 migration schema and no cross-schema foreign key. The build fails on any violation.

---

## 14. Migrations (Flyway) — authoring rules

The platform-wide migration principles of the source, applied to MySMS's existing Flyway layout (`database/migrations/<module>/V<release>_<module-order>_<seq>__description.sql`):

- One logical change per migration file; descriptive name; the owning module's folder. A migration touches only its own module's schema; a foreign key never crosses schemas — a reference into another module is an indexed uuid column registered in `database/logical-references.md` (regenerate with the schema docs).
- Applied migrations are immutable (§1.2). Post-go-live changes are backward-compatible with the previous running binary: add nullable/defaulted columns, add tables/indexes (`CONCURRENTLY` outside transactions where needed); renames and drops are expand-contract across two releases; `NOT NULL` lands as `NOT VALID` + `VALIDATE` after backfill.
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
| A1 | Module depends only on core/sdk/auth-client (messaging per §2); cross-module data fetched via `sdk` clients; SQL touches only the module's own schema |
| B1 | One plain service per aggregate, explicit methods; no generic CRUD base service, no `@Transactional` — each service extends `AppTransactionManager` and runs its work through `doInReadTransaction` / `doInWriteTransaction` on its module's `PlatformTransactionManager` bean |
| B2 | Audit + outbox rows written explicitly in the same transaction as the aggregate write |
| B3 | `row_version` guard present on update/patch/delete; 0 rows → 412 |
| R1 | Reads filter `tenant_id` and `deleted_at IS NULL`; partitioned queries carry the range filter |
| R2 | SQL as constants/resources, named params, `ANY(:ids)` child loads, sort allowlist |
| R3 | SQL references only the module's own schema — no cross-schema join, read, write or lock; another module's data comes through `sdk` |
| R4 | New cross-module reference columns are validated through the owner's SDK before the write, indexed, registered in `logical-references.md`, and handled in the owner's delete/archive event consumer |
| M1 | Every mapper uses the module's one `@MapperConfig` (`unmappedTargetPolicy=ERROR`); the way in ignores id, tenant and the whole audit set; no null-value strategies, no `@MappingTarget` patching, no logic or state in mappers (§8) |
| D1 | No entity beyond the business layer; DTOs match the contract; Update DTO contains only writable fields |
| D2 | Transport DTOs only in the api/model contract package (no ad-hoc `*DTO` classes); PATCH + `updateMask` (no `Optional` fields, no PUT replace); contract shapes only (`<Resource>List`, 204 delete, no envelopes); read mask via `ReadMask` (no `@JsonView`/`ResponseBodyAdvice`) (§7) |
| N1 | Entities: exact audit set (`OffsetDateTime`, nullable `deletedAt`, `rowVersion`, UUIDv7), exact Lombok, no Jackson/mapping annotations, no view data, no `Map` columns; projections are records in `repository/`; the table named through `TABLE`; hooks called by the service with actor and clock (§5) |
| R5 | Repositories are plain `@Repository` classes: no repository interfaces, derived queries, `@Query` or `*Impl` fragments; no SQL built at runtime from names or metadata; no database functions; keyset paging (§6) |
| L1 | After-commit code (cache refresh, notification triggers) writes no business rows and runs only after a successful commit |
| O1 | Idempotency keys, operations and document numbers use the module's own tables; long-running work returns 202 + `Operation` with `module` set |
| G1 | gRPC delegates to the same business service; REST/gRPC error parity |
| E1 | Typed exceptions + problem+json codes; no personal data in messages/logs |
| E2 | Errors only through `GlobalExceptionHandler` / `ProblemResponseWriter` (problem+json); `detail` never carries an exception's message; one typed exception per meaning (§11.3), no catch-all business exception |
| S1 | No secrets/PII in code, config, fixtures; `*_enc` handled write-only |
| T1–T5 | Tests in the right layer, Testcontainers+Flyway for components, contracts updated with surface changes, E2E still excluded by default (N/A when no test paths in diff) |

Pre-PR self-check = the same list plus: `mvn verify` green locally, no new suppressions without comments, migration files immutable.

---

## 16. Authentication and authorization

**Protocol.** OAuth 2.0 + OpenID Connect, built the OAuth 2.1 way (PKCE everywhere, no implicit/password grants, refresh-token rotation with reuse detection). The **iam module is the OIDC issuer** (Spring Authorization Server embedded — identity data stays in the `iam` schema); every module, iam included, is an OAuth2 **resource server** validating JWTs against the issuer's JWKS. Because trust is "an OIDC issuer", swapping or adding an external IdP later (Zitadel, Keycloak, Ory Hydra — e.g. for enterprise SSO, P1-39) is an issuer-URI/brokering change, never a service-code change.

**The `auth-client` module is the only auth surface a module ever touches.** It is a hand-written facade owned by the iam team, plugged into all 21 modules, and it hides every protocol and vendor detail behind MySMS's own wrapper types:

- `AuthContext` — the request's identity: user id, tenant id, institution ids, permission codes, token id, impersonation flag. Read-only; the only thing business code sees.
- `TokenVerifier` — validates the bearer token (local JWKS verification, no network call per request; key cache + rotation handling). Interface ours; the Spring Security/Nimbus machinery is an implementation detail inside auth-client.
- `PermissionChecker` — answers "may this principal perform `<x-permission code>`?" from token claims (plus the iam SDK for the rare dynamic check).
- Filters/interceptors — **all checks happen at the edge, never scattered in business code** (the servlet `AuthFilter` is built; the gRPC interceptor follows with the first gRPC service): one servlet filter chain (REST) and one gRPC interceptor (both auto-configured) that verify the token, resolve the tenant, enforce the route's `x-permission` code from the contract, populate `AuthContext`, and emit the access-log/audit hooks. A controller or service never parses a token, never reads a header, never checks a permission inline; it reads `AuthContext`.

**The issuer today.** iam signs in MySMS's own apps through the contract's first-party operations — `iam.Login`, `iam.RefreshToken`, `iam.Logout` — not through an OAuth password grant for third parties. Passwords are argon2id (19 MiB, 2 iterations, 1 lane); every refusal in a known tenant costs one verification and one write transaction whether or not the account exists; every failure gets the same 401 `INVALID_CREDENTIALS`; from the 5th consecutive failure password sign-in is locked for 1, 2, 4… up to 60 minutes (the lockout never ends existing sessions: refresh needs only an active account in an active tenant). Access tokens are RS256 JWTs (15 min) with `sub`, `tid`, `perms`, `ins` (`*` for a tenant-wide grant: every institution), `roles`, `pid` (linked person), `sid` (session), `jti`, `amr`, audience `mysms`; the key comes only from the environment and its public half is served at `/oauth2/jwks`, together with the retiring key during a rotation (`MYSMS_IAM_PREVIOUS_PUBLIC_KEY`). Refresh tokens are opaque (`<tenant>.<256 random bits>`), stored as SHA-256, rotated on every use, and a reused one revokes its whole session (a token revoked by sign-out is refused without that alarm). Sign-out also deactivates the session's push device (or all devices). A complete permission set is compacted in `perms` (`*`, `<module>.*`, `<module>.<resource>.*`) and `AuthContext.hasPermission` expands it, so broad roles fit in a header. The authorization-code + PKCE endpoints for partners and external IdP brokering (Spring Authorization Server) are the next step and change nothing for modules.

**How the edge decides (servlet filter, in order).** (1) Infrastructure paths (`/actuator/health/**`, `/actuator/info`, `/oauth2/jwks`; plus `/v3/api-docs*` and `/swagger-ui*` only when the operator sets `mysms.auth.api-docs` — `MYSMS_API_DOCS`, off by default: the document maps every operation and permission) pass. (2) The request must match an operation of the contract — the operation table is each module's own `META-INF/mysms/operations/<module>.json` (owned data, pinned to the contract by the conformance build), and the most specific path template wins — otherwise 404. (3) A bearer token, when present, is verified locally (signature against the issuer's JWKS, issuer, expiry with 60 s skew, audience `mysms` or `mysms-platform-admin`); invalid → 401 (`TOKEN_EXPIRED` / `UNAUTHENTICATED`, with `WWW-Authenticate`). (4) Operations whose contract allows calling without a token (`security: []` or `{}`) proceed, with the identity bound if a valid token came along. (5) Otherwise the token kind must match the operation's scheme: tenant users (`bearerAuth`, token has `tid`) or platform staff (`platformAdminAuth`, audience `mysms-platform-admin` and `amr` containing `mfa`) → else 403. (6) `X-Institution-Id`, when sent, must be one of the token's `ins` → else 403 (400 if not a UUID). (7) The operation's `x-permission` must be in the token's `perms` → else 403 `PERMISSION_DENIED`. Only then are `AuthContext` and the core call context (tenant, user) bound for the request and cleared afterwards. The matched operation is marked on the request, so an operation of the contract that this release does not serve yet is answered **501 `NOT_IMPLEMENTED`**, never 404. Claims used: `sub`, `tid`, `perms`, `ins`, `jti`, `amr`, `act`, `pid`, `sid`.

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

**16.2 The edge enforces every operation's permission, in the service.** Each contract operation's `x-permission` is checked by the `AuthFilter` of the process that serves it, before the controller runs (steps 5–7 above). Permissions are never left to an external gateway, an access-control service fed by published permission data, or a downstream check: a request that reaches the service without passing through that check — another route, an internal call, a misconfigured gateway — is still refused.

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

- **Nested Maven modules and per-module `-contract`/`-client`/`-service`/`-boot` artifacts** — per project decision the layout is flat: one module per schema owning its own contract package, one generated `sdk`, exec-classifier jars instead of `-boot` launchers.
- **Per-operation springdoc annotation blocks and ad-hoc `*DTO` classes** (`@Operation`/`@ApiResponse` walls on every method; `*DTO` classes with `Optional` fields for patches; custom paging and success envelopes; `@JsonView` / response-body advice for slim responses) — MySMS's contract interfaces carry the Spring mappings and validation, core's `OpenApiConfiguration` restores ids, permissions and security document-wide, patches use `updateMask`, and responses are the contract's shapes and the read mask (§7).
- **Spring Data repository interfaces and framework lifecycle callbacks** (`CrudRepository`, derived queries, `@Query`, `*Impl` fragments, `BeforeSaveCallback` stamping audit columns) — repositories are plain classes and the service calls the hooks (§5.8, §6).
- **A generic `BaseController` / `BaseEntityMapper` and one catch-all `BusinessException`** — controllers implement the contract interfaces, each module has one mapper configuration, and failures are typed (§8, §9, §11.3).
- **Token parsing in each service and permission enforcement outside it** (a per-service JWT filter; permissions published to an external access-control service) — one auth facade, and the edge of the serving process checks every operation's permission (§16.1, §16.2).
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
| §3 | project-conventions.md, layer-reference.md | package tree by layer role | `example/` vertical dropped; `api/` holds the module's own contract (owned source, §7.1); cross-module access still only via the `sdk` module |
| §4 | canonical-template-reference.md §1–§4, §12–§13, §23–§24, §30; layer-reference.md | the write-path *obligations*: one-tx writes, lifecycle hooks, audit+outbox in-tx, 412 on stale version | generic base service, listener pipeline and `MapperContext` rejected — plain per-aggregate services; `@Transactional` and `TransactionTemplate` replaced by programmatic `PlatformTransactionManager`; re-based from Spring Data JDBC onto JdbcClient |
| §5 | data-model.md, canonical §9, §22 | BaseEntity + hooks, no plain POJOs, records for projections | TaggedEntity default narrowed: MySMS tags via platform.tag_assignment where the schema says so |
| §6 | ADR bi-java-0001, canonical §10, §16, §24, §26, §32 | explicit SQL, batch template, sort allowlists, no N+1 | no repository derivation at all; tenant predicate + RLS added |
| §7 | rest-api.md DTO + Partial DTO sections | DTO/entity separation; partial-update DTOs carry only writable fields | DTOs are owned source in the module's `api/model` package (materialized once from the retired generator) instead of hand-written Lombok classes; three-way patch semantics come from `updateMask` (core `UpdateMask`) instead of `Optional` fields |
| §8 | canonical §6, §8; design-review M1 | id/audit ignores on the way in, AfterMapping discipline | per-module `@MapperConfig` + per-aggregate mappers instead of the generic BaseEntityMapper (the contract DTOs share no base type); unmappedTargetPolicy=ERROR made mandatory; partial updates applied by an explicit per-field switch (update-mask semantics); MapperContext transport binding dropped with the service template |
| §9 | canonical §7, §20; ADR bi-0007 | thin controllers, explicit validator call, single exception handler, `PagingInfo`/`NavigationInfo`/`PagingUtil` list responses | status map replaced by MySMS api/README (ETag/412, Idempotency-Key); the paging under `PagingInfo` is keyset-by-cursor with a capped offset for page jumps instead of plain offset |
| §10 | canonical §14, §27; ADR bi-0008 | shared business service, proto-as-T, status parity | field-numbers lock is MySMS's own mechanism |
| §11 | rest-api.md validation mapping; ADR bi-0007 §3 | typed exceptions, code registry discipline, no-PII details | codes from MySMS problem+json registry |
| §12 | ADR bi-java-0002, testing-philosophy.md | four layers, roots, naming, triggers, Testcontainers, no prod secrets | H2 explicitly banned; Flyway-in-tests required |
| §13 | project-conventions.md static analysis | checkstyle+spotbugs+jacoco on verify, suppression policy | — |
| §14 | ADR bi-0006, liquibase-migrations.md | immutability, one-change-per-file, expand-contract, comments | Flyway + MySMS naming/audit set |
| §15 | design-compliance-review.md | aspect checklist | IDs remapped to MySMS layers |
