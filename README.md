# MySMS

MySMS is an education management system covering school administration, higher-education academics, online learning (self-paced courses and teacher-led classes with gradebooks, discussions and rich assessments), finance, HR/payroll, library, transport, communication and careers. The backend is 21 Java 17 / Spring Boot services — one per PostgreSQL schema, migrated by Flyway — that also run together as one process; the clients are a React web app, a platform console, React Native phone apps for students, parents and teachers, and a tablet learning app.

## Documentation map

The files marked *reference* are large catalogues: look things up in them, do not read them end to end.

### Start here

| Doc | What it answers | Who reads it |
|---|---|---|
| [README.md](README.md) | What MySMS is and where each topic is documented | Everyone, first |
| [BUILD-AND-RUN.md](BUILD-AND-RUN.md) | Prerequisites, building backend and clients, test suites, build time, running locally, troubleshooting the build | Every developer |
| [ROADMAP.md](ROADMAP.md) | What is built, what is open, owner decisions pending, what to do next | Owner, coordinator, anyone picking up work |

### Architecture and rules

| Doc | What it answers | Who reads it |
|---|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Module-per-schema services, cross-module rules, allowed module dependencies, data access, tenant isolation, deep links, module interdependencies, the module-merge proposal | Backend developers, reviewers |
| [JAVA-CODING-RULES.md](JAVA-CODING-RULES.md) | The binding rules for every line of Java (layout, JDBC, transactions, contracts, gRPC, errors, tests, conformance checks) | Backend developers, reviewers |
| [CLAUDE.md](CLAUDE.md) | Standing instructions for every agent session: owner rules, models, how to start work | AI agents, owner |
| [backend/README.md](backend/README.md) | The backend modules and what each area serves today | Backend developers |
| [clients/README.md](clients/README.md) | The client workspace: apps, packages, and the rules ESLint and the tests enforce | Front-end developers |

### API

| Doc | What it answers | Who reads it |
|---|---|---|
| [api/README.md](api/README.md) | API design: URL and naming conventions, standard methods, errors, auth and tenancy, concurrency, idempotency, key workflows | API consumers, backend and front-end developers |
| [api/operations.md](api/operations.md) | *Reference:* every operation with its REST path, gRPC method and permission code | Looked up per operation |
| [api/operations-status.md](api/operations-status.md) | *Reference (generated):* whether each operation is served, and why not when it answers 501 | Looked up per operation |
| [api/state-machines.md](api/state-machines.md) | *Reference (generated):* the state transitions served, their allowed states and effects | Looked up per resource |

The per-module contracts themselves are written by the build (`backend/<module>/target/openapi/api-docs.yml`) and owned as protos (`backend/<module>/src/main/resources/proto`).

### Database

| Doc | What it answers | Who reads it |
|---|---|---|
| [database/README.md](database/README.md) | Database design: conventions, multi-tenancy and RLS, module boundaries, schema principles, applying the schema with Flyway | Backend developers, DBAs |
| [database/data-dictionary.md](database/data-dictionary.md) | *Reference:* every table and column | Looked up per table |
| [database/erd/README.md](database/erd/README.md) | *Reference:* ER diagrams, one per module | Looked up per module |
| [database/relationships.md](database/relationships.md) | *Reference:* every foreign key (all intra-module) | Looked up per table |
| [database/logical-references.md](database/logical-references.md) | *Reference:* every cross-module logical reference and its cleanup rule | Looked up per column |

Migrations: [database/migrations/](database/migrations/) (one Flyway baseline per module).

### UI

| Doc | What it answers | Who reads it |
|---|---|---|
| [ui/README.md](ui/README.md) | The six clients, ground rules binding every client, guide to the UI documents | Front-end developers, designers |
| [ui/01-product-map.md](ui/01-product-map.md) | Personas, roles, role → client matrix, feature parity, interoperability | Planning anything |
| [ui/02-navigation-and-screens.md](ui/02-navigation-and-screens.md) | Navigation per client and the full screen inventory | Building a screen |
| [ui/03-user-flows.md](ui/03-user-flows.md) | End-to-end journeys with the exact API calls | Building a flow |
| [ui/04-design-system.md](ui/04-design-system.md) | Tokens, theming, components, accessibility, i18n | Building components, reviewing design |
| [ui/05-frontend-architecture.md](ui/05-frontend-architecture.md) | Monorepo, SDK, auth, data layer, offline, testing | Setting up or extending the client code |
| [ui/06-agent-playbook.md](ui/06-agent-playbook.md) | How a UI agent picks up and finishes work | UI agents |

### Operations

| Doc | What it answers | Who reads it |
|---|---|---|
| [DEPLOYMENT.md](DEPLOYMENT.md) | Database roles, keys and secrets, the configuration reference, monolith or microservices, events, gRPC, service identity, production checklist, serving the web and mobile apps, runtime troubleshooting | Operators, backend developers |

### Product

Product thinking and designs that are not yet built; read them when working on that area.

| Doc | What it answers | Who reads it |
|---|---|---|
| [docs/product/SCHOOL-COMMUNITY-NETWORK-BRAINSTORM.md](docs/product/SCHOOL-COMMUNITY-NETWORK-BRAINSTORM.md) | Product brainstorm and requirements for the school community network (launch focus: Indian schools) | Owner, product |
| [docs/product/SCHOOL-COMMUNITY-NETWORK-TECHNICAL-DESIGN.md](docs/product/SCHOOL-COMMUNITY-NETWORK-TECHNICAL-DESIGN.md) | Technical design for the school community network | Owner, developers of that feature |
| [docs/design/PRIVACY-AND-CONSENT-CENTRE.md](docs/design/PRIVACY-AND-CONSENT-CENTRE.md) | Technical design for the Privacy and Consent Centre (DPDP consent, rights requests, erasure, retention, breach) | Owner, backend and UI developers |
| [docs/benchmark/GEGOK12-COMPARISON.md](docs/benchmark/GEGOK12-COMPARISON.md) | Feature-by-feature benchmark against GegoK12 (191 features), gaps and consolidation ideas; gaps tracked as ROADMAP §3.13 | Owner, product |
| [docs/product/LEARNING-AND-ALUMNI-BRAINSTORM.md](docs/product/LEARNING-AND-ALUMNI-BRAINSTORM.md) | Brainstorm of learning tools and alumni opportunities | Owner, product |
| [docs/product/CLASSROOM-VOICE-ASSISTANT-BRAINSTORM.md](docs/product/CLASSROOM-VOICE-ASSISTANT-BRAINSTORM.md) | Product and device brainstorm for a classroom voice assistant | Owner, product |
| [docs/product/HARDWARE-INTEGRATION-REQUIREMENTS-DRAFT.md](docs/product/HARDWARE-INTEGRATION-REQUIREMENTS-DRAFT.md) | Draft requirements for hardware and school-systems integration (not approved for implementation) | Owner, product |
| [docs/product/FUNDING-AND-CLOUD-COST-ESTIMATE.md](docs/product/FUNDING-AND-CLOUD-COST-ESTIMATE.md) | Google Cloud cost, N-year model and India funding plan | Owner |
