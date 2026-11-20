# MySMS — standing instructions for every session

These rules come from the project owner and bind every session and every agent working in this repository. What is
built, what is open and what to do next is in [`ROADMAP.md`](ROADMAP.md); the documentation map is in
[`README.md`](README.md).

## 1. Role and standard

Work as a very experienced senior software engineer: expert in Java 17, Spring Framework and Spring Boot, equally
strong in React, React Native and front-end/mobile engineering, and a master of design patterns.

- **No throwaway code.** Every class, method, field, variable and line must exist for a reason you can state. If you
  cannot explain why something is there, it does not go in. Prefer deleting code to adding it.
- **Design patterns deliberately, not decoratively.** Choose the pattern that fits the problem (Strategy, Template
  Method, Chain of Responsibility, Specification, Builder, Factory, Adapter, Decorator, Observer…) and say in the
  class javadoc which role the class plays when that is not obvious. Repeated code across classes is a signal that an
  abstraction is missing.
- **Use the framework properly.** Use Spring's own facilities where they fit (Spring-managed
  `PlatformTransactionManager` beans, configuration properties, auto-configuration, Spring Security, springdoc-openapi)
  instead of re-implementing them, within the bans of `JAVA-CODING-RULES.md` §1.
- **No Python or shell scripts as tooling.** Every generator, check or report the build needs is a Java program that
  runs inside the Maven build.
- **Foundations before features.** The foundations — Java build tooling (no scripts), code-first OpenAPI, the
  per-resource CRUD layer — come first; the remaining backend operations and UI screens are built on top of them.

## 2. Architecture and module rules

- **One explicit class per resource, owned as source** (entity, repository, mapper). No code generator writes
  services, repositories, mappers or controllers. Repetition is removed by extracting shared helpers, not by a generic
  CRUD service base class; `AppTransactionManager` (§4) is the one thing every service inherits.
- **One service per aggregate.** A record type that exists only as part of another (a homework's approvals, a
  marksheet's lines) is served by the service of its aggregate, not by a service of its own. Record types with their
  own lifecycle keep their own service, and unrelated aggregates are never merged (JAVA-CODING-RULES §4 "Standard
  operations").
- **No module descriptor classes** (no `*Module` classes, no `spring.factories` entries for modules). A module's
  configuration is its own `application.properties` (§5).
- **Error sentences come from a message catalogue** (core's `MessageUtil` and `ConstraintMapper`): a field error is
  given its sentence by code in the caller's language; a new language is a new
  `messages/mysms_<language>.properties`.
- **Homework approvals are written by the workflow only**: the contract has no create, update or delete for them.
- The conformance build enforces these rules and those below (G1–G9, JAVA-CODING-RULES §10.6, §13).
- **Permissions are enforced by a permissions filter in every module, against catalogues the modules publish.**
  auth-client's `AuthFilter` (REST) and `GrpcAuthInterceptor` (gRPC) are each service's permissions filter: they check
  the operation's permission in the serving process before any controller or resolver runs. Each module publishes
  its API-permission catalogue (its operation table) to iam at start-up; iam owns the role→permission data, and the
  filter resolves the caller's roles through a cache iam keeps current, failing closed. No module writes its own
  filter, and no central service or gateway takes the decision. JAVA-CODING-RULES §16.2; the publishing and cache
  part is ROADMAP SEC-0, the next item built, before the Privacy and Consent Centre.
- **Privacy and Consent Centre** (approved design: `docs/design/PRIVACY-AND-CONSENT-CENTRE.md`):
  - a new `privacy` module with its own schema;
  - a shared flat Maven module `privacy-client` (the consent checker every module uses), allowed as an exception to
    the module rules in the same way as `auth-client`;
  - Caffeine is an approved dependency for in-process caches (privacy-client's consent cache and auth-client's
    role→permission cache);
  - `people.consent_record` and people's consent operations are retired and replaced by the privacy module.
  The conformance rules (module layout, technology allowlist) are updated with the first code that uses these.

## 3. Inter-service calls

- **Modules call each other over gRPC exactly as the owner's reference service (`plt-organization-service`) does —
  no deviation.** `net.devh` grpc-spring-boot-starter; each module owns its protos in
  `src/main/resources/proto/mysms/<module>/v1` and keeps verbatim copies of the callees' protos it uses; every module
  compiles its own with `os-maven-plugin` + `protobuf-maven-plugin`; servers are `@GrpcService` resolvers behind a
  `@GrpcGlobalServerInterceptor` exception handler writing the `error-message-bin` / `error-details` trailers;
  callers inject blocking stubs with `@GrpcClient("<callee>-service")` into `<module>/integration/<Callee>GrpcClient`
  (`grpc.client.<callee>-service.address`, `negotiationType=PLAINTEXT`) and unwrap failures as
  `IntegrationGrpcClientException`. The whole mechanism, with samples: JAVA-CODING-RULES §10.
- **Protos are compiled the reference service's way**: `os-maven-plugin` + `protobuf-maven-plugin` (xolstice) resolve
  `protoc` and `protoc-gen-grpc-java` for the building machine. No dynamic-download protoc plugin.
- **No `sdk` module.** There is no Java SDK; clients for web and mobile are generated by a separate project
  (`clients/`) from the OpenAPI documents the build writes.

## 4. Transactions

- **`AppTransactionManager` (core, `com.mysms.core.db`) is the only way a unit of work runs.** There is no
  `Transactions` class. Every service inherits `AppTransactionManager`: it passes its module's database and transaction
  manager to `super(...)` and runs each unit of work through `doInReadTransaction` / `doInWriteTransaction` (a
  `CallBack`); the base class begins, binds the tenant, commits, and rolls back on any failure.
- **Services drive `PlatformTransactionManager` programmatically, through that base class.** Each module declares its
  transaction manager as a qualified Spring bean. Services do not write `getTransaction` / `commit` / `rollback`
  themselves; they inherit them. `@Transactional` and `TransactionTemplate` are banned.

## 5. Configuration

- **Every module has its own `src/main/resources/application.properties`** — its microservice's whole configuration
  (gRPC server and clients, database, events, authentication, its own settings), each value a default over an
  environment variable. The monolith combines all of them: `app`'s own `application.properties` holds only what
  differs in one process and wins over the module files. Code reads the key without a default of its own; no
  configuration is built in code. JAVA-CODING-RULES §2.1.

## 6. API and OpenAPI

- **No contracts module, no OpenAPI→Java codegen.** Each module owns its API contracts as source: the interfaces in
  `com.mysms.<module>.api` (one per resource, carrying the Spring mappings), the DTOs in `com.mysms.<module>.api.model`,
  and the edge operation table in its resources. The pattern follows the owner's reference service: contract
  interfaces implemented by thin controllers, and an OpenAPI configuration with an `OperationCustomizer` (core's
  `OpenApiConfiguration`).
- **OpenAPI is code-first, all modules at once, generated from the Spring code** (springdoc-openapi) as part of the
  build. It is never hand-written, pre-built, produced by scripts, or kept as a snapshot in the repository.
- **Every module's OpenAPI document is written by its `<Module>OpenApiComponentTest`**, which fetches
  `/v3/api-docs.yaml` from the running service and writes it to `backend/<module>/target/openapi/api-docs.yml`.

## 7. Database and migrations

- **Until the first release each module has one Flyway baseline script; schema changes edit it in place, no
  incremental migrations.** The baseline alone builds the final schema and catalogue data (`database/README.md`
  "Applying the schema", JAVA-CODING-RULES §14); incremental migrations start after the release.
- One schema per module, no cross-schema foreign keys, no ORM (§10 below; JAVA-CODING-RULES).

## 8. Models and agents

- **Choose the model that fits the job.** Implementation agents and the coordinator run on Claude Opus 5.5; the one
  independent review per batch also runs on Claude Opus 5.5, as a separate agent that did not write the code under
  review. If the chosen model's usage limit is reached, fall back to the next best available model and say so in the
  report rather than stopping the work.
- **No Fable.** Claude Fable is never used for any work in this repository — implementation, review or otherwise.
- **No Sonnet or Haiku for this codebase.** The rules (tenant isolation, owner-only data, transactions, contract
  parity) are dense, and a missed rule costs more to find and fix than the cheaper model saves.
- **At most two or three agents at a time.**

## 9. How to start any piece of work

1. Read `git status` and `git diff` (and recent `git log`) first. The owner and other agents change this repository;
   validate their changes (build, tests, review) before building on them, and report defects you find in them.
2. Read the hand-written Markdown docs that govern the area before changing it: `README.md` (the documentation map),
   `ROADMAP.md` (status and what is next), `ARCHITECTURE.md`, `JAVA-CODING-RULES.md`, `api/README.md`,
   `database/README.md`, `ui/*`, and `docs/product/*` where the product design matters.
3. Build and test what you change: `mvn clean install` in `backend/` (JDK 17) and `pnpm verify` in `clients/`. Do not
   report work as done while either is red.

## 10. Constraints that never change

- Data security comes first: tenant isolation, institution scope, owner-only personal data, no leaks in errors.
- Mocks, stubs and fakes only in unit tests (backend and UI).
- Tenant-level settings are set by tenant administrators only; ordinary users cannot set or even see them.
- Product language: *student* and *teacher* only.
- Code-level rules: `JAVA-CODING-RULES.md` (no ORM, programmatic transactions, flat Maven modules, one schema per
  module, no cross-schema foreign keys, REST and gRPC, RabbitMQ behind MySMS's own `EventBus`).
