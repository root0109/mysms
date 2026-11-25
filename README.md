# MySMS

MySMS is a multi-tenant education management system for schools and colleges, built first for India: school
administration, academics, online learning, assessment, finance, HR and payroll, library, transport, communication,
careers, and a DPDP privacy and consent centre.

## What it is made of

| Part | What | Docs |
|---|---|---|
| Backend | Java 17, Spring Boot 4.1, plain Spring JDBC, PostgreSQL 16, Flyway. 22 domain modules, one per schema; each runs as its own service or all together in one process (`app`). REST and gRPC on every operation; events through an outbox to RabbitMQ (or Pub/Sub) | [backend/README.md](backend/README.md), [ARCHITECTURE.md](ARCHITECTURE.md) |
| Database | One schema per module, one Flyway baseline each, row-level security per tenant, no cross-schema foreign keys | [database/README.md](database/README.md) |
| Clients | pnpm/turbo workspace: web app, platform console, phone apps (student, parent, teacher), tablet app; shared SDK generated from the backend's OpenAPI | [clients/README.md](clients/README.md), [ui/README.md](ui/README.md) |

The 22 modules: `master`, `platform`, `iam`, `org`, `people`, `academics`, `sis`, `hr`, `payroll`, `admission`,
`attendance`, `facility`, `timetable`, `assessment`, `lms`, `finance`, `library`, `transport`, `comms`, `frontoffice`,
`careers`, `privacy`.

## Build and run

```bash
(cd backend && mvn install)                     # JDK 17, Maven 3.9+, PostgreSQL 16 and RabbitMQ, or Docker
(cd clients && pnpm install && pnpm verify)     # Node 22.12+, pnpm 10; after the backend build
```

Prerequisites, running locally and troubleshooting: [BUILD-AND-RUN.md](BUILD-AND-RUN.md). Production:
[DEPLOYMENT.md](DEPLOYMENT.md).

## Where to look

| Question | Doc |
|---|---|
| What is open, owner decisions, what comes next | [ROADMAP.md](ROADMAP.md) |
| Which API operations are served (generated) | [api/operations-status.md](api/operations-status.md) |
| Personas, roles, features per client | [ui/01-product-map.md](ui/01-product-map.md) |
| Screens and navigation; user flows | [ui/02](ui/02-navigation-and-screens.md), [ui/03](ui/03-user-flows.md) |
| Module boundaries, allowed calls, tenant isolation | [ARCHITECTURE.md](ARCHITECTURE.md) |
| Binding Java rules | [JAVA-CODING-RULES.md](JAVA-CODING-RULES.md) |
| Agent instructions and owner rules | [CLAUDE.md](CLAUDE.md) |
| API design and conventions | [api/README.md](api/README.md) |
| Every operation, path and permission (reference) | [api/operations.md](api/operations.md) |
| State transitions (generated reference) | [api/state-machines.md](api/state-machines.md) |
| Tables, foreign keys, cross-module references, ERDs (reference) | [database/data-dictionary.md](database/data-dictionary.md), [relationships.md](database/relationships.md), [logical-references.md](database/logical-references.md), [erd/](database/erd/README.md) |
| Design system; front-end architecture; UI agent playbook | [ui/04](ui/04-design-system.md), [ui/05](ui/05-frontend-architecture.md), [ui/06](ui/06-agent-playbook.md) |
| Designs: privacy centre, legal brief, background jobs, module and product reorganisation | [docs/design/](docs/design/) |
| School Community Network design (for approval); product brainstorms, hardware draft, cost estimate (not approved for build) | [SCHOOL-COMMUNITY-NETWORK-TECHNICAL-DESIGN.md](docs/product/SCHOOL-COMMUNITY-NETWORK-TECHNICAL-DESIGN.md), [docs/product/](docs/product/) |
| Benchmark against GegoK12 (gaps tracked as ROADMAP §3.13) | [docs/benchmark/GEGOK12-COMPARISON.md](docs/benchmark/GEGOK12-COMPARISON.md) |

Contracts are owned in code: `com.mysms.<module>.api` and the protos in `backend/<module>/src/main/resources/proto`;
the build writes each module's OpenAPI to `backend/<module>/target/openapi/api-docs.yml`.
