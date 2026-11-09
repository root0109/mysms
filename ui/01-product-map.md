# 01 · Product map — personas, clients, interoperability

What MySMS is for each kind of user, which client serves them, and how the six clients stay one product.

## 1. Personas

Roles come from `iam.role` grants scoped by `iam.user_role` (tenant, institution, department, class section, course, LMS course or cohort). One person can hold several personas at once — a teacher who is also a parent — and switches with `iam.SwitchRole`; the UI treats personas as *workspaces*, never as separate accounts.

| # | Persona | Primary client(s) | Top jobs to be done |
|---|---|---|---|
| 1 | Platform operator | C | Provision tenants, plans and subscriptions; global catalogues; sales enquiries; clear demo data |
| 2 | Tenant administrator | W | Institutions, academic years, roles and permissions, users, branding/theme, reference data, imports |
| 3 | Principal / institution head | W | Dashboards, approvals (leave, lesson plans, promotions), notices, reports |
| 4 | Admissions officer | W | Admission cycles, applications pipeline, review → confirm → enrol |
| 5 | Office / front desk | W | Visitors, calls, postal log, directory, enquiries |
| 6 | Accountant | W | Fee structures and plans, dues, invoices, payments and receipts, refunds, reminders, reports |
| 7 | HR / payroll officer | W | Staff records, leave policy and approvals, salary templates, payslip runs, payments |
| 8 | Librarian | W | Catalogue, copies, cards, circulation, reservations, fines, purchase requests |
| 9 | Transport coordinator | W | Routes, stops, student transport assignment, bus passes |
| 10 | Class teacher / subject teacher (K-12) | T, W | Attendance, homework, marks, timetable, class wall, messages to parents |
| 11 | Teacher (higher education) | W, T | Sessions, subject registration approvals, exams and marks, batches |
| 12 | LMS author | W | Courses, chapters, lessons, question bank, quizzes, assignments, rubrics, catalogue page |
| 13 | LMS teacher | W, B | Cohorts, outline dates, roster, gradebook, grading, forums, live classes, release rules |
| 14 | Student (K-12 / HE) | S, W, B | Today's timetable, homework, attendance, results, fees, notices, learning |
| 15 | Parent / guardian | P, W | Children's attendance, homework, fees → payment, messages, consent, events |
| 16 | External student | W, B | Catalogue → enrol → learn → certificates; billing |
| 17 | Evaluator | W | Availability, evaluation schedule, conduct evaluations, certificates |
| 18 | Observer / mentor | W, B | Read-only view of a cohort or mentee progress |
| 19 | Alumni / candidate | W | Public profile, job openings, applications |

## 2. Role → client matrix

● full surface · ◐ focused subset · ○ read-mostly · — not served.

| Persona | W | S | P | T | B | C |
|---|---|---|---|---|---|---|
| Platform operator | — | — | — | — | — | ● |
| Tenant admin / principal | ● | — | — | ◐ (notices, approvals) | — | — |
| Office roles (4–9) | ● | — | — | — | — | — |
| Teacher (K-12 / HE) | ● | — | — | ● | ◐ (teacher mode) | — |
| LMS author | ● | — | — | — | ○ | — |
| LMS teacher | ● | — | — | ◐ | ● | — |
| Student | ● | ● | — | — | ● (student mode) | — |
| Parent | ● | — | ● | — | — | — |
| External student | ● | ◐ | — | — | ● | — |
| Evaluator / observer | ● | — | — | — | ○ | — |

## 3. Feature parity matrix

What each domain offers per client. "Capture" = create/edit; "view" = read + drill-down. The web client always has the full surface; the matrix below states what the *focused* clients carry, so scope is a decision, not an accident.

| Domain | S (student) | P (parent) | T (teacher) | B (tablet) |
|---|---|---|---|---|
| Dashboard / today | ● own day | ● per child | ● teaching day | ● cohort day |
| Timetable | view | view per child | view + session status | cohort schedule |
| Attendance | view own + summary | view per child | **capture** + summaries | view (cohort engagement) |
| Homework (K-12) | view + submit | view per child | **capture** + check | — |
| Assignments & exams (official) | view + submit | view results | marks **capture** | — |
| Results / marksheets | view published | view published | view class results | — |
| Fees | view dues | view + **pay** (checkout) | — | — |
| Learning: catalogue & enrol | browse, enrol | — | — | browse, enrol |
| Learning: consume (lessons, quizzes, assignments, SCORM, e-books) | ● | — | — | ● **offline** |
| Learning: cohort (forum, live class, gradebook view) | ● | — | — | ● |
| Grading (LMS) | — | — | ◐ quick grade | ● gradebook + rubric grading |
| Authoring (courses, quizzes, questions) | — | — | — | ○ (edit text, reorder) |
| Notices & calendar | view | view | view + post to own classes | cohort calendar |
| Messaging / conversations | ● | ● | ● | ◐ |
| Notifications (push + inbox) | ● | ● | ● | ● |
| Library | view own loans | view per child | view own loans | — |
| Transport | view route | view per child | — | — |
| Leave | request own leave | request child leave | own leave + approvals | — |
| Payroll | — | — | own payslips | — |
| Careers / jobs | ◐ | — | — | — |
| Profile, consent, settings | ● | ● + consent per child | ● | ◐ |

## 4. Tenancy, institution and persona context

Every signed-in screen runs inside three nested contexts, surfaced in the app shell (02 §1.2):

1. **Tenant** — fixed by the host / sign-in; never user-switchable inside a client.
2. **Institution** — from `iam.GetMe → institutions`. Users with more than one get an institution switcher; the choice scopes every institution-bound list and create. Contract wiring is pending (PRINCIPAL-REVIEW Decision D2): until settled, clients pass the record's `institutionId` field and keep the switcher purely client-side.
3. **Persona (active role)** — from `Me.roles`; `iam.SwitchRole` returns fresh tokens and the new permission set. The shell re-renders navigation from the new `Me`. (Whether authority is the union of grants or the active role is Decision 1; the shell works identically either way — it renders from `Me.permissions`.)

A parent's **child context** is a fourth, P-app-only switcher fed by `sis.ListMyChildren`; every P screen is scoped to the selected child.

## 5. Interoperability model — one product on six surfaces

| Mechanism | Rule | Source |
|---|---|---|
| One account | Same credentials and tokens in every client; `iam.Login` → `TokenPair` + `Me` everywhere | api/README §Auth |
| One API + one SDK | All clients use the generated TypeScript SDK from the same OpenAPI; no client-specific endpoints (BFF exception: PRINCIPAL-REVIEW P1-37) | 05 §4 |
| One link space | `mysms://<module>/<path>` ↔ `https://<tenant host>/app/<path>`; every client registers the scheme + universal/app links and routes by the same table (02 §1.3). A client that cannot show a path opens the web URL | ARCHITECTURE §4 |
| One notification fabric | `comms.in_app_notification.actionUrl` carries the deep link; push (FCM/APNs) and the in-app inbox render the same item; read state syncs through `comms.MarkNotificationRead` | 03 F12 |
| One theme | Tenant theme tokens fetched once (`platform.GetBranding`, theme resource) and mapped to the shared token set; all clients restyle identically | 04 §2 |
| One permission model | All clients hide/show by the same permission codes from `Me` | README rule 3 |
| One i18n catalogue | Shared message catalogue package; locale from `Me` (user) falling back to tenant | 04 §7 |
| Shared logic, native UI | Domain logic, API hooks, validation and formatting live in shared packages consumed by web and React Native; **rendering is per platform** (no forced react-native-web) | 05 §1 |
| Continuity | "Continue on another device" = send yourself the deep link; drafts that must travel use server drafts (e.g. quiz autosave via `lms.SaveQuizResponses`), never device-local-only state for cross-device work | 03 F9 |

**Is building all three together doable?** Yes, and this is the cheapest way to do it: one contract → one SDK → shared core packages → thin native UIs. The cost that must be paid deliberately is the tablet's offline store (05 §7) — everything else is shared by construction.

## 6. Build order (recommended)

1. **Foundations** — monorepo, SDK generation, test fakes, tokens, auth/session, app shell, notifications plumbing (05).
2. **W: admin + academics core** — org setup, people/students, attendance, timetable, homework, fees (unblocks every school pilot).
3. **T + P phones** — teacher capture and parent pay loops (the two highest-frequency mobile jobs).
4. **LMS on W** — authoring + cohort delivery + gradebook.
5. **B tablet** — student offline, then teacher grading.
6. **S phone, C console, careers** — last; S is largely a re-skin of P/W read surfaces plus learning.
