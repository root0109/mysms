# MySMS clients

pnpm/turbo workspace (pnpm 10, Node 22.12+, React 19.1, Vite 7, Expo 54). Architecture:
[ui/05](../ui/05-frontend-architecture.md); screens: [ui/02](../ui/02-navigation-and-screens.md). Build and dev
servers: [BUILD-AND-RUN.md](../BUILD-AND-RUN.md) §3.2 and §4.3; phone and tablet builds and build-time variables:
[DEPLOYMENT.md](../DEPLOYMENT.md).

The apps always talk to a real backend (the monolith on `:8080`; `MYSMS_API_PROXY` for web,
`EXPO_PUBLIC_API_BASE_URL` for Expo). Mocks exist only in tests.

## Layout

| Path                                    | What                                                                                                                                                                                                                           |
| --------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `apps/web`                              | W: school web app. Every inventory screen is routed (`src/routes.ts`); unbuilt ones show a placeholder                                                                                                                         |
| `apps/platform-console`                 | C: platform staff web app (tenants, plans, enquiries, global settings, jobs, master data)                                                                                                                                      |
| `apps/mobile`                           | S/P/T: one Expo project, three store apps by `APP_VARIANT` (`student`, `parent`, `teacher`)                                                                                                                                    |
| `apps/tablet`                           | B: Expo, landscape                                                                                                                                                                                                             |
| `packages/sdk`                          | Generated (OpenAPI Generator `typescript-fetch`, version in `openapitools.json`) from each module's `backend/<module>/target/openapi/api-docs.yml`. Needs a JDK; `OPENAPI_GENERATOR_JAR` for a local jar. Never edited by hand |
| `packages/core`                         | API wrapper (refresh-and-replay, problem+json, ETag, idempotency, retries), session, permissions, route table and deep links, query hooks per feature (`src/hooks/<feature>.ts`), long-running operations. Platform-neutral    |
| `packages/web-kit`                      | Web screens by feature (`src/screens/<feature>/`) and shell shared by both web apps                                                                                                                                            |
| `packages/native-kit`                   | Phone and tablet screens and shell shared by both Expo apps                                                                                                                                                                    |
| `packages/ui-web`, `packages/ui-native` | Presentational components with matching names and props; no data access, no literal copy                                                                                                                                       |
| `packages/tokens`                       | Design tokens for CSS and React Native; light, dark, high-contrast, densities, text size, reduced motion; WCAG AA checks                                                                                                       |
| `packages/i18n`                         | ICU catalogues (`en` source, `hi` partial) and Intl formatters                                                                                                                                                                 |
| `packages/fixtures`                     | Seed data and a stateful fake backend, for tests only                                                                                                                                                                          |
| `packages/config`                       | Shared TypeScript and Vitest presets                                                                                                                                                                                           |

## Rules

Enforced by ESLint, the type checker, tests or the build:

- Apps and kits reach the API only through `@mysms/core`; `ui-*` import only `tokens` and `i18n`.
- Nothing outside tests imports `@mysms/fixtures`, test runners or mocking libraries.
- Permission codes, the route table and operation owners are generated from the contracts.
- `@mysms/core` coverage: 90 % lines. Web components pass an axe check.
- Initial JavaScript of each web app ≤ 300 KB gzip (`scripts/check-budget.mjs` fails the build).
- One React version across the workspace (pnpm override).

Conventions:

- No literal copy: every string from the catalogue, keys `module.screen.element`; enums through catalogue labels.
- In shared code use `pathOf`, `searchOf`, `parseQuery` from core (React Native lacks `URL.pathname` and
  `URLSearchParams.get`/`has`).
- A list-manager screen links rows to their own screen with `detail` in `RESOURCE_SCREENS`
  (`web-kit/src/screens/resourceScreens.ts`); two inventory screens can share one list through an address filter.
- Small lists read every page through `core/src/api/pages.ts`; unsaved work is guarded by `UnsavedChangesGuard`;
  web tests sign in through `apps/web/src/test/signedIn.tsx`.
