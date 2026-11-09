# 07 · Open backend dependencies for the UI

Everything in `ui/` is buildable against the current contracts and the backend, **except** the items below, which depend on pending backend decisions or fixes in [`../PRINCIPAL-REVIEW.md`](../PRINCIPAL-REVIEW.md). Each has an interim approach so UI work never blocks, and a feature flag so the switch-over is one toggle.

Resolved and removed from this table (now in the contracts): placement-bound quiz start, MemberCard public profiles, MFA sign-in/enrol/disable, counter payments (`finance.RecordPayment`), fee structures and concessions, tenant-readable notification catalogues, writable-field classification (readOnly + pruned `*Update` schemas), declared `429`s, student leave + staff attendance resources, and the student/teacher naming — SDK identifiers now match the docs.

| # | UI need | Depends on | Flag | Interim approach until it lands |
|---|---|---|---|---|
| D-04 | Institution context header vs field (shell switcher) | **Decision D2** (PRINCIPAL-REVIEW §1) | `ctx.institutionHeader` | Switcher is client-side scope for filters (`institutionId` params); no header sent |
| D-05 | Delta sync feed for offline (B/T) | **P1-35** (`:changes`) | `sync.changesFeed` | `updated_at` overlap-window re-pull per 05 §7.3 behind the `SyncSource` adapter |
| D-06 | Idempotent device registration for push | **P1-35** | `push.registerUpsert` | Handle 409 on `iam.CreateUserDevice` by fetch-and-update |
| D-07 | Offline content packages for tablets | **P2** (E10) | `offline.packages` | Per-resource hydration (05 §7.2) |
| D-08 | Client-generated ids for offline-created rows | **P2-05** | `offline.clientIds` | Dedupe via Idempotency-Key only; avoid offline-create for anything but the contract-native quiz queue and outbox'd grading/attendance |
| D-09 | Batch-get + `*Card` types for people shown in lists | **P1-34** | `sdk.batchGet` | `core` memoised per-id fetch with request coalescing; swap to BatchGet when present |
| D-12 | Tenant self-service settings (ADM-15) | **P1-28** | `tenant.selfService` | Read-only values from branding; edits via platform console |
| D-16 | Aggregate editors save whole question/rubric in one call (LMS-43/46) | **P1-25** | `authoring.aggregates` | Editors operate on child CRUD under a facade in `core` so the UI code doesn't change when the aggregate ops land |
| D-17 | Read-only views as sub-resources (gradebook et al.) | **P2-17** | — | Routes already use sub-paths; `core` maps to the current `:verb` GETs |
| D-18 | Report/search permission corrections | **P1-27** | — | UI gates on current codes; regen updates `core/permissions.ts` |
| D-20 | Outbound webhooks / events console (integrations admin) | **P1-36** | `integrations.webhooks` | Not built |
| D-21 | App config endpoint (min version, flags) | **P1-35** | — | Static config served beside the web app |
| D-22 | SSO (Google/Microsoft) buttons | **P1-39** (E29) | `auth.sso` | Hidden |
| D-23 | HLS renditions for lesson video | **P2** (E11) | `media.hls` | Single MP4 URL playback |
| D-24 | Quiet hours in notification preferences (ACC-10) | **P2** (E12) | `notif.quietHours` | Hidden section |
| D-25 | Tenant and role UI defaults: default dashboards and quick actions per role, terminology per institution type (04 §9). **Settings an admin sets or locks at tenant, institution, department or class-section scope are resolved:** the iam setting catalogue and values (ACC-14, ADM-16) | **P1-28** (tenant settings resource, with D-12) for dashboards, quick actions and terminology | `tenant.uiDefaults` | Persona defaults shipped with the clients; hierarchical settings already apply |


**Process.** When a backend item ships: regenerate the SDK, flip the flag in `packages/core/flags.ts`, delete the interim adapter, and close the row here. Rows are never deleted before their flag is removed from code.
