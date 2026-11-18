# 05 · Front-end architecture

How the six clients are built from one codebase, one SDK and shared packages — and the exact client-side rules for auth, data, errors, offline and files. The backend rules it encodes come from [`../api/README.md`](../api/README.md) and [`../ARCHITECTURE.md`](../ARCHITECTURE.md).

## 1. Monorepo

pnpm workspaces + Turborepo; TypeScript strict everywhere; one lint/format config. The workspace lives in `clients/` beside `api/`, `database/` and `ui/`; its README has the commands.

```
clients/
├── apps/
│   ├── web/                  # W — React, Vite
│   ├── platform-console/     # C — React, Vite (separate build, platformAdminAuth)
│   ├── mobile/               # S, P, T — one Expo (React Native) project, three build
│   │                         #   targets (app id, icon, default tab tree per app);
│   │                         #   ~90 % shared code, per-target entry config
│   └── tablet/               # B — Expo (React Native), landscape-first, offline store (§7)
├── packages/
│   ├── sdk/                  # generated TypeScript client from ../backend/*/target/openapi/api-docs.yml (never hand-edited)
│   ├── core/                 # auth/session, api-client wrapper, query hooks, offline engine,
│   │                         #   deep-link table, permission helpers, notification handling
│   ├── tokens/               # design tokens (JSON → CSS vars + RN theme)
│   ├── ui-web/               # web components (04 §5)
│   ├── ui-native/            # RN components (same names/props)
│   ├── i18n/                 # message catalogues + Intl helpers
│   ├── web-kit/              # what W and C share: sign-in, session guard and restore, inventory-driven
│   │                         #   routing (every screen of 02 mounted; unbuilt ones show a placeholder), shell
│   ├── native-kit/           # what the Expo apps share: root, secure-storage vault, sign-in, deep links
│   ├── fixtures/             # typed seed data + a stateful fake backend — imported by *.test.* files only
│   └── config/               # shared tsconfig and Vitest presets (ESLint and Prettier config at the root)
```

Code generation lives with the package it feeds: `sdk/scripts/generate.mjs` (types from the contract) and `core/scripts/generate.mjs` (permission codes, the screen/route table of 02, the operation-owning modules). Turborepo runs `generate` before every typecheck, test and build, so contract drift fails the build.

Rules (enforced by the workspace ESLint config): `apps/*` may import only `packages/*`; `ui-*` may import only `tokens` + `i18n`; **all** server types come from `sdk` (no hand-written API types); `core` owns every `fetch` — components never call the SDK directly, only `core` hooks.

**Why not react-native-web:** admin tables, gradebooks and timetable builders need real web DOM ergonomics, while phones need native navigation and gestures. Logic is shared in `core`; UI is twin-built against the same component contract — the parity cost is paid where it is cheap (components), not where it hurts (interaction quality).

## 2. Routing and deep links

- One route table in `core/routes.ts`: `pattern → { screen, params, requiredPermission }`, generated from the screen inventory (02). Web (React Router) and RN (Expo Router) both consume it.
- Deep links: `mysms://<path>` and `https://<tenant host>/app/<path>` register on all apps; `resolveLink(path)` returns the native screen or `web-fallback`. Unknown/unauthorised paths → web URL / polite dead-end screen.
- Web URLs are the canonical share format; `mysms://` is emitted only by the backend in `actionUrl`.
- Guards: route `requiredPermission` checked against `Me.permissions` before render; institution-scoped routes also verify the record's institution is in `Me.institutions` after fetch.

## 3. Auth and session (`core/auth`)

- **Login** `iam.Login` → `{ tokens, me }`. Access token in memory only; refresh token in platform secure storage (web: httpOnly cookie set by the auth endpoint if available, else encrypted storage with the documented XSS trade-off; RN: Keychain/Keystore via `expo-secure-store`).
- **Refresh**: single-flight mutex; on 401 → refresh → replay the request once; refresh failure → session-expired sign-in. Clock skew tolerated by refreshing 60 s early.
- **Persona switch**: `iam.SwitchRole` replaces the token pair and `Me`; `core` emits `sessionChanged` — the shell rebuilds navigation and the query cache is cleared of permission-dependent lists.
- **Impersonation**: `iam.StartImpersonation` opens a visually distinct session (persistent banner, 30-min, no refresh) in a separate browser tab context; `iam.StopImpersonation` restores.
- **Sign-out**: `iam.Logout` (revokes device + refresh), wipe caches, keep device push token registered off.
- **MFA**: `iam.Login` returns `mfaRequired` + `mfaToken`; the client calls `iam.CompleteMfa` with the code to get tokens. Enrol/disable via `iam.EnrollMfa` / `iam.DisableMfa` (ACC-08).
- Headers on every call: `Authorization`, `Accept-Language`, `X-Client` (`app/version/platform`), and the institution context per Decision 2 once wired (07).

## 4. Generated SDK and data layer

- **Generation**: [OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator) (`typescript-fetch`, version pinned in `packages/sdk/openapitools.json`) generates one client per module from that module's file in `api/openapi`, after `api/tools/OpenApiBundler.java` has made the file self-contained (the Java build uses the same bundler and generator, so both sides compile against identical inputs). Each module is a namespace (`master.CountryApi`, `master.Country`); all share one runtime. Models are plain interfaces and dates stay ISO strings, exactly as the API sends them. `core/api` supplies the transport under every client (`Configuration.fetchApi`): base URL per tenant, auth with refresh-and-replay, idempotency keys, retry, problem+json parsing, ETag capture. `core` imports SDK values per class (`@mysms/sdk/master/apis/CountryApi`) so React Native bundles carry only the clients they use (Metro does not tree-shake); an ESLint rule enforces it. Regenerated by `pnpm generate` and in CI on contract change; the build fails on type breaks — that is the point.
- **Server state**: TanStack Query. Query keys follow the API: `[module, resource, params]`; lists use `useInfiniteQuery` whose next page parameter is the page number and cursor of the last response's `paging.next` link (core's `nextPage`). Invalidation map per mutation lives next to the hook (`gradeEntry → invalidate [lms,'gradebook',cohortId]`).
- **Client state**: Zustand for UI-only state (switchers, drafts, table prefs); never a copy of server data.
- **Forms**: react-hook-form + zod schemas derived from SDK types; server 422 `errors[]` mapped to fields by name.
- **Freshness defaults**: reference data `staleTime` 10 min (+ ETag/304 when C5 lands); lists 30 s; detail 0 with background refetch; dashboards refetch on focus.
- **Pagination/caps**: `size` 50 web / 25 phone; every partitioned-table list (logs, messages, notifications, attendance) always sends a time-range filter (02 §1.5).

## 5. Permissions in the client

`can(code)` checks `Me.permissions`; `<PermissionGate>` wraps controls; route guards per §2. Codes are taken from `operations.md` and regenerated into `core/permissions.ts` so a typo is a compile error. The UI never *enforces* security — it mirrors it; the server remains the authority.

## 6. Errors, concurrency, idempotency (`core/api`)

- **problem+json** parsed into `ApiError { status, code, title, detail, errors[], traceId }`; every surfaced error shows `title` + mapped copy (04 §8) and logs `traceId`.
- **Retry**: automatic (expo backoff, max 3) only for idempotent GETs on network/5xx/429 honouring `Retry-After`; never for writes — writes retry only via their Idempotency-Key path.
- **Idempotency-Key**: `core` attaches a UUIDv7 per logical attempt to every POST; the key is held with the queued action so a retry replays the same key. Money/enrol/bulk-send flows disable their trigger until the first attempt resolves.
- **ETag / If-Match**: every fetched row keeps `etag`; updates/deletes send `If-Match`. On **412/409 conflict**: refetch, show `ConflictDialog` (their version vs your pending fields; "keep server" default; merge per-field where the form allows), never auto-overwrite. The same component serves the offline conflict tray (§7.5).
- **403 vs 404**: both render as "not available" detail states (the API hides invisible rows as 404); 403 on an action the UI showed = permissions changed → refetch `Me`.
- **Global handling**: 401 → refresh path; maintenance 503 → full-screen retry card; everything else is local to the screen (no global error toasts for query errors).

## 7. Offline engine (tablet B in full; teacher T attendance-lite)

### 7.1 Store
SQLite (`expo-sqlite`) per account: tables mirror the cached resources (`id TEXT PK, etag, updated_at, json BLOB, scope`), plus `outbox` (queued writes) and `sync_meta` (cursors per scope). Encrypted at rest (SQLCipher key in secure storage). Cache budget 2 GB with LRU eviction by cohort.

### 7.2 Scope and hydration
Offline scope = explicitly downloaded cohorts/courses (LMS-21 "Make available offline"). Hydration = scoped list calls per resource (outline, lessons, items, forum heads, gradebook for teachers) + content files via `platform.GetDownloadUrl` into app storage; packaged download replaces this when E10 ships.

### 7.3 Delta refresh
On connect + every 15 min online: re-pull scoped lists with `updated_at`-overlap windows (idempotent upsert by `(id, row_version)`); replaced by the per-module `:changes` feed when P1-35 lands (the engine isolates this behind `SyncSource` so the swap is one adapter). Deletions reconcile from `platform.deletion_log` scope queries where exposed; until then a full re-list per scope runs daily.

### 7.4 Outbox
Every offline-capable mutation enqueues `{ opId, sdkCall, params, idempotencyKey, etag, enqueueAt }` and applies an optimistic local patch flagged `queued`. Flush strictly per-scope FIFO on connectivity; each item: success → clear flag; 412/409 → conflict tray; 4xx validation → error tray with edit-and-retry; 5xx → keep with backoff. Quiz answers use their own contract-native queue (`attempt_token` + `client_sequence` + `answeredAt`) and therefore survive even app reinstall once synced.

### 7.5 Conflict tray
`ConflictTray` lists items needing a human: shows local vs server values, actions *keep server* / *reapply mine* (re-issues with fresh ETag where legal). Grades and attendance never auto-overwrite (03 F3/F9).

### 7.6 Teacher-lite (T)
Attendance capture only: roster cached for the day's sessions; marks queue through the same outbox; no content store.

## 8. Push notifications and inbox

- Expo notifications → FCM/APNs; token registered at login (device upsert pending P1-35 — until then handle the 409 by update), unregistered at logout.
- Payload carries `actionUrl` + title/body only (no PII beyond that); tap → deep-link resolver; foreground push → in-app banner + badge update (`lms.GetUnreadCounts`).
- The inbox (ACC-09) is the source of truth; push is only a pointer. Web uses in-app polling for the badge (no web push in v1).

## 9. Files and media

- Upload: `platform.CreateUploadUrl` → direct PUT (progress, resume-by-retry) → `platform.CompleteUpload`; `UploadManager` shows the global queue; images client-compressed (≤ 2048 px) before upload.
- Download/view: always via `platform.GetDownloadUrl` at click time (links expire); PDFs in-app viewer; video via the returned URL (HLS renditions when E11 lands).
- Never cache signed URLs — cache the file bytes (B) keyed by file id + checksum.

## 10. Performance budgets

- **W**: initial route JS ≤ 300 KB gzip (code-split per module); TTI ≤ 3 s on mid-tier laptop; table interactions < 100 ms; gradebook virtualised to 5,000×100 cells.
- **Phones**: cold start ≤ 2.5 s mid-tier Android; Hermes; screen JS bundles lazy by tab; list rendering via FlashList.
- **B**: offline open-lesson ≤ 1 s from SQLite; sync batched ≤ 50 ops/request group.
- Images: responsive sizes, lazy, blur placeholder; icons as a single sprite/font.
- Budgets enforced in CI (bundle-size check per app; Lighthouse CI for W).

## 11. Observability

- Crash + error reporting (Sentry) on all apps with `traceId` attachment; API latency and failure-rate metrics from the client wrapper; sync-queue depth metric on B/T.
- Product analytics: screen views + the 15 flows' funnel events, named `flow.step` (e.g. `F4.quote`, `F4.paid`); no PII in event payloads; analytics respects tenant-level opt-out.

## 12. Testing strategy

| Layer | Tool | Gate |
|---|---|---|
| Components | Storybook + interaction tests + axe | zero critical a11y violations |
| Hooks/logic | Vitest unit | core 90 % lines (enforced as a coverage threshold) |
| Contract | generated SDK compile; the fake backend in tests follows the contract's semantics | build fails on drift |
| Flows | Playwright (W) / Detox or Maestro (RN) against a real backend (the monolith on PostgreSQL) | the 15 flows green |
| Offline | deterministic sync tests: scripted connectivity + conflict fixtures | outbox invariants hold |
| Visual | Storybook snapshots light/dark/RTL | reviewed diffs only |

## 13. Mocking only in tests (rule)

**Fakes exist only inside tests.** The stateful fake backend in `packages/fixtures` (bearer auth, ETag/If-Match with 412,
problem+json, per-module operations) is imported by `*.test.*` files and test setup only. No app, kit, `core` or `ui-*`
file imports it, and no shipping file imports a test runner (`vitest`, `@testing-library/*`) or a mocking library
(`msw`, `nock`, `sinon`, `fetch-mock`, `miragejs`, `jest-mock`) — statically, through `import()`, `require` or a
re-export — or touches the `vi`/`jest` globals. The workspace ESLint config fails the build on any of these; only
`*.test.*` files, `src/test-setup.ts`, `src/test/` folders, the fixtures package itself and the Vite/Vitest
configuration are exempt. There is no mock server:
in development the web dev servers proxy `/v1` to a real backend (the monolith on `:8080` by default,
`MYSMS_API_PROXY` to change it) and the Expo apps call `EXPO_PUBLIC_API_BASE_URL`. A screen is built against the
contract, tested with the fake in its tests, and checked in a real browser against the real backend. The same rule
holds in the backend: mocks only in unit tests (JAVA-CODING-RULES §1).

## 14. CI/CD

- PR: typecheck, lint, unit, SDK-drift check, bundle budgets, Storybook a11y, affected-app Playwright smoke.
- Main: full flow suites against the monolith on PostgreSQL; web preview deploy per PR; Expo EAS builds (dev/staging) with OTA updates for JS-only changes; store releases follow the app-config min-version gate (D-21; pending P1-35) — until then a static config file served beside the web app.
- Versioning: apps follow the API's additive-only rule; a client is always safe against a newer backend (unknown fields ignored, unknown enum values rendered as `neutral` chips — the contract marks open enums `x-extensible-enum`).
