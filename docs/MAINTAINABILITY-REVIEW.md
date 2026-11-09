# MySMS maintainability, performance and scale review

**Reviewed:** 2026-11-09  
**Scope:** backend Maven modules and messaging, current documentation, and the React/React Native client workspace.  
**Status:** findings and decisions for discussion. This document does not authorize the follow-on architecture changes below.

## Executive summary

The repository has useful boundaries already: one schema per backend module, generated API clients, tenant-scoped SQL rules, a shared client workspace and outbox-based event delivery. The main risks are that the written architecture describes a more complete and more independently deployable product than the code currently provides, the target UI architecture is mixed with shipped implementation status, and the default monolith opens many database pools while event relays poll frequently.

The immediate messaging ownership and Maven dependency mismatch has been corrected in this change. The remaining items below are proposals for your decision before implementation.

## Verified discrepancies

| Area | Evidence in the repository | Why it matters |
|---|---|---|
| Documentation organization | There is no existing root `docs/` directory. Project guidance is split among root documents and `api/`, `database/`, and `ui/`; facts are repeated in `STATUS.md`, `backend/README.md`, `HANDOFF.md`, and `BUILD-AND-RUN.md`. | The same fact can drift independently. This review is placed in a new `docs/` directory; no existing documents were moved. |
| Messaging ownership | The Java rules, architecture and deployment docs used to place `EventBus` in `core.events`, while the implementations were in `messaging`. The API and event runtime now live in `backend/messaging` under `com.mysms.messaging.events`; the provider adapters live in sibling packages in the same Maven artifact. Docs and generated dependency rules have been updated. | The old package and dependency graph contradicted the actual Maven module design and caused Eclipse/Maven resolution errors. |
| Backend completion counts | `api/operations-status.md` currently reports **1,698 of 2,052** operations served. `STATUS.md` describes **1,663 of 2,052**. | A reader cannot tell which coverage number is current. Keep the generated operation report authoritative and refresh summary snapshots from it. |
| Backend build status | `backend/README.md` says `mvn verify` is green; `STATUS.md` records an earlier successful baseline before the unified messaging layout. The current full reactor attempt stops when Testcontainers cannot find a Docker environment. | The current source has not yet had a complete test run in this environment. Separate source/static-check results from integration-test results in status docs. |
| Deployment readiness | `ARCHITECTURE.md` describes 21 independently deployable services. The Maven modules do create executable jars, but the operation report still shows 354 operations returning 501. The docs also identify unfinished service-to-service identity and reference reconciliation. | A buildable service artifact is not the same as a production-ready microservice. Operators need a per-service readiness definition. |
| Client implementation vs specification | `ui/02-navigation-and-screens.md` inventories 186 screens. `STATUS.md` says 76 web screens are implemented and the rest show placeholders; `clients/README.md` separately lists the built screens, and the platform console has a short list of built screens. The mobile and tablet entry points currently render the shared native shell. | The UI specification is a product target, not a description of current capability. Planning from the screen inventory alone can overstate shipped scope. |
| Offline capability | `ui/05-frontend-architecture.md` specifies SQLite caching, a write queue, conflict handling and offline budgets. `clients/README.md`, the tablet app package description and its app config call the offline store the next increment; no offline store is present in the tablet app source. | Keep offline behavior clearly labeled as planned until persistence, sync, conflict and security behavior are implemented and tested. |
| Frontend performance/CI claims | The web and platform-console packages have local 300 KB initial-JS budget scripts. The architecture doc also says budgets are enforced per app and mentions Lighthouse CI, but equivalent mobile/tablet checks were not found. No CI workflow or pipeline file was found in the repository inventory. | A local build check is useful, but it does not demonstrate continuous enforcement or native-app performance. Clarify which checks are implemented and which are target requirements. |
| Resource use in the default monolith | Deployment docs set a default pool size of 10 per module and explicitly calculate 21 × pool size for the monolith. Messaging config schedules event polling every 500 ms by default. | The default monolith can reserve up to 210 database connections and perform regular idle polling. These should be measured and bounded against the actual database capacity. |

## Decisions to make before broader implementation

### 1. Choose the operating model and readiness language

**Recommended direction:** treat the current system as a modular monolith first, with service-shaped modules and executable jars. Promote a module to a separately operated production service only after it meets a written readiness gate: its required API operations are implemented, its own database and broker configuration are provisioned, service authentication and health checks are verified, and deployment/rollback/monitoring are documented.

**Decision:** keep the existing “21 independent services” target as-is, or distinguish *module boundary*, *runnable artifact*, and *production-ready service* in architecture and status docs. The second choice better reflects the current implementation without discarding the extraction path.

### 2. Set database and event-runtime budgets

**Recommended direction:** retain one pool per schema, but define the monolith's total connection budget from the target PostgreSQL capacity instead of inheriting 10 connections for every module. Measure query latency, pool wait time, event lag, relay batch time and idle query rate before changing pooling or relay behavior.

**Decision:** choose a supported default profile for local/small deployments and a production profile; then decide whether relays stay on fixed polling or adopt idle backoff/database notifications. Avoid increasing pools or relay concurrency without measurements because either can increase database pressure.

### 3. Define the module-call path

The architecture currently requires SDK calls to use loopback HTTP/gRPC even inside the monolith. This gives one behavior across deployment shapes, at the cost of serialization and network-stack overhead for colocated modules.

**Decision:** preserve the single remote call path until benchmarks show it is a bottleneck, or allow an in-process call adapter for the monolith and require contract-level parity tests. If keeping remote calls, specify timeouts, bounded retries, circuit breaking/bulkheads and per-call latency targets before scaling service count.

### 4. Separate shipped client scope from the UI roadmap

**Recommended direction:** keep `ui/` as the product specification and make current implementation state explicit in one maintained status source. Identify the first production client personas and flows, then implement those end-to-end rather than treating all 186 inventory routes as equivalent readiness.

**Decision:** choose which clients and flows are the first release (web, platform console, student/parent/teacher mobile variants, tablet), and whether offline tablet support is a release requirement or a later phase. Record placeholders and unimplemented operations as unavailable features in the release status.

### 5. Make performance and quality gates executable

**Recommended direction:** keep local `pnpm verify` and Maven quality gates, and add a repository CI pipeline that runs the backend with PostgreSQL and broker services, runs client checks, and publishes test/build status. Keep bundle budgets per web entry point; add measured startup/bundle budgets for mobile/tablet only after choosing target devices and build profiles. Treat Lighthouse, accessibility, and end-to-end checks as required only when they exist in the pipeline.

**Decision:** pick CI hosting and required checks, plus the performance targets and device/database profiles they enforce. Current docs mention CI and several budgets that are not represented by repository pipeline files.

### 6. Establish one source of truth for status

**Recommended direction:** keep API operation coverage generated from contracts and controllers, and derive summary counts from that report. Assign a freshness date to manual implementation notes. Use `STATUS.md` as the current project snapshot and link other READMEs to it instead of copying counts and completion claims.

**Decision:** approve a status owner and refresh policy, including whether build and test state is updated by CI or by a dated manual verification.

## Review boundaries

This pass does not change the deployment model, database pool sizes, event polling policy, UI roadmap, CI hosting, or unimplemented product features. It records the discrepancies and candidate decisions so those changes can be made after review and approval.
