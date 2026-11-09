# 03 · User flows

The fifteen journeys that define the product, step by step with the exact API operations, the error paths the UI must handle, and what each participant sees afterwards. Screen IDs refer to [02-navigation-and-screens.md](02-navigation-and-screens.md); conflict/idempotency mechanics are specified once in [05 §6](05-frontend-architecture.md).

Conventions used below: every POST carries an `Idempotency-Key`; every update/delete sends `If-Match`; `→ 🔔` means the recipients get a push + inbox notification whose `actionUrl` deep-links to the named screen.

---

## F1 · Sign-in, personas and first run

Actors: any user · Clients: all

1. ACC-01: `iam.Login` (username/email + password, device info) → `TokenPair` + `Me`. Errors: invalid credentials (401, generic message), account locked (`iam.UnlockUser` is admin-side; show "contact administrator"), `mfaRequired` → MFA step: `iam.CompleteMfa` with `mfaToken` + code returns the tokens.
2. If `Me.user.termsAcceptedAt` empty → ACC-04 `iam.AcceptTerms` (blocking).
3. Shell boots: navigation from `Me.permissions`; institution switcher from `Me.institutions`; theme from `platform.GetBranding`; unread badge `lms.GetUnreadCounts`.
4. Multi-persona users see the persona sheet (ACC-06) on first run; `iam.SwitchRole` → replace tokens, refetch `Me`, rebuild navigation.
5. Phones register for push on first consent (05 §8); token sent with login's device info.
6. Session lifetime: silent refresh via `iam.RefreshToken` (single-flight); refresh failure → sign-in with "session expired"; `iam.Logout` revokes device + tokens.

## F2 · Admission: enquiry → application → review → enrolment

Actors: applicant/guardian, admissions officer · Clients: W (public form + console)

1. Officer prepares the cycle (ADMN-01): create → `admission.StartAdmission` → `admission.OpenApplications`.
2. Applicant on the public form (ADMN-05): `admission.CreateApplication` (draft, resumable) → uploads (photo/documents via `platform.CreateUploadUrl` + `platform.CompleteUpload`) → `admission.SubmitApplication`. Confirmation screen with application number. → 🔔 officer.
3. Officer pipeline (ADMN-02/03): review → `admission.ApproveApplication` | `admission.RejectApplication` | `admission.MarkApplicationPending`. Approved + fee paid (F4 variant `purpose=admission`) → `admission.ConfirmApplication` / `admission.ConfirmAdmission`.
4. Enrol (ADMN-04): `admission.EnrollApplicant` with section/batch + roll number; the response is the application with `studentId`, and portal-account creation follows as an operation. UI shows the operation's progress, then links to SIS-02. → 🔔 guardian (welcome + portal credentials flow).
5. Errors: duplicate application (409), cycle closed (`FAILED_PRECONDITION`), seat/validation errors inline (422).

## F3 · Teacher takes attendance

Actors: teacher · Clients: T (primary), W

1. T Today tab: sessions from `timetable.GetTimetable`; tap a session → `attendance.TakeAttendanceForSession` (gets or creates the sheet) → ATT-03.
2. Grid: all present by default; tap to cycle present/absent/late/excused; absent prompts reason (`absence_reason` list, cached). Save in pages: `attendance.RecordAttendance` (batch; per-row errors per P1-31).
3. `attendance.CompleteAttendanceSheet` locks the sheet. Reopen = `attendance.ResetAttendanceSheet` (permission-gated). → 🔔 guardians of absentees (absence notice, deep link ATT-04).
4. Connectivity dips: marks queue locally and flush in order; the sheet ETag guards against two devices (412 → reload roster, re-apply unsaved marks, show banner).
5. Admin views ATT-05 summaries (`attendance.SummarizeAttendance`).

## F4 · Parent pays fees

Actors: guardian · Clients: P (primary), W, S

1. P Fees tab: `sis.ListMyChildren` → per child `finance.GetFeeStatement` (dues, paid, balance).
2. Select dues → `finance.QuoteCheckout` (amounts, taxes, coupon try-on) → review screen shows quote breakdown.
3. `finance.StartCheckout` (Idempotency-Key **required**; quote reference per P1-29) → gateway SDK/redirect.
4. Return → poll payment status (`finance.GetPayment`) until webhook lands; show receipt; statement refreshes. → 🔔 guardian (receipt), accountant dashboards update.
5. Errors: quote expired (`FAILED_PRECONDITION` → re-quote), gateway failure/cancel (payment `failed` → retry keeps the same dues), double-tap (idempotent replay returns the first payment).
6. Counter payment (accountant, FIN-06): `finance.RecordPayment` (cash/cheque/DD/transfer) with allocations against dues or invoices; the receipt number comes from the server.

## F5 · Homework lifecycle (K-12)

Actors: teacher, head (optional approval), student, guardian · Clients: T/W, S, P

1. Teacher (ASM-01): create homework (section, subject, due, attachment) → `assessment.PublishHomework` (or submits into approval: head runs ASM-02 `assessment.ApproveHomework`/`RejectHomework`). → 🔔 students + guardians (deep link ASM-01 item).
2. Student (ASM-04): reads, submits text/files → `assessment.CreateHomeworkSubmission`.
3. Teacher checks (ASM-03): `assessment.CheckHomeworkSubmission` per student (checked/unchecked + comments); replies thread via `assessment.ReplyToHomeworkFeedback`. → 🔔 student on check/comment.
4. Parent sees status per child on P School tab.

## F6 · Exams: schedule → marks → marksheet → publish

Actors: exam cell, teachers, students/parents · Clients: W (T for marks)

1. ASM-08: create session → `assessment.ScheduleExamSession`; papers in ASM-09 → `assessment.ScheduleExam`, `assessment.PopulateExamResults`, `assessment.AllocateExamRooms`, invigilators. → 🔔 students (hall ticket/schedule, COM-03 calendar entries).
2. After the paper: `assessment.MarkExamHeld` → marks grid ASM-10 `assessment.RecordExamMarks` (row errors inline) → `assessment.MarkExamResultsUpdated`, `assessment.CompleteExam`.
3. Results (ASM-11): `assessment.GenerateResults` (202 + operation) builds the marksheet from a result template → review → `assessment.ValidateMarksheet` (publication gate; after it, lines are immutable — corrections only via the logged re-evaluation path). → 🔔 students/guardians → ASM-12.
4. Corrections pre-validation: `assessment.ResetMarksheet`/`CancelMarksheet`; post-validation only via the re-evaluation path (result changes are logged).

## F7 · LMS authoring → publish → cohort delivery

Actors: author, teacher · Clients: W

1. LMS-40: `lms.CreateLmsCourse` → LMS-41 structure (chapters, lessons LMS-42, activities), LMS-43/44 quizzes, LMS-45 assignments, LMS-46 rubrics; objectives; `lms.CheckCourseDependencies` before publish; `lms.ApproveCourse` publishes the version; `lms.CreateCourseVersion` for the next edition.
2. Catalogue (LMS-50): page content → `lms.PublishCatalogListing`.
3. Cohort setup: create cohort → `lms.CopyCourseOutlineToCohort` (dates materialise from day offsets) → adjust in LMS-22 → roster LMS-23 (`lms.SetCohortMembers` / `lms.ImportCohortMembers` / self-enrol) → `lms.ActivateCohort`. → 🔔 members (welcome → LMS-21).
4. During delivery, template fixes flow by re-copy of selected items (`lms.CopyContentItem`), never silent edits (published content is versioned; edits create a new version).

## F8 · Student works through a cohort (with offline tablet)

Actors: student · Clients: B (primary), W, S

1. LMS-21 cohort home: syllabus `lms.GetCohortSyllabus`, progress `lms.GetCohortProgress`; B downloads the cohort for offline (02 §3.16, 05 §7).
2. Lessons (LMS-06): content renders from blocks; completion `lms.MarkLessonComplete`; video heartbeats `lms.TrackVideoWatch`; views `lms.RecordItemView` (fire-and-forget).
3. Release rules gate items: `lms.GetReleaseStatus` chips ("unlocks after Quiz 1 ≥ 60%").
4. Quiz (LMS-08): start online (`lms.StartQuizAttempt`, bound to the student's placement) → answer offline, autosave queue `lms.SaveQuizResponses` (attempt token + client sequence + answeredAt) → pause/resume → `lms.SubmitQuiz` on reconnect. Timer trusts `serverTime`/`deadlineAt` from autosave responses.
5. Assignment (LMS-10): draft → upload → `lms.FinalizeSubmission`; plagiarism status appears when the webhook lands.
6. Grades (LMS-33): own gradebook row; published grades only. → 🔔 on publish.

## F9 · Teacher grades and publishes (tablet-first)

Actors: teacher, grader · Clients: B, W

1. LMS-20 to-grade queue → LMS-32: submission viewer + rubric (`lms.GetRubric`) → score via `lms.GradeEntry` (ETag per entry) or grid `lms.BatchGradeEntries` (per-row results); quiz manual marks `lms.GradeQuizAnswers`; self-paced items `lms.GradeLearningSubmission`; bulk download `lms.DownloadSubmissions`; reassign `lms.ReassignItem`; peer points `lms.FinalizePeerReview`.
2. Publish per item `lms.PublishItemGrades` → 🔔 students. Term end: `lms.PublishFinalGrades`, `lms.ExportGrades`, certificates `lms.GenerateCohortCertificates`.
3. Offline (B): grades queue; row conflicts (stale ETag) surface in a review tray on sync — never silently overwritten.

## F10 · Live class

Actors: teacher, students · Clients: W, B, S

1. Schedule (LMS-27): `lms.CreateLiveClass`/`lms.ScheduleLiveClass` (provider account) → appears in COM-03 calendars. → 🔔 cohort.
2. Join: student `lms.JoinLiveClass` (personal join URL); teacher `lms.GetLiveClassStartUrl`.
3. End: `lms.EndLiveClass`; attendance sync `lms.SyncLiveClassAttendance`; recording link appears on the live-class card.

## F11 · Forums with graded discussion

1. LMS-26: topic list (`lms.ListForumTopics`, unread chips from `lms.GetUnreadCounts`) → thread (`lms.GetForumTopic`) → reply `lms.CreateForumPost` (drafts allowed), votes, `lms.MarkTopicRead`.
2. Moderation: `lms.FlagForumPost` → teacher `lms.HideForumPost`/`lms.ConfirmPostAbuse`; `lms.MarkPostSubstantive` feeds participation grading; graded topics write through the gradebook (LMS-32).

## F12 · Notices, calendar and messaging

1. Notice (COM-02): compose, audiences (institutions/roles/section/cohort), `comms.PublishNotice`; replies via linked topic; `comms.CloseNoticeReplies`. → 🔔 audience → COM-01.
2. Calendar (COM-03): `comms.GetMyCalendar` merges school events, cohort sessions, due items, personal events; `comms.ExportCalendar` for external calendars; occurrence actions (`comms.CancelEventOccurrence`, `comms.CompletePersonalEvent`).
3. Conversations (COM-05): `comms.CreateConversation` → messages `comms.CreateConversationMessage`; read state `comms.MarkConversationRead`. Poll-based refresh v1.
4. Broadcast (COM-06): recipients builder → `comms.SendMessage` (Idempotency-Key required) → queue monitoring `comms.ListOutboundMessages`, `comms.RetryOutboundMessage`.

## F13 · Library circulation

1. LIB-03 desk: scan card → member panel (loans, fines, card limits) → scan copy → `library.CreateLoan`. Errors: copy already out (unique open-loan), limit reached, expired card (`FAILED_PRECONDITION` with reason).
2. Return: `library.ReturnLoan`; overdue → fine on the loan → `library.InvoiceLoanFine` → collect via F4 machinery. Lost: `library.MarkLoanLost`.
3. Member self-service: LIB-05 reservations (`library.AcceptReservation` librarian-side), LIB-07 my loans; renew = `library.RequestLoanReturn`-free re-issue policy decision (tenant rule).

## F14 · Staff leave and payroll run

1. Leave: staff (HR-04/T) apply → approver `hr.ApproveLeave`/`hr.RejectLeave` (balance check `hr.GetLeaveBalance`). → 🔔 both sides.
2. Payroll (HR-07): pick period → `payroll.GeneratePayslips` (202 + operation) → review payslips (deductions from leave) → `payroll.MarkPayslipPaid` with payment records. → 🔔 staff → HR-08 payslip PDF.

## F15 · Guardian consent (minors)

1. On actions that need consent (publishing photos COM-04, public profile ACC-12), the UI checks `people.GetConsentStatus`; missing → consent prompt.
2. Guardian (ACC-13, per child): `people.CreateConsentRecord` (purpose, policy version shown in full) / `people.WithdrawConsent`. Withdrawal immediately hides dependent UI affordances; server unpublishes async.
3. Blocked server calls return 412 `CONSENT_REQUIRED` + purpose — the UI maps it to the consent prompt, never a raw error.

---

### Cross-flow rules

- **Notifications**: every 🔔 lands in ACC-09 with `actionUrl`; push taps route through the shared deep-link table. Unread badges refresh on foreground and after `comms.MarkNotificationRead`.
- **Long-running work** (`finance.GenerateFeeDues`, `payroll.GeneratePayslips`, `assessment.GenerateResults`, imports/exports): 202 + operation → progress UI polls the owning module's `GetOperation` (e.g. `finance.GetOperation`, `payroll.GetOperation`, `assessment.GetOperation` — the operation's `module` field names it); completion → 🔔 with result deep link.
- **Money, enrolment, bulk sends**: never fire twice — Idempotency-Key + disabled-while-pending + replay-safe retry (05 §6).
- **Printables** (receipts, marksheets, payslips, bonafide, bus pass, hall tickets): server-rendered documents fetched via `platform.GetDownloadUrl`; the UI never re-computes official numbers.
