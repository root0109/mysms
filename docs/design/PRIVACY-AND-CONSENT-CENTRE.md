# Privacy and Consent Centre — technical design

The `privacy` module and the `privacy-client` library: consent, notices, the processing register, data-principal
rights, retention and breaches under India's DPDP Act 2023 and DPDP Rules 2025. Approved by the owner. Open work and
its order: ROADMAP §3.1 (SEC-11). Questions for counsel: [PRIVACY-LEGAL-REVIEW-BRIEF.md](PRIVACY-LEGAL-REVIEW-BRIEF.md).
Binding rules stay in [`CLAUDE.md`](../../CLAUDE.md), [`ARCHITECTURE.md`](../../ARCHITECTURE.md) and
[`JAVA-CODING-RULES.md`](../../JAVA-CODING-RULES.md).

Each section says what is **built** (present tense) and what is **planned** (marked *Planned*). Sources of truth for
what is built: the baseline `database/migrations/privacy/V1_220_001__privacy_baseline.sql`, the contract in
`com.mysms.privacy.api`, `backend/privacy/src/main/resources/proto/mysms/privacy/v1/privacy.proto` and the operation
table `META-INF/mysms/operations/privacy.json`.

This design absorbs: the subject-level part of ROADMAP ARC-6 (export, erasure, retention; `platform.ExportMyData` and
`platform.retention_policy` are replaced by §9–§10); people's former consent table and operations (removed); the
consent prerequisites of the community network design
([`SCHOOL-COMMUNITY-NETWORK-TECHNICAL-DESIGN.md`](../product/SCHOOL-COMMUNITY-NETWORK-TECHNICAL-DESIGN.md) §8.4 rule
K17, §12.7, its consent-centre panel COM-39 in ui/02 ACC-13, PR-1, PR-2, PR-3, PR-18, PR-22); and the DPDP notes of
[`CLASSROOM-VOICE-ASSISTANT-BRAINSTORM.md`](../product/CLASSROOM-VOICE-ASSISTANT-BRAINSTORM.md) §8.

---

## 1. Summary, goals, non-goals, and where it lives

### 1.1 Summary

MySMS processes children's personal data at scale: names, photos, marks, attendance, health notes, fees, bus GPS
positions, learning analytics. Under the DPDP Act each school (the tenant) is the **Data Fiduciary** and MySMS, for
almost everything, its **Data Processor**. The fiduciary needs tools a processor must supply: notices, verifiable
parental consent, purpose-bound processing, rights requests with deadlines, erasure that reaches every module,
retention and a breach register.

`privacy` is a domain module with its own schema. It owns:

1. the catalogue of purposes, data categories and processing activities (a RoPA-lite register);
2. versioned, multilingual notices;
3. consent records with evidence, guardian capacity and verification, and a precomputed **effective consent state**;
4. a small decision API (gRPC) and the `privacy-client` facade every module uses to ask "may I process subject S for
   activity A at scope W?" in batch, cached, fail-closed;
5. data-principal rights requests with deadlines and (planned) event-driven fan-out to the modules holding the data;
6. *Planned:* a retention and deletion engine (policies per data category, legal holds, per-module sweeps);
7. a breach register with the Board's 72-hour clock and the notices recorded (sending them through comms is planned).

### 1.2 Goals

| # | Goal | Measured by |
|---|---|---|
| G1 | No processing that needs consent happens without a live, verified consent at the right scope | Every enforcement point in §8 has a component test that proves a refusal blocks it |
| G2 | One place for parents, adult students and staff to see and change what they agreed to | The consent centre (ACC-13) on privacy's self-service API |
| G3 | Rights requests answered within the deadline the tenant configures (default 30 days; legal maximum to verify) | SLA dashboard; overdue = 0 |
| G4 | Erasure reaches every module holding the subject's data, idempotently, with evidence | Per-module task receipts; `ErasureSagaComponentTest` across a monolith with all modules |
| G5 | Consent checks do not slow hot paths | p99 of a batch check of 500 subjects < 15 ms in-process, < 40 ms remote; cache hit ratio > 95 % |
| G6 | Ready for DPDP's main obligations (13 May 2027) with a margin | Phases P1–P3 of §15.1 done before then |

### 1.3 Non-goals

- **Not an authorisation service.** Who may run an operation stays with iam roles, auth-client's filter and
  `DataScope` (JAVA-CODING-RULES §16). Consent answers a different question: *may this data be used for this
  purpose*, whoever asks. API permissions are published to iam, not to privacy (JAVA-CODING-RULES §16.2).
- **Not a general policy engine** (no OPA/Cedar; §15.4 keeps the door open).
- **Not a legal opinion.** Statements of law are engineering inputs; anything marked *to verify with counsel* is
  confirmed before the feature it drives ships.
- **Not a tenant-offboarding tool.** Whole-tenant export and erasure (ARC-6) reuse the per-module handlers of §9.1 but
  are a platform operation.
- **No field-level redaction framework.** Modules omit data by explicit mapping, never by reflection (§3).

### 1.4 Why a separate `privacy` module

`privacy` is the 22nd domain module, with its own schema, service and contract; in the decided reorganisation
([MODULE-AND-PRODUCT-REORGANISATION.md](MODULE-AND-PRODUCT-REORGANISATION.md)) it stays a kernel module of its own,
not part of `identity`.

| Option | For | Against |
|---|---|---|
| A. Inside `people` | No new service; consent beside the person | people is a profile store; rights orchestration, retention, breach and notices are a different capability with a different user (the school's grievance / data-protection officer). people would receive completion events from all modules and become the busiest event consumer; it is read by 8 modules and consent checks would make it hot for every module |
| B. Inside `iam` | iam is on every request | ARCHITECTURE §6 rule 3: identity scales on its own and stays small and security-reviewed. Consent is not authentication; mixing invites "consent as a permission" |
| C. Inside `platform` | Tenant infrastructure; owns files and audit log | platform is read-mostly, changed by tenant admins; consent and requests are written by parents and students at scale |
| **D. New `privacy` module (chosen)** | One capability, one owner, one audit boundary; scaled and secured on its own; narrow, stable decision contract; orchestration in one place | One more service (in the monolith one more schema and a few beans); one more proto copy (inside `privacy-client`) |

D meets the merge rules of ARCHITECTURE §6: a distinct capability with distinct users (rule 1), weak synchronous
coupling (subject facts arrive by events, rule 2), its own sensitivity and audit regime (rule 3).

**Place in the dependency graph** (ARCHITECTURE §2):

| Module | Calls (gRPC) | Learns from events only |
|---|---|---|
| `privacy` | none today. *Planned:* `platform` (files for evidence and export packages) | `people.person.*`, `sis.student.*`, `sis.student_guardian.*`, `hr.staff.*`, `<module>.privacy_task.completed` of each module of `mysms.privacy.erasure.modules`. *Planned:* `<module>.retention_run.completed` |
| modules that enforce consent or hold personal data | `privacy` through `privacy-client` (people today; §8 lists the rest). *Planned:* `ListActiveHolds` from their erasure and retention handlers | *Planned:* `privacy.rights_request.task_assigned`, `privacy.retention.due` |

privacy never calls a module that calls privacy, so the graph stays acyclic: guardian links and enrolments arrive as
events (the "event-only, against the arrow" pattern of `database/logical-references.md`). Erasure and export are
events out, events back, never gRPC from privacy into the modules.

---

## 2. Regulatory drivers (engineering view)

Sources: the DPDP Act 2023 and the DPDP Rules 2025 (notified 13/14 Nov 2025; summary in
`CLASSROOM-VOICE-ASSISTANT-BRAINSTORM.md` §8; commencement questions: legal brief Q1). Phasing: Board provisions at
once; consent-manager provisions from 13 Nov 2026; children's data, notice and consent standards and most fiduciary
duties from **13 May 2027**. Each row is a requirement on the product, phrased so it can be tested.

| Ref | Obligation (summary) | What the product does | Confidence |
|---|---|---|---|
| Act s.5, Rule 3 | Notice before or at consent: itemised data and purposes, how to withdraw, how to complain, how to reach the Board; plain language; English and Eighth Schedule languages on request | Versioned, itemised notices per purpose; translations per language; the notice version shown is recorded with every consent; withdraw and complaint links in every notice | High; itemisation format *to verify* |
| s.6 | Consent free, specific, informed, unconditional, unambiguous, by clear affirmative action, limited to necessary data; withdrawal as easy as giving | No pre-ticked boxes; one purpose per toggle; no non-essential purpose bundled into enrolment; withdraw is one tap on the same screen | High |
| s.6(7)–(9), Rule 4 | Registered Consent Managers may give, manage, review and withdraw consent for a principal | Records carry `channel = consent_manager` and an external artefact id; inbound adapter planned (§15.4) | Medium: artefact standards *to verify* |
| s.7 | Legitimate uses without consent (voluntary provision for a specified purpose, legal obligation, medical emergency, employment …) | Every processing activity records its **legal basis**; non-consent bases skip the consent check | High that the section exists; which school activities fit *to verify* (brief Q2–Q8) |
| s.8(4)–(5) | Reasonable security safeguards | RLS, `_enc` columns, no PII in logs, privacy's own evidence ledger | High |
| s.8(6), Rule 7 | Breach: intimate the Board and each affected principal without delay; detailed report to the Board within 72 hours | Breach register with two clocks; templated notices through comms (§11) | High on 72 h; contents *to verify* |
| s.8(7) | Erase when consent is withdrawn or the purpose is served, unless law requires retention; make processors erase | Erasure saga, retention engine, per-module handlers, legal holds (§9–§10) | High |
| Rule 8 + schedule | Specified classes erase after inactivity with 48 h prior notice; logs kept at least one year | "Inactive for N" triggers with pre-erasure notice; audit and access logs ≥ 1 year | Medium: whether schools are a class *to verify* |
| s.8(9)–(10), s.13 | Publish a contact who answers questions; grievance redressal within the period the Rules fix (stated as up to 90 days) | Grievance request type with a deadline; every notice version names the grievance officer | Medium on the period |
| s.9(1), Rule 10 | **Verifiable consent of a parent or lawful guardian** before processing a child's (under 18) data; check the giver is an identifiable adult (reliable details held, or a virtual token such as DigiLocker) | Guardian capacity and verification on every consent; a minor's own decision never counts | High on the rule; DigiLocker *to verify* |
| s.9(2)–(3) | No processing harmful to a child; **no tracking, behavioural monitoring or targeted advertising directed at children** | Purposes `behavioural_analytics`, `targeted_advertising`, `biometric_or_video_monitoring`, `product_analytics` are forbidden for minors (no consent can allow them) | High |
| Fourth Schedule | Educational institutions exempt from s.9(1) and 9(3) for tracking restricted to educational activities or children's safety | `processing_activity.education_exemption_clause` records the clause relied on; read narrowly | Medium: scope contested (brief Q3, Q4) |
| s.11 | Summary of the personal data processed, the processing activities and the recipients | Access summary from the register (§9.6) | High; "may concern" vs "applies to" (brief Q15) |
| s.12 | Correction, completion, updating, erasure | Correction routed to the owning module's update operation (§9.4); erasure saga (§9.2) | High |
| s.14 | Nominate a person to exercise rights on death or incapacity | `privacy.nominee` and a nomination request (§9.4) | High |
| s.10 | Significant Data Fiduciary (if notified): DPO in India, independent audit, DPIA | Reports exportable for an auditor | Medium |
| Penalties | Up to ₹250 crore for security failures; up to ₹200 crore for breach-notification and children's obligations | Prioritise G1, G3 and the breach clock | Amounts *to verify* |

**POCSO and safeguarding.** Counselling notes, health records and incident cases can hold disclosures staff must
report under the POCSO Act 2012. Withdrawal and erasure never delete records under a safeguarding hold; a guardian's
access request excludes case files about that guardian's own conduct (counted, not shown); `health` and `counselling`
are restricted categories whose erasure needs a second reviewer. All *to verify with counsel*; a hold always wins over
automation.

**Other retention law.** School registers, transfer certificates, board exam records, accounts (commonly 8 years) and
payroll records impose minimum retention that overrides erasure. Each retention policy carries a `legal_reference` so
counsel reviews one table (brief Q11).

**Roles.** The tenant is the fiduciary and MySMS the processor; notices name the school. Where MySMS acts on its own
account (its own marketing, cross-tenant product analytics) it is the fiduciary: those purposes have
`fiduciary = platform`. Model training on children's data is not offered.

---

## 3. What the reference projects do, and what MySMS takes

`plt-access-control-service` (ACS) combines RBAC with a FHIR-flavoured consent model: versioned **consent templates**
(`parent_id` groups versions; DRAFT / PROPOSED / ACTIVE / REJECTED / INACTIVE / ENTERED_IN_ERROR; PERMIT / DENY;
JSONB action, scope, category, purpose, policy, security label; an attachment for the legal text), **field groups**
published by each service at start-up, and **consent assignments** (giver, receiver user/role, organisation,
validity window, performer, verification fields, field groups down to a resource id). A ShedLock scheduler
soft-deletes expired assignments; `LegalDocumentService` derives ACCEPTED / ACCEPTED_EXPIRED / REVIEW_REQUIRED /
NOT_ACCEPTED per user; `BaseGdprService.hardDelete(subjectId)` returns FAILED / PARTIALLY / SUCCEED; Redis is
populated with assignments, templates, field groups and roles, guarded by "population in progress" keys.
`access-control-library` is the embedded client: `checkConsentOnResource` per action returning consented fields,
reflection-based field blanking, list filtering, Redis first with gRPC fallback, a global on/off flag, "own data is
always consented", start-up publishers and a `PermissionsFilter`.

| Reference idea | Verdict | In MySMS |
|---|---|---|
| Versioned template with version group and lifecycle | Adopt, simplified | `notice` + `notice_version` (draft → active → retired); `requires_reconsent` on a material change |
| Accepted / expired / review-required / not-accepted per user | Adopt | `MyConsentStatus`: `GRANTED`, `REFUSED`, `NOT_ANSWERED`, `RECONSENT_REQUIRED`, `EXPIRED` |
| PERMIT vs DENY | Adopt as `decision` | Explicit `refused` records; most restrictive guardian wins (K17) |
| Validity window, verified, verified-with, verification date, performer | Adopt | `valid_from`/`valid_until`, `verification_status`, `verification_method`, `verified_by_user_id`, `recorded_by_user_id` |
| Field groups published at start-up | Adapt: build time | *Planned:* per-module manifests `META-INF/mysms/privacy/<module>.json` checked by conformance; today the baseline seeds the register |
| `hardDelete(subjectId)` with typed outcome | Adopt and extend | Per-module task handler for erase / anonymise / export / restrict, idempotent by task id (§9.1; built in every module holding personal data except lms) |
| Scheduled expiry with a cluster lock | Adopt | Expiry and majority jobs on core's job runtime (ARC-4, `privacy.job_schedule`) |
| Batch checks over resource ids | Adopt | `BatchCheckConsent`, `ConsentChecker.checkAll` |
| Cache first, remote fallback | Adopt | Per-process Caffeine cache in `privacy-client`, TTL only (§7.2) |
| Global enable flag | Adapt | *Planned:* `off / shadow / enforce` when the first rollout needs it; enforce-only today |
| Owner always consents to own data | Adopt | Self-access never consults consent |
| Attachment with the legal text | Adopt | `notice_version.document_file_id` plus the rendered text in `notice_translation` |
| Requirement traceability | Adopt lightly | Requirement ids map to named component tests |
| Receiver model, org-hierarchy search | Skip | Access is RBAC + `DataScope`; consent is purpose-based; a recipient is recorded only for third-party sharing |
| CRUD-per-field consent | Skip | Purposes × scopes cover the real decisions |
| Reflection-based field blanking | Skip | Hidden DTO mutation; MySMS maps explicitly |
| Shared Redis populated by the server | Skip | Per-process caches and a precomputed state table |
| API permissions published to the access service; per-service filter | Adopted outside this design | Each module publishes its operation table to iam; auth-client enforces (JAVA-CODING-RULES §16.2) |
| FHIR Consent shape | Skip now, map later | `purpose.external_code` for consent-manager interoperability |
| Thread-pool fan-out of single checks | Skip | One batch RPC |
| Liquibase, Spring Data JDBC, Lombok, Feign | Skip | Flyway baseline, JdbcClient, records, gRPC |

---

## 4. Domain model

### 4.1 Concepts

| Concept | Meaning | Owner | Lifecycle |
|---|---|---|---|
| **Purpose** | Why data is processed (`photo_video_publication`, `community_naming`, `marketing_comms`, `learning_analytics` …; 20 in the baseline) | platform catalogue, seeded by the baseline; a tenant switches a purpose on (`tenant_purpose`) and attaches a notice | `active`, `deprecated` (no new consents; existing still evaluated), `retired` |
| **Legal basis** | `consent`, `legitimate_use`, `legitimate_use_voluntary`, `legal_obligation`, `education_exemption`, `medical_emergency`, `employment`, `contract` (mapping to the Act: brief Q2) | per processing activity | fixed per activity version |
| **Scope (width)** | How far processing reaches: `class < institution < inter_school < public` for width purposes; `tenant` for the rest | per purpose (`scope_kind`) | a consent at a width covers every narrower width (K17) |
| **Data category** | `identity`, `contact`, `photo_media`, `academic_record`, `attendance`, `health`, `counselling`, `financial`, `location`, `biometric`, `device_usage`, `government_id`, with a sensitivity | catalogue | restricted = health, counselling, biometric; sensitive = government_id, financial, location, photo_media; normal = the rest (levels: DEC-17) |
| **Processing activity** | module × activity × purpose × basis × categories × recipients × retention policy (a register row) | catalogue (18 seeded; *planned:* module manifests) | `version` increments on change; a material change can force re-consent |
| **Notice** | The text shown for a purpose or bundle of purposes, per tenant and language | tenant admin | versions `draft → active → retired`; one active version per notice |
| **Consent record** | One decision by one giver for one subject × purpose × scope, under one notice version | privacy | `granted` or `refused`; may be withdrawn, superseded by the same giver's newer decision, or expire |
| **Consent evidence** | Append-only, hash-chained ledger of everything that happened to consent records | privacy | never updated or deleted |
| **Effective consent state** | The answer per subject × purpose × scope: allowed or denied, with a reason | privacy | rewritten in the transaction of every change; published as an event |
| **Subject** | A data principal known to privacy: person, accounts, student records, institutions, majority status | projection of people and sis events | `minor`, `adult`, `unknown` (= minor) |
| **Guardian link** | Projection of `sis.student_guardian` | from sis events | only an active link of a legal guardian may consent for a minor |
| **Verification** | How the giver was established as an adult guardian: `existing_record`, `in_person_staff_check`, `digilocker_token`, `otp_on_registered_mobile` (weak; to verify), `paper_form_countersigned` | privacy | a consent counts only when `verified` |
| **Rights request** | `access`, `correction`, `grievance`, `nomination`, `export`, `erasure`, `withdraw_all` | privacy | §4.3, §9.6 |
| **Legal hold** | Freezes erasure and retention for a subject, a category or a record set (safeguarding, litigation, regulator, audit) | DPO, safeguarding lead | `active → released`, with a review date |
| **Retention policy** | category × trigger × period (days) × action (`delete`, `anonymise`, `archive`), with a legal floor | platform catalogue (`retention_policy`); the tenant's own period and action (`tenant_retention_policy`), never below the floor | the catalogue changes with the baseline; a tenant setting is versioned by `row_version` |
| **Breach** | An incident affecting personal data, with the Board's clock and the notices sent | DPO | §11 |
| **Nominee** | Person an adult principal names to exercise rights on death or incapacity; acts for them once their nomination request is fulfilled | privacy | `active` (acting once activated), `revoked` |

### 4.2 Effective-consent rule

Generalises the community design's K17 to every purpose. Inputs: the subject's majority status, the live records for
(subject, purpose), the active legal-guardian links, the tenant's purpose setting, the activity's legal basis.
Implemented as `EffectiveConsentRule` (Strategy; `DefaultEffectiveConsentRule`), a pure function; its output for every
scope is stored in `privacy.consent_state` and published. Consumers never re-implement it.

```
decide(subject S, purpose P, scope W, activity A):
  if A.legalBasis != consent                      -> ALLOWED (basis = A.legalBasis)        // answered, not stored
  if P.forbiddenForMinors and S.status != adult   -> DENIED  (FORBIDDEN_FOR_MINOR)
  if tenantPurpose(P) is off                      -> DENIED  (PURPOSE_DISABLED)
  live = records(S, P) not withdrawn, not superseded, not expired, verification_status = verified
  givers = S.status == adult ? {S} : { g | link(g, S) active and legal guardian }
  relevant = live where giver in givers
  if any refused with scope <= W                            -> DENIED (REFUSED)
  if S not adult and any granted with scope < W             -> DENIED (NARROWER_GRANT)   // most restrictive guardian
  if any granted with scope >= W                            -> ALLOWED (CONSENT)
  if the purpose's active notice requires reconsent and the grant predates it -> DENIED (RECONSENT_REQUIRED)
  -> DENIED (NOT_ANSWERED)                                                               // silence never allows
```

Only *recorded* refusals and narrower grants of other legal guardians count, so a second guardian's silence does not
block.

### 4.3 State machines

**Consent record** (a row's decision never changes; a change is a new row):

```
            record(granted|refused)
   (none) ───────────────────────────▶ LIVE ──withdraw──▶ WITHDRAWN
                                         ├──newer decision by the same giver──▶ SUPERSEDED
                                         └──valid_until passed (expiry job)───▶ EXPIRED
   LIVE + unverified ──verify (a second staff member)──▶ LIVE (verified)
```

**Rights request** (statuses in `ck_rights_request_status`; behaviour in §9.6):

```
SUBMITTED ─verifyIdentity─▶ VERIFIED ─assign─▶ IN_PROGRESS ─close(fulfilled | partially_fulfilled | declined)─▶ CLOSED
    │ (signed-in requester: VERIFIED at once;        │ └─fulfilAccess (access only)─▶ CLOSED (fulfilled)
    │  a nominee's request stays SUBMITTED)          │   (nomination: fulfilled activates the nominee; never partly)
    └──────────────reject (SUBMITTED | VERIFIED)─────┴──▶ REJECTED (reason; appealable as a grievance)
IN_PROGRESS (erasure | export) ─startFulfilment─▶ IN_PROGRESS with tasks
    ─every task answered─▶ CLOSED (fulfilled | partially_fulfilled when a module kept rows)
    ─a task failed for the 5th time, the others answered─▶ ACTION_REQUIRED ─retryTask─▶ IN_PROGRESS
IN_PROGRESS (withdraw_all) ─fulfilWithdrawAll─▶ CLOSED (fulfilled)
Extension moves due_at (not a status); overdue is derived at read time.
```

*Planned:* reminders at 50 %, 80 % and 100 % of the deadline to the assignee and the DPO role through comms (blocked on
comms delivery, ARC-2).

**Rights request task** (`rights_request_task`, one per module of an erasure or export request): `PENDING` (its wave
has not started) `→ DISPATCHED → DONE | PARTIAL | FAILED | NOT_APPLICABLE`; a `FAILED` answer before the fifth attempt
waits its back-off (`next_attempt_at`, shown on the task as `nextAttemptAt`) and goes back to `DISPATCHED`, as a staff retry does at once, under the same task id, as does the sweep's re-dispatch of an unanswered task.
The answer keeps the counts per table, `heldItems` (rows kept under a hold or a legal-retention policy) and the
policy codes; a failure its error code.

**Legal hold** (`legal_hold`): `ACTIVE ─updateLegalHold (review date)─▶ ACTIVE ─release─▶ RELEASED`; released holds
never change and are never deleted; due for review once active on or past `review_by`.

**Age-of-majority transition.** `MajorityJob` (core job runtime, per tenant, every
`mysms.privacy.majority-job.interval`) turns subjects whose majority date has come into adults
(`mysms.privacy.age-of-majority`, default 18), publishes `privacy.subject.majority_reached` and recomputes their
states. From then on only the student's own records count: every consent purpose becomes `NOT_ANSWERED` unless the
purpose's `majority_grace` keeps it; guardians lose the toggles for that student. Grace per purpose: publication
(`photo_video_publication`, `community_naming`) is `keep_published` — already published items stay, new processing
needs the student's own grant; marketing and analytics lapse at once. The community design's open decision 9 remains
the owner's; this is its default. *Planned:* a "you can now decide" task for the student; age of majority as tenant
data (country) rather than configuration.

### 4.4 Legitimate-use bases

The register marks each activity's basis. `privacy-client` answers a non-consent activity without asking privacy once
it has learned the basis (§7.2), and parents are never asked to consent to what the school must do anyway
(attendance, marks, fees): asking where the basis is a legal obligation would mislead, since withdrawal could not be
honoured.

---

## 5. Database schema (`privacy`)

Conventions (database/README, JAVA-CODING-RULES §5, §6, §14): `tenant_id` on every tenant table with `FORCE ROW
LEVEL SECURITY` and policy `tenant_isolation`; tenant-composite in-module foreign keys; audit columns, `row_version`,
`deleted_at` only where a user can remove a row; enums as `varchar` + CHECK; COMMENT on every table and column. The
baseline is the source of truth; this section records what each table is for and the constraints that carry a rule.
Global catalogue ids are `md5('mysms.<table>:' || code)::uuid`, stable across environments.

### 5.1 Catalogue (global, platform-maintained)

| Table | Holds | Rules in the schema |
|---|---|---|
| `purpose` | 20 purposes: `scope_kind` (`width` / `tenant`), `fiduciary` (`tenant` / `platform`), `forbidden_for_minors`, `majority_grace` (`none` / `keep_published` / `keep_narrow_widths`), `default_valid_months`, `external_code`, `status`, `version` | Forbidden for minors: `behavioural_analytics`, `targeted_advertising`, `biometric_or_video_monitoring`, `product_analytics` (platform fiduciary) |
| `data_category` | 12 categories with sensitivity `normal` / `sensitive` / `restricted` | §4.1 |
| `processing_activity` | The register: module, code `<module>.<activity>`, purpose, `legal_basis`, `education_exemption_clause`, `targets_minors`, `audiences` (whose data: `student`, `guardian`, `staff`), `recipients` (jsonb array), `cross_border`, `retention_policy_code`, `version` | Code must start with its module; `education_exemption` needs a clause; audiences a non-empty subset of the three roles |
| `processing_activity_category` | Categories per activity | |

The baseline seeds the 18 activities of §8. Recipients, cross-border flags and retention policies are empty until
legal review (brief Q9–Q11). Served as `privacy.ListProcessingActivities`.

*Planned:* per-module manifests `META-INF/mysms/privacy/<module>.json` (activities, categories with the tables and
subject columns holding them, supported handlers; example in §8) that the build checks and seeds from; a conformance
rule failing the build when an activity with `targets_minors` uses a purpose forbidden for minors, or when code uses an
`ActivityCode` absent from the register.

### 5.2 Tenant settings and notices

| Table | Holds | Rules |
|---|---|---|
| `tenant_purpose` | A tenant's switch per purpose: `enabled`, `max_scope` (tenant ceiling, e.g. never `inter_school`), `notice_id` | No row = off; one row per tenant and purpose; set by tenant administrators only |
| `notice` | Tenant notice: `code`, `title`; soft-deletable | Code unique per tenant among live notices |
| `notice_version` | `version`, `status` (`draft` / `active` / `retired`), `requires_reconsent`, `purpose_codes`, `grievance_contact` (jsonb: name, role, email, phone), `published_at`, `superseded_by_id`, `document_file_id` | One active version per notice; published versions never change |
| `notice_translation` | `language` (BCP 47), `body_markdown`, `summary_for_children`, `content_sha256` | One per version and language; changes only while the version is a draft; the hash is copied into evidence |

### 5.3 Subjects and guardian links (projections)

Kept from other modules' events (event-carried state); privacy reads no other schema and makes no call for them.
`people.person` events carry `{id, dateOfBirth, rowVersion}`; `sis.student` and `hr.staff` events `{id, userId,
personId, institutionId, rowVersion}`; `sis.student_guardian` events `{id, studentPersonId, guardianPersonId, guardianUserId,
isLegalGuardian, hasCustody, status, rowVersion}` (sis resolves persons and the guardian's account when publishing).
A copy changes only to a newer source row version, so out-of-order and repeated deliveries are harmless; nothing is
hard-deleted (a deleted person keeps its row with the birth date cleared, a deleted link turns `inactive`, a deleted
or moved student record stays as a tombstone). Handlers: `SubjectSync`, `StaffSync`, `GuardianLinkSync`. The roles they give a person — student (a live student
record), guardian (an active link as guardian), staff (a live staff record) — filter the access summary (§9.6), and
the accounts they name feed the conflict-of-interest check of rights requests.

| Table | Holds | Rules |
|---|---|---|
| `subject` | Per person: `status` (`minor` / `adult` / `unknown`), `birth_date`, `majority_date`, `student_ids`, `user_ids`, `institution_ids`, `student_records` (jsonb per sis student: user, institution, row version, live), `staff_records` (the same per hr staff record, `StaffSync`), `person_row_version`, `state_version` | Unknown status ⇔ no birth date; `ix_subject_majority` drives the majority job. `student_records` keeps a row version per record, so a person with several records, or a record moving to another person, stays correct out of order |
| `guardian_link` | Per sis link: student and guardian persons, guardian account, `is_legal_guardian`, `has_custody`, `status`, `source_row_version` | Indexed by student and by guardian on active links |

### 5.4 Consent records, evidence and state

| Table | Holds | Rules |
|---|---|---|
| `consent_record` | subject, giver, `giver_capacity` (`self` / `guardian` / `nominee`), purpose, `scope`, `decision`, notice version and language, validity, `withdrawn_at`, `superseded_by_id`, `channel` (`web`, `mobile_app`, `paper_form`, `in_person`, `email`, `consent_manager`, `import`), verification status / method / verifier / time, `recorded_by_user_id`, `evidence_file_id`, `external_artefact_id` | One live decision per giver, subject and purpose (`ux_consent_record_live`); `self` ⇒ giver = subject; a paper form needs an evidence file and the recording staff member; the verifier differs from the recorder (four eyes); a verified record has a method and a time |
| `consent_evidence` | Ledger entries: `event` (`recorded`, `verified`, `rejected`, `withdrawn`, `superseded`, `expired`, `majority_lapsed`), actor user and person, capacity, channel (also `system`), notice content hash, IP, user-agent hash (never the raw agent), `impersonator_user_id`, `occurred_at`, `previous_hash`, `entry_hash` | Partitioned by month (`occurred_at`; core partition maintenance adds months; the baseline creates 2026–2027 and a default partition). Per-tenant hash chain |
| `evidence_chain` | The head of each tenant's chain (last hash, last time) | A writer locks the tenant's row, so entries are appended one at a time, each naming its predecessor |
| `consent_state` | One row per subject × purpose × scope: `result` (`allowed` / `denied`), `reason`, `state_version`, `computed_at` | allowed ⇔ reason `CONSENT`; `ix_consent_state_allowed` serves `ListSubjectsWithConsent`. Not partitioned: a 3,000-student school with 8 purposes × ~4 widths is ≈ 100 k rows |

Evidence is append-only by convention: the application role still holds UPDATE and DELETE on it. Append-only by
privilege needs core's `ApplicationRoleGrants` (ROADMAP DEC-18, SEC-6). *Planned:* a nightly verification of each
tenant's chain with an alert on mismatch; evidence retention (longest consent life + limitation period, *to verify*,
default 8 years).

### 5.5 Rights requests

| Table | Holds | Rules |
|---|---|---|
| `rights_request` | `number` (`PRV-<year>-<6+ digits>`), `type`, subject, requester, `requester_capacity`, `status`, `identity_check` (`signed_in_session` / `in_person` / `document`), `details` (jsonb), `assignee_user_id`, `submitted_at`, `due_at`, `extended_reason`, `closed_at`, `outcome_code`, `outcome_note`, `outcome_response` (the answer the requester sees), `outcome_file_id`, `access_summary` (jsonb) | CHECKs: type in the seven of §4.1; status in `submitted`, `verified`, `in_progress`, `action_required`, `rejected`, `closed` (fulfilled and partially fulfilled are outcomes of `closed`); verified unless submitted or rejected; in progress or action required ⇒ assignee; closed or rejected ⇔ `closed_at` ⇔ outcome; an access summary only on a closed access request, and an access request closed as (partially) fulfilled must carry one; a nominee's request is verified in person or by document, never by session |
| `number_sequence` | Counter per tenant, sequence code (`rights_request`, `breach`) and period (modelled on `admission.number_sequence`, without institution: privacy's data is tenant-level) | Row locked in the issuing transaction |

| `nominee` | Per adult principal: `nominee_person_id` (when the nominee signs in), `nominee_name_enc`, `nominee_contact_enc` (core `FieldCipher`, answered masked), `status` (`active` / `revoked`), `activated_at` and `activated_by_request_id` (the fulfilled nomination request), `revoked_at` | One active nominee per principal (`uq_nominee_active`); never oneself; activated ⇔ the activating request is named; rows never deleted |

| `rights_request_task` | Per erasure or export request and module of `mysms.privacy.erasure.modules`: `module`, `action` (`erase` / `export`), `wave` (1: `mysms.privacy.erasure.first-wave`, comms and iam, for an erasure, and every export task; 2: the rest), `status`, `attempts`, `dispatched_at`, `completed_at`, `result` (jsonb `{counts: {table: rows}, heldItems, policyCodes, fileId}`), `last_error_code` | `uq_rights_request_task (tenant_id, request_id, module, action)`; FK to `rights_request`; pending ⇔ no dispatch ⇔ 0 attempts; answered ⇔ `completed_at`; a result only once answered; rows never deleted |
| `legal_hold` | `reason_code` (`safeguarding` / `litigation` / `regulator` / `audit`), `scope` (`subject` / `category` / `records`), `subject_person_id`, `category_code` (FK to `data_category.code`), `module`, `entity_type`, `entity_ids` (uuid[]), `placed_by_user_id`, `placed_at`, `review_by` (date, mandatory), `released_at`, `released_by_user_id` | CHECKs per scope: subject ⇒ the subject and nothing else; category ⇒ the category, optionally a subject; records ⇒ module, entity type and 1–1000 ids, optionally a subject; released ⇔ released by; rows never deleted |

### 5.6 Breach register (P4) and retention (P3)

| Table | Holds | Rules |
|---|---|---|
| `breach` | `number` (`PRB-<year>-<6+ digits>`, from `number_sequence`), `status`, `detected_at`, `became_aware_at` (the clock start), `board_report_due_at`, `description`, `categories` (data-category codes), `modules`, `estimated_principals`, `likely_consequences`, `mitigation`, `owner_user_id`, `board_report_sent_at`, `not_a_breach_reason`, `closed_at`; audit columns, `row_version` | CHECKs: status in `reported`, `triaged`, `contained`, `board_initial_sent`, `board_report_sent`, `principals_notified`, `closed`, `not_a_breach`; `became_aware_at >= detected_at`; `board_report_due_at = became_aware_at + 72 h`; `board_report_sent_at` set at `board_report_sent` and `closed`, absent before; `not_a_breach` ⇔ its reason; closed or not a breach ⇔ `closed_at`; texts 1–4000 characters (the reason 1–1000); rows never deleted |
| `breach_notification` | Per notice recorded: `breach_id` (FK to `breach`), `audience` (`board` / `principals` / `processor` / `tenant`), `kind` (`initial` / `detailed` / `update`), `sent_at`, `document_file_id` | Append-only; no `comms_message_id` until notices go through comms |

`description`, `likely_consequences` and `mitigation` hold **ids and counts only** (record ids, tables, numbers of rows
or people) — never a name, contact or other personal data — and never reach an event. *Planned:* `affected_query`
(jsonb, how the affected set was derived) with the identify task of §11, and `comms_message_id` on a notice once comms
delivers breach notices.

Retention:

| Table | Holds | Rules |
|---|---|---|
| `retention_policy` (global) | The platform catalogue, seeded by the baseline: `code` (`<module>.<what>`), `category_code` (FK to `data_category.code`), `module`, `trigger_code` (`after_leaving` / `after_record_date` / `after_inactivity` / `after_purpose_end`), `period_days`, `minimum_period_days` (the legal floor), `action` (`delete` / `anonymise` / `archive`), `notify_before_days`, `legal_reference`; ids `md5('mysms.retention_policy:' \|\| code)` | `period_days >= minimum_period_days >= 1`; ten policies, every value conservative and **to verify** with counsel (COMMENTs say so) |
| `tenant_retention_policy` | A tenant's own `period_days` and `action` for one policy; no row = the catalogue's | `uq (tenant_id, policy_id)`; never below the floor (the service: 422 `RETENTION_BELOW_LEGAL_FLOOR`) |
| `retention_run` | Per tenant, policy and cutoff: `module`, `action` (in force that day), `cutoff`, `status` (`dispatched` / `done` / `partial` / `failed`), `affected`, `held`, `started_at`, `completed_at` | `uq (tenant_id, policy_id, cutoff)`: one run a day; dispatched ⇔ not completed; rows never deleted |


Every tenant table, built or planned, has RLS (`tenant_isolation`). Volumes: requests and breaches are low.

### 5.7 People's consent parts

`people.consent_record`, people's seven consent operations and their permission seeds are gone; consent lives only
in privacy. Community-design prerequisites map here: PR-1 (schema) and PR-2 (operations) → §5.4 and §6; PR-3 events
`people.consent_record.*` → `privacy.consent_record.*` and `privacy.consent_state.changed`;
`people.person.majority_reached` → `privacy.subject.majority_reached`; PR-18 erasure → §9; PR-22 purposes
`community_naming`, `community_participation` → the catalogue.

*Planned:* if a pilot tenant ever holds consents from another system, a platform-operator-only
`privacy.ImportLegacyConsents` copies them as `channel = import`, `verification_status = unverified`, photo consents at
`scope = class` (the narrowest, community §8.4 "legacy and paper consents"), with evidence entries.

---

## 6. APIs

### 6.1 REST

Contract interfaces in `com.mysms.privacy.api` (one per resource), DTOs in `api.model`, thin controllers. Permission
codes follow `<module>.<resource>.<action>`. Self-service operations (`my_` resources) act only on the token's person
and the caller's active legal-guardian links (a data rule in the service, JAVA-CODING-RULES §16).

**Built** (71 operations; generated status: [api/operations-status.md](../../api/operations-status.md)):

| Area | Operations (all under `/v1/privacy`) | Permission | Who |
|---|---|---|---|
| Purposes | `ListPurposes`, `GetPurpose`, `UpdatePurpose` (`/purposes`) — catalogue plus the tenant's switch, ceiling and notice | `privacy.tenant_purpose.read` / `.update` | tenant admin |
| Notices | `List/Get/Create/Update/DeleteNotice`; `List/Get/Create/UpdateNoticeVersion`, `PublishNoticeVersion` (`:publish`); `List/Get/Create/Update/DeleteNoticeTranslation` | `privacy.notice.*`, `privacy.notice_version.*`, `privacy.notice_translation.*` | tenant admin |
| Register | `ListProcessingActivities` (`/processing-activities`); `GetProcessingRegisterExport` (GET `/processing-activities:export`: the whole register as CSV text — code, module, purpose, legal basis, exemption clause, minors, audiences, categories, recipients, cross-border, retention policy — inside a JSON answer `{fileName, generatedAt, activities, csv}`, so REST and gRPC answer alike) | `privacy.processing_activity.read` | DPO, auditor |
| My consents | `ListMyConsents`, `RecordMyConsent` (`/my/consents`), `WithdrawMyConsent` (`:withdraw`), `GetMyNotice` (`/my/notices/{noticeCode}`) | `privacy.my_consent.read` / `.create` / `.withdraw` | guardian, adult student, staff |
| Consent records | `ListConsentRecords`, `GetConsentRecord`, `RecordConsentOnBehalf` (paper forms), `VerifyConsentRecord` (`:verify`, a second staff member) | `privacy.consent_record.read` / `.record_on_behalf` / `.verify` | DPO, office |
| My requests | `SubmitMyRequest`, `ListMyRequests`, `GetMyRequest`, `GetMyAccessSummary` (`/my/requests/{id}/access-summary`) | `privacy.my_request.create` / `.read` | principal, guardian, nominee |
| My nominee | `GetMyNominee` (GET `/my/nominee`), `SetMyNominee` (POST `/my/nominee:set`), `RevokeMyNominee` (POST `/my/nominee:revoke`, 204) | `privacy.my_nominee.read` / `.set` / `.revoke` | adult principal |
| Rights requests | `ListRightsRequests`, `GetRightsRequest` (with an erasure or export request's `tasks`), `GetAccessSummary`; `VerifyRightsRequestIdentity`, `AssignRightsRequest`, `ExtendRightsRequest`, `RejectRightsRequest`, `CloseRightsRequest`, `FulfilAccessRequest`, `StartRightsRequestFulfilment`, `FulfilWithdrawAllRequest` (`:verifyIdentity`, `:assign`, `:extend`, `:reject`, `:close`, `:fulfilAccess`, `:startFulfilment`, `:fulfilWithdrawAll`); `RetryRightsRequestTask` (`/rights-requests/{id}/tasks/{taskId}:retry`) — §9.2, §9.6 | `privacy.rights_request.read` / `.verify_identity` / `.assign` / `.extend` / `.reject` / `.close` / `.fulfil` / `.retry_task` | DPO / grievance officer |
| Legal holds | `ListLegalHolds` (filter `active`, `reviewBy` — review due: `active = true AND reviewBy <= "<today>"`), `GetLegalHold`, `PlaceLegalHold` (POST `/legal-holds`), `UpdateLegalHold` (PATCH, If-Match: the review date), `ReleaseLegalHold` (`:release`) — tenant-wide grant, refused while impersonating | `privacy.legal_hold.read` / `.place` / `.update` / `.release` | DPO, safeguarding lead |
| Retention | `ListRetentionPolicies`, `GetRetentionPolicy`, `UpdateRetentionPolicy` (PATCH `/retention-policies/{id}`, If-Match `"0"` while never set: the tenant's period and action, never below the floor); `ListRetentionRuns` (`/retention-runs`) — §10 | `privacy.retention_policy.read` / `.update`, `privacy.retention_run.read` | tenant admin, DPO |
| Breaches | `ListBreaches`, `GetBreach`, `ReportBreach` (POST `/breaches`), `UpdateBreach` (PATCH, If-Match, while open); `TriageBreach`, `ContainBreach`, `RecordBreachNotification`, `CloseBreach`, `MarkBreachNotABreach` (`:triage`, `:contain`, `:recordNotification`, `:close`, `:markNotABreach`) — §11 | `privacy.breach.read` / `.report` / `.update` / `.triage` / `.contain` / `.record_notification` / `.close` / `.mark_not_a_breach` | DPO |
| Coverage | `GetConsentCoverage` (GET `/coverage?purposeCode=&scope=&institutionId=`): per width the purpose takes (or the one asked), subjects allowed, denied per reason (all six, zero included) and not answered (stored `NOT_ANSWERED` or no answer computed yet), over the tenant's data principals (a tenant-wide grant) or one institution's students (`subject.institution_ids`, within the caller's grant) — counts only, never person ids | `privacy.coverage.read` | principal, class teacher (own institution), DPO |
| Decisions (internal REST twins of §6.2) | `CheckConsent` (`/consent-decisions:check`), `BatchCheckConsent` (`:batchCheck`), `GetPurposeStatus` (`:purposeStatus`), `ListSubjectsWithConsent` (`:subjects`) | `privacy.consent_decision.check` / `.batch_check` / `.get_purpose_status` / `.list_subjects` | service principals |
| Holds check (internal) | `ListActiveHolds` (GET `/legal-holds:active?subjectPersonId=&module=`): the active holds that may cover a subject's data — the subject's own and every category or record-set hold of the subject or of everyone, record sets narrowed to the module — read on the primary, for a module's erasure or sweep (§9.1) | `privacy.legal_hold.check` | service principals |

*Planned:*

| Operation | Path | Permission | Who |
|---|---|---|---|
| Coverage per class | `classSectionId` on `GetConsentCoverage` (privacy keeps no class membership yet) | `privacy.coverage.read` | class teacher: counts only |
| Export package download | a short-lived signed link to the assembled ZIP (§9.3) | `privacy.my_request.read` | requester |
| Breach notices through comms | `:notify` sending the Board's and the principals' notices (comms delivery is not built: notices are recorded today) | `privacy.breach.*` | DPO |
| RoPA export as PDF | the register as a printable document (CSV is built) | `privacy.processing_activity.read` | DPO, auditor |
| `ImportLegacyConsents` | §5.7 | platform operator | |

**`RecordMyConsent`** takes `subjectPersonId` (self, or a minor the caller is an active legal guardian of),
`purposeCode`, `scope`, `decision` (`GRANTED` / `REFUSED`), `noticeVersionId` (the version shown) and
`noticeLanguage`; the giver is always the caller, never a field. Supports `Idempotency-Key`; a newer decision by the
same giver supersedes the live one.

**`MyConsent`** (one row of the consent centre): id, subject, purpose, `capacity` (`SELF` / `GUARDIAN` /
`MINOR_SELF`), scope, `myStatus` (`GRANTED`, `REFUSED`, `NOT_ANSWERED`, `RECONSENT_REQUIRED`, `EXPIRED`; absent on a
`MINOR_SELF` row), `effectiveWidestScope`, `noticeCode` and `noticeVersionId` (to show and decide under),
`allowedScopes` (the purpose's widths within the tenant's ceiling), `decidesFrom` (`MINOR_SELF` only: the majority
date), decided-at, notice version, channel, etag. privacy keeps no names: clients take a child's name from
`sis.ListMyChildren` and show "You" on the caller's own rows. A minor's own rows carry the effective result and
`decidesFrom` only — no decision, record id or giver — so a student never sees which guardian decided what; a person
of unknown age sees no own row.

**Errors** (problem+json, message catalogue): a caller who is not the subject's active legal guardian gets core's
generic 404 `NOT_FOUND` (never confirming a child exists); a decision while impersonating gets core's generic 403
`PERMISSION_DENIED`. Field errors: `CONSENT_NOTICE_NOT_ACTIVE`, `CONSENT_PURPOSE_DISABLED`,
`CONSENT_FORBIDDEN_FOR_MINOR`, `CONSENT_SCOPE_ABOVE_TENANT_MAX`, `RIGHTS_REQUEST_DUE_AT_NOT_LATER`,
`RIGHTS_REQUEST_DETAILS_REQUIRED`, `RIGHTS_REQUEST_DETAILS_NOT_ALLOWED`, `RIGHTS_REQUEST_TYPE_NOT_ALLOWED`,
`RIGHTS_REQUEST_NOMINEE_NOT_ACTIVE`, `RIGHTS_REQUEST_RESPONSE_REQUIRED`, `BREACH_TIME_IN_FUTURE`, `BREACH_AWARE_BEFORE_DETECTED`,
`BREACH_NOTICE_BEFORE_AWARE`, `BREACH_CATEGORY_UNKNOWN`, `BREACH_MODULE_INVALID`, `SCOPE_NOT_APPLICABLE`,
`LEGAL_HOLD_TARGET_REQUIRED`, `LEGAL_HOLD_TARGET_NOT_ALLOWED`, `LEGAL_HOLD_CATEGORY_UNKNOWN`, `LEGAL_HOLD_MODULE_INVALID`,
`LEGAL_HOLD_REVIEW_BY_PAST`, `RETENTION_BELOW_LEGAL_FLOOR`; 409 `RIGHTS_REQUEST_CONFLICT_OF_INTEREST`; planned
`REQUEST_IDENTITY_NOT_VERIFIED`. Rights-request rules: §9.6.

### 6.2 gRPC

privacy's one proto file, `privacy.proto` (`PrivacyProto`), serves `PurposeService`, `NoticeService`,
`ProcessingActivityService`, `ConsentService`, `RightsRequestService`, `NomineeService`, `BreachService`,
`ConsentCoverageService`, `LegalHoldService` (with the internal `ListActiveHolds`), `RetentionService` (one RPC per REST
operation) and the hot-path
`ConsentDecisionService`:

```proto
// Hot-path decisions for other modules; tenant from the caller's service token and tenant metadata.
service ConsentDecisionService {
  rpc CheckConsent(CheckConsentRequest) returns (CheckConsentResponse);
  rpc BatchCheckConsent(BatchCheckConsentRequest) returns (BatchCheckConsentResponse);   // <= 1000 subjects
  rpc GetPurposeStatus(GetPurposeStatusRequest) returns (GetPurposeStatusResponse);      // tenant switch + notice
  rpc ListSubjectsWithConsent(ListSubjectsWithConsentRequest) returns (ListSubjectsWithConsentResponse); // paged
}
```

Requests carry an **activity code** (`comms.marketing_broadcast`), subject person ids and `scope` as a string; the
answer per subject is a `ConsentDecision` (allowed, `ConsentDecision.Reason` with `REASON_` prefix, legal basis,
state version), in request order. Enums are nested in their messages (`ConsentDecision.Reason`,
`BatchCheckConsentRequest.Scope`). Callers pass an activity, not a purpose: privacy resolves purpose and basis from
the register, so a module cannot pick a convenient basis at the call site and the register stays true. Resolvers sit
behind the global exception interceptor (JAVA-CODING-RULES §10.3); reads run in `doInReadTransaction`.

### 6.3 Events (outbox → EventBus)

Payloads carry ids, codes and statuses only, never names, descriptions, reasons or notes.

| Type | When | Payload |
|---|---|---|
| `privacy.tenant_purpose.updated` | purpose switch changes | `{id, purposeId, enabled}` |
| `privacy.notice*.created` / `.updated` / `.deleted`; `privacy.notice_version.published` / `.retired` | notice changes | ids; a published version adds `{noticeId, version, purposeCodes, requiresReconsent}` |
| `privacy.consent_record.recorded` / `.withdrawn` / `.superseded` / `.expired` / `.verified` | each change | `{id, subjectPersonId, giverPersonId, giverCapacity, purposeCode, scope, decision, validUntil, verificationStatus, withdrawnAt, rowVersion}` |
| `privacy.consent_state.changed` | a recompute changes a row | the subject, purpose, per-scope results and `stateVersion` (event-carried state, enough for a projection such as `comms.media_consent_state`) |
| `privacy.subject.majority_reached` | majority job | `{personId, majorityDate, stateVersion}` |
| `privacy.rights_request.submitted` / `.verified` / `.assigned` / `.extended` / `.rejected` / `.closed` | each step | ids, type, status |
| `privacy.breach.reported` / `.updated` / `.triaged` / `.contained` / `.notification_recorded` / `.closed` / `.not_a_breach` | each change of a breach | `{id, number, status, boardReportDueAt, boardReportSent}`; a recorded notice adds `{notificationId, audience, kind}` — never the description, consequences, mitigation or reason (to ops alerting) |
| `privacy.rights_request.fulfilment_started` / `.task_retried` / `.action_required` | an erasure or export request's fulfilment started, a task retried, a task failed for good | as the request's other steps; a saga close is `.closed` with outcome `fulfilled` or `partially_fulfilled` |
| `privacy.rights_request.task_assigned` | a task dispatched (start, next wave, a failed answer before the fifth attempt, retry, sweep) | `{requestId, taskId, type, action, subjectPersonId, module, wave, deadline}` — no hold: the module asks `ListActiveHolds` |
| `privacy.erasure.completed` | an erasure closed (fulfilled or partially) | `{requestId, subjectPersonId}` — tombstone for DR replay (§9.5) |
| `privacy.legal_hold.placed` / `.updated` / `.released` | each change of a hold | `{id, reasonCode, scope, subjectPersonId, categoryCode, module, reviewBy, active}` |
| `privacy.tenant_retention_policy.updated` | a tenant sets a retention policy | `{id, policyId, policyCode, periodDays, action}` |
| `privacy.retention.due` | the daily retention run, per policy | `{runId, policyCode, categoryCode, module, cutoff, action}` |
| `privacy.nominee.created` / `.revoked` / `.activated` | a nominee named, revoked or replaced, or activated by a fulfilled nomination request | `{id, principalPersonId, nomineePersonId, status, activated}` — never the name or contact |
| `<module>.privacy_task.completed` (published by the modules with a handler, §9.1) | a module's handler answered its task (§9.1); privacy's `TaskAnswerHandler` records it (§9.2) | aggregate `<module>.privacy_task`, id = the task id; `{taskId, requestId, status (done / partial / failed / not_applicable), counts: {table: rows}, heldItems, policyCodes, fileId}`, or `{taskId, requestId, status: failed, errorCode}` |
| `<module>.privacy_retention.completed` (published by sis and iam) | a module answered a retention run (§10); privacy's `RetentionAnswerHandler` records it on the run | aggregate `<module>.privacy_retention`, id = the run id; `{runId, status (done / partial / failed), affected, held, errorCode}` |

*Planned:*

| Type | When | Payload |
|---|---|---|
| `privacy.consent_state.snapshot` | replay per tenant for a consumer's cold start | as `.changed`, batched, with `snapshotId`, `lastStateVersion` |
| `<module>.privacy_task.completed`, `<module>.privacy_retention.completed` | every other module holding personal data, once its handler exists (§9.1) | as above |
| `privacy.rights_request.task_assigned` additions | `relatedPersonIds`, `categories`, `exportFormat` once the modules' handlers need them | |

---

## 7. `privacy-client`: the consent checker every module uses

### 7.1 Shape and placement

`backend/privacy-client` is a flat shared library beside `auth-client`: the only privacy surface a domain module
touches. It is an approved exception to CLAUDE.md §3 (each caller keeps its own proto copies and gRPC client) for the
same reason as `auth-client`: the behaviour must be identical and fail-closed in every module. Conformance treats it
as a shared module, an allowed dependency of domain modules, with no application.properties; rule G1 checks its copy
of `privacy.proto` is byte-identical to privacy's.

Public API: `ConsentChecker` (`check`, `checkAll`, `allowed`), `ActivityCode` (`<module>.<activity>`),
`ConsentScope`, `ConsentDecision` (allowed, reason, legal basis, state version), `ConsentReason` (privacy's reasons
plus `UNAVAILABLE`).

**Explicit calls, no annotations or aspects.** Permissions are checked at the edge and data rules in the service
(`DataScope.current(...)`); consent is a data rule, so it is an explicit call in the business service, next to
`DataScope`. An `@RequiresConsent` aspect was rejected: it hides a remote call, cannot batch, and cannot see which
subjects a list operation touches.

### 7.2 Behaviour

- `PrivacyDecisionGrpcClient` (Strategy `ConsentDecisionSource`) calls `BatchCheckConsent` as the **asking module's
  service principal** (the module part of the activity code) in the tenant of the current call, within
  `mysms.privacy-client.deadline`.
- `CachingConsentChecker` (Decorator, cache-aside, Caffeine) keys answers by tenant, activity, scope and subject for
  `mysms.privacy-client.cache-ttl`, refusals included, and asks only the misses, 1000 at a time. **No invalidation
  event**: every replica of a module shares one event queue, so an event would reach one replica only; a change shows
  everywhere within the TTL, like the role cache.
- The activity's legal basis is learned from an allow that rests on a non-consent basis and cached for one TTL from
  that answer, so legitimate-use processing keeps working through a privacy outage of up to one TTL. A process that
  has never reached privacy cannot tell, and fails closed.
- **Fail closed**: a failed call, or an answer that does not fit the question (count, order, an unknown reason, an
  allow its reason does not support), answers every subject of the batch `UNAVAILABLE`, never cached; only the count,
  the activity and privacy's status are logged. Any other exception — a check inside a database transaction, where
  no gRPC call may be made — is a programming error and reaches the caller.
- **Enforce only.** An `off / shadow / enforce` switch (global default, tenant override set by platform operators;
  shadow logs would-be denials without blocking) is *planned* for the first module whose rollout needs it.
- **Monolith and services behave the same**: in `app` the client calls privacy's in-process gRPC server.
- **Self-access short-circuit**: a principal reading their own data, or a guardian reading their child's under
  `DataScope`, is access, not a new purpose; no check.
- *Planned* **projection mode** for very hot per-item reads (the community feed): the consumer keeps its own table fed
  by `privacy.consent_state.changed` and `.snapshot` (community design `comms.media_consent_state`, dirty = unavailable)
  instead of calling per read; `privacy-client` would supply the payload record and a `ConsentStateProjector` for the
  version check, while the table and its SQL stay in the consumer's schema.

**Adopting it in a module**: add the dependency; set `mysms.privacy-client.deadline`, `mysms.privacy-client.cache-ttl`
and `grpc.client.privacy-service.*` in the module's application.properties; give the module's service identity
`privacy.consent_decision.batch_check` (DEPLOYMENT §5.9); add an auto-configuration test. `people` is the first
consumer.

### 7.3 Usage examples

```java
// comms: a marketing broadcast filters recipients in one call — before the write transaction, since no gRPC call
// may be made inside one (GrpcCallerInterceptor refuses it).
public SendResult sendBroadcast(UUID broadcastId) {
    List<Recipient> recipients = doInReadTransaction(() -> audience.resolve(
            broadcasts.load(DataScope.current(PERMISSION_SEND), broadcastId)));
    Set<UUID> allowed = consent.allowed(MARKETING_BROADCAST, Recipient.personIds(recipients), ConsentScope.TENANT);
    return doInWriteTransaction(() -> {
        Broadcast b = broadcasts.load(DataScope.current(PERMISSION_SEND), broadcastId);
        List<Recipient> sendable = recipients.stream().filter(r -> allowed.contains(r.personId())).toList();
        deliveries.queue(b, sendable);
        audit.record("comms.broadcast", broadcastId, Map.of("excludedForConsent", recipients.size() - sendable.size()));
        return new SendResult(sendable.size(), recipients.size() - sendable.size());
    });
}
// Operational notices use comms.operational_notice (legitimate use): never filtered; privacy is asked once per TTL.
```

```java
// lms: learning analytics on a cohort; minors are covered only if the activity is declared under the education
// exemption (the register records it, counsel signs off).
Map<UUID, ConsentDecision> d = consent.checkAll(LEARNING_ANALYTICS, cohort.learnerPersonIds(), ConsentScope.TENANT);
analytics.computeFor(cohort.learners().stream().filter(l -> d.get(l.personId()).allowed()).toList());
```

```java
// comms community: at approval, the subjects of each image are checked at the post's width and the projection row
// is written in the same unit of work (community design §8.4); reads use the projection.
Map<UUID, ConsentDecision> d = consent.checkAll(COMMUNITY_PHOTO_PUBLICATION, media.subjectPersonIds(), post.width());
mediaConsentState.write(media.id(), post.width(), d.values().stream().allMatch(ConsentDecision::allowed));
```

---

## 8. Enforcement points across modules

| Module | Processing | Activity code (purpose) | Basis | Check | Status |
|---|---|---|---|---|---|
| people | Public profile, alumni directory | `people.public_profile` (`public_profile`) | consent | `CreatePublicProfile` needs the person's consent (422 `CONSENT_REQUIRED`, after the write-permission and person checks); `GetPublicProfile` and `UpdatePublicProfile` serve another person's profile only with it (404 otherwise, the same with or without a profile), the person's own always; privacy unavailable → 503 | **Enforced** |
| comms | Community photos and videos of students | `comms.community_photo_publication` (`photo_video_publication`, width) | consent | at approval + projection on read | processing not built |
| comms | Naming students in posts | `comms.community_naming` | consent | at approval; projection | not built |
| comms | Student participation in the community | `comms.community_participation` | consent (guardian) | at membership grant | not built |
| comms | Promotional or third-party messages | `comms.marketing_broadcast` (`marketing_comms`) | consent | recipient resolution, batch | not built (delivery 501, ARC-2) |
| comms | WhatsApp / SMS for non-essential messages | `comms.whatsapp_optional` (`sms_whatsapp_messages`) | consent | channel selection per recipient | not built |
| comms | Operational notices | `comms.operational_notice` (`operational_communication`) | legitimate use | none | — |
| lms | Learning analytics, engagement scores, recommendations | `lms.learning_analytics` | consent (adults) / education exemption for minors (*to verify*, brief Q3) | before computing; dashboards filter | not built |
| lms / assessment | AI features with an external model | `lms.ai_assist_external` (`third_party_ai_processing`) | consent; vendor named as processor | before each provider call, batch per class | not built |
| lms | Webcam proctoring | `lms.proctoring_video` (`biometric_or_video_monitoring`) | consent, adults only | at exam start | not built |
| lms | Plagiarism check by an outside provider | none yet | — | — | **built without a register row** (brief Q14) |
| attendance | Biometric or face attendance | `attendance.biometric_capture` (`biometric_data`) | consent | device enrolment; manual fallback | not built |
| transport | Live bus location to parents | `transport.live_location_share` | legitimate use (safety; brief Q4) | none | — |
| sis | Health and medical notes | `sis.health_record` (`health_record_keeping`) | legal obligation / medical emergency (*to verify*, brief Q5) | permission-restricted; erasure needs a second reviewer | — |
| finance | Payment gateway sharing | `finance.payment_gateway` (`third_party_payment`) | contract (adult payer; brief Q2) | none; recipient recorded | — |
| careers | Sharing CVs with employers | `careers.employer_sharing` (`third_party_sharing`) | consent (adult student) | per application batch | not built |
| admission | Applicant data before enrolment | `admission.application` (`admission_processing`) | legitimate use, voluntary (brief Q8) | none; retention `applicant.not_admitted` | — |
| frontoffice | Visitor photo and ID | `frontoffice.visitor_log` (`visitor_safety`) | legitimate use (brief Q7) | none; short retention | — |
| platform | MySMS product analytics | `platform.product_analytics` (fiduciary = platform) | consent, adults only | client SDK init | not built |

Each consent-based row adds its check through `privacy-client` when its processing is built (ROADMAP SEC-11 P1).

*Planned* module manifest (one entry per row, `META-INF/mysms/privacy/<module>.json`):

```json
{
  "module": "comms",
  "activities": [
    { "code": "comms.marketing_broadcast", "purpose": "marketing_comms", "legalBasis": "consent",
      "targetsMinors": false, "categories": ["contact"], "recipients": [], "retentionPolicy": "comms.message_log" }
  ],
  "categories": {
    "contact": [ { "table": "comms.message_recipient", "subjectColumn": "person_id", "erase": "anonymise" } ]
  },
  "handlers": ["export", "erase", "anonymise", "restrict"]
}
```

---

## 9. Data-principal rights fulfilment

### 9.1 The per-module handler (built in every module holding personal data except lms; P3)

privacy dispatches tasks as `privacy.rights_request.task_assigned` and retention runs as `privacy.retention.due`; a
module answers with `<module>.privacy_task.completed` (§9.2) or `<module>.privacy_retention.completed` (§10) and asks
`ListActiveHolds` which holds cover the subject (the task carries none, so a hold placed after the dispatch still
counts). A module without a handler leaves its tasks `dispatched` (re-dispatched by the sweep every
`mysms.privacy.erasure.redispatch-after`) and its runs `dispatched`.

**privacy-client's `PrivacyTaskHandler`** (Template Method; a module extends it once, in its `events/` package, and
declares its tables as `ErasureStep`s — table, data category, the select and the erase statement, constants of the
module's repository layer — and its `RetentionSweep`s):

```
inbox (event's tenant transaction)   task_assigned / retention.due naming this module
  → <module>.privacy_task_receipt (task_id PK, tenant_id, kind, payload, status, result, created_at, completed_at; RLS)
      new → received, tenant enrolled in the job <module>.privacy_tasks
      failed → received again (privacy's retry under the same task id)
      done / partial / not_applicable → the stored answer published again; received → nothing (it runs)
job <module>.privacy_tasks (core job runtime, every mysms.privacy-client.task-interval, batches of 100)
  erase / anonymise: ListActiveHolds (gRPC, no transaction open; privacy.legal_hold.check as service:<module>)
      privacy cannot answer → failed LEGAL_HOLDS_UNAVAILABLE, nothing erased
  one write transaction: each step's rows of the subject — a row a hold keeps (subject hold; category hold of the
      step's category; record-set hold naming the table and id) is counted in heldItems, every other row erased,
      audited and announced (<table>.deleted or .updated); receipt finished; <module>.privacy_task.completed
  export: rows per table, no file yet (fileId absent); restrict: not_applicable
  retention: the policy's RetentionSweep — subjects with rows due, 100 at a time; per subject the holds asked, then
      one write transaction; <module>.privacy_retention.completed {runId, status, affected, held}; a policy the module
      does not sweep, or the action archive, answers failed RETENTION_POLICY_UNSUPPORTED
  an unexpected failure rolls the work back and answers failed PRIVACY_TASK_ERROR
```

Erasure clears every identifying column (text the schema requires gets a fixed placeholder such as `Erased`) and
soft-deletes the row, or anonymises a row other records refer to and keeps it; soft-deleted rows are erased too (soft
delete is not erasure); `_enc` columns are cleared. Rows under a legal hold are kept and counted, never silently
skipped.

| Module | Erasure | Retention |
|---|---|---|
| people | addresses, emergency contacts, education history, work experience, certifications, skills, documents, public profile, social links cleared and soft-deleted; the person anonymised (`Erased person`, national id envelope cleared) and kept | no policy |
| sis | health records, activities, leave requests cleared and soft-deleted; the student's exit reason cleared; the subject as a guardian anonymised (occupation, income, contact). Kept: the admission register (number, dates, enrolments, promotions) and discipline incidents — *owner and counsel to decide* | `sis.health_after_leaving`: health records of students who left before the cutoff |
| iam (wave 1) | each account anonymised (`erased-<id>`, no address, number, password or signature) and deactivated through iam.DeactivateUser's service path (signed out everywhere) | `iam.after_inactivity` not swept: its 48 hours' notice is not built (answers failed) |
| hr | the staff record anonymised and kept (qualification, specialisation, interests cleared); leave applications' remarks and decision comments cleared | no policy |
| payroll | bank accounts cleared and soft-deleted (holder `Erased`, account-number envelope cleared); salary structures, components, payslips, lines and payments kept and counted under `payroll.after_leaving` | kept rows answered with the policy code |
| admission | each application of the person with its guardians and emergency contacts cleared and soft-deleted (birth date `1900-01-01`, gender `other`; the application number stays); a guardian's own request finds nothing (guardians have no person link) | `admission.applicant_not_admitted` not swept: most applications have no person |
| attendance | nothing changed: the person's student attendance counted as kept under `attendance.after_leaving` (person known from `attendance.member_record`, fed by sis.student events) | `attendance.after_leaving`: student marks anonymised (student `md5(id)`, remark and leave request cleared) for students not marked since the cutoff (no leaving date in attendance) |
| time_attendance (hrms) | nothing changed: the person's staff attendance days counted as kept under `time_attendance.after_leaving` (person from the sibling `hr.staff`) | archive: not swept (the days back the wage records, as staff attendance was never swept under `attendance.after_leaving`) |
| assessment | homework submissions cleared and soft-deleted; assignment answers and feedback cleared, marks kept; exam results and marksheet lines kept and counted under `assessment.after_leaving` (person known from `assessment.student_record`, fed by sis.student events) | archive: not swept |
| comms (wave 1) | for a student (person from `comms.student_record`, fed by sis.student events): unsent outbound messages cancelled and every message to them cleared; in-app notifications cleared and archived; preferences, group memberships, mailbox, receipts, chat messages and participation, feedback threads about them, tasks, event attendance, calendar exports, class-page membership, reactions, content views, posts and comments cleared and soft-deleted. Guardian and staff accounts are not reached (iam's account events carry no person) | `comms.media_after_withdrawal`: comms stores no media of a person — answers `RETENTION_POLICY_UNSUPPORTED` |
| academics | lesson plans' notes cleared (plan kept); teaching assignments and class-teacher designations kept (operational records); person from `academics.staff_record`, fed by hr.staff events | no policy |
| careers | candidate preferences (functions, industries, profile) cleared and soft-deleted; job applications are keyed by account only and not reached | no policy |
| finance | gateway notifications' raw bodies cleared for the subject's payments; fee dues, concessions, invoices, payments and refunds kept and counted under `finance.after_leaving` (person from `finance.student_record`, fed by sis.student events; account-only payers not found) | archive: not swept |
| frontoffice | no register names a person: erase and export answer `not_applicable` | `frontoffice.visitor_log`: entries older than the cutoff anonymised and soft-deleted, each entry a pseudo-subject (a hold on the entry keeps it) |
| library | reservations cleared and soft-deleted; purchase requests' requester cleared; loans' borrower account cleared; cards deactivated, anonymised and soft-deleted; an open loan (copy out or fine not yet invoiced) kept with its card and counted | no policy |
| platform | notes, attachments, tag assignments and files owned by or attached only to the person (polymorphic references to `people.person`) erased and soft-deleted; the audit log kept (the erasure's evidence); file bytes stay in the store until platform's purge exists | `platform.access_log` has no platform table (the access log is `lms.access_log`): answers failed — *owner: move the policy to lms* |
| transport | `not_applicable`: its rows name sis students and transport consumes no events | no policy |

Rows a module keeps under a retention policy are declared once as `keptRows()` (privacy-client `KeptRows`): the base
handler counts them as held with their policy code and adds them to an export's counts.

*Still to do* (no stubs): lms (it keys everyone by account and stores no person: needs iam's account events to carry
`personId`, or an `lms.member_record` fed by sis.student and hr.staff events — owner decision); guardian and staff
accounts in comms and job applications in careers (same cause); export parts written as platform files (§9.3); files
deleted through platform's file operation (people's documents and photo, platform's erased files keep their bytes);
iam's MFA and legacy credentials; restriction (`restrict`); privacy's pre-erasure notice for `iam.after_inactivity`;
purging soft-deleted rows past a grace period (§10).

### 9.2 Erasure and export saga (privacy side built, P3)

`RightsRequestSaga` (a saga of the rights request aggregate), `TaskAnswerHandler` (the answers), `TaskSweepJob` (the
SLA sweep).

```
DPO verifies identity, assigns ─▶ :startFulfilment (etag required) ─▶ one task per module of
  mysms.privacy.erasure.modules; wave 1 dispatched in the same transaction:
  outbox privacy.rights_request.task_assigned × N {requestId, taskId, type, action, subjectPersonId, module, wave, deadline}
modules: ListActiveHolds → handle → commit → <module>.privacy_task.completed       (modules of §9.1)
privacy inbox (the request's row locked first): the task's answer recorded;
   failed before the 5th attempt → waits retry-delay doubled per attempt (≤ redispatch-after, next_attempt_at),
                                   then the sweep dispatches it again under the same task id
   once no started task waits: any failed → action_required; else the next wave dispatched; else, all answered:
   all done / not_applicable     → closed, fulfilled
   any partial (holds, policies) → closed, partially_fulfilled
   an erasure, either way        → every live consent decision about the subject withdrawn (evidence, channel system),
                                   privacy.erasure.completed {requestId, subjectPersonId}; fully fulfilled only:
                                   the subject's birth date and majority forgotten (status unknown) — a partial
                                   erasure keeps them, since what a hold kept still needs the subject's age
retryTask (action_required or in progress): a failed or unanswered task dispatched again, the request in progress
sweep (TaskSweepJob, every mysms.privacy.erasure.sweep-interval, default 15 min): a task dispatched longer ago than
   mysms.privacy.erasure.redispatch-after (default PT24H) without an answer is dispatched again; so is a failed task
   whose next_attempt_at has come
```

An erasure runs in two waves: `mysms.privacy.erasure.first-wave` (`comms` to stop sending and hide media, `iam` to
disable sign-in) first, the rest once every first-wave task answered done or not applicable, so nothing goes out to
an account being erased; an export's tasks all start at once (wave 1). A late or repeated answer changes nothing; an
answer about no task of that module is rejected (never retried). `:close` declines an erasure or export request only
before its fulfilment started, and never fulfils one (409). Evidence: the tasks' results (counts, held items, policy
codes), the request's events and audit records, and `platform.audit_log` entries written by each module's
`AuditWriter` with the task id. *Planned:* overdue alerts; the outcome letter (file).

### 9.3 Access and export

The legal minimum (s.11) is a **summary**: categories, processing activities and purposes, recipients. Built: the
summary from privacy's register (§9.6). An **export** request runs as the saga of §9.2 (action `export`, every task in
wave 1); each module's answer names its JSON part (`fileId`), kept in the task's result. *Planned (P3):* per-module
counts (export tasks with `summaryOnly = true`) and the **export package**: each module writes a JSON part to
`platform.file_object` (bucket with 7-day expiry), privacy assembles a ZIP with a manifest and an index in the requester's language, delivered by a short-lived signed link after
re-authentication. A guardian's export of a child excludes safeguarding cases and other people's data (counts only,
community §12.7). This replaces ROADMAP's `platform.ExportMyData`.

### 9.4 Correction, grievance, nomination

- **Correction**: the request names the record — the module holding it, the kind of record, its id when known — and
  each field with the value asked for (`details.correction`, 1–20 fields). privacy never writes another module's
  data: the staff correct the record there through its own operation, then close the request as fulfilled, or
  decline it with the reason the requester sees (422 without). *Planned:* a deep link from the request to the record
  (ARCHITECTURE §4), and closing the task when the module's `*.updated` event for that record arrives.
- **Grievance**: free text and a category (`consent`, `access`, `correction`, `data_security`, `communication`,
  `other`); always closed with an answer the requester sees. *Planned:* the answer as a letter file and a comms message
  (ARC-2), and the escalation path to the Board in the closing message.
- **Nomination**: an adult principal names one nominee (`SetMyNominee`; name and contact encrypted, answered masked;
  naming another revokes the active one; `RevokeMyNominee`). A nominee named with their person must be a person
  privacy knows (404) and an adult (403). A nominee who signs in (named with their person)
  submits a `nomination` request for the principal — that the principal died or is incapacitated, with proof — which
  waits in `submitted` for the staff to verify in person or by document, and is fulfilled or declined, never partly.
  Fulfilling it activates the nominee: from then on they submit any other request for the principal as `nominee`,
  always staff-verified. Only a nominee submits a nomination request (422 `RIGHTS_REQUEST_TYPE_NOT_ALLOWED`), and a
  nominee not yet activated submits nothing else (422 `RIGHTS_REQUEST_NOMINEE_NOT_ACTIVE`). A nominee outside the
  school reaches the staff in person. *Planned:* staff recording a request on a nominee's behalf.

### 9.5 Backups and disaster recovery (*planned*)

Erased data stays in database backups until they rotate (the notice states the window). privacy publishes an erasure
tombstone for every erasure it closes (ids only, `privacy.erasure.completed`), and its closed erasure requests are the
ledger. *Planned:* the DR runbook that replays tombstones after any restore, as the community design's off-lineage
safety log (PR-21).

### 9.6 Rights requests: behaviour

`RightsRequestService` (requests), `AccessSummaryFactory` (summaries), `RightsRequestSla` (deadlines).

- **Types accepted:** `access`, `correction`, `grievance`, `nomination`, `export`, `erasure`, `withdraw_all` (clients
  treat enums as open). Erasure and export are fulfilled by the saga of §9.2 (`:startFulfilment`); withdraw-all by
  `:fulfilWithdrawAll` (`privacy.rights_request.fulfil`, etag required; in progress only, 409 otherwise), which takes
  the subject's row lock and withdraws every live consent decision the requester gave about the subject — the
  principal's own too when the requester is their nominee — each with a `withdrawn` evidence entry (channel `system`,
  the staff member as actor), audit record and `privacy.consent_record.withdrawn`, the answers recomputed, and closes
  the request as fulfilled, in one transaction. A separate step rather than on assignment: assigning only staffs a
  request (and may reassign it), and the withdrawal, which cannot be undone, is the staff's deliberate act on the
  version they reviewed, as `:fulfilAccess` is. None of the three is closed as (partially) fulfilled by `:close` (409).
- **Who may submit:** as for consent decisions — an adult for themself (`self`), the active legal guardian of a minor
  (`guardian`; unknown age counts as a minor) — or the active nominee of an adult (`nominee`, §9.4). Anyone else, **a minor asking for themself included**, gets core's
  generic 404 `NOT_FOUND` on the subject (their legal guardian exercises their rights). Refused while impersonating
  (403 `PERMISSION_DENIED`, staff steps too). The requester is always the token's person. Entitlement is read under
  the subject's row lock, as consent changes take it.
- **Identity:** a signed-in requester is verified by the session (`identity_check = signed_in_session`, status
  `verified` at once; one `.submitted` event carrying status `verified`). A nominee's request stays `submitted` until
  the staff verify it (`:verifyIdentity`, `in_person` / `document`).
- **Deadline:** `due_at = submitted_at + mysms.privacy.rights.sla.<type>` (ISO-8601, default `P30D` each, DEPLOYMENT
  §4.2a; legal periods *to verify*). **Extension is not a status**: `:extend` moves `due_at` later (422
  `RIGHTS_REQUEST_DUE_AT_NOT_LATER` otherwise) and keeps `extended_reason`; every extension is audited with its reason.
  `overdue` is derived at read time. The number's year and the deadline are UTC.
- **Steps:** submitted → verified (`:verifyIdentity`); verified | in_progress → in_progress (`:assign`, reassignment
  allowed); submitted | verified → rejected (`:reject`, reason `identity_not_verified` | `not_entitled` | `duplicate`
  | `other`, kept in `outcome_code` and shown to the requester, who may appeal as a grievance); in_progress → closed
  (`:close`, outcome `fulfilled` | `partially_fulfilled` | `declined`, an optional staff note — a blank note is no
  note — and the answer the requester sees, `response`, required for a grievance and a declined correction; the
  requester sees the outcome and the answer, not the note).
- **Separation of duties:** no staff member takes a step on a request they are the subject or the requester of —
  recognised by the token's person or by its login account — and none is assigned one through a login account privacy
  knows for either person: of their student or staff records (`privacy.subject`, tombstones of deleted records keep
  their account) or their guardian links: 409 `RIGHTS_REQUEST_CONFLICT_OF_INTEREST`, the four-eyes answer
  consent verification gives. A staff account privacy has not seen (no hr staff record yet) is not caught.
- **Numbers:** `PRV-<year>-<6 digits>` per tenant from `privacy.number_sequence`.
- **Details:** `details` holds `{"description": "..."}` (required, ≤ 4000 characters), a correction's record and
  fields, a grievance's category (§9.4); no other type carries either (422). Events never carry them.
- **The requester's view** (`MyRightsRequest`): number, type, subject, capacity, status, description, submitted, due,
  overdue, closed, outcome, the correction or grievance details and the school's answer — never the assignee, how
  identity was checked, the extension reason or the staff note. A requester sees their requests only while they still
  decide for the subject (a guardian whose link ended, or whose ward came of age, no longer sees them; a nominee
  whose nomination was revoked, the principal's; staff still handle them).

**Access summary (s.11), from the register only.** `AccessSummaryFactory` generates it from one query over the
register (`processing_activity`, `purpose`, `data_category`) with the tenant's `tenant_purpose` and the subject's
`consent_state`, plus the subject's status:

- **Content:** subject, status (`minor` / `adult` / `unknown`; no subject row = unknown), `subjectRoles`,
  `generatedAt`, and per activity that may concern them — code, module, purpose, legal basis, data categories (sorted), recipients,
  cross-border, retention policy, and for a consent-based activity `consentAllowedScopes` (the widths allowed,
  narrowest first; empty = not allowed). Non-consent activities are always listed; a consent-based one only while the
  tenant has its purpose on (so a platform-fiduciary purpose never). For anyone not known to be an adult, only
  activities with `targets_minors` and never a purpose forbidden for minors. No other module's records or counts yet.
- **Filtered by role:** each register activity names its audiences (`student`, `guardian`, `staff`); the summary
  lists an activity only when the subject holds one of them — student (a live student record), guardian (an active
  link as guardian), staff (a live staff record, from hr's events). A person privacy knows no role of (a visitor, or
  someone whose records have not arrived) gets every activity that may concern them: the conservative answer, still
  "may concern" (brief Q15).
- **`GetAccessSummary`** (staff, `privacy.rights_request.read`; 409 for a non-access request) previews the current
  summary only while the request is open (`submitted` | `verified` | `in_progress`); once ended it answers the kept
  summary, or 404 when it ended without one (rejected, or closed as `declined`). The preview takes no lock.
- **`FulfilAccessRequest`** (`privacy.rights_request.fulfil`; tenant-wide; refused while impersonating; **body etag
  required** — 422 `VALIDATION_FAILED` without, 412 when stale; only an `access` request `in_progress`, 409 otherwise)
  takes the subject's row lock, then the time of the step (taken after the lock), generates the summary, keeps it in
  `rights_request.access_summary` and closes the request as `fulfilled` in one transaction (`generatedAt` =
  `closedAt`); audit step `fulfil_access`; event `privacy.rights_request.closed` without the summary.
- **`GetMyAccessSummary`** answers the kept summary with `GetMyRequest`'s visibility; 404 until fulfilled, for another
  type, another person's request, or a ward the requester no longer decides for.
- **Only `FulfilAccessRequest` fulfils an access request:** `:close` closes one only as `declined` (409
  `INVALID_STATE_TRANSITION` for `fulfilled` | `partially_fulfilled`, after the permission, existence, etag and state
  checks); the guarded close statement and `ck_rights_request_access_fulfilled` enforce it too. A partial answer to an
  access request is not modelled.
- **Versioning:** the kept summary carries `schemaVersion` (`AccessSummarySnapshot.SCHEMA_VERSION`, 2 — adding the
  roles; a kept version 1 reads with no roles; not in the contract) and is never rewritten; the reader skips unknown properties, so a summary kept by a later version stays
  readable. A change a reader must understand raises the version.
- **Consistency:** the factory reads the subject's status and then the register under READ COMMITTED. Consent changes
  and coming of age cannot land in between while fulfilling (both take the subject's lock), but a tenant switching a
  purpose is not serialised by it: a switch committed in between shows in a summary dated a moment before it.
  Accepted — a switch is a rare administrative act.


---

## 10. Retention and deletion engine (privacy side built, P3)

- **Policies** (`RetentionPolicyService`): the platform catalogue `retention_policy` (seeded by the baseline, eleven
  policies: `admission.applicant_not_admitted` 365 days, floor 90; `assessment.after_leaving` 10 years, archive;
  `attendance.after_leaving` 5 years, anonymise; `comms.media_after_withdrawal` 30 days; `finance` and `payroll` 8
  years at the floor; `frontoffice.visitor_log` 90 days; `iam.after_inactivity` 3 years with 2 days' notice;
  `platform.access_log` 1 year at the floor; `sis.health_after_leaving` 365 days; `time_attendance.after_leaving` 5 years, archive — *all values to verify with counsel
  and the owner*, brief Q11) with each tenant's own period and action (`tenant_retention_policy`; no row = the
  catalogue's), set by a tenant administrator (tenant-wide grant, If-Match `"0"` while never set) and never below the
  floor (422 `RETENTION_BELOW_LEGAL_FLOOR`) — the pattern of purposes and `tenant_purpose`. Periods are whole days.
- **Scheduler** (`RetentionJob`, core job runtime, every `mysms.privacy.retention-job.interval`, default a day; a
  tenant enrols when privacy first learns of one of its persons): `RetentionRunService` writes one `retention_run` per
  policy with the cutoff = start of the UTC day − the period in force, and publishes `privacy.retention.due` to the
  owning module; a repeated run of the same day sends nothing (`uq (tenant, policy, cutoff)`). The module's sweep
  (§9.1, the same handler) deletes or anonymises rows past the cutoff in bounded batches, keeps rows under holds
  (`ListActiveHolds`) and answers `<module>.privacy_retention.completed {runId, status, affected, held}`, which
  `RetentionAnswerHandler` records on the run (status, counts, `completed_at`; a late or repeated answer changes
  nothing). Built: `sis.health_after_leaving`, `attendance.after_leaving`, `frontoffice.visitor_log`; every other policy's run stays `dispatched` until its module has a
  sweep (`iam.after_inactivity` answers failed until its notice exists).
- **Pre-erasure notice**: for policies with `notify_before` (Rule 8 inactivity classes, *to verify*) privacy sends the
  notice through comms and sweeps only after the interval.
- **Soft delete is not erasure**: retention also purges rows soft-deleted longer than the policy's grace period
  (default 30 days).
- **Partitions**: for partitioned logs (audit, xAPI statements, message logs) retention is core partition maintenance's
  partition drop (JAVA-CODING-RULES §1 rule 2); privacy's policy is the single source of the number, exposed to each
  module's `mysms.<module>.partition.retention`.
- **Legal holds** (`LegalHoldService`, §5.5) override everything; a hold has a mandatory review date and
  `ListLegalHolds` lists the holds due for review (`active = true AND reviewBy <= "<today>"`). *Planned:* the DPO
  dashboard and a reminder when a review falls due.

---

## 11. Breach management (P4)

```
reported ─▶ triaged ─▶ contained ─▶ board_initial_sent ─▶ board_report_sent (≤ 72 h after became_aware_at)
                │                         │                        └──▶ principals_notified ─▶ closed
                │                         └──▶ principals_notified (the principals told before the detailed report)
                └─▶ not_a_breach (reason, kept)
```

The data protection officer keeps the tenant's register (`privacy.breach`, §5.6): `ReportBreach` numbers the breach
`PRB-<year>-<6 digits>`, makes the reporter its owner and fixes the clock — `board_report_due_at` is
`became_aware_at` + 72 h (which instant counts is *to verify* with counsel); neither time may lie in the future, nor
awareness before detection (422). While it is open, `UpdateBreach` (If-Match) changes what is known — description,
categories, modules, estimated principals, consequences, mitigation, owner — never the times. Every change needs a
tenant-wide grant, is refused while impersonating (403), compares its etag (412) and checks the state it leaves (409),
and writes its audit record and an event with ids, number and status only.

**Notices are recorded, not sent.** comms cannot deliver them yet (its delivery answers 501), so the officer sends
each notice outside MySMS and records it with `RecordBreachNotification` (audience, kind, `sentAt` between awareness
and now, optionally the document as sent). A notice moves the status where it is a step: the Board's `initial`
intimation moves a `contained` breach to `board_initial_sent`; its `detailed` report, recorded once and only after the
intimation, sets `board_report_sent_at` and moves `board_initial_sent` to `board_report_sent`; the principals' notice,
after the intimation, moves the breach to `principals_notified`. A Board update and a processor's or the tenant's
notice change no status. `CloseBreach` needs the principals told and the Board's detailed report recorded;
`MarkBreachNotABreach` ends a triaged breach with its reason.

**Clocks** are derived at read time: `overdue` (the detailed report not recorded and `board_report_due_at` passed,
never for `not_a_breach`) and `hoursRemaining` (whole hours to the deadline, negative once past, absent once the
report is recorded or the breach is not one). The register lists the nearest deadline first.

*Planned:* notices sent through comms in each principal's language with the content the Rules require (*to verify*);
the affected principals from a reproducible query run in the affected modules through a privacy task
(`action = identify`, `affected_query`); clocks on the DPO dashboard and a page to the on-call operator when 24 h
remain (`privacy_breach_clock_remaining_seconds`); when MySMS (as processor) detects a breach, opening the record for
every affected tenant and telling the tenant first — the tenant (fiduciary) owns the Board submission.

---

## 12. User interface

**Built** (ui/02): ACC-13 consent centre (web and phone: guardian per child through the child switcher; a student
under 18 sees guardians' decisions read-only with the date they decide from; an adult decides), ADM-17 purposes and
ADM-18 notices (web, tenant admin); ACC-15 my privacy requests (web: see my data, correct my data with the record and
fields, complain with a category, about oneself or a child; each request's deadline, outcome and answer; a fulfilled
access summary; the nominee, masked, named, changed and removed — phone: see my data and complain, with the same
follow-up); ADM-19 privacy requests (web: the open queue nearest deadline first, coloured overdue / due soon / on
track; each request's details and the steps its state allows; closing writes the requester's answer; an access request
answered with its previewed summary). Hooks in `packages/core` (`privacy.ts`, `privacyAdmin.ts`, `privacyRights.ts`),
screens in `web-kit` `screens/privacy/` and `native-kit` (`ConsentCentreScreen`, `MyRequestsScreen`). Purpose titles come from client i18n
(`enum.privacyPurpose.*`; an unknown code shows "Other"). Notices render as plain text.

*Planned:*

- **Parents and adult students:** "What is affected" counts before withdrawing; badge for `RECONSENT_REQUIRED`;
  notice viewer with the tenant's languages, the short "for your child" summary, full notice and grievance contact;
  consent buttons equal in weight ("Allow" / "Don't allow"), never pre-selected; export and erasure requests (P3) and
  the export download after re-authentication; corrections and the nominee on the phone; onboarding shows consent tasks as a checklist, never as a
  gate to essential services (fees, attendance, notices).
- **Students:** age-appropriate copy; "You can now decide" from the majority date.
- **Administrators and DPO:** per-module task status in the queue (P3); an assignee picker instead of a user id; coverage report
  (counts per class, never names to teachers outside their class); paper-consent entry and the four-eyes verification
  queue; legal holds; retention policies; the breach register and the RoPA export screens (their APIs are built).
- **UX rules:** large targets and plain words (Grade 6 reading level in English; reviewed Hindi and regional
  translations); every toggle change shows its effect in one sentence; withdrawal as quick as a grant (one tap +
  confirm); offline phone queues consent changes with the notice version actually shown; a markdown renderer for
  notices; axe checks on the privacy screens.

---

## 13. Security, multi-tenancy, audit, performance, observability

- **Tenancy**: RLS on every tenant table; services bind `app.tenant_id` through `AppTransactionManager`; every query
  adds `tenant_id = :tenantId`. Global catalogue tables are read-only for the application role.
- **Owner-only data**: self-service uses the token's person and active legal-guardian links only; a guardian never
  learns another guardian's decision; an unknown or unrelated subject → 404.
- **Impersonation**: consent decisions, rights-request steps and breach changes while impersonating are refused (403
  `PERMISSION_DENIED`); support staff can view, not decide; evidence records the impersonator.
- **Breach register**: tenant-level, changed only through a tenant-wide grant; its texts hold ids and counts only and
  never reach an event. **Coverage** answers counts only, never person ids; the whole tenant needs a tenant-wide grant,
  an institution must be within the caller's.
- **Evidence integrity**: per-tenant hash chain built; append-only by privilege and nightly chain verification
  planned (§5.4).
- **No PII in logs, metrics or traces** (JAVA-CODING-RULES §1 rule 5): metrics carry activity codes and counts only.
- **Encryption**: the nominee's name and contact are `_enc` columns (core `FieldCipher`, per tenant and column),
  answered masked, never in an event or a log. *Planned:* export packages encrypted at rest with expiring links.
- **Performance**: the hot path is `consent_state` primary-key lookups (a batch is one `= ANY(:ids)` query); a
  subject's recompute touches at most purposes × widths rows; cache hit ratio target > 95 %.
- **Observability** (*planned*, with OPS-1): `privacy_decisions_total{activity,result,reason}`,
  `privacy_client_unavailable_total{module}`, `privacy_client_cache_hit_ratio`, `privacy_request_overdue{type}`,
  `privacy_task_age_seconds{module}`, `privacy_breach_clock_remaining_seconds`, outbox lag of
  `privacy.consent_state.changed` (the community design's withdrawal-exposure bound depends on it); a health
  indicator for the decision service.

---

## 14. Testing strategy

| Layer (JAVA-CODING-RULES §12) | What |
|---|---|
| Unit | `EffectiveConsentRule` truth table (adult / minor / unknown × guardians granted / refused / narrower / silent × expiry × reconsent × forbidden-for-minor); majority dates; projection merging (`SubjectSync`, `GuardianLinkSync`, `StudentRecords`); *planned:* property-based tests over random record sets, deadline and retention cutoff maths |
| Component (Testcontainers PostgreSQL, real auth filter with test tokens) | Every REST operation's 401 / 403 / 404; two-guardian disclosure; majority transition; scheduled jobs; paper consent four-eyes; notice publish immutability; consent decisions, rights requests, access summary, processing register and its CSV export, breach register, consent coverage, recompute (`privacy/src/component-test`); people's public-profile consent (`PublicProfileConsentComponentTest`) |
| Cross-tenant negatives | Tenant A cannot read, count or infer tenant B's consents, requests, coverage or projections; RLS returns nothing even with a missing `tenant_id` predicate |
| Contract | `privacy.proto` field numbers locked by conformance (`field-numbers.lock.json`); G1 byte-identical copy in `privacy-client`. *Planned:* consumer tests of event payloads in comms |
| End-to-end in `app` (*planned*) | `ErasureSagaComponentTest`: one subject with data in every module; erasure → every module's receipt, held items reported, re-dispatch idempotent. Fail-closed test: privacy stopped → marketing sends nothing, operational notices still go |
| Conformance (*planned* rule) | Every `ActivityCode` constant exists in a manifest; no consent activity targets minors with a forbidden purpose; every module with personal-data tables declares handlers; evidence UPDATE fails with permission denied once SEC-6 lands |

---

## 15. Rollout, effort, future-proofing

### 15.1 Phases still open (one experienced developer + agents; person-weeks)

| Phase | Scope | Effort | Exit |
|---|---|---|---|
| P1 Enforcement | Adopt `privacy-client` at each §8 point whose processing exists (people done); shadow mode when the first rollout needs it; coverage counts | grows with the features of §8 | Each point's refusal test green; for a risky rollout, shadow mode in a pilot tenant for two weeks with no unexpected denials |
| P2 Rights (rest) | Correction deep links and task closing on the owner's event; grievance letters through comms; staff recording a request on a nominee's behalf; corrections and the nominee on the phone; an assignee picker | 1–2 | A pilot DPO handles a mock request of each type end to end |
| P3 Erasure and retention (rest) | Task handlers and retention sweeps in every other module holding personal data (§9.1 list; people, sis and iam are built), export parts as files and the ZIP assembly and download, the DR tombstone replay runbook, manifests + conformance rule (privacy's side is built: tasks and saga, holds, retention policies and runs, tombstones) | 5–7 | An erasure across all modules end to end, in a component test per module and the monolith |
| P4 Breach and reports (rest) | Breach notices through comms (once comms delivers), affected-set identification, clock paging and the DPO dashboard, the breach and coverage screens, coverage per class, RoPA as PDF, consent-manager inbound placeholder (the register, its clocks, the CSV export and coverage counts are built) | 1–2 | Tabletop breach exercise |

P1–P3 should be done before 13 May 2027. Purposes start disabled per tenant; community photos stay blocked until
their enforcement exists (fail closed, as the community design requires).

### 15.2 Seeding

*Planned:* system notices in English and Hindi seeded as templates a tenant must review before enabling a purpose;
every consent purpose of an existing tenant stays off until it publishes a notice (today: no `tenant_purpose` row = off).

### 15.3 Versioning and compatibility

Purposes and activities are versioned; a material change bumps the version and, if the notice says so, sets
`requires_reconsent`. The proto evolves additively (package `mysms.privacy.v1`; a breaking change is `v2` served side
by side). Event payloads are additive; consumers ignore unknown fields. Kept access summaries carry their own
`schemaVersion` (§9.6).

### 15.4 Future-proofing

- **Consent managers** (Rule 4): an inbound adapter (`privacy/integration/ConsentManagerAdapter`) maps external
  artefacts to `consent_record` (`channel = consent_manager`, `external_artefact_id`, purposes via `external_code`);
  outbound withdrawal notifications. Waits for the Board's technical standards (*to verify*).
- **DigiLocker verification** of parents: a `VerificationMethod` strategy slot.
- **Policy engine**: `EffectiveConsentRule` is a pure function behind an interface; OPA or Cedar can replace it if
  rules become tenant-programmable.
- **Other jurisdictions**: age of majority, purposes and floors are data (tenant country), not code.

---

## 16. Open questions and risks

**Approved by the owner:** a separate `privacy` module (a kernel module in the decided reorganisation); `privacy-client` as a shared
facade, an exception like `auth-client` (CLAUDE.md §2); Caffeine for its cache; people's consent parts retired into
privacy; purpose titles from client i18n; `privacy-client` TTL-only and enforce-only; impersonation and not-a-guardian
answered with core's generic 403 / 404; a signed-in requester verified by the session; extension as a moved deadline,
not a status.

**Built on the implementer's judgement, for the owner to confirm (ROADMAP DEC-20):** separation of duties as a 409
like consent's four eyes; the nominee activated by a fulfilled nomination request, the nominee's other requests always
staff-verified; nominee operations as `:set` / `:revoke` custom methods (conformance allows no PUT); the access summary
filtered by the register's audiences and the subject's roles (no role = the conservative "may concern" list), with the
audiences seeded per activity in the baseline; a grievance and a declined correction always answered.

**Still open (owner):**

1. Default deadline for rights requests (30 days proposed) and grievances.
2. Majority transition defaults (community open decision 9).
3. Is MySMS ever a fiduciary (product analytics, its own marketing)? If yes, a platform-level notice and purposes with
   `fiduciary = platform`.
4. Retention values per category (counsel input for every catalogue row).
5. Data-category sensitivity levels (ROADMAP DEC-17) and evidence append-only by privilege (DEC-18).

**To verify with counsel** ([legal brief](PRIVACY-LEGAL-REVIEW-BRIEF.md)): Rule 3 notice contents; Rule 10
verification methods acceptable for schools; scope of the Fourth Schedule education exemption (analytics, transport
GPS, CCTV); legal-basis mapping; retention laws for school records; grievance period; breach notice contents and clock
start; whether schools fall in a Rule 8 erasure class; POCSO and access requests; the plagiarism provider; whether a
"may concern" summary satisfies s.11.

**Risks**

| Risk | Mitigation |
|---|---|
| Fail-closed blocks legitimate operations when privacy is down | Legitimate-use activities need privacy only once per TTL; consent activities degrade only non-essential features; health alert; shadow mode before enforce for risky rollouts |
| Low guardian response → photos rarely publishable | Coverage reports, reminders through notices, paper-form flow with four eyes |
| Erasure misses copies (person cards, exports, caches, backups) | Manifests + conformance; C-5 copies handled per module; tombstone replay after restore |
| Evidence becomes its own privacy risk | User-agent hash, no device fingerprint, evidence retention policy |
| Scope creep into a policy engine | Non-goal; the rule is one pure function |
| Rules change before May 2027 | Purposes, floors and deadlines are data; legal text lives in notices |
