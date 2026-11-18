# School Community Network — technical design (developer's view)

**Date:** 18 November 2026 (revised the same day after the design gate) · **Status:** design for review; **no code is
to be written from this document until the owner approves it** · **Companion:**
[`SCHOOL-COMMUNITY-NETWORK-BRAINSTORM.md`](SCHOOL-COMMUNITY-NETWORK-BRAINSTORM.md) (the product requirements;
requirement ids such as NET-04 or SAFE-11 below point there).

The brainstorm says *what* the product should be. This document says *what we would build, piece by piece*, in the
MySMS codebase as it is today: which existing parts we reuse, which tables, operations, events, jobs and screens are new,
how every administrator switch is enforced, and how privacy is kept right at each layer. It is written for the engineers
who will build it and for the owner who decides scope.

### Decisions that gate the first build batch

**§18 item 2 is ruled (18 Nov 2026): policy is stored in comms**, so **B1** (the staff-only text slice, §16) may
start. Before the first (S1) pilot: items 4, 10, 12, 16 and 21. Before guardians (S2): items 8, 13, 15,
17, 19, 22, 23. Before photos (S3): items 9, 10, 11, 14, 16, 18. Before students (S4): item 20. Phase names used in older
sections map to slices: P0a-1 = S1 + S2, P0a-2 = S3, P0b = S4.

### Gate changes (18 Nov 2026)

The design gate's 60 questions (`community-gate/QUESTIONS.md`, answers in `community-gate/ANSWERS.md`) changed:

- **Membership rule made explicit** (§7.4): class-page membership is the AND of sis link state, custody, iam account and
  role, enrolment and policy; any source saying no wins. Mid-year changes, history per role and drafts in a table.
- **Class page DDL fixed** (§5.1): `class_section_id` and `academic_year_id` not null, one live page per section,
  `page_category`, `class_page_member`, `class_page_attachment` and `post_comment.attachment_file_id` dropped; membership
  only in `space_member` with a check that class-page rows come from relationships.
- **Consent model made real** (§8.4, §15): people's `consent_record` gains `scope` (class < institution < inter_school <
  public), `decision`, `giver_capacity`, `valid_until`, verification and a per-giver unique index; most restrictive
  legal guardian wins; adult students give their own; unknown = blocked.
- **sis gains a restricted guardian link** (§7.7, §15) with confidential restriction records, two operations and events
  that carry no reason.
- **Surfaced copies** get a state machine evaluated at read time (§5.2.1).
- **Review**: author and implicated people can never approve; two distinct moderators are a precondition of enabling
  community; every image with students needs a second person's subject confirmation (§6.5) — every image at all, since round 3.
- **Revision hash** covers every content-bearing field, media generations and the audience snapshot (§5.2).
- **Audience semantics** are formal, exclude wins, groups are snapshot ∩ live, policy narrowing applies at read time
  (§9.1); the feed statement is given in full with indexes and dataset (§9.2).
- **Knob order and validation** for every knob (§4.5); widening approval and the two-person rule (§4.6); switches read
  uncached on every request, platform kill switch (§4.7); floor values are code (§4.8).
- **Media pipeline** state machine, limits, library (TwelveMonkeys ImageIO; HEIC converted on the client), originals
  deleted after processing, emergency takedown deletes object generations (§8).
- **Revocation** measured from source commit, relay heartbeats, degrade-then-close policy (§7.3).
- **Safeguarding** stored apart, encrypted, every read logged; cases about staff route to an independent handler (§5.4,
  §10.2).
- **Notifications** carry no class or child names off-app; rows are rendered at read time and redacted on revocation
  (§11).
- **Retention table**, purge with a deletion ledger and all object generations (§5.6, §5.7); erasure discovery (§5.8).
- **P0a split** into a text-only slice and a photo slice, with a dependency matrix, gates per phase and a traceability
  table (§15, §16, §20); incident runbook (§21).
- New knobs **K31a, K36a, K36b** (no existing knob renumbered); nine new open decisions (§18, items 8–16); open decisions 2, 3 and 5 updated.

**Round 2 of the gate (same day, `community-gate/ANSWERS-ROUND2.md`):**

- **Feed admission in one statement** (§9.2): membership, relationship validity, viewer keys, live groups, denies and
  hydration are all read by the one feed statement (one snapshot); the Java pre-step can only remove homes.
- **Lost-event detection** (§7.1, §7.3): the source relays stamp a gapless sequence on the streams that revoke
  (consent, guardian links, enrolment, staff, accounts); gaps fail closed (made per tenant and reorder-tolerant in round
  3; the sis screen step added in round 2 was replaced in round 3 by comms reading restricted links from sis, §7.7).
- **Community media bucket** (§5.6, §5.7, PR-11): a dedicated bucket with versioning off, soft delete off and no backup
  copy; purge verified against the bucket's real settings; guardian wording states the real recoverability.
- **After hours** (§21, K34a): any responder may take down at any time; outside coverage a report on an image withholds
  it at once; platform operators can stop media for one tenant or institution without seeing content.
- **Outside reviewers** see text only (§6.5); **majority** mechanism (§8.4); **consent centre** wording (§12.7);
  **silence vs no consent** (§8.4); **deny-row lifecycle** (§5.1); **PR-8 and PR-12 verified** from the code (§15).
- Open decisions 2, 9, 12, 13 restated with recommendations; new 17 (MEM-02/MEM-03 deferral) and 18 (platform on-call).

**Round 3 — independent gap review (`community-gate/GAP-REVIEW.md`, answers in `ANSWERS-ROUND3.md`):**

- **Protected subjects** (§7.7): a restriction now suppresses the protected child, for that guardian, in every home,
  surfaced copy, inter-school post, comment, notification and media URL, and removes the guardian from school-wide
  spaces; the sis screen step is gone, replaced by comms reading restricted links from sis.
- **Global switch row policy** (§4.7, PR-19): tenant transactions can read the global switch; `FOR SHARE` only on
  tenant rows.
- **Revocation machinery per tenant and reorder-tolerant** (§7.1, §7.3): per-tenant sequences, a reorder window,
  automatic repair for callable owners, states for all six sources, a maintenance flag; bulk read RPCs (PR-20); id-only
  payloads no longer acceptable (PR-5/PR-7 hard); early events parked.
- **Membership of every space type** (§7.4a); **switch × operation table** (§4.9); **write-surface test** by operation.
- **Slices restructured** (§16): S1 staff-only text is the first shippable slice; guardians S2, photos S3, students S4;
  the dependency matrix is per slice (§15.1).
- **Tests** run in two tiers with an injected clock; a named load-test environment (§14).
- **Images:** every image needs a second person at every width; covers too; no pixels kept as evidence by default; names
  have their own consent; posts hide whole when a subject's consent lapses; stable media path and batch URLs;
  orientation, memory and a separate media worker; precomputed consent state (§8).
- **Staffing and bootstrap** (§6.5, §4.6): co-teachers confirm, minimum staffing stated, platform onboarding approves the
  first enablement; proposal lifecycle; safeguarding reads tied to K36/K36a; case-bound member actions (§10.2);
  report-to-withhold limits and after-hours holds (§21); participation consent for students (§7.4); DR-safe deletion
  ledger (§5.7).
- New open decisions 19–21.

**Round 4 — re-check (`community-gate/GAP-RECHECK.md`, answers in `ANSWERS-ROUND4.md`):** §5.5 data model aligned
with §4.7/§7.3 (heartbeat and per-tenant state tables, no tenant ids readable by tenant sessions); every module's relay
heartbeats (PR-9); targetless `ReportConcern` and a legal-block column (§4.9); mobile cache revalidates after 120 s;
`no-store` everywhere; security-incident cases (§21); S2 naming rule and decision 23; zero-subject images available
(§8.4); DR safety log covers moderation, denies, switches and policy (§5.7); text revocation bound as decision 22; tests
without fakes (§14); stale text from earlier rounds cleaned.

**Round 5 — final pass (`community-gate/GAP-FINAL.md`):** the safety log has a bounded retention, a local outbox (never
blocks a safety action) and a reader rule (§5.7, PR-21); every post whose audience includes guardians or students at
school width or wider is reviewed before publication, official and trusted publishers included (K07 floor, §6.6, §8.4);
maintenance windows are declared by the deploy tooling with automatic expiry, and short blips cannot close tenants
(§7.3); B0 is split into B0a and B0b (§16.1).

---

## 1. The product in one page

A **school-run, closed social network**: think a controlled Instagram (photos, moments, albums) crossed with a
controlled Twitter (short updates, replies, topic spaces), where **the school decides every switch**.

- **Three different kinds of place, deliberately not one model** (owner decision of 18 Nov 2026):
  - **Class page** — one per class section per academic year, the *sacred* space of the people enrolled in it (its
    students, its teachers, and its students' guardians where the school allows). It is never a general space and never
    gains members by following or by hand. Its teacher may choose to **surface selected class posts in the school
    space**; by default nothing leaves the class page.
  - **School space** — the one general, common space of a school (plus the school-owned spaces under it: clubs,
    houses, staff room, events). Moderated **by that school's own moderation team**; each school moderates only itself.
  - **Inter-school space** — a space shared by several schools of the same tenant (a chain), with much tighter rules on
    what may be shared, by whom, and who controls it (section 19). Later: alumni batches.
- **Posts** are short text updates with up to five images (later: moments that expire, polls, showcases from the LMS,
  event recaps, recognition cards).
- **People see only what their verified school relationships allow**: a student sees their class, clubs and school; a
  parent sees what is addressed to *their child's* class, child by child; staff see their classes, clubs and staff
  rooms. Following is a preference, never a permission.
- **Every post has an explicit audience** shown before publishing, and **the narrowest audience is the default**.
- **Student and parent posts go through review** by default; staff can be trusted publishers for text; every image is
  confirmed by a second person.
- **Media is checked before anyone sees it**: type, size, metadata stripped, the depicted people's photo consent checked.
- **Reporting, moderation, freezing and appeals are built in**, not bolted on.
- **No popularity mechanics for children**: no follower counts, no public like counts on student posts, no streaks, no
  ranking, no autoplay; a "you're caught up" end to the feed.
- **Administrators hold the knobs** (section 4): on/off per institution, per role, per age band, per space type, per
  feature; a platform safety floor that no school setting can lower.

It lives inside the **`comms`** module, next to notices, events, galleries and the existing class pages and posts — the
same module already owns most of the building blocks (section 2).

---

## 2. What we reuse — checked against the code on 18 Nov 2026

| Need | Already in the codebase | State today | What the community needs from it |
|---|---|---|---|
| Class pages | `comms.class_page` (nullable `academic_year_id`, required `page_category_id`, no section, "class / club page that members follow"), `page_category`, `class_page_member` (`is_following`, `reaction like/dislike`), `class_page_attachment` | CRUD served; `FollowClassPage` 501 | **Reshaped** into the class page (one per section per year, §5.1); category, hand membership, follow, dislike and raw attachments **dropped** |
| School and inter-school spaces | — | new | `comms.community_space` beside class pages (section 5.1, 19) |
| Posts | `comms.post` (`class_page_id`, `visibility`, status `draft/pending/posted/cancelled`), `post_audience_class`, `post_comment` (with a raw `attachment_file_id`), `PostService` (approve/cancel, audit, outbox) | CRUD served; `SubmitPost`, `GetFeed` 501 | Revisions, audiences by rule, review workflow, feed; **generic CRUD removed** (§6.4) |
| Reactions | `comms.reaction` (post or comment, `is_saved`) | all reaction operations 501 | Reaction kinds set by policy; counts hidden for students |
| Events, galleries, magazines | `comms.event`, `event_gallery_item`, `magazine` | served | Event recaps and albums link to them |
| Notices | `comms.notice` (+ audience, versions) | served | Stay the *authoritative* channel; never mixed with optional social items (HOME-06) |
| In-app notifications, preferences | `in_app_notification` (stores `title`, `body`, `entity_type/id`; monthly partitions), `notification_*` tables | in-app reads 501 | Community rows store no rendered text (§11) |
| Audience groups | `audience_group`, `audience_group_member` | served | Reusable named audiences for staff publishers; snapshot ∩ live (§9.1) |
| Files | platform `file_object` (`storage_generation`, `status pending/available/quarantined/deleted`), signed upload/download | served; upload URLs single-use and size-bound, downloads pinned to the checked generation; **download URLs live 10 minutes (fixed)**; `DeleteFileObject` soft-deletes the row and deletes the *live* object only | Media pipeline (§8); platform needs a caller-chosen URL lifetime and a purge of all generations (§15) |
| Consent | `people.consent_record`: purposes `data_processing, photo_video_publication, public_profile, marketing, third_party_sharing, sms_whatsapp_messages, biometric_data`; one live row per (person, purpose); **no scope, decision or expiry column** | **5 consent operations 501**; no consent events | Schema and operations changed and built first (§15); comms keeps a projection |
| Identity, roles, settings | iam roles, `user_role` scoped grants (`tenant / institution / class_section / …`), `DataScope`, token `amr` (pwd/mfa), access tokens 15 min, refresh 30 days, impersonation | served | New community permissions (§10); MFA required for high-risk actions; impersonation refused for community writes and case reads |
| Relationships | sis `student_guardian` (`status active/inactive`, `has_custody`, `is_legal_guardian`, `receives_communication`; "a link alone does not grant data access; access is granted by iam.user_role"); sis's own persona rule reads only `status = 'active'` links of a guardian record tied to the account; `student_enrollment` (`effective_from/to`, `change_reason`); academics `class_section` (per academic year), `teaching_assignment`, `grade_level` (no age); hr `staff.status active/inactive/exited`; org `academic_year`, `academic_term` | served | Projected into comms (§7). **Events today are `<schema>.<table>.created/updated/deleted` with the id only** (sis student and hr staff carry a member payload: user, person, institution, row version — no status); §15 lists the payload changes |
| Guest access | `X-Tenant-Code` resolved through iam, guest projections (lms, careers) | served | Only for *approved public derivatives* later (P2); the community itself is never guest-readable |
| Platform plumbing | `AppTransactionManager`, outbox/inbox events (`EventEnvelope.time` = source commit time; the inbox handler runs **inside** the consumer's transaction), idempotency, gRPC clients, REST/gRPC parity kit, conformance rules, `FieldCipher` (`_enc` columns), per-module `application.properties` | served | Used as is; **no general scheduled-jobs runner exists** (only partition maintenance) |
| Moderation in lms | lms forum report categories, reports, moderation (hide/unhide/confirm abuse) | served | Same *shape* for community moderation; not shared code across modules (no cross-module dependency) |

**Dependency graph today (ARCHITECTURE §2):** `comms` may call academics, iam, org, platform and sis. It may **not**
call people, hr or lms. So consent and employment reach comms only as **events into local projections**. Because inbox
handlers run inside a transaction and calls are never made inside one, the owners' events must carry the state comms needs
(PR-5, PR-7 are hard prerequisites); where an id-only event remains (iam role grants), comms **marks the affected rows
stale (fail closed) and queues a refresh** that a job performs outside any transaction by calling the owner — §7.1. For people and hr, which comms cannot call, the events themselves
must carry the state (§15).

**The lesson from the lms review applies here first.** The lms batch shipped student write paths where the user id,
status and grading fields came from the request body. Community is the riskiest place to repeat that. Rule for this
feature: **members never write community rows through generic CRUD**; every member action is a domain command whose
author, audience and state come from the token and the policy (section 6).

---

## 3. Architecture at a glance

```
             web / phone / tablet clients (React, React Native)
                    │  REST (and gRPC for service callers)
                    ▼
   ┌──────────────────────── comms ─────────────────────────┐
   │ controllers/resolvers ─► CommunityCommands (one per aggregate)
   │                              │
   │                              ▼
   │  SwitchGate (uncached, 1 query) ─► CommunityAccess ◄── CommunityPolicy (effective knobs, cached by version)
   │          (one decision path for every read, write, count, media URL)
   │                              │
   │   repositories: home (class page / space), membership projection, post+revision,
   │   audience, media, comment, reaction, report/case, safeguarding, policy, consent projection
   │                              │
   │   jobs: projection refresh, reconciliation, dispatch, media processing, purge,
   │         digest, SLA escalation, retention, lag monitor
   └──────────┬──────────────────────────────────────┬───────┘
     events in│ (outbox/inbox)                 calls │ (gRPC, outside transactions)
   sis · hr · iam · org · people · academics    platform (files) · sis · academics · iam · org (refresh, high-risk checks)
```

- **One decision path** (`CommunityAccess`) decides every read and write; repositories never return rows that path has
  not admitted (the feed query embeds the same rules in SQL, section 9).
- **One switch gate** (`SwitchGate`) reads the emergency switches and policy versions uncached at the start of every
  request and every job item (§4.7).
- **One policy resolver** (`CommunityPolicy`) turns the layered settings into the effective knobs for a context.
- Everything else is the normal MySMS stack: one service per aggregate on `AppTransactionManager`, typed errors, RLS +
  `tenant_id` in every statement, outbox events, REST/gRPC parity.

---

## 4. The administrator's knobs — the control model

### 4.1 Layers and precedence

Settings resolve top-down; a lower layer may only **narrow** what the layer above allows (brainstorm §6.3):

1. **Platform safety floor** — code constants released with the software (§4.8), plus tightening values a platform
   administrator may set (never loosening). Not editable by tenants or schools.
2. **Tenant ceiling** — the trust/chain administrator (a tenant-level setting: per CLAUDE.md, only tenant administrators
   can see or set it; §4.6 says what a school sees of it).
3. **Institution policy** — the school's community owner.
4. **Space policy** — the space owner (for a class page: its class teacher and designated co-teacher, who hold
   `manage_spaces` for it), within the institution's limits.
5. **Post choice** — the author picks audience, comments on/off, within the space's limits.
6. **Member preference** — mute, digest, language; never widens anything.

**"Narrow" is defined per knob** by the order in §4.5 (its "safe direction"). For every knob, the effective value is
the safest of the values set at every layer, and a stored lower-layer value is validated on write to be at least as
safe as the effective value above it. "Lowest layer" in §4.2 means the most local layer allowed to *store* a value;
storing never widens beyond the layer above, whatever the knob's kind — so a teacher can only take names *off* the
institution's K05 list for their class page, never add any (§4.6).

Each layer is **versioned**: a change creates a new policy version (who, when, before/after, reason). A change that
widens the layer's own current value is a **proposal** and takes effect only when approved under §4.6 (CFG-08). The
effective decision records the policy versions it used, so "why could this parent see that post?" is answerable.

### 4.2 The knob catalogue

`F` = feature switch (on/off), `V` = value, `L` = list. "Lowest layer" = the most local layer allowed to store it.
Defaults are the **restrictive pilot defaults** ("Product A + small student pilot" in the brainstorm). "Phase" = when
the knob enters the catalogue; a knob not yet in the catalogue cannot be stored (422 `POLICY_UNKNOWN_KNOB`) and its
behaviour is fixed at the default.

| # | Knob | Kind | Default | Lowest layer | Phase | What it does / where enforced |
|---|---|---|---|---|---|---|
| **Activation** ||||||
| K01 | Community enabled | F | off | institution | S1 | Master switch per institution. Off = all community operations 404 for that institution; notices, homework etc. unaffected (HOME-07). Read uncached (§4.7). Turning on requires the §4.6 invariants |
| K02 | Enabled for role | L | staff (guardians from S2, when the school adds them) | institution | S1 (guardians S2) | Which member categories see Community at all: staff, students, guardians, alumni (later). `CommunityAccess` |
| K03 | Enabled for age band / grade levels | L | none for students | institution | S4 | Students only in the listed grade levels; age band from `community_grade_band` (§4.8), unknown = under-13 |
| K04 | Space types allowed | L | staff room (the school space and class pages always exist when community is on) | institution | S1 | Which school-owned space types may exist: club, house, staff room, event, parent group; inter-school and alumni need K46/K47 |
| K04a | Class pages may surface posts to the school space | V | off | institution | S3 | off / teacher's choice per post. Surfacing re-reviews and re-checks consent at institution width (§5.2.1) |
| K04b | Guardians on class pages | F | on | institution | S2 | Whether a class page's members include its students' guardians. Off = no guardian membership rows at all |
| **Publishing** ||||||
| K05 | Who may publish without review | L | staff with "community publisher" | institution (space may only remove entries) | S1 | Trusted publishers, **text only**, and never for posts the K07 floor sends to review (school-wide audiences with guardians or students); floor: never students or guardians; images always need the second-person confirmation (§6.5) |
| K06 | Who may submit posts | L | staff | institution (space may only remove entries) | S1 (guardians S2, students S4) | Students / guardians may submit; their posts always reviewed |
| K07 | Review required for | L | student, guardian posts; any audience wider than one space; **any post at institution width or wider whose audience includes guardians or students — whoever the author, official capacity and K05 included (floor)**; every image (second-person confirmation, §6.5) | space (may only add triggers) | S1 | Review triggers by author category, audience width, media present |
| K08 | Default audience | V | the space only | space | S1 | Composer default; narrowest available (NET-07); never wider than K09 |
| K09 | Widest audience allowed | V | the institution | institution | S1 | Ceiling: space → class sections → grade levels → whole school → chain (tenant layer). Applies at read time to published posts (§9.1) |
| K10 | Guardians in audience | F | on (class and school posts) | space (may only turn off) | S2 | Whether posts of that home reach linked guardians; applies at read time |
| K11 | Post types allowed | L | update, recap, event recap | space | S1 | update, recap, showcase, achievement, moment, poll, question |
| K12 | Scheduling allowed | F | on for staff | institution | S1 | Scheduled posts re-check eligibility at dispatch (POST-07, §6.6) |
| K13 | Editing after publish | V | staff only, creates revision, re-review if audience/media change | institution | S1 | Approved content cannot be swapped silently (POST-06) |
| **Media** ||||||
| K14 | Images allowed | F | on | space | S3 | Off = text only |
| K15 | Max images per post | V | 5 | institution | S3 | Floor maximum 10 |
| K16 | Video allowed | F | off (P2) | institution | P2 | Floor: off until the video pipeline exists |
| K17 | Photo consent check | F (floor: always on) | on | — | S3 | Depicted students need consent at the post's width (§8.4); not storable at any layer |
| K18 | Unidentified people in an image | V | block publish | institution | S3 | block / require reviewer confirmation of each unidentified adult (minors who are unidentified always block, §8.5) |
| K19 | Download original media | L | nobody (originals are deleted after processing) | space | P1 | Who may download originals once a scan provider exists; others view variants only |
| K20 | Storage allowance | V | per plan | tenant | S3 | Upload refused (422 `STORAGE_ALLOWANCE_USED`) when the allowance is used up |
| **Interaction** ||||||
| K21 | Comments | V | staff and guardians; guardian comments held outside K34 coverage; held for review if from students | space | S1 | off / staff only / members with review / members; outside coverage `members` behaves as `members_with_review` (§21) |
| K22 | Reply depth | V | 1 | institution | S1 | Floor maximum 2 (INT-03) |
| K23 | Reactions | L | appreciate, celebrate, helpful | institution | S1 | Allowed kinds from the floor set; never a dislike (INT-01) |
| K24 | Show reaction counts | V | staff posts only | institution | S1 | Floor: never on student-authored posts for under-13s (INT-06) |
| K25 | Mentions | V | off | space | S4 | Mention search lists only eligible members of the space (INT-04); protected subjects omitted (§7.7) |
| K26 | Polls | F | off | space | P1 | Advisory polls; respondent visibility stated (INT-07) |
| **Moments (stories, P1)** ||||||
| K27 | Moments enabled | F | off | institution | P1 | Image/text moments with expiry |
| K28 | Moment lifetime | V | 24 h | institution | P1 | 1–72 h |
| K29 | Viewer list visible | F (floor: off for students) | off | — | P1 | No "who viewed" for minors (STORY-07) |
| **Visibility and history** ||||||
| K30 | History for new members | V | this academic term | space | S1 | none / this term / this year / all — what a *student or guardian* joining a class page sees (MEM-07); staff rule in §7.4 |
| K31 | Archive after | V | academic year end | institution | S1 | Posts leave feeds and become read-only archive |
| K31a | Archive readable by former members for | V | 12 months | institution | S1 | 0–24 months after archiving; then staff-of-the-page only until deletion (§7.5) |
| K32 | Member directory in space | V | names only, staff see roles | space | S1 | off / names / names + photos; never contact details (MSG-04) |
| **Safety** ||||||
| K33 | Moderators | L | institution community owner + named moderators; on a class page also its class teacher and designated co-teacher (derived, §6.5) | space (adds to the institution's team) | S1 | Who reviews and confirms; at least two distinct persons per institution (§4.6); the implicated person is never a reviewer (§6.5) |
| K34 | Moderation coverage hours | V | school hours | institution | S1 | Outside coverage, submissions wait (never auto-published, SAFE-08); high-risk modes off (SAFE-16) |
| K34a | After-hours responders | L | the safeguarding lead and deputy | institution | S3 | Who is alerted outside K34 for image and urgent reports; must be set before images are enabled (§21) |
| K35 | Report categories | L | bullying, privacy, inappropriate image, impersonation, concern about staff, urgent help, other | institution (may only add) | S1 | Shown in the report form; routed by category (§10.2) |
| K36 | Safeguarding lead | L | — (must be set before community is enabled) | institution | S1 | Receives urgent and safeguarding reports; separate permission from moderators (SAFE-13) |
| K36a | Deputy safeguarding lead | L | — (must be set before community is enabled; a distinct person from K36) | institution | S1 | Covers absence and every case that implicates the lead |
| K36b | Independent escalation route | V | tenant safeguarding contact if the tenant names one, else the external contact text | institution | S1 | Where cases implicating the lead, the principal or the community owner go (§10.2) |
| K37 | Freeze posting | F | off | space / institution | S1 | Emergency: nobody posts or comments; reading and reporting continue (SAFE-11, CFG-12). Uncached (§4.7) |
| K38 | Slow mode | V | off | space | S4 | Minimum seconds between comments per member |
| K39 | Lock thread | F | off | post | S1 | No new comments |
| K40 | Rate limits | V | floor values | institution | S1 | Posts/comments/reports per member per hour; mention fan-out cap (SAFE-12) |
| K41 | Word filter | L | floor list + school additions | institution | S1 | Holds (never silently drops) matching content for review |
| **Notifications** ||||||
| K42 | Social notifications | V | in-app, daily digest | institution | S1 | Never a push per reaction (NOTIF-02); push/email only when providers exist |
| K43 | Quiet hours | V | 20:00–07:00 | institution | S1 | No social delivery inside; members may narrow further |
| **Sharing outside** ||||||
| K44 | Private links | F | off | institution | P1 | Link needs sign-in; generic preview only (EXT-02) |
| K45 | Public release | F (floor: off until P2) | off | institution | P2 | Approved public derivatives (brainstorm §15) |
| **Chain and alumni (P1)** ||||||
| K46 | Inter-school spaces | F | off | tenant | P1 | The tenant may allow inter-school spaces; each school still opts in per space (section 19) |
| K47 | Alumni network | F | off | institution | P1 | Separate verified network; never sees current students' spaces (ALUM-*) |

**Presets** (CFG-14) are named bundles of the values above: *Primary — family window*, *Secondary — guided
participation*, *Staff only*, *Chain bulletin*, *Alumni adults*. Applying a preset creates a policy version like any
other change (widening parts go through §4.6); the administrator sees the diff first.

### 4.3 How a knob is enforced (so none is "UI only")

Every knob has exactly one enforcement point on the server, listed in the table; the UI only *reflects* the effective
policy (hides a button, shows a reason). A test per knob proves the server refuses when the knob says no — the
component-test suite runs a matrix of role × knob value × action (section 14).

`CommunityPolicy.effective(institution, space, actorContext)` returns an immutable `EffectivePolicy` (values + policy
version ids). It is memoised in-process keyed by the **policy head versions** that `SwitchGate` reads uncached on every
request (§4.7), so a cached value is never used after a change has committed.

### 4.4 "Preview as" (CFG-07)

Administrators can preview the effective policy and a **synthetic** feed for a role and space ("a Grade 7 student in
7B", "a guardian of a 7B student") — computed from the policy and the audience rules over *published staff posts only*,
never by impersonating a real child or showing their private items.

### 4.5 Order and validation of every knob

"Safe →" gives the direction a lower layer may move (narrowing). A stored value outside the domain is 422
`POLICY_VALUE_INVALID`; less safe than the effective value of the layer above is 422 `POLICY_WIDER_THAN_PARENT` (the
message names no value of a tenant ceiling); beyond the floor is 422 `POLICY_BELOW_FLOOR`; a missing precondition is
422 `POLICY_PREREQUISITE_MISSING`; a widening of the layer's own current value is stored as a proposal (202,
`status: pending_approval`). "2nd" = widening needs a second approver (§4.6); "1" = proposer alone, audited and
announced to the tenant administrators.

| # | Type / domain | Order (safe →) | Write validation | Widening |
|---|---|---|---|---|
| K01 | bool | on → off | turning on: §4.6 invariants | 2nd |
| K02 | set ⊆ {staff, guardian, student, alumnus} | subset | student needs K03 non-empty and K36a; alumnus needs K47 | 2nd |
| K03 | set of grade-level ids of the institution | subset | ids must exist (academics); each must have a band (§4.8); needs K36, K36a, K34 | 2nd |
| K04 | set ⊆ {staff_room, club, house, event, parent_group} | subset | inter_school/alumni_batch never here (K46/K47) | 2nd |
| K04a | enum off < teacher_choice | left | needs K14 on | 2nd |
| K04b | bool | on → off | — | 2nd |
| K05 | set of role ids / staff categories | subset; space may only remove | floor: no student or guardian category; each role must exist | 2nd |
| K06 | set ⊆ {staff, guardian, student} | subset; space may only remove | student/guardian need K34 and K36a | 2nd |
| K07 | set of triggers {author:student, author:guardian, author:staff, width>home, wide_family_audience, media:any, media:students} | **superset** is safer | floor: {author:student, author:guardian, wide_family_audience, media:any} always present; K07's floor triggers override K05 and official capacity (§6.6) | 2nd (removing a trigger) |
| K08 | enum home < class_sections < grade_levels < institution < chain | left | ≤ effective K09 | 1 |
| K09 | same enum | left | ≤ tenant ceiling; chain only with K46 | 2nd |
| K10 | bool | on → off; space may only turn off | — | 2nd |
| K11 | set of post types | subset | moment needs K27; poll needs K26 | 1 |
| K12 | bool | on → off | — | 1 |
| K13 | enum none < staff_rereview < authors_rereview | left | floor: re-review on audience/media/meaning change always | 2nd |
| K14 | bool | on → off | — | 2nd |
| K15 | int 0–10 | lower | floor ≤ 10 | 1 |
| K16 | bool | — | floor: off (not storable before P2) | — |
| K17 | — | — | not storable (floor constant on) | — |
| K18 | enum block < confirm_adults | left | — | 2nd |
| K19 | set of role ids | subset | empty until a scan provider is configured | 2nd |
| K20 | bytes | lower | ≤ plan allowance | tenant only |
| K21 | enum off < staff_only < members_with_review < members | left | students in `members` need S4 + K38 available | 2nd |
| K22 | int 0–2 | lower | floor ≤ 2 (3 → 422 `POLICY_BELOW_FLOOR`) | 1 |
| K23 | set ⊆ floor set {appreciate, celebrate, helpful, thanks} | subset | anything outside the floor set → 422 | 1 |
| K24 | enum never < staff_posts < all_allowed | left | floor removes under-13 student-authored posts from any value | 2nd |
| K25 | enum off < staff_same_space < members_same_space | left | — | 2nd |
| K26 | bool | on → off | — | 1 |
| K27 | bool | on → off | needs K34 | 2nd |
| K28 | duration 1–72 h | shorter | floor ≤ 72 h | 1 |
| K29 | bool | on → off | floor: off where viewers include students | — |
| K30 | enum none < this_term < this_year < all | left ("this year" is wider than "this term") | — | 1 |
| K31 | enum term_end < year_end | left (earlier archive = less exposure) | — | 1 |
| K31a | months 0–24 | lower | floor ≤ 24 | 1 |
| K32 | enum off < names < names_photos | left | photos need K14 | 2nd |
| K33 | set of user ids (an assignment) | **no order**; adding moderators at space layer is allowed | each must hold `comms.community.review` in the institution; ≥ 2 distinct persons per institution | 1 |
| K34 | set of weekly windows | **subset** is narrower: fewer covered hours means more items wait or are held (§21) and fewer modes are allowed — less real-time review, never less control | windows inside 06:00–22:00 local | 1 |
| K34a | set of user ids (assignment) | no order | each must hold `comms.community.moderate_cases` or `.safeguarding`; ≥ 1 before K14 on | 1 |
| K35 | set of categories | **superset** is safer; space/institution may only add | floor categories always present | 1 |
| K36 / K36a | set of user ids (assignment) | no order | each must hold `comms.community.safeguarding`; K36a ≠ K36 persons; changes notified to tenant administrators and the platform contact | 2nd |
| K36b | enum tenant_contact / external_text (+ text) | no order | external text ≤ 300 chars, no URL outside the tenant's domains | 1 |
| K37 | bool | off → on is safer; any layer may freeze; only the freezing layer or above may unfreeze | — | unfreeze: 1 |
| K38 | seconds 0–3600 | higher | — | 1 |
| K39 | bool (post) | off → on | moderators only | 1 |
| K40 | ints per action/hour | lower | ≤ floor maxima | 1 |
| K41 | set of terms | **superset** is safer (school additions narrow) | floor list always present; ≤ 2,000 terms | 1 (removing a school term) |
| K42 | set ⊆ {in_app, digest, push, email} | subset | floor: no push/email for reactions; push/email need providers | 1 |
| K43 | window | **longer** is safer | members may only extend for themselves | 1 |
| K44 | bool | on → off | — | 2nd |
| K45 | bool | — | floor off until P2 | — |
| K46 | bool (tenant) | on → off | — | tenant admin, 2nd |
| K47 | bool | on → off | — | 2nd |

### 4.6 Widening approval, the two-person rule and preconditions

- **Who approves.** A proposal at the institution layer is approved by another holder of `comms.community.manage_policy`
  in that institution, or by a tenant administrator; at the space layer by the institution's community owner; at the
  tenant layer by a second tenant administrator. **Two-person rule:** approver ≠ proposer by user id **and** by
  `person_id` (one person with two accounts or two roles is one person), neither session impersonated, both sessions
  MFA (`amr` contains `mfa`). A school with a single administrator gets approvals from the tenant administrator; a
  tenant with a single administrator cannot widen a "2nd" knob — the proposal stays pending and COM-43 says why.
- **Bootstrap and one-administrator schools.** The **first** enablement of K01 for an institution is approved by
  platform onboarding (a platform operator acting as second approver for K01 only; no access to content), after a
  call with the school confirming the named people. Later "2nd" widenings follow the rule above; a single-administrator
  tenant uses platform onboarding as second approver for K01/K02/K03 only.
- **Minimum staffing** (stated to schools before they enable): two distinct staff persons — A and B — can satisfy every
  precondition: A = safeguarding lead + moderator, B = deputy + moderator, one of them in K34a. Any school, however
  small, needs those two willing people; a school without them cannot enable community.
- **Residual, stated:** the two-person rule rests on identity administration. An administrator who controls hr and iam
  could create a second person and account for himself. Out-of-band checks: platform onboarding approves the first
  enablement; every "2nd" approval and every change of K36/K36a/K34a is notified to the tenant administrators **and**
  the platform contact on file; a new person created within 30 days before acting as approver is flagged on COM-43.
- **Proposal lifecycle.** A proposal names the base version it was made against; one pending proposal per (layer,
  scope, knob) (a second → 409 `POLICY_PROPOSAL_PENDING`); the proposer may withdraw it; it expires after 7 days; on
  approval it is **re-validated** against the current parent, floor and preconditions (`POLICY_WIDER_THAN_PARENT` etc.)
  and, if the base version is no longer current, refused with 409 `POLICY_PROPOSAL_STALE` (the approver always sees
  the diff against the current active version).
- **In-flight items when a setting changes:** K07 or K05 change → applies to submissions made after it; items already
  in review stay in review (never released into publication by a policy change). K33 change → claims held by a removed
  reviewer are released at once; their pending confirmations return to the queue. K09/K10/K02/K04b narrowing → read
  time (§9.1), scheduled posts at dispatch (§6.6). K14 off → posts with images are hidden at read time (their text
  stays only if the author edits them, with review). K21 narrowing → existing comments stay; new ones follow. K37 →
  §4.9. Digests are built at send time and follow the current policy.
- **Announcement.** Every approved widening, and every "1" widening, creates an in-app notification for the tenant
  administrators (knob, institution, before/after, who).
- **Preconditions checked on the policy write itself** (not only in the UI), 422 `POLICY_PREREQUISITE_MISSING`:
  - turning K01 on: K36 and K36a set with two distinct persons who hold the safeguarding permission and active
    accounts; K33 resolves to at least two distinct persons; K34 set; K36b resolvable;
  - turning K14 (images) on: K34a set with at least one responder;
  - adding students to K02/K03/K06 or opening K27: K34 set, at least two moderators whose coverage includes the K34
    windows, participation consent available (PR-22);
  - removing a moderator or the lead: refused while it would break the rules above (the school must name a successor
    first); an `hr` exit or iam deactivation that breaks them freezes student posting for that institution (K37 on
    automatically, CFG-13) and notifies the community owner and tenant administrators.
- **What a school sees of the tenant ceiling** (CLAUDE.md: tenant settings are invisible to ordinary users): COM-40
  offers only the values the school may choose and marks the rest "not available for your school"; it never shows the
  tenant's value, who set it, its history or its reason. Tenant ceiling rows are readable only with
  `comms.community.manage_tenant_policy` (tenant administrators). Open decision 12 asks the owner to confirm that this
  minimum disclosure is acceptable.

### 4.7 Switches read on every request; caching; the platform kill switch

- **Uncached, every request and every job item:** the platform switch (`community_global_switch`: community off for
  all tenants, media delivery off, notifications off), the tenant switch (community off for the tenant), K01, K37 at
  institution and at each space in scope, the legal block, the caller's tenant's `tenant_source_state` rows (§7.3), the
  `source_heartbeat` rows and the policy **head version** per institution and space. One statement over
  these small tables with primary-key lookups: `SELECT … FROM comms.community_global_switch UNION ALL SELECT … FROM
  comms.community_switch WHERE tenant_id = :t AND scope_id = ANY(:scopes)` — estimated 0.1–0.3 ms; at 200 requests/s
  per school that is one index probe per request, negligible beside the feed. A feed or media request reads it once, not
  per item; the per-home values it needs are passed into the feed statement (§9.2).
- **Cached:** `EffectivePolicy` objects only, keyed by the head versions just read — so there is nothing to invalidate
  across nodes: a node that has not seen the new version simply misses its memo.
- **Reading the global row.** The database README's global-row policies show a tenantless row only to a transaction
  that binds no tenant, so a tenant transaction could not see `community_global_switch` under them. This table gets its
  own policies (PR-19): `global_switch_read FOR SELECT USING (true)` and writes only from a transaction that binds no
  tenant (`global_switch_write`, like `global_insert`); the README documents the third case; `smoke.sql` asserts that a
  tenant-bound session reads the global row and cannot write it. `source_heartbeat` (§5.5) uses the same read-all
  policy (it holds no tenant data); `heartbeat_mark`, which holds tenant ids, keeps the database README's standard
  global policies, so **no tenant session can read it** (§7.3).
- **In-flight writes:** every community write transaction reads its **tenant, institution and home** switch rows `FOR
  SHARE`; freezing updates them, so a freeze waits for writes already past their check and every later write sees it.
  The global row is not locked (it would serialise every tenant's writers on one row); a platform switch takes effect on
  the next request and at each job item's publish check.
- **Jobs:** dispatch, digest, notification and media jobs call `SwitchGate` per item inside the item's transaction and
  follow §4.9: frozen → scheduled posts returned to their author (§6.6), notifications continue; K01, tenant or platform
  `community_off` → scheduled posts returned and notifications dropped (not delayed); `media_delivery_off` → image posts
  returned; media processing continues but nothing is published. Purge, retention and erasure jobs do **not** consult
  the switches (only a legal block stops them). **Unfreezing never publishes a backlog** (CFG-12): scheduled items whose
  time passed while frozen stay returned.
- **Platform kill switch:** `SetCommunityGlobalSwitch` (platform operators only, MFA, reason required, audited) writes
  the global row, or a `community_switch` row for **one tenant or one institution** (`community_off`,
  `media_delivery_off`, `notifications_off`) — so an operator can stop one school's media without touching others and
  without reading any content; the tenant administrators and the school's community owner are notified. Effective on the
  next request on every node, no deployment.

### 4.8 The platform floor, age bands and floor changes

- **Floor values are code constants** in `CommunityFloor` (released and reviewed with the code): K16 off, K17 on, K22
  ≤ 2, K15 ≤ 10, K23 floor set, K24 never on under-13 student-authored posts, K29 off for students, K45 off, K05 never
  students/guardians, K07 floor triggers, K35 floor categories, the second-person image confirmation, consent widths,
  media limits (§8.3). **Loosening the floor needs a release.** PC-COM-01 shows them read-only with their reasons.
- **Tightening floor values** (e.g. a shorter media URL lifetime, lower rate-limit maxima, extra K41 terms) are data in
  `community_global_switch`/`community_floor_override`, set by platform operators with reason and audit; they can only
  be at or below the code constants.
- **Floor change against existing policies:** stored school values are never rewritten; the effective value is the
  safest of floor and stored value at read time, so a tightened floor applies at once; COM-40 shows "limited by the
  platform" on affected knobs.
- **Age band without date of birth:** `comms.community_grade_band(institution_id, grade_level_id, band under_13 |
  13_to_17 | adult_programme, set_by, set_at)`, set by the community owner per grade level (academics' `grade_level`
  has no age). A grade with no row is `under_13` (most restrictive). Students' bands come from their enrolment's grade;
  a student with no current enrolment has no student membership at all. The adult *person* status that consent needs
  comes from people's events (§8.4), not from the grade.

### 4.9 What each switch state allows

Safety operations stay available in every state; only a tenant-specific **legal block** stops them for that tenant
(`community_switch.legal_block` with `legal_reference`, set and cleared only by a platform operator acting on a
recorded legal order, MFA, audited).

| Operation class | Normal | Home/institution frozen (K37) | K01 off (institution) | Tenant `community_off` | Platform `community_off` | `media_delivery_off` | Source `degraded` | Tenant `closed` |
|---|---|---|---|---|---|---|---|---|
| Read posts and comments | yes | yes | no (404) | no | no | text yes, images no | text yes, images no | no (503) |
| Post, comment, react, surface, share | yes | no | no | no | no | text yes, images no | text only; nothing wider than the home | no |
| Targeted report (`ReportContent`) | yes | yes | no — targets are unreadable; use `ReportConcern` | no (same) | no (same) | yes | yes | no (same) |
| Targetless report (`ReportConcern`: urgent help, privacy, concern about staff, other) | yes | yes | **yes** | **yes** | **yes** | yes | yes | **yes** |
| Case handling, safeguarding reads and actions | yes | yes | yes | yes | yes | yes | yes | yes |
| `EmergencyTakedown`, `RemovePost`, `RevokeMemberAccess`, freeze | yes | yes | yes | yes | yes | yes | yes | yes |
| Purge, retention, erasure jobs and operations | yes | yes | yes | yes | yes | yes | yes | yes |
| Policy reads and proposals | yes | yes | yes | yes | proposals no | yes | yes | yes |
| Dispatch of scheduled posts | yes | returned | returned | returned | returned | image posts returned | retried, then returned | returned |
| Notifications and digest | yes | yes | none | none | none | yes (no images anyway) | yes | none |

`KillSwitchComponentTest` walks this table.

---

## 5. Data model (comms baseline, edited in place — nothing is released yet)

Names are proposals; every table has `tenant_id`, RLS `ENABLE + FORCE`, the tenant isolation policy, `row_version`,
audit columns and soft delete as the conventions require, except where §5.6/§5.7 say rows are hard-deleted (recorded in
the deletion ledger). No foreign keys out of the comms schema. A **home** is a class page or a community space; every
post, membership and switch names its home as `(home_kind, home_id)`.

### 5.1 Homes and membership

**Class pages and spaces are separate models** (owner decision of 18 Nov 2026). A post belongs to exactly one home.

**`comms.class_page`** (reshaped in the baseline) — **exactly one live page per class section**; academics'
`class_section` is itself per academic year, so this is one per section per year:

```sql
CREATE TABLE comms.class_page (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    academic_year_id uuid NOT NULL,
    class_section_id uuid NOT NULL,
    grade_level_id uuid NOT NULL,
    name varchar(150) NOT NULL,                 -- "Grade 7 B", copied from academics, refreshed by its events
    description text,
    cover_media_id uuid,                        -- a ready comms.community_media row, never a raw file
    surface_to_school varchar(20) NOT NULL DEFAULT 'off',
    status varchar(20) NOT NULL DEFAULT 'active',
    archived_at timestamptz,
    source_version integer NOT NULL,            -- academics.class_section row version last applied
    created_at timestamptz NOT NULL DEFAULT now(), created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(), updated_by uuid,
    deleted_at timestamptz,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_class_page PRIMARY KEY (id),
    CONSTRAINT uq_class_page_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT ck_class_page_surface CHECK (surface_to_school IN ('off', 'teacher_choice')),
    CONSTRAINT ck_class_page_status CHECK (status IN ('active', 'archived')),
    CONSTRAINT ck_class_page_archived CHECK ((status = 'archived') = (archived_at IS NOT NULL))
);
CREATE UNIQUE INDEX uq_class_page_section ON comms.class_page (tenant_id, class_section_id) WHERE deleted_at IS NULL;
COMMENT ON TABLE comms.class_page IS 'The class page of one academic-year class section: its enrolled students, teachers and (per policy) their guardians. Created and archived from academics/org events; never followed, never joined by hand.';
```

- Dropped from the baseline: `page_category` (and its CRUD), `class_page.owner_user_id`, `is_active`,
  **`class_page_member`** (its follow flag and like/dislike reaction), **`class_page_attachment`** (files reach a class
  page only as post media through the pipeline), `post_comment.attachment_file_id`, `post_audience_class`. Clubs and
  any other non-class page are `community_space` rows (`space_type = club`), never class pages. The baseline carries no
  seed rows for these tables (checked), so nothing migrates; the generated web/mobile clients lose the `ClassPageApi`
  member calls and `PageCategoryApi` when the OpenAPI document changes.
- Freeze is not a page status: it is a switch (§4.7). Pages are created by the `academics.class_section` handler and
  archived by the `org.academic_year` close event (§7.5).

**Surfacing a class post to the school space** — §5.2.1.

**`comms.community_space`** (new) — the school's common space and the school-owned spaces under it, plus
inter-school spaces (section 19):
`id, institution_id (owning school; for inter-school spaces: the host), space_type (school|club|house|staff_room|
event|parent_group|inter_school|alumni_batch), name, description, cover_media_id, owner_user_id, membership_rule
(all_school_members|invite|request_approval|participating_schools), audience_categories (students|guardians|staff),
status (draft|active|archived)`. Exactly one `school` space per institution, created when community is enabled.

**`comms.community_relationship`** — the **projection** of who someone is at school, fed by events (section 7):
`user_id, person_id, institution_id, kind (student|guardian_of|staff|teacher_of|alumnus), class_section_id,
child_person_id (guardian_of), grade_level_id, valid_from, valid_to, status (active|ended|stale), source_module,
source_record_id, source_version, link_status (guardian_of: active|restricted|inactive), has_custody, is_legal_guardian,
receives_communication, academic_year_id`. Unique on (tenant_id, source_module, source_record_id, kind). **A late or
duplicate event never resurrects an ended relationship** (compare `source_version`, ARCH-03). `stale` = an id-only event
arrived and the refresh job has not yet re-read the owner: treated exactly like `ended` by every read. `link_status`
`restricted` and `inactive` are both stored, but **no API, event or screen of comms exposes which one** applies; the
only output is "not a member".

**`comms.community_account`** — iam projection: `user_id, person_id, status (active|deactivated|stale),
source_version` plus `community_role_grant(user_id, institution_id, persona student|guardian|staff, valid_from,
valid_to, active)` from iam's `user_role` events (refreshed through iam's `ListUserRoles`).

**`comms.space_member`** — the materialised membership of every home (rules in §7.4):

```sql
CREATE TABLE comms.space_member (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    institution_id uuid NOT NULL,
    home_kind varchar(12) NOT NULL,
    home_id uuid NOT NULL,
    user_id uuid NOT NULL,
    context_child_person_id uuid,               -- guardian rows: the child this membership comes through
    member_category varchar(10) NOT NULL,
    member_role varchar(12) NOT NULL DEFAULT 'member',
    basis varchar(16) NOT NULL,
    relationship_id uuid,
    valid_from timestamptz NOT NULL,
    valid_to timestamptz,
    history_from timestamptz NOT NULL,          -- oldest post this member may read (K30, §7.4)
    history_to timestamptz,                     -- set when the member leaves or the home is archived
    archive_read_until timestamptz,             -- K31a, §7.5
    status varchar(10) NOT NULL,
    muted boolean NOT NULL DEFAULT false,
    last_seen_post_at timestamptz,
    ... audit columns, row_version,
    CONSTRAINT ck_space_member_home CHECK (home_kind IN ('class_page', 'space')),
    CONSTRAINT ck_space_member_category CHECK (member_category IN ('student', 'guardian', 'staff')),
    CONSTRAINT ck_space_member_role CHECK (member_role IN ('member', 'contributor', 'moderator', 'owner')),
    CONSTRAINT ck_space_member_basis CHECK (basis IN ('relationship', 'invite', 'staff_assignment')),
    CONSTRAINT ck_space_member_status CHECK (status IN ('active', 'ended')),
    CONSTRAINT ck_space_member_class_page CHECK (home_kind <> 'class_page' OR (basis = 'relationship' AND relationship_id IS NOT NULL)),
    CONSTRAINT ck_space_member_guardian CHECK ((member_category = 'guardian') = (context_child_person_id IS NOT NULL))
);
CREATE UNIQUE INDEX uq_space_member_live ON comms.space_member
    (tenant_id, home_kind, home_id, user_id, COALESCE(context_child_person_id, '00000000-0000-0000-0000-000000000000'))
    WHERE status = 'active' AND deleted_at IS NULL;
CREATE INDEX ix_space_member_user ON comms.space_member (tenant_id, user_id, institution_id, status);
```

Class-page rows are written **only** by `ClassMembershipDeriver` (the relationship handlers, the refresh job and the
nightly reconciliation); no operation writes them (§6.4). A guardian with two children in one class has two rows, one
per child context.

**`comms.community_access_deny`** — immediate local denies that need no projection: `user_id, scope (tenant|
institution|home|post|media), scope_id, reason_code (internal: revoke_now|link_restricted|account_deactivated|
emergency_takedown), source_kind (manual|sis_link|iam_account|takedown), source_ref, created_by, created_at,
lifted_at, lifted_by`. Read in every admission (§9.2); `reason_code` is never returned by any API.
**Lifecycle:** each row is lifted only by its own source — `LiftAccessRevocation` lifts `manual` rows,
`sis.student_guardian.restriction_lifted` lifts `sis_link` rows with that link as `source_ref`, `activateUser` (after the
refresh confirms) lifts `iam_account` rows, takedown rows are never lifted (the item is purged). Lifting one source never
lifts another: a guardian whose sis restriction ends but who was also revoked by hand stays denied until a person lifts
the manual row. Lifted rows are deleted after 1 year (§5.6). Index `(tenant_id, user_id) WHERE lifted_at IS NULL`;
expected live rows ≈ 1 % of members × their homes (≈ 130 per 1,500-student school), a handful per user.

**`comms.protected_subject`** — per-viewer suppression of a protected person (§7.7): `viewer_user_id,
protected_person_id, institution_id, source_kind (sis_link|manual), source_ref, created_at, lifted_at`; no reason.
Index `(tenant_id, viewer_user_id) WHERE lifted_at IS NULL`. **`comms.post_person`** — every person a published
revision involves: `revision_id, person_id, role (subject|credit|mention|author)`, written at approval (and for
comments `comment_id, person_id` for authors), read by the suppression predicate.

**`comms.event_parking`** — an event that references a parent comms does not know yet (an enrolment for a section whose
page does not exist, a teaching assignment for an unknown section): `source_module, event_id, waiting_for_kind,
waiting_for_id, received_at, deadline (24 h), attempts`. Parked events are retried when the parent arrives (or by the
refresh job calling the owner); past the deadline they alert. Parking never grants access: the rows they would create
do not exist until applied.

**Moderation is per school.** Every class page and school-owned space is moderated only by its own school's team
(community owner, moderators, safeguarding lead — K33/K36). No moderator of school A can see school B's queue, reports
or cases. Inter-school spaces have their own panel (section 19.4).

### 5.2 Posts

**`comms.post`** (reshaped): `institution_id, home_kind, home_id, academic_year_id, author_user_id,
author_person_id, author_institution_id, author_category, author_capacity (self|official|on_behalf_of),
represented_publisher_id (an `official_publisher` row: the school or an office), on_behalf_of_user_id, post_kind
(original|surfaced|inter_school_share), source_post_id, source_revision_id (surfaced/shared copies), surfaced_by_user_id,
origin_institution_id (inter-school), post_type, status (draft|submitted|in_review|returned|rejected|scheduled|
released_by_origin (inter-school)|published|withdrawn|restricted|removed|expired|archived), current_revision_id, published_revision_id,
approved_revision_id, approved_hash, audience_width (home|class_sections|grade_levels|institution|chain) with
`audience_width_rank` 1–5 for the read-time ceiling comparison, consent_width (class|institution|inter_school|public, §8.4),
scheduled_for, published_at, expires_at (moments), comments_mode, pinned_until, review_reason, claimed_by,
claimed_until, sla_due_at`. The old `class_page_id`, `visibility`, `body` and status values are replaced.

**`comms.post_revision`** (new, immutable): `post_id, revision_no, body, body_language, post_type, comments_mode,
scheduled_for, expires_at, author_capacity, represented_publisher_id, credits (person ids), content_hash, created_by,
created_at`, with `post_revision_media(revision_id, position, media_id, alt_text)` and its audience rows.

**The content hash** (ARCH-10) is SHA-256 over a canonical JSON of: `home_kind, home_id, post_type, body,
body_language, comments_mode, scheduled_for, expires_at, author_capacity, represented_publisher_id, credits` (sorted),
every media item in order as `{media_id, alt_text, variant_file_ids with their storage_generation and sha256, subjects
(person_id, confirmed) sorted, unidentified flag}`, and the audience rows (sorted) **including the audience-group
snapshot** (the user ids frozen at approval, §9.1). `pinned_until` and moderator flags are not content: they change only
through moderator commands that are audited on their own. Approval stores `approved_revision_id` and `approved_hash`;
publishing (now or at dispatch) recomputes the hash from the stored rows and refuses if it differs (409
`REVISION_CHANGED`). Media files cannot change behind an id: variants are written only by the comms job as new file
objects pinned to a generation, and their generation and digest are in the hash.

**Approve versus edit** (optimistic locking): `ApprovePost` carries `revisionId`, `contentHash` and `If-Match` (post
row version). The approval is one `UPDATE comms.post SET status = …, approved_revision_id = :rev, approved_hash = :hash,
row_version = row_version + 1 WHERE id = :id AND row_version = :expected AND current_revision_id = :rev AND status =
'in_review'`; an edit inserts a revision and bumps `current_revision_id` and `row_version` in one transaction. Whichever
commits second gets 0 rows: an approver who lost gets 412 and sees the new revision; an editor who lost gets 412 and the
approved state; a second approver gets 409 `INVALID_STATE_TRANSITION`.

**`comms.post_audience`** (replaces `post_audience_class`): rule rows per revision —
`effect (include|exclude), kind (home|class_section|grade_level|institution|user), target_id, category
(student|guardian|staff|any), via_group_id`; an `audience_group` choice is compiled at approval into `user` rows (the
snapshot) carrying `via_group_id`, which the read uses for the live half of the rule. Semantics in §9.1. The composer
previews the audience in words before publishing (NET-06; numbers per §9.3).

**`comms.post_comment`** (reshaped): `status (visible|held|hidden|removed), parent_comment_id (depth ≤ K22),
held_reason, body`; no attachment. **`comms.reaction`** (reused): `kind` limited by K23 (the old `reaction` column
renamed `kind`), one active row per (user, target); `is_saved` kept for bookmarks.

#### 5.2.1 Surfaced copies

Surfacing is done only by the class page's class teacher or designated co-teacher (its moderators, §6.5) or the
community owner, per post,
when K04a and the page's `surface_to_school` allow it. `SurfaceClassPost(classPostId)` creates a **separate post** in
the school space (`post_kind = surfaced`, `source_post_id`, `source_revision_id` = the class post's published
revision, its own audience defaulting to the school space, its own comments and reactions). It **always** enters the
school's review queue (a text-only class post can name a child in free text), and
every depicted student needs consent at **institution** width (§8.4). Student authors and credited students appear as "a student of 7B"
unless `community_naming` consent at institution width exists (§8.4).

The copy is visible only while **all** hold, evaluated in the read statement itself (no job):

| Source change | Effect on the copy | How fast |
|---|---|---|
| Source gets a new published revision | copy hidden (`source.published_revision_id ≠ copy.source_revision_id`); the teacher may re-surface the new revision (new review) | next read |
| Source withdrawn, restricted, removed or expired | copy hidden; a removal also withdraws the copy (`status = withdrawn`) in the same transaction | next read |
| A comment on the class post reveals something | no effect: comments never travel | — |
| A depicted or credited student's consent no longer covers institution width | the whole copy is hidden (§8.1 step 6); the class post too if class width is also gone | as soon as the consent state is recomputed (same transaction as the event, §8.4) |
| Class page archived | copy follows the school space's own archive (K31) | — |
| Surfacer leaves or is demoted | copy stays (it was approved by the school's review); responsibility passes to the page's current teachers and the community owner, who may withdraw it | — |

States: `in_review → published → withdrawn`, with `returned/rejected` from review; "hidden" is computed, not a state.

### 5.3 Media

**`comms.community_media`**: `uploader_user_id, home_kind, home_id, draft_post_id, original_file_id,
original_generation, status (requested|uploaded|processing|ready|rejected|failed|withdrawn|purged), attempts,
next_attempt_at, width, height, frame_count, variant_thumb_file_id/_generation/_sha256, variant_feed_…, variant_full_…,
rejection_code, unidentified_people (none|adults|minors), processed_at, withdrawn_at, purged_at`. State machine in §8.2.

**`comms.media_subject`**: `media_id, person_id (depicted student or staff), declared_by, declared_at, confirmed_by,
confirmed_at` — the consent result is not stored here: it lives in the precomputed `media_consent_state` /
`post_consent_state` (§8.4), rewritten in the same transaction as the consent or relationship event, so a withdrawal
applies at once.

### 5.4 Safety

**`comms.community_report`**: `target_kind (post|comment|media|space|member), target_id, home_kind, home_id,
category, reporter_user_id (visible only to the case owner and deputy), about_user_ids (who the reporter says it is
about), detail_enc, severity, status (open|triaged|actioned|dismissed|escalated)`.
**`comms.moderation_case`**: groups reports; `institution_id, owner_user_id, deputy_user_id, implicated_user_ids,
route (routine|staff_concern|safeguarding|independent), severity, sla_due_at, outcome, reason_code, policy_version,
legal_hold`. **`comms.moderation_action`**: `case_id, action (hide|restrict|remove|warn|suspend_member|lock_thread|
freeze_space|restore|revoke_access), actor, reason_code, created_at` — append-only.
**`comms.moderation_appeal`**: author's appeal, independent reviewer, outcome.
**`comms.member_restriction`**: `user_id, scope (space|institution), kind (mute_posting|review_all|suspended),
valid_until, case_id` — write-side only; reading is never removed by a restriction (§10.3).
**`comms.evidence_record`**: the reported revision's body (encrypted, `body_enc`), and for media **only the SHA-256
of each variant, the post/revision/media ids, the declared subjects and the review trail — not the pixels**;
`case_id, evidence_class (routine|safeguarding), retain_until, hold_reason_code, hold_review_at, pixels_retained`.
Pixels are kept only when the safeguarding lead explicitly retains them under a legal hold (`RetainEvidencePixels`,
reason required, safeguarding cases only): then a platform copy in the community bucket under `owner_entity_type =
comms.evidence`, readable only through `GetSafeguardingEvidence`, every access logged.
**Safeguarding apart:** `comms.safeguarding_case` (detail columns `_enc` with `FieldCipher`, bound to tenant and column;
outcome, referral and statutory fields only here), `comms.safeguarding_access_log` (append-only: who, which case or
evidence, which operation, purpose code, when — every read, including list views). Only the operations of §10.2 read
these tables; `ExportCommunityAudit`, `GetCommunityHealth` and ordinary case operations never join them (counts only).

### 5.5 Policy, switches, consent, notifications

**`comms.community_policy_version`**: `layer (tenant|institution|space), scope_id, version_no, values jsonb (validated
against §4.5), status (pending_approval|active|rejected|superseded), proposed_by, proposed_person_id, approved_by,
approved_person_id, reason, effective_at` with **`comms.community_policy_head(layer, scope_id, active_version_no)`**.
**`comms.community_switch(tenant_id, scope_kind tenant|institution|space|class_page, scope_id, community_off, frozen,
media_delivery_off, notifications_off, legal_block, legal_reference, set_by_kind school|platform, changed_by, reason,
changed_at, version)`** (a switch set by a platform operator can be cleared only by a platform operator) and the global
**`comms.community_global_switch(switch, value, changed_by, reason, changed_at)`** (no `tenant_id`; written only by
platform operators in a transaction that binds no tenant; readable by tenant transactions through its **own**
read-all policy, PR-19 — the database README's standard global policies would hide it, §4.7).
**`comms.community_grade_band`** (§4.8).
**`comms.consent_projection`** (from people events, §8.4): `consent_record_id, person_id, given_by_person_id,
giver_capacity (self|guardian), purpose (photo_video_publication|community_naming|community_participation|
public_profile), scope (class|institution|inter_school|public; null for participation), decision (granted|refused), valid_until, verification, withdrawn_at, source_version`, and
**`comms.person_status(person_id, adult, source_version)`**; the precomputed `media_consent_state` and
`post_consent_state` are described in §8.4.
**Revocation machinery (§7.1, §7.3):** `comms.source_heartbeat(module, last_heartbeat_at, maintenance_until)`
(tenantless, read-all policy, no tenant data); `comms.heartbeat_mark(module, stream, tenant_id, last_seq, seen_at)`
(tenantless table under the README's standard global policies — visible only to transactions that bind no tenant, so
no tenant session can see other tenants' ids or activity); `comms.stream_position(tenant_id, module, stream,
last_contiguous_seq, pending_seqs, gap_since)` and `comms.tenant_source_state(tenant_id, module, state
ok|degraded|closed, reason gap|silence|checkpoint|failing, since)` (tenant RLS; `SwitchGate` reads the caller's tenant's
rows on every request). **`comms.projection_refresh(source_module, source_record_id, requested_at, attempts)`** (§7.1). **`comms.safety_log_outbox(seq, entry_kind, entry jsonb, created_at, shipped_at)`** (§5.7).
**Read state:** `space_member.last_seen_post_at`; saved posts reuse `reaction.is_saved`.
**Notifications:** reuse `in_app_notification` (community rows carry `entity_type/entity_id` and a type code; `title`
and `body` are left empty and rendered at read time, §11) and `notification_preference`.

### 5.6 Retention

Defaults below; "range" is what an institution may set within the tenant ceiling (owner and legal to confirm, open
decision 10). Every deletion is recorded in the deletion ledger (§5.7).

| Data class | Default | Range | Who may change | Method |
|---|---|---|---|---|
| Drafts | 90 days after last edit | 30–180 d | institution | hard delete (row + media purge) |
| Returned / rejected submissions | 90 days after decision | 30–180 d | institution | hard delete |
| Published posts, comments, reactions | live until K31 archive; archive kept 2 academic years | 0–5 y | institution | hard delete of revisions, comments, reactions; media purge; post row reduced to a tombstone (ids, home, dates) |
| Withdrawn / removed posts (no case) | media purged at withdrawal; rows 30 days | fixed | — | hard delete |
| Surfaced copies | as the school space; withdrawn copies 30 days | — | — | hard delete |
| Expired moments (P1) | visible copy 7 days after expiry | fixed | — | hard delete + purge |
| Media originals | deleted as soon as variants are ready | fixed (K19 later) | — | purge all generations |
| Orphan uploads (never attached to a submitted post) | 24 h | fixed | — | purge |
| Reports without a case (dismissed) | 1 year after closure | 6 m–3 y | institution | hard delete; counts kept |
| Routine moderation cases, actions, appeals, routine evidence | 3 years after closure (evidence 1 year; **routine evidence holds no pixels**, §5.4) | 1–7 y | institution | hard delete; reason-code counts kept |
| Safeguarding cases and evidence | legal hold; reviewed every 12 months by the safeguarding lead; without a recorded reason, 7 years after closure | per hold | safeguarding lead (hold), tenant (ceiling) | hard delete + purge after hold ends |
| In-app notifications (community) | 90 days | fixed | — | partition drop |
| Digest records | 30 days | fixed | — | hard delete |
| Policy versions and approvals | life of the institution's community + 3 years | fixed | — | hard delete |
| Relationship and membership projection, ended rows | until the archives they gate are deleted | — | — | hard delete |
| Consent projection, withdrawn rows | 30 days (people keeps the record and evidence) | fixed | — | hard delete |
| Deny rows (lifted) | 1 year | fixed | — | hard delete |
| Safeguarding access log, deletion ledger | 7 years | fixed | — | hard delete |
| Off-lineage safety log (PR-21) | 45 days (backup window + 10) | fixed | — | bucket expiry after the retention lock; shipped `safety_log_outbox` rows deleted after 7 days |
| Application logs (no personal data, §13.12) | 30 days | ops | — | log store expiry |
| Database backups | 35 days point-in-time | ops | — | expire; a deleted row survives in backups ≤ 35 days (they hold no image bytes) and is never restored into service except by full disaster recovery, after which the off-lineage safety log is replayed (§5.7) |
| Community media objects | dedicated bucket (PR-11): object versioning **off**, soft delete **off** (retention 0), excluded from the daily file-backup copy | ops (bucket settings), platform (startup check) | — | a purged object is unrecoverable at once; if operations cannot disable soft delete, the bucket's actual retention (GCS default 7 days) is read by platform and used in every statement to guardians (§12.7) |

### 5.7 Deletion, purge and the ledger

- `comms.deletion_ledger(id, entity_kind, entity_id, reason (retention|withdrawal|erasure|takedown|orphan),
  request_ref, file_ids, generations_deleted, rows_deleted, requested_at, completed_at, recoverable_until, verified_at,
  failure)`.
- **Where the bytes live.** Platform today uses one bucket (`MYSMS_FILES_BUCKET`) for all files, and GCS versioning and
  soft delete are **bucket-level** settings; the cost plan's recommended configuration also copies files daily to a
  Coldline bucket. Community media therefore goes to a **dedicated bucket** (PR-11: a second platform storage disk,
  `storage_disk = gcs-community`, `MYSMS_COMMUNITY_FILES_BUCKET`) configured with object versioning **off**, soft
  delete **off**, no lifecycle copy and **excluded from the daily backup copy** (losing community media in a disaster is
  accepted; it is not a record). Evidence pixels, when a safeguarding hold retains them (§5.4), live in the same bucket under the evidence owner type. At startup
  platform reads the bucket's metadata (versioning, soft-delete retention) and exposes it through `GetStoragePolicy`;
  comms refuses to enable images for a tenant whose community disk reports versioning on.
- **Purge procedure** for a media item: (1) in a transaction: `community_media.status = withdrawn` (URLs stop at once,
  §8.1), ledger row `requested`; (2) outside any transaction: platform `PurgeFileObject` for the original and every
  variant — deletes the object with an `ifGenerationMatch` precondition, then lists the key's versions and deletes any
  that exist (none, with versioning off; a found one means misconfiguration and is reported), refunds quota, writes
  `platform.deletion_log` (§15); (3) transaction: ledger `completed`, `status = purged`, `recoverable_until` = now +
  the bucket's soft-delete retention (now, with the recommended setting); (4) verification pass (next run): describe of
  the key and of each recorded generation must answer not found → `verified_at`; a failure stays in the ledger and
  alerts. A pinned signed URL is pinned to a generation: once that generation is deleted the URL answers 404 (soft-deleted
  objects are not served), so pinning does not keep content alive.
- **Safety log outside the backup lineage.** A database restore to time T would undo everything after T — deletions,
  and also moderation and access decisions. So every safety-relevant change is also recorded (ids, kinds, values, times;
  no content). **Write path:** the action writes a `comms.safety_log_outbox` row in its **own transaction** (so the log
  can never block `EmergencyTakedown`, `RevokeMemberAccess` or any other safety action), and a job ships unshipped rows
  in order to an append-only object log outside the database backups (PR-21), marking them shipped; shipping lag is a
  metric (target < 1 min, alert > 5 min). **Retention:** the bucket's retention lock is the backup window plus a margin
  — **45 days** (35-day point-in-time backups + 10) — after which objects expire; only entries newer than the oldest
  restorable backup are ever needed, and an open-ended lock would keep (guardian, protected child) pairs and moderation
  history beyond erasure. **Protection:** objects are encrypted with a per-tenant key (FieldCipher envelope) and readable
  only by the replay job's service identity and two named platform operators; every read is logged. Entries cover: completed ledger entries (purges), takedowns and media withdrawals, moderation actions (hide, restrict,
  remove, lock, restrictions and their lifting), deny and protected-subject rows (created, lifted), switch changes
  (freeze, community off, media off, legal block), and policy-version activations. **After any database restore**,
  community stays `closed` for the restored tenants until a replay job has re-applied every log entry newer than T in
  order (purges re-run; actions, denies, protected subjects, switches and policy versions re-written as they were);
  people's consent replay and the sis/iam/academics reconciliation have run; stream positions are reset from fresh
  snapshots; and an operator has checked the replay report, including the `safety_log_outbox` rows that were
  **unshipped at the moment of failure** (the bounded loss — at most the shipping lag — listed so the operator can
  re-apply them from incident records). Then community reopens. The DR runbook (DEPLOYMENT) gets
  this step.
- **Rows:** content rows (revisions, comments, reactions, media, subjects) are hard-deleted by the purge and retention
  jobs after their soft-delete grace (30 days for author/moderator removals, none for erasure and takedown); the post
  row keeps a tombstone. Notifications that point at purged items are deleted with them.
- No CDN sits in front of community media; signed downloads are served `Cache-Control: no-store` through the stable media path (§8.1).

### 5.8 Erasure of a person's community data (PRIV-09)

The identity-verified request and its accountable owner live in people (not built anywhere yet, §15); comms provides
the discovery and the action for its own data, run by a holder of `comms.community.erasure` (the school's data
protection owner):

- **Discovery** (`DiscoverPersonContent(personId, nameVariants[])`, institution-scoped, rate-limited, audited): posts
  and comments authored by the person's accounts; `post_revision.credits`; `media_subject`; mentions; reactions and
  saves; reports **made by** the person; reports and cases **about** them; evidence records; surfaced and inter-school
  copies of any of these; and a body text search of posts and comments in the institution for the supplied name
  variants (comms stores no names; the operator types them, they are not stored after the request).
- **Rule per location:** content authored by the child → removed and purged; reactions/saves → deleted; images where the
  child is a subject → image removed from the post and purged (the rest of the post stays); credits and mentions →
  removed; others' comments and posts naming the child → the name redacted by a moderator revision ("[removed]"), the
  rest kept; surfaced and inter-school copies → follow the source; reports the child made → kept while the case is open
  or under hold, then deleted; cases and evidence about or involving the child → kept under the evidence rules, the
  exception recorded on the request ("kept for safeguarding obligations", no detail to the requester).
- `ApplyErasure(requestRef, selections)` executes and writes the ledger; the request's exceptions are returned to
  people's workflow as a list of record kinds and counts.

---

## 6. Operations (the API) — commands, not CRUD

All operations are REST + gRPC with parity, in comms' contract interfaces, with operation-table rows and permissions.
**Author, actor, institution and tenant always come from the token; status only from commands.** Request bodies carry
*what* the user writes, never *who* they are. Community writes and case reads are refused under impersonation (403
`IMPERSONATION_NOT_ALLOWED`).

### 6.1 Member-facing

| Operation | What it does | Key refusals |
|---|---|---|
| `GetCommunityHome` | Homes the caller belongs to in the current context, unread counts (§9.3), pinned official items | 404 when community off for the caller's context (K01–K03) |
| `GetFeed` (exists, 501) | Merged feed of the caller's homes in the selected context, newest first, cursor-paged, "caught up" | — |
| `GetHomeFeed` | One class page's or space's posts | 404 out of membership |
| `GetPost`, `ListComments` | One post; its visible comments | 404 not admitted |
| `ListMyHomes`, `ListClassPages`, `GetClassPage`, `GetSpace` | Homes and their rules (effective knobs as plain sentences) | 404 out of membership |
| `CreatePostDraft`, `UpdatePostDraft`, `DeletePostDraft` | Author's drafts | 422 over limits; 403 post type not allowed |
| `SubmitPost` (exists, 501) | Draft → submitted / scheduled / published | 409 not draft; 422 `AUDIENCE_EMPTY`, audience wider than allowed; 422 media not ready / `IMAGE_NOT_SHAREABLE` |
| `WithdrawPost` | Author takes a post down | 409 already removed |
| `ListMySubmissions` | Author's own drafts, pending, returned with reasons (POST-05) | |
| `RequestMediaUpload`, `CompleteMediaUpload`, `GetMediaStatus` | Through platform files; binds the media to the author's draft | 422 type/size/allowance |
| `DeclareMediaSubjects` | Who is in each photo and whether anyone else is identifiable. Picker: on a class page, the section's **roster** from `community_relationship` student rows (students need not be members); in a school space, the rosters of the teacher's own sections plus an "another class" path searching the institution's current students (at least 3 characters, 30 searches per teacher per day, every search logged with who, when and the result count, counts shown in COM-45); names fetched from sis for display at picker time (comms stores none) | 404 person not a current student or staff of the institution |
| `GET /v1/comms/media/{mediaId}/{variant}` | **Stable media path**: authenticated; runs admission and consent; answers 302 to a fresh signed URL (`no-store`); clients cache by this path | 404 |
| `GetMediaUrls(mediaIds[])` | Batch of signed URLs for a feed page; counts as one call for rate limits | 404 items omitted |
| `AddComment`, `EditComment`, `DeleteComment` | Comments within K21/K22 | 403 comments off; 429 slow mode; 409 locked |
| `React`, `RemoveReaction` | One reaction per member per target (replaces the reaction CRUD set) | 422 kind not allowed |
| `SavePost`, `UnsavePost` | Personal bookmarks | |
| `MuteHome`, `UnmuteHome`, `SetCommunityPreferences` | Preferences only (digest, quiet hours, hide previews, hide community for myself) | |
| `ReportContent` | Report a post, comment, image, member or space; includes "concern about staff" and "urgent help"; needs a target the reporter can still read — otherwise `ReportConcern` (§4.9) | 429 report flooding |
| `ReportConcern` | **Targetless** report (urgent help, privacy, concern about staff, other) with free text and an optional child context; works in every switch state (§4.9), reachable from the home card, from the "Community is unavailable" and 404 screens and from COM-07; routes to the safeguarding lead and deputy (or the independent route) | 429 flooding |
| `GetMyReport` | Outcome as far as the reporter may know (SAFE-09) | |
| `AppealModerationAction` | Author appeals | 409 appeal already open |
| `ListInAppNotifications`, `GetInAppNotification`, `MarkNotificationsRead` (exist, 501) | Community and other in-app rows, rendered after an access check | |
| `GetMyChildMediaUse(childPersonId)` | For the consent centre: how many admitted items show this child, by width | 404 not the caller's child |
| `VoteInPoll` (P1), `ViewMoment` (P1) | | |

`SubmitPost`, `AddComment`, `CompleteMediaUpload`, `React`, `ReportContent`, `ReportConcern` and `SurfaceClassPost` declare an optional
`Idempotency-Key` (core's filter), so a double tap on a poor connection does not duplicate.

### 6.2 Staff and moderators

`ListReviewQueue`, `ClaimReviewItem`, `ApprovePost` (exists — revision- and hash-bound), `ConfirmMediaSubjects`,
`ReturnPost` (with reason), `RejectPost`, `PublishOnBehalf`, `SurfaceClassPost`, `WithdrawSurfacedPost`,
`PinPost`/`UnpinPost`, `HideComment`/`RestoreComment`, `RemoveMemberComments`, `LockThread`, `SetSlowMode`,
`RestrictPost`/`RemovePost`/`RestorePost`, `EmergencyTakedown`, `UpdateClassPage` (description, cover,
`surface_to_school`), `CreateSpace`, `UpdateSpace`, `ArchiveSpace`, `AddSpaceMembers` (invite-type **spaces** only),
`RemoveSpaceMember`, `ListCases`, `GetCase`, `AssignCase`, `ActOnCase`, `EscalateCase`, `DecideAppeal`,
`RestrictMember`, `LiftRestriction`, `RevokeMemberAccess`/`LiftAccessRevocation` (deny rows), `ListAuthorActivity`
(a member's posts, comments and reviews of the last 30 days, for incidents). `OpenSecurityIncident(targetUserId, reasonCode)` — one step, any moderator or the community owner: opens a case of
route `security` (not safeguarding) for a suspected compromised or misused account, and is the case those actions cite.
**`ListAuthorActivity`, `RemoveMemberComments`, `RestrictMember` and `RevokeMemberAccess` require an open case id** (or, for revocation, a
safeguarding case or the sis-link source) and are logged to that case.
Safeguarding (§10.2): `ListSafeguardingCases`, `GetSafeguardingCase`, `GetSafeguardingEvidence`,
`RecordSafeguardingAction`, `ReviewLegalHold`.

### 6.3 Administrators and platform

`GetCommunityPolicy`, `ProposePolicyChange`, `ApprovePolicyChange`, `RejectPolicyChange`, `ApplyPreset`,
`PreviewAsRole`, `FreezeInstitution`/`UnfreezeInstitution`, `FreezeHome`/`UnfreezeHome`, `SetGradeBands`,
`ListPolicyHistory`, `GetCommunityHealth` (aggregates only, §13.12), `ExportCommunityAudit` (§10.2),
`DiscoverPersonContent`, `ApplyErasure`, `GetDeletionLedger`; platform operators: `SetCommunityGlobalSwitch`,
`SetFloorOverride`, `GetProjectionHealth`.

### 6.4 What stays and what goes

**Deleted from the contract, the operation table and iam's permission catalogue** (with their permissions):
`CreateClassPage`, `DeleteClassPage`; `Create/Get/List/Update/DeleteClassPageMember`; `Create/Get/List/Update/
DeleteClassPageAttachment`; `FollowClassPage`; `Create/Get/List/Update/DeletePageCategory`; `CreatePost`,
`UpdatePost`, `DeletePost`, `CancelPost`, `ListPosts`; `Create/Get/List/Update/DeletePostAudienceClass`;
`CreatePostComment`, `GetPostComment`, `ListPostComments`, `UpdatePostComment`, `DeletePostComment`;
`Create/Get/List/Update/DeletePostReaction`.

**Retained with new semantics:** `GetClassPage`, `ListClassPages` (both through `CommunityAccess`, caller's homes
only), `UpdateClassPage` (only `description`, `cover_media_id`, `surface_to_school`; caller must be the section's class teacher or
designated co-teacher, or the community owner, with `comms.community.manage_spaces`), `GetPost` (through `CommunityAccess`), `ApprovePost`
(`comms.community.review`, revision-bound), `SubmitPost`, `GetFeed`, the in-app notification reads.

**No administrator tool edits `post.status` or membership directly.** Every state change is one of the commands above,
each of which: loads under its permission scope (404/403), checks the transition guard (409), binds revision and hash,
writes `moderation_action` or the audit transition, and publishes its event in the same transaction.

**Enforced by a build test:** `CommunityWriteSurfaceTest` (comms, plain JUnit over
`META-INF/mysms/operations/comms.json`) holds the exact allowlist of write operations (POST/PATCH/PUT/DELETE) on the
community resources (`class_page`, `community_space`, `space_member`, `post`, `post_revision`, `post_audience`,
`post_comment`, `reaction`, `community_media`, `media_subject`, reports, cases, policy, switches) with the permission
each requires; any new write operation on them, or a changed permission, fails the build until the allowlist and this
section are changed together. A second test bans **actor-identity and state fields** in every community request DTO — `authorUserId`,
`actorUserId`, `userId`, `createdBy`, `tenantId`, `institutionId` (taken from the context header),
`publishedRevisionId`, `approvedHash`, `basis` — and allows target fields only where an allowlist names the operation:
`targetUserId` (`RestrictMember`, `RevokeMemberAccess`, `AddSpaceMembers`, `ListAuthorActivity`, `AssignCase`
(`assigneeUserId`)), `subjectPersonIds` (`DeclareMediaSubjects`), `memberRole` (`AddSpaceMembers`), `status` never
(state moves only through named commands); and
the class-page membership test (§14) tries every write operation against a non-enrolled user.

Estimated contract (all slices to S4): **≈ 108 operations** (S1 ≈ 60, S2 ≈ 25, S3 ≈ 15, S4 ≈ 8 — §16),
≈ 37 tables new or reshaped, ≈ 35 event types in, ≈ 8 out.

### 6.5 Review: who may approve, subject confirmation, single-moderator schools

- **Eligible reviewers** of an item = holders of `comms.community.review` in the home's institution who are moderators
  of the home (K33), **minus** the author (by person, so a second account does not help), anyone credited, mentioned or
  declared as a subject in it, any guardian of a student subject, and anyone in `implicated_user_ids` of an open case
  on it. Computed server-side at claim and at approval; the UI only reflects it.
- **Self-approval is impossible**, including for trusted publishers: K05 lets a trusted publisher *skip editorial
  review* for **text only**; K05 never covers images.
- **Second-person image confirmation (floor): every image, at every width, whatever the declaration** — including
  "no students" and including class/space covers and directory photos — needs `ConfirmMediaSubjects` by an eligible
  reviewer other than the uploader. The confirmer attests either "no child is identifiable in this image" or the
  subject list (and the unidentified flag). Consent is then evaluated for the confirmed subjects. So a declaration of
  "no students" never skips the look or the check: the confirmer sees the children and corrects the list. Open decision
  11 asks whether to relax this at class width after the pilot.
- **Who confirms on a class page:** its class teacher and the designated co-teacher (teaching assignments carry
  `teacher_role`; academics' `class_teacher_staff_id` names the class teacher) are the page's moderators and hold
  `review`; other subject teachers are **contributors** (view and post, no review) unless the school designates them.
  K33's school moderators can confirm anywhere in the school. This reconciles K33 with §10.1.
- **Workload, sized:** a 50-section school with one photo post per section per school day → 50 confirmations a day;
  with consent state precomputed beside each subject (§8.4) the confirmer checks identity only, ≈ 1–2 minutes each →
  50–100 minutes a day spread over ≈ 100 class/co-teachers, i.e. one or two a day each, because a co-teacher of the
  same class confirms. A "photo day" composer lets a teacher submit up to 10 posts in one go and a confirmer step through
  them one image at a time (no bulk approve).
- **Subjects from another class:** the picker is the roster path of §6.1 (own sections, then "another class"); the
  confirmer must be an eligible reviewer of the home where the image is posted.
- **Tiny staff:** enabling community requires two distinct moderator persons (§4.6). If at a given moment no eligible
  reviewer exists (the only other moderator is the subject's parent, or absent), the item stays `submitted`, the queue
  shows it as "needs a reviewer from outside". A **text-only** item is offered to the **tenant community moderators** (a
  tenant-scoped grant of `comms.community.review` the tenant administrator may give: MFA session, every grant and every
  review audited and listed to each school's community owner in COM-46). **Items with images of students are never
  offered outside the school**: consent at class or institution width does not cover staff of other schools; such an
  item waits for a school reviewer, and the author may remove the image to publish the text. If nobody eligible exists,
  the author sees "No reviewer is available at your school right now". Nothing publishes by default.

### 6.6 Queue states, SLA, official posting and scheduled dispatch

- **Queue states:** `submitted` → `in_review` (claimed by one reviewer for 30 minutes; the claim expires back to
  `submitted`) → `published`/`scheduled` | `returned` (reason) | `rejected`. `sla_due_at` = submission + 4 covered hours
  (K34). At breach: deputy moderator (K33 order), then the community owner, then (safeguarding category) K36a; in-app
  plus home card; nothing is published by a breach (SAFE-08).
- **Author view:** "Waiting for review. Reviews happen Mon–Fri 08:00–16:00; you'll hear back after that." A Friday 16:00
  submission is reviewed Monday.
- **Official and on-behalf posting:** `PublishOnBehalf` and `author_capacity = official` need
  `comms.community.publish_official` (community owner, principal) and an MFA session. They skip editorial review only for
  text whose audience is staff only, or that stays within a class page; **any official or trusted (K05) post at
  institution width or wider whose audience includes guardians or students is reviewed before publication** by an
  eligible reviewer other than the author (K07 floor — it overrides K05 and official capacity), so an undeclared student
  name in a school-wide post is caught before guardians see it; **the consent check and the second-person image
  confirmation always apply** (K17 floor). Any official post to the chain needs a second approver as well. Every
  official text post that skipped review goes to a non-blocking **second-reader** queue (read within 24 covered hours by a moderator who is not the poster and does not
  report to them, or by a tenant moderator; the reader can restrict it). The real author
  (`author_user_id`) is always stored and shown to staff and case handlers; members see the represented publisher with
  an "Official" mark and "posted by a member of staff", and only accounts granted that publisher can produce the mark.
- **Scheduled dispatch checklist** (job, per post, `SwitchGate` first, then one transaction): (1) platform/tenant
  switches and K01 on; (2) no freeze on institution or home; (3) author still an active member of the home with the
  permission that published it, account active, no `member_restriction` and no deny row; for audience wider than the
  home, the author's iam account state is re-read over gRPC first (outside the transaction; ARCH-06); (4) recomputed
  hash = `approved_hash`; (5) audience ≤ current K09, K02/K10 allow its categories; (6) every image: the precomputed consent
  state at the post's width is available and not dirty (§8.4), confirmation present, media `ready`; (7) the tenant's
  `tenant_source_state` is `ok` for sis, iam and people (§7.3).
  Any failure → `returned` with a coded reason, nothing published (fail closed); a check that cannot be answered (iam
  timeout, projection degraded) → retried at 15-minute intervals for 2 hours, then returned. The author's notification
  says only "Your scheduled post was not published — open it to see what to change"; inside the app the reason is from a
  fixed list (`IMAGE_NOT_SHAREABLE` for any consent, link or restriction cause on a subject, `HOME_FROZEN`,
  `AUDIENCE_NOT_ALLOWED`, `NO_LONGER_ALLOWED_TO_POST`, `CHANGED_SINCE_APPROVAL`) that never says why an image is not
  shareable.

---

## 7. Relationships, events and revocation

### 7.1 Inbound events (owners publish, comms projects)

Event names follow the codebase (`<schema>.<table>.<step>`); payload changes the owners must make are in §15.

| Event | From | Payload comms needs | Effect in comms |
|---|---|---|---|
| `sis.student_enrollment.created / updated / deleted` | sis | member payload + `classSectionId, academicYearId, enrollmentStatus, effectiveFrom, effectiveTo, changeReason, rowVersion` (§15) | student relationship rows; class-page membership re-derived |
| `sis.student_guardian.created / updated / deleted` | sis | `id, studentId, studentPersonId, guardianId, guardianUserId, guardianPersonId, institutionId, status, hasCustody, isLegalGuardian, receivesCommunication, rowVersion` (§15) | guardian_of rows; membership re-derived |
| `sis.student_guardian.restricted / restriction_lifted` (new) | sis | same payload, **no reason** | §7.7 |
| `academics.class_section.created / updated / deleted` | academics | PR-7 payload (hard for S1): `institutionId, academicYearId, gradeLevelId, sectionId, classTeacherStaffId, isActive, rowVersion` | class page created/renamed; class teacher → page moderator; inactive section → page archived |
| `academics.teaching_assignment.*` | academics | PR-7 payload: `classSectionId, staffId, teacherRole, isActive, rowVersion` | teacher_of rows → contributor, or moderator for the designated co-teacher |
| `org.academic_year.closeAcademicYear`, `org.academic_term.*`, `org.institution.updated/deleted` | org | PR-7 payload with dates and institution `status` | archive class pages (§7.5); term calendar for K30; an institution whose `status` becomes inactive → K01 forced off for it (other institution edits change nothing) |
| `hr.staff.created / updated / deleted` | hr | member payload today (user, person, institution, row version); §15 adds `status` | staff rows; `exited`/`inactive` end staff membership and moderator duties (CFG-13) |
| `iam.user_account.deactivateUser`, `…activateUser`, `iam.user_role.*` | iam | id-only | deactivate → deny row and `community_account.status = deactivated` **in the handler** (no data needed); others → refresh via iam `GetUserAccount` / `ListUserRoles` |
| `people.consent_record.recorded / withdrawn / superseded / expired / snapshot` (new) | people | §8.4 | consent projection |
| `people.person.majority_reached`, `people.person.status_snapshot` (new) | people | `personId, adult, rowVersion` | `person_status` |
| `<module>.relay.heartbeat` (new, every 30 s) | sis, iam, people, hr, academics, org | `module, time, streams: {name: {tenantId: lastSeq}}` | `source_heartbeat`, `heartbeat_mark`; per-tenant state by the comparison job (§7.3) |
| `people.consent.checkpoint`, `hr.staff.checkpoint` (new, daily per tenant) | people, hr | `tenantId, rows, maxRowVersion, digest` | drift detection (§7.3) |
| `lms.showcase.proposed` (P1) | lms | | draft showcase post for the student's review |

**Handling an event.** Handlers are idempotent (inbox), compare `source_version`, and never call another module (they
run inside the inbox transaction). An event with the state it needs is applied directly, and rows are re-derived
**only when a membership-relevant field changed** (status, custody, legal guardian, section, effective dates, active
flag, user/person link) — editing a section's room or capacity changes nothing. Because an id-only event would force
"stale = ended" on every edit (a whole class losing its page on a capacity change, thousands of rows on a role import),
**state-carrying payloads are hard prerequisites** for the slices that use them (PR-5 sis, PR-7 academics/org, §15.1);
iam's `user_role` events stay id-only, and for them the refresh marks only the affected grant stale, not the person's
memberships, and refreshes in bulk (PR-20). Where an id-only event remains, the projected rows from that source record
are marked `stale` (ended for every read) and refreshed by the job outside any transaction (1 min, 5 min, 15 min, then
hourly) — a member can lose access for that long, never gain it.

**Early events and rollover order.** An event referencing a parent comms does not know yet is parked
(`event_parking`, §5.1) and applied when the parent arrives. Rollover runs in this order regardless of arrival order:
new-year sections create new pages (inactive until the year's start date); enrolments and assignments for those
sections attach to them (parked if early); `closeAcademicYear` archives the old year's pages; at the new year's start
date the new pages become active. Membership rows reference section ids, so old and new years never mix. The
term-start burst (a whole school's enrolments and assignments in an hour) is part of the heavy-tier load test (§14).

**Detecting a single lost event — per tenant, tolerant of reordering.** The streams that can revoke access are
sequenced at the source (PR-9): `people.consent`, `people.person_status`, `sis.guardian_link`, `sis.enrollment`,
`hr.staff`, `iam.account`. The source's outbox relay stamps each published event of such a stream with `tenantSeq`, a
gapless counter per (module, stream, tenant) kept in `<schema>.event_stream(tenant_id, stream, last_seq)` and advanced
in the relay's own transaction only when the publish succeeds (rollbacks leave no holes; a redelivery repeats the same
number and the inbox drops the duplicate). Its heartbeat (every 30 s) carries, per stream, the `last_seq` of every
tenant that published on that stream in the last 30 minutes. comms keeps `comms.stream_position(module, stream,
tenant_id, last_contiguous_seq, pending_seqs, gap_since)`:
- events may arrive **out of order** (Pub/Sub has no default ordering; RabbitMQ with prefetch 10, several consumers or a
  requeue reorders): an event beyond `last_contiguous_seq + 1` is applied normally and recorded in `pending_seqs`; the
  position advances when the hole fills;
- a hole still open after a **reorder window of 2 minutes**, or a heartbeat mark ahead of the position for 2 minutes,
  is a **gap for that tenant only** — detected ≈ 2.5 minutes after the loss;
- a gap fails closed **for that tenant**: consent/person-status gap → no media URL with student subjects and no
  publication with student images in that tenant; guardian-link gap → that tenant's guardian contexts answer 503;
  enrolment/staff/account gap → that tenant's source state `degraded` (§7.3). Other tenants are untouched;
- **repair is automatic** for callable owners: the refresh job re-reads that tenant's guardian links, enrolments or
  accounts through the bulk reads (PR-20) and closes the gap (minutes). For people and hr (not callable) comms publishes
  `comms.stream_repair.requested {module, stream, tenantId, fromSeq}`; if the owner approves the event-only
  subscription (open decision 19), people/hr answer with a replay of that tenant's snapshots automatically; otherwise the
  on-call operator is paged to run `people.ReplayConsentEvents` / `hr.ReplayStaffEvents` for that tenant. Until repair
  the tenant's fail-closed state holds: a lost event costs that tenant availability, not privacy.
- Sequencing is required on both buses; ordering is not (the reorder window covers it).
The daily checkpoint (people, hr) stays as a second net for drift that sequencing cannot see (a source-side bug that
never published). Tenantless events (heartbeats) are handled as the inbox already handles global events — not recorded,
idempotent handler (the position only moves forward) — and write only tenantless comms tables under the policies of §4.7.

**Reconciliation.** Nightly, comms re-reads sis, academics and iam through the bulk reads (PR-20), and org through its
per-item reads (small volume), for each institution with community on, and repairs drift (counted as `community.projection.repairs`). For people and hr, which comms cannot call, the daily
checkpoint is compared with the projection; a mismatch sets that source's state for the tenant to `degraded` (§7.3) and
alerts; the operator runs the owner's replay (`people.ReplayConsentEvents`, `hr.ReplayStaffEvents`, §15), after which
the checkpoint matches again.

### 7.2 Outbound events

`comms.post.published`, `comms.post.removed`, `comms.media.ready`, `comms.report.created` (safeguarding category →
notification to the lead), `comms.policy.changed`, `comms.member.restricted`. Payloads carry ids and versions only —
never body text, names or reasons (ARCH-09).

### 7.3 Revocation barrier (ARCH-05/07)

- **Every read re-checks** the relationship's `status`/`valid_to`, the member row, deny rows, protected subjects, the
  consent state and the post's state *in the same statement* (§9.2). A projection can therefore only be stale in the *restrictive* direction once the
  revocation has reached it; the questions are how fast it reaches it and what happens when it does not.
- **Lag metric:** `community.projection.enforcement_lag_seconds{source}` = comms commit time of the projection change −
  `EventEnvelope.time` (the source's commit time). Refreshes add their own time, so the metric is source change to
  enforcement, not delivery. Targets: p95 ≤ 60 s; warn when p95 > 60 s for 10 min; page when any event > 300 s.
- **Heartbeats** detect silence: each source's outbox relay publishes `<module>.relay.heartbeat` every 30 s (§15). For
  each of the six sources — sis, iam, people, hr, academics, org — comms keeps `tenant_source_state` (from `source_heartbeat` and the marks, below) (hr, academics
  and org affect staff and class membership; their `degraded` stops publishing and image delivery only for decisions
  that depend on them):
  - `ok` — last heartbeat < 2 min old and no delivered event failing for more than 3 min;
  - `degraded` — heartbeat older than 2 min (heartbeats travel the same exchange and consumer queue as the source's
    other events, so a backed-up queue shows as an old heartbeat), or a delivered event still failing after 3 min, or a
    failed checkpoint: **no
    media URLs are issued, nothing is published, surfaced, dispatched or shared**, reading text continues;
  - **How per-tenant state is derived:** the heartbeat handler runs in a transaction that binds no tenant and writes
    only `source_heartbeat` and `heartbeat_mark`. Every 30 s a comparison job reads the marks in such a tenantless
    transaction, then, **per tenant, in a transaction bound to that tenant**, compares them with that tenant's
    `stream_position` and writes `tenant_source_state`. Tenant ids never pass through a table a tenant session can read.
  - **Guardian contexts close sooner** (default pending open decision 22): when sis or iam is `degraded` for 15 min,
    every guardian context of the affected tenants answers 503 (staff contexts continue), so a guardian restricted while
    sis is silent keeps text access for ≤ 17 min, not an hour;
  - `closed` — **per tenant**: a tenant whose stream positions have an unrepaired gap for 15 min, or a source silent for
    60 min, answers 503 `COMMUNITY_UNAVAILABLE` for community reads (the home card shows "Community is unavailable",
    HOME-07); notices, learning and every other module are unaffected. A **maintenance window** keeps `degraded` (no images, no
    publishing) but suspends guardian closing and `closed` until it ends. It is declared **by the deploy tooling**, not
    by hand: the deployment pipeline and the broker-restart runbook (DEPLOYMENT) call `SetProjectionMaintenance(module,
    until)` with a service identity holding only `comms.community.declare_maintenance`, before stopping a module or the
    broker; `until` is at most 30 minutes ahead and the window **expires automatically** (a pipeline that finishes
    early clears it). A heartbeat that stops **without** a declared window pages the on-call at once.
  - **Short blips close nothing:** `degraded` needs the heartbeat to be older than 2 minutes on two consecutive
    comparison runs, and guardian closing needs 15 minutes of `degraded`; a broker restart of a minute or two is
    invisible, a longer undeclared one stops images and publishing for every tenant (fail closed — the stream says
    nothing then), and only a source silent for 15 minutes outside a window closes guardian contexts across tenants,
    which is decision 22's intended trade-off.
  So the worst case for a revocation that reaches nobody is bounded: ≤ 2 min before degrade (photos stop), ≤ 17 min
  before guardian contexts close (open decision 22), ≤ 62 min before all community reads stop outside a maintenance
  window. A single event lost while heartbeats still flow is caught by the stream sequence
  (§7.1) within ≈ 2.5 minutes and fails closed for that tenant; the nightly reconciliation and the daily checkpoint remain as second nets.
- **High-risk actions** (publishing or surfacing wider than the home, enabling students, consent-dependent dispatch,
  public release) check the owner's current answer over gRPC when the owner is an allowed callee (sis, academics, iam):
  800 ms deadline, one retry; a circuit breaker opens after 5 failures in 30 s and stays open 30 s; open breaker,
  timeout or error → the action is refused (503 `COMMUNITY_CHECK_UNAVAILABLE`), never allowed.
- **Emergency invalidation:** `RevokeMemberAccess` writes a deny row immediately (no projection involved); the
  projection catches up later; `LiftAccessRevocation` removes it.
- **Test:** `RevocationUnderBrokerFailureComponentTest` (fast tier with configured short thresholds, §14; heavy tier
  with a paused RabbitMQ container and the production thresholds) stops heartbeats, asserts `degraded` (media 503,
  publish 503), then guardian contexts closed, then `closed`, restarts, replays, and asserts a guardian link ended during
  the outage is enforced before `ok` returns.

### 7.4 Membership rules and mid-year changes

**Class-page membership is the AND of every source; any source saying no wins.** sis says a link grants no access by
itself; iam says a role grants no child reach by itself; each is necessary, none sufficient. Rows exist only while:

| Member | Conditions (all required) |
|---|---|
| Student S | K01 on; K02 includes students and S's grade band is in K03 (S4); a `student` relationship active today for the page's section (`enrollment_status = enrolled`, `effective_from ≤ today < effective_to`); S's account active in iam with an active student role in the institution; **participation consent**: for a minor (people's `adult` flag false or unknown — the legal threshold, 18 in India) a live `community_participation` consent from a current legal guardian (PR-22); for an adult, their own acceptance in COM-11. The grade bands (`under_13` …) govern age-appropriate design only, never consent |
| Guardian G of S | K01 on; K02 includes guardians; K04b on; S has an active enrolment in the page's section (S need not be a member: students are not members before S4); the link row active (`status = active`, not restricted, not deleted); `has_custody = true` (non-custodial guardians: open decision 8, default no membership); G's guardian record has an account (`guardianUserId` set); that account active; an active guardian role in the institution; no deny row |
| Teacher T | K01 on; K02 includes staff; an active `teacher_of` (teaching assignment of a subject of the section) or class-teacher assignment; hr status `active`; account active with a staff role in the institution |

`is_legal_guardian` does not decide membership; it decides who may **give consent** (§8.4). `receives_communication =
false` keeps membership but suppresses that guardian's community notifications (it is a channel choice; a guardian who
must not see the class is a restriction, §7.7).

**Per event** (effective time = the source's effective date where it has one, else the commit time):

| Event | Membership | History visible | Drafts and pending submissions |
|---|---|---|---|
| Student transfers 7B → 7C (`section_change`, effective date d) | 7B row ends at d; 7C row starts at d; same for each of the student's guardians (per child context) | 7B: nothing after d (the student and guardians lose 7B entirely; their own past 7B posts stay on 7B). 7C: student and guardians see 7C posts from `history_from` = start of the term containing d (K30 `this_term`; `none` = from d; `this_year` = year start; `all` = page creation) | drafts and pending submissions in 7B → `withdrawn` (author told "kept in My submissions for 30 days; it can no longer be shared in 7B"); nothing moves to 7C |
| Student withdrawn from school (`left`, left_on d) | all student and guardian rows of that child end at d | none after d; archives per §7.5 only if they were members at archive time — a leaver was not | withdrawn as above |
| New teacher assigned to 7C | teacher row starts at the assignment | **staff rule:** the current academic year of the page (teaching continuity), never earlier years' archives | — |
| Teacher reassigned away from 7B | teacher row ends | none after; their posts stay | their drafts in 7B withdrawn; review items they claimed released; cases reassigned (CFG-13) |
| Guardian link added mid-year | guardian row starts | same K30 rule as a new student | — |
| Guardian link ended / restricted | rows end at once | none | §7.7 |
| Account deactivated in iam | all rows suspended at once (deny row) | none | drafts kept, unusable until reactivation |
| hr exit | staff rows end | none | as teacher reassigned; iam deactivation is the hard stop |

**Consent and new members.** Photo consent is given for an *audience width* (class, institution, chain), not for named
viewers. A teacher who joins 7C in October is inside the class width the guardians consented to; no per-viewer consent
exists or is implied. This is stated in the consent text the guardian sees (§12.7).

**Leavers and iam.** iam deactivation is the primary hard stop: comms acts on the `deactivateUser` event at once (deny
row); if that event were lost, the account's access token still expires within 15 minutes and iam refuses the refresh
(verified in iam's code and `IamTokenGrantsComponentTest`, PR-8), so the bound for a deactivated account is 15 minutes even with every comms
event lost. An hr exit without iam deactivation is bounded only by the hr event or the daily checkpoint; §15 therefore
requires hr's `ChangeStaffStatus` to `exited` to deactivate the iam account. The terminated-staff test asserts: after
`exited`, class pages, media URLs and the review queue refuse the teacher within the lag window with events flowing,
and within 15 minutes with every event to comms dropped.

### 7.4a Membership of the other spaces

`SpaceMembershipDeriver` (handlers, refresh, nightly reconciliation) derives rows for every space except invite-type ones:

| Space | Staff | Guardians | Students |
|---|---|---|---|
| **School space** (one per institution) | every staff member with hr status `active` (part-time included; `inactive`/on leave = ended) and an active staff role in the institution | one row **per child context** for each child with an active enrolment in the institution, under the §7.4 guardian conditions (active link, custody, account, role); K02 must include guardians; non-custodial: no (open decision 8); **no row at all** while the guardian has any `protected_subject` in the institution (§7.7) | S4, under the §7.4 student conditions, K03 |
| **Staff room** | as the school space | never | never |
| **Club / house / event** (P1) | invite (`AddSpaceMembers`) by the space owner, from school-space members only | invite, from school-space members only, per child context; protected-subject rule as for the school space | invite, P1 with K03 |
| **Parent group** (P1) | owner and moderators | invite, per child context | never |
| **Inter-school** | §19.5, from the school's own categories | chain bulletin read-only if the school allows; the protected-subject rule applies | P1 curated only |

A member of an invite-type space loses it automatically when they lose school-space membership.

### 7.5 Year rollover and archives (MEM-07)

- `org.academic_year.closeAcademicYear` → every class page of that year becomes `archived` (read-only, out of feeds);
  memberships get `history_to` = the archive time and `archive_read_until` = archive time + K31a.
- **Who reads an archived page:** exactly the people who were members **at archive time**, only posts from their own
  `history_from` to `history_to`, until `archive_read_until`; after that only the page's last-year teachers and the
  community owner, until retention deletes it (§5.6). A student promoted from 7B to 8B reads 7B's archive (they were in
  7B); a student who joined 8B from another school does not; guardians of a student who left before archive time do
  not; consent, deny rows and restrictions apply to archive reads and archive media exactly as to live ones (same
  `CommunityAccess` path, tested by `ArchiveAccessComponentTest`). The retention clock starts at archive time.
- New year's sections create new pages (empty); nothing is copied forward.
- **Merge or split mid-year:** academics creates new sections; each new section gets a new page; the old pages are
  archived at the section's end date with the same archive rule; no content moves.

### 7.6 People in several institutions (NET-18)

- A **context** is (institution, persona, child for guardians). The token carries the institutions; the client picks a
  context; every community request carries `X-Community-Context` (institution id + persona + child person id), which
  `CommunityAccess` validates against the caller's own relationships (404 if not theirs).
- `space_member` rows carry `institution_id`; every read filters by the context's institution, so a teacher at two
  campuses or a guardian with children in two schools never gets school A's homes, counts, notifications or mention
  candidates while in school B's context. Inter-school spaces are reached only from the context of the member's own
  participating school.
- **Authoring identity:** the composer shows "Posting as Ms Rao · Teacher · Campus North · to Grade 7 B"; the post
  stores `author_institution_id`; a post can only be created in a home of the current context's institution.
- Test: `MultiInstitutionIsolationComponentTest` (one teacher in two institutions, one guardian with a child in each).

### 7.7 Restricted guardian links and protected subjects (MEM-06, PRIV-08)

The trigger lives in sis (§15): an authorised sis user with `sis.student_guardian.restrict` records the restriction
through `sis.RestrictGuardianLink` (reason, legal reference and evidence are stored only in sis's restricted table);
sis sets `status = restricted` and publishes `sis.student_guardian.restricted` with the ordinary payload and no reason.
Ending the link alone is not enough: the guardian may still reach the child through a sibling's context or the school
space, where posts can show or name the child. So the restriction creates a **protected subject**.

In comms, in the handler's transaction:
- the guardian_of relationship rows from that link → `ended` (`link_status = restricted`, never exposed), membership
  rows of that child context end, deny rows for each home reached through that child;
- a `protected_subject(viewer = the guardian's user, protected = the child)` row (`source_kind = sis_link`);
- **the guardian loses every school-wide space of the institution** — the school space, clubs, houses, events,
  inter-school spaces — in every child context (§7.4a), because free text in a school-wide post can name the child and
  cannot be detected. They keep the class pages of their other children (that child's sacred space), where the
  suppression below applies;
- **suppression everywhere the guardian still reads:** a post is not admitted for that viewer if its published
  revision involves the protected person in any role (`post_person`: subject, credit, mention, author), including
  surfaced copies (through their source revision) and inter-school posts; comments authored by the protected child are
  not shown; notifications about such items are not created and existing ones are archived; media URLs are refused;
  member lists and mention candidates omit the child. Free text naming the child on a sibling's class page is the
  residual (a moderator may remove it on request);
- the guardian's in-app community notifications about the lost homes → archived and their entity reference cleared
  (§11.3); digests are built at send time;
- saved posts of those homes are deleted; drafts and pending submissions of the guardian there → `withdrawn`;
- the guardian's past comments stay as they are (hiding them would itself signal something); moderators may remove them
  with `RemoveMemberComments` (case-bound, §6.2);
- consents that guardian gave for the child stop counting at once (§8.4) — shown to staff only as `IMAGE_NOT_SHAREABLE`;
- the guardian sees the child disappear and the school-wide spaces disappear with no message; comms has no other output.

**Belt and braces without the screen step.** sis cannot call comms (ARCHITECTURE §2), and a UI step run by a registrar
without comms permissions is not a control, so the earlier "Revoke community access now" step is removed. Instead,
comms (which may call sis) reads every restricted link of each institution with community on every 10 minutes through
`sis.ListRestrictedGuardianLinks(institutionId)` (PR-4) and applies any it has not seen; with the per-tenant stream
sequence (§7.1) this bounds a lost event to ≈ 2.5 minutes, and an unreachable sis to the 10-minute read.

**Lifting:** `restriction_lifted` lifts the `sis_link` deny and protected-subject rows of that link; manual rows stay
until a person lifts them. `ProtectedSubjectComponentTest`: a guardian with A restricted and B in another class —
asserts no school-space membership, no post involving A in B's class page, no surfaced copy or inter-school post
involving A, no comment by A, no notification about A, media URLs refused, and B's class page otherwise unchanged.
Already-delivered push notifications cannot be recalled; they contain no class or child (§11.1).

---

## 8. Media pipeline

### 8.1 Steps

1. **Upload:** `RequestMediaUpload` → platform signed upload URL (single-use, size-bound, generation-pinned). Allowed
   declared types: `image/jpeg`, `image/png`, `image/webp`; ≤ 15 MB. The object lands under a quarantine key nobody
   can download.
2. **Complete:** `CompleteMediaUpload` → platform `CompleteUpload` (size, type and SHA-256 checked; quota spent) →
   `community_media(status = uploaded)` → processing queued.
3. **Process (job):** see §8.3. Produces three variants (thumb 320 px, feed 1080 px, full 2048 px long side) as new
   platform file objects uploaded by the comms service identity, then **purges the original** (§5.7).
4. **Subjects and consent:** §8.4, §8.5.
5. **Delivery:** clients address an image by its **stable path** `GET /v1/comms/media/{mediaId}/{variant}`
   (authenticated), which runs admission and the precomputed consent state (§8.4) and answers 302 to a signed URL for
   that variant at its pinned generation, **lifetime 120 s** (comms asks platform for 120 s within platform's 30–600 s
   range; a floor override may lower it to 60 s). The redirect and the signed response carry `Cache-Control:
   no-store`. The mobile apps cache images keyed by the stable path with a **120-second lifetime**; after that an image
   is shown again only after a conditional request to the stable path, which the server answers 304 only after
   admission and the consent state pass (404 → the app evicts it). The cache is cleared at sign-out and context switch.
   The web app fetches the stable path with its bearer token (`fetch`, since `<img>` cannot send the header), turns the
   response into an object URL held in memory and revoked when the image leaves the screen; the 302 to GCS needs the
   community bucket's CORS to allow GET from the web app's origins (PR-11). Shared lab and library computers keep
   nothing on disk. For a feed page the client may call `GetMediaUrls(mediaIds[])` once (one rate-limit unit). Rate
   limits (K40 floor) per member per 10 minutes: 60 batch calls; 2,000 thumb and feed variants (≈ 20 feed pages);
   **100 full-size variants** (the only variant big enough to be worth scraping, ≈ 50 MB). Variant file objects get
   neutral names (`<mediaId>-<variant>.jpg`), so no download name carries a camera or child name. Platform signs with the
   `ServiceAccountSigner` of its credentials: a key file signs locally; on Cloud Run default credentials sign through
   the IAM API (a network call) — measured in the load test, and the stable path signs lazily per image.
6. **Withdrawal:** consent withdrawn (or a name consent lapses, §8.4) → the **whole post** — text, alt text, credits
   and images — is hidden from non-moderators at that width, because a caption often names or locates the child; the
   author may edit it (new revision, review) to restore it without the child. Post removed or takedown → media
   `withdrawn` and purged (§5.7); evidence keeps hashes and the trail, not pixels (§5.4).
7. **Covers and directory photos** (`class_page.cover_media_id`, `community_space.cover_media_id`, K32 photos) go
   through the same pipeline and the same second-person confirmation; a cover may not show any identifiable child
   (the confirmer attests it); directory photos are each member's own photo with that member's (or guardian's)
   class-width consent.

### 8.2 State machine of `community_media`

`requested` (upload URL issued) → `uploaded` (platform verified) → `processing` (claimed; `attempts + 1`) → `ready` |
`rejected` (`rejection_code`: `TYPE_NOT_ALLOWED`, `TOO_LARGE`, `TOO_MANY_PIXELS`, `ANIMATED`, `UNREADABLE`) |
back to `uploaded` with `next_attempt_at` on a transient failure (1 min, 5 min, 30 min) → `failed` after 3 attempts
(dead letter: alert, author sees "This photo couldn't be processed — try another"). `ready` → `withdrawn` → `purged`.
`requested`/`uploaded`/`rejected`/`failed` items not attached to a submitted post within 24 h are purged by the orphan
sweep. A retry writes variants under deterministic keys (`media id + variant`); a variant already written by an earlier
attempt is replaced and its old generation purged in the same step, so retries never leave duplicates; quota is refunded
by the platform purge. Who is told: the author (status on the draft); operations on dead letters and on any purge
verification failure.

### 8.3 Processing limits and the library

- Library: **TwelveMonkeys ImageIO** (`imageio-jpeg`, `imageio-webp`, `imageio-metadata`, version pinned by the build
  batch) with the JDK's ImageIO for PNG decode and JPEG encode. The EXIF **orientation is read and applied to the raster
  first**, then all variants are **re-encoded JPEGs from the oriented pixels**, so portrait photos stay upright and no
  EXIF, GPS, ICC-embedded text, XMP or appended payload survives by construction (MEDIA-02).
- File signature checked against the declared type; the reader is chosen by signature, never by name.
- **Decompression-bomb limits** read from the header before any pixel decode: JPEG ≤ 8,000 px per side and ≤ 40 MP,
  decoded with source subsampling to ≤ 2,048 px long side (≤ 16 MB raster); **PNG and WebP ≤ 12 MP** (they may decode
  in full: ≤ 48 MB raster), else `rejected TOO_MANY_PIXELS`.
- **A separate media worker process.** Decoding never runs in the API process. The comms module is deployed a second
  time as `comms-media-worker` (same code, a Spring profile that starts only the media job), one decode at a time, its
  own heap (`-Xmx768m`) and a CPU limit of 1 vCPU, 30 s per image; an out-of-memory or crash there restarts the worker
  and returns the item to `uploaded` — it cannot take down the 21-module process. On the 7 GB build/dev machine the
  worker is not started by default; the media component tests start it in-process with the same limits.
- **Animated** images (more than one frame) → `rejected ANIMATED`. GIF is not accepted.
- **HEIC** is not decoded on the server: the mobile and tablet apps convert camera output to JPEG before upload (Expo
  image manipulation), the web app refuses HEIC with a message.
- **Scan provider:** none in S1–S3 (open decision 5 narrowed to S4). Compensations: originals are never served and are
  purged after processing; variants are re-encoded pixels; every image is confirmed by a second person
  (§6.5 — every image); images may be uploaded only by staff in S1–S3. Before student or guardian uploads (S4) a content-scan adapter
  is required; unconfigured = uploads by those categories refused.

### 8.4 Consent rule (K17)

Consent comes from people's `consent_record` as changed in §15: purpose `photo_video_publication` with
`scope ∈ class < institution < inter_school < public` (a consent at a width covers every narrower width), `decision
granted | refused`, `giver_capacity self | guardian`, `valid_until`, `verification`. **Width of a post:** class page →
`class`; school space, clubs, staff room and surfaced copies → `institution`; inter-school → `inter_school`; public
(P2) → `public`.

An image may be served for a post of width W only if, for **every** confirmed student subject P (§6.5):

- **P is adult** (`person_status.adult = true`): a live granted row given by P themself (`given_by_person_id = P`) with
  scope ≥ W, unexpired, verified; and no live refused row by P at scope ≤ W.
- **P is a minor or unknown** (no `person_status` row = minor): at least one live granted row with scope ≥ W, unexpired,
  verified, whose giver **currently** holds a `guardian_of` relationship to P with `link_status = active`,
  `is_legal_guardian = true` (comms projection); and **no** live refused row at scope ≤ W, and no granted row with a
  narrower scope, from any other such legal guardian — the most restrictive legal guardian decides (PRIV-04, PRIV-08).
  A guardian who has said nothing does not block; a guardian who restricted or lost the link no longer counts either way.
- **No projection row = no consent** (blocked). On a cold start the projection is empty until people's replay has run
  and the checkpoint matches; until then every image with student subjects is blocked.
- Staff subjects need no consent row (`not_required_staff`) but staff may refuse being pictured through the same people
  record (a refused row blocks).

**Names have their own consent.** A child can be identified by name as much as by a photo. People gains a purpose
`community_naming` with the same widths (PR-22). A student may be **credited, mentioned, or shown as a named author**
at a width only with naming consent at that width (else "a student of Grade 7 B"); directory names inside a class page
use class-width naming consent; free text naming a student is declared by the author (credits picker) and checked like
a credit; undeclared names in school-wide posts are for the reviewer, since every post at institution width or wider whose
audience includes guardians or students is reviewed before publication, official and trusted publishers included (K07
floor, §6.6) — the automatic field filter exists only for inter-school posts (§19.5). **Before naming consent exists (S2):**
student credits and mentions are switched off; a class-page post may refer to students of that class by first name
only (the class's own audience; open decision 23); school-space posts must not name students and are reviewed. When a subject's photo consent or
a credited student's naming consent no longer covers the post's width, **the whole post is hidden** for
non-moderators (§8.1 step 6). Surfaced copies are **always** reviewed by the school, whatever their content.

**Precomputed consent state.** The feed must not evaluate the rule per image per subject per guardian on every read.
`comms.media_consent_state(media_id, width, available, computed_at, dirty)` and `comms.post_consent_state(post_id,
width, available, dirty)` are maintained **in the same handler transaction** as the consent, person-status or
relationship change that affects them (the handler finds the media through `media_subject` / `post_person` of the
person), and at approval. A `dirty` row (a recompute pending) reads as **not available** (fail closed); a job
recomputes dirty rows within seconds. The feed and the media path read one row. **Every image gets
`media_consent_state` rows for every width at confirmation**, in the same transaction: an image confirmed as "no child
identifiable" (and no refusing staff subject) is `available = true`; so a missing row only ever means "not confirmed",
and reads as unavailable. `post_consent_state` rows are written at approval for posts with subjects or credits; a post
without any has none and needs none.

**Silence and no consent.** "Silence does not block" means: once one legal guardian has granted at the width, another
legal guardian who has recorded nothing does not block. A child with **no granted row at all is not consented** and
blocks every image showing them. Consequently photos publish only for children whose guardians have answered; the
subject picker shows each child's state at the post's width to the posting teacher (school staff, a legitimate
need), so teachers photograph consented children or take photos without identifiable faces. **Collecting consent
before S3:** the school sends a consent request through notices (with acknowledgement) linking to COM-10; guardians
answer per width; paper forms are recorded and verified (PRIV-06); COM-45 shows per-class coverage as a count
("24 of 31 children consented at class width") to the community owner and class teachers. No share of photos is
promised: in a class at 80 % coverage most whole-class group photos will block and individual or small-group photos of
consented children will publish.

**Reaching majority** (`people.person.majority_reached`, open decision 9 — the recommendation below is the default until
the owner rules): (1) from that moment only the student's own consent counts for new publication, surfacing and sharing;
(2) items already published under guardian consent stay at **class and institution** width (grandfathered: they were
lawful when published) and the student is told in COM-10 "You can now decide about photos of you", where withdrawing
hides them at once; (3) items at **inter-school** width are hidden after 60 days unless the student has granted
`inter_school` themself; (4) guardian membership of the class page continues (it is class content, not the student's
private data). The job that applies (3) runs daily from `person_status` rows; `MajorityTransitionComponentTest` covers
all four.

Legacy and paper consents: a generic "photo publication" form that does not state an audience is recorded with
`scope = class` (narrowest); a paper consent needs the evidence file and the recording staff member and is
`unverified` until a second staff member verifies it (PRIV-06); unverified rows do not count.

**Exposure after a withdrawal** (normal path): people commit → people relay → bus → comms inbox: p95 60 s; if the
queue backs up, people's heartbeat ages past 2 min and the source goes `degraded`, after which *no* media URL is issued
at all; a delivered event that keeps failing degrades it after 3 min. Add a URL issued just before (120 s) and the
client's cache (≤ 120 s, after which it must revalidate, §8.1): worst case ≈ 3 + 2 + 2 = **7 minutes**. A single event lost while heartbeats keep flowing is
detected by the tenant's stream sequence within ≈ 2.5 minutes and blocks all student images of that tenant (§7.1), so
it stays inside the same bound. **Emergency
path** (`EmergencyTakedown`, case handler or community owner): media `withdrawn` and a deny row at commit, then purge of
every generation of the variants (§5.7) — outstanding signed URLs answer 404 as soon as the objects are deleted
(seconds), the client cache aside (≤ 120 s before it must revalidate). **Cannot be recalled:** screenshots and photos of screens, images already
displayed in an open app session, anything a staff member downloaded under K19 (no one in S1–S3), and images seen before
the takedown. Notifications never contain images (§11.1).

### 8.5 Declared subjects and group photos (MEDIA-04/05)

- The uploader declares, per image, the depicted students and staff (picker: §6.1 — the section roster, or the
  teacher's rosters plus "another class") **and** one of: "these are everyone identifiable in this photo" or "other
  people are identifiable" with whether any of them are children. The declaration only prepares the confirmation:
  every image is confirmed by a second person (§6.5), who corrects the list. A face **count** (detection, not
  recognition) compared with the declared count would help the confirmer; it is not in S1–S3 (library, cost and the
  privacy review of running a face detector are open) and listed in §20.3.
- **Unidentified children** (including non-members, visitors' children, children of other schools) → the image cannot
  publish at any width until cropped or removed (MEDIA-05). Unidentified adults → K18 (`block` default, or reviewer
  confirmation).
- The confirmer (§6.5) must open the full variant (the button is disabled until it has loaded) and tick, per image,
  "I checked the people in this photo against the list"; the record keeps who and when.
- **False declarations** are recorded (declared_by, confirmed_by, times); a report "image shows my child without
  consent" opens a case where the declaration history is evidence; outcomes for staff are `warn` or `review_all`; the
  composer says before submit that declarations are recorded.
- **What cannot be detected:** a face the uploader did not declare and the confirmer did not notice; partial faces,
  reflections, name badges, uniforms, a school logo identifying the school. There is no face recognition (MEDIA-04);
  the controls are the declaration, the second person's look, consent per subject, and fast takedown.

---

## 9. The feed — query-based, no ranking

### 9.1 Audience semantics (formal)

- **Viewer keys.** For a context V (user u, institution i, category c, child χ for guardians) the server computes, from
  active `space_member` and `community_relationship` rows only, a key set K(V) of `(kind, target, category)`:
  student in section C, grade G: `(class_section,C,student) (grade_level,G,student) (institution,i,student)
  (home,h,student)` for each member home h; guardian through χ (in C, G): the same with `guardian`, from χ's rows only;
  staff: `(institution,i,staff) (home,h,staff)` and `(class_section,C,staff)` for each section taught; always
  `(user,u,any)`.
- **Rule match.** A rule r = (effect, kind, target, category) matches V iff some k ∈ K(V) has k.kind = r.kind,
  k.target = r.target and (r.category = any or r.category = k.category). Kinds: `home` (the post's own home; "guardians
  of the space" = `(home,h,guardian)`), `class_section`, `grade_level`, `institution` ("staff of the institution" =
  `(institution,i,staff)`), `user` (compiled audience-group snapshot).
- **Admission.** admit(p, V) ⇔ V is an active member of home(p) (in V's context, with `history_from ≤ published_at <
  history_to`) ∧ (∃ include r matching V, where a `user` row compiled from an audience group matches only while u is still in that
  group now) ∧ ¬(∃ exclude r matching V) ∧ policy now: category c enabled (K02), guardians
  allowed on the home (K04b/K10) if c = guardian, `p.audience_width ≤` current K09 of the home ∧ post state published,
  not expired, origin school active (inter-school) ∧ no deny row for (u, post/home/institution/tenant) ∧ no per-school
  hide for V's institution ∧ the revision involves no person protected for u (§7.7) ∧ the post's consent state at its
  width is available (§8.4).

| include matches | exclude matches | result |
|---|---|---|
| no | no | not admitted |
| no | yes | not admitted |
| yes | no | admitted (if the other conditions hold) |
| yes | yes | **not admitted (exclude wins)** |

- **Empty include set:** admits nobody; `SubmitPost` refuses it (422 `AUDIENCE_EMPTY`).
- **Snapshot or live:** structural kinds (`home, class_section, grade_level, institution`) are evaluated **live** against
  current relationships (so leavers lose posts at once, and joiners gain only what K30 gives them); audience groups are
  **snapshot ∩ live** (a later change can only narrow); **policy is always live** — narrowing K09, turning K10 or K04b
  off, removing a category from K02 hides already-published posts on the next read. A post hidden by a policy narrowing
  shows to its author and moderators as "Hidden by a setting change"; widening the policy again does not need a re-review
  because the post's own audience never changed.
- **Worked examples.** (1) include `(class_section,7B,any)`, exclude `(class_section,7B,guardian)` → 7B students and
  teachers in the home; 7B guardians not admitted. (2) Guardian with A in 7B and B in 7C; include `(home,7B-page,
  guardian)`: admitted in A's context, not in B's. (3) Same post after K10 is turned off on the 7B page: no guardian
  admitted in any context; the post is unchanged. (4) Guardian with children in grade 7 and grade 9, include
  `(grade_level,G9,guardian)` in the school space: admitted only in the grade-9 child's context. (5) Audience group
  "Sports day volunteers" approved with users {x, y}; z added later → not admitted; y removed later → no longer admitted.
  (6) School-space post include `(institution,i,any)` while K09 = institution; K09 narrowed to class_sections → hidden
  for everyone but author and moderators.

### 9.2 The statement

**One statement, one snapshot.** Java reads the switches and effective policy (§4.7) and passes, per candidate home,
only *policy* values (`ceiling_rank`, whether the viewer's category is allowed there); it can only **remove** homes.
Everything that a revocation changes — the member row, its relationship's status and validity, the viewer's keys, live
audience-group membership, deny rows, protected subjects, the consent state — and the hydration of the page are read **by the one statement below**, so
PostgreSQL evaluates them on a single snapshot. A relationship ended between the policy read and the statement is seen
by the statement; one ended after it applies to the next request (and to every media URL and comment request, which
re-run admission). Each home contributes its newest admitted rows inside the lateral subquery, so the merge is exact.

```sql
WITH policy AS (                         -- from Java: policy values only, never membership
    SELECT * FROM unnest(:homeKinds::text[], :homeIds::uuid[], :ceilingRanks::int[])
                  AS x(home_kind, home_id, ceiling_rank)
), homes AS (                            -- membership re-checked here
    SELECT m.home_kind, m.home_id, m.history_from, m.history_to, x.ceiling_rank
      FROM policy x
      JOIN comms.space_member m ON m.tenant_id = :tenantId AND m.home_kind = x.home_kind AND m.home_id = x.home_id
     WHERE m.user_id = :me AND m.institution_id = :institutionId AND m.deleted_at IS NULL
       AND (m.status = 'active' OR (:archiveRead AND m.archive_read_until > :now))   -- archive reads, §7.5
       AND m.context_child_person_id IS NOT DISTINCT FROM :childPersonId
       AND (:archiveRead OR m.valid_to IS NULL OR m.valid_to > :now)
       AND (:archiveRead OR m.relationship_id IS NULL OR EXISTS (SELECT 1 FROM comms.community_relationship r
                 WHERE r.tenant_id = :tenantId AND r.id = m.relationship_id AND r.status = 'active'
                   AND (r.valid_to IS NULL OR r.valid_to > :now)))
       AND NOT EXISTS (SELECT 1 FROM comms.community_access_deny d
                 WHERE d.tenant_id = :tenantId AND d.user_id = :me AND d.lifted_at IS NULL
                   AND ((d.scope = 'tenant') OR (d.scope = 'institution' AND d.scope_id = :institutionId)
                        OR (d.scope = 'home' AND d.scope_id = m.home_id)))
), rel AS (                              -- the viewer's active relationships in this context
    SELECT r.* FROM comms.community_relationship r
     WHERE r.tenant_id = :tenantId AND r.user_id = :me AND r.institution_id = :institutionId AND r.status = 'active'
       AND (r.valid_to IS NULL OR r.valid_to > :now)
       AND (:childPersonId IS NULL OR r.child_person_id = :childPersonId)
), keys AS (                             -- K(V) of §9.1, derived in the statement
    SELECT 'class_section' AS kind, class_section_id AS target_id, :category AS category FROM rel
     WHERE class_section_id IS NOT NULL
    UNION SELECT 'grade_level', grade_level_id, :category FROM rel WHERE grade_level_id IS NOT NULL
    UNION SELECT 'institution', :institutionId, :category
    UNION SELECT 'home', home_id, :category FROM homes
    UNION SELECT 'user', :me, 'any'
), groups AS (
    SELECT gm.audience_group_id FROM comms.audience_group_member gm
     WHERE gm.tenant_id = :tenantId AND gm.user_id = :me AND gm.deleted_at IS NULL
)
SELECT p.id, p.home_kind, p.home_id, p.published_at, p.post_kind, v.body, v.body_language,
       (SELECT json_agg(json_build_object('mediaId', rm.media_id, 'altText', rm.alt_text,
                                          'available', COALESCE((SELECT cs.available AND NOT cs.dirty FROM comms.media_consent_state cs
                                                       WHERE cs.tenant_id = :tenantId AND cs.media_id = rm.media_id
                                                         AND cs.width = p.consent_width), false))
                        ORDER BY rm.position)
          FROM comms.post_revision_media rm WHERE rm.tenant_id = :tenantId AND rm.revision_id = v.id) AS media
  FROM homes h
  CROSS JOIN LATERAL (
    SELECT p.*
      FROM comms.post p
     WHERE p.tenant_id = :tenantId AND p.home_kind = h.home_kind AND p.home_id = h.home_id
       AND p.status = 'published' AND p.deleted_at IS NULL
       AND p.published_at >= h.history_from AND (h.history_to IS NULL OR p.published_at < h.history_to)
       AND (p.expires_at IS NULL OR p.expires_at > :now)
       AND p.audience_width_rank <= h.ceiling_rank
       AND (p.published_at, p.id) < (:cursorTime, :cursorId)
       AND EXISTS (SELECT 1 FROM comms.post_audience a JOIN keys k
                     ON a.kind = k.kind AND a.target_id = k.target_id AND (a.category = 'any' OR a.category = k.category)
                    WHERE a.tenant_id = :tenantId AND a.revision_id = p.published_revision_id AND a.effect = 'include'
                      AND (a.via_group_id IS NULL OR a.via_group_id IN (SELECT audience_group_id FROM groups)))
       AND NOT EXISTS (SELECT 1 FROM comms.post_audience a JOIN keys k
                     ON a.kind = k.kind AND a.target_id = k.target_id AND (a.category = 'any' OR a.category = k.category)
                    WHERE a.tenant_id = :tenantId AND a.revision_id = p.published_revision_id AND a.effect = 'exclude')
       AND NOT EXISTS (SELECT 1 FROM comms.community_access_deny d
                        WHERE d.tenant_id = :tenantId AND d.user_id = :me AND d.lifted_at IS NULL
                          AND d.scope = 'post' AND d.scope_id = p.id)
       AND (p.post_kind <> 'surfaced' OR EXISTS (SELECT 1 FROM comms.post s
                        WHERE s.tenant_id = :tenantId AND s.id = p.source_post_id AND s.status = 'published'
                          AND s.published_revision_id = p.source_revision_id))
       AND (p.origin_institution_id IS NULL OR EXISTS (SELECT 1 FROM comms.community_space_school ss
                        WHERE ss.tenant_id = :tenantId AND ss.space_id = p.home_id
                          AND ss.institution_id = p.origin_institution_id AND ss.status = 'active'))
       AND NOT EXISTS (SELECT 1 FROM comms.community_space_hide x
                        WHERE x.tenant_id = :tenantId AND x.space_id = p.home_id
                          AND x.institution_id = :institutionId AND x.post_id = p.id)
       AND NOT EXISTS (SELECT 1 FROM comms.post_person pp
                        JOIN comms.protected_subject ps ON ps.tenant_id = :tenantId AND ps.viewer_user_id = :me
                             AND ps.lifted_at IS NULL AND ps.protected_person_id = pp.person_id
                        WHERE pp.tenant_id = :tenantId
                          AND pp.revision_id IN (p.published_revision_id, p.source_revision_id))
       AND NOT EXISTS (SELECT 1 FROM comms.post_consent_state cs
                        WHERE cs.tenant_id = :tenantId AND cs.post_id = p.id AND cs.width = p.consent_width
                          AND (cs.available = false OR cs.dirty))
     ORDER BY p.published_at DESC, p.id DESC
     LIMIT :size + 1
  ) p
  JOIN comms.post_revision v ON v.tenant_id = :tenantId AND v.id = p.published_revision_id
 ORDER BY p.published_at DESC, p.id DESC
 LIMIT :size + 1;
```

- `:category` is the context's category (student / guardian / staff); for staff, `rel` also yields the taught sections.
- **Archive reads** (`GetHomeFeed` on an archived class page, `:archiveRead = true`, `policy` holding that one page)
  admit ended member rows until `archive_read_until`, within their own history window (§7.5); the account must still be
  active, deny rows, protected subjects and consent still apply; the live feed always passes `false`.
- `available` reads the precomputed consent state (§8.4: one row per media and width, dirty = unavailable); posts whose
  post-level state is unavailable are not admitted at all (the whole post hides). Counts
  (§9.3) are computed by a sibling statement with the same CTEs. Author restrictions never hide posts; freeze does not
  stop reading; mutes are removed in Java (a preference, not a revocation).
- **Test:** `FeedSnapshotComponentTest` calls the repository with a home list computed *before* a guardian link is
  ended in another connection and asserts that the statement returns none of that home's posts and no media marked
  available; the same for a deny row and for a consent withdrawal (media flagged unavailable).
- **Indexes:** `post (tenant_id, home_kind, home_id, published_at DESC, id DESC) WHERE status = 'published' AND
  deleted_at IS NULL`; `post_audience (tenant_id, revision_id, effect, kind, target_id, category)`;
  `community_access_deny (tenant_id, user_id) WHERE lifted_at IS NULL`;
  `community_space_school (tenant_id, space_id, institution_id)`; `community_relationship (tenant_id, user_id,
  institution_id, status)` and `(tenant_id, id)`; `audience_group_member (tenant_id, user_id)`; `post_revision_media
  (tenant_id, revision_id, position)`; `media_subject (tenant_id, media_id)`; `consent_projection (tenant_id,
  person_id, purpose)`; `protected_subject (tenant_id, viewer_user_id) WHERE lifted_at IS NULL`; `post_person
  (tenant_id, revision_id, person_id)`; `post_consent_state (tenant_id, post_id, width)`; `media_consent_state
  (tenant_id, media_id, width)`; `community_space_hide (tenant_id, space_id,
  institution_id, post_id)`; `space_member (tenant_id, user_id, institution_id, status)`; `post (tenant_id, id)` for the
  surfaced-source probe.
- **Plan assumptions:** ≤ 30 homes per context (guardians 2–4, staff ≤ 15); each lateral is an index range scan that
  stops after `size + 1` admitted rows; each candidate costs ≤ 8 index probes (audience ×2, deny, surfaced, origin,
  hide, protected subject — usually empty for the viewer, consent state) plus one per image for hydration. A guardian
  page (3 homes × 21 rows) is ≈ 60 candidates ≈ 500 probes; a staff page with 15 homes ≈ 2,500 probes.
- **Load-test dataset** (`CommunityFeedLoadTest`, §14): a tenant of 3 schools plus an equally large second tenant (RLS
  selectivity); per school 1,500 students, 2,600 guardian accounts, 150 staff; 50 class pages, school space, staff room,
  8 clubs; 60 posts/day for 3 academic years ≈ 36,000 posts per school, 1.3 revisions per post, 2.5 audience rows per
  revision (≈ 117,000), 3 comments per post, 20 % of posts with images; 1 % of members with deny rows; one inter-school
  space with 3 schools. Load: 2,000 concurrent members on one institution, 10 s think time, mixed contexts.
- **Target:** p95 < 300 ms for `GetFeed` (the one statement, hydration included) with every predicate on. This is a target to be proven,
  not a measurement; the load test is a pilot release blocker (§14). **Fallback if it fails:** (1) a per-context
  candidate table `feed_candidate(user_id, context, post_id, published_at)` written by fan-out on publish, read
  instead of the lateral scan, with **every** predicate above still applied to the candidates (ARCH-04: candidates never
  decide access); (2) cap the homes scanned per page to the 10 most recently active and page the rest.

### 9.3 Counts and other aggregates (NET-10)

- Every count is computed from admitted rows only, with the same predicates, and is capped: unread per home and on the
  home card `LIMIT 100` → "99+"; the "all children" badge counts **distinct** unread posts across the selected child
  contexts (a post admitted through two children counts once, NOTIF-05).
- **Composer audience preview:** students and guardians see words only ("Students and guardians of Grade 7 B"); staff
  see a rounded count of members admitted now ("about 60"; "fewer than 10" below 10; rounded to 10), never a list.
- **Comment counts:** visible comments admitted to the viewer; held comments are counted only for their author and
  reviewers. **Reaction counts:** per K24; students see "Your classmates appreciated this" without numbers.
- **Member lists** (K32) list only active members of that home, names only, no child link for guardians.
- **`caughtUp`** is computed after admission (the page reached `last_seen_post_at` among admitted rows).
- **Health aggregates** (`GetCommunityHealth`): institution-level counts only, shown in rounding bands (0, 1–9, 10–24,
  25–49, 50–99, then to the nearest 50), so small schools see useful numbers and period-to-period differencing reveals
  nothing finer than a band.
- **Search (none yet):** any future search index must apply this same admission per query and live in comms' own
  database; a third-party index of community content is not allowed.
- Guardians with several children: the feed is computed **per selected child**, or "all children" as the union with a
  per-post child chip; switching child is a visible context change (NET-05).
- **Caught up:** the client shows "You're all caught up" and stops — no infinite scroll.

---

## 10. Permissions (iam), case routing and the safeguarding boundary

### 10.1 Permissions

New permission codes (`comms.community.*`), granted through roles — capability only; *reach* still comes from
relationships and the policy:

| Permission | Default roles |
|---|---|
| `comms.community.read` | student, guardian, teacher, staff (when K02 lists them) |
| `comms.community.submit_post` | teacher, staff; student/guardian only if K06 |
| `comms.community.publish_post` (trusted, no editorial review for **text only**; never images) | community publisher role |
| `comms.community.publish_official` (official / on behalf; MFA session) | community owner, principal |
| `comms.community.comment`, `.react` | per K21/K23 |
| `comms.community.report` | everyone with read — **never removable by a social restriction** (NET-09) |
| `comms.community.review` | class teacher and designated co-teacher (their class pages, §6.5), school moderators (K33); tenant community moderators (tenant scope, optional, text only) — other subject teachers are contributors without review |
| `comms.community.moderate_cases` | community owner, moderators |
| `comms.community.safeguarding` | safeguarding lead and deputy only — not implied by administrator or any other role |
| `comms.community.tenant_safeguarding` | the tenant's independent safeguarding contact (optional, tenant scope) |
| `comms.community.manage_spaces` | community owner; class teacher and designated co-teacher for their class pages |
| `comms.community.manage_policy` | institution community owner |
| `comms.community.manage_tenant_policy` | tenant administrator |
| `comms.community.erasure` | the school's data protection owner |
| `comms.community.view_health` | principal, community owner |
| `comms.community.platform_switch` | platform operators (platform console tokens only) |
| `comms.community.declare_maintenance` | the deployment pipeline's service identity only (`SetProjectionMaintenance`, §7.3) |

The "safeguarding" permission is deliberately **separate from administrator** (SAFE-13): a principal with every other
permission does not read safeguarding evidence unless granted it. Impersonation sessions are refused for every write,
every case read and every safeguarding read.

### 10.2 Case routing, implicated people and safeguarding storage

- **Routing by report category** (K35): routine → the institution's moderators; `privacy`, `inappropriate image` →
  moderators with priority; `urgent help` and safeguarding categories → the safeguarding lead and deputy (K36/K36a);
  `concern about staff` → the safeguarding lead, never the ordinary moderator queue.
- **Implicated users** of a case: the reported member, the author of reported content, anyone the reporter names in
  "who is this about", and for a home-level report the home's teachers. `ListCases`/`GetCase`/evidence and every
  safeguarding read refuse them (404) whatever their permissions; assignment cannot pick them.
- **The class teacher is not the default handler** of a case about their own class: the default owner is a moderator
  who is not a teacher of that home; the owner may add the class teacher on need-to-know (logged), without reporter
  identity.
- **Independent route** (K36b) when the implicated set contains the safeguarding lead, the deputy, the principal or the
  community owner: the case goes to `route = independent`, owned by the tenant's independent safeguarding contact
  (`tenant_safeguarding`); if the tenant names none, the case is held unassigned and readable by nobody in the school,
  the tenant administrators get an in-app alert "A community concern needs an independent handler" (no detail), and the
  reporter sees "Your concern will be handled by someone outside your school" plus the external contact text from K36b.
  A school cannot enable community without K36b resolvable (§4.6).
- **Reporter identity** is visible to the case owner and deputy only; never to reviewers, the class teacher, the
  reported person, the panel of another school, or in notifications. Removal notices to authors say which rule, never
  that a report was made, and are delivered with the next digest rather than at the moment of action, to weaken
  timing inference. A case about a member who later reports or posts about the reporter is linked to the first case
  automatically ("possible retaliation") for the owner to review. Residual: in a class of 30, content and timing may
  still let an author guess; this is stated in the report flow's "what happens next".
- **Who may read safeguarding data:** the permission **and** being named in K36 or K36a of that institution (or the
  tenant's independent contact for `independent` cases). An administrator who grants himself the permission still reads
  nothing; changing K36/K36a is a "2nd" policy change announced to tenant administrators and the platform contact
  (§4.6). Every grant of `comms.community.safeguarding`, `.tenant_safeguarding` or `.manage_policy` seen in iam's role
  events alerts the tenant administrators (comms reads the role's permissions through the refresh).
- **Safeguarding storage:** `safeguarding_case` and safeguarding `evidence_record` rows hold details in `_enc` columns
  (FieldCipher, per-tenant key from the secret store, bound to tenant and column); retained evidence pixels (only under
  an explicit hold, §5.4) are platform copies under an evidence owner type, downloadable only through
  `GetSafeguardingEvidence` (60 s URL). Every safeguarding read
  writes `safeguarding_access_log` in the same transaction; a read that cannot be logged fails. Safeguarding cases are
  invisible to `ListCases`/`GetCase` (routine operations) and to `GetCommunityHealth` except as counts.
- **`ExportCommunityAudit`** (community owner, MFA) contains: policy versions with before/after values and approvers,
  moderation actions (case id, action, reason code, actor id, time), switch changes, takedowns, deletion-ledger rows.
  It never contains report details, reporter identities, evidence, comment or post bodies, or any safeguarding row.
- **Platform operators and DBAs:** the application role sees only RLS-filtered rows; `mysms_platform_admin` (BYPASSRLS)
  is used only by the platform service and must hold **no grant on the comms schema** (§15: checked by
  `verification/smoke.sql`); `mysms_relay` reads only outbox/inbox. A DBA with the migration role sees ciphertext for
  safeguarding detail; decryption needs the master key, which only the running service holds. **Break-glass** for a
  platform operator (a legal order or a tenant's written request): two platform operators, time-boxed role grant, every
  statement logged, the tenant administrator informed; there is no product path to safeguarding content for platform
  staff. Residual, stated honestly: operations staff who hold both database access and the secret store can decrypt;
  that risk is procedural (separation of those duties).

### 10.3 Restrictions and essential access (NET-09, NET-11, NET-16)

| Restriction | Read class page | Read school space | Post / comment / react | Report | Notices (comms notices) | Community notifications |
|---|---|---|---|---|---|---|
| `mute_posting` (space or institution) | yes | yes | no in scope | yes | yes | yes |
| `review_all` | yes | yes | everything held for review | yes | yes | yes |
| `suspended` (space) | yes | yes | no in that space | yes | yes | yes (incl. "your post was removed") |
| `suspended` (institution) | yes | yes | no anywhere in the institution | yes | yes | yes |
| Access revocation (deny row) — safeguarding or access decision, not moderation | no (scope) | no (scope) | no | yes — `ReportConcern` always works | yes | no for the revoked scope |
| Guardian link restricted (sis) | no (that child) | no (that child) | no | yes | per sis/notices rules | no for that child |

Moderation restrictions never remove reading, reporting, notices, notice acknowledgement or learning; class-page
membership is never changed by a moderation restriction (only relationships change it). A restricted guardian still
acknowledges notices through the notices module, which does not consult community restrictions. **NET-16:** comms has
no dependency on finance and no input from fee status; `CommunityAccessFeeIndependenceTest` asserts that the community
decision path reads no finance data and that no restriction kind is derived from one.

---

## 11. Notifications

### 11.1 What is sent

- Community notifications go through `in_app_notification` (whose read operations this work serves) and the existing
  preference model. **New posts never create a notification row per recipient**: they appear in the feed's unread
  counts and in the daily digest. Rows are created only for personal events: your post was approved / returned /
  removed / not published at dispatch; a comment on your post; a mention (S4); your report has an outcome; a moderator
  has items waiting or an SLA breach; a policy proposal awaits your approval.
- **Payloads per channel:**

| Type | In-app (signed in; rendered at read time) | Push lock-screen (when providers exist) | Email | Data fields |
|---|---|---|---|---|
| Post approved / returned / removed / not published | "Your post in Grade 7 B was returned — open to see why" (staff); students and guardians: "Your post was returned" with the home shown only on the opened screen | "You have an update in MySMS" | not sent | notification id only |
| Comment on your post | "New comment on your post" + home name on open | "You have an update in MySMS" (only if the member enabled push for comments) | digest only | notification id |
| Report outcome | "There's an update on your report" | "You have an update in MySMS" | not sent | notification id |
| Moderator queue / SLA breach (staff) | "3 items are waiting for review in Campus North" | "Items are waiting for review" | optional | notification id |
| Daily digest | list of homes with counts of new admitted posts | — | "New activity in your school community" + count | digest id |
| Policy approval (administrators) | knob and institution | "A setting change needs your approval" | yes | proposal id |

  Never in any payload: body text, child names, image URLs or thumbnails, the class name off-app, the reason a post was
  removed, the word "report" in an author's notification. The app fetches details after unlock and sign-in.
- **Shared phones (MEM-03):** lock-screen text is identical for every type, so the holder of a family phone learns only
  that the account has an update; the app's "hide previews" setting is on by default for students and guardians;
  notifications are addressed to accounts, never to a phone number.
- **Delivery:** in-app always; push and email only when the providers exist (not in S1–S3, §16) and only for the types
  above; never for reactions; quiet hours (K43); `receives_communication = false` guardians get in-app rows but no
  push/email.
- Official notices remain notices (comms notices module) with acknowledgement; a like is never an acknowledgement.

**Table hygiene:** community notification types keep `title`, `body`, `sms_text`, `action_url` and `cohort_id` null and
`email_status`/`sms_status` `not_required` (a CHECK on the community type range is not possible across a catalogue id, so
the writer is the only path and `CommunityNotificationHygieneComponentTest` asserts it); the SMS and e-mail dispatchers
skip community types; push/e-mail for community go only through §11.1's generic texts.

### 11.2 Rendering at read time and on send

- Community rows store only `notification_type_id`, `entity_type`, `entity_id` and the recipient context; `title` and
  `body` are null. `ListInAppNotifications` renders each row after `CommunityAccess` admits the recipient to the entity
  **now**; a row whose entity is no longer admitted renders as "This item is no longer available" (NOTIF-04).
- **On send:** push and email dispatchers re-check access at send time; the digest is built per recipient at send time
  from the current feed admission (not from rows queued earlier), so a guardian restricted at 10:00 gets nothing of that
  child in the 07:00 digest the next day; a digest generated and queued before the restriction and not yet sent is
  rebuilt (digests are built in the sending transaction, never pre-rendered).

### 11.3 Redaction on revocation

When membership of a home ends for a user (transfer, link ended or restricted, deactivation, deny row), the same
transaction archives that user's community notification rows whose entity is in that home and clears their
`entity_id`, so the inbox keeps no pointer into the home. Pushes already delivered to a device cannot be recalled; they
contain nothing beyond "You have an update in MySMS".

### 11.4 Fan-out and limits

- New posts: no per-recipient rows; digest only. A 1,500-student school posting to all guardians produces no immediate
  fan-out; the next digest run builds ≈ 2,600 guardian digests.
- Digest job: per institution, recipients in batches of 500, each recipient in its own short transaction; at most 2
  concurrent institutions per node; a run that does not finish by 08:00 local stops and resumes the next day for the
  remaining recipients (no catch-up burst).
- Comment notifications: one row per post author per comment, coalesced to one row per post per hour.
- Mentions (S4): K40 caps at 5 mentions per post and 20 per member per day; staff only by default.
- Official posts: same as any post (digest); urgent matters belong in notices.
- **S1–S3:** in-app rows and the in-app digest card only; no push or email dependency at all. When providers arrive
  (§15), push/email are added per §11.1 without changing the rules.

---

## 12. The user interface — every role, every device

Screen ids `COM-xx` (to add to `ui/02-navigation-and-screens.md` §3). W = web, S = phone, B = tablet.

### 12.1 Home integration (the first screen after sign-in)

Order on every home (brainstorm §8.1): **context bar** (school / role / child) → **urgent official notices** →
**today / needs action** → **community preview** (3–5 latest items from my homes + "Open community") → **upcoming
events**. Community failing to load shows a small "Community is unavailable" card and never blocks the rest
(HOME-07). Each role's home is a separate composition:

| Role | Home cards (in order) | Community on home |
|---|---|---|
| **Student** | Timetable today, homework due, LMS "continue learning", notices | "From your class and clubs" preview; "My submissions" chip when one is returned |
| **Teacher** | Today's classes, attendance to take, work to grade, **review queue count**, notices | Quick composer for "my class" (one tap: photo + caption → submit), latest from my homes |
| **Guardian** | Child switcher, per-child: fees due, attendance alerts, homework, notices | Per-child "What's happening in Grade 7 B" preview; consent reminders when a photo is waiting on them |
| **Staff (non-teaching)** | Job tasks (library, transport, office), notices | Staff room preview |
| **Principal / community owner** | School KPIs (existing dashboards), **community health** card (queue age, open cases, posts this week), urgent cases | Official composer ("post as the school") |
| **Tenant administrator** | Tenant dashboards; community ceiling and presets status; pending approvals; cases awaiting an independent handler (count only) | — |
| **Alumni (P1)** | Alumni events, batch updates | Alumni network only |

### 12.2 Community area — members

| Id | Screen | W | S | B | What it holds |
|---|---|---|---|---|---|
| COM-01 | Community feed | ✓ | ✓ | ✓ | Merged feed, home filter chips, "caught up" end, pinned official items, child switcher for guardians ("all children" shows a child chip per post) |
| COM-02 | Home page (class page or space) | ✓ | ✓ | ✓ | Header (rules in plain words, "who can see posts here"), its feed, members list per K32, mute |
| COM-03 | Post detail | ✓ | ✓ | ✓ | Post, images (tap to full variant), reactions per K24, comments (depth ≤ K22), report |
| COM-04 | Composer | ✓ | ✓ | ✓ | Text, up to K15 photos, alt text prompts, "who is in this photo" (subject picker + "anyone else identifiable?"), **audience chip with plain-language preview**, posting identity line, post type, schedule (staff), review notice |
| COM-05 | My submissions | ✓ | ✓ | ✓ | Drafts, pending, returned with reason (edit & resubmit), published, withdrawn, hidden by a setting change |
| COM-06 | Moments viewer (P1) | ✓ | ✓ | ✓ | Tap-through, pause, text alternative, no viewer counts for students |
| COM-07 | Report flow | ✓ | ✓ | ✓ | `ReportContent` from an item, or `ReportConcern` without one (also from the home card and the "unavailable" screens); category (incl. "concern about a staff member", "I need help now"), optional detail, confirmation, "what happens next", staffed hours, urgent-help numbers (SAFE-05) |
| COM-08 | My community settings | ✓ | ✓ | ✓ | Muted homes, digest, quiet hours, language, hide previews; link to consent centre (guardians, adult students) |
| COM-09 | Saved posts | ✓ | ✓ | ✓ | |
| COM-10 | Consent centre (guardians, students 18+) | ✓ | ✓ | ✓ | §12.7 — uses people's consent operations and comms' `GetMyChildMediaUse` |
| COM-11 | Community rules and privacy (first use) | ✓ | ✓ | ✓ | MEM-04 rules, what is shared and with whom, what guardians can see (students), before optional participation; acceptance stored as a preference |

### 12.3 Staff and moderators

| Id | Screen | W | S | B | What it holds |
|---|---|---|---|---|---|
| COM-20 | Review queue | ✓ | ✓ | ✓ | Pending posts and held comments with age, author category, audience, media subjects and consent state; claim; approve / return with reason / reject; keyboard-first on web; §12.8 |
| COM-21 | Home management | ✓ | — | ✓ | Spaces (types per K04), owners/moderators, rules within the institution's limits, archive; class pages: description, cover, surfacing |
| COM-22 | Cases | ✓ | — | ✓ | Reports grouped into cases, severity, SLA, evidence (restricted), actions, escalation to safeguarding |
| COM-23 | Case detail | ✓ | — | ✓ | Timeline of reports and actions, evidence record (hashes, ids, review trail; pixels only under a safeguarding hold), decision with reason code, outcome messages |
| COM-24 | Safeguarding inbox | ✓ | — | ✓ | Safeguarding and staff-concern cases only; visible only with the safeguarding permission; every open logged |
| COM-25 | Member restrictions | ✓ | — | ✓ | Who is muted / under review-all / suspended, until when, linked case |
| COM-26 | Classroom quick post | — | ✓ | ✓ | Camera-first composer for teachers; shared classroom tablet rules in §12.9 |
| COM-27 | Emergency takedown | ✓ | ✓ | ✓ | One item, reason, confirm; shows completion of purge |

### 12.4 Administrators

| Id | Screen | W | S | B | What it holds |
|---|---|---|---|---|---|
| COM-40 | Community settings | ✓ | — | ✓ | The knob catalogue grouped as in §4.2; each knob shows **inherited value, local value, "limited by your organisation / the platform"** and who set the school's value; widening shows a warning and becomes a proposal (§4.6) |
| COM-41 | Presets | ✓ | — | — | Apply a preset with a diff preview |
| COM-42 | Preview as role | ✓ | — | — | Effective policy and synthetic feed for a role/space |
| COM-43 | Policy history and approvals | ✓ | — | — | Versions, before/after, approver, pending approvals and why they are blocked |
| COM-44 | Emergency controls | ✓ | ✓ | ✓ | Freeze institution / home, lock posting, with reason; visible banner while active |
| COM-45 | Community health | ✓ | — | ✓ | Aggregates only, in rounding bands (§9.3); consent coverage per class; picker-search counts; median review time; no leaderboards, no per-child engagement |
| COM-46 | Moderation team | ✓ | — | — | Community owner, moderators, safeguarding lead and deputy, independent route, coverage hours, grade bands — **community cannot be enabled until §4.6 is met** |
| COM-47 | Person content (erasure) | ✓ | — | — | Discovery and erasure (§5.8), exceptions list |
| PC-COM-01 | Platform safety floor (platform console) | ✓ | — | — | Floor constants (read-only), tightening overrides, global switches, projection health; changes audited |

**≈ 32 screens.** Phone screens are the member set plus review queue, takedown and emergency controls; heavy
administration is web and tablet.

### 12.5 UI rules that matter for privacy

- The audience is **always visible** on the composer and on every post ("Shared with: Grade 7 B students and
  guardians").
- **Guardians are shown to other guardians as "a guardian"** by default (comments, member lists); staff see names. A
  school may show guardian names inside a class page (K32), never with the child.
- **Screens are protected where the platform allows:** Android `FLAG_SECURE` and a blurred app-switcher snapshot on iOS
  for community screens; screenshots cannot be prevented on every device, and the consent text says so.
- **Shared web computers:** images are never written to the browser's disk cache (`no-store`, §8.1); sign-out clears
  memory caches; the web app offers "This is a shared computer" at sign-in, which shortens the session to 15 minutes idle.
- Student authors (S4) show **first name + class** inside their own class page only with class-width naming consent
  (§8.4), else "a student of Grade 7 B"; in the school space "a student of Grade 7 B" unless naming consent at
  institution width exists; never contact details or full date of birth (MSG-04). To staff, guardians show as their own
  display name and "guardian", never with the child's name or class.
- Reaction counts follow K24; students see "Your classmates appreciated this" without numbers.
- Switching child, role or school **clears the feed cache** and scroll state (HOME-10); sign-out clears local protected
  state (UX-07, §12.9).
- Images load as thumbnails first; a data-saver toggle keeps them tap-to-load (UX-02).
- Every string in en and hi (and the pilot's languages); every action gated by `useCan`; every write sends its ETag;
  409/412/422 explained in words.

### 12.6 Student UI (S4)

- **Who can see this post:** every post and the composer show the audience in words for the reading level ("Your class
  and your class's parents can see this"; "Your teacher will check it first"); pending posts show "Only you and your
  teacher can see this right now".
- **What guardians can see** (PRIV-14): COM-11 and the class page header say "Your parents or guardians can see posts
  in your class page" when K04b/K10 are on; "they cannot see your reports or messages to the safeguarding lead".
- **Help routes:** COM-07 in the student's language, short sentences (written to a Grade 4 reading level for under-13
  bands), an "I need help now" path showing who is on duty and staffed hours (SAFE-05); never labelled an emergency
  service.
- **Unknown grade band:** shown as the under-13 experience (no counts, guided composer); the student is not told a band.

### 12.7 Consent centre (COM-10)

- Lists only the children the guardian holds an active, unrestricted **legal-guardian** link to (students 18+ see only
  themselves). Per child and per width (class / whole school / schools of our group): the guardian's **own** decision
  (granted / refused / not answered, date, notice version, channel) and the **effective result** ("Photos of Aarav are
  currently shared with: his class") **with no explanation of why it differs** — the effective result can be narrower
  because of school settings, the student's own choice or another guardian, and the screen names none of them; "Ask
  the school office if this is not what you expect". It never shows another guardian's decision, name or date, any
  wording about other guardians, or anything about siblings' or classmates' consent (open decision 13).
  `ConsentCentreDisclosureComponentTest`: two legal guardians, one grants school width, the other refuses; neither
  response contains the other's decision, identity, date or a reason field.
- **What is affected** (PRIV-07), from `GetMyChildMediaUse`: "Aarav appears in 4 posts in Grade 7 B, 1 in the school
  space, 0 shared with other schools". On withdrawal: "New sharing stops at once. Photos already shared are hidden
  within about 7 minutes and deleted from our storage; once deleted they cannot be recovered [after N days — shown
  only when the community bucket keeps deleted files, §5.7]. Copies someone saved or photographed cannot be recalled."
  If the school's safeguarding lead must keep an image as evidence of a serious incident, it is held privately under a
  legal hold and every access is logged; the guardian is told "kept privately by the school's safeguarding lead as
  evidence" without detail. The same wording, with the real number, is used in the erasure outcome.
- **What is held about my child** (PRIV-09 access, S4): `GetMyChildCommunityData` lists the posts, credits, comments
  and reactions involving the child that the guardian may see, with counts of items in other homes; cases and evidence
  are excluded and counted only.
- The consent text explains that a width covers the people in that audience as it changes over the year (a new
  teacher, a new classmate's parent), and that screenshots cannot be prevented.
- Withdrawal and changes call people's operations; comms never stores the decision itself.

### 12.8 Review ergonomics (COM-20)

- No bulk approve for any item with images or student authors; bulk actions exist only for returning items or for
  text-only staff posts.
- Approve is disabled until the reviewer has opened every image at full variant and ticked each image's subject check;
  the subject list with each subject's consent state at the post's width is shown beside the image; on phones the image
  opens full-screen first.
- The approval records reviewer, claim time, time spent on each image and device class; COM-45 shows median review time
  per institution (not per person) so rubber-stamping is visible.

### 12.9 Shared classroom tablet and sign-out (React Native, Expo)

- **Classroom device mode** (set by an administrator when enrolling the device): a teacher signs in per session; the
  refresh token is **not** persisted (memory only); the access token lives in memory; idle 5 minutes → lock screen,
  15 minutes or the end of the period → sign-out; the app never shows the previous teacher's content after lock.
- **Personal devices:** refresh token in `expo-secure-store` (Keychain / Keystore), never in AsyncStorage.
- **Camera:** COM-26 captures in-app into the app's cache directory; the app requests no media-library write
  permission, so photos never reach the device gallery; the picked or captured image is re-encoded to JPEG on the device
  (dropping EXIF and GPS) before upload and again on the server.
- **Drafts:** in memory, or encrypted in secure storage on personal devices; dropped at lock in classroom mode.
- **Sign-out (all devices):** clears the React Query cache, secure-store tokens, the app's image cache and cache
  directory, any persisted drafts, and dismisses delivered notifications of the app.
- **Tests:** client unit tests for the storage policies; a device test (Maestro or Detox, chosen by the client batch)
  that signs in on a classroom device, captures a photo, locks, asserts the gallery has no new item and the cache
  directory is empty after sign-out.

---

## 13. Privacy and security invariants (each one becomes a test)

1. Author, actor, tenant and institution come from the token; **no community request field can set them**.
2. A member reads a post only if all of §9.1 holds: community on for their context (switches, K01–K03), active
   membership of the post's home with the post inside their history window, admitted by the published revision's
   audience rules (exclude wins), the **current** policy allows it (K02, K04b/K10, K09 at read time), post `published`
   and not expired, no deny row, origin school active, not hidden for their school, no person protected for them (§7.7)
   involved, consent state available. Otherwise **404**, never 403.
3. Lists, counts, search, mentions, notifications, media URLs and comment threads apply the same check *before*
   paging (NET-10).
4. Pending student submissions are visible only to the author and reviewers (POST-05).
5. A guardian's reach is per child and per link; a restricted guardian link removes reach immediately.
6. A depicted or credited student without consent at the post's width blocks the post; withdrawal hides the whole
   post at once (§8.1 step 6).
7. Approval binds a revision hash; any edit that changes audience, media or meaning returns to review.
8. Media is never readable before processing; URLs are short-lived and per variant; originals are never served.
9. Reports and safeguarding evidence are readable only by case handlers / the safeguarding lead; a reporter's identity
   never reaches the reported person.
10. The implicated person never decides or reads their own case (SAFE-04); the author never approves their own item.
11. Switches and community-off take effect on the next request (no cache).
12. Event payloads, logs, error messages, audit rows and metrics carry no body text, names of children or images; audit
    rows of content changes carry field names, not values.
13. No cross-tenant read anywhere; no global person search; no directory export; no contact sync.
14. Reaction counts, viewer lists and follower counts never shown for students where the floor forbids.
15. Disabling a feature never disables reporting, notices or learning.
16. Class-page membership comes only from relationships; no operation adds a member to a class page.
17. Community access never depends on fee status.

---

## 14. Testing strategy

**Time and tiers — no fakes outside unit tests** (CLAUDE.md). All time-based logic reads the injected `java.time.Clock`
bean the services already take; **unit tests** may give it a fixed or advancing clock. **Component tests** use the real
clock, real PostgreSQL and a real bus, and shorten waits through **configuration**, not doubles: every threshold
(reorder window, degrade, guardian close, closed, SLA, claim, expiry) is a property in comms' `application.properties`
(`mysms.comms.community.*`), set to seconds in the component-test profile (e.g. reorder window 2 s, degrade 3 s, closed
6 s). **Drop and reorder without a fake bus:** the test is a real producer — it publishes crafted events (with chosen
`tenantSeq` values: 1, 3 and never 2 for a drop; 3 then 2 for a reorder) and crafted heartbeats through the production
`EventBus` API, with the source module's own relay heartbeat disabled by configuration; the fast tier runs on the
messaging library's **in-process adapter** (a production adapter used by the single-process deployment), the heavy tier
on RabbitMQ and the Pub/Sub emulator. Two tiers:

| Tier | Where | Runs |
|---|---|---|
| **Fast** — part of `mvn clean install` | the 2-core / 7 GB build machine, one PostgreSQL Testcontainer, the in-process bus adapter | all component tests of comms (HTTP, real PostgreSQL); matrix **≈ 600 pairwise cases** run against one migrated database, each case in a fresh tenant of its own (no cleanup,
no rollback tricks — services commit through `AppTransactionManager`); property test **20 seeds × 20 viewers**; revocation, gap, kill-switch and after-hours tests with the configured short thresholds (seconds of wall time); write-surface, redaction, snapshot, protected-subject, membership, multi-institution, archive, majority, consent-centre tests. Budget: ≤ 6 minutes for comms' component tests |
| **Heavy** — `-Pheavy`, nightly and before each pilot | a larger CI runner (8 vCPU / 32 GB) | full matrix ≈ 3,000 cases; property test 500 seeds × 50 viewers; RabbitMQ container pause and Pub/Sub emulator reordering variants of the gap and broker tests; term-start burst (a school's enrolments and assignments in one hour) |

- **Matrix:** generated cases of member category (6) × grade band (4) × knob value (every value of every knob enforced
  in the slice) × action, checked against an expected-answer table written from §4.2/§4.5, not from the code.
- **Property:** `AudiencePropertyComponentTest` compares `GetFeed`/`GetPost` with `AudienceReference`, a plain-Java
  evaluator written **from §9.1 only** by a different agent than the one writing `FeedRepository`, sharing no code
  (set membership, never SQL); failing seeds are kept as regression cases in the fast tier.
- **Named component tests:** `ClassPageMembershipComponentTest`, `CommunityWriteSurfaceTest` (§6.4),
  `CommunityLogRedactionComponentTest` (sentinel body text and child name absent from logs, `outbox_event.data`, audit,
  metric labels, error bodies), `ArchiveAccessComponentTest`, `MultiInstitutionIsolationComponentTest`,
  `GuardianMultiChildComponentTest`, `KillSwitchComponentTest` (§4.9, §21), `FeedSnapshotComponentTest` (§9.2),
  `StreamGapComponentTest` (one dropped and one reordered event per sequenced stream; per-tenant fail-closed only for the
  affected tenant; automatic repair), `ProtectedSubjectComponentTest` (§7.7), `MajorityTransitionComponentTest`,
  `ConsentCentreDisclosureComponentTest`, `AfterHoursWithholdComponentTest`, `CommunityNotificationHygieneComponentTest`,
  `RevocationUnderBrokerFailureComponentTest`, terminated staff (§7.4), REST/gRPC parity (existing kit).
- **Client tests:** composer audience preview, review queue gating, child switcher clearing state, report flow, sign-out
  clearing, classroom device mode (§12.9), stable media path caching.
- **Abuse drills** (SAFE-17), as gates in §16.2.
- **Load test** `CommunityFeedLoadTest` (Java harness, `-Pload`) runs in a **named environment, `community-load`**: a
  GCP project mirroring the cost plan's recommended configuration (Cloud SQL Enterprise 4 vCPU / 16 GiB, the API on
  Cloud Run with 2 × 2 vCPU / 4 GiB instances, the media worker separately, the community bucket), the §9.2 dataset, and
  the load generator on its own e2-standard-4 VM. Owner: operations (the owner names the person and approves the cost, §18 item 21); started
  for the run and deleted afterwards. It never runs on the build machine.
- **Release blockers for any pilot:** the fast and heavy tiers green; the load test passed in `community-load` within
  the last 30 days; the drills for the slice recorded.

---

## 15. Prerequisites outside comms (exact contracts)

| # | Prerequisite | Module | Contract / schema change | Needed by |
|---|---|---|---|---|
| PR-1 | **Consent schema** | people | `consent_record` gains: `scope varchar(20)` (CHECK in `class, institution, inter_school, public`; required iff `purpose = 'photo_video_publication'`), `decision varchar(10) NOT NULL DEFAULT 'granted'` (CHECK `granted, refused`), `giver_capacity varchar(10) NOT NULL` (CHECK `self, guardian`), `valid_until date`, `verification varchar(12) NOT NULL` (CHECK `verified, unverified`), `verified_by_user_id uuid`, `superseded_by_id uuid`; CHECK `channel NOT IN ('paper_form','in_person') OR (evidence_file_id IS NOT NULL AND recorded_by_user_id IS NOT NULL)`; unique index replaced by `(tenant_id, person_id, purpose, given_by_person_id) WHERE withdrawn_at IS NULL AND deleted_at IS NULL AND superseded_by_id IS NULL`. The design's earlier `photo_publication` is `photo_video_publication` | S3 |
| PR-2 | **Consent operations** (the 5 × 501) | people | `CreateConsentRecord` (purpose, scope, decision, channel, evidence; giver from the token for self-service; staff recording a paper form need `people.consent_record.record_on_behalf` and evidence; a new row at the same (person, purpose, giver) supersedes the old one), `WithdrawConsent`, `GetConsentRecord`, `ListConsentRecords` (a guardian sees only rows they gave and the effective status), `GetConsentStatus` (effective width per purpose, computed with the same most-restrictive rule as §8.4). Guardian authority: people keeps `people.guardian_authority` from `sis.student_guardian` events (an event-only reference against the dependency arrow, as ARCHITECTURE §2 allows) and refuses a guardian giver without an active legal-guardian link (403) | S3 |
| PR-3 | **Consent events** | people | `people.consent_record.recorded / withdrawn / superseded / expired` (daily expiry job) and `.snapshot` (replay) with `{id, personId, givenByPersonId, giverCapacity, purpose, scope, decision, validUntil, verification, withdrawnAt, subjectAdult, rowVersion}`; `people.person.majority_reached {personId, adult: true, rowVersion}` from a daily job (age of majority from the tenant's jurisdiction, 18 in India) and `people.person.status_snapshot`; daily `people.consent.checkpoint {tenantId, rows, maxRowVersion, digest}`; **`people.ReplayConsentEvents(tenantId)`** (platform operator or tenant administrator) re-publishes `.snapshot` for every live row and `status_snapshot` for their subjects. Owner of replay: people | S3 |
| PR-4 | **Restricted guardian link** | sis | `student_guardian.status` CHECK becomes `active, restricted, inactive`; new `sis.student_guardian_restriction(id, tenant_id, student_guardian_id, reason_code, legal_reference_enc, evidence_file_id, decided_by_user_id, valid_from, valid_to, lifted_at, lifted_by_user_id, …)` readable only with `sis.student_guardian.restrict`; operations `sis.RestrictGuardianLink`, `sis.LiftGuardianLinkRestriction`, `sis.ListGuardianLinkRestrictions` (that permission; MFA); internal gRPC `sis.ListRestrictedGuardianLinks(institutionId)` returning `{linkId, studentPersonId, guardianUserId, rowVersion}` only (no reason), for comms' 10-minute read (service token, §7.7); generic `UpdateStudentGuardian` can no longer set or clear `restricted` (422); events `sis.student_guardian.restricted / restriction_lifted` with the PR-5 payload and no reason. sis's own persona rule already reads `status = 'active'` only, so the restriction also applies to sis's guardian views | S2 |
| PR-5 | **Relationship payloads** (hard for S2) | sis | `sis.student_guardian.*`: `{id, studentId, studentPersonId, guardianId, guardianUserId, guardianPersonId, institutionId, status, hasCustody, isLegalGuardian, receivesCommunication, rowVersion}`; `sis.student_enrollment.*`: `{id, studentId, studentUserId, studentPersonId, institutionId, academicYearId, classSectionId, enrollmentStatus, effectiveFrom, effectiveTo, changeReason, rowVersion}` | S2 |
| PR-6 | **Staff status** | hr | `hr.staff.*` member payload adds `status`; `ChangeStaffStatus` to `exited` deactivates the iam account (hr may call iam; outside the transaction, retried); daily `hr.staff.checkpoint`; `hr.ReplayStaffEvents(tenantId)` | S1 |
| PR-7 | **Academics / org payloads** (hard for S1) | academics, org | `academics.class_section.*` `{id, institutionId, academicYearId, gradeLevelId, sectionId, classTeacherStaffId, isActive, rowVersion}`; `academics.teaching_assignment.*` `{id, classSectionId, staffId, teacherRole, isActive, rowVersion}`; `org.academic_year.closeAcademicYear` and `org.academic_term.*` with dates; `org.institution.*` with `status`. Id-only events would mark whole classes stale on every edit (§7.1), so the fallback is not acceptable | S1 |
| PR-8 | **iam confirmations** | iam | **Verified 18 Nov from the code:** `UserAccountService.deactivateUser` runs `UserAccountRules.afterDeactivate` → `signOut.everywhere` in the same transaction (refresh and API tokens revoked, sessions ended) and publishes `iam.user_account.deactivateUser`; `SignInService` refuses a refresh without an active account; `IamTokenGrantsComponentTest` asserts that a deactivated (and even re-activated) account's token no longer refreshes (401). Remaining: confirm that `activateUser` and `user_role` events are published on every path (import, sis/hr portal-account flows) — an iam test to add, `IamAccountEventsComponentTest` | S1 |
| PR-9 | **Relay heartbeat and per-tenant stream sequence** | messaging (core) for the heartbeat of **every** module's relay (comms consumes those of sis, iam, people, hr, academics and org); sis, people, hr, iam for the sequences | each module's outbox relay publishes `<module>.relay.heartbeat {module, time, streams: {name: {tenantId: lastSeq}}}` every 30 s (no tenant; tenants that published on the stream in the last 30 min) on the same exchange as its events; for the designated streams (`people.consent`, `people.person_status`, `sis.guardian_link`, `sis.enrollment`, `hr.staff`, `iam.account`) the relay stamps `tenantSeq` from `<schema>.event_stream(tenant_id, stream, last_seq)` in its own transaction, advanced only on a successful publish; `ApplicationRoleGrants` grants the relay role `SELECT, INSERT, UPDATE` on `event_stream`; replay snapshots carry the current `tenantSeq`; consumers tolerate reordering (§7.1) | S2 (heartbeat alone for S1) |
| PR-10 | **Scheduled-jobs runner** | core | leader-elected or row-claimed jobs with per-item transactions, retries with backoff, dead-letter table, metrics; runs projection refresh, reconciliation, dispatch, digest, SLA, retention, purge, media | S1 |
| PR-11 | **Platform files** | platform + ops | file objects created for comms carry neutral names (`<mediaId>-<variant>.jpg`); `PurgeFileObject` also overwrites `file_name` and `original_name` of the purged row with the neutral name; a second storage disk `gcs-community` (`MYSMS_COMMUNITY_FILES_BUCKET`) for comms' media and evidence — bucket with versioning off, soft delete off, no lifecycle or backup copy (§5.7); `GetStoragePolicy(disk)` returning versioning and soft-delete retention read from the bucket at startup; `GetDownloadUrl` takes `ttlSeconds` (30–600; today fixed 10 min); `PurgeFileObject` (delete with generation precondition, list and delete any remaining versions, refund quota, write `deletion_log`, report misconfiguration); service-identity upload for derived files (`owner_entity_type = comms.community_media`); signed downloads carry `Cache-Control: no-store` (the `response-cache-control` parameter of the signed URL); the community bucket's CORS allows GET from the web app's origins | S3 |
| PR-12 | **Database grants** | ops / database | **Verified 18 Nov from the code and docs:** migrations grant only `mysms_app` (DML) and the relay role (outbox/inbox) per schema (`core.db.ApplicationRoleGrants`); DEPLOYMENT.md gives `mysms_platform_admin` only `CONNECT`; no default privileges exist, and `BYPASSRLS` does not bypass table privileges — so it reads no comms table today. **Defect found:** `ModuleDatabaseSettings` javadoc and `TestDatabase` comment call the relay role `mysms_platform_admin`; if an operator followed them, that role would gain the outbox/inbox grants (ids only, no business table) — the comments must be corrected by core's owner. To add: a `smoke.sql` assertion `NOT has_table_privilege('mysms_platform_admin', 'comms.safeguarding_case', 'SELECT')` for every comms table | S1 |
| PR-19 | **Global-row read policy** | database (comms baseline) + docs | for comms' tenantless tables without tenant data (`community_global_switch`, `source_heartbeat`): `FOR SELECT USING (true)` for tenant sessions, writes only from a transaction binding no tenant; `heartbeat_mark` keeps the standard global policies (invisible to tenant sessions); database README gains this third case; `smoke.sql` asserts a tenant session reads and cannot write the global switch | S1 |
| PR-20 | **Bulk read RPCs and service identity** | sis, iam, academics (+ iam service tokens) | internal gRPC, paged, with `rowVersion`: `sis.ListGuardianLinksByInstitution`, `sis.ListEnrollmentsByInstitution`, `iam.ListAccountsByInstitution` (status + role grants), `academics.ListClassSectionsByInstitution`, `academics.ListTeachingAssignmentsByInstitution`; each takes `updatedSince`. Called by comms' refresh, reconciliation and gap repair with a **service token** (iam already issues them, 5-minute TTL, `service-token-ttl`; `GrpcAuthInterceptor` exists in auth-client — `backend/README.md` line 76 is stale) carrying the tenant and an internal audience; comms' service principal needs only these read permissions. PRINCIPAL-REVIEW P1-34 (batch reads) is thereby partly delivered for these resources | S1 (academics, iam), S2 (sis) |
| PR-21 | **Off-lineage safety log** | platform + ops | an append-only bucket outside database backups with a **45-day retention lock** (backup window + margin) and expiry after it; objects encrypted per tenant; readers limited to comms' replay service identity and two named operators, reads logged; comms ships `safety_log_outbox` rows to it through platform (§5.7); DR runbook step "replay the log, list unshipped entries, replay consent, reconcile, operator check, then reopen" | S1 (text actions) |
| PR-22 | **People purposes** | people | add `community_naming` (names, credits, directory names; same widths as photos) and `community_participation` (a guardian's consent to a minor's participation; a minor = people's `adult` false) to `consent_record.purpose`; both carried by the PR-3 events | S3 (naming), S4 (participation) |
| PR-13 | **Delivery providers** (push/email) | comms + infra | not needed for S1–S3 (§11.4) | S4 (S3 soft) |
| PR-14 | **Gateway rate limiting** | infra | per-account and per-IP limits on community writes and media URL issuance (SAFE-12 scraping/bulk download) in addition to K40 in the service | S4 (soft before) |
| PR-15 | **Image library and media worker** | comms + ops | TwelveMonkeys ImageIO (`imageio-jpeg`, `imageio-webp`, `imageio-metadata`), version pinned; the `comms-media-worker` deployment (§8.3) | S3 |
| PR-16 | **Scan adapter** | comms + provider | content/malware scan port with one adapter | S4 (student/guardian uploads) |
| PR-17 | **Data-protection paperwork** | owner / legal | processor agreements and data-location statements for GCS, push and email providers (PRIV-12) | before the first pilot |
| PR-18 | **Erasure request workflow** (PRIV-09) | people | identity-verified request, owner, exceptions; comms supplies §5.8 | S3 |

### 15.1 Dependency matrix by slice

"Hard" = the slice cannot open without it; "soft" = the stated fallback applies. A fallback that is "the slice is not
enabled" is written as hard.

| Item | S1 staff text | S2 guardians text | S3 photos | S4 students | Status today | Fallback when soft |
|---|---|---|---|---|---|---|
| PR-6 hr status + exit→iam deactivation | **hard** | hard | hard | hard | payload lacks status | — |
| PR-7 academics/org payloads | **hard** | hard | hard | hard | id-only | — |
| PR-8 iam confirmations | **hard** | hard | hard | hard | refresh refusal verified; event paths to confirm | — |
| PR-9 heartbeat (all six sources) | **hard** (heartbeat only) | **hard** (+ sequences) | hard | hard | missing | — |
| PR-10 jobs runner | **hard** | hard | hard | hard | missing | — |
| PR-12 grants | **hard** | hard | hard | hard | verified; smoke assertion to add | — |
| PR-17 paperwork | **hard** | hard | hard | hard | not started | — |
| PR-19 global-row policy | **hard** | hard | hard | hard | missing | — |
| PR-20 bulk reads | **hard** (academics, iam) | **hard** (sis) | hard | hard | missing | — |
| PR-4 sis restricted link | — | **hard** | hard | hard | missing | — |
| PR-5 sis payloads | — | **hard** | hard | hard | id-only | — |
| PR-14 gateway limits | soft | soft | soft | **hard** | missing | service-side K40 |
| PR-1–3 people consent | — | soft | **hard** | hard | 501, no events | no images |
| PR-22 people purposes | — | — | **hard** (naming) | **hard** (participation) | missing | — |
| PR-11 platform files | — | — | **hard** | hard | fixed 10-min URLs, live-object delete | no images |
| PR-15 image library, media worker | — | — | **hard** | hard | — | no images |
| PR-21 off-lineage safety log | **hard** | hard | hard | hard | missing | — |
| PR-18 erasure workflow | soft | soft | **hard** | hard | missing | school's manual log + §5.8 tools |
| PR-13 delivery providers | — | — | soft | soft | missing | in-app only; on-call phones K34a |
| PR-16 scan adapter | — | — | — (staff uploads) | **hard** | missing | — |

**S1 needs none of the guardian, consent or photo prerequisites.** Media posts stay disabled until S3's hard items are
live and the tenant's consent checkpoint and stream positions are current.

---

## 16. Delivery plan

The earlier phases P0a-1 / P0a-2 / P0b are now four **slices**, each shippable to a pilot school on its own:

| Slice | Scope | Members | Gate to open it (§16.2) |
|---|---|---|---|
| **S1 — Staff text** (was the staff part of P0a-1) | Staff room and class pages with **teachers only**; staff text posts; review for staff posts wider than a home; staff comments and reactions; reports, routine cases, staff-concern route and safeguarding inbox; policy engine for the S1 knobs; switches and kill switch; in-app notifications and digest card; teacher and principal home cards | staff | S1 gates |
| **S2 — Guardians, text** (rest of P0a-1) | Guardians on class pages and the school space; guardian comments (held outside coverage); protected subjects; guardian home card; consent-free (no images) | + guardians | S2 gates |
| **S3 — Photos** (was P0a-2) | Media pipeline and worker, subjects and confirmation, consent and naming projections, surfacing, takedown, consent centre, erasure | same | S3 gates |
| **S4 — Students** (was P0b) | Student submissions, held comments, slow mode, mentions, invite spaces, scan adapter, push/e-mail | + students | S4 gates |
| **P1, P2** | as before (clubs, moments, polls, inter-school, alumni; public release, video) | | |

### 16.1 Batches

| Batch | Content | Size |
|---|---|---|
| **B0a** (platform plumbing, for S1) | PR-9 heartbeat (messaging, for every module), PR-10 jobs runner (core), PR-19 global-row policy and README, PR-21 safety-log bucket and platform append path, PR-12 smoke assertion | medium |
| **B0b** (owner modules, for S1) | PR-6 hr status and exit → iam deactivation, PR-7 academics/org payloads, PR-8 iam event-path test, PR-20 bulk reads (academics, iam) with service-token permissions | medium |
| **B1 — S1 backend (first reviewable slice; ships to a pilot together with B2)** | comms baseline reshaped (§5) for the tables S1 uses; staff projections (hr, academics, iam, org) and refresh; `ClassMembershipDeriver` and `SpaceMembershipDeriver` for staff; staff room; policy engine with §4.5 validation, §4.6 approvals and bootstrap; `SwitchGate`, §4.9; `CommunityAccess`; the feed statement; drafts, submit, review, comments, reactions, saves; reports, cases, staff-concern and safeguarding; in-app notifications; retention and ledger for text; ≈ 60 ops; fast-tier tests | large |
| **B2 — S1 UI** | the S1 screens on web, phone, tablet | medium |
| **B3 — S2** | PR-4, PR-5, PR-9 sequences, PR-20 (sis) first; guardian projections, school space, protected subjects, guardian contexts, digests, guardian screens | large |
| **B4 — S3 photos** | people (PR-1–3, PR-22 naming, PR-18), platform PR-11; §8 backend, media worker, consent state, screens | large |
| **B5 — S4 students** | PR-22 participation, PR-16, PR-13, PR-14; student features | medium |

**S1 knobs in the catalogue:** K01, K02 (staff only), K04 (staff room), K05, K06 (staff), K07, K08, K09, K11, K12,
K13, K21 (off / staff only), K22, K23, K24, K30, K31, K31a, K32, K33, K34, K35, K36, K36a, K36b, K37, K39, K40, K41,
K42, K43. **S2 adds** K02 guardians, K04b, K10, K21 members values. **S3 adds** K04a, K14, K15, K17 (floor), K18, K20,
K34a. **Absent until S4/P1:** K03, K16, K19, K25–K29, K38, K44–K47.

### 16.2 Gates per slice

- **Any slice:** safeguarding lead and deputy named and trained; §4.6 invariants and bootstrap approval; PR-17
  paperwork; fast and heavy tiers green; load test passed in `community-load` (for S1 on the staff dataset); the
  incident runbook (§21) rehearsed with the school's lead.
- **S1:** open decisions 4, 10, 12, 16 and 21 ruled; drills "compromised staff account" and "report against an administrator". Stated residual for S1 (heartbeats
  only, no sequences): a single lost `hr.staff` event is caught by the daily hr checkpoint (≤ 24 h) and a lost
  `iam.account` event by the nightly bulk reconciliation — bounded for deactivated accounts by iam's 15-minute token and
  refused refresh (PR-8); the audience is staff only.
- **S2** (guardians): open decisions 8, 13, 15, 17, 19, 22 and 23 ruled; + "bullying thread in guardian comments" and a **restricted-guardian drill** (a guardian with a
  restricted child and an unrestricted sibling sees nothing of the protected child).
- **S3** (photos): "mistaken wide child image" run inside and outside coverage hours; K34a set; consent checkpoint and
  stream positions current; `GetStoragePolicy` reporting versioning off; open decisions 9, 11, 14 and 18 ruled.
- **S4** (students): all drills repeated with student accounts; participation consent live; scan adapter live; coverage
  staffed; open decision 20 (legal threshold) ruled.

**Effort:** B0a and B0b medium each (they may run in parallel); B1 large (smaller than the earlier B1 + B2 because guardians, consent and photos are out);
B2 medium; B3 large; B4 large; B5 medium.

---

## 17. Ideas that make it feel like *a school*, not a generic social app

- **Class story of the week** — the teacher picks three moments; guardians get one calm weekly card instead of a
  stream.
- **Kudos wall** — recognition for effort, kindness and service, including support staff (bus drivers, cleaners,
  librarians); no counts, no rankings (USE-08).
- **Achievement cards** — generated *only* from official outcomes the school has published (a competition result),
  with the student's and guardian's consent; never from marks.
- **Exam quiet mode** — the school schedules quiet weeks: no student posting or social notifications during exams.
- **Ask the school** — staff-confirmed answers to recurring questions (uniform, timings), searchable; private cases
  routed to the office, never answered in public.
- **Event live board** — during annual day / sports day, staff post moments to the event space; after the event the
  best ones become the album; guardians of participants are notified once.
- **Gratitude notes** — students and guardians send a short thank-you to a teacher, delivered privately, optionally
  shown (with permission) on a staff kudos wall.
- **Yearbook builder (P1)** — curated albums at year end, with consent re-checked per photo and per student.
- **Lost & found / volunteer board** — practical boards for guardians, staff-moderated, auto-expiring.
- **Student newsroom (P1)** — student editors draft, a teacher edits, the issue publishes as a space post series.
- **Digital-citizenship nudges** — the composer gently asks "Is everyone in this photo okay with it being shared?" and
  shows who will see it — teaching good habits instead of only blocking.

---

## 18. Open decisions for the owner

1. ~~Generalise class pages into spaces?~~ **Decided 18 Nov 2026: no.** A class page is one per section per year, the
   sacred space of its enrolled members; the school space is separate and common; a teacher may surface selected class
   posts to the school space (section 5.2.1). Moderation is per school; inter-school has tighter controls (section 19).
2. ~~Policy storage: comms or iam?~~ **Decided 18 Nov 2026: comms.** Community settings and policies are stored in
   comms as versioned rows with proposals and approvals, read uncached from comms' own database — this design as
   written (§4.5–§4.7, §5.5). B1 may start.
3. **Pilot scope:** S1, then S2, then S3 (recommended — each a separate pilot step), or several slices together?
4. **Moderation coverage and minimum staffing — question:** will pilot schools commit a safeguarding lead, a distinct
   deputy, coverage hours and (for photos) an after-hours responder? **Recommendation:** state to every school that two
   willing staff persons are the minimum (§4.6), and that a school without them cannot enable community. Consequence:
   very small schools may not qualify; the alternative (a tenant-level lead covering several schools) needs the
   independent-contact route of item 15 to exist first.
5. **Scan provider** — narrowed: not needed for S1–S3 (staff uploads, originals purged, second-person confirmation,
   §8.3); a provider must be chosen before student or guardian uploads (S4).
6. **Languages for the pilot** beyond English and Hindi.
7. **Alumni** — design now, build in P1 as a separate network (recommended), or postpone entirely?
8. **Non-custodial guardians** (`has_custody = false`): no community membership by default; should a school be able to
   grant them read access per link (some families and courts expect it)?
9. **Students reaching majority — question:** when a student turns 18 (India), what happens to images already published
   under guardian consent, and to guardians' class-page membership? **Recommendation (implemented as the default,
   §8.4):** keep class- and school-width images (they were lawful when published) and tell the student they may now
   decide; hide inter-school images after 60 days unless the student consents; keep guardian membership of the class
   page. Consequence of the stricter alternative (hide everything until the student confirms; end guardian membership
   unless the student agrees): more protective of the adult, but class histories lose photos and guardians lose the
   class view abruptly. Legal to confirm either way.
10. **Retention values** in §5.6 (archive 2 years, routine cases 3 years, safeguarding evidence reviewed yearly with a
    7-year default) — owner and legal to confirm before the pilot.
11. **Second-person image confirmation** is a floor for every image at every width, including class pages and images declared "no students"; after the pilot, may
    a trusted teacher confirm alone at class width?
12. **Tenant ceiling disclosure — question:** CLAUDE.md says tenant settings are set by tenant administrators only and
    "ordinary users cannot … even see" them. May a school's community owner see which values the tenant ceiling lets the
    school choose? **Recommendation: yes, only that** — COM-40 offers the allowed values and marks the rest "not available
    for your school", never showing the tenant's value, setter, history or reason (§4.6). Consequence of yes: schools can
    configure themselves without trial and error; the ceiling's position is inferable. Consequence of no: COM-40 must
    offer every value and refuse on save with a reason-free error, which is confusing and still reveals the ceiling by
    refusal — so a strict reading is not achievable in practice; the owner should state which reading of the rule applies
    to institution administrators.
13. **Guardian disagreement on photo consent — question:** the most restrictive legal guardian decides; what may each
    guardian see? **Recommendation (designed, §12.7):** only their own choice and the effective result, with no wording
    about other guardians and no reason for a difference. Consequence: no co-guardian choice is disclosed by the product;
    a guardian puzzled by a narrower result is sent to the school office, which handles disputes privately (PRIV-08).
    Legal to confirm that "most restrictive wins" is the right rule.
14. **People in photos who are not members** (visiting children, parents at events): unidentified children always
    block; unidentified adults block by default (K18). Acceptable for event recaps?
15. **Independent safeguarding route:** will tenants name a tenant-level safeguarding contact? Without one, cases about
    a school's lead, principal or community owner rely on the external contact text alone.
16. **Processors and data location** (PR-17): which push/email providers, and GCS region, are acceptable to schools.
17. **Deferring MEM-02 and MEM-03 — question:** may invitation-based spaces (MEM-02) wait for S4, and may assisted
    onboarding and account recovery for users without a personal e-mail (MEM-03) stay with iam/sis rather than this
    project? **Recommendation: yes to both.** Consequence: S1–S3 needs no invitation feature (class pages and the school
    space are relationship-based; a QR code never proves membership), and families without e-mail are onboarded as today
    (the administrator sets the first password); the cost is that guardians who share one phone number cannot each
    recover their own account until iam builds a recovery design. Consequence of no: an invitation subsystem (S1–S3 + ≈ 6
    operations) and an iam recovery design become pilot blockers. Needs the owner's recorded sign-off before the pilot.
18. **Platform on-call — question:** will MySMS staff a 24×7 platform on-call able to stop media for one school
    (`media_delivery_off`, §21)? **Recommendation: yes before photos (S3).** Consequence of no: outside coverage hours
    the only protections are report-to-withhold and the school's own responders (K34a); an incident reported to the
    school by phone at night waits for a school responder.
19. **Automatic repair from people and hr — question:** may people and hr subscribe to comms' event
    `comms.stream_repair.requested` and answer with a replay for that tenant (an event-only subscription against the
    dependency arrow, like the event-only references ARCHITECTURE §2 already allows)? **Recommendation: yes.**
    Consequence of yes: a lost consent or staff event repairs itself within minutes. Consequence of no: an on-call
    operator must run the replay; until then that tenant's images (or staff decisions) stay blocked.
20. **Legal threshold for minors — question:** this design treats everyone under 18 (people's `adult` flag) as a minor
    for consent and requires a guardian's `community_participation` consent before a minor joins as a student member;
    the under-13 bands are product design only. **Recommendation: confirm with legal (DPDP) before S4.** Consequence:
    secondary students cannot participate without a guardian's recorded consent.
21. **Load-test environment — question:** who owns `community-load` (§14) and its cost (a few days a month of a
    recommended-size stack)? **Recommendation:** operations, run before each pilot and monthly while pilots run.
    Consequence of not funding it: the p95 target stays unproven and the pilot gate cannot be met.
22. **Text revocation bound when sis or iam is silent — question:** round 3 made `closed` per tenant and only after 60
    minutes of silence (to avoid one tenant or a blip closing everyone), which let a guardian restricted during an sis
    outage keep reading text for up to an hour. **Recommendation (designed as the default, §7.3):** close **guardian**
    contexts after 15 minutes of sis or iam `degraded`, keep staff working, close everything at 60 minutes, suspend
    during declared maintenance. Consequence: a 15-minute sis or iam outage takes community away from guardians (who
    also lose sis's own portal during it); the alternative keeps them reading for up to 60 minutes.
23. **Naming students before naming consent (S2) — question:** may a class-page post name students of that class by
    first name before the `community_naming` consent exists? **Recommendation: yes, first names only, class page only;
    no credits, mentions or surnames; school-space posts name no student.** Consequence of no: S2 posts cannot name
    any child until S3, which makes class updates stilted; consequence of yes: class families learn first names they
    usually already know from class life.

---

## 19. Inter-school community — tighter by design

An inter-school space connects several schools **of the same tenant** (a chain or trust): teacher practice circles,
a joint science fair, an inter-house competition across campuses, a chain-wide bulletin. Schools in *different*
tenants are out of scope (NET-15: that needs a federation agreement and its own design). Everything below is stricter
than inside one school, because each school answers to its own families.

### 19.1 Principles

1. **Two keys to exist, one key to leave.** The tenant (chain) administrator creates the space and its ceiling; it
   becomes active for a school only when **that school's community owner accepts the invitation**. The tenant cannot
   make a school active, cannot add any member, and cannot widen the ceiling for a school that has not re-accepted
   (§19.5). Any school can leave at any time; leaving withdraws its content from the space immediately.
2. **The content-owning school keeps control of its content.** A post made by school A's member stays school A's: A's
   moderators review it before it leaves A, A can withdraw it at any time, and A's consent rules apply to its
   students. Other schools can only *hide it in their own view* and report it.
3. **People do not cross; selected content does.** No member directory across schools, no profiles, no mentions of
   another school's members, no direct messages, no "who reacted" lists. Students never interact with students of
   other schools by default.
4. **Staff-first.** The default inter-school audience is staff; students and guardians are added only per space, only
   in read-only or curated-showcase mode unless the tenant ceiling and every participating school allow more.
5. **Strictest school for content rules, each school for its own members.** What *kinds* of content the space allows
   (student showcases, photos with students, comments, polls) is the intersection of every active school's settings —
   recomputed at read time whenever a school joins, leaves or narrows, so a stricter school joining hides content that
   is no longer allowed. *Which of its members* see the space is each school's own choice.

### 19.2 What can and cannot be shared

| Content | Inside one school | Inter-school space |
|---|---|---|
| Staff text updates, resources, teaching ideas | yes | **yes** (staff audience by default) |
| Official school announcements (chain bulletin) | yes | **yes**, from tenant/school official publishers only |
| Event announcements, results of official competitions | yes | **yes**, as official outcomes published by the organising school |
| Student work showcase (project, art, writing) | yes, with consent | **only curated**: chosen by staff, re-reviewed by the owning school, student named only with `community_naming` consent at `inter_school` width (otherwise "a Grade 8 student of School A") |
| Photos with students | yes, with photo consent | **only** with consent at `inter_school` scope (PR-1) for every depicted student; any unidentified person blocks; no close-up of a student without their own consent |
| Class page content | class members only (unless surfaced to the school space) | **never directly**; a surfaced copy is **not eligible** either: the school's community owner may create a new inter-school share from it, which is a new post with full origin re-review and inter-school consent check; no automated path from surfacing to sharing |
| Student comments and reactions | per school policy | **off** for students; staff comments only, per space |
| Student-to-student contact, mentions, profiles | per school policy | **never** |
| Guardian discussions | per school policy | **never**; guardians at most read the chain bulletin |
| Member lists, rosters, class names of individuals | per school policy | **never** |
| Marks, attendance, fees, health, discipline, safeguarding, addresses, phone numbers, transport | never published | **never** (§11.4 of the brainstorm, and a field filter on inter-school posts) |
| Moments (expiring stories) | per K27 | **off** |
| Polls | per K26 | staff-only polls; results aggregated across schools, no per-school breakdown unless every school agrees |
| Download of original media | per K19 | **off**; viewers get display variants only |
| External/public release | P2 | **never from an inter-school space**; only the owning school can release its own content, from its own school space |

### 19.3 Who controls what

| Control | Tenant (chain) administrator | Each participating school's community owner | Inter-school panel (incl. chair) | Content-owning school's moderators |
|---|---|---|---|---|
| Allow inter-school spaces at all (K46) | **sets** | — | — | — |
| Create a space, set its purpose, ceiling, end date; invite schools | **sets** | proposes | — | — |
| Accept / leave the space | — | **decides for its school** | — | — |
| Which of its members may see / post | — | **decides** (within the ceiling) | — | — |
| Approve content leaving the school | — | — | — | **decides** (every post, before the panel sees it) |
| Hide content in its own school's view | — | **can** | — | — |
| Remove content from the whole space | — | — | **can** (with reason; the owning school is notified) | **can** (withdraw) |
| Freeze the whole space | **can** | — | **can** (chair or two panel members) | — |
| Handle reports | — | — | **triage** content only; any case about a person goes to that person's school | **owns cases about its members** |
| See reporter identities and evidence | — | — | never another school's reporter; only the content | only for cases it owns, and the reporter's identity only if the reporter's school shares it with the reporter's agreement |

### 19.4 The inter-school moderation panel

- Each participating school nominates one moderator; the tenant nominates a chair. The chair's powers are those of a
  panel member plus freezing the space alone; the chair cannot read cases, reporter identities or evidence of any school
  and cannot publish.
- **Order of operations:** origin school review and the `inter_school` consent check complete → `released_by_origin`
  → panel queue (when the space requires panel review) → `published`. **A panel member never sees content the origin
  school has not released**; the panel queue shows released content in full (including images, since they will reach the
  whole audience) with students as "a student of School B" and no names, credits or subject lists of another school.
- **Case ownership by subject:** content of school A, or a member of school A (staff or student) → case owned by A's
  moderation team (or A's safeguarding route, including the independent route when A's lead or principal is implicated);
  reported by school B's teacher → B's handler knows the reporter; A's handler sees "a member of another participating
  school" unless the reporter agrees to be named. The panel keeps only the content action (hide/remove) and its reason.
- **Audit:** every panel read of another school's content and every panel action writes an audit row visible to the
  content-owning school's community owner.

### 19.5 Data and enforcement

- `community_space(space_type = inter_school)` with **`community_space_school`**: `space_id, institution_id, status
  (invited|active|left|declined), invited_by, invited_at, accepted_by, joined_at, left_at, member_categories,
  settings_override (only narrowing), accepted_ceiling_version`.
- **Invitation state machine:** `invited` → `active` (school owner accepts, choosing categories and narrowing) |
  `declined`; `active` → `left` (school owner, any time); `left` → `invited` (tenant re-invites). Tenant changes to the
  ceiling: narrowing applies at once to everyone; **widening** creates a new ceiling version, and each school stays on
  the version it accepted (`accepted_ceiling_version`) until its owner re-accepts — the space's effective rules for that
  school's members and content never widen without that school.
- A post into an inter-school space carries `origin_institution_id`; its revision passes the origin school's review
  before `released_by_origin`, then the panel's.
- `CommunityAccess` admits a reader only if their school's row is `active`, their category is allowed by *their* school
  for that space, and the post's origin school is still `active` (a school that leaves takes its posts with it,
  instantly — enforced at read time, not by a cleanup job, §9.2).
- Consent uses `scope = inter_school` (PR-1) for every depicted student.
- Per-school hides: `community_space_hide(space_id, institution_id, post_id)` filtered in that school's members' feed.
- **Field filter and its limits:** inter-school posts pass a filter for phone numbers, e-mail addresses, roll and
  admission number patterns, marks/percent patterns and the institution's own class names next to a person's name; a
  match holds the post. It **cannot** catch free-text identification ("our Priya from 8B…"), nicknames, a name tag,
  uniform or logo in a photo. Compensation: **every inter-school post with any student content (student author, credit,
  subject or a student named in text by the author's declaration) needs human review by the origin school and by the
  panel**, and student content is staff-chosen only.

### 19.6 Screens (in addition to section 12)

- **COM-50 Inter-school spaces** (tenant administrator, W): create, ceiling, end date, invitations, panel.
- **COM-51 Inter-school invitations** (school community owner, W/B): accept/leave, choose member categories, narrowing
  overrides, re-accept a widened ceiling, see what of the school's content is shared.
- **COM-52 Share to inter-school** (school community owner / staff publisher, W/B): pick a school-space post, see the
  consent check for every depicted student at inter-school scope, submit to the origin review.
- **COM-53 Inter-school panel queue** (panel moderators, W/B).

### 19.7 Phase

Inter-school spaces are **P1**, after per-school community runs well: first staff-only practice circles and the chain
bulletin; curated student showcases only after a school has run S4 successfully.

### 19.8 When a school leaves

| Item | Owner | On leave |
|---|---|---|
| The leaving school's posts | the leaving school | invisible at once (read time); kept in the school's own data under its retention, withdrawable; re-visible only if it rejoins and re-releases them |
| Other schools' staff comments on those posts | the commenter's school | invisible with the post (there is no quote or repost feature, so nothing else carries the text); deleted with the post at its retention |
| The leaving school's members' comments on other schools' posts | the leaving school | hidden at read time (origin of the comment no longer active) |
| Reports and cases about the leaving school's content or members | the leaving school (as before) | continue in that school's queue; the panel's content-only record stays with the panel |
| Panel cases (content actions) | the panel | kept with their reason codes; no content copy beyond the evidence record |
| Evidence records and legal holds | the case owner | unaffected by leaving; retention and hold rules of §5.6 apply |

---

## 20. Brainstorm traceability (gate L4) and outbound data flows

### 20.1 P0 items the gate asked about

| Brainstorm id | Where | Test / status |
|---|---|---|
| MEM-02 invitations | class pages and the school space need none (relationship-based); invite-type spaces (`AddSpaceMembers`) are **deferred to S4**: invitations are per space, to an existing account of the institution, expire after 7 days, single-use, revocable, rate-limited (K40); a QR code never proves membership. Account invitations are iam's (iam sends none yet) | deferred — **open decision 17** (owner sign-off before the pilot) |
| MEM-03 assisted onboarding, recovery, shared phones | iam and sis portal accounts (administrator sets the first password today); community addresses notifications to accounts, never phone numbers (§11.1) | account recovery deferred to iam — **open decision 17** |
| MEM-04 rules before participation | COM-11 | client test; acceptance stored |
| MEM-05 essential vs optional | K02 + COM-11; a member may hide community for themselves (`SetCommunityPreferences`); notices and learning never depend on it (§13.15) | `CommunityPolicyMatrixComponentTest` |
| PRIV-02 separate consent classes | participation (`community_participation` for minors, COM-11 acceptance for adults), depiction (`photo_video_publication` per width), naming (`community_naming` per width), directory (K32), external (`public` scope, P2), marketing and channels (people's own purposes) | PR-1 tests in people |
| PRIV-06 paper evidence | §8.4: evidence file, recorder, second-person verification; imported rows `unverified` | people PR-1/2 tests |
| NET-16 fee independence | §10.3 | `CommunityAccessFeeIndependenceTest` |
| NET-17 no global search / contact sync | §13.13; mention candidates only from the home (S4) | component test on mention search |
| SAFE-05 urgent help | K35 "urgent help" category, COM-07 urgent path with staffed hours | client + routing test |
| SAFE-12 scraping, bulk download | K40 limits on writes and on media (60 batch calls, 2,000 thumb/feed and 100 full-size variants per member per 10 min, §8.1), no bulk download operation, originals never served, PR-14 at the gateway | rate-limit component tests |
| PRIV-12 vendors, logs, no model training | §13.12, §20.2; no student content is sent to any model or analytics provider; PR-17 | `CommunityLogRedactionComponentTest` |
| SAFE-17 drills | §16.2 | recorded per phase |
| MEM-06, MEM-07, MEM-12 | §7.7, §7.5, open decision 9 | revocation and archive tests |
| PRIV-07, PRIV-09, PRIV-10, PRIV-11 | §12.7, §5.8, §5.6, §5.6/§10.2 | |
| CFG-07, CFG-08, CFG-12, CFG-13 | §4.4, §4.6, §4.7, §7.4/§4.6 | matrix and kill-switch tests |

### 20.2 Every outbound data flow

| Flow | Data | Processor / location | Status |
|---|---|---|---|
| Object storage (GCS) | image variants (pixels only, no metadata); evidence pixels only under a safeguarding hold | Google Cloud, the deployment's bucket region | in use by platform; DPA under PR-17 |
| Event bus (RabbitMQ / Pub/Sub) | ids, versions, codes; no text or names | MySMS infrastructure | in use |
| Push (FCM/APNs), S4 | notification id and the fixed generic text | Google / Apple | not built; DPA under PR-17 |
| Email, S4 | generic text, digest counts, link | provider to be chosen | not built; DPA under PR-17 |
| Scan provider, S4 | image bytes for scanning, no names | to be chosen (open decision 5) | not chosen |
| Logs and metrics | ids, codes, timings; never bodies or names (§13.12) | MySMS log store | redaction test |
| Analytics | none; `GetCommunityHealth` aggregates are computed in the database and shown only in the product | — | — |
| Client telemetry (crash reporting, RUM) | none today (no SDK in the web, mobile or tablet apps); if one is added it must drop URLs, query strings and screenshots from community screens, and it is listed here first | — | rule |
| GCS and load-balancer access logs | signed URLs (path + signature) — never names or bodies; the stable media path carries only ids | ops log store | 30-day retention |
| Search index | none exists; no external search | — | — |

### 20.3 Review improvements not (yet) taken

- **Face count to check declarations** (GAP-REVIEW 2.1 help): deferred — running a face detector on children's photos
  needs its own privacy review and a library choice; the second-person look on every image is the control.
- **Data-subject access export** (improvement 9): designed as `GetMyChildCommunityData` but scheduled for S4, not S3,
  because people's request workflow (PR-18) must exist first.
- All other improvements of the review were taken (stable media path and batch URLs, screen protection, idempotency
  keys, archive reads, notification hygiene, guardian identity, health bands, separate view and review roles,
  independent second reader, search rule, contradiction pass, decisions gating B1 at the top). Round 4: all eight
  re-check improvements were taken (N-6, N-10, N-14, N-15, N-16, N-17, N-19, N-20).

---

## 21. Incident runbook and kill switches

**Switches, narrowest first:** `EmergencyTakedown` (one item, purge) → `FreezeHome` → `FreezeInstitution` (K37) →
K01 off (institution) → tenant community off → platform `SetCommunityGlobalSwitch`: `community_off` (all tenants),
`media_delivery_off` (no media URL anywhere), `notifications_off`. All are read uncached (§4.7) and need no
deployment. `KillSwitchComponentTest` runs two service instances on one database with warm policy memos and asserts each
switch refuses the very next request on both instances, stops queued dispatch and digest items, and that unfreezing
publishes nothing.

**Who may act at any time.** `EmergencyTakedown`, `FreezeHome` and `FreezeInstitution` are permissions, not
coverage-hour duties: the community owner, every moderator (K33), the safeguarding lead and deputy and the after-hours
responders (K34a) may use them at any hour from a phone (COM-27, COM-44). Coverage hours (K34, open decision 4) govern
review SLAs, not the ability to take something down.

**Outside coverage hours** (K34):
- **Report-to-withhold:** a report in the categories `privacy`, `inappropriate image` or `urgent help` on a post with
  images, made by a member of the home, **withholds the reported images at once** for everyone except moderators (the
  text stays) until a moderator decides; a withheld image whose report is dismissed is restored with no trace to the
  reporter. Abuse limits (K40 floor): **3 withholds per member per day**; a member with 2 dismissed withholding reports
  in 30 days loses the withhold effect (their reports are still filed and reviewed, just not auto-withheld) and the
  moderator sees their dismissed count; audited. So the exposure after the **first report** is seconds, whoever is
  awake.
- **Text outside coverage:** guardian comments are held for review outside coverage (K21 default); school-wide posts to
  guardians or students wait for review anyway (K07 floor) and are reviewed at the next coverage start; urgent matters
  go through notices, not community.
- **Alerting:** the same reports alert the K34a responders — in-app always; push/SMS when the providers exist (PR-13);
  until then (S3) the school gives the responders' phone numbers to the platform on-call, which phones them.
- **Late images:** images published at institution width or wider during the last two covered hours before a gap
  (e.g. Friday 14:00–16:00) get a second-reader check at the next coverage start.
- **Platform on-call** (open decision 18 asks whether MySMS staffs one 24×7): may set `media_delivery_off` for **one
  institution or tenant** (§4.7) without viewing any content, informs the tenant administrator and the community owner,
  and only a platform operator clears it.
- **Exposure, Friday 18:00 to Monday 08:00, image never reported:** it remains visible to its audience until someone
  notices — that cannot be bounded by software; **once reported:** seconds (withhold) + client cache ≤ 120 s; **reported
  through the school by phone instead:** as fast as a responder or the platform on-call acts (target 60 minutes).

**Mistaken wide child image** (a photo of a child without consent at that width is published):

| Time | Who | Step |
|---|---|---|
| T+0 | anyone | report ("privacy" / "inappropriate image") or staff discovery |
| ≤ T+15 min inside coverage; seconds outside coverage (report-to-withhold), then ≤ 60 min (K34a responder or platform on-call) | case handler, community owner or K34a responder | `EmergencyTakedown` on the post or image: evidence record (hashes, ids, trail — pixels only if the safeguarding lead holds them), deny, purge of every generation (URLs dead in seconds); if the scope is unclear, `FreezeHome` or `FreezeInstitution` |
| ≤ T+30 min | safeguarding lead | informed automatically (privacy/image categories notify the lead); decides whether it is a safeguarding matter |
| systemic cause (a bug, many items, several tenants) | platform operator | `media_delivery_off` for all tenants, then fix, then re-enable per tenant |
| ≤ T+24 h | school | inform the child's guardians (template), record the case outcome |
| ≤ T+72 h | owner / legal | assess breach-notification duties (DPDP); post-incident review |

**Compromised staff account:** iam deactivate or force reset (comms denies on the event); any moderator or the community
owner runs `OpenSecurityIncident` (one step) and, under that case, `RevokeMemberAccess` at once without waiting; `ListAuthorActivity` lists the account's posts, comments and reviews of
the last 30 days for the moderator to restrict or remove; any approval the account gave in that window is re-reviewed;
if it held `manage_policy`, pending and recent widenings are reverted from COM-43.
