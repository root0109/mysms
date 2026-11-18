# MySMS

MySMS is an education management system covering school administration, higher-education academics, online learning (self-paced courses and teacher-led classes with gradebooks, discussions and rich assessments), finance, HR/payroll, library, transport, communication and careers. Clients: React web, React Native mobile apps (students, parents, teachers) and a tablet learning app; backend: Java Spring Boot with Flyway migrations on PostgreSQL.

- **Continuing the work — handoff (where everything is, rules, what is left in backend and frontend, the next chunk):** [HANDOFF.md](HANDOFF.md)
- **Project status — what is built, tested and still open:** [STATUS.md](STATUS.md); every API operation, served or not: [api/operations-status.md](api/operations-status.md)
- **Build and run locally — prerequisites, the one-command backend and client builds, local run, troubleshooting:** [BUILD-AND-RUN.md](BUILD-AND-RUN.md)
- **Deploying and running everything** (database roles, keys, monolith or microservices, events, web, mobile, tests, production checklist): [DEPLOYMENT.md](DEPLOYMENT.md)
- Database design: [database/README.md](database/README.md)
- Flyway migrations (PostgreSQL): [database/migrations/](database/migrations/)
- ER diagrams (Mermaid + PNG): [database/erd/README.md](database/erd/README.md)
- Principal review (open findings): [PRINCIPAL-REVIEW.md](PRINCIPAL-REVIEW.md)
- Schema principles & remaining proposals: [database/table-consolidation.md](database/table-consolidation.md)
- API design (REST + gRPC): [api/README.md](api/README.md), operations index [api/operations.md](api/operations.md), contracts written by each module (`backend/<module>/target/openapi/api-docs.yml`) and its protos (`backend/<module>/src/main/resources/proto`)
- UI specification (web, mobile, tablet): [ui/README.md](ui/README.md) — product map, 186-screen inventory, user flows, design system, front-end architecture
- Architecture (modules, dependency graph, deep links): [ARCHITECTURE.md](ARCHITECTURE.md)
- Java coding rules (Maven multi-module layout, plain Spring JDBC, MapStruct, programmatic transactions, testing, review checklist): [JAVA-CODING-RULES.md](JAVA-CODING-RULES.md)
- Backend (Java 17, Spring Boot 4.1; 21 microservices + monolith): [backend/README.md](backend/README.md)
- Clients (React web, platform console, React Native phone and tablet apps): [clients/README.md](clients/README.md)
- Design-conformance check: the `conformance` Maven module (`backend/conformance`), run by every `mvn verify`
- Proposed enhancements for discussion: [ENHANCEMENTS.md](ENHANCEMENTS.md)
- Maintainability, performance and scalability review (decisions proposed for approval): [docs/MAINTAINABILITY-REVIEW.md](docs/MAINTAINABILITY-REVIEW.md)
