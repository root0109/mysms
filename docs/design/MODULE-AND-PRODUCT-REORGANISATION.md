# Module and product reorganisation — backend and clients

The decided target shape of MySMS: a platform kernel and products (school management, HRMS, learning), on the backend
and on every client (web, phone, tablet, console), with products and feature areas switched per tenant. **Backend
module structure built** (22 → 10 modules of areas, 2026-11-25); what is built and what remains: §11. Still open:
product vocabulary (§9) and confirming the packaging (§10).

## 1. Where things stand

| Layer | Today | Problem for a product split |
|---|---|---|
| Backend | 22 domain modules, one schema each, 26 gRPC edges; staff data spread over `hr` (staff, designations, departments, subject competencies, leave), `payroll` (pay, payslips, bank accounts), `attendance` (`staff_attendance` beside students' registers) | HR functions cannot ship alone: `hr.staff_subject` is school-specific, staff attendance lives in a school module, and eight modules reference `hr` staff ids (payroll and seven school modules: academics, sis, attendance, timetable, assessment, library, frontoffice). Correction found while building: none of them calls `hr` over gRPC — payroll, comms, finance and iam keep event-fed copies, the others hold logical references only (ARCHITECTURE §5.1) |
| Organisation model | `org.institution.institution_type` ∈ school, college, university, coaching centre, training centre | No company / branch / office type for an HRMS-only tenant |
| Web (`apps/web`) | One app for every tenant role; screens in `packages/web-kit/src/screens/<area>` (staff, leave, payroll, students, lms …), list managers in `resourceScreens.ts`, routes generated from the single inventory `ui/02` | Every product is compiled into one app; no product boundary in packages; navigation knows no subscribed product |
| Phone (`apps/mobile`) | One Expo project, three store variants (`APP_VARIANT` = student, parent, teacher); screens flat in `packages/native-kit/src` (payslips, leave, timetable, fees, LMS …) | No employee app; teacher and employee self-service are mixed; native-kit has no domain structure |
| Tablet (`apps/tablet`) | Offline-first LMS client (offline engine not built, UI-2) | Fine as the LMS product's client |
| Console (`apps/platform-console`) | Platform staff | Needs plan/product entitlements per tenant |
| Shared packages | `core` (auth, API wrapper, hooks per module in `core/src/hooks`), `sdk` (generated per module), `fixtures`, `i18n`, `tokens`, `ui-web`, `ui-native` | Hooks of every product in one package; one i18n catalogue; product terminology fixed to school words |

## 2. Target: a platform kernel and products

```
                       ┌──────────────── products (sold separately, switched per tenant) ────────────────┐
                       │  School management     HRMS        Learning (LMS)     Community (later)          │
                       │  students, academics,  employees,  courses, cohorts,  school network            │
                       │  finance, campus       payroll,    quizzes            (design for approval)       │
                       │                        time, leave                                               │
                       └──────────────────────────────┬──────────────────────────────────────────────────┘
                                                      │ depend only on ↓ (never on each other, except
                                                      │ school → HRMS by id and events)
        platform kernel: platform (tenancy, files, settings, reference data, org) · identity (iam + people)
                         · privacy (consent, rights, retention) · comms (delivery, notices, inbox)
```

Rules:

1. **Kernel modules** serve every product and know none of them.
2. **A product depends only on the kernel.** The one allowed cross-product dependency is **school → HRMS**: a
   teacher is an employee; school modules hold a staff id and an event-fed copy of what they show (the pattern
   already used by `attendance.member_record` and `academics.staff_record`), and may call HRMS over gRPC in that
   direction only. HRMS never knows a student, class or subject.
3. **Each product owns its contract prefixes** (`/v1/hr`, `/v1/payroll` stay, so the SDK and apps keep working) and
   ships as its own feature package on the clients.
4. **Products and feature areas are tenant data**: a tenant subscribes to products (platform plan entitlements,
   GK-20) and switches feature areas inside them (§3.1). A switched-off area answers 404 and disappears from every
   client's navigation.

## 3. Backend modules (22 → 10)

| Target module | Merges today's | Product | Notes |
|---|---|---|---|
| platform | platform, master, org | kernel | `org` gains institution types `company`, `branch`, `office` for HRMS-only tenants |
| identity | iam, people | kernel | role templates per product (HR admin, payroll officer, manager, employee) |
| privacy | privacy | kernel | unchanged; erasure handlers move with their tables |
| comms | comms | kernel | delivery (ARC-2), notices and inbox serve every product; community stays a feature area of school |
| **hrms** | hr, payroll, staff part of attendance | **HRMS** | adds `staff_attendance` (moved from attendance), keeps leave, designations, departments (on `org.department`), bank accounts, payslips; future appraisal and CPD (NM-12), recruitment of staff, onboarding and exit, staff documents, employee self-service. `hr.staff_subject` (teaching competencies) moves to academics as the teacher profile |
| students | sis, admission, careers | school | unchanged |
| academics | academics, timetable, student attendance, assessment | school | keeps student registers only; reads teachers through its `staff_record` projection |
| learning | lms | LMS | unchanged; sellable alone on the kernel |
| finance | finance | school (fees) | payroll disbursement stays in hrms; a shared ledger is a later decision (GK-5) |
| campus | library, transport, frontoffice, facility | school | unchanged |

**HRMS boundary in detail**

| Today | Goes to | Notes |
|---|---|---|
| `hr.staff`, `designation`, `staff_department`, `leave_type`, `leave_application`, `leave_balance` | hrms | `staff` gains employment type, grade, manager, probation and contract dates (C-7 / P2-04) |
| `hr.staff_subject` | academics (`teacher_subject`) | school-only concept — **done 2026-11-26**: `/v1/academics/teacher-subjects`, `academics.teacher_subject.*`; deleting a staff record archives its rows |
| `payroll.*` | hrms | the `payroll.staff_member` copy of hr staff disappears (same module) — **done 2026-11-26**: payroll reads `hr.staff` |
| `attendance.staff_attendance` (partitioned) | hrms (`time_attendance`) | **done 2026-11-27**: the 23rd area — own schema (baseline V1_095, in-module FK to `hr.leave_application`), `/v1/time-attendance`, `time_attendance.*` codes, `mysms.time_attendance.v1`, `com.mysms.hrms.timeattendance`; biometric/device capture (HW drafts) lands here; holidays still from `org` |
| school callers of hr over gRPC (8 modules) | event-fed `staff_record` projections where a module only shows names; gRPC school → hrms where it needs a live answer | removes the "school knows HR internals" coupling |
| leave ignoring holidays, payroll ignoring leave (BE-6) | fixed inside hrms | becomes one module's rule, no cross-module call |

### 3.1 Feature areas a tenant switches on

| Product | Feature area | What the tenant gets | Modules |
|---|---|---|---|
| School | Administration | School setup, years and terms, users and roles, reference data, files | platform, identity |
| School | Admissions and students | Enquiry, application, enrolment, records, promotion, alumni, careers | students |
| School | Academics | Classes, subjects, timetable, attendance, exams, marks, report cards | academics |
| School | Fees and finance | Fee plans, invoices, online payment, receipts, ledgers | finance |
| School | Campus services | Library, transport, front office and visitors, facilities | campus |
| School | Communication and community | Notices, messages, events, class pages, moderated community | comms |
| HRMS | Staff and payroll (school) / HRMS (company) | §8 | hrms |
| Learning | Learning | Courses, content, assignments, quizzes, grading | learning |

Privacy and consent is never switched off (legal obligation).

**Standalone products**: one assembly per product beside `app`, built from the same modules (no fork):
`app-hrms` = platform + identity + privacy + comms + hrms; `app-learning` = platform + identity + privacy + comms +
learning (LMS sold on its own, for coaching and training centres and companies). Their tenants have only that product
entitled; HRMS-only tenants use institution types company/branch/office. Product vocabulary: §9.

## 4. Clients (web, phone, tablet, console)

### 4.1 Packages: by product feature, not by layer

```
clients/packages/
  kernel/            today's core: auth, session, API client, offline engine, routing engine, permissions, deep links
  sdk/               generated per backend contract (unchanged names, @mysms/sdk/<module>)
  ui-web/ ui-native/ tokens/ i18n/      design system and message infrastructure (per-product catalogues below)
  features/
    platform/        tenant settings, users and roles, files, branding (web + native parts)
    privacy/         consent centre, requests, DPO and governance screens
    comms/           notices, inbox, calendar, conversations
    hrms/            employees, departments, leave, time attendance, salary, payslips, self-service
    students/  academics/  finance/  campus/  learning/  community/
```

Each feature package holds, for its product area: hooks (moved out of `core/src/hooks`), web screens (out of
`web-kit/src/screens/<area>`), native screens (out of the flat `native-kit/src`), fixtures for tests, its i18n
namespace (`hrms.*`, terminology per product), its slice of the screen inventory and permission codes, and its
navigation entries with the feature-area switch they depend on. ESLint boundaries: a feature imports the kernel, the
SDK and the design system, never another feature (cross-product views go through the kernel's composition slots).

### 4.2 Apps are compositions of features

| App | Today | Target |
|---|---|---|
| School web (W) | `apps/web` | `apps/web` composes kernel features + students, academics, finance, campus, learning, hrms (school staff), community |
| HRMS web (new) | — | `apps/web-hrms`: kernel features + hrms, own branding and domain; the same feature package as in the school web |
| Phone: student, parent | `apps/mobile` variants | unchanged variants; features students, academics, finance, learning, comms, privacy |
| Phone: teacher | `apps/mobile` variant | teacher = school features (attendance, homework, marks) + hrms self-service (leave, payslips, check-in) |
| Phone: employee (new) | — | new variant `APP_VARIANT=employee` for HRMS-only tenants: hrms self-service + comms + privacy; offline check-in queue |
| Tablet (B) | `apps/tablet` | the learning product's offline client (UI-2) |
| Console (C) | `apps/platform-console` | adds product entitlements per tenant and the HRMS tenant onboarding |

Navigation everywhere is built from (entitled products × switched-on feature areas × permissions); the route table is
generated per app from the inventory slices of the features it composes.

### 4.3 The inventory and docs

`ui/02-navigation-and-screens.md` splits into one inventory per product (`ui/screens/<product>.md`, the same row
format and ids; HR-* ids become HRMS-*), and `packages/kernel/scripts/generate.mjs` reads the slices of the features
an app composes. `ui/01` gains the HRMS personas (HR admin, payroll officer, manager, employee) and the product matrix.

## 5. Order of work (after the privacy work)

| Step | What | Gate |
|---|---|---|
| 0 | Owner decisions (§7); amend CLAUDE.md, JAVA-CODING-RULES, conformance (`ModuleBoundaryRule`, `MavenLayoutRule`) for multi-schema modules and the kernel/product rule | rules merged, build green |
| 1 | Product entitlements and feature-area switches (platform data, edge 404, navigation filter) | component tests + client tests |
| 2 | **hrms first**: merge hr + payroll; move `staff_attendance` from attendance; move `staff_subject` to academics; replace the seven school → hr calls by projections or one-way gRPC | full build; every former hr/payroll test green; no school module imported by hrms (conformance) |
| 3 | Clients: create `packages/kernel` and `packages/features/*`; move hrms, privacy, platform features first; ESLint boundaries | `pnpm verify` green after each feature move |
| 4 | `apps/web-hrms`, employee phone variant, console entitlements, `app-hrms` assembly, `company` institution types | an HRMS-only tenant signs in, runs a payroll, applies for leave on the phone |
| 5 | Remaining backend merges, leaves first (careers → students; transport, frontoffice, library → campus; platform + master + org; iam + people; sis + admission; academics group — break the academics ↔ students cycle first) | full build per merge |
| 6 | Remaining client feature moves; split `ui/02` per product | `pnpm verify` green |

Each step is one change set, green before the next; diagrams in `docs/diagrams/architecture` change in the same
commit as the code.

## 6. Effects and risks

| Effect | Note |
|---|---|
| gRPC edges | today's 26 → 16 edges; HRMS removes the payroll → hr edge and turns 7 school → hr edges into projections |
| Schemas and data | Schemas stay; no data moves except `staff_attendance` and `staff_subject` (pre-release baselines edited in place, CLAUDE.md §7) |
| Contracts | URL prefixes and operation ids stay; the SDK is unchanged; HR-* screen ids renamed in the inventory only |
| Privacy | Erasure handlers and retention policies (`payroll.after_leaving`, `attendance.after_leaving`) follow their tables |
| Risk: HRMS needs features schools do not | Statutory payroll (PF, ESI, PT, TDS), attendance devices, multi-entity companies — scope the HRMS v1 with the owner before step 4 |
| Risk: two web apps diverge | Same feature packages; app shells differ only in composition and branding |

## 7. Owner decisions

1. Kernel + products, 10 backend modules, hrms as its own product (§2–§3).
2. HRMS sold standalone; v1 scope §8.1, v2 later (§8.2).
3. Learning may be sold standalone (`app-learning`); learning never depends on school modules.
4. Clients restructured into product feature packages and app compositions (§4).
5. Packaging and paid add-ons: proposal in §10, to confirm.

## 8. HRMS scope

### 8.1 v1 — the HRMS a school or a small company can run payroll on

| Area | v1 contents | Built on today |
|---|---|---|
| Organisation | Multiple legal entities (companies) per tenant, each with its statutory registrations (PF establishment code, ESIC code, PT and LWF registrations per state, TAN, PAN); branches/offices as `org` units with state; departments, designations, grades, reporting manager | `org.institution`, `hr.designation`, `staff_department` |
| Employee record | Employment type, joining, probation, confirmation, contract end, exit; identity and bank (encrypted), statutory ids (UAN, ESIC number, PAN); documents; history of position, grade and pay (effective-dated, C-7) | `hr.staff`, `payroll.staff_bank_account` |
| Time and attendance | Shifts and rosters, attendance from devices (biometric/face/RFID through the hardware integration drafts), mobile check-in with optional geofence, regularisation requests, overtime | `attendance.staff_attendance` (moves) |
| Leave | Leave types and policies per entity/state, accrual and carry-forward, holidays per location, approvals by manager, balance; leave counted in pay (fixes BE-6) | `hr.leave_*`, `org.holiday` |
| Payroll (India) | Salary structures and templates; monthly run with attendance and leave inputs, arrears and pro-ration; **PF (EPF/EPS), ESI, Professional Tax by state, LWF, TDS on salary** with investment declarations and proofs (old/new regime), gratuity provision; the **Labour Codes' wage definition** (statutory wage base, the 50 % rule) as a configurable component classification; payslips (PDF), bank transfer files, statutory outputs (PF ECR, ESI return data, PT/LWF registers, Form 24Q data, Form 16); full-and-final settlement | `payroll.*` |
| Self-service | Employee: profile, leave, attendance, payslips, tax declarations, documents (web and the employee phone app); manager: approvals, team calendar | `MyPayslipsScreen`, leave screens |
| Compliance and audit | Statutory registers kept digitally, audit trail, privacy erasure and retention (`payroll.after_leaving`) | privacy handlers built |
| Reports | Headcount, attrition, payroll register, statutory reports, leave and attendance summaries | — |

The Labour Codes were notified with immediate effect on 21 Nov 2025; central and state rules are still being issued,
so statutory rates, wage-definition rules and state variants are **tenant/platform data with effective dates**, never
code constants, and each rule set is *to verify* against the notified rules before release.

### 8.2 v2 — what established Indian HRMS products offer beyond v1 (later)

| Area | v2 contents |
|---|---|
| Recruitment (ATS) | Requisitions, job posts, candidate pipeline, interviews and scorecards, offer letters, background verification hand-off |
| Onboarding and exit | Pre-boarding portal, document collection, asset allocation, induction checklists; exit workflow, clearances, exit interviews, experience/relieving letters |
| Performance | Goals and OKRs (cascading), KPIs, review cycles with self/manager/360° reviews, continuous feedback, calibration, PIPs (absorbs NM-12) |
| Compensation | Salary revision cycles and budgets, bonus and incentive plans, flexible benefits plan (FBP) declarations |
| Expenses and travel | Claims with receipts, approvals, reimbursement through payroll, travel requests and advances |
| Learning | Training plans and CPD hours, linked to the learning product when the tenant has it |
| Engagement | Pulse surveys, eNPS, recognition and rewards, announcements, polls (through comms) |
| Assets | Asset register and assignment to employees (with campus/inventory NM-4 for schools) |
| Helpdesk | HR tickets and policies knowledge base |
| Workforce planning and analytics | Org chart, headcount plans, attrition and cost dashboards, people analytics |
| Wider payroll | Multi-country payroll (GCC, US) as separate rule packs; loans and advances; contractor/consultant payments |
| Integrations | Accounting export (Tally, Zoho Books), bank APIs for payouts, EPFO/ESIC portals, single sign-on, open API and webhooks (P1-36) |
| AI | HR assistant over policies, payslip and tax explanations, anomaly checks on payroll runs (needs the AI provider decision) |

## 9. Product vocabulary (open: naming mechanics)

The rule "student and teacher only" keeps one name per concept in the school product. With several products one
person or record is called differently (a school's *teacher* or *staff member* is an HRMS *employee*; an LMS
*learner* may be a school's *student*). Options (decision open):

| Option | How | For | Against |
|---|---|---|---|
| A. Neutral contract, product terms in the clients | Backend names stay as they are (`/v1/hr/staff`); each feature package resolves display terms through i18n term keys (`term.staff` → "Teacher"/"Staff" in the school web, "Employee" in web-hrms) chosen by the app's product | No API change; one place per product | Two words for one API field in docs |
| B. Product terms plus tenant overrides | As A, and a tenant may override a term within an allowed list (e.g. "Faculty" for colleges, "Associate" for companies) | Fits colleges, coaching centres, companies | Needs a small terms setting and its admin screen |

Recommendation: **B** (A now, tenant overrides later); amend CLAUDE.md to "one name per concept **per product
vocabulary**: school — student, teacher; HRMS — employee, manager; LMS — learner, instructor", contracts neutral.

## 10. Packaging and paid add-ons (proposal for the owner)

Price anchors seen in the Indian HRMS market (public listings, to verify before pricing): greytHR's entry plan from
₹2,495 per month for 50 employees plus ₹45 per employee above; Keka about ₹6,999–9,999 per month up to 100 employees.
Statutory payroll (PF, ESI, PT, TDS) is part of the core plan in these products, not an add-on.

| Product / tier | Included | Paid add-ons |
|---|---|---|
| **School — Core** (per student per year) | Kernel (users, roles, privacy centre, notices, in-app messages), students and admissions, academics (classes, timetable, attendance, homework, exams, report cards), fees with online payment, parent/student/teacher apps | — |
| School add-ons | | Learning (LMS + tablet offline); Community network; Transport tracking (GPS, NM-5); Library; Front office and gate/pickup (NM-6); Hostel and canteen (NM-2, NM-3); Inventory/store (NM-4, CP-5); Admissions CRM (CP-1); School website (CP-4); AI features (report-card explainer, rulebook Q&A — IN-5, IN-9); SMS/WhatsApp message packs (pass-through cost) |
| **HRMS — Core** (per employee per month) | v1 of §8.1: employee records, leave, attendance with devices and mobile check-in, India payroll with statutory compliance and outputs, self-service apps, reports | — |
| HRMS add-ons (v2) | | Recruitment and onboarding; Performance and OKRs; Expenses and travel; Engagement; Advanced analytics; Multi-country payroll |
| School bundle | School Core + HRMS Core for the school's own staff at a bundle discount (the school's staff are its employees anyway) | as above |
| **Learning — standalone** (per active learner per month) | Courses, cohorts, quizzes, certificates, live classes, tablet/phone apps | Proctoring, AI tutoring, e-commerce for paid courses |
| Always free / included | Privacy and consent centre (legal obligation), platform security, the kernel | — |

Principles: compliance and safety are never paid extras (privacy, statutory payroll, audit); usage costs passed
through (SMS, WhatsApp, storage above quota); entitlements are platform data (GK-20), so plans change without a
release.

## 11. Status as built (2026-11-25)

### 11.1 Built

| Step | What | Where |
|---|---|---|
| 0 | Rules amended for modules of areas: CLAUDE.md §2/§6/§7/§10, JAVA-CODING-RULES §2.2 (added; §2, §3, §10.5, §14 carry a pointer, no rule dropped), ARCHITECTURE §1/§2/§6. Conformance knows areas (`SourceTree.Area`: an area is a migration folder its module's pom packages); `ModuleBoundaryRule` checks area packages, per-area SpotBugs coverage and migrations within the module's schemas; G1/G3/G8/G9, mapper, persistence, SQL and status-report rules work per area. Core test kit: `ModuleChecks.moduleSchemas`, sibling schemas allowed | `backend/conformance`, `core/src/test/.../testkit/ModuleChecks.java` |
| 2 (structure) | `hrms` = hr + payroll + time_attendance in one module: `com.mysms.hrms.hr`, `com.mysms.hrms.payroll`, `com.mysms.hrms.timeattendance`, one `HrmsApplication`, one `application.properties`, one gRPC server | `backend/hrms` |
| 5 | All remaining merges: `platform` (platform, master, org), `identity` (iam, people), `students` (sis, admission, careers), `academics` (academics, timetable, attendance, assessment), `campus` (library, transport, frontoffice, facility), `learning` (lms); `privacy`, `comms`, `finance` in the same shape. `app` assembles the 10 modules | `backend/*`, `backend/app` |
| — | OpenAPI per area: core `OpenApiGroupsEnvironmentPostProcessor` declares one springdoc group per served area (`/v3/api-docs/<area>`), and the required-field restoration resolves contract classes within the document's area; `<Area>OpenApiComponentTest` writes `target/openapi/<area>/api-docs.yml`; the clients' SDK and core generators find each area's operation table and document in its module, so the SDK keeps one namespace per area (23 since 2c) | `core`, `clients/packages/{sdk,core}/scripts/generate.mjs` |
| — | Monolith configuration: a module file counts when its first area's operation table sits beside it (`ModulePropertiesEnvironmentPostProcessor`); callers address an area at its module's host (`dns:///<module>-service`) | `core`, every module's `application.properties` |
| — | Split deployment on RabbitMQ: a consumer expects an event from the broker user of the module serving its source area (`MessagingModules` area → module table, kept equal to the modules' areas by conformance G9); DEPLOYMENT §RabbitMQ lists each module's areas for its permissions | `backend/messaging`, `backend/conformance` |

### 11.2 Not built yet (in order)

| Step | What remains | Note |
|---|---|---|
| 1 | Product entitlements and feature-area switches (platform data, edge 404, navigation filter) | needs the entitlement data model in `platform` and an auth-client edge check |
| 2 (data) | Fix BE-6 (leave ↔ holidays ↔ pay) inside hrms, holidays from ARC-8's `master.holiday` through an hr event-fed copy. **BE-6 done 2026-11-27 (2d):** leave counts working days (weekend property + holidays), payroll deducts unpaid leave as its own line, approved leave writes staff attendance; the hr copy `hr.holiday_record` is fed by `org.holiday` events until ARC-8 creates `master.holiday` (then only the feed switches); percentage pay components wait for a schema notion (ROADMAP BE-6). **Done 2026-11-27:** `attendance.staff_attendance` moved to hrms as the new area `time_attendance` (own schema, `/v1/time-attendance`, `time_attendance.*`, in-module FK to `hr.leave_application`). **Done 2026-11-26:** `payroll.staff_member` dropped (payroll reads `hr.staff`); `hr.staff_subject` → `academics.teacher_subject` | contract paths of the moved resources changed; clients followed |
| 2/5 | Replace gRPC between sibling areas by direct calls: admission → sis (students), attendance → timetable, assessment → academics, attendance/timetable/assessment → academics (academics), iam → people (identity), org/platform → master (platform), library → … | **Done 2026-11-27.** attendance → timetable, assessment → academics, iam → people, platform and org → master, admission → sis call the sibling's business service; auth-client's `SiblingAccess` makes the edge's permission check, core's `InProcessIdempotency` keeps the callee's `request_id` rules (admission's sis creates); timetable/attendance → academics, library and hr ↔ payroll had no sibling gRPC calls. The callees' gRPC servers stay for other modules (JAVA-CODING-RULES §2.2) |
| 5 | Break the module-level students ↔ academics cycle (sis → academics; attendance/assessment → sis): event-fed projections in one direction | the area graph stays a DAG |
| 3, 6 | Clients: `packages/kernel` and `packages/features/*`, ESLint boundaries; split `ui/02` per product | the SDK keeps one namespace per area |
| 4 | `apps/web-hrms`, employee phone variant, console entitlements, `app-hrms` / `app-learning` assemblies, `company`/`branch`/`office` institution types | after step 1 and the HRMS v1 scope (§8.1) |
