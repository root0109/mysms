package com.mysms.academics.grpc;

import com.mysms.academics.api.model.ClassSectionUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateClassSection over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.ClassSectionUpdate} message as REST's
 * {@link ClassSectionUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ClassSectionProtoBridge {

    private ClassSectionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ClassSectionUpdate changes(ProtoBridge bridge, com.mysms.academics.v1.ClassSectionUpdate request) {
        ClassSectionUpdate changes = new ClassSectionUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setGradeLevelId(bridge.uuid(request.hasGradeLevelId(), request.getGradeLevelId(), "grade_level_id"));
        changes.setSectionId(bridge.uuid(request.hasSectionId(), request.getSectionId(), "section_id"));
        changes.setClassTeacherStaffId(bridge.uuid(
                request.hasClassTeacherStaffId(), request.getClassTeacherStaffId(), "class_teacher_staff_id"));
        changes.setCapacity(ProtoBridge.number(request.hasCapacity(), request.getCapacity()));
        changes.setStream(ProtoBridge.text(request.hasStream(), request.getStream()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
