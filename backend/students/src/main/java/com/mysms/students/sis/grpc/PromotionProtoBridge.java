package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.sis.api.model.PromotionUpdate;

/**
 * UpdatePromotion over gRPC (JAVA-CODING-RULES §7.2): the {@code sis.v1.PromotionUpdate} message as REST's {@link
 * PromotionUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class PromotionProtoBridge {

    private PromotionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PromotionUpdate changes(ProtoBridge bridge, com.mysms.sis.v1.PromotionUpdate request) {
        PromotionUpdate changes = new PromotionUpdate();
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setFromAcademicYearId(bridge.uuid(
                request.hasFromAcademicYearId(), request.getFromAcademicYearId(), "from_academic_year_id"));
        changes.setFromGradeLevelId(bridge.uuid(
                request.hasFromGradeLevelId(), request.getFromGradeLevelId(), "from_grade_level_id"));
        changes.setFromSectionId(bridge.uuid(
                request.hasFromSectionId(), request.getFromSectionId(), "from_section_id"));
        changes.setToAcademicYearId(bridge.uuid(
                request.hasToAcademicYearId(), request.getToAcademicYearId(), "to_academic_year_id"));
        changes.setToGradeLevelId(bridge.uuid(
                request.hasToGradeLevelId(), request.getToGradeLevelId(), "to_grade_level_id"));
        changes.setToSectionId(bridge.uuid(request.hasToSectionId(), request.getToSectionId(), "to_section_id"));
        changes.setBasedOnExamSessionId(bridge.uuid(
                request.hasBasedOnExamSessionId(), request.getBasedOnExamSessionId(), "based_on_exam_session_id"));
        changes.setRemarks(ProtoBridge.text(request.hasRemarks(), request.getRemarks()));
        return bridge.patch(changes);
    }
}
