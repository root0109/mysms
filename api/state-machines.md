# State machines

The state transitions the services serve. Keep this page in step with each resource's service when a step changes.

Each step below is served as a generated operation. The caller needs the operation's permission and
must be able to write the row (its institution, or a tenant-wide grant for a tenant-level row). The row
must be in one of the listed states, otherwise the answer is **409 `INVALID_STATE_TRANSITION`** and
nothing changes. A body `etag` that does not match the row is **412**; where the operation declares
`If-Match`, it is required (**428**) and must match (**412**). Each step writes an audit
record (with the reason, when the request carries one) and a `<resource>.<step>` event to the module
outbox, in the same transaction.

Steps that have further effects are hand-written work and are not listed here. Examples are
notifications, approval records, balances, enrolment and grading. Until they are built they answer 501.

`$now` is the time of the step, `$today` its UTC date, `$actor` the calling user, and
`$param:x` the request field `x`.

## academics.LessonPlan (`academics.lesson_plan`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `academics.SubmitLessonPlan` | `status` ∈ {"draft"} | `status` = "pending" | no |
| `academics.ApproveLessonPlan` | `status` ∈ {"pending"} | `status` = "approved"; a review record: `decision` = "approved", `decided_by_user_id` = `$actor`, `decided_at` = `$now`, `comments` = `$param:reason` | yes (optional) |
| `academics.RejectLessonPlan` | `status` ∈ {"pending"} | `status` = "rejected"; a review record: `decision` = "rejected", `decided_by_user_id` = `$actor`, `decided_at` = `$now`, `comments` = `$param:reason` | yes (optional) |
| `academics.PublishLessonPlan` | `status` ∈ {"approved"}; `is_published` ∈ {false} | `is_published` = true, `published_at` = `$now` | no |
| `academics.ArchiveLessonPlan` | `status` ∈ {"approved", "rejected", "cancelled"} | `status` = "archived" | no |

Approve and reject are hand-written (they keep the decision as an `academics.lesson_plan_approval` row, in the
transaction of the step); a reason is trimmed, at most 4,000 characters (422 beyond). A plan with a review record
is archived, not deleted (the standard delete answers 409), and review records are written by these two steps
only (their standard create, update and delete answer 409) and are readable where the plan is. Who may review is
decided by the permission alone: a head who also teaches can decide on their own plan. Events:
`academics.lesson_plan.approve`, `.reject`.

## admission.AdmissionCycle (`admission.admission_cycle`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `admission.ConfirmAdmissionCycle` | `status` ∈ {"draft"} | `status` = "confirmed" | no |
| `admission.OpenApplications` | `status` ∈ {"confirmed"} | `status` = "open" | no |
| `admission.StartAdmission` | `status` ∈ {"open"} | `status` = "admission" | no |
| `admission.CloseAdmissionCycle` | `status` ∈ {"open", "admission"} | `status` = "closed" | no |
| `admission.CancelAdmissionCycle` | `status` ∈ {"draft", "confirmed", "open"} | `status` = "cancelled" | yes |
| `admission.ResetAdmissionCycle` | `status` ∈ {"cancelled"} | `status` = "draft" | no |

A cycle runs draft → confirmed → open (takes applications) → admission (decides and admits what it took, no new
applications) → closed. While it is open, its record is readable by every holder of
`admission.admission_cycle.read` in the tenant (an applicant has no institution in scope); otherwise only inside
the caller's institutions.

## admission.Application (`admission.application`) — hand-written (flow F2)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `admission.SubmitApplication` | `status` ∈ {"draft"}; a live guardian (422); for an application of a cycle: the cycle `open` (409), not past its `end_date` (409), of the application's institution (422), fewer handed-in applications than `max_applications` (409) and the applicant at least `minimum_age_years` old on the cycle's `start_date` (422) | `status` = "submitted", `application_date` = `$now` | no |
| `admission.ConfirmApplication` | `status` ∈ {"submitted", "pending"} | `status` = "confirmed" | no |
| `admission.MarkApplicationPending` | `status` ∈ {"submitted", "confirmed"} | `status` = "pending" | yes |
| `admission.ApproveApplication` | `status` ∈ {"confirmed"} | `status` = "approved" | no |
| `admission.ConfirmAdmission` | `status` ∈ {"approved"}; an `application_fee` above zero `paid` or `waived` (409); a cycle it belongs to `open` or `admission` (409) | `status` = "admitted", `admission_date` = `$today` when empty | no |
| `admission.RejectApplication` | `status` ∈ {"submitted", "pending", "confirmed", "approved"} | `status` = "rejected" | yes |
| `admission.CancelApplication` | `status` ∈ {"draft", "submitted", "pending", "confirmed", "approved", "admitted"} | `status` = "cancelled" | yes |
| `admission.ResetApplication` | `status` ∈ {"rejected", "cancelled"} | `status` = "draft" | no |
| `admission.EnrollApplicant` | `status` ∈ {"admitted"} (one already `done` answers its student, 200, nothing changes) | `applicant_person_id`, `student_id` (people and sis, through their SDK), `status` = "done", `section_id` / `batch_id` of the placement | no |

The table has no decision columns: the audit record of each step (actor, time, reason) is the record of who decided.
A handed-in application is counted against the cycle's places from submit until it is rejected or cancelled; the
cycle row is locked while an application is handed in, so two submissions never both take the last place.
**Who reaches an application:** the office of its institution (the institution scope), and the applicant who
started it in an admission cycle — an account that holds the permissions without the institution in scope reaches
only its own applications of a cycle (others are 404), creates one only in a cycle of that institution that is
open — numbered by the school, not by the applicant: the number sent is replaced by the next of the institution's
series for the cycle's start year in `admission.number_sequence` (`APP-<year>-000001`, the counter locked, numbers
the office gave by hand skipped; a `validateOnly` create takes none) — never reads the office's `remarks` and
`payment_status_note` (left out of its responses, and refused as list filter fields with 400 `INVALID_FILTER`; an office caller filtering on them reads only its institutions' rows), never sets what the office decides (dates, placement, person, student, fee fields, remarks: 422), changes it
and its guardians and contacts only while it is a draft (409), and takes only `SubmitApplication` and
`CancelApplication` on it (403 for the other steps). **Enrolment** (`admission.EnrollApplicant`, Idempotency-Key
required) needs a class section (the request's or the application's `section_id`) with the application's academic
year, or the application's course with an optional batch (422 otherwise); it creates in turn the person (people),
the student of the institution (sis; an existing student of that person is reused), the class or course placement
(unless the student has one for that year or course), and each guardian with its link — a guardian sis already
knows by e-mail, then mobile (the parent of a sibling), is reused and only linked; one it does not know is created
unless one of that name is linked already; the relationship types are read once per enrolment; each result is kept on the application in its own transaction under the row-version guard, and `done` is
set last, only on a row with a student. A repeated call completes what a failed one left (the creates carry
Idempotency-Keys derived from the application, the caller and the step — never the row version, so a retry after
an office edit replays the same create instead of making a second person). People or sis unconfigured,
failing or not serving a step: 503 and the application stays `admitted`. With `createPortalAccounts` (honoured on a
repeat of an enrolled application too) the student's and the guardians' accounts are requested without an
invitation (`sendInvite` false: message delivery is not built), an account that exists already is kept, and they are requested as an `admission.EnrollApplicant.portalAccounts` operation, answered in
the result already finished (succeeded or failed: a failure never undoes the enrolment). From `admitted` on, the
standard update leaves `applicant_person_id` and `student_id` alone (409), and an application with its student is
not deleted (409). Events: `admission.application.submit`, `.confirm`, `.markPending`, `.approve`,
`.confirmAdmission`, `.reject`, `.cancel`, `.reset`, `.enroll` (with `studentId`). Fee dues are not created by
enrolment (see STATUS).

## assessment.Homework (`assessment.homework`) — hand-written (flow F5)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `assessment.PublishHomework` | `status` ∈ {"draft"} | `status` = "published" and `assigned_on` = `$now` when empty — or, when the caller does not hold `assessment.homework.approve_homework` in the homework's institution (as far as the token tells institutions apart: STATUS, known limit 1), `status` = "pending" and a pending `homework_approval` | no |
| `assessment.ApproveHomework` | `status` ∈ {"pending"} | `status` = "published", `assigned_on` = `$now` when empty; the pending approval: `decision` = "approved", `decided_by_user_id` = `$actor`, `decided_at` = `$now`, `comments` = `$param:reason` | yes |
| `assessment.RejectHomework` | `status` ∈ {"pending"} | `status` = "draft"; the pending approval: `decision` = "rejected", `decided_by_user_id` = `$actor`, `decided_at` = `$now`, `comments` = `$param:reason` | yes |

Who needs a review is decided by permissions, not by a setting: a school that wants none gives its teachers the
approving permission. A rejected homework is a draft again; publishing it once more opens a new review. The reason
of an approval or rejection is optional. The standard update answers 409 while a homework is `pending`, and for a
`published` one unless the caller may approve homework there; approvals are written only by these steps (the
contract has no create, update or delete for them). Events:
`assessment.homework.publish`, `.sendForApproval`, `.approve`, `.reject` (ids only).

## assessment.HomeworkSubmission (`assessment.homework_submission`) — hand-written (flow F5)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `assessment.CheckHomeworkSubmission` | any state (checking again corrects a comment) | `status` = "checked", `checked_by_user_id` = `$actor`, `checked_on` = `$today`, `teacher_comments` = `$param:teacherComments` when given | no |
| `assessment.ReplyToHomeworkFeedback` | `teacher_comments` not empty, and only for the account that created the submission (403 otherwise) | `student_reply` = `$param:studentReply` | no |

Comments, replies and reasons are kept trimmed, at most 4,000 characters (422 beyond). The check and the reply are
read-only in the standard create and update of a submission: only these two operations write them. A check without
comments keeps the comments already given (they are replaced, never removed).

## assessment.Assignment (`assessment.assignment`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `assessment.PublishAssignment` | `status` ∈ {"draft"} | `status` = "published" | no |
| `assessment.CancelAssignment` | `status` ∈ {"draft", "published", "ongoing"} | `status` = "cancelled" | yes |
| `assessment.ResetAssignment` | `status` ∈ {"cancelled"} | `status` = "draft" | no |

## assessment.AssignmentSubmission (`assessment.assignment_submission`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `assessment.SubmitAssignmentSubmission` | `status` ∈ {"draft", "change_requested"} | `status` = "submitted", `submitted_at` = `$now` | no |
| `assessment.AcceptAssignmentSubmission` | `status` ∈ {"submitted"} | `status` = "accepted" | no |
| `assessment.RejectAssignmentSubmission` | `status` ∈ {"submitted"} | `status` = "rejected" | yes |
| `assessment.RequestSubmissionChanges` | `status` ∈ {"submitted"} | `status` = "change_requested", `feedback` = `$param:reason` | yes |
| `assessment.ResetAssignmentSubmission` | `status` ∈ {"rejected", "change_requested"} | `status` = "draft"; the grade cleared | no |
| `assessment.GradeAssignmentSubmission` | `status` ∈ {"submitted", "accepted"} | `status` = "graded", `marks_obtained` = `$param:marks` (at most the assignment's maximum, 422 beyond), `graded_by_user_id` = `$actor`, `graded_on` = `$today`, `feedback` = `$param:feedback` | no |

A submission is a student's own work. Staff — callers holding a reviewing permission (grade, accept, reject,
request changes) in the assignment's institution — read and work on every submission there. Anyone else (a student,
a guardian) reads, creates, changes, submits and resets only the submissions of their own students, which sis
confirms with the caller's token (`sis.GetStudentSummary` / `sis.ListMyChildren`, asked outside any transaction; 503
when sis cannot answer): another student's submission is 404, a submission in another student's name is 403, and
their own changes only while it is a draft or returned for changes (409). The standard create and update never
write the submission time, the grade, the grader, the grading date or the feedback, and the update never changes the
student (those fields are not writable). Homework submissions follow the same owner rule, with
`assessment.homework_submission.check_homework_submission` as the staff permission.

## assessment.Exam (`assessment.exam`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `assessment.ScheduleExam` | `status` ∈ {"draft"} | `status` = "scheduled" | no |
| `assessment.CompleteExam` | `status` ∈ {"held", "result_updated"} | `status` = "done" | no |
| `assessment.CancelExam` | `status` ∈ {"draft", "scheduled"} | `status` = "cancelled" | yes |
| `assessment.ResetExam` | `status` ∈ {"cancelled"} | `status` = "draft" | no |
| `assessment.MarkExamHeld` | `status` ∈ {"scheduled"}, with at least one attendee | `status` = "held" | no |
| `assessment.MarkExamResultsUpdated` | `status` ∈ {"held"}, every attendee marked (present with marks, absent or exempted) | `status` = "result_updated", `results_entered` = true | no |

`assessment.PopulateExamResults` (scheduled exams) and `assessment.AllocateExamRooms` read the exam in one
transaction, ask sis (roster), academics (course-plan credits) and facility (each room, which must belong to the
exam's institution, be in use and have a capacity: 422 otherwise) with the caller's token outside any transaction,
and write with the exam row locked, re-checking that the exam (and its session) did not change meanwhile (412);
without those services the answer is 503 and nothing is written. `assessment.RecordExamMarks` (held or
result_updated) writes many attendees at once, all or nothing: an entry may carry the attendee's `etag` (412 when
the attendee changed since); present attendees need marks (pass or fail against the pass marks), absent and
exempted ones have none; a changed result of a `result_updated` exam reopens marks entry (`status` = "held"). An
exam result's attendance changes only through `RecordExamMarks`, and a result linked to a marksheet line is
immutable (409) — through the update and through `RecordExamMarks` — until that marksheet is cancelled.

## assessment.ExamSession (`assessment.exam_session`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `assessment.ScheduleExamSession` | `status` ∈ {"draft"} | `status` = "scheduled" | no |
| `assessment.MarkExamSessionHeld` | `status` ∈ {"scheduled"} | `status` = "held" | no |
| `assessment.CompleteExamSession` | `status` ∈ {"held"} | `status` = "done" | no |
| `assessment.CancelExamSession` | `status` ∈ {"draft", "scheduled"} | `status` = "cancelled" | yes |
| `assessment.ResetExamSession` | `status` ∈ {"cancelled"} | `status` = "draft" | no |

## assessment.Marksheet (`assessment.marksheet`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `assessment.CancelMarksheet` | `status` ∈ {"draft", "validated"} | `status` = "cancelled"; its exam results released; its template back to "draft" | yes |
| `assessment.ResetMarksheet` | `status` ∈ {"cancelled"}, and the marksheet has no generated lines | `status` = "draft" | no |

Cancelling a marksheet releases what generation fixed, in the same transaction: every `exam_result` linked to one
of its lines gets `marksheet_line_id`, `grade_label` and `grade_point` cleared (an audit record `releaseResult` with
the reason and an `assessment.exam_result.resultReleased` event each), and its result template returns to "draft"
(`assessment.result_template.reopened`) once none of the template's marksheets is live — so marks can be corrected
and the results generated again. A cancelled marksheet with generated lines stays cancelled (`ResetMarksheet` 409);
the new generation writes a new marksheet.

`assessment.GenerateResults` (template `status` ∈ {"draft"}) runs in one transaction with the template row locked
(a second generation waits, then answers 409) and the grading scheme held `FOR SHARE`; it is refused (409) while the
session's results belong to a marksheet that is not cancelled (a second template of the same session included), and
until every paper that is not cancelled is `result_updated` or `done` with every attendee final (present with
marks, absent, or exempted) and a max-marks snapshot. Cancelled papers and exempted attendees count for nothing (no
maximum, no credits). Each paper's percentage is rounded to 2 places half-up (the bands' scale; band edges are
inclusive); a student fails when any paper that is not exempted was failed or missed, whatever the overall band,
and a `pass_fail` scheme's label follows that decision. A percentage between two bands gets no grade. A grading
scheme, or its bands, cannot change (409) while a result template that is no longer a draft grades with it; band
and scheme writes lock the scheme `FOR UPDATE`.

## attendance.AttendanceSheet (`attendance.attendance_sheet`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `attendance.StartAttendanceSheet` | `status` ∈ {"draft"} | `status` = "in_progress" | no |
| `attendance.CompleteAttendanceSheet` | `status` ∈ {"in_progress"}, with at least one mark | `status` = "done" | no |
| `attendance.CancelAttendanceSheet` | `status` ∈ {"draft", "in_progress"} | `status` = "cancelled" | yes |
| `attendance.ResetAttendanceSheet` | `status` ∈ {"cancelled", "done"} | `status` = "draft" | no |

Marks are recorded with `attendance.RecordAttendance` while `status` = "in_progress" (each call gives the sheet a new
version). Reopening a completed sheet (`ResetAttendanceSheet`, then `StartAttendanceSheet`) keeps its marks.

## careers.JobApplication (`careers.job_application`) — hand-written

`careers.UpdateApplicationStatus` moves an application through the hiring steps; a step the table does not list —
the same status included — is 409 and nothing changes. The audit record keeps the old and the new status; the event
is `careers.job_application.set_status`.

| From | To |
|---|---|
| "submitted" | "shortlisted", "rejected", "withdrawn" |
| "shortlisted" | "hired", "rejected", "withdrawn" |
| "rejected", "hired", "withdrawn" | — (decided) |

## careers.JobOpening (`careers.job_opening`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `careers.CloseJobOpening` | `status` ∈ {"open"} | `status` = "closed" | no |
| `careers.ReopenJobOpening` | `status` ∈ {"closed"} | `status` = "open" | no |

## comms.Event (`comms.event`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `comms.ActivateEvent` | `status` ∈ {"inactive"} | `status` = "active" | no |
| `comms.DeactivateEvent` | `status` ∈ {"active"} | `status` = "inactive" | no |

## comms.EventOccurrence (`comms.event_occurrence`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `comms.CancelEventOccurrence` | `is_cancelled` ∈ {false} | `is_cancelled` = true | yes |

## comms.FeedbackThread (`comms.feedback_thread`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `comms.CloseFeedbackThread` | `status` ∈ {"open"} | `status` = "closed" | no |
| `comms.ReopenFeedbackThread` | `status` ∈ {"closed"} | `status` = "open" | no |

## comms.Notice (`comms.notice`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `comms.PublishNotice` | `is_published` ∈ {false}; `is_closed` ∈ {false} | `is_published` = true | no |
| `comms.UnpublishNotice` | `is_published` ∈ {true} | `is_published` = false | no |
| `comms.DeactivateNotice` | `is_closed` ∈ {false} | `is_closed` = true | yes |
| `comms.ReactivateNotice` | `is_closed` ∈ {true} | `is_closed` = false | no |

## comms.Post (`comms.post`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `comms.ApprovePost` | `status` ∈ {"pending"} | `status` = "posted", `posted_at` = `$now` | no |
| `comms.CancelPost` | `status` ∈ {"draft", "pending", "posted"} | `status` = "cancelled" | yes |

## comms.Task (`comms.task`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `comms.CompleteTask` | `is_completed` ∈ {false} | `is_completed` = true | no |
| `comms.ReopenTask` | `is_completed` ∈ {true} | `is_completed` = false | no |
| `comms.SnoozeTask` | `is_completed` ∈ {false} | `is_snoozed` = true, `reminder_at` = `$param:until` | no |

## finance.FeeStructure (`finance.fee_structure`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `finance.ArchiveFeeStructure` | `status` ∈ {"published"} | `status` = "archived" | no |

## finance fees, invoices and payments — hand-written

**Publish and dues.** `finance.PublishFeeStructure` (202, a `finance` operation) moves a draft structure to
`published` and charges its roster (the class section's or course's students in the year, read from academics and
sis with the caller's token) with the structure's plan; `finance.GenerateFeeDues` (202) charges named students
(at most 500), a class section's roster or a course's registrations with a plan of the caller's institution. A
plan's installments' shares must total 100 and its heads share one currency (422). Each installment head's amount
is its share of the total (the request's, else the heads' default amounts), rounded half-up with the last share
taking the remainder; the plan's discount percent comes off first, then one concession of the student (the head's
own before an all-heads one; an amount concession is a budget spent across the dues in date order). Each student is
charged in its own transaction; a slot already charged is kept. The operation's `result` holds `students`, `dues`,
`kept`, `skippedCount` and up to 200 `skipped`. A due (`draft` → `invoiced` → back to `draft` when its invoice is
cancelled; `cancelled`) changes through `UpdateStudentFeeDue` only while draft. Event
`finance.student_fee_due.generated`.

**Invoices.** `CreateInvoiceFromDues` bills draft dues of one student, one institution and one currency (409 when a
due is invoiced, cancelled or paid; 422 otherwise) as a `draft` invoice numbered `DRAFT-…`. `PostInvoice`
(`draft` → `posted`, numbered from the institution's `INV-` series under a row lock; 422 with no lines or a total of
0) and `CancelInvoice` (`draft` → `cancelled`, its dues go back to draft) take the body etag (412). A posted
invoice is `partially_paid` / `paid` by its allocations and never changes otherwise.

**Payments.** `RecordPayment` (counter: cash, cheque, demand draft, bank transfer, card machine) allocates the amount
exactly (422 when the allocations do not add up or exceed what is outstanding, `OVER_ALLOCATED`) under row locks of
its dues and invoices, numbered from the `RCT-` series. Online: `QuoteCheckout` / `StartCheckout` (invoice purpose;
payer consent required) create a Razorpay order through the institution's `finance.payment_gateway_account` and an
`initiated` payment; 503 `PAYMENT_GATEWAY_NOT_CONFIGURED` when the account, its secret or the gateway is missing or
unreachable, 409 `CHECKOUT_STALE` when the invoice changed meanwhile. Money is recorded only by
`ReceiveGatewayWebhook`: the `X-Razorpay-Signature` over the raw body under the account's webhook secret (401
otherwise), each gateway event id once (`finance.gateway_event`; a replay or an unknown event is acknowledged with no
effect), amount and currency equal to the order's (422 `GATEWAY_AMOUNT_MISMATCH`, the event is kept as
`AMOUNT_MISMATCH`). `payment.captured` / `order.paid` → `received` with the receipt; `payment.failed` → `failed`.

**Refunds.** `RefundPayment` refunds at most what is left (422) under the payment's row lock; a retry with the same
`Idempotency-Key` finds its refund instead of refunding again. A gateway refund is `initiated` before the gateway is
called and `processed` / `failed` by its answer or by `refund.processed` / `refund.failed`; with no answer it stays
initiated (503). A refund takes back the payment's allocations newest first; a payment refunded in full is
`refunded`.

**Owner-only reads.** Dues, invoices, payments, refunds and `GetFeeStatement` show the fee office the rows of its
institutions; others see their own student record's and their children's (sis confirms the relation), and payments
they paid; anything else is 404. A statement mixing currencies is 409 `MIXED_CURRENCIES`. `SendFeeReminders`
records one `finance.fee_reminder.requested` event (ids only) per open invoice; delivery is not built yet.

## hr.LeaveApplication (`hr.leave_application`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `hr.ApproveLeave` | `status` ∈ {"pending"}; not the applicant themselves — their account, their person, or someone acting as them (403); a leave type named where the school has leave types, and enough days left (409) | `status` = "approved", `decided_by_user_id` = `$actor`, `decided_on` = `$today`, `decision_comments` = `$param:reason`; the days are added to `used_days` of the staff member's `leave_balance` for the type and year (opened with the type's `max_days_per_year` if absent) | yes |
| `hr.RejectLeave` | `status` ∈ {"pending"} | `status` = "rejected", `decided_by_user_id` = `$actor`, `decided_on` = `$today`, `decision_comments` = `$param:reason` | yes |
| `hr.CancelLeave` | `status` ∈ {"pending", "approved"}; an approved leave only before it starts (409 once it has started); only the applicant (their account or person) or a holder of `hr.leave_application.approve_leave` in the institution (403) | `status` = "cancelled"; for an approved leave its days are taken off `used_days` of the balance its approval counted them against (never below zero) | yes |

Approve is hand-written (the leave ledger). The days of an application are the calendar days (UTC) from the day
of `from_at` to the day of `to_at`, both counted, halved for a half-day part, which is a leave of one day (422
otherwise); weekends and holidays inside the leave are counted. An application is for a staff member of its own
institution and names a leave type that institution offers for the year (422 otherwise). The approval locks the
application and the balance; what other applications still ask for does not hold days back — the first approved
gets them. The decision comment is written by approve and reject only; a decided application is not changed by
the standard update (409), and an approved one is not deleted (409). A leave balance keeps its staff member, leave
type and academic year (409 on change), names a year, and is never set below the days used (422).
Cancelling an approved leave locks the application and the balance, as an approval does, so a cancellation and an
approval cannot pass each other.
`hr.GetStaffLeaveBalance` answers entitled, taken, pending and available days (after both, never below zero) per
active leave type for the academic year the caller names (required); the staff member reads their own, anyone else
also needs `hr.leave_balance.read` in the staff member's institution.

## hr.Staff (`hr.staff`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `hr.ChangeStaffStatus` | `status` = "active" → "inactive" or "exited"; "inactive" → "active" or "exited"; "exited" → "active" (the status asked for decides the move; any other, the current status included, is 409) | `status` = `$param:status` | no |
| `hr.CreateStaffPortalAccount` | `user_id` is null and `status` ≠ "exited" (409); `employee_code` set (422: it becomes the login name) | `user_id` = the person's iam account — the one iam already keeps for the person, otherwise a new staff account (`userType` "staff", username = employee number) created in iam with the caller's own token | no |

Both are hand-written. A staff member who left comes back on the same record (an institution keeps one staff
record per person). The contract has no exit date, so none is kept. Creating the account asks iam outside any
hr transaction: the caller also needs `iam.user_account.read` and `iam.user_account.create` for the tenant (403
otherwise); a login name another account uses is 409; iam unconfigured or failing is 503. The account is stored
only on a staff row that has none, so a repeated or concurrent call is harmless: iam keeps one account per person,
and a call that failed after iam answered finds that account next time. `sendInvite: true` is refused (422):
iam sends no invitations yet; an administrator sets the first password (`iam.SetUserCredentials`). Unlike the
other steps, both publish `hr.staff.updated` with the staff member payload (id, account, person, institution, row
version), because iam keeps `iam.member_record` — what `iam.GetMe` answers as `staffIds` — from hr.staff's
created / updated / deleted events.

## iam.UserAccount (`iam.user_account`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `iam.ActivateUser` | `status` ∈ {"inactive"} | `status` = "active" | no |
| `iam.DeactivateUser` | `status` ∈ {"active", "locked"} | `status` = "inactive" | yes |
| `iam.UnlockUser` | any state | `failed_login_count` = 0, `locked_until` = null | no |

Account workflows (not simple transitions): `iam.ForcePasswordReset` sets `must_reset_password` on an active account
with an e-mail address (422 `NO_EMAIL` without one, 409 when not active), signs it out everywhere and mails a
30-minute reset code (503 `NOT_CONFIGURED` without SMTP); `iam.ResetPassword` clears it and sets the password;
`iam.ConfirmVerification` sets `email_verified_at` while the account still has the address the code went to;
`iam.SwitchRole` sets the session's `active_role_id` / `active_scope_id` and the account's `last_login_role_id`;
`iam.StartImpersonation` opens a session with `impersonator_user_id` that `iam.StopImpersonation` ends.

## library.PurchaseRequest (`library.purchase_request`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `library.SubmitPurchaseRequest` | `status` ∈ {"draft"} | `status` = "requested" | no |
| `library.AcceptPurchaseRequest` | `status` ∈ {"requested"} | `status` = "accepted" | no |
| `library.RejectPurchaseRequest` | `status` ∈ {"requested"} | `status` = "rejected" | yes |

## library.Reservation (`library.reservation`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `library.AcceptReservation` | `status` ∈ {"requested"} | `status` = "accepted" | no |
| `library.RejectReservation` | `status` ∈ {"requested"} | `status` = "rejected" | yes |
| `library.RequestReservationAgain` | `status` ∈ {"rejected", "cancelled"} | `status` = "requested" | no |

`RequestReservationAgain` is hand-written: the requester (their account or person) or a librarian in reach may ask
again; the wanted dates must not have passed (422), and another open request of the requester for the title is 409.

## library.Loan (`library.loan`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `library.RequestLoanReturn` | `status` ∈ {"issued"} | `status` = "return_pending" | no |
| `library.ReturnLoan` | `status` ∈ {"issued", "return_pending"} | `status` = "returned", `returned_on` = `$param:returnedOn`, `fine_amount` = the card type's `fine_per_day` × days past `due_on`; the copy's `status` = "available" | no |
| `library.MarkLoanLost` | `status` ∈ {"issued", "return_pending"} | `status` = "lost", `fine_amount` = the overdue fine up to `$today`; the copy's `status` = "lost" | yes |
| `library.InvoiceLoanFine` | `status` ∈ {"returned", "lost"}; `fine_amount` > 0 | `invoice_id` = the draft invoice finance raises for the fine (`finance.RaiseChargeInvoice`, one per loan) | no |

All four are hand-written. The borrower may drop their own copy for return; the other steps are a librarian's, in
reach. A return on the day it was already returned answers the loan as it is (idempotent); on another day it is 409.
The fine's invoice is raised through finance's gRPC with the caller's token (the holder's name from people), outside
the loan's transaction; the loan is read again before the invoice is recorded on it.

## lms.AssignmentGroupMember (`lms.assignment_group_member`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.AcceptGroupRequest` | `status` ∈ {"requested"} | `status` = "active" | no |
| `lms.RejectGroupRequest` | `status` ∈ {"requested"} | `status` = "rejected" | yes |
| `lms.RequestToJoinGroup` | `status` ∈ {"rejected", "left", "removed"} (or no row yet) | `status` = "requested" | no |
| `lms.SetGroupMembers` | any | `status` = "active" (added) or "removed" (removed) | no |

## lms.EnrollmentRequest (`lms.enrollment_request`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.ApproveEnrollmentRequest` | `status` ∈ {"pending"} | `status` = "approved", `decided_by` = `$actor`, `decided_at` = `$now`; enrols the student in the named cohort | no |
| `lms.RejectEnrollmentRequest` | `status` ∈ {"pending"} | `status` = "rejected", `decided_by` = `$actor`, `decided_at` = `$now` | yes |

## lms.ForumTopic (`lms.forum_topic`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.HideForumTopic` | `is_hidden` ∈ {false} | `is_hidden` = true | no |
| `lms.UnhideForumTopic` | `is_hidden` ∈ {true} | `is_hidden` = false | no |

## lms.ImportExportJob (`lms.import_export_job`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.CancelImportExportJob` | `status` ∈ {"pending"} | `status` = "cancelled" | no |
| `lms.RestoreCohortArchive` | `direction` = "export", `package_format` = "cohort_archive", `status` ∈ {"complete"}, `unarchive_status` ∉ {"complete"}, cohort `lifecycle_status` ∈ {"archived"} | `unarchive_status` = "complete"; the cohort's `lifecycle_status` = "completed" | no |

## lms.QuizSubmission (`lms.quiz_submission`) — hand-written (quiz attempt)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.SaveQuizResponses` | `status` ∈ {"in_progress"}, time not up, attempt token matches | the changed answers, `answered_count`, `last_client_sequence` | no |
| `lms.PauseQuizAttempt` | `status` ∈ {"in_progress"}, quiz `allow_pause` | `status` = "paused", `time_taken_seconds` | no |
| `lms.ResumeQuizAttempt` | `status` ∈ {"paused"} | `status` = "in_progress", `resumed_at` = `$now` | no |
| `lms.SubmitQuiz` | `status` ∈ {"in_progress", "paused"}, attempt token matches | every answer marked against its key; `status` = "graded" (or "submitted" while open-ended answers wait), `score`, `percentage`, `submitted_at` = `$now`; a cohort placement's score posts to the gradebook | no |
| `lms.GradeQuizAnswers` | `status` ∈ {"submitted", "graded"} | the marked answers' `marks_awarded`; `status` = "graded" once no answer waits; the score re-posts | no |

## lms.AssignmentSubmission (`lms.assignment_submission`) — hand-written

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.FinalizeSubmission` | own draft, `finalized_at` IS NULL | `is_final` = true, `finalized_at` = `submitted_at` = `$now` | no |
| `lms.GradeLearningSubmission` | `is_final` = true | `status` = "pass" / "fail" / "not_applicable", `evaluator_user_id` = `$actor`, `evaluator_comments` | no |
| `lms.DeleteLmsAssignmentSubmission` | own draft, `finalized_at` IS NULL, `status` = "not_graded" | soft delete | no |

## lms.PlagiarismReport (`lms.plagiarism_report`) — hand-written

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.SubmitForPlagiarismCheck` | an account is configured (503 otherwise) | a report with the provider's `provider_submission_ref`, `status` = "uploaded" | no |
| `lms.ReceivePlagiarismWebhook` | the provider's signed callback | `status` = "pending" / "complete" / "error", `similarity_score`, `error_code` | no |

## lms.Ebook conversion (`lms.ebook`) — hand-written

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.ConvertEbook` | `source_file_id` set (409 otherwise); the service configured (503 otherwise) | `conversion_job_ref`; when the job finishes within the wait, `file_id` (the stored converted file) | no |
| `lms.ReceiveEbookConversion` | the service's signed callback naming a sent job | `conversion_output_url` or `conversion_error`; the next ConvertEbook stores or surfaces it | no |

## lms.ExternalTool (`lms.external_tool`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.ActivateExternalTool` | `is_paid` = true, `is_included` = false, the licence key's SHA-256 matches the tool's `activation_key_sha256` setting | `is_included` = true, setting `activated_at` = `$now` | no |

## lms.Cohort (`lms.cohort`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.PublishCohort` | `is_published` ∈ {false} and `lifecycle_status` ∈ {"draft", "active"} | `is_published` = true | no |
| `lms.UnpublishCohort` | `is_published` ∈ {true} | `is_published` = false | no |
| `lms.ActivateCohort` | `lifecycle_status` ∈ {"draft", "suspended"} | `lifecycle_status` = "active" | no |
| `lms.DeactivateCohort` | `lifecycle_status` ∈ {"active"} | `lifecycle_status` = "suspended" | yes |
| `lms.ArchiveCohort` | `lifecycle_status` ∈ {"active", "suspended", "completed"} | `lifecycle_status` = "archived", `is_published` = false | no |

## lms.LiveClass (`lms.live_class`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.EndLiveClass` | `ended_at` IS NULL | `ended_at` = `$now`, `is_active` = false | no |
| `lms.RetryLiveClassInvites` | `ended_at` IS NULL | `invite_status` = "retrying", `invite_attempts` + 1 | no |

## lms.Evaluator (`lms.evaluator`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.ConnectEvaluatorCalendar` | any live row | `calendar_provider` = "google", `calendar_state` = fresh nonce, `calendar_requested_at` = `$now`, `calendar_connected_at` = NULL | no |
| `lms.DisconnectEvaluatorCalendar` | `calendar_provider` IS NOT NULL | `calendar_provider` = NULL, `calendar_state` = NULL, `calendar_requested_at` = NULL, `calendar_connected_at` = NULL | no |

## lms.ForumPost (`lms.forum_post`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.HideForumPost` | `is_hidden` ∈ {false} | `is_hidden` = true (replies covered too) | yes |
| `lms.UnhideForumPost` | `is_hidden` ∈ {true} | `is_hidden` = false | no |
| `lms.ConfirmPostAbuse` | `is_abusive` ∈ {false} and reported | `is_abusive` = true, `is_hidden` = true, `abuse_confirmed_by` = `$actor`, `abuse_confirmed_at` = `$now` | yes |
| `lms.MarkPostSubstantive` | any live row | `substantive_flag` = 1 or 0 | no |

## lms.CertificateRequest (`lms.certificate_request`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.CancelCertificateRequest` | `status` ∈ {"upcoming"} | `status` = "cancelled" | yes |
| `lms.StartEvaluation` | `status` ∈ {"upcoming"} and no evaluation yet | `status` = "completed", an in-progress `lms.certificate_evaluation` created | no |

## lms.LmsCourse (`lms.course`)

Review (`status`) and lifecycle (`lifecycle_status`, kept in step with `is_published`) are separate: a course is
reviewed, then published; the public (api/README "Guests") sees published courses only, and the catalogue only those
whose listing is published too.

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.SubmitCourseForReview` | `status` ∈ {"in_progress"} | `status` = "under_review" | no |
| `lms.ApproveCourse` | `status` ∈ {"under_review"} | `status` = "approved" | no |
| `lms.PublishCourse` | `lifecycle_status` ∈ {"draft"} and `status` = "approved" | `lifecycle_status` = "published", `is_published` = true, `published_on` = `$today` when not set | no |
| `lms.UnpublishCourse` | `lifecycle_status` ∈ {"published"} | `lifecycle_status` = "draft", `is_published` = false | no |
| `lms.DeactivateCourse` | `lifecycle_status` ∈ {"published"} | `lifecycle_status` = "deactivated", `is_published` = false, `deactivated_on` = `$today` | yes |
| `lms.ReactivateCourse` | `lifecycle_status` ∈ {"deactivated"} | `lifecycle_status` = "published", `is_published` = true, `deactivated_on` = null | no |
| `lms.PublishCatalogListing` | `catalog_status` ∈ {"draft", "archived"} and `lifecycle_status` = "published" | `catalog_status` = "published", `is_in_catalog` = true | no |
| `lms.HideCatalogListing` | `catalog_status` ∈ {"published"} | `catalog_status` = "archived", `is_in_catalog` = false | no |

## lms.ReleaseRule (`lms.release_rule`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.DisableReleaseRule` | `is_disabled` ∈ {false} | `is_disabled` = true | no |
| `lms.EnableReleaseRule` | `is_disabled` ∈ {true} | `is_disabled` = false | no |

## org.AcademicYear (`org.academic_year`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `org.ActivateAcademicYear` | `status` ∈ {"planned"}; no other year of the institution "active" | `status` = "active", `is_current` = true; any other year of the institution `is_current` = false | no |
| `org.CloseAcademicYear` | `status` ∈ {"active"} | `status` = "closed", `is_current` = false | no |

`ActivateAcademicYear` is hand-written: one active year per institution (409 while another is active; the unique index
`uq_academic_year_active` backs it), and its `org.academic_year.activate` event names the institution.
`GenerateTerms` creates the terms of the year's `term_structure` (quarters inside semesters), keeps those already
there and refuses overlapping ones (409) and closed years.

## payroll.Payslip (`payroll.payslip`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `payroll.CancelPayslip` | `status` ∈ {"unpaid"} | `status` = "cancelled" | yes |
| `payroll.MarkPayslipPaid` | `status` ∈ {"unpaid"} | `status` = "paid"; a payroll payment: `amount` = the payslip's `net_amount`, `staff_id`, `payslip_id`, `institution_id` of the payslip, `disbursement_account_id` = `$param:disbursementAccountId`, `payment_method` = `$param:paymentMethod`, `transaction_date` = `$param:transactionDate`, `reference_number` = `$param:referenceNumber`, `transaction_number` from the institution's `PP-` series | no |

A payslip is created only by `payroll.GeneratePayslips` (hand-written, 202 with a `payroll` operation whose
`result` holds `generated`, `kept`, `skippedCount` and up to 200 `skipped` staff with a reason: `NO_SALARY`,
`NO_COMPONENTS`, `NEGATIVE_NET`, `OVERLAPPING_PERIOD`). A run pays every staff member whose salary in force on the
period's last day belongs to the institution through its salary template (or only the `staffIds` named), for a
period within one calendar month (422 otherwise): salary amounts are monthly, so each line is a staff salary
component's amount × the days of the period that salary is in force ÷ the days of the month, rounded half-up to the
cent — a month paid in two periods is paid once. Only the salaries of the run's institution pay: days under another
institution's salary (a transfer within the month) are that institution's, paid by its own run for its part of the
month. Gross is the sum of the earning lines, deductions the sum of the deduction lines, net their difference
(`defined` lines are listed and count in neither). Each payslip is written with its lines in its own transaction,
numbered from the institution's `PS-` series, audited, with a `payroll.payslip.created` event. A staff member who
already has an unpaid or paid payslip for the same period keeps it (to recalculate, cancel it and run again); one
for a period sharing days with it is skipped. Concurrent runs lock the staff member's salary rows and are backed by
the unique index `uq_payslip_period`. The operation's start and end are audited (`finish` step) with
`payroll.operation.created`, `.succeeded` / `.failed` events. `MarkPayslipPaid` needs a disbursement account of the
payslip's institution (422) and writes no payment for a net of 0; the payment is audited with a
`payroll.payroll_payment.created` event. A paid or cancelled payslip and its lines never change.
Events: `payroll.payslip.created`, `.markPaid`, `.cancel`.

Payslips are owner-only personal data: `payroll.GetPayslip`, `ListPayslips`, `GetPayslipLine` and
`ListPayslipLines` show a caller the payslips of the institutions where they hold `payroll.payslip.generate_payslips`
(the payroll office), and otherwise only the payslips of their own staff records — known from `hr.staff` events
(`payroll.staff_member`); any other payslip is 404 on a get and absent from a list.

## platform.FileObject (`platform.file_object`) — hand-written (api/README "Files")

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `platform.CreateUploadUrl` | — (creates the file) | a new file, `status` = "pending", `uploaded_by_user_id` = `$actor`; a V4 signed PUT URL (15 minutes) | no |
| `platform.CompleteUpload` | `status` ∈ {"pending"}, by the uploader only (another user: 404); the stored object exists (409 `UPLOAD_MISSING`) with the declared size and content type and, when declared, SHA-256 (422; the object is removed so the upload can be repeated); the bytes fit the uploader's enforced quota (409 `QUOTA_EXCEEDED`) | `status` = "available"; the uploader's `storage_quota.used_bytes` += size | no |
| `platform.DeleteFileObject` | any live file, by the uploader or a tenant-wide `platform.file_object.delete`; no live attachment or theme uses it (409) | `deleted_at` = `$now`, `status` = "deleted"; the uploader's quota gives the bytes back; the object leaves the store after the commit | no |

The upload is refused before it starts (422) when the tenant's upload policy for the owner's module (role-less
policies; the one naming the collection wins) or the platform defaults (100 MB, the extension catalogue of
`validation/FileKinds`) do not allow it, and (409) when the uploader's quota or the plan's `max_files` is used up.
Attaching a file to a record (`ownerEntityType` = `schema.table`) needs the owner's `<schema>.<table>.create` or
`.update` permission; reading it, the uploader, a tenant-wide `platform.file_object.read`, the owner's `.read`, the
`.read` of a record type it is attached to, or a theme that shows it. No malware scanner is configured: `scanned_at`
stays empty. `platform.CopyFile` makes a new available file of the caller from a file they may read (quota and plan
limit apply). Events: `platform.file_object.created`, `.completeUpload`, `.deleted`.

## platform imports (`platform.import_job`, its `platform.operation`) — hand-written

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `platform.StartImport` | — (creates the import and its operation); the caller holds the entity type's create permission (`sis.student.create`, `sis.guardian.create`, `sis.promotion.create`, `hr.staff.create`, `library.media.create`, `org.holiday.create`) for `institutionId` — tenant-wide for tenant-level records (guardians; holidays without an institution) — else 403; the file is the caller's readable, available (409), `.csv` (422) upload of at most 5 MB and 10,000 rows (422) | a new operation `status` = "running", `started_by_user_id` = `$actor`; the import with its row count and the template check's row errors; one `platform.import.batch_requested` event per 200 valid rows (rows sealed for the tenant). A dry run, or a file without a valid row, sends nothing: the operation is `succeeded` at once | no |
| `<module>.import.batch_applied` (event of the owning module) | the operation's import sent that batch and has not had its answer (a repeated answer changes nothing); the answer comes from the module that owns the entity type and fits the import (else rejected) | `applied_batch_count` + 1, `created_count` and `error_count` added, the batch's row errors stored; `progress_percent` = answered share; with the last batch the operation `status` = "succeeded", `progress_percent` = 100, `finished_at` = `$now` | no |
| `platform.CancelOperation` (an import) | never: its rows are with the owning module (409) | — | — |

The owning module (sis, hr, library, org) creates each row through its own create service as the importing user,
holding only that create permission in only that institution (core `ImportBatchApplier`, auth-client
`ImportCallerIdentity`); a refused row is a row error, the others are kept; a system failure rolls the batch back
for redelivery. The operation's `result`: `entityType`, `institutionId`, `fileId`, `dryRun`, `rowCount`,
`createdCount`, `errorCount` and the first 1,000 row errors by row (`rowNumber`, `field`, `message`). Events:
`platform.operation.created`, `.succeeded`; `platform.import.batch_requested`; `<module>.import.batch_applied`.

## platform.SalesEnquiry (`platform.sales_enquiry`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `platform.MarkEnquiryContacted` | `status` ∈ {"new"} | `status` = "contacted" | yes (optional) |

Platform staff only (`platformAdminAuth`); the enquiry is global data, so its audit record has no tenant. Event:
`platform.sales_enquiry.markContacted`.

## platform.TenantSubscription (`platform.tenant_subscription`) — hand-written

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `platform.StartSubscriptionCheckout` | — (creates the subscription); the plan active and priced (422) | a new subscription, `status` = "pending", the plan's limits, price, currency and cycle copied, `gateway_txn_ref` = the gateway order | no |
| `platform.ConfirmSubscriptionCheckout` | `status` ∈ {"pending"} (an active one on the same order answers unchanged; else 409); the checkout signature over the subscription's own order and the payment (422 `INVALID_SIGNATURE`) | `status` = "active", `start_date` = `$today` or the day after the scope's last paid day, `end_date` = start + cycle − 1 day, `amount_paid` = `price`, `paid_at` = `$now` | no |
| `platform.ReceiveSubscriptionWebhook` (`order.paid`) | the body signed with the webhook secret (401); `status` ∈ {"pending"} | as ConfirmSubscriptionCheckout, with the payment's method and payer contact; on a cancelled subscription only the payment is kept (`gateway_status` = "paid_after_cancel") for a refund | no |
| `platform.CancelSubscription` | `status` ∈ {"pending", "active"} | `status` = "cancelled" (an active subscription stays in force until `end_date`) | yes (optional) |

A subscription becomes active only on proof of payment, once, whichever proof arrives first. The tenant of a webhook
comes from the order notes the signature covers. Gateway calls are never made inside a database transaction. Events:
`platform.tenant_subscription.created`, `.activate`, `.cancel`.

## privacy.Breach (`privacy.breach`) — hand-written

`privacy.ReportBreach` creates the row with `status` = "reported" and `board_report_due_at` = `became_aware_at` + 72 h.
Every step needs a tenant-wide grant, is refused while impersonating (403), compares the body etag (412) and is one
update guarded by `row_version` and the status it leaves. `privacy.UpdateBreach` is no step: it changes what is known
of an open breach (`closed_at` is null, 409 otherwise) under If-Match. Notices are recorded, not sent (comms delivery
is not built). Overdue and the hours remaining are derived at read time. Events: `privacy.breach.reported`,
`.updated`, `.triaged`, `.contained`, `.notification_recorded`, `.closed`, `.not_a_breach`.

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `privacy.TriageBreach` | `status` ∈ {"reported"} | `status` = "triaged" | no |
| `privacy.ContainBreach` | `status` ∈ {"triaged"} | `status` = "contained" | no |
| `privacy.RecordBreachNotification` (`audience` = "board", `kind` = "initial") | `status` ∈ {"contained"}, `$param:sentAt` between `became_aware_at` and `$now` (422) | a `privacy.breach_notification` row; `status` = "board_initial_sent" | no |
| `privacy.RecordBreachNotification` (`audience` = "board", `kind` = "detailed") | `status` ∈ {"board_initial_sent", "principals_notified"} and `board_report_sent_at` is null | the notice; `board_report_sent_at` = `$param:sentAt`; `status` = "board_report_sent" from "board_initial_sent" | no |
| `privacy.RecordBreachNotification` (`audience` = "principals") | `status` ∈ {"board_initial_sent", "board_report_sent", "principals_notified"} | the notice; `status` = "principals_notified" | no |
| `privacy.RecordBreachNotification` (a Board update; `audience` = "processor" or "tenant") | `closed_at` is null (a Board update: `status` ∈ {"board_initial_sent", "board_report_sent", "principals_notified"}) | the notice; the status stays | no |
| `privacy.CloseBreach` | `status` ∈ {"principals_notified"} and `board_report_sent_at` is set | `status` = "closed", `closed_at` = `$now` | no |
| `privacy.MarkBreachNotABreach` | `status` ∈ {"triaged"} | `status` = "not_a_breach", `not_a_breach_reason` = `$param:reason`, `closed_at` = `$now` | yes (`reason`) |

## privacy.NoticeVersion (`privacy.notice_version`) — hand-written

A version is edited while `draft`; once published it never changes, and its translations change only while it is a
draft (409 otherwise). One version per notice is `active`.

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `privacy.PublishNoticeVersion` | `status` ∈ {"draft"} and at least one live translation | `status` = "active", `published_at` = `$now`; the notice's previously active version: `status` = "retired", `superseded_by_id` = this version (its own `retire` audit record and `privacy.notice_version.retired` event) | no |

## privacy.RightsRequest (`privacy.rights_request`) — hand-written

A request submitted while signed in is `verified` at once (`identity_check` = "signed_in_session"), except a nominee's
(`requester_capacity` = "nominee"), which stays `submitted` until staff verify it. Every step needs a tenant-wide grant,
is refused while impersonating (403) and to a staff member who is the request's subject or requester (409
`RIGHTS_REQUEST_CONFLICT_OF_INTEREST`).
An extension is not a state: it moves `due_at`. An erasure or export request is fulfilled by one task per module
(`privacy.RightsRequestTask` below); its saga moves the request without a staff step (no actor): to "action_required"
once no started task waits (for an answer or its next attempt) and one failed for the fifth time, and to "closed" once every task answered. Events:
`privacy.rights_request.submitted`, `.verified`, `.assigned`, `.extended`, `.rejected`, `.closed`,
`.fulfilment_started`, `.task_retried`, `.action_required`; an erasure's close also `privacy.erasure.completed`.

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `privacy.VerifyRightsRequestIdentity` | `status` ∈ {"submitted"} | `status` = "verified", `identity_check` = `$param:identityCheck` | no |
| `privacy.AssignRightsRequest` | `status` ∈ {"verified", "in_progress"}, and `$param:assigneeUserId` is no account privacy knows for the subject or requester (409) | `status` = "in_progress", `assignee_user_id` = `$param:assigneeUserId` | no |
| `privacy.ExtendRightsRequest` | `closed_at` is null, and `$param:dueAt` later than `due_at` (422 otherwise) | `due_at` = `$param:dueAt`, `extended_reason` = `$param:reason` | yes (`reason`) |
| `privacy.RejectRightsRequest` | `status` ∈ {"submitted", "verified"} | `status` = "rejected", `outcome_code` = `$param:reasonCode`, `closed_at` = `$now` | yes (the reason code) |
| `privacy.CloseRightsRequest` | `status` ∈ {"in_progress"}, and not `type` ∈ {"access", "erasure", "export", "withdraw_all"} with `$param:outcomeCode` ∈ {"fulfilled", "partially_fulfilled"} (each is fulfilled by its own step; 409), not an erasure or export whose fulfilment started (409), not `type` = "nomination" with "partially_fulfilled" (409), and `$param:response` given for a grievance or a declined correction (422) | `status` = "closed", `outcome_code` = `$param:outcomeCode`, `outcome_note` = `$param:note`, `outcome_response` = `$param:response`, `closed_at` = `$now`; a nomination closed as "fulfilled" also sets the requester's `privacy.nominee` row `activated_at` = `$now`, `activated_by_request_id` = this request (event `privacy.nominee.activated`) | no |
| `privacy.FulfilAccessRequest` | `type` = "access" and `status` ∈ {"in_progress"} | `status` = "closed", `outcome_code` = "fulfilled", `access_summary` = the subject's access summary generated at `$now`, `closed_at` = `$now` (event `.closed`) | no |
| `privacy.StartRightsRequestFulfilment` | `type` ∈ {"erasure", "export"}, `status` ∈ {"in_progress"}, no task yet (409); body etag required | one `privacy.rights_request_task` per module of `mysms.privacy.erasure.modules`, wave 1 dispatched; `status` stays "in_progress" | no |
| `privacy.RetryRightsRequestTask` | `type` ∈ {"erasure", "export"}, `status` ∈ {"in_progress", "action_required"}; the task (of this request, 404) "failed" or "dispatched" (409) | the task dispatched again; `status` = "in_progress" | no (the task id is kept as the step's reason) |
| `privacy.FulfilWithdrawAllRequest` | `type` = "withdraw_all" and `status` ∈ {"in_progress"}; body etag required | every live consent record the requester gave about the subject (the subject's own too for a nominee) withdrawn; `status` = "closed", `outcome_code` = "fulfilled", `closed_at` = `$now` (event `.closed`) | no |
| (saga) a task answered | `status` ∈ {"in_progress", "action_required"} and no started task still dispatched or waiting for its next attempt | a failed task → `status` = "action_required"; else pending tasks dispatched; else `status` = "closed", `outcome_code` = "partially_fulfilled" (a task partial) or "fulfilled", `closed_at` = `$now` | no |

## privacy.RightsRequestTask (`privacy.rights_request_task`) — hand-written

A record of the rights request: created "pending" by `privacy.StartRightsRequestFulfilment` (wave 1 dispatched at once),
answered by its module's `<module>.privacy_task.completed` (the request's row locked first). Each dispatch increments
`attempts`, clears `next_attempt_at` and publishes `privacy.rights_request.task_assigned`; an answer to a task not
"dispatched" changes nothing. A failed answer before the fifth attempt waits a back-off before the sweep dispatches it
again: `mysms.privacy.erasure.retry-delay` doubled per attempt after the first, at most
`mysms.privacy.erasure.redispatch-after`.

| Transition | Allowed from | Sets |
|---|---|---|
| dispatch (start, the next wave, a retry, the sweep after `mysms.privacy.erasure.redispatch-after` unanswered or once a failed task's `next_attempt_at` has come) | "pending", "dispatched" (sweep, retry), "failed" (retry; sweep when `next_attempt_at` ≤ `$now`) | `status` = "dispatched", `attempts` + 1, `dispatched_at` = `$now`, `next_attempt_at` = null, the answer cleared |
| the module's answer | "dispatched" | `status` = the answer's ("done", "partial", "failed", "not_applicable"), `completed_at` = `$now`, `result`; "failed" also `last_error_code`, and before the fifth attempt `next_attempt_at` = `$now` + the back-off |

## privacy.LegalHold (`privacy.legal_hold`) — hand-written

`privacy.PlaceLegalHold` creates the row active (`released_at` null) with `review_by` today or later (422). Every
change needs a tenant-wide grant and is refused while impersonating (403); a released hold changes no more (409).
Due for review = active and `review_by` ≤ today (derived). Events: `privacy.legal_hold.placed`, `.updated`,
`.released`.

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `privacy.UpdateLegalHold` | `released_at` is null; If-Match (412); `review_by` today or later (422) | `review_by` | no |
| `privacy.ReleaseLegalHold` | `released_at` is null; body etag when given (412) | `released_at` = `$now`, `released_by_user_id` = the caller | no |

## privacy.Nominee (`privacy.nominee`) — hand-written

Self-service of an adult principal; refused while impersonating (403). Events: `privacy.nominee.created`, `.revoked`,
`.activated`.

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `privacy.SetMyNominee` | the caller is an adult (403 otherwise); the active row's etag when given (412) | the active row: `status` = "revoked", `revoked_at` = `$now`; a new row `status` = "active" with the encrypted name and contact | no |
| `privacy.RevokeMyNominee` | an active row (404 otherwise), its etag (412) | `status` = "revoked", `revoked_at` = `$now` | no |

## sis.Promotion (`sis.promotion`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `sis.ApprovePromotion` | `status` ∈ {"proposed"} | `status` = "approved" | no |
| `sis.RejectPromotion` | `status` ∈ {"proposed"} | `status` = "rejected" | yes |
| `sis.ApplyPromotions` (hand-written, flow SIS-03) | `status` ∈ {"approved"}, a next year and grade named | `status` = "applied"; the student's open `student_enrollment` of the promotion's year ends (`enrollment_status` = "promoted", or "detained" when the grade stays, `effective_to` = `$today`) and a placement of the next year begins (`change_reason` = "promotion", `previous_enrollment_id` = the ended one, `effective_from` = `enrolled_on` = `$today`, `class_section_id` = academics' class of the target year, grade and section, none without a section) | no |

`sis.ProposePromotions` (hand-written) writes a `proposed` promotion for each student placed in a class (or each
listed one) after checking that the target year (org), grade and section (academics) are the class's institution's
own (422 on the field otherwise); a student with a standing promotion out of the class's year keeps it and gets no second one. Apply is
one transaction for all the promotions it names (1–500): a promotion that is not approved, a student without an
open placement in the year or already placed in the next year, or a named target class academics does not have is
409 and nothing is applied; the answer is a finished `Operation` (202). Events: `sis.promotion.created`, `.apply`,
`sis.student_enrollment.updated` / `.created`, `sis.operation.succeeded`.

## sis.StudentLeaveRequest (`sis.student_leave_request`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `sis.ApproveStudentLeaveRequest` | `status` ∈ {"pending"} | `status` = "approved", `decided_by_user_id` = `$actor`, `decided_at` = `$now`, `decision_note` = `$param:decisionNote` | no |
| `sis.RejectStudentLeaveRequest` | `status` ∈ {"pending"} | `status` = "rejected", `decided_by_user_id` = `$actor`, `decided_at` = `$now`, `decision_note` = `$param:decisionNote` | no |

## sis.SubjectRegistration (`sis.subject_registration`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `sis.SubmitSubjectRegistration` | `status` ∈ {"draft"} | `status` = "submitted" | no |
| `sis.RejectSubjectRegistration` | `status` ∈ {"submitted"} | `status` = "rejected" | yes |
| `sis.ResetSubjectRegistration` | `status` ∈ {"rejected"} | `status` = "draft" | no |
| `sis.ApproveSubjectRegistration` (hand-written) | `status` ∈ {"submitted"}, at least one line (409) | `status` = "approved"; each line's subject the student's course registration does not cover yet becomes a `program_enrollment_subject` of `program_enrollment_id` | no |
| `sis.LoadCourseSubjects` (hand-written) | `status` ∈ {"draft"} | a `subject_registration_line` for each subject of the course's plan in academics that the draft does not list yet: `selection_type` = "elective" when the plan's `subjectCategory` is elective, else "compulsory" | no |

The course's plan is read from academics with the caller's token (403 names academics' permission, 503 without
the service); a draft changed while the plan was read is 412. The registration is locked while lines are added, and
added lines give it a new `row_version` (event `sis.subject_registration.loadCourseSubjects`), so a submit made
meanwhile waits and then finds a draft whose etag moved on. Events: `sis.subject_registration.approve`,
`sis.subject_registration_line.created`, `sis.program_enrollment_subject.created`.

## sis.DisciplineIncident (`sis.discipline_incident`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `sis.MarkIncidentActionTaken` | `action_taken` ∈ {false} | `action_taken` = true, `response` = `$param:response` when given (trimmed; the stored one stays otherwise) | no |
| `sis.MarkIncidentSeen` (hand-written) | `notify_guardians` ∈ {true}, `seen_by_guardian` ∈ {false} | `seen_by_guardian` = true | no |

Mark seen is the guardian's acknowledgement: an active guardian of the incident's student (a `student_guardian`
link of the caller's account or person) may do it for their own child in any institution, and to them an incident
the school did not share does not exist (404). Anyone else records it for the guardian as staff: they also need
`sis.discipline_incident.read` and the incident's institution in their writable scope.

## sis.Student (`sis.student`) — hand-written

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `sis.ChangeStudentStatus` | any `status` but the requested one | `status` = `$param:status`; for an exit status (withdrawn, transferred, graduated, alumni) `exit_date` = `$param:exitDate` (required, not before admission or joining) and `exit_reason` = `$param:exitReason`, and every open `student_enrollment` ends on the exit date (`enrollment_status` = "completed" for graduated and alumni, else "left" with `left_on`); for active or inactive the exit details are cleared (and may not be sent: 422) | yes (`exitReason`) |
| `sis.IssueBonafideCertificate` | `status` ∈ {"active"} | `bonafide_certificate_number` = the next number of the institution's `bonafide_certificate` series for the UTC year (`sis.number_sequence`, `BC/<year>/000001`) | yes (`purpose`) |
| `sis.CreateStudentPortalAccount` | `status` ∈ {"active"}, no `user_id`, an admission number | `user_id` = the account opened in iam (signs in with the admission number; role `student` in the student's institution) | no |

`sis.CreatePortalAccount` does the same for a guardian (no `user_id`, at least one active child link, an e-mail
address or mobile number to sign in with; role `guardian` in each child's institution, all of which the caller must
be able to write in). The account is opened in iam with the caller's token before the row is linked (the person's
existing account is reused, a tenant without the persona's role is 409); the link is written only while the row
still has no account, and the row is checked again there (active, in the caller's writable scope — for a guardian,
every child's institution). `sendInvite: true` is refused (422), as in hr: iam sends no invitations yet. Events: `sis.student.changeStatus` and `.issueBonafideCertificate` (with the member payload),
`sis.student.updated` / `sis.guardian.updated` for a linked account, `sis.operation.succeeded`. Issuing a bonafide
certificate answers a finished `Operation` (202) whose result is the student; the PDF is not rendered yet (the
platform file store is not served).

## timetable.Session (`timetable.session`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `timetable.ConfirmSession` | `status` ∈ {"draft"} | `status` = "confirmed" | no |
| `timetable.CompleteSession` | `status` ∈ {"confirmed"} | `status` = "done" | no |
| `timetable.CancelSession` | `status` ∈ {"draft", "confirmed"} | `status` = "cancelled" | yes |
| `timetable.ResetSession` | `status` ∈ {"cancelled"} | `status` = "draft" | no |
| `timetable.BatchConfirmSessions` | each `status` ∈ {"draft", "confirmed"} | `status` = "confirmed" (a confirmed one is kept) | no |

`BatchConfirmSessions` is hand-written: up to 500 sessions, all or nothing (409 when one is done or cancelled).

## frontoffice.VisitorLog (`frontoffice.visitor_log`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `frontoffice.CheckOutVisitor` | `exit_time` empty | `exit_time` = `$param:exitTime` (not before `entry_time`: 422) | no |
