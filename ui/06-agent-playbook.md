# 06 · UI agent playbook

How an AI agent (or a new developer) picks up and completes UI work in this repo without supervision. The specs are written to be executed from; this file is the operating procedure.

## 1. Inputs, in reading order

1. [README.md](README.md) — ground rules (the seven rules bind every change).
2. The work item: a screen ID (02 §3), a flow (03), or a component (04 §5).
3. For the screen's operations: [`../api/operations.md`](../api/operations.md) row(s) → exact REST path, gRPC method, permission code; the module's `../api/openapi/<module>.yaml` for request/response shapes.
4. [05-frontend-architecture.md](05-frontend-architecture.md) — where code goes and which `core` hooks to use.
5. [07-open-dependencies.md](07-open-dependencies.md) — if the screen cites a pending item, build the interim behaviour named there, behind the named feature flag.

## 2. Hard rules (agent must self-enforce)

- **Never invent an operation, field or enum value.** Everything server-side comes from the generated SDK; if a needed operation is missing, stop and file it against 07 — do not fake it client-side.
- **No new colours, spacings or font sizes** — tokens only (04 §2). No literal user-facing strings — i18n keys only (04 §7).
- **Components before screens**: if a needed component exists in 04 §5, use it; if it needs a new prop, extend it with a story; create new components only when 04 lists none.
- **Every screen ships the five states** (02 §1.4) and a Storybook story per state.
- **Permission-gate every action** with the code from operations.md via `PermissionGate`/`can()`.
- **Writes**: ETag on update/delete, Idempotency-Key via `core` (never hand-rolled), conflict dialog on 409/412 (05 §6).
- **Routes** come from the inventory (02) and are added to `core/routes.ts` with their permission — never ad-hoc `navigate('/string')`.
- Do not modify `packages/sdk` (generated) or token values without a design-review label.

## 3. Procedure per screen

1. Create/extend the route in `core/routes.ts` (pattern, screen, permission) — web and native tables.
2. Write the data hooks in `core` (`useX` wrapping SDK + query keys + invalidation map). One hook per operation; no fetch in components.
3. Build the screen from `ui-*` components per the shape (02 §1.1): layout first with skeletons, then wiring.
4. Implement the five states; wire errors per 05 §6; empty-state action only when `can(create)`.
5. i18n: add keys under `module.screen.*` to `packages/i18n/en`; no literals.
6. Stories: content/loading/empty/error (+offline where relevant), light+dark, one RTL smoke.
7. Tests: hook unit tests (the fixtures fake backend, inside the test), interaction test for the primary action, flow test if the screen completes a numbered flow step.
8. Self-check (below), then PR titled `SCR-XXX <screen>` linking the spec row.

## 4. Definition of done (checklist)

- [ ] Matches the inventory row: route, clients, personas, operations — no extra server calls.
- [ ] Five states implemented and storied; skeleton mirrors final layout.
- [ ] All interactive elements permission-gated; nothing disabled-but-visible for missing permissions (hidden instead).
- [ ] ETag/Idempotency handled through `core`; conflict path demonstrated in a test.
- [ ] Zero axe criticals; keyboard path on W; touch targets ≥ 44 px on RN.
- [ ] Zero literal strings/colours/sizes (lint rules enforce; must pass).
- [ ] i18n keys added; pseudo-locale render not clipped.
- [ ] Bundle budget not exceeded (CI check).
- [ ] Deep link opens the screen cold (test via `resolveLink`).
- [ ] If the screen depends on a 07 item: feature flag named, interim behaviour implemented, TODO references the 07 ID.

## 5. Fakes in tests, the real backend in development

Mocking is done only in tests (05 §13). A screen's tests use the fake backend from `packages/fixtures`; when a fixture
is missing, add it there (typed, realistic, school-plausible names; no real-person data). Development and the
browser check of a screen run against the real backend (the monolith on PostgreSQL, `DEPLOYMENT.md` §8). Shipping
code never imports fixtures or mocks — ESLint fails the build. Never point dev builds at production.

## 6. When the spec and the contract disagree

The contract (`../api/*`) wins for shapes; the spec (this folder) wins for behaviour. Record the discrepancy in `ui/DISCREPANCIES.md` (create if absent) with screen ID, what differed, what you built — these feed the next spec revision. Do not silently improvise.

## 7. Suggested first work orders

1. Foundations: tokens package, `core/auth`, SDK generation, route table, app shells (W, mobile, B) — unlocks everything.
2. ACC-01…ACC-09 (auth + inbox) on W and mobile.
3. ATT-02/03 + TT-03 on T (teacher loop), SIS-11 + FIN-07/08 on P (parent loop).
4. SIS-01/02, ADM-05, ACA-05/06 on W.
5. LMS-01…LMS-08 on W, then B hydration (05 §7).
