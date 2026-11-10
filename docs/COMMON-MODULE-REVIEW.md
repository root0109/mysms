# The owner's `common` module and MySMS — what was taken, what was not, and why

Written 2026-11-10 after reading `C:\MyWork\Workspace\backend\common` (`com.zaprit:common`, 59 classes, about 3,000
lines of main code). The owner asked for it to be incorporated into MySMS "unless there is a better way". This page
records the comparison class by class, so the decision can be checked and changed.

The short version: ideas of `common` that were missing in MySMS are now in `core` — **transactions inherited by
every service**, **slow-statement reporting**, the **message catalogue**, and (owner decisions of 2026-11-10)
**read/write routing to a replica and the primary**, the **hand-built JSON mapper** and the **`PagingInfo` /
`NavigationInfo` / `PagingUtil` list responses**. The rest already exists in MySMS in a form that fits the rules the
owner set for this project (CLAUDE.md, JAVA-CODING-RULES), or contradicts one of those rules; copying it in would have
put two mechanisms side by side for the same job. Nothing was copied verbatim: `common` targets Spring Boot 3.4 and
Jackson 2 and depends on a `util` module that is not part of MySMS, and several of its classes have defects listed
below that should not travel.

## 1. Taken

| `common` | In MySMS now | What changed on the way |
|---|---|---|
| `db.AppTransactionManager`, `callback.CallBack` | `com.mysms.core.db.AppTransactionManager`, `CallBack` — every class that works in a transaction (363 of them) extends it and calls `doInReadTransaction` / `doInWriteTransaction`; 1,712 hand-written transaction blocks and about 12,000 lines are gone | See §3: the original applies read-only, isolation and propagation *after* the transaction has begun, so they have no effect; it rolls back with a null status when the begin fails; it casts every exception to a type parameter unchecked; an `Error` is not rolled back. The MySMS class uses the module's own transaction definitions (read-only for reads, READ COMMITTED, the module's timeout), binds the caller's tenant for row-level security, rolls back on any `RuntimeException` or `Error` and rethrows it unchanged. `ConnectionId` is not needed: a module has one database, chosen by the class's own `ModuleDatabase`. `run…` variants serve work that answers nothing, `…Global…` variants global tables. Work that calls other work joins its transaction; a write inside a read-only transaction and tenant work inside a global one are refused. |
| `db.CustomJdbcTemplate`, `db.QueryStats` (slow-query log) | `com.mysms.core.db.SlowStatementJdbcTemplate`, wired into every module's `JdbcClient`; limit `MYSMS_DB_SLOW_STATEMENT` (default 200 ms, per module `MYSMS_<MODULE>_DB_SLOW_STATEMENT`, `0` = off) | The original overrides some forty `JdbcTemplate` methods and keeps a map of every distinct SQL string for the life of the process (unbounded for generated SQL). The MySMS class overrides the three template methods the list, single-row and update statements of a `JdbcClient` pass through (streamed results and batches are not timed) and keeps nothing; it logs the statement text with placeholders, never a bound value. |
| `db.RoutingDataSource` (owner request of 2026-11-10: "we may read from slave db and write to master db") | `com.mysms.core.db.RoutingDataSource`, `DatabaseRole`, `DatabaseRoleHolder`: every module's DataSource routes by the transaction — `doInReadTransaction` reads on the module's replica pool, `doInWriteTransaction` writes on the primary; replica settings `MYSMS_DB_REPLICA_URL` / `_USER` / `_PASSWORD` (per module `MYSMS_<MODULE>_DB_REPLICA_*`), none → both roles use the primary; health reports the replica | The original routes by a connection id per tenant database, kept in a holder that a nested call clears for its caller. The MySMS routing key is the transaction's read-only flag, set by `AppTransactionManager` only around `getTransaction` (restored afterwards), so nesting cannot lose it: a read nested in a write joins the write's primary connection, a write nested in a read is refused. Replica connections are opened read-only, migrations run on the primary only, and the routing map is built once at start (no lenient fallback). Replica lag is bounded by the ETag check of every update (a stale read cannot overwrite), and security reads (sign-in) are made on the primary. An unavailable replica is skipped (reads fall back to the primary) rather than taking the module down. |
| `config.CommonConfig` (the hand-built primary `ObjectMapper`) | `com.mysms.core.json.JsonMappers.create()`, declared by core as the one primary `JsonMapper` (Boot's backs off) and used for page cursors, events and the operation table too | Built on Jackson 3 (`tools.jackson`), feature by feature with the reason in its javadoc: ISO-8601 in UTC, unknown properties ignored, missing primitives and single values for lists refused, declaration order kept. The message bundles of `CommonConfig` are core's `MessageUtil` (§5). |
| `model.PagingInfo`, `NavigationInfo`, `util.PagingUtil` (owner request of 2026-11-10) | `com.mysms.core.paging.PagingInfo`, `NavigationInfo`, `PagingUtil` — the response of every list operation (392 REST lists and 2 composite views; the gRPC lists answer `mysms.common.v1.Pagination`); requests take `page`, `size`, `sort` (repeated `field,asc|desc`), `cursor` | `PagingInfo` and `NavigationInfo` are records (`data`, `paging` with `page`, `size`, `first`, `previous`, `next`); `PagingUtil` builds relative links from the current request. Underneath, the original's `LIMIT … OFFSET` became core's `PageRequest`: the `next` link carries a cursor (the last row's sort keys) and is read by keyset — no rows are skipped however deep the page, and rows added in front do not shift it; a page number without a cursor uses OFFSET, capped at 10,000 rows. The look-ahead row replaces the count (`totalSize` is gone). |

## 2. Not taken, because MySMS already has it in a form the project rules require

| `common` | MySMS equivalent | Why the MySMS form stays |
|---|---|---|
| `context.SecurityContext`, `SecurityContextHolder` (mutable thread-local with setters, created empty on first read) | `auth-client`'s `AuthContext` and `DataScope`, core's `CallContextHolder` | The MySMS context is immutable, built once from a verified token by the edge filter, and scoped to the call. A holder that hands out an empty, writable context on first read lets any code set an identity, and thread-locals without scoping leak across pooled or virtual threads. JAVA-CODING-RULES §16 allows a module to read the context and nothing else. |
| `error.ExceptionTranslator`, `ErrorMessage`, `ErrorDetail` | core's `GlobalExceptionHandler`, RFC 9457 `application/problem+json` with `code`, `traceId`, `errors[]` | The API contract (api/README) fixes the problem format; both clients parse it. The original answers 500 for an unreadable body and logs every exception message. |
| `error.exception.BusinessException(code, args)` (status defaults to 400) | One typed exception per meaning (`ResourceNotFoundException` 404, `InvalidStateException` 409, `PreconditionFailedException` 412, `ValidationFailedException` 422 …) | JAVA-CODING-RULES §11.3 bans the catch-all on purpose: with one general exception every rule answers 400 unless someone remembers to say otherwise. |
| `config.handler.CustomResponseBodyHandler`, `BodyWrapper`, `ListBodyWrapper`, `model.ResponseDTO` | Contract shapes only: the resource, `<Resource>List`, 204 for delete | Rule D2: no envelopes and no `ResponseBodyAdvice`; a header that changes the response shape makes two APIs out of one. |
| `domain.BaseEntity`, `AuditableVersionEntity`, `ScopeEntity`, `StateScopeEntity`, `UserScopeEntity` | core's `BaseEntity` (UUIDv7 id, audit stamps, `rowVersion`, lifecycle hooks) | The originals are `Externalizable` through the absent `util` module and use random UUIDs (v4), which fragment the primary-key index; MySMS ids are time-ordered UUIDv7. |
| `db.query.EntityRowMapperFactory`, the four Spring Data JDBC converters, `domain.JsonEntity` | An explicit `RowMapper` per repository | Spring Data's mapping context is an object-relational mapper; the owner's rule is no ORM, and conformance fails the build on a Spring Data dependency. |
| `validator.AppValidator`, `CustomSpringValidatorAdapter`, `config.SpringValidatorConfig` | core's `DtoValidator` (Jakarta validation → 422 with field errors) | Same job; the MySMS one produces the contract's `errors[]`. |
| `converter.StringToUUIDConverter`, `StringToEnumConverterFactory`, `LocalDateTimeConverter` | Typed parameters on the owned API interfaces; ISO-8601 with offsets | Spring converts UUID and enum parameters itself; MySMS uses `OffsetDateTime`/`LocalDate`, never a zone-less `LocalDateTime`, so there is nothing to convert. |

## 3. Not taken, because it should not be used as it is

| `common` | What is wrong | What MySMS does instead |
|---|---|---|
| `db.AppTransactionManager` (as written) | (1) `setReadOnly`, `setIsolationLevel` and `setPropagationBehavior` are called after `getTransaction`, so every transaction is a default read-write one. (2) If `getTransaction` throws, `rollback(null)` throws a second exception that hides the first. (3) `throw (T) e` is an unchecked cast: the declared exception type is not what is thrown. (4) `catch (Exception)` lets an `Error` leave the transaction open until the connection is returned. (5) A failing `commit` is followed by a `rollback` of a completed transaction. | The class in core, §1. |
| `db.ConnectionId`, `ConnectionIdHolder` | `equals` compares the id with `==` (string identity) and is not matched by `hashCode`; the holder is cleared in a `finally` even when the call is nested inside another, so the outer transaction loses its connection id. | One database per module; no routing key. |
| `db.DataSourceManager`, `DataSourceWatcher` (the routing itself is taken, §1) | Data sources are created at run time from property files dropped into a watched directory, with the database password in the file; the routing map is a plain map changed while requests read it; a failure to add a data source is logged and swallowed. | The primary and replica pools are configured from the environment at start (`MYSMS_<MODULE>_DB_*`, `…_DB_REPLICA_*`, secrets from the deployment's secret store), and tenants are separated by row-level security in one schema per module, not by a database per tenant. A replica is added by configuration and a restart (rolling, per module). If a tenant-per-database tier is ever wanted, it is an architecture decision (migrations, pools, backups per tenant), not a library class. |
| `context.AppContext` (static `getBean(name)`) | A service locator: dependencies become invisible and untestable, and a bean of the same name in two contexts resolves by registration order. | Constructor injection everywhere. |
| `config.MessageUtil`, `error.ConstraintMapper`, `ConstraintMessageProvider`, `data.CommonMessages` | The idea is right — error sentences from a message catalogue by code — but the implementation is tied to `ErrorMessage` and to Jackson 2. | Open as ENHANCEMENTS E52 (problem details in the reader's language): decide whether the server localises or the clients do, then build it once on the RFC 9457 format. |

## 4. Consolidating services — done 2026-11-10

The owner agreed: **one service per aggregate.** The 139 record types that exist only as part of another (an approval
of a homework, a line of a marksheet, a property of a notification type) no longer have a service class of their own;
their methods live in the 67 services of the aggregates they belong to — the grouping the contract interfaces and
controllers already had. Three aggregates whose root has no standard operations of its own (finance `Invoice`, lms
`Lesson` and `Settings`) got an aggregate service for their records. Record types with their own lifecycle and
contract interface keep their own service; unrelated aggregates were not merged, because a "module service" of
several thousand lines that every change touches is worse to maintain, not better. The rule is in JAVA-CODING-RULES §4
"Standard operations"; the component tests drive every operation over HTTP and do not care which class serves it.

The repeated list / get / create / update / delete code inside a service is still the larger cost. The next step is
the shared helpers HANDOFF §5.2 names (scope check, ETag check, paging), still without a generic base service.

## 5. Second pass over `common` (2026-11-10, after the owner asked for every package)

| `common` package | State in MySMS |
|---|---|
| `callback`, `db.AppTransactionManager` | In core, inherited by every service (§1). |
| `db.CustomJdbcTemplate`, `QueryStats` | In core as `SlowStatementJdbcTemplate` (§1). |
| `error` (`ConstraintMapper`, `ConstraintMessageProvider`, `ConstraintConstant`), `config.MessageUtil`, `data.CommonMessages` | **Now in core**: `com.mysms.core.error.MessageUtil` (the catalogue `messages/mysms*.properties`, resolved in the language of the call) and `ConstraintMapper` (a field error gets its sentence by code). Every field error of a typed exception or a validated body, in every module, passes through it in `GlobalExceptionHandler` (parameter errors Spring raises as 400 keep the validator's sentence). The base bundle is English and holds the codes whose sentence needs no parameter; a language is added as a bundle. What changed on the way: no Jackson-2 JSON provider, no static mutable constants, and it sits on the RFC 9457 problem format instead of `ErrorMessage`. |
| `model.PagingInfo`, `NavigationInfo`, `util.PagingUtil` | **Taken on the owner's decision of 2026-11-10** (§1): every list operation, REST and gRPC, both clients and the SDKs migrated in one change. |
| `error.ExceptionTranslator`, `ErrorMessage`, `ErrorDetail`, `BusinessException`; `config.handler` and `model` (`BodyWrapper`, `ResponseDTO`) | **Not taken — each replaces the published API** (§2): the error body and the response envelope are what the 2,049 operations, both clients and the OpenAPI and proto contracts are written against. Adopting them is a contract migration of every operation and both clients, not an incorporation; it needs the owner's explicit decision per item. |
| `db.query` (Spring Data JDBC row mapping), `domain` (`Externalizable` entities) | **Not taken — contradicts "no ORM"**, a constraint CLAUDE.md lists under "never change"; Checkstyle and the conformance build fail on a Spring Data import. |
| `db.RoutingDataSource` | **Taken** (§1): read transactions on the replica, write transactions on the primary. |
| `context` (`SecurityContextHolder`, `AppContext`), `DataSourceManager`, `DataSourceWatcher` | **Not taken — weaker on data security** (§2, §3): an identity any code can set, and database passwords read from files in a watched directory. CLAUDE.md puts data security first. |
| `config.CommonConfig` | **Taken** (§1) as core's hand-built `JsonMapper`. |
| `validator`, `converter`, `util` | Same job already done by core (`DtoValidator`) or by Spring itself (§2). |
