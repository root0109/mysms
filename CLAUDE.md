# MySMS — standing instructions for every session

Owner's rules; they bind every session and agent. Status and open work: [`ROADMAP.md`](ROADMAP.md). Documentation map:
[`README.md`](README.md). Code-level rules: [`JAVA-CODING-RULES.md`](JAVA-CODING-RULES.md).

**`JAVA-CODING-RULES.md` is owner-controlled: never shorten, summarise or rewrite it, and never drop a rule or a sample. Change it only when the owner asks; then only add, consolidate or reorganise (more samples welcome), keeping every rule and every cited § number.**

## 1. Role and standard

Work as a very experienced senior engineer: expert in Java 17, Spring Framework and Spring Boot, equally strong in
React, React Native and front-end/mobile engineering, a master of design patterns.

- **No throwaway code.** Every class, method, field and line exists for a reason you can state; otherwise it does not
  go in. Prefer deleting code to adding it.
- **Design patterns deliberately.** Pick the pattern that fits (Strategy, Template Method, Chain of Responsibility,
  Specification, Builder, Factory, Adapter, Decorator, Observer …); name the class's role in its javadoc when not
  obvious. Repeated code means a missing abstraction.
- **Use the framework properly** (Spring-managed `PlatformTransactionManager` beans, configuration properties,
  auto-configuration, Spring Security, springdoc-openapi) within the bans of JAVA-CODING-RULES §1.
- **No Python or shell scripts as tooling.** Every generator, check or report is Java running inside the Maven build.
- **Foundations before features**: Java build tooling, code-first OpenAPI and the per-resource CRUD layer first;
  operations and screens build on them.

## 2. Architecture and modules

- **Ten domain modules of 22 areas** (ARC-7, JAVA-CODING-RULES §2.2): an *area* is one schema with its contract
  (protos, operation table, `/v1/<area>`, `mysms.<area>.*`); a module serves one or more areas. Kernel: `platform`,
  `identity`, `privacy`, `comms`; products: `hrms`; school `students`, `academics`, `finance`, `campus`; `learning`.
  A product depends only on the kernel, except school → hrms by id and events; the kernel knows no product.

- **One explicit class per resource, owned as source** (entity, repository, mapper). No code generator writes
  services, repositories, mappers or controllers. Remove repetition with shared helpers, never a generic CRUD base
  service; `AppTransactionManager` is the only thing every service inherits.
- **One service per aggregate.** A record type that exists only as part of another (a homework's approvals, a
  marksheet's lines) is served by its aggregate's service; record types with their own lifecycle keep their own;
  unrelated aggregates are never merged (JAVA-CODING-RULES §4).
- **No module descriptor classes** (no `*Module` classes, no `spring.factories` entries for modules).
- **Error sentences come from the message catalogue** (core's `MessageUtil`, `ConstraintMapper`) in the caller's
  language; a new language is a new `messages/mysms_<language>.properties`.
- **Homework approvals are written by the workflow only**; the contract has no create, update or delete for them.
- **Permissions are enforced in every serving process**: auth-client's `AuthFilter` (REST) and `GrpcAuthInterceptor`
  (gRPC) check the operation's permission before any controller or resolver. Each module publishes its operation
  table to iam as its API-permission catalogue; iam owns role→permission data; the filter resolves roles through a
  cache and fails closed. No module writes its own filter; no gateway or central service decides (JAVA-CODING-RULES
  §16.2).
- **Privacy and Consent Centre** (`docs/design/PRIVACY-AND-CONSENT-CENTRE.md`): the `privacy` module owns consent in
  its own schema; `privacy-client` is a shared flat module every module may use, an exception to the module rules like
  `auth-client`. `people.consent_record` does not exist.
- **Caffeine** is approved for in-process caches (privacy-client's consent cache, auth-client's role→permission cache).
- The conformance build enforces these rules (JAVA-CODING-RULES §13).

## 3. Inter-service calls

- **gRPC exactly as the owner's reference service (`plt-organization-service`) does — no deviation.** Mechanism:
  JAVA-CODING-RULES §10.
- Protos compile with `os-maven-plugin` + xolstice `protobuf-maven-plugin`; no dynamic-download protoc plugin.
- **No `sdk` module, no Java SDK.** Web and mobile clients are generated in `clients/` from the OpenAPI documents the
  build writes.

## 4. Transactions

- **`AppTransactionManager` (`com.mysms.core.db`) is the only way a unit of work runs.** No `Transactions` class.
  Services extend it, pass their module's database and qualified `PlatformTransactionManager` bean to `super(...)`,
  and run work through `doInReadTransaction` / `doInWriteTransaction`.
- Services never write `getTransaction` / `commit` / `rollback`. `@Transactional` and `TransactionTemplate` are banned.

## 5. Configuration

- Every module has its own `src/main/resources/application.properties` — its microservice's whole configuration,
  each value a default over an environment variable. `app`'s file holds only what differs in one process and wins.
  Code reads keys without defaults of its own; no configuration is built in code (JAVA-CODING-RULES §2.1).

## 6. API and OpenAPI

- **No contracts module, no OpenAPI→Java codegen.** Each area owns its contract as source: interfaces in
  `com.mysms.<module>[.<area>].api` (one per resource, carrying the mappings), DTOs in `….api.model`, the operation
  table in its module's resources; thin controllers implement the interfaces; core's `OpenApiConfiguration` (`GlobalOperationCustomizer`) follows
  the reference service.
- **OpenAPI is code-first**, generated by springdoc during the build — never hand-written, pre-built, scripted or kept
  as a snapshot. Each area's `<Area>OpenApiComponentTest` writes
  `backend/<module>/target/openapi/<area>/api-docs.yml`.

## 7. Database and migrations

- **Until the first release each area has one Flyway baseline; schema changes edit it in place**, no incremental
  migrations (`database/README.md`, JAVA-CODING-RULES §14).
- One schema per area, no foreign keys across modules (an area may reference its module's sibling schemas), no ORM.

## 8. Models and agents

- Implementation agents and the coordinator run on **Claude Opus 5.5**.
- One independent review per batch runs on **Claude Sonnet, the highest version in the owner's plan without separate
  billing**, as a separate agent that did not write the code. Sonnet reviews; it never implements.
- **No Fable** for any work. **No Haiku.** Never a model that needs separate billing.
- If a model's usage limit is reached, fall back to the next best model and say so in the report; do not stop.
- **At most two or three agents at a time.**

## 9. Starting any piece of work

1. Read `git status`, `git diff` and recent `git log`. The owner and other agents change this repository: validate
   their changes (build, tests, review) before building on them, and report defects found.
2. Read the docs governing the area: `README.md`, `ROADMAP.md`, `ARCHITECTURE.md`, `JAVA-CODING-RULES.md`,
   `api/README.md`, `database/README.md`, `ui/*`, and `docs/product/*` where product design matters.
3. Build and test what you change: `mvn clean install` in `backend/` (JDK 17), `pnpm verify` in `clients/`. Work is
   not done while either is red.

## 10. Constraints that never change

- Data security first: tenant isolation, institution scope, owner-only personal data, no leaks in errors.
- Mocks, stubs and fakes only in unit tests (backend and UI).
- Tenant-level settings are set by tenant administrators only; ordinary users cannot set or see them.
- Product language: *student* and *teacher* only.
- Code rules (JAVA-CODING-RULES.md): no ORM, programmatic transactions, flat Maven modules, one schema per area, no
  foreign keys across modules, REST and gRPC, events through MySMS's own `EventBus` (RabbitMQ by default).
