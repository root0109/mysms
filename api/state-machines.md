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
| `admission.CloseAdmissionCycle` | `status` ∈ {"open", "admission"} | `status` = "closed" | no |
| `admission.CancelAdmissionCycle` | `status` ∈ {"draft", "confirmed", "open"} | `status` = "cancelled" | yes |
| `admission.ResetAdmissionCycle` | `status` ∈ {"cancelled"} | `status` = "draft" | no |

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

## assessment.Exam (`assessment.exam`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `assessment.ScheduleExam` | `status` ∈ {"draft"} | `status` = "scheduled" | no |
| `assessment.CompleteExam` | `status` ∈ {"held", "result_updated"} | `status` = "done" | no |
| `assessment.CancelExam` | `status` ∈ {"draft", "scheduled"} | `status` = "cancelled" | yes |
| `assessment.ResetExam` | `status` ∈ {"cancelled"} | `status` = "draft" | no |

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
| `assessment.CancelMarksheet` | `status` ∈ {"draft", "validated"} | `status` = "cancelled" | yes |
| `assessment.ResetMarksheet` | `status` ∈ {"cancelled"} | `status` = "draft" | no |

## attendance.AttendanceSheet (`attendance.attendance_sheet`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `attendance.StartAttendanceSheet` | `status` ∈ {"draft"} | `status` = "in_progress" | no |
| `attendance.CompleteAttendanceSheet` | `status` ∈ {"in_progress"}, with at least one mark | `status` = "done" | no |
| `attendance.CancelAttendanceSheet` | `status` ∈ {"draft", "in_progress"} | `status` = "cancelled" | yes |
| `attendance.ResetAttendanceSheet` | `status` ∈ {"cancelled", "done"} | `status` = "draft" | no |

Marks are recorded with `attendance.RecordAttendance` while `status` = "in_progress" (each call gives the sheet a new
version). Reopening a completed sheet (`ResetAttendanceSheet`, then `StartAttendanceSheet`) keeps its marks.

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

## iam.UserAccount (`iam.user_account`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `iam.ActivateUser` | `status` ∈ {"inactive"} | `status` = "active" | no |
| `iam.DeactivateUser` | `status` ∈ {"active", "locked"} | `status` = "inactive" | yes |
| `iam.UnlockUser` | any state | `failed_login_count` = 0, `locked_until` = null | no |

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

## lms.AssignmentGroupMember (`lms.assignment_group_member`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `lms.AcceptGroupRequest` | `status` ∈ {"requested"} | `status` = "active" | no |
| `lms.RejectGroupRequest` | `status` ∈ {"requested"} | `status` = "rejected" | yes |

## lms.EnrollmentRequest (`lms.enrollment_request`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
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

## org.AcademicYear (`org.academic_year`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `org.CloseAcademicYear` | `status` ∈ {"active"} | `status` = "closed", `is_current` = false | no |

## payroll.Payslip (`payroll.payslip`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `payroll.CancelPayslip` | `status` ∈ {"unpaid"} | `status` = "cancelled" | yes |

## sis.Promotion (`sis.promotion`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `sis.ApprovePromotion` | `status` ∈ {"proposed"} | `status` = "approved" | no |
| `sis.RejectPromotion` | `status` ∈ {"proposed"} | `status` = "rejected" | yes |

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

## timetable.Session (`timetable.session`)

| Operation | Allowed from | Sets | Reason kept |
|---|---|---|---|
| `timetable.ConfirmSession` | `status` ∈ {"draft"} | `status` = "confirmed" | no |
| `timetable.CompleteSession` | `status` ∈ {"confirmed"} | `status` = "done" | no |
| `timetable.CancelSession` | `status` ∈ {"draft", "confirmed"} | `status` = "cancelled" | yes |
| `timetable.ResetSession` | `status` ∈ {"cancelled"} | `status` = "draft" | no |
