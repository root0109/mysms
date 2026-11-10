# MySMS — principal review: open items

Two full review-and-fix cycles have run over the design. The first review produced the P0/P1 catalogue that drove the big revision (tenancy, money integrity, versioning, document immutability, partitioning, API field behaviour, terminology). A **second, independent principal review** was then performed against the revised tree — every behavioral database claim reproduced on a freshly built PostgreSQL 16 instance, every API claim checked mechanically across all 21 modules — and **everything it found that is fixable at the design stage has been applied and re-verified**. This file is now the register of what remains open: product decisions, contract-scale work consciously deferred, and implementation-stage items. Fixed findings are not listed; the evidence that they are fixed is at the end.

Related registers: `ui/07-open-dependencies.md` (how the clients cope with each open item until it lands), `database/table-consolidation.md` §C (code-stage consolidation rules C-1…C-8), `ENHANCEMENTS.md` (feature backlog).

---

## 1 · Decisions still needed

| # | Decision | Default until decided |
|---|---|---|
| D1 | **Authority model** — a user's permissions are the *union* of all role grants (recommended), or only the active role counts. Blocks the permission-model rework (P1-27). | Union of grants |
| D2 | **Institution scope** — scope comes from the token's role grants plus each record's `institutionId` (recommended; drop the unused `X-Institution-Id` header), or wire the header into every operation. Clients currently keep the switcher client-side (ui D-04). | Record-field scoping |
| D4 | **Currencies** — two-decimal currencies only for v1 (`numeric(14,2)`, documented in the database README), or widen to four decimals now. | Two-decimal |
| D5 | **Inventory / store module** — not in v1 (recommended), or design it now. | Not in v1 |

(The former Decision 3 — tenant copies of platform defaults at provisioning — is decided and implemented; no shared NULL-tenant rows exist.)

---

## 2 · Open API items (contract-scale, carried from the first review)

These keep their original IDs because `ui/07-open-dependencies.md` references them. They are deliberate deferrals, not oversights: each changes the shape of many operations and is cheapest done in one coordinated pass before SDK generation is frozen.

| ID | Item | Size |
|----|------|---|
| P1-24 | **39 pure link tables are five-operation resources** (≈195 ops). Expose the set on the parent (`authorIds` via update mask) or a `:set…` method; drop the child CRUD, keep the tables. Fold in the drive-sharing duplicate (`POST …/shares` vs `:share`) when this lands. | M |
| P1-25 | **Aggregate children are standalone resources** (question options/blanks/hotspots, rubric grid, invoice/payslip lines, fee-plan installments, release-rule lines). Make Question, Rubric, FeePlan, ReleaseRule and draft Invoice aggregates: message embeds children, create/update write the whole. | M |
| P1-26 | **Residual parity polish**: no `read_mask` on Get/List (gRPC or REST `readMask`); parity script compares operations and shapes but not yet every parameter. (The 412/ETag, page-token and enum-typing parts are done.) | S |
| P1-27 | **Permission model**: 1,671+ operation-level codes → resource-level codes (`read`/`write`/`delete`/`manage` + genuinely separate duties), permission as a proto method option. Depends on D1. | M |
| P1-28 | **Tenant self-service**: `GET`/`PATCH /v1/platform/tenant` for the current tenant (time zone, locale, currency); decide whether `platform.tenant_domain` (custom domains) gets a tenant-facing resource or stays platform-internal. | S |
| P1-29 | **Residual**: `StartCheckout` should take a short-lived `quoteId` from `QuoteCheckout` so the price cannot change between quote and payment. (Idempotency-Key required on money/bulk ops, key scope/mismatch semantics, 412 + `current` in the problem body — all done.) | S |
| P1-31 | **Residual**: one shared `BatchResult` shape (per-row result/error, counts, atomic flag) for the batch writes; sweep the remaining unbounded synchronous ops to 202 + `Operation` (`GenerateResults` is converted; `EvaluateReleaseRules`, `PublishFinalGrades` and bulk sends should follow the same pattern where row counts are unbounded). | S |
| P1-32 | **PATCH cannot clear a field**: declare nullable types where NULL is legal; "in the mask and null ⇒ set NULL"; read-only fields in the mask → 422. | S |
| P1-33 | **Filter / sort surface is unbounded**: declared filterable/sortable fields per resource (from indexes), simple AND grammar, `totalSize` opt-in. Partitioned-list time filters are already mandatory. | S |
| P1-34 | **Service-to-service identity and batch reads** (open; references are stored unchecked meanwhile, decision of 2026-11-07): service-principal tokens, audience tag per operation (public / internal / platform), `BatchGet` + `*Card` projections for the most-referenced resources. | M |
| P1-35 | **Mobile/tablet essentials**: per-module `:changes?since=` feed (gRPC streaming twin), idempotent `me/devices:register` (today: handle 409 on `iam.CreateUserDevice` by update), `renditions[]` on download URLs, public `GET /v1/platform/app-config`. | M |
| P1-36 | **Events and outbound webhooks**: event messages in proto with a CloudEvents envelope, AsyncAPI generated from them; `webhook_subscription` / `webhook_delivery` with HMAC signatures and retries. | M |
| P1-37 | **Composite summaries** that read across modules (`sis.GetStudentSummary` reads attendance, finance, library) move to a `/v1/bff/<app>/…` namespace — the only place cross-module reads are composed. | S |
| P1-38 | **File, report and import permissions**: the owning module issues upload/download links for its files; report and import definitions name their own permission. | M |
| P1-39 | **Launch integrations**: LTI 1.3 (1.1 is deprecated by the standards body) and Google / Microsoft single sign-on with PKCE. | M |

Also open from the old P2 API list: common.proto slimming (P2-12), lms.proto split (P2-13), typed operation results per operation kind instead of the generic `result` value (remainder of P2-14 — operations themselves are now module-owned), gRPC idempotency levels/service config/streaming (P2-15), caching headers on reference lists (P2-16), read-only views as sub-resources (P2-17, ui D-17), fire-and-forget analytics writes (P2-18), typed maps (P2-19), error-code registry (P2-20), teacher bulk endpoints (P2-21).

---

## 3 · Open database / platform items (implementation- or scale-stage)

| ID | Item | Size |
|----|------|---|
| P2-01 | **Index hygiene at scale**: replace most single-column `tenant_id` indexes with the composites list screens need, checked with `EXPLAIN`. (The eight duplicate unique-index pairs and the shadowed index found by the second review are already dropped.) | S |
| P2-02 | Cross-service copies and batch reads (consolidation §6.4, §7.4): copied keys J2–J4 kept consistent by the owner's events (there are no cross-module FKs to lean on; by decision of 2026-11-07 references are not validated against the owner when written); `BatchGet` card reads for the most-referenced resources (overlaps P1-34). | M |
| P2-03 | **Weekly timetable template** (class × weekday × period → subject, teacher, room, effective dates); staff/room clash detection stays an application rule. | M |
| P2-04 | Effective-dated staff history `hr.staff_position`; exit/probation/contract dates on staff. | S |
| P2-05 | **Offline-created rows**: client-generated UUIDv7 ids, `client_device_id`, `client_created_at`, idempotent insert; offline content packages. (ui D-08.) | S |
| P2-07 | Institution type stored twice (varchar + FK): keep one; EAV attributes only for custom fields. | S |
| P2-08 | `data_region` on the tenant before expanding to the US. | S |
| P2-09 | Audit log immutable by grant: provisioning scripts give `mysms_app` INSERT + SELECT only on `platform.audit_log` and the other evidence tables (no UPDATE/DELETE), add request id and actor tenant to the audit event, and a (tenant, time) index. | S |
| P2-10 | Coupons: per-redemption table, atomic usage limit, per-user limit, minimum amount. (The `redemption_count <= usage_limit` CHECK exists.) | S |
| P2-11 | Full-text search (`tsvector` + GIN), LTI/xAPI tables, holiday/working-day calendar, messaging provider accounts, app release & feature flags, HLS renditions, push quiet hours, gradebook→report-card bridge, retention & erasure jobs, read replica. | S–M each |
| P3-a | `academics.subject.code` is `varchar(256)`; make it 30 like other codes. | S |
| P3-b | `password_changed_at` / password history if a tenant policy needs it. | S |
| P3-c | Admission workflow history: interviews, document checklist (audit log covers status changes for v1). | M |
| P3-d | Inbound webhooks REST-only (exclude from gRPC and the SDK). | S |
| P3-e | One timestamp naming style in the API (`*Time`); `Recipients` gains guardians, staff and roles. | S |

---

## 4 · Small items noted and deliberately left

- **`/v1/attendance/staff-attendance-days`** vs table `attendance.staff_attendance`: the resource name is the plural noun the UI uses; left as is.
- **`FeeStructureUpdate` / `FeeConcessionUpdate`** still allow changing scope fields (`institutionId`, `academicYearId`, `gradeLevelId`, `programId`, `feePlanId` / `feeHeadId`): legitimate while the row is `draft`, locked by the workflow after `:publish`. The server enforces draft-only; revisit if a stricter contract (separate draft resource) is wanted with P1-25.
- **Consolidation C-1…C-8** (`database/table-consolidation.md`): aggregate version columns, copied keys, authorisation cache etc. — code-stage rules for the service implementation, re-stated in `JAVA-CODING-RULES.md` where they shape code.
- **ENHANCEMENTS.md** rows remain the feature backlog; items raised to P1 by review (LTI 1.3, SSO, change feed, webhooks) are listed in §2 above.

---

## 5 · Scale-out pass — schemas may live on separate instances

Every module schema may be deployed on its own PostgreSQL instance, so nothing may couple two schemas. Applied and verified:

- **All cross-module foreign keys removed** (the former cross-module migration plus 31 cross-schema FKs found inside module baselines). They are now 494 registered *logical references* (468 forward + 26 against-DAG) in `database/logical-references.md`, each indexed and validated against the live catalog by its generator; integrity comes from archive-not-delete, event-driven cleanup (built) and RLS; write-time validation through the owner's API was dropped by decision of 2026-11-07, and the reconciliation job waits for P1-34. Every money- and evidence-protecting `RESTRICT` rule was already in-module and is untouched.
- **No cross-schema writes:** every service schema has its own `idempotency_key` and `operation`; the seven number-issuing modules have their own `number_sequence` (unused copies in `platform` and `assessment` removed); audit records flow through each module's outbox into `platform.audit_log`.
- **Module-owned operations in the API:** ten modules expose a uniform `OperationService` (`/v1/<module>/operations…`), the shared `Operation` carries `module`, and `platform.ListOperations` is the job-centre index fed by operation events. `platform.GetOperation` now answers 200 (it is a read). Resolves P2-06 (each module's folder is its own Flyway history by design) and most of P2-14.
- **Docs:** ARCHITECTURE.md and JAVA-CODING-RULES.md describe the flat 25-module build, logical references, module-local infrastructure, SDK allowlist and trace propagation; the database README, data dictionary (logical references shown per column and per owner), relationship register and ER diagrams are regenerated from one validated source.

## 5a · Code-generation and skeleton pass — found and fixed

Generating real code from the contracts (OpenAPI Generator for Java and TypeScript, protoc for gRPC) and building the skeleton surfaced contract defects that linting could not see. All fixed and re-verified:

- **Schema name collision:** `LeaveBalance` was defined in both `common.yaml` and `hr.yaml` with different shapes; the joined spec silently kept one. The common one is now `LeaveBalanceSummary` (OpenAPI + proto), and `verify_api` fails on any name defined in two files.
- **71 inline JSON bodies named:** 67 responses (mostly item pages) and 4 request bodies were inline objects, so generators invented names such as `PlatformListOperations200Response`. They now reference named schemas (`OperationList`, `ResultCardList`, `RecoveryCodes`, `StudentLeaveDecisionRequest`…); `verify_api` rejects new inline bodies (free-form webhook payloads excepted).
- **External schema names lost in generation:** the generator's OpenAPI 3.1 parser inlines schemas referenced from `common.yaml`. `api/tools/OpenApiBundler.java` now makes each module file self-contained first; the Java and TypeScript builds share it.
- **master advertised idempotency it could not honour:** the database deliberately has no `master.idempotency_key` (global reference data), but the four master creates accepted `Idempotency-Key` / `request_id`. The parameter and proto field are removed (field 2 reserved); a retried create is answered 409 by the unique natural key; api/README states the exception.
- **Edge semantics of token-optional reads:** reference lists are public per api/README; the operation table now records `anonymous` for `security: [{}, …]` so the edge lets them through without a token while still verifying one when sent (the platform console lists countries with an operator token).

Noted for a later migration (not changed here): `uq_country_iso2` and `uq_currency_code` are full unique constraints, so a soft-deleted country or currency blocks its code forever, unlike the partial uniques of P1-15; and the RLS policies cast `current_setting('app.tenant_id', true)` directly, which errors (fails closed) instead of returning no rows on a pooled connection whose transaction-local tenant was reset to an empty string — `NULLIF(…, '')` would make that a clean empty result. api/README says output-only values sent by a client are ignored (Standard methods) but also that writing them is rejected with 400 `FIELD_NOT_WRITABLE`; the server currently ignores them, and the text should keep only one rule.

## 5b · Sign-in, profile and hierarchical settings pass — found and fixed

Building real sign-in, the profile screen and settings defined top-down (tenant → institution → department → class section → user) surfaced these, all fixed and covered by component tests against PostgreSQL:

- **No self-service profile in the contract:** ACC-07 could only use `people.UpdatePerson`, which needs the administrators' permission and edits names and identity. Added `people.GetMyPerson` / `people.UpdateMyPerson` (`/v1/people/me`, permissions `people.person.read_mine` / `update_mine`, contact and self-description fields only, the person chosen by the token's new `pid` claim).
- **Settings had no home:** added the iam setting catalogue and values (`iam.setting_definition`, `iam.setting_value`, primary grants on `iam.user_role`) and the caller's effective settings (`ListMySettings`, `SetMySetting`, `ResetMySetting`), with locks and scope-limited administration; ui/04 §9 and D-25 rewritten to match.
- **The permission catalogue was empty:** no migration seeded `iam.permission`, so no role could hold any permission. `V1_030_004` now seeds all 1,643 tenant permission codes of the contracts (ids derived from the code; the build's conformance rule P1 checks the catalogue against the contracts); self-service codes apply to every role and cannot be removed.
- **Tokens would not fit a header:** an administrator's 1,643 codes make a ~50 KB JWT. The issuer compacts complete sets (`*`, `<module>.*`, `<module>.<resource>.*`) and `AuthContext.hasPermission` expands them.
- **Sign-in could not resolve a tenant** without reading the platform schema: `iam.tenant_directory` (no RLS, codes and status only) plus an idempotent installation bootstrap for the first tenant and administrator.
- **Unserved operations answered a misleading 404:** they now answer 501 `NOT_IMPLEMENTED` (edge marker + explicit controller stubs); api/README "Errors" lists it.
- **Monolith bean-name collisions:** two modules may each have a `PersonMapper`; the monolith now names components by fully qualified class name.
- **Text blocks drop trailing blanks:** SQL fragments concatenated after `WHERE`/`AND` lost their space; caught by the component tests, fixed, and recorded in JAVA-CODING-RULES §6.

An independent review of this pass (separate agent, fresh context) found three major defects, all fixed with tests: the password lockout also refused token refresh, so anyone could end a user's sessions with five wrong passwords (refresh now ignores the lockout); refresh ignored a suspended tenant (now refused and the session ended); and the settings screen needed `iam.setting_definition.read`, which ordinary users lacked (now a self-service permission). Its minor findings were fixed as well: tenant-wide grants cover every institution (`ins: ["*"]`), refusals cost the same work whether or not the account exists, sign-out no longer raises a false token-theft alarm and unregisters push devices, refused refreshes end their session, the bootstrap trims the username once, the self-service profile no longer accepts a photo file id until uploads are served, the JWKS can carry the retiring key during a rotation, the client signs out with a fresh token, and the settings and administration screens recover from load, permission and paging failures.

Still open, named in backend/README "Status": MFA sign-in (accounts with a second factor are refused with 501), password reset and change, user and role administration, the outbox relay that keeps `iam.tenant_directory` in step with platform tenant events, and the parts of `iam.GetMe` that come from org, sis and hr (institutions, student, staff and guardian ids) until those modules are served.

## 6 · Second review — what was found and fixed (evidence)

**Database re-review** (every claim reproduced live, twice): 3 P0 · 6 P1 · 5 P2 · 8 P3 — **all applied**, among them: versioning slot uniques made partial (v2 of every artefact type now creatable — exercised in the smoke suite), evidence FKs to RESTRICT (`result_change_log→exam_result`, `attendance_record→sheet`, `gateway_event→payment`), money dedupe keys made absolute (gateway txn, receipt) + new refund dedupe, manual-due slot unscoped, stale absolute iam indexes dropped (role re-grant, mobile reuse), allocation `currency_id` with composite FKs, subject-registration line coherence FK, money CHECKs (discount/net, percent ≤ 100, invoice-purpose, line totals, coupon redemption, credit-note self-reference, register scope, sibling self-link), duplicate indexes dropped, quiz-attempt NULL edge, outbox relay role + `master.outbox_event.tenant_id` nullable, gateway default unique, holiday/state-code/reservation uniques, README counts and notes (role/grant matrix, soft-delete-unique exceptions, partition-id uniqueness, allocation precedence).

**API re-review** (mechanical sweeps over all modules): 5 P0 · 16 P1 · 15 P2 · 7 P3 — **all applied except the two "left" notes in §4**, among them: MFA login made a challenge-or-success union and the flow completed (`ListMfaCredentials`, `RegenerateRecoveryCodes`, enrolment verify leg), encrypted host-start URL and meeting link removed from generic schemas (gated ops only), tenant-subscription writes moved behind `platformAdminAuth` with typed read-only billing fields, workflow-owned rows stripped from generic writes (`StudentFeeDueUpdate`, `EnrollmentUpdate`, `CohortEnrollmentUpdate`, `ExamResultUpdate`, eight dead `*Update` schemas deleted), refunds/gateway-accounts/staff-bank-accounts/allocations exposed, money-document schemas aligned to the DB (credit notes, tax fields, payslip totals, snapshots, versioning columns, exercise placement, file scan lifecycle), Idempotency-Key and 429 header gaps closed, operations.md public column populated, identity fields removed from Update schemas, `GenerateResults` → 202 + Operation, `ArchiveFeeStructure` added, naming mismatches resolved.

**Events re-review** (independent agent, fresh context, 2026-11-07): 3 high · 6 medium · 6 low — **14 applied, 1 declined**, each with a test on real PostgreSQL or RabbitMQ: a failed confirm no longer kills the publishing channel (a new one is opened); an event published before any consumer bound its queue waits in an alternate-exchange queue instead of being dropped; every message carries the broker-verified `user-id`, consumers accept an event only from its module's broker user and only if its source owns its type, and per-service broker users with topic permissions are documented and were proven on a live broker; a bad outbox row is set aside (`publish_error`) instead of blocking the module; relay batches are time-bounded; failed deliveries are returned after a doubling delay, and dead-lettered or unrouted events can be replayed (`EventsAdmin`); the inbox is keyed by `(tenant_id, event_id)`; the relay has a role of its own (`mysms_relay`, column-level `UPDATE`) that a service refuses to share with its application role; several subscription beans of a module are merged and duplicates refused; the 26 against-DAG references are now part of the generated cleanup (the generator fails on an unknown action text); processed inbox rows are pruned; relay passes are fair across modules; cancelled consumers subscribe again; the in-process bus parks a poison event. **Declined:** skipping archived rows when clearing a column — a restored row would otherwise bring back a dangling reference.

**Current verification state** (re-run after the last change):

- Migrations: clean apply to PostgreSQL 16.15 with `ON_ERROR_STOP` → **632 relations** (457 tables + partitions); `verification/smoke.sql` → **SMOKE OK**, **SMOKE2 OK** and **SMOKE3 OK** (cross-tenant attack rejected in all directions, soft-delete re-creation, money dedupe incl. refunds, allocation currency guard, evidence-delete rejection, lesson v1→v2 versioning flow; no cross-schema FK, module-local infrastructure present exactly where required, RLS on it, module-local document numbering).
- Catalog ↔ docs: 457 tables, 7,043 columns, 468 FKs (0 cross-schema), 494 logical references (all indexed), 360 CHECKs, 230 unique constraints, 249 partial/expression unique indexes, 445 RLS policies (every tenant-owned table, all `FORCE`d); no extensions, functions or triggers — README, data dictionary, relationship register, logical-reference register and ER diagrams regenerated from the same parse.
- Contracts: all 22 protos compile (grpcio-tools) — **224 services / 2,040 RPCs / 4,525 messages**, field-numbers lock regenerated (no existing number changed); all OpenAPI files parse; `redocly lint` on the joined `mysms-api.yaml`: **0 errors, 0 warnings**; REST↔gRPC parity **0 mismatches in both directions**; `operations.md` regenerated from the YAMLs (generator proven byte-identical on the previous version first).
- UI docs: every `module.Method` reference resolves to an existing operation (0 dangling).
- Terminology: zero occurrences of the two retired role words (everything says *student* / *teacher*) and of all reference-project names anywhere in the tree.
