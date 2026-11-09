# MySMS UI specification

Everything a UI developer — or a UI agent — needs to design and build the MySMS clients. The backend inputs are frozen contracts: the API in [`../api/`](../api/README.md) (OpenAPI 3.1 + gRPC, 2,052 operations) and the database design in [`../database/`](../database/README.md). This package translates them into products, screens, flows and front-end architecture.

**Status.** Design specification; the client workspace that implements it is [`../clients/`](../clients/README.md). Where a spec depends on a pending backend decision (see [`../PRINCIPAL-REVIEW.md`](../PRINCIPAL-REVIEW.md)), it says so and names the item; the full list is in [07-open-dependencies.md](07-open-dependencies.md). Nothing here requires waiting: UI work is built against the OpenAPI contract and the running backend; fakes are used only inside tests (see 05 §13, "Mocking only in tests").

## The clients

| Code | Client | Platform | For |
|---|---|---|---|
| **W** | MySMS Web | React (responsive web) | Every tenant role: administrators, office staff, teachers, students, parents; full feature surface |
| **S** | Student app | React Native (phone) | Students: timetable, homework, attendance, fees, learning, notices |
| **P** | Parent app | React Native (phone) | Guardians: children's day, fees and payment, communication, consent |
| **T** | Teacher app | React Native (phone) | Teachers: attendance, homework, marks, timetable, classes, messages |
| **B** | Tablet LMS | React Native (tablet, offline-first) | Students and teachers: courses, cohorts, quizzes, grading — works without a connection |
| **C** | Platform console | React (web, internal) | Platform staff only (`platformAdminAuth`): tenants, plans, global catalogues |

One account works in every client; a deep link (`mysms://…` or `https://<tenant host>/app/…`) opens the same content in whichever client receives it. How the six stay interoperable is specified in 01 §5 and 05 §2.

## The documents

| Doc | Contents | Read it when |
|---|---|---|
| [01-product-map.md](01-product-map.md) | Personas, roles, the six clients, role → client matrix, feature parity matrix, interoperability model | Planning anything |
| [02-navigation-and-screens.md](02-navigation-and-screens.md) | Navigation trees per client and the full screen inventory: 186 screens with routes, roles, API operations and states | Building a screen |
| [03-user-flows.md](03-user-flows.md) | 15 end-to-end journeys, step by step with the exact API calls, error paths and notifications | Building a flow that spans screens |
| [04-design-system.md](04-design-system.md) | Design tokens, tenant theming, the component inventory, interaction patterns, accessibility, internationalisation, configurability and personalisation | Building components, reviewing design |
| [05-frontend-architecture.md](05-frontend-architecture.md) | Monorepo layout, stack, generated SDK, auth and session, data layer, offline sync, push, files, errors, testing, mocking only in tests | Setting up or extending the codebase |
| [06-agent-playbook.md](06-agent-playbook.md) | How a UI agent picks up work: inputs, conventions, definition of done, self-checks | Automating UI work |
| [07-open-dependencies.md](07-open-dependencies.md) | Backend items the UI depends on, each mapped to a PRINCIPAL-REVIEW id, with the interim approach | Sprint planning |

## Ground rules (bind every client)

1. **The API is the only data source.** No client talks to the database, another service, or undocumented endpoints. Everything renders from the generated SDK (05 §4).
2. **Routes mirror the API.** A web route is the API resource path without `/v1`, under `/app`: `/app/lms/cohorts/{cohortId}/gradebook`. The same path string is the deep link (`mysms://lms/cohorts/{cohortId}/gradebook`) and the push-notification target, so every client resolves the same links (ARCHITECTURE.md §4).
3. **Permissions decide what renders.** A control is hidden (not disabled) when `iam.GetMe` grants no permission for its operation; lists never show rows the server would refuse. The permission code of every operation is in [`../api/operations.md`](../api/operations.md).
4. **Server state is the truth.** Optimistic UI only where a flow spec allows it; every write sends the ETag where the contract requires one and handles 409/412 as specified in 05 §6.
5. **Tenant theming, not tenant forks.** All visual variation comes through design tokens resolved from the tenant's theme (04 §2). No per-tenant code.
6. **Every screen ships its five states:** loading (skeleton), empty, error, offline, and content (02 §1.4).
7. **Accessible and localised by default:** WCAG 2.2 AA, 44 px touch targets, every string through i18n, no hard-coded formats (04 §6–7).

## What exists for UI work today, and what was missing

Already available: the complete REST/gRPC contracts with permission codes, the deep-link scheme, tenant theme storage, consent gating rules, offline quiz primitives (attempt token, client sequence, per-answer timestamps), two-step file upload, cursor pagination, and problem+json errors.

This package adds what was missing: a product map, navigation and a screen inventory, user flows, a design system, client architecture, and the dependency list. With these and the backend, UI development is unblocked end to end; fakes are used only inside tests.
