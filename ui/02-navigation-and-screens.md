# 02 · Navigation and the screen inventory

The information architecture of each client and the catalogue of screens. Every screen lists its web route (also the deep-link path), the clients that carry it, the personas it serves, and the API operations that feed it. Operation names are written `<module>.<Method>` as listed in [`../api/operations.md`](../api/operations.md); each operation's REST path, gRPC method and permission code are there.

## 1. Conventions

### 1.1 Screen anatomy

Every screen is one of five shapes, so components and specs stay uniform:

| Shape | Pattern | Examples |
|---|---|---|
| **Dashboard** | Cards + lists composed from 1–4 query operations | Teacher today, parent child summary |
| **List** | Toolbar (search, filters, sort) + server-paged table/cards + row actions + bulk bar | Students, invoices, applications |
| **Detail** | Header (identity + status + actions) + tabs | Student 360°, cohort home, invoice |
| **Form** | Create/edit, sectioned; drawer for short forms, full page for long; `validateOnly` for pre-flight | Admission form, course settings |
| **Player/Workspace** | Purpose-built interaction surface | Attendance grid, quiz player, gradebook, timetable, calendar, chat |

### 1.2 App shell

- **W**: left navigation (persona-grouped, from permissions) · top bar: global search (`platform.GlobalSearch`), institution switcher, notification bell (`lms.GetUnreadCounts` + `comms.ListInAppNotifications`), persona switcher (`iam.SwitchRole`), profile menu. Content area max-width 1440 px; tables full-width.
- **S/P/T**: bottom tab bar (5 tabs max, per-app trees in §2) + stack navigation; notification bell in the header; P adds the child switcher in the header.
- **B**: landscape-first; left rail (cohort switcher + sections); split view (list → detail side-by-side); prominent offline/sync status chip.
- **C**: same shell as W with a red "platform" environment band; no institution switcher.

### 1.3 Routes and deep links

- Web route = `/app/<module>/<resource path>` mirroring the API path without `/v1`; path params keep API names (`/app/sis/students/{studentId}`).
- Deep link = the same path on the `mysms://` scheme; push notifications carry it in `actionUrl`.
- Read-only views currently exposed as `:verb` GETs (e.g. the gradebook) are routed as sub-paths (`/app/lms/cohorts/{cohortId}/gradebook`); the API is expected to align (ROADMAP P2-17).
- Every client ships one route table (05 §2) mapping path patterns → native screen; unknown paths open the web URL.

### 1.4 The five states (mandatory per screen)

1. **Loading** — skeleton matching the final layout (no spinners for content areas).
2. **Empty** — illustration + one-line explanation + the primary action if the user may create.
3. **Error** — problem+json `title`/`code` rendered with retry; field errors inline on forms (05 §6).
4. **Offline** — cached content with an "offline" chip, or a dedicated offline card when nothing is cached; B specifies per-screen offline behaviour in §3.16.
5. **Content.**

### 1.5 Lists, filters, identity

- Server pagination only (`page`/`size`, following each response's `paging.next` link, api/README "Pagination"); infinite scroll on phones, paged tables on W; `sort` and `filter` map to the declared sortable and filterable fields (ROADMAP P1-33).
- Row identity everywhere is the UUID `id`; `etag` is kept with each fetched row for writes.
- People render through the shared `PersonCard`/avatar component; money through `Money` (04 §5) — never raw strings.

### 1.6 Screen depth (applies to every list manager)

- **List page:** heading with the record count, a filter bar (status, search, the record type's key filters), the
  columns that identify a row, and an empty state that offers the next action ("Add the first class section").
- **A row opens a detail page,** not a dialog: the record's fields in sections, its actions (edit, state steps,
  delete) in the header, and **tabs for its related records** — every record type of the same module that refers to
  it (a student: enrolments, guardians, health, discipline, activities, subject registrations). Editing happens on the
  detail page.
- Names held by another module (a student's name lives in `people`) show once batch card reads exist (P1-34); until
  then the related module's id is shown with a link.

## 2. Navigation trees

### 2.1 W — MySMS Web (groups appear only when permissions allow)

Navigation is organised by **task**, not by table. Daily work comes first; every catalogue and configuration screen
lives in **Setup**, at the bottom, collapsed, so it never crowds the work a role does every day. A group with nothing the
role may open is not shown; an entry the role cannot use is not shown either (never a dead link).

```
Home (per role, §2.1.1)
Students & staff
  Students (360°) · Guardians · Enrolments · Admissions (applications, cycles) · Health · Discipline · Activities ·
  Staff (profile) · Leave · Staff attendance
Academics
  Class sections · Subjects · Teaching assignments · Timetable · Attendance (registers, sheets) ·
  Curriculum & lesson plans · Subject registration (HE)
Assessment
  Homework · Assignments · Exam sessions · Exams · Marksheets & results
Learning (LMS)
  Catalogue · My learning · Teaching (cohorts, live classes) · Question bank · Forums · Certificates · Mentorships ·
  Annotations
Money
  Dues & invoices · Payments & receipts · Staff salaries · Payslips · Payroll payments · Reports
Communication
  Notices · Calendar & events · Messages (outbound, mailbox) · Class pages & feed · Tasks · Feedback · Audience groups ·
  Magazine
Services
  Library (media, cards, loans, reservations, purchase requests) · Transport (student transport) · Front office
  (visitors, calls, post, directory) · Careers
Setup (administrators; collapsed, last)
  Organisation: Institutions · Departments · Academic years · Holidays · Subscription
  Academic structure: Programs · Courses · Batches · Grade levels · Periods
  Catalogues: Leave types · Salary templates · Coupons · Transport routes · External tools · Plagiarism accounts ·
    and every other catalogue (categories, types, reasons, templates)
  Access: Users & roles · Settings by scope · Branding & theme · Notifications setup
  Data: Records (every record type, ADM-11) · Imports & exports · Logs
```

**2.1.1 Home per role.** The first screen after sign-in answers "what do I do now?" for the signed-in role, from the
operations that role may call (a card the role cannot fill is not shown):

| Role | Home shows |
|---|---|
| Administrator (tenant or school) | Counts with a link each: open admission applications, pending leave applications, attendance sheets not yet submitted today, open feedback threads, upcoming events; recent records |
| Teacher | Today's sessions; attendance sheets to take; homework and assignments to grade; my class pages |
| Student / guardian | Today's timetable; homework due; recent results; notices; fees due (guardian) |
| Platform staff (console) | Tenants, open sales enquiries, jobs (C) |

**2.1.2 Find anything.** The top bar has one search box (`/` or Ctrl/⌘-K) that finds screens by name, recent records
and, once `platform.GlobalSearch` is served, records across modules. Recently opened records and the user's
favourite screens are offered first.

### 2.2 S — Student app tabs
`Today` (timetable + due + notices) · `Learn` (courses/cohorts) · `School` (attendance, homework, results, fees, library, transport) · `Inbox` (notifications + messages) · `Me`

### 2.3 P — Parent app tabs
`Home` (child summary) · `Fees` (dues → pay) · `School` (attendance, homework, results, events, transport, library) · `Inbox` · `Me` (children, consent, profile)

### 2.4 T — Teacher app tabs
`Today` (sessions + quick attendance) · `Classes` (my sections → attendance, homework, marks, wall) · `Approvals` (leave, submissions) · `Inbox` · `Me` (leave, payslips, profile)

### 2.5 B — Tablet LMS rail
`Home` · `My courses / cohorts` · per-cohort sections: `Outline · Forum · Gradebook (teacher) · Roster · Live` · `Downloads & Sync` · `Profile`

### 2.6 C — Platform console
`Tenants` · `Plans & Subscriptions` · `Enquiries` · `Global catalogues` (user types, notification types & channels, release condition types, reserved keywords) · `System settings` · `Operations & Jobs`

## 3. Screen inventory

Columns: **ID** · Screen · Web route under `/app` (also the deep link) · Clients · Personas · Key operations · Notes. CRUD = the resource's standard List/Get/Create/Update/Delete operations. Clients use the letters of 01 §2; **all** means every client including the platform console (C). A route may add a query condition (`?mine`, `?view=room`) to name a variant screen on the same path; the variant wins when the condition holds. Screens without a route (sheets, drawers, wizards) name where they appear instead.

### 3.1 Account, shell and session

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| ACC-01 | Sign in | `/auth/login` | all | all | `iam.Login` | Branding via public `platform.GetBranding`; MFA step via `iam.CompleteMfa` when `mfaRequired` |
| ACC-02 | Forgot / reset password | `/auth/reset` | all | all | `iam.RequestPasswordReset`, `iam.ResetPassword` | Always-204 messaging |
| ACC-03 | Sign up (LMS self-serve) | `/auth/signup` | W B | external student | `iam.SignUp`, `iam.ListSignUpInstitutions`, `iam.CheckUsernameAvailable` | Only when tenant allows; the student picks the school |
| ACC-04 | Accept terms | `/auth/terms` | all | all | `iam.AcceptTerms` | Blocking interstitial |
| ACC-05 | Verify email / mobile | `/auth/verify` | all | all | `iam.RequestVerification`, `iam.ConfirmVerification` | |
| ACC-06 | Persona switcher | (sheet) | all | multi-role | `iam.GetMe`, `iam.SwitchRole` | Re-issues tokens; shell re-renders |
| ACC-07 | My profile | `/people/me` | all | all | `iam.GetMe`, `people.GetMyPerson`, `people.UpdateMyPerson`, `platform.CreateUploadUrl` | Contact and self-description fields only (names and identity are changed by administrators); photo via 2-step upload |
| ACC-08 | Security & sessions | `/iam/me/security` | W | all | `iam.ChangePassword`, `iam.ListUserSessions`, `iam.ListUserDevices`, `iam.ListAuthTokens`, `iam.CreateApiToken`, `iam.EnrollMfa`, `iam.ListMfaCredentials`, `iam.DisableMfa` | Token and MFA secrets shown once |
| ACC-09 | Notification inbox | `/comms/in-app-notifications` | all | all | `comms.ListInAppNotifications`, `comms.MarkNotificationRead`, `comms.MarkAllNotificationsRead`, `comms.ArchiveNotifications` | Tap → deep link |
| ACC-10 | Notification preferences | `/comms/notification-preferences` | all | all | `comms.ListNotificationPreferenceSets`, `comms.ListNotificationPreferences`, `comms.UpdateNotificationPreference` | Per role; quiet hours pending E12 |
| ACC-11 | Global search | `/platform/search` | W | staff | `platform.GlobalSearch` | Results deep-link |
| ACC-12 | Public profile editor | `/people/public-profiles/{publicProfileId}` | W | student, alumni | `people.UpdatePublicProfile` | Gated by the person's `public_profile` consent (privacy) |
| ACC-13 | Consent centre | `/privacy/my/consents` | W P S B | guardian, student | `privacy.ListMyConsents`, `privacy.RecordMyConsent`, `privacy.WithdrawMyConsent`, `privacy.GetMyNotice`, `sis.ListMyChildren` (the children's names) | One card per purpose the school asks about: the notice in the user's language, the decision, "currently shared with" (widest allowed width), withdraw. P: per child (the child switcher). S/B: a student under 18 sees what their guardians decided, read-only, with "you can decide from your 18th birthday"; from 18 the student decides. Design PRIVACY-AND-CONSENT-CENTRE §12 |
| ACC-14 | My settings | `/iam/me/settings` | all | all | `iam.ListMySettings`, `iam.SetMySetting`, `iam.ResetMySetting`, `iam.ListSettingDefinitions` | Appearance, language, navigation, notifications; each row shows where its value comes from (default, school, department, class, mine) and locked values cannot be changed; settings only administrators may change are not shown to other users (04 §9 rule 8) |
| ACC-15 | My privacy requests | `/privacy/my/requests` | W P S | guardian, student, staff | `privacy.SubmitMyRequest`, `privacy.ListMyRequests`, `privacy.GetMyRequest`, `privacy.GetMyAccessSummary`, `privacy.GetMyNominee`, `privacy.SetMyNominee`, `privacy.RevokeMyNominee`, `sis.ListMyChildren` | New request (see my data, correct my data, complain) about oneself or a child one is the legal guardian of; a correction names the record and what should change, a complaint its category; each request's deadline, outcome and the school's answer; a fulfilled access request's summary. An adult names, changes or removes their nominee (name and contact shown masked). Design PRIVACY-AND-CONSENT-CENTRE §9.6, §12 |

### 3.2 Administration (org, iam, platform)

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| ADM-01 | Admin dashboard | `/org/overview` | W | tenant admin | composed queries | Counts, pending approvals, jobs |
| ADM-02 | Institutions | `/org/institutions` | W | tenant admin | CRUD, `org.FindInstitutions`, `org.ListInstitutionAttributes` | Tree by parent |
| ADM-03 | Institution detail & attributes | `/org/institutions/{institutionId}` | W | tenant admin | `org.GetInstitution`, `org.UpdateInstitution`, attribute CRUD | Type-driven attribute form |
| ADM-04 | Departments | `/org/departments` | W | tenant admin | CRUD | |
| ADM-05 | Academic years & terms | `/org/academic-years` | W | tenant admin | CRUD, `org.ActivateAcademicYear`, `org.CloseAcademicYear`, `org.GenerateTerms` | Year switcher feeds all academics screens |
| ADM-06 | Holidays | `/org/holidays` | W | tenant admin | CRUD | Working-day kinds pending (S7) |
| ADM-07 | Branding & theme | `/platform/themes` | W | tenant admin | theme CRUD, `platform.GetBranding` | Live token preview (04 §2) |
| ADM-08 | Users | `/iam/user-accounts` | W | tenant admin | CRUD, `iam.ActivateUser`, `iam.DeactivateUser`, `iam.UnlockUser`, `iam.ForcePasswordReset`, `iam.SetUserCredentials` | |
| ADM-09 | User detail & grants | `/iam/user-accounts/{userAccountId}` | W | tenant admin | `iam.ListUserRoles`, `iam.SetUserRoles`, `iam.StartImpersonation` | Impersonation shows persistent banner + `iam.StopImpersonation` |
| ADM-10 | Roles & permissions | `/iam/roles` | W | tenant admin | role CRUD, `iam.ListPermissions`, role-permission edit | Becomes `SetRolePermissions` after P1-24 |
| ADM-11 | Records | `/admin/records` | W | tenant admin, institution admin | the standard operations and state transitions of every resource in `api/standard-resources.json` | One generic list-manager UI for all pick-lists and records (04 §5): only the record types the role may list, create/edit/delete only with their permissions, and on a row only the state transitions (`api/state-machines.md`) its permissions and the row's state allow; the server applies the institution scope and keeps tenant-level records to tenant-wide administrators |
| ADM-12 | Imports & exports | `/platform/import-export-jobs` | W | tenant admin | `lms.ListImportExportJobs`, `platform.StartImport`, `iam.ImportUsers`, `lms.GetJobPackage`, `lms.CancelImportExportJob` | Job progress via operations |
| ADM-13 | Audit & access logs | `/platform/audit-logs` | W | tenant admin | `platform.ListAuditLogs`, `lms.ListAccessLogs` | Time-range filter mandatory (partitioned) |
| ADM-14 | Subscription & billing | `/platform/tenant-subscriptions` | W | tenant admin | `platform.ListTenantSubscriptions`, `platform.ListSaasPlans`, `platform.StartSubscriptionCheckout`, `platform.CancelSubscription` | |
| ADM-15 | Tenant settings | `/platform/tenant` | W | tenant admin | pending P1-28 (`GET/PATCH /platform/tenant`) | Locale, timezone, currency |
| ADM-16 | Settings by scope | `/iam/setting-values` | W | tenant admin, department head | SettingValue CRUD, `iam.ListSettingDefinitions` | Scope picker tenant → institution → department → class section → user; lock toggle; a scoped administrator sees and edits only the units of their grants (04 §9) |
| ADM-17 | Privacy purposes | `/privacy/purposes` | W | tenant admin | `privacy.ListPurposes`, `privacy.GetPurpose`, `privacy.UpdatePurpose` | The purpose catalogue with the school's switch, width ceiling and notice per purpose; a platform purpose or one forbidden for minors is shown, not switchable where the rules say so |
| ADM-18 | Privacy notices | `/privacy/notices` | W | tenant admin | `privacy.ListNotices`, `privacy.CreateNotice`, `privacy.UpdateNotice`, `privacy.DeleteNotice`, `privacy.ListNoticeVersions`, `privacy.CreateNoticeVersion`, `privacy.UpdateNoticeVersion`, `privacy.PublishNoticeVersion`, `privacy.ListNoticeTranslations`, `privacy.CreateNoticeTranslation`, `privacy.UpdateNoticeTranslation`, `privacy.DeleteNoticeTranslation` | Notice → versions → translations; a draft is edited, a published version is frozen; publishing can ask everyone again (re-consent) |
| ADM-19 | Privacy requests | `/privacy/rights-requests` | W | privacy officer, tenant admin | `privacy.ListRightsRequests`, `privacy.GetRightsRequest`, `privacy.GetAccessSummary`, `privacy.VerifyRightsRequestIdentity`, `privacy.AssignRightsRequest`, `privacy.ExtendRightsRequest`, `privacy.RejectRightsRequest`, `privacy.CloseRightsRequest`, `privacy.FulfilAccessRequest`, `privacy.StartRightsRequestFulfilment`, `privacy.FulfilWithdrawAllRequest`, `privacy.RetryRightsRequestTask` | The open requests, nearest deadline first, coloured overdue / due soon / on track; a request's details (a correction's record and fields, a grievance's category) and the steps its state allows; closing writes the answer the requester sees (required for a grievance and a declined correction); an access request is answered with the previewed summary; an erasure or export request's fulfilment is started and its tasks shown per module (status, attempts, last error) with a retry of a failed or unanswered task; a withdraw-all request is fulfilled in one step. A request the officer made or is about is refused (conflict of interest) |
| ADM-20 | Breach register | `/privacy/breaches` | W | privacy officer, tenant admin | `privacy.ListBreaches`, `privacy.GetBreach`, `privacy.ReportBreach`, `privacy.UpdateBreach`, `privacy.TriageBreach`, `privacy.ContainBreach`, `privacy.RecordBreachNotification`, `privacy.CloseBreach`, `privacy.MarkBreachNotABreach` | Breaches with their status and the Board report clock (due, overdue, hours remaining), filtered open / overdue / all; a breach's details and the steps its status allows: triage, contain, record a notification per audience and kind, close, mark not a breach (with a reason); report a breach; its details edited while open |
| ADM-21 | Legal holds | `/privacy/legal-holds` | W | privacy officer, tenant admin | `privacy.ListLegalHolds`, `privacy.GetLegalHold`, `privacy.PlaceLegalHold`, `privacy.UpdateLegalHold`, `privacy.ReleaseLegalHold` | Active holds (review due first) or all; place a hold on a subject, a data category or named records of a module, with its reason and review date; move the review date; release. A held record is kept by erasure and retention |
| ADM-22 | Retention | `/privacy/retention` | W | privacy officer, tenant admin | `privacy.ListRetentionPolicies`, `privacy.GetRetentionPolicy`, `privacy.UpdateRetentionPolicy`, `privacy.ListRetentionRuns` | The retention policies with the school's setting of each (period in days, never below the policy's floor; anonymise, archive or delete), saved under the row's ETag; the runs with their cutoff, status and counts |

### 3.3 People & students (people, sis)

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| SIS-01 | Students | `/sis/students` | W | admin, teacher, office | `sis.ListStudents`, `sis.CreateStudent` | Filters: year, section, status, category |
| SIS-02 | Student 360° | `/sis/students/{studentId}` | W T | admin, teacher | `sis.GetStudent`, `sis.GetStudentSummary` + per-tab lists | Tabs: profile, guardians, enrolment, attendance, homework, results, fees, documents, health, discipline, activity, transport, library |
| SIS-03 | Enrolment & promotion | `/sis/student-enrollments` | W | admin | enrolment CRUD, `sis.ProposePromotions`, `sis.ApplyPromotions`, `sis.ApprovePromotion`, `sis.RejectPromotion` | Promotion wizard (list → review → apply) |
| SIS-04 | Guardians | `/sis/guardians` | W | admin, office | CRUD, `sis.ListStudentGuardians` | Link to students with relationship type |
| SIS-05 | Health records | `/sis/student-health-records` | W | admin, nurse | CRUD | Sensitive-permission gated (separate read permission; reads audited) |
| SIS-06 | Discipline | `/sis/discipline-incidents` | W T | admin, teacher | CRUD, `sis.MarkIncidentActionTaken`, `sis.MarkIncidentSeen` | Guardian visibility flag |
| SIS-07 | Activities | `/sis/student-activities` | W | teacher | CRUD | |
| SIS-08 | Documents | `/people/person-documents` | W | admin | CRUD + uploads | Per person/student |
| SIS-09 | Bonafide certificate | (action in SIS-02) | W | admin | `sis.IssueBonafideCertificate` | Returns document |
| SIS-10 | Subject registration (HE) | `/sis/subject-registrations` | W S | teacher (HE), student | CRUD, `sis.SubmitSubjectRegistration`, `sis.ApproveSubjectRegistration`, `sis.RejectSubjectRegistration`, `sis.ResetSubjectRegistration`, `sis.LoadCourseSubjects` | Student submits; teacher approves |
| SIS-11 | My children | `/sis/students?mine` | P | guardian | `sis.ListMyChildren`, `sis.GetStudentSummary` | P home data source |
| SIS-12 | Birthdays | `/people/birthdays` | W | admin, teacher | `people.ListBirthdays`, `people.SendGreetings` | |

### 3.4 Admission

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| ADMN-01 | Admission cycles | `/admission/admission-cycles` | W | admissions | CRUD, `admission.StartAdmission`, `admission.OpenApplications`, `admission.CloseAdmissionCycle`, `admission.ConfirmAdmissionCycle`, `admission.CancelAdmissionCycle`, `admission.ResetAdmissionCycle` | Status timeline header |
| ADMN-02 | Applications pipeline | `/admission/applications` | W | admissions | `admission.ListApplications` | Kanban by status + table toggle |
| ADMN-03 | Application detail | `/admission/applications/{applicationId}` | W | admissions | `admission.GetApplication`, `admission.ApproveApplication`, `admission.RejectApplication`, `admission.MarkApplicationPending`, `admission.ConfirmApplication`, `admission.ConfirmAdmission`, `admission.CancelApplication`, `admission.ResetApplication` | Full form review; guardians & contacts |
| ADMN-04 | Enrol applicant | (wizard from ADMN-03) | W | admissions | `admission.EnrollApplicant` | Section/batch + roll no; returns the application with `studentId`, portal accounts as an operation |
| ADMN-05 | Public application form | `/admission/apply/{admissionCycleId}` | W (public) | applicant | `admission.CreateApplication`, `admission.SubmitApplication`, uploads | Multi-step, save-and-resume |

### 3.5 Academics

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| ACA-01 | Programs | `/academics/programs` | W | admin | CRUD | |
| ACA-02 | Courses & subjects (HE) | `/academics/courses` | W | admin, teacher (HE) | course CRUD, course-subject edit | Term/credits per subject (course-subject rows) |
| ACA-03 | Batches | `/academics/batches` | W | admin | CRUD | |
| ACA-04 | Grade levels & sections | `/academics/grade-levels` | W | admin | CRUD both lists | |
| ACA-05 | Class sections | `/academics/class-sections` | W | admin | CRUD | Class teacher, capacity, stream |
| ACA-06 | Class section workspace | `/academics/class-sections/{classSectionId}` | W T | teacher, admin | roster, class subjects CRUD, `academics.ListTeachingAssignments` | Tabs: students, subjects & teachers, timetable, attendance, homework, wall |
| ACA-07 | Subjects | `/academics/subjects` | W | admin | CRUD | |
| ACA-08 | Teaching assignments | `/academics/teaching-assignments` | W | admin | CRUD | Periods/week tracking |
| ACA-09 | Curriculum units | `/academics/curriculum-units` | W | teacher | CRUD | Per class subject |
| ACA-10 | Lesson plans | `/academics/lesson-plans` | W | teacher, head | CRUD, `academics.PublishLessonPlan`, `academics.ApproveLessonPlan`, `academics.RejectLessonPlan` | Approval queue for heads |

### 3.6 Timetable

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| TT-01 | Periods | `/timetable/periods` | W | admin | CRUD | |
| TT-02 | Timetable builder | `/timetable/sessions` | W | admin | `timetable.GenerateSessions`, session CRUD, `timetable.BatchConfirmSessions`, `timetable.ConfirmSession`, `timetable.CancelSession`, `timetable.ResetSession`, `timetable.CompleteSession` | Week grid; clash warnings server-side |
| TT-03 | My timetable | `/timetable/my` | W S T | student, teacher | `timetable.GetTimetable` | S: own; T: teaching; P: per child |
| TT-04 | Room/staff view | `/timetable/sessions?view=room` | W | admin | `timetable.ListSessions` | Filters by room/staff |

### 3.7 Attendance

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| ATT-01 | Registers | `/attendance/attendance-registers` | W | admin | CRUD | Scope: section / course-batch / subject |
| ATT-02 | Sheets | `/attendance/attendance-sheets` | W T | teacher | `attendance.ListAttendanceSheets`, `attendance.CreateAttendanceSheet`, `attendance.StartAttendanceSheet`, `attendance.CancelAttendanceSheet`, `attendance.ResetAttendanceSheet` | Day/part filters |
| ATT-03 | **Take attendance** | `/attendance/attendance-sheets/{attendanceSheetId}` | T W | teacher | `attendance.TakeAttendanceForSession`, `attendance.RecordAttendance`, `attendance.CompleteAttendanceSheet` | Roster grid, one-tap status, reasons; T offline-tolerant queue (05 §7.4) |
| ATT-04 | Student attendance summary | `/attendance/summary/{studentId}` | W S P T | all | `attendance.SummarizeAttendance`, `attendance.ListAttendanceRecords` | Month heatmap + % |
| ATT-05 | Class summary & defaulters | `/attendance/summary` | W | admin, teacher | `attendance.SummarizeAttendance` | Threshold filters |
| ATT-06 | Staff attendance | `/attendance/staff` | W | HR | `attendance.ListStaffAttendances`, `attendance.CreateStaffAttendance`, `attendance.UpdateStaffAttendance`, `hr.ListLeaveBalances` | Day grid per staff; biometric rows read-only |

### 3.8 Assessment (homework, assignments, exams, results)

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| ASM-01 | Homework board | `/assessment/homeworks` | W T S P B | teacher, student, parent | `assessment.ListHomeworks`, CRUD, `assessment.PublishHomework` | T: mine; S/P: by section |
| ASM-02 | Homework approval | `/assessment/homeworks?status=pending` | W B | head | `assessment.ApproveHomework`, `assessment.RejectHomework` | Queue |
| ASM-03 | Homework check | `/assessment/homeworks/{homeworkId}` | W T B | teacher | `assessment.ListHomeworkSubmissions`, `assessment.CheckHomeworkSubmission`, `assessment.ReplyToHomeworkFeedback` | Per-student checklist |
| ASM-04 | Homework submit | (S detail) | W S B | student | `assessment.CreateHomeworkSubmission` + uploads | |
| ASM-05 | Assignments (official) | `/assessment/assignments` | W T B | teacher | CRUD, `assessment.PublishAssignment`, `assessment.ApproveAssignment`, `assessment.RejectAssignment`, `assessment.FinishAssignment`, `assessment.CancelAssignment`, `assessment.ResetAssignment` | Allocation to students |
| ASM-06 | Assignment grading | `/assessment/assignments/{assignmentId}` | W T B | teacher | `assessment.ListAssignmentSubmissions`, `assessment.GradeAssignmentSubmission`, `assessment.AcceptAssignmentSubmission`, `assessment.RejectAssignmentSubmission`, `assessment.RequestSubmissionChanges` | |
| ASM-07 | Assignment submit | (S detail) | W S B | student | `assessment.SubmitAssignmentSubmission`, `assessment.ResetAssignmentSubmission` | |
| ASM-08 | Exam sessions | `/assessment/exam-sessions` | W B | exam cell | CRUD, `assessment.ScheduleExamSession`, `assessment.MarkExamSessionHeld`, `assessment.CompleteExamSession`, `assessment.CancelExamSession`, `assessment.ResetExamSession` | |
| ASM-09 | Exams (papers) | `/assessment/exams` | W B | exam cell | CRUD, `assessment.ScheduleExam`, `assessment.MarkExamHeld`, `assessment.CompleteExam`, `assessment.CancelExam`, `assessment.ResetExam`, `assessment.PopulateExamResults`, `assessment.AllocateExamRooms` | Invigilators, rooms |
| ASM-10 | **Marks entry** | `/assessment/exams/{examId}/marks` | W T B | teacher | `assessment.RecordExamMarks`, `assessment.MarkExamResultsUpdated` | Grid; per-row errors in the batch result (P1-31 shape) |
| ASM-11 | Result templates & marksheets | `/assessment/marksheets` | W B | exam cell | `assessment.GenerateResults`, `assessment.ValidateMarksheet`, `assessment.CancelMarksheet`, `assessment.ResetMarksheet`, marksheet lists | Validation = publish gate |
| ASM-12 | Results (student/parent) | `/assessment/results/{studentId}` | W S P B | student, parent | marksheet/result lists | Published only |
| ASM-13 | Grading schemes | `/assessment/grading-schemes` | W B | admin | CRUD + bands | |

### 3.9 Finance (fees, invoices, payments)

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| FIN-01 | Fee setup | `/finance/fee-heads` + `/finance/fee-plans` + `/finance/fee-structures` | W | accountant | CRUD all three, `finance.PublishFeeStructure`; concessions via `finance.*FeeConcession*` | Publish generates dues |
| FIN-02 | Generate dues | `/finance/student-fee-dues:generate` | W | accountant | `finance.GenerateFeeDues` | 202 + operation progress |
| FIN-03 | Dues ledger | `/finance/student-fee-dues` | W | accountant | `finance.ListStudentFeeDues`, `finance.CreateInvoiceFromDues` | Filters: year, head, status |
| FIN-04 | Invoices | `/finance/invoices` | W | accountant | CRUD, `finance.PostInvoice`, `finance.CancelInvoice` | Posted = immutable; corrections by credit note |
| FIN-05 | Invoice detail | `/finance/invoices/{invoiceId}` | W | accountant | `finance.GetInvoice`, lines list | Print/PDF |
| FIN-06 | Payments & receipts | `/finance/payments` | W | accountant | `finance.ListPayments`, `finance.RecordPayment`, `finance.RefundPayment`, `finance.ListRefunds` | Counter payments with allocations; receipt number from the server |
| FIN-07 | **Pay fees (parent)** | `/finance/pay` | P W S | guardian, student | `finance.GetFeeStatement`, `finance.QuoteCheckout`, `finance.StartCheckout` | Flow F4; idempotency key mandatory |
| FIN-08 | Fee statement | `/finance/statement/{studentId}` | W P S | guardian | `finance.GetFeeStatement` | Downloadable |
| FIN-09 | Coupons | `/finance/coupons` | W | accountant | CRUD + scopes | |
| FIN-10 | Reminders | `/finance/reminders` | W | accountant | `finance.SendFeeReminders` | Audience preview |

### 3.10 HR & payroll

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| HR-01 | Staff directory | `/hr/staff` | W | HR, all staff (read) | CRUD, `hr.ChangeStaffStatus`, `hr.CreateStaffPortalAccount` | |
| HR-02 | Staff detail | `/hr/staff/{staffId}` | W | HR | tabs: profile, departments/subjects, leave, salary, documents | |
| HR-03 | Leave types & balance | `/hr/leave-types` | W T | HR, staff | CRUD, `hr.GetStaffLeaveBalance`, `hr.ListLeaveBalances` | |
| HR-04 | Leave applications | `/hr/leave-applications` | W T | staff, approver | CRUD, `hr.ApproveLeave`, `hr.RejectLeave`, `hr.CancelLeave` | T: apply + approve queue |
| HR-05 | Salary templates | `/payroll/salary-templates` | W | payroll | CRUD + components | |
| HR-06 | Staff salary | `/payroll/staff-salaries` | W | payroll | CRUD | Effective-dated |
| HR-07 | **Payslip run** | `/payroll/payslips` | W | payroll | `payroll.GeneratePayslips`, `payroll.MarkPayslipPaid`, `payroll.CancelPayslip` | Period picker → 202 operation → review |
| HR-08 | My payslips | `/payroll/payslips?mine` | W T | staff | `payroll.ListPayslips` | PDF download |
| HR-09 | Payroll payments | `/payroll/payroll-payments` | W | payroll | CRUD, disbursement accounts | |

### 3.11 Library

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| LIB-01 | Catalogue | `/library/media` | W | librarian, all (read) | CRUD, copies (`media-units`) | Search by title/author/ISBN |
| LIB-02 | Title detail | `/library/media/{mediaId}` | W | librarian | `library.GetMedia`, units, loans history | |
| LIB-03 | **Circulation desk** | `/library/loans` | W | librarian | `library.CreateLoan`, `library.ReturnLoan`, `library.RequestLoanReturn`, `library.MarkLoanLost`, `library.InvoiceLoanFine` | Scan card + copy barcode |
| LIB-04 | Cards | `/library/library-cards` | W | librarian | CRUD + card types | |
| LIB-05 | Reservations | `/library/reservations` | W S | librarian, member | CRUD, `library.AcceptReservation`, `library.RejectReservation`, `library.RequestReservationAgain` | |
| LIB-06 | Purchase requests | `/library/purchase-requests` | W | librarian | CRUD, `library.SubmitPurchaseRequest`, `library.AcceptPurchaseRequest`, `library.RejectPurchaseRequest` | |
| LIB-07 | My loans | `/library/loans?mine` | W S P T | member | `library.ListLoans` | P: per child |

### 3.12 Transport & front office

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| TRN-01 | Routes & stops | `/transport/routes` | W | transport | CRUD + stops | |
| TRN-02 | Student transport | `/transport/student-transports` | W | transport | CRUD | Bus pass print |
| TRN-03 | My route | `/transport/my` | S P | student, parent | `transport.ListStudentTransports` | |
| FO-01 | Visitor log | `/frontoffice/visitor-logs` | W | front desk | CRUD | Badge print |
| FO-02 | Call log | `/frontoffice/call-logs` | W | front desk | CRUD | |
| FO-03 | Postal log | `/frontoffice/postal-records` | W | front desk | CRUD | |
| FO-04 | Directory | `/frontoffice/directory-contacts` | W | front desk | CRUD | |

### 3.13 Communication & community (comms)

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| COM-01 | Notice board | `/comms/notices` | all | all | `comms.ListNotices` | Audience-filtered server-side |
| COM-02 | Notice composer | `/comms/notices/{noticeId}` | W T | staff | CRUD, audiences, `comms.PublishNotice`, `comms.UnpublishNotice`, `comms.DeactivateNotice`, `comms.ReactivateNotice`, `comms.CloseNoticeReplies` | Replies via linked forum topic |
| COM-03 | **My calendar** | `/comms/calendar` | all | all | `comms.GetMyCalendar`, `comms.ExportCalendar`, `comms.CancelEventOccurrence`, `comms.CompletePersonalEvent` | Merges school/cohort/personal; month-week-day |
| COM-04 | Events & gallery | `/comms/events` | W S P | staff, all | event CRUD, `comms.ActivateEvent`, `comms.DeactivateEvent`, gallery items | RSVP via attendees |
| COM-05 | Conversations | `/comms/conversations` | all | all | `comms.ListConversations`, `comms.CreateConversation`, `comms.ListConversationMessages`, `comms.CreateConversationMessage`, `comms.MarkConversationRead`, `comms.HideMessageForMe` | Chat UI; poll/refresh (no socket v1) |
| COM-06 | Broadcast / emergency | `/comms/outbound-messages` | W | admin | `comms.SendMessage`, `comms.ListOutboundMessages`, `comms.RetryOutboundMessage`, `comms.CancelOutboundMessage`, `comms.SendTestMessage` | Recipients builder + preview |
| COM-07 | Feedback threads | `/comms/feedback-threads` | W P | guardian, staff | CRUD, `comms.MarkFeedbackMessage`, `comms.CloseFeedbackThread`, `comms.ReopenFeedbackThread` | |
| COM-08 | Class pages & feed | `/comms/class-pages` | W S P T | all | pages CRUD, `comms.GetFeed`, posts CRUD, `comms.SubmitPost`, `comms.ApprovePost`, `comms.CancelPost`, reactions, comments, `comms.FollowClassPage` | Moderated posting |
| COM-09 | Tasks | `/comms/tasks` | W T | staff | CRUD, `comms.ClaimTask`, `comms.CompleteTask`, `comms.ReopenTask`, `comms.SnoozeTask` | Assignee kinds: user/section/group |
| COM-10 | Audience groups | `/comms/audience-groups` | W | admin | CRUD, `comms.AddGroupMembers` | |
| COM-11 | Mailbox | `/comms/mailbox-messages` | W | staff | `comms.ListMailboxMessages`, `comms.ComposeMail`, `comms.MarkMailRead` | |
| COM-12 | Magazine / bulletins | `/comms/magazines` | W | admin | CRUD | |
| COM-13 | Notification setup (tenant) | `/comms/notification-rules` | W | admin | rules/channels/templates CRUD | Channel secrets write-only |

### 3.14 Careers

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| CAR-01 | Job board | `/careers/job-openings` | W (public) | candidate | `careers.ListJobOpenings` (public) | |
| CAR-02 | Opening detail + apply | `/careers/job-openings/{jobOpeningId}` | W | candidate | `careers.GetJobOpening`, `careers.CreateJobApplication` + resume upload | |
| CAR-03 | Employer console | `/careers/job-openings?mine` | W | poster | CRUD, `careers.CloseJobOpening`, `careers.ReopenJobOpening`, `careers.UpdateApplicationStatus`, `careers.EmailApplicant` | Applicant pipeline |
| CAR-04 | My applications & preferences | `/careers/job-applications?mine` | W | candidate | applications list, candidate-preference CRUD | |

### 3.15 LMS — learn, teach, author

**Catalogue & enrolment**

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| LMS-01 | Catalogue home | `/lms/catalog` | W S B (public) | student | `lms.GetCatalogHome`, `lms.ListCategories` | Banners, featured, upcoming |
| LMS-02 | Course page | `/lms/courses/{courseId}/catalog` | W S B (public) | student | `lms.GetLmsCourse`, `lms.GetCourseSyllabus`, `lms.ListCourseReviews`, `lms.CompareCourses` | Authors, previews, testimonials; public profile limited to consented card fields |
| LMS-03 | Enrol / buy | (sheet on LMS-02) | W S B | student | `lms.EnrollInCourse`, `lms.EnrollInCohort`, `lms.CreateEnrollmentRequest`, `finance.QuoteCheckout`, `finance.StartCheckout` | Free vs paid vs request |
| LMS-04 | Student dashboard | `/lms/home` | W S B | student | `lms.GetStudentDashboard`, `lms.GetUnreadCounts` | Continue-learning cards |

**Learning**

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| LMS-05 | Course home / outline | `/lms/courses/{courseId}` | W S B | student | `lms.GetCourseOutline`, `lms.ListLessonProgress` | Progress ring per chapter |
| LMS-06 | **Lesson player** | `/lms/lessons/{lessonId}` | W S B | student | `lms.GetLessonContent`, `lms.MarkLessonComplete`, `lms.TrackVideoWatch`, notes via annotations | Block renderer (04 §5); B caches |
| LMS-07 | SCORM player | `/lms/chapters/{chapterId}/scorm` | W B | student | `lms.GetScormLaunch`, `lms.SaveScormProgress` | Suspend/resume |
| LMS-08 | **Quiz player** | `/lms/content-items/{contentItemId}/quiz` | W S B | student | `lms.StartQuizAttempt`, `lms.SaveQuizResponses`, `lms.PauseQuizAttempt`, `lms.ResumeQuizAttempt`, `lms.SubmitQuiz`, `lms.CheckQuizAnswer` | Timer, autosave, offline (05 §7); proctor events |
| LMS-09 | Quiz result & review | `/lms/quiz-submissions/{quizSubmissionId}` | W S B | student | `lms.GetQuizSubmission`, `lms.ListQuizAnswers` | Feedback per quiz settings |
| LMS-10 | LMS assignment | `/lms/content-items/{contentItemId}/assignment` | W S B | student | `lms.GetLmsAssignment`, `lms.CreateLmsAssignmentSubmission`, `lms.FinalizeSubmission`, `lms.SubmitForPlagiarismCheck` | Drafts until final |
| LMS-11 | Group workspace | `/lms/assignment-groups/{assignmentGroupId}` | W B | student | `lms.RequestToJoinGroup`, `lms.AcceptGroupRequest`, `lms.RejectGroupRequest`, `lms.SetGroupMembers`, `lms.SubmitPeerReview` | |
| LMS-12 | Programming exercise | `/lms/programming-exercises/{programmingExerciseId}` | W | student | `lms.GetProgrammingExercise`, `lms.SubmitExercise` | Code editor + test results |
| LMS-13 | E-book reader | `/lms/ebooks/{ebookId}` | W B | student | `lms.GetEbook`, annotations CRUD, `lms.GetMyRelatedMaterials`, `lms.RecordItemView` | Highlights, notes, share |
| LMS-14 | My notes & bookmarks | `/lms/annotations` | W S B | student | `lms.ListAnnotations`, bookmarks CRUD | Cross-content list |
| LMS-15 | Certificates & badges | `/lms/certificates?mine` | W S B | student | `lms.ListCertificates`, `lms.DownloadCertificate`, `lms.IssueMyCertificate`, `lms.ListBadgeAwards` | Public verify: `lms.VerifyCertificate` |
| LMS-16 | Evaluation booking | `/lms/certificate-requests` | W | student, evaluator | `lms.GetEvaluatorSlots`, `lms.CreateCertificateRequest`, `lms.CancelCertificateRequest`, evaluator: `lms.ListCertificateEvaluations`, `lms.IssueCertificateFromEvaluation` | |
| LMS-17 | Learning programs | `/lms/learning-programs` | W | student | list/detail, `lms.EnrollInLearningProgram` | Ordered course path |

**Cohort (teacher-led delivery)**

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| LMS-20 | Teaching dashboard | `/lms/teaching` | W B | teacher | `lms.GetTeacherDashboard` | To-grade, upcoming, alerts |
| LMS-21 | Cohort home | `/lms/cohorts/{cohortId}` | W S B | member | `lms.GetCohort`, `lms.GetCohortSyllabus`, `lms.GetCohortProgress` | Tabs below |
| LMS-22 | Cohort outline editor | `/lms/cohorts/{cohortId}/outline` | W B◐ | teacher | chapters/content-items CRUD, `lms.MoveChapter`, `lms.MoveContentItem`, `lms.CopyContentItem`, `lms.AddFromRepository`, `lms.SetContentItemVisibility`, `lms.CopyCourseOutlineToCohort` | Dates, visibility windows, locks |
| LMS-23 | Roster | `/lms/cohorts/{cohortId}/roster` | W B | teacher | `lms.GetCohortRoster`, `lms.SetCohortMembers`, `lms.ImportCohortMembers`, `lms.SetStudentAccess`, `lms.BatchEnrollInCohort` | Students + staff + observers |
| LMS-24 | Cohort groups | `/lms/cohort-groups` | W | teacher | CRUD | |
| LMS-25 | Schedule & sessions | `/lms/cohorts/{cohortId}/schedule` | W B | member | schedule slots, sessions, holidays CRUD, `lms.GetCohortTimetable`, `lms.ApplyTimetableTemplate` | |
| LMS-26 | **Forums** | `/lms/cohorts/{cohortId}/forums` | W S B | member | `lms.ListForums`, `lms.ListForumTopics`, `lms.GetForumTopic`, `lms.CreateForumPost`, votes, `lms.MarkTopicRead`, `lms.FlagForumPost`, moderation (`lms.HideForumPost`, `lms.HideForumTopic`, `lms.ConfirmPostAbuse`, `lms.MarkPostSubstantive`) | Threaded; graded topics link gradebook |
| LMS-27 | Live classes | `/lms/live-classes` | W S B | member | CRUD, `lms.ScheduleLiveClass`, `lms.JoinLiveClass`, `lms.GetLiveClassStartUrl`, `lms.EndLiveClass`, `lms.SyncLiveClassAttendance` | Provider-agnostic join |
| LMS-28 | Announcements (cohort) | via COM-02 scoped | W S B | teacher | notices with cohort audience | |
| LMS-29 | Release rules | `/lms/cohorts/{cohortId}/release-rules` | W | teacher | CRUD, `lms.EnableReleaseRule`, `lms.DisableReleaseRule`, `lms.CopyReleaseRule`, `lms.ReorderReleaseRules`, `lms.SetReleaseRulesEnabled`, `lms.EvaluateReleaseRules`, `lms.GetReleaseStatus`, `lms.GrantItemAccess` | Rule builder |
| LMS-30 | Student alerts & analytics | `/lms/cohorts/{cohortId}/analytics` | W | teacher | `lms.GetCohortPerformance`, `lms.ListStudentAlerts`, `lms.GetCourseProgressDistribution` | |

**Gradebook & grading**

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| LMS-31 | **Gradebook** | `/lms/cohorts/{cohortId}/gradebook` | W B | teacher | `lms.GetGradebook` (paged, 304), `lms.ExportGrades`, `lms.PublishItemGrades`, `lms.PublishFinalGrades` | Virtualised grid (04 §5) |
| LMS-32 | **Item grading** | `/lms/content-items/{contentItemId}/grading` | W B | teacher, grader | `lms.GradeEntry`, `lms.BatchGradeEntries`, `lms.GradeQuizAnswers`, `lms.GradeLearningSubmission`, `lms.DownloadSubmissions`, `lms.ReassignItem`, `lms.FinalizePeerReview`, rubric: `lms.GetRubric` + assessments | SpeedGrader layout: queue → viewer → rubric/score |
| LMS-33 | My grades (student) | `/lms/cohorts/{cohortId}/grades` | W S B | student | `lms.GetGradebook` (own row) | |
| LMS-34 | Item overrides | `/lms/item-overrides` | W | teacher | CRUD | Extra time/attempts, dates |
| LMS-35 | Grade & reassignment history | (drawer in LMS-31/32) | W | teacher | `lms.ListGradeChangeLogs`, `lms.ListReassignmentLogs` | Read-only |

**Authoring**

| ID | Screen | Route | Clients | Personas | Key operations | Notes |
|---|---|---|---|---|---|---|
| LMS-40 | My courses (author) | `/lms/authoring/courses` | W | author | `lms.ListLmsCourses`, `lms.CreateLmsCourse`, `lms.CreateCourseVersion`, `lms.ApproveCourse`, `lms.DeactivateCourse`, `lms.ReactivateCourse`, `lms.CheckCourseDependencies` | Version lanes |
| LMS-41 | Course structure editor | `/lms/courses/{courseId}/edit` | W | author | chapters CRUD, lessons CRUD, `lms.MoveChapter`, lesson activities | Tree + inspector |
| LMS-42 | **Lesson editor** | `/lms/lessons/{lessonId}/edit` | W | author | lesson CRUD, uploads | Block editor (04 §5) |
| LMS-43 | Question bank | `/lms/questions` | W | author | question CRUD (aggregate per P1-25), pools, `lms.ImportQuestions` | 9 types; per-type editors |
| LMS-44 | Quiz builder | `/lms/quizzes/{quizId}/edit` | W | author | quiz CRUD, `lms.SetQuizQuestions`, settings | Preview as student |
| LMS-45 | LMS assignment builder | `/lms/lms-assignments/{lmsAssignmentId}/edit` | W | author | CRUD + grading keys, group settings | |
| LMS-46 | Rubric builder | `/lms/rubrics/{rubricId}/edit` | W | author | rubric aggregate CRUD | Grid editor |
| LMS-47 | Repository | `/lms/repository` | W | author, teacher | repo-flagged lists + `lms.AddFromRepository` | Quizzes, assignments, prompts, resources |
| LMS-48 | Web resources & e-books (admin) | `/lms/web-resources`, `/lms/ebooks` | W | author | CRUD, `lms.ConvertEbook`, `lms.PublishEbookCuration`, `lms.GetWebResourceLaunch` | Conversion job status |
| LMS-49 | External tools (LTI) | `/lms/external-tools` | W | admin | CRUD, `lms.ActivateExternalTool`, `lms.LaunchExternalTool`, resources/links | LTI 1.3 pending P1-39 |
| LMS-50 | Catalogue page editor | `/lms/courses/{courseId}/catalog/edit` | W | author | catalogue items CRUD, `lms.PublishCatalogListing`, `lms.HideCatalogListing` | Authors, previews, banners, testimonials |
| LMS-51 | Course import/export | `/lms/import-export` | W | author | `lms.ExportCourse`, `platform.StartImport`, `lms.ListImportExportJobs`, `lms.RestoreCohortArchive` | Package formats |
| LMS-52 | Plagiarism accounts & reports | `/lms/plagiarism-accounts` | W | admin, teacher | CRUD, `lms.GetPlagiarismReportLink` | |
| LMS-53 | LMS settings | `/lms/settings` | W | admin | settings singleton, sidebar items | Feature toggles drive S/B menus |
| LMS-54 | Drive (file cabinet) | `/platform/drive-items` | W B | all | CRUD, `lms.ShareDriveItem`, `platform.GetMyStorageUsage`, `platform.CopyFile` | Quota meter |
| LMS-55 | Mentorship | `/lms/mentorships` | W | mentor | CRUD, mentee progress views | |

### 3.16 B tablet — offline behaviour per surface

| Surface | Offline capability |
|---|---|
| Cohort outline, lessons, e-books | Fully cached per enrolled cohort (packages per E10 when available; else pre-fetch on Wi-Fi) |
| Quiz attempt | Start requires connection (window check); answering/autosave fully offline via local queue of `lms.SaveQuizResponses` batches (client sequence); submit queued |
| Assignments | Draft text + files stored locally; upload + `lms.FinalizeSubmission` queued |
| Forum | Read cached; posts queued |
| Gradebook & grading (teacher) | Read cached; `lms.BatchGradeEntries` queued with per-row results on sync |
| Roster, schedule, announcements | Read cached |
| Everything else | Online-only with explicit offline card |

### 3.17 C — Platform console

| ID | Screen | Route | Key operations | Notes |
|---|---|---|---|---|
| PLT-01 | Tenants | `/platform/tenants` | tenant CRUD, `platform.ClearDemoData` | |
| PLT-02 | Plans & subscriptions | `/platform/saas-plans` | plan CRUD, subscriptions list | |
| PLT-03 | Sales enquiries | `/platform/sales-enquiries` | list, `platform.MarkEnquiryContacted` | |
| PLT-04 | Global catalogues | `/comms/notification-types`, `/comms/notification-channels`, `/platform/reserved-keywords` | CRUD | Notification catalogues are tenant-readable (GET with user token); user types are a fixed enum |
| PLT-05 | System settings | `/platform/system-settings` | CRUD | |
| PLT-06 | Operations & jobs | `/platform/operations` | `platform.ListOperations` (job centre across modules), then the owning module's operation service per row — e.g. `finance.GetOperation`, `finance.CancelOperation` | Row's `module` routes refresh/cancel; job-centre status may lag the owner by seconds |
| PLT-07 | Master data | `/master/countries`, `/master/states`, `/master/cities`, `/master/currencies` | `master.ListCountries`, `master.CreateCountry`, `master.UpdateCountry`, `master.DeleteCountry` (same set for states, cities, currencies) | Global reference data; writes by platform operators only, reads public. Reference implementation screen of the skeleton |
