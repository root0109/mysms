package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.ExamSessionUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateExamSession over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.ExamSessionUpdate} message as REST's
 * {@link ExamSessionUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ExamSessionProtoBridge {

    private ExamSessionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ExamSessionUpdate changes(ProtoBridge bridge, com.mysms.assessment.v1.ExamSessionUpdate request) {
        ExamSessionUpdate changes = new ExamSessionUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setAcademicTermId(bridge.uuid(
                request.hasAcademicTermId(), request.getAcademicTermId(), "academic_term_id"));
        changes.setExamTypeId(bridge.uuid(request.hasExamTypeId(), request.getExamTypeId(), "exam_type_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setGradeLevelId(bridge.uuid(request.hasGradeLevelId(), request.getGradeLevelId(), "grade_level_id"));
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setBatchId(bridge.uuid(request.hasBatchId(), request.getBatchId(), "batch_id"));
        changes.setEvaluationType(bridge.choice(
                request.hasEvaluationType(), request.getEvaluationType(),
                ExamSessionUpdate.EvaluationTypeEnum::fromValue));
        changes.setStartDate(bridge.date(request.hasStartDate(), request.getStartDate(), "start_date"));
        changes.setEndDate(bridge.date(request.hasEndDate(), request.getEndDate(), "end_date"));
        changes.setVenue(ProtoBridge.text(request.hasVenue(), request.getVenue()));
        return bridge.patch(changes);
    }
}
