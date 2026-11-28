# Cross-module logical references

**There are no cross-module foreign keys anywhere in MySMS** — each module's schemas may be deployed on their own PostgreSQL instance, so a reference into another module's schema is *logical*: a plain uuid column, always indexed. Inside one module the schemas deploy together, so a foreign key between hrms's sibling schemas (`hr`, `payroll`, `time_attendance`) is allowed and not registered here. Integrity is enforced by five mechanisms instead of the database:

1. **No live check at write time** (owner decision, option C): a reference is stored as given — a uuid, in the caller's tenant (row-level security) — without asking the owning module. A live check would need service-to-service identity (ROADMAP P1-34); with the caller's own token, a user who may not read the owner's records could not save a valid reference. Dangling references are prevented and repaired by rules 2 and 3.
2. **No hard deletes of referenced entities** — owners archive by status / soft delete; every money- and evidence-protecting RESTRICT rule is an in-module foreign key and stays in the DDL.
3. **Events drive cleanup** — the owner's `*.deleted` outbox events reach dependents' inboxes over the bus (RabbitMQ by default); the *event action* column below says what the dependent does. Built: each dependent module's `<Module>ReferenceCleanup` (owned source; first written by a generator that has since been retired) clears the column or soft-deletes the row, audits it and announces it on its own outbox (`<table>.updated` / `<table>.deleted`), so the cleanup cascades; it runs exactly once per event through the module's inbox. Both tables below are built — 153 actions in 20 modules: in the against-DAG table "delete the row" is the same soft delete and "archive/retain" needs no action; each action of these tables is implemented in its module's cleanup class. Two actions are kept by design: `comms.in_app_notification` is append-only (no `deleted_at`), so the notifications of a deleted user or cohort age out with the table's partition retention.
4. **Reconciliation** — a periodic per-module job batch-verifies reference ids against the owner via the SDK and reports orphans (never auto-fixes). **Not built:** like a live check, it needs service-to-service identity (P1-34).
5. **Row-level security** stays the cross-tenant backstop on both sides.

**Tenant references:** every tenant-owned table carries `tenant_id` (logically `platform.tenant.id`; 450 tables). Tenant validity comes from the authenticated token and host→tenant resolution; RLS scopes every row. No FK.

**Not registered here by design:** actor columns (`created_by`, `updated_by`, `operation.started_by_user_id`, `idempotency_key.user_id`, `outbox_event.aggregate_id`) — audit/scope values that must outlive the account or aggregate they name — and the polymorphic `*_type` / `*_id` pairs listed in the database README.

Validated against the live catalog when it was written (every column exists, is indexed, and its owner exists in another schema); keep it in step with the migrations and the ER diagrams.

## academics (14 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `academics.class_section.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `academics.class_section.class_teacher_staff_id` | `hr.staff` | null the column |
| `academics.class_section.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `academics.course.department_id` | `org.department` | owner is never hard-deleted (archived by status); no action needed |
| `academics.course.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `academics.grade_level.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `academics.lesson_plan_approval.decided_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `academics.program.department_id` | `org.department` | owner is never hard-deleted (archived by status); no action needed |
| `academics.program.image_file_id` | `platform.file_object` | null the column |
| `academics.program.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `academics.section.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `academics.subject.department_id` | `org.department` | owner is never hard-deleted (archived by status); no action needed |
| `academics.subject.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `academics.teaching_assignment.staff_id` | `hr.staff` | owner is never hard-deleted (archived by status); no action needed |
| `academics.teacher_subject.staff_id` | `hr.staff` | `hr.staff.deleted` archives the row (`AcademicsReferenceCleanup`) |

`academics.staff_record` is not a set of references but a copy kept from events, like `iam.member_record`: each row
mirrors the person of one `hr.staff` row from that row's own created and updated events (`StaffRecordSync`), so
privacy's tasks find a teacher's lesson plans without reading another schema. A deleted staff record keeps its row.

## admission (26 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `admission.admission_cycle.academic_term_id` | `org.academic_term` | owner is never hard-deleted (archived by status); no action needed |
| `admission.admission_cycle.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `admission.admission_cycle.application_fee_head_id` | `finance.fee_head` | owner is never hard-deleted (archived by status); no action needed |
| `admission.admission_cycle.course_id` | `academics.course` | owner is never hard-deleted (archived by status); no action needed |
| `admission.admission_cycle.grade_level_id` | `academics.grade_level` | owner is never hard-deleted (archived by status); no action needed |
| `admission.admission_cycle.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `admission.admission_cycle.program_id` | `academics.program` | owner is never hard-deleted (archived by status); no action needed |
| `admission.admission_cycle_fee.course_id` | `academics.course` | owner is never hard-deleted (archived by status); no action needed |
| `admission.admission_cycle_fee.fee_head_id` | `finance.fee_head` | owner is never hard-deleted (archived by status); no action needed |
| `admission.application.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `admission.application.applicant_person_id` | `people.person` | null the column |
| `admission.application.batch_id` | `academics.batch` | owner is never hard-deleted (archived by status); no action needed |
| `admission.application.country_id` | `master.country` | owner is never hard-deleted (archived by status); no action needed |
| `admission.application.course_id` | `academics.course` | owner is never hard-deleted (archived by status); no action needed |
| `admission.application.fee_plan_id` | `finance.fee_plan` | null the column |
| `admission.application.grade_level_id` | `academics.grade_level` | owner is never hard-deleted (archived by status); no action needed |
| `admission.application.honorific_id` | `people.honorific` | null the column |
| `admission.application.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `admission.application.photo_file_id` | `platform.file_object` | null the column |
| `admission.application.program_id` | `academics.program` | owner is never hard-deleted (archived by status); no action needed |
| `admission.application.section_id` | `academics.section` | owner is never hard-deleted (archived by status); no action needed |
| `admission.application.state_province_id` | `master.state_province` | owner is never hard-deleted (archived by status); no action needed |
| `admission.application.student_id` | `sis.student` | null the column |
| `admission.application_guardian.photo_file_id` | `platform.file_object` | null the column |
| `admission.application_guardian.qualification_id` | `people.qualification` | owner is never hard-deleted (archived by status); no action needed |
| `admission.number_sequence.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |

## assessment (40 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `assessment.assignment.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.assignment.assigned_by_staff_id` | `hr.staff` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.assignment.attachment_file_id` | `platform.file_object` | null the column |
| `assessment.assignment.batch_id` | `academics.batch` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.assignment.class_section_id` | `academics.class_section` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.assignment.course_id` | `academics.course` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.assignment.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.assignment.reviewer_staff_id` | `hr.staff` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.assignment_allocation.student_id` | `sis.student` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.assignment_approval.decided_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.assignment_subject.subject_id` | `academics.subject` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.assignment_submission.graded_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.assignment_submission.student_id` | `sis.student` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.exam.class_section_id` | `academics.class_section` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.exam.subject_id` | `academics.subject` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.exam_invigilator.staff_id` | `hr.staff` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.exam_result.evaluated_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.exam_result.room_id` | `facility.room` | null the column |
| `assessment.exam_result.student_id` | `sis.student` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.exam_session.academic_term_id` | `org.academic_term` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.exam_session.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.exam_session.batch_id` | `academics.batch` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.exam_session.course_id` | `academics.course` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.exam_session.grade_level_id` | `academics.grade_level` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.exam_session.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.grading_scheme.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.grading_scheme.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.homework.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.homework.assigned_by_staff_id` | `hr.staff` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.homework.attachment_file_id` | `platform.file_object` | null the column |
| `assessment.homework.class_section_id` | `academics.class_section` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.homework.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.homework.subject_id` | `academics.subject` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.homework_approval.decided_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.homework_submission.checked_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.homework_submission.student_id` | `sis.student` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.marksheet.generated_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.marksheet.published_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.marksheet_line.student_id` | `sis.student` | owner is never hard-deleted (archived by status); no action needed |
| `assessment.result_change_log.changed_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |

`assessment.student_record` is not a set of references but a copy kept from events, like `iam.member_record`: each
row mirrors the person of one `sis.student` row from that row's own created and updated events (`StudentRecordSync`),
so privacy's tasks find a person's submissions and results without reading sis's schema. A deleted student keeps its
row: its results stay the person's.

## attendance (12 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `attendance.attendance_record.leave_request_id` | `sis.student_leave_request` | null the column |
| `attendance.attendance_record.recorded_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `attendance.attendance_record.student_id` | `sis.student` | owner is never hard-deleted (archived by status); no action needed |
| `attendance.attendance_register.batch_id` | `academics.batch` | owner is never hard-deleted (archived by status); no action needed |
| `attendance.attendance_register.class_section_id` | `academics.class_section` | owner is never hard-deleted (archived by status); no action needed |
| `attendance.attendance_register.course_id` | `academics.course` | owner is never hard-deleted (archived by status); no action needed |
| `attendance.attendance_register.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `attendance.attendance_register.subject_id` | `academics.subject` | owner is never hard-deleted (archived by status); no action needed |
| `attendance.attendance_sheet.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `attendance.attendance_sheet.taken_by_staff_id` | `hr.staff` | owner is never hard-deleted (archived by status); no action needed |
| `attendance.attendance_sheet.timetable_session_id` | `timetable.session` | null the column |
| `attendance.attendance_type.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |

`attendance.member_record` is not a set of references but a copy kept from events, like `iam.member_record`: each
row mirrors the person of one `sis.student` row from that row's own created and updated events
(`MemberRecordSync`), so privacy's tasks and the `attendance.after_leaving` sweep find a person's attendance without
reading another schema. A deleted record keeps its row: its attendance stays the person's. Staff attendance moved to
hrms (`time_attendance`, reorganisation 2c), and its staff entries with it.

## careers (6 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `careers.candidate_preference.person_id` | `people.person` | archive/delete the dependent row |
| `careers.job_application.applicant_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `careers.job_application.resume_file_id` | `platform.file_object` | owner is never hard-deleted (archived by status); no action needed |
| `careers.job_opening.company_logo_file_id` | `platform.file_object` | owner is never hard-deleted (archived by status); no action needed |
| `careers.job_opening.country_id` | `master.country` | owner is never hard-deleted (archived by status); no action needed |
| `careers.job_opening.posted_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |

## comms (72 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `comms.audience_group.class_section_id` | `academics.class_section` | owner is never hard-deleted (archived by status); no action needed |
| `comms.audience_group.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.audience_group_member.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.calendar_export.user_id` | `iam.user_account` | archive/delete the dependent row |
| `comms.class_page.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `comms.class_page.cover_image_file_id` | `platform.file_object` | null the column |
| `comms.class_page.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.class_page.owner_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.class_page_attachment.file_id` | `platform.file_object` | owner is never hard-deleted (archived by status); no action needed |
| `comms.class_page_member.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.content_view.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.content_view.student_id` | `sis.student` | archive/delete the dependent row |
| `comms.content_view.viewer_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.conversation.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.conversation_message.sender_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.conversation_participant.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.event.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `comms.event.class_section_id` | `academics.class_section` | owner is never hard-deleted (archived by status); no action needed |
| `comms.event.image_file_id` | `platform.file_object` | null the column |
| `comms.event.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.event.owner_user_id` | `iam.user_account` | archive/delete the dependent row |
| `comms.event_attendee.user_id` | `iam.user_account` | archive/delete the dependent row |
| `comms.event_gallery_item.file_id` | `platform.file_object` | owner is never hard-deleted (archived by status); no action needed |
| `comms.event_institution.institution_id` | `org.institution` | archive/delete the dependent row |
| `comms.event_occurrence.user_id` | `iam.user_account` | archive/delete the dependent row |
| `comms.feedback_message.attachment_file_id` | `platform.file_object` | null the column |
| `comms.feedback_message.sender_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.feedback_thread.assigned_to_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.feedback_thread.guardian_id` | `sis.guardian` | owner is never hard-deleted (archived by status); no action needed |
| `comms.feedback_thread.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.feedback_thread.student_id` | `sis.student` | owner is never hard-deleted (archived by status); no action needed |
| `comms.in_app_notification.recipient_user_id` | `iam.user_account` | archive/delete the dependent row |
| `comms.magazine.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `comms.magazine.cover_image_file_id` | `platform.file_object` | null the column |
| `comms.magazine.document_file_id` | `platform.file_object` | null the column |
| `comms.magazine.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.mailbox_message.about_student_id` | `sis.student` | null the column |
| `comms.mailbox_message.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `comms.mailbox_message.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.mailbox_message.owner_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.message_receipt.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.message_template.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.notice.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `comms.notice.attachment_file_id` | `platform.file_object` | null the column |
| `comms.notice.class_section_id` | `academics.class_section` | owner is never hard-deleted (archived by status); no action needed |
| `comms.notice.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.notice_audience.institution_id` | `org.institution` | archive/delete the dependent row |
| `comms.notice_audience.role_id` | `iam.role` | archive/delete the dependent row |
| `comms.notice_background.image_file_id` | `platform.file_object` | null the column |
| `comms.notice_background.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.notification_preference_set.role_id` | `iam.role` | archive/delete the dependent row |
| `comms.notification_preference_set.user_id` | `iam.user_account` | archive/delete the dependent row |
| `comms.notification_rule.permission_id` | `iam.permission` | archive/delete the dependent row |
| `comms.notification_rule.role_id` | `iam.role` | archive/delete the dependent row |
| `comms.outbound_message.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `comms.outbound_message.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.outbound_message.recipient_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.page_category.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `comms.page_category.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.post.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `comms.post.author_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.post.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.post_audience_class.class_section_id` | `academics.class_section` | archive/delete the dependent row |
| `comms.post_comment.attachment_file_id` | `platform.file_object` | null the column |
| `comms.post_comment.author_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.reaction.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.task.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `comms.task.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `comms.task.owner_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.task_assignee.claimed_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `comms.task_assignee.class_section_id` | `academics.class_section` | owner is never hard-deleted (archived by status); no action needed |
| `comms.task_assignee.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |

`comms.staff_record` and `comms.guardian_link` are copies kept from events, like `comms.student_record` (which now also
keeps the student's institution): each row mirrors one `hr.staff` or `sis.student_guardian` row from its own created,
updated and deleted events (`StaffRecordSync`, `GuardianLinkSync`), so feedback threads reach their guardian, and mail
and conversations reach only accounts of the sender's school, without calling sis or hr. A deleted record keeps its
row, marked deleted.

## facility (6 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `facility.asset_item.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `facility.facility.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `facility.room.default_batch_id` | `academics.batch` | null the column |
| `facility.room.default_class_section_id` | `academics.class_section` | null the column |
| `facility.room.default_course_id` | `academics.course` | null the column |
| `facility.room.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |

## finance (35 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `finance.course_fee_default.course_id` | `academics.course` | archive/delete the dependent row |
| `finance.fee_concession.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `finance.fee_concession.approved_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `finance.fee_concession.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `finance.fee_concession.student_id` | `sis.student` | owner is never hard-deleted (archived by status); no action needed |
| `finance.fee_head.currency_id` | `master.currency` | owner is never hard-deleted (archived by status); no action needed |
| `finance.fee_head.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `finance.fee_plan.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `finance.fee_structure.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `finance.fee_structure.grade_level_id` | `academics.grade_level` | owner is never hard-deleted (archived by status); no action needed |
| `finance.fee_structure.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `finance.fee_structure.program_id` | `academics.program` | owner is never hard-deleted (archived by status); no action needed |
| `finance.invoice.bill_to_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `finance.invoice.currency_id` | `master.currency` | owner is never hard-deleted (archived by status); no action needed |
| `finance.invoice.document_file_id` | `platform.file_object` | null the column |
| `finance.invoice.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `finance.invoice.student_id` | `sis.student` | owner is never hard-deleted (archived by status); no action needed |
| `finance.number_sequence.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `finance.payment.billing_address_id` | `people.person_address` | null the column |
| `finance.payment.currency_id` | `master.currency` | owner is never hard-deleted (archived by status); no action needed |
| `finance.payment.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `finance.payment.payer_person_id` | `people.person` | owner is never hard-deleted (archived by status); no action needed |
| `finance.payment.payer_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `finance.payment.received_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `finance.payment_allocation.currency_id` | `master.currency` | owner is never hard-deleted (archived by status); no action needed |
| `finance.payment_gateway_account.currency_id` | `master.currency` | owner is never hard-deleted (archived by status); no action needed |
| `finance.payment_gateway_account.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `finance.program_enrollment_fee.program_enrollment_id` | `sis.program_enrollment` | archive/delete the dependent row |
| `finance.refund.refunded_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `finance.student_fee_due.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `finance.student_fee_due.currency_id` | `master.currency` | owner is never hard-deleted (archived by status); no action needed |
| `finance.student_fee_due.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `finance.student_fee_due.program_enrollment_id` | `sis.program_enrollment` | null the column |
| `finance.student_fee_due.student_enrollment_id` | `sis.student_enrollment` | null the column |
| `finance.student_fee_due.student_id` | `sis.student` | owner is never hard-deleted (archived by status); no action needed |

`finance.student_record` is not a set of references but a copy kept from events, like `iam.member_record`: each row
mirrors the person of one `sis.student` row from that row's own created and updated events (`StudentRecordSync`), so
privacy's tasks find a person's fee dues, concessions, invoices and payments without reading sis's schema. A deleted
student keeps its row: its books of account stay the person's.

`finance.staff_record` is a copy kept from events, like `finance.student_record` (which now also keeps the student's
account and institution): each row mirrors one `hr.staff` row from its own created, updated and deleted events
(`StaffRecordSync`), so `RaiseChargeInvoice` bills only students and accounts of the charge's institution.

## frontoffice (12 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `frontoffice.call_log.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `frontoffice.call_log.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `frontoffice.call_log.logged_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `frontoffice.directory_contact.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `frontoffice.postal_record.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `frontoffice.postal_record.attachment_file_id` | `platform.file_object` | null the column |
| `frontoffice.postal_record.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `frontoffice.postal_record.logged_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `frontoffice.visitor_log.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `frontoffice.visitor_log.host_staff_id` | `hr.staff` | owner is never hard-deleted (archived by status); no action needed |
| `frontoffice.visitor_log.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `frontoffice.visitor_log.student_id` | `sis.student` | owner is never hard-deleted (archived by status); no action needed |

## hr (13 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `hr.leave_application.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `hr.leave_application.decided_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `hr.leave_application.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `hr.leave_balance.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `hr.leave_type.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `hr.leave_type.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `hr.number_sequence.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `hr.staff.highest_qualification_id` | `people.qualification` | owner is never hard-deleted (archived by status); no action needed |
| `hr.staff.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `hr.staff.person_id` | `people.person` | owner is never hard-deleted (archived by status); no action needed |
| `hr.staff.primary_department_id` | `org.department` | null the column |
| `hr.staff.user_id` | `iam.user_account` | null the column |
| `hr.staff_department.department_id` | `org.department` | owner is never hard-deleted (archived by status); no action needed |

## iam (2 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `iam.user_account.person_id` | `people.person` | owner is never hard-deleted (archived by status); no action needed |
| `iam.user_department_access.department_id` | `org.department` | archive/delete the dependent row |

`iam.member_record` is not a set of references but a copy kept from events, like
`iam.tenant_directory`: each row mirrors one `hr.staff` or `sis.student` row — its account, person and institution —
from that row's own created, updated and deleted events (`MemberRecordSync`), so the owner's deletion is applied by
the copy itself (a tombstone), and `iam.GetMe` reads the caller's records without reading another schema.

## library (19 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `library.library_card.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `library.library_card.person_id` | `people.person` | owner is never hard-deleted (archived by status); no action needed |
| `library.library_card.staff_id` | `hr.staff` | owner is never hard-deleted (archived by status); no action needed |
| `library.library_card.student_id` | `sis.student` | owner is never hard-deleted (archived by status); no action needed |
| `library.loan.borrower_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `library.loan.invoice_id` | `finance.invoice` | owner is never hard-deleted (archived by status); no action needed |
| `library.loan.issued_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `library.media.acquired_academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `library.media.cover_image_file_id` | `platform.file_object` | null the column |
| `library.media.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `library.media_category.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `library.media_course.course_id` | `academics.course` | archive/delete the dependent row |
| `library.media_subject.subject_id` | `academics.subject` | archive/delete the dependent row |
| `library.number_sequence.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `library.purchase_request.requested_by_person_id` | `people.person` | owner is never hard-deleted (archived by status); no action needed |
| `library.purchase_request_course.course_id` | `academics.course` | owner is never hard-deleted (archived by status); no action needed |
| `library.purchase_request_subject.subject_id` | `academics.subject` | owner is never hard-deleted (archived by status); no action needed |
| `library.reservation.requested_by_person_id` | `people.person` | owner is never hard-deleted (archived by status); no action needed |
| `library.reservation.requested_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |

## lms (125 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `lms.access_log.user_id` | `iam.user_account` | null the column |
| `lms.annotation.file_id` | `platform.file_object` | archive/delete the dependent row |
| `lms.annotation.media_file_id` | `platform.file_object` | null the column |
| `lms.annotation.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.annotation_share.shared_with_user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.assignment.institution_id` | `org.institution` | null the column |
| `lms.assignment_group_member.user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.assignment_submission.attachment_file_id` | `platform.file_object` | null the column |
| `lms.assignment_submission.evaluator_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.assignment_submission.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.badge.image_file_id` | `platform.file_object` | owner is never hard-deleted (archived by status); no action needed |
| `lms.badge_award.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.bookmark.user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.catalog_banner.image_file_id` | `platform.file_object` | null the column |
| `lms.certificate.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.certificate_evaluation.evaluator_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.certificate_evaluation.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.certificate_request.evaluator_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.certificate_request.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.chapter.scorm_package_file_id` | `platform.file_object` | null the column |
| `lms.cohort.academic_term_id` | `org.academic_term` | null the column |
| `lms.cohort.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `lms.cohort.batch_id` | `academics.batch` | owner is never hard-deleted (archived by status); no action needed |
| `lms.cohort.class_subject_id` | `academics.class_subject` | owner is never hard-deleted (archived by status); no action needed |
| `lms.cohort.confirmation_template_id` | `comms.message_template` | null the column |
| `lms.cohort.currency_id` | `master.currency` | owner is never hard-deleted (archived by status); no action needed |
| `lms.cohort.grading_scheme_id` | `assessment.grading_scheme` | null the column |
| `lms.cohort.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `lms.cohort.intro_video_file_id` | `platform.file_object` | null the column |
| `lms.cohort.meta_image_file_id` | `platform.file_object` | null the column |
| `lms.cohort.program_id` | `academics.program` | null the column |
| `lms.cohort_enrollment.acquisition_source_id` | `finance.acquisition_source` | null the column |
| `lms.cohort_enrollment.graded_by` | `iam.user_account` | null the column |
| `lms.cohort_enrollment.payment_id` | `finance.payment` | owner is never hard-deleted (archived by status); no action needed |
| `lms.cohort_enrollment.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.cohort_feedback.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.cohort_holiday.holiday_id` | `org.holiday` | archive/delete the dependent row |
| `lms.cohort_teacher.role_id` | `iam.role` | null the column |
| `lms.cohort_teacher.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.conferencing_account.owner_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.content_author.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.content_item.file_id` | `platform.file_object` | owner is never hard-deleted (archived by status); no action needed |
| `lms.content_item_rating.user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.content_view_session.file_id` | `platform.file_object` | null the column |
| `lms.content_view_session.user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.course.currency_id` | `master.currency` | owner is never hard-deleted (archived by status); no action needed |
| `lms.course.grading_scheme_id` | `assessment.grading_scheme` | null the column |
| `lms.course.image_file_id` | `platform.file_object` | null the column |
| `lms.course.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `lms.course_author_profile.photo_file_id` | `platform.file_object` | null the column |
| `lms.course_interest.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.course_mentor.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.course_preview.file_id` | `platform.file_object` | owner is never hard-deleted (archived by status); no action needed |
| `lms.course_review.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.course_teacher.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.course_testimonial.photo_file_id` | `platform.file_object` | null the column |
| `lms.discussion_prompt.institution_id` | `org.institution` | null the column |
| `lms.drive_item.file_id` | `platform.file_object` | null the column |
| `lms.drive_item.owner_user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.drive_item_share.user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.ebook.file_id` | `platform.file_object` | null the column |
| `lms.ebook.institution_id` | `org.institution` | null the column |
| `lms.ebook.source_file_id` | `platform.file_object` | null the column |
| `lms.enrollment.payment_id` | `finance.payment` | owner is never hard-deleted (archived by status); no action needed |
| `lms.enrollment.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.enrollment_request.decided_by` | `iam.user_account` | null the column |
| `lms.enrollment_request.user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.evaluator.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.exercise_submission.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.external_tool_institution.institution_id` | `org.institution` | archive/delete the dependent row |
| `lms.external_tool_resource.institution_id` | `org.institution` | null the column |
| `lms.external_tool_role.role_id` | `iam.role` | archive/delete the dependent row |
| `lms.forum_post.abuse_confirmed_by` | `iam.user_account` | null the column |
| `lms.forum_post.author_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.forum_post_report.reported_by` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.forum_post_vote.user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.forum_read_state.user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.forum_topic.individual_user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.grade_change_log.changed_by` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.grade_change_log.student_user_id` | `iam.user_account` | null the column |
| `lms.gradebook_entry.graded_by` | `iam.user_account` | null the column |
| `lms.gradebook_entry.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.import_export_job.institution_id` | `org.institution` | null the column |
| `lms.import_export_job.package_file_id` | `platform.file_object` | null the column |
| `lms.import_export_job.requested_by` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.item_override.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.learning_program_member.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.lesson_note.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.lesson_progress.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.live_class.calendar_event_id` | `comms.event` | null the column |
| `lms.live_class.host_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.live_class_participant.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.mentorship.mentee_user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.mentorship.mentor_user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.number_sequence.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `lms.participation_activity.institution_id` | `org.institution` | null the column |
| `lms.peer_review.reviewee_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.peer_review.reviewer_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.plagiarism_account.default_teacher_user_id` | `iam.user_account` | null the column |
| `lms.plagiarism_report.entity_attachment_id` | `platform.entity_attachment` | null the column |
| `lms.plagiarism_report.file_id` | `platform.file_object` | owner is never hard-deleted (archived by status); no action needed |
| `lms.plagiarism_user_link.user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.question_hotspot.image_file_id` | `platform.file_object` | null the column |
| `lms.quiz.institution_id` | `org.institution` | null the column |
| `lms.quiz_answer.graded_by` | `iam.user_account` | null the column |
| `lms.quiz_submission.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.reassignment_log.reassigned_by` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.reassignment_log.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.related_material_rank.file_id` | `platform.file_object` | archive/delete the dependent row |
| `lms.related_material_rank.user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.release_grant.user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.rubric.institution_id` | `org.institution` | null the column |
| `lms.rubric_assessment.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.settings.certificate_template_id` | `comms.message_template` | null the column |
| `lms.settings.cohort_confirmation_template_id` | `comms.message_template` | null the column |
| `lms.settings.default_currency_id` | `master.currency` | owner is never hard-deleted (archived by status); no action needed |
| `lms.settings.mentor_request_template_id` | `comms.message_template` | null the column |
| `lms.settings.mentor_status_template_id` | `comms.message_template` | null the column |
| `lms.settings.meta_image_file_id` | `platform.file_object` | null the column |
| `lms.settings.payment_reminder_template_id` | `comms.message_template` | null the column |
| `lms.settings_primary_country.country_id` | `master.country` | owner is never hard-deleted (archived by status); no action needed |
| `lms.student_alert.sent_by` | `iam.user_account` | null the column |
| `lms.student_alert.user_id` | `iam.user_account` | archive/delete the dependent row |
| `lms.video_watch.user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `lms.web_resource.institution_id` | `org.institution` | null the column |

## org (7 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `org.institution.city_id` | `master.city` | owner is never hard-deleted (archived by status); no action needed |
| `org.institution.country_id` | `master.country` | owner is never hard-deleted (archived by status); no action needed |
| `org.institution.currency_id` | `master.currency` | owner is never hard-deleted (archived by status); no action needed |
| `org.institution.logo_file_id` | `platform.file_object` | null the column |
| `org.institution.signature_file_id` | `platform.file_object` | null the column |
| `org.institution.state_province_id` | `master.state_province` | owner is never hard-deleted (archived by status); no action needed |
| `org.institution.theme_id` | `platform.theme` | null the column |

`org.tenant_directory` is a copy kept from `platform.tenant` events (`TenantDirectorySync`), like
`iam.tenant_directory`, and `org.institution_directory` a copy of org's own institutions written with each of their
changes; its `city_id` and `country_id` are the institution's own references into master, copied. Neither is a set of
references of its own: the owner's deletion reaches them through the rows they copy. `org.finder_listing` is likewise
a copy, of the institution values of iam's setting `directory.listedInSchoolFinder`, kept from `iam.setting_value`
events (`FinderListingSync`, against the dependency arrow: org never calls iam); a deleted value is kept as a
tombstone that lists nothing.

## payroll (12 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `payroll.disbursement_account.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `payroll.disbursement_account.managed_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `payroll.number_sequence.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `payroll.payroll_payment.attachment_file_id` | `platform.file_object` | null the column |
| `payroll.payroll_payment.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `payroll.payroll_payment.staff_id` | `hr.staff` | owner is never hard-deleted (archived by status); no action needed |
| `payroll.payslip.document_file_id` | `platform.file_object` | null the column |
| `payroll.payslip.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `payroll.payslip.staff_id` | `hr.staff` | owner is never hard-deleted (archived by status); no action needed |
| `payroll.salary_template.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `payroll.staff_bank_account.staff_id` | `hr.staff` | archive/delete the dependent row |
| `payroll.staff_salary.staff_id` | `hr.staff` | owner is never hard-deleted (archived by status); no action needed |

payroll and hr are sibling areas of the hrms module (JAVA-CODING-RULES §2.2): payslip reads apply the owner-only rule,
and privacy's tasks find a person's payroll records (`PayrollPrivacyTaskHandler`), by reading `hr.staff` (account,
person, `deleted_at`) directly; payroll keeps no copy of it.

## time_attendance (2 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `time_attendance.staff_attendance.recorded_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `time_attendance.staff_attendance.staff_id` | `hr.staff` | owner is never hard-deleted (archived by status); no action needed |

time_attendance and hr are sibling areas of the hrms module, so
`time_attendance.staff_attendance.leave_application_id` → `hr.leave_application` is **not** in this register: since
the 2c move it is a declared in-module, tenant-composite foreign key. Its owner is only ever soft-deleted, so the
link is still released by `hr.leave_application.deleted` (`TimeAttendanceReferenceCleanup`: null the column), and
privacy's tasks find a person's attendance days by reading `hr.staff` directly (`TimeAttendancePrivacyTaskHandler`),
like payroll.

## people (9 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `people.person.nationality_country_id` | `master.country` | owner is never hard-deleted (archived by status); no action needed |
| `people.person.photo_file_id` | `platform.file_object` | null the column |
| `people.person_address.city_id` | `master.city` | owner is never hard-deleted (archived by status); no action needed |
| `people.person_address.country_id` | `master.country` | owner is never hard-deleted (archived by status); no action needed |
| `people.person_address.state_province_id` | `master.state_province` | owner is never hard-deleted (archived by status); no action needed |
| `people.person_document.file_id` | `platform.file_object` | owner is never hard-deleted (archived by status); no action needed |
| `people.person_document.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `people.public_profile.country_id` | `master.country` | owner is never hard-deleted (archived by status); no action needed |
| `people.public_profile.cover_image_file_id` | `platform.file_object` | null the column |

`people.member_link` is not a set of references but a copy kept from events, like `iam.member_record`: each row
mirrors one `sis.student`, `hr.staff` or `sis.student_guardian` row — its person and institution, for a guardian link
the child and whether the link is active instead (the guardian belongs to the child's institutions), for staff the
joining date — from that row's own created, updated and deleted events (`MemberLinkSync`, against the dependency
arrow: people never calls sis or hr), so the owner's deletion is applied by the copy itself (a tombstone).

`people.greeting_template` is a copy kept from events: each row mirrors one `comms.message_template` row (its
institution and whether it is used) from its own created, updated and deleted events (`GreetingTemplateSync`), so
`SendGreetings` refuses a template comms would not deliver. people may not call comms.

## platform (4 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `platform.saas_plan.currency_id` | `master.currency` | owner is never hard-deleted (archived by status); no action needed |
| `platform.tenant.country_id` | `master.country` | owner is never hard-deleted (archived by status); no action needed |
| `platform.tenant.default_currency_id` | `master.currency` | owner is never hard-deleted (archived by status); no action needed |
| `platform.tenant_subscription.currency_id` | `master.currency` | owner is never hard-deleted (archived by status); no action needed |

## privacy (10 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `privacy.consent_record.evidence_file_id` | `platform.file_object` | kept: the signed form is consent evidence; no action needed |
| `privacy.consent_record.giver_person_id` | `people.person` | kept: a consent decision is evidence and never changes; no action needed |
| `privacy.consent_record.recorded_by_user_id` | `iam.user_account` | kept: who recorded a paper form is evidence; no action needed |
| `privacy.consent_record.verified_by_user_id` | `iam.user_account` | kept: who verified a paper form is evidence; no action needed |
| `privacy.nominee.nominee_person_id` | `people.person` | kept: who was named is evidence of the nomination; no action needed |
| `privacy.notice_version.document_file_id` | `platform.file_object` | kept: a notice version is consent evidence and never changes once published; no action needed |
| `privacy.rights_request.assignee_user_id` | `iam.user_account` | kept: who handled a request is evidence of how a right was honoured; no action needed |
| `privacy.rights_request.outcome_file_id` | `platform.file_object` | kept: the response letter or export package is evidence of how a right was honoured; no action needed |
| `privacy.rights_request.requester_person_id` | `people.person` | kept: who asked is evidence of how a right was honoured; no action needed |
| `privacy.rights_request.subject_person_id` | `people.person` | kept: whom a request was about is evidence of how a right was honoured; no action needed |

`privacy.subject` and `privacy.guardian_link` are not sets of references but copies kept from events, like
`iam.member_record`: a subject mirrors one `people.person` row's birth date and the `sis.student` rows of that person
(their accounts and institutions) and the `hr.staff` rows of that person (`staff_records`), a guardian link one
`sis.student_guardian` row with the persons it joins and the guardian's account, each from the row's own created,
updated and deleted events (`SubjectSync`, `StaffSync`, `GuardianLinkSync`). The owner's deletion is applied by the
copy itself — the person's birth date cleared, the student or staff record a tombstone, the link inactive — and the
consent rules, the access summary's roles and the rights requests' conflict-of-interest check read them without
reading another schema.
Consent records, consent states and nominees name their subject or principal through an in-module foreign key to
`privacy.subject`, not a logical reference.

## sis (38 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `sis.discipline_incident.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `sis.discipline_incident.class_section_id` | `academics.class_section` | owner is never hard-deleted (archived by status); no action needed |
| `sis.discipline_incident.evidence_file_id` | `platform.file_object` | null the column |
| `sis.discipline_incident.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `sis.discipline_incident.reported_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `sis.guardian.person_id` | `people.person` | owner is never hard-deleted (archived by status); no action needed |
| `sis.guardian.qualification_id` | `people.qualification` | owner is never hard-deleted (archived by status); no action needed |
| `sis.guardian.user_id` | `iam.user_account` | null the column |
| `sis.number_sequence.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `sis.program_enrollment.academic_term_id` | `org.academic_term` | owner is never hard-deleted (archived by status); no action needed |
| `sis.program_enrollment.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `sis.program_enrollment.batch_id` | `academics.batch` | owner is never hard-deleted (archived by status); no action needed |
| `sis.program_enrollment.course_id` | `academics.course` | owner is never hard-deleted (archived by status); no action needed |
| `sis.program_enrollment_subject.subject_id` | `academics.subject` | owner is never hard-deleted (archived by status); no action needed |
| `sis.promotion.from_academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `sis.promotion.from_grade_level_id` | `academics.grade_level` | owner is never hard-deleted (archived by status); no action needed |
| `sis.promotion.from_section_id` | `academics.section` | owner is never hard-deleted (archived by status); no action needed |
| `sis.promotion.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `sis.promotion.to_academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `sis.promotion.to_grade_level_id` | `academics.grade_level` | owner is never hard-deleted (archived by status); no action needed |
| `sis.promotion.to_section_id` | `academics.section` | owner is never hard-deleted (archived by status); no action needed |
| `sis.student.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `sis.student.person_id` | `people.person` | owner is never hard-deleted (archived by status); no action needed |
| `sis.student.user_id` | `iam.user_account` | null the column |
| `sis.student_activity.staff_id` | `hr.staff` | owner is never hard-deleted (archived by status); no action needed |
| `sis.student_category.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `sis.student_enrollment.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `sis.student_enrollment.class_section_id` | `academics.class_section` | owner is never hard-deleted (archived by status); no action needed |
| `sis.student_health_record.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `sis.student_leave_request.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `sis.student_leave_request.decided_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `sis.student_leave_request.requested_by_user_id` | `iam.user_account` | owner is never hard-deleted (archived by status); no action needed |
| `sis.subject_registration.academic_term_id` | `org.academic_term` | null the column |
| `sis.subject_registration.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `sis.subject_registration.batch_id` | `academics.batch` | owner is never hard-deleted (archived by status); no action needed |
| `sis.subject_registration.course_id` | `academics.course` | owner is never hard-deleted (archived by status); no action needed |
| `sis.subject_registration_line.course_subject_id` | `academics.course_subject` | owner is never hard-deleted (archived by status); no action needed |
| `sis.subject_registration_line.subject_id` | `academics.subject` | owner is never hard-deleted (archived by status); no action needed |

## timetable (9 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `timetable.period.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `timetable.session.batch_id` | `academics.batch` | owner is never hard-deleted (archived by status); no action needed |
| `timetable.session.class_section_id` | `academics.class_section` | owner is never hard-deleted (archived by status); no action needed |
| `timetable.session.course_id` | `academics.course` | owner is never hard-deleted (archived by status); no action needed |
| `timetable.session.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `timetable.session.room_id` | `facility.room` | null the column |
| `timetable.session.staff_id` | `hr.staff` | owner is never hard-deleted (archived by status); no action needed |
| `timetable.session.subject_id` | `academics.subject` | owner is never hard-deleted (archived by status); no action needed |
| `timetable.session_student.student_id` | `sis.student` | owner is never hard-deleted (archived by status); no action needed |

`timetable.institution_zone` is not a set of references but a copy kept from events, like `iam.member_record`: each
row mirrors the time zone of one `org.institution` row from that row's own created, updated and deleted events
(`InstitutionZoneSync`), so `GenerateSessions` uses the school's zone without calling org. A deleted school keeps its
row, marked deleted.

## transport (4 references)

| Column | References (owner) | Event action on owner deletion |
|---|---|---|
| `transport.route.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `transport.route.institution_id` | `org.institution` | owner is never hard-deleted (archived by status); no action needed |
| `transport.student_transport.academic_year_id` | `org.academic_year` | owner is never hard-deleted (archived by status); no action needed |
| `transport.student_transport.student_id` | `sis.student` | owner is never hard-deleted (archived by status); no action needed |

## Against-DAG references (event-only consumers)

These point against the module dependency graph; the owner is unaware of them. Validation and cleanup are event-driven only:

| Reference | Action |
|---|---|
| `platform.tenant_subscription.institution_id` → `org.institution` | on owner deletion, archive/retain (the row is evidence; it keeps the id). |
| `platform.tenant_subscription.requested_by_user_id` → `iam.user_account` | on owner deletion, archive/retain (the row is evidence; it keeps the id). |
| `platform.entity_note.institution_id` → `org.institution` | on owner deletion, archive/retain (the row is evidence; it keeps the id). |
| `platform.upload_policy.role_id` → `iam.role` | on owner deletion, delete the row. |
| `platform.deletion_log.cohort_id` → `lms.cohort` | on owner deletion, null the column. |
| `platform.storage_quota.user_id` → `iam.user_account` | on owner deletion, delete the row. |
| `platform.storage_quota.institution_id` → `org.institution` | on owner deletion, delete the row. |
| `platform.import_job.institution_id` → `org.institution` | on owner deletion, archive/retain (the row is evidence; it keeps the id). |
| `sis.promotion.based_on_exam_session_id` → `assessment.exam_session` | on owner deletion, null the column. |
| `hr.leave_application.absence_reason_id` → `attendance.absence_reason` | on owner deletion, archive/retain (the row is evidence; it keeps the id). |
| `hr.leave_application.class_section_id` → `academics.class_section` | on owner deletion, archive/retain (the row is evidence; it keeps the id). |
| `finance.coupon_scope.lms_course_id` → `lms.course` | on owner deletion, delete the row. |
| `finance.coupon_scope.lms_cohort_id` → `lms.cohort` | on owner deletion, delete the row. |
| `finance.payment.lms_course_id` → `lms.course` | on owner deletion, archive/retain (the row is evidence; it keeps the id). |
| `finance.payment.lms_cohort_id` → `lms.cohort` | on owner deletion, archive/retain (the row is evidence; it keeps the id). |
| `finance.payment.admission_application_id` → `admission.application` | on owner deletion, archive/retain (the row is evidence; it keeps the id). |
| `comms.in_app_notification.cohort_id` → `lms.cohort` | on owner deletion, delete the row. |
| `comms.notice.cohort_id` → `lms.cohort` | on owner deletion, delete the row. |
| `comms.notice.discussion_topic_id` → `lms.forum_topic` | on owner deletion, null the column. |
| `comms.event.cohort_id` → `lms.cohort` | on owner deletion, delete the row. |
| `comms.notice_version.discussion_topic_id` → `lms.forum_topic` | on owner deletion, null the column. |
| `comms.event_occurrence.cohort_id` → `lms.cohort` | on owner deletion, delete the row. |
| `comms.calendar_export.cohort_id` → `lms.cohort` | on owner deletion, delete the row. |
| `comms.notification_cohort_setting.cohort_id` → `lms.cohort` | on owner deletion, delete the row. |
| `platform.file_object.uploaded_by_user_id` → `iam.user_account` | on owner deletion, null the column. |

**Totals: 468 forward logical references + 27 against-DAG references; 450 tenant-owned tables.**
