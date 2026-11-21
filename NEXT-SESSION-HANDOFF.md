# NEXT SESSION HANDOFF — read fully, then delete this file (do not commit it)

Updated 2026-11-21 (Cowork session). Binding rules: `CLAUDE.md` (read first), `JAVA-CODING-RULES.md`.
Current item: **ROADMAP SEC-11 — Privacy and Consent Centre**, phase P0 (design: `docs/design/PRIVACY-AND-CONSENT-CENTRE.md`).

## Owner rules (still in force)
- Stop and save at 80 % of the 5-hour usage limit; ask the owner for usage at checkpoints. Weekly limit was 94 % on
  2026-11-21, so work resumes the following week.
- Store nothing outside this repo and `~/Development/Repos`; no scratch files outside the repo.
- Do not guess: read the code or search the web; mark anything unverified "to verify".
- Reference projects, READ-ONLY (never edit, never copy verbatim; follow their patterns and style):
  `~/Development/Repos/plt-access-control-service`, `~/Development/Repos/access-control-library`.
  **Core must stay in the reference service's style** — the owner rejected a `MessageUtil.fieldError` factory; core's
  `MessageUtil` is lookup-only like the reference's `MessageUtils`. Do not change core/auth-client Java without asking.
- Models: Opus implements; one independent **Sonnet** review per batch (review only); no Fable, no Haiku; ≤ 2–3 agents.
- The owner builds on his Mac (`mvn` in `backend/`, `pnpm verify` in `clients/`) and pastes results. The device shell has
  no Maven and Java 11 only. Running a downloaded Checkstyle jar is blocked by the session's auto permission check, so
  before every hand-off run the python style check (lines ≤ 130, unused/star imports, space after keywords, final
  newline, utility classes final + private ctor, braces) and look for SpotBugs patterns (a `@Nullable` getter called
  twice, throwing constructors in non-final classes, exposed mutable collections). Ask the owner to run
  `mvn -o -q checkstyle:check -pl <modules>` before a full build.
- Never hand over while an agent is still writing files (a build that starts mid-write fails on stale poms).
- The owner commits. **Nothing is committed yet** (SEC-0 and all privacy slices are uncommitted).
  Never commit this file or `.atl/`.

## Status
SEC-0 (API permissions published to iam, role-based permissions filter, Caffeine cache) — DONE, green, uncommitted.

Privacy P0 plan (owner-approved), a full build after each slice:
| # | Slice | Status |
|---|---|---|
| 1 | Module skeleton + purpose catalogue (ListPurposes, GetPurpose; 17 purposes seeded) | DONE, green |
| 2 | Retire `people.consent_record` (table, 5 ops, proto + 9 copies, iam seeds, reference cleanup) | DONE, green |
| 3 | Notices (notice / version / translation, publish, 15 ops) + UpdatePurpose | DONE, green |
| 4 | `privacy.subject` + `privacy.guardian_link` projections; people.person and sis.student_guardian events carry state | DONE, green |
| 5a | consent_record, consent_evidence (monthly partitions, per-tenant hash chain), evidence_chain, consent_state, EffectiveConsentRule, 8 consent ops | DONE, green (2026-11-21) |
| 5b | Recompute triggers + daily majority/expiry job | not started |
| 6 | `privacy-client` skeleton (ConsentChecker, Caffeine, fail closed) + ConsentDecisionService gRPC; needs data_category + processing_activity catalogue | not started |
| 7 | ACC-13 consent-centre screen (web + mobile), SDK, docs, one Sonnet review | not started |

Pinned counts now: 22 modules, **682 tables** (privacy 40 = 16 tables + 24 evidence partitions), **2077 operations**
(privacy 26). Places: `PrivacyServiceComponentTest.TABLES`, `MySmsApplicationComponentTest` (tables, operations,
databases 22, histories 22, class javadoc), `OperationTablesComponentTest` (3×), `OperationStatusReportTest`,
`ConformanceTest` (22 modules), `clients/packages/sdk/scripts/generate.mjs` (22).

## First thing next session
Slice 5a built green on 2026-11-21. Confirm `cd clients && pnpm verify` is green (not run since slice 3; it regenerates
the privacy SDK), ask the owner the open decisions below, then start slice 5b. The SDK move (below) must be done before slice 7.

## Open decisions for the owner
1. Impersonation / not-a-guardian answer core's generic `PERMISSION_DENIED` / `NOT_FOUND`; the design names
   `CONSENT_IMPERSONATION_FORBIDDEN` / `CONSENT_NOT_GUARDIAN` (needs a core change). Recommendation: keep generic.
2. Evidence is append-only by code only; by privilege needs core's `ApplicationRoleGrants` (ROADMAP SEC-6).
3. Data-category sensitivity levels (normal / sensitive / restricted) for slice 6's catalogue — not in the design; ask.
4. Whether ACC-13 (slice 7) also gets the admin screens for purposes and notices, or they wait for P1.

## Pending owner task: move client SDK generation out of `clients/` (do before slice 7)
The owner has a separate SDK project generating clients in several languages and wants `clients/packages/sdk` removed.
Found 2026-11-21: only `packages/core` and `packages/fixtures` depend on `@mysms/sdk` (`workspace:*`); ~51 files import
it, as namespace types (`import type { iam } from '@mysms/sdk'`) and deep paths (`@mysms/sdk/people/apis/PersonApi`).
`packages/core/scripts/generate.mjs` (permissions/screens/resources from operation tables) is NOT the SDK and stays.
Steps: (1) ask how the external SDK is consumed (private npm registry, git dependency or local path) and whether its
TypeScript output matches today's `typescript-fetch` naming and layout (package name `@mysms/sdk`, `<module>/apis|models`
paths) — if yes it is a dependency swap, else fix call sites; (2) point core and fixtures at it, delete
`clients/packages/sdk` and its turbo `generate` step; (3) agree how the external project gets the backend's OpenAPI
documents (`backend/<module>/target/openapi/api-docs.yml` from each `<Module>OpenApiComponentTest`) and how its version
tracks the backend; (4) update CLAUDE.md §3 (it names `clients/` as the generator), `clients/README.md`, `ui/05`,
ROADMAP; (5) `pnpm verify` green. Slice 7 (ACC-13) is then built against the external SDK.

## Slice 5b — what to build
- Call `ConsentStateWriter.recompute(subject, purposeCode)` from `SubjectSync` (status/majority change),
  `GuardianLinkSync`, `PurposeService.updatePurpose` (switch, ceiling, notice) and `PublishNoticeVersion` when the new
  version requires re-consent.
- Daily job (no `@Scheduled` exists in the repo yet — find the house way for background work first): minors whose
  `majority_date` ≤ today become adults, `privacy.subject.majority_reached`, `majority_lapsed` evidence; consents past
  `valid_until` expire (`expired` evidence, `privacy.consent_record.expired`).
- Known gap: `guardian_link.guardian_user_id` can go stale (sis.guardian events are id-only); consent identifies the
  giver by person id, so it does not matter for consent.

## Docs still to update (slice 7)
`database/data-dictionary.md` + ERD for privacy, `backend/README.md`, DEPLOYMENT tables for the privacy module (DB, gRPC,
broker vars), ROADMAP SEC-11 status, `ui/02` + `ui/03` (ACC-13 still names the removed people operations),
`api/operations.md` and `ui/README.md` (stale totals), design §6.1/§13 error codes per decision 1.

## Earlier open follow-ups (pre-existing)
- New permission codes from a catalogue have no display name.
- No platform-staff token issuance exists in iam yet.
- `JAVA-CODING-RULES.md` §7.1a still mentions `common`'s `CommonConfig`.
- Finder recreates `.DS_Store` under `backend/` and conformance M0 fails on it.
- JwtTokenVerifierTest:120 is over 130 columns; `JwtTokenVerifier.map` has no caller.
