# MySMS relationship register

All 469 foreign-key relationships — every one inside a single module schema; there are no cross-module foreign keys (references into another module are logical references, registered in `logical-references.md`). `tenant_id` columns carry no foreign key outside the platform schema; the 11 platform-internal `tenant_id` FKs are summarised rather than listed individually. Where both sides are tenant-owned, the FK is **tenant-composite** — `(column, tenant_id)` referencing `(id, tenant_id)` — so a reference can never point into another tenant. Cardinality is read child → parent: **N:1** many child rows per parent, **1:1** when the FK column is also unique. *Mandatory* means the FK column is NOT NULL.

- Foreign keys: **469**, all intra-module and always enforced, on one shared instance or one instance per module.
- Cross-module foreign keys: **0**. Cross-module logical references: **494** (see `logical-references.md`).

| # | Child table.column | Parent table | Cardinality | On delete | Tenant-composite | Scope |
|---|---|---|---|---|---|---|
| 1 | `master.state_province.country_id` | `master.country` | N:1 (mandatory) | RESTRICT |  | intra-module |
| 2 | `master.city.country_id` | `master.country` | N:1 (mandatory) | RESTRICT |  | intra-module |
| 3 | `master.city.state_province_id` | `master.state_province` | N:1 (optional) | RESTRICT |  | intra-module |
| 4 | `platform.tenant_subscription.plan_id` | `platform.saas_plan` | N:1 (optional) | RESTRICT |  | intra-module |
| 5 | `platform.entity_attachment.file_id` | `platform.file_object` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 6 | `platform.tag_assignment.tag_id` | `platform.tag` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 7 | `platform.theme.thumbnail_file_id` | `platform.file_object` | N:1 (optional) | SET NULL | yes | intra-module |
| 8 | `platform.theme.thumbnail_alt_file_id` | `platform.file_object` | N:1 (optional) | SET NULL | yes | intra-module |
| 9 | `platform.theme_token.theme_id` | `platform.theme` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 10 | `iam.user_account.referred_by_user_id` | `iam.user_account` | N:1 (optional) | SET NULL | yes | intra-module |
| 11 | `iam.user_account.last_login_role_id` | `iam.role` | N:1 (optional) | SET NULL | yes | intra-module |
| 12 | `iam.role.parent_role_id` | `iam.role` | N:1 (optional) | SET NULL | yes | intra-module |
| 13 | `iam.role_permission.role_id` | `iam.role` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 14 | `iam.role_permission.permission_id` | `iam.permission` | N:1 (mandatory) | CASCADE |  | intra-module |
| 15 | `iam.user_role.user_id` | `iam.user_account` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 16 | `iam.user_role.role_id` | `iam.role` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 17 | `iam.user_permission.user_id` | `iam.user_account` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 18 | `iam.user_permission.permission_id` | `iam.permission` | N:1 (mandatory) | CASCADE |  | intra-module |
| 19 | `iam.user_department_access.user_id` | `iam.user_account` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 20 | `iam.auth_token.user_id` | `iam.user_account` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 21 | `iam.user_device.user_id` | `iam.user_account` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 22 | `iam.user_preference.user_id` | `iam.user_account` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 23 | `iam.user_session.user_id` | `iam.user_account` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 24 | `iam.user_session.device_id` | `iam.user_device` | N:1 (optional) | SET NULL | yes | intra-module |
| 25 | `iam.mfa_credential.user_id` | `iam.user_account` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 26 | `iam.legacy_credential.user_id` | `iam.user_account` | 1:1 (mandatory) | CASCADE | yes | intra-module |
| 27 | `iam.setting_value.setting_key` | `iam.setting_definition` | N:1 (mandatory) | RESTRICT |  | intra-module |
| 28 | `org.institution.parent_institution_id` | `org.institution` | N:1 (optional) | SET NULL | yes | intra-module |
| 29 | `org.institution.institution_type_id` | `org.institution_type` | N:1 (optional) | SET NULL | yes | intra-module |
| 30 | `org.institution_attribute.institution_id` | `org.institution` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 31 | `org.institution_attribute.attribute_definition_id` | `org.attribute_definition` | N:1 (optional) | SET NULL | yes | intra-module |
| 32 | `org.department.institution_id` | `org.institution` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 33 | `org.department.parent_department_id` | `org.department` | N:1 (optional) | SET NULL | yes | intra-module |
| 34 | `org.academic_year.institution_id` | `org.institution` | 1:1 (mandatory) | RESTRICT | yes | intra-module |
| 35 | `org.academic_term.academic_year_id` | `org.academic_year` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 36 | `org.academic_term.parent_term_id` | `org.academic_term` | N:1 (optional) | SET NULL | yes | intra-module |
| 37 | `org.institution_type_attribute.institution_type_id` | `org.institution_type` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 38 | `org.institution_type_attribute.attribute_definition_id` | `org.attribute_definition` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 39 | `org.holiday.institution_id` | `org.institution` | N:1 (optional) | CASCADE | yes | intra-module |
| 40 | `people.person.honorific_id` | `people.honorific` | N:1 (optional) | SET NULL | yes | intra-module |
| 41 | `people.person_address.person_id` | `people.person` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 42 | `people.emergency_contact.person_id` | `people.person` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 43 | `people.emergency_contact.contact_person_id` | `people.person` | N:1 (optional) | SET NULL | yes | intra-module |
| 44 | `people.education_history.person_id` | `people.person` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 45 | `people.education_history.qualification_id` | `people.qualification` | N:1 (optional) | RESTRICT | yes | intra-module |
| 46 | `people.work_experience.person_id` | `people.person` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 47 | `people.certification.person_id` | `people.person` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 48 | `people.person_skill.person_id` | `people.person` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 49 | `people.person_skill.skill_id` | `people.skill` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 50 | `people.person_document.person_id` | `people.person` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 51 | `people.public_profile.person_id` | `people.person` | 1:1 (mandatory) | CASCADE | yes | intra-module |
| 52 | `people.person_social_link.person_id` | `people.person` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 53 | `people.consent_record.person_id` | `people.person` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 54 | `people.consent_record.given_by_person_id` | `people.person` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 55 | `academics.program.program_level_id` | `academics.program_level` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 56 | `academics.course.program_id` | `academics.program` | N:1 (optional) | RESTRICT | yes | intra-module |
| 57 | `academics.course.parent_course_id` | `academics.course` | N:1 (optional) | SET NULL | yes | intra-module |
| 58 | `academics.batch.course_id` | `academics.course` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 59 | `academics.class_section.grade_level_id` | `academics.grade_level` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 60 | `academics.class_section.section_id` | `academics.section` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 61 | `academics.course_subject.course_id` | `academics.course` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 62 | `academics.course_subject.subject_id` | `academics.subject` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 63 | `academics.class_subject.class_section_id` | `academics.class_section` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 64 | `academics.class_subject.subject_id` | `academics.subject` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 65 | `academics.teaching_assignment.class_subject_id` | `academics.class_subject` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 66 | `academics.curriculum_unit.class_subject_id` | `academics.class_subject` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 67 | `academics.lesson_plan.teaching_assignment_id` | `academics.teaching_assignment` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 68 | `academics.lesson_plan.curriculum_unit_id` | `academics.curriculum_unit` | N:1 (optional) | SET NULL | yes | intra-module |
| 69 | `academics.lesson_plan_approval.lesson_plan_id` | `academics.lesson_plan` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 70 | `sis.student.student_category_id` | `sis.student_category` | N:1 (optional) | SET NULL | yes | intra-module |
| 71 | `sis.student_guardian.student_id` | `sis.student` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 72 | `sis.student_guardian.guardian_id` | `sis.guardian` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 73 | `sis.student_guardian.relationship_type_id` | `sis.relationship_type` | N:1 (optional) | RESTRICT | yes | intra-module |
| 74 | `sis.student_sibling.student_id` | `sis.student` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 75 | `sis.student_sibling.sibling_student_id` | `sis.student` | N:1 (optional) | SET NULL | yes | intra-module |
| 76 | `sis.student_enrollment.student_id` | `sis.student` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 77 | `sis.program_enrollment.student_id` | `sis.student` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 78 | `sis.program_enrollment_subject.program_enrollment_id` | `sis.program_enrollment` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 79 | `sis.subject_registration.student_id` | `sis.student` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 80 | `sis.subject_registration_line.subject_registration_id` | `sis.subject_registration` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 81 | `sis.promotion.student_id` | `sis.student` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 82 | `sis.student_health_record.student_id` | `sis.student` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 83 | `sis.discipline_incident.student_id` | `sis.student` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 84 | `sis.student_activity.student_id` | `sis.student` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 85 | `sis.student_activity.activity_type_id` | `sis.activity_type` | N:1 (optional) | RESTRICT | yes | intra-module |
| 86 | `sis.student_leave_request.student_id` | `sis.student` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 87 | `sis.subject_registration.program_enrollment_id` | `sis.program_enrollment` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 88 | `sis.student_enrollment.previous_enrollment_id` | `sis.student_enrollment` | N:1 (optional) | SET NULL | yes | intra-module |
| 89 | `hr.staff.designation_id` | `hr.designation` | N:1 (optional) | RESTRICT | yes | intra-module |
| 90 | `hr.staff_department.staff_id` | `hr.staff` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 91 | `hr.staff_subject.staff_id` | `hr.staff` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 92 | `hr.leave_application.staff_id` | `hr.staff` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 93 | `hr.leave_application.leave_type_id` | `hr.leave_type` | N:1 (optional) | RESTRICT | yes | intra-module |
| 94 | `hr.leave_balance.staff_id` | `hr.staff` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 95 | `hr.leave_balance.leave_type_id` | `hr.leave_type` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 96 | `hr.staff.reporting_to_staff_id` | `hr.staff` | N:1 (optional) | SET NULL | yes | intra-module |
| 97 | `payroll.salary_template_component.salary_template_id` | `payroll.salary_template` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 98 | `payroll.salary_template_component.pay_component_id` | `payroll.pay_component` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 99 | `payroll.salary_template_component.pay_category_id` | `payroll.pay_category` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 100 | `payroll.staff_salary.salary_template_id` | `payroll.salary_template` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 101 | `payroll.staff_salary_component.staff_salary_id` | `payroll.staff_salary` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 102 | `payroll.staff_salary_component.salary_template_component_id` | `payroll.salary_template_component` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 103 | `payroll.payslip.staff_salary_id` | `payroll.staff_salary` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 104 | `payroll.payslip_line.payslip_id` | `payroll.payslip` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 105 | `payroll.payslip_line.staff_salary_component_id` | `payroll.staff_salary_component` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 106 | `payroll.payroll_payment.payment_type_id` | `payroll.payment_type` | N:1 (optional) | RESTRICT | yes | intra-module |
| 107 | `payroll.payroll_payment.disbursement_account_id` | `payroll.disbursement_account` | N:1 (optional) | RESTRICT | yes | intra-module |
| 108 | `payroll.payroll_payment.payslip_id` | `payroll.payslip` | N:1 (optional) | RESTRICT | yes | intra-module |
| 109 | `admission.admission_cycle_fee.admission_cycle_id` | `admission.admission_cycle` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 110 | `admission.application.admission_cycle_id` | `admission.admission_cycle` | N:1 (optional) | RESTRICT | yes | intra-module |
| 111 | `admission.application_guardian.application_id` | `admission.application` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 112 | `admission.application_emergency_contact.application_id` | `admission.application` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 113 | `attendance.attendance_sheet.register_id` | `attendance.attendance_register` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 114 | `attendance.attendance_record.sheet_id` | `attendance.attendance_sheet` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 115 | `attendance.attendance_record.attendance_type_id` | `attendance.attendance_type` | N:1 (optional) | RESTRICT | yes | intra-module |
| 116 | `attendance.attendance_record.absence_reason_id` | `attendance.absence_reason` | N:1 (optional) | RESTRICT | yes | intra-module |
| 117 | `attendance.attendance_record.sheet_id` | `attendance.attendance_sheet` | N:1 (mandatory) | RESTRICT |  | intra-module |
| 118 | `facility.room_facility.room_id` | `facility.room` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 119 | `facility.room_facility.facility_id` | `facility.facility` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 120 | `facility.room_asset.room_id` | `facility.room` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 121 | `facility.room_asset.asset_item_id` | `facility.asset_item` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 122 | `timetable.session.period_id` | `timetable.period` | N:1 (optional) | SET NULL | yes | intra-module |
| 123 | `timetable.session_student.session_id` | `timetable.session` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 124 | `assessment.homework_approval.homework_id` | `assessment.homework` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 125 | `assessment.homework_submission.homework_id` | `assessment.homework` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 126 | `assessment.assignment.assignment_type_id` | `assessment.assignment_type` | N:1 (optional) | RESTRICT | yes | intra-module |
| 127 | `assessment.assignment_subject.assignment_id` | `assessment.assignment` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 128 | `assessment.assignment_allocation.assignment_id` | `assessment.assignment` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 129 | `assessment.assignment_approval.assignment_id` | `assessment.assignment` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 130 | `assessment.assignment_submission.assignment_id` | `assessment.assignment` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 131 | `assessment.grading_band.grading_scheme_id` | `assessment.grading_scheme` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 132 | `assessment.exam_session.exam_type_id` | `assessment.exam_type` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 133 | `assessment.exam.exam_session_id` | `assessment.exam_session` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 134 | `assessment.exam_invigilator.exam_id` | `assessment.exam` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 135 | `assessment.result_template.exam_session_id` | `assessment.exam_session` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 136 | `assessment.result_template.grading_scheme_id` | `assessment.grading_scheme` | N:1 (optional) | RESTRICT | yes | intra-module |
| 137 | `assessment.result_template_band.result_template_id` | `assessment.result_template` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 138 | `assessment.result_template_band.grading_band_id` | `assessment.grading_band` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 139 | `assessment.marksheet.exam_session_id` | `assessment.exam_session` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 140 | `assessment.marksheet.result_template_id` | `assessment.result_template` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 141 | `assessment.marksheet_line.marksheet_id` | `assessment.marksheet` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 142 | `assessment.exam_result.exam_id` | `assessment.exam` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 143 | `assessment.exam_result.marksheet_line_id` | `assessment.marksheet_line` | N:1 (optional) | SET NULL | yes | intra-module |
| 144 | `assessment.result_change_log.exam_result_id` | `assessment.exam_result` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 145 | `lms.evaluator_availability.evaluator_id` | `lms.evaluator` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 146 | `lms.course.category_id` | `lms.category` | N:1 (optional) | SET NULL | yes | intra-module |
| 147 | `lms.course.default_evaluator_id` | `lms.evaluator` | N:1 (optional) | SET NULL | yes | intra-module |
| 148 | `lms.course.previous_version_id` | `lms.course` | N:1 (optional) | SET NULL | yes | intra-module |
| 149 | `lms.course.root_course_id` | `lms.course` | N:1 (optional) | SET NULL | yes | intra-module |
| 150 | `lms.course_teacher.course_id` | `lms.course` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 151 | `lms.related_course.course_id` | `lms.course` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 152 | `lms.related_course.related_course_id` | `lms.course` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 153 | `lms.related_course.author_profile_id` | `lms.course_author_profile` | N:1 (optional) | SET NULL | yes | intra-module |
| 154 | `lms.course_mentor.course_id` | `lms.course` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 155 | `lms.chapter.course_id` | `lms.course` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 156 | `lms.chapter.cohort_id` | `lms.cohort` | N:1 (optional) | CASCADE | yes | intra-module |
| 157 | `lms.chapter.parent_chapter_id` | `lms.chapter` | N:1 (optional) | CASCADE | yes | intra-module |
| 158 | `lms.chapter.copied_from_id` | `lms.chapter` | N:1 (optional) | SET NULL | yes | intra-module |
| 159 | `lms.lesson.chapter_id` | `lms.chapter` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 160 | `lms.quiz.course_id` | `lms.course` | N:1 (optional) | SET NULL | yes | intra-module |
| 161 | `lms.quiz.lesson_id` | `lms.lesson` | N:1 (optional) | SET NULL | yes | intra-module |
| 162 | `lms.question_option.question_id` | `lms.question` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 163 | `lms.question_accepted_answer.question_id` | `lms.question` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 164 | `lms.quiz_question.quiz_id` | `lms.quiz` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 165 | `lms.quiz_question.question_id` | `lms.question` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 166 | `lms.quiz_submission.quiz_id` | `lms.quiz` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 167 | `lms.quiz_submission.course_id` | `lms.course` | N:1 (optional) | SET NULL | yes | intra-module |
| 168 | `lms.quiz_submission.content_item_id` | `lms.content_item` | N:1 (optional) | SET NULL | yes | intra-module |
| 169 | `lms.quiz_submission.cohort_id` | `lms.cohort` | N:1 (optional) | CASCADE | yes | intra-module |
| 170 | `lms.quiz_submission.reassigned_from_id` | `lms.quiz_submission` | N:1 (optional) | SET NULL | yes | intra-module |
| 171 | `lms.quiz_answer.quiz_submission_id` | `lms.quiz_submission` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 172 | `lms.quiz_answer.question_id` | `lms.question` | N:1 (optional) | SET NULL | yes | intra-module |
| 173 | `lms.quiz_answer.learning_objective_id` | `lms.learning_objective` | N:1 (optional) | SET NULL | yes | intra-module |
| 174 | `lms.quiz_violation.quiz_submission_id` | `lms.quiz_submission` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 175 | `lms.assignment.course_id` | `lms.course` | N:1 (optional) | SET NULL | yes | intra-module |
| 176 | `lms.assignment_submission.assignment_id` | `lms.assignment` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 177 | `lms.assignment_submission.lesson_id` | `lms.lesson` | N:1 (optional) | SET NULL | yes | intra-module |
| 178 | `lms.assignment_submission.course_id` | `lms.course` | N:1 (optional) | SET NULL | yes | intra-module |
| 179 | `lms.assignment_submission.content_item_id` | `lms.content_item` | N:1 (optional) | SET NULL | yes | intra-module |
| 180 | `lms.assignment_submission.group_id` | `lms.assignment_group` | N:1 (optional) | SET NULL | yes | intra-module |
| 181 | `lms.exercise_test_case.exercise_id` | `lms.programming_exercise` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 182 | `lms.exercise_submission.exercise_id` | `lms.programming_exercise` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 183 | `lms.exercise_test_result.exercise_submission_id` | `lms.exercise_submission` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 184 | `lms.exercise_test_result.test_case_id` | `lms.exercise_test_case` | N:1 (optional) | SET NULL | yes | intra-module |
| 185 | `lms.content_author.quiz_id` | `lms.quiz` | N:1 (optional) | CASCADE | yes | intra-module |
| 186 | `lms.content_author.question_id` | `lms.question` | N:1 (optional) | CASCADE | yes | intra-module |
| 187 | `lms.content_author.assignment_id` | `lms.assignment` | N:1 (optional) | CASCADE | yes | intra-module |
| 188 | `lms.content_author.exercise_id` | `lms.programming_exercise` | N:1 (optional) | CASCADE | yes | intra-module |
| 189 | `lms.content_author.learning_program_id` | `lms.learning_program` | N:1 (optional) | CASCADE | yes | intra-module |
| 190 | `lms.lesson_activity.lesson_id` | `lms.lesson` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 191 | `lms.lesson_activity.quiz_id` | `lms.quiz` | N:1 (optional) | CASCADE | yes | intra-module |
| 192 | `lms.lesson_activity.assignment_id` | `lms.assignment` | N:1 (optional) | CASCADE | yes | intra-module |
| 193 | `lms.lesson_activity.exercise_id` | `lms.programming_exercise` | N:1 (optional) | CASCADE | yes | intra-module |
| 194 | `lms.enrollment.course_id` | `lms.course` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 195 | `lms.enrollment.current_lesson_id` | `lms.lesson` | N:1 (optional) | SET NULL | yes | intra-module |
| 196 | `lms.enrollment.source_cohort_id` | `lms.cohort` | N:1 (optional) | SET NULL | yes | intra-module |
| 197 | `lms.enrollment.certificate_id` | `lms.certificate` | N:1 (optional) | SET NULL | yes | intra-module |
| 198 | `lms.lesson_progress.course_id` | `lms.course` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 199 | `lms.lesson_progress.chapter_id` | `lms.chapter` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 200 | `lms.lesson_progress.lesson_id` | `lms.lesson` | N:1 (optional) | CASCADE | yes | intra-module |
| 201 | `lms.video_watch.lesson_id` | `lms.lesson` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 202 | `lms.course_review.course_id` | `lms.course` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 203 | `lms.course_interest.course_id` | `lms.course` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 204 | `lms.cohort.category_id` | `lms.category` | N:1 (optional) | SET NULL | yes | intra-module |
| 205 | `lms.cohort.conferencing_account_id` | `lms.conferencing_account` | N:1 (optional) | SET NULL | yes | intra-module |
| 206 | `lms.cohort.timetable_template_id` | `lms.timetable_template` | N:1 (optional) | SET NULL | yes | intra-module |
| 207 | `lms.cohort_course.cohort_id` | `lms.cohort` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 208 | `lms.cohort_course.course_id` | `lms.course` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 209 | `lms.cohort_course.evaluator_id` | `lms.evaluator` | N:1 (optional) | SET NULL | yes | intra-module |
| 210 | `lms.cohort_teacher.cohort_id` | `lms.cohort` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 211 | `lms.timetable_entry.cohort_id` | `lms.cohort` | N:1 (optional) | CASCADE | yes | intra-module |
| 212 | `lms.timetable_entry.timetable_template_id` | `lms.timetable_template` | N:1 (optional) | CASCADE | yes | intra-module |
| 213 | `lms.timetable_entry.lesson_id` | `lms.lesson` | N:1 (optional) | CASCADE | yes | intra-module |
| 214 | `lms.timetable_entry.quiz_id` | `lms.quiz` | N:1 (optional) | CASCADE | yes | intra-module |
| 215 | `lms.timetable_entry.assignment_id` | `lms.assignment` | N:1 (optional) | CASCADE | yes | intra-module |
| 216 | `lms.timetable_entry.exercise_id` | `lms.programming_exercise` | N:1 (optional) | CASCADE | yes | intra-module |
| 217 | `lms.timetable_entry.live_class_id` | `lms.live_class` | N:1 (optional) | CASCADE | yes | intra-module |
| 218 | `lms.timetable_legend.cohort_id` | `lms.cohort` | N:1 (optional) | CASCADE | yes | intra-module |
| 219 | `lms.timetable_legend.timetable_template_id` | `lms.timetable_template` | N:1 (optional) | CASCADE | yes | intra-module |
| 220 | `lms.cohort_enrollment.cohort_id` | `lms.cohort` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 221 | `lms.cohort_feedback.cohort_id` | `lms.cohort` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 222 | `lms.live_class.cohort_id` | `lms.cohort` | N:1 (optional) | CASCADE | yes | intra-module |
| 223 | `lms.live_class.conferencing_account_id` | `lms.conferencing_account` | N:1 (optional) | SET NULL | yes | intra-module |
| 224 | `lms.live_class_participant.live_class_id` | `lms.live_class` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 225 | `lms.learning_program_course.learning_program_id` | `lms.learning_program` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 226 | `lms.learning_program_course.course_id` | `lms.course` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 227 | `lms.learning_program_member.learning_program_id` | `lms.learning_program` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 228 | `lms.certificate.course_id` | `lms.course` | N:1 (optional) | RESTRICT | yes | intra-module |
| 229 | `lms.certificate.cohort_id` | `lms.cohort` | N:1 (optional) | RESTRICT | yes | intra-module |
| 230 | `lms.certificate.evaluator_id` | `lms.evaluator` | N:1 (optional) | SET NULL | yes | intra-module |
| 231 | `lms.certificate_request.course_id` | `lms.course` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 232 | `lms.certificate_request.cohort_id` | `lms.cohort` | N:1 (optional) | SET NULL | yes | intra-module |
| 233 | `lms.certificate_evaluation.course_id` | `lms.course` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 234 | `lms.certificate_evaluation.cohort_id` | `lms.cohort` | N:1 (optional) | SET NULL | yes | intra-module |
| 235 | `lms.certificate_evaluation.certificate_request_id` | `lms.certificate_request` | N:1 (optional) | SET NULL | yes | intra-module |
| 236 | `lms.badge_award.badge_id` | `lms.badge` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 237 | `lms.settings_sidebar_item.settings_id` | `lms.settings` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 238 | `lms.settings_primary_country.settings_id` | `lms.settings` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 239 | `lms.learning_objective.course_id` | `lms.course` | N:1 (optional) | CASCADE | yes | intra-module |
| 240 | `lms.learning_objective.chapter_id` | `lms.chapter` | N:1 (optional) | CASCADE | yes | intra-module |
| 241 | `lms.learning_objective.copied_from_id` | `lms.learning_objective` | N:1 (optional) | SET NULL | yes | intra-module |
| 242 | `lms.grading_category.course_id` | `lms.course` | N:1 (optional) | CASCADE | yes | intra-module |
| 243 | `lms.grading_category.cohort_id` | `lms.cohort` | N:1 (optional) | CASCADE | yes | intra-module |
| 244 | `lms.content_item.chapter_id` | `lms.chapter` | N:1 (optional) | CASCADE |  | intra-module |
| 245 | `lms.content_item.course_id` | `lms.course` | N:1 (optional) | CASCADE | yes | intra-module |
| 246 | `lms.content_item.cohort_id` | `lms.cohort` | N:1 (optional) | CASCADE | yes | intra-module |
| 247 | `lms.content_item.lesson_id` | `lms.lesson` | N:1 (optional) | CASCADE | yes | intra-module |
| 248 | `lms.content_item.assignment_id` | `lms.assignment` | N:1 (optional) | RESTRICT | yes | intra-module |
| 249 | `lms.content_item.quiz_id` | `lms.quiz` | N:1 (optional) | RESTRICT | yes | intra-module |
| 250 | `lms.content_item.discussion_prompt_id` | `lms.discussion_prompt` | N:1 (optional) | RESTRICT | yes | intra-module |
| 251 | `lms.content_item.participation_activity_id` | `lms.participation_activity` | N:1 (optional) | RESTRICT | yes | intra-module |
| 252 | `lms.content_item.web_resource_id` | `lms.web_resource` | N:1 (optional) | RESTRICT | yes | intra-module |
| 253 | `lms.content_item.ebook_id` | `lms.ebook` | N:1 (optional) | RESTRICT | yes | intra-module |
| 254 | `lms.content_item.external_tool_resource_id` | `lms.external_tool_resource` | N:1 (optional) | RESTRICT | yes | intra-module |
| 255 | `lms.content_item.live_class_id` | `lms.live_class` | N:1 (optional) | CASCADE | yes | intra-module |
| 256 | `lms.content_item.grading_category_id` | `lms.grading_category` | N:1 (optional) | SET NULL | yes | intra-module |
| 257 | `lms.content_item.rubric_id` | `lms.rubric` | N:1 (optional) | SET NULL | yes | intra-module |
| 258 | `lms.content_item.copied_from_id` | `lms.content_item` | N:1 (optional) | SET NULL | yes | intra-module |
| 259 | `lms.content_item_rating.content_item_id` | `lms.content_item` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 260 | `lms.content_item_rating.cohort_id` | `lms.cohort` | N:1 (optional) | CASCADE | yes | intra-module |
| 261 | `lms.item_override.content_item_id` | `lms.content_item` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 262 | `lms.item_override.group_id` | `lms.assignment_group` | N:1 (optional) | CASCADE | yes | intra-module |
| 263 | `lms.gradebook_entry.cohort_id` | `lms.cohort` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 264 | `lms.gradebook_entry.content_item_id` | `lms.content_item` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 265 | `lms.gradebook_entry.group_id` | `lms.assignment_group` | N:1 (optional) | RESTRICT | yes | intra-module |
| 266 | `lms.gradebook_entry.quiz_submission_id` | `lms.quiz_submission` | N:1 (optional) | SET NULL | yes | intra-module |
| 267 | `lms.gradebook_entry.forum_topic_id` | `lms.forum_topic` | N:1 (optional) | SET NULL | yes | intra-module |
| 268 | `lms.grade_change_log.gradebook_entry_id` | `lms.gradebook_entry` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 269 | `lms.reassignment_log.content_item_id` | `lms.content_item` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 270 | `lms.reassignment_log.group_id` | `lms.assignment_group` | N:1 (optional) | CASCADE | yes | intra-module |
| 271 | `lms.reassignment_log.quiz_submission_id` | `lms.quiz_submission` | N:1 (optional) | SET NULL | yes | intra-module |
| 272 | `lms.assignment_group.content_item_id` | `lms.content_item` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 273 | `lms.assignment_group.cohort_id` | `lms.cohort` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 274 | `lms.assignment_group.discussion_topic_id` | `lms.forum_topic` | N:1 (optional) | SET NULL | yes | intra-module |
| 275 | `lms.assignment_group_member.assignment_group_id` | `lms.assignment_group` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 276 | `lms.assignment_group_member.content_item_id` | `lms.content_item` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 277 | `lms.peer_review.assignment_group_id` | `lms.assignment_group` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 278 | `lms.rubric_criterion.rubric_id` | `lms.rubric` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 279 | `lms.rubric_level.rubric_id` | `lms.rubric` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 280 | `lms.rubric_cell.rubric_id` | `lms.rubric` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 281 | `lms.rubric_cell.rubric_criterion_id` | `lms.rubric_criterion` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 282 | `lms.rubric_cell.rubric_level_id` | `lms.rubric_level` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 283 | `lms.rubric_assessment.content_item_id` | `lms.content_item` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 284 | `lms.rubric_assessment.group_id` | `lms.assignment_group` | N:1 (optional) | CASCADE | yes | intra-module |
| 285 | `lms.rubric_assessment.rubric_cell_id` | `lms.rubric_cell` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 286 | `lms.web_resource.category_id` | `lms.resource_category` | N:1 (optional) | SET NULL | yes | intra-module |
| 287 | `lms.ebook.category_id` | `lms.resource_category` | N:1 (optional) | SET NULL | yes | intra-module |
| 288 | `lms.related_material.ebook_id` | `lms.ebook` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 289 | `lms.related_material_rank.ebook_id` | `lms.ebook` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 290 | `lms.related_material_rank.course_id` | `lms.course` | N:1 (optional) | CASCADE | yes | intra-module |
| 291 | `lms.annotation.content_item_id` | `lms.content_item` | N:1 (optional) | CASCADE | yes | intra-module |
| 292 | `lms.annotation.cohort_id` | `lms.cohort` | N:1 (optional) | CASCADE | yes | intra-module |
| 293 | `lms.annotation.ebook_id` | `lms.ebook` | N:1 (optional) | CASCADE | yes | intra-module |
| 294 | `lms.annotation.forum_topic_id` | `lms.forum_topic` | N:1 (optional) | SET NULL | yes | intra-module |
| 295 | `lms.annotation_share.annotation_id` | `lms.annotation` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 296 | `lms.external_tool_setting.external_tool_id` | `lms.external_tool` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 297 | `lms.external_tool_institution.external_tool_id` | `lms.external_tool` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 298 | `lms.external_tool_role.external_tool_id` | `lms.external_tool` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 299 | `lms.external_tool_option.external_tool_id` | `lms.external_tool` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 300 | `lms.external_tool_link.external_tool_resource_id` | `lms.external_tool_resource` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 301 | `lms.external_tool_link.external_tool_id` | `lms.external_tool` | N:1 (optional) | SET NULL | yes | intra-module |
| 302 | `lms.forum.cohort_id` | `lms.cohort` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 303 | `lms.forum_section.forum_id` | `lms.forum` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 304 | `lms.forum_section.cohort_group_id` | `lms.cohort_group` | N:1 (optional) | SET NULL | yes | intra-module |
| 305 | `lms.forum_topic.forum_section_id` | `lms.forum_section` | N:1 (optional) | CASCADE | yes | intra-module |
| 306 | `lms.forum_topic.content_item_id` | `lms.content_item` | N:1 (optional) | SET NULL | yes | intra-module |
| 307 | `lms.forum_topic.discussion_prompt_id` | `lms.discussion_prompt` | N:1 (optional) | SET NULL | yes | intra-module |
| 308 | `lms.forum_topic.last_post_id` | `lms.forum_post` | N:1 (optional) | SET NULL | yes | intra-module |
| 309 | `lms.forum_post.forum_topic_id` | `lms.forum_topic` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 310 | `lms.forum_post.parent_post_id` | `lms.forum_post` | N:1 (optional) | CASCADE | yes | intra-module |
| 311 | `lms.forum_post_vote.forum_post_id` | `lms.forum_post` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 312 | `lms.forum_post_report.forum_post_id` | `lms.forum_post` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 313 | `lms.forum_post_report.report_category_id` | `lms.forum_report_category` | N:1 (optional) | SET NULL | yes | intra-module |
| 314 | `lms.forum_read_state.forum_topic_id` | `lms.forum_topic` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 315 | `lms.forum_read_state.forum_post_id` | `lms.forum_post` | N:1 (optional) | CASCADE | yes | intra-module |
| 316 | `lms.cohort_group.cohort_id` | `lms.cohort` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 317 | `lms.cohort_schedule_slot.cohort_id` | `lms.cohort` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 318 | `lms.cohort_schedule_slot.cohort_teacher_id` | `lms.cohort_teacher` | N:1 (optional) | CASCADE | yes | intra-module |
| 319 | `lms.cohort_session.cohort_id` | `lms.cohort` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 320 | `lms.cohort_holiday.cohort_id` | `lms.cohort` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 321 | `lms.course_author_profile.course_id` | `lms.course` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 322 | `lms.course_preview.course_id` | `lms.course` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 323 | `lms.course_testimonial.course_id` | `lms.course` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 324 | `lms.catalog_banner.course_id` | `lms.course` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 325 | `lms.enrollment_request.course_id` | `lms.course` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 326 | `lms.enrollment_request.cohort_id` | `lms.cohort` | N:1 (optional) | SET NULL | yes | intra-module |
| 327 | `lms.release_rule.course_id` | `lms.course` | N:1 (optional) | CASCADE | yes | intra-module |
| 328 | `lms.release_rule.cohort_id` | `lms.cohort` | N:1 (optional) | CASCADE | yes | intra-module |
| 329 | `lms.release_rule.source_content_item_id` | `lms.content_item` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 330 | `lms.release_rule.copied_from_id` | `lms.release_rule` | N:1 (optional) | SET NULL | yes | intra-module |
| 331 | `lms.release_rule_condition.release_rule_id` | `lms.release_rule` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 332 | `lms.release_rule_target.release_rule_id` | `lms.release_rule` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 333 | `lms.release_rule_target.content_item_id` | `lms.content_item` | N:1 (optional) | CASCADE | yes | intra-module |
| 334 | `lms.release_rule_target.chapter_id` | `lms.chapter` | N:1 (optional) | CASCADE | yes | intra-module |
| 335 | `lms.release_grant.content_item_id` | `lms.content_item` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 336 | `lms.release_grant.chapter_id` | `lms.chapter` | N:1 (optional) | CASCADE | yes | intra-module |
| 337 | `lms.student_alert.cohort_id` | `lms.cohort` | N:1 (optional) | CASCADE | yes | intra-module |
| 338 | `lms.student_alert.course_id` | `lms.course` | N:1 (optional) | SET NULL | yes | intra-module |
| 339 | `lms.student_alert.content_item_id` | `lms.content_item` | N:1 (optional) | SET NULL | yes | intra-module |
| 340 | `lms.content_view_session.content_item_id` | `lms.content_item` | N:1 (optional) | CASCADE | yes | intra-module |
| 341 | `lms.content_view_session.cohort_id` | `lms.cohort` | N:1 (optional) | CASCADE | yes | intra-module |
| 342 | `lms.bookmark.cohort_id` | `lms.cohort` | N:1 (optional) | CASCADE | yes | intra-module |
| 343 | `lms.plagiarism_cohort_link.cohort_id` | `lms.cohort` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 344 | `lms.plagiarism_cohort_link.plagiarism_account_id` | `lms.plagiarism_account` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 345 | `lms.plagiarism_user_link.plagiarism_account_id` | `lms.plagiarism_account` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 346 | `lms.plagiarism_report.plagiarism_account_id` | `lms.plagiarism_account` | N:1 (optional) | SET NULL | yes | intra-module |
| 347 | `lms.quiz_question_history.quiz_id` | `lms.quiz` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 348 | `lms.quiz_question_history.question_id` | `lms.question` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 349 | `lms.question_blank.question_id` | `lms.question` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 350 | `lms.question_blank_answer.question_blank_id` | `lms.question_blank` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 351 | `lms.question_hotspot.question_id` | `lms.question` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 352 | `lms.question_hotspot_region.question_hotspot_id` | `lms.question_hotspot` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 353 | `lms.question_match_pair.question_id` | `lms.question` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 354 | `lms.question_pattern.question_id` | `lms.question` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 355 | `lms.question_pool_item.question_pool_id` | `lms.question_pool` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 356 | `lms.question_pool_item.question_id` | `lms.question` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 357 | `lms.drive_item_share.drive_item_id` | `lms.drive_item` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 358 | `lms.import_export_job.course_id` | `lms.course` | N:1 (optional) | SET NULL | yes | intra-module |
| 359 | `lms.import_export_job.cohort_id` | `lms.cohort` | N:1 (optional) | SET NULL | yes | intra-module |
| 360 | `lms.import_export_job.target_chapter_id` | `lms.chapter` | N:1 (optional) | SET NULL | yes | intra-module |
| 361 | `lms.access_log.cohort_id` | `lms.cohort` | N:1 (optional) | SET NULL | yes | intra-module |
| 362 | `lms.drive_item.course_id` | `lms.course` | N:1 (optional) | SET NULL | yes | intra-module |
| 363 | `lms.content_item.exercise_id` | `lms.programming_exercise` | N:1 (optional) | CASCADE | yes | intra-module |
| 364 | `lms.related_material_rank_item.related_material_rank_id` | `lms.related_material_rank` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 365 | `lms.related_material_rank_item.related_material_id` | `lms.related_material` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 366 | `lms.annotation.lesson_id` | `lms.lesson` | N:1 (optional) | CASCADE | yes | intra-module |
| 367 | `lms.conferencing_token.conferencing_account_id` | `lms.conferencing_account` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 368 | `lms.lesson.superseded_by_id` | `lms.lesson` | N:1 (optional) | SET NULL | yes | intra-module |
| 369 | `lms.quiz.superseded_by_id` | `lms.quiz` | N:1 (optional) | SET NULL | yes | intra-module |
| 370 | `lms.question.superseded_by_id` | `lms.question` | N:1 (optional) | SET NULL | yes | intra-module |
| 371 | `lms.assignment.superseded_by_id` | `lms.assignment` | N:1 (optional) | SET NULL | yes | intra-module |
| 372 | `lms.rubric.superseded_by_id` | `lms.rubric` | N:1 (optional) | SET NULL | yes | intra-module |
| 373 | `finance.fee_plan_installment.fee_plan_id` | `finance.fee_plan` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 374 | `finance.fee_plan_installment_component.installment_id` | `finance.fee_plan_installment` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 375 | `finance.fee_plan_installment_component.fee_head_id` | `finance.fee_head` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 376 | `finance.student_fee_due.installment_id` | `finance.fee_plan_installment` | N:1 (optional) | RESTRICT | yes | intra-module |
| 377 | `finance.student_fee_due.fee_head_id` | `finance.fee_head` | N:1 (optional) | RESTRICT | yes | intra-module |
| 378 | `finance.student_fee_due.invoice_id` | `finance.invoice` | N:1 (optional) | RESTRICT | yes | intra-module |
| 379 | `finance.invoice_line.invoice_id` | `finance.invoice` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 380 | `finance.invoice_line.fee_head_id` | `finance.fee_head` | N:1 (optional) | RESTRICT | yes | intra-module |
| 381 | `finance.coupon_scope.coupon_id` | `finance.coupon` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 382 | `finance.payment.invoice_id` | `finance.invoice` | N:1 (optional) | RESTRICT | yes | intra-module |
| 383 | `finance.payment.coupon_id` | `finance.coupon` | N:1 (optional) | SET NULL | yes | intra-module |
| 384 | `finance.payment.gateway_account_id` | `finance.payment_gateway_account` | N:1 (optional) | RESTRICT | yes | intra-module |
| 385 | `finance.payment_allocation.payment_id` | `finance.payment` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 386 | `finance.payment_allocation.invoice_id` | `finance.invoice` | N:1 (optional) | RESTRICT | yes | intra-module |
| 387 | `finance.payment_allocation.student_fee_due_id` | `finance.student_fee_due` | N:1 (optional) | RESTRICT | yes | intra-module |
| 388 | `finance.refund.payment_id` | `finance.payment` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 389 | `finance.gateway_event.gateway_account_id` | `finance.payment_gateway_account` | N:1 (optional) | SET NULL | yes | intra-module |
| 390 | `finance.gateway_event.payment_id` | `finance.payment` | N:1 (optional) | RESTRICT | yes | intra-module |
| 391 | `finance.fee_structure.fee_plan_id` | `finance.fee_plan` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 392 | `finance.fee_concession.fee_head_id` | `finance.fee_head` | N:1 (optional) | SET NULL | yes | intra-module |
| 393 | `finance.student_fee_due.fee_structure_id` | `finance.fee_structure` | N:1 (optional) | SET NULL | yes | intra-module |
| 394 | `finance.student_fee_due.concession_id` | `finance.fee_concession` | N:1 (optional) | SET NULL | yes | intra-module |
| 395 | `finance.program_enrollment_fee.fee_plan_id` | `finance.fee_plan` | N:1 (optional) | RESTRICT | yes | intra-module |
| 396 | `finance.program_enrollment_fee.fee_head_id` | `finance.fee_head` | N:1 (optional) | SET NULL | yes | intra-module |
| 397 | `finance.course_fee_default.fee_plan_id` | `finance.fee_plan` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 398 | `finance.payment.acquisition_source_id` | `finance.acquisition_source` | N:1 (optional) | SET NULL | yes | intra-module |
| 399 | `finance.invoice.credit_for_invoice_id` | `finance.invoice` | N:1 (optional) | RESTRICT | yes | intra-module |
| 400 | `finance.payment_allocation.payment_id` | `finance.payment` | N:1 (mandatory) | NO ACTION |  | intra-module |
| 401 | `finance.payment_allocation.invoice_id` | `finance.invoice` | N:1 (optional) | NO ACTION |  | intra-module |
| 402 | `finance.payment_allocation.student_fee_due_id` | `finance.student_fee_due` | N:1 (optional) | NO ACTION |  | intra-module |
| 403 | `library.media.media_type_id` | `library.media_type` | N:1 (optional) | RESTRICT | yes | intra-module |
| 404 | `library.media.media_category_id` | `library.media_category` | N:1 (optional) | RESTRICT | yes | intra-module |
| 405 | `library.media_author.media_id` | `library.media` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 406 | `library.media_author.author_id` | `library.author` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 407 | `library.media_publisher.media_id` | `library.media` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 408 | `library.media_publisher.publisher_id` | `library.publisher` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 409 | `library.media_course.media_id` | `library.media` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 410 | `library.media_subject.media_id` | `library.media` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 411 | `library.media_unit.media_id` | `library.media` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 412 | `library.library_card.card_type_id` | `library.library_card_type` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 413 | `library.loan.media_unit_id` | `library.media_unit` | 1:1 (mandatory) | RESTRICT |  | intra-module |
| 414 | `library.loan.media_id` | `library.media` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 415 | `library.loan.library_card_id` | `library.library_card` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 416 | `library.reservation.media_id` | `library.media` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 417 | `library.purchase_request.media_type_id` | `library.media_type` | N:1 (optional) | RESTRICT | yes | intra-module |
| 418 | `library.purchase_request_course.purchase_request_id` | `library.purchase_request` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 419 | `library.purchase_request_subject.purchase_request_id` | `library.purchase_request` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 420 | `transport.route_stop.route_id` | `transport.route` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 421 | `transport.student_transport.route_id` | `transport.route` | N:1 (optional) | SET NULL | yes | intra-module |
| 422 | `transport.student_transport.route_stop_id` | `transport.route_stop` | N:1 (optional) | SET NULL | yes | intra-module |
| 423 | `comms.outbound_message.template_id` | `comms.message_template` | N:1 (optional) | SET NULL | yes | intra-module |
| 424 | `comms.in_app_notification.notification_type_id` | `comms.notification_type` | N:1 (mandatory) | RESTRICT |  | intra-module |
| 425 | `comms.notice.background_id` | `comms.notice_background` | N:1 (optional) | SET NULL | yes | intra-module |
| 426 | `comms.event_gallery_item.event_id` | `comms.event` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 427 | `comms.conversation_participant.conversation_id` | `comms.conversation` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 428 | `comms.conversation_message.conversation_id` | `comms.conversation` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 429 | `comms.message_receipt.message_id` | `comms.conversation_message` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 430 | `comms.feedback_message.thread_id` | `comms.feedback_thread` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 431 | `comms.audience_group_member.group_id` | `comms.audience_group` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 432 | `comms.task_assignee.task_id` | `comms.task` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 433 | `comms.task_assignee.group_id` | `comms.audience_group` | N:1 (optional) | RESTRICT | yes | intra-module |
| 434 | `comms.class_page.page_category_id` | `comms.page_category` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 435 | `comms.class_page_attachment.class_page_id` | `comms.class_page` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 436 | `comms.class_page_member.class_page_id` | `comms.class_page` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 437 | `comms.post.class_page_id` | `comms.class_page` | N:1 (optional) | RESTRICT | yes | intra-module |
| 438 | `comms.post_audience_class.post_id` | `comms.post` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 439 | `comms.post_comment.post_id` | `comms.post` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 440 | `comms.post_comment.parent_comment_id` | `comms.post_comment` | N:1 (optional) | CASCADE | yes | intra-module |
| 441 | `comms.notice_audience.notice_id` | `comms.notice` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 442 | `comms.notice_version.notice_id` | `comms.notice` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 443 | `comms.event_institution.event_id` | `comms.event` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 444 | `comms.event_occurrence.event_id` | `comms.event` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 445 | `comms.event_attendee.event_id` | `comms.event` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 446 | `comms.notification_type_property.notification_type_id` | `comms.notification_type` | N:1 (mandatory) | CASCADE |  | intra-module |
| 447 | `comms.notification_rule.notification_type_id` | `comms.notification_type` | N:1 (mandatory) | CASCADE |  | intra-module |
| 448 | `comms.notification_cohort_setting.notification_type_id` | `comms.notification_type` | N:1 (mandatory) | CASCADE |  | intra-module |
| 449 | `comms.notification_channel_property.notification_channel_id` | `comms.notification_channel` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 450 | `comms.notification_preference.preference_set_id` | `comms.notification_preference_set` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 451 | `comms.notification_preference.notification_type_id` | `comms.notification_type` | N:1 (optional) | CASCADE |  | intra-module |
| 452 | `comms.reaction.post_id` | `comms.post` | N:1 (optional) | CASCADE | yes | intra-module |
| 453 | `comms.reaction.comment_id` | `comms.post_comment` | N:1 (optional) | CASCADE | yes | intra-module |
| 454 | `careers.job_application.job_opening_id` | `careers.job_opening` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 455 | `careers.candidate_preferred_function.candidate_preference_id` | `careers.candidate_preference` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 456 | `careers.candidate_preferred_function.function_area_id` | `careers.function_area` | N:1 (mandatory) | RESTRICT | yes | intra-module |
| 457 | `careers.candidate_preferred_industry.candidate_preference_id` | `careers.candidate_preference` | N:1 (mandatory) | CASCADE | yes | intra-module |
| 458 | `careers.candidate_preferred_industry.industry_id` | `careers.industry` | N:1 (mandatory) | RESTRICT | yes | intra-module |
