# Operation status

Written by the build (`OperationStatusReport` in `backend/conformance`) from the contracts and the
controllers — do not edit by hand. *Served* operations are implemented and covered by component tests
against PostgreSQL; every other operation answers **501 `NOT_IMPLEMENTED`** at the edge, never a
misleading 404.

| Module | Operations | Served | 501 |
|---|---:|---:|---:|
| academics | 75 | 75 | 0 |
| admission | 43 | 32 | 11 |
| assessment | 132 | 112 | 20 |
| attendance | 33 | 33 | 0 |
| careers | 40 | 35 | 5 |
| comms | 235 | 202 | 33 |
| facility | 25 | 25 | 0 |
| finance | 79 | 56 | 23 |
| frontoffice | 21 | 20 | 1 |
| hr | 40 | 38 | 2 |
| iam | 88 | 73 | 15 |
| library | 95 | 90 | 5 |
| lms | 711 | 543 | 168 |
| master | 20 | 20 | 0 |
| org | 49 | 46 | 3 |
| payroll | 58 | 56 | 2 |
| people | 73 | 66 | 7 |
| platform | 84 | 53 | 31 |
| sis | 108 | 96 | 12 |
| timetable | 25 | 23 | 2 |
| transport | 15 | 15 | 0 |
| **all** | **2049** | **1709** | **340** |

## academics

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `academics.ListProgramLevels` | GET | `/v1/academics/program-levels` | `academics.program_level.read` | served |  |
| `academics.CreateProgramLevel` | POST | `/v1/academics/program-levels` | `academics.program_level.create` | served |  |
| `academics.GetProgramLevel` | GET | `/v1/academics/program-levels/{programLevelId}` | `academics.program_level.read` | served |  |
| `academics.UpdateProgramLevel` | PATCH | `/v1/academics/program-levels/{programLevelId}` | `academics.program_level.update` | served |  |
| `academics.DeleteProgramLevel` | DELETE | `/v1/academics/program-levels/{programLevelId}` | `academics.program_level.delete` | served |  |
| `academics.ListPrograms` | GET | `/v1/academics/programs` | `academics.program.read` | served |  |
| `academics.CreateProgram` | POST | `/v1/academics/programs` | `academics.program.create` | served |  |
| `academics.GetProgram` | GET | `/v1/academics/programs/{programId}` | `academics.program.read` | served |  |
| `academics.UpdateProgram` | PATCH | `/v1/academics/programs/{programId}` | `academics.program.update` | served |  |
| `academics.DeleteProgram` | DELETE | `/v1/academics/programs/{programId}` | `academics.program.delete` | served |  |
| `academics.ListCourses` | GET | `/v1/academics/courses` | `academics.course.read` | served |  |
| `academics.CreateCourse` | POST | `/v1/academics/courses` | `academics.course.create` | served |  |
| `academics.GetCourse` | GET | `/v1/academics/courses/{courseId}` | `academics.course.read` | served |  |
| `academics.UpdateCourse` | PATCH | `/v1/academics/courses/{courseId}` | `academics.course.update` | served |  |
| `academics.DeleteCourse` | DELETE | `/v1/academics/courses/{courseId}` | `academics.course.delete` | served |  |
| `academics.ListBatches` | GET | `/v1/academics/batches` | `academics.batch.read` | served |  |
| `academics.CreateBatch` | POST | `/v1/academics/batches` | `academics.batch.create` | served |  |
| `academics.GetBatch` | GET | `/v1/academics/batches/{batchId}` | `academics.batch.read` | served |  |
| `academics.UpdateBatch` | PATCH | `/v1/academics/batches/{batchId}` | `academics.batch.update` | served |  |
| `academics.DeleteBatch` | DELETE | `/v1/academics/batches/{batchId}` | `academics.batch.delete` | served |  |
| `academics.ListGradeLevels` | GET | `/v1/academics/grade-levels` | `academics.grade_level.read` | served |  |
| `academics.CreateGradeLevel` | POST | `/v1/academics/grade-levels` | `academics.grade_level.create` | served |  |
| `academics.GetGradeLevel` | GET | `/v1/academics/grade-levels/{gradeLevelId}` | `academics.grade_level.read` | served |  |
| `academics.UpdateGradeLevel` | PATCH | `/v1/academics/grade-levels/{gradeLevelId}` | `academics.grade_level.update` | served |  |
| `academics.DeleteGradeLevel` | DELETE | `/v1/academics/grade-levels/{gradeLevelId}` | `academics.grade_level.delete` | served |  |
| `academics.ListSections` | GET | `/v1/academics/sections` | `academics.section.read` | served |  |
| `academics.CreateSection` | POST | `/v1/academics/sections` | `academics.section.create` | served |  |
| `academics.GetSection` | GET | `/v1/academics/sections/{sectionId}` | `academics.section.read` | served |  |
| `academics.UpdateSection` | PATCH | `/v1/academics/sections/{sectionId}` | `academics.section.update` | served |  |
| `academics.DeleteSection` | DELETE | `/v1/academics/sections/{sectionId}` | `academics.section.delete` | served |  |
| `academics.ListClassSections` | GET | `/v1/academics/class-sections` | `academics.class_section.read` | served |  |
| `academics.CreateClassSection` | POST | `/v1/academics/class-sections` | `academics.class_section.create` | served |  |
| `academics.GetClassSection` | GET | `/v1/academics/class-sections/{classSectionId}` | `academics.class_section.read` | served |  |
| `academics.UpdateClassSection` | PATCH | `/v1/academics/class-sections/{classSectionId}` | `academics.class_section.update` | served |  |
| `academics.DeleteClassSection` | DELETE | `/v1/academics/class-sections/{classSectionId}` | `academics.class_section.delete` | served |  |
| `academics.ListSubjects` | GET | `/v1/academics/subjects` | `academics.subject.read` | served |  |
| `academics.CreateSubject` | POST | `/v1/academics/subjects` | `academics.subject.create` | served |  |
| `academics.GetSubject` | GET | `/v1/academics/subjects/{subjectId}` | `academics.subject.read` | served |  |
| `academics.UpdateSubject` | PATCH | `/v1/academics/subjects/{subjectId}` | `academics.subject.update` | served |  |
| `academics.DeleteSubject` | DELETE | `/v1/academics/subjects/{subjectId}` | `academics.subject.delete` | served |  |
| `academics.ListCourseSubjects` | GET | `/v1/academics/courses/{courseId}/subjects` | `academics.course_subject.read` | served |  |
| `academics.CreateCourseSubject` | POST | `/v1/academics/courses/{courseId}/subjects` | `academics.course_subject.create` | served |  |
| `academics.GetCourseSubject` | GET | `/v1/academics/courses/{courseId}/subjects/{courseSubjectId}` | `academics.course_subject.read` | served |  |
| `academics.UpdateCourseSubject` | PATCH | `/v1/academics/courses/{courseId}/subjects/{courseSubjectId}` | `academics.course_subject.update` | served |  |
| `academics.DeleteCourseSubject` | DELETE | `/v1/academics/courses/{courseId}/subjects/{courseSubjectId}` | `academics.course_subject.delete` | served |  |
| `academics.ListClassSubjects` | GET | `/v1/academics/class-sections/{classSectionId}/subjects` | `academics.class_subject.read` | served |  |
| `academics.CreateClassSubject` | POST | `/v1/academics/class-sections/{classSectionId}/subjects` | `academics.class_subject.create` | served |  |
| `academics.GetClassSubject` | GET | `/v1/academics/class-sections/{classSectionId}/subjects/{classSubjectId}` | `academics.class_subject.read` | served |  |
| `academics.UpdateClassSubject` | PATCH | `/v1/academics/class-sections/{classSectionId}/subjects/{classSubjectId}` | `academics.class_subject.update` | served |  |
| `academics.DeleteClassSubject` | DELETE | `/v1/academics/class-sections/{classSectionId}/subjects/{classSubjectId}` | `academics.class_subject.delete` | served |  |
| `academics.ListTeachingAssignments` | GET | `/v1/academics/teaching-assignments` | `academics.teaching_assignment.read` | served |  |
| `academics.CreateTeachingAssignment` | POST | `/v1/academics/teaching-assignments` | `academics.teaching_assignment.create` | served |  |
| `academics.GetTeachingAssignment` | GET | `/v1/academics/teaching-assignments/{teachingAssignmentId}` | `academics.teaching_assignment.read` | served |  |
| `academics.UpdateTeachingAssignment` | PATCH | `/v1/academics/teaching-assignments/{teachingAssignmentId}` | `academics.teaching_assignment.update` | served |  |
| `academics.DeleteTeachingAssignment` | DELETE | `/v1/academics/teaching-assignments/{teachingAssignmentId}` | `academics.teaching_assignment.delete` | served |  |
| `academics.ListCurriculumUnits` | GET | `/v1/academics/curriculum-units` | `academics.curriculum_unit.read` | served |  |
| `academics.CreateCurriculumUnit` | POST | `/v1/academics/curriculum-units` | `academics.curriculum_unit.create` | served |  |
| `academics.GetCurriculumUnit` | GET | `/v1/academics/curriculum-units/{curriculumUnitId}` | `academics.curriculum_unit.read` | served |  |
| `academics.UpdateCurriculumUnit` | PATCH | `/v1/academics/curriculum-units/{curriculumUnitId}` | `academics.curriculum_unit.update` | served |  |
| `academics.DeleteCurriculumUnit` | DELETE | `/v1/academics/curriculum-units/{curriculumUnitId}` | `academics.curriculum_unit.delete` | served |  |
| `academics.ListLessonPlans` | GET | `/v1/academics/lesson-plans` | `academics.lesson_plan.read` | served |  |
| `academics.CreateLessonPlan` | POST | `/v1/academics/lesson-plans` | `academics.lesson_plan.create` | served |  |
| `academics.GetLessonPlan` | GET | `/v1/academics/lesson-plans/{lessonPlanId}` | `academics.lesson_plan.read` | served |  |
| `academics.UpdateLessonPlan` | PATCH | `/v1/academics/lesson-plans/{lessonPlanId}` | `academics.lesson_plan.update` | served |  |
| `academics.DeleteLessonPlan` | DELETE | `/v1/academics/lesson-plans/{lessonPlanId}` | `academics.lesson_plan.delete` | served |  |
| `academics.ListLessonPlanApprovals` | GET | `/v1/academics/lesson-plans/{lessonPlanId}/approvals` | `academics.lesson_plan_approval.read` | served |  |
| `academics.CreateLessonPlanApproval` | POST | `/v1/academics/lesson-plans/{lessonPlanId}/approvals` | `academics.lesson_plan_approval.create` | served |  |
| `academics.GetLessonPlanApproval` | GET | `/v1/academics/lesson-plans/{lessonPlanId}/approvals/{lessonPlanApprovalId}` | `academics.lesson_plan_approval.read` | served |  |
| `academics.UpdateLessonPlanApproval` | PATCH | `/v1/academics/lesson-plans/{lessonPlanId}/approvals/{lessonPlanApprovalId}` | `academics.lesson_plan_approval.update` | served |  |
| `academics.DeleteLessonPlanApproval` | DELETE | `/v1/academics/lesson-plans/{lessonPlanId}/approvals/{lessonPlanApprovalId}` | `academics.lesson_plan_approval.delete` | served |  |
| `academics.SubmitLessonPlan` | POST | `/v1/academics/lesson-plans/{lessonPlanId}:submit` | `academics.lesson_plan.submit_lesson_plan` | served |  |
| `academics.ApproveLessonPlan` | POST | `/v1/academics/lesson-plans/{lessonPlanId}:approve` | `academics.lesson_plan.approve_lesson_plan` | served |  |
| `academics.RejectLessonPlan` | POST | `/v1/academics/lesson-plans/{lessonPlanId}:reject` | `academics.lesson_plan.reject_lesson_plan` | served |  |
| `academics.PublishLessonPlan` | POST | `/v1/academics/lesson-plans/{lessonPlanId}:publish` | `academics.lesson_plan.publish_lesson_plan` | served |  |
| `academics.ArchiveLessonPlan` | POST | `/v1/academics/lesson-plans/{lessonPlanId}:archive` | `academics.lesson_plan.archive_lesson_plan` | served |  |

## admission

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `admission.ListAdmissionCycles` | GET | `/v1/admission/admission-cycles` | `admission.admission_cycle.read` | served |  |
| `admission.CreateAdmissionCycle` | POST | `/v1/admission/admission-cycles` | `admission.admission_cycle.create` | served |  |
| `admission.GetAdmissionCycle` | GET | `/v1/admission/admission-cycles/{admissionCycleId}` | `admission.admission_cycle.read` | served |  |
| `admission.UpdateAdmissionCycle` | PATCH | `/v1/admission/admission-cycles/{admissionCycleId}` | `admission.admission_cycle.update` | served |  |
| `admission.DeleteAdmissionCycle` | DELETE | `/v1/admission/admission-cycles/{admissionCycleId}` | `admission.admission_cycle.delete` | served |  |
| `admission.ListAdmissionCycleFees` | GET | `/v1/admission/admission-cycles/{admissionCycleId}/fees` | `admission.admission_cycle_fee.read` | served |  |
| `admission.CreateAdmissionCycleFee` | POST | `/v1/admission/admission-cycles/{admissionCycleId}/fees` | `admission.admission_cycle_fee.create` | served |  |
| `admission.GetAdmissionCycleFee` | GET | `/v1/admission/admission-cycles/{admissionCycleId}/fees/{admissionCycleFeeId}` | `admission.admission_cycle_fee.read` | served |  |
| `admission.UpdateAdmissionCycleFee` | PATCH | `/v1/admission/admission-cycles/{admissionCycleId}/fees/{admissionCycleFeeId}` | `admission.admission_cycle_fee.update` | served |  |
| `admission.DeleteAdmissionCycleFee` | DELETE | `/v1/admission/admission-cycles/{admissionCycleId}/fees/{admissionCycleFeeId}` | `admission.admission_cycle_fee.delete` | served |  |
| `admission.ListApplications` | GET | `/v1/admission/applications` | `admission.application.read` | served |  |
| `admission.CreateApplication` | POST | `/v1/admission/applications` | `admission.application.create` | served |  |
| `admission.GetApplication` | GET | `/v1/admission/applications/{applicationId}` | `admission.application.read` | served |  |
| `admission.UpdateApplication` | PATCH | `/v1/admission/applications/{applicationId}` | `admission.application.update` | served |  |
| `admission.DeleteApplication` | DELETE | `/v1/admission/applications/{applicationId}` | `admission.application.delete` | served |  |
| `admission.ListApplicationGuardians` | GET | `/v1/admission/applications/{applicationId}/guardians` | `admission.application_guardian.read` | served |  |
| `admission.CreateApplicationGuardian` | POST | `/v1/admission/applications/{applicationId}/guardians` | `admission.application_guardian.create` | served |  |
| `admission.GetApplicationGuardian` | GET | `/v1/admission/applications/{applicationId}/guardians/{applicationGuardianId}` | `admission.application_guardian.read` | served |  |
| `admission.UpdateApplicationGuardian` | PATCH | `/v1/admission/applications/{applicationId}/guardians/{applicationGuardianId}` | `admission.application_guardian.update` | served |  |
| `admission.DeleteApplicationGuardian` | DELETE | `/v1/admission/applications/{applicationId}/guardians/{applicationGuardianId}` | `admission.application_guardian.delete` | served |  |
| `admission.ListApplicationEmergencyContacts` | GET | `/v1/admission/applications/{applicationId}/emergency-contacts` | `admission.application_emergency_contact.read` | served |  |
| `admission.CreateApplicationEmergencyContact` | POST | `/v1/admission/applications/{applicationId}/emergency-contacts` | `admission.application_emergency_contact.create` | served |  |
| `admission.GetApplicationEmergencyContact` | GET | `/v1/admission/applications/{applicationId}/emergency-contacts/{applicationEmergencyContactId}` | `admission.application_emergency_contact.read` | served |  |
| `admission.UpdateApplicationEmergencyContact` | PATCH | `/v1/admission/applications/{applicationId}/emergency-contacts/{applicationEmergencyContactId}` | `admission.application_emergency_contact.update` | served |  |
| `admission.DeleteApplicationEmergencyContact` | DELETE | `/v1/admission/applications/{applicationId}/emergency-contacts/{applicationEmergencyContactId}` | `admission.application_emergency_contact.delete` | served |  |
| `admission.ConfirmAdmissionCycle` | POST | `/v1/admission/admission-cycles/{admissionCycleId}:confirm` | `admission.admission_cycle.confirm_admission_cycle` | served |  |
| `admission.OpenApplications` | POST | `/v1/admission/admission-cycles/{admissionCycleId}:openApplications` | `admission.admission_cycle.open_applications` | 501 | workflow / custom operation |
| `admission.StartAdmission` | POST | `/v1/admission/admission-cycles/{admissionCycleId}:startAdmission` | `admission.admission_cycle.start_admission` | 501 | workflow / custom operation |
| `admission.CloseAdmissionCycle` | POST | `/v1/admission/admission-cycles/{admissionCycleId}:close` | `admission.admission_cycle.close_admission_cycle` | served |  |
| `admission.CancelAdmissionCycle` | POST | `/v1/admission/admission-cycles/{admissionCycleId}:cancel` | `admission.admission_cycle.cancel_admission_cycle` | served |  |
| `admission.ResetAdmissionCycle` | POST | `/v1/admission/admission-cycles/{admissionCycleId}:resetToDraft` | `admission.admission_cycle.reset_admission_cycle` | served |  |
| `admission.SubmitApplication` | POST | `/v1/admission/applications/{applicationId}:submit` | `admission.application.submit_application` | 501 | workflow / custom operation |
| `admission.ConfirmApplication` | POST | `/v1/admission/applications/{applicationId}:confirm` | `admission.application.confirm_application` | 501 | workflow / custom operation |
| `admission.MarkApplicationPending` | POST | `/v1/admission/applications/{applicationId}:markPending` | `admission.application.mark_application_pending` | 501 | workflow / custom operation |
| `admission.RejectApplication` | POST | `/v1/admission/applications/{applicationId}:reject` | `admission.application.reject_application` | 501 | workflow / custom operation |
| `admission.CancelApplication` | POST | `/v1/admission/applications/{applicationId}:cancel` | `admission.application.cancel_application` | 501 | workflow / custom operation |
| `admission.ResetApplication` | POST | `/v1/admission/applications/{applicationId}:resetToDraft` | `admission.application.reset_application` | 501 | workflow / custom operation |
| `admission.ApproveApplication` | POST | `/v1/admission/applications/{applicationId}:approve` | `admission.application.approve_application` | 501 | workflow / custom operation |
| `admission.ConfirmAdmission` | POST | `/v1/admission/applications/{applicationId}:confirmAdmission` | `admission.application.confirm_admission` | 501 | workflow / custom operation |
| `admission.EnrollApplicant` | POST | `/v1/admission/applications/{applicationId}:enroll` | `admission.application.enroll_applicant` | 501 | workflow / custom operation |
| `admission.ListOperations` | GET | `/v1/admission/operations` | `admission.operation.read` | served |  |
| `admission.GetOperation` | GET | `/v1/admission/operations/{operationId}` | `admission.operation.read` | served |  |
| `admission.CancelOperation` | POST | `/v1/admission/operations/{operationId}:cancel` | `admission.operation.cancel_operation` | served |  |

## assessment

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `assessment.ListHomeworks` | GET | `/v1/assessment/homeworks` | `assessment.homework.read` | served |  |
| `assessment.CreateHomework` | POST | `/v1/assessment/homeworks` | `assessment.homework.create` | served |  |
| `assessment.GetHomework` | GET | `/v1/assessment/homeworks/{homeworkId}` | `assessment.homework.read` | served |  |
| `assessment.UpdateHomework` | PATCH | `/v1/assessment/homeworks/{homeworkId}` | `assessment.homework.update` | served |  |
| `assessment.DeleteHomework` | DELETE | `/v1/assessment/homeworks/{homeworkId}` | `assessment.homework.delete` | served |  |
| `assessment.ListHomeworkApprovals` | GET | `/v1/assessment/homeworks/{homeworkId}/approvals` | `assessment.homework_approval.read` | served |  |
| `assessment.GetHomeworkApproval` | GET | `/v1/assessment/homeworks/{homeworkId}/approvals/{homeworkApprovalId}` | `assessment.homework_approval.read` | served |  |
| `assessment.ListHomeworkSubmissions` | GET | `/v1/assessment/homework-submissions` | `assessment.homework_submission.read` | served |  |
| `assessment.CreateHomeworkSubmission` | POST | `/v1/assessment/homework-submissions` | `assessment.homework_submission.create` | served |  |
| `assessment.GetHomeworkSubmission` | GET | `/v1/assessment/homework-submissions/{homeworkSubmissionId}` | `assessment.homework_submission.read` | served |  |
| `assessment.UpdateHomeworkSubmission` | PATCH | `/v1/assessment/homework-submissions/{homeworkSubmissionId}` | `assessment.homework_submission.update` | served |  |
| `assessment.DeleteHomeworkSubmission` | DELETE | `/v1/assessment/homework-submissions/{homeworkSubmissionId}` | `assessment.homework_submission.delete` | served |  |
| `assessment.ListAssignmentTypes` | GET | `/v1/assessment/assignment-types` | `assessment.assignment_type.read` | served |  |
| `assessment.CreateAssignmentType` | POST | `/v1/assessment/assignment-types` | `assessment.assignment_type.create` | served |  |
| `assessment.GetAssignmentType` | GET | `/v1/assessment/assignment-types/{assignmentTypeId}` | `assessment.assignment_type.read` | served |  |
| `assessment.UpdateAssignmentType` | PATCH | `/v1/assessment/assignment-types/{assignmentTypeId}` | `assessment.assignment_type.update` | served |  |
| `assessment.DeleteAssignmentType` | DELETE | `/v1/assessment/assignment-types/{assignmentTypeId}` | `assessment.assignment_type.delete` | served |  |
| `assessment.ListAssignments` | GET | `/v1/assessment/assignments` | `assessment.assignment.read` | served |  |
| `assessment.CreateAssignment` | POST | `/v1/assessment/assignments` | `assessment.assignment.create` | served |  |
| `assessment.GetAssignment` | GET | `/v1/assessment/assignments/{assignmentId}` | `assessment.assignment.read` | served |  |
| `assessment.UpdateAssignment` | PATCH | `/v1/assessment/assignments/{assignmentId}` | `assessment.assignment.update` | served |  |
| `assessment.DeleteAssignment` | DELETE | `/v1/assessment/assignments/{assignmentId}` | `assessment.assignment.delete` | served |  |
| `assessment.ListAssignmentSubjects` | GET | `/v1/assessment/assignments/{assignmentId}/subjects` | `assessment.assignment_subject.read` | served |  |
| `assessment.CreateAssignmentSubject` | POST | `/v1/assessment/assignments/{assignmentId}/subjects` | `assessment.assignment_subject.create` | served |  |
| `assessment.GetAssignmentSubject` | GET | `/v1/assessment/assignments/{assignmentId}/subjects/{assignmentSubjectId}` | `assessment.assignment_subject.read` | served |  |
| `assessment.UpdateAssignmentSubject` | PATCH | `/v1/assessment/assignments/{assignmentId}/subjects/{assignmentSubjectId}` | `assessment.assignment_subject.update` | served |  |
| `assessment.DeleteAssignmentSubject` | DELETE | `/v1/assessment/assignments/{assignmentId}/subjects/{assignmentSubjectId}` | `assessment.assignment_subject.delete` | served |  |
| `assessment.ListAssignmentAllocations` | GET | `/v1/assessment/assignments/{assignmentId}/allocations` | `assessment.assignment_allocation.read` | served |  |
| `assessment.CreateAssignmentAllocation` | POST | `/v1/assessment/assignments/{assignmentId}/allocations` | `assessment.assignment_allocation.create` | served |  |
| `assessment.GetAssignmentAllocation` | GET | `/v1/assessment/assignments/{assignmentId}/allocations/{assignmentAllocationId}` | `assessment.assignment_allocation.read` | served |  |
| `assessment.UpdateAssignmentAllocation` | PATCH | `/v1/assessment/assignments/{assignmentId}/allocations/{assignmentAllocationId}` | `assessment.assignment_allocation.update` | served |  |
| `assessment.DeleteAssignmentAllocation` | DELETE | `/v1/assessment/assignments/{assignmentId}/allocations/{assignmentAllocationId}` | `assessment.assignment_allocation.delete` | served |  |
| `assessment.ListAssignmentApprovals` | GET | `/v1/assessment/assignments/{assignmentId}/approvals` | `assessment.assignment_approval.read` | served |  |
| `assessment.CreateAssignmentApproval` | POST | `/v1/assessment/assignments/{assignmentId}/approvals` | `assessment.assignment_approval.create` | served |  |
| `assessment.GetAssignmentApproval` | GET | `/v1/assessment/assignments/{assignmentId}/approvals/{assignmentApprovalId}` | `assessment.assignment_approval.read` | served |  |
| `assessment.UpdateAssignmentApproval` | PATCH | `/v1/assessment/assignments/{assignmentId}/approvals/{assignmentApprovalId}` | `assessment.assignment_approval.update` | served |  |
| `assessment.DeleteAssignmentApproval` | DELETE | `/v1/assessment/assignments/{assignmentId}/approvals/{assignmentApprovalId}` | `assessment.assignment_approval.delete` | served |  |
| `assessment.ListAssignmentSubmissions` | GET | `/v1/assessment/assignment-submissions` | `assessment.assignment_submission.read` | served |  |
| `assessment.CreateAssignmentSubmission` | POST | `/v1/assessment/assignment-submissions` | `assessment.assignment_submission.create` | served |  |
| `assessment.GetAssignmentSubmission` | GET | `/v1/assessment/assignment-submissions/{assignmentSubmissionId}` | `assessment.assignment_submission.read` | served |  |
| `assessment.UpdateAssignmentSubmission` | PATCH | `/v1/assessment/assignment-submissions/{assignmentSubmissionId}` | `assessment.assignment_submission.update` | served |  |
| `assessment.DeleteAssignmentSubmission` | DELETE | `/v1/assessment/assignment-submissions/{assignmentSubmissionId}` | `assessment.assignment_submission.delete` | served |  |
| `assessment.ListGradingSchemes` | GET | `/v1/assessment/grading-schemes` | `assessment.grading_scheme.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `assessment.CreateGradingScheme` | POST | `/v1/assessment/grading-schemes` | `assessment.grading_scheme.create` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `assessment.GetGradingScheme` | GET | `/v1/assessment/grading-schemes/{gradingSchemeId}` | `assessment.grading_scheme.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `assessment.UpdateGradingScheme` | PATCH | `/v1/assessment/grading-schemes/{gradingSchemeId}` | `assessment.grading_scheme.update` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `assessment.DeleteGradingScheme` | DELETE | `/v1/assessment/grading-schemes/{gradingSchemeId}` | `assessment.grading_scheme.delete` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `assessment.ListGradingBands` | GET | `/v1/assessment/grading-bands` | `assessment.grading_band.read` | served |  |
| `assessment.CreateGradingBand` | POST | `/v1/assessment/grading-bands` | `assessment.grading_band.create` | served |  |
| `assessment.GetGradingBand` | GET | `/v1/assessment/grading-bands/{gradingBandId}` | `assessment.grading_band.read` | served |  |
| `assessment.UpdateGradingBand` | PATCH | `/v1/assessment/grading-bands/{gradingBandId}` | `assessment.grading_band.update` | served |  |
| `assessment.DeleteGradingBand` | DELETE | `/v1/assessment/grading-bands/{gradingBandId}` | `assessment.grading_band.delete` | served |  |
| `assessment.ListExamTypes` | GET | `/v1/assessment/exam-types` | `assessment.exam_type.read` | served |  |
| `assessment.CreateExamType` | POST | `/v1/assessment/exam-types` | `assessment.exam_type.create` | served |  |
| `assessment.GetExamType` | GET | `/v1/assessment/exam-types/{examTypeId}` | `assessment.exam_type.read` | served |  |
| `assessment.UpdateExamType` | PATCH | `/v1/assessment/exam-types/{examTypeId}` | `assessment.exam_type.update` | served |  |
| `assessment.DeleteExamType` | DELETE | `/v1/assessment/exam-types/{examTypeId}` | `assessment.exam_type.delete` | served |  |
| `assessment.ListExamSessions` | GET | `/v1/assessment/exam-sessions` | `assessment.exam_session.read` | served |  |
| `assessment.CreateExamSession` | POST | `/v1/assessment/exam-sessions` | `assessment.exam_session.create` | served |  |
| `assessment.GetExamSession` | GET | `/v1/assessment/exam-sessions/{examSessionId}` | `assessment.exam_session.read` | served |  |
| `assessment.UpdateExamSession` | PATCH | `/v1/assessment/exam-sessions/{examSessionId}` | `assessment.exam_session.update` | served |  |
| `assessment.DeleteExamSession` | DELETE | `/v1/assessment/exam-sessions/{examSessionId}` | `assessment.exam_session.delete` | served |  |
| `assessment.ListExams` | GET | `/v1/assessment/exams` | `assessment.exam.read` | served |  |
| `assessment.CreateExam` | POST | `/v1/assessment/exams` | `assessment.exam.create` | served |  |
| `assessment.GetExam` | GET | `/v1/assessment/exams/{examId}` | `assessment.exam.read` | served |  |
| `assessment.UpdateExam` | PATCH | `/v1/assessment/exams/{examId}` | `assessment.exam.update` | served |  |
| `assessment.DeleteExam` | DELETE | `/v1/assessment/exams/{examId}` | `assessment.exam.delete` | served |  |
| `assessment.ListExamInvigilators` | GET | `/v1/assessment/exams/{examId}/invigilators` | `assessment.exam_invigilator.read` | served |  |
| `assessment.CreateExamInvigilator` | POST | `/v1/assessment/exams/{examId}/invigilators` | `assessment.exam_invigilator.create` | served |  |
| `assessment.GetExamInvigilator` | GET | `/v1/assessment/exams/{examId}/invigilators/{examInvigilatorId}` | `assessment.exam_invigilator.read` | served |  |
| `assessment.UpdateExamInvigilator` | PATCH | `/v1/assessment/exams/{examId}/invigilators/{examInvigilatorId}` | `assessment.exam_invigilator.update` | served |  |
| `assessment.DeleteExamInvigilator` | DELETE | `/v1/assessment/exams/{examId}/invigilators/{examInvigilatorId}` | `assessment.exam_invigilator.delete` | served |  |
| `assessment.ListExamResults` | GET | `/v1/assessment/exam-results` | `assessment.exam_result.read` | served |  |
| `assessment.GetExamResult` | GET | `/v1/assessment/exam-results/{examResultId}` | `assessment.exam_result.read` | served |  |
| `assessment.UpdateExamResult` | PATCH | `/v1/assessment/exam-results/{examResultId}` | `assessment.exam_result.update` | served |  |
| `assessment.ListResultTemplates` | GET | `/v1/assessment/result-templates` | `assessment.result_template.read` | served |  |
| `assessment.CreateResultTemplate` | POST | `/v1/assessment/result-templates` | `assessment.result_template.create` | served |  |
| `assessment.GetResultTemplate` | GET | `/v1/assessment/result-templates/{resultTemplateId}` | `assessment.result_template.read` | served |  |
| `assessment.UpdateResultTemplate` | PATCH | `/v1/assessment/result-templates/{resultTemplateId}` | `assessment.result_template.update` | served |  |
| `assessment.DeleteResultTemplate` | DELETE | `/v1/assessment/result-templates/{resultTemplateId}` | `assessment.result_template.delete` | served |  |
| `assessment.ListResultTemplateBands` | GET | `/v1/assessment/result-templates/{resultTemplateId}/result-template-bands` | `assessment.result_template_band.read` | served |  |
| `assessment.CreateResultTemplateBand` | POST | `/v1/assessment/result-templates/{resultTemplateId}/result-template-bands` | `assessment.result_template_band.create` | served |  |
| `assessment.GetResultTemplateBand` | GET | `/v1/assessment/result-templates/{resultTemplateId}/result-template-bands/{resultTemplateBandId}` | `assessment.result_template_band.read` | served |  |
| `assessment.UpdateResultTemplateBand` | PATCH | `/v1/assessment/result-templates/{resultTemplateId}/result-template-bands/{resultTemplateBandId}` | `assessment.result_template_band.update` | served |  |
| `assessment.DeleteResultTemplateBand` | DELETE | `/v1/assessment/result-templates/{resultTemplateId}/result-template-bands/{resultTemplateBandId}` | `assessment.result_template_band.delete` | served |  |
| `assessment.ListMarksheets` | GET | `/v1/assessment/marksheets` | `assessment.marksheet.read` | served |  |
| `assessment.CreateMarksheet` | POST | `/v1/assessment/marksheets` | `assessment.marksheet.create` | served |  |
| `assessment.GetMarksheet` | GET | `/v1/assessment/marksheets/{marksheetId}` | `assessment.marksheet.read` | served |  |
| `assessment.UpdateMarksheet` | PATCH | `/v1/assessment/marksheets/{marksheetId}` | `assessment.marksheet.update` | served |  |
| `assessment.DeleteMarksheet` | DELETE | `/v1/assessment/marksheets/{marksheetId}` | `assessment.marksheet.delete` | served |  |
| `assessment.ListMarksheetLines` | GET | `/v1/assessment/marksheet-lines` | `assessment.marksheet_line.read` | served |  |
| `assessment.GetMarksheetLine` | GET | `/v1/assessment/marksheet-lines/{marksheetLineId}` | `assessment.marksheet_line.read` | served |  |
| `assessment.PublishHomework` | POST | `/v1/assessment/homeworks/{homeworkId}:publish` | `assessment.homework.publish_homework` | served |  |
| `assessment.ApproveHomework` | POST | `/v1/assessment/homeworks/{homeworkId}:approve` | `assessment.homework.approve_homework` | served |  |
| `assessment.RejectHomework` | POST | `/v1/assessment/homeworks/{homeworkId}:reject` | `assessment.homework.reject_homework` | served |  |
| `assessment.CheckHomeworkSubmission` | POST | `/v1/assessment/homework-submissions/{homeworkSubmissionId}:check` | `assessment.homework_submission.check_homework_submission` | served |  |
| `assessment.ReplyToHomeworkFeedback` | POST | `/v1/assessment/homework-submissions/{homeworkSubmissionId}:reply` | `assessment.homework_submission.reply_to_homework_feedback` | served |  |
| `assessment.PublishAssignment` | POST | `/v1/assessment/assignments/{assignmentId}:publish` | `assessment.assignment.publish_assignment` | served |  |
| `assessment.ApproveAssignment` | POST | `/v1/assessment/assignments/{assignmentId}:approve` | `assessment.assignment.approve_assignment` | 501 | workflow / custom operation |
| `assessment.RejectAssignment` | POST | `/v1/assessment/assignments/{assignmentId}:reject` | `assessment.assignment.reject_assignment` | 501 | workflow / custom operation |
| `assessment.FinishAssignment` | POST | `/v1/assessment/assignments/{assignmentId}:finish` | `assessment.assignment.finish_assignment` | 501 | workflow / custom operation |
| `assessment.CancelAssignment` | POST | `/v1/assessment/assignments/{assignmentId}:cancel` | `assessment.assignment.cancel_assignment` | served |  |
| `assessment.ResetAssignment` | POST | `/v1/assessment/assignments/{assignmentId}:resetToDraft` | `assessment.assignment.reset_assignment` | served |  |
| `assessment.SubmitAssignmentSubmission` | POST | `/v1/assessment/assignment-submissions/{assignmentSubmissionId}:submit` | `assessment.assignment_submission.submit_assignment_submission` | served |  |
| `assessment.AcceptAssignmentSubmission` | POST | `/v1/assessment/assignment-submissions/{assignmentSubmissionId}:accept` | `assessment.assignment_submission.accept_assignment_submission` | served |  |
| `assessment.RequestSubmissionChanges` | POST | `/v1/assessment/assignment-submissions/{assignmentSubmissionId}:requestChanges` | `assessment.assignment_submission.request_submission_changes` | 501 | workflow / custom operation |
| `assessment.RejectAssignmentSubmission` | POST | `/v1/assessment/assignment-submissions/{assignmentSubmissionId}:reject` | `assessment.assignment_submission.reject_assignment_submission` | served |  |
| `assessment.ResetAssignmentSubmission` | POST | `/v1/assessment/assignment-submissions/{assignmentSubmissionId}:resetToDraft` | `assessment.assignment_submission.reset_assignment_submission` | 501 | workflow / custom operation |
| `assessment.GradeAssignmentSubmission` | POST | `/v1/assessment/assignment-submissions/{assignmentSubmissionId}:grade` | `assessment.assignment_submission.grade_assignment_submission` | 501 | workflow / custom operation |
| `assessment.ScheduleExamSession` | POST | `/v1/assessment/exam-sessions/{examSessionId}:schedule` | `assessment.exam_session.schedule_exam_session` | served |  |
| `assessment.MarkExamSessionHeld` | POST | `/v1/assessment/exam-sessions/{examSessionId}:markHeld` | `assessment.exam_session.mark_exam_session_held` | served |  |
| `assessment.CompleteExamSession` | POST | `/v1/assessment/exam-sessions/{examSessionId}:complete` | `assessment.exam_session.complete_exam_session` | served |  |
| `assessment.CancelExamSession` | POST | `/v1/assessment/exam-sessions/{examSessionId}:cancel` | `assessment.exam_session.cancel_exam_session` | served |  |
| `assessment.ResetExamSession` | POST | `/v1/assessment/exam-sessions/{examSessionId}:resetToDraft` | `assessment.exam_session.reset_exam_session` | served |  |
| `assessment.ScheduleExam` | POST | `/v1/assessment/exams/{examId}:schedule` | `assessment.exam.schedule_exam` | served |  |
| `assessment.MarkExamHeld` | POST | `/v1/assessment/exams/{examId}:markHeld` | `assessment.exam.mark_exam_held` | 501 | workflow / custom operation |
| `assessment.MarkExamResultsUpdated` | POST | `/v1/assessment/exams/{examId}:markResultsUpdated` | `assessment.exam.mark_exam_results_updated` | 501 | workflow / custom operation |
| `assessment.CompleteExam` | POST | `/v1/assessment/exams/{examId}:complete` | `assessment.exam.complete_exam` | served |  |
| `assessment.CancelExam` | POST | `/v1/assessment/exams/{examId}:cancel` | `assessment.exam.cancel_exam` | served |  |
| `assessment.ResetExam` | POST | `/v1/assessment/exams/{examId}:resetToDraft` | `assessment.exam.reset_exam` | served |  |
| `assessment.PopulateExamResults` | POST | `/v1/assessment/exams/{examId}:populateResults` | `assessment.exam.populate_exam_results` | 501 | workflow / custom operation |
| `assessment.AllocateExamRooms` | POST | `/v1/assessment/exams/{examId}:allocateRooms` | `assessment.exam.allocate_exam_rooms` | 501 | workflow / custom operation |
| `assessment.RecordExamMarks` | POST | `/v1/assessment/exams/{examId}:recordMarks` | `assessment.exam.record_exam_marks` | 501 | workflow / custom operation |
| `assessment.GenerateResults` | POST | `/v1/assessment/result-templates/{resultTemplateId}:generateResults` | `assessment.result_template.generate_results` | 501 | workflow / custom operation |
| `assessment.ValidateMarksheet` | POST | `/v1/assessment/marksheets/{marksheetId}:validate` | `assessment.marksheet.validate_marksheet` | 501 | workflow / custom operation |
| `assessment.CancelMarksheet` | POST | `/v1/assessment/marksheets/{marksheetId}:cancel` | `assessment.marksheet.cancel_marksheet` | served |  |
| `assessment.ResetMarksheet` | POST | `/v1/assessment/marksheets/{marksheetId}:resetToDraft` | `assessment.marksheet.reset_marksheet` | served |  |
| `assessment.GetStudentResults` | GET | `/v1/assessment/students/{studentId}/results` | `assessment.marksheet_line.get_student_results` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `assessment.PublishMarksheet` | POST | `/v1/assessment/marksheets/{marksheetId}:publish` | `assessment.marksheet.publish` | 501 | workflow / custom operation |
| `assessment.ListOperations` | GET | `/v1/assessment/operations` | `assessment.operation.read` | served |  |
| `assessment.GetOperation` | GET | `/v1/assessment/operations/{operationId}` | `assessment.operation.read` | served |  |
| `assessment.CancelOperation` | POST | `/v1/assessment/operations/{operationId}:cancel` | `assessment.operation.cancel_operation` | served |  |

## attendance

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `attendance.ListAttendanceTypes` | GET | `/v1/attendance/attendance-types` | `attendance.attendance_type.read` | served |  |
| `attendance.CreateAttendanceType` | POST | `/v1/attendance/attendance-types` | `attendance.attendance_type.create` | served |  |
| `attendance.GetAttendanceType` | GET | `/v1/attendance/attendance-types/{attendanceTypeId}` | `attendance.attendance_type.read` | served |  |
| `attendance.UpdateAttendanceType` | PATCH | `/v1/attendance/attendance-types/{attendanceTypeId}` | `attendance.attendance_type.update` | served |  |
| `attendance.DeleteAttendanceType` | DELETE | `/v1/attendance/attendance-types/{attendanceTypeId}` | `attendance.attendance_type.delete` | served |  |
| `attendance.ListAbsenceReasons` | GET | `/v1/attendance/absence-reasons` | `attendance.absence_reason.read` | served |  |
| `attendance.CreateAbsenceReason` | POST | `/v1/attendance/absence-reasons` | `attendance.absence_reason.create` | served |  |
| `attendance.GetAbsenceReason` | GET | `/v1/attendance/absence-reasons/{absenceReasonId}` | `attendance.absence_reason.read` | served |  |
| `attendance.UpdateAbsenceReason` | PATCH | `/v1/attendance/absence-reasons/{absenceReasonId}` | `attendance.absence_reason.update` | served |  |
| `attendance.DeleteAbsenceReason` | DELETE | `/v1/attendance/absence-reasons/{absenceReasonId}` | `attendance.absence_reason.delete` | served |  |
| `attendance.ListAttendanceRegisters` | GET | `/v1/attendance/attendance-registers` | `attendance.attendance_register.read` | served |  |
| `attendance.CreateAttendanceRegister` | POST | `/v1/attendance/attendance-registers` | `attendance.attendance_register.create` | served |  |
| `attendance.GetAttendanceRegister` | GET | `/v1/attendance/attendance-registers/{attendanceRegisterId}` | `attendance.attendance_register.read` | served |  |
| `attendance.UpdateAttendanceRegister` | PATCH | `/v1/attendance/attendance-registers/{attendanceRegisterId}` | `attendance.attendance_register.update` | served |  |
| `attendance.DeleteAttendanceRegister` | DELETE | `/v1/attendance/attendance-registers/{attendanceRegisterId}` | `attendance.attendance_register.delete` | served |  |
| `attendance.ListAttendanceSheets` | GET | `/v1/attendance/attendance-sheets` | `attendance.attendance_sheet.read` | served |  |
| `attendance.CreateAttendanceSheet` | POST | `/v1/attendance/attendance-sheets` | `attendance.attendance_sheet.create` | served |  |
| `attendance.GetAttendanceSheet` | GET | `/v1/attendance/attendance-sheets/{attendanceSheetId}` | `attendance.attendance_sheet.read` | served |  |
| `attendance.UpdateAttendanceSheet` | PATCH | `/v1/attendance/attendance-sheets/{attendanceSheetId}` | `attendance.attendance_sheet.update` | served |  |
| `attendance.DeleteAttendanceSheet` | DELETE | `/v1/attendance/attendance-sheets/{attendanceSheetId}` | `attendance.attendance_sheet.delete` | served |  |
| `attendance.ListAttendanceRecords` | GET | `/v1/attendance/attendance-sheets/{attendanceSheetId}/records` | `attendance.attendance_record.read` | served |  |
| `attendance.GetAttendanceRecord` | GET | `/v1/attendance/attendance-sheets/{attendanceSheetId}/records/{attendanceRecordId}` | `attendance.attendance_record.read` | served |  |
| `attendance.StartAttendanceSheet` | POST | `/v1/attendance/attendance-sheets/{attendanceSheetId}:start` | `attendance.attendance_sheet.start_attendance_sheet` | served |  |
| `attendance.CompleteAttendanceSheet` | POST | `/v1/attendance/attendance-sheets/{attendanceSheetId}:complete` | `attendance.attendance_sheet.complete_attendance_sheet` | served |  |
| `attendance.CancelAttendanceSheet` | POST | `/v1/attendance/attendance-sheets/{attendanceSheetId}:cancel` | `attendance.attendance_sheet.cancel_attendance_sheet` | served |  |
| `attendance.ResetAttendanceSheet` | POST | `/v1/attendance/attendance-sheets/{attendanceSheetId}:resetToDraft` | `attendance.attendance_sheet.reset_attendance_sheet` | served |  |
| `attendance.RecordAttendance` | POST | `/v1/attendance/attendance-sheets/{attendanceSheetId}:recordAttendance` | `attendance.attendance_sheet.record_attendance` | served |  |
| `attendance.TakeAttendanceForSession` | POST | `/v1/attendance/attendance-sheets:forSession` | `attendance.attendance_sheet.take_attendance_for_session` | served |  |
| `attendance.SummarizeAttendance` | GET | `/v1/attendance/summaries` | `attendance.attendance_record.summarize_attendance` | served |  |
| `attendance.ListStaffAttendances` | GET | `/v1/attendance/staff-attendance-days` | `attendance.staff_attendance.read` | served |  |
| `attendance.CreateStaffAttendance` | POST | `/v1/attendance/staff-attendance-days` | `attendance.staff_attendance.create` | served |  |
| `attendance.GetStaffAttendance` | GET | `/v1/attendance/staff-attendance-days/{staffAttendanceId}` | `attendance.staff_attendance.read` | served |  |
| `attendance.UpdateStaffAttendance` | PATCH | `/v1/attendance/staff-attendance-days/{staffAttendanceId}` | `attendance.staff_attendance.update` | served |  |

## careers

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `careers.ListFunctionAreas` | GET | `/v1/careers/function-areas` | `careers.function_area.read` | served |  |
| `careers.CreateFunctionArea` | POST | `/v1/careers/function-areas` | `careers.function_area.create` | served |  |
| `careers.GetFunctionArea` | GET | `/v1/careers/function-areas/{functionAreaId}` | `careers.function_area.read` | served |  |
| `careers.UpdateFunctionArea` | PATCH | `/v1/careers/function-areas/{functionAreaId}` | `careers.function_area.update` | served |  |
| `careers.DeleteFunctionArea` | DELETE | `/v1/careers/function-areas/{functionAreaId}` | `careers.function_area.delete` | served |  |
| `careers.ListIndustries` | GET | `/v1/careers/industries` | `careers.industry.read` | served |  |
| `careers.CreateIndustry` | POST | `/v1/careers/industries` | `careers.industry.create` | served |  |
| `careers.GetIndustry` | GET | `/v1/careers/industries/{industryId}` | `careers.industry.read` | served |  |
| `careers.UpdateIndustry` | PATCH | `/v1/careers/industries/{industryId}` | `careers.industry.update` | served |  |
| `careers.DeleteIndustry` | DELETE | `/v1/careers/industries/{industryId}` | `careers.industry.delete` | served |  |
| `careers.ListJobOpenings` | GET | `/v1/careers/job-openings` | `careers.job_opening.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `careers.CreateJobOpening` | POST | `/v1/careers/job-openings` | `careers.job_opening.create` | served |  |
| `careers.GetJobOpening` | GET | `/v1/careers/job-openings/{jobOpeningId}` | `careers.job_opening.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `careers.UpdateJobOpening` | PATCH | `/v1/careers/job-openings/{jobOpeningId}` | `careers.job_opening.update` | served |  |
| `careers.DeleteJobOpening` | DELETE | `/v1/careers/job-openings/{jobOpeningId}` | `careers.job_opening.delete` | served |  |
| `careers.ListJobApplications` | GET | `/v1/careers/job-openings/{jobOpeningId}/applications` | `careers.job_application.read` | served |  |
| `careers.CreateJobApplication` | POST | `/v1/careers/job-openings/{jobOpeningId}/applications` | `careers.job_application.create` | served |  |
| `careers.GetJobApplication` | GET | `/v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}` | `careers.job_application.read` | served |  |
| `careers.UpdateJobApplication` | PATCH | `/v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}` | `careers.job_application.update` | served |  |
| `careers.DeleteJobApplication` | DELETE | `/v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}` | `careers.job_application.delete` | served |  |
| `careers.ListCandidatePreferences` | GET | `/v1/careers/candidate-preferences` | `careers.candidate_preference.read` | served |  |
| `careers.CreateCandidatePreference` | POST | `/v1/careers/candidate-preferences` | `careers.candidate_preference.create` | served |  |
| `careers.GetCandidatePreference` | GET | `/v1/careers/candidate-preferences/{candidatePreferenceId}` | `careers.candidate_preference.read` | served |  |
| `careers.UpdateCandidatePreference` | PATCH | `/v1/careers/candidate-preferences/{candidatePreferenceId}` | `careers.candidate_preference.update` | served |  |
| `careers.DeleteCandidatePreference` | DELETE | `/v1/careers/candidate-preferences/{candidatePreferenceId}` | `careers.candidate_preference.delete` | served |  |
| `careers.ListCandidatePreferredFunctions` | GET | `/v1/careers/candidate-preferences/{candidatePreferenceId}/functions` | `careers.candidate_preferred_function.read` | served |  |
| `careers.CreateCandidatePreferredFunction` | POST | `/v1/careers/candidate-preferences/{candidatePreferenceId}/functions` | `careers.candidate_preferred_function.create` | served |  |
| `careers.GetCandidatePreferredFunction` | GET | `/v1/careers/candidate-preferences/{candidatePreferenceId}/functions/{candidatePreferredFunctionId}` | `careers.candidate_preferred_function.read` | served |  |
| `careers.UpdateCandidatePreferredFunction` | PATCH | `/v1/careers/candidate-preferences/{candidatePreferenceId}/functions/{candidatePreferredFunctionId}` | `careers.candidate_preferred_function.update` | served |  |
| `careers.DeleteCandidatePreferredFunction` | DELETE | `/v1/careers/candidate-preferences/{candidatePreferenceId}/functions/{candidatePreferredFunctionId}` | `careers.candidate_preferred_function.delete` | served |  |
| `careers.ListCandidatePreferredIndustries` | GET | `/v1/careers/candidate-preferences/{candidatePreferenceId}/industries` | `careers.candidate_preferred_industry.read` | served |  |
| `careers.CreateCandidatePreferredIndustry` | POST | `/v1/careers/candidate-preferences/{candidatePreferenceId}/industries` | `careers.candidate_preferred_industry.create` | served |  |
| `careers.GetCandidatePreferredIndustry` | GET | `/v1/careers/candidate-preferences/{candidatePreferenceId}/industries/{candidatePreferredIndustryId}` | `careers.candidate_preferred_industry.read` | served |  |
| `careers.UpdateCandidatePreferredIndustry` | PATCH | `/v1/careers/candidate-preferences/{candidatePreferenceId}/industries/{candidatePreferredIndustryId}` | `careers.candidate_preferred_industry.update` | served |  |
| `careers.DeleteCandidatePreferredIndustry` | DELETE | `/v1/careers/candidate-preferences/{candidatePreferenceId}/industries/{candidatePreferredIndustryId}` | `careers.candidate_preferred_industry.delete` | served |  |
| `careers.CloseJobOpening` | POST | `/v1/careers/job-openings/{jobOpeningId}:close` | `careers.job_opening.close_job_opening` | served |  |
| `careers.ReopenJobOpening` | POST | `/v1/careers/job-openings/{jobOpeningId}:reopen` | `careers.job_opening.reopen_job_opening` | served |  |
| `careers.ReportJobOpening` | POST | `/v1/careers/job-openings/{jobOpeningId}:report` | `careers.job_opening.report_job_opening` | 501 | workflow / custom operation |
| `careers.EmailApplicant` | POST | `/v1/careers/job-applications/{applicationId}:email` | `careers.job_application.email_applicant` | 501 | workflow / custom operation |
| `careers.UpdateApplicationStatus` | POST | `/v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}:setStatus` | `careers.job_application.update_application_status` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |

## comms

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `comms.ListMessageTemplates` | GET | `/v1/comms/message-templates` | `comms.message_template.read` | served |  |
| `comms.CreateMessageTemplate` | POST | `/v1/comms/message-templates` | `comms.message_template.create` | served |  |
| `comms.GetMessageTemplate` | GET | `/v1/comms/message-templates/{messageTemplateId}` | `comms.message_template.read` | served |  |
| `comms.UpdateMessageTemplate` | PATCH | `/v1/comms/message-templates/{messageTemplateId}` | `comms.message_template.update` | served |  |
| `comms.DeleteMessageTemplate` | DELETE | `/v1/comms/message-templates/{messageTemplateId}` | `comms.message_template.delete` | served |  |
| `comms.ListOutboundMessages` | GET | `/v1/comms/outbound-messages` | `comms.outbound_message.read` | served |  |
| `comms.GetOutboundMessage` | GET | `/v1/comms/outbound-messages/{outboundMessageId}` | `comms.outbound_message.read` | served |  |
| `comms.ListMailboxMessages` | GET | `/v1/comms/mailbox-messages` | `comms.mailbox_message.read` | served |  |
| `comms.GetMailboxMessage` | GET | `/v1/comms/mailbox-messages/{mailboxMessageId}` | `comms.mailbox_message.read` | served |  |
| `comms.DeleteMailboxMessage` | DELETE | `/v1/comms/mailbox-messages/{mailboxMessageId}` | `comms.mailbox_message.delete` | served |  |
| `comms.ListInAppNotifications` | GET | `/v1/comms/notifications` | `comms.in_app_notification.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `comms.GetInAppNotification` | GET | `/v1/comms/notifications/{inAppNotificationId}` | `comms.in_app_notification.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `comms.ListNoticeBackgrounds` | GET | `/v1/comms/notice-backgrounds` | `comms.notice_background.read` | served |  |
| `comms.CreateNoticeBackground` | POST | `/v1/comms/notice-backgrounds` | `comms.notice_background.create` | served |  |
| `comms.GetNoticeBackground` | GET | `/v1/comms/notice-backgrounds/{noticeBackgroundId}` | `comms.notice_background.read` | served |  |
| `comms.UpdateNoticeBackground` | PATCH | `/v1/comms/notice-backgrounds/{noticeBackgroundId}` | `comms.notice_background.update` | served |  |
| `comms.DeleteNoticeBackground` | DELETE | `/v1/comms/notice-backgrounds/{noticeBackgroundId}` | `comms.notice_background.delete` | served |  |
| `comms.ListNotices` | GET | `/v1/comms/notices` | `comms.notice.read` | served |  |
| `comms.CreateNotice` | POST | `/v1/comms/notices` | `comms.notice.create` | served |  |
| `comms.GetNotice` | GET | `/v1/comms/notices/{noticeId}` | `comms.notice.read` | served |  |
| `comms.UpdateNotice` | PATCH | `/v1/comms/notices/{noticeId}` | `comms.notice.update` | served |  |
| `comms.DeleteNotice` | DELETE | `/v1/comms/notices/{noticeId}` | `comms.notice.delete` | served |  |
| `comms.ListEvents` | GET | `/v1/comms/events` | `comms.event.read` | served |  |
| `comms.CreateEvent` | POST | `/v1/comms/events` | `comms.event.create` | served |  |
| `comms.GetEvent` | GET | `/v1/comms/events/{eventId}` | `comms.event.read` | served |  |
| `comms.UpdateEvent` | PATCH | `/v1/comms/events/{eventId}` | `comms.event.update` | served |  |
| `comms.DeleteEvent` | DELETE | `/v1/comms/events/{eventId}` | `comms.event.delete` | served |  |
| `comms.ListEventGalleryItems` | GET | `/v1/comms/events/{eventId}/gallery-items` | `comms.event_gallery_item.read` | served |  |
| `comms.CreateEventGalleryItem` | POST | `/v1/comms/events/{eventId}/gallery-items` | `comms.event_gallery_item.create` | served |  |
| `comms.GetEventGalleryItem` | GET | `/v1/comms/events/{eventId}/gallery-items/{eventGalleryItemId}` | `comms.event_gallery_item.read` | served |  |
| `comms.UpdateEventGalleryItem` | PATCH | `/v1/comms/events/{eventId}/gallery-items/{eventGalleryItemId}` | `comms.event_gallery_item.update` | served |  |
| `comms.DeleteEventGalleryItem` | DELETE | `/v1/comms/events/{eventId}/gallery-items/{eventGalleryItemId}` | `comms.event_gallery_item.delete` | served |  |
| `comms.ListMagazines` | GET | `/v1/comms/magazines` | `comms.magazine.read` | served |  |
| `comms.CreateMagazine` | POST | `/v1/comms/magazines` | `comms.magazine.create` | served |  |
| `comms.GetMagazine` | GET | `/v1/comms/magazines/{magazineId}` | `comms.magazine.read` | served |  |
| `comms.UpdateMagazine` | PATCH | `/v1/comms/magazines/{magazineId}` | `comms.magazine.update` | served |  |
| `comms.DeleteMagazine` | DELETE | `/v1/comms/magazines/{magazineId}` | `comms.magazine.delete` | served |  |
| `comms.ListConversations` | GET | `/v1/comms/conversations` | `comms.conversation.read` | served |  |
| `comms.CreateConversation` | POST | `/v1/comms/conversations` | `comms.conversation.create` | served |  |
| `comms.GetConversation` | GET | `/v1/comms/conversations/{conversationId}` | `comms.conversation.read` | served |  |
| `comms.UpdateConversation` | PATCH | `/v1/comms/conversations/{conversationId}` | `comms.conversation.update` | served |  |
| `comms.DeleteConversation` | DELETE | `/v1/comms/conversations/{conversationId}` | `comms.conversation.delete` | served |  |
| `comms.ListConversationParticipants` | GET | `/v1/comms/conversations/{conversationId}/participants` | `comms.conversation_participant.read` | served |  |
| `comms.CreateConversationParticipant` | POST | `/v1/comms/conversations/{conversationId}/participants` | `comms.conversation_participant.create` | served |  |
| `comms.GetConversationParticipant` | GET | `/v1/comms/conversations/{conversationId}/participants/{conversationParticipantId}` | `comms.conversation_participant.read` | served |  |
| `comms.UpdateConversationParticipant` | PATCH | `/v1/comms/conversations/{conversationId}/participants/{conversationParticipantId}` | `comms.conversation_participant.update` | served |  |
| `comms.DeleteConversationParticipant` | DELETE | `/v1/comms/conversations/{conversationId}/participants/{conversationParticipantId}` | `comms.conversation_participant.delete` | served |  |
| `comms.ListConversationMessages` | GET | `/v1/comms/conversations/{conversationId}/messages` | `comms.conversation_message.read` | served |  |
| `comms.CreateConversationMessage` | POST | `/v1/comms/conversations/{conversationId}/messages` | `comms.conversation_message.create` | served |  |
| `comms.GetConversationMessage` | GET | `/v1/comms/conversations/{conversationId}/messages/{conversationMessageId}` | `comms.conversation_message.read` | served |  |
| `comms.UpdateConversationMessage` | PATCH | `/v1/comms/conversations/{conversationId}/messages/{conversationMessageId}` | `comms.conversation_message.update` | served |  |
| `comms.DeleteConversationMessage` | DELETE | `/v1/comms/conversations/{conversationId}/messages/{conversationMessageId}` | `comms.conversation_message.delete` | served |  |
| `comms.ListFeedbackThreads` | GET | `/v1/comms/feedback-threads` | `comms.feedback_thread.read` | served |  |
| `comms.CreateFeedbackThread` | POST | `/v1/comms/feedback-threads` | `comms.feedback_thread.create` | served |  |
| `comms.GetFeedbackThread` | GET | `/v1/comms/feedback-threads/{feedbackThreadId}` | `comms.feedback_thread.read` | served |  |
| `comms.UpdateFeedbackThread` | PATCH | `/v1/comms/feedback-threads/{feedbackThreadId}` | `comms.feedback_thread.update` | served |  |
| `comms.DeleteFeedbackThread` | DELETE | `/v1/comms/feedback-threads/{feedbackThreadId}` | `comms.feedback_thread.delete` | served |  |
| `comms.ListFeedbackMessages` | GET | `/v1/comms/feedback-threads/{feedbackThreadId}/messages` | `comms.feedback_message.read` | served |  |
| `comms.CreateFeedbackMessage` | POST | `/v1/comms/feedback-threads/{feedbackThreadId}/messages` | `comms.feedback_message.create` | served |  |
| `comms.GetFeedbackMessage` | GET | `/v1/comms/feedback-threads/{feedbackThreadId}/messages/{feedbackMessageId}` | `comms.feedback_message.read` | served |  |
| `comms.UpdateFeedbackMessage` | PATCH | `/v1/comms/feedback-threads/{feedbackThreadId}/messages/{feedbackMessageId}` | `comms.feedback_message.update` | served |  |
| `comms.DeleteFeedbackMessage` | DELETE | `/v1/comms/feedback-threads/{feedbackThreadId}/messages/{feedbackMessageId}` | `comms.feedback_message.delete` | served |  |
| `comms.ListAudienceGroups` | GET | `/v1/comms/audience-groups` | `comms.audience_group.read` | served |  |
| `comms.CreateAudienceGroup` | POST | `/v1/comms/audience-groups` | `comms.audience_group.create` | served |  |
| `comms.GetAudienceGroup` | GET | `/v1/comms/audience-groups/{audienceGroupId}` | `comms.audience_group.read` | served |  |
| `comms.UpdateAudienceGroup` | PATCH | `/v1/comms/audience-groups/{audienceGroupId}` | `comms.audience_group.update` | served |  |
| `comms.DeleteAudienceGroup` | DELETE | `/v1/comms/audience-groups/{audienceGroupId}` | `comms.audience_group.delete` | served |  |
| `comms.ListAudienceGroupMembers` | GET | `/v1/comms/audience-groups/{audienceGroupId}/members` | `comms.audience_group_member.read` | served |  |
| `comms.CreateAudienceGroupMember` | POST | `/v1/comms/audience-groups/{audienceGroupId}/members` | `comms.audience_group_member.create` | served |  |
| `comms.GetAudienceGroupMember` | GET | `/v1/comms/audience-groups/{audienceGroupId}/members/{audienceGroupMemberId}` | `comms.audience_group_member.read` | served |  |
| `comms.UpdateAudienceGroupMember` | PATCH | `/v1/comms/audience-groups/{audienceGroupId}/members/{audienceGroupMemberId}` | `comms.audience_group_member.update` | served |  |
| `comms.DeleteAudienceGroupMember` | DELETE | `/v1/comms/audience-groups/{audienceGroupId}/members/{audienceGroupMemberId}` | `comms.audience_group_member.delete` | served |  |
| `comms.ListTasks` | GET | `/v1/comms/tasks` | `comms.task.read` | served |  |
| `comms.CreateTask` | POST | `/v1/comms/tasks` | `comms.task.create` | served |  |
| `comms.GetTask` | GET | `/v1/comms/tasks/{taskId}` | `comms.task.read` | served |  |
| `comms.UpdateTask` | PATCH | `/v1/comms/tasks/{taskId}` | `comms.task.update` | served |  |
| `comms.DeleteTask` | DELETE | `/v1/comms/tasks/{taskId}` | `comms.task.delete` | served |  |
| `comms.ListTaskAssignees` | GET | `/v1/comms/tasks/{taskId}/assignees` | `comms.task_assignee.read` | served |  |
| `comms.CreateTaskAssignee` | POST | `/v1/comms/tasks/{taskId}/assignees` | `comms.task_assignee.create` | served |  |
| `comms.GetTaskAssignee` | GET | `/v1/comms/tasks/{taskId}/assignees/{taskAssigneeId}` | `comms.task_assignee.read` | served |  |
| `comms.UpdateTaskAssignee` | PATCH | `/v1/comms/tasks/{taskId}/assignees/{taskAssigneeId}` | `comms.task_assignee.update` | served |  |
| `comms.DeleteTaskAssignee` | DELETE | `/v1/comms/tasks/{taskId}/assignees/{taskAssigneeId}` | `comms.task_assignee.delete` | served |  |
| `comms.ListContentViews` | GET | `/v1/comms/content-views` | `comms.content_view.read` | served |  |
| `comms.CreateContentView` | POST | `/v1/comms/content-views` | `comms.content_view.create` | served |  |
| `comms.GetContentView` | GET | `/v1/comms/content-views/{contentViewId}` | `comms.content_view.read` | served |  |
| `comms.UpdateContentView` | PATCH | `/v1/comms/content-views/{contentViewId}` | `comms.content_view.update` | served |  |
| `comms.DeleteContentView` | DELETE | `/v1/comms/content-views/{contentViewId}` | `comms.content_view.delete` | served |  |
| `comms.ListPageCategories` | GET | `/v1/comms/page-categories` | `comms.page_category.read` | served |  |
| `comms.CreatePageCategory` | POST | `/v1/comms/page-categories` | `comms.page_category.create` | served |  |
| `comms.GetPageCategory` | GET | `/v1/comms/page-categories/{pageCategoryId}` | `comms.page_category.read` | served |  |
| `comms.UpdatePageCategory` | PATCH | `/v1/comms/page-categories/{pageCategoryId}` | `comms.page_category.update` | served |  |
| `comms.DeletePageCategory` | DELETE | `/v1/comms/page-categories/{pageCategoryId}` | `comms.page_category.delete` | served |  |
| `comms.ListClassPages` | GET | `/v1/comms/class-pages` | `comms.class_page.read` | served |  |
| `comms.CreateClassPage` | POST | `/v1/comms/class-pages` | `comms.class_page.create` | served |  |
| `comms.GetClassPage` | GET | `/v1/comms/class-pages/{classPageId}` | `comms.class_page.read` | served |  |
| `comms.UpdateClassPage` | PATCH | `/v1/comms/class-pages/{classPageId}` | `comms.class_page.update` | served |  |
| `comms.DeleteClassPage` | DELETE | `/v1/comms/class-pages/{classPageId}` | `comms.class_page.delete` | served |  |
| `comms.ListClassPageAttachments` | GET | `/v1/comms/class-pages/{classPageId}/attachments` | `comms.class_page_attachment.read` | served |  |
| `comms.CreateClassPageAttachment` | POST | `/v1/comms/class-pages/{classPageId}/attachments` | `comms.class_page_attachment.create` | served |  |
| `comms.GetClassPageAttachment` | GET | `/v1/comms/class-pages/{classPageId}/attachments/{classPageAttachmentId}` | `comms.class_page_attachment.read` | served |  |
| `comms.UpdateClassPageAttachment` | PATCH | `/v1/comms/class-pages/{classPageId}/attachments/{classPageAttachmentId}` | `comms.class_page_attachment.update` | served |  |
| `comms.DeleteClassPageAttachment` | DELETE | `/v1/comms/class-pages/{classPageId}/attachments/{classPageAttachmentId}` | `comms.class_page_attachment.delete` | served |  |
| `comms.ListClassPageMembers` | GET | `/v1/comms/class-pages/{classPageId}/members` | `comms.class_page_member.read` | served |  |
| `comms.CreateClassPageMember` | POST | `/v1/comms/class-pages/{classPageId}/members` | `comms.class_page_member.create` | served |  |
| `comms.GetClassPageMember` | GET | `/v1/comms/class-pages/{classPageId}/members/{classPageMemberId}` | `comms.class_page_member.read` | served |  |
| `comms.UpdateClassPageMember` | PATCH | `/v1/comms/class-pages/{classPageId}/members/{classPageMemberId}` | `comms.class_page_member.update` | served |  |
| `comms.DeleteClassPageMember` | DELETE | `/v1/comms/class-pages/{classPageId}/members/{classPageMemberId}` | `comms.class_page_member.delete` | served |  |
| `comms.ListPosts` | GET | `/v1/comms/posts` | `comms.post.read` | served |  |
| `comms.CreatePost` | POST | `/v1/comms/posts` | `comms.post.create` | served |  |
| `comms.GetPost` | GET | `/v1/comms/posts/{postId}` | `comms.post.read` | served |  |
| `comms.UpdatePost` | PATCH | `/v1/comms/posts/{postId}` | `comms.post.update` | served |  |
| `comms.DeletePost` | DELETE | `/v1/comms/posts/{postId}` | `comms.post.delete` | served |  |
| `comms.ListPostAudienceClasses` | GET | `/v1/comms/posts/{postId}/audience-classes` | `comms.post_audience_class.read` | served |  |
| `comms.CreatePostAudienceClass` | POST | `/v1/comms/posts/{postId}/audience-classes` | `comms.post_audience_class.create` | served |  |
| `comms.GetPostAudienceClass` | GET | `/v1/comms/posts/{postId}/audience-classes/{postAudienceClassId}` | `comms.post_audience_class.read` | served |  |
| `comms.UpdatePostAudienceClass` | PATCH | `/v1/comms/posts/{postId}/audience-classes/{postAudienceClassId}` | `comms.post_audience_class.update` | served |  |
| `comms.DeletePostAudienceClass` | DELETE | `/v1/comms/posts/{postId}/audience-classes/{postAudienceClassId}` | `comms.post_audience_class.delete` | served |  |
| `comms.ListPostReactions` | GET | `/v1/comms/posts/{postId}/reactions` | `comms.post_reaction.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `comms.CreatePostReaction` | POST | `/v1/comms/posts/{postId}/reactions` | `comms.post_reaction.create` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `comms.GetPostReaction` | GET | `/v1/comms/posts/{postId}/reactions/{postReactionId}` | `comms.post_reaction.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `comms.UpdatePostReaction` | PATCH | `/v1/comms/posts/{postId}/reactions/{postReactionId}` | `comms.post_reaction.update` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `comms.DeletePostReaction` | DELETE | `/v1/comms/posts/{postId}/reactions/{postReactionId}` | `comms.post_reaction.delete` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `comms.ListPostComments` | GET | `/v1/comms/posts/{postId}/comments` | `comms.post_comment.read` | served |  |
| `comms.CreatePostComment` | POST | `/v1/comms/posts/{postId}/comments` | `comms.post_comment.create` | served |  |
| `comms.GetPostComment` | GET | `/v1/comms/posts/{postId}/comments/{postCommentId}` | `comms.post_comment.read` | served |  |
| `comms.UpdatePostComment` | PATCH | `/v1/comms/posts/{postId}/comments/{postCommentId}` | `comms.post_comment.update` | served |  |
| `comms.DeletePostComment` | DELETE | `/v1/comms/posts/{postId}/comments/{postCommentId}` | `comms.post_comment.delete` | served |  |
| `comms.ListCommentReactions` | GET | `/v1/comms/comment-reactions` | `comms.comment_reaction.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `comms.CreateCommentReaction` | POST | `/v1/comms/comment-reactions` | `comms.comment_reaction.create` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `comms.GetCommentReaction` | GET | `/v1/comms/comment-reactions/{commentReactionId}` | `comms.comment_reaction.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `comms.UpdateCommentReaction` | PATCH | `/v1/comms/comment-reactions/{commentReactionId}` | `comms.comment_reaction.update` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `comms.DeleteCommentReaction` | DELETE | `/v1/comms/comment-reactions/{commentReactionId}` | `comms.comment_reaction.delete` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `comms.ListNoticeAudiences` | GET | `/v1/comms/notices/{noticeId}/audiences` | `comms.notice_audience.read` | served |  |
| `comms.CreateNoticeAudience` | POST | `/v1/comms/notices/{noticeId}/audiences` | `comms.notice_audience.create` | served |  |
| `comms.GetNoticeAudience` | GET | `/v1/comms/notices/{noticeId}/audiences/{noticeAudienceId}` | `comms.notice_audience.read` | served |  |
| `comms.UpdateNoticeAudience` | PATCH | `/v1/comms/notices/{noticeId}/audiences/{noticeAudienceId}` | `comms.notice_audience.update` | served |  |
| `comms.DeleteNoticeAudience` | DELETE | `/v1/comms/notices/{noticeId}/audiences/{noticeAudienceId}` | `comms.notice_audience.delete` | served |  |
| `comms.ListNoticeVersions` | GET | `/v1/comms/notices/{noticeId}/versions` | `comms.notice_version.read` | served |  |
| `comms.GetNoticeVersion` | GET | `/v1/comms/notices/{noticeId}/versions/{noticeVersionId}` | `comms.notice_version.read` | served |  |
| `comms.ListEventInstitutions` | GET | `/v1/comms/events/{eventId}/institutions` | `comms.event_institution.read` | served |  |
| `comms.CreateEventInstitution` | POST | `/v1/comms/events/{eventId}/institutions` | `comms.event_institution.create` | served |  |
| `comms.GetEventInstitution` | GET | `/v1/comms/events/{eventId}/institutions/{eventInstitutionId}` | `comms.event_institution.read` | served |  |
| `comms.UpdateEventInstitution` | PATCH | `/v1/comms/events/{eventId}/institutions/{eventInstitutionId}` | `comms.event_institution.update` | served |  |
| `comms.DeleteEventInstitution` | DELETE | `/v1/comms/events/{eventId}/institutions/{eventInstitutionId}` | `comms.event_institution.delete` | served |  |
| `comms.ListEventOccurrences` | GET | `/v1/comms/events/{eventId}/occurrences` | `comms.event_occurrence.read` | served |  |
| `comms.CreateEventOccurrence` | POST | `/v1/comms/events/{eventId}/occurrences` | `comms.event_occurrence.create` | served |  |
| `comms.GetEventOccurrence` | GET | `/v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}` | `comms.event_occurrence.read` | served |  |
| `comms.UpdateEventOccurrence` | PATCH | `/v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}` | `comms.event_occurrence.update` | served |  |
| `comms.DeleteEventOccurrence` | DELETE | `/v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}` | `comms.event_occurrence.delete` | served |  |
| `comms.ListEventAttendees` | GET | `/v1/comms/events/{eventId}/attendees` | `comms.event_attendee.read` | served |  |
| `comms.CreateEventAttendee` | POST | `/v1/comms/events/{eventId}/attendees` | `comms.event_attendee.create` | served |  |
| `comms.GetEventAttendee` | GET | `/v1/comms/events/{eventId}/attendees/{eventAttendeeId}` | `comms.event_attendee.read` | served |  |
| `comms.UpdateEventAttendee` | PATCH | `/v1/comms/events/{eventId}/attendees/{eventAttendeeId}` | `comms.event_attendee.update` | served |  |
| `comms.DeleteEventAttendee` | DELETE | `/v1/comms/events/{eventId}/attendees/{eventAttendeeId}` | `comms.event_attendee.delete` | served |  |
| `comms.ListCalendarExports` | GET | `/v1/comms/calendar-exports` | `comms.calendar_export.read` | served |  |
| `comms.GetCalendarExport` | GET | `/v1/comms/calendar-exports/{calendarExportId}` | `comms.calendar_export.read` | served |  |
| `comms.DeleteCalendarExport` | DELETE | `/v1/comms/calendar-exports/{calendarExportId}` | `comms.calendar_export.delete` | served |  |
| `comms.ListNotificationTypes` | GET | `/v1/comms/notification-types` | `comms.notification_type.read` | served |  |
| `comms.CreateNotificationType` | POST | `/v1/comms/notification-types` | `comms.notification_type.create` | served |  |
| `comms.GetNotificationType` | GET | `/v1/comms/notification-types/{notificationTypeId}` | `comms.notification_type.read` | served |  |
| `comms.UpdateNotificationType` | PATCH | `/v1/comms/notification-types/{notificationTypeId}` | `comms.notification_type.update` | served |  |
| `comms.DeleteNotificationType` | DELETE | `/v1/comms/notification-types/{notificationTypeId}` | `comms.notification_type.delete` | served |  |
| `comms.ListNotificationTypeProperties` | GET | `/v1/comms/notification-types/{notificationTypeId}/properties` | `comms.notification_type_property.read` | served |  |
| `comms.CreateNotificationTypeProperty` | POST | `/v1/comms/notification-types/{notificationTypeId}/properties` | `comms.notification_type_property.create` | served |  |
| `comms.GetNotificationTypeProperty` | GET | `/v1/comms/notification-types/{notificationTypeId}/properties/{notificationTypePropertyId}` | `comms.notification_type_property.read` | served |  |
| `comms.UpdateNotificationTypeProperty` | PATCH | `/v1/comms/notification-types/{notificationTypeId}/properties/{notificationTypePropertyId}` | `comms.notification_type_property.update` | served |  |
| `comms.DeleteNotificationTypeProperty` | DELETE | `/v1/comms/notification-types/{notificationTypeId}/properties/{notificationTypePropertyId}` | `comms.notification_type_property.delete` | served |  |
| `comms.ListNotificationRules` | GET | `/v1/comms/notification-types/{notificationTypeId}/rules` | `comms.notification_rule.read` | served |  |
| `comms.CreateNotificationRule` | POST | `/v1/comms/notification-types/{notificationTypeId}/rules` | `comms.notification_rule.create` | served |  |
| `comms.GetNotificationRule` | GET | `/v1/comms/notification-types/{notificationTypeId}/rules/{notificationRuleId}` | `comms.notification_rule.read` | served |  |
| `comms.UpdateNotificationRule` | PATCH | `/v1/comms/notification-types/{notificationTypeId}/rules/{notificationRuleId}` | `comms.notification_rule.update` | served |  |
| `comms.DeleteNotificationRule` | DELETE | `/v1/comms/notification-types/{notificationTypeId}/rules/{notificationRuleId}` | `comms.notification_rule.delete` | served |  |
| `comms.ListNotificationCohortSettings` | GET | `/v1/comms/notification-types/{notificationTypeId}/cohort-settings` | `comms.notification_cohort_setting.read` | served |  |
| `comms.CreateNotificationCohortSetting` | POST | `/v1/comms/notification-types/{notificationTypeId}/cohort-settings` | `comms.notification_cohort_setting.create` | served |  |
| `comms.GetNotificationCohortSetting` | GET | `/v1/comms/notification-types/{notificationTypeId}/cohort-settings/{notificationCohortSettingId}` | `comms.notification_cohort_setting.read` | served |  |
| `comms.UpdateNotificationCohortSetting` | PATCH | `/v1/comms/notification-types/{notificationTypeId}/cohort-settings/{notificationCohortSettingId}` | `comms.notification_cohort_setting.update` | served |  |
| `comms.DeleteNotificationCohortSetting` | DELETE | `/v1/comms/notification-types/{notificationTypeId}/cohort-settings/{notificationCohortSettingId}` | `comms.notification_cohort_setting.delete` | served |  |
| `comms.ListNotificationChannels` | GET | `/v1/comms/notification-channels` | `comms.notification_channel.read` | served |  |
| `comms.CreateNotificationChannel` | POST | `/v1/comms/notification-channels` | `comms.notification_channel.create` | served |  |
| `comms.GetNotificationChannel` | GET | `/v1/comms/notification-channels/{notificationChannelId}` | `comms.notification_channel.read` | served |  |
| `comms.UpdateNotificationChannel` | PATCH | `/v1/comms/notification-channels/{notificationChannelId}` | `comms.notification_channel.update` | served |  |
| `comms.DeleteNotificationChannel` | DELETE | `/v1/comms/notification-channels/{notificationChannelId}` | `comms.notification_channel.delete` | served |  |
| `comms.ListNotificationChannelProperties` | GET | `/v1/comms/notification-channels/{notificationChannelId}/properties` | `comms.notification_channel_property.read` | served |  |
| `comms.CreateNotificationChannelProperty` | POST | `/v1/comms/notification-channels/{notificationChannelId}/properties` | `comms.notification_channel_property.create` | served |  |
| `comms.GetNotificationChannelProperty` | GET | `/v1/comms/notification-channels/{notificationChannelId}/properties/{notificationChannelPropertyId}` | `comms.notification_channel_property.read` | served |  |
| `comms.UpdateNotificationChannelProperty` | PATCH | `/v1/comms/notification-channels/{notificationChannelId}/properties/{notificationChannelPropertyId}` | `comms.notification_channel_property.update` | served |  |
| `comms.DeleteNotificationChannelProperty` | DELETE | `/v1/comms/notification-channels/{notificationChannelId}/properties/{notificationChannelPropertyId}` | `comms.notification_channel_property.delete` | served |  |
| `comms.ListNotificationPreferenceSets` | GET | `/v1/comms/notification-preference-sets` | `comms.notification_preference_set.read` | served |  |
| `comms.CreateNotificationPreferenceSet` | POST | `/v1/comms/notification-preference-sets` | `comms.notification_preference_set.create` | served |  |
| `comms.GetNotificationPreferenceSet` | GET | `/v1/comms/notification-preference-sets/{notificationPreferenceSetId}` | `comms.notification_preference_set.read` | served |  |
| `comms.UpdateNotificationPreferenceSet` | PATCH | `/v1/comms/notification-preference-sets/{notificationPreferenceSetId}` | `comms.notification_preference_set.update` | served |  |
| `comms.DeleteNotificationPreferenceSet` | DELETE | `/v1/comms/notification-preference-sets/{notificationPreferenceSetId}` | `comms.notification_preference_set.delete` | served |  |
| `comms.ListNotificationPreferences` | GET | `/v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences` | `comms.notification_preference.read` | served |  |
| `comms.CreateNotificationPreference` | POST | `/v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences` | `comms.notification_preference.create` | served |  |
| `comms.GetNotificationPreference` | GET | `/v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences/{notificationPreferenceId}` | `comms.notification_preference.read` | served |  |
| `comms.UpdateNotificationPreference` | PATCH | `/v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences/{notificationPreferenceId}` | `comms.notification_preference.update` | served |  |
| `comms.DeleteNotificationPreference` | DELETE | `/v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences/{notificationPreferenceId}` | `comms.notification_preference.delete` | served |  |
| `comms.PublishNotice` | POST | `/v1/comms/notices/{noticeId}:publish` | `comms.notice.publish_notice` | served |  |
| `comms.UnpublishNotice` | POST | `/v1/comms/notices/{noticeId}:unpublish` | `comms.notice.unpublish_notice` | served |  |
| `comms.ActivateEvent` | POST | `/v1/comms/events/{eventId}:activate` | `comms.event.activate_event` | served |  |
| `comms.DeactivateEvent` | POST | `/v1/comms/events/{eventId}:deactivate` | `comms.event.deactivate_event` | served |  |
| `comms.SendMessage` | POST | `/v1/comms/outbound-messages:send` | `comms.outbound_message.send_message` | 501 | workflow / custom operation |
| `comms.CancelOutboundMessage` | POST | `/v1/comms/outbound-messages/{outboundMessageId}:cancel` | `comms.outbound_message.cancel_outbound_message` | 501 | workflow / custom operation |
| `comms.RetryOutboundMessage` | POST | `/v1/comms/outbound-messages/{outboundMessageId}:retry` | `comms.outbound_message.retry_outbound_message` | 501 | workflow / custom operation |
| `comms.MarkNotificationRead` | POST | `/v1/comms/notifications/{inAppNotificationId}:markRead` | `comms.in_app_notification.mark_notification_read` | 501 | workflow / custom operation |
| `comms.MarkAllNotificationsRead` | POST | `/v1/comms/notifications:markAllRead` | `comms.in_app_notification.mark_all_notifications_read` | 501 | workflow / custom operation |
| `comms.ComposeMail` | POST | `/v1/comms/mailbox-messages:compose` | `comms.mailbox_message.compose_mail` | 501 | workflow / custom operation |
| `comms.MarkMailRead` | POST | `/v1/comms/mailbox-messages/{mailboxMessageId}:markRead` | `comms.mailbox_message.mark_mail_read` | 501 | workflow / custom operation |
| `comms.MarkConversationRead` | POST | `/v1/comms/conversations/{conversationId}:markRead` | `comms.conversation.mark_conversation_read` | 501 | workflow / custom operation |
| `comms.HideMessageForMe` | POST | `/v1/comms/messages/{messageId}:hideForMe` | `comms.conversation_message.hide_message_for_me` | 501 | workflow / custom operation |
| `comms.CloseFeedbackThread` | POST | `/v1/comms/feedback-threads/{feedbackThreadId}:close` | `comms.feedback_thread.close_feedback_thread` | served |  |
| `comms.ReopenFeedbackThread` | POST | `/v1/comms/feedback-threads/{feedbackThreadId}:reopen` | `comms.feedback_thread.reopen_feedback_thread` | served |  |
| `comms.MarkFeedbackMessage` | POST | `/v1/comms/feedback-messages/{messageId}:markReviewStatus` | `comms.feedback_message.mark_feedback_message` | 501 | workflow / custom operation |
| `comms.AddGroupMembers` | POST | `/v1/comms/audience-groups/{audienceGroupId}:addMembers` | `comms.audience_group.add_group_members` | 501 | workflow / custom operation |
| `comms.CompleteTask` | POST | `/v1/comms/tasks/{taskId}:complete` | `comms.task.complete_task` | served |  |
| `comms.ReopenTask` | POST | `/v1/comms/tasks/{taskId}:reopen` | `comms.task.reopen_task` | served |  |
| `comms.SnoozeTask` | POST | `/v1/comms/tasks/{taskId}:snooze` | `comms.task.snooze_task` | served |  |
| `comms.ClaimTask` | POST | `/v1/comms/tasks/{taskId}:claim` | `comms.task.claim_task` | 501 | workflow / custom operation |
| `comms.SubmitPost` | POST | `/v1/comms/posts/{postId}:submit` | `comms.post.submit_post` | 501 | workflow / custom operation |
| `comms.ApprovePost` | POST | `/v1/comms/posts/{postId}:approve` | `comms.post.approve_post` | served |  |
| `comms.CancelPost` | POST | `/v1/comms/posts/{postId}:cancel` | `comms.post.cancel_post` | served |  |
| `comms.GetFeed` | GET | `/v1/comms/posts:feed` | `comms.post.get_feed` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `comms.FollowClassPage` | POST | `/v1/comms/class-pages/{classPageId}:follow` | `comms.class_page.follow_class_page` | 501 | workflow / custom operation |
| `comms.CloseNoticeReplies` | POST | `/v1/comms/notices/{noticeId}:closeReplies` | `comms.notice.close_notice_replies` | 501 | workflow / custom operation |
| `comms.DeactivateNotice` | POST | `/v1/comms/notices/{noticeId}:deactivate` | `comms.notice.deactivate_notice` | served |  |
| `comms.ReactivateNotice` | POST | `/v1/comms/notices/{noticeId}:reactivate` | `comms.notice.reactivate_notice` | served |  |
| `comms.GetMyCalendar` | GET | `/v1/comms/me/calendar` | `comms.event.get_my_calendar` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `comms.CancelEventOccurrence` | POST | `/v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}:cancel` | `comms.event_occurrence.cancel_event_occurrence` | served |  |
| `comms.CompletePersonalEvent` | POST | `/v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}:complete` | `comms.event_occurrence.complete_personal_event` | 501 | workflow / custom operation |
| `comms.ExportCalendar` | POST | `/v1/comms/me/calendar:export` | `comms.calendar_export.export_calendar` | 501 | workflow / custom operation |
| `comms.SendTestMessage` | POST | `/v1/comms/notification-channels/{notificationChannelId}:sendTest` | `comms.notification_channel.send_test_message` | 501 | workflow / custom operation |
| `comms.ArchiveNotifications` | POST | `/v1/comms/notifications:archive` | `comms.in_app_notification.archive_notifications` | 501 | workflow / custom operation |
| `comms.ListOperations` | GET | `/v1/comms/operations` | `comms.operation.read` | served |  |
| `comms.GetOperation` | GET | `/v1/comms/operations/{operationId}` | `comms.operation.read` | served |  |
| `comms.CancelOperation` | POST | `/v1/comms/operations/{operationId}:cancel` | `comms.operation.cancel_operation` | served |  |

## facility

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `facility.ListFacilities` | GET | `/v1/facility/facilities` | `facility.facility.read` | served |  |
| `facility.CreateFacility` | POST | `/v1/facility/facilities` | `facility.facility.create` | served |  |
| `facility.GetFacility` | GET | `/v1/facility/facilities/{facilityId}` | `facility.facility.read` | served |  |
| `facility.UpdateFacility` | PATCH | `/v1/facility/facilities/{facilityId}` | `facility.facility.update` | served |  |
| `facility.DeleteFacility` | DELETE | `/v1/facility/facilities/{facilityId}` | `facility.facility.delete` | served |  |
| `facility.ListRooms` | GET | `/v1/facility/rooms` | `facility.room.read` | served |  |
| `facility.CreateRoom` | POST | `/v1/facility/rooms` | `facility.room.create` | served |  |
| `facility.GetRoom` | GET | `/v1/facility/rooms/{roomId}` | `facility.room.read` | served |  |
| `facility.UpdateRoom` | PATCH | `/v1/facility/rooms/{roomId}` | `facility.room.update` | served |  |
| `facility.DeleteRoom` | DELETE | `/v1/facility/rooms/{roomId}` | `facility.room.delete` | served |  |
| `facility.ListRoomFacilities` | GET | `/v1/facility/rooms/{roomId}/facilities` | `facility.room_facility.read` | served |  |
| `facility.CreateRoomFacility` | POST | `/v1/facility/rooms/{roomId}/facilities` | `facility.room_facility.create` | served |  |
| `facility.GetRoomFacility` | GET | `/v1/facility/rooms/{roomId}/facilities/{roomFacilityId}` | `facility.room_facility.read` | served |  |
| `facility.UpdateRoomFacility` | PATCH | `/v1/facility/rooms/{roomId}/facilities/{roomFacilityId}` | `facility.room_facility.update` | served |  |
| `facility.DeleteRoomFacility` | DELETE | `/v1/facility/rooms/{roomId}/facilities/{roomFacilityId}` | `facility.room_facility.delete` | served |  |
| `facility.ListAssetItems` | GET | `/v1/facility/asset-items` | `facility.asset_item.read` | served |  |
| `facility.CreateAssetItem` | POST | `/v1/facility/asset-items` | `facility.asset_item.create` | served |  |
| `facility.GetAssetItem` | GET | `/v1/facility/asset-items/{assetItemId}` | `facility.asset_item.read` | served |  |
| `facility.UpdateAssetItem` | PATCH | `/v1/facility/asset-items/{assetItemId}` | `facility.asset_item.update` | served |  |
| `facility.DeleteAssetItem` | DELETE | `/v1/facility/asset-items/{assetItemId}` | `facility.asset_item.delete` | served |  |
| `facility.ListRoomAssets` | GET | `/v1/facility/rooms/{roomId}/assets` | `facility.room_asset.read` | served |  |
| `facility.CreateRoomAsset` | POST | `/v1/facility/rooms/{roomId}/assets` | `facility.room_asset.create` | served |  |
| `facility.GetRoomAsset` | GET | `/v1/facility/rooms/{roomId}/assets/{roomAssetId}` | `facility.room_asset.read` | served |  |
| `facility.UpdateRoomAsset` | PATCH | `/v1/facility/rooms/{roomId}/assets/{roomAssetId}` | `facility.room_asset.update` | served |  |
| `facility.DeleteRoomAsset` | DELETE | `/v1/facility/rooms/{roomId}/assets/{roomAssetId}` | `facility.room_asset.delete` | served |  |

## finance

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `finance.ListFeeHeads` | GET | `/v1/finance/fee-heads` | `finance.fee_head.read` | served |  |
| `finance.CreateFeeHead` | POST | `/v1/finance/fee-heads` | `finance.fee_head.create` | served |  |
| `finance.GetFeeHead` | GET | `/v1/finance/fee-heads/{feeHeadId}` | `finance.fee_head.read` | served |  |
| `finance.UpdateFeeHead` | PATCH | `/v1/finance/fee-heads/{feeHeadId}` | `finance.fee_head.update` | served |  |
| `finance.DeleteFeeHead` | DELETE | `/v1/finance/fee-heads/{feeHeadId}` | `finance.fee_head.delete` | served |  |
| `finance.ListFeePlans` | GET | `/v1/finance/fee-plans` | `finance.fee_plan.read` | served |  |
| `finance.CreateFeePlan` | POST | `/v1/finance/fee-plans` | `finance.fee_plan.create` | served |  |
| `finance.GetFeePlan` | GET | `/v1/finance/fee-plans/{feePlanId}` | `finance.fee_plan.read` | served |  |
| `finance.UpdateFeePlan` | PATCH | `/v1/finance/fee-plans/{feePlanId}` | `finance.fee_plan.update` | served |  |
| `finance.DeleteFeePlan` | DELETE | `/v1/finance/fee-plans/{feePlanId}` | `finance.fee_plan.delete` | served |  |
| `finance.ListFeePlanInstallments` | GET | `/v1/finance/fee-plans/{feePlanId}/installments` | `finance.fee_plan_installment.read` | served |  |
| `finance.CreateFeePlanInstallment` | POST | `/v1/finance/fee-plans/{feePlanId}/installments` | `finance.fee_plan_installment.create` | served |  |
| `finance.GetFeePlanInstallment` | GET | `/v1/finance/fee-plans/{feePlanId}/installments/{feePlanInstallmentId}` | `finance.fee_plan_installment.read` | served |  |
| `finance.UpdateFeePlanInstallment` | PATCH | `/v1/finance/fee-plans/{feePlanId}/installments/{feePlanInstallmentId}` | `finance.fee_plan_installment.update` | served |  |
| `finance.DeleteFeePlanInstallment` | DELETE | `/v1/finance/fee-plans/{feePlanId}/installments/{feePlanInstallmentId}` | `finance.fee_plan_installment.delete` | served |  |
| `finance.ListFeePlanInstallmentComponents` | GET | `/v1/finance/fee-plan-installment-components` | `finance.fee_plan_installment_component.read` | served |  |
| `finance.CreateFeePlanInstallmentComponent` | POST | `/v1/finance/fee-plan-installment-components` | `finance.fee_plan_installment_component.create` | served |  |
| `finance.GetFeePlanInstallmentComponent` | GET | `/v1/finance/fee-plan-installment-components/{feePlanInstallmentComponentId}` | `finance.fee_plan_installment_component.read` | served |  |
| `finance.UpdateFeePlanInstallmentComponent` | PATCH | `/v1/finance/fee-plan-installment-components/{feePlanInstallmentComponentId}` | `finance.fee_plan_installment_component.update` | served |  |
| `finance.DeleteFeePlanInstallmentComponent` | DELETE | `/v1/finance/fee-plan-installment-components/{feePlanInstallmentComponentId}` | `finance.fee_plan_installment_component.delete` | served |  |
| `finance.ListStudentFeeDues` | GET | `/v1/finance/student-fee-dues` | `finance.student_fee_due.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `finance.GetStudentFeeDue` | GET | `/v1/finance/student-fee-dues/{studentFeeDueId}` | `finance.student_fee_due.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `finance.UpdateStudentFeeDue` | PATCH | `/v1/finance/student-fee-dues/{studentFeeDueId}` | `finance.student_fee_due.update` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `finance.ListInvoices` | GET | `/v1/finance/invoices` | `finance.invoice.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `finance.CreateInvoice` | POST | `/v1/finance/invoices` | `finance.invoice.create` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `finance.GetInvoice` | GET | `/v1/finance/invoices/{invoiceId}` | `finance.invoice.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `finance.UpdateInvoice` | PATCH | `/v1/finance/invoices/{invoiceId}` | `finance.invoice.update` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `finance.ListInvoiceLines` | GET | `/v1/finance/invoices/{invoiceId}/lines` | `finance.invoice_line.read` | served |  |
| `finance.GetInvoiceLine` | GET | `/v1/finance/invoices/{invoiceId}/lines/{invoiceLineId}` | `finance.invoice_line.read` | served |  |
| `finance.ListCoupons` | GET | `/v1/finance/coupons` | `finance.coupon.read` | served |  |
| `finance.CreateCoupon` | POST | `/v1/finance/coupons` | `finance.coupon.create` | served |  |
| `finance.GetCoupon` | GET | `/v1/finance/coupons/{couponId}` | `finance.coupon.read` | served |  |
| `finance.UpdateCoupon` | PATCH | `/v1/finance/coupons/{couponId}` | `finance.coupon.update` | served |  |
| `finance.DeleteCoupon` | DELETE | `/v1/finance/coupons/{couponId}` | `finance.coupon.delete` | served |  |
| `finance.ListCouponScopes` | GET | `/v1/finance/coupons/{couponId}/scopes` | `finance.coupon_scope.read` | served |  |
| `finance.CreateCouponScope` | POST | `/v1/finance/coupons/{couponId}/scopes` | `finance.coupon_scope.create` | served |  |
| `finance.GetCouponScope` | GET | `/v1/finance/coupons/{couponId}/scopes/{couponScopeId}` | `finance.coupon_scope.read` | served |  |
| `finance.UpdateCouponScope` | PATCH | `/v1/finance/coupons/{couponId}/scopes/{couponScopeId}` | `finance.coupon_scope.update` | served |  |
| `finance.DeleteCouponScope` | DELETE | `/v1/finance/coupons/{couponId}/scopes/{couponScopeId}` | `finance.coupon_scope.delete` | served |  |
| `finance.ListPayments` | GET | `/v1/finance/payments` | `finance.payment.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `finance.GetPayment` | GET | `/v1/finance/payments/{paymentId}` | `finance.payment.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `finance.GenerateFeeDues` | POST | `/v1/finance/student-fee-dues:generate` | `finance.student_fee_due.generate_fee_dues` | 501 | workflow / custom operation |
| `finance.CreateInvoiceFromDues` | POST | `/v1/finance/invoices:fromDues` | `finance.invoice.create_invoice_from_dues` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `finance.PostInvoice` | POST | `/v1/finance/invoices/{invoiceId}:post` | `finance.invoice.post_invoice` | 501 | workflow / custom operation |
| `finance.CancelInvoice` | POST | `/v1/finance/invoices/{invoiceId}:cancel` | `finance.invoice.cancel_invoice` | 501 | workflow / custom operation |
| `finance.SendFeeReminders` | POST | `/v1/finance/invoices:sendReminders` | `finance.invoice.send_fee_reminders` | 501 | workflow / custom operation |
| `finance.GetFeeStatement` | GET | `/v1/finance/students/{studentId}/fee-statement` | `finance.invoice.get_fee_statement` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `finance.QuoteCheckout` | POST | `/v1/finance/checkout:quote` | `finance.payment.quote_checkout` | 501 | workflow / custom operation |
| `finance.StartCheckout` | POST | `/v1/finance/checkout:start` | `finance.payment.start_checkout` | 501 | workflow / custom operation |
| `finance.ReceiveGatewayWebhook` | POST | `/v1/finance/payment-gateways/{gateway}:webhook` | `finance.gateway_event.receive_webhook` | 501 | workflow / custom operation |
| `finance.RefundPayment` | POST | `/v1/finance/payments/{paymentId}:refund` | `finance.payment.refund_payment` | 501 | workflow / custom operation |
| `finance.ListAcquisitionSources` | GET | `/v1/finance/acquisition-sources` | `finance.acquisition_source.read` | served |  |
| `finance.CreateAcquisitionSource` | POST | `/v1/finance/acquisition-sources` | `finance.acquisition_source.create` | served |  |
| `finance.GetAcquisitionSource` | GET | `/v1/finance/acquisition-sources/{acquisitionSourceId}` | `finance.acquisition_source.read` | served |  |
| `finance.UpdateAcquisitionSource` | PATCH | `/v1/finance/acquisition-sources/{acquisitionSourceId}` | `finance.acquisition_source.update` | served |  |
| `finance.DeleteAcquisitionSource` | DELETE | `/v1/finance/acquisition-sources/{acquisitionSourceId}` | `finance.acquisition_source.delete` | served |  |
| `finance.ListFeeStructures` | GET | `/v1/finance/fee-structures` | `finance.fee_structure.read` | served |  |
| `finance.CreateFeeStructure` | POST | `/v1/finance/fee-structures` | `finance.fee_structure.create` | served |  |
| `finance.GetFeeStructure` | GET | `/v1/finance/fee-structures/{feeStructureId}` | `finance.fee_structure.read` | served |  |
| `finance.UpdateFeeStructure` | PATCH | `/v1/finance/fee-structures/{feeStructureId}` | `finance.fee_structure.update` | served |  |
| `finance.DeleteFeeStructure` | DELETE | `/v1/finance/fee-structures/{feeStructureId}` | `finance.fee_structure.delete` | served |  |
| `finance.PublishFeeStructure` | POST | `/v1/finance/fee-structures/{feeStructureId}:publish` | `finance.fee_structure.publish` | 501 | workflow / custom operation |
| `finance.ListFeeConcessions` | GET | `/v1/finance/fee-concessions` | `finance.fee_concession.read` | served |  |
| `finance.CreateFeeConcession` | POST | `/v1/finance/fee-concessions` | `finance.fee_concession.create` | served |  |
| `finance.GetFeeConcession` | GET | `/v1/finance/fee-concessions/{feeConcessionId}` | `finance.fee_concession.read` | served |  |
| `finance.UpdateFeeConcession` | PATCH | `/v1/finance/fee-concessions/{feeConcessionId}` | `finance.fee_concession.update` | served |  |
| `finance.DeleteFeeConcession` | DELETE | `/v1/finance/fee-concessions/{feeConcessionId}` | `finance.fee_concession.delete` | served |  |
| `finance.RecordPayment` | POST | `/v1/finance/payments:record` | `finance.payment.record` | 501 | workflow / custom operation |
| `finance.ListRefunds` | GET | `/v1/finance/refunds` | `finance.refund.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `finance.GetRefund` | GET | `/v1/finance/refunds/{refundId}` | `finance.refund.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `finance.ListPaymentGatewayAccounts` | GET | `/v1/finance/payment-gateway-accounts` | `finance.payment_gateway_account.read` | served |  |
| `finance.CreatePaymentGatewayAccount` | POST | `/v1/finance/payment-gateway-accounts` | `finance.payment_gateway_account.create` | served |  |
| `finance.GetPaymentGatewayAccount` | GET | `/v1/finance/payment-gateway-accounts/{paymentGatewayAccountId}` | `finance.payment_gateway_account.read` | served |  |
| `finance.UpdatePaymentGatewayAccount` | PATCH | `/v1/finance/payment-gateway-accounts/{paymentGatewayAccountId}` | `finance.payment_gateway_account.update` | served |  |
| `finance.DeletePaymentGatewayAccount` | DELETE | `/v1/finance/payment-gateway-accounts/{paymentGatewayAccountId}` | `finance.payment_gateway_account.delete` | served |  |
| `finance.ArchiveFeeStructure` | POST | `/v1/finance/fee-structures/{feeStructureId}:archive` | `finance.fee_structure.archive` | served |  |
| `finance.ListOperations` | GET | `/v1/finance/operations` | `finance.operation.read` | served |  |
| `finance.GetOperation` | GET | `/v1/finance/operations/{operationId}` | `finance.operation.read` | served |  |
| `finance.CancelOperation` | POST | `/v1/finance/operations/{operationId}:cancel` | `finance.operation.cancel_operation` | served |  |

## frontoffice

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `frontoffice.ListVisitorLogs` | GET | `/v1/frontoffice/visitor-logs` | `frontoffice.visitor_log.read` | served |  |
| `frontoffice.CreateVisitorLog` | POST | `/v1/frontoffice/visitor-logs` | `frontoffice.visitor_log.create` | served |  |
| `frontoffice.GetVisitorLog` | GET | `/v1/frontoffice/visitor-logs/{visitorLogId}` | `frontoffice.visitor_log.read` | served |  |
| `frontoffice.UpdateVisitorLog` | PATCH | `/v1/frontoffice/visitor-logs/{visitorLogId}` | `frontoffice.visitor_log.update` | served |  |
| `frontoffice.DeleteVisitorLog` | DELETE | `/v1/frontoffice/visitor-logs/{visitorLogId}` | `frontoffice.visitor_log.delete` | served |  |
| `frontoffice.ListCallLogs` | GET | `/v1/frontoffice/call-logs` | `frontoffice.call_log.read` | served |  |
| `frontoffice.CreateCallLog` | POST | `/v1/frontoffice/call-logs` | `frontoffice.call_log.create` | served |  |
| `frontoffice.GetCallLog` | GET | `/v1/frontoffice/call-logs/{callLogId}` | `frontoffice.call_log.read` | served |  |
| `frontoffice.UpdateCallLog` | PATCH | `/v1/frontoffice/call-logs/{callLogId}` | `frontoffice.call_log.update` | served |  |
| `frontoffice.DeleteCallLog` | DELETE | `/v1/frontoffice/call-logs/{callLogId}` | `frontoffice.call_log.delete` | served |  |
| `frontoffice.ListPostalRecords` | GET | `/v1/frontoffice/postal-records` | `frontoffice.postal_record.read` | served |  |
| `frontoffice.CreatePostalRecord` | POST | `/v1/frontoffice/postal-records` | `frontoffice.postal_record.create` | served |  |
| `frontoffice.GetPostalRecord` | GET | `/v1/frontoffice/postal-records/{postalRecordId}` | `frontoffice.postal_record.read` | served |  |
| `frontoffice.UpdatePostalRecord` | PATCH | `/v1/frontoffice/postal-records/{postalRecordId}` | `frontoffice.postal_record.update` | served |  |
| `frontoffice.DeletePostalRecord` | DELETE | `/v1/frontoffice/postal-records/{postalRecordId}` | `frontoffice.postal_record.delete` | served |  |
| `frontoffice.ListDirectoryContacts` | GET | `/v1/frontoffice/directory-contacts` | `frontoffice.directory_contact.read` | served |  |
| `frontoffice.CreateDirectoryContact` | POST | `/v1/frontoffice/directory-contacts` | `frontoffice.directory_contact.create` | served |  |
| `frontoffice.GetDirectoryContact` | GET | `/v1/frontoffice/directory-contacts/{directoryContactId}` | `frontoffice.directory_contact.read` | served |  |
| `frontoffice.UpdateDirectoryContact` | PATCH | `/v1/frontoffice/directory-contacts/{directoryContactId}` | `frontoffice.directory_contact.update` | served |  |
| `frontoffice.DeleteDirectoryContact` | DELETE | `/v1/frontoffice/directory-contacts/{directoryContactId}` | `frontoffice.directory_contact.delete` | served |  |
| `frontoffice.CheckOutVisitor` | POST | `/v1/frontoffice/visitor-logs/{visitorLogId}:checkOut` | `frontoffice.visitor_log.check_out_visitor` | 501 | workflow / custom operation |

## hr

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `hr.ListDesignations` | GET | `/v1/hr/designations` | `hr.designation.read` | served |  |
| `hr.CreateDesignation` | POST | `/v1/hr/designations` | `hr.designation.create` | served |  |
| `hr.GetDesignation` | GET | `/v1/hr/designations/{designationId}` | `hr.designation.read` | served |  |
| `hr.UpdateDesignation` | PATCH | `/v1/hr/designations/{designationId}` | `hr.designation.update` | served |  |
| `hr.DeleteDesignation` | DELETE | `/v1/hr/designations/{designationId}` | `hr.designation.delete` | served |  |
| `hr.ListStaff` | GET | `/v1/hr/staff` | `hr.staff.read` | served |  |
| `hr.CreateStaff` | POST | `/v1/hr/staff` | `hr.staff.create` | served |  |
| `hr.GetStaff` | GET | `/v1/hr/staff/{staffId}` | `hr.staff.read` | served |  |
| `hr.UpdateStaff` | PATCH | `/v1/hr/staff/{staffId}` | `hr.staff.update` | served |  |
| `hr.DeleteStaff` | DELETE | `/v1/hr/staff/{staffId}` | `hr.staff.delete` | served |  |
| `hr.ListStaffDepartments` | GET | `/v1/hr/staff/{staffId}/departments` | `hr.staff_department.read` | served |  |
| `hr.CreateStaffDepartment` | POST | `/v1/hr/staff/{staffId}/departments` | `hr.staff_department.create` | served |  |
| `hr.GetStaffDepartment` | GET | `/v1/hr/staff/{staffId}/departments/{staffDepartmentId}` | `hr.staff_department.read` | served |  |
| `hr.UpdateStaffDepartment` | PATCH | `/v1/hr/staff/{staffId}/departments/{staffDepartmentId}` | `hr.staff_department.update` | served |  |
| `hr.DeleteStaffDepartment` | DELETE | `/v1/hr/staff/{staffId}/departments/{staffDepartmentId}` | `hr.staff_department.delete` | served |  |
| `hr.ListStaffSubjects` | GET | `/v1/hr/staff/{staffId}/subjects` | `hr.staff_subject.read` | served |  |
| `hr.CreateStaffSubject` | POST | `/v1/hr/staff/{staffId}/subjects` | `hr.staff_subject.create` | served |  |
| `hr.GetStaffSubject` | GET | `/v1/hr/staff/{staffId}/subjects/{staffSubjectId}` | `hr.staff_subject.read` | served |  |
| `hr.UpdateStaffSubject` | PATCH | `/v1/hr/staff/{staffId}/subjects/{staffSubjectId}` | `hr.staff_subject.update` | served |  |
| `hr.DeleteStaffSubject` | DELETE | `/v1/hr/staff/{staffId}/subjects/{staffSubjectId}` | `hr.staff_subject.delete` | served |  |
| `hr.ListLeaveTypes` | GET | `/v1/hr/leave-types` | `hr.leave_type.read` | served |  |
| `hr.CreateLeaveType` | POST | `/v1/hr/leave-types` | `hr.leave_type.create` | served |  |
| `hr.GetLeaveType` | GET | `/v1/hr/leave-types/{leaveTypeId}` | `hr.leave_type.read` | served |  |
| `hr.UpdateLeaveType` | PATCH | `/v1/hr/leave-types/{leaveTypeId}` | `hr.leave_type.update` | served |  |
| `hr.DeleteLeaveType` | DELETE | `/v1/hr/leave-types/{leaveTypeId}` | `hr.leave_type.delete` | served |  |
| `hr.ListLeaveApplications` | GET | `/v1/hr/leave-applications` | `hr.leave_application.read` | served |  |
| `hr.CreateLeaveApplication` | POST | `/v1/hr/leave-applications` | `hr.leave_application.create` | served |  |
| `hr.GetLeaveApplication` | GET | `/v1/hr/leave-applications/{leaveApplicationId}` | `hr.leave_application.read` | served |  |
| `hr.UpdateLeaveApplication` | PATCH | `/v1/hr/leave-applications/{leaveApplicationId}` | `hr.leave_application.update` | served |  |
| `hr.DeleteLeaveApplication` | DELETE | `/v1/hr/leave-applications/{leaveApplicationId}` | `hr.leave_application.delete` | served |  |
| `hr.ChangeStaffStatus` | POST | `/v1/hr/staff/{staffId}:changeStatus` | `hr.staff.change_staff_status` | 501 | workflow / custom operation |
| `hr.CreateStaffPortalAccount` | POST | `/v1/hr/staff/{staffId}:createPortalAccount` | `hr.staff.create_staff_portal_account` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `hr.GetStaffLeaveBalance` | GET | `/v1/hr/staff/{staffId}:leaveBalance` | `hr.staff.get_leave_balance` | served |  |
| `hr.ApproveLeave` | POST | `/v1/hr/leave-applications/{leaveApplicationId}:approve` | `hr.leave_application.approve_leave` | served |  |
| `hr.RejectLeave` | POST | `/v1/hr/leave-applications/{leaveApplicationId}:reject` | `hr.leave_application.reject_leave` | served |  |
| `hr.CancelLeave` | POST | `/v1/hr/leave-applications/{leaveApplicationId}:cancel` | `hr.leave_application.cancel_leave` | served |  |
| `hr.ListLeaveBalances` | GET | `/v1/hr/leave-balances` | `hr.leave_balance.read` | served |  |
| `hr.CreateLeaveBalance` | POST | `/v1/hr/leave-balances` | `hr.leave_balance.create` | served |  |
| `hr.GetLeaveBalance` | GET | `/v1/hr/leave-balances/{leaveBalanceId}` | `hr.leave_balance.read` | served |  |
| `hr.UpdateLeaveBalance` | PATCH | `/v1/hr/leave-balances/{leaveBalanceId}` | `hr.leave_balance.update` | served |  |

## iam

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `iam.ListUserAccounts` | GET | `/v1/iam/users` | `iam.user_account.read` | served |  |
| `iam.CreateUserAccount` | POST | `/v1/iam/users` | `iam.user_account.create` | served |  |
| `iam.GetUserAccount` | GET | `/v1/iam/users/{userAccountId}` | `iam.user_account.read` | served |  |
| `iam.UpdateUserAccount` | PATCH | `/v1/iam/users/{userAccountId}` | `iam.user_account.update` | served |  |
| `iam.DeleteUserAccount` | DELETE | `/v1/iam/users/{userAccountId}` | `iam.user_account.delete` | served |  |
| `iam.ListRoles` | GET | `/v1/iam/roles` | `iam.role.read` | served |  |
| `iam.CreateRole` | POST | `/v1/iam/roles` | `iam.role.create` | served |  |
| `iam.GetRole` | GET | `/v1/iam/roles/{roleId}` | `iam.role.read` | served |  |
| `iam.UpdateRole` | PATCH | `/v1/iam/roles/{roleId}` | `iam.role.update` | served |  |
| `iam.DeleteRole` | DELETE | `/v1/iam/roles/{roleId}` | `iam.role.delete` | served |  |
| `iam.ListPermissions` | GET | `/v1/iam/permissions` | `iam.permission.read` | served |  |
| `iam.GetPermission` | GET | `/v1/iam/permissions/{permissionId}` | `iam.permission.read` | served |  |
| `iam.ListRolePermissions` | GET | `/v1/iam/roles/{roleId}/permissions` | `iam.role_permission.read` | served |  |
| `iam.CreateRolePermission` | POST | `/v1/iam/roles/{roleId}/permissions` | `iam.role_permission.create` | served |  |
| `iam.GetRolePermission` | GET | `/v1/iam/roles/{roleId}/permissions/{rolePermissionId}` | `iam.role_permission.read` | served |  |
| `iam.UpdateRolePermission` | PATCH | `/v1/iam/roles/{roleId}/permissions/{rolePermissionId}` | `iam.role_permission.update` | served |  |
| `iam.DeleteRolePermission` | DELETE | `/v1/iam/roles/{roleId}/permissions/{rolePermissionId}` | `iam.role_permission.delete` | served |  |
| `iam.ListUserRoles` | GET | `/v1/iam/users/{userAccountId}/roles` | `iam.user_role.read` | served |  |
| `iam.CreateUserRole` | POST | `/v1/iam/users/{userAccountId}/roles` | `iam.user_role.create` | served |  |
| `iam.GetUserRole` | GET | `/v1/iam/users/{userAccountId}/roles/{userRoleId}` | `iam.user_role.read` | served |  |
| `iam.UpdateUserRole` | PATCH | `/v1/iam/users/{userAccountId}/roles/{userRoleId}` | `iam.user_role.update` | served |  |
| `iam.DeleteUserRole` | DELETE | `/v1/iam/users/{userAccountId}/roles/{userRoleId}` | `iam.user_role.delete` | served |  |
| `iam.ListUserPermissions` | GET | `/v1/iam/users/{userAccountId}/permissions` | `iam.user_permission.read` | served |  |
| `iam.CreateUserPermission` | POST | `/v1/iam/users/{userAccountId}/permissions` | `iam.user_permission.create` | served |  |
| `iam.GetUserPermission` | GET | `/v1/iam/users/{userAccountId}/permissions/{userPermissionId}` | `iam.user_permission.read` | served |  |
| `iam.UpdateUserPermission` | PATCH | `/v1/iam/users/{userAccountId}/permissions/{userPermissionId}` | `iam.user_permission.update` | served |  |
| `iam.DeleteUserPermission` | DELETE | `/v1/iam/users/{userAccountId}/permissions/{userPermissionId}` | `iam.user_permission.delete` | served |  |
| `iam.ListUserDepartmentAccesses` | GET | `/v1/iam/users/{userAccountId}/departments` | `iam.user_department_access.read` | served |  |
| `iam.CreateUserDepartmentAccess` | POST | `/v1/iam/users/{userAccountId}/departments` | `iam.user_department_access.create` | served |  |
| `iam.GetUserDepartmentAccess` | GET | `/v1/iam/users/{userAccountId}/departments/{userDepartmentAccessId}` | `iam.user_department_access.read` | served |  |
| `iam.UpdateUserDepartmentAccess` | PATCH | `/v1/iam/users/{userAccountId}/departments/{userDepartmentAccessId}` | `iam.user_department_access.update` | served |  |
| `iam.DeleteUserDepartmentAccess` | DELETE | `/v1/iam/users/{userAccountId}/departments/{userDepartmentAccessId}` | `iam.user_department_access.delete` | served |  |
| `iam.ListAuthTokens` | GET | `/v1/iam/users/{userAccountId}/api-tokens` | `iam.auth_token.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `iam.DeleteAuthToken` | DELETE | `/v1/iam/users/{userAccountId}/api-tokens/{authTokenId}` | `iam.auth_token.delete` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `iam.ListUserDevices` | GET | `/v1/iam/users/{userAccountId}/devices` | `iam.user_device.read` | served |  |
| `iam.CreateUserDevice` | POST | `/v1/iam/users/{userAccountId}/devices` | `iam.user_device.create` | served |  |
| `iam.GetUserDevice` | GET | `/v1/iam/users/{userAccountId}/devices/{userDeviceId}` | `iam.user_device.read` | served |  |
| `iam.UpdateUserDevice` | PATCH | `/v1/iam/users/{userAccountId}/devices/{userDeviceId}` | `iam.user_device.update` | served |  |
| `iam.DeleteUserDevice` | DELETE | `/v1/iam/users/{userAccountId}/devices/{userDeviceId}` | `iam.user_device.delete` | served |  |
| `iam.ListUserPreferences` | GET | `/v1/iam/users/{userAccountId}/preferences` | `iam.user_preference.read` | served |  |
| `iam.CreateUserPreference` | POST | `/v1/iam/users/{userAccountId}/preferences` | `iam.user_preference.create` | served |  |
| `iam.GetUserPreference` | GET | `/v1/iam/users/{userAccountId}/preferences/{userPreferenceId}` | `iam.user_preference.read` | served |  |
| `iam.UpdateUserPreference` | PATCH | `/v1/iam/users/{userAccountId}/preferences/{userPreferenceId}` | `iam.user_preference.update` | served |  |
| `iam.DeleteUserPreference` | DELETE | `/v1/iam/users/{userAccountId}/preferences/{userPreferenceId}` | `iam.user_preference.delete` | served |  |
| `iam.ListUserSessions` | GET | `/v1/iam/users/{userAccountId}/sessions` | `iam.user_session.read` | served |  |
| `iam.GetUserSession` | GET | `/v1/iam/users/{userAccountId}/sessions/{userSessionId}` | `iam.user_session.read` | served |  |
| `iam.DeleteUserSession` | DELETE | `/v1/iam/users/{userAccountId}/sessions/{userSessionId}` | `iam.user_session.delete` | served |  |
| `iam.Login` | POST | `/v1/iam/auth:login` | `iam.user_account.login` | served |  |
| `iam.RefreshToken` | POST | `/v1/iam/auth:refresh` | `iam.user_account.refresh_token` | served |  |
| `iam.Logout` | POST | `/v1/iam/auth:logout` | `iam.user_account.logout` | served |  |
| `iam.SignUp` | POST | `/v1/iam/auth:signUp` | `iam.user_account.sign_up` | 501 | workflow / custom operation |
| `iam.RequestPasswordReset` | POST | `/v1/iam/auth:requestPasswordReset` | `iam.user_account.request_password_reset` | 501 | workflow / custom operation |
| `iam.ResetPassword` | POST | `/v1/iam/auth:resetPassword` | `iam.user_account.reset_password` | 501 | workflow / custom operation |
| `iam.ChangePassword` | POST | `/v1/iam/auth:changePassword` | `iam.user_account.change_password` | served |  |
| `iam.RequestVerification` | POST | `/v1/iam/auth:requestVerification` | `iam.user_account.request_verification` | 501 | workflow / custom operation |
| `iam.ConfirmVerification` | POST | `/v1/iam/auth:confirmVerification` | `iam.user_account.confirm_verification` | 501 | workflow / custom operation |
| `iam.GetMe` | GET | `/v1/iam/me` | `iam.user_account.get_me` | served |  |
| `iam.StartImpersonation` | POST | `/v1/iam/users/{userAccountId}:impersonate` | `iam.user_account.start_impersonation` | 501 | workflow / custom operation |
| `iam.StopImpersonation` | POST | `/v1/iam/auth:stopImpersonation` | `iam.user_account.stop_impersonation` | 501 | workflow / custom operation |
| `iam.ActivateUser` | POST | `/v1/iam/users/{userAccountId}:activate` | `iam.user_account.activate_user` | served |  |
| `iam.DeactivateUser` | POST | `/v1/iam/users/{userAccountId}:deactivate` | `iam.user_account.deactivate_user` | served |  |
| `iam.UnlockUser` | POST | `/v1/iam/users/{userAccountId}:unlock` | `iam.user_account.unlock_user` | served |  |
| `iam.ForcePasswordReset` | POST | `/v1/iam/users/{userAccountId}:forcePasswordReset` | `iam.user_account.force_password_reset` | 501 | workflow / custom operation |
| `iam.CreateApiToken` | POST | `/v1/iam/users/{userAccountId}:createApiToken` | `iam.user_account.create_api_token` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `iam.SetUserCredentials` | POST | `/v1/iam/users/{userAccountId}:setCredentials` | `iam.user_account.set_user_credentials` | served |  |
| `iam.SetUserRoles` | POST | `/v1/iam/users/{userAccountId}:setRoles` | `iam.user_account.set_user_roles` | 501 | workflow / custom operation |
| `iam.SwitchRole` | POST | `/v1/iam/me:switchRole` | `iam.user_account.switch_role` | 501 | workflow / custom operation |
| `iam.AcceptTerms` | POST | `/v1/iam/me:acceptTerms` | `iam.user_account.accept_terms` | served |  |
| `iam.CheckUsernameAvailable` | GET | `/v1/iam/users:checkUsername` | `iam.user_account.check_username_available` | 501 | workflow / custom operation |
| `iam.ImportUsers` | POST | `/v1/iam/users:import` | `iam.user_account.import_users` | 501 | workflow / custom operation |
| `iam.EnrollMfa` | POST | `/v1/iam/mfa:enroll` | `iam.mfa_credential.enroll` | served |  |
| `iam.CompleteMfa` | POST | `/v1/iam/mfa:complete` | `iam.user_account.complete_mfa` | served |  |
| `iam.DisableMfa` | POST | `/v1/iam/mfa:disable` | `iam.mfa_credential.disable` | served |  |
| `iam.ListMfaCredentials` | GET | `/v1/iam/mfa` | `iam.mfa_credential.read` | served |  |
| `iam.RegenerateRecoveryCodes` | POST | `/v1/iam/mfa:regenerateRecoveryCodes` | `iam.mfa_credential.regenerate_recovery_codes` | served |  |
| `iam.ListOperations` | GET | `/v1/iam/operations` | `iam.operation.read` | served |  |
| `iam.GetOperation` | GET | `/v1/iam/operations/{operationId}` | `iam.operation.read` | served |  |
| `iam.CancelOperation` | POST | `/v1/iam/operations/{operationId}:cancel` | `iam.operation.cancel_operation` | served |  |
| `iam.ListSettingDefinitions` | GET | `/v1/iam/setting-definitions` | `iam.setting_definition.read` | served |  |
| `iam.GetSettingDefinition` | GET | `/v1/iam/setting-definitions/{settingDefinitionId}` | `iam.setting_definition.read` | served |  |
| `iam.ListSettingValues` | GET | `/v1/iam/setting-values` | `iam.setting_value.read` | served |  |
| `iam.CreateSettingValue` | POST | `/v1/iam/setting-values` | `iam.setting_value.create` | served |  |
| `iam.GetSettingValue` | GET | `/v1/iam/setting-values/{settingValueId}` | `iam.setting_value.read` | served |  |
| `iam.UpdateSettingValue` | PATCH | `/v1/iam/setting-values/{settingValueId}` | `iam.setting_value.update` | served |  |
| `iam.DeleteSettingValue` | DELETE | `/v1/iam/setting-values/{settingValueId}` | `iam.setting_value.delete` | served |  |
| `iam.ListMySettings` | GET | `/v1/iam/me/settings` | `iam.setting.read_mine` | served |  |
| `iam.SetMySetting` | PUT | `/v1/iam/me/settings/{settingKey}` | `iam.setting.update_mine` | served |  |
| `iam.ResetMySetting` | DELETE | `/v1/iam/me/settings/{settingKey}` | `iam.setting.update_mine` | served |  |

## library

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `library.ListMediaTypes` | GET | `/v1/library/media-types` | `library.media_type.read` | served |  |
| `library.CreateMediaType` | POST | `/v1/library/media-types` | `library.media_type.create` | served |  |
| `library.GetMediaType` | GET | `/v1/library/media-types/{mediaTypeId}` | `library.media_type.read` | served |  |
| `library.UpdateMediaType` | PATCH | `/v1/library/media-types/{mediaTypeId}` | `library.media_type.update` | served |  |
| `library.DeleteMediaType` | DELETE | `/v1/library/media-types/{mediaTypeId}` | `library.media_type.delete` | served |  |
| `library.ListMediaCategories` | GET | `/v1/library/media-categories` | `library.media_category.read` | served |  |
| `library.CreateMediaCategory` | POST | `/v1/library/media-categories` | `library.media_category.create` | served |  |
| `library.GetMediaCategory` | GET | `/v1/library/media-categories/{mediaCategoryId}` | `library.media_category.read` | served |  |
| `library.UpdateMediaCategory` | PATCH | `/v1/library/media-categories/{mediaCategoryId}` | `library.media_category.update` | served |  |
| `library.DeleteMediaCategory` | DELETE | `/v1/library/media-categories/{mediaCategoryId}` | `library.media_category.delete` | served |  |
| `library.ListAuthors` | GET | `/v1/library/authors` | `library.author.read` | served |  |
| `library.CreateAuthor` | POST | `/v1/library/authors` | `library.author.create` | served |  |
| `library.GetAuthor` | GET | `/v1/library/authors/{authorId}` | `library.author.read` | served |  |
| `library.UpdateAuthor` | PATCH | `/v1/library/authors/{authorId}` | `library.author.update` | served |  |
| `library.DeleteAuthor` | DELETE | `/v1/library/authors/{authorId}` | `library.author.delete` | served |  |
| `library.ListPublishers` | GET | `/v1/library/publishers` | `library.publisher.read` | served |  |
| `library.CreatePublisher` | POST | `/v1/library/publishers` | `library.publisher.create` | served |  |
| `library.GetPublisher` | GET | `/v1/library/publishers/{publisherId}` | `library.publisher.read` | served |  |
| `library.UpdatePublisher` | PATCH | `/v1/library/publishers/{publisherId}` | `library.publisher.update` | served |  |
| `library.DeletePublisher` | DELETE | `/v1/library/publishers/{publisherId}` | `library.publisher.delete` | served |  |
| `library.ListMedia` | GET | `/v1/library/media` | `library.media.read` | served |  |
| `library.CreateMedia` | POST | `/v1/library/media` | `library.media.create` | served |  |
| `library.GetMedia` | GET | `/v1/library/media/{mediaId}` | `library.media.read` | served |  |
| `library.UpdateMedia` | PATCH | `/v1/library/media/{mediaId}` | `library.media.update` | served |  |
| `library.DeleteMedia` | DELETE | `/v1/library/media/{mediaId}` | `library.media.delete` | served |  |
| `library.ListMediaAuthors` | GET | `/v1/library/media/{mediaId}/authors` | `library.media_author.read` | served |  |
| `library.CreateMediaAuthor` | POST | `/v1/library/media/{mediaId}/authors` | `library.media_author.create` | served |  |
| `library.GetMediaAuthor` | GET | `/v1/library/media/{mediaId}/authors/{mediaAuthorId}` | `library.media_author.read` | served |  |
| `library.UpdateMediaAuthor` | PATCH | `/v1/library/media/{mediaId}/authors/{mediaAuthorId}` | `library.media_author.update` | served |  |
| `library.DeleteMediaAuthor` | DELETE | `/v1/library/media/{mediaId}/authors/{mediaAuthorId}` | `library.media_author.delete` | served |  |
| `library.ListMediaPublishers` | GET | `/v1/library/media/{mediaId}/publishers` | `library.media_publisher.read` | served |  |
| `library.CreateMediaPublisher` | POST | `/v1/library/media/{mediaId}/publishers` | `library.media_publisher.create` | served |  |
| `library.GetMediaPublisher` | GET | `/v1/library/media/{mediaId}/publishers/{mediaPublisherId}` | `library.media_publisher.read` | served |  |
| `library.UpdateMediaPublisher` | PATCH | `/v1/library/media/{mediaId}/publishers/{mediaPublisherId}` | `library.media_publisher.update` | served |  |
| `library.DeleteMediaPublisher` | DELETE | `/v1/library/media/{mediaId}/publishers/{mediaPublisherId}` | `library.media_publisher.delete` | served |  |
| `library.ListMediaCourses` | GET | `/v1/library/media/{mediaId}/courses` | `library.media_course.read` | served |  |
| `library.CreateMediaCourse` | POST | `/v1/library/media/{mediaId}/courses` | `library.media_course.create` | served |  |
| `library.GetMediaCourse` | GET | `/v1/library/media/{mediaId}/courses/{mediaCourseId}` | `library.media_course.read` | served |  |
| `library.UpdateMediaCourse` | PATCH | `/v1/library/media/{mediaId}/courses/{mediaCourseId}` | `library.media_course.update` | served |  |
| `library.DeleteMediaCourse` | DELETE | `/v1/library/media/{mediaId}/courses/{mediaCourseId}` | `library.media_course.delete` | served |  |
| `library.ListMediaSubjects` | GET | `/v1/library/media/{mediaId}/subjects` | `library.media_subject.read` | served |  |
| `library.CreateMediaSubject` | POST | `/v1/library/media/{mediaId}/subjects` | `library.media_subject.create` | served |  |
| `library.GetMediaSubject` | GET | `/v1/library/media/{mediaId}/subjects/{mediaSubjectId}` | `library.media_subject.read` | served |  |
| `library.UpdateMediaSubject` | PATCH | `/v1/library/media/{mediaId}/subjects/{mediaSubjectId}` | `library.media_subject.update` | served |  |
| `library.DeleteMediaSubject` | DELETE | `/v1/library/media/{mediaId}/subjects/{mediaSubjectId}` | `library.media_subject.delete` | served |  |
| `library.ListMediaUnits` | GET | `/v1/library/media/{mediaId}/units` | `library.media_unit.read` | served |  |
| `library.CreateMediaUnit` | POST | `/v1/library/media/{mediaId}/units` | `library.media_unit.create` | served |  |
| `library.GetMediaUnit` | GET | `/v1/library/media/{mediaId}/units/{mediaUnitId}` | `library.media_unit.read` | served |  |
| `library.UpdateMediaUnit` | PATCH | `/v1/library/media/{mediaId}/units/{mediaUnitId}` | `library.media_unit.update` | served |  |
| `library.DeleteMediaUnit` | DELETE | `/v1/library/media/{mediaId}/units/{mediaUnitId}` | `library.media_unit.delete` | served |  |
| `library.ListLibraryCardTypes` | GET | `/v1/library/library-card-types` | `library.library_card_type.read` | served |  |
| `library.CreateLibraryCardType` | POST | `/v1/library/library-card-types` | `library.library_card_type.create` | served |  |
| `library.GetLibraryCardType` | GET | `/v1/library/library-card-types/{libraryCardTypeId}` | `library.library_card_type.read` | served |  |
| `library.UpdateLibraryCardType` | PATCH | `/v1/library/library-card-types/{libraryCardTypeId}` | `library.library_card_type.update` | served |  |
| `library.DeleteLibraryCardType` | DELETE | `/v1/library/library-card-types/{libraryCardTypeId}` | `library.library_card_type.delete` | served |  |
| `library.ListLibraryCards` | GET | `/v1/library/library-cards` | `library.library_card.read` | served |  |
| `library.CreateLibraryCard` | POST | `/v1/library/library-cards` | `library.library_card.create` | served |  |
| `library.GetLibraryCard` | GET | `/v1/library/library-cards/{libraryCardId}` | `library.library_card.read` | served |  |
| `library.UpdateLibraryCard` | PATCH | `/v1/library/library-cards/{libraryCardId}` | `library.library_card.update` | served |  |
| `library.DeleteLibraryCard` | DELETE | `/v1/library/library-cards/{libraryCardId}` | `library.library_card.delete` | served |  |
| `library.ListLoans` | GET | `/v1/library/loans` | `library.loan.read` | served |  |
| `library.CreateLoan` | POST | `/v1/library/loans` | `library.loan.create` | served |  |
| `library.GetLoan` | GET | `/v1/library/loans/{loanId}` | `library.loan.read` | served |  |
| `library.UpdateLoan` | PATCH | `/v1/library/loans/{loanId}` | `library.loan.update` | served |  |
| `library.DeleteLoan` | DELETE | `/v1/library/loans/{loanId}` | `library.loan.delete` | served |  |
| `library.ListReservations` | GET | `/v1/library/reservations` | `library.reservation.read` | served |  |
| `library.CreateReservation` | POST | `/v1/library/reservations` | `library.reservation.create` | served |  |
| `library.GetReservation` | GET | `/v1/library/reservations/{reservationId}` | `library.reservation.read` | served |  |
| `library.UpdateReservation` | PATCH | `/v1/library/reservations/{reservationId}` | `library.reservation.update` | served |  |
| `library.DeleteReservation` | DELETE | `/v1/library/reservations/{reservationId}` | `library.reservation.delete` | served |  |
| `library.ListPurchaseRequests` | GET | `/v1/library/purchase-requests` | `library.purchase_request.read` | served |  |
| `library.CreatePurchaseRequest` | POST | `/v1/library/purchase-requests` | `library.purchase_request.create` | served |  |
| `library.GetPurchaseRequest` | GET | `/v1/library/purchase-requests/{purchaseRequestId}` | `library.purchase_request.read` | served |  |
| `library.UpdatePurchaseRequest` | PATCH | `/v1/library/purchase-requests/{purchaseRequestId}` | `library.purchase_request.update` | served |  |
| `library.DeletePurchaseRequest` | DELETE | `/v1/library/purchase-requests/{purchaseRequestId}` | `library.purchase_request.delete` | served |  |
| `library.ListPurchaseRequestCourses` | GET | `/v1/library/purchase-requests/{purchaseRequestId}/courses` | `library.purchase_request_course.read` | served |  |
| `library.CreatePurchaseRequestCourse` | POST | `/v1/library/purchase-requests/{purchaseRequestId}/courses` | `library.purchase_request_course.create` | served |  |
| `library.GetPurchaseRequestCourse` | GET | `/v1/library/purchase-requests/{purchaseRequestId}/courses/{purchaseRequestCourseId}` | `library.purchase_request_course.read` | served |  |
| `library.UpdatePurchaseRequestCourse` | PATCH | `/v1/library/purchase-requests/{purchaseRequestId}/courses/{purchaseRequestCourseId}` | `library.purchase_request_course.update` | served |  |
| `library.DeletePurchaseRequestCourse` | DELETE | `/v1/library/purchase-requests/{purchaseRequestId}/courses/{purchaseRequestCourseId}` | `library.purchase_request_course.delete` | served |  |
| `library.ListPurchaseRequestSubjects` | GET | `/v1/library/purchase-requests/{purchaseRequestId}/subjects` | `library.purchase_request_subject.read` | served |  |
| `library.CreatePurchaseRequestSubject` | POST | `/v1/library/purchase-requests/{purchaseRequestId}/subjects` | `library.purchase_request_subject.create` | served |  |
| `library.GetPurchaseRequestSubject` | GET | `/v1/library/purchase-requests/{purchaseRequestId}/subjects/{purchaseRequestSubjectId}` | `library.purchase_request_subject.read` | served |  |
| `library.UpdatePurchaseRequestSubject` | PATCH | `/v1/library/purchase-requests/{purchaseRequestId}/subjects/{purchaseRequestSubjectId}` | `library.purchase_request_subject.update` | served |  |
| `library.DeletePurchaseRequestSubject` | DELETE | `/v1/library/purchase-requests/{purchaseRequestId}/subjects/{purchaseRequestSubjectId}` | `library.purchase_request_subject.delete` | served |  |
| `library.RequestLoanReturn` | POST | `/v1/library/loans/{loanId}:requestReturn` | `library.loan.request_loan_return` | 501 | workflow / custom operation |
| `library.ReturnLoan` | POST | `/v1/library/loans/{loanId}:return` | `library.loan.return_loan` | 501 | workflow / custom operation |
| `library.MarkLoanLost` | POST | `/v1/library/loans/{loanId}:markLost` | `library.loan.mark_loan_lost` | 501 | workflow / custom operation |
| `library.InvoiceLoanFine` | POST | `/v1/library/loans/{loanId}:invoiceFine` | `library.loan.invoice_loan_fine` | 501 | workflow / custom operation |
| `library.AcceptReservation` | POST | `/v1/library/reservations/{reservationId}:accept` | `library.reservation.accept_reservation` | served |  |
| `library.RejectReservation` | POST | `/v1/library/reservations/{reservationId}:reject` | `library.reservation.reject_reservation` | served |  |
| `library.RequestReservationAgain` | POST | `/v1/library/reservations/{reservationId}:requestAgain` | `library.reservation.request_reservation_again` | 501 | workflow / custom operation |
| `library.SubmitPurchaseRequest` | POST | `/v1/library/purchase-requests/{purchaseRequestId}:submit` | `library.purchase_request.submit_purchase_request` | served |  |
| `library.AcceptPurchaseRequest` | POST | `/v1/library/purchase-requests/{purchaseRequestId}:accept` | `library.purchase_request.accept_purchase_request` | served |  |
| `library.RejectPurchaseRequest` | POST | `/v1/library/purchase-requests/{purchaseRequestId}:reject` | `library.purchase_request.reject_purchase_request` | served |  |

## lms

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `lms.ListCategories` | GET | `/v1/lms/categories` | `lms.category.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateCategory` | POST | `/v1/lms/categories` | `lms.category.create` | served |  |
| `lms.GetCategory` | GET | `/v1/lms/categories/{categoryId}` | `lms.category.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.UpdateCategory` | PATCH | `/v1/lms/categories/{categoryId}` | `lms.category.update` | served |  |
| `lms.DeleteCategory` | DELETE | `/v1/lms/categories/{categoryId}` | `lms.category.delete` | served |  |
| `lms.ListEvaluators` | GET | `/v1/lms/evaluators` | `lms.evaluator.read` | served |  |
| `lms.CreateEvaluator` | POST | `/v1/lms/evaluators` | `lms.evaluator.create` | served |  |
| `lms.GetEvaluator` | GET | `/v1/lms/evaluators/{evaluatorId}` | `lms.evaluator.read` | served |  |
| `lms.UpdateEvaluator` | PATCH | `/v1/lms/evaluators/{evaluatorId}` | `lms.evaluator.update` | served |  |
| `lms.DeleteEvaluator` | DELETE | `/v1/lms/evaluators/{evaluatorId}` | `lms.evaluator.delete` | served |  |
| `lms.ListEvaluatorAvailabilities` | GET | `/v1/lms/evaluators/{evaluatorId}/availability` | `lms.evaluator_availability.read` | served |  |
| `lms.CreateEvaluatorAvailability` | POST | `/v1/lms/evaluators/{evaluatorId}/availability` | `lms.evaluator_availability.create` | served |  |
| `lms.GetEvaluatorAvailability` | GET | `/v1/lms/evaluators/{evaluatorId}/availability/{evaluatorAvailabilityId}` | `lms.evaluator_availability.read` | served |  |
| `lms.UpdateEvaluatorAvailability` | PATCH | `/v1/lms/evaluators/{evaluatorId}/availability/{evaluatorAvailabilityId}` | `lms.evaluator_availability.update` | served |  |
| `lms.DeleteEvaluatorAvailability` | DELETE | `/v1/lms/evaluators/{evaluatorId}/availability/{evaluatorAvailabilityId}` | `lms.evaluator_availability.delete` | served |  |
| `lms.ListLmsCourses` | GET | `/v1/lms/courses` | `lms.course.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateLmsCourse` | POST | `/v1/lms/courses` | `lms.course.create` | served |  |
| `lms.GetLmsCourse` | GET | `/v1/lms/courses/{courseId}` | `lms.course.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.UpdateLmsCourse` | PATCH | `/v1/lms/courses/{courseId}` | `lms.course.update` | served |  |
| `lms.DeleteLmsCourse` | DELETE | `/v1/lms/courses/{courseId}` | `lms.course.delete` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ListCourseTeachers` | GET | `/v1/lms/courses/{courseId}/teachers` | `lms.course_teacher.read` | served |  |
| `lms.CreateCourseTeacher` | POST | `/v1/lms/courses/{courseId}/teachers` | `lms.course_teacher.create` | served |  |
| `lms.GetCourseTeacher` | GET | `/v1/lms/courses/{courseId}/teachers/{courseTeacherId}` | `lms.course_teacher.read` | served |  |
| `lms.UpdateCourseTeacher` | PATCH | `/v1/lms/courses/{courseId}/teachers/{courseTeacherId}` | `lms.course_teacher.update` | served |  |
| `lms.DeleteCourseTeacher` | DELETE | `/v1/lms/courses/{courseId}/teachers/{courseTeacherId}` | `lms.course_teacher.delete` | served |  |
| `lms.ListRelatedCourses` | GET | `/v1/lms/courses/{courseId}/related-courses` | `lms.related_course.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateRelatedCourse` | POST | `/v1/lms/courses/{courseId}/related-courses` | `lms.related_course.create` | served |  |
| `lms.GetRelatedCourse` | GET | `/v1/lms/courses/{courseId}/related-courses/{relatedCourseId}` | `lms.related_course.read` | served |  |
| `lms.UpdateRelatedCourse` | PATCH | `/v1/lms/courses/{courseId}/related-courses/{relatedCourseId}` | `lms.related_course.update` | served |  |
| `lms.DeleteRelatedCourse` | DELETE | `/v1/lms/courses/{courseId}/related-courses/{relatedCourseId}` | `lms.related_course.delete` | served |  |
| `lms.ListCourseMentors` | GET | `/v1/lms/courses/{courseId}/mentors` | `lms.course_mentor.read` | served |  |
| `lms.CreateCourseMentor` | POST | `/v1/lms/courses/{courseId}/mentors` | `lms.course_mentor.create` | served |  |
| `lms.GetCourseMentor` | GET | `/v1/lms/courses/{courseId}/mentors/{courseMentorId}` | `lms.course_mentor.read` | served |  |
| `lms.UpdateCourseMentor` | PATCH | `/v1/lms/courses/{courseId}/mentors/{courseMentorId}` | `lms.course_mentor.update` | served |  |
| `lms.DeleteCourseMentor` | DELETE | `/v1/lms/courses/{courseId}/mentors/{courseMentorId}` | `lms.course_mentor.delete` | served |  |
| `lms.ListChapters` | GET | `/v1/lms/courses/{courseId}/chapters` | `lms.chapter.read` | served |  |
| `lms.CreateChapter` | POST | `/v1/lms/courses/{courseId}/chapters` | `lms.chapter.create` | served |  |
| `lms.GetChapter` | GET | `/v1/lms/courses/{courseId}/chapters/{chapterId}` | `lms.chapter.read` | served |  |
| `lms.UpdateChapter` | PATCH | `/v1/lms/courses/{courseId}/chapters/{chapterId}` | `lms.chapter.update` | served |  |
| `lms.DeleteChapter` | DELETE | `/v1/lms/courses/{courseId}/chapters/{chapterId}` | `lms.chapter.delete` | served |  |
| `lms.ListLessons` | GET | `/v1/lms/lessons` | `lms.lesson.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateLesson` | POST | `/v1/lms/lessons` | `lms.lesson.create` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.GetLesson` | GET | `/v1/lms/lessons/{lessonId}` | `lms.lesson.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.UpdateLesson` | PATCH | `/v1/lms/lessons/{lessonId}` | `lms.lesson.update` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.DeleteLesson` | DELETE | `/v1/lms/lessons/{lessonId}` | `lms.lesson.delete` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ListQuizzes` | GET | `/v1/lms/quizzes` | `lms.quiz.read` | served |  |
| `lms.CreateQuiz` | POST | `/v1/lms/quizzes` | `lms.quiz.create` | served |  |
| `lms.GetQuiz` | GET | `/v1/lms/quizzes/{quizId}` | `lms.quiz.read` | served |  |
| `lms.UpdateQuiz` | PATCH | `/v1/lms/quizzes/{quizId}` | `lms.quiz.update` | served |  |
| `lms.DeleteQuiz` | DELETE | `/v1/lms/quizzes/{quizId}` | `lms.quiz.delete` | served |  |
| `lms.ListQuestions` | GET | `/v1/lms/questions` | `lms.question.read` | served |  |
| `lms.CreateQuestion` | POST | `/v1/lms/questions` | `lms.question.create` | served |  |
| `lms.GetQuestion` | GET | `/v1/lms/questions/{questionId}` | `lms.question.read` | served |  |
| `lms.UpdateQuestion` | PATCH | `/v1/lms/questions/{questionId}` | `lms.question.update` | served |  |
| `lms.DeleteQuestion` | DELETE | `/v1/lms/questions/{questionId}` | `lms.question.delete` | served |  |
| `lms.ListQuestionOptions` | GET | `/v1/lms/questions/{questionId}/options` | `lms.question_option.read` | served |  |
| `lms.CreateQuestionOption` | POST | `/v1/lms/questions/{questionId}/options` | `lms.question_option.create` | served |  |
| `lms.GetQuestionOption` | GET | `/v1/lms/questions/{questionId}/options/{questionOptionId}` | `lms.question_option.read` | served |  |
| `lms.UpdateQuestionOption` | PATCH | `/v1/lms/questions/{questionId}/options/{questionOptionId}` | `lms.question_option.update` | served |  |
| `lms.DeleteQuestionOption` | DELETE | `/v1/lms/questions/{questionId}/options/{questionOptionId}` | `lms.question_option.delete` | served |  |
| `lms.ListQuestionAcceptedAnswers` | GET | `/v1/lms/questions/{questionId}/accepted-answers` | `lms.question_accepted_answer.read` | served |  |
| `lms.CreateQuestionAcceptedAnswer` | POST | `/v1/lms/questions/{questionId}/accepted-answers` | `lms.question_accepted_answer.create` | served |  |
| `lms.GetQuestionAcceptedAnswer` | GET | `/v1/lms/questions/{questionId}/accepted-answers/{questionAcceptedAnswerId}` | `lms.question_accepted_answer.read` | served |  |
| `lms.UpdateQuestionAcceptedAnswer` | PATCH | `/v1/lms/questions/{questionId}/accepted-answers/{questionAcceptedAnswerId}` | `lms.question_accepted_answer.update` | served |  |
| `lms.DeleteQuestionAcceptedAnswer` | DELETE | `/v1/lms/questions/{questionId}/accepted-answers/{questionAcceptedAnswerId}` | `lms.question_accepted_answer.delete` | served |  |
| `lms.ListQuizQuestions` | GET | `/v1/lms/quizzes/{quizId}/questions` | `lms.quiz_question.read` | served |  |
| `lms.CreateQuizQuestion` | POST | `/v1/lms/quizzes/{quizId}/questions` | `lms.quiz_question.create` | served |  |
| `lms.GetQuizQuestion` | GET | `/v1/lms/quizzes/{quizId}/questions/{quizQuestionId}` | `lms.quiz_question.read` | served |  |
| `lms.UpdateQuizQuestion` | PATCH | `/v1/lms/quizzes/{quizId}/questions/{quizQuestionId}` | `lms.quiz_question.update` | served |  |
| `lms.DeleteQuizQuestion` | DELETE | `/v1/lms/quizzes/{quizId}/questions/{quizQuestionId}` | `lms.quiz_question.delete` | served |  |
| `lms.ListQuizSubmissions` | GET | `/v1/lms/quiz-submissions` | `lms.quiz_submission.read` | served |  |
| `lms.GetQuizSubmission` | GET | `/v1/lms/quiz-submissions/{quizSubmissionId}` | `lms.quiz_submission.read` | served |  |
| `lms.ListQuizAnswers` | GET | `/v1/lms/quiz-submissions/{quizSubmissionId}/answers` | `lms.quiz_answer.read` | served |  |
| `lms.GetQuizAnswer` | GET | `/v1/lms/quiz-submissions/{quizSubmissionId}/answers/{quizAnswerId}` | `lms.quiz_answer.read` | served |  |
| `lms.ListQuizViolations` | GET | `/v1/lms/quiz-submissions/{quizSubmissionId}/violations` | `lms.quiz_violation.read` | served |  |
| `lms.GetQuizViolation` | GET | `/v1/lms/quiz-submissions/{quizSubmissionId}/violations/{quizViolationId}` | `lms.quiz_violation.read` | served |  |
| `lms.ListLmsAssignments` | GET | `/v1/lms/assignments` | `lms.assignment.read` | served |  |
| `lms.CreateLmsAssignment` | POST | `/v1/lms/assignments` | `lms.assignment.create` | served |  |
| `lms.GetLmsAssignment` | GET | `/v1/lms/assignments/{assignmentId}` | `lms.assignment.read` | served |  |
| `lms.UpdateLmsAssignment` | PATCH | `/v1/lms/assignments/{assignmentId}` | `lms.assignment.update` | served |  |
| `lms.DeleteLmsAssignment` | DELETE | `/v1/lms/assignments/{assignmentId}` | `lms.assignment.delete` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ListLmsAssignmentSubmissions` | GET | `/v1/lms/assignment-submissions` | `lms.assignment_submission.read` | served |  |
| `lms.CreateLmsAssignmentSubmission` | POST | `/v1/lms/assignment-submissions` | `lms.assignment_submission.create` | served |  |
| `lms.GetLmsAssignmentSubmission` | GET | `/v1/lms/assignment-submissions/{assignmentSubmissionId}` | `lms.assignment_submission.read` | served |  |
| `lms.UpdateLmsAssignmentSubmission` | PATCH | `/v1/lms/assignment-submissions/{assignmentSubmissionId}` | `lms.assignment_submission.update` | served |  |
| `lms.DeleteLmsAssignmentSubmission` | DELETE | `/v1/lms/assignment-submissions/{assignmentSubmissionId}` | `lms.assignment_submission.delete` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ListProgrammingExercises` | GET | `/v1/lms/programming-exercises` | `lms.programming_exercise.read` | served |  |
| `lms.CreateProgrammingExercise` | POST | `/v1/lms/programming-exercises` | `lms.programming_exercise.create` | served |  |
| `lms.GetProgrammingExercise` | GET | `/v1/lms/programming-exercises/{programmingExerciseId}` | `lms.programming_exercise.read` | served |  |
| `lms.UpdateProgrammingExercise` | PATCH | `/v1/lms/programming-exercises/{programmingExerciseId}` | `lms.programming_exercise.update` | served |  |
| `lms.DeleteProgrammingExercise` | DELETE | `/v1/lms/programming-exercises/{programmingExerciseId}` | `lms.programming_exercise.delete` | served |  |
| `lms.ListExerciseTestCases` | GET | `/v1/lms/programming-exercises/{programmingExerciseId}/test-cases` | `lms.exercise_test_case.read` | served |  |
| `lms.CreateExerciseTestCase` | POST | `/v1/lms/programming-exercises/{programmingExerciseId}/test-cases` | `lms.exercise_test_case.create` | served |  |
| `lms.GetExerciseTestCase` | GET | `/v1/lms/programming-exercises/{programmingExerciseId}/test-cases/{exerciseTestCaseId}` | `lms.exercise_test_case.read` | served |  |
| `lms.UpdateExerciseTestCase` | PATCH | `/v1/lms/programming-exercises/{programmingExerciseId}/test-cases/{exerciseTestCaseId}` | `lms.exercise_test_case.update` | served |  |
| `lms.DeleteExerciseTestCase` | DELETE | `/v1/lms/programming-exercises/{programmingExerciseId}/test-cases/{exerciseTestCaseId}` | `lms.exercise_test_case.delete` | served |  |
| `lms.ListExerciseSubmissions` | GET | `/v1/lms/exercise-submissions` | `lms.exercise_submission.read` | served |  |
| `lms.GetExerciseSubmission` | GET | `/v1/lms/exercise-submissions/{exerciseSubmissionId}` | `lms.exercise_submission.read` | served |  |
| `lms.ListExerciseTestResults` | GET | `/v1/lms/exercise-submissions/{exerciseSubmissionId}/test-results` | `lms.exercise_test_result.read` | served |  |
| `lms.GetExerciseTestResult` | GET | `/v1/lms/exercise-submissions/{exerciseSubmissionId}/test-results/{exerciseTestResultId}` | `lms.exercise_test_result.read` | served |  |
| `lms.ListContentAuthors` | GET | `/v1/lms/content-authors` | `lms.content_author.read` | served |  |
| `lms.CreateContentAuthor` | POST | `/v1/lms/content-authors` | `lms.content_author.create` | served |  |
| `lms.GetContentAuthor` | GET | `/v1/lms/content-authors/{contentAuthorId}` | `lms.content_author.read` | served |  |
| `lms.UpdateContentAuthor` | PATCH | `/v1/lms/content-authors/{contentAuthorId}` | `lms.content_author.update` | served |  |
| `lms.DeleteContentAuthor` | DELETE | `/v1/lms/content-authors/{contentAuthorId}` | `lms.content_author.delete` | served |  |
| `lms.ListLessonActivities` | GET | `/v1/lms/lessons/{lessonId}/activities` | `lms.lesson_activity.read` | served |  |
| `lms.CreateLessonActivity` | POST | `/v1/lms/lessons/{lessonId}/activities` | `lms.lesson_activity.create` | served |  |
| `lms.GetLessonActivity` | GET | `/v1/lms/lessons/{lessonId}/activities/{lessonActivityId}` | `lms.lesson_activity.read` | served |  |
| `lms.UpdateLessonActivity` | PATCH | `/v1/lms/lessons/{lessonId}/activities/{lessonActivityId}` | `lms.lesson_activity.update` | served |  |
| `lms.DeleteLessonActivity` | DELETE | `/v1/lms/lessons/{lessonId}/activities/{lessonActivityId}` | `lms.lesson_activity.delete` | served |  |
| `lms.ListEnrollments` | GET | `/v1/lms/enrollments` | `lms.enrollment.read` | served |  |
| `lms.GetEnrollment` | GET | `/v1/lms/enrollments/{enrollmentId}` | `lms.enrollment.read` | served |  |
| `lms.UpdateEnrollment` | PATCH | `/v1/lms/enrollments/{enrollmentId}` | `lms.enrollment.update` | served |  |
| `lms.DeleteEnrollment` | DELETE | `/v1/lms/enrollments/{enrollmentId}` | `lms.enrollment.delete` | served |  |
| `lms.ListLessonProgress` | GET | `/v1/lms/lesson-progress` | `lms.lesson_progress.read` | served |  |
| `lms.GetLessonProgress` | GET | `/v1/lms/lesson-progress/{lessonProgressId}` | `lms.lesson_progress.read` | served |  |
| `lms.ListLessonNotes` | GET | `/v1/lms/lessons/{lessonId}/notes` | `lms.lesson_note.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateLessonNote` | POST | `/v1/lms/lessons/{lessonId}/notes` | `lms.lesson_note.create` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.GetLessonNote` | GET | `/v1/lms/lessons/{lessonId}/notes/{lessonNoteId}` | `lms.lesson_note.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.UpdateLessonNote` | PATCH | `/v1/lms/lessons/{lessonId}/notes/{lessonNoteId}` | `lms.lesson_note.update` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.DeleteLessonNote` | DELETE | `/v1/lms/lessons/{lessonId}/notes/{lessonNoteId}` | `lms.lesson_note.delete` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ListVideoWatches` | GET | `/v1/lms/lessons/{lessonId}/video-watches` | `lms.video_watch.read` | served |  |
| `lms.GetVideoWatch` | GET | `/v1/lms/lessons/{lessonId}/video-watches/{videoWatchId}` | `lms.video_watch.read` | served |  |
| `lms.ListCourseReviews` | GET | `/v1/lms/courses/{courseId}/reviews` | `lms.course_review.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateCourseReview` | POST | `/v1/lms/courses/{courseId}/reviews` | `lms.course_review.create` | served |  |
| `lms.GetCourseReview` | GET | `/v1/lms/courses/{courseId}/reviews/{courseReviewId}` | `lms.course_review.read` | served |  |
| `lms.UpdateCourseReview` | PATCH | `/v1/lms/courses/{courseId}/reviews/{courseReviewId}` | `lms.course_review.update` | served |  |
| `lms.DeleteCourseReview` | DELETE | `/v1/lms/courses/{courseId}/reviews/{courseReviewId}` | `lms.course_review.delete` | served |  |
| `lms.ListCourseInterests` | GET | `/v1/lms/courses/{courseId}/interests` | `lms.course_interest.read` | served |  |
| `lms.CreateCourseInterest` | POST | `/v1/lms/courses/{courseId}/interests` | `lms.course_interest.create` | served |  |
| `lms.GetCourseInterest` | GET | `/v1/lms/courses/{courseId}/interests/{courseInterestId}` | `lms.course_interest.read` | served |  |
| `lms.UpdateCourseInterest` | PATCH | `/v1/lms/courses/{courseId}/interests/{courseInterestId}` | `lms.course_interest.update` | served |  |
| `lms.DeleteCourseInterest` | DELETE | `/v1/lms/courses/{courseId}/interests/{courseInterestId}` | `lms.course_interest.delete` | served |  |
| `lms.ListConferencingAccounts` | GET | `/v1/lms/conferencing-accounts` | `lms.conferencing_account.read` | served |  |
| `lms.CreateConferencingAccount` | POST | `/v1/lms/conferencing-accounts` | `lms.conferencing_account.create` | served |  |
| `lms.GetConferencingAccount` | GET | `/v1/lms/conferencing-accounts/{conferencingAccountId}` | `lms.conferencing_account.read` | served |  |
| `lms.UpdateConferencingAccount` | PATCH | `/v1/lms/conferencing-accounts/{conferencingAccountId}` | `lms.conferencing_account.update` | served |  |
| `lms.DeleteConferencingAccount` | DELETE | `/v1/lms/conferencing-accounts/{conferencingAccountId}` | `lms.conferencing_account.delete` | served |  |
| `lms.ListTimetableTemplates` | GET | `/v1/lms/timetable-templates` | `lms.timetable_template.read` | served |  |
| `lms.CreateTimetableTemplate` | POST | `/v1/lms/timetable-templates` | `lms.timetable_template.create` | served |  |
| `lms.GetTimetableTemplate` | GET | `/v1/lms/timetable-templates/{timetableTemplateId}` | `lms.timetable_template.read` | served |  |
| `lms.UpdateTimetableTemplate` | PATCH | `/v1/lms/timetable-templates/{timetableTemplateId}` | `lms.timetable_template.update` | served |  |
| `lms.DeleteTimetableTemplate` | DELETE | `/v1/lms/timetable-templates/{timetableTemplateId}` | `lms.timetable_template.delete` | served |  |
| `lms.ListCohorts` | GET | `/v1/lms/cohorts` | `lms.cohort.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateCohort` | POST | `/v1/lms/cohorts` | `lms.cohort.create` | served |  |
| `lms.GetCohort` | GET | `/v1/lms/cohorts/{cohortId}` | `lms.cohort.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.UpdateCohort` | PATCH | `/v1/lms/cohorts/{cohortId}` | `lms.cohort.update` | served |  |
| `lms.DeleteCohort` | DELETE | `/v1/lms/cohorts/{cohortId}` | `lms.cohort.delete` | served |  |
| `lms.ListCohortCourses` | GET | `/v1/lms/cohorts/{cohortId}/courses` | `lms.cohort_course.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateCohortCourse` | POST | `/v1/lms/cohorts/{cohortId}/courses` | `lms.cohort_course.create` | served |  |
| `lms.GetCohortCourse` | GET | `/v1/lms/cohorts/{cohortId}/courses/{cohortCourseId}` | `lms.cohort_course.read` | served |  |
| `lms.UpdateCohortCourse` | PATCH | `/v1/lms/cohorts/{cohortId}/courses/{cohortCourseId}` | `lms.cohort_course.update` | served |  |
| `lms.DeleteCohortCourse` | DELETE | `/v1/lms/cohorts/{cohortId}/courses/{cohortCourseId}` | `lms.cohort_course.delete` | served |  |
| `lms.ListCohortTeachers` | GET | `/v1/lms/cohorts/{cohortId}/teachers` | `lms.cohort_teacher.read` | served |  |
| `lms.CreateCohortTeacher` | POST | `/v1/lms/cohorts/{cohortId}/teachers` | `lms.cohort_teacher.create` | served |  |
| `lms.GetCohortTeacher` | GET | `/v1/lms/cohorts/{cohortId}/teachers/{cohortTeacherId}` | `lms.cohort_teacher.read` | served |  |
| `lms.UpdateCohortTeacher` | PATCH | `/v1/lms/cohorts/{cohortId}/teachers/{cohortTeacherId}` | `lms.cohort_teacher.update` | served |  |
| `lms.DeleteCohortTeacher` | DELETE | `/v1/lms/cohorts/{cohortId}/teachers/{cohortTeacherId}` | `lms.cohort_teacher.delete` | served |  |
| `lms.ListCohortActivities` | GET | `/v1/lms/cohorts/{cohortId}/activities` | `lms.cohort_activity.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateCohortActivity` | POST | `/v1/lms/cohorts/{cohortId}/activities` | `lms.cohort_activity.create` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.GetCohortActivity` | GET | `/v1/lms/cohorts/{cohortId}/activities/{cohortActivityId}` | `lms.cohort_activity.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.UpdateCohortActivity` | PATCH | `/v1/lms/cohorts/{cohortId}/activities/{cohortActivityId}` | `lms.cohort_activity.update` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.DeleteCohortActivity` | DELETE | `/v1/lms/cohorts/{cohortId}/activities/{cohortActivityId}` | `lms.cohort_activity.delete` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ListTimetableEntries` | GET | `/v1/lms/timetable-entries` | `lms.timetable_entry.read` | served |  |
| `lms.CreateTimetableEntry` | POST | `/v1/lms/timetable-entries` | `lms.timetable_entry.create` | served |  |
| `lms.GetTimetableEntry` | GET | `/v1/lms/timetable-entries/{timetableEntryId}` | `lms.timetable_entry.read` | served |  |
| `lms.UpdateTimetableEntry` | PATCH | `/v1/lms/timetable-entries/{timetableEntryId}` | `lms.timetable_entry.update` | served |  |
| `lms.DeleteTimetableEntry` | DELETE | `/v1/lms/timetable-entries/{timetableEntryId}` | `lms.timetable_entry.delete` | served |  |
| `lms.ListTimetableLegends` | GET | `/v1/lms/timetable-legends` | `lms.timetable_legend.read` | served |  |
| `lms.CreateTimetableLegend` | POST | `/v1/lms/timetable-legends` | `lms.timetable_legend.create` | served |  |
| `lms.GetTimetableLegend` | GET | `/v1/lms/timetable-legends/{timetableLegendId}` | `lms.timetable_legend.read` | served |  |
| `lms.UpdateTimetableLegend` | PATCH | `/v1/lms/timetable-legends/{timetableLegendId}` | `lms.timetable_legend.update` | served |  |
| `lms.DeleteTimetableLegend` | DELETE | `/v1/lms/timetable-legends/{timetableLegendId}` | `lms.timetable_legend.delete` | served |  |
| `lms.ListCohortEnrollments` | GET | `/v1/lms/cohort-enrollments` | `lms.cohort_enrollment.read` | served |  |
| `lms.GetCohortEnrollment` | GET | `/v1/lms/cohort-enrollments/{cohortEnrollmentId}` | `lms.cohort_enrollment.read` | served |  |
| `lms.UpdateCohortEnrollment` | PATCH | `/v1/lms/cohort-enrollments/{cohortEnrollmentId}` | `lms.cohort_enrollment.update` | served |  |
| `lms.DeleteCohortEnrollment` | DELETE | `/v1/lms/cohort-enrollments/{cohortEnrollmentId}` | `lms.cohort_enrollment.delete` | served |  |
| `lms.ListCohortFeedback` | GET | `/v1/lms/cohorts/{cohortId}/feedback` | `lms.cohort_feedback.read` | served |  |
| `lms.CreateCohortFeedback` | POST | `/v1/lms/cohorts/{cohortId}/feedback` | `lms.cohort_feedback.create` | served |  |
| `lms.GetCohortFeedback` | GET | `/v1/lms/cohorts/{cohortId}/feedback/{cohortFeedbackId}` | `lms.cohort_feedback.read` | served |  |
| `lms.UpdateCohortFeedback` | PATCH | `/v1/lms/cohorts/{cohortId}/feedback/{cohortFeedbackId}` | `lms.cohort_feedback.update` | served |  |
| `lms.DeleteCohortFeedback` | DELETE | `/v1/lms/cohorts/{cohortId}/feedback/{cohortFeedbackId}` | `lms.cohort_feedback.delete` | served |  |
| `lms.ListLiveClasses` | GET | `/v1/lms/live-classes` | `lms.live_class.read` | served |  |
| `lms.CreateLiveClass` | POST | `/v1/lms/live-classes` | `lms.live_class.create` | served |  |
| `lms.GetLiveClass` | GET | `/v1/lms/live-classes/{liveClassId}` | `lms.live_class.read` | served |  |
| `lms.UpdateLiveClass` | PATCH | `/v1/lms/live-classes/{liveClassId}` | `lms.live_class.update` | served |  |
| `lms.DeleteLiveClass` | DELETE | `/v1/lms/live-classes/{liveClassId}` | `lms.live_class.delete` | served |  |
| `lms.ListLiveClassParticipants` | GET | `/v1/lms/live-classes/{liveClassId}/participants` | `lms.live_class_participant.read` | served |  |
| `lms.GetLiveClassParticipant` | GET | `/v1/lms/live-classes/{liveClassId}/participants/{liveClassParticipantId}` | `lms.live_class_participant.read` | served |  |
| `lms.ListLearningPrograms` | GET | `/v1/lms/learning-programs` | `lms.learning_program.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateLearningProgram` | POST | `/v1/lms/learning-programs` | `lms.learning_program.create` | served |  |
| `lms.GetLearningProgram` | GET | `/v1/lms/learning-programs/{learningProgramId}` | `lms.learning_program.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.UpdateLearningProgram` | PATCH | `/v1/lms/learning-programs/{learningProgramId}` | `lms.learning_program.update` | served |  |
| `lms.DeleteLearningProgram` | DELETE | `/v1/lms/learning-programs/{learningProgramId}` | `lms.learning_program.delete` | served |  |
| `lms.ListLearningProgramCourses` | GET | `/v1/lms/learning-programs/{learningProgramId}/courses` | `lms.learning_program_course.read` | served |  |
| `lms.CreateLearningProgramCourse` | POST | `/v1/lms/learning-programs/{learningProgramId}/courses` | `lms.learning_program_course.create` | served |  |
| `lms.GetLearningProgramCourse` | GET | `/v1/lms/learning-programs/{learningProgramId}/courses/{learningProgramCourseId}` | `lms.learning_program_course.read` | served |  |
| `lms.UpdateLearningProgramCourse` | PATCH | `/v1/lms/learning-programs/{learningProgramId}/courses/{learningProgramCourseId}` | `lms.learning_program_course.update` | served |  |
| `lms.DeleteLearningProgramCourse` | DELETE | `/v1/lms/learning-programs/{learningProgramId}/courses/{learningProgramCourseId}` | `lms.learning_program_course.delete` | served |  |
| `lms.ListLearningProgramMembers` | GET | `/v1/lms/learning-programs/{learningProgramId}/members` | `lms.learning_program_member.read` | served |  |
| `lms.CreateLearningProgramMember` | POST | `/v1/lms/learning-programs/{learningProgramId}/members` | `lms.learning_program_member.create` | served |  |
| `lms.GetLearningProgramMember` | GET | `/v1/lms/learning-programs/{learningProgramId}/members/{learningProgramMemberId}` | `lms.learning_program_member.read` | served |  |
| `lms.UpdateLearningProgramMember` | PATCH | `/v1/lms/learning-programs/{learningProgramId}/members/{learningProgramMemberId}` | `lms.learning_program_member.update` | served |  |
| `lms.DeleteLearningProgramMember` | DELETE | `/v1/lms/learning-programs/{learningProgramId}/members/{learningProgramMemberId}` | `lms.learning_program_member.delete` | served |  |
| `lms.ListCertificates` | GET | `/v1/lms/certificates` | `lms.certificate.read` | served |  |
| `lms.GetCertificate` | GET | `/v1/lms/certificates/{certificateId}` | `lms.certificate.read` | served |  |
| `lms.ListCertificateRequests` | GET | `/v1/lms/certificate-requests` | `lms.certificate_request.read` | served |  |
| `lms.CreateCertificateRequest` | POST | `/v1/lms/certificate-requests` | `lms.certificate_request.create` | served |  |
| `lms.GetCertificateRequest` | GET | `/v1/lms/certificate-requests/{certificateRequestId}` | `lms.certificate_request.read` | served |  |
| `lms.UpdateCertificateRequest` | PATCH | `/v1/lms/certificate-requests/{certificateRequestId}` | `lms.certificate_request.update` | served |  |
| `lms.DeleteCertificateRequest` | DELETE | `/v1/lms/certificate-requests/{certificateRequestId}` | `lms.certificate_request.delete` | served |  |
| `lms.ListCertificateEvaluations` | GET | `/v1/lms/certificate-evaluations` | `lms.certificate_evaluation.read` | served |  |
| `lms.CreateCertificateEvaluation` | POST | `/v1/lms/certificate-evaluations` | `lms.certificate_evaluation.create` | served |  |
| `lms.GetCertificateEvaluation` | GET | `/v1/lms/certificate-evaluations/{certificateEvaluationId}` | `lms.certificate_evaluation.read` | served |  |
| `lms.UpdateCertificateEvaluation` | PATCH | `/v1/lms/certificate-evaluations/{certificateEvaluationId}` | `lms.certificate_evaluation.update` | served |  |
| `lms.DeleteCertificateEvaluation` | DELETE | `/v1/lms/certificate-evaluations/{certificateEvaluationId}` | `lms.certificate_evaluation.delete` | served |  |
| `lms.ListBadges` | GET | `/v1/lms/badges` | `lms.badge.read` | served |  |
| `lms.CreateBadge` | POST | `/v1/lms/badges` | `lms.badge.create` | served |  |
| `lms.GetBadge` | GET | `/v1/lms/badges/{badgeId}` | `lms.badge.read` | served |  |
| `lms.UpdateBadge` | PATCH | `/v1/lms/badges/{badgeId}` | `lms.badge.update` | served |  |
| `lms.DeleteBadge` | DELETE | `/v1/lms/badges/{badgeId}` | `lms.badge.delete` | served |  |
| `lms.ListBadgeAwards` | GET | `/v1/lms/badge-awards` | `lms.badge_award.read` | served |  |
| `lms.GetBadgeAward` | GET | `/v1/lms/badge-awards/{badgeAwardId}` | `lms.badge_award.read` | served |  |
| `lms.GetSettings` | GET | `/v1/lms/settings` | `lms.settings.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.UpdateSettings` | PATCH | `/v1/lms/settings` | `lms.settings.update` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ListSettingsSidebarItems` | GET | `/v1/lms/settings/{settingsId}/sidebar-items` | `lms.settings_sidebar_item.read` | served |  |
| `lms.CreateSettingsSidebarItem` | POST | `/v1/lms/settings/{settingsId}/sidebar-items` | `lms.settings_sidebar_item.create` | served |  |
| `lms.GetSettingsSidebarItem` | GET | `/v1/lms/settings/{settingsId}/sidebar-items/{settingsSidebarItemId}` | `lms.settings_sidebar_item.read` | served |  |
| `lms.UpdateSettingsSidebarItem` | PATCH | `/v1/lms/settings/{settingsId}/sidebar-items/{settingsSidebarItemId}` | `lms.settings_sidebar_item.update` | served |  |
| `lms.DeleteSettingsSidebarItem` | DELETE | `/v1/lms/settings/{settingsId}/sidebar-items/{settingsSidebarItemId}` | `lms.settings_sidebar_item.delete` | served |  |
| `lms.ListSettingsPrimaryCountries` | GET | `/v1/lms/settings/{settingsId}/primary-countries` | `lms.settings_primary_country.read` | served |  |
| `lms.CreateSettingsPrimaryCountry` | POST | `/v1/lms/settings/{settingsId}/primary-countries` | `lms.settings_primary_country.create` | served |  |
| `lms.GetSettingsPrimaryCountry` | GET | `/v1/lms/settings/{settingsId}/primary-countries/{settingsPrimaryCountryId}` | `lms.settings_primary_country.read` | served |  |
| `lms.UpdateSettingsPrimaryCountry` | PATCH | `/v1/lms/settings/{settingsId}/primary-countries/{settingsPrimaryCountryId}` | `lms.settings_primary_country.update` | served |  |
| `lms.DeleteSettingsPrimaryCountry` | DELETE | `/v1/lms/settings/{settingsId}/primary-countries/{settingsPrimaryCountryId}` | `lms.settings_primary_country.delete` | served |  |
| `lms.ListLearningObjectives` | GET | `/v1/lms/learning-objectives` | `lms.learning_objective.read` | served |  |
| `lms.CreateLearningObjective` | POST | `/v1/lms/learning-objectives` | `lms.learning_objective.create` | served |  |
| `lms.GetLearningObjective` | GET | `/v1/lms/learning-objectives/{learningObjectiveId}` | `lms.learning_objective.read` | served |  |
| `lms.UpdateLearningObjective` | PATCH | `/v1/lms/learning-objectives/{learningObjectiveId}` | `lms.learning_objective.update` | served |  |
| `lms.DeleteLearningObjective` | DELETE | `/v1/lms/learning-objectives/{learningObjectiveId}` | `lms.learning_objective.delete` | served |  |
| `lms.ListGradingCategories` | GET | `/v1/lms/grading-categories` | `lms.grading_category.read` | served |  |
| `lms.CreateGradingCategory` | POST | `/v1/lms/grading-categories` | `lms.grading_category.create` | served |  |
| `lms.GetGradingCategory` | GET | `/v1/lms/grading-categories/{gradingCategoryId}` | `lms.grading_category.read` | served |  |
| `lms.UpdateGradingCategory` | PATCH | `/v1/lms/grading-categories/{gradingCategoryId}` | `lms.grading_category.update` | served |  |
| `lms.DeleteGradingCategory` | DELETE | `/v1/lms/grading-categories/{gradingCategoryId}` | `lms.grading_category.delete` | served |  |
| `lms.ListContentItems` | GET | `/v1/lms/content-items` | `lms.content_item.read` | served |  |
| `lms.CreateContentItem` | POST | `/v1/lms/content-items` | `lms.content_item.create` | served |  |
| `lms.GetContentItem` | GET | `/v1/lms/content-items/{contentItemId}` | `lms.content_item.read` | served |  |
| `lms.UpdateContentItem` | PATCH | `/v1/lms/content-items/{contentItemId}` | `lms.content_item.update` | served |  |
| `lms.DeleteContentItem` | DELETE | `/v1/lms/content-items/{contentItemId}` | `lms.content_item.delete` | served |  |
| `lms.ListContentItemRatings` | GET | `/v1/lms/content-items/{contentItemId}/ratings` | `lms.content_item_rating.read` | served |  |
| `lms.CreateContentItemRating` | POST | `/v1/lms/content-items/{contentItemId}/ratings` | `lms.content_item_rating.create` | served |  |
| `lms.GetContentItemRating` | GET | `/v1/lms/content-items/{contentItemId}/ratings/{contentItemRatingId}` | `lms.content_item_rating.read` | served |  |
| `lms.UpdateContentItemRating` | PATCH | `/v1/lms/content-items/{contentItemId}/ratings/{contentItemRatingId}` | `lms.content_item_rating.update` | served |  |
| `lms.DeleteContentItemRating` | DELETE | `/v1/lms/content-items/{contentItemId}/ratings/{contentItemRatingId}` | `lms.content_item_rating.delete` | served |  |
| `lms.ListItemOverrides` | GET | `/v1/lms/content-items/{contentItemId}/overrides` | `lms.item_override.read` | served |  |
| `lms.CreateItemOverride` | POST | `/v1/lms/content-items/{contentItemId}/overrides` | `lms.item_override.create` | served |  |
| `lms.GetItemOverride` | GET | `/v1/lms/content-items/{contentItemId}/overrides/{itemOverrideId}` | `lms.item_override.read` | served |  |
| `lms.UpdateItemOverride` | PATCH | `/v1/lms/content-items/{contentItemId}/overrides/{itemOverrideId}` | `lms.item_override.update` | served |  |
| `lms.DeleteItemOverride` | DELETE | `/v1/lms/content-items/{contentItemId}/overrides/{itemOverrideId}` | `lms.item_override.delete` | served |  |
| `lms.ListGradebookEntries` | GET | `/v1/lms/gradebook-entries` | `lms.gradebook_entry.read` | served |  |
| `lms.GetGradebookEntry` | GET | `/v1/lms/gradebook-entries/{gradebookEntryId}` | `lms.gradebook_entry.read` | served |  |
| `lms.ListGradeChangeLogs` | GET | `/v1/lms/grade-change-logs` | `lms.grade_change_log.read` | served |  |
| `lms.GetGradeChangeLog` | GET | `/v1/lms/grade-change-logs/{gradeChangeLogId}` | `lms.grade_change_log.read` | served |  |
| `lms.ListReassignmentLogs` | GET | `/v1/lms/reassignment-logs` | `lms.reassignment_log.read` | served |  |
| `lms.GetReassignmentLog` | GET | `/v1/lms/reassignment-logs/{reassignmentLogId}` | `lms.reassignment_log.read` | served |  |
| `lms.ListAssignmentGroups` | GET | `/v1/lms/content-items/{contentItemId}/groups` | `lms.assignment_group.read` | served |  |
| `lms.CreateAssignmentGroup` | POST | `/v1/lms/content-items/{contentItemId}/groups` | `lms.assignment_group.create` | served |  |
| `lms.GetAssignmentGroup` | GET | `/v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}` | `lms.assignment_group.read` | served |  |
| `lms.UpdateAssignmentGroup` | PATCH | `/v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}` | `lms.assignment_group.update` | served |  |
| `lms.DeleteAssignmentGroup` | DELETE | `/v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}` | `lms.assignment_group.delete` | served |  |
| `lms.ListAssignmentGroupMembers` | GET | `/v1/lms/assignment-group-members` | `lms.assignment_group_member.read` | served |  |
| `lms.CreateAssignmentGroupMember` | POST | `/v1/lms/assignment-group-members` | `lms.assignment_group_member.create` | served |  |
| `lms.GetAssignmentGroupMember` | GET | `/v1/lms/assignment-group-members/{assignmentGroupMemberId}` | `lms.assignment_group_member.read` | served |  |
| `lms.UpdateAssignmentGroupMember` | PATCH | `/v1/lms/assignment-group-members/{assignmentGroupMemberId}` | `lms.assignment_group_member.update` | served |  |
| `lms.DeleteAssignmentGroupMember` | DELETE | `/v1/lms/assignment-group-members/{assignmentGroupMemberId}` | `lms.assignment_group_member.delete` | served |  |
| `lms.ListPeerReviews` | GET | `/v1/lms/peer-reviews` | `lms.peer_review.read` | served |  |
| `lms.CreatePeerReview` | POST | `/v1/lms/peer-reviews` | `lms.peer_review.create` | served |  |
| `lms.GetPeerReview` | GET | `/v1/lms/peer-reviews/{peerReviewId}` | `lms.peer_review.read` | served |  |
| `lms.UpdatePeerReview` | PATCH | `/v1/lms/peer-reviews/{peerReviewId}` | `lms.peer_review.update` | served |  |
| `lms.DeletePeerReview` | DELETE | `/v1/lms/peer-reviews/{peerReviewId}` | `lms.peer_review.delete` | served |  |
| `lms.ListRubrics` | GET | `/v1/lms/rubrics` | `lms.rubric.read` | served |  |
| `lms.CreateRubric` | POST | `/v1/lms/rubrics` | `lms.rubric.create` | served |  |
| `lms.GetRubric` | GET | `/v1/lms/rubrics/{rubricId}` | `lms.rubric.read` | served |  |
| `lms.UpdateRubric` | PATCH | `/v1/lms/rubrics/{rubricId}` | `lms.rubric.update` | served |  |
| `lms.DeleteRubric` | DELETE | `/v1/lms/rubrics/{rubricId}` | `lms.rubric.delete` | served |  |
| `lms.ListRubricCriteria` | GET | `/v1/lms/rubrics/{rubricId}/criteria` | `lms.rubric_criterion.read` | served |  |
| `lms.CreateRubricCriterion` | POST | `/v1/lms/rubrics/{rubricId}/criteria` | `lms.rubric_criterion.create` | served |  |
| `lms.GetRubricCriterion` | GET | `/v1/lms/rubrics/{rubricId}/criteria/{rubricCriterionId}` | `lms.rubric_criterion.read` | served |  |
| `lms.UpdateRubricCriterion` | PATCH | `/v1/lms/rubrics/{rubricId}/criteria/{rubricCriterionId}` | `lms.rubric_criterion.update` | served |  |
| `lms.DeleteRubricCriterion` | DELETE | `/v1/lms/rubrics/{rubricId}/criteria/{rubricCriterionId}` | `lms.rubric_criterion.delete` | served |  |
| `lms.ListRubricLevels` | GET | `/v1/lms/rubrics/{rubricId}/levels` | `lms.rubric_level.read` | served |  |
| `lms.CreateRubricLevel` | POST | `/v1/lms/rubrics/{rubricId}/levels` | `lms.rubric_level.create` | served |  |
| `lms.GetRubricLevel` | GET | `/v1/lms/rubrics/{rubricId}/levels/{rubricLevelId}` | `lms.rubric_level.read` | served |  |
| `lms.UpdateRubricLevel` | PATCH | `/v1/lms/rubrics/{rubricId}/levels/{rubricLevelId}` | `lms.rubric_level.update` | served |  |
| `lms.DeleteRubricLevel` | DELETE | `/v1/lms/rubrics/{rubricId}/levels/{rubricLevelId}` | `lms.rubric_level.delete` | served |  |
| `lms.ListRubricCells` | GET | `/v1/lms/rubrics/{rubricId}/cells` | `lms.rubric_cell.read` | served |  |
| `lms.CreateRubricCell` | POST | `/v1/lms/rubrics/{rubricId}/cells` | `lms.rubric_cell.create` | served |  |
| `lms.GetRubricCell` | GET | `/v1/lms/rubrics/{rubricId}/cells/{rubricCellId}` | `lms.rubric_cell.read` | served |  |
| `lms.UpdateRubricCell` | PATCH | `/v1/lms/rubrics/{rubricId}/cells/{rubricCellId}` | `lms.rubric_cell.update` | served |  |
| `lms.DeleteRubricCell` | DELETE | `/v1/lms/rubrics/{rubricId}/cells/{rubricCellId}` | `lms.rubric_cell.delete` | served |  |
| `lms.ListRubricAssessments` | GET | `/v1/lms/rubric-assessments` | `lms.rubric_assessment.read` | served |  |
| `lms.CreateRubricAssessment` | POST | `/v1/lms/rubric-assessments` | `lms.rubric_assessment.create` | served |  |
| `lms.GetRubricAssessment` | GET | `/v1/lms/rubric-assessments/{rubricAssessmentId}` | `lms.rubric_assessment.read` | served |  |
| `lms.UpdateRubricAssessment` | PATCH | `/v1/lms/rubric-assessments/{rubricAssessmentId}` | `lms.rubric_assessment.update` | served |  |
| `lms.DeleteRubricAssessment` | DELETE | `/v1/lms/rubric-assessments/{rubricAssessmentId}` | `lms.rubric_assessment.delete` | served |  |
| `lms.ListDiscussionPrompts` | GET | `/v1/lms/discussion-prompts` | `lms.discussion_prompt.read` | served |  |
| `lms.CreateDiscussionPrompt` | POST | `/v1/lms/discussion-prompts` | `lms.discussion_prompt.create` | served |  |
| `lms.GetDiscussionPrompt` | GET | `/v1/lms/discussion-prompts/{discussionPromptId}` | `lms.discussion_prompt.read` | served |  |
| `lms.UpdateDiscussionPrompt` | PATCH | `/v1/lms/discussion-prompts/{discussionPromptId}` | `lms.discussion_prompt.update` | served |  |
| `lms.DeleteDiscussionPrompt` | DELETE | `/v1/lms/discussion-prompts/{discussionPromptId}` | `lms.discussion_prompt.delete` | served |  |
| `lms.ListParticipationActivities` | GET | `/v1/lms/participation-activities` | `lms.participation_activity.read` | served |  |
| `lms.CreateParticipationActivity` | POST | `/v1/lms/participation-activities` | `lms.participation_activity.create` | served |  |
| `lms.GetParticipationActivity` | GET | `/v1/lms/participation-activities/{participationActivityId}` | `lms.participation_activity.read` | served |  |
| `lms.UpdateParticipationActivity` | PATCH | `/v1/lms/participation-activities/{participationActivityId}` | `lms.participation_activity.update` | served |  |
| `lms.DeleteParticipationActivity` | DELETE | `/v1/lms/participation-activities/{participationActivityId}` | `lms.participation_activity.delete` | served |  |
| `lms.ListResourceCategories` | GET | `/v1/lms/resource-categories` | `lms.resource_category.read` | served |  |
| `lms.CreateResourceCategory` | POST | `/v1/lms/resource-categories` | `lms.resource_category.create` | served |  |
| `lms.GetResourceCategory` | GET | `/v1/lms/resource-categories/{resourceCategoryId}` | `lms.resource_category.read` | served |  |
| `lms.UpdateResourceCategory` | PATCH | `/v1/lms/resource-categories/{resourceCategoryId}` | `lms.resource_category.update` | served |  |
| `lms.DeleteResourceCategory` | DELETE | `/v1/lms/resource-categories/{resourceCategoryId}` | `lms.resource_category.delete` | served |  |
| `lms.ListWebResources` | GET | `/v1/lms/web-resources` | `lms.web_resource.read` | served |  |
| `lms.CreateWebResource` | POST | `/v1/lms/web-resources` | `lms.web_resource.create` | served |  |
| `lms.GetWebResource` | GET | `/v1/lms/web-resources/{webResourceId}` | `lms.web_resource.read` | served |  |
| `lms.UpdateWebResource` | PATCH | `/v1/lms/web-resources/{webResourceId}` | `lms.web_resource.update` | served |  |
| `lms.DeleteWebResource` | DELETE | `/v1/lms/web-resources/{webResourceId}` | `lms.web_resource.delete` | served |  |
| `lms.ListEbooks` | GET | `/v1/lms/ebooks` | `lms.ebook.read` | served |  |
| `lms.CreateEbook` | POST | `/v1/lms/ebooks` | `lms.ebook.create` | served |  |
| `lms.GetEbook` | GET | `/v1/lms/ebooks/{ebookId}` | `lms.ebook.read` | served |  |
| `lms.UpdateEbook` | PATCH | `/v1/lms/ebooks/{ebookId}` | `lms.ebook.update` | served |  |
| `lms.DeleteEbook` | DELETE | `/v1/lms/ebooks/{ebookId}` | `lms.ebook.delete` | served |  |
| `lms.ListRelatedMaterials` | GET | `/v1/lms/ebooks/{ebookId}/related-materials` | `lms.related_material.read` | served |  |
| `lms.CreateRelatedMaterial` | POST | `/v1/lms/ebooks/{ebookId}/related-materials` | `lms.related_material.create` | served |  |
| `lms.GetRelatedMaterial` | GET | `/v1/lms/ebooks/{ebookId}/related-materials/{relatedMaterialId}` | `lms.related_material.read` | served |  |
| `lms.UpdateRelatedMaterial` | PATCH | `/v1/lms/ebooks/{ebookId}/related-materials/{relatedMaterialId}` | `lms.related_material.update` | served |  |
| `lms.DeleteRelatedMaterial` | DELETE | `/v1/lms/ebooks/{ebookId}/related-materials/{relatedMaterialId}` | `lms.related_material.delete` | served |  |
| `lms.ListRelatedMaterialRanks` | GET | `/v1/lms/ebooks/{ebookId}/related-material-ranks` | `lms.related_material_rank.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.GetRelatedMaterialRank` | GET | `/v1/lms/ebooks/{ebookId}/related-material-ranks/{relatedMaterialRankId}` | `lms.related_material_rank.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ListAnnotations` | GET | `/v1/lms/annotations` | `lms.annotation.read` | served |  |
| `lms.CreateAnnotation` | POST | `/v1/lms/annotations` | `lms.annotation.create` | served |  |
| `lms.GetAnnotation` | GET | `/v1/lms/annotations/{annotationId}` | `lms.annotation.read` | served |  |
| `lms.UpdateAnnotation` | PATCH | `/v1/lms/annotations/{annotationId}` | `lms.annotation.update` | served |  |
| `lms.DeleteAnnotation` | DELETE | `/v1/lms/annotations/{annotationId}` | `lms.annotation.delete` | served |  |
| `lms.ListAnnotationShares` | GET | `/v1/lms/annotations/{annotationId}/shares` | `lms.annotation_share.read` | served |  |
| `lms.CreateAnnotationShare` | POST | `/v1/lms/annotations/{annotationId}/shares` | `lms.annotation_share.create` | served |  |
| `lms.GetAnnotationShare` | GET | `/v1/lms/annotations/{annotationId}/shares/{annotationShareId}` | `lms.annotation_share.read` | served |  |
| `lms.UpdateAnnotationShare` | PATCH | `/v1/lms/annotations/{annotationId}/shares/{annotationShareId}` | `lms.annotation_share.update` | served |  |
| `lms.DeleteAnnotationShare` | DELETE | `/v1/lms/annotations/{annotationId}/shares/{annotationShareId}` | `lms.annotation_share.delete` | served |  |
| `lms.ListExternalTools` | GET | `/v1/lms/external-tools` | `lms.external_tool.read` | served |  |
| `lms.CreateExternalTool` | POST | `/v1/lms/external-tools` | `lms.external_tool.create` | served |  |
| `lms.GetExternalTool` | GET | `/v1/lms/external-tools/{externalToolId}` | `lms.external_tool.read` | served |  |
| `lms.UpdateExternalTool` | PATCH | `/v1/lms/external-tools/{externalToolId}` | `lms.external_tool.update` | served |  |
| `lms.DeleteExternalTool` | DELETE | `/v1/lms/external-tools/{externalToolId}` | `lms.external_tool.delete` | served |  |
| `lms.ListExternalToolSettings` | GET | `/v1/lms/external-tools/{externalToolId}/settings` | `lms.external_tool_setting.read` | served |  |
| `lms.CreateExternalToolSetting` | POST | `/v1/lms/external-tools/{externalToolId}/settings` | `lms.external_tool_setting.create` | served |  |
| `lms.GetExternalToolSetting` | GET | `/v1/lms/external-tools/{externalToolId}/settings/{externalToolSettingId}` | `lms.external_tool_setting.read` | served |  |
| `lms.UpdateExternalToolSetting` | PATCH | `/v1/lms/external-tools/{externalToolId}/settings/{externalToolSettingId}` | `lms.external_tool_setting.update` | served |  |
| `lms.DeleteExternalToolSetting` | DELETE | `/v1/lms/external-tools/{externalToolId}/settings/{externalToolSettingId}` | `lms.external_tool_setting.delete` | served |  |
| `lms.ListExternalToolInstitutions` | GET | `/v1/lms/external-tools/{externalToolId}/institutions` | `lms.external_tool_institution.read` | served |  |
| `lms.CreateExternalToolInstitution` | POST | `/v1/lms/external-tools/{externalToolId}/institutions` | `lms.external_tool_institution.create` | served |  |
| `lms.GetExternalToolInstitution` | GET | `/v1/lms/external-tools/{externalToolId}/institutions/{externalToolInstitutionId}` | `lms.external_tool_institution.read` | served |  |
| `lms.UpdateExternalToolInstitution` | PATCH | `/v1/lms/external-tools/{externalToolId}/institutions/{externalToolInstitutionId}` | `lms.external_tool_institution.update` | served |  |
| `lms.DeleteExternalToolInstitution` | DELETE | `/v1/lms/external-tools/{externalToolId}/institutions/{externalToolInstitutionId}` | `lms.external_tool_institution.delete` | served |  |
| `lms.ListExternalToolRoles` | GET | `/v1/lms/external-tools/{externalToolId}/roles` | `lms.external_tool_role.read` | served |  |
| `lms.CreateExternalToolRole` | POST | `/v1/lms/external-tools/{externalToolId}/roles` | `lms.external_tool_role.create` | served |  |
| `lms.GetExternalToolRole` | GET | `/v1/lms/external-tools/{externalToolId}/roles/{externalToolRoleId}` | `lms.external_tool_role.read` | served |  |
| `lms.UpdateExternalToolRole` | PATCH | `/v1/lms/external-tools/{externalToolId}/roles/{externalToolRoleId}` | `lms.external_tool_role.update` | served |  |
| `lms.DeleteExternalToolRole` | DELETE | `/v1/lms/external-tools/{externalToolId}/roles/{externalToolRoleId}` | `lms.external_tool_role.delete` | served |  |
| `lms.ListExternalToolOptions` | GET | `/v1/lms/external-tools/{externalToolId}/options` | `lms.external_tool_option.read` | served |  |
| `lms.CreateExternalToolOption` | POST | `/v1/lms/external-tools/{externalToolId}/options` | `lms.external_tool_option.create` | served |  |
| `lms.GetExternalToolOption` | GET | `/v1/lms/external-tools/{externalToolId}/options/{externalToolOptionId}` | `lms.external_tool_option.read` | served |  |
| `lms.UpdateExternalToolOption` | PATCH | `/v1/lms/external-tools/{externalToolId}/options/{externalToolOptionId}` | `lms.external_tool_option.update` | served |  |
| `lms.DeleteExternalToolOption` | DELETE | `/v1/lms/external-tools/{externalToolId}/options/{externalToolOptionId}` | `lms.external_tool_option.delete` | served |  |
| `lms.ListExternalToolResources` | GET | `/v1/lms/external-tool-resources` | `lms.external_tool_resource.read` | served |  |
| `lms.CreateExternalToolResource` | POST | `/v1/lms/external-tool-resources` | `lms.external_tool_resource.create` | served |  |
| `lms.GetExternalToolResource` | GET | `/v1/lms/external-tool-resources/{externalToolResourceId}` | `lms.external_tool_resource.read` | served |  |
| `lms.UpdateExternalToolResource` | PATCH | `/v1/lms/external-tool-resources/{externalToolResourceId}` | `lms.external_tool_resource.update` | served |  |
| `lms.DeleteExternalToolResource` | DELETE | `/v1/lms/external-tool-resources/{externalToolResourceId}` | `lms.external_tool_resource.delete` | served |  |
| `lms.ListExternalToolLinks` | GET | `/v1/lms/external-tool-resources/{externalToolResourceId}/links` | `lms.external_tool_link.read` | served |  |
| `lms.CreateExternalToolLink` | POST | `/v1/lms/external-tool-resources/{externalToolResourceId}/links` | `lms.external_tool_link.create` | served |  |
| `lms.GetExternalToolLink` | GET | `/v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId}` | `lms.external_tool_link.read` | served |  |
| `lms.UpdateExternalToolLink` | PATCH | `/v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId}` | `lms.external_tool_link.update` | served |  |
| `lms.DeleteExternalToolLink` | DELETE | `/v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId}` | `lms.external_tool_link.delete` | served |  |
| `lms.ListForums` | GET | `/v1/lms/cohorts/{cohortId}/forums` | `lms.forum.read` | served |  |
| `lms.CreateForum` | POST | `/v1/lms/cohorts/{cohortId}/forums` | `lms.forum.create` | served |  |
| `lms.GetForum` | GET | `/v1/lms/cohorts/{cohortId}/forums/{forumId}` | `lms.forum.read` | served |  |
| `lms.UpdateForum` | PATCH | `/v1/lms/cohorts/{cohortId}/forums/{forumId}` | `lms.forum.update` | served |  |
| `lms.DeleteForum` | DELETE | `/v1/lms/cohorts/{cohortId}/forums/{forumId}` | `lms.forum.delete` | served |  |
| `lms.ListForumSections` | GET | `/v1/lms/forum-sections` | `lms.forum_section.read` | served |  |
| `lms.CreateForumSection` | POST | `/v1/lms/forum-sections` | `lms.forum_section.create` | served |  |
| `lms.GetForumSection` | GET | `/v1/lms/forum-sections/{forumSectionId}` | `lms.forum_section.read` | served |  |
| `lms.UpdateForumSection` | PATCH | `/v1/lms/forum-sections/{forumSectionId}` | `lms.forum_section.update` | served |  |
| `lms.DeleteForumSection` | DELETE | `/v1/lms/forum-sections/{forumSectionId}` | `lms.forum_section.delete` | served |  |
| `lms.ListForumTopics` | GET | `/v1/lms/forum-topics` | `lms.forum_topic.read` | served |  |
| `lms.CreateForumTopic` | POST | `/v1/lms/forum-topics` | `lms.forum_topic.create` | served |  |
| `lms.GetForumTopic` | GET | `/v1/lms/forum-topics/{forumTopicId}` | `lms.forum_topic.read` | served |  |
| `lms.UpdateForumTopic` | PATCH | `/v1/lms/forum-topics/{forumTopicId}` | `lms.forum_topic.update` | served |  |
| `lms.DeleteForumTopic` | DELETE | `/v1/lms/forum-topics/{forumTopicId}` | `lms.forum_topic.delete` | served |  |
| `lms.ListForumPosts` | GET | `/v1/lms/forum-topics/{forumTopicId}/posts` | `lms.forum_post.read` | served |  |
| `lms.CreateForumPost` | POST | `/v1/lms/forum-topics/{forumTopicId}/posts` | `lms.forum_post.create` | served |  |
| `lms.GetForumPost` | GET | `/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}` | `lms.forum_post.read` | served |  |
| `lms.UpdateForumPost` | PATCH | `/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}` | `lms.forum_post.update` | served |  |
| `lms.DeleteForumPost` | DELETE | `/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}` | `lms.forum_post.delete` | served |  |
| `lms.ListForumPostVotes` | GET | `/v1/lms/forum-post-votes` | `lms.forum_post_vote.read` | served |  |
| `lms.CreateForumPostVote` | POST | `/v1/lms/forum-post-votes` | `lms.forum_post_vote.create` | served |  |
| `lms.UpdateForumPostVote` | PATCH | `/v1/lms/forum-post-votes/{forumPostVoteId}` | `lms.forum_post_vote.update` | served |  |
| `lms.DeleteForumPostVote` | DELETE | `/v1/lms/forum-post-votes/{forumPostVoteId}` | `lms.forum_post_vote.delete` | served |  |
| `lms.ListForumReportCategories` | GET | `/v1/lms/forum-report-categories` | `lms.forum_report_category.read` | served |  |
| `lms.CreateForumReportCategory` | POST | `/v1/lms/forum-report-categories` | `lms.forum_report_category.create` | served |  |
| `lms.GetForumReportCategory` | GET | `/v1/lms/forum-report-categories/{forumReportCategoryId}` | `lms.forum_report_category.read` | served |  |
| `lms.UpdateForumReportCategory` | PATCH | `/v1/lms/forum-report-categories/{forumReportCategoryId}` | `lms.forum_report_category.update` | served |  |
| `lms.DeleteForumReportCategory` | DELETE | `/v1/lms/forum-report-categories/{forumReportCategoryId}` | `lms.forum_report_category.delete` | served |  |
| `lms.ListForumPostReports` | GET | `/v1/lms/forum-post-reports` | `lms.forum_post_report.read` | served |  |
| `lms.CreateForumPostReport` | POST | `/v1/lms/forum-post-reports` | `lms.forum_post_report.create` | served |  |
| `lms.GetForumPostReport` | GET | `/v1/lms/forum-post-reports/{forumPostReportId}` | `lms.forum_post_report.read` | served |  |
| `lms.UpdateForumPostReport` | PATCH | `/v1/lms/forum-post-reports/{forumPostReportId}` | `lms.forum_post_report.update` | served |  |
| `lms.DeleteForumPostReport` | DELETE | `/v1/lms/forum-post-reports/{forumPostReportId}` | `lms.forum_post_report.delete` | served |  |
| `lms.ListForumReadStates` | GET | `/v1/lms/forum-topics/{forumTopicId}/read-states` | `lms.forum_read_state.read` | served |  |
| `lms.ListCohortGroups` | GET | `/v1/lms/cohorts/{cohortId}/groups` | `lms.cohort_group.read` | served |  |
| `lms.CreateCohortGroup` | POST | `/v1/lms/cohorts/{cohortId}/groups` | `lms.cohort_group.create` | served |  |
| `lms.GetCohortGroup` | GET | `/v1/lms/cohorts/{cohortId}/groups/{cohortGroupId}` | `lms.cohort_group.read` | served |  |
| `lms.UpdateCohortGroup` | PATCH | `/v1/lms/cohorts/{cohortId}/groups/{cohortGroupId}` | `lms.cohort_group.update` | served |  |
| `lms.DeleteCohortGroup` | DELETE | `/v1/lms/cohorts/{cohortId}/groups/{cohortGroupId}` | `lms.cohort_group.delete` | served |  |
| `lms.ListCohortScheduleSlots` | GET | `/v1/lms/cohorts/{cohortId}/schedule-slots` | `lms.cohort_schedule_slot.read` | served |  |
| `lms.CreateCohortScheduleSlot` | POST | `/v1/lms/cohorts/{cohortId}/schedule-slots` | `lms.cohort_schedule_slot.create` | served |  |
| `lms.GetCohortScheduleSlot` | GET | `/v1/lms/cohorts/{cohortId}/schedule-slots/{cohortScheduleSlotId}` | `lms.cohort_schedule_slot.read` | served |  |
| `lms.UpdateCohortScheduleSlot` | PATCH | `/v1/lms/cohorts/{cohortId}/schedule-slots/{cohortScheduleSlotId}` | `lms.cohort_schedule_slot.update` | served |  |
| `lms.DeleteCohortScheduleSlot` | DELETE | `/v1/lms/cohorts/{cohortId}/schedule-slots/{cohortScheduleSlotId}` | `lms.cohort_schedule_slot.delete` | served |  |
| `lms.ListCohortSessions` | GET | `/v1/lms/cohorts/{cohortId}/sessions` | `lms.cohort_session.read` | served |  |
| `lms.CreateCohortSession` | POST | `/v1/lms/cohorts/{cohortId}/sessions` | `lms.cohort_session.create` | served |  |
| `lms.GetCohortSession` | GET | `/v1/lms/cohorts/{cohortId}/sessions/{cohortSessionId}` | `lms.cohort_session.read` | served |  |
| `lms.UpdateCohortSession` | PATCH | `/v1/lms/cohorts/{cohortId}/sessions/{cohortSessionId}` | `lms.cohort_session.update` | served |  |
| `lms.DeleteCohortSession` | DELETE | `/v1/lms/cohorts/{cohortId}/sessions/{cohortSessionId}` | `lms.cohort_session.delete` | served |  |
| `lms.ListCohortHolidays` | GET | `/v1/lms/cohorts/{cohortId}/holidays` | `lms.cohort_holiday.read` | served |  |
| `lms.CreateCohortHoliday` | POST | `/v1/lms/cohorts/{cohortId}/holidays` | `lms.cohort_holiday.create` | served |  |
| `lms.GetCohortHoliday` | GET | `/v1/lms/cohorts/{cohortId}/holidays/{cohortHolidayId}` | `lms.cohort_holiday.read` | served |  |
| `lms.UpdateCohortHoliday` | PATCH | `/v1/lms/cohorts/{cohortId}/holidays/{cohortHolidayId}` | `lms.cohort_holiday.update` | served |  |
| `lms.DeleteCohortHoliday` | DELETE | `/v1/lms/cohorts/{cohortId}/holidays/{cohortHolidayId}` | `lms.cohort_holiday.delete` | served |  |
| `lms.ListCourseAuthorProfiles` | GET | `/v1/lms/courses/{courseId}/author-profiles` | `lms.course_author_profile.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateCourseAuthorProfile` | POST | `/v1/lms/courses/{courseId}/author-profiles` | `lms.course_author_profile.create` | served |  |
| `lms.GetCourseAuthorProfile` | GET | `/v1/lms/courses/{courseId}/author-profiles/{courseAuthorProfileId}` | `lms.course_author_profile.read` | served |  |
| `lms.UpdateCourseAuthorProfile` | PATCH | `/v1/lms/courses/{courseId}/author-profiles/{courseAuthorProfileId}` | `lms.course_author_profile.update` | served |  |
| `lms.DeleteCourseAuthorProfile` | DELETE | `/v1/lms/courses/{courseId}/author-profiles/{courseAuthorProfileId}` | `lms.course_author_profile.delete` | served |  |
| `lms.ListCoursePreviews` | GET | `/v1/lms/courses/{courseId}/previews` | `lms.course_preview.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateCoursePreview` | POST | `/v1/lms/courses/{courseId}/previews` | `lms.course_preview.create` | served |  |
| `lms.GetCoursePreview` | GET | `/v1/lms/courses/{courseId}/previews/{coursePreviewId}` | `lms.course_preview.read` | served |  |
| `lms.UpdateCoursePreview` | PATCH | `/v1/lms/courses/{courseId}/previews/{coursePreviewId}` | `lms.course_preview.update` | served |  |
| `lms.DeleteCoursePreview` | DELETE | `/v1/lms/courses/{courseId}/previews/{coursePreviewId}` | `lms.course_preview.delete` | served |  |
| `lms.ListCourseTestimonials` | GET | `/v1/lms/courses/{courseId}/testimonials` | `lms.course_testimonial.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateCourseTestimonial` | POST | `/v1/lms/courses/{courseId}/testimonials` | `lms.course_testimonial.create` | served |  |
| `lms.GetCourseTestimonial` | GET | `/v1/lms/courses/{courseId}/testimonials/{courseTestimonialId}` | `lms.course_testimonial.read` | served |  |
| `lms.UpdateCourseTestimonial` | PATCH | `/v1/lms/courses/{courseId}/testimonials/{courseTestimonialId}` | `lms.course_testimonial.update` | served |  |
| `lms.DeleteCourseTestimonial` | DELETE | `/v1/lms/courses/{courseId}/testimonials/{courseTestimonialId}` | `lms.course_testimonial.delete` | served |  |
| `lms.ListCatalogBanners` | GET | `/v1/lms/courses/{courseId}/banners` | `lms.catalog_banner.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateCatalogBanner` | POST | `/v1/lms/courses/{courseId}/banners` | `lms.catalog_banner.create` | served |  |
| `lms.GetCatalogBanner` | GET | `/v1/lms/courses/{courseId}/banners/{catalogBannerId}` | `lms.catalog_banner.read` | served |  |
| `lms.UpdateCatalogBanner` | PATCH | `/v1/lms/courses/{courseId}/banners/{catalogBannerId}` | `lms.catalog_banner.update` | served |  |
| `lms.DeleteCatalogBanner` | DELETE | `/v1/lms/courses/{courseId}/banners/{catalogBannerId}` | `lms.catalog_banner.delete` | served |  |
| `lms.ListEnrollmentRequests` | GET | `/v1/lms/courses/{courseId}/enrollment-requests` | `lms.enrollment_request.read` | served |  |
| `lms.CreateEnrollmentRequest` | POST | `/v1/lms/courses/{courseId}/enrollment-requests` | `lms.enrollment_request.create` | served |  |
| `lms.GetEnrollmentRequest` | GET | `/v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}` | `lms.enrollment_request.read` | served |  |
| `lms.UpdateEnrollmentRequest` | PATCH | `/v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}` | `lms.enrollment_request.update` | served |  |
| `lms.DeleteEnrollmentRequest` | DELETE | `/v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}` | `lms.enrollment_request.delete` | served |  |
| `lms.ListReleaseRules` | GET | `/v1/lms/release-rules` | `lms.release_rule.read` | served |  |
| `lms.CreateReleaseRule` | POST | `/v1/lms/release-rules` | `lms.release_rule.create` | served |  |
| `lms.GetReleaseRule` | GET | `/v1/lms/release-rules/{releaseRuleId}` | `lms.release_rule.read` | served |  |
| `lms.UpdateReleaseRule` | PATCH | `/v1/lms/release-rules/{releaseRuleId}` | `lms.release_rule.update` | served |  |
| `lms.DeleteReleaseRule` | DELETE | `/v1/lms/release-rules/{releaseRuleId}` | `lms.release_rule.delete` | served |  |
| `lms.ListReleaseRuleConditions` | GET | `/v1/lms/release-rules/{releaseRuleId}/release-rule-conditions` | `lms.release_rule_condition.read` | served |  |
| `lms.CreateReleaseRuleCondition` | POST | `/v1/lms/release-rules/{releaseRuleId}/release-rule-conditions` | `lms.release_rule_condition.create` | served |  |
| `lms.GetReleaseRuleCondition` | GET | `/v1/lms/release-rules/{releaseRuleId}/release-rule-conditions/{releaseRuleConditionId}` | `lms.release_rule_condition.read` | served |  |
| `lms.UpdateReleaseRuleCondition` | PATCH | `/v1/lms/release-rules/{releaseRuleId}/release-rule-conditions/{releaseRuleConditionId}` | `lms.release_rule_condition.update` | served |  |
| `lms.DeleteReleaseRuleCondition` | DELETE | `/v1/lms/release-rules/{releaseRuleId}/release-rule-conditions/{releaseRuleConditionId}` | `lms.release_rule_condition.delete` | served |  |
| `lms.ListReleaseRuleTargets` | GET | `/v1/lms/release-rules/{releaseRuleId}/release-rule-targets` | `lms.release_rule_target.read` | served |  |
| `lms.CreateReleaseRuleTarget` | POST | `/v1/lms/release-rules/{releaseRuleId}/release-rule-targets` | `lms.release_rule_target.create` | served |  |
| `lms.GetReleaseRuleTarget` | GET | `/v1/lms/release-rules/{releaseRuleId}/release-rule-targets/{releaseRuleTargetId}` | `lms.release_rule_target.read` | served |  |
| `lms.UpdateReleaseRuleTarget` | PATCH | `/v1/lms/release-rules/{releaseRuleId}/release-rule-targets/{releaseRuleTargetId}` | `lms.release_rule_target.update` | served |  |
| `lms.DeleteReleaseRuleTarget` | DELETE | `/v1/lms/release-rules/{releaseRuleId}/release-rule-targets/{releaseRuleTargetId}` | `lms.release_rule_target.delete` | served |  |
| `lms.ListReleaseGrants` | GET | `/v1/lms/content-items/{contentItemId}/release-grants` | `lms.release_grant.read` | served |  |
| `lms.GetReleaseGrant` | GET | `/v1/lms/content-items/{contentItemId}/release-grants/{releaseGrantId}` | `lms.release_grant.read` | served |  |
| `lms.DeleteReleaseGrant` | DELETE | `/v1/lms/content-items/{contentItemId}/release-grants/{releaseGrantId}` | `lms.release_grant.delete` | served |  |
| `lms.ListStudentAlerts` | GET | `/v1/lms/student-alerts` | `lms.student_alert.read` | served |  |
| `lms.CreateStudentAlert` | POST | `/v1/lms/student-alerts` | `lms.student_alert.create` | served |  |
| `lms.GetStudentAlert` | GET | `/v1/lms/student-alerts/{studentAlertId}` | `lms.student_alert.read` | served |  |
| `lms.UpdateStudentAlert` | PATCH | `/v1/lms/student-alerts/{studentAlertId}` | `lms.student_alert.update` | served |  |
| `lms.DeleteStudentAlert` | DELETE | `/v1/lms/student-alerts/{studentAlertId}` | `lms.student_alert.delete` | served |  |
| `lms.ListContentViewSessions` | GET | `/v1/lms/content-view-sessions` | `lms.content_view_session.read` | served |  |
| `lms.CreateContentViewSession` | POST | `/v1/lms/content-view-sessions` | `lms.content_view_session.create` | served |  |
| `lms.GetContentViewSession` | GET | `/v1/lms/content-view-sessions/{contentViewSessionId}` | `lms.content_view_session.read` | served |  |
| `lms.UpdateContentViewSession` | PATCH | `/v1/lms/content-view-sessions/{contentViewSessionId}` | `lms.content_view_session.update` | served |  |
| `lms.ListBookmarks` | GET | `/v1/lms/bookmarks` | `lms.bookmark.read` | served |  |
| `lms.CreateBookmark` | POST | `/v1/lms/bookmarks` | `lms.bookmark.create` | served |  |
| `lms.GetBookmark` | GET | `/v1/lms/bookmarks/{bookmarkId}` | `lms.bookmark.read` | served |  |
| `lms.UpdateBookmark` | PATCH | `/v1/lms/bookmarks/{bookmarkId}` | `lms.bookmark.update` | served |  |
| `lms.DeleteBookmark` | DELETE | `/v1/lms/bookmarks/{bookmarkId}` | `lms.bookmark.delete` | served |  |
| `lms.ListMentorships` | GET | `/v1/lms/mentorships` | `lms.mentorship.read` | served |  |
| `lms.CreateMentorship` | POST | `/v1/lms/mentorships` | `lms.mentorship.create` | served |  |
| `lms.GetMentorship` | GET | `/v1/lms/mentorships/{mentorshipId}` | `lms.mentorship.read` | served |  |
| `lms.UpdateMentorship` | PATCH | `/v1/lms/mentorships/{mentorshipId}` | `lms.mentorship.update` | served |  |
| `lms.DeleteMentorship` | DELETE | `/v1/lms/mentorships/{mentorshipId}` | `lms.mentorship.delete` | served |  |
| `lms.ListPlagiarismAccounts` | GET | `/v1/lms/plagiarism-accounts` | `lms.plagiarism_account.read` | served |  |
| `lms.CreatePlagiarismAccount` | POST | `/v1/lms/plagiarism-accounts` | `lms.plagiarism_account.create` | served |  |
| `lms.GetPlagiarismAccount` | GET | `/v1/lms/plagiarism-accounts/{plagiarismAccountId}` | `lms.plagiarism_account.read` | served |  |
| `lms.UpdatePlagiarismAccount` | PATCH | `/v1/lms/plagiarism-accounts/{plagiarismAccountId}` | `lms.plagiarism_account.update` | served |  |
| `lms.DeletePlagiarismAccount` | DELETE | `/v1/lms/plagiarism-accounts/{plagiarismAccountId}` | `lms.plagiarism_account.delete` | served |  |
| `lms.ListPlagiarismCohortLinks` | GET | `/v1/lms/cohorts/{cohortId}/plagiarism-links` | `lms.plagiarism_cohort_link.read` | served |  |
| `lms.CreatePlagiarismCohortLink` | POST | `/v1/lms/cohorts/{cohortId}/plagiarism-links` | `lms.plagiarism_cohort_link.create` | served |  |
| `lms.GetPlagiarismCohortLink` | GET | `/v1/lms/cohorts/{cohortId}/plagiarism-links/{plagiarismCohortLinkId}` | `lms.plagiarism_cohort_link.read` | served |  |
| `lms.UpdatePlagiarismCohortLink` | PATCH | `/v1/lms/cohorts/{cohortId}/plagiarism-links/{plagiarismCohortLinkId}` | `lms.plagiarism_cohort_link.update` | served |  |
| `lms.DeletePlagiarismCohortLink` | DELETE | `/v1/lms/cohorts/{cohortId}/plagiarism-links/{plagiarismCohortLinkId}` | `lms.plagiarism_cohort_link.delete` | served |  |
| `lms.ListPlagiarismUserLinks` | GET | `/v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links` | `lms.plagiarism_user_link.read` | served |  |
| `lms.CreatePlagiarismUserLink` | POST | `/v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links` | `lms.plagiarism_user_link.create` | served |  |
| `lms.GetPlagiarismUserLink` | GET | `/v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links/{plagiarismUserLinkId}` | `lms.plagiarism_user_link.read` | served |  |
| `lms.UpdatePlagiarismUserLink` | PATCH | `/v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links/{plagiarismUserLinkId}` | `lms.plagiarism_user_link.update` | served |  |
| `lms.DeletePlagiarismUserLink` | DELETE | `/v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links/{plagiarismUserLinkId}` | `lms.plagiarism_user_link.delete` | served |  |
| `lms.ListPlagiarismReports` | GET | `/v1/lms/plagiarism-reports` | `lms.plagiarism_report.read` | served |  |
| `lms.GetPlagiarismReport` | GET | `/v1/lms/plagiarism-reports/{plagiarismReportId}` | `lms.plagiarism_report.read` | served |  |
| `lms.ListQuizQuestionHistory` | GET | `/v1/lms/quizzes/{quizId}/question-history` | `lms.quiz_question_history.read` | served |  |
| `lms.GetQuizQuestionHistory` | GET | `/v1/lms/quizzes/{quizId}/question-history/{quizQuestionHistoryId}` | `lms.quiz_question_history.read` | served |  |
| `lms.ListQuestionBlanks` | GET | `/v1/lms/questions/{questionId}/blanks` | `lms.question_blank.read` | served |  |
| `lms.CreateQuestionBlank` | POST | `/v1/lms/questions/{questionId}/blanks` | `lms.question_blank.create` | served |  |
| `lms.GetQuestionBlank` | GET | `/v1/lms/questions/{questionId}/blanks/{questionBlankId}` | `lms.question_blank.read` | served |  |
| `lms.UpdateQuestionBlank` | PATCH | `/v1/lms/questions/{questionId}/blanks/{questionBlankId}` | `lms.question_blank.update` | served |  |
| `lms.DeleteQuestionBlank` | DELETE | `/v1/lms/questions/{questionId}/blanks/{questionBlankId}` | `lms.question_blank.delete` | served |  |
| `lms.ListQuestionBlankAnswers` | GET | `/v1/lms/question-blank-answers` | `lms.question_blank_answer.read` | served |  |
| `lms.CreateQuestionBlankAnswer` | POST | `/v1/lms/question-blank-answers` | `lms.question_blank_answer.create` | served |  |
| `lms.GetQuestionBlankAnswer` | GET | `/v1/lms/question-blank-answers/{questionBlankAnswerId}` | `lms.question_blank_answer.read` | served |  |
| `lms.UpdateQuestionBlankAnswer` | PATCH | `/v1/lms/question-blank-answers/{questionBlankAnswerId}` | `lms.question_blank_answer.update` | served |  |
| `lms.DeleteQuestionBlankAnswer` | DELETE | `/v1/lms/question-blank-answers/{questionBlankAnswerId}` | `lms.question_blank_answer.delete` | served |  |
| `lms.ListQuestionHotspots` | GET | `/v1/lms/questions/{questionId}/hotspots` | `lms.question_hotspot.read` | served |  |
| `lms.CreateQuestionHotspot` | POST | `/v1/lms/questions/{questionId}/hotspots` | `lms.question_hotspot.create` | served |  |
| `lms.GetQuestionHotspot` | GET | `/v1/lms/questions/{questionId}/hotspots/{questionHotspotId}` | `lms.question_hotspot.read` | served |  |
| `lms.UpdateQuestionHotspot` | PATCH | `/v1/lms/questions/{questionId}/hotspots/{questionHotspotId}` | `lms.question_hotspot.update` | served |  |
| `lms.DeleteQuestionHotspot` | DELETE | `/v1/lms/questions/{questionId}/hotspots/{questionHotspotId}` | `lms.question_hotspot.delete` | served |  |
| `lms.ListQuestionHotspotRegions` | GET | `/v1/lms/question-hotspot-regions` | `lms.question_hotspot_region.read` | served |  |
| `lms.CreateQuestionHotspotRegion` | POST | `/v1/lms/question-hotspot-regions` | `lms.question_hotspot_region.create` | served |  |
| `lms.GetQuestionHotspotRegion` | GET | `/v1/lms/question-hotspot-regions/{questionHotspotRegionId}` | `lms.question_hotspot_region.read` | served |  |
| `lms.UpdateQuestionHotspotRegion` | PATCH | `/v1/lms/question-hotspot-regions/{questionHotspotRegionId}` | `lms.question_hotspot_region.update` | served |  |
| `lms.DeleteQuestionHotspotRegion` | DELETE | `/v1/lms/question-hotspot-regions/{questionHotspotRegionId}` | `lms.question_hotspot_region.delete` | served |  |
| `lms.ListQuestionMatchPairs` | GET | `/v1/lms/questions/{questionId}/match-pairs` | `lms.question_match_pair.read` | served |  |
| `lms.CreateQuestionMatchPair` | POST | `/v1/lms/questions/{questionId}/match-pairs` | `lms.question_match_pair.create` | served |  |
| `lms.GetQuestionMatchPair` | GET | `/v1/lms/questions/{questionId}/match-pairs/{questionMatchPairId}` | `lms.question_match_pair.read` | served |  |
| `lms.UpdateQuestionMatchPair` | PATCH | `/v1/lms/questions/{questionId}/match-pairs/{questionMatchPairId}` | `lms.question_match_pair.update` | served |  |
| `lms.DeleteQuestionMatchPair` | DELETE | `/v1/lms/questions/{questionId}/match-pairs/{questionMatchPairId}` | `lms.question_match_pair.delete` | served |  |
| `lms.ListQuestionPatterns` | GET | `/v1/lms/questions/{questionId}/patterns` | `lms.question_pattern.read` | served |  |
| `lms.CreateQuestionPattern` | POST | `/v1/lms/questions/{questionId}/patterns` | `lms.question_pattern.create` | served |  |
| `lms.GetQuestionPattern` | GET | `/v1/lms/questions/{questionId}/patterns/{questionPatternId}` | `lms.question_pattern.read` | served |  |
| `lms.UpdateQuestionPattern` | PATCH | `/v1/lms/questions/{questionId}/patterns/{questionPatternId}` | `lms.question_pattern.update` | served |  |
| `lms.DeleteQuestionPattern` | DELETE | `/v1/lms/questions/{questionId}/patterns/{questionPatternId}` | `lms.question_pattern.delete` | served |  |
| `lms.ListQuestionPools` | GET | `/v1/lms/question-pools` | `lms.question_pool.read` | served |  |
| `lms.CreateQuestionPool` | POST | `/v1/lms/question-pools` | `lms.question_pool.create` | served |  |
| `lms.GetQuestionPool` | GET | `/v1/lms/question-pools/{questionPoolId}` | `lms.question_pool.read` | served |  |
| `lms.UpdateQuestionPool` | PATCH | `/v1/lms/question-pools/{questionPoolId}` | `lms.question_pool.update` | served |  |
| `lms.DeleteQuestionPool` | DELETE | `/v1/lms/question-pools/{questionPoolId}` | `lms.question_pool.delete` | served |  |
| `lms.ListQuestionPoolItems` | GET | `/v1/lms/question-pools/{questionPoolId}/items` | `lms.question_pool_item.read` | served |  |
| `lms.CreateQuestionPoolItem` | POST | `/v1/lms/question-pools/{questionPoolId}/items` | `lms.question_pool_item.create` | served |  |
| `lms.GetQuestionPoolItem` | GET | `/v1/lms/question-pools/{questionPoolId}/items/{questionPoolItemId}` | `lms.question_pool_item.read` | served |  |
| `lms.UpdateQuestionPoolItem` | PATCH | `/v1/lms/question-pools/{questionPoolId}/items/{questionPoolItemId}` | `lms.question_pool_item.update` | served |  |
| `lms.DeleteQuestionPoolItem` | DELETE | `/v1/lms/question-pools/{questionPoolId}/items/{questionPoolItemId}` | `lms.question_pool_item.delete` | served |  |
| `lms.PublishCourse` | POST | `/v1/lms/courses/{courseId}:publish` | `lms.course.publish_course` | 501 | workflow / custom operation |
| `lms.UnpublishCourse` | POST | `/v1/lms/courses/{courseId}:unpublish` | `lms.course.unpublish_course` | 501 | workflow / custom operation |
| `lms.SubmitCourseForReview` | POST | `/v1/lms/courses/{courseId}:submitForReview` | `lms.course.submit_course_for_review` | 501 | workflow / custom operation |
| `lms.ApproveCourse` | POST | `/v1/lms/courses/{courseId}:approve` | `lms.course.approve_course` | 501 | workflow / custom operation |
| `lms.GetCourseOutline` | GET | `/v1/lms/courses/{courseId}:outline` | `lms.course.get_course_outline` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ReorderChapters` | POST | `/v1/lms/courses/{courseId}:reorderChapters` | `lms.course.reorder_chapters` | 501 | workflow / custom operation |
| `lms.EnrollInCourse` | POST | `/v1/lms/courses/{courseId}:enroll` | `lms.course.enroll_in_course` | 501 | workflow / custom operation |
| `lms.BatchEnrollInCourse` | POST | `/v1/lms/courses/{courseId}:batchEnroll` | `lms.course.batch_enroll_in_course` | 501 | workflow / custom operation |
| `lms.IssueMyCertificate` | POST | `/v1/lms/courses/{courseId}:issueMyCertificate` | `lms.course.issue_my_certificate` | 501 | workflow / custom operation |
| `lms.GetCourseProgressDistribution` | GET | `/v1/lms/courses/{courseId}:progressDistribution` | `lms.course.get_course_progress_distribution` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.GetEvaluatorSlots` | GET | `/v1/lms/courses/{courseId}:evaluatorSlots` | `lms.course.get_evaluator_slots` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ExportCourse` | POST | `/v1/lms/courses/{courseId}:export` | `lms.course.export_course` | 501 | workflow / custom operation |
| `lms.GetScormLaunch` | GET | `/v1/lms/chapters/{chapterId}:scormLaunch` | `lms.chapter.get_scorm_launch` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.SaveScormProgress` | POST | `/v1/lms/chapters/{chapterId}:saveScormProgress` | `lms.chapter.save_scorm_progress` | 501 | workflow / custom operation |
| `lms.GetLessonContent` | GET | `/v1/lms/lessons/{lessonId}:content` | `lms.lesson.get_lesson_content` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.MoveLesson` | POST | `/v1/lms/lessons/{lessonId}:move` | `lms.lesson.move_lesson` | 501 | workflow / custom operation |
| `lms.MarkLessonComplete` | POST | `/v1/lms/lessons/{lessonId}:markComplete` | `lms.lesson.mark_lesson_complete` | 501 | workflow / custom operation |
| `lms.TrackVideoWatch` | POST | `/v1/lms/lessons/{lessonId}:trackVideoWatch` | `lms.lesson.track_video_watch` | 501 | workflow / custom operation |
| `lms.StartQuizAttempt` | POST | `/v1/lms/content-items/{contentItemId}:startQuizAttempt` | `lms.quiz.start_quiz_attempt` | 501 | workflow / custom operation |
| `lms.CheckQuizAnswer` | POST | `/v1/lms/quizzes/{quizId}:checkAnswer` | `lms.quiz.check_quiz_answer` | 501 | workflow / custom operation |
| `lms.SubmitQuiz` | POST | `/v1/lms/quiz-submissions/{quizSubmissionId}:submit` | `lms.quiz.submit_quiz` | 501 | workflow / custom operation |
| `lms.SearchQuestionBank` | GET | `/v1/lms/questions:search` | `lms.question.search_question_bank` | 501 | workflow / custom operation |
| `lms.GradeLearningSubmission` | POST | `/v1/lms/assignment-submissions/{assignmentSubmissionId}:grade` | `lms.assignment_submission.grade_learning_submission` | 501 | workflow / custom operation |
| `lms.SubmitExercise` | POST | `/v1/lms/programming-exercises/{programmingExerciseId}:submit` | `lms.programming_exercise.submit_exercise` | 501 | workflow / custom operation |
| `lms.PublishCohort` | POST | `/v1/lms/cohorts/{cohortId}:publish` | `lms.cohort.publish_cohort` | 501 | workflow / custom operation |
| `lms.UnpublishCohort` | POST | `/v1/lms/cohorts/{cohortId}:unpublish` | `lms.cohort.unpublish_cohort` | 501 | workflow / custom operation |
| `lms.EnrollInCohort` | POST | `/v1/lms/cohorts/{cohortId}:enroll` | `lms.cohort.enroll_in_cohort` | 501 | workflow / custom operation |
| `lms.BatchEnrollInCohort` | POST | `/v1/lms/cohorts/{cohortId}:batchEnroll` | `lms.cohort.batch_enroll_in_cohort` | 501 | workflow / custom operation |
| `lms.GetCohortTimetable` | GET | `/v1/lms/cohorts/{cohortId}:timetable` | `lms.cohort.get_cohort_timetable` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ApplyTimetableTemplate` | POST | `/v1/lms/cohorts/{cohortId}:applyTimetableTemplate` | `lms.cohort.apply_timetable_template` | 501 | workflow / custom operation |
| `lms.SendCohortAnnouncement` | POST | `/v1/lms/cohorts/{cohortId}:sendAnnouncement` | `lms.cohort.send_cohort_announcement` | 501 | workflow / custom operation |
| `lms.GetCohortProgress` | GET | `/v1/lms/cohorts/{cohortId}:progress` | `lms.cohort.get_cohort_progress` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.GenerateCohortCertificates` | POST | `/v1/lms/cohorts/{cohortId}:generateCertificates` | `lms.cohort.generate_cohort_certificates` | 501 | workflow / custom operation |
| `lms.ScheduleLiveClass` | POST | `/v1/lms/live-classes:schedule` | `lms.live_class.schedule_live_class` | 501 | workflow / custom operation |
| `lms.GetLiveClassStartUrl` | GET | `/v1/lms/live-classes/{liveClassId}:startUrl` | `lms.live_class.get_live_class_start_url` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.JoinLiveClass` | POST | `/v1/lms/live-classes/{liveClassId}:join` | `lms.live_class.join_live_class` | 501 | workflow / custom operation |
| `lms.RetryLiveClassInvites` | POST | `/v1/lms/live-classes/{liveClassId}:retryInvites` | `lms.live_class.retry_live_class_invites` | 501 | workflow / custom operation |
| `lms.ReceiveConferencingWebhook` | POST | `/v1/lms/integrations/{provider}:webhook` | `lms.webhook.receive` | 501 | workflow / custom operation |
| `lms.ConnectEvaluatorCalendar` | POST | `/v1/lms/evaluators/{evaluatorId}:connectCalendar` | `lms.evaluator.connect_evaluator_calendar` | 501 | workflow / custom operation |
| `lms.DisconnectEvaluatorCalendar` | POST | `/v1/lms/evaluators/{evaluatorId}:disconnectCalendar` | `lms.evaluator.disconnect_evaluator_calendar` | 501 | workflow / custom operation |
| `lms.CancelCertificateRequest` | POST | `/v1/lms/certificate-requests/{certificateRequestId}:cancel` | `lms.certificate_request.cancel_certificate_request` | 501 | workflow / custom operation |
| `lms.StartEvaluation` | POST | `/v1/lms/certificate-requests/{certificateRequestId}:startEvaluation` | `lms.certificate_request.start_evaluation` | 501 | workflow / custom operation |
| `lms.IssueCertificateFromEvaluation` | POST | `/v1/lms/certificate-evaluations/{certificateEvaluationId}:issueCertificate` | `lms.certificate_evaluation.issue_certificate_from_evaluation` | 501 | workflow / custom operation |
| `lms.VerifyCertificate` | GET | `/v1/lms/certificates/{certificateId}:verify` | `lms.certificate.verify_certificate` | 501 | workflow / custom operation |
| `lms.DownloadCertificate` | GET | `/v1/lms/certificates/{certificateId}:download` | `lms.certificate.download_certificate` | 501 | workflow / custom operation |
| `lms.AwardBadge` | POST | `/v1/lms/badges/{badgeId}:award` | `lms.badge.award_badge` | 501 | workflow / custom operation |
| `lms.EnrollInLearningProgram` | POST | `/v1/lms/learning-programs/{learningProgramId}:enroll` | `lms.learning_program.enroll_in_learning_program` | 501 | workflow / custom operation |
| `lms.GetStudentDashboard` | GET | `/v1/lms/me/dashboard` | `lms.enrollment.get_student_dashboard` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.GetLmsStatistics` | GET | `/v1/lms/statistics` | `lms.enrollment.get_lms_statistics` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.GetPublicSettings` | GET | `/v1/lms/public-settings` | `lms.settings.get_public_settings` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.SendContactMessage` | POST | `/v1/lms/contact` | `lms.settings.send_contact_message` | 501 | workflow / custom operation |
| `lms.GetMemberProfile` | GET | `/v1/lms/members/{userId}:profile` | `lms.enrollment.get_member_profile` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CreateCourseVersion` | POST | `/v1/lms/courses/{courseId}:createVersion` | `lms.course.create_course_version` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.DeactivateCourse` | POST | `/v1/lms/courses/{courseId}:deactivate` | `lms.course.deactivate_course` | 501 | workflow / custom operation |
| `lms.ReactivateCourse` | POST | `/v1/lms/courses/{courseId}:reactivate` | `lms.course.reactivate_course` | 501 | workflow / custom operation |
| `lms.PublishCatalogListing` | POST | `/v1/lms/courses/{courseId}:publishCatalogListing` | `lms.course.publish_catalog_listing` | 501 | workflow / custom operation |
| `lms.HideCatalogListing` | POST | `/v1/lms/courses/{courseId}:hideCatalogListing` | `lms.course.hide_catalog_listing` | 501 | workflow / custom operation |
| `lms.CheckCourseDependencies` | GET | `/v1/lms/courses/{courseId}:dependencies` | `lms.course.check_course_dependencies` | 501 | workflow / custom operation |
| `lms.GetCourseSyllabus` | GET | `/v1/lms/courses/{courseId}:syllabus` | `lms.course.get_course_syllabus` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.GetCatalogHome` | GET | `/v1/lms/catalog` | `lms.course.get_catalog_home` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.CompareCourses` | GET | `/v1/lms/catalog:compare` | `lms.course.compare_courses` | 501 | workflow / custom operation |
| `lms.MoveChapter` | POST | `/v1/lms/chapters/{chapterId}:move` | `lms.chapter.move_chapter` | 501 | workflow / custom operation |
| `lms.MoveContentItem` | POST | `/v1/lms/content-items/{contentItemId}:move` | `lms.content_item.move_content_item` | 501 | workflow / custom operation |
| `lms.CopyContentItem` | POST | `/v1/lms/content-items/{contentItemId}:copy` | `lms.content_item.copy_content_item` | 501 | workflow / custom operation |
| `lms.AddFromRepository` | POST | `/v1/lms/content-items:addFromRepository` | `lms.content_item.add_from_repository` | 501 | workflow / custom operation |
| `lms.SetContentItemVisibility` | POST | `/v1/lms/content-items/{contentItemId}:setVisibility` | `lms.content_item.set_content_item_visibility` | 501 | workflow / custom operation |
| `lms.GetWebResourceLaunch` | GET | `/v1/lms/content-items/{contentItemId}:launch` | `lms.content_item.get_web_resource_launch` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ActivateCohort` | POST | `/v1/lms/cohorts/{cohortId}:activate` | `lms.cohort.activate_cohort` | 501 | workflow / custom operation |
| `lms.DeactivateCohort` | POST | `/v1/lms/cohorts/{cohortId}:deactivate` | `lms.cohort.deactivate_cohort` | 501 | workflow / custom operation |
| `lms.ArchiveCohort` | POST | `/v1/lms/cohorts/{cohortId}:archive` | `lms.cohort.archive_cohort` | 501 | workflow / custom operation |
| `lms.CopyCourseOutlineToCohort` | POST | `/v1/lms/cohorts/{cohortId}:copyCourseOutline` | `lms.cohort.copy_course_outline_to_cohort` | 501 | workflow / custom operation |
| `lms.GetCohortRoster` | GET | `/v1/lms/cohorts/{cohortId}:roster` | `lms.cohort.get_cohort_roster` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.SetCohortMembers` | POST | `/v1/lms/cohorts/{cohortId}:setMembers` | `lms.cohort.set_cohort_members` | 501 | workflow / custom operation |
| `lms.ImportCohortMembers` | POST | `/v1/lms/cohorts/{cohortId}:importMembers` | `lms.cohort.import_cohort_members` | 501 | workflow / custom operation |
| `lms.SetStudentAccess` | POST | `/v1/lms/cohorts/{cohortId}:setStudentAccess` | `lms.cohort.set_student_access` | 501 | workflow / custom operation |
| `lms.GetCohortSyllabus` | GET | `/v1/lms/cohorts/{cohortId}:syllabus` | `lms.cohort.get_cohort_syllabus` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.GetGradebook` | GET | `/v1/lms/cohorts/{cohortId}:gradebook` | `lms.cohort.get_gradebook` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.GetCohortPerformance` | GET | `/v1/lms/cohorts/{cohortId}:performance` | `lms.cohort.get_cohort_performance` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ExportGrades` | POST | `/v1/lms/cohorts/{cohortId}:exportGrades` | `lms.cohort.export_grades` | 501 | workflow / custom operation |
| `lms.PublishFinalGrades` | POST | `/v1/lms/cohorts/{cohortId}:publishFinalGrades` | `lms.cohort.publish_final_grades` | 501 | workflow / custom operation |
| `lms.EvaluateReleaseRules` | POST | `/v1/lms/cohorts/{cohortId}:evaluateReleaseRules` | `lms.cohort.evaluate_release_rules` | 501 | workflow / custom operation |
| `lms.GradeEntry` | POST | `/v1/lms/gradebook-entries:grade` | `lms.gradebook_entry.grade_entry` | 501 | workflow / custom operation |
| `lms.PublishItemGrades` | POST | `/v1/lms/content-items/{contentItemId}:publishGrades` | `lms.content_item.publish_item_grades` | 501 | workflow / custom operation |
| `lms.ReassignItem` | POST | `/v1/lms/content-items/{contentItemId}:reassign` | `lms.content_item.reassign_item` | 501 | workflow / custom operation |
| `lms.DownloadSubmissions` | POST | `/v1/lms/content-items/{contentItemId}:downloadSubmissions` | `lms.content_item.download_submissions` | 501 | workflow / custom operation |
| `lms.GetReleaseStatus` | GET | `/v1/lms/content-items/{contentItemId}:releaseStatus` | `lms.content_item.get_release_status` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.GrantItemAccess` | POST | `/v1/lms/content-items/{contentItemId}:grantAccess` | `lms.content_item.grant_item_access` | 501 | workflow / custom operation |
| `lms.SetReleaseRulesEnabled` | POST | `/v1/lms/content-items/{contentItemId}:setReleaseRulesEnabled` | `lms.content_item.set_release_rules_enabled` | 501 | workflow / custom operation |
| `lms.ReorderReleaseRules` | POST | `/v1/lms/content-items/{contentItemId}:reorderReleaseRules` | `lms.content_item.reorder_release_rules` | 501 | workflow / custom operation |
| `lms.EnableReleaseRule` | POST | `/v1/lms/release-rules/{releaseRuleId}:enable` | `lms.release_rule.enable_release_rule` | 501 | workflow / custom operation |
| `lms.DisableReleaseRule` | POST | `/v1/lms/release-rules/{releaseRuleId}:disable` | `lms.release_rule.disable_release_rule` | 501 | workflow / custom operation |
| `lms.CopyReleaseRule` | POST | `/v1/lms/release-rules/{releaseRuleId}:copyTo` | `lms.release_rule.copy_release_rule` | 501 | workflow / custom operation |
| `lms.FinalizeSubmission` | POST | `/v1/lms/assignment-submissions/{assignmentSubmissionId}:finalize` | `lms.assignment_submission.finalize_submission` | 501 | workflow / custom operation |
| `lms.RequestToJoinGroup` | POST | `/v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:requestToJoin` | `lms.assignment_group.request_to_join_group` | 501 | workflow / custom operation |
| `lms.SetGroupMembers` | POST | `/v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:setMembers` | `lms.assignment_group.set_group_members` | 501 | workflow / custom operation |
| `lms.AcceptGroupRequest` | POST | `/v1/lms/assignment-group-members/{assignmentGroupMemberId}:accept` | `lms.assignment_group_member.accept_group_request` | served |  |
| `lms.RejectGroupRequest` | POST | `/v1/lms/assignment-group-members/{assignmentGroupMemberId}:reject` | `lms.assignment_group_member.reject_group_request` | served |  |
| `lms.SubmitPeerReview` | POST | `/v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:submitPeerReview` | `lms.assignment_group.submit_peer_review` | 501 | workflow / custom operation |
| `lms.FinalizePeerReview` | POST | `/v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:finalizePeerReview` | `lms.assignment_group.finalize_peer_review` | 501 | workflow / custom operation |
| `lms.SubmitForPlagiarismCheck` | POST | `/v1/lms/plagiarism-reports:submit` | `lms.plagiarism_report.submit_for_plagiarism_check` | 501 | workflow / custom operation |
| `lms.GetPlagiarismReportLink` | GET | `/v1/lms/plagiarism-reports/{plagiarismReportId}:reportLink` | `lms.plagiarism_report.get_plagiarism_report_link` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ReceivePlagiarismWebhook` | POST | `/v1/lms/integrations/plagiarism/{provider}:webhook` | `lms.webhook.receive` | 501 | workflow / custom operation |
| `lms.SaveQuizResponses` | POST | `/v1/lms/quiz-submissions/{quizSubmissionId}:saveResponses` | `lms.quiz_submission.save_quiz_responses` | 501 | workflow / custom operation |
| `lms.PauseQuizAttempt` | POST | `/v1/lms/quiz-submissions/{quizSubmissionId}:pause` | `lms.quiz_submission.pause_quiz_attempt` | 501 | workflow / custom operation |
| `lms.ResumeQuizAttempt` | POST | `/v1/lms/quiz-submissions/{quizSubmissionId}:resume` | `lms.quiz_submission.resume_quiz_attempt` | 501 | workflow / custom operation |
| `lms.GradeQuizAnswers` | POST | `/v1/lms/quiz-submissions/{quizSubmissionId}:gradeAnswers` | `lms.quiz_submission.grade_quiz_answers` | 501 | workflow / custom operation |
| `lms.SetQuizQuestions` | POST | `/v1/lms/quizzes/{quizId}:setQuestions` | `lms.quiz.set_quiz_questions` | 501 | workflow / custom operation |
| `lms.ImportQuestions` | POST | `/v1/lms/questions:import` | `lms.question.import_questions` | 501 | workflow / custom operation |
| `lms.MarkTopicRead` | POST | `/v1/lms/forum-topics/{forumTopicId}:markRead` | `lms.forum_topic.mark_topic_read` | 501 | workflow / custom operation |
| `lms.HideForumTopic` | POST | `/v1/lms/forum-topics/{forumTopicId}:hide` | `lms.forum_topic.hide_forum_topic` | served |  |
| `lms.UnhideForumTopic` | POST | `/v1/lms/forum-topics/{forumTopicId}:unhide` | `lms.forum_topic.unhide_forum_topic` | served |  |
| `lms.HideForumPost` | POST | `/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:hide` | `lms.forum_post.hide_forum_post` | 501 | workflow / custom operation |
| `lms.UnhideForumPost` | POST | `/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:unhide` | `lms.forum_post.unhide_forum_post` | 501 | workflow / custom operation |
| `lms.ConfirmPostAbuse` | POST | `/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:confirmAbuse` | `lms.forum_post.confirm_post_abuse` | 501 | workflow / custom operation |
| `lms.FlagForumPost` | POST | `/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:flag` | `lms.forum_post.flag_forum_post` | 501 | workflow / custom operation |
| `lms.MarkPostSubstantive` | POST | `/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:markSubstantive` | `lms.forum_post.mark_post_substantive` | 501 | workflow / custom operation |
| `lms.GetUnreadCounts` | GET | `/v1/lms/me/unread-counts` | `lms.forum.get_unread_counts` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.EndLiveClass` | POST | `/v1/lms/live-classes/{liveClassId}:end` | `lms.live_class.end_live_class` | 501 | workflow / custom operation |
| `lms.SyncLiveClassAttendance` | POST | `/v1/lms/live-classes/{liveClassId}:syncAttendance` | `lms.live_class.sync_live_class_attendance` | 501 | workflow / custom operation |
| `lms.ConvertEbook` | POST | `/v1/lms/ebooks/{ebookId}:convert` | `lms.ebook.convert_ebook` | 501 | workflow / custom operation |
| `lms.ReceiveEbookConversion` | POST | `/v1/lms/integrations/conversion/{provider}:webhook` | `lms.webhook.receive` | 501 | workflow / custom operation |
| `lms.PublishEbookCuration` | POST | `/v1/lms/ebooks/{ebookId}:publishCuration` | `lms.ebook.publish_ebook_curation` | 501 | workflow / custom operation |
| `lms.GetMyRelatedMaterials` | GET | `/v1/lms/ebooks/{ebookId}:myRelatedMaterials` | `lms.ebook.get_my_related_materials` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.RecordItemView` | POST | `/v1/lms/content-items/{contentItemId}:recordView` | `lms.content_item.record_item_view` | 501 | workflow / custom operation |
| `lms.LaunchExternalTool` | POST | `/v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId}:launch` | `lms.external_tool_link.launch_external_tool` | 501 | workflow / custom operation |
| `lms.ReceiveToolOutcome` | POST | `/v1/lms/integrations/lti:outcome` | `lms.webhook.receive` | 501 | workflow / custom operation |
| `lms.ActivateExternalTool` | POST | `/v1/lms/external-tools/{externalToolId}:activate` | `lms.external_tool.activate_external_tool` | 501 | workflow / custom operation |
| `lms.ApproveEnrollmentRequest` | POST | `/v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}:approve` | `lms.enrollment_request.approve_enrollment_request` | 501 | workflow / custom operation |
| `lms.RejectEnrollmentRequest` | POST | `/v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}:reject` | `lms.enrollment_request.reject_enrollment_request` | served |  |
| `lms.GetTeacherDashboard` | GET | `/v1/lms/me/teacher-dashboard` | `lms.course.get_teacher_dashboard` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.BatchGradeEntries` | POST | `/v1/lms/gradebook-entries:batchGrade` | `lms.gradebook_entry.batch_grade_entries` | 501 | workflow / custom operation |
| `lms.ListImportExportJobs` | GET | `/v1/lms/import-export-jobs` | `lms.import_export_job.read` | served |  |
| `lms.CreateImportExportJob` | POST | `/v1/lms/import-export-jobs` | `lms.import_export_job.create` | served |  |
| `lms.GetImportExportJob` | GET | `/v1/lms/import-export-jobs/{importExportJobId}` | `lms.import_export_job.read` | served |  |
| `lms.ListAccessLogs` | GET | `/v1/lms/access-logs` | `lms.access_log.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.GetAccessLog` | GET | `/v1/lms/access-logs/{accessLogId}` | `lms.access_log.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ListDriveItems` | GET | `/v1/lms/drive-items` | `lms.drive_item.read` | served |  |
| `lms.CreateDriveItem` | POST | `/v1/lms/drive-items` | `lms.drive_item.create` | served |  |
| `lms.GetDriveItem` | GET | `/v1/lms/drive-items/{driveItemId}` | `lms.drive_item.read` | served |  |
| `lms.UpdateDriveItem` | PATCH | `/v1/lms/drive-items/{driveItemId}` | `lms.drive_item.update` | served |  |
| `lms.DeleteDriveItem` | DELETE | `/v1/lms/drive-items/{driveItemId}` | `lms.drive_item.delete` | served |  |
| `lms.ListDriveItemShares` | GET | `/v1/lms/drive-items/{driveItemId}/shares` | `lms.drive_item_share.read` | served |  |
| `lms.CreateDriveItemShare` | POST | `/v1/lms/drive-items/{driveItemId}/shares` | `lms.drive_item_share.create` | served |  |
| `lms.UpdateDriveItemShare` | PATCH | `/v1/lms/drive-items/{driveItemId}/shares/{driveItemShareId}` | `lms.drive_item_share.update` | served |  |
| `lms.DeleteDriveItemShare` | DELETE | `/v1/lms/drive-items/{driveItemId}/shares/{driveItemShareId}` | `lms.drive_item_share.delete` | served |  |
| `lms.CancelImportExportJob` | POST | `/v1/lms/import-export-jobs/{importExportJobId}:cancel` | `lms.import_export_job.cancel_import_export_job` | served |  |
| `lms.RestoreCohortArchive` | POST | `/v1/lms/import-export-jobs/{importExportJobId}:restore` | `lms.import_export_job.restore_cohort_archive` | 501 | workflow / custom operation |
| `lms.GetJobPackage` | GET | `/v1/lms/import-export-jobs/{importExportJobId}:package` | `lms.import_export_job.get_job_package` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `lms.ShareDriveItem` | POST | `/v1/lms/drive-items/{driveItemId}:share` | `lms.drive_item.share_drive_item` | 501 | workflow / custom operation |
| `lms.ListOperations` | GET | `/v1/lms/operations` | `lms.operation.read` | served |  |
| `lms.GetOperation` | GET | `/v1/lms/operations/{operationId}` | `lms.operation.read` | served |  |
| `lms.CancelOperation` | POST | `/v1/lms/operations/{operationId}:cancel` | `lms.operation.cancel_operation` | served |  |

## master

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `master.ListCountries` | GET | `/v1/master/countries` | `master.country.read` | served |  |
| `master.CreateCountry` | POST | `/v1/master/countries` | `master.country.create` | served |  |
| `master.GetCountry` | GET | `/v1/master/countries/{countryId}` | `master.country.read` | served |  |
| `master.UpdateCountry` | PATCH | `/v1/master/countries/{countryId}` | `master.country.update` | served |  |
| `master.DeleteCountry` | DELETE | `/v1/master/countries/{countryId}` | `master.country.delete` | served |  |
| `master.ListStateProvinces` | GET | `/v1/master/states` | `master.state_province.read` | served |  |
| `master.CreateStateProvince` | POST | `/v1/master/states` | `master.state_province.create` | served |  |
| `master.GetStateProvince` | GET | `/v1/master/states/{stateProvinceId}` | `master.state_province.read` | served |  |
| `master.UpdateStateProvince` | PATCH | `/v1/master/states/{stateProvinceId}` | `master.state_province.update` | served |  |
| `master.DeleteStateProvince` | DELETE | `/v1/master/states/{stateProvinceId}` | `master.state_province.delete` | served |  |
| `master.ListCities` | GET | `/v1/master/cities` | `master.city.read` | served |  |
| `master.CreateCity` | POST | `/v1/master/cities` | `master.city.create` | served |  |
| `master.GetCity` | GET | `/v1/master/cities/{cityId}` | `master.city.read` | served |  |
| `master.UpdateCity` | PATCH | `/v1/master/cities/{cityId}` | `master.city.update` | served |  |
| `master.DeleteCity` | DELETE | `/v1/master/cities/{cityId}` | `master.city.delete` | served |  |
| `master.ListCurrencies` | GET | `/v1/master/currencies` | `master.currency.read` | served |  |
| `master.CreateCurrency` | POST | `/v1/master/currencies` | `master.currency.create` | served |  |
| `master.GetCurrency` | GET | `/v1/master/currencies/{currencyId}` | `master.currency.read` | served |  |
| `master.UpdateCurrency` | PATCH | `/v1/master/currencies/{currencyId}` | `master.currency.update` | served |  |
| `master.DeleteCurrency` | DELETE | `/v1/master/currencies/{currencyId}` | `master.currency.delete` | served |  |

## org

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `org.ListInstitutions` | GET | `/v1/org/institutions` | `org.institution.read` | served |  |
| `org.CreateInstitution` | POST | `/v1/org/institutions` | `org.institution.create` | served |  |
| `org.GetInstitution` | GET | `/v1/org/institutions/{institutionId}` | `org.institution.read` | served |  |
| `org.UpdateInstitution` | PATCH | `/v1/org/institutions/{institutionId}` | `org.institution.update` | served |  |
| `org.DeleteInstitution` | DELETE | `/v1/org/institutions/{institutionId}` | `org.institution.delete` | served |  |
| `org.ListInstitutionAttributes` | GET | `/v1/org/institutions/{institutionId}/attributes` | `org.institution_attribute.read` | served |  |
| `org.CreateInstitutionAttribute` | POST | `/v1/org/institutions/{institutionId}/attributes` | `org.institution_attribute.create` | served |  |
| `org.GetInstitutionAttribute` | GET | `/v1/org/institutions/{institutionId}/attributes/{institutionAttributeId}` | `org.institution_attribute.read` | served |  |
| `org.UpdateInstitutionAttribute` | PATCH | `/v1/org/institutions/{institutionId}/attributes/{institutionAttributeId}` | `org.institution_attribute.update` | served |  |
| `org.DeleteInstitutionAttribute` | DELETE | `/v1/org/institutions/{institutionId}/attributes/{institutionAttributeId}` | `org.institution_attribute.delete` | served |  |
| `org.ListDepartments` | GET | `/v1/org/departments` | `org.department.read` | served |  |
| `org.CreateDepartment` | POST | `/v1/org/departments` | `org.department.create` | served |  |
| `org.GetDepartment` | GET | `/v1/org/departments/{departmentId}` | `org.department.read` | served |  |
| `org.UpdateDepartment` | PATCH | `/v1/org/departments/{departmentId}` | `org.department.update` | served |  |
| `org.DeleteDepartment` | DELETE | `/v1/org/departments/{departmentId}` | `org.department.delete` | served |  |
| `org.ListAcademicYears` | GET | `/v1/org/academic-years` | `org.academic_year.read` | served |  |
| `org.CreateAcademicYear` | POST | `/v1/org/academic-years` | `org.academic_year.create` | served |  |
| `org.GetAcademicYear` | GET | `/v1/org/academic-years/{academicYearId}` | `org.academic_year.read` | served |  |
| `org.UpdateAcademicYear` | PATCH | `/v1/org/academic-years/{academicYearId}` | `org.academic_year.update` | served |  |
| `org.DeleteAcademicYear` | DELETE | `/v1/org/academic-years/{academicYearId}` | `org.academic_year.delete` | served |  |
| `org.ListAcademicTerms` | GET | `/v1/org/academic-years/{academicYearId}/terms` | `org.academic_term.read` | served |  |
| `org.CreateAcademicTerm` | POST | `/v1/org/academic-years/{academicYearId}/terms` | `org.academic_term.create` | served |  |
| `org.GetAcademicTerm` | GET | `/v1/org/academic-years/{academicYearId}/terms/{academicTermId}` | `org.academic_term.read` | served |  |
| `org.UpdateAcademicTerm` | PATCH | `/v1/org/academic-years/{academicYearId}/terms/{academicTermId}` | `org.academic_term.update` | served |  |
| `org.DeleteAcademicTerm` | DELETE | `/v1/org/academic-years/{academicYearId}/terms/{academicTermId}` | `org.academic_term.delete` | served |  |
| `org.ListInstitutionTypes` | GET | `/v1/org/institution-types` | `org.institution_type.read` | served |  |
| `org.CreateInstitutionType` | POST | `/v1/org/institution-types` | `org.institution_type.create` | served |  |
| `org.GetInstitutionType` | GET | `/v1/org/institution-types/{institutionTypeId}` | `org.institution_type.read` | served |  |
| `org.UpdateInstitutionType` | PATCH | `/v1/org/institution-types/{institutionTypeId}` | `org.institution_type.update` | served |  |
| `org.DeleteInstitutionType` | DELETE | `/v1/org/institution-types/{institutionTypeId}` | `org.institution_type.delete` | served |  |
| `org.ListAttributeDefinitions` | GET | `/v1/org/attribute-definitions` | `org.attribute_definition.read` | served |  |
| `org.CreateAttributeDefinition` | POST | `/v1/org/attribute-definitions` | `org.attribute_definition.create` | served |  |
| `org.GetAttributeDefinition` | GET | `/v1/org/attribute-definitions/{attributeDefinitionId}` | `org.attribute_definition.read` | served |  |
| `org.UpdateAttributeDefinition` | PATCH | `/v1/org/attribute-definitions/{attributeDefinitionId}` | `org.attribute_definition.update` | served |  |
| `org.DeleteAttributeDefinition` | DELETE | `/v1/org/attribute-definitions/{attributeDefinitionId}` | `org.attribute_definition.delete` | served |  |
| `org.ListInstitutionTypeAttributes` | GET | `/v1/org/institution-types/{institutionTypeId}/attributes` | `org.institution_type_attribute.read` | served |  |
| `org.CreateInstitutionTypeAttribute` | POST | `/v1/org/institution-types/{institutionTypeId}/attributes` | `org.institution_type_attribute.create` | served |  |
| `org.GetInstitutionTypeAttribute` | GET | `/v1/org/institution-types/{institutionTypeId}/attributes/{institutionTypeAttributeId}` | `org.institution_type_attribute.read` | served |  |
| `org.UpdateInstitutionTypeAttribute` | PATCH | `/v1/org/institution-types/{institutionTypeId}/attributes/{institutionTypeAttributeId}` | `org.institution_type_attribute.update` | served |  |
| `org.DeleteInstitutionTypeAttribute` | DELETE | `/v1/org/institution-types/{institutionTypeId}/attributes/{institutionTypeAttributeId}` | `org.institution_type_attribute.delete` | served |  |
| `org.ListHolidays` | GET | `/v1/org/holidays` | `org.holiday.read` | served |  |
| `org.CreateHoliday` | POST | `/v1/org/holidays` | `org.holiday.create` | served |  |
| `org.GetHoliday` | GET | `/v1/org/holidays/{holidayId}` | `org.holiday.read` | served |  |
| `org.UpdateHoliday` | PATCH | `/v1/org/holidays/{holidayId}` | `org.holiday.update` | served |  |
| `org.DeleteHoliday` | DELETE | `/v1/org/holidays/{holidayId}` | `org.holiday.delete` | served |  |
| `org.FindInstitutions` | GET | `/v1/org/institutions:find` | `org.institution.find_institutions` | 501 | workflow / custom operation |
| `org.ActivateAcademicYear` | POST | `/v1/org/academic-years/{academicYearId}:activate` | `org.academic_year.activate_academic_year` | 501 | workflow / custom operation |
| `org.CloseAcademicYear` | POST | `/v1/org/academic-years/{academicYearId}:close` | `org.academic_year.close_academic_year` | served |  |
| `org.GenerateTerms` | POST | `/v1/org/academic-years/{academicYearId}:generateTerms` | `org.academic_year.generate_terms` | 501 | workflow / custom operation |

## payroll

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `payroll.ListPayComponents` | GET | `/v1/payroll/pay-components` | `payroll.pay_component.read` | served |  |
| `payroll.CreatePayComponent` | POST | `/v1/payroll/pay-components` | `payroll.pay_component.create` | served |  |
| `payroll.GetPayComponent` | GET | `/v1/payroll/pay-components/{payComponentId}` | `payroll.pay_component.read` | served |  |
| `payroll.UpdatePayComponent` | PATCH | `/v1/payroll/pay-components/{payComponentId}` | `payroll.pay_component.update` | served |  |
| `payroll.DeletePayComponent` | DELETE | `/v1/payroll/pay-components/{payComponentId}` | `payroll.pay_component.delete` | served |  |
| `payroll.ListPayCategories` | GET | `/v1/payroll/pay-categories` | `payroll.pay_category.read` | served |  |
| `payroll.CreatePayCategory` | POST | `/v1/payroll/pay-categories` | `payroll.pay_category.create` | served |  |
| `payroll.GetPayCategory` | GET | `/v1/payroll/pay-categories/{payCategoryId}` | `payroll.pay_category.read` | served |  |
| `payroll.UpdatePayCategory` | PATCH | `/v1/payroll/pay-categories/{payCategoryId}` | `payroll.pay_category.update` | served |  |
| `payroll.DeletePayCategory` | DELETE | `/v1/payroll/pay-categories/{payCategoryId}` | `payroll.pay_category.delete` | served |  |
| `payroll.ListSalaryTemplates` | GET | `/v1/payroll/salary-templates` | `payroll.salary_template.read` | served |  |
| `payroll.CreateSalaryTemplate` | POST | `/v1/payroll/salary-templates` | `payroll.salary_template.create` | served |  |
| `payroll.GetSalaryTemplate` | GET | `/v1/payroll/salary-templates/{salaryTemplateId}` | `payroll.salary_template.read` | served |  |
| `payroll.UpdateSalaryTemplate` | PATCH | `/v1/payroll/salary-templates/{salaryTemplateId}` | `payroll.salary_template.update` | served |  |
| `payroll.DeleteSalaryTemplate` | DELETE | `/v1/payroll/salary-templates/{salaryTemplateId}` | `payroll.salary_template.delete` | served |  |
| `payroll.ListSalaryTemplateComponents` | GET | `/v1/payroll/salary-templates/{salaryTemplateId}/components` | `payroll.salary_template_component.read` | served |  |
| `payroll.CreateSalaryTemplateComponent` | POST | `/v1/payroll/salary-templates/{salaryTemplateId}/components` | `payroll.salary_template_component.create` | served |  |
| `payroll.GetSalaryTemplateComponent` | GET | `/v1/payroll/salary-templates/{salaryTemplateId}/components/{salaryTemplateComponentId}` | `payroll.salary_template_component.read` | served |  |
| `payroll.UpdateSalaryTemplateComponent` | PATCH | `/v1/payroll/salary-templates/{salaryTemplateId}/components/{salaryTemplateComponentId}` | `payroll.salary_template_component.update` | served |  |
| `payroll.DeleteSalaryTemplateComponent` | DELETE | `/v1/payroll/salary-templates/{salaryTemplateId}/components/{salaryTemplateComponentId}` | `payroll.salary_template_component.delete` | served |  |
| `payroll.ListStaffSalaries` | GET | `/v1/payroll/staff-salaries` | `payroll.staff_salary.read` | served |  |
| `payroll.CreateStaffSalary` | POST | `/v1/payroll/staff-salaries` | `payroll.staff_salary.create` | served |  |
| `payroll.GetStaffSalary` | GET | `/v1/payroll/staff-salaries/{staffSalaryId}` | `payroll.staff_salary.read` | served |  |
| `payroll.UpdateStaffSalary` | PATCH | `/v1/payroll/staff-salaries/{staffSalaryId}` | `payroll.staff_salary.update` | served |  |
| `payroll.DeleteStaffSalary` | DELETE | `/v1/payroll/staff-salaries/{staffSalaryId}` | `payroll.staff_salary.delete` | served |  |
| `payroll.ListStaffSalaryComponents` | GET | `/v1/payroll/staff-salaries/{staffSalaryId}/components` | `payroll.staff_salary_component.read` | served |  |
| `payroll.CreateStaffSalaryComponent` | POST | `/v1/payroll/staff-salaries/{staffSalaryId}/components` | `payroll.staff_salary_component.create` | served |  |
| `payroll.GetStaffSalaryComponent` | GET | `/v1/payroll/staff-salaries/{staffSalaryId}/components/{staffSalaryComponentId}` | `payroll.staff_salary_component.read` | served |  |
| `payroll.UpdateStaffSalaryComponent` | PATCH | `/v1/payroll/staff-salaries/{staffSalaryId}/components/{staffSalaryComponentId}` | `payroll.staff_salary_component.update` | served |  |
| `payroll.DeleteStaffSalaryComponent` | DELETE | `/v1/payroll/staff-salaries/{staffSalaryId}/components/{staffSalaryComponentId}` | `payroll.staff_salary_component.delete` | served |  |
| `payroll.ListPayslips` | GET | `/v1/payroll/payslips` | `payroll.payslip.read` | served |  |
| `payroll.GetPayslip` | GET | `/v1/payroll/payslips/{payslipId}` | `payroll.payslip.read` | served |  |
| `payroll.ListPayslipLines` | GET | `/v1/payroll/payslips/{payslipId}/lines` | `payroll.payslip_line.read` | served |  |
| `payroll.GetPayslipLine` | GET | `/v1/payroll/payslips/{payslipId}/lines/{payslipLineId}` | `payroll.payslip_line.read` | served |  |
| `payroll.ListPaymentTypes` | GET | `/v1/payroll/payment-types` | `payroll.payment_type.read` | served |  |
| `payroll.CreatePaymentType` | POST | `/v1/payroll/payment-types` | `payroll.payment_type.create` | served |  |
| `payroll.GetPaymentType` | GET | `/v1/payroll/payment-types/{paymentTypeId}` | `payroll.payment_type.read` | served |  |
| `payroll.UpdatePaymentType` | PATCH | `/v1/payroll/payment-types/{paymentTypeId}` | `payroll.payment_type.update` | served |  |
| `payroll.DeletePaymentType` | DELETE | `/v1/payroll/payment-types/{paymentTypeId}` | `payroll.payment_type.delete` | served |  |
| `payroll.ListDisbursementAccounts` | GET | `/v1/payroll/disbursement-accounts` | `payroll.disbursement_account.read` | served |  |
| `payroll.CreateDisbursementAccount` | POST | `/v1/payroll/disbursement-accounts` | `payroll.disbursement_account.create` | served |  |
| `payroll.GetDisbursementAccount` | GET | `/v1/payroll/disbursement-accounts/{disbursementAccountId}` | `payroll.disbursement_account.read` | served |  |
| `payroll.UpdateDisbursementAccount` | PATCH | `/v1/payroll/disbursement-accounts/{disbursementAccountId}` | `payroll.disbursement_account.update` | served |  |
| `payroll.DeleteDisbursementAccount` | DELETE | `/v1/payroll/disbursement-accounts/{disbursementAccountId}` | `payroll.disbursement_account.delete` | served |  |
| `payroll.ListPayrollPayments` | GET | `/v1/payroll/payroll-payments` | `payroll.payroll_payment.read` | served |  |
| `payroll.CreatePayrollPayment` | POST | `/v1/payroll/payroll-payments` | `payroll.payroll_payment.create` | served |  |
| `payroll.GetPayrollPayment` | GET | `/v1/payroll/payroll-payments/{payrollPaymentId}` | `payroll.payroll_payment.read` | served |  |
| `payroll.GeneratePayslips` | POST | `/v1/payroll/payslips:generate` | `payroll.payslip.generate_payslips` | 501 | workflow / custom operation |
| `payroll.MarkPayslipPaid` | POST | `/v1/payroll/payslips/{payslipId}:markPaid` | `payroll.payslip.mark_payslip_paid` | 501 | workflow / custom operation |
| `payroll.CancelPayslip` | POST | `/v1/payroll/payslips/{payslipId}:cancel` | `payroll.payslip.cancel_payslip` | served |  |
| `payroll.ListStaffBankAccounts` | GET | `/v1/payroll/staff-bank-accounts` | `payroll.staff_bank_account.read` | served |  |
| `payroll.CreateStaffBankAccount` | POST | `/v1/payroll/staff-bank-accounts` | `payroll.staff_bank_account.create` | served |  |
| `payroll.GetStaffBankAccount` | GET | `/v1/payroll/staff-bank-accounts/{staffBankAccountId}` | `payroll.staff_bank_account.read` | served |  |
| `payroll.UpdateStaffBankAccount` | PATCH | `/v1/payroll/staff-bank-accounts/{staffBankAccountId}` | `payroll.staff_bank_account.update` | served |  |
| `payroll.DeleteStaffBankAccount` | DELETE | `/v1/payroll/staff-bank-accounts/{staffBankAccountId}` | `payroll.staff_bank_account.delete` | served |  |
| `payroll.ListOperations` | GET | `/v1/payroll/operations` | `payroll.operation.read` | served |  |
| `payroll.GetOperation` | GET | `/v1/payroll/operations/{operationId}` | `payroll.operation.read` | served |  |
| `payroll.CancelOperation` | POST | `/v1/payroll/operations/{operationId}:cancel` | `payroll.operation.cancel_operation` | served |  |

## people

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `people.ListHonorifics` | GET | `/v1/people/honorifics` | `people.honorific.read` | served |  |
| `people.CreateHonorific` | POST | `/v1/people/honorifics` | `people.honorific.create` | served |  |
| `people.GetHonorific` | GET | `/v1/people/honorifics/{honorificId}` | `people.honorific.read` | served |  |
| `people.UpdateHonorific` | PATCH | `/v1/people/honorifics/{honorificId}` | `people.honorific.update` | served |  |
| `people.DeleteHonorific` | DELETE | `/v1/people/honorifics/{honorificId}` | `people.honorific.delete` | served |  |
| `people.ListPersons` | GET | `/v1/people/persons` | `people.person.read` | served |  |
| `people.CreatePerson` | POST | `/v1/people/persons` | `people.person.create` | served |  |
| `people.GetPerson` | GET | `/v1/people/persons/{personId}` | `people.person.read` | served |  |
| `people.UpdatePerson` | PATCH | `/v1/people/persons/{personId}` | `people.person.update` | served |  |
| `people.DeletePerson` | DELETE | `/v1/people/persons/{personId}` | `people.person.delete` | served |  |
| `people.ListPersonAddresses` | GET | `/v1/people/persons/{personId}/addresses` | `people.person_address.read` | served |  |
| `people.CreatePersonAddress` | POST | `/v1/people/persons/{personId}/addresses` | `people.person_address.create` | served |  |
| `people.GetPersonAddress` | GET | `/v1/people/persons/{personId}/addresses/{personAddressId}` | `people.person_address.read` | served |  |
| `people.UpdatePersonAddress` | PATCH | `/v1/people/persons/{personId}/addresses/{personAddressId}` | `people.person_address.update` | served |  |
| `people.DeletePersonAddress` | DELETE | `/v1/people/persons/{personId}/addresses/{personAddressId}` | `people.person_address.delete` | served |  |
| `people.ListEmergencyContacts` | GET | `/v1/people/persons/{personId}/emergency-contacts` | `people.emergency_contact.read` | served |  |
| `people.CreateEmergencyContact` | POST | `/v1/people/persons/{personId}/emergency-contacts` | `people.emergency_contact.create` | served |  |
| `people.GetEmergencyContact` | GET | `/v1/people/persons/{personId}/emergency-contacts/{emergencyContactId}` | `people.emergency_contact.read` | served |  |
| `people.UpdateEmergencyContact` | PATCH | `/v1/people/persons/{personId}/emergency-contacts/{emergencyContactId}` | `people.emergency_contact.update` | served |  |
| `people.DeleteEmergencyContact` | DELETE | `/v1/people/persons/{personId}/emergency-contacts/{emergencyContactId}` | `people.emergency_contact.delete` | served |  |
| `people.ListQualifications` | GET | `/v1/people/qualifications` | `people.qualification.read` | served |  |
| `people.CreateQualification` | POST | `/v1/people/qualifications` | `people.qualification.create` | served |  |
| `people.GetQualification` | GET | `/v1/people/qualifications/{qualificationId}` | `people.qualification.read` | served |  |
| `people.UpdateQualification` | PATCH | `/v1/people/qualifications/{qualificationId}` | `people.qualification.update` | served |  |
| `people.DeleteQualification` | DELETE | `/v1/people/qualifications/{qualificationId}` | `people.qualification.delete` | served |  |
| `people.ListEducationHistory` | GET | `/v1/people/persons/{personId}/education` | `people.education_history.read` | served |  |
| `people.CreateEducationHistory` | POST | `/v1/people/persons/{personId}/education` | `people.education_history.create` | served |  |
| `people.GetEducationHistory` | GET | `/v1/people/persons/{personId}/education/{educationHistoryId}` | `people.education_history.read` | served |  |
| `people.UpdateEducationHistory` | PATCH | `/v1/people/persons/{personId}/education/{educationHistoryId}` | `people.education_history.update` | served |  |
| `people.DeleteEducationHistory` | DELETE | `/v1/people/persons/{personId}/education/{educationHistoryId}` | `people.education_history.delete` | served |  |
| `people.ListWorkExperiences` | GET | `/v1/people/persons/{personId}/work-experience` | `people.work_experience.read` | served |  |
| `people.CreateWorkExperience` | POST | `/v1/people/persons/{personId}/work-experience` | `people.work_experience.create` | served |  |
| `people.GetWorkExperience` | GET | `/v1/people/persons/{personId}/work-experience/{workExperienceId}` | `people.work_experience.read` | served |  |
| `people.UpdateWorkExperience` | PATCH | `/v1/people/persons/{personId}/work-experience/{workExperienceId}` | `people.work_experience.update` | served |  |
| `people.DeleteWorkExperience` | DELETE | `/v1/people/persons/{personId}/work-experience/{workExperienceId}` | `people.work_experience.delete` | served |  |
| `people.ListCertifications` | GET | `/v1/people/persons/{personId}/certifications` | `people.certification.read` | served |  |
| `people.CreateCertification` | POST | `/v1/people/persons/{personId}/certifications` | `people.certification.create` | served |  |
| `people.GetCertification` | GET | `/v1/people/persons/{personId}/certifications/{certificationId}` | `people.certification.read` | served |  |
| `people.UpdateCertification` | PATCH | `/v1/people/persons/{personId}/certifications/{certificationId}` | `people.certification.update` | served |  |
| `people.DeleteCertification` | DELETE | `/v1/people/persons/{personId}/certifications/{certificationId}` | `people.certification.delete` | served |  |
| `people.ListSkills` | GET | `/v1/people/skills` | `people.skill.read` | served |  |
| `people.CreateSkill` | POST | `/v1/people/skills` | `people.skill.create` | served |  |
| `people.GetSkill` | GET | `/v1/people/skills/{skillId}` | `people.skill.read` | served |  |
| `people.UpdateSkill` | PATCH | `/v1/people/skills/{skillId}` | `people.skill.update` | served |  |
| `people.DeleteSkill` | DELETE | `/v1/people/skills/{skillId}` | `people.skill.delete` | served |  |
| `people.ListPersonSkills` | GET | `/v1/people/persons/{personId}/skills` | `people.person_skill.read` | served |  |
| `people.CreatePersonSkill` | POST | `/v1/people/persons/{personId}/skills` | `people.person_skill.create` | served |  |
| `people.GetPersonSkill` | GET | `/v1/people/persons/{personId}/skills/{personSkillId}` | `people.person_skill.read` | served |  |
| `people.UpdatePersonSkill` | PATCH | `/v1/people/persons/{personId}/skills/{personSkillId}` | `people.person_skill.update` | served |  |
| `people.DeletePersonSkill` | DELETE | `/v1/people/persons/{personId}/skills/{personSkillId}` | `people.person_skill.delete` | served |  |
| `people.ListPersonDocuments` | GET | `/v1/people/persons/{personId}/documents` | `people.person_document.read` | served |  |
| `people.CreatePersonDocument` | POST | `/v1/people/persons/{personId}/documents` | `people.person_document.create` | served |  |
| `people.GetPersonDocument` | GET | `/v1/people/persons/{personId}/documents/{personDocumentId}` | `people.person_document.read` | served |  |
| `people.UpdatePersonDocument` | PATCH | `/v1/people/persons/{personId}/documents/{personDocumentId}` | `people.person_document.update` | served |  |
| `people.DeletePersonDocument` | DELETE | `/v1/people/persons/{personId}/documents/{personDocumentId}` | `people.person_document.delete` | served |  |
| `people.CreatePublicProfile` | POST | `/v1/people/persons/{personId}/public-profile` | `people.public_profile.create` | served |  |
| `people.GetPublicProfile` | GET | `/v1/people/persons/{personId}/public-profile/{publicProfileId}` | `people.public_profile.read` | served |  |
| `people.UpdatePublicProfile` | PATCH | `/v1/people/persons/{personId}/public-profile/{publicProfileId}` | `people.public_profile.update` | served |  |
| `people.DeletePublicProfile` | DELETE | `/v1/people/persons/{personId}/public-profile/{publicProfileId}` | `people.public_profile.delete` | served |  |
| `people.ListPersonSocialLinks` | GET | `/v1/people/persons/{personId}/social-links` | `people.person_social_link.read` | served |  |
| `people.CreatePersonSocialLink` | POST | `/v1/people/persons/{personId}/social-links` | `people.person_social_link.create` | served |  |
| `people.GetPersonSocialLink` | GET | `/v1/people/persons/{personId}/social-links/{personSocialLinkId}` | `people.person_social_link.read` | served |  |
| `people.UpdatePersonSocialLink` | PATCH | `/v1/people/persons/{personId}/social-links/{personSocialLinkId}` | `people.person_social_link.update` | served |  |
| `people.DeletePersonSocialLink` | DELETE | `/v1/people/persons/{personId}/social-links/{personSocialLinkId}` | `people.person_social_link.delete` | served |  |
| `people.ListConsentRecords` | GET | `/v1/people/persons/{personId}/consents` | `people.consent_record.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `people.CreateConsentRecord` | POST | `/v1/people/persons/{personId}/consents` | `people.consent_record.create` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `people.GetConsentRecord` | GET | `/v1/people/persons/{personId}/consents/{consentRecordId}` | `people.consent_record.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `people.ListBirthdays` | GET | `/v1/people/persons:birthdays` | `people.person.list_birthdays` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `people.SendGreetings` | POST | `/v1/people/persons:sendGreetings` | `people.person.send_greetings` | 501 | workflow / custom operation |
| `people.WithdrawConsent` | POST | `/v1/people/persons/{personId}/consents/{consentRecordId}:withdraw` | `people.consent_record.withdraw_consent` | 501 | workflow / custom operation |
| `people.GetConsentStatus` | GET | `/v1/people/persons/{personId}:consentStatus` | `people.person.get_consent_status` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `people.GetMyPerson` | GET | `/v1/people/me` | `people.person.read_mine` | served |  |
| `people.UpdateMyPerson` | PATCH | `/v1/people/me` | `people.person.update_mine` | served |  |

## platform

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `platform.ListTenants` | GET | `/v1/platform/tenants` | `platform.tenant.read` | served |  |
| `platform.CreateTenant` | POST | `/v1/platform/tenants` | `platform.tenant.create` | served |  |
| `platform.GetTenant` | GET | `/v1/platform/tenants/{tenantId}` | `platform.tenant.read` | served |  |
| `platform.UpdateTenant` | PATCH | `/v1/platform/tenants/{tenantId}` | `platform.tenant.update` | served |  |
| `platform.DeleteTenant` | DELETE | `/v1/platform/tenants/{tenantId}` | `platform.tenant.delete` | served |  |
| `platform.ListSaasPlans` | GET | `/v1/platform/saas-plans` | `platform.saas_plan.read` | served |  |
| `platform.CreateSaasPlan` | POST | `/v1/platform/saas-plans` | `platform.saas_plan.create` | served |  |
| `platform.GetSaasPlan` | GET | `/v1/platform/saas-plans/{saasPlanId}` | `platform.saas_plan.read` | served |  |
| `platform.UpdateSaasPlan` | PATCH | `/v1/platform/saas-plans/{saasPlanId}` | `platform.saas_plan.update` | served |  |
| `platform.DeleteSaasPlan` | DELETE | `/v1/platform/saas-plans/{saasPlanId}` | `platform.saas_plan.delete` | served |  |
| `platform.ListTenantSubscriptions` | GET | `/v1/platform/tenant-subscriptions` | `platform.tenant_subscription.read` | served |  |
| `platform.GetTenantSubscription` | GET | `/v1/platform/tenant-subscriptions/{tenantSubscriptionId}` | `platform.tenant_subscription.read` | served |  |
| `platform.ListFileObjects` | GET | `/v1/platform/files` | `platform.file_object.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.GetFileObject` | GET | `/v1/platform/files/{fileObjectId}` | `platform.file_object.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.DeleteFileObject` | DELETE | `/v1/platform/files/{fileObjectId}` | `platform.file_object.delete` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.ListEntityAttachments` | GET | `/v1/platform/attachments` | `platform.entity_attachment.read` | served |  |
| `platform.CreateEntityAttachment` | POST | `/v1/platform/attachments` | `platform.entity_attachment.create` | served |  |
| `platform.DeleteEntityAttachment` | DELETE | `/v1/platform/attachments/{entityAttachmentId}` | `platform.entity_attachment.delete` | served |  |
| `platform.ListTags` | GET | `/v1/platform/tags` | `platform.tag.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.CreateTag` | POST | `/v1/platform/tags` | `platform.tag.create` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.GetTag` | GET | `/v1/platform/tags/{tagId}` | `platform.tag.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.UpdateTag` | PATCH | `/v1/platform/tags/{tagId}` | `platform.tag.update` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.DeleteTag` | DELETE | `/v1/platform/tags/{tagId}` | `platform.tag.delete` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.ListTagAssignments` | GET | `/v1/platform/tag-assignments` | `platform.tag_assignment.read` | served |  |
| `platform.CreateTagAssignment` | POST | `/v1/platform/tag-assignments` | `platform.tag_assignment.create` | served |  |
| `platform.DeleteTagAssignment` | DELETE | `/v1/platform/tag-assignments/{tagAssignmentId}` | `platform.tag_assignment.delete` | served |  |
| `platform.ListEntityNotes` | GET | `/v1/platform/entity-notes` | `platform.entity_note.read` | served |  |
| `platform.CreateEntityNote` | POST | `/v1/platform/entity-notes` | `platform.entity_note.create` | served |  |
| `platform.GetEntityNote` | GET | `/v1/platform/entity-notes/{entityNoteId}` | `platform.entity_note.read` | served |  |
| `platform.UpdateEntityNote` | PATCH | `/v1/platform/entity-notes/{entityNoteId}` | `platform.entity_note.update` | served |  |
| `platform.DeleteEntityNote` | DELETE | `/v1/platform/entity-notes/{entityNoteId}` | `platform.entity_note.delete` | served |  |
| `platform.ListAuditLogs` | GET | `/v1/platform/audit-logs` | `platform.audit_log.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.GetAuditLog` | GET | `/v1/platform/audit-logs/{auditLogId}` | `platform.audit_log.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.ListReservedKeywords` | GET | `/v1/platform/reserved-keywords` | `platform.reserved_keyword.read` | served |  |
| `platform.CreateReservedKeyword` | POST | `/v1/platform/reserved-keywords` | `platform.reserved_keyword.create` | served |  |
| `platform.GetReservedKeyword` | GET | `/v1/platform/reserved-keywords/{reservedKeywordId}` | `platform.reserved_keyword.read` | served |  |
| `platform.UpdateReservedKeyword` | PATCH | `/v1/platform/reserved-keywords/{reservedKeywordId}` | `platform.reserved_keyword.update` | served |  |
| `platform.DeleteReservedKeyword` | DELETE | `/v1/platform/reserved-keywords/{reservedKeywordId}` | `platform.reserved_keyword.delete` | served |  |
| `platform.ListSalesEnquiries` | GET | `/v1/platform/sales-enquiries` | `platform.sales_enquiry.read` | served |  |
| `platform.CreateSalesEnquiry` | POST | `/v1/platform/sales-enquiries` | `platform.sales_enquiry.create` | served |  |
| `platform.GetSalesEnquiry` | GET | `/v1/platform/sales-enquiries/{salesEnquiryId}` | `platform.sales_enquiry.read` | served |  |
| `platform.UpdateSalesEnquiry` | PATCH | `/v1/platform/sales-enquiries/{salesEnquiryId}` | `platform.sales_enquiry.update` | served |  |
| `platform.DeleteSalesEnquiry` | DELETE | `/v1/platform/sales-enquiries/{salesEnquiryId}` | `platform.sales_enquiry.delete` | served |  |
| `platform.ListSystemSettings` | GET | `/v1/platform/system-settings` | `platform.system_setting.read` | served |  |
| `platform.CreateSystemSetting` | POST | `/v1/platform/system-settings` | `platform.system_setting.create` | served |  |
| `platform.GetSystemSetting` | GET | `/v1/platform/system-settings/{systemSettingId}` | `platform.system_setting.read` | served |  |
| `platform.UpdateSystemSetting` | PATCH | `/v1/platform/system-settings/{systemSettingId}` | `platform.system_setting.update` | served |  |
| `platform.DeleteSystemSetting` | DELETE | `/v1/platform/system-settings/{systemSettingId}` | `platform.system_setting.delete` | served |  |
| `platform.ListThemes` | GET | `/v1/platform/themes` | `platform.theme.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.CreateTheme` | POST | `/v1/platform/themes` | `platform.theme.create` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.GetTheme` | GET | `/v1/platform/themes/{themeId}` | `platform.theme.read` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.UpdateTheme` | PATCH | `/v1/platform/themes/{themeId}` | `platform.theme.update` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.DeleteTheme` | DELETE | `/v1/platform/themes/{themeId}` | `platform.theme.delete` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.ListUploadPolicies` | GET | `/v1/platform/upload-policies` | `platform.upload_policy.read` | served |  |
| `platform.CreateUploadPolicy` | POST | `/v1/platform/upload-policies` | `platform.upload_policy.create` | served |  |
| `platform.GetUploadPolicy` | GET | `/v1/platform/upload-policies/{uploadPolicyId}` | `platform.upload_policy.read` | served |  |
| `platform.UpdateUploadPolicy` | PATCH | `/v1/platform/upload-policies/{uploadPolicyId}` | `platform.upload_policy.update` | served |  |
| `platform.DeleteUploadPolicy` | DELETE | `/v1/platform/upload-policies/{uploadPolicyId}` | `platform.upload_policy.delete` | served |  |
| `platform.ListDeletionLogs` | GET | `/v1/platform/deletion-logs` | `platform.deletion_log.read` | served |  |
| `platform.GetDeletionLog` | GET | `/v1/platform/deletion-logs/{deletionLogId}` | `platform.deletion_log.read` | served |  |
| `platform.ListStorageQuotas` | GET | `/v1/platform/storage-quotas` | `platform.storage_quota.read` | served |  |
| `platform.CreateStorageQuota` | POST | `/v1/platform/storage-quotas` | `platform.storage_quota.create` | served |  |
| `platform.GetStorageQuota` | GET | `/v1/platform/storage-quotas/{storageQuotaId}` | `platform.storage_quota.read` | served |  |
| `platform.UpdateStorageQuota` | PATCH | `/v1/platform/storage-quotas/{storageQuotaId}` | `platform.storage_quota.update` | served |  |
| `platform.DeleteStorageQuota` | DELETE | `/v1/platform/storage-quotas/{storageQuotaId}` | `platform.storage_quota.delete` | served |  |
| `platform.CreateUploadUrl` | POST | `/v1/platform/files:createUploadUrl` | `platform.file_object.create_upload_url` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.CompleteUpload` | POST | `/v1/platform/files/{fileObjectId}:complete` | `platform.file_object.complete_upload` | 501 | workflow / custom operation |
| `platform.GetDownloadUrl` | GET | `/v1/platform/files/{fileObjectId}:downloadUrl` | `platform.file_object.get_download_url` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.GetBranding` | GET | `/v1/platform/branding` | `platform.tenant.get_branding` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.ClearDemoData` | POST | `/v1/platform/tenants/{tenantId}:clearDemoData` | `platform.tenant.clear_demo_data` | 501 | workflow / custom operation |
| `platform.GlobalSearch` | GET | `/v1/platform/search` | `platform.tenant.global_search` | 501 | workflow / custom operation |
| `platform.GetOperation` | GET | `/v1/platform/operations/{operationId}` | `platform.operation.read` | served |  |
| `platform.ListOperations` | GET | `/v1/platform/operations` | `platform.operation.read` | served |  |
| `platform.CancelOperation` | POST | `/v1/platform/operations/{operationId}:cancel` | `platform.operation.cancel_operation` | served |  |
| `platform.ListReportDefinitions` | GET | `/v1/platform/reports` | `platform.tenant.list_report_definitions` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.RunReport` | POST | `/v1/platform/reports:run` | `platform.tenant.run_report` | 501 | workflow / custom operation |
| `platform.GetImportTemplate` | GET | `/v1/platform/imports/templates/{entityType}` | `platform.tenant.get_import_template` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.StartImport` | POST | `/v1/platform/imports` | `platform.tenant.start_import` | 501 | workflow / custom operation |
| `platform.SearchStockImages` | GET | `/v1/platform/stock-images` | `platform.tenant.search_stock_images` | 501 | workflow / custom operation |
| `platform.StartSubscriptionCheckout` | POST | `/v1/platform/tenant-subscriptions:checkout` | `platform.tenant_subscription.start_subscription_checkout` | 501 | workflow / custom operation |
| `platform.CancelSubscription` | POST | `/v1/platform/tenant-subscriptions/{tenantSubscriptionId}:cancel` | `platform.tenant_subscription.cancel_subscription` | 501 | workflow / custom operation |
| `platform.MarkEnquiryContacted` | POST | `/v1/platform/sales-enquiries/{salesEnquiryId}:markContacted` | `platform.sales_enquiry.mark_enquiry_contacted` | 501 | workflow / custom operation |
| `platform.GetMyStorageUsage` | GET | `/v1/platform/me/storage` | `platform.storage_quota.get_my_storage_usage` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `platform.CopyFile` | POST | `/v1/platform/files/{fileObjectId}:copy` | `platform.file_object.copy_file` | 501 | workflow / custom operation |

## sis

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `sis.ListStudentCategories` | GET | `/v1/sis/student-categories` | `sis.student_category.read` | served |  |
| `sis.CreateStudentCategory` | POST | `/v1/sis/student-categories` | `sis.student_category.create` | served |  |
| `sis.GetStudentCategory` | GET | `/v1/sis/student-categories/{studentCategoryId}` | `sis.student_category.read` | served |  |
| `sis.UpdateStudentCategory` | PATCH | `/v1/sis/student-categories/{studentCategoryId}` | `sis.student_category.update` | served |  |
| `sis.DeleteStudentCategory` | DELETE | `/v1/sis/student-categories/{studentCategoryId}` | `sis.student_category.delete` | served |  |
| `sis.ListStudents` | GET | `/v1/sis/students` | `sis.student.read` | served |  |
| `sis.CreateStudent` | POST | `/v1/sis/students` | `sis.student.create` | served |  |
| `sis.GetStudent` | GET | `/v1/sis/students/{studentId}` | `sis.student.read` | served |  |
| `sis.UpdateStudent` | PATCH | `/v1/sis/students/{studentId}` | `sis.student.update` | served |  |
| `sis.DeleteStudent` | DELETE | `/v1/sis/students/{studentId}` | `sis.student.delete` | served |  |
| `sis.ListGuardians` | GET | `/v1/sis/guardians` | `sis.guardian.read` | served |  |
| `sis.CreateGuardian` | POST | `/v1/sis/guardians` | `sis.guardian.create` | served |  |
| `sis.GetGuardian` | GET | `/v1/sis/guardians/{guardianId}` | `sis.guardian.read` | served |  |
| `sis.UpdateGuardian` | PATCH | `/v1/sis/guardians/{guardianId}` | `sis.guardian.update` | served |  |
| `sis.DeleteGuardian` | DELETE | `/v1/sis/guardians/{guardianId}` | `sis.guardian.delete` | served |  |
| `sis.ListRelationshipTypes` | GET | `/v1/sis/relationship-types` | `sis.relationship_type.read` | served |  |
| `sis.CreateRelationshipType` | POST | `/v1/sis/relationship-types` | `sis.relationship_type.create` | served |  |
| `sis.GetRelationshipType` | GET | `/v1/sis/relationship-types/{relationshipTypeId}` | `sis.relationship_type.read` | served |  |
| `sis.UpdateRelationshipType` | PATCH | `/v1/sis/relationship-types/{relationshipTypeId}` | `sis.relationship_type.update` | served |  |
| `sis.DeleteRelationshipType` | DELETE | `/v1/sis/relationship-types/{relationshipTypeId}` | `sis.relationship_type.delete` | served |  |
| `sis.ListStudentGuardians` | GET | `/v1/sis/students/{studentId}/guardians` | `sis.student_guardian.read` | served |  |
| `sis.CreateStudentGuardian` | POST | `/v1/sis/students/{studentId}/guardians` | `sis.student_guardian.create` | served |  |
| `sis.GetStudentGuardian` | GET | `/v1/sis/students/{studentId}/guardians/{studentGuardianId}` | `sis.student_guardian.read` | served |  |
| `sis.UpdateStudentGuardian` | PATCH | `/v1/sis/students/{studentId}/guardians/{studentGuardianId}` | `sis.student_guardian.update` | served |  |
| `sis.DeleteStudentGuardian` | DELETE | `/v1/sis/students/{studentId}/guardians/{studentGuardianId}` | `sis.student_guardian.delete` | served |  |
| `sis.ListStudentSiblings` | GET | `/v1/sis/students/{studentId}/siblings` | `sis.student_sibling.read` | served |  |
| `sis.CreateStudentSibling` | POST | `/v1/sis/students/{studentId}/siblings` | `sis.student_sibling.create` | served |  |
| `sis.GetStudentSibling` | GET | `/v1/sis/students/{studentId}/siblings/{studentSiblingId}` | `sis.student_sibling.read` | served |  |
| `sis.UpdateStudentSibling` | PATCH | `/v1/sis/students/{studentId}/siblings/{studentSiblingId}` | `sis.student_sibling.update` | served |  |
| `sis.DeleteStudentSibling` | DELETE | `/v1/sis/students/{studentId}/siblings/{studentSiblingId}` | `sis.student_sibling.delete` | served |  |
| `sis.ListStudentEnrollments` | GET | `/v1/sis/student-enrollments` | `sis.student_enrollment.read` | served |  |
| `sis.CreateStudentEnrollment` | POST | `/v1/sis/student-enrollments` | `sis.student_enrollment.create` | served |  |
| `sis.GetStudentEnrollment` | GET | `/v1/sis/student-enrollments/{studentEnrollmentId}` | `sis.student_enrollment.read` | served |  |
| `sis.UpdateStudentEnrollment` | PATCH | `/v1/sis/student-enrollments/{studentEnrollmentId}` | `sis.student_enrollment.update` | served |  |
| `sis.DeleteStudentEnrollment` | DELETE | `/v1/sis/student-enrollments/{studentEnrollmentId}` | `sis.student_enrollment.delete` | served |  |
| `sis.ListProgramEnrollments` | GET | `/v1/sis/program-enrollments` | `sis.program_enrollment.read` | served |  |
| `sis.CreateProgramEnrollment` | POST | `/v1/sis/program-enrollments` | `sis.program_enrollment.create` | served |  |
| `sis.GetProgramEnrollment` | GET | `/v1/sis/program-enrollments/{programEnrollmentId}` | `sis.program_enrollment.read` | served |  |
| `sis.UpdateProgramEnrollment` | PATCH | `/v1/sis/program-enrollments/{programEnrollmentId}` | `sis.program_enrollment.update` | served |  |
| `sis.DeleteProgramEnrollment` | DELETE | `/v1/sis/program-enrollments/{programEnrollmentId}` | `sis.program_enrollment.delete` | served |  |
| `sis.ListProgramEnrollmentSubjects` | GET | `/v1/sis/program-enrollments/{programEnrollmentId}/subjects` | `sis.program_enrollment_subject.read` | served |  |
| `sis.CreateProgramEnrollmentSubject` | POST | `/v1/sis/program-enrollments/{programEnrollmentId}/subjects` | `sis.program_enrollment_subject.create` | served |  |
| `sis.GetProgramEnrollmentSubject` | GET | `/v1/sis/program-enrollments/{programEnrollmentId}/subjects/{programEnrollmentSubjectId}` | `sis.program_enrollment_subject.read` | served |  |
| `sis.UpdateProgramEnrollmentSubject` | PATCH | `/v1/sis/program-enrollments/{programEnrollmentId}/subjects/{programEnrollmentSubjectId}` | `sis.program_enrollment_subject.update` | served |  |
| `sis.DeleteProgramEnrollmentSubject` | DELETE | `/v1/sis/program-enrollments/{programEnrollmentId}/subjects/{programEnrollmentSubjectId}` | `sis.program_enrollment_subject.delete` | served |  |
| `sis.ListSubjectRegistrations` | GET | `/v1/sis/subject-registrations` | `sis.subject_registration.read` | served |  |
| `sis.CreateSubjectRegistration` | POST | `/v1/sis/subject-registrations` | `sis.subject_registration.create` | served |  |
| `sis.GetSubjectRegistration` | GET | `/v1/sis/subject-registrations/{subjectRegistrationId}` | `sis.subject_registration.read` | served |  |
| `sis.UpdateSubjectRegistration` | PATCH | `/v1/sis/subject-registrations/{subjectRegistrationId}` | `sis.subject_registration.update` | served |  |
| `sis.DeleteSubjectRegistration` | DELETE | `/v1/sis/subject-registrations/{subjectRegistrationId}` | `sis.subject_registration.delete` | served |  |
| `sis.ListSubjectRegistrationLines` | GET | `/v1/sis/subject-registrations/{subjectRegistrationId}/lines` | `sis.subject_registration_line.read` | served |  |
| `sis.CreateSubjectRegistrationLine` | POST | `/v1/sis/subject-registrations/{subjectRegistrationId}/lines` | `sis.subject_registration_line.create` | served |  |
| `sis.GetSubjectRegistrationLine` | GET | `/v1/sis/subject-registrations/{subjectRegistrationId}/lines/{subjectRegistrationLineId}` | `sis.subject_registration_line.read` | served |  |
| `sis.UpdateSubjectRegistrationLine` | PATCH | `/v1/sis/subject-registrations/{subjectRegistrationId}/lines/{subjectRegistrationLineId}` | `sis.subject_registration_line.update` | served |  |
| `sis.DeleteSubjectRegistrationLine` | DELETE | `/v1/sis/subject-registrations/{subjectRegistrationId}/lines/{subjectRegistrationLineId}` | `sis.subject_registration_line.delete` | served |  |
| `sis.ListPromotions` | GET | `/v1/sis/promotions` | `sis.promotion.read` | served |  |
| `sis.CreatePromotion` | POST | `/v1/sis/promotions` | `sis.promotion.create` | served |  |
| `sis.GetPromotion` | GET | `/v1/sis/promotions/{promotionId}` | `sis.promotion.read` | served |  |
| `sis.UpdatePromotion` | PATCH | `/v1/sis/promotions/{promotionId}` | `sis.promotion.update` | served |  |
| `sis.DeletePromotion` | DELETE | `/v1/sis/promotions/{promotionId}` | `sis.promotion.delete` | served |  |
| `sis.ListStudentHealthRecords` | GET | `/v1/sis/student-health-records` | `sis.student_health_record.read` | served |  |
| `sis.CreateStudentHealthRecord` | POST | `/v1/sis/student-health-records` | `sis.student_health_record.create` | served |  |
| `sis.GetStudentHealthRecord` | GET | `/v1/sis/student-health-records/{studentHealthRecordId}` | `sis.student_health_record.read` | served |  |
| `sis.UpdateStudentHealthRecord` | PATCH | `/v1/sis/student-health-records/{studentHealthRecordId}` | `sis.student_health_record.update` | served |  |
| `sis.DeleteStudentHealthRecord` | DELETE | `/v1/sis/student-health-records/{studentHealthRecordId}` | `sis.student_health_record.delete` | served |  |
| `sis.ListDisciplineIncidents` | GET | `/v1/sis/discipline-incidents` | `sis.discipline_incident.read` | served |  |
| `sis.CreateDisciplineIncident` | POST | `/v1/sis/discipline-incidents` | `sis.discipline_incident.create` | served |  |
| `sis.GetDisciplineIncident` | GET | `/v1/sis/discipline-incidents/{disciplineIncidentId}` | `sis.discipline_incident.read` | served |  |
| `sis.UpdateDisciplineIncident` | PATCH | `/v1/sis/discipline-incidents/{disciplineIncidentId}` | `sis.discipline_incident.update` | served |  |
| `sis.DeleteDisciplineIncident` | DELETE | `/v1/sis/discipline-incidents/{disciplineIncidentId}` | `sis.discipline_incident.delete` | served |  |
| `sis.ListActivityTypes` | GET | `/v1/sis/activity-types` | `sis.activity_type.read` | served |  |
| `sis.CreateActivityType` | POST | `/v1/sis/activity-types` | `sis.activity_type.create` | served |  |
| `sis.GetActivityType` | GET | `/v1/sis/activity-types/{activityTypeId}` | `sis.activity_type.read` | served |  |
| `sis.UpdateActivityType` | PATCH | `/v1/sis/activity-types/{activityTypeId}` | `sis.activity_type.update` | served |  |
| `sis.DeleteActivityType` | DELETE | `/v1/sis/activity-types/{activityTypeId}` | `sis.activity_type.delete` | served |  |
| `sis.ListStudentActivities` | GET | `/v1/sis/student-activities` | `sis.student_activity.read` | served |  |
| `sis.CreateStudentActivity` | POST | `/v1/sis/student-activities` | `sis.student_activity.create` | served |  |
| `sis.GetStudentActivity` | GET | `/v1/sis/student-activities/{studentActivityId}` | `sis.student_activity.read` | served |  |
| `sis.UpdateStudentActivity` | PATCH | `/v1/sis/student-activities/{studentActivityId}` | `sis.student_activity.update` | served |  |
| `sis.DeleteStudentActivity` | DELETE | `/v1/sis/student-activities/{studentActivityId}` | `sis.student_activity.delete` | served |  |
| `sis.ChangeStudentStatus` | POST | `/v1/sis/students/{studentId}:changeStatus` | `sis.student.change_student_status` | 501 | workflow / custom operation |
| `sis.GetStudentSummary` | GET | `/v1/sis/students/{studentId}:summary` | `sis.student.get_student_summary` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `sis.IssueBonafideCertificate` | POST | `/v1/sis/students/{studentId}:issueBonafideCertificate` | `sis.student.issue_bonafide_certificate` | 501 | workflow / custom operation |
| `sis.CreateStudentPortalAccount` | POST | `/v1/sis/students/{studentId}:createPortalAccount` | `sis.student.create_student_portal_account` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `sis.CreatePortalAccount` | POST | `/v1/sis/guardians/{guardianId}:createPortalAccount` | `sis.guardian.create_portal_account` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `sis.ListMyChildren` | GET | `/v1/sis/me/children` | `sis.guardian.list_my_children` | 501 | standard operation not built yet (public, platform-staff or extra-parameter variant) |
| `sis.ProposePromotions` | POST | `/v1/sis/promotions:propose` | `sis.promotion.propose_promotions` | 501 | workflow / custom operation |
| `sis.ApprovePromotion` | POST | `/v1/sis/promotions/{promotionId}:approve` | `sis.promotion.approve_promotion` | served |  |
| `sis.RejectPromotion` | POST | `/v1/sis/promotions/{promotionId}:reject` | `sis.promotion.reject_promotion` | served |  |
| `sis.ApplyPromotions` | POST | `/v1/sis/promotions:apply` | `sis.promotion.apply_promotions` | 501 | workflow / custom operation |
| `sis.SubmitSubjectRegistration` | POST | `/v1/sis/subject-registrations/{subjectRegistrationId}:submit` | `sis.subject_registration.submit_subject_registration` | served |  |
| `sis.ApproveSubjectRegistration` | POST | `/v1/sis/subject-registrations/{subjectRegistrationId}:approve` | `sis.subject_registration.approve_subject_registration` | 501 | workflow / custom operation |
| `sis.RejectSubjectRegistration` | POST | `/v1/sis/subject-registrations/{subjectRegistrationId}:reject` | `sis.subject_registration.reject_subject_registration` | served |  |
| `sis.ResetSubjectRegistration` | POST | `/v1/sis/subject-registrations/{subjectRegistrationId}:resetToDraft` | `sis.subject_registration.reset_subject_registration` | served |  |
| `sis.LoadCourseSubjects` | POST | `/v1/sis/subject-registrations/{subjectRegistrationId}:loadCourseSubjects` | `sis.subject_registration.load_course_subjects` | 501 | workflow / custom operation |
| `sis.MarkIncidentActionTaken` | POST | `/v1/sis/discipline-incidents/{disciplineIncidentId}:markActionTaken` | `sis.discipline_incident.mark_incident_action_taken` | 501 | workflow / custom operation |
| `sis.MarkIncidentSeen` | POST | `/v1/sis/discipline-incidents/{disciplineIncidentId}:markSeen` | `sis.discipline_incident.mark_incident_seen` | 501 | workflow / custom operation |
| `sis.ListClassRoster` | GET | `/v1/sis/class-sections/{classSectionId}:roster` | `sis.student_enrollment.list_class_roster` | served |  |
| `sis.ListStudentLeaveRequests` | GET | `/v1/sis/student-leave-requests` | `sis.student_leave_request.read` | served |  |
| `sis.CreateStudentLeaveRequest` | POST | `/v1/sis/student-leave-requests` | `sis.student_leave_request.create` | served |  |
| `sis.GetStudentLeaveRequest` | GET | `/v1/sis/student-leave-requests/{studentLeaveRequestId}` | `sis.student_leave_request.read` | served |  |
| `sis.UpdateStudentLeaveRequest` | PATCH | `/v1/sis/student-leave-requests/{studentLeaveRequestId}` | `sis.student_leave_request.update` | served |  |
| `sis.DeleteStudentLeaveRequest` | DELETE | `/v1/sis/student-leave-requests/{studentLeaveRequestId}` | `sis.student_leave_request.delete` | served |  |
| `sis.ApproveStudentLeaveRequest` | POST | `/v1/sis/student-leave-requests/{studentLeaveRequestId}:approve` | `sis.student_leave_request.approve` | served |  |
| `sis.RejectStudentLeaveRequest` | POST | `/v1/sis/student-leave-requests/{studentLeaveRequestId}:reject` | `sis.student_leave_request.reject` | served |  |
| `sis.ListOperations` | GET | `/v1/sis/operations` | `sis.operation.read` | served |  |
| `sis.GetOperation` | GET | `/v1/sis/operations/{operationId}` | `sis.operation.read` | served |  |
| `sis.CancelOperation` | POST | `/v1/sis/operations/{operationId}:cancel` | `sis.operation.cancel_operation` | served |  |

## timetable

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `timetable.ListPeriods` | GET | `/v1/timetable/periods` | `timetable.period.read` | served |  |
| `timetable.CreatePeriod` | POST | `/v1/timetable/periods` | `timetable.period.create` | served |  |
| `timetable.GetPeriod` | GET | `/v1/timetable/periods/{periodId}` | `timetable.period.read` | served |  |
| `timetable.UpdatePeriod` | PATCH | `/v1/timetable/periods/{periodId}` | `timetable.period.update` | served |  |
| `timetable.DeletePeriod` | DELETE | `/v1/timetable/periods/{periodId}` | `timetable.period.delete` | served |  |
| `timetable.ListSessions` | GET | `/v1/timetable/sessions` | `timetable.session.read` | served |  |
| `timetable.CreateSession` | POST | `/v1/timetable/sessions` | `timetable.session.create` | served |  |
| `timetable.GetSession` | GET | `/v1/timetable/sessions/{sessionId}` | `timetable.session.read` | served |  |
| `timetable.UpdateSession` | PATCH | `/v1/timetable/sessions/{sessionId}` | `timetable.session.update` | served |  |
| `timetable.DeleteSession` | DELETE | `/v1/timetable/sessions/{sessionId}` | `timetable.session.delete` | served |  |
| `timetable.ListSessionStudents` | GET | `/v1/timetable/sessions/{sessionId}/students` | `timetable.session_student.read` | served |  |
| `timetable.CreateSessionStudent` | POST | `/v1/timetable/sessions/{sessionId}/students` | `timetable.session_student.create` | served |  |
| `timetable.GetSessionStudent` | GET | `/v1/timetable/sessions/{sessionId}/students/{sessionStudentId}` | `timetable.session_student.read` | served |  |
| `timetable.UpdateSessionStudent` | PATCH | `/v1/timetable/sessions/{sessionId}/students/{sessionStudentId}` | `timetable.session_student.update` | served |  |
| `timetable.DeleteSessionStudent` | DELETE | `/v1/timetable/sessions/{sessionId}/students/{sessionStudentId}` | `timetable.session_student.delete` | served |  |
| `timetable.ConfirmSession` | POST | `/v1/timetable/sessions/{sessionId}:confirm` | `timetable.session.confirm_session` | served |  |
| `timetable.CompleteSession` | POST | `/v1/timetable/sessions/{sessionId}:complete` | `timetable.session.complete_session` | served |  |
| `timetable.CancelSession` | POST | `/v1/timetable/sessions/{sessionId}:cancel` | `timetable.session.cancel_session` | served |  |
| `timetable.ResetSession` | POST | `/v1/timetable/sessions/{sessionId}:resetToDraft` | `timetable.session.reset_session` | served |  |
| `timetable.BatchConfirmSessions` | POST | `/v1/timetable/sessions:batchConfirm` | `timetable.session.batch_confirm_sessions` | 501 | workflow / custom operation |
| `timetable.GenerateSessions` | POST | `/v1/timetable/sessions:generate` | `timetable.session.generate_sessions` | 501 | workflow / custom operation |
| `timetable.GetTimetable` | GET | `/v1/timetable/sessions:timetable` | `timetable.session.get_timetable` | served |  |
| `timetable.ListOperations` | GET | `/v1/timetable/operations` | `timetable.operation.read` | served |  |
| `timetable.GetOperation` | GET | `/v1/timetable/operations/{operationId}` | `timetable.operation.read` | served |  |
| `timetable.CancelOperation` | POST | `/v1/timetable/operations/{operationId}:cancel` | `timetable.operation.cancel_operation` | served |  |

## transport

| Operation | Method | Path | Permission | Status | Why not served |
|---|---|---|---|---|---|
| `transport.ListRoutes` | GET | `/v1/transport/routes` | `transport.route.read` | served |  |
| `transport.CreateRoute` | POST | `/v1/transport/routes` | `transport.route.create` | served |  |
| `transport.GetRoute` | GET | `/v1/transport/routes/{routeId}` | `transport.route.read` | served |  |
| `transport.UpdateRoute` | PATCH | `/v1/transport/routes/{routeId}` | `transport.route.update` | served |  |
| `transport.DeleteRoute` | DELETE | `/v1/transport/routes/{routeId}` | `transport.route.delete` | served |  |
| `transport.ListRouteStops` | GET | `/v1/transport/routes/{routeId}/stops` | `transport.route_stop.read` | served |  |
| `transport.CreateRouteStop` | POST | `/v1/transport/routes/{routeId}/stops` | `transport.route_stop.create` | served |  |
| `transport.GetRouteStop` | GET | `/v1/transport/routes/{routeId}/stops/{routeStopId}` | `transport.route_stop.read` | served |  |
| `transport.UpdateRouteStop` | PATCH | `/v1/transport/routes/{routeId}/stops/{routeStopId}` | `transport.route_stop.update` | served |  |
| `transport.DeleteRouteStop` | DELETE | `/v1/transport/routes/{routeId}/stops/{routeStopId}` | `transport.route_stop.delete` | served |  |
| `transport.ListStudentTransports` | GET | `/v1/transport/student-transports` | `transport.student_transport.read` | served |  |
| `transport.CreateStudentTransport` | POST | `/v1/transport/student-transports` | `transport.student_transport.create` | served |  |
| `transport.GetStudentTransport` | GET | `/v1/transport/student-transports/{studentTransportId}` | `transport.student_transport.read` | served |  |
| `transport.UpdateStudentTransport` | PATCH | `/v1/transport/student-transports/{studentTransportId}` | `transport.student_transport.update` | served |  |
| `transport.DeleteStudentTransport` | DELETE | `/v1/transport/student-transports/{studentTransportId}` | `transport.student_transport.delete` | served |  |
