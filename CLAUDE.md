# MySMS — standing instructions for every session

These rules come from the project owner and bind every session and every agent working in this repository.

## Role and standard

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
- **OpenAPI documents are generated from the Spring code** (springdoc-openapi) as part of the build. They are never
  hand-written, pre-built, or produced by scripts.
- **No Python or shell scripts as tooling.** Every generator, check or report the build needs is a Java program that
  runs inside the Maven build.

## Owner decisions of 2026-11-10

- **No contracts module, no OpenAPI→Java codegen.** Each module owns its API contracts as source: the interfaces in
  `com.mysms.<module>.api` (one per resource, carrying the Spring mappings), the DTOs in `com.mysms.<module>.api.model`,
  and the edge operation table in its resources. The pattern follows the owner's reference service: contract
  interfaces implemented by thin controllers, an OpenAPI configuration with an `OperationCustomizer` (core's
  `OpenApiConfiguration`), and a component test per module that fetches `/v3/api-docs.yaml` from the running service
  and writes `target/openapi/<module>.yaml`.
- **Protos are handled the reference service's way**: `os-maven-plugin` + `protobuf-maven-plugin` (xolstice) resolve
  `protoc` and `protoc-gen-grpc-java` for the building machine and compile `api/proto` in one run — into `sdk`, so
  every module has every stub on its classpath without cross-module dependencies (the previous dynamic-download
  protoc plugin stalled builds).
- **Every service inherits `AppTransactionManager`** (core, `com.mysms.core.db`), modelled on the owner's `common`
  module: a service passes its module's database and transaction manager to `super(...)` and runs each unit of work
  through `doInReadTransaction` / `doInWriteTransaction` (a `CallBack`); the base class begins, binds the tenant,
  commits, and rolls back on any failure. What else of `common` was taken, and what was not and why:
  `docs/COMMON-MODULE-REVIEW.md`.
- **One service per aggregate.** A record type that exists only as part of another (a homework's approvals, a
  marksheet's lines) is served by the service of its aggregate, not by a service of its own; the 139 child services
  were merged into 67 aggregate services. Record types with their own lifecycle keep their own service, and unrelated
  aggregates are never merged (JAVA-CODING-RULES §4 "Standard operations").
- **No module descriptor classes.** Core reads which modules a process runs from the application class (the
  packages it scans); the `*Module` classes and their `spring.factories` entries are gone. A module's own settings
  are read where they are used, property first, then the environment variable.
- **Error sentences come from a message catalogue** (core's `MessageUtil` and `ConstraintMapper`, modelled on
  `common`): a field error is given its sentence by code in the caller's language; a new language is a new
  `messages/mysms_<language>.properties`.
- **Homework approvals are written by the workflow only**: the contract has no create, update or delete for them.

## Owner decisions of 2026-11-09

- **OpenAPI is code-first, all modules at once.** The Java API interfaces and DTOs are owned source; the build writes
  each module's OpenAPI document (springdoc). Done 2026-11-10 (see above); the `api/openapi` snapshot remains only as
  the SDK generators' input until they cut over to the springdoc output, pinned to the code by the conformance build.
- **One explicit class per resource, owned as source** (entity, repository, mapper; since 2026-11-10 one *service*
  per aggregate — see above). No code generator writes services, repositories, mappers or
  controllers. Repetition is removed by extracting shared helpers, not by a generic CRUD service base class (the
  transaction base class of 2026-11-10 is the one thing every service inherits).
- **Services drive `PlatformTransactionManager` programmatically.** Each module declares its transaction manager as a
  qualified Spring bean. (Superseded in form on 2026-11-10: services no longer write `getTransaction` / `commit` /
  `rollback` themselves, they inherit them — see the decisions of 2026-11-10.) `@Transactional` and
  `TransactionTemplate` stay banned.
- **Foundations before features:** Java build tooling replaces every Python script, then code-first OpenAPI, then
  the CRUD-layer refactor; the remaining backend operations and UI screens follow on top.

## How to start any piece of work

1. Read `git status` and `git diff` (and recent `git log`) first. The owner and other agents change this repository;
   validate their changes (build, tests, review) before building on them, and report defects you find in them.
2. Read the hand-written Markdown docs that govern the area (README, ARCHITECTURE, JAVA-CODING-RULES, api/README,
   database/README, ui/*, STATUS, HANDOFF, docs/*) before changing it.
3. Build and test what you change: `mvn clean install` in `backend/` (JDK 17) and `pnpm verify` in `clients/`. Do not
   report work as done while either is red.

## Constraints that never change

- Data security comes first: tenant isolation, institution scope, owner-only personal data, no leaks in errors.
- Mocks, stubs and fakes only in unit tests (backend and UI).
- Tenant-level settings are set by tenant administrators only; ordinary users cannot set or even see them.
- Product language: *student* and *teacher* only.
- Code-level rules: `JAVA-CODING-RULES.md` (no ORM, programmatic transactions, flat Maven modules, one schema per
  module, no cross-schema foreign keys, REST and gRPC, RabbitMQ behind MySMS's own `EventBus`).
