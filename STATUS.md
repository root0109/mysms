# MySMS — project status

Source status updated on 2026-11-17. To continue the work, start with [HANDOFF.md](HANDOFF.md). The exact state of every API operation is in
[api/operations-status.md](api/operations-status.md). The build checks that file against the contracts and the controllers
(`OperationStatusReport` in `backend/conformance`), so it always matches the code.

## Summary

| Area | State |
|---|---|
| Specifications | Done: database (21 schemas, Flyway), API (2,055 operations: each module's contract interfaces and protos, its OpenAPI document written by the build), UI (186 screens, flows, design system, frontend architecture), Java coding rules |
| Backend operations | **2,004 of 2,055 served (97.5 %)** over REST and gRPC; the other **51 answer `501 NOT_IMPLEMENTED`** (comms 33, people 7, library 5, org 3, timetable 2, frontoffice 1; each listed with its reason in `api/operations-status.md`). lms (712), careers, finance, platform and iam: every operation served. |
| Backend build | `mvn clean install -Dmaven.build.cache.enabled=false` **BUILD SUCCESS** on JDK 17 (2026-11-18): 28 reactor projects, **5,500 tests** (unit + component against real PostgreSQL 16 and RabbitMQ, REST/gRPC parity per module), Checkstyle, SpotBugs, JaCoCo and the conformance checks all green, 36 min on the 2-core build machine. |
| Clients | `pnpm verify --concurrency=1`: **39 of 39 tasks green** (2026-11-18) — prettier, TypeScript SDK generated from the backend's OpenAPI documents, typecheck, lint, tests, builds; web initial JS 287.5 KB gzip of a 300 KB budget. (Default turbo concurrency runs out of memory on the 7 GB build machine.) |
| Web screens | Built (93 of the 186 inventory screens; TT-03 My timetable, HR-03 leave balance, HR-04 Leave applications, HR-08 My payslips, SIS-02 student summary and SIS-06 discipline also on phone and tablet, SIS-10 subject registration on the student app, SIS-11 My children on the parent app): sign-in (ACC-01), my profile (ACC-07), **security & sessions (ACC-08**: password change, authenticator app with one-time secret and recovery codes, turning a factor off, replacing recovery codes, ending sessions), notification preferences (ACC-10), my settings (ACC-14), records (ADM-11, a generic screen over all 327 record types), settings by scope (ADM-16), **attendance taking** (ATT-03 take attendance, ATT-04 student month, ATT-05 class summary & defaulters), **homework** (ASM-02 approval queue, ASM-03 homework check), **students** (SIS-02 student 360° with SIS-09 bonafide certificate, SIS-03 promotion wizard), **admission** (ADMN-01 cycles with their stage timeline, ADMN-02 applications pipeline, ADMN-03 application detail with ADMN-04 enrolment, ADMN-05 application form), **staff** (HR-01 directory, HR-02 staff detail, HR-03 leave types & balances), **payroll** (HR-05 salary templates, HR-06 staff salary, HR-07 payslip run, HR-08 my payslips, HR-09 payroll payments), and **65 list-manager screens** — one per record type the inventory gives its own screen (students, guardians, admission cycles, institutions, departments, academic years, programs, class sections, periods, attendance registers and sheets, staff attendance with its date range, homework, exams, staff, payroll, library, transport, front office, notices and events, LMS question bank and live classes…), each with its own route, heading, columns, status filter, create/edit/delete and the state transitions the role may use, grouped in the navigation as in ui/02 §2.1. Console: tenants, SaaS plans, sales enquiries, reserved keywords, notification types and system settings (PLT-01…05), operations and jobs, master data. Phone and tablet: sign-in, profile, settings, notification preferences, my timetable (TT-03), staff leave (HR-04), leave balance (HR-03), my payslips (HR-08), student summary and open incidents (SIS-02, SIS-06), subject registration (SIS-10, student) and my children (SIS-11, parent). The other screens are routed and show a placeholder naming the screen. |
| Deployment | [DEPLOYMENT.md](DEPLOYMENT.md). Every step was rehearsed on a fresh database: roles, keys, bootstrap, the monolith and separate microservices, the web app against the real backend. |

## What is served

- **master** (all 20 operations): countries, states, cities and currencies. Hand-written.
- **Sign-in, tokens and the caller's own account (iam)**. Hand-written:
  - Password sign-in: argon2id; progressive lockout of password sign-in only; a suspended tenant cannot sign in or refresh.
  - Tokens: refresh rotation with reuse detection; RS256 tokens published at JWKS; sign-out of one device or all.
  - `GetMe`, and the first-administrator bootstrap.
  - Second factors: authenticator apps (TOTP) with ten one-time recovery codes; sign-in waits for the code
    (`CompleteMfa`); codes are never accepted twice and wrong codes count towards the lockout; disabling a factor
    or replacing the recovery codes needs a current code, and a locked account cannot try. Verifying a new factor
    issues no tokens, and a further authenticator can only be added from a session signed in with a second factor.
    SMS and e-mail factors are not available yet (501).
  - Changing one's password (the current one is required; other sessions and any open MFA step end),
    administrators setting another account's credentials (only for accounts whose current and scheduled
    permissions they hold tenant-wide, never their own), and accepting the terms.
  - A sign-in identifier names one account: no account may use another's username, e-mail or mobile number in any
    of the three.
- **Settings (iam)**. Hand-written:
  - The setting catalogue, administrators' values at every scope with locks, and each user's effective settings.
  - Precedence: user > class section > department > institution > tenant > default.
  - Settings users may not change are invisible to everyone who does not administer them.
- **Access management (iam)**. Generated, plus hand-written rules (no escalation, fixed system roles, sign-out on account deletion):
  - roles and role permissions
  - the permission catalogue
  - user accounts
  - role grants and direct permissions
  - department access, devices, preferences and sessions
- **Standard operations of 327 record types in 20 modules** (1,513 operations). They cover every resource whose contract maps column for column onto its table. Each has:
  - validation, decimal precision, durations, 64-bit integers and times;
  - uniqueness (409), in-module references (422), If-Match (428/412), update and read masks, filters, sorting;
  - paging as `PagingInfo` (`data` + `paging` with `first` / `previous` / `next` links): `next` carries a cursor
    and is read by keyset, a page number alone by offset within the first 10,000 rows; lists are not counted;
  - soft delete, refused while live rows still reference the row;
  - audit and domain events in the module's outbox;
  - the institution data scope;
  - secrets (`*_enc` columns: national ids, bank account numbers, gateway, channel, tool and meeting secrets) taken
    write-only, stored only as per-tenant encrypted envelopes and shown masked (`••••1234`).
  - Each one is proven by its own component test.
- **State transitions of 30 record types** (74 operations),
  listed with their states in [api/state-machines.md](api/state-machines.md). Examples: submit, reject and archive a
  lesson plan; confirm, close and cancel an admission cycle; publish, cancel and reset assignments and exams; open,
  close and reopen job openings, feedback threads and tasks; publish and close notices; cancel a payslip; close an
  academic year.
  - Each step is allowed only from its listed states (else 409, nothing changes).
  - Each step needs its own permission and the same data scope as an update, and checks the body `etag` (412).
  - Each step writes an audit record with the reason, and an outbox event, in the same transaction.
  - The web Records screen offers each step on a row only when the role holds its permission and the row's state
    allows it.
- **The caller's own person record (people)**. Hand-written.
- **Staff leave (HR-04)**. Hand-written, in hr (api/state-machines.md "LeaveApplication"): `hr.ApproveLeave`
  approves a pending application and counts its days against the staff member's balance for the leave type and
  year — locked for the approval, opened with the type's yearly allowance when absent, audited, refused (409) when
  too few days are left — and nobody approves their own leave (403). Approve and reject keep the decider's comment
  on the application. `hr.GetStaffLeaveBalance` answers entitled, taken,
  pending and available days per active leave type for the year the caller names; the staff member reads their
  own, anyone else also needs `hr.leave_balance.read`. A decided application is no longer changed by the standard
  update, and approved leave is not deleted. `hr.CancelLeave` withdraws a pending application and cancels an
  approved leave before it starts, giving its days back to the balance in the same locked transaction (409 once
  the leave has started). Web, phone and tablet: what leave is left, applying (days counted as you type),
  withdrawing a pending application, cancelling an approved leave that has not started, and the approver's queue
  with a comment; all records stay in the records manager.
- **My timetable (TT-03)**. Hand-written, in timetable: `timetable.GetTimetable` answers the published timetable
  of a class, batch, teacher, student (a session's own list) or room — at least one — over up to 62 UTC days, in
  start order, page by page, inside the caller's institutions; drafts are left out and cancelled sessions stay
  visible (timetable's baseline has the indexes the view reads by). Web: a week day by day for the signed-in
  teacher or a chosen class, the week and class in the address. Phone: one day at a time from today; tablet: the
  week on one screen. Which sessions list one student (the `studentId` filter) also needs
  `timetable.session_student.read`.
- **Lesson plan review**. Hand-written, in academics (api/state-machines.md "LessonPlan"): `academics.ApproveLessonPlan`
  approves a plan that waits for review and `RejectLessonPlan` sends it back; each decision is kept as a review
  record (who, when, the reason). An approved plan can be published; a reviewed plan is archived, not deleted.
  Every academics operation is now served. Web: ACA-10 offers Approve and Reject on a pending plan to roles that
  hold the permission, with an optional reason; the approval queue is the list at `?status=pending`.
- **Homework lifecycle (flow F5)**. Hand-written, in assessment (api/state-machines.md "Homework"):
  - `assessment.PublishHomework` publishes a draft — at once when the caller also holds
    `assessment.homework.approve_homework` for its institution, otherwise it becomes `pending` with a pending
    review. `ApproveHomework` publishes it; `RejectHomework` makes it a draft again, the reason kept on the review
    record. Who needs a review is decided by the permissions of the role, not by a setting. A homework has at most
    one open review (assessment's baseline has the `pending` state and the unique index). The text of a
    homework under review cannot be changed, and a published one only by someone who may approve homework; review
    records are written by these steps alone (the contract has no create, update or delete of an approval).
  - `assessment.CheckHomeworkSubmission` marks a submission checked with the teacher's comments (checking again
    corrects them); `assessment.ReplyToHomeworkFeedback` stores the student's reply — only for the account that
    handed the submission in, and only once the teacher has commented. The check and the reply are read-only in
    the standard create and update of a submission.
  - Web: the homework board (ASM-01) offers Publish / Approve / Reject on a row in the states that allow them; the
    approval queue (ASM-02) is the same list at `?status=pending`, in the navigation of roles that may approve;
    ASM-03 shows one homework, its next step and the class as a checklist (handed in or not, comments, the
    student's reply). List filters now live in the address, so a filtered list can be linked and reloaded.
- **Official assessment workflows (ASM-01-13).** All 20 assessment gaps are implemented; [api/operations-status.md](api/operations-status.md) lists all 132 assessment operations as served.
  - Assignment workflows cover approvals, rejection, completion, change requests, submission reset and grading. Exams cover scheduling, SIS rosters, room allocation and marks entry. Results cover marksheet generation, validation, publishing, student/guardian report cards, grading schemes and bands.
  - Cumulative GPA is credit-weighted from earlier validated marksheet lines dated before the current results date, excluding the current exam session; course credits are snapshotted from Academics during roster population. SIS, Facility and Academics access requires `MYSMS_SIS_URL`, `MYSMS_FACILITY_URL` and `MYSMS_ACADEMICS_URL` respectively.
  - Assignment review uses pending `AssignmentApproval` records on draft assignments. Marks are saved only for edited rows; finalization is a separate permission-gated action. Once an exam result is linked to a marksheet, `RecordExamMarks` rejects corrections until a safe marksheet regeneration path exists.
  - Web and tablet cover all 13 inventory screens; phone targets cover ASM-01, ASM-03, ASM-04, ASM-05, ASM-06, ASM-07, ASM-10 and ASM-12. UI checks use the existing Vitest harness; no Storybook tooling was added.
  - Homework submissions have no answer-text field in the contract. File attachment support depends on `platform.CreateUploadUrl`, `platform.CompleteUpload` and `platform.GetDownloadUrl`, which still answer 501; the project has no storage provider. ASM-04 creates the supported submission receipt and supports feedback replies; students cannot add homework answer text or file uploads. Other file attachments share this platform limit.
- **Attendance taking (flow F3)**. Hand-written, across attendance, sis, timetable and org:
  - `attendance.TakeAttendanceForSession` gets or creates the sheet of a timetable session (one sheet per session,
    enforced by the unique index `uq_attendance_sheet_session` of attendance's baseline). The session, its
    students (the session's own list, else the class roster on that date) and the institution's time zone are read
    from timetable, sis and org through their SDK clients **with the caller's own token**, so the caller sees through
    attendance only what those modules let them read (403 names the missing permission; an unknown session is a 422
    on `timetableSessionId`; an unreachable or unconfigured service is 503 — `MYSMS_TIMETABLE_URL`,
    `MYSMS_SIS_URL`, `MYSMS_ORG_URL`). Every expected student starts as present.
  - `attendance.RecordAttendance` sets up to 500 marks in one all-or-nothing call while the sheet is in progress
    (field errors as `marks[i].field`). A mark for a student the sheet has no record of is a reference into sis
    and is checked there first, with the caller's token: the student must be on the class roster of the sheet's
    date or a student of the sheet's institution, else 422 `marks[i].studentId` (503 without sis). The optional
    body `etag` is the sheet's — every stored call gives the sheet a new version, so marks saved from another
    device answer 412 instead of being overwritten; an unchanged mark is not written; every stored mark has its own
    audit record naming the changed fields. With `complete` it also completes the sheet, which then needs the
    completing permission as well. `attendance.CompleteAttendanceSheet` locks the sheet (etag, at least one mark)
    and publishes `attendance.attendance_sheet.complete` with the absent students; reset reopens a completed or
    cancelled sheet and keeps its marks.
  - `attendance.SummarizeAttendance` counts the marks of completed sheets by student, class section or date over at
    most 366 days, inside the caller's institutions. `sis.ListClassRoster` lists the enrolled students of a class
    under its own permission.
  - Web: ATT-03 (roster grid ordered by roll number, one-tap marks, reason and remark, save / complete / start /
    reopen, unsaved-changes guard), ATT-04 (a student's month as a calendar with the percentage), ATT-05 (students
    lowest first against a chosen minimum, by institution and class), a link from every sheet of ATT-02, and
    "Take attendance" on every session of TT-02, which opens the session's sheet. A sheet opened for a session
    lists exactly the students it was pre-filled with; a sheet made by hand lists the class as placed on its date.
  - Calls between modules (the SDK clients) now time out (2 s to connect, 10 s to answer) and never follow a
    redirect, so the caller's token reaches only the configured service.
- **Students (sis), every operation served.** Hand-written (api/state-machines.md "sis"):
  - `sis.ChangeStudentStatus` moves a student to any other status (409 for the same one); a leaving status
    (withdrawn, transferred, graduated, alumni) needs an exit date not before admission or joining and closes the
    open placement on it ("completed" for graduated and alumni, "left" otherwise); going back to active or inactive
    clears the exit details. `sis.IssueBonafideCertificate` numbers a certificate per institution and calendar year
    (`BC/<year>/000001`) under a row lock, for an active student only.
  - Portal accounts: `sis.CreateStudentPortalAccount` and `sis.CreatePortalAccount` (guardian) check the record
    (active student with an admission number; guardian with e-mail or mobile and an active child link), then open or
    reuse the person's iam account **with the caller's own token** in the tenant's `student` / `guardian` role
    (409 when the tenant has no such role), and link it; a guardian's account needs write access to every child's
    institution (403 otherwise).
  - `sis.GetStudentSummary` is readable by the student, their own guardians (active link) and anyone with
    `sis.student.read` in the student's institution (404 otherwise); family and the student see only the incidents
    shared with guardians. `sis.ListMyChildren` lists the caller's own active guardian links with the institution
    name (org) and the class label (academics).
  - Promotions: `sis.ProposePromotions` proposes for every student placed in a class (or the listed ones, each
    checked); `sis.ApplyPromotions` applies 1–500 approved promotions all or nothing: the old placement ends
    promoted (detained when the grade stays) and next year's starts in the class academics has for that year, grade
    and section. `sis.ApproveSubjectRegistration`, `sis.LoadCourseSubjects` (the course's subjects from academics),
    `sis.MarkIncidentActionTaken` and `sis.MarkIncidentSeen` (a guardian for their own child; an unshared incident
    does not exist for them).
  - Web: SIS-02 student 360° (profile, guardians, enrolment, health, discipline, activities and subject
    registrations tabs; status change, bonafide certificate, student and guardian portal accounts), SIS-03 promotion
    wizard, the discipline and registration steps in SIS-06 / SIS-10. Teacher phone and tablet: the student summary
    and open incidents; student app: subject registration; parent app: my children.
- **Admission (admission), every operation served.** Hand-written (api/state-machines.md "admission"):
  - `admission.OpenApplications` and `StartAdmission` move a cycle on. The application steps (submit, confirm, mark
    pending, approve, confirm admission, reject, cancel, reset) each check scope, permission, etag and state in one
    version-guarded statement with audit and event. Submitting needs a guardian, an open locked cycle of the same
    institution before its end date, a free place under the cycle's maximum (safe under concurrent submits) and the
    cycle's minimum age; confirming admission needs the fee paid or waived.
  - `admission.EnrollApplicant` creates the person (people), the student and the class or course placement (sis) and
    each guardian with the caller's own token, one recorded step at a time with idempotency keys derived from the
    application, so a retry completes a half-finished enrolment; an unavailable service answers 503 and the
    application stays admitted.
  - Applicant self-service: an account holding the permissions without the institution in scope reaches only the
    applications it created, creates only in an open cycle, never sets office fields and may only submit or cancel.
  - Web: ADMN-01 cycles with a stage timeline, ADMN-02 pipeline (board and table), ADMN-03 application detail with
    every allowed step, ADMN-04 enrolment wizard, ADMN-05 application form for a signed-in applicant (saved as a
    draft from the first step).
- **Staff (hr), every operation served.** `hr.ChangeStaffStatus` (active ↔ inactive, either → exited, exited →
  active; etag and scope checked) and `hr.CreateStaffPortalAccount` (opens or reuses the person's iam account with the
  caller's token, login name = employee number; refused for staff who left or already have an account). The standard
  create and update refuse a typed `userId` (it would hand someone a staff record through `iam.GetMe`). Web: HR-01
  staff directory (search, filters in the address), HR-02 staff detail (profile, departments & subjects, leave and
  salary tabs, status and login account), HR-03 leave types & balances; phone and tablet: one's own leave position.
- **Assessment (assessment), every operation served** (the owner's commit of 2026-11-11, validated and
  completed in review). Homework and assignment workflows (approvals, rejection, completion, change requests, reset,
  grading only through the workflow); exams (scheduling, sis rosters and facility rooms read outside the write
  transaction and re-checked, marks entry with an optional per-row etag, absent and exempted); results
  (generation per result template with per-paper and overall percentages rounded half-up to 2 decimals, a student
  who fails or misses any non-exempted paper fails overall, cancelled papers left out, credit-weighted SGPA/CGPA;
  validation, publishing; cancelling a marksheet releases its results and returns its template to draft so marks
  can be corrected and generated again); grading schemes and bands (locked while a template uses them). Submissions
  and results are owner-only below staff: staff means a reviewing permission in the institution; anyone else sees
  only their own student's (or child's) rows, proved through sis with the caller's token. Web: ASM-01…13; phone and
  tablet: the assessment screens with student entries for ASM-04, ASM-07 and ASM-12. Needs `MYSMS_SIS_URL`,
  `MYSMS_FACILITY_URL`, `MYSMS_ACADEMICS_URL`. Homework submissions carry no answer text or files yet (the platform
  file store is built in the batch after this one).
- **Payroll (payroll), every operation served.** `payroll.GeneratePayslips` writes one payslip per staff member of a
  school whose salary is in force on the period's last day, for a period inside one calendar month (422
  otherwise): each monthly component amount × the days a salary of that school was in force ÷ the days of the
  month, rounded half-up to the cent (two half-month runs pay the month once); a payslip that exists for the
  period is kept, an overlapping one skips the staff member, and the result names who was skipped and why. Payslips
  and their lines are owner-only below the payroll officer: a caller without `payroll.payslip.generate_payslips`
  in the payslip's institution sees only the payslips of their own staff records (payroll keeps which account owns
  which staff record from hr's staff events in `payroll.staff_member`); any other is 404. `payroll.MarkPayslipPaid`
  marks an unpaid payslip paid and records the payment of its net from an account of the payslip's school, in one
  transaction. Web: HR-05 salary templates, HR-06 staff salary, HR-07 payslip run (generate, progress, mark paid or
  cancel), HR-08 my payslips (also phone and tablet), HR-09 payroll payments and disbursement accounts (account
  numbers only ever shown masked).
- **Partitioned tables** (attendance records, staff attendance, outbound messages, content views: 12 operations),
  generated. A list must bound the partition key on both sides (`attendanceDate >= "…" AND attendanceDate < "…"`,
  at most 366 days, not inside OR or NOT), so the database reads only those partitions; without it the list answers
  400 `INVALID_FILTER`. An attendance record takes its date from its sheet. Core's `PartitionMaintenanceJob` creates
  the monthly partitions ahead, logs an ERROR when a default partition receives rows, and drops old months only when
  a retention is configured (DEPLOYMENT.md §5.6).
- **Events between modules** use the MySMS `messaging` library: RabbitMQ is the default adapter, Google Cloud Pub/Sub
  and in-process delivery are alternate implementations. Every module's outbox is relayed (as `mysms_relay`, a role of its own
  granted the outbox and inbox only), confirmed by the broker, and consumed exactly once through the consumer's inbox
  (keyed by tenant and event id), with delayed redelivery, a dead-letter queue per module, a queue for events no
  consumer had bound yet, and an operator command to replay both. Each service publishes as its own broker user: the
  broker limits it to its module's event types and consumers accept an event only from its module's user. A bad outbox
  row is set aside instead of holding back the module's later events; `/actuator/health` shows the bus and the relays.
  Flowing today: every module's audit records into `platform.audit_log`, and the **reference cleanup** of
  database/logical-references.md — 153 actions in 19 modules (the forward and the against-DAG references), generated: when an owner is
  deleted, the rows that referenced it clear the column or are archived, audited and announced in turn. References
  are not checked against their owner when written (decision of 2026-11-07).
- **Long-running operations of 10 modules** (30 operations: `List`/`Get`/`CancelOperation`). A caller sees and cancels their own operations; the module's
  operation-read permission held tenant-wide covers every operation of the tenant. Only a `running` operation can be
  cancelled (409 otherwise); a cancel is audited and publishes `<module>.operation.cancelled`.
- **Idempotency-Key on every operation that declares it** (core's `IdempotencyFilter`). The first request with a key
  runs and its response is kept for 24 hours, encrypted with the field cipher; a repeat with the same request gets
  the kept response with `Idempotent-Replayed: true`; the same key with a different request answers 422
  `IDEMPOTENCY_KEY_REUSED`; a repeat while the first still runs answers 409 `REQUEST_IN_PROGRESS`. A 5xx or an
  exception releases the key so the request can be retried. Keys are per tenant and per user.
- **Design conformance check** (the `conformance` Maven module, run by every `mvn verify`): every repository statement over a tenant table
  states `tenant_id = :tenantId`; SQL only as repository constants; no `@Transactional`/`TransactionTemplate`, ORM,
  ArchUnit or Modulith; module dependencies, package layers and entity base class per the rules; no hard DELETE on
  audited tables; mocks only in unit tests; migrations touch only their own schema, with no cross-schema foreign
  keys. 0 violations.

## Data security as built

- **Tenant isolation.** Every SQL statement over a tenant table filters `tenant_id = :tenantId` itself, bound from the
  call's tenant by core's `TenantJdbcClient` (a statement without a bound tenant fails instead of running
  unfiltered). PostgreSQL row-level security binds every transaction to the token's tenant as the backstop. Every generated resource is tested so that another tenant never sees its rows.
- **Institution scope (`DataScope`).** A row belongs to an institution through its own column, or through its parent chain, or is tenant-level. Institution rows are visible only inside the caller's institutions (404 otherwise). Tenant-level rows change only through a permission held tenant-wide (the token's `tperms`). A tenant-wide grant of some other permission never widens a write.
- **Administrator-only settings** are invisible to users who don't administer them.
- **No privilege escalation in access management.**
  - Nobody hands out a permission they do not hold, at a reach they do not have.
  - Nobody changes their own access or deletes their own account.
  - System roles are fixed.
  - A deleted account is signed out everywhere in the same transaction.
- **Secrets.** Signing keys, the field-encryption master key and passwords come only from the environment. Refresh
  tokens, MFA step tokens and recovery codes are stored as SHA-256 hashes only. Secret columns are encrypted with
  AES-256-GCM under a per-tenant key (HKDF from the master key), bound to tenant and column; the plaintext is never
  returned, logged or filterable.
- **No database errors leak.** Every database error answers with a problem document that names no values. NOT NULL, CHECK and precision violations give 422; duplicates and blocked references give 409.

## Known limits (deliberate, documented, and on the list below)

1. **Union of institution grants.** A token carries one set of institutions (`ins`) for all of the caller's institution grants. Someone holding different roles in two schools has the union of those roles' permissions in both. Tenant-wide permissions are tracked separately (`tperms`).
2. **References into other modules are stored as given** (decision of 2026-11-07). For example, a student's `personId` or `institutionId` is not checked against the owning module. Row-level security still keeps tenants apart, in-module references are checked, and when an owner is deleted the referencing rows are cleaned up from its event (limit 4).
3. **References across schools.** An in-module reference can point to a row of another school of the same tenant, for example a category of school B on a record of school A.
4. **Deleting a row does not check other modules' references to it first.** The owner's `deleted` event cleans them up afterwards (cleared or archived, per database/logical-references.md), within seconds while the bus runs. Event handling is at least once and unordered across replicas (the inbox makes it exactly once); an event a module fails five times waits in its dead-letter queue for an operator.
5. **Rows with no institution path are tenant-level.** Any holder of the read permission can read them across the tenant. Personal rows such as addresses, emergency contacts and job applications are not yet limited to the person's own rows; that needs the persona operations.
6. **Changes to a user's access reach their token at the next refresh**, at most one access-token lifetime (15 minutes).
7. **A password change or a temporary password does not yet force the change at sign-in.** `mustResetPassword` is
   set and returned with the account, and clients must send the user to change it; the server does not yet limit
   such a session to the password change.
8. **Encrypted values are bound to their tenant and column, not to their row.** Someone with write access to the
   database could move a ciphertext between two rows of the same tenant and column. The API cannot.
9. **Starting a sign-in replaces the open MFA step.** Someone who knows the password can keep restarting sign-in,
    which ends the owner's pending step (an annoyance; it never signs anyone in).
10. **Some state flags are also writable through the standard update.** Where the contract lists a flag as an ordinary
   field (for example a notice's `isClosed`, a forum topic's `isHidden` or an occurrence's `isCancelled`), a holder of
   the update permission can change it with PATCH as well as through its step, and that change carries no reason.
   Status columns and the decision stamps (`decidedBy…`, `decidedAt`/`decidedOn`, `publishedAt`, `postedAt`) are
   read-only in the contracts, so they change only through their steps.
11. **Attendance taking.**
    - `attendance.SummarizeAttendance` counts every mark of every completed sheet. A school that keeps both daily
      (class) and period (subject) registers gets both kinds added together per student, and the class grouping and
      class filter see only class registers. Choosing one kind of register needs a contract parameter (not decided).
    - `attendance.SummarizeAttendance` and ATT-04/ATT-05 are staff views (their own permission, institution scope).
      Students and guardians do not have their own attendance view yet; that needs the persona operations.
    - Completing a sheet publishes the event with the absent students; nothing delivers the absence notice to
      guardians yet (message delivery is not built).
    - The web screens name a student from sis and people (`sis.student.read`, `people.person.read`); a role without
      them sees roll numbers or positions instead of names. ATT-05 names classes by grade and section, so the same
      class of two academic years reads the same.
    - The phone and tablet surfaces of ATT-03 (with the offline queue of ui/05 §7.4) are not built, so flow F3
      starts from the web timetable list (TT-02), not yet from the teacher app's Today tab.
    - A mark for a student outside the sheet's class needs `sis.student.read`; the class roster alone
      (`sis.student_enrollment.list_class_roster`) covers the class. `sis.ListClassRoster` returns whole
      enrolment rows (with id-card and board registration numbers) for the whole institution's classes, not only
      the teacher's own sections; narrowing it is a persona rule still to build.
    - Field errors and state conflicts show the server's sentence, which is English until the server localises
      problem details.
12. **Homework.** Students and guardians have no homework screens yet (ASM-04 handing in and the reply are phone
    screens; the operations are served). The class and the reviewer are not notified: the events are published,
    message delivery is not built. A homework's students are the class as placed now, not as placed when it was set.
    "Only the student replies" is judged by the account that created the submission; a submission a teacher records
    for a student cannot be replied to. Whoever may read submissions in an institution reads all of them, a
    student's included: "a student sees only their own" is a persona rule still to build. "May approve in the
    homework's institution" is as exact as the token (limit 1).
13. **The bootstrap tenant is not registered with platform.** The first administrator's tenant is created in iam
    only; `platform.tenant` has no row for it, so platform's audit consumer rejects that tenant's `audit.recorded`
    events (foreign key `fk_audit_log_tenant_id`) and they end in the dead-letter queue. Tenants created through the
    platform are not affected. Found 2026-11-10, not fixed yet (HANDOFF.md §6).
14. **My timetable (TT-03).** Built for teachers on web, phone and tablet, and for a class on web.
    `timetable.session.get_timetable` is a grant for staff roles for now: the view shows any class's, teacher's
    or room's timetable of the caller's institutions and does not yet narrow to "own" (ui/02: S own, T teaching,
    P per child), because timetable does not know the caller's own student or staff record. `iam.GetMe` names the
    caller's staff and student records (`staffIds`, `studentIds`: the records of the account and of its person,
    kept by iam from hr's and sis's events in `iam.member_record`), so an account that is not staff is told so
    without asking hr; reading the staff rows themselves (their institution) is still `hr.ListStaff` by those ids,
    so the teacher role needs `hr.staff.read`, which also shows colleagues' staff records — the remaining fix is a
    self-scoped read. Records created before iam kept them are named once they next change; until then the
    apps look an account's records up by `userId` when iam names none. Students and
    guardians have no timetable of their own yet: a student's timetable is their class's plus the sessions that
    list them, and which class that is belongs to the sis persona operations (`sis.ListMyChildren`, enrolment of
    the caller), which are not served. The server answers whole UTC days because timetable does not know an
    institution's time zone; the clients ask for a day more on each side and lay sessions on the reader's days (the
    reader's time zone setting, else the device's — not the institution's). Subject, teacher and room names are
    not filled by the server (other modules' data): the clients read subject and room names themselves and do not
    show the teacher's name on a class timetable. The phone and tablet screens are covered by type checking and
    lint and by the tests of the shared hooks; the native apps have no screen tests yet, and nothing is kept for
    offline reading.
15. **Staff leave (HR-04).** A day of leave is a calendar day (UTC) from the first to the last day of the
    application, halved for a half-day part: weekends and holidays inside a leave are counted, because hr has no
    calendar of them, and a client that sends local times near midnight far from UTC can be a day off (the apps send
    UTC midnights). Approved leave can be cancelled only before it starts, and as a whole: a leave that has begun
    is not shortened (no such step), and `used_days` is not writable, so HR corrects a mistake by raising the
    entitlement. Cancelling returns the days the application counts now, to the balance of its type and year (a
    balance deleted and reopened in between is credited, never below zero); "before it starts" is judged against
    the leave's first instant in UTC, like its days. Of several
    pending applications the first approved gets the days; the position shows what is left after all of them.
    `hr.GetStaffLeaveBalance` requires the academic year (the contract was changed from "optional, default
    current": hr does not know which year is current); the clients take the year whose dates cover today, and file
    an application under the year its first day lies in. The standard create still lets a holder of
    `hr.leave_application.create` apply in the name of another staff member of the same institution; the apps
    only apply for the signed-in account. As with the timetable, reading one's own staff record (named by `iam.GetMe`)
    needs `hr.staff.read`, and an approver without it sees their own applications in the queue (the server refuses the
    approval). On phone and tablet the dates are typed as year-month-day (no date picker yet), a decision carries
    no comment, and the screen is covered by type checking, lint and the shared hooks' tests only.
16. **Reads may come from a replica.** With `MYSMS_DB_REPLICA_URL` (or a module's own) set, every read transaction
    runs on the replica and every write on the primary. A read just after a write by another request may show the
    row as it was before (replication lag): an update made from it answers 412 (the ETag is stale), never a lost
    update, and a read inside a write transaction always sees the primary. Sign-in reads (accounts, lockout,
    second factor, tenant status) always use the primary, and an unavailable replica is skipped (reads go to the
    primary). Without a replica nothing changes.
17. **Deep pages follow links.** A page number without a cursor is read by offset and is refused beyond the first
    10,000 rows (400); deeper pages are reached through `next` links, whose cursor expires after 24 hours. Lists
    give no total count.
18. **Students (sis).**
    - No bonafide certificate document is produced (the number is issued and kept): the platform file store is not
      served yet. `sendInvite: true` on a portal account is refused (422), as in hr: message delivery is not built.
    - The student summary leaves attendance, open loans and outstanding fees empty: attendance, library and finance
      depend on sis (ARCHITECTURE §2), so sis may not call them; the clients would have to read them themselves.
    - The steps that call iam, people, org and academics are component-tested up to their refusals and the 503 of a
      missing service; no test runs them end to end against the real services yet.
    - Applying more than 500 promotions is done in batches; a subject registration's unit load is not checked on
      approval; placements close and open on the UTC date. A parent sees a child's name on the apps only when their
      role may read persons.
    - Deployment: sis needs `MYSMS_IAM_URL`, `MYSMS_PEOPLE_URL`, `MYSMS_ACADEMICS_URL` and `MYSMS_ORG_URL`, and each
      tenant needs roles coded `student` and `guardian` for the portal accounts.
19. **Admission.**
    - Enrolment does not create fee dues (operations.md says it should; finance is not called). An application
      guardian is matched to an existing sis guardian by e-mail, then mobile (by name among the already-linked ones
      when it has neither). Each enrolment step's Idempotency-Key is stable per application, caller and step; an
      application edited after people took the person step's key answers 422 `IDEMPOTENCY_KEY_REUSED` rather than
      creating a second person.
    - The application form (ADMN-05) is for a signed-in applicant account: the contract has no public operation, and
      the applicant role must never be granted tenant-wide (no role template for it exists yet). Uploads (photo,
      documents) are not built. An applicant's application is numbered by the school (`APP-<year>-000001` from
      `admission.number_sequence`); the number the applicant sends is replaced, because the contract requires one.
      An applicant never sees the office's `remarks` and `paymentStatusNote` and cannot filter by them (400).
    - The enrolment wizard names classes when the role may read class sections, grade levels and sections, else it
      takes the class section or batch id (prefilled from the application); the classes are not narrowed to the
      application's academic year.
    - Not run end to end against people and sis yet (refusals, 503 and the already-enrolled answer are tested).
20. **Staff (hr).** Reading one's own staff record still needs `hr.staff.read` (limits 14 and 15): no self-scoped
    read exists in the contract. The staff documents tab is not built (no served operation). `sendInvite: true` on a
    staff login account is refused (422) rather than ignored. Salary amounts on HR-02 have no currency (the contract
    carries none).
21. **Payroll.**
    - "Payroll officer" is whoever holds `payroll.payslip.generate_payslips` in the institution (the owner-only
      rule above). A staff member who moves school within a month is paid by each school for its own days only
      when each school runs its own part of the month: a full-month run of the first school skips them
      (`NO_SALARY`). Payroll does not know when someone has left hr (hr's events carry no status), so a salary in
      force keeps being paid until it is ended.
    - A payslip run ignores leave and attendance (pay percentage 100, no leave deduction) and pays the amounts stored
      on each salary component; a template's "40 % of basic" rate is shown, not calculated (the schema has no
      machine-readable calculation type). The run works inside the request, so it grows with the staff count.
    - Template components cannot be reordered (the contract keeps no order); no payslip PDF (no served operation).
    - Creating a salary writes the salary and each component separately (the contract's shape); if one fails, HR-06
      shows the amount still to fill in.

## What is not built yet

| Area | Operations | What it needs |
|---|---:|---|
| comms | 33 | Messaging and mail workflows (send, compose, read marks, retry/cancel, test message), in-app notifications, reactions on posts and comments, feed and calendar reads |
| people, library, org, timetable, frontoffice | 18 | Per operation in `api/operations-status.md` |
| Infrastructure | — | Done since 2026-11-12: gRPC servers for all 21 modules with the edge (authentication, permission, idempotency, error trailers), inter-module gRPC clients, service tokens (client credentials). Still to build: reference reconciliation, message delivery to e-mail/SMS/push providers, gateway rate limiting |
| Screens | about 45 | The LMS screens are built (LMS-01…55 on web, phone and tablet per the inventory, 2026-11-17). Left: inventory screens that need more than a list manager — dashboards, grids (timetable builder week view, gradebook), detail pages with tabs, "my …" views and the phone/tablet surfaces. Lists show the record type's own fields: names held by another module (a student's name lives in people) need batch card reads (PRINCIPAL-REVIEW P1-34) |

## How this was verified

- **Backend baseline (2026-11-07, before messaging refactor).** `mvn install`: 3,856 tests, 0 failures, Checkstyle 0 violations, SpotBugs 0 bugs, JaCoCo thresholds met across 28 reactor projects (the parent plus 27 child modules). The current messaging changes still need a Maven build; this environment has Java but no `mvn` executable.
  - The contract tests are generated per resource, from samples that are first proven against a migrated database.
    Each state transition is tested the same way: the row is put into an allowed state, the step must answer 200 and
    set its values, a stale etag must answer 412, and a repeat must answer 409 when the new state does not allow it.
  - Each module is also checked as one shared database run.
- **Clients baseline (2026-11-10).** `pnpm verify`: 39 of 39 tasks (the mock-server package is gone: fakes live only in tests).
- **End to end.** A real browser ran the web app against the real monolith on PostgreSQL: sign-in, Records create/validate/duplicate/edit/delete. Separately, three microservices verified each other's tokens over JWKS, and forged or altered tokens were refused.
- **Independent review.** A different model (Sonnet; Fable was rate-limited) reviewed the generated code. Its security findings were fixed and test-covered: permission-specific scope, partial-index uniqueness, wire-value 500s, the institution's own scope, and nested-create existence leaks.
  - A second Sonnet review covered the field encryption, MFA and credential code. Its findings were fixed and test-covered:
    - verifying a new factor no longer mints a session;
    - a second authenticator needs a session signed in with a second factor;
    - locked accounts cannot keep guessing through a session;
    - a password change or reset ends open MFA steps;
    - sign-in identifiers cannot collide across username, e-mail and mobile;
    - scheduled grants count in the takeover check;
    - an unreadable MFA secret fails only that factor.
  - The two low-severity findings it left are known limits 8 and 9 above.
  - A third Sonnet review covered this round (tenant filter, idempotency, partitions, conformance check, the new
    screens and the mocking rule). Fixed and test-covered:
    - a user reaches only their own sign-in sessions; another user's sessions need the permission tenant-wide (404
      otherwise). A user's own factors and sessions are now self-service permissions (iam's permission catalogue);
    - a failure to keep an Idempotency-Key response after the request ran no longer frees the key, and the client
      always gets its answer; 408/425/429 answers are not kept; responses over 1 MB are kept without their body;
      expired keys are purged as the tenant claims new ones;
    - partition maintenance uses one owner connection in UTC with a 5-second lock timeout, drops a month in one
      statement, validates every name, refuses a retention under 12 months or more than 24 months ahead;
    - a timestamp range sent as `< next day` is accepted up to 366 whole days; the web app bounds it in the viewer's
      own time zone;
    - the tenant-predicate parser now requires the predicate at each statement's own level (a sub-select's or an OR
      branch's does not count) and refuses UNION, comma joins, `DELETE … USING` and `UPDATE … FROM`; the
      conformance check R1 uses that parser on every statement, and its other checks were widened (any
      `Transactional`, ORM dependencies in poms, every mock or stub server outside unit tests, schema names in any
      case);
    - the ESLint rule now also catches `import()`, `require`, re-exports, test runners and `vi`/`jest` member access;
    - the MFA secret and recovery codes leave memory once used.
  - Left as known limits: a crash between a request's commit and the keeping of its Idempotency-Key response lets a
    retry run again after the 2-minute lock; a kept response is replayed for 24 hours without re-checking access;
    only list operations require the partition range (a get by id probes every month's index).
  - The assessment batch received one independent review using GPT-6.1 Terra because Fable 5.2 was unavailable in the configured reviewers. All nine actionable findings were fixed. Suggestions not taken: split the native assessment screens into smaller components (no behavior defect depends on the file shape); add a second cancellation-specific test for the test-only Request shim (it removes AbortSignal to avoid the known Node/jsdom mismatch and does not change production transport); split the existing large Vite entry chunk (the bundle stays under the 300 KB gzip budget at 281.8 KB).
- **Assessment batch verification (2026-11-11).** Focused assessment reactor tests: 3 unit tests passed, all 10 assessment test sources compiled; the two PostgreSQL lifecycle component tests could not run without Docker. Conformance checks: 7/7 passed. Client counts: core 120/120 (96.05% lines/statements, 94.87% branches, 99.46% functions), web-kit 11/11, web app 73/73, i18n 7/7 and platform console 13/13. The full client Turbo graph passed 39/39 tasks. Web and console gzip budgets passed at 282.0 KB and 242.9 KB respectively; Vite emitted its advisory about large uncompressed chunks. The full Maven run and the repo-wide Prettier gate remain blocked by the environment/workspace conditions stated above.

## LMS batch (2026-11-17): decisions taken and known limits

Decisions taken on the owner's behalf, within the rules:
- Guests name their school with `X-Tenant-Code` (the code users type at sign-in); the edge resolves it through iam over gRPC (cached 60 s), binds that tenant and serves published rows only, with internal fields left out. An unknown, suspended or missing code is 404; iam unavailable is 503. careers' public job pages use the same mechanism.
- Providers are real adapters behind ports chosen by configuration: Zoom (live classes, meeting events), CloudConvert (e-book conversion), Turnitin (plagiarism), Google Calendar OAuth (evaluator calendars), LTI 1.1 (tool launch and Basic Outcomes; LTI Advantage grade service answers 422). An unconfigured provider answers 503 with a code naming it.
- Provider webhooks are verified before any tenant is bound: `X-Signature` = hex HMAC-SHA256 of `<X-Timestamp>.<raw body>`, 5-minute tolerance; the tenant comes only from the provider-attested payload. The LTI outcome endpoint verifies its own OAuth 1 signature (HMAC-SHA1 with body hash) at the edge, for that operation only.
- Lesson content is stored as blocks (paragraph, heading, list, uploaded file, https link); unknown blocks are dropped and markup is never rendered.
- Graders may delete any draft submission; owners their own.
- Owner decisions (2026-11-17): admission and application numbers stay unique per institution without `tenant_id` in the index; the self-sign-up role is granted for one school, not tenant-wide (in progress).
- Review fixes (2026-11-17): the records a person writes for themself (assignment submissions, forum posts, votes and
  reports, certificate bookings, bookmarks, annotations, lesson notes, content views and ratings, cohort feedback,
  course reviews and interest, enrolment requests, drive items, student alerts' sender) take their owner from the
  token, and their grading, moderation, status and counter fields only from their workflows; the contract marks those
  fields output only and the Update DTOs no longer carry them. Another person's row is 404. A cohort's *teaching
  staff* (its active teachers and who holds lms.cohort.update in its institution) reach its students' submissions,
  quiz attempts and plagiarism reports, and only they grade (gradebook, quiz answers, submissions) or launch a tool as
  LTI Instructor; a student sees correctness, marks and feedback of their own answers only after submitting and as
  the quiz shows them. `allowGuestAccess` false answers every guest read 404 (the sign-in page still gets its
  settings). GetCohort, ListCohorts and ListCohortCourses serve the public open cohorts (published, self-enrolling,
  not ended) in a guest projection; VerifyCertificate serves guests; GetMemberProfile is no longer public (lms keeps
  no names and cannot honour a student's hideStudentInfo for a guest). A quiz attempt ends at its timer or when its
  placement closes (due time, or the end of its late days); `maxViolations` ends it for violations.

Known limits:
- Tablet: no offline cache of lessons; SCORM packages open in the browser (no in-app web view); e-book pages are not rendered in the app (title, conversion state, notes and materials are).
- No course-comparison screen (the operation is served); LMS-20 still composes its reads instead of the served teacher dashboard.
- A paid course answers 409 PAYMENT_REQUIRED (course checkout is not part of finance yet).
- Unread counts carry no notifications or mail until comms serves those reads; cohort announcements are one outbox event that comms will deliver.
- Cross-module happy paths that need the platform file store running (question import by file, course-package import against real files) are covered by unit tests and the 503 paths, not end to end.
- Native screens have no automated test harness.
- An initiated gateway refund is reconciled on retry, on its webhook or before the next refund of the payment; there is no background sweep job yet.
