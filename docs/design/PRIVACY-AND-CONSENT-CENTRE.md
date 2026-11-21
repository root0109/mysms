# Privacy and Consent Centre — technical design

Status: **approved by the owner; nothing built yet.** Its prerequisite (API permissions published to iam, role-based permissions filter, JAVA-CODING-RULES §16.2) is built; implementation is tracked as ROADMAP SEC-11. Author: principal engineering review, 18 Nov 2026. Binding rules stay in
[`CLAUDE.md`](../../CLAUDE.md), [`ARCHITECTURE.md`](../../ARCHITECTURE.md) and
[`JAVA-CODING-RULES.md`](../../JAVA-CODING-RULES.md); where this document proposes to change one of them it says so
and has the owner's approval (section 16).

Related work this design absorbs or re-targets: ROADMAP `ARC-6` (tenant export, erasure, archival, `platform.ExportMyData`,
`platform.retention_policy`), `BE-1` (people's 7 consent operations answering 501), the community network design
([`SCHOOL-COMMUNITY-NETWORK-TECHNICAL-DESIGN.md`](../product/SCHOOL-COMMUNITY-NETWORK-TECHNICAL-DESIGN.md) §8.4 consent
rule K17, §12.7 consent centre COM-10, prerequisites PR-1, PR-2, PR-3, PR-18, PR-22) and the DPDP notes in
[`CLASSROOM-VOICE-ASSISTANT-BRAINSTORM.md`](../product/CLASSROOM-VOICE-ASSISTANT-BRAINSTORM.md) §8.

---

## 1. Summary, goals, non-goals, and where it lives

### 1.1 Summary

MySMS processes the personal data of children at scale: names, photos, marks, attendance, health notes, fees, GPS
positions on the bus, learning analytics. Under India's DPDP Act 2023 and DPDP Rules 2025 each school (the tenant) is
the **Data Fiduciary** and MySMS is, for almost everything, its **Data Processor**. The fiduciary needs tools that a
processor must supply: notices, verifiable parental consent, purpose-bound processing, rights requests with deadlines,
erasure that actually reaches every module, retention, and a breach register. Today MySMS has one table
(`people.consent_record`, `database/migrations/people/V1_050_001__people_baseline.sql` line 539), seven consent
operations that answer 501, and no erasure or retention machinery.

The **Privacy and Consent Centre** is a new domain module, `privacy`, with its own schema, that owns:

1. the catalogue of purposes, data categories and processing activities (a RoPA-lite generated from the modules);
2. versioned, multilingual notices;
3. consent records with full evidence, guardian capacity and verification, and a precomputed **effective consent state**;
4. a small **decision API** (gRPC) and a thin client facade (`privacy-client`) that every module uses to ask
   "may I process subject S for purpose P at scope W?" in batch, cached, fail-closed;
5. data-principal rights requests (access, correction, erasure, grievance, nomination) with SLA state machines and
   event-driven fan-out to the modules that hold the data;
6. a retention and deletion engine (policies per data category, legal holds, per-module sweeps);
7. a breach register with the notification clock.

### 1.2 Goals

| # | Goal | Measured by |
|---|---|---|
| G1 | No processing that needs consent happens without a live, verified consent at the right scope | Every enforcement point in §8 has a component test that proves a refusal blocks it |
| G2 | One place for parents, adult students and staff to see and change what they agreed to | COM-10 screens built on privacy's self-service API |
| G3 | Rights requests answered within the deadline the tenant configures (default 30 days; legal maximum to verify) | SLA dashboard; overdue = 0 |
| G4 | Erasure reaches all modules that hold the subject's data, idempotently, with evidence | Per-module task receipts; `ErasureSagaComponentTest` across a monolith with all modules |
| G5 | Consent checks do not slow down hot paths | p99 of a batch check of 500 subjects < 15 ms in-process, < 40 ms remote; cache hit ratio > 95 % |
| G6 | Ready for DPDP's main obligations (13 May 2027) with a margin | Phases P0–P3 of §15 done by March 2027 |

### 1.3 Non-goals

- **Not an authorisation service.** Who may run an operation stays with iam roles, the edge `AuthFilter`/`EdgeAuthorizer`
  and `DataScope` (JAVA-CODING-RULES §16). Consent answers a different question: *may this data be used for this
  purpose*, independent of who is asking. API permissions are published to iam, not to privacy (JAVA-CODING-RULES §16.2).
- **Not a general policy engine** (no OPA/Cedar now; §15.4 keeps the door open).
- **Not a legal opinion.** Statements of law here are engineering inputs; anything marked *to verify with counsel* must be
  confirmed before the feature it drives ships.
- **Not a tenant-offboarding tool.** Whole-tenant export/erasure (ARC-6 first half) reuses the same per-module handlers
  but is a platform operation, specified in ARC-6.
- **No field-level redaction framework.** Modules decide what to omit with explicit mapping, not reflection (§3).

### 1.4 Decision: a separate `privacy` module

**Decision: build `privacy` as the 22nd module with its own schema, service and contract. Under ARCHITECTURE §6.2
Plan A it stays a module of its own (the 10th), not part of `identity`.**

Options considered:

| Option | For | Against |
|---|---|---|
| A. Inside `people` (where `consent_record` lives today) | No new service; consent sits beside the person | people is a profile store with a self-service and HR flavour; rights orchestration, retention, breach and notices are a different capability with a different user (the school's grievance/data-protection officer). people would have to *receive* completion events from all 21 modules and orchestrate them: it becomes the busiest event consumer in the system and its blast radius grows. people is read by 8 modules today; consent checks would make it hot for every module. |
| B. Inside `iam` | iam is already on every request | Rule 3 of ARCHITECTURE §6.1: identity must scale on its own and stay small and security-reviewed. Consent is not authentication. Mixing them invites "consent as a permission"; permissions belong to iam (§16.2), consent to privacy. |
| C. Inside `platform` (ARC-6 assumed `platform.retention_policy` and `platform.ExportMyData`) | platform is tenant-level infrastructure; owns files and audit log | platform is "read-mostly, changed by tenant admins only" (ARCHITECTURE §6.2). Consent and rights requests are written by parents and students at scale. platform's other users (sales enquiry, themes, storage quota) are unrelated. |
| **D. New `privacy` module** | One capability, one owner, one audit boundary; can be scaled and secured on its own; decision API is a narrow, stable contract; orchestration (fan-out, SLAs, holds) lives in one place; matches the owner's inclination | One more service (in the monolith it is one more schema and a few beans); one more proto copy in callers; needs a dependency-graph slot (below) |

Why D survives the merge rules of ARCHITECTURE §6.1: it is a **distinct business capability with distinct users**
(rule 1), has **weak synchronous coupling** to people (only a person card lookup; subject facts arrive by events —
rule 2), and has **its own data sensitivity and audit regime** (rule 3: like finance and iam, it stays apart). If the
owner ever takes Plan B (6 modules), privacy may join `foundation` as a second schema without any change to its
contract, because callers only ever see the decision API.

**Place in the dependency DAG** (ARCHITECTURE §2):

| Module | May depend on (gRPC) | Learns from events only |
|---|---|---|
| `privacy` | `people` (person card), `iam` (user → person), `org` (institution names, jurisdiction), `platform` (files for evidence and export packages, tenant settings) | `sis.student_guardian.*`, `sis.student.*`, `sis.student_enrollment.*`, `people.person.*`, `hr.staff.*`, every module's `<module>.privacy_task.*` |
| every module that enforces consent (`comms`, `lms`, `sis`, `assessment`, `attendance`, `transport`, `finance`, `admission`, `careers`, `library`, `frontoffice`, `hr`) | gains `privacy` | `privacy.consent_state.changed`, `privacy.rights_request.*`, `privacy.retention.due` |

privacy never calls `sis` or any module that calls privacy, so the graph stays a DAG: guardian links and enrolments
reach privacy as events (the same "event-only, against the arrow" pattern listed at the end of
`database/logical-references.md`). Orchestration of erasure and export is **events out, events back**, never gRPC from
privacy into the modules.

---

## 2. Regulatory drivers (engineering view)

Sources the team already relies on: the DPDP Act 2023 and the DPDP Rules 2025 (notified 13/14 Nov 2025), as summarised in
`CLASSROOM-VOICE-ASSISTANT-BRAINSTORM.md` §8. Phasing: the Board provisions applied at once; **consent-manager
provisions from 13 Nov 2026** (in force now); **children's data, notice/consent standards and most fiduciary duties from
13 May 2027**. Everything below is a requirement on the *product*, phrased so a developer can test it.

| Ref | Obligation (summary) | What the product must do | Confidence |
|---|---|---|---|
| Act s.5, Rule 3 | Notice before or at consent: itemised data and purposes, how to withdraw, how to complain, how to reach the Board; plain language; available in English and Eighth Schedule languages on request | Versioned, itemised notices per purpose; per-language translations; a notice version is recorded with every consent; withdraw and complaint links on every notice | High; exact itemisation format *to verify with counsel* |
| s.6 | Consent free, specific, informed, unconditional, unambiguous, by clear affirmative action, limited to necessary data; withdrawal as easy as giving | No pre-ticked boxes; one purpose per toggle; no bundling a non-essential purpose into enrolment; withdraw is one tap in the same screen | High |
| s.6(7)–(9), Rule 4 | Consent Managers (registered with the Board) may give, manage, review and withdraw consent on a principal's behalf | Consent records carry `channel = consent_manager` and an external artefact id; an inbound adapter placeholder (§15.4) | Medium: technical standards for artefacts *to verify* |
| s.7 | Legitimate uses that do not need consent (e.g. data voluntarily provided for a specified purpose where the principal has not objected; legal obligations; medical emergency; employment) | Every processing activity records its **legal basis**; consent checks are skipped (and logged as such) for non-consent bases | High that the section exists; which school activities fit *to verify with counsel* |
| s.8(4)–(5) | Reasonable security safeguards | Existing floor (RLS, `_enc` columns, no PII in logs) plus privacy's own audit | High |
| s.8(6), Rule 7 | Breach: intimate the Board and each affected principal; Rules require intimation without delay and a detailed report to the Board within 72 hours | Breach register with two clocks; templated principal notices through comms | High on the 72 h report; exact contents *to verify* |
| s.8(7) | Erase when consent is withdrawn or the purpose is no longer served, unless retention is required by law; also cause processors to erase | Erasure saga; retention engine; per-module handlers; legal holds | High |
| Rule 8 + schedule | Specified classes of fiduciaries erase after periods of inactivity with 48 h prior notice; logs kept a minimum of one year | Retention policies support "inactive for N" triggers and pre-erasure notice; audit and access logs kept ≥ 1 year | Medium: which classes apply to schools *to verify* |
| s.8(9)–(10), s.13 | Publish contact of a person who answers questions; grievance redressal; Rules fix a response period (stated as up to 90 days) | Grievance request type with SLA; tenant publishes DPO/grievance contact in every notice | Medium on the period: *to verify* |
| s.9(1), Rule 10 | **Verifiable consent of a parent or lawful guardian** before processing a child's (under 18) data; check the person is an identifiable adult (reliable details already held, or a virtual token e.g. through DigiLocker) | Guardian capacity on each consent; verification method and evidence; a minor's own toggle never counts alone | High on the rule; DigiLocker integration specifics *to verify* |
| s.9(2)–(3) | No processing likely to harm a child's well-being; **no tracking, behavioural monitoring or targeted advertising directed at children** | Purposes `behavioural_analytics`, `targeted_advertising` are **forbidden for minors** in the catalogue (not even with consent); a build-time check that no activity of those kinds targets minors | High |
| Fourth Schedule | Educational institutions exempt from s.9(1) and s.9(3) for tracking/monitoring restricted to educational activities or children's safety | Activities flagged `education_exemption = true` record the clause relied on; read narrowly (brainstorm §8) | Medium: scope contested, *to verify* |
| s.11 | Right to a summary of personal data processed, processing activities, and the fiduciaries/processors it was shared with | Access request returns a summary generated from the RoPA-lite and per-module counts; full export is an extra, not the legal minimum | High |
| s.12 | Correction, completion, updating, erasure | Correction request routed to the owning module's normal update operation, with audit | High |
| s.14 | Nominate another person to exercise rights on death or incapacity | `privacy.nominee` table and a nomination request type | High |
| s.10 | Significant Data Fiduciary (if notified): DPO in India, independent audit, DPIA | Reports exportable for an auditor; DPIA templates are outside the product | Medium: schools unlikely to be notified; MySMS itself might be if it becomes a fiduciary at scale |
| Penalties | Up to ₹250 crore for security failures; up to ₹200 crore for breach-notification and children's obligations | Prioritise G1, G3, breach clock | Amounts *to verify* |

**POCSO and safeguarding touch points.** Counselling notes, health records and incident cases can contain disclosures
that the POCSO Act 2012 obliges staff to report (failure to report is an offence). Engineering consequences:
consent withdrawal and erasure **never** delete records under a safeguarding legal hold; a guardian's access request
excludes case files about that guardian's own conduct (counted, not shown — mirrors community design §12.7); the
`health` and `counselling` data categories are restricted-access by default and their erasure requires a second
reviewer. All *to verify with counsel*, and the hold always wins over automation.

**Other retention law to verify.** School record-keeping (admission registers, transfer certificates, board exam
records), accounts (tax and company law commonly cited as 8 years), and payroll records each impose minimum retention
that overrides erasure. The retention catalogue (§10) carries a `legal_reference` text per policy so counsel can
review one table.

**Roles.** By default the tenant is the fiduciary and MySMS the processor; notices name the school. Where MySMS acts on
its own account (marketing its own products, product analytics across tenants, model training) MySMS is a fiduciary and
those purposes are platform purposes with MySMS as the named fiduciary (`purpose.fiduciary = platform`). Model training
on children's data is not offered.

---

## 3. What the reference projects do, and what we take

`plt-access-control-service` (ACS) is a Spring service (Spring Data JDBC, Liquibase, Redis, GCP Pub/Sub, gRPC) that
combines RBAC (roles, permissions, features/operations, user roles) with a FHIR-flavoured consent model:
**consent templates** (`consent_template`: name, `version`, `parent_id` grouping versions, `status` DRAFT / PROPOSED /
ACTIVE / REJECTED / INACTIVE / ENTERED_IN_ERROR, `type` PERMIT / DENY, JSONB `action`, `scope`, `category`, `purpose`,
`policy`, `security_label`, attachment for the legal text), **field groups** (`field_group`: a named set of fields of
a `resource_type`, published by each service at start-up), and **consent assignments** (`consent_assignment`: giver,
receiver type user/role, receiver id, organisation, `start_date`/`end_date`, `performer`, `verified`, `verified_with`,
`verification_date`, with `assignment_field_group` rows down to a resource id). A scheduler with ShedLock soft-deletes
expired assignments; `LegalDocumentService` derives ACCEPTED / ACCEPTED_EXPIRED / REVIEW_REQUIRED / NOT_ACCEPTED by
comparing a user's accepted version with the latest version of each template; a `BaseGdprService.hardDelete(subjectId)`
interface with FAILED / PARTIALLY / SUCCEED results deletes a subject's rows. The service populates Redis with
consent assignments, templates, field groups and user roles (on authentication events), guarded by
"population in progress" keys.

`access-control-library` is the client every service embeds: a 2,000-line `ConsentService` answering
`checkConsentOnResource(giverId, action, resourceType, resourceIds)` (READ/CREATE/UPDATE/DELETE) and returning the
consented fields, a `checkFieldsConsent` that **blanks un-consented fields by reflection**, `findConsentResources`
for list filtering, a Redis-first read with fallback to the ACS gRPC stub, a global on/off property, "a user always has
consent to their own data", start-up publishers that push the service's field groups and API permissions to ACS, and a
`PermissionsFilter` that enforces API permissions from the cache.

| Idea in the references | Verdict for MySMS | How it appears here |
|---|---|---|
| Versioned consent template with a version group (`parent_id`) and a lifecycle | **Adopt, simplified** | `notice` + `notice_version` (DRAFT → ACTIVE → RETIRED); a material change sets `requires_reconsent` |
| Accepted / accepted-expired / review-required / not-accepted derived per user | **Adopt** | `ConsentStatus` in the self-service API: `GRANTED`, `REFUSED`, `NOT_ANSWERED`, `RECONSENT_REQUIRED`, `EXPIRED` |
| PERMIT vs DENY templates | **Adopt as `decision`** | Explicit `refused` records; most-restrictive wins among legal guardians (community K17) |
| Validity window, `verified`, `verified_with`, `verification_date`, `performer` | **Adopt** | `valid_from`/`valid_until`, `verification_status`, `verification_method`, `verified_by`, `recorded_by` |
| Field groups published by each service at start-up | **Adapt**: build-time, not runtime | Each module ships `META-INF/mysms/privacy/<module>.json` (processing activities, data categories with tables/columns, handlers); the conformance build checks it; privacy's baseline seeds it. Runtime publication is not needed here: the manifest only changes with a release, so the build checks and seeds it |
| `hardDelete(subjectId)` with FAILED/PARTIALLY/SUCCEED | **Adopt and extend** | Per-module `PrivacyTaskHandler` for erase/anonymise/export/restrict with typed outcomes and counts, idempotent by task id |
| Scheduled expiry of consents with a cluster lock | **Adopt** | Daily expiry and majority jobs on the ARC-4 job pattern (one runner per tenant shard, lock in privacy's schema) |
| Client checks in batch over resource ids | **Adopt** | `BatchCheckConsent` and `ConsentChecker.check(Set<UUID>…)` |
| Cache first, remote fallback | **Adopt** | In-process bounded cache, remote on miss, event-driven invalidation |
| Global enable flag | **Adapt** | `mysms.privacy.enforcement = off / shadow / enforce`, global default with a per-tenant override; `shadow` logs would-be denials without blocking |
| Owner always has consent to own data | **Adopt** | Self-access never consults consent (it is access, not a new purpose) |
| Attachment holding the legal text | **Adopt** | `notice_version.document_file_id` in `platform.file_object` plus the rendered text stored in privacy |
| Requirement traceability (API-ACSxx → SRS ids) | **Adopt lightly** | Requirement ids `PCC-nn` in this doc map to named component tests |
| Receiver model (consent given *to* a user or role), org-hierarchy search | **Skip** | MySMS access is RBAC + `DataScope`; consent here is *purpose*-based. A recipient is recorded only for third-party sharing |
| CRUD-per-field consent (READ/CREATE/UPDATE/DELETE per field) | **Skip** | Over-fine for schools; purposes × scopes cover the real decisions |
| Reflection-based field blanking (`checkFieldsConsent`) | **Skip** | Hidden mutation of DTOs; MySMS maps explicitly (MapStruct) and modules omit data by design |
| Shared Redis cache populated by the server and read by every client; "population in progress" flags | **Skip** | Complex, cross-service coupling through a cache; MySMS uses per-process caches + events + a precomputed state table |
| API permissions published to the access-control service; `PermissionsFilter` in each service | **Adopted, outside this design** | Each module publishes its operation table to iam at start-up and auth-client's filter enforces role permissions through an iam-fed cache (JAVA-CODING-RULES §16.2, ROADMAP SEC-0, built before this centre). Consent is not part of that filter |
| FHIR Consent resource shape (JSONB `policy`, `security_label`, `class`, `code`) | **Skip now, map later** | Purposes carry an optional `external_code` for consent-manager interoperability |
| Thread-pool fan-out of many single checks | **Skip** | One batch RPC instead |
| Liquibase, Spring Data JDBC, Lombok, Feign | **Skip** | MySMS conventions (Flyway baseline, JdbcClient, records, gRPC) |

---

## 4. Domain model

### 4.1 Concepts

| Concept | Meaning | Owner | Lifecycle |
|---|---|---|---|
| **Purpose** | Why data is processed (`photo_video_publication`, `community_naming`, `marketing_comms`, `learning_analytics`, `third_party_payment`, `transport_live_location`, `biometric_attendance`, `health_record_keeping` …) | platform-wide catalogue, seeded by privacy's baseline; tenant can enable/disable and attach notices | `active`, `deprecated` (no new consents; existing still evaluated), `retired` |
| **Legal basis** | `consent`, `legitimate_use_voluntary`, `legal_obligation`, `education_exemption`, `medical_emergency`, `employment`, `contract` (adults, e.g. fee payer) | per processing activity | fixed per activity version |
| **Scope (width)** | How far the processing reaches: `class < institution < inter_school < public` for publication purposes; `tenant` for others | per purpose (`scope_kind`) | a consent at a width covers every narrower width (community K17) |
| **Data category** | `identity`, `contact`, `photo_media`, `academic_record`, `attendance`, `health`, `counselling`, `financial`, `location`, `biometric`, `device_usage`, `government_id` … each with a sensitivity level | catalogue | used by notices, RoPA, export, erasure, retention |
| **Processing activity** | Module × activity × purpose × legal basis × categories × recipients × retention policy (RoPA-lite row) | declared by each module's manifest; versioned | `version` increments on change; a material change can force re-consent |
| **Notice** | The text shown to the principal for a purpose (or a bundle of purposes), per tenant, per language | tenant admin (`privacy.notice.*`) | `draft → active → retired`; one active version per notice |
| **Consent record** | One decision by one giver for one subject × purpose × scope, under one notice version | privacy | `granted` or `refused`; may be **withdrawn**, **superseded** by a newer decision of the same giver, or **expire** (`valid_until`) |
| **Consent evidence** | Append-only ledger of everything that happened to consent records (who, how, channel, IP, device, notice version, verification) | privacy | never updated or deleted (kept for the retention of consent evidence) |
| **Effective consent state** | The computed answer per subject × purpose × scope: `allowed`, `denied`, `not_answered`, with the basis | privacy, recomputed on every change | rewritten in the transaction of the change; published as an event |
| **Subject** | A data principal known to privacy: person id, user ids, majority status, institutions | projection from people, iam, sis, hr events | `minor`, `adult`, `unknown` (= minor) |
| **Guardian link** | Projection of `sis.student_guardian` (legal guardian, custody, status) | from sis events | only `active` + `is_legal_guardian` links can give consent for a minor |
| **Verification** | How the giver was established as an adult guardian (Rule 10): `existing_record` (guardian identity already held and linked by the school), `in_person_staff_check`, `digilocker_token`, `otp_on_registered_mobile` (weak; to verify), `paper_form_countersigned` | privacy | a consent counts only when its verification is `verified` |
| **Rights request** | `access`, `export`, `correction`, `erasure`, `grievance`, `nomination`, `withdraw_all` | privacy | SLA state machine (§4.3) with per-module **tasks** |
| **Legal hold** | Freezes erasure/retention for a subject, a category or a record set (safeguarding, litigation, audit) | privacy (safeguarding lead, DPO) | `active → released` |
| **Retention policy** | Data category × trigger × period × action (`delete`, `anonymise`, `archive`) | catalogue default, tenant override within legal bounds | versioned |
| **Breach** | An incident affecting personal data, with clocks for Board and principal notifications | privacy (DPO) | §11 |
| **Nominee** | Person nominated by an adult principal to exercise rights on death or incapacity | privacy | `active`, `revoked` |

### 4.2 Effective-consent rule

The rule generalises community design §8.4 (K17) to every purpose. Inputs: the subject's majority status, the live
records for (subject, purpose), the active legal-guardian links, the purpose's settings for the tenant, the legal basis
of the activity asking.

```
decide(subject S, purpose P, scope W, activity A):
  if A.legalBasis != consent                      -> ALLOWED (basis = A.legalBasis)        // logged, not stored
  if P.forbiddenForMinors and S.status != adult   -> DENIED  (reason = FORBIDDEN_FOR_MINOR)
  if tenantPurpose(P).disabled                    -> DENIED  (reason = PURPOSE_DISABLED)
  live = records(S, P) where withdrawn_at is null and not superseded
                         and (valid_until is null or valid_until >= today)
                         and verification_status = verified
  if S.status == adult:
      givers = {S}                                // only the student's own records count
  else:
      givers = { g | link(g, S).active and link.isLegalGuardian }
  relevant = live where giver in givers
  if any r in relevant: r.decision == refused and r.scope <= W   -> DENIED (REFUSED)
  if any r in relevant: r.decision == granted and r.scope <  W
         and S.status != adult                                    -> DENIED (NARROWER_GRANT)  // most restrictive guardian
  if any r in relevant: r.decision == granted and r.scope >= W    -> ALLOWED (CONSENT)
  if notice(P).activeVersion.requiresReconsent and the grant predates it -> DENIED (RECONSENT_REQUIRED)
  -> DENIED (NOT_ANSWERED)                                         // silence never allows
```

"Silence of a second guardian does not block" holds because only *recorded* refusals and narrower grants of other legal
guardians count. The rule is a pure function (`EffectiveConsentRule`, a Strategy) so a policy engine can replace it later
(§15.4). Its output for every scope is stored in `privacy.consent_state` and published; consumers never re-implement it.

### 4.3 State machines

**Consent record** (`decision` is fixed for the row; changes create a new row):

```
            record(granted|refused)
   (none) ───────────────────────────▶ LIVE ──withdraw──▶ WITHDRAWN
                                         │
                                         ├──newer decision by same giver──▶ SUPERSEDED
                                         └──valid_until passed (daily job)─▶ EXPIRED
   LIVE + verification_status=unverified ──verify (second staff member)──▶ LIVE(verified)
```

**Rights request**:

```
SUBMITTED ─identity verified─▶ VERIFIED ─triage─▶ IN_PROGRESS ─all tasks done─▶ FULFILLED ─▶ CLOSED
    │                              │                    │   │
    │                              │                    │   └─task failed / partly─▶ ACTION_REQUIRED ─retry/override─▶ IN_PROGRESS
    │                              │                    └─hold blocks part─▶ PARTIALLY_FULFILLED ─▶ CLOSED
    └─identity not verified────────┴─▶ REJECTED (reason code, appealable as grievance)
EXTENDED: an SLA extension with recorded reason (tenant policy); OVERDUE is a derived flag, not a state.
```

SLA clocks: `due_at = submitted_at + tenant SLA for the type`; reminders at 50 %, 80 %, 100 % to the assignee and the DPO
role through comms. Grievances have their own SLA (default 30 days, legal outer bound *to verify*).

**Erasure task** (one per module per request): `PENDING → DISPATCHED → DONE | PARTIAL | FAILED | NOT_APPLICABLE`,
with `attempts`, `last_error_code`, counts per table, and `held_items` (count of rows kept under a hold or a legal-retention
policy, with the policy code).

**Age-of-majority transition.** A daily job (per tenant, jurisdiction age from the tenant's country, 18 for India)
marks subjects whose 18th birthday is today: `subject.status = adult`, publish `privacy.subject.majority_reached`,
recompute all states. From that moment only the student's own records count, so every consent purpose for that student
becomes `NOT_ANSWERED` unless the policy below applies; the student gets a "you can now decide" task in the consent centre;
guardians lose the consent toggles for that student (they keep the read of what was published under their consent).
Grandfathering per purpose (`purpose.majority_grace`): publication at class/institution width stays allowed for already
published items (community design §8.4 default), new processing needs the student's own grant; marketing and analytics
lapse at once. *Open decision 9 in the community design remains the owner's to rule; this design implements that
default.*

### 4.4 Legitimate-use bases

The catalogue marks each activity; the client library short-circuits non-consent bases locally (no RPC), records a
`basis` in its metrics, and never asks parents to consent to what the school must do anyway (attendance, marks,
fees). Asking for consent where the basis is legal obligation would be misleading: withdrawal could not be honoured.

---

## 5. Database schema (`privacy`)

Conventions (database/README, JAVA-CODING-RULES §5, §6, §14): `tenant_id uuid NOT NULL` on every tenant table; ids are
UUIDv7 from Java (`gen_random_uuid()` only as fallback default); `FORCE ROW LEVEL SECURITY` with the policy
`tenant_isolation`; in-module FKs are tenant-composite `(x_id, tenant_id) → (id, tenant_id)` with a matching unique
key; audit columns `created_at/by, updated_at/by`, `deleted_at` soft delete only where a row can be "removed" by a user,
`row_version` for optimistic locking; enums as `varchar` with CHECK; partial unique indexes `WHERE deleted_at IS NULL`;
COMMENT on every table and column (omitted below for length). File:
`database/migrations/privacy/V1_215_001__privacy_baseline.sql` (next free number after `V1_210`).

### 5.1 Catalogue (global, platform-maintained)

```sql
CREATE SCHEMA IF NOT EXISTS privacy;

-- Global catalogue: no tenant_id, read-only for the application role (like iam.permission).
CREATE TABLE privacy.data_category (
    id uuid NOT NULL,                              -- md5('mysms.data_category:' || code)::uuid, stable across envs
    code varchar(40) NOT NULL,
    sensitivity varchar(12) NOT NULL,              -- normal | sensitive | restricted
    description text NOT NULL,
    CONSTRAINT pk_data_category PRIMARY KEY (id),
    CONSTRAINT uq_data_category_code UNIQUE (code),
    CONSTRAINT ck_data_category_sensitivity CHECK (sensitivity IN ('normal', 'sensitive', 'restricted'))
);

CREATE TABLE privacy.purpose (
    id uuid NOT NULL,
    code varchar(40) NOT NULL,                     -- photo_video_publication, marketing_comms, learning_analytics ...
    scope_kind varchar(12) NOT NULL,               -- width (class..public) | tenant
    fiduciary varchar(10) NOT NULL DEFAULT 'tenant',   -- tenant | platform (MySMS on its own account)
    forbidden_for_minors boolean NOT NULL DEFAULT false,
    majority_grace varchar(20) NOT NULL DEFAULT 'none',  -- none | keep_published | keep_narrow_widths
    default_valid_months integer,                  -- NULL = until withdrawn
    external_code varchar(80),                     -- mapping for consent-manager artefacts (future)
    status varchar(12) NOT NULL DEFAULT 'active',
    version integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_purpose PRIMARY KEY (id),
    CONSTRAINT uq_purpose_code UNIQUE (code),
    CONSTRAINT ck_purpose_scope_kind CHECK (scope_kind IN ('width', 'tenant')),
    CONSTRAINT ck_purpose_fiduciary CHECK (fiduciary IN ('tenant', 'platform')),
    CONSTRAINT ck_purpose_status CHECK (status IN ('active', 'deprecated', 'retired'))
);

-- RoPA-lite, generated from every module's META-INF/mysms/privacy/<module>.json at build time.
CREATE TABLE privacy.processing_activity (
    id uuid NOT NULL,
    module varchar(30) NOT NULL,
    code varchar(60) NOT NULL,                     -- comms.marketing_broadcast, lms.learning_analytics ...
    purpose_id uuid NOT NULL REFERENCES privacy.purpose (id),
    legal_basis varchar(30) NOT NULL,
    education_exemption_clause varchar(60),        -- Fourth Schedule clause relied on, if any
    targets_minors boolean NOT NULL,
    recipients jsonb NOT NULL DEFAULT '[]',        -- [{kind: processor|fiduciary, name, country}]
    cross_border boolean NOT NULL DEFAULT false,
    retention_policy_code varchar(60),
    version integer NOT NULL,
    CONSTRAINT pk_processing_activity PRIMARY KEY (id),
    CONSTRAINT uq_processing_activity_code UNIQUE (code),
    CONSTRAINT ck_processing_activity_basis CHECK (legal_basis IN ('consent', 'legitimate_use_voluntary',
        'legal_obligation', 'education_exemption', 'medical_emergency', 'employment', 'contract')),
    CONSTRAINT ck_processing_activity_exemption CHECK (legal_basis <> 'education_exemption'
        OR education_exemption_clause IS NOT NULL)
);

CREATE TABLE privacy.processing_activity_category (
    activity_id uuid NOT NULL REFERENCES privacy.processing_activity (id),
    category_id uuid NOT NULL REFERENCES privacy.data_category (id),
    CONSTRAINT pk_processing_activity_category PRIMARY KEY (activity_id, category_id)
);
```

A conformance rule (P1, §14) fails the build when an activity with `targets_minors = true` uses a purpose with
`forbidden_for_minors = true`, or when code calls `ConsentChecker` with a purpose code absent from the catalogue.

### 5.2 Tenant settings and notices

```sql
CREATE TABLE privacy.tenant_purpose (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    purpose_id uuid NOT NULL,                      -- logical reference into the global catalogue
    enabled boolean NOT NULL DEFAULT false,        -- off until the school switches it on with a notice
    max_scope varchar(20),                         -- school-level ceiling (e.g. never inter_school)
    notice_id uuid,
    created_at timestamptz NOT NULL DEFAULT now(), created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(), updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_tenant_purpose PRIMARY KEY (id),
    CONSTRAINT uq_tenant_purpose UNIQUE (tenant_id, purpose_id),
    CONSTRAINT uq_tenant_purpose_tenant UNIQUE (id, tenant_id)
);

CREATE TABLE privacy.notice (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    code varchar(60) NOT NULL,                     -- e.g. 'community_photos', 'general_privacy_notice'
    title varchar(200) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(), created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(), updated_by uuid,
    deleted_at timestamptz,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_notice PRIMARY KEY (id),
    CONSTRAINT uq_notice_tenant UNIQUE (id, tenant_id)
);
CREATE UNIQUE INDEX ux_notice_code ON privacy.notice (tenant_id, code) WHERE deleted_at IS NULL;

CREATE TABLE privacy.notice_version (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    notice_id uuid NOT NULL,
    version integer NOT NULL,
    status varchar(10) NOT NULL DEFAULT 'draft',   -- draft | active | retired (published versions are immutable)
    requires_reconsent boolean NOT NULL DEFAULT false,
    purpose_codes varchar(40)[] NOT NULL,
    grievance_contact jsonb NOT NULL,              -- name/role, email, phone of the school's officer
    published_at timestamptz,
    superseded_by_id uuid,
    document_file_id uuid,                         -- optional signed PDF in platform.file_object
    created_at timestamptz NOT NULL DEFAULT now(), created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(), updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_notice_version PRIMARY KEY (id),
    CONSTRAINT uq_notice_version_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uq_notice_version_number UNIQUE (tenant_id, notice_id, version),
    CONSTRAINT fk_notice_version_notice FOREIGN KEY (notice_id, tenant_id) REFERENCES privacy.notice (id, tenant_id),
    CONSTRAINT ck_notice_version_status CHECK (status IN ('draft', 'active', 'retired'))
);
CREATE UNIQUE INDEX ux_notice_version_active ON privacy.notice_version (tenant_id, notice_id) WHERE status = 'active';

CREATE TABLE privacy.notice_translation (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    notice_version_id uuid NOT NULL,
    language varchar(10) NOT NULL,                 -- BCP 47: en, hi, ta, te, kn, mr, bn ...
    body_markdown text NOT NULL,                   -- itemised data, purposes, withdraw and complaint links
    summary_for_children text,                     -- short plain version shown to the minor alongside
    content_sha256 char(64) NOT NULL,              -- hash of what was shown, copied into consent evidence
    created_at timestamptz NOT NULL DEFAULT now(), created_by uuid,
    CONSTRAINT pk_notice_translation PRIMARY KEY (id),
    CONSTRAINT uq_notice_translation UNIQUE (tenant_id, notice_version_id, language),
    CONSTRAINT fk_notice_translation_version FOREIGN KEY (notice_version_id, tenant_id)
        REFERENCES privacy.notice_version (id, tenant_id)
);
```

### 5.3 Subjects and guardian links (projections)

Both tables are copies kept from other modules' events (event-carried state, the house precedent of `sis.student` →
`iam.member_record`); privacy never reads another schema and makes no gRPC call for them. `people.person` events carry
`{id, dateOfBirth, rowVersion}`; `sis.student` events `{id, userId, personId, institutionId, rowVersion}`;
`sis.student_guardian` events `{id, studentPersonId, guardianPersonId, guardianUserId, isLegalGuardian, hasCustody,
status, rowVersion}` (sis resolves the persons and the guardian's account in the publishing transaction). Every copy
changes only to a newer source row version, so out-of-order and repeated deliveries are harmless, and nothing is
hard-deleted: a deleted person keeps its row with the birth date cleared, a deleted link turns `inactive`, a deleted or
moved student record stays as a tombstone. Handlers: `SubjectSync` (people.person, sis.student) and `GuardianLinkSync`
(sis.student_guardian). The age of majority is `mysms.privacy.age-of-majority` (`MYSMS_PRIVACY_AGE_OF_MAJORITY`,
default 18) until it becomes tenant data (§4.3).

```sql
CREATE TABLE privacy.subject (
    tenant_id uuid NOT NULL,
    person_id uuid NOT NULL,                       -- logical reference to people.person
    status varchar(8) NOT NULL DEFAULT 'unknown',  -- minor | adult | unknown (treated as minor; no birth date)
    birth_date date,                               -- copied from people events for the majority only
    majority_date date,                            -- birth_date + age of majority
    student_ids uuid[] NOT NULL DEFAULT '{}',      -- live sis.student records of the person ...
    user_ids uuid[] NOT NULL DEFAULT '{}',         -- ... their login accounts
    institution_ids uuid[] NOT NULL DEFAULT '{}',  -- ... and institutions, all derived from student_records
    student_records jsonb NOT NULL DEFAULT '{}',   -- per sis.student id: {userId, institutionId, rowVersion, live}
    person_row_version integer NOT NULL DEFAULT 0, -- last applied people.person row_version (0 = none yet)
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_subject PRIMARY KEY (tenant_id, person_id),
    CONSTRAINT ck_subject_status CHECK (status IN ('minor', 'adult', 'unknown'))
);
CREATE INDEX ix_subject_majority ON privacy.subject (tenant_id, majority_date) WHERE status <> 'adult';
CREATE INDEX ix_subject_student_records ON privacy.subject USING gin (student_records);

CREATE TABLE privacy.guardian_link (
    tenant_id uuid NOT NULL,
    link_id uuid NOT NULL,                         -- sis.student_guardian.id
    student_person_id uuid NOT NULL,
    guardian_person_id uuid NOT NULL,
    guardian_user_id uuid,
    is_legal_guardian boolean NOT NULL,
    has_custody boolean NOT NULL,
    status varchar(10) NOT NULL,                   -- active | inactive (inactive also once deleted)
    source_row_version integer NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_guardian_link PRIMARY KEY (tenant_id, link_id)
);
CREATE INDEX ix_guardian_link_student ON privacy.guardian_link (tenant_id, student_person_id) WHERE status = 'active';
CREATE INDEX ix_guardian_link_guardian ON privacy.guardian_link (tenant_id, guardian_person_id) WHERE status = 'active';
```

The per-record `student_records` (not in the first draft) is what makes the arrays safe under out-of-order delivery: a
person can hold several student records, and a record can move to another person, so each entry keeps its own row
version. The daily job that turns minors into adults (§4.3) reads `ix_subject_majority`.

### 5.4 Consent records, evidence and state

```sql
CREATE TABLE privacy.consent_record (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    subject_person_id uuid NOT NULL,
    giver_person_id uuid NOT NULL,
    giver_capacity varchar(10) NOT NULL,           -- self | guardian | nominee
    purpose_code varchar(40) NOT NULL,
    scope varchar(20) NOT NULL,                    -- class | institution | inter_school | public | tenant
    decision varchar(8) NOT NULL,                  -- granted | refused
    notice_version_id uuid NOT NULL,
    notice_language varchar(10) NOT NULL,
    valid_from timestamptz NOT NULL,
    valid_until date,
    withdrawn_at timestamptz,
    superseded_by_id uuid,
    channel varchar(20) NOT NULL,                  -- web | mobile_app | paper_form | in_person | email | consent_manager | import
    verification_status varchar(12) NOT NULL,      -- verified | unverified | rejected
    verification_method varchar(30),
    verified_by_user_id uuid,
    verified_at timestamptz,
    recorded_by_user_id uuid,                      -- staff recording a paper form
    evidence_file_id uuid,                         -- signed form (platform.file_object)
    external_artefact_id varchar(120),             -- consent-manager artefact id (future)
    created_at timestamptz NOT NULL DEFAULT now(), created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(), updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_consent_record PRIMARY KEY (id),
    CONSTRAINT uq_consent_record_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_consent_record_notice FOREIGN KEY (notice_version_id, tenant_id)
        REFERENCES privacy.notice_version (id, tenant_id),
    CONSTRAINT ck_consent_record_decision CHECK (decision IN ('granted', 'refused')),
    CONSTRAINT ck_consent_record_capacity CHECK (giver_capacity IN ('self', 'guardian', 'nominee')),
    CONSTRAINT ck_consent_record_self CHECK (giver_capacity <> 'self' OR giver_person_id = subject_person_id),
    CONSTRAINT ck_consent_record_paper CHECK (channel <> 'paper_form'
        OR (evidence_file_id IS NOT NULL AND recorded_by_user_id IS NOT NULL)),
    CONSTRAINT ck_consent_record_verifier CHECK (verified_by_user_id IS NULL
        OR verified_by_user_id IS DISTINCT FROM recorded_by_user_id),   -- four-eyes on paper consents
    CONSTRAINT ck_consent_record_withdrawn CHECK (withdrawn_at IS NULL OR withdrawn_at >= valid_from)
);
-- One live decision per giver, subject, purpose (a newer decision supersedes).
CREATE UNIQUE INDEX ux_consent_record_live ON privacy.consent_record
    (tenant_id, subject_person_id, purpose_code, giver_person_id)
    WHERE withdrawn_at IS NULL AND superseded_by_id IS NULL;
CREATE INDEX ix_consent_record_giver ON privacy.consent_record (tenant_id, giver_person_id);
CREATE INDEX ix_consent_record_expiry ON privacy.consent_record (tenant_id, valid_until)
    WHERE withdrawn_at IS NULL AND superseded_by_id IS NULL AND valid_until IS NOT NULL;

-- Append-only evidence ledger; monthly partitions (volume: every toggle, every verification, every replay).
CREATE TABLE privacy.consent_evidence (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    consent_record_id uuid NOT NULL,
    event varchar(20) NOT NULL,                    -- recorded | verified | rejected | withdrawn | superseded | expired | majority_lapsed
    actor_user_id uuid, actor_person_id uuid,
    on_behalf_of_capacity varchar(10),
    channel varchar(20) NOT NULL,
    notice_content_sha256 char(64),
    ip_address inet,
    user_agent_hash char(64),                      -- hash only: no device fingerprint stored in clear
    impersonator_user_id uuid,                     -- AuthContext.impersonatorId, never hidden
    occurred_at timestamptz NOT NULL,
    previous_hash char(64),                        -- hash chain per tenant: tamper evidence
    entry_hash char(64) NOT NULL,
    CONSTRAINT pk_consent_evidence PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);
-- The app role gets INSERT and SELECT only; no UPDATE/DELETE grant (append-only by privilege, not by convention).

-- Precomputed answer: one row per subject × purpose × scope; the only table read on the hot path.
CREATE TABLE privacy.consent_state (
    tenant_id uuid NOT NULL,
    subject_person_id uuid NOT NULL,
    purpose_code varchar(40) NOT NULL,
    scope varchar(20) NOT NULL,
    result varchar(14) NOT NULL,                   -- allowed | denied
    reason varchar(30) NOT NULL,                   -- CONSENT | REFUSED | NARROWER_GRANT | NOT_ANSWERED | EXPIRED | RECONSENT_REQUIRED | FORBIDDEN_FOR_MINOR | PURPOSE_DISABLED
    state_version bigint NOT NULL,                 -- monotonic per (tenant, subject): consumers drop stale events
    computed_at timestamptz NOT NULL,
    CONSTRAINT pk_consent_state PRIMARY KEY (tenant_id, subject_person_id, purpose_code, scope)
);
CREATE INDEX ix_consent_state_allowed ON privacy.consent_state (tenant_id, purpose_code, scope)
    WHERE result = 'allowed';                      -- ListSubjectsWithConsent
```

### 5.5 Rights requests, nominees, holds

```sql
CREATE TABLE privacy.rights_request (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    number varchar(30) NOT NULL,                   -- module document numbering: PRV-2026-000123
    type varchar(14) NOT NULL,                     -- access | export | correction | erasure | grievance | nomination | withdraw_all
    subject_person_id uuid NOT NULL,
    requester_person_id uuid NOT NULL,
    requester_capacity varchar(10) NOT NULL,       -- self | guardian | nominee
    status varchar(20) NOT NULL DEFAULT 'submitted',
    identity_check varchar(20),                    -- signed_in_session | in_person | document
    details jsonb NOT NULL DEFAULT '{}',           -- typed per request type (validated by DtoValidator)
    assignee_user_id uuid,
    submitted_at timestamptz NOT NULL,
    due_at timestamptz NOT NULL,
    extended_reason text,
    closed_at timestamptz,
    outcome_code varchar(30),
    outcome_file_id uuid,                          -- export package or response letter
    created_at timestamptz NOT NULL DEFAULT now(), created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(), updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_rights_request PRIMARY KEY (id),
    CONSTRAINT uq_rights_request_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uq_rights_request_number UNIQUE (tenant_id, number),
    CONSTRAINT ck_rights_request_status CHECK (status IN ('submitted', 'verified', 'in_progress', 'action_required',
        'extended', 'partially_fulfilled', 'fulfilled', 'rejected', 'closed'))
);
CREATE INDEX ix_rights_request_queue ON privacy.rights_request (tenant_id, status, due_at) WHERE closed_at IS NULL;

CREATE TABLE privacy.rights_request_task (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    request_id uuid NOT NULL,
    module varchar(30) NOT NULL,
    action varchar(12) NOT NULL,                   -- export | erase | anonymise | restrict | correct
    status varchar(16) NOT NULL DEFAULT 'pending',
    attempts integer NOT NULL DEFAULT 0,
    dispatched_at timestamptz, completed_at timestamptz,
    result jsonb,                                  -- {tables: {name: count}, heldItems: n, policyCodes: [...], fileId}
    last_error_code varchar(60),
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_rights_request_task PRIMARY KEY (id),
    CONSTRAINT uq_rights_request_task UNIQUE (tenant_id, request_id, module, action),
    CONSTRAINT fk_rights_request_task_request FOREIGN KEY (request_id, tenant_id)
        REFERENCES privacy.rights_request (id, tenant_id),
    CONSTRAINT ck_rights_request_task_status CHECK (status IN ('pending', 'dispatched', 'done', 'partial',
        'failed', 'not_applicable'))
);

CREATE TABLE privacy.nominee (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    principal_person_id uuid NOT NULL,
    nominee_name_enc bytea NOT NULL,               -- may be outside the school: encrypted, masked for display
    nominee_contact_enc bytea NOT NULL,
    nominee_contact_masked varchar(40) NOT NULL,
    status varchar(10) NOT NULL DEFAULT 'active',
    created_at timestamptz NOT NULL DEFAULT now(), created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(), updated_by uuid,
    deleted_at timestamptz,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_nominee PRIMARY KEY (id)
);
CREATE UNIQUE INDEX ux_nominee_active ON privacy.nominee (tenant_id, principal_person_id)
    WHERE status = 'active' AND deleted_at IS NULL;

CREATE TABLE privacy.legal_hold (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    reason_code varchar(30) NOT NULL,              -- safeguarding | litigation | regulator | audit
    subject_person_id uuid,                        -- NULL = category-wide hold
    category_code varchar(40),
    module varchar(30), entity_type varchar(80), entity_ids uuid[],
    placed_by_user_id uuid NOT NULL,
    placed_at timestamptz NOT NULL,
    review_by date NOT NULL,                       -- holds are reviewed, never forgotten
    released_at timestamptz, released_by_user_id uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_legal_hold PRIMARY KEY (id),
    CONSTRAINT ck_legal_hold_target CHECK (subject_person_id IS NOT NULL OR category_code IS NOT NULL)
);
```

### 5.6 Retention, breach, standard tables

```sql
CREATE TABLE privacy.retention_policy (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    code varchar(60) NOT NULL,                     -- e.g. attendance.after_leaving
    category_code varchar(40) NOT NULL,
    trigger varchar(30) NOT NULL,                  -- after_leaving | after_record_date | after_inactivity | after_purpose_end
    period interval NOT NULL,
    minimum_period interval NOT NULL,              -- legal floor from the catalogue; tenant cannot go below
    action varchar(10) NOT NULL,                   -- delete | anonymise | archive
    notify_before interval,                        -- e.g. 48 hours (Rule 8 classes)
    legal_reference text,
    version integer NOT NULL DEFAULT 1,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_retention_policy PRIMARY KEY (id),
    CONSTRAINT uq_retention_policy UNIQUE (tenant_id, code),
    CONSTRAINT ck_retention_policy_floor CHECK (period >= minimum_period)
);

CREATE TABLE privacy.retention_run (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    policy_code varchar(60) NOT NULL,
    module varchar(30) NOT NULL,
    cutoff timestamptz NOT NULL,
    status varchar(10) NOT NULL,                   -- dispatched | done | partial | failed
    affected integer, held integer,
    started_at timestamptz NOT NULL, completed_at timestamptz,
    CONSTRAINT pk_retention_run PRIMARY KEY (id)
);

CREATE TABLE privacy.breach (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    number varchar(30) NOT NULL,
    status varchar(20) NOT NULL,                   -- reported | triaged | contained | board_initial_sent | board_report_sent | principals_notified | closed | not_a_breach
    detected_at timestamptz NOT NULL,
    became_aware_at timestamptz NOT NULL,          -- clock start (to verify which instant counts)
    board_report_due_at timestamptz NOT NULL,      -- became_aware_at + 72 h
    description text NOT NULL,                     -- ids and counts, never names
    categories varchar(40)[] NOT NULL,
    modules varchar(30)[] NOT NULL,
    estimated_principals integer,
    affected_query jsonb,                          -- how the affected set was derived (reproducible)
    likely_consequences text, mitigation text,
    owner_user_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(), created_by uuid,
    updated_at timestamptz NOT NULL DEFAULT now(), updated_by uuid,
    row_version integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_breach PRIMARY KEY (id),
    CONSTRAINT uq_breach_number UNIQUE (tenant_id, number)
);
CREATE TABLE privacy.breach_notification (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    tenant_id uuid NOT NULL,
    breach_id uuid NOT NULL,
    audience varchar(12) NOT NULL,                 -- board | principals | processor | tenant
    kind varchar(10) NOT NULL,                     -- initial | detailed | update
    sent_at timestamptz, comms_message_id uuid, document_file_id uuid,
    CONSTRAINT pk_breach_notification PRIMARY KEY (id)
);

-- Standard module tables, same shape as people's: outbox_event, inbox_event, idempotency_key, operation.

-- RLS on every tenant table (pattern; repeat per table):
ALTER TABLE privacy.consent_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy.consent_record FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON privacy.consent_record
    USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
```

**Partitioning.** `consent_evidence` by month (core partition maintenance attaches months; retention of evidence =
the longest consent life + limitation period, *to verify*, default keep 8 years). `consent_state` is not partitioned:
rows = subjects × enabled purposes × widths (a 3,000-student school with 8 purposes × ~4 widths ≈ 100 k rows), small.
`rights_request` and `breach` are low volume.

### 5.7 Migrating `people.consent_record`

Pre-release, so the baseline rule applies (CLAUDE.md §7: edit baselines in place):

1. Remove `people.consent_record`, its indexes, FKs, RLS lines and the 7 consent operations from people's baseline,
   contract (`PersonApi.PATH_CREATE_CONSENT_RECORD` and siblings, `api/model/ConsentRecord`, `WithdrawConsentRequest`),
   proto (`people.proto` rpcs `ListConsentRecords` … `GetConsentStatus`) and the iam permission seeds
   (`people.consent_record.*`, `people.person.get_consent_status`; iam baseline lines ~2330–2347). They answer 501
   today (ROADMAP BE-1), so no client depends on working behaviour.
2. Update the verbatim proto copies in the 8 modules that carry `people.proto` (admission, assessment, attendance,
   careers, finance, hr, iam, lms) when people's proto changes.
3. Re-target community-design prerequisites: PR-1 (consent schema) and PR-2 (operations) → privacy §5.4 and §6;
   PR-3 events `people.consent_record.*` → `privacy.consent_record.*` plus `privacy.consent_state.changed`;
   `people.person.majority_reached` → `privacy.subject.majority_reached`; PR-18 erasure workflow → §9; PR-22 purposes
   `community_naming`, `community_participation` → privacy catalogue.
4. If any pilot tenant holds rows, a one-off Java import (`privacy` `ImportLegacyConsents` operation, platform-operator
   only) copies them as `channel = import`, `verification_status = unverified`, `scope = class` for photo consents
   (narrowest, per community §8.4 "legacy and paper consents"), and writes `consent_evidence` rows marked `imported`.

---

## 6. APIs

### 6.1 REST (contract interfaces in `com.mysms.privacy.api`, DTOs in `api.model`)

Permission codes follow the existing `<module>.<resource>.<action>` form (seeded in iam like
`people.consent_record.create`). Self-service operations carry a `my_` resource and act only on the token's `pid` and
the caller's active legal-guardian links (data rule in the service, JAVA-CODING-RULES §16 "Data rules stay in the service").

| Operation id | Method and path | Permission | Who |
|---|---|---|---|
| `privacy.ListMyConsents` | GET `/v1/privacy/my/consents` | `privacy.my_consent.read` | guardian, adult student, staff |
| `privacy.RecordMyConsent` | POST `/v1/privacy/my/consents` | `privacy.my_consent.create` | guardian (for a linked minor), adult self |
| `privacy.WithdrawMyConsent` | POST `/v1/privacy/my/consents/{id}:withdraw` | `privacy.my_consent.withdraw` | the giver |
| `privacy.GetMyNotice` | GET `/v1/privacy/my/notices/{code}?lang=hi` | `privacy.my_consent.read` | everyone signed in |
| `privacy.SubmitMyRequest` | POST `/v1/privacy/my/requests` | `privacy.my_request.create` | principal, guardian, nominee |
| `privacy.ListMyRequests` / `GetMyRequest` | GET `/v1/privacy/my/requests[/{id}]` | `privacy.my_request.read` | requester |
| `privacy.ManageMyNominee` | PUT `/v1/privacy/my/nominee` | `privacy.my_nominee.update` | adult principal |
| `privacy.RecordConsentOnBehalf` | POST `/v1/privacy/consent-records` | `privacy.consent_record.record_on_behalf` | office staff (paper forms) |
| `privacy.VerifyConsentRecord` | POST `/v1/privacy/consent-records/{id}:verify` | `privacy.consent_record.verify` | second staff member |
| `privacy.ListConsentRecords` | GET `/v1/privacy/consent-records` | `privacy.consent_record.read` | DPO, office |
| `privacy.GetConsentCoverage` | GET `/v1/privacy/coverage?purpose=&scope=&classSectionId=` | `privacy.coverage.read` | class teacher, principal: counts only |
| `privacy.*Notice*` | `/v1/privacy/notices`, `/notices/{id}/versions`, `:publish`, `/translations` | `privacy.notice.*` | tenant admin |
| `privacy.*TenantPurpose*` | `/v1/privacy/purposes` (catalogue + tenant switches) | `privacy.tenant_purpose.*` | tenant admin |
| `privacy.*RightsRequest*` | `/v1/privacy/requests`, `:verify-identity`, `:assign`, `:extend`, `:close`, `/tasks/{id}:retry` | `privacy.rights_request.*` | DPO / grievance officer |
| `privacy.*LegalHold*` | `/v1/privacy/legal-holds`, `:release` | `privacy.legal_hold.*` | DPO, safeguarding lead |
| `privacy.*RetentionPolicy*` | `/v1/privacy/retention-policies` | `privacy.retention_policy.*` | tenant admin (within floors) |
| `privacy.*Breach*` | `/v1/privacy/breaches`, `:triage`, `:notify` | `privacy.breach.*` | DPO |
| `privacy.GetProcessingRegister` | GET `/v1/privacy/processing-activities` | `privacy.processing_activity.read` | DPO, auditor (RoPA export) |

Contract interface, MySMS style (mappings on the interface, thin controller implements it):

```java
@Validated
public interface MyConsentApi {

    String PATH_RECORD_MY_CONSENT = "/v1/privacy/my/consents";

    /**
     * POST /v1/privacy/my/consents : Record a consent decision for oneself or a linked minor.
     * Supports Idempotency-Key. A newer decision by the same giver supersedes the live one.
     */
    @RequestMapping(method = RequestMethod.POST, value = PATH_RECORD_MY_CONSENT,
            produces = {"application/json", "application/problem+json"}, consumes = {"application/json"})
    ResponseEntity<MyConsent> recordMyConsent(
            @Valid @RequestBody RecordMyConsentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey);
}
```

```java
/** Body of RecordMyConsent; the giver is always the caller (AuthContext.personId), never a field. */
public record RecordMyConsentRequest(
        @NotNull UUID subjectPersonId,            // self, or a minor the caller is an active legal guardian of
        @NotNull @Size(max = 40) String purposeCode,
        @NotNull ConsentScope scope,
        @NotNull ConsentDecision decision,        // GRANTED | REFUSED
        @NotNull UUID noticeVersionId,            // the version actually shown
        @NotNull @Size(max = 10) String noticeLanguage) {
}

/** One row of the consent centre: own decision and the effective result, never another giver's decision. */
public record MyConsent(UUID id, UUID subjectPersonId, String subjectDisplayName, String purposeCode,
        ConsentScope scope, MyConsentStatus myStatus,   // GRANTED | REFUSED | NOT_ANSWERED | RECONSENT_REQUIRED | EXPIRED
        ConsentScope effectiveWidestScope,               // "currently shared with: his class"
        OffsetDateTime decidedAt, Integer noticeVersion, String channel, long rowVersion) {
}
```

Errors (problem+json codes from the message catalogue): `CONSENT_NOT_GUARDIAN` (404, not 403: no confirmation that the
child exists), `CONSENT_NOTICE_NOT_ACTIVE`, `CONSENT_PURPOSE_DISABLED`, `CONSENT_FORBIDDEN_FOR_MINOR`,
`CONSENT_SCOPE_ABOVE_TENANT_MAX`, `REQUEST_IDENTITY_NOT_VERIFIED`, `RETENTION_BELOW_LEGAL_FLOOR`.

### 6.2 gRPC (`backend/privacy/src/main/resources/proto/mysms/privacy/v1/privacy_decision.proto`)

```proto
syntax = "proto3";
package mysms.privacy.v1;
option java_multiple_files = true;
option java_package = "com.mysms.privacy.grpc.v1";

// Hot-path decisions for other modules. Tenant comes from the caller's service token + tenant metadata.
service ConsentDecisionService {
  rpc CheckConsent(CheckConsentRequest) returns (CheckConsentResponse);
  rpc BatchCheckConsent(BatchCheckConsentRequest) returns (BatchCheckConsentResponse);   // <= 1000 subjects
  rpc GetPurposeStatus(GetPurposeStatusRequest) returns (GetPurposeStatusResponse);      // tenant switch + notice
  rpc ListSubjectsWithConsent(ListSubjectsWithConsentRequest) returns (ListSubjectsWithConsentResponse); // paged
}

message CheckConsentRequest {
  string activity_code = 1;          // e.g. "comms.marketing_broadcast" - resolves purpose and legal basis
  string subject_person_id = 2;
  Scope scope = 3;
}
message CheckConsentResponse { Decision decision = 1; }

message BatchCheckConsentRequest {
  string activity_code = 1;
  repeated string subject_person_ids = 2;
  Scope scope = 3;
}
message BatchCheckConsentResponse { repeated Decision decisions = 1; }   // same order as the request

message Decision {
  string subject_person_id = 1;
  bool allowed = 2;
  Reason reason = 3;
  string legal_basis = 4;            // "consent", "legal_obligation", ...
  int64 state_version = 5;           // for cache coherence with events
}

enum Scope { SCOPE_UNSPECIFIED = 0; SCOPE_TENANT = 1; SCOPE_CLASS = 2; SCOPE_INSTITUTION = 3;
             SCOPE_INTER_SCHOOL = 4; SCOPE_PUBLIC = 5; }
enum Reason { REASON_UNSPECIFIED = 0; CONSENT = 1; NON_CONSENT_BASIS = 2; REFUSED = 3; NARROWER_GRANT = 4;
              NOT_ANSWERED = 5; EXPIRED = 6; RECONSENT_REQUIRED = 7; FORBIDDEN_FOR_MINOR = 8;
              PURPOSE_DISABLED = 9; UNKNOWN_ACTIVITY = 10; }

message ListSubjectsWithConsentRequest {
  string activity_code = 1; Scope scope = 2;
  repeated string institution_ids = 3;   // optional narrowing
  string page_token = 4; int32 page_size = 5;
}
message ListSubjectsWithConsentResponse { repeated string subject_person_ids = 1; string next_page_token = 2; }
```

Callers pass an **activity code**, not a purpose: privacy resolves purpose and legal basis from the RoPA, so a module
cannot pick a convenient basis at the call site, and the register stays true. The resolver is a `@GrpcService` behind
the global exception interceptor (JAVA-CODING-RULES §10.3); reads run in `doInReadTransaction` on the replica pool.

### 6.3 Events (outbox → EventBus)

| Type | When | Payload (no names, ids only) |
|---|---|---|
| `privacy.consent_record.recorded` / `.withdrawn` / `.superseded` / `.expired` / `.verified` | each change | `{id, subjectPersonId, giverPersonId, giverCapacity, purposeCode, scope, decision, validUntil, verificationStatus, withdrawnAt, rowVersion}` |
| `privacy.consent_state.changed` | every recompute that changes a row | `{subjectPersonId, purposeCode, states: [{scope, allowed, reason}], stateVersion}` (event-carried state: enough for a projection such as `comms.media_consent_state`) |
| `privacy.consent_state.snapshot` | replay on demand per tenant (cold start of a consumer) | same, batched, with `snapshotId` and `lastStateVersion` |
| `privacy.subject.majority_reached` | daily job | `{personId, majorityDate, stateVersion}` |
| `privacy.rights_request.task_assigned` | request moves to `in_progress` | `{requestId, taskId, type, action, subjectPersonId, relatedPersonIds, categories, exportFormat, deadline}` |
| `<module>.privacy_task.completed` | each module, after its handler commits | `{taskId, requestId, status, tables: {name: count}, heldItems, policyCodes, fileId}` |
| `privacy.rights_request.fulfilled` / `.closed` | end of saga | `{requestId, type, outcomeCode}` |
| `privacy.erasure.completed` | erasure fulfilled | `{requestId, subjectPersonId}`: tombstone for DR replay (§9.5) |
| `privacy.retention.due` | retention scheduler | `{runId, policyCode, categoryCode, module, cutoff, action}` |
| `privacy.breach.reported` | DPO registers | `{breachId, number, boardReportDueAt}` (to ops alerting) |

---

## 7. `privacy-client`: the consent checker every module uses

### 7.1 Shape and placement

A new flat Maven module **`privacy-client`**, built like `auth-client`: a hand-written facade, the only privacy
surface a domain module touches. It owns the copy of `privacy_decision.proto`, the gRPC client, the cache and the event
subscription that invalidates it, and exposes three types. **This deviates** from CLAUDE.md §3 ("each caller keeps
verbatim copies of the callees' protos and its own `<Callee>GrpcClient`") and Appendix A ("no shared client module").
The justification is the same one that made `auth-client` an exception: the behaviour must be **identical and
fail-closed in every module**; twelve hand-copied cache-and-fallback implementations would drift. If the owner rejects
the exception, the fallback is the standard pattern (each module a `PrivacyGrpcClient` in `integration/`) plus a
`ConsentCache` in `core`; the API below stays the same.

**Explicit calls, no annotations or aspects.** The project has no AOP anywhere, permissions are checked at the edge
and data rules in the service (`DataScope.current(...)`). Consent is a data rule, so it is an explicit call in the
business service, next to `DataScope`. An `@RequiresConsent` aspect was considered and rejected: it hides a remote call,
cannot batch, and cannot see which subjects a list operation will touch.

```java
package com.mysms.privacyclient;

/** Answers whether personal data of subjects may be used for a module's processing activity. Fail-closed. */
public interface ConsentChecker {

    ConsentDecision check(ActivityCode activity, UUID subjectPersonId, ConsentScope scope);

    /** One remote call at most for the cache misses; result keyed by subject. */
    Map<UUID, ConsentDecision> checkAll(ActivityCode activity, Collection<UUID> subjectPersonIds, ConsentScope scope);

    /** Convenience for filters: the subset allowed. */
    default Set<UUID> allowed(ActivityCode activity, Collection<UUID> subjects, ConsentScope scope) {
        return checkAll(activity, subjects, scope).entrySet().stream()
                .filter(e -> e.getValue().allowed()).map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet());
    }
}

/** A module's processing activity as declared in META-INF/mysms/privacy/<module>.json; checked by conformance. */
public record ActivityCode(String value) {
    public ActivityCode {
        if (!value.matches("[a-z]+\\.[a-z_]+")) {
            throw new IllegalArgumentException("activity code must be <module>.<activity>");
        }
    }
}

public record ConsentDecision(boolean allowed, ConsentReason reason, String legalBasis, long stateVersion) {
    public static ConsentDecision unavailable() {      // fail-closed answer when privacy cannot be reached
        return new ConsentDecision(false, ConsentReason.UNAVAILABLE, "consent", -1);
    }
}
```

### 7.2 Implementation

```java
/** Cache-aside decorator over the gRPC stub (Decorator); invalidated by privacy.consent_state.changed. */
final class CachingConsentChecker implements ConsentChecker {

    private final PrivacyDecisionGrpc grpc;              // wraps ConsentDecisionServiceBlockingStub, deadline 300 ms
    private final ActivityCatalogue catalogue;           // basis per activity, loaded from privacy at start, refreshed hourly
    private final DecisionCache cache;                   // bounded, per process: key (tenant, activity, subject, scope)
    private final EnforcementMode mode;                  // off | shadow | enforce (global default, tenant override)
    private final ConsentMetrics metrics;

    @Override
    public Map<UUID, ConsentDecision> checkAll(ActivityCode activity, Collection<UUID> subjects, ConsentScope scope) {
        UUID tenant = AuthContextHolder.current().tenantId().orElseThrow();
        if (catalogue.basis(activity).isNonConsent()) {                  // legitimate use: no remote call
            return metrics.nonConsent(activity, subjects);
        }
        Map<UUID, ConsentDecision> result = new HashMap<>(cache.getAll(tenant, activity, scope, subjects));
        List<UUID> misses = subjects.stream().filter(s -> !result.containsKey(s)).toList();
        for (List<UUID> chunk : chunks(misses, 1000)) {           // the RPC accepts at most 1000 subjects
            try {
                Map<UUID, ConsentDecision> fetched = grpc.batchCheck(activity, chunk, scope);
                cache.putAll(tenant, activity, scope, fetched);
                result.putAll(fetched);
            } catch (IntegrationGrpcClientException e) {
                metrics.unavailable(activity, chunk.size());               // counted, alerted; no subject ids logged
                chunk.forEach(s -> result.put(s, ConsentDecision.unavailable()));
            }
        }
        return mode.apply(tenant, activity, result, metrics);           // shadow: log would-be denials, allow
    }
}
```

- **Cache.** Bounded (default 200 k entries per process), TTL 120 s as a safety net, keys include `tenant_id`; entries
  are dropped by `ConsentCacheInvalidator` (an `EventSubscriptions` bean in privacy-client) on
  `privacy.consent_state.changed` and `privacy.subject.majority_reached`, comparing `stateVersion` so an old event never
  resurrects a stale allow. Negative answers are cached too. Library choice: Caffeine (one new dependency, owner
  approval) or core's planned `CacheAdapter`.
- **Fail-closed.** Timeouts (300 ms deadline), unavailability or an unknown activity answer `allowed = false` for
  consent-based activities. Legitimate-use activities never depend on privacy being up.
- **Monolith vs services.** In `app` the same client calls privacy's in-process gRPC server (ARCHITECTURE §1 "calls are
  remote by contract"), so behaviour is identical; latency is sub-millisecond and the cache is shared by all modules in
  the process, but each module still subscribes through its own inbox.
- **Projection mode** for very hot, per-item reads (the community feed): the consumer keeps its own table fed by
  `privacy.consent_state.changed` and `.snapshot` (community design `comms.media_consent_state`, dirty = unavailable)
  and does not call `ConsentChecker` per read. privacy-client supplies the payload record and a
  `ConsentStateProjector` helper for the version check; the table and its SQL stay in the consumer's schema.
- **Self-access short-circuit.** `ConsentChecker` is not called for a principal reading their own data (or a guardian
  reading their child's data under `DataScope`): that is access, not a new purpose.

### 7.3 Usage examples

```java
// comms: a marketing broadcast to guardians filters recipients in one call.
public SendResult sendBroadcast(UUID broadcastId) {
    return doInWriteTransaction(() -> {
        Broadcast b = broadcasts.load(DataScope.current(PERMISSION_SEND), broadcastId);
        List<Recipient> recipients = audience.resolve(b);                       // person ids of guardians/students
        Set<UUID> allowed = consent.allowed(MARKETING_BROADCAST, Recipient.personIds(recipients), ConsentScope.TENANT);
        List<Recipient> sendable = recipients.stream().filter(r -> allowed.contains(r.personId())).toList();
        deliveries.queue(b, sendable);
        audit.record("comms.broadcast", broadcastId, Map.of("excludedForConsent", recipients.size() - sendable.size()));
        return new SendResult(sendable.size(), recipients.size() - sendable.size());
    });
}
// Operational notices (fee due, school closed, attendance alert) use activity comms.operational_notice:
// legal basis legitimate_use, no remote call, never filtered.
```

```java
// lms: learning analytics on a cohort; minors are excluded by the catalogue (behavioural monitoring is forbidden
// unless the activity is declared under the education exemption, which the RoPA records and counsel signs off).
Map<UUID, ConsentDecision> d = consent.checkAll(LEARNING_ANALYTICS, cohort.learnerPersonIds(), ConsentScope.TENANT);
analytics.computeFor(cohort.learners().stream().filter(l -> d.get(l.personId()).allowed()).toList());
```

```java
// comms community: at approval of a post, the subjects of each image are checked at the post's width and the
// projection rows are written in the same transaction (community design §8.4); reads use the projection.
Map<UUID, ConsentDecision> d = consent.checkAll(COMMUNITY_PHOTO_PUBLICATION, media.subjectPersonIds(), post.width());
mediaConsentState.write(media.id(), post.width(), d.values().stream().allMatch(ConsentDecision::allowed));
```

---

## 8. Enforcement points across modules

| Module | Processing | Activity code (purpose) | Basis | Where the check runs |
|---|---|---|---|---|
| comms | Community photos and videos of students | `comms.community_photo_publication` (`photo_video_publication`, width) | consent | at approval + projection on read (§7.2) |
| comms | Naming/crediting students in posts | `comms.community_naming` (`community_naming`) | consent | at approval; projection |
| comms | Student participation in the community | `comms.community_participation` | consent (guardian) | at membership grant |
| comms | Promotional or third-party messages | `comms.marketing_broadcast` (`marketing_comms`) | consent | recipient resolution, batch |
| comms | WhatsApp/SMS channel for non-essential messages | `comms.whatsapp_optional` (`sms_whatsapp_messages`) | consent | channel selection per recipient |
| comms | Operational notices | `comms.operational_notice` | legitimate use | none (catalogue short-circuit) |
| lms | Learning analytics, engagement scores, recommendations | `lms.learning_analytics` | consent (adults) / education exemption (minors, *to verify*) | before computing; dashboards filter |
| lms / assessment | AI features (tutoring, auto-grading with external model) | `lms.ai_assist_external` (`third_party_ai_processing`) | consent; vendor named as processor | before each call to the provider, batch per class |
| lms | Proctoring with webcam | `lms.proctoring_video` (`biometric_or_video_monitoring`) | consent, adults only; forbidden for minors unless counsel approves | at exam start |
| attendance | Biometric or face attendance | `attendance.biometric_capture` (`biometric_data`) | consent | device enrolment; fallback manual |
| transport | Live bus location to parents | `transport.live_location_share` (`transport_live_location`) | legitimate use (safety, Fourth Schedule *to verify*) | none, recorded in RoPA |
| sis / people | Public profile, alumni directory | `people.public_profile` (`public_profile`) | consent | on publish of profile |
| sis | Health and medical notes | `sis.health_record` (`health_record_keeping`) | legal obligation / medical emergency (*to verify*) | access restricted by permission; erasure needs second reviewer |
| finance | Payment gateway sharing | `finance.payment_gateway` (`third_party_payment`) | contract (payer is an adult) | none; recipient recorded |
| careers | Sharing CVs with employers | `careers.employer_sharing` (`third_party_sharing`) | consent (adult student) | per application batch |
| admission | Applicant data before enrolment | `admission.application` | legitimate use voluntary | none; retention policy `applicant.not_admitted` |
| frontoffice | Visitor photo and ID | `frontoffice.visitor_log` | legitimate use (safety) | none; short retention |
| platform | MySMS product analytics across tenants | `platform.product_analytics` (fiduciary = platform) | consent, adults only, off for minors | client SDK init |

Every row is one entry in the module's `META-INF/mysms/privacy/<module>.json`:

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

### 9.1 The per-module handler (the `hardDelete(subjectId)` idea, generalised)

Each module that holds personal data implements one `EventSubscriptions` bean in its `events/` package, subscribed to
`privacy.rights_request.task_assigned` for its own module name. It reads its manifest, runs one write transaction per
table batch, and publishes `<module>.privacy_task.completed` through its outbox in the same transaction as the last
batch. Idempotency: the inbox already de-duplicates by event id; the handler also writes a receipt row
(`<module>.privacy_task_receipt(task_id PK, status, result)`) so a re-dispatched task returns the stored result.

```java
/** Erases, anonymises, restricts or exports one subject's data held by this module (Template Method per action). */
@Component
public class SisPrivacyTasks implements EventSubscriptions {

    @Override public String module() { return "sis"; }

    @Override public Map<String, EventHandler> handlers() {
        return Map.of("privacy.rights_request.task_assigned", this::handle);
    }

    int handle(EventEnvelope event) {
        PrivacyTask task = PrivacyTask.from(event);                    // record from privacy-client
        if (!task.module().equals("sis")) {
            return 0;
        }
        return receipts.once(task.taskId(), () -> switch (task.action()) {
            case ERASE, ANONYMISE -> eraser.run(task, holds.activeFor(task.subjectPersonId()));
            case EXPORT -> exporter.run(task);                          // writes a JSON part to platform files
            case RESTRICT -> restrictor.run(task);                      // flags rows: processing paused
            case CORRECT -> TaskResult.notApplicable();                 // corrections go through normal update operations
        });
    }
}
```

Rules every handler follows: rows under a legal hold or a legal-retention policy are **kept and counted** (`heldItems`,
`policyCodes`), never silently skipped; anonymisation overwrites identifying columns with deterministic placeholders
(`"Former student"`, `NULL`, masked) and clears `_enc` columns; files are deleted through platform's file operation
(which purges object storage); cross-module copies (person cards on `sis.student`, `iam.user_account`, ROADMAP C-5)
are cleared by each holder in its own handler.

### 9.2 Erasure saga (orchestrated by privacy, choreographed by events)

```
DPO verifies identity ─▶ privacy: request in_progress; one task per module in the manifest union
  (same tx) outbox: privacy.rights_request.task_assigned × N (with deadline)
modules: handle → commit → <module>.privacy_task.completed
privacy inbox: task status, result; when all tasks terminal:
   all done/not_applicable      → fulfilled; consent records withdrawn; subject projection anonymised
   any partial (holds/policies) → partially_fulfilled with the reasons shown to the requester
   any failed after 5 attempts  → action_required (DPO dashboard), retry button re-dispatches the same task id
SLA sweep every 15 min: tasks dispatched > 24 h without completion are re-dispatched; overdue requests alert.
```

Order matters for two modules only: `comms` (stop sending, hide media) and `iam` (disable sign-in of the erased account)
run first; privacy dispatches them in wave 1 and the rest in wave 2, so no notification goes out to an account being
erased. Evidence: `rights_request_task.result`, the request's outcome letter (file), and `platform.audit_log` entries
written by each module's `AuditWriter` carrying the task id.

### 9.3 Access and export

The legal minimum (s.11) is a **summary**: categories held, processing activities and their purposes, recipients —
generated by privacy from the RoPA plus per-module counts (export tasks run with `summaryOnly = true`). The full
**export package** (a nicety, and ROADMAP's `platform.ExportMyData`, which this replaces) asks each module for a JSON
part written to `platform.file_object` (bucket with 7-day expiry), then privacy assembles a ZIP with a manifest and
an index in the requester's language, and delivers it through a short-lived signed link after re-authentication. A
guardian's export of a child excludes safeguarding cases and other people's data (counts only, community §12.7).

### 9.4 Correction, grievance, nomination

- **Correction**: the request names fields; privacy routes it to the owning module's existing update operation as a
  task for staff (the request page deep-links to the record, ARCHITECTURE §4). privacy never writes another module's
  data. The task closes when the module's `*.updated` event for that record arrives or the staff member records a refusal
  with a reason.
- **Grievance**: free text + category; assigned to the tenant's grievance officer; SLA clock; the response is a letter
  file and a comms message; escalation path to the Board is shown in the closing message.
- **Nomination**: an adult principal names one nominee (encrypted contact). A nominee submits requests with
  `requester_capacity = nominee` and proof (death certificate or incapacity document), always verified in person or by
  document by the DPO.

### 9.5 Backups and disaster recovery

Erased data stays in database backups until they rotate (state the window in the notice). privacy keeps an erasure
tombstone ledger (ids only, `privacy.erasure.completed`); the DR runbook replays tombstones after any restore, the same
approach as the community design's off-lineage safety log (PR-21).

---

## 10. Retention and deletion engine

- **Policies** per data category and trigger, seeded from the catalogue with legal floors; tenants may lengthen, or
  shorten down to the floor (CHECK in §5.6). Examples: `applicant.not_admitted` 1 year after decision;
  `attendance.after_leaving` 5 years; `visitor_log` 90 days; `community_media.after_withdrawal` 30 days (*all values to
  verify with counsel and the owner*).
- **Scheduler** (daily per tenant, ARC-4 job pattern): computes the cutoff per policy, writes `retention_run`, publishes
  `privacy.retention.due` to the owning module. The module's sweep (same handler class as §9.1, action `RETENTION`)
  deletes or anonymises rows past the cutoff in bounded batches, skipping rows under holds, and replies with counts.
- **Pre-erasure notice**: for policies with `notify_before` (inactivity classes in Rule 8, *to verify*) privacy sends the
  notice through comms and runs the sweep only after the interval.
- **Soft delete**: `deleted_at` is not erasure. Retention also purges rows soft-deleted longer than the policy's grace
  period (default 30 days), so "deleted" eventually means gone.
- **Partitions**: for partitioned logs (audit, xAPI statements, message logs) retention is the configured partition drop
  of core's partition maintenance (JAVA-CODING-RULES §1 rule 2); privacy's policy is the single source of the number,
  exposed to each module's configuration (`mysms.<module>.partition.retention`).
- **Legal holds** override everything; a hold has a mandatory review date and the DPO dashboard lists holds due for review.

---

## 11. Breach management

```
reported ─▶ triaged ─▶ contained ─▶ board_initial_sent ─▶ board_report_sent (≤ 72 h after became_aware_at)
                │                         └──▶ principals_notified (affected set from affected_query) ─▶ closed
                └─▶ not_a_breach (reason, kept)
```

The register stores ids and counts only. "Affected principals" is computed by a reproducible query run in the affected
modules through a privacy task (`action = IDENTIFY`), returning person ids to privacy, which sends notices through comms
in the principal's language with the content the Rules require (*to verify*). Clocks show on the DPO dashboard and page
the on-call operator when 24 h remain. When MySMS (as processor) detects a breach it opens the record for every
affected tenant and notifies the tenant first; the tenant (fiduciary) owns the Board submission.

---

## 12. User interface

**Parents (web, mobile — React / React Native):**

- *Privacy dashboard* ("Privacy and consent" in the profile menu): per child, per purpose, toggles with the width
  selector where relevant; own decision and the effective result without explaining other guardians (community §12.7
  disclosure rule); badge for `RECONSENT_REQUIRED`; "What is affected" counts before withdrawing.
- *Notice viewer*: language switcher (all languages the tenant published), short "for your child" summary, link to the
  full notice and the grievance contact. Consent buttons are equal in weight ("Allow" / "Don't allow"); no pre-selection.
- *Requests*: submit access, export, correction, erasure, grievance; timeline with status and due date; download of the
  export after re-authentication.
- *Onboarding*: at first sign-in, consent tasks appear as a checklist, never as a gate to essential services (fees,
  attendance, notices).

**Students:** minors see a read-only, plain-language view of what their guardians decided and how to ask questions
(age-appropriate copy, no toggles). From the majority date they see "You can now decide" with all toggles; adult students
in higher education see only themselves.

**Administrators:** purposes (switch on with a notice), notice editor with versioning, translations and preview,
coverage report (counts per class, never names to teachers outside their class), paper-consent entry and four-eyes
verification queue, requests queue with SLA colours and per-module task status, legal holds, retention policies, breach
register, RoPA export (CSV/PDF) for auditors.

**UX notes:** large targets and plain words (Grade 6 reading level in English, reviewed translations for Hindi and the
tenant's regional languages); right-to-left not needed for current languages; every toggle change shows the effect
sentence; withdrawals are as quick as grants (one tap + confirm); offline mobile queues consent changes with the notice
version actually displayed.

---

## 13. Security, multi-tenancy, audit, performance, observability

- **Tenancy**: RLS on every tenant table; services bind `app.tenant_id` through `AppTransactionManager`; every query adds
  `tenant_id = :tenantId`. Global catalogue tables are read-only for the app role.
- **Owner-only data**: self-service operations use `AuthContext.personId` and active legal-guardian links only; a guardian
  never learns another guardian's decision; unknown subject → 404.
- **Impersonation**: consent changes while impersonating are refused (`CONSENT_IMPERSONATION_FORBIDDEN`); support staff
  can view but not decide. Recorded in evidence when viewing.
- **Evidence integrity**: append-only by privilege; per-tenant hash chain verified nightly; mismatch alerts.
- **No PII in logs/metrics/traces** (JAVA-CODING-RULES §1 rule 5): metrics carry activity codes and counts only.
- **Encryption**: nominee contact `_enc` via core `FieldCipher`; export packages encrypted at rest, links expire.
- **Performance**: hot path is `consent_state` primary-key lookups (batch = one `= ANY(:ids)` query); in-process cache
  hit ratio target > 95 %; recompute of a subject's state touches ≤ purposes × widths rows.
- **Observability**: `privacy_decisions_total{activity,result,reason}`, `privacy_client_unavailable_total{module}`,
  `privacy_client_cache_hit_ratio`, `privacy_request_overdue{type}`, `privacy_task_age_seconds{module}`,
  `privacy_breach_clock_remaining_seconds`, outbox lag of `privacy.consent_state.changed` (the withdrawal-exposure bound in
  community §8.4 depends on it). Health indicator: decision service reachable.

---

## 14. Testing strategy

| Layer (JAVA-CODING-RULES §12) | What |
|---|---|
| Unit | `EffectiveConsentRule` truth table (adult/minor/unknown × guardians granted/refused/narrower/silent × expiry × reconsent × forbidden-for-minor), property-based over random record sets; SLA clock maths; retention cutoff maths |
| Component (Testcontainers PostgreSQL, real `AuthFilter` with `TestTokens`) | Every REST operation 401/403/404; `ConsentCentreDisclosureComponentTest` (two guardians); `MajorityTransitionComponentTest`; paper consent four-eyes; notice publish immutability; evidence append-only (UPDATE fails with permission denied) |
| Cross-tenant negatives | Tenant A's token cannot read, count or infer tenant B's consents, requests, coverage, or projections; RLS with a deliberately missing `tenant_id` predicate still returns nothing |
| Contract | `privacy_decision.proto` compatibility check in the build (no field renumbering); events payload schemas checked by a consumer test in comms |
| End-to-end in `app` | `ErasureSagaComponentTest`: one subject with data in all modules, erasure → every module's receipt, held items reported, re-dispatch idempotent; fail-closed test: privacy stopped → comms marketing sends nothing, operational notices still go |
| Conformance (new rule P1) | Every `ActivityCode` constant exists in a manifest; no consent-basis activity targets minors with a forbidden purpose; every module with personal-data tables declares handlers |

---

## 15. Rollout, effort, future-proofing

### 15.1 Phases (one experienced developer + agents; person-weeks)

| Phase | Scope | Effort | Exit |
|---|---|---|---|
| P0 Foundations | Module skeleton, baseline schema, catalogue + manifests for comms/lms/sis, notices, consent records + evidence + state, subject/guardian projections, remove people's consent parts (§5.7) | 3–4 | Component tests green; COM-10 API usable |
| P1 Enforcement | `privacy-client` (cache, fail-closed, shadow mode), decision gRPC, events; enforcement in comms (marketing, community photos/naming), lms analytics, attendance biometrics; parent consent centre UI | 4–5 | Shadow mode in pilot tenant two weeks with zero unexpected denials, then enforce |
| P2 Rights | Requests (access summary, correction, grievance, nomination) with SLAs, admin queue UI, export package | 3–4 | Pilot DPO handles a mock request end to end |
| P3 Erasure and retention | Per-module handlers in all modules holding personal data, saga, holds, retention engine, DR tombstones | 6–8 | `ErasureSagaComponentTest` across all modules |
| P4 Breach and reports | Breach register and clocks, RoPA export, coverage reports, consent-manager inbound placeholder | 2–3 | Tabletop breach exercise |

Total ≈ 18–24 person-weeks; P0–P3 should finish by March 2027 for the 13 May 2027 obligations.

**Feature flags**: `mysms.privacy.enforcement` (`off` → `shadow` → `enforce`, global default, tenant override set only by
platform operators); per-tenant purposes start disabled; community photos stay blocked until enforcement is `enforce`
(fail closed, matching the community design).

### 15.2 Migration

Pre-release baseline edits (§5.7); seed system notices in English and Hindi as templates the tenant must review before
enabling a purpose; existing tenants get all consent purposes disabled until they publish notices.

### 15.3 Versioning and compatibility

Purposes and activities are versioned; a material change bumps the version and, if the notice says so, sets
`requires_reconsent`. Proto evolves additively (`v1` package; a breaking change is `v2` served side by side). Event
payloads are additive; consumers ignore unknown fields.

### 15.4 Future-proofing

- **Consent managers** (Rule 4): an inbound adapter (`privacy/integration/ConsentManagerAdapter`) maps external
  artefacts to `consent_record` with `channel = consent_manager`, `external_artefact_id`, purposes via `external_code`;
  outbound notifications of withdrawals. Wait for the Board's technical standards (*to verify*).
- **DigiLocker-based verification** for parental consent: a `VerificationMethod` strategy slot.
- **Policy engine**: `EffectiveConsentRule` is a pure function behind an interface; OPA or Cedar could replace it if
  rules become tenant-programmable. Not needed now.
- **Other jurisdictions**: age of majority, purposes and floors are data (tenant country), not code.

---

## 16. Open questions and risks

**Owner decisions**

**Approved:** a separate `privacy` module (kept separate under Plan A); `privacy-client` as a shared facade module,
an exception like `auth-client` (recorded in CLAUDE.md §2); Caffeine as the cache dependency; retiring
`people.consent_record` and people's consent operations into privacy (§5.7).

**Still open**

1. Default SLA for rights requests (30 days proposed) and grievances.
2. Majority transition defaults (community open decision 9).
3. Is MySMS ever a fiduciary (product analytics, marketing of its own services)? If yes, a platform-level notice and
   purposes with `fiduciary = platform`.
4. Retention values per category (counsel input needed for every row of the catalogue).

**To verify with counsel**: exact Rule 3 notice contents; Rule 10 verification methods acceptable for schools; scope of
the Fourth Schedule education exemption (analytics, transport GPS, CCTV); which retention laws bind school records;
grievance period; breach notice contents and clock start; whether schools fall in any Rule 8 erasure class; POCSO
interaction with access requests.

**Risks**

| Risk | Mitigation |
|---|---|
| Fail-closed blocks legitimate operations if privacy is down | Legitimate-use activities never call privacy; consent activities degrade only non-essential features; health alert; shadow mode before enforce |
| Low guardian response → photos rarely publishable | Coverage reports, reminders through notices, paper-form flow with four-eyes |
| Erasure misses copies (person cards, exports, caches, backups) | Manifests + conformance; C-5 copies handled per module; tombstone replay after restore |
| Over-collection of evidence becomes its own privacy risk | Hash user agent, no device fingerprint, evidence retention policy |
| Scope creep into a generic policy engine | Non-goal; rule is one pure function |
| Rules change before May 2027 | Purposes, floors and SLAs are data; legal text lives in notices |
